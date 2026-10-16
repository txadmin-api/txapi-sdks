-- Pagination helpers block, so run them in a thread. `<method>Pages` yields whole pages,
-- `<method>All` yields single rows. After the loop, `.err` is the error that ended it.
Citizen.CreateThread(function()
    local api = TxApi.new()

    local bans = api.history.bansAll({ order = 'desc' })
    local n = 0
    for ban in bans do
        n = n + 1
        if ban.isRevoked then print('revoked: ' .. ban.id) end
        if n >= 500 then break end -- stop whenever you like; nothing more is fetched
    end
    if bans.err then print('stopped early: ' .. tostring(bans.err)) end

    for page in api.players.searchPages({ query = 'John', sortBy = 'lastConnection' }) do
        for _, p in ipairs(page.players) do print(p.displayName, p.license) end
    end

    for chat in api.events.chatAll({ since = '1h' }) do
        print(chat.player, chat.message)
    end
end)
