--[[
    Offline stand-in for the FXServer Lua runtime, just enough for the SDK:

      promise.new / :resolve / :reject      (citizen deferred.lua semantics)
      Citizen.CreateThread / Wait / Await / SetTimeout
      PerformHttpRequest(url, cb, method, data, headers, options)
      GetConvar, GetGameTimer, exports, AddEventHandler, RegisterCommand,
      IsPlayerAceAllowed, GetCurrentResourceName

    Time is virtual: H.run() advances a clock to the next timer instead of sleeping.

    PerformHttpRequest reproduces what FXServer really does (checked against
    citizenfx/fivem: citizen-server-impl/src/HttpScriptFunctions.cpp and
    http-client/src/HttpClient.cpp):
      * 2xx/3xx:     cb(status, body, headers, nil)       headers: name → value, or array if repeated
      * >= 400:      cb(status, nil, {}, "HTTP <status>: <body>")   — NO headers on errors
      * transport:   cb(0, nil, {}, "<text> - CURL error code N (<curl text>)")
    A scripted response with `mode = 'raw'` instead passes body + headers on errors too
    (what a runtime that kept error headers would do) so the header code paths are tested.
]]
local json = dofile(TESTS_DIR .. 'harness/json.lua')

local H = {}

function H.reset()
    H.now = 0
    H.timers = {}       -- { at, seq, fn }
    H.ready = {}        -- coroutines to resume
    H.requests = {}
    H.waits = {}
    H.responses = {}    -- FIFO of scripted responses
    H.responder = nil   -- function(req) → response (wins over the FIFO)
    H.convars = {}
    H.exports = {}
    H.events = {}
    H.commands = {}
    H.aces = {}
    H.prints = {}
    H.errors = {}
    H.seq = 0
    json.emptyTableAs = 'object'
    json.nullAsNil = false
end

local function schedule(at, fn)
    H.seq = H.seq + 1
    H.timers[#H.timers + 1] = { at = at, seq = H.seq, fn = fn }
end

local function resume(co, ...)
    local ok, req = coroutine.resume(co, ...)
    if not ok then
        H.errors[#H.errors + 1] = debug.traceback(co, tostring(req))
        return
    end
    if coroutine.status(co) == 'dead' then return end
    if type(req) == 'table' and req.wake then
        schedule(req.wake, function() resume(co) end)
    elseif type(req) == 'table' and req.promise then
        local p = req.promise
        if p.state ~= 0 then
            H.ready[#H.ready + 1] = co
        else
            p.waiters[#p.waiters + 1] = co
        end
    else
        H.errors[#H.errors + 1] = 'coroutine yielded something unexpected: ' .. tostring(req)
    end
end

-- -------------------------------------------------------------------- promise
local Promise = {}
Promise.__index = Promise

local function settle(p, state, v)
    if p.state ~= 0 then return end
    p.state, p.value = state, v
    for _, co in ipairs(p.waiters) do H.ready[#H.ready + 1] = co end
    p.waiters = {}
end
function Promise:resolve(v) settle(self, 1, v) end
function Promise:reject(v) settle(self, 2, v) end

promise = { new = function() return setmetatable({ state = 0, waiters = {} }, Promise) end }

-- -------------------------------------------------------------------- Citizen
Citizen = {}

function Citizen.CreateThread(fn)
    local co = coroutine.create(fn)
    H.ready[#H.ready + 1] = co
end
CreateThread = Citizen.CreateThread

function Citizen.Wait(ms)
    local _, main = coroutine.running()
    assert(not main, 'Citizen.Wait outside a thread')
    H.waits[#H.waits + 1] = ms
    coroutine.yield({ wake = H.now + ms })
end
Wait = Citizen.Wait

function Citizen.Await(p)
    local _, main = coroutine.running()
    assert(not main, 'Current execution context is not in the scheduler')
    if p.state == 0 then coroutine.yield({ promise = p }) end
    if p.state == 2 then error(p.value, 2) end
    return p.value
end

function Citizen.SetTimeout(ms, fn)
    schedule(H.now + ms, function() Citizen.CreateThread(fn) end)
end
SetTimeout = Citizen.SetTimeout

--- Runs every thread and timer until nothing is left (or `limit` steps).
function H.run(limit)
    limit = limit or 100000
    local steps = 0
    while true do
        steps = steps + 1
        if steps > limit then error('scheduler did not settle') end
        if #H.ready > 0 then
            local batch = H.ready
            H.ready = {}
            for _, co in ipairs(batch) do resume(co) end
        elseif #H.timers > 0 then
            table.sort(H.timers, function(a, b)
                if a.at ~= b.at then return a.at < b.at end
                return a.seq < b.seq
            end)
            local t = table.remove(H.timers, 1)
            if t.at > H.now then H.now = t.at end
            t.fn()
        else
            break
        end
    end
    if #H.errors > 0 then
        local e = table.concat(H.errors, '\n')
        H.errors = {}
        error(e, 0)
    end
end

--- Runs fn in a thread, drives the scheduler and returns fn's results.
function H.call(fn)
    local results
    Citizen.CreateThread(function() results = table.pack(fn()) end)
    H.run()
    assert(results, 'thread did not finish')
    return table.unpack(results, 1, results.n)
end

-- -------------------------------------------------------------------- HTTP
function H.respond(...)
    for _, r in ipairs({ ... }) do H.responses[#H.responses + 1] = r end
end

local function splitUrl(url)
    local path, query = url:match('^https?://[^/]+([^?]*)%??(.*)$')
    local q = {}
    for pair in (query or ''):gmatch('[^&]+') do
        local k, v = pair:match('^([^=]*)=?(.*)$')
        q[k] = v
    end
    return path, q, query
end

function PerformHttpRequest(url, cb, method, data, headers, options)
    local path, q, rawQuery = splitUrl(url)
    local req = {
        url = url, path = path, query = q, rawQuery = rawQuery, method = method,
        data = data, headers = headers, options = options,
    }
    if data ~= nil and data ~= '' then
        local ok, decoded = pcall(json.decode, data)
        req.json = ok and decoded or nil
    end
    H.requests[#H.requests + 1] = req

    local r
    if H.responder then r = H.responder(req) end
    if r == nil then r = table.remove(H.responses, 1) end
    if r == nil then r = { status = 200, body = { success = true } } end
    if r.never then return end

    local body = r.body
    if type(body) == 'table' then body = json.encode(body) end
    body = body or ''
    local status = r.status or 200

    schedule(H.now + (r.delay or 0), function()
        if r.transportError then
            cb(0, nil, {}, ' - CURL error code ' .. r.transportError)
        elseif status >= 400 and r.mode ~= 'raw' then
            cb(status, nil, {}, r.errorData or ('HTTP ' .. status .. ': ' .. body))
        else
            cb(status, body, r.headers or {}, nil)
        end
    end)
end

-- -------------------------------------------------------------------- misc natives
function GetConvar(name, default)
    local v = H.convars[name]
    if v == nil then return default end
    return v
end

function GetGameTimer() return H.now end

function GetCurrentResourceName() return 'txapi' end

exports = setmetatable({}, {
    __call = function(_, name, fn) H.exports[name] = fn end,
})

function AddEventHandler(name, fn)
    H.events[name] = H.events[name] or {}
    table.insert(H.events[name], fn)
end

function RegisterCommand(name, fn, restricted)
    H.commands[name] = { fn = fn, restricted = restricted }
end

function IsPlayerAceAllowed(src, ace)
    return H.aces[tostring(src) .. '|' .. ace] == true
end

function GetPlayerName(src) return 'Player' .. tostring(src) end

H.print = print
function print(...)
    local parts = {}
    for i = 1, select('#', ...) do parts[#parts + 1] = tostring((select(i, ...))) end
    H.prints[#H.prints + 1] = table.concat(parts, ' ')
end

_G.json = json
H.json = json
H.reset()
return H
