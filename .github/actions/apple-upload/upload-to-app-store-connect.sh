#!/usr/bin/env bash
#
# Upload to App Store Connect. Run by .github/actions/apple-upload/action.yml.
#
set -eo pipefail

./scripts/release/require-env.sh APP_STORE_CONNECT_API_KEY_ID APP_STORE_CONNECT_API_ISSUER_ID APP_STORE_CONNECT_API_KEY_P8

# altool takes one --file: an export with none, or two, is an error worth saying plainly.
shopt -s nullglob
# shellcheck disable=SC2206
files=( $UPLOAD_FILE )
if [ "${#files[@]}" -ne 1 ]; then
  echo "::error::Expected exactly one file matching '$UPLOAD_FILE', found ${#files[@]}."
  exit 1
fi

key_path="$(./scripts/release/write-asc-key.sh)"
trap 'rm -f "$key_path"' EXIT
xcrun altool --upload-app --type "$UPLOAD_TYPE" --file "${files[0]}" \
  --apiKey "$APP_STORE_CONNECT_API_KEY_ID" --apiIssuer "$APP_STORE_CONNECT_API_ISSUER_ID"
