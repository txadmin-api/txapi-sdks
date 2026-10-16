-- Example resource using the txApi SDK in library mode. Not loaded by default:
-- copy this folder into resources/ and add `ensure txapi-example` AFTER `ensure txapi`.
fx_version 'cerulean'
games { 'gta5', 'rdr3' }
rdr3_warning 'I acknowledge that this is a prerelease build of RedM, and I am aware my resources *will* become incompatible once RedM ships.'
lua54 'yes'

name 'txapi-example'
description '/txban, /txstatus and a banOnline export — built on the txApi SDK'
version '1.0.0'

dependency 'txapi'

server_scripts {
    '@txapi/lib/endpoints.lua',
    '@txapi/lib/txapi.lua',
    'server.lua',
}
