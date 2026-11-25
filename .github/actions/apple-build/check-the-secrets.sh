#!/usr/bin/env bash
#
# Check the secrets. Run by .github/actions/apple-build/action.yml.
#
set -eo pipefail

./scripts/release/require-env.sh GOOGLE_SERVICE_INFO_PLIST APP_STORE_CONNECT_API_KEY_ID APP_STORE_CONNECT_API_ISSUER_ID APP_STORE_CONNECT_API_KEY_P8 MATCH_PASSWORD MATCH_SSH_PRIVATE_KEY
# The Crashlytics dSYM phase of the archive reads GOOGLE_APP_ID from the plist and fails the build ten
# minutes in when it is missing (a google-services.json, or a value that is not the base64 of the plist):
# fail here, before the caches and the project generation.
plist="$RUNNER_TEMP/GoogleService-Info.plist"
if ! echo "$GOOGLE_SERVICE_INFO_PLIST" | base64 --decode > "$plist" 2> /dev/null \
  || ! plutil -extract GOOGLE_APP_ID raw "$plist" > /dev/null 2>&1; then
  echo "::error::The GOOGLE_SERVICE_INFO_PLIST secret has no GOOGLE_APP_ID: set it to the base64 of the iOS GoogleService-Info.plist (base64 -i GoogleService-Info.plist)."
  exit 1
fi
