-- SPEC §7: auto-paging iterators (blocking style, inside a thread).

local function player(license, playTime, tsJoined, tsLast)
    return { license = license, displayName = license, playTime = playTime, tsJoined = tsJoined, tsLastConnection = tsLast }
end

test('players.searchPages: offset = sort key of last row (sessions → tsJoined), cursor = license, query kept', function()
    local api = newApi({ serverId = 1 })
    H.respond(
        { status = 200, body = { players = { player('a', 10, 100, 1000), player('b', 20, 200, 2000) }, hasReachedEnd = false } },
        { status = 200, body = { players = { player('c', 30, 300, 3000) }, hasReachedEnd = true } }
    )
    local pages = H.call(function()
        local out = {}
        local it = api.players.searchPages({ query = 'J o', sortBy = 'sessions', order = 'desc' })
        for page in it do out[#out + 1] = #page.players end
        eq(it.err, nil); eq(it.pages, 2)
        return out
    end)
    deq(pages, { 2, 1 })
    eq(#H.requests, 2)
    eq(H.requests[1].rawQuery, 'serverId=1&query=J%20o&sortBy=sessions&order=desc')
    eq(H.requests[2].rawQuery, 'serverId=1&query=J%20o&sortBy=sessions&order=desc&offset=200&cursor=b')
end)

test('players.searchAll with tsLastConnection / lastConnection alias, default playTime', function()
    for _, c in ipairs({ { 'lastConnection', '2000' }, { 'tsLastConnection', '2000' }, { nil, '20' } }) do
        H.reset()
        local api = newApi()
        H.respond(
            { status = 200, body = { players = { player('a', 10, 100, 1000), player('b', 20, 200, 2000) }, hasReachedEnd = false } },
            { status = 200, body = { players = {}, hasReachedEnd = false } }
        )
        local n = H.call(function()
            local count = 0
            for _ in api.players.searchAll({ sortBy = c[1] }) do count = count + 1 end
            return count
        end)
        eq(n, 2)
        eq(H.requests[2].query.offset, c[2], tostring(c[1]))
        eq(#H.requests, 2, 'stops on an empty page')
    end
end)

test('players.onlineAll / lookupAll page by playTime', function()
    local api = newApi()
    H.respond(
        { status = 200, body = { players = { player('a', 10, 100, 1000) }, hasReachedEnd = false } },
        { status = 200, body = { players = { player('b', 5, 1, 1) }, hasReachedEnd = true } },
        { status = 200, body = { players = { player('c', 7, 1, 1) }, hasReachedEnd = false } },
        { status = 200, body = { players = {}, hasReachedEnd = true } }
    )
    local online, lookup = H.call(function()
        local a, b = {}, {}
        for p in api.players.onlineAll() do a[#a + 1] = p.license end
        for p in api.players.lookupAll({ identifier = 'discord:1' }) do b[#b + 1] = p.license end
        return a, b
    end)
    deq(online, { 'a', 'b' }); deq(lookup, { 'c' })
    eq(H.requests[2].rawQuery, 'offset=10&cursor=a')
    eq(H.requests[4].rawQuery, 'identifier=discord%3A1&offset=7&cursor=c')
end)

test('history.bansPages / warnsAll / searchAll: offset = timestamp, cursor = id', function()
    local api = newApi()
    H.respond(
        { status = 200, body = { history = { { id = 'B1', timestamp = 300 }, { id = 'B2', timestamp = 200 } }, hasReachedEnd = false } },
        { status = 200, body = { history = { { id = 'B3', timestamp = 100 } }, hasReachedEnd = true } },
        { status = 200, body = { history = { { id = 'W1', timestamp = 50 } }, hasReachedEnd = true } },
        { status = 200, body = { history = { { id = 'S1', timestamp = 9 } }, hasReachedEnd = false } },
        { status = 200, body = { history = { { id = 'S2', timestamp = 8 } }, hasReachedEnd = true } }
    )
    local bans, warns, found = H.call(function()
        local a, b, c = {}, {}, {}
        for page in api.history.bansPages({ order = 'desc' }) do
            for _, r in ipairs(page.history) do a[#a + 1] = r.id end
        end
        for r in api.history.warnsAll() do b[#b + 1] = r.id end
        for r in api.history.searchAll({ searchType = 'reason', search = 'cheat' }) do c[#c + 1] = r.id end
        return a, b, c
    end)
    deq(bans, { 'B1', 'B2', 'B3' }); deq(warns, { 'W1' }); deq(found, { 'S1', 'S2' })
    eq(H.requests[2].rawQuery, 'order=desc&offset=200&cursor=B2')
    eq(#H.requests, 5)
    eq(H.requests[5].rawQuery, 'searchType=reason&search=cheat&offset=9&cursor=S1')
end)

for _, nullAsNil in ipairs({ false, true }) do
    test('events: cursor = nextCursor until it is missing or null (nullAsNil=' .. tostring(nullAsNil) .. ')', function()
        H.json.nullAsNil = nullAsNil
        local api = newApi()
        H.respond(
            { status = 200, body = '{"success":true,"events":[{"id":1},{"id":2}],"nextCursor":"1759838400123_2"}' },
            { status = 200, body = '{"success":true,"events":[{"id":3}],"nextCursor":null}' },
            { status = 200, body = '{"success":true,"events":[{"id":4}]}' }
        )
        local ids = H.call(function()
            local out = {}
            for e in api.events.deathsAll({ pvp = true, limit = 2 }) do out[#out + 1] = e.id end
            for e in api.events.adminActionsAll() do out[#out + 1] = e.id end
            return out
        end)
        deq(ids, { 1, 2, 3, 4 })
        eq(H.requests[2].rawQuery, 'pvp=true&cursor=1759838400123_2&limit=2')
        eq(#H.requests, 3)
    end)
end

test('every paged events endpoint has Pages/All helpers', function()
    local api = newApi()
    for _, n in ipairs({ 'list', 'deaths', 'kills', 'chat', 'commands', 'adminActions', 'actions', 'sessions' }) do
        eq(type(api.events[n .. 'Pages']), 'function', n)
        eq(type(api.events[n .. 'All']), 'function', n)
    end
end)

test('an error ends the loop and is kept in .err', function()
    local api = newApi({ maxRetries = 0 })
    H.respond(
        { status = 200, body = { events = { { id = 1 } }, nextCursor = 'c1' } },
        { status = 403, body = { error = 'event log disabled' } }
    )
    local it, rows = H.call(function()
        local i = api.events.listAll()
        local n = 0
        for _ in i do n = n + 1 end
        return i, n
    end)
    eq(rows, 1)
    eq(it.err.type, 'PermissionDeniedError'); eq(it.err.message, 'event log disabled')
end)

test('a cursor that does not move stops the loop instead of spinning', function()
    local api = newApi()
    H.responder = function() return { status = 200, body = { events = { { id = 1 } }, nextCursor = 'same' } } end
    local n = H.call(function()
        local c = 0
        for _ in api.events.listPages() do c = c + 1 end
        return c
    end)
    eq(n, 2); eq(#H.requests, 2)
end)

test('breaking out of the loop stops fetching', function()
    local api = newApi()
    H.responder = function(req)
        return { status = 200, body = { players = { player('p' .. #H.requests, #H.requests, 1, 1) }, hasReachedEnd = false } }
    end
    H.call(function()
        for page in api.players.searchPages() do
            if #H.requests == 3 then break end
        end
    end)
    eq(#H.requests, 3)
end)
