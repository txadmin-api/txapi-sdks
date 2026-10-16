-- Offline test runner:  lua5.4 sdks/fivem/tests/run.lua   (from anywhere)
local here = debug.getinfo(1, 'S').source:sub(2):match('^(.*[/\\])') or './'
TESTS_DIR = here
SDK_DIR = here .. '../'
SPEC_PATH = here .. '../../spec/endpoints.json'

H = dofile(here .. 'harness/fivem.lua')
dofile(SDK_DIR .. 'lib/endpoints.lua')
dofile(SDK_DIR .. 'lib/txapi.lua')

local passed, failed, failures = 0, 0, {}
local current = {}

function test(name, fn)
    current[#current + 1] = { name = name, fn = fn }
end

-- assertions ---------------------------------------------------------------
local function fmt(v)
    if type(v) == 'string' then return string.format('%q', v) end
    return tostring(v)
end

function eq(actual, expected, msg)
    if actual ~= expected then
        error(('%sexpected %s, got %s'):format(msg and (msg .. ': ') or '', fmt(expected), fmt(actual)), 2)
    end
end

function ok(v, msg)
    if not v then error(msg or 'expected a truthy value', 2) end
end

local function deepEqual(a, b)
    if type(a) ~= type(b) then return false end
    if type(a) ~= 'table' then return a == b end
    for k, v in pairs(a) do if not deepEqual(v, b[k]) then return false end end
    for k in pairs(b) do if a[k] == nil then return false end end
    return true
end
function deq(a, b, msg)
    if not deepEqual(a, b) then
        error(('%sexpected %s, got %s'):format(msg and (msg .. ': ') or '', H.json.encode(b), H.json.encode(a)), 2)
    end
end

function contains(haystack, needle, msg)
    if type(haystack) ~= 'string' or not haystack:find(needle, 1, true) then
        error(('%sexpected %s to contain %s'):format(msg and (msg .. ': ') or '', fmt(haystack), fmt(needle)), 2)
    end
end

function raises(fn, needle)
    local okk, err = pcall(fn)
    if okk then error('expected an error', 2) end
    if needle then contains(tostring(err), needle) end
    return err
end

function readSpec()
    local f = assert(io.open(SPEC_PATH, 'r'))
    local s = f:read('a')
    f:close()
    return H.json.decode(s)
end

KEY = 'txp_test_SECRETKEY_1234567890'
function newApi(cfg)
    cfg = cfg or {}
    if cfg.apiKey == nil then cfg.apiKey = KEY end
    if cfg.baseUrl == nil then cfg.baseUrl = 'https://api.test' end
    if cfg.random == nil then cfg.random = function() return 1 end end
    return TxApi.new(cfg)
end

-- run -----------------------------------------------------------------------
local files = {
    'test_spec.lua', 'test_coverage.lua', 'test_request.lua', 'test_errors.lua',
    'test_retries.lua', 'test_ratelimit.lua', 'test_pagination.lua', 'test_callbacks.lua',
    'test_exports.lua', 'test_examples.lua', 'test_full_params.lua',
}
for _, f in ipairs(files) do
    current = {}
    dofile(here .. f)
    for _, t in ipairs(current) do
        H.reset()
        local okk, err = xpcall(t.fn, debug.traceback)
        if okk then
            passed = passed + 1
        else
            failed = failed + 1
            failures[#failures + 1] = ('[%s] %s\n    %s'):format(f, t.name, tostring(err):gsub('\n', '\n    '))
        end
    end
end

for _, f in ipairs(failures) do H.print('FAIL ' .. f) end
H.print(('%d passed, %d failed'):format(passed, failed))
os.exit(failed == 0 and 0 or 1)
