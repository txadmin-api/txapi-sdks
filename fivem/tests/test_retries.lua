-- SPEC §5: retries. newApi() injects random() = 1, so backoff = the cap.

test('429 + Retry-After: any method is retried after that delay', function()
    local api = newApi()
    H.respond(
        { status = 429, mode = 'raw', headers = { ['Retry-After'] = '2' }, body = { error = 'Rate limit exceeded' } },
        { status = 200, body = { success = true, message = 'banned' } }
    )
    local res, err, resp = H.call(function() return api.players.ban({ identifier = 'a', reason = 'x' }) end)
    eq(err, nil); eq(res.message, 'banned')
    eq(#H.requests, 2); deq(H.waits, { 2000 }); eq(resp.attempts, 2)
end)

test('429 behind FXServer (headers hidden): per-minute limit retried after rateLimitRetryDelay', function()
    local api = newApi({ rateLimitRetryDelay = 15 })
    H.respond(
        { status = 429, body = { success = false, error = 'Rate limit exceeded — slow down or upgrade your plan' } },
        { status = 200, body = { success = true } }
    )
    local res = H.call(function() return api.players.ban({ identifier = 'a', reason = 'x' }) end)
    ok(res); eq(#H.requests, 2); deq(H.waits, { 15000 })
end)

test('429 with Retry-After > 60 s is not waited for; quota never retried', function()
    local api = newApi()
    H.respond({ status = 429, mode = 'raw', headers = { ['Retry-After'] = '120' }, body = { error = 'x' } })
    local _, err = H.call(function() return api.players.stats() end)
    eq(err.retryAfter, 120); eq(#H.requests, 1); deq(H.waits, {})
end)

test('502 on GET is retried with backoff up to maxRetries', function()
    local api = newApi({ maxRetries = 2 })
    H.responder = function() return { status = 502, body = { error = 'txAdmin unreachable' } } end
    local _, err = H.call(function() return api.server.status() end)
    eq(err.type, 'TxAdminUnavailableError'); eq(err.attempts, 3)
    eq(#H.requests, 3); deq(H.waits, { 500, 1000 })
end)

test('backoff is min(8 s, 0.5 s * 2^attempt) with full jitter', function()
    local api = newApi({ maxRetries = 6, random = function() return 0.5 end })
    H.responder = function() return { status = 503, body = 'x' } end
    H.call(function() return api.server.status() end)
    deq(H.waits, { 250, 500, 1000, 2000, 4000, 4000 })
end)

test('502/503/504 and connection errors on POST are NOT retried', function()
    local api = newApi()
    for _, r in ipairs({ { status = 502 }, { status = 503 }, { status = 504 }, { transportError = '7 (x)' } }) do
        H.requests = {}
        H.respond(r)
        H.call(function() return api.server.restart() end)
        eq(#H.requests, 1, 'POST retried for ' .. tostring(r.status or r.transportError))
    end
    deq(H.waits, {})
end)

test('connection error and timeout on GET are retried, then succeed', function()
    local api = newApi({ timeout = 1 })
    H.respond({ transportError = '7 (x)' }, { never = true }, { status = 200, body = { ok = 1 } })
    local res = H.call(function() return api.server.status() end)
    eq(res.ok, 1); eq(#H.requests, 3)
end)

test('503 Retry-After is honoured for GET', function()
    local api = newApi()
    H.respond({ status = 503, mode = 'raw', headers = { ['Retry-After'] = '3' } }, { status = 200, body = {} })
    H.call(function() return api.server.status() end)
    deq(H.waits, { 3000 })
end)

test('maxRetries = 0 disables every retry', function()
    local api = newApi({ maxRetries = 0 })
    H.respond({ status = 429, mode = 'raw', headers = { ['Retry-After'] = '1' } }, { status = 502 }, { status = 429 })
    H.call(function()
        api.players.stats(); api.players.stats(); api.players.stats()
    end)
    eq(#H.requests, 3); deq(H.waits, {})
end)

test('4xx other than 429 are never retried', function()
    local api = newApi()
    for _, s in ipairs({ 400, 401, 403, 404, 409, 422 }) do H.respond({ status = s }) end
    H.call(function() for _ = 1, 6 do api.players.stats() end end)
    eq(#H.requests, 6)
end)
