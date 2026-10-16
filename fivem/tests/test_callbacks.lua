-- Callback variant: last argument a function → the call returns at once and works outside threads.

test('callback variant returns immediately and delivers (res, err, resp) later', function()
    local api = newApi()
    local got
    local ret = api.players.ban({ identifier = 'license:a', reason = 'x' }, function(res, err, resp)
        got = { res = res, err = err, resp = resp }
    end)
    eq(ret, nil); eq(got, nil, 'callback must not run synchronously')
    H.run()
    ok(got); eq(got.err, nil); eq(got.res.success, true); eq(got.resp.status, 200)
end)

test('callback variant with positional path params and with no params', function()
    local api = newApi()
    local a, b
    api.history.revoke('W1', { reason = 'r' }, function(res) a = res end)
    api.server.status(function(res) b = res end)
    H.run()
    ok(a and b)
    eq(H.requests[1].path, '/api/v1/history/revoke/W1'); eq(H.requests[2].path, '/api/v1/server/status')
end)

test('errors and validation errors arrive through the callback', function()
    local api = newApi({ maxRetries = 0 })
    H.respond({ status = 404, body = { error = 'unknown action' } })
    local e1, e2
    api.history.action('X', function(_, err) e1 = err end)
    api.players.ban({ reason = 'x' }, function(_, err) e2 = err end)
    H.run()
    eq(e1.type, 'NotFoundError'); eq(e2.type, 'ValidationError')
    eq(#H.requests, 1)
end)

test('api.call with a callback', function()
    local api = newApi()
    local r
    api.call('players.stats', {}, function(res) r = res end)
    H.run()
    ok(r)
end)
