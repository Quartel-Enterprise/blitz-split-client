#!/usr/bin/env bash
#
# Writes the App Store Connect API key where xcodebuild and altool look for it, and prints its path. Reads
# APP_STORE_CONNECT_API_KEY_ID and APP_STORE_CONNECT_API_KEY_P8 from the environment.
#
# Usage: key_path="$(scripts/release/write-asc-key.sh)"
set -euo pipefail

dir="$HOME/.appstoreconnect/private_keys"
mkdir -p "$dir"
path="$dir/AuthKey_${APP_STORE_CONNECT_API_KEY_ID}.p8"
printf '%s\n' "$APP_STORE_CONNECT_API_KEY_P8" > "$path"
chmod 600 "$path"
echo "$path"
