-- SPEC §1/§2: configuration and request building.

local function one() eq(#H.requests, 1, 'requests'); return H.requests[1] end

test('path parameters are percent-encoded', function()
    local api = newApi()
    H.call(function()
        api.admins.get('name with space')
        api.whitelist.approvals.remove('license:abc')
        api.history.action('B1/2?x')
    end)
    eq(H.requests[1].path, '/api/v1/txadmin/admins/name%20with%20space')
    eq(H.requests[2].path, '/api/v1/whitelist/approvals/license%3Aabc')
    eq(H.requests[3].path, '/api/v1/history/action/B1%2F2%3Fx')
end)

test('query values are percent-encoded, nil omitted, booleans true/false', function()
    local api = newApi()
    H.call(function()
        api.players.lookup({ identifier = 'license:abc' })
        api.events.deaths({ pvp = false, player = 'John Doe', cause = nil })
        api.events.actions({ revoked = true, limit = 5.0 })
    end)
    eq(H.requests[1].rawQuery, 'identifier=license%3Aabc')
    eq(H.requests[2].rawQuery, 'player=John%20Doe&pvp=false')
    eq(H.requests[3].rawQuery, 'revoked=true&limit=5')
end)

test('serverId: client default, per-call override, never on account-level endpoints', function()
    local api = newApi({ serverId = 7 })
    H.call(function()
        api.server.status()
        api.server.status({ serverId = 9 })
        api.account.me()
        api.account.servers({ serverId = 9 })
        api.history.revoke('W1', { serverId = 2 })
    end)
    eq(H.requests[1].rawQuery, 'serverId=7')
    eq(H.requests[2].rawQuery, 'serverId=9')
    eq(H.requests[3].rawQuery, '')
    eq(H.requests[4].rawQuery, '')
    eq(H.requests[5].rawQuery, 'serverId=2')
    eq(H.requests[5].json.serverId, nil, 'serverId is never a body field')
end)

test('no serverId configured → none sent; convar txapi_server_id is the default', function()
    local api = newApi()
    H.call(function() api.server.status() end)
    eq(H.requests[1].rawQuery, '')
    H.convars.txapi_server_id = '12'
    local api2 = newApi()
    H.call(function() api2.server.status() end)
    eq(H.requests[2].rawQuery, 'serverId=12')
end)

test('JSON body: absent fields omitted, never null, booleans explicit', function()
    local api = newApi()
    H.call(function()
        api.players.whitelist({ identifier = 'license:abc', whitelisted = false })
        api.players.kick({ identifier = 'license:abc', reason = H.json.null })
        api.players.ban({ identifier = 'license:abc', reason = 'Cheating', durationHours = 24.0, discordBan = true })
    end)
    eq(H.requests[1].data, '{"identifier":"license:abc","whitelisted":false}')
    eq(H.requests[2].data, '{"identifier":"license:abc"}')
    eq(H.requests[3].data, '{"discordBan":true,"durationHours":24,"identifier":"license:abc","reason":"Cheating"}')
    eq(H.requests[3].headers['Content-Type'], 'application/json')
end)

for _, mode in ipairs({ 'object', 'array' }) do
    test('empty body is {} and empty list is [] whatever json does with {} (' .. mode .. ' mode)', function()
        H.json.emptyTableAs = mode
        local api = newApi()
        H.call(function()
            api.server.kickAll()
            api.whitelist.denyAll({})
            api.history.revoke('W1')
            api.admins.edit('Jane', { permissions = {} })
            api.admins.add({ name = 'Jane', permissions = { 'players.ban', 'console.write' } })
        end)
        eq(H.requests[1].data, '{}')
        eq(H.requests[2].data, '{}')
        eq(H.requests[3].data, '{}')
        eq(H.requests[4].data, '{"permissions":[]}')
        eq(H.requests[5].data, '{"name":"Jane","permissions":["players.ban","console.write"]}')
    end)
end

test('endpoints without a body send no body and no Content-Type', function()
    local api = newApi()
    H.call(function() api.server.restart(); api.whitelist.approve('R1'); api.admins.delete('Jane') end)
    for i = 1, 3 do
        eq(H.requests[i].data, '', 'data #' .. i)
        eq(H.requests[i].headers['Content-Type'], nil, 'content-type #' .. i)
    end
    eq(H.requests[1].method, 'POST'); eq(H.requests[3].method, 'DELETE')
end)

test('headers: Bearer key, Accept, User-Agent; redirects are not followed', function()
    local api = newApi({ userAgent = 'myres/2.1' })
    H.call(function() api.account.me() end)
    local r = one()
    eq(r.headers['Authorization'], 'Bearer ' .. KEY)
    eq(r.headers['Accept'], 'application/json')
    eq(r.headers['User-Agent'], 'txapi-fivem/1.0.0 myres/2.1')
    eq(r.options.followLocation, false)
    eq(r.url, 'https://api.test/api/v1/me')
end)

test('baseUrl: trailing slash stripped, convar and default', function()
    local a = newApi({ baseUrl = 'https://x.example///' })
    eq(a.baseUrl, 'https://x.example')
    H.convars.txapi_base_url = 'https://from-convar.example/'
    eq(TxApi.new({ apiKey = KEY }).baseUrl, 'https://from-convar.example')
    H.convars.txapi_base_url = nil
    eq(TxApi.new({ apiKey = KEY }).baseUrl, 'https://txapi.cloud')
    raises(function() TxApi.new({ apiKey = KEY, baseUrl = 'ftp://x' }) end, 'http')
end)

test('api key: required at construction, from convar, never in tostring or fields', function()
    local e = raises(function() TxApi.new({}) end, 'set txapi_key')
    contains(tostring(e), 'NOT setr')
    H.convars.txapi_key = '  ' .. KEY .. '  '
    local api = TxApi.new({ baseUrl = 'https://api.test' })
    H.call(function() api.account.me() end)
    eq(one().headers['Authorization'], 'Bearer ' .. KEY)
    ok(not tostring(api):find('SECRET'), 'tostring leaks key')
    -- walk every string reachable from the client table
    local seen = {}
    local function walk(t)
        if seen[t] then return end
        seen[t] = true
        for k, v in pairs(t) do
            if type(v) == 'string' then ok(not v:find('SECRET'), 'field ' .. tostring(k) .. ' leaks key') end
            if type(v) == 'table' then walk(v) end
        end
    end
    walk(api)
    raises(function() TxApi.new({ apiKey = KEY, maxRetries = -1 }) end, 'maxRetries')
    raises(function() TxApi.new({ apiKey = KEY, timeout = 0 }) end, 'timeout')
end)

test('validation errors are returned without sending a request', function()
    local api = newApi()
    local results = {}
    H.call(function()
        local function try(f) local r, e = f(); results[#results + 1] = { r, e } end
        try(function() return api.players.ban({ reason = 'x' }) end)                       -- missing identifier
        try(function() return api.players.ban({ identifier = '   ', reason = 'x' }) end)   -- blank
        try(function() return api.players.ban({ identifier = 'a', reason = string.rep('x', 513) }) end)
        try(function() return api.players.lookup({}) end)                                  -- required query
        try(function() return api.history.revoke(nil, { reason = 'x' }) end)              -- path param
        try(function() return api.players.ban({ identifier = 'a', reason = 'x', durationhours = 24 }) end) -- typo
        try(function() return api.admins.add({ name = 'J', permissions = 'players.ban' }) end)
        try(function() return api.players.ban({ identifier = 'a', reason = 'x', durationHours = '24' }) end)
        try(function() return api.players.whitelist({ identifier = 'a' }) end)              -- whitelisted required
        try(function() return api.console.command({ command = string.rep('c', 1025) }) end)
    end)
    eq(#H.requests, 0, 'no request may be sent')
    local fields = { 'identifier', 'identifier', 'reason', 'identifier', 'actionId', 'durationhours', 'permissions', 'durationHours', 'whitelisted', 'command' }
    for i, r in ipairs(results) do
        eq(r[1], nil, '#' .. i)
        eq(r[2].type, 'ValidationError', '#' .. i)
        eq(r[2].field, fields[i], '#' .. i)
        ok(TxApi.isError(r[2]))
    end
end)

test('maxLength counts characters, not bytes', function()
    local api = newApi()
    H.call(function()
        local r, e = api.players.ban({ identifier = 'a', reason = string.rep('ü', 512) })
        ok(r, tostring(e))
    end)
    eq(#H.requests, 1)
end)

test('path params may be passed in the table; colon calls are tolerated; api.call dispatcher', function()
    local api = newApi()
    H.call(function()
        api.history.revoke({ actionId = 'W1', reason = 'appeal' })
        api.players:ban({ identifier = 'a', reason = 'x' })
        api.call('server.resource.restart', { name = 'chat' })
        api.call('server.resources.restart', { name = 'chat' })
        api.call('admins.edit', { name = 'John Doe', discordId = '1' })
        local r, e = api.call('nope.nope', {})
        eq(e.type, 'ValidationError')
    end)
    eq(H.requests[1].path, '/api/v1/history/revoke/W1'); eq(H.requests[1].data, '{"reason":"appeal"}')
    eq(H.requests[2].path, '/api/v1/players/ban')
    eq(H.requests[3].path, '/api/v1/server/resource/restart')
    eq(H.requests[4].path, '/api/v1/server/resource/restart')
    eq(H.requests[5].path, '/api/v1/txadmin/admins/John%20Doe'); eq(H.requests[5].data, '{"discordId":"1"}')
    eq(#H.requests, 5)
end)

test('blocking call outside a thread raises a helpful error', function()
    local api = newApi()
    raises(function() api.players.stats() end, 'Citizen.CreateThread')
    raises(function() api.players.searchPages({}) end, 'Citizen.CreateThread')
end)

test('2xx response is returned as parsed JSON plus the raw response', function()
    local api = newApi()
    H.respond({ status = 200, body = '{"success":true,"players":[{"license":"x"}],"hasReachedEnd":true,"extra":{"new":1}}',
        headers = { ['X-RateLimit-Limit'] = '60' } })
    local res, err, resp = H.call(function() return api.players.online() end)
    eq(err, nil)
    eq(res.players[1].license, 'x')
    eq(res.extra.new, 1)
    eq(resp.status, 200); eq(resp.attempts, 1); eq(resp.rateLimit.limit, 60)
end)

test('hooks get redacted headers and cannot break a request', function()
    local seenReq, seenRes
    local api = newApi({
        onRequest = function(i) seenReq = i; error('boom') end,
        onResponse = function(i) seenRes = i; error('boom') end,
    })
    local res = H.call(function() return api.players.stats() end)
    ok(res)
    eq(seenReq.headers['Authorization'], 'Bearer ***')
    eq(seenReq.method, 'GET'); eq(seenRes.status, 200)
end)

test('request: list values for comma-separated query parameters are joined', function()
    local api = newApi({ serverId = 1 })
    H.call(function()
        api.players.search({ filters = { 'isOnline', 'hasNote' } })
        api.events.list({ type = { 'death', 'ban' } })
    end)
    eq(H.requests[1].query.filters, 'isOnline%2ChasNote')
    eq(H.requests[2].query.type, 'death%2Cban')
    H.call(function()
        local res, err = api.players.search({ filters = { 'isOnline', 7 } })
        eq(res, nil)
        contains(err.message, "'filters'[2]")
        res, err = api.players.search({ filters = {} })
        eq(res, nil)
        contains(err.message, 'empty list')
        -- other string parameters still reject a table
        res, err = api.history.search({ type = { 'ban' } })
        eq(res, nil)
        contains(err.message, "'type' must be a string")
    end)
end)
