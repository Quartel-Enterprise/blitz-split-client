#!/usr/bin/env bash
#
# Sign with the team's distribution certificate. Run by .github/actions/apple-build/action.yml.
# Reads from the step's env: INPUTS_SCHEME.
#
set -eo pipefail

adhoc=()
if [ -n "$ADHOC_EXPORT_PATH" ]; then
  adhoc=(adhoc_export_options:"$RUNNER_TEMP/ExportOptions-adhoc.plist")
fi
fastlane apple_signing scheme:${INPUTS_SCHEME} export_options:"$RUNNER_TEMP/ExportOptions.plist" "${adhoc[@]}"
