-- The txapi resource itself: convar-configured singleton + exports (server/main.lua).

local function loadResource()
    dofile(SDK_DIR .. 'server/main.lua')
end

test('exports.call dispatches by spec id and Lua name, path params by name', function()
    H.convars.txapi_key = KEY
    H.convars.txapi_base_url = 'https://api.test'
    H.convars.txapi_server_id = '4'
    loadResource()
    local results = {}
    local started = H.exports.call('players.ban', { identifier = 'license:a', reason = 'x', durationHours = 1 },
        function(res, err) results[1] = { res, err } end)
    H.exports.call('server.resources.restart', { name = 'chat' }, function(res, err) results[2] = { res, err } end)
    H.exports.call('history.revoke', { actionId = 'B1', reason = 'r' }, function(res, err) results[3] = { res, err } end)
    H.exports.call('does.not.exist', {}, function(res, err) results[4] = { res, err } end)
    eq(started, true)
    H.run()
    ok(results[1][1]); ok(results[2][1]); ok(results[3][1])
    eq(results[4][2].type, 'ValidationError')
    eq(H.requests[1].url, 'https://api.test/api/v1/players/ban?serverId=4')
    eq(H.requests[2].path, '/api/v1/server/resource/restart')
    eq(H.requests[3].path, '/api/v1/history/revoke/B1')
    eq(#H.requests, 3)
end)

test('exports without a key report a ConfigurationError, then pick the key up when set', function()
    loadResource()
    local e
    local started = H.exports.call('players.stats', {}, function(_, err) e = err end)
    H.run()
    eq(started, false)
    eq(e.type, 'ConfigurationError'); contains(e.message, 'txapi_key')
    eq(H.exports.isConfigured(), false)
    H.convars.txapi_key = KEY
    eq(H.exports.isConfigured(), true)
    local r
    H.exports.call('players.stats', {}, function(res) r = res end)
    H.run()
    ok(r)
end)

test('exports.getRateLimit / version / methods; a callback is required', function()
    H.convars.txapi_key = KEY
    loadResource()
    H.respond({ status = 200, body = {}, headers = { ['X-RateLimit-Limit'] = '60', ['X-RateLimit-Remaining'] = '10', ['X-RateLimit-Reset'] = '9' } })
    H.exports.call('players.stats', nil, function() end)
    H.run()
    deq(H.exports.getRateLimit(), { limit = 60, remaining = 10, resetSeconds = 9 })
    local v, av = H.exports.version()
    eq(v, '1.0.0'); eq(av, '1.8.0')
    eq(#H.exports.methods(), 67)
    raises(function() H.exports.call('players.stats', {}) end, 'callback')
end)

test('onResourceStart prints readiness without the key', function()
    H.convars.txapi_key = KEY
    loadResource()
    for _, fn in ipairs(H.events.onResourceStart) do fn('txapi') end
    contains(H.prints[#H.prints], '[txapi] ready')
    ok(not H.prints[#H.prints]:find('SECRET'))
end)
