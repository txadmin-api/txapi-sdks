--[[
    The `txapi` resource itself: a convar-configured client plus exports, so other
    resources can use txApi without loading the library.

    server.cfg:
        set txapi_key "txp_…"          # set — NEVER setr (setr replicates the key to every player)
        set txapi_server_id "12"       # optional: default txAdmin server
        set txapi_base_url "https://txapi.cloud"   # optional
        ensure txapi

    From any server script:
        exports.txapi:call('players.ban', { identifier = 'license:…', reason = 'Cheating', durationHours = 24 },
            function(res, err) … end)
]]

local client, clientKey, initError

local function currentKey()
    return GetConvar('txapi_key', '')
end

--- The shared client. Rebuilt when the key/base URL/server convars change.
local function getClient()
    local fingerprint = currentKey() .. '\0' .. GetConvar('txapi_base_url', '') .. '\0' .. GetConvar('txapi_server_id', '')
    if client and clientKey == fingerprint then return client end
    local ok, result = pcall(TxApi.new, {})
    if ok then
        client, clientKey, initError = result, fingerprint, nil
    else
        client, clientKey, initError = nil, nil, tostring(result)
    end
    return client
end

local function configurationError()
    return setmetatable({
        type = 'ConfigurationError', isTxApiError = true,
        message = initError or 'txapi is not configured',
    }, { __tostring = function(e) return e.type .. ': ' .. e.message end })
end

--- exports.txapi:call(method, params, cb)
-- method: spec id ('server.resource.restart') or Lua name ('server.resources.restart').
-- params: named fields, including path parameters (actionId, requestId, identifier, name)
--         and an optional serverId override.
-- cb(res, err, resp): called once, from a FiveM thread. Returns true if the call was started.
exports('call', function(method, params, cb)
    if type(cb) ~= 'function' and not (type(cb) == 'table' and getmetatable(cb) and getmetatable(cb).__call) then
        error('exports.txapi:call(method, params, cb): a callback is required', 2)
    end
    local c = getClient()
    if not c then
        Citizen.CreateThread(function() cb(nil, configurationError(), nil) end)
        return false
    end
    c.call(method, params or {}, function(res, err, resp) cb(res, err, resp) end)
    return true
end)

--- exports.txapi:getRateLimit() → { limit, remaining, resetSeconds } of the last response, or nil
exports('getRateLimit', function()
    return client and client.lastRateLimit or nil
end)

--- exports.txapi:isConfigured() → true if txapi_key is set and valid-looking
exports('isConfigured', function()
    return getClient() ~= nil
end)

--- exports.txapi:methods() → list of method names
exports('methods', function()
    return TxApi.methods()
end)

--- exports.txapi:version() → sdk version, API version
exports('version', function()
    return TxApi.VERSION, TxApi.API_VERSION
end)

AddEventHandler('onResourceStart', function(name)
    if name ~= GetCurrentResourceName() then return end
    if getClient() then
        print(('[txapi] ready — SDK %s, API %s, %s'):format(TxApi.VERSION, TxApi.API_VERSION, tostring(client)))
    else
        print('[txapi] not configured: ' .. tostring(initError))
    end
end)
