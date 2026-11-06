#!/usr/bin/env bash
#
# Says whether blitzsplit.com's assetlinks.json lets an Android build open the invite links (App Links): it must list
# the build's package and signing key's SHA-256 under handle_all_urls, served as application/json with no redirect,
# which is what Android checks. Prints one sentence and exits 0 when listed, 1 when not, 2 when the file can't be read,
# so the release workflow can warn without calling a broken site a missing key. Android reads the file through
# Google's Digital Asset Links API, which caches it for up to an hour, so a key just added may still fail on devices.
#
# Usage: scripts/release/app-links-status.sh <package> <SHA-256 as AB:CD:… or abcd…>
set -euo pipefail

package="$1"
# apksigner prints abcd…, assetlinks.json lists AB:CD:… (tr rather than ${hex^^}, which macOS's bash 3 lacks)
hex="${2//:/}"
fingerprint="$(tr '[:lower:]' '[:upper:]' <<< "$hex" | sed 's/../&:/g; s/:$//')"
url="https://blitzsplit.com/.well-known/assetlinks.json"

body="$(mktemp)"
trap 'rm -f "$body"' EXIT
if ! content_type="$(curl -fsS --retry 2 --max-redirs 0 --max-time 20 -o "$body" -w '%{content_type}' "$url")"; then
  echo "$url didn't load (an error, a timeout or a redirect), so invite links may not open the app."
  exit 2
fi
if [[ "$content_type" != application/json* ]]; then
  echo "$url is served as ${content_type:-no type}, not application/json, so invite links don't open the app."
  exit 2
fi

status=0
jq -e --arg package "$package" --arg fingerprint "$fingerprint" 'any(.[];
    any(.relation[]?; . == "delegate_permission/common.handle_all_urls")
    and .target.package_name == $package
    and any(.target.sha256_cert_fingerprints[]?; ascii_upcase == $fingerprint))' "$body" > /dev/null || status=$?
case "$status" in
  0) echo "$url lists $package with this key, so invite links open the app (Google's cache may take up to an hour)." ;;
  1) echo "$url doesn't list $package with this key, so invite links don't open the app (docs/release-process.md#outside-the-stores)." ;;
  *)
    echo "$url isn't a list of statements jq can read, so invite links may not open the app."
    exit 2
    ;;
esac
exit "$status"
