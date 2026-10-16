-- Every endpoint called with EVERY field spec/endpoints.json lists (built from the raw spec,
-- not from lib/endpoints.lua): each query parameter and body field must arrive under exactly
-- its wire name — nothing missing, nothing extra.

local VALUES = { string = 'license:abc', integer = 5, boolean = false }

local function value(field)
    if field.type == 'string[]' then return { 'players.read', 'players.ban' } end
    local v = VALUES[field.type]
    assert(v ~= nil, 'no test value for type ' .. tostring(field.type))
    return v
end

local function keys(t)
    local out = {}
    for k in pairs(t or {}) do if k ~= 'serverId' then out[#out + 1] = k end end
    table.sort(out)
    return out
end

test('full parameters: every query parameter and body field is sent under its name', function()
    local spec = readSpec()
    local api = newApi({ serverId = 3 })
    H.responder = function() return { status = 200, body = { success = true } } end
    local results = {}
    H.call(function()
        for _, e in ipairs(spec.endpoints) do
            local opts, pathValues = {}, {}
            for _, p in ipairs(e.params) do
                if p['in'] == 'path' then pathValues[#pathValues + 1] = 'X1' else opts[p.name] = value(p) end
            end
            for _, b in ipairs(e.body) do opts[b.name] = value(b) end
            local node = api
            local luaName
            for _, ep in ipairs(TxApi._spec.endpoints) do if ep.id == e.id then luaName = ep.lua end end
            ok(luaName, 'no Lua method for ' .. e.id)
            for part in luaName:gmatch('[^%.]+') do node = node[part] end
            local args = {}
            for _, v in ipairs(pathValues) do args[#args + 1] = v end
            args[#args + 1] = opts
            local before = #H.requests
            local res, err = node(table.unpack(args))
            ok(res, e.id .. ' failed: ' .. tostring(err and err.message or err))
            eq(#H.requests, before + 1, e.id .. ' request count')
            results[#results + 1] = { e = e, req = H.requests[#H.requests] }
        end
    end)
    eq(#results, 67, 'endpoints exercised')
    for _, r in ipairs(results) do
        local e, req = r.e, r.req
        eq(req.method, e.method, e.id .. ' method')
        local wantQuery = {}
        for _, p in ipairs(e.params) do if p['in'] == 'query' then wantQuery[#wantQuery + 1] = p.name end end
        table.sort(wantQuery)
        deq(keys(req.query), wantQuery, e.id .. ' query')
        local wantBody = {}
        for _, b in ipairs(e.body) do wantBody[#wantBody + 1] = b.name end
        table.sort(wantBody)
        deq(keys(req.json), wantBody, e.id .. ' body')
    end
end)
