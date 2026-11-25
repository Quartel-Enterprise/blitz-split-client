#!/usr/bin/env bash
#
# Write the Google Play key. Run by .github/actions/play-store-lane/action.yml.
#
set -eo pipefail

./scripts/release/require-env.sh PLAY_STORE_SERVICE_ACCOUNT_JSON
printf '%s' "$PLAY_STORE_SERVICE_ACCOUNT_JSON" > "$RUNNER_TEMP/play-service-account.json"
if ! jq -e '.client_email' "$RUNNER_TEMP/play-service-account.json" > /dev/null 2>&1; then
  echo "::error::The PLAY_STORE_SERVICE_ACCOUNT_JSON secret is not a service account key (JSON). See docs/release-process.md#secrets."
  exit 1
fi
