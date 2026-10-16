--[[
    Minimal JSON for the offline tests, modelled on FiveM's built-in `json`
    (citizenfx/lua-rapidjson, branch "grit"):

      * encode: a table is an ARRAY if its metatable has __jsontype = 'array', or it has no
        __jsontype and #t > 0; otherwise an OBJECT (only string keys are written).
        An EMPTY table without __jsontype is ambiguous between FiveM builds
        (rapidjson: {}, the older dkjson-based json.lua: []). `json.emptyTableAs` selects
        which behaviour to simulate so tests can prove the SDK never relies on either.
      * decode: objects/arrays get { __jsontype = 'object'|'array' } metatables;
        JSON null becomes json.null (rapidjson) or nil when `json.nullAsNil` is set (dkjson).
      * integers decode to Lua integers.
]]
local json = { emptyTableAs = 'object', nullAsNil = false }
json.null = setmetatable({}, { __tostring = function() return 'json.null' end, __jsonnull = true })

local escapes = { ['"'] = '\\"', ['\\'] = '\\\\', ['\b'] = '\\b', ['\f'] = '\\f', ['\n'] = '\\n', ['\r'] = '\\r', ['\t'] = '\\t' }

local function encodeString(s)
    return '"' .. s:gsub('[%c"\\]', function(c)
        return escapes[c] or string.format('\\u%04x', c:byte())
    end) .. '"'
end

local encode

local function isArray(t)
    local mt = getmetatable(t)
    if mt and mt.__jsontype == 'array' then return true end
    if mt and mt.__jsontype == 'object' then return false end
    if next(t) == nil then return json.emptyTableAs == 'array' end
    return #t > 0
end

encode = function(v, seen)
    local t = type(v)
    if v == nil or v == json.null then return 'null' end
    if t == 'boolean' then return v and 'true' or 'false' end
    if t == 'number' then
        if math.type(v) == 'integer' then return string.format('%d', v) end
        if v ~= v or v == math.huge or v == -math.huge then error('cannot encode ' .. tostring(v)) end
        return string.format('%.17g', v)
    end
    if t == 'string' then return encodeString(v) end
    if t == 'table' then
        seen = seen or {}
        if seen[v] then error('circular reference') end
        seen[v] = true
        local out = {}
        if isArray(v) then
            for i = 1, #v do out[i] = encode(v[i], seen) end
            seen[v] = nil
            return '[' .. table.concat(out, ',') .. ']'
        end
        local keys = {}
        for k in pairs(v) do if type(k) == 'string' then keys[#keys + 1] = k end end
        table.sort(keys)
        for _, k in ipairs(keys) do out[#out + 1] = encodeString(k) .. ':' .. encode(v[k], seen) end
        seen[v] = nil
        return '{' .. table.concat(out, ',') .. '}'
    end
    error('cannot encode a ' .. t)
end

function json.encode(v) return encode(v) end

-- ---------------------------------------------------------------- decode
local function decodeError(str, pos, msg)
    error(('json.decode: %s at position %d'):format(msg, pos), 0)
end

local function skip(str, pos)
    return str:find('[^ \t\r\n]', pos) or #str + 1
end

local parse

local function parseString(str, pos)
    local out, i = {}, pos + 1
    while true do
        local c = str:sub(i, i)
        if c == '' then decodeError(str, i, 'unterminated string') end
        if c == '"' then return table.concat(out), i + 1 end
        if c == '\\' then
            local n = str:sub(i + 1, i + 1)
            local map = { ['"'] = '"', ['\\'] = '\\', ['/'] = '/', b = '\b', f = '\f', n = '\n', r = '\r', t = '\t' }
            if map[n] then out[#out + 1] = map[n]; i = i + 2
            elseif n == 'u' then
                local cp = tonumber(str:sub(i + 2, i + 5), 16)
                if not cp then decodeError(str, i, 'bad \\u escape') end
                i = i + 6
                if cp >= 0xD800 and cp <= 0xDBFF and str:sub(i, i + 1) == '\\u' then
                    local lo = tonumber(str:sub(i + 2, i + 5), 16)
                    cp = 0x10000 + (cp - 0xD800) * 0x400 + (lo - 0xDC00)
                    i = i + 6
                end
                out[#out + 1] = utf8.char(cp)
            else decodeError(str, i, 'bad escape') end
        else
            out[#out + 1] = c
            i = i + 1
        end
    end
end

parse = function(str, pos)
    pos = skip(str, pos)
    local c = str:sub(pos, pos)
    if c == '{' then
        local obj = setmetatable({}, { __jsontype = 'object' })
        pos = skip(str, pos + 1)
        if str:sub(pos, pos) == '}' then return obj, pos + 1 end
        while true do
            if str:sub(pos, pos) ~= '"' then decodeError(str, pos, 'expected key') end
            local k; k, pos = parseString(str, pos)
            pos = skip(str, pos)
            if str:sub(pos, pos) ~= ':' then decodeError(str, pos, "expected ':'") end
            local v; v, pos = parse(str, pos + 1)
            obj[k] = v
            pos = skip(str, pos)
            local d = str:sub(pos, pos)
            if d == '}' then return obj, pos + 1 end
            if d ~= ',' then decodeError(str, pos, "expected ',' or '}'") end
            pos = skip(str, pos + 1)
        end
    elseif c == '[' then
        local arr = setmetatable({}, { __jsontype = 'array' })
        pos = skip(str, pos + 1)
        if str:sub(pos, pos) == ']' then return arr, pos + 1 end
        local n = 0
        while true do
            local v; v, pos = parse(str, pos)
            n = n + 1
            arr[n] = v
            pos = skip(str, pos)
            local d = str:sub(pos, pos)
            if d == ']' then return arr, pos + 1 end
            if d ~= ',' then decodeError(str, pos, "expected ',' or ']'") end
            pos = pos + 1
        end
    elseif c == '"' then
        return parseString(str, pos)
    elseif str:sub(pos, pos + 3) == 'true' then return true, pos + 4
    elseif str:sub(pos, pos + 4) == 'false' then return false, pos + 5
    elseif str:sub(pos, pos + 3) == 'null' then
        if json.nullAsNil then return nil, pos + 4 end
        return json.null, pos + 4
    else
        local num = str:match('^-?%d+%.?%d*[eE]?[-+]?%d*', pos)
        if not num or num == '' then decodeError(str, pos, 'unexpected character') end
        local v = tonumber(num)
        if not num:find('[.eE]') then v = math.tointeger(v) or v end
        return v, pos + #num
    end
end

function json.decode(str)
    if type(str) ~= 'string' then error('json.decode expects a string', 2) end
    local v, pos = parse(str, 1)
    pos = skip(str, pos)
    if pos <= #str then decodeError(str, pos, 'trailing garbage') end
    return v
end

return json
