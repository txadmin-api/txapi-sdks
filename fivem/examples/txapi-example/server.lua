--[[
    server.cfg:
        set txapi_key "txp_…"                          # set, NOT setr
        set txapi_server_id "12"                        # only if your account has several servers
        add_ace group.admin txapi.ban allow             # who may use /txban
        ensure txapi
        ensure txapi-example
]]

local api = TxApi.new({ userAgent = GetCurrentResourceName() })

local function reply(src, msg)
    if src == 0 then
        print('[txban] ' .. msg)
    else
        TriggerClientEvent('chat:addMessage', src, { args = { 'txApi', msg } })
    end
end

-- /txban <name> <hours|0> <reason…>   — search by name, then ban (0 = permanent).
-- Allowed for the server console and for players with the ACE "txapi.ban".
RegisterCommand('txban', function(src, args)
    if src ~= 0 and not IsPlayerAceAllowed(src, 'txapi.ban') then
        return reply(src, 'You are not allowed to use /txban.')
    end
    local name, hours, reason = args[1], tonumber(args[2]), table.concat(args, ' ', 3)
    if not name or not hours or reason == '' then
        return reply(src, 'usage: /txban <name> <hours|0> <reason>')
    end

    -- Command handlers run in a FiveM thread, so blocking calls are fine here.
    local found, err = api.players.search({ query = name })
    if not found then return reply(src, 'search failed: ' .. tostring(err)) end
    local target = found.players and found.players[1]
    if not target then return reply(src, 'no player named ' .. name) end

    local res, banErr = api.players.ban({
        identifier = 'license:' .. target.license,
        reason = reason,
        durationHours = hours > 0 and hours or nil, -- nil = permanent
    })
    if not res then
        if banErr.type == 'RateLimitError' and banErr.quotaExhausted then
            return reply(src, 'monthly API quota used up — resets on the 1st (UTC)')
        end
        return reply(src, 'ban failed: ' .. tostring(banErr))
    end
    reply(src, ('banned %s (%s)'):format(target.displayName, res.message or 'ok'))
end, false)

-- /txstatus — console only: server status + remaining requests this minute.
RegisterCommand('txstatus', function(src)
    if src ~= 0 then return end
    local status, err = api.server.status()
    if not status then return print('[txstatus] ' .. tostring(err)) end
    print(('[txstatus] %s — FXServer running=%s, txAdmin reachable=%s'):format(
        tostring(status.serverName), tostring(status.online), tostring(status.txAdminReachable)))
    local rl = api.lastRateLimit
    if rl then print(('[txstatus] %s/%s requests left this minute'):format(rl.remaining, rl.limit)) end
end, true)

-- For other resources (anticheat …): exports['txapi-example']:banOnline(source, 'Cheating')
exports('banOnline', function(src, reason)
    local license = GetPlayerIdentifierByType(src, 'license')
    if not license then return false end
    api.players.ban({ identifier = license, reason = reason }, function(res, err)
        print(res and ('[txapi-example] banned ' .. license) or ('[txapi-example] ban failed: ' .. tostring(err)))
    end)
    return true
end)
