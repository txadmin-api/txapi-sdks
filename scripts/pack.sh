#!/usr/bin/env sh
# Builds the release files of the SDKs: the npm tarball, the Python wheel +
# sdist, the Java jar + pom and the FiveM resource zip.
#
#   sh scripts/pack.sh <typescript|python|java|fivem|all> <out-dir>
#
# They are attached to every GitHub release and served by txapi.cloud under
# /sdks/ for setups that cannot reach npm / PyPI / Maven Central. File names
# carry the version: a new release is a new URL, nothing is ever overwritten
# in a cache.
set -eu

sdk="${1:?usage: pack.sh <typescript|python|java|fivem|all> <out-dir>}"
out="${2:?usage: pack.sh <typescript|python|java|fivem|all> <out-dir>}"
root="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$out"
out="$(cd "$out" && pwd)"

pack_typescript() {
  cd "$root/typescript"
  npm run build >/dev/null
  npm pack --silent --pack-destination "$out" >/dev/null
}

pack_python() {
  cd "$root/python"
  rm -rf dist
  ${PYTHON:-python} -m build --wheel --sdist --outdir "$out" >/dev/null
}

pack_java() {
  cd "$root/java"
  ${MVN:-mvn} -B -q --no-transfer-progress -DskipTests package
  version="$(sed -n 's:.*<version>\([^<]*\)</version>.*:\1:p' pom.xml | head -1)"
  cp "target/txapi-sdk-$version.jar" "$out/"
  # The pom travels with the jar: `mvn install:install-file -DpomFile=…` reads
  # the jackson dependency from it, so the download behaves like a real artifact.
  cp pom.xml "$out/txapi-sdk-$version.pom"
}

pack_fivem() {
  version="$(sed -n "s/^version '\([^']*\)'.*/\1/p" "$root/fivem/fxmanifest.lua" | head -1)"
  [ -n "$version" ] || { echo "no version in fxmanifest.lua" >&2; exit 1; }
  stage="$(mktemp -d)"
  # The folder inside the zip must be called "txapi": other resources load the
  # library as @txapi/lib/txapi.lua. Tests and generator scripts stay out.
  mkdir "$stage/txapi"
  cp -R "$root/fivem/fxmanifest.lua" "$root/fivem/lib" "$root/fivem/server" \
        "$root/fivem/examples" "$root/fivem/README.md" "$root/fivem/CHANGELOG.md" \
        "$root/fivem/LICENSE" "$stage/txapi/"
  (cd "$stage" && zip -qr "$out/txapi-fivem-$version.zip" txapi)
  rm -rf "$stage"
}

case "$sdk" in
  typescript|python|java|fivem) "pack_$sdk" ;;
  all) for s in typescript python java fivem; do (pack_$s); done ;;
  *) echo "unknown sdk: $sdk" >&2; exit 2 ;;
esac

cd "$out" && sha256sum -- * 2>/dev/null | grep -v SHA256SUMS > SHA256SUMS || true
