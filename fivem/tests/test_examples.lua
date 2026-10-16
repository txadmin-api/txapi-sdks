-- The example resource works against the mock (examples/txapi-example/server.lua).

local chat = {}
function TriggerClientEvent(name, src, payload) chat[#chat + 1] = { src = src, msg = payload.args[2] } end
function GetPlayerIdentifierByType(src, kind) return 'license:abc' .. src end

local function loadExample()
    H.convars.txapi_key = KEY
    H.convars.txapi_base_url = 'https://api.test'
    chat = {}
    dofile(SDK_DIR .. 'examples/txapi-example/server.lua')
end

local function runCommand(name, src, args)
    Citizen.CreateThread(function() H.commands[name].fn(src, args) end) -- FiveM runs commands in a thread
    H.run()
end

test('/txban searches, then bans the first match', function()
    loadExample()
    H.aces['5|txapi.ban'] = true
    H.respond(
        { status = 200, body = { players = { { license = 'deadbeef', displayName = 'John' } }, hasReachedEnd = true } },
        { status = 200, body = { success = true, message = 'Banned' } }
    )
    runCommand('txban', 5, { 'John', '24', 'Cheating', 'hard' })
    eq(#H.requests, 2)
    eq(H.requests[1].rawQuery, 'query=John')
    eq(H.requests[2].data, '{"durationHours":24,"identifier":"license:deadbeef","reason":"Cheating hard"}')
    contains(chat[#chat].msg, 'banned John')
end)

test('/txban is refused without the ACE and bans permanently with 0 hours from the console', function()
    loadExample()
    runCommand('txban', 7, { 'John', '24', 'x' })
    eq(#H.requests, 0)
    contains(chat[1].msg, 'not allowed')
    H.respond(
        { status = 200, body = { players = { { license = 'beef', displayName = 'Jane' } }, hasReachedEnd = true } },
        { status = 200, body = { success = true } }
    )
    runCommand('txban', 0, { 'Jane', '0', 'perm' })
    eq(H.requests[2].data, '{"identifier":"license:beef","reason":"perm"}')
end)

test('banOnline export uses the callback style', function()
    loadExample()
    eq(H.exports.banOnline(3, 'Cheating'), true)
    H.run()
    eq(H.requests[1].data, '{"identifier":"license:abc3","reason":"Cheating"}')
end)
