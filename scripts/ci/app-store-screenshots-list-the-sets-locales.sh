#!/usr/bin/env bash
#
# List the set's locales. Run by .github/workflows/app-store-screenshots.yml.
#
set -eo pipefail

ios_locales="$(./scripts/release/staged-screenshot-locales.sh fastlane/screenshots)"
mac_locales="$(./scripts/release/staged-screenshot-locales.sh fastlane/screenshots-mac)"
if [ -n "$LOCALES" ]; then
  if [ -n "$ios_locales" ]; then ios_locales="$(node scripts/store-screenshots/local-set.mjs locales --staged "$ios_locales" --only "$LOCALES")"; fi
  if [ -n "$mac_locales" ]; then mac_locales="$(node scripts/store-screenshots/local-set.mjs locales --staged "$mac_locales" --only "$LOCALES")"; fi
  echo "Only **$LOCALES** of the set go up: the other locales' shelves are left as they are." >> "$GITHUB_STEP_SUMMARY"
fi
{
  echo "ios_locales=$ios_locales"
  echo "mac_locales=$mac_locales"
} >> "$GITHUB_OUTPUT"
