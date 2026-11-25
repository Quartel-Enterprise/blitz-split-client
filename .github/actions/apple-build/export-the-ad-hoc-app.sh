#!/usr/bin/env bash
#
# Export the ad hoc app. Run by .github/actions/apple-build/action.yml.
# Reads from the step's env: INPUTS_SCHEME, INPUTS_ADHOC_EXPORT_PATH.
#
set -eo pipefail

if [ ! -f "$RUNNER_TEMP/ExportOptions-adhoc.plist" ]; then
  echo "::warning::No ad hoc profile, so no Firebase App Distribution build."
  exit 0
fi
if ! xcodebuild -exportArchive -archivePath "$RUNNER_TEMP/${INPUTS_SCHEME}.xcarchive" \
  -exportPath "${INPUTS_ADHOC_EXPORT_PATH}" -exportOptionsPlist "$RUNNER_TEMP/ExportOptions-adhoc.plist"; then
  echo "::warning::The ad hoc export failed, so no Firebase App Distribution build."
fi
