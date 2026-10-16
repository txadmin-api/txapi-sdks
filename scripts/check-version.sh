#!/usr/bin/env sh
# All four SDKs are released together under one version. This prints the
# version every file carries and fails if they disagree — or, with an
# argument, if they differ from it (the release tag without its "v").
#
#   sh scripts/check-version.sh            # consistent?
#   sh scripts/check-version.sh 1.2.0      # consistent and equal to 1.2.0?
set -eu
root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"

pick() { sed -n "$2" "$1" | head -1; }

versions="
typescript/package.json|$(pick typescript/package.json 's/^  "version": "\([^"]*\)".*/\1/p')
typescript/src/version.ts|$(pick typescript/src/version.ts 's/^export const VERSION = "\([^"]*\)".*/\1/p')
python/pyproject.toml|$(pick python/pyproject.toml 's/^version = "\([^"]*\)".*/\1/p')
python/src/txapi/_version.py|$(pick python/src/txapi/_version.py 's/^__version__ = "\([^"]*\)".*/\1/p')
java/pom.xml|$(pick java/pom.xml 's:^  <version>\([^<]*\)</version>.*:\1:p')
java/.../TxApi.java|$(pick java/src/main/java/cloud/txapi/sdk/TxApi.java 's/.*String VERSION = "\([^"]*\)".*/\1/p')
fivem/fxmanifest.lua|$(pick fivem/fxmanifest.lua "s/^version '\([^']*\)'.*/\1/p")
fivem/lib/txapi.lua|$(pick fivem/lib/txapi.lua "s/^TxApi.VERSION = '\([^']*\)'.*/\1/p")
"

expected="${1:-}"
echo "$versions" | while IFS='|' read -r file v; do
  [ -z "$file" ] || printf '%-30s %s\n' "$file" "${v:-<missing>}"
done
distinct="$(echo "$versions" | sed -n 's/^[^|][^|]*|//p' | sort -u)"
missing="$(echo "$versions" | grep -c '|$' || true)"
if [ "$missing" != "0" ] || [ "$(echo "$distinct" | wc -l)" -ne 1 ]; then
  echo "SDK versions disagree (or one is missing) — bump all of them together" >&2
  exit 1
fi
if [ -n "$expected" ] && [ "$expected" != "$distinct" ]; then
  echo "SDK version is $distinct, but the release is $expected" >&2
  exit 1
fi
echo "all SDKs at $distinct"
