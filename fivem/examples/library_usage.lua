-- Library mode, all features in one place. Put this in a server script of a resource
-- whose fxmanifest loads '@txapi/lib/endpoints.lua' and '@txapi/lib/txapi.lua' first.

local api = TxApi.new({
    -- apiKey   = defaults to the convar txapi_key (server.cfg: set txapi_key "txp_…" — set, NOT setr)
    -- serverId = defaults to the convar txapi_server_id
    timeout = 20,
    maxRetries = 2,
    onResponse = function(info)
        if info.error then print(('[txapi] %s %s → %s'):format(info.method, info.url, tostring(info.error))) end
    end,
})

Citizen.CreateThread(function()
    -- 1. Who am I? (plan, quota, key)
    local me, err = api.account.me()
    if not me then return print('txApi not usable: ' .. tostring(err)) end
    print(('plan %s, %s requests left this month'):format(me.plan.code, me.quota.unlimited and 'unlimited' or tostring(me.quota.remaining)))

    -- 2. Ban with Discord ban, read the raw response (status, headers, rate limit)
    local res, banErr, resp = api.players.ban({
        identifier = 'license:0123456789abcdef0123456789abcdef01234567',
        reason = 'Cheating',
        durationHours = 168,
        discordBan = true,
    })
    if res then
        print('ban ok, HTTP ' .. resp.status)
        if res.discord then print('discord: ' .. json.encode(res.discord)) end
    elseif banErr.type == 'NotFoundError' then
        print('no such player')
    else
        print('ban failed: ' .. tostring(banErr))
    end

    -- 3. Every online player, across pages
    local online = api.players.onlineAll()
    for p in online do print(p.displayName, p.playTime) end
    if online.err then print('stopped: ' .. tostring(online.err)) end

    -- 4. Kills of the last 24 h, page by page
    for page in api.events.killsPages({ since = '24h', limit = 200 }) do
        print(#page.events .. ' kills on this page')
    end

    -- 5. Console snapshot instead of a WebSocket (FiveM Lua has no WebSocket client)
    local console = api.console.buffer()
    if console then print(json.encode(console)) end

    -- 6. Revoke a ban, also lifting the Discord ban
    api.history.revoke('B1234', { reason = 'appeal accepted', discordUnban = true })

    -- 7. Restart a resource on a different server than the default
    api.server.resources.restart({ name = 'chat', serverId = 3 })
end)

-- Callback style works from anywhere, e.g. the top level of a script:
api.server.announcement({ message = 'Restart in 5 minutes' }, function(res, err)
    print(res and 'announced' or ('announcement failed: ' .. tostring(err)))
end)
