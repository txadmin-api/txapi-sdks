fx_version 'cerulean'
games { 'gta5', 'rdr3' }
rdr3_warning 'I acknowledge that this is a prerelease build of RedM, and I am aware my resources *will* become incompatible once RedM ships.'
lua54 'yes'

name 'txapi'
author 'Josef Peter'
description 'Official txApi SDK for FiveM/RedM — server-side Lua client for https://txapi.cloud'
version '1.0.0'
license 'MIT'
repository 'https://github.com/txadmin-api/txapi-sdks'

-- Server only: there are no client scripts — the API key must never reach a player.

server_scripts {
    'lib/endpoints.lua',
    'lib/txapi.lua',
    'server/main.lua',
}
