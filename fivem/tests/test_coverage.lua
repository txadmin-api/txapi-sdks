-- SPEC §3: every endpoint has exactly one method; calling every method hits exactly the
-- METHOD + path-template set of spec/endpoints.json (query string ignored).

local function templateFor(spec, method, path)
    -- literal match first (/txadmin/admins/available-permissions beats /txadmin/admins/{name})
    for _, e in ipairs(spec.endpoints) do
        if e.method == method and e.path == path then return e.path end
    end
    for _, e in ipairs(spec.endpoints) do
        if e.method == method then
            local pat = '^' .. e.path:gsub('[%-%.]', '%%%0'):gsub('{[^}]+}', '[^/]+') .. '$'
            if path:match(pat) then return e.path end
        end
    end
    return '??' .. path
end

test('coverage: all 67 endpoints have a method and hit their path', function()
    local spec = readSpec()
    local api = newApi({ serverId = 3 })
    H.responder = function() return { status = 200, body = { success = true } } end
    H.call(function()
        local calls = {
            function() return api.account.me() end,
            function() return api.account.servers() end,
            function() return api.players.search({ query = 'John' }) end,
            function() return api.players.online() end,
            function() return api.players.lookup({ identifier = 'license:abc' }) end,
            function() return api.players.info({ identifier = 'license:abc' }) end,
            function() return api.players.stats() end,
            function() return api.players.warn({ identifier = 'license:abc', reason = 'r' }) end,
            function() return api.players.ban({ identifier = 'license:abc', reason = 'r', durationHours = 24, discordBan = true }) end,
            function() return api.players.kick({ identifier = 'license:abc' }) end,
            function() return api.players.dm({ identifier = 'license:abc', message = 'hi' }) end,
            function() return api.players.notes({ identifier = 'license:abc', note = 'n' }) end,
            function() return api.players.whitelist({ identifier = 'license:abc', whitelisted = true }) end,
            function() return api.history.bans() end,
            function() return api.history.warns() end,
            function() return api.history.search({ search = 'x', searchType = 'reason' }) end,
            function() return api.history.banTemplates() end,
            function() return api.history.action('B1234') end,
            function() return api.history.stats() end,
            function() return api.history.revoke('B1234', { reason = 'appeal', discordUnban = true }) end,
            function() return api.server.status() end,
            function() return api.server.diagnostics() end,
            function() return api.server.drops() end,
            function() return api.server.performance({ thread = 'svMain' }) end,
            function() return api.server.restart() end,
            function() return api.server.start() end,
            function() return api.server.stop() end,
            function() return api.server.scheduleRestart({ time = '+10' }) end,
            function() return api.server.skipRestart({ skip = true }) end,
            function() return api.server.kickAll({ reason = 'restart' }) end,
            function() return api.server.announcement({ message = 'hello' }) end,
            function() return api.server.resources.start({ name = 'chat' }) end,
            function() return api.server.resources.stop({ name = 'chat' }) end,
            function() return api.server.resources.restart({ name = 'chat' }) end,
            function() return api.server.resources.ensure({ name = 'chat' }) end,
            function() return api.server.resources.refresh() end,
            function() return api.console.buffer() end,
            function() return api.console.command({ command = 'status' }) end,
            function() return api.serverlog.buffer() end,
            function() return api.serverlog.history({ direction = 'older', ref = 1717689601000 }) end,
            function() return api.events.list({ limit = 10 }) end,
            function() return api.events.deaths({ pvp = true }) end,
            function() return api.events.kills() end,
            function() return api.events.leaderboard({ sort = 'kd' }) end,
            function() return api.events.player({ player = 'John' }) end,
            function() return api.events.summary({ since = '24h' }) end,
            function() return api.events.chat() end,
            function() return api.events.commands() end,
            function() return api.events.adminActions() end,
            function() return api.events.actions({ revoked = false }) end,
            function() return api.events.sessions() end,
            function() return api.events.playtime() end,
            function() return api.events.activity({ bucket = 'hour' }) end,
            function() return api.events.actionLog({ lines = 50 }) end,
            function() return api.whitelist.requests({ page = 1 }) end,
            function() return api.whitelist.approve('R1') end,
            function() return api.whitelist.deny('R1') end,
            function() return api.whitelist.denyAll({ before = 1717689601 }) end,
            function() return api.whitelist.approvals.list() end,
            function() return api.whitelist.approvals.add({ identifier = 'discord:1' }) end,
            function() return api.whitelist.approvals.remove('discord:1') end,
            function() return api.admins.list() end,
            function() return api.admins.get('John Doe') end,
            function() return api.admins.permissions() end,
            function() return api.admins.add({ name = 'Jane', permissions = { 'players.ban' } }) end,
            function() return api.admins.edit('Jane', { permissions = {} }) end,
            function() return api.admins.delete('Jane') end,
        }
        eq(#calls, 67, 'calls in this test')
        for i, c in ipairs(calls) do
            local res, err = c()
            ok(res, 'call #' .. i .. ' failed: ' .. tostring(err))
        end
    end)

    local expected, got = {}, {}
    for _, e in ipairs(spec.endpoints) do expected[e.method .. ' ' .. e.path] = true end
    eq(#H.requests, 67, 'requests sent')
    for _, r in ipairs(H.requests) do
        local key = r.method .. ' ' .. templateFor(spec, r.method, r.path)
        ok(not got[key], 'endpoint hit twice: ' .. key)
        got[key] = true
    end
    deq(got, expected, 'METHOD template set')
end)

test('every method name in TxApi.methods() exists on the client', function()
    local api = newApi()
    for _, name in ipairs(TxApi.methods()) do
        local node = api
        for part in name:gmatch('[^%.]+') do node = node[part] end
        eq(type(node), 'function', name)
    end
    eq(#TxApi.methods(), 67)
end)
