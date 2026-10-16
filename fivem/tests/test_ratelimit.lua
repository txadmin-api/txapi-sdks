-- SPEC §6: rate-limit headers, case-insensitive, exposed on the client, the response and errors.

test('X-RateLimit-* parsed case-insensitively into lastRateLimit and resp', function()
    local api = newApi()
    H.respond(
        { status = 200, body = {}, headers = { ['x-ratelimit-limit'] = '120', ['X-RATELIMIT-REMAINING'] = ' 119 ', ['X-RateLimit-Reset'] = '42' } },
        { status = 200, body = {}, headers = { ['X-RateLimit-Limit'] = { '60', '60' }, ['X-RateLimit-Remaining'] = '3', ['X-RateLimit-Reset'] = '5' } }
    )
    local r1, r2 = H.call(function()
        local _, _, a = api.players.stats()
        local _, _, b = api.players.stats()
        return a, b
    end)
    deq(r1.rateLimit, { limit = 120, remaining = 119, resetSeconds = 42 })
    deq(r2.rateLimit, { limit = 60, remaining = 3, resetSeconds = 5 })
    deq(api.lastRateLimit, { limit = 60, remaining = 3, resetSeconds = 5 })
    eq(api.lastResponse.status, 200)
end)

test('no rate-limit headers → rateLimit nil, lastRateLimit kept', function()
    local api = newApi({ maxRetries = 0 })
    H.respond(
        { status = 200, body = {}, headers = { ['X-RateLimit-Limit'] = '60', ['X-RateLimit-Remaining'] = '59', ['X-RateLimit-Reset'] = '30' } },
        { status = 200, body = {} },
        { status = 404, body = { error = 'nope' } }
    )
    local r2, err = H.call(function()
        api.players.stats()
        local _, _, b = api.players.stats()
        local _, e = api.players.stats()
        return b, e
    end)
    eq(r2.rateLimit, nil)
    eq(api.lastRateLimit.remaining, 59)
    eq(err.rateLimit.remaining, 59, 'errors carry the last known rate limit (FXServer hides error headers)')
end)

test('TxApi.header and parseRateLimit helpers', function()
    eq(TxApi.header({ ['content-TYPE'] = 'a' }, 'Content-Type'), 'a')
    eq(TxApi.header(nil, 'x'), nil)
    eq(TxApi.parseRateLimit({}), nil)
end)
