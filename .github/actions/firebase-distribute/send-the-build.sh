#!/usr/bin/env bash
#
# Send the build. Run by .github/actions/firebase-distribute/action.yml.
#
set -eo pipefail

trap 'rm -f "$RUNNER_TEMP/firebase-service-account.json"' EXIT
printf '%s' "$FIREBASE_SERVICE_ACCOUNT_JSON" > "$RUNNER_TEMP/firebase-service-account.json"
export GOOGLE_APPLICATION_CREDENTIALS="$RUNNER_TEMP/firebase-service-account.json"
# Unquoted on purpose: the glob resolves to the one file the job downloaded
# shellcheck disable=SC2086
npx --yes "firebase-tools@${FIREBASE_TOOLS_VERSION}" appdistribution:distribute $FILE \
  --project "$PROJECT" \
  --app "$APP_ID" \
  --groups "$GROUP" \
  --release-notes "$RELEASE_NOTES"
