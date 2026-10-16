-- Exports mode: no library files needed in your resource, only `ensure txapi`
-- (configured from the convars txapi_key / txapi_server_id / txapi_base_url).
-- Exports are callback-only.

exports.txapi:call('players.ban', {
    identifier = 'license:0123456789abcdef0123456789abcdef01234567',
    reason = 'Cheating',
    durationHours = 24,
}, function(res, err)
    if res then
        print('banned: ' .. tostring(res.message))
    else
        print(('ban failed (%s): %s'):format(err.type, err.message))
    end
end)

-- Path parameters go into the table by name; method names may be the spec id or the Lua name.
exports.txapi:call('history.revoke', { actionId = 'B1234', reason = 'appeal' }, function(res, err) end)
exports.txapi:call('server.resources.restart', { name = 'chat' }, function(res, err) end)

local rl = exports.txapi:getRateLimit()          -- { limit, remaining, resetSeconds } or nil
local configured = exports.txapi:isConfigured()  -- false when txapi_key is missing
