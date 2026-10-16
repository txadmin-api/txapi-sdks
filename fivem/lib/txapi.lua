--[[
    txApi SDK for FiveM / RedM (server-side Lua 5.4)  —  version 1.0.0, MIT
    https://txapi.cloud/docs

    Load order in YOUR resource's fxmanifest.lua:

        server_scripts {
            '@txapi/lib/endpoints.lua',
            '@txapi/lib/txapi.lua',
            'server.lua',
        }

    SERVER ONLY. Never load these files as client scripts: the client would need
    the API key, and every player could read it.

    Two calling styles for every method:

        -- blocking (inside Citizen.CreateThread, an event handler or a command):
        local res, err, resp = api.players.ban({ identifier = 'license:…', reason = 'Cheating' })

        -- callback (anywhere; the call returns immediately):
        api.players.ban({ identifier = 'license:…', reason = 'Cheating' }, function(res, err, resp) end)

    Errors are RETURNED, never thrown: `nil, err` where err.type is one of the
    names in TxApi.ERROR_TYPES. Only misconfiguration at TxApi.new() raises.
]]

TxApi = TxApi or {}

if type(TxApi._spec) ~= 'table' then
    error("txapi: load '@txapi/lib/endpoints.lua' BEFORE '@txapi/lib/txapi.lua' in your fxmanifest.lua", 0)
end

TxApi.VERSION = '1.0.0'
TxApi.API_VERSION = TxApi._spec.apiVersion
TxApi.DEFAULT_BASE_URL = 'https://txapi.cloud'
TxApi.WEBHOOK_EVENTS = TxApi._spec.webhookEvents

--- Every error `type` an SDK call can return (SPEC §4).
TxApi.ERROR_TYPES = {
    'BadRequestError', 'AuthenticationError', 'PermissionDeniedError', 'NotFoundError',
    'ConflictError', 'TxAdminRejectedError', 'RateLimitError', 'TxAdminUnavailableError',
    'ServerError', 'ApiError', 'ConnectionError', 'TimeoutError', 'ValidationError',
}

local REASONS = {
    [400] = 'Bad Request', [401] = 'Unauthorized', [403] = 'Forbidden', [404] = 'Not Found',
    [405] = 'Method Not Allowed', [409] = 'Conflict', [415] = 'Unsupported Media Type',
    [422] = 'Unprocessable Entity', [429] = 'Too Many Requests', [500] = 'Internal Server Error',
    [502] = 'Bad Gateway', [503] = 'Service Unavailable', [504] = 'Gateway Timeout',
}

local MAX_RETRY_AFTER = 60 -- seconds; a longer Retry-After is not waited for (SPEC §5)

-- ---------------------------------------------------------------------------
-- small helpers
-- ---------------------------------------------------------------------------

local function isNull(v)
    if v == nil then return true end
    -- FiveM's json (lua-rapidjson) decodes JSON null to json.null; dkjson-style libs to nil.
    local j = rawget(_G, 'json')
    if type(j) == 'table' and j.null ~= nil and v == j.null then return true end
    return false
end
TxApi.isNull = isNull

local function trim(s)
    return (tostring(s):gsub('^%s+', ''):gsub('%s+$', ''))
end

local function ulen(s)
    local n = utf8.len(s)
    return n or #s
end

--- Percent-encodes everything except the RFC 3986 unreserved characters.
local function urlencode(s)
    return (tostring(s):gsub('[^%w%-_%.~]', function(c)
        return string.format('%%%02X', c:byte())
    end))
end
TxApi.urlencode = urlencode

--- Numbers for the query string / JSON: integral floats are written as integers.
local function normalizeNumber(n)
    if math.type(n) == 'float' and n == math.floor(n) and n >= -2^53 and n <= 2^53 then
        return math.tointeger(n)
    end
    return n
end

local function numberToString(n)
    n = normalizeNumber(n)
    if math.type(n) == 'integer' then return string.format('%d', n) end
    return string.format('%.17g', n)
end

local function headerValue(headers, name)
    if type(headers) ~= 'table' then return nil end
    local wanted = name:lower()
    for k, v in pairs(headers) do
        if type(k) == 'string' and k:lower() == wanted then
            if type(v) == 'table' then v = v[1] end -- FiveM gives an array for repeated headers
            if v == nil then return nil end
            return trim(v)
        end
    end
    return nil
end
TxApi.header = headerValue

local function parseRateLimit(headers)
    local limit = tonumber(headerValue(headers, 'X-RateLimit-Limit') or '')
    local remaining = tonumber(headerValue(headers, 'X-RateLimit-Remaining') or '')
    local reset = tonumber(headerValue(headers, 'X-RateLimit-Reset') or '')
    if limit == nil and remaining == nil and reset == nil then return nil end
    return { limit = limit, remaining = remaining, resetSeconds = reset }
end
TxApi.parseRateLimit = parseRateLimit

local function setMeta(t, kind)
    -- Both FiveM json libraries (lua-rapidjson today, dkjson in older builds) honour
    -- __jsontype. Without it an EMPTY table is ambiguous: rapidjson writes {}, dkjson [].
    return setmetatable(t, { __jsontype = kind })
end

local function jsonEncode(v)
    local j = rawget(_G, 'json')
    if type(j) ~= 'table' or type(j.encode) ~= 'function' then
        error('txapi: the global `json` library is missing (FiveM provides it; are you running outside FXServer?)', 0)
    end
    return j.encode(v)
end

local function jsonDecode(text)
    if type(text) ~= 'string' or text == '' then return nil end
    local first = text:match('^%s*(.)')
    if first ~= '{' and first ~= '[' and first ~= '"' then return nil end
    local j = rawget(_G, 'json')
    if type(j) ~= 'table' or type(j.decode) ~= 'function' then return nil end
    local ok, value = pcall(j.decode, text)
    if not ok or value == nil then return nil end
    return value
end

local function now()
    local f = rawget(_G, 'GetGameTimer')
    if type(f) == 'function' then return f() end
    return math.floor(os.clock() * 1000)
end

-- ---------------------------------------------------------------------------
-- errors
-- ---------------------------------------------------------------------------

local ErrorMeta = {}
ErrorMeta.__index = ErrorMeta
ErrorMeta.__tostring = function(e)
    local s = e.type .. ': ' .. tostring(e.message)
    if e.status then s = e.type .. ' (HTTP ' .. e.status .. '): ' .. tostring(e.message) end
    return s
end

local function newError(t)
    t.isTxApiError = true
    return setmetatable(t, ErrorMeta)
end

--- true if `v` is an error returned by this SDK
function TxApi.isError(v)
    return type(v) == 'table' and getmetatable(v) == ErrorMeta
end

--- Maps an HTTP status to the SDK error type (SPEC §4).
function TxApi.errorTypeForStatus(status)
    if status == 400 or status == 405 or status == 415 then return 'BadRequestError' end
    if status == 401 then return 'AuthenticationError' end
    if status == 403 then return 'PermissionDeniedError' end
    if status == 404 then return 'NotFoundError' end
    if status == 409 then return 'ConflictError' end
    if status == 422 then return 'TxAdminRejectedError' end
    if status == 429 then return 'RateLimitError' end
    if status == 502 then return 'TxAdminUnavailableError' end
    if status >= 500 and status <= 599 then return 'ServerError' end
    return 'ApiError'
end

local function validationError(ep, field, message)
    return newError({ type = 'ValidationError', field = field, message = message, endpoint = ep and ep.id })
end

-- ---------------------------------------------------------------------------
-- endpoints & request building (SPEC §2)
-- ---------------------------------------------------------------------------

local ENDPOINTS, BY_ID, BY_LUA = TxApi._spec.endpoints, {}, {}
for _, ep in ipairs(ENDPOINTS) do
    BY_ID[ep.id] = ep
    BY_LUA[ep.lua] = ep
end

--- Looks up an endpoint by spec id ('server.resource.restart') or Lua name ('server.resources.restart').
function TxApi.endpoint(name)
    return BY_ID[name] or BY_LUA[name]
end

local function isBlank(v)
    return type(v) == 'string' and trim(v) == ''
end

-- Query parameters that take several values as one comma-separated string. A list is
-- joined here, like the other SDKs do (`filters = { 'isOnline', 'hasNote' }`).
local CSV_PARAMS = { ['players.search'] = { filters = true }, ['events.list'] = { type = true } }

local function queryValue(ep, p, v)
    local t = type(v)
    if t == 'table' and CSV_PARAMS[ep.id] and CSV_PARAMS[ep.id][p.name] then
        local parts = {}
        for i, item in ipairs(v) do
            if type(item) ~= 'string' or item == '' then
                return nil, validationError(ep, p.name, ("'%s'[%d] must be a non-empty string"):format(p.name, i))
            end
            parts[i] = item
        end
        if #parts == 0 then
            return nil, validationError(ep, p.name, ("'%s' must not be an empty list"):format(p.name))
        end
        return table.concat(parts, ',')
    end
    if t == 'boolean' then return v and 'true' or 'false' end
    if t == 'number' then return numberToString(v) end
    if t == 'string' then return v end
    return nil, validationError(ep, p.name, ("'%s' must be a %s, got %s"):format(p.name, p.type, t))
end

local function bodyValue(ep, b, v)
    local t = type(v)
    if b.type == 'string[]' then
        if t ~= 'table' then
            return nil, validationError(ep, b.name, ("'%s' must be a list of strings"):format(b.name))
        end
        local arr = {}
        for i, item in ipairs(v) do
            if type(item) ~= 'string' then
                return nil, validationError(ep, b.name, ("'%s'[%d] must be a string"):format(b.name, i))
            end
            arr[i] = item
        end
        return setMeta(arr, 'array')
    end
    if b.type == 'integer' then
        if t ~= 'number' then
            return nil, validationError(ep, b.name, ("'%s' must be an integer, got %s"):format(b.name, t))
        end
        return normalizeNumber(v)
    end
    if b.type == 'boolean' then
        if t ~= 'boolean' then
            return nil, validationError(ep, b.name, ("'%s' must be true or false, got %s"):format(b.name, t))
        end
        return v
    end
    -- string
    if t == 'number' then v = numberToString(v) elseif t ~= 'string' then
        return nil, validationError(ep, b.name, ("'%s' must be a string, got %s"):format(b.name, t))
    end
    if b.maxLength and ulen(v) > b.maxLength then
        return nil, validationError(ep, b.name, ("'%s' is longer than %d characters"):format(b.name, b.maxLength))
    end
    return v
end

--- Builds { method, url, body, hasBody } for an endpoint. Never touches the network.
-- `pathValues` is the list of positional path arguments; `opts` holds the named fields.
local function buildRequest(cfg, ep, pathValues, opts)
    opts = opts or {}
    if type(opts) ~= 'table' then
        return nil, validationError(ep, nil, 'expected a table of parameters, got ' .. type(opts))
    end

    local known = { serverId = true }
    for _, n in ipairs(ep.pathParams) do known[n] = true end
    for _, p in ipairs(ep.query) do known[p.name] = true end
    for _, b in ipairs(ep.body) do known[b.name] = true end
    for k in pairs(opts) do
        if not known[k] then
            -- a typo such as `durationhours` would otherwise silently turn a 24 h ban into a permanent one
            return nil, validationError(ep, tostring(k), ("unknown parameter '%s' for %s"):format(tostring(k), ep.lua))
        end
    end

    local path = ep.path
    for i, name in ipairs(ep.pathParams) do
        local v = pathValues[i]
        if isNull(v) then v = opts[name] end
        if isNull(v) or isBlank(v) or (type(v) ~= 'string' and type(v) ~= 'number') then
            return nil, validationError(ep, name, ("'%s' is required"):format(name))
        end
        if type(v) == 'number' then v = numberToString(v) end
        path = path:gsub('{' .. name .. '}', function() return urlencode(v) end)
    end

    local query = {}
    if not ep.accountLevel then
        local sid = opts.serverId
        if isNull(sid) then sid = cfg.serverId end
        if not isNull(sid) and not isBlank(sid) then
            local s, err = queryValue(ep, { name = 'serverId', type = 'integer' }, sid)
            if not s then return nil, err end
            query[#query + 1] = 'serverId=' .. urlencode(s)
        end
    end
    for _, p in ipairs(ep.query) do
        local v = opts[p.name]
        if isNull(v) or (p.required and isBlank(v)) then
            if p.required then return nil, validationError(ep, p.name, ("'%s' is required"):format(p.name)) end
        else
            local s, err = queryValue(ep, p, v)
            if not s then return nil, err end
            query[#query + 1] = urlencode(p.name) .. '=' .. urlencode(s)
        end
    end

    local body
    if ep.hasBody then
        body = setMeta({}, 'object')
        for _, b in ipairs(ep.body) do
            local v = opts[b.name]
            if isNull(v) or (b.required and isBlank(v)) then
                if b.required then return nil, validationError(ep, b.name, ("'%s' is required"):format(b.name)) end
            else
                local value, err = bodyValue(ep, b, v)
                if value == nil then return nil, err end
                body[b.name] = value
            end
        end
    end

    local url = cfg.baseUrl .. path
    if #query > 0 then url = url .. '?' .. table.concat(query, '&') end
    return { method = ep.method, url = url, path = path, body = body, hasBody = ep.hasBody }
end
TxApi._buildRequest = buildRequest

-- ---------------------------------------------------------------------------
-- transport & response handling
-- ---------------------------------------------------------------------------

--- One HTTP attempt. Must run inside a coroutine (Citizen.Await).
local function perform(cfg, req, headers, data)
    local p = promise.new()
    local settled = false
    local function settle(v)
        if settled then return end
        settled = true
        p:resolve(v)
    end
    local transport = cfg.transport or rawget(_G, 'PerformHttpRequest')
    if type(transport) ~= 'function' then
        return { transportError = 'PerformHttpRequest is not available (not running in FXServer?)' }
    end
    local ok, err = pcall(transport, req.url, function(status, text, respHeaders, errorData)
        settle({ status = status, text = text, headers = respHeaders, errorData = errorData })
    end, req.method, data or '', headers, { followLocation = false })
    if not ok then
        settle({ transportError = tostring(err) })
    end
    -- PerformHttpRequest has no timeout option: FXServer aborts on its own only when no
    -- byte arrives for ~5 s. This timer bounds the whole attempt; a late answer is ignored.
    Citizen.SetTimeout(math.floor(cfg.timeout * 1000), function()
        settle({ timedOut = true })
    end)
    return Citizen.Await(p)
end

local function messageFrom(body, text, status)
    if type(body) == 'table' then
        for _, k in ipairs({ 'error', 'message', 'msg' }) do
            local v = body[k]
            if type(v) == 'string' and v ~= '' then return v end
        end
    end
    if type(text) == 'string' and trim(text) ~= '' then
        local t = trim(text)
        if #t > 500 then t = t:sub(1, 500) .. '…' end
        return t
    end
    return REASONS[status] or ('HTTP ' .. tostring(status))
end

--- Turns one raw attempt into (data, err, resp).
local function interpret(cfg, ep, req, raw)
    if raw.timedOut then
        return nil, newError({ type = 'TimeoutError', endpoint = ep.id, method = req.method, path = req.path,
            message = ('no response from txApi within %s s'):format(cfg.timeout) }), nil
    end
    if raw.transportError then
        return nil, newError({ type = 'ConnectionError', endpoint = ep.id, method = req.method, path = req.path,
            message = raw.transportError }), nil
    end

    local status = tonumber(raw.status) or 0
    local headers = type(raw.headers) == 'table' and raw.headers or {}

    if status == 0 then
        -- FXServer reports transport failures as status 0 + "<curl text> - CURL error code N (…)".
        local detail = tostring(raw.errorData or 'no response')
        local code = tonumber(detail:match('CURL error code (%d+)'))
        local isTimeout = code == 28 or code == 42 or detail:lower():find('timed out', 1, true) ~= nil
        return nil, newError({
            type = isTimeout and 'TimeoutError' or 'ConnectionError', endpoint = ep.id,
            method = req.method, path = req.path,
            message = (isTimeout and 'txApi did not answer in time: ' or 'could not reach txApi: ') .. detail,
        }), nil
    end

    local text = raw.text
    if text == nil and raw.errorData ~= nil then
        -- FXServer: for status >= 400 the body arrives as errorData "HTTP <code>: <body>"
        -- (and the response headers are NOT passed — they are an empty table).
        local s = tostring(raw.errorData)
        text = s:gsub('^HTTP%s+%d+:?%s?', '', 1)
    end
    local body = jsonDecode(text)
    local rateLimit = parseRateLimit(headers)
    local resp = { status = status, headers = headers, rateLimit = rateLimit, body = body, text = text }

    if status >= 200 and status <= 299 then
        local data = body
        if data == nil or isNull(data) then data = text or '' end
        return data, nil, resp
    end

    local errType = TxApi.errorTypeForStatus(status)
    local err = newError({
        type = errType, status = status, endpoint = ep.id, method = req.method, path = req.path,
        message = messageFrom(body, text, status), body = body ~= nil and body or text,
        headers = headers, rateLimit = rateLimit,
    })

    local retryAfter = tonumber(headerValue(headers, 'Retry-After') or '')
    err.retryAfter = retryAfter
    if errType == 'RateLimitError' then
        local headersAvailable = next(headers) ~= nil
        err.headersAvailable = headersAvailable
        if headersAvailable then
            -- SPEC §4: no Retry-After = monthly quota (resets on the 1st, UTC)
            err.quotaExhausted = retryAfter == nil
        else
            -- FXServer drops all headers of error responses, so the body has to tell:
            -- API-key quota → "Monthly request limit reached", Discord-bot quota → code "QUOTA".
            local msg = err.message:lower()
            err.quotaExhausted = msg:find('monthly', 1, true) ~= nil
                or (type(body) == 'table' and body.code == 'QUOTA')
        end
    end
    return nil, err, resp
end

local function retryDelay(cfg, req, err, attempt)
    if attempt >= cfg.maxRetries then return nil end
    if err.type == 'RateLimitError' then
        if err.quotaExhausted then return nil end
        local wait = err.retryAfter
        if wait == nil then wait = cfg.rateLimitRetryDelay end -- FXServer hides Retry-After
        if wait > MAX_RETRY_AFTER then return nil end
        return math.max(0, wait)
    end
    local transient = err.type == 'ConnectionError' or err.type == 'TimeoutError'
        or err.status == 502 or err.status == 503 or err.status == 504
    if not transient or req.method ~= 'GET' then return nil end
    if err.retryAfter and err.retryAfter <= MAX_RETRY_AFTER then return err.retryAfter end
    local cap = math.min(8, 0.5 * 2 ^ attempt)
    return cfg.random() * cap -- full jitter
end

local function redactedHeaders(headers)
    local copy = {}
    for k, v in pairs(headers) do copy[k] = v end
    copy['Authorization'] = 'Bearer ***'
    return copy
end

--- Executes a call with retries. Must run inside a coroutine.
local function execute(client, cfg, ep, pathValues, opts)
    local req, verr = buildRequest(cfg, ep, pathValues, opts)
    if not req then return nil, verr, nil end

    local headers = {
        ['Authorization'] = 'Bearer ' .. cfg.apiKey(),
        ['Accept'] = 'application/json',
        ['User-Agent'] = cfg.userAgent,
    }
    local data = ''
    if req.hasBody then
        headers['Content-Type'] = 'application/json'
        data = jsonEncode(req.body)
    end

    local attempt = 0
    while true do
        if cfg.onRequest then
            pcall(cfg.onRequest, { endpoint = ep.id, method = req.method, url = req.url,
                headers = redactedHeaders(headers), body = data, attempt = attempt + 1 })
        end
        local started = now()
        local raw = perform(cfg, req, headers, data)
        local result, err, resp = interpret(cfg, ep, req, raw)
        if resp then
            resp.attempts = attempt + 1
            resp.durationMs = now() - started
            client.lastResponse = resp
            if resp.rateLimit then client.lastRateLimit = resp.rateLimit end
        end
        if cfg.onResponse then
            pcall(cfg.onResponse, { endpoint = ep.id, method = req.method, url = req.url,
                status = resp and resp.status or 0, headers = resp and resp.headers or {},
                rateLimit = resp and resp.rateLimit, error = err, attempt = attempt + 1,
                durationMs = now() - started })
        end
        if not err then return result, nil, resp end
        if err.rateLimit == nil then err.rateLimit = client.lastRateLimit end
        local delay = retryDelay(cfg, req, err, attempt)
        if delay == nil then
            err.attempts = attempt + 1
            return nil, err, resp
        end
        attempt = attempt + 1
        Citizen.Wait(math.floor(delay * 1000))
    end
end

local function assertInThread(name)
    local _, isMain = coroutine.running()
    if isMain then
        error(("txapi: %s blocks and must run inside Citizen.CreateThread, an event handler or a command — "
            .. "or pass a callback as the last argument"):format(name), 3)
    end
end

-- ---------------------------------------------------------------------------
-- pagination (SPEC §7)
-- ---------------------------------------------------------------------------

local PLAYER_SORT_KEYS = {
    playTime = 'playTime', tsJoined = 'tsJoined', tsLastConnection = 'tsLastConnection',
    sessions = 'tsJoined', lastConnection = 'tsLastConnection',
}

local function copy(t)
    local c = {}
    if type(t) == 'table' then for k, v in pairs(t) do c[k] = v end end
    return c
end

local function listOf(page, field)
    if type(page) ~= 'table' then return nil end
    local rows = page[field]
    if type(rows) ~= 'table' then return nil end
    return rows
end

--- Pager over one endpoint. `nextOpts(page, rows, opts)` returns the opts of the next
--- page or nil to stop. The returned object is callable (use it in a generic for);
--- after the loop, `.err` holds the error that ended it (nil = all pages read).
local function makePager(call, field, opts, nextOpts)
    local pager = { err = nil, pages = 0 }
    local current, done, seen = copy(opts), false, {}
    setmetatable(pager, {
        __call = function()
            if done then return nil end
            local page, err = call(current)
            if err then pager.err = err; done = true; return nil end
            local rows = listOf(page, field)
            if rows == nil or #rows == 0 then done = true; return nil end
            pager.pages = pager.pages + 1
            local nxt = nextOpts(page, rows, current)
            if nxt == nil then
                done = true
            else
                local key = tostring(nxt.offset) .. '\0' .. tostring(nxt.cursor)
                if seen[key] then done = true else seen[key] = true; current = nxt end
            end
            return page
        end,
    })
    return pager
end

local function makeRowIterator(pager, field)
    local rows, i = nil, 0
    local it = {}
    setmetatable(it, {
        __index = function(_, k)
            if k == 'err' then return pager.err end
            if k == 'pages' then return pager.pages end
        end,
        __call = function()
            while true do
                if rows and i < #rows then
                    i = i + 1
                    return rows[i]
                end
                local page = pager()
                if page == nil then return nil end
                rows, i = page[field], 0
            end
        end,
    })
    return it
end

local function playerNext(sortKey)
    return function(page, rows, opts)
        if page.hasReachedEnd == true then return nil end
        local last = rows[#rows]
        local key = sortKey or PLAYER_SORT_KEYS[opts.sortBy or 'playTime'] or 'playTime'
        local offset, cursor = last[key], last.license
        if isNull(offset) or isNull(cursor) then return nil end
        local n = copy(opts)
        n.offset, n.cursor = offset, cursor
        return n
    end
end

local function historyNext(page, rows, opts)
    if page.hasReachedEnd == true then return nil end
    local last = rows[#rows]
    if isNull(last.timestamp) or isNull(last.id) then return nil end
    local n = copy(opts)
    n.offset, n.cursor = last.timestamp, last.id
    return n
end

local function eventsNext(page, _, opts)
    local c = page.nextCursor
    if isNull(c) or c == '' then return nil end
    local n = copy(opts)
    n.cursor = c
    return n
end

local PAGERS = {
    -- endpoint id = { list field, next-page function, pager name, row iterator name }
    ['players.search'] = { 'players', playerNext(nil), 'searchPages', 'searchAll' },
    ['players.online'] = { 'players', playerNext('playTime'), 'onlinePages', 'onlineAll' },
    ['players.lookup'] = { 'players', playerNext('playTime'), 'lookupPages', 'lookupAll' },
    ['history.bans'] = { 'history', historyNext, 'bansPages', 'bansAll' },
    ['history.warns'] = { 'history', historyNext, 'warnsPages', 'warnsAll' },
    ['history.search'] = { 'history', historyNext, 'searchPages', 'searchAll' },
    ['events.list'] = { 'events', eventsNext, 'listPages', 'listAll' },
    ['events.deaths'] = { 'events', eventsNext, 'deathsPages', 'deathsAll' },
    ['events.kills'] = { 'events', eventsNext, 'killsPages', 'killsAll' },
    ['events.chat'] = { 'events', eventsNext, 'chatPages', 'chatAll' },
    ['events.commands'] = { 'events', eventsNext, 'commandsPages', 'commandsAll' },
    ['events.admin-actions'] = { 'events', eventsNext, 'adminActionsPages', 'adminActionsAll' },
    ['events.actions'] = { 'events', eventsNext, 'actionsPages', 'actionsAll' },
    ['events.sessions'] = { 'events', eventsNext, 'sessionsPages', 'sessionsAll' },
}
TxApi._pagers = PAGERS

-- ---------------------------------------------------------------------------
-- the client
-- ---------------------------------------------------------------------------

local ClientMeta = {}
ClientMeta.__index = ClientMeta
ClientMeta.__tostring = function(c)
    return ('TxApi(baseUrl=%s, serverId=%s, sdk=%s)'):format(c.baseUrl, tostring(c.serverId), TxApi.VERSION)
end

local function convar(name, default)
    local f = rawget(_G, 'GetConvar')
    if type(f) ~= 'function' then return default end
    local v = f(name, default or '')
    if v == nil or v == '' then return default end
    return v
end

--[[
    TxApi.new(config) → client          (raises on misconfiguration)

    config.apiKey      default: convar txapi_key            (set it with `set`, NEVER `setr`)
    config.baseUrl     default: convar txapi_base_url, else https://txapi.cloud
    config.serverId    default: convar txapi_server_id      (default server for server-scoped calls)
    config.timeout     seconds per attempt, default 30
    config.maxRetries  default 2 (0 disables retries)
    config.rateLimitRetryDelay  seconds to wait before retrying a per-minute 429 when the
                       Retry-After header is not visible (FXServer hides it), default 15
    config.userAgent   appended to "txapi-fivem/1.0.0"
    config.onRequest / config.onResponse  hooks for logging/metrics (errors in them are ignored;
                       the Authorization header is redacted)
    config.transport   replacement for PerformHttpRequest (same signature) — for tests/proxies
]]
function TxApi.new(config)
    config = config or {}
    if type(config) ~= 'table' then error('txapi: TxApi.new expects a table', 2) end

    local apiKey = config.apiKey
    if isNull(apiKey) then apiKey = convar('txapi_key', nil) end
    if type(apiKey) ~= 'string' or trim(apiKey) == '' then
        error('txapi: no API key. Put `set txapi_key "txp_…"` in server.cfg (set — NOT setr, which would send '
            .. 'the key to every player) or pass apiKey to TxApi.new', 2)
    end
    apiKey = trim(apiKey)

    local baseUrl = config.baseUrl or convar('txapi_base_url', nil) or TxApi.DEFAULT_BASE_URL
    baseUrl = tostring(baseUrl):gsub('/+$', '')
    if not baseUrl:match('^https?://[^/]') then
        error('txapi: baseUrl must start with http:// or https:// (got ' .. baseUrl .. ')', 2)
    end

    local serverId = config.serverId
    if isNull(serverId) then serverId = convar('txapi_server_id', nil) end
    if type(serverId) == 'string' and tonumber(serverId) then serverId = math.tointeger(tonumber(serverId)) or serverId end

    local timeout = config.timeout or 30
    if type(timeout) ~= 'number' or timeout <= 0 then error('txapi: timeout must be a positive number of seconds', 2) end
    local maxRetries = config.maxRetries
    if maxRetries == nil then maxRetries = 2 end
    if math.type(maxRetries) ~= 'integer' or maxRetries < 0 then
        error('txapi: maxRetries must be an integer >= 0', 2)
    end
    local rateLimitRetryDelay = config.rateLimitRetryDelay or 15
    if type(rateLimitRetryDelay) ~= 'number' or rateLimitRetryDelay < 0 then
        error('txapi: rateLimitRetryDelay must be a number of seconds >= 0', 2)
    end

    local userAgent = 'txapi-fivem/' .. TxApi.VERSION
    if config.userAgent and config.userAgent ~= '' then userAgent = userAgent .. ' ' .. tostring(config.userAgent) end

    -- The key lives only in this closure — not as a field — so printing or json-encoding
    -- the client can never leak it.
    local cfg = {
        apiKey = function() return apiKey end,
        baseUrl = baseUrl, serverId = serverId, timeout = timeout, maxRetries = maxRetries,
        rateLimitRetryDelay = rateLimitRetryDelay, userAgent = userAgent,
        onRequest = config.onRequest, onResponse = config.onResponse,
        transport = config.transport, random = config.random or math.random,
    }

    local client = setmetatable({
        baseUrl = baseUrl, serverId = serverId, lastRateLimit = nil, lastResponse = nil,
        VERSION = TxApi.VERSION, API_VERSION = TxApi.API_VERSION,
    }, ClientMeta)

    --- Runs an endpoint: blocking, or in a thread with a callback.
    local function run(ep, pathValues, opts, cb)
        if cb then
            Citizen.CreateThread(function()
                local data, err, resp = execute(client, cfg, ep, pathValues, opts)
                cb(data, err, resp)
            end)
            return
        end
        assertInThread('api.' .. ep.lua .. '()')
        return execute(client, cfg, ep, pathValues, opts)
    end

    local function method(ep, group)
        local npath = #ep.pathParams
        return function(...)
            local args = table.pack(...)
            local first = 1
            if args.n >= 1 and args[1] == group then first = 2 end -- tolerate api.players:ban(...)
            local cb
            if args.n >= first and type(args[args.n]) == 'function' then
                cb = args[args.n]
                args[args.n] = nil
                args.n = args.n - 1
            end
            local pathValues, opts = {}, nil
            if npath > 0 and type(args[first]) == 'table' then
                opts = args[first] -- everything (incl. path params) in one table
            else
                for i = 1, npath do pathValues[i] = args[first + i - 1] end
                opts = args[first + npath]
            end
            return run(ep, pathValues, opts, cb)
        end
    end

    local function groupFor(luaName)
        local node, parts = client, {}
        for part in luaName:gmatch('[^%.]+') do parts[#parts + 1] = part end
        for i = 1, #parts - 1 do
            local p = parts[i]
            if node[p] == nil then node[p] = {} end
            node = node[p]
        end
        return node, parts[#parts]
    end

    for _, ep in ipairs(ENDPOINTS) do
        local group, name = groupFor(ep.lua)
        group[name] = method(ep, group)
        local pg = PAGERS[ep.id]
        if pg then
            local field, nextFn, pagesName, allName = pg[1], pg[2], pg[3], pg[4]
            local function caller(o)
                return execute(client, cfg, ep, {}, o)
            end
            group[pagesName] = function(opts)
                assertInThread('api.' .. ep.lua:gsub('[^%.]+$', pagesName) .. '()')
                return makePager(caller, field, opts, nextFn)
            end
            group[allName] = function(opts)
                assertInThread('api.' .. ep.lua:gsub('[^%.]+$', allName) .. '()')
                return makeRowIterator(makePager(caller, field, opts, nextFn), field)
            end
        end
    end

    --- Generic dispatcher: api.call('players.ban', { … }[, cb]).
    -- `name` is the spec id ('server.resource.restart') or the Lua name
    -- ('server.resources.restart'); path parameters go into `params` by name.
    function client.call(name, params, cb)
        if name == client then name, params, cb = params, cb, nil end -- tolerate api:call(...)
        local ep = TxApi.endpoint(name)
        if not ep then
            local err = validationError(nil, nil, ("unknown method '%s'"):format(tostring(name)))
            if cb then cb(nil, err, nil); return end
            return nil, err, nil
        end
        return run(ep, {}, params or {}, cb)
    end

    return client
end

--- Names of every method, e.g. { 'account.me', 'players.ban', … } (Lua names).
function TxApi.methods()
    local out = {}
    for _, ep in ipairs(ENDPOINTS) do out[#out + 1] = ep.lua end
    return out
end
