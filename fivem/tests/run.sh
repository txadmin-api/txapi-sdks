#!/usr/bin/env sh
# Offline tests for the txApi FiveM SDK. Run from anywhere.
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
LUA="${LUA:-lua5.4}"
LUAC="${LUAC:-luac5.4}"

# 1. generated endpoint table is up to date with sdks/spec/endpoints.json (needs Node)
if command -v node >/dev/null 2>&1; then
  node "$DIR/../scripts/gen-endpoints.mjs" --check
else
  echo "node not found — skipping the generator freshness check (test_spec.lua still compares the tables)"
fi

# 2. every Lua file of the resource and the examples parses as Lua 5.4
for f in $(find "$DIR/.." -name '*.lua' -not -path '*/tests/*'); do
  "$LUAC" -p "$f"
done
echo "syntax ok"

# 3. the test suite
exec "$LUA" "$DIR/run.lua"
