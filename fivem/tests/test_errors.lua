-- SPEC §4: error mapping. FXServer delivers error bodies as errorData "HTTP <code>: <body>"
-- with NO headers ('fivem' mode, the default of the mock); 'raw' mode passes body + headers.

local CASES = {
    { 400, 'BadRequestError' }, { 405, 'BadRequestError' }, { 415, 'BadRequestError' },
    { 401, 'AuthenticationError' }, { 403, 'PermissionDeniedError' }, { 404, 'NotFoundError' },
    { 409, 'ConflictError' }, { 422, 'TxAdminRejectedError' }, { 429, 'RateLimitError' },
    { 502, 'TxAdminUnavailableError' }, { 500, 'ServerError' }, { 503, 'ServerError' },
    { 504, 'ServerError' }, { 418, 'ApiError' },
}

for _, mode in ipairs({ 'fivem', 'raw' }) do
    test('status → error type (' .. mode .. ' mode)', function()
        local api = newApi({ maxRetries = 0 })
        for _, c in ipairs(CASES) do
            H.respond({ status = c[1], mode = mode, body = { success = false, error = 'msg ' .. c[1] } })
            local res, err = H.call(function() return api.players.ban({ identifier = 'a', reason = 'x' }) end)
            eq(res, nil)
            eq(err.type, c[2], 'status ' .. c[1])
            eq(err.status, c[1])
            eq(err.message, 'msg ' .. c[1])
            eq(err.body.success, false)
            eq(err.endpoint, 'players.ban')
            ok(TxApi.isError(err))
            ok(not tostring(err):find('SECRET'), 'error text leaks the key')
            contains(tostring(err), 'HTTP ' .. c[1])
        end
    end)
end

test('message: {error} > plain text > reason phrase; old FXServer errorData without body', function()
    local api = newApi({ maxRetries = 0 })
    H.respond(
        { status = 404, body = '{"error":"Player not found"}' },
        { status = 502, body = 'Bad gateway from proxy' },
        { status = 404, body = '' },
        { status = 404, errorData = 'HTTP 404' },
        { status = 403, mode = 'raw', body = '<html>forbidden</html>' }
    )
    local errs = H.call(function()
        local out = {}
        for _ = 1, 5 do local _, e = api.players.info({ identifier = 'x' }); out[#out + 1] = e end
        return out
    end)
    eq(errs[1].message, 'Player not found'); eq(errs[1].type, 'NotFoundError')
    eq(errs[2].message, 'Bad gateway from proxy'); eq(errs[2].body, 'Bad gateway from proxy')
    eq(errs[3].message, 'Not Found')
    eq(errs[4].message, 'Not Found')
    eq(errs[5].message, '<html>forbidden</html>'); eq(errs[5].type, 'PermissionDeniedError')
end)

test('429 behind FXServer (no headers): quota recognised from the body', function()
    local api = newApi()
    H.respond({ status = 429, body = { success = false, error = 'Monthly request limit reached' } })
    local _, err = H.call(function() return api.players.ban({ identifier = 'a', reason = 'x' }) end)
    eq(err.type, 'RateLimitError'); eq(err.quotaExhausted, true); eq(err.retryAfter, nil)
    eq(err.headersAvailable, false)
    eq(#H.requests, 1, 'quota is never retried')

    H.respond({ status = 429, body = { success = false, code = 'QUOTA', error = 'Monthly request limit reached' } })
    local _, err2 = H.call(function() return api.players.stats() end)
    eq(err2.quotaExhausted, true)
    eq(#H.requests, 2)
end)

test('429 with headers: Retry-After present = per-minute limit, absent = quota', function()
    local api = newApi({ maxRetries = 0 })
    H.respond(
        { status = 429, mode = 'raw', headers = { ['retry-after'] = '12' }, body = { error = 'Rate limit exceeded' } },
        { status = 429, mode = 'raw', headers = { ['X-RateLimit-Limit'] = '60' }, body = { error = 'Monthly request limit reached' } }
    )
    local e1, e2 = H.call(function()
        local _, a = api.players.stats()
        local _, b = api.players.stats()
        return a, b
    end)
    eq(e1.retryAfter, 12); eq(e1.quotaExhausted, false); eq(e1.headersAvailable, true)
    eq(e2.retryAfter, nil); eq(e2.quotaExhausted, true); eq(e2.rateLimit.limit, 60)
end)

test('transport failures: status 0 → ConnectionError, curl 28/42 → TimeoutError', function()
    local api = newApi({ maxRetries = 0 })
    H.respond(
        { transportError = "7 (Couldn't connect to server)" },
        { transportError = '28 (Timeout was reached)' },
        { transportError = '42 (Callback aborted)' }
    )
    local a, b, c = H.call(function()
        local _, x = api.players.stats()
        local _, y = api.players.stats()
        local _, z = api.players.stats()
        return x, y, z
    end)
    eq(a.type, 'ConnectionError'); contains(a.message, 'CURL error code 7')
    eq(b.type, 'TimeoutError'); eq(c.type, 'TimeoutError')
    eq(a.status, nil)
end)

test('no answer within timeout → TimeoutError; a late answer is ignored', function()
    local api = newApi({ timeout = 2, maxRetries = 0 })
    H.respond({ status = 200, delay = 5000, body = { success = true } })
    local res, err = H.call(function() return api.players.ban({ identifier = 'a', reason = 'x' }) end)
    eq(res, nil); eq(err.type, 'TimeoutError'); contains(err.message, '2 s')
end)

test('errors are tables with a readable tostring and every field the docs promise', function()
    local api = newApi({ maxRetries = 0 })
    H.respond({ status = 422, body = { success = false, error = 'Player is offline' } })
    local _, err = H.call(function() return api.players.kick({ identifier = 'a' }) end)
    eq(tostring(err), 'TxAdminRejectedError (HTTP 422): Player is offline')
    eq(err.method, 'POST'); eq(err.path, '/api/v1/players/kick'); eq(err.attempts, 1)
end)
