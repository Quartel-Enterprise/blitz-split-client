#!/usr/bin/env bash
#
# Check the set. Run by .github/workflows/app-store-screenshots.yml.
#
set -eo pipefail

options=()
if [ -n "$DEVICES" ]; then options+=(--devices "$DEVICES" --no-left-out); fi
if [ "$IOS" = true ]; then options+=(--ios fastlane/screenshots); fi
if [ "$MACOS" = true ]; then options+=(--mac fastlane/screenshots-mac); fi
if [ "$ALLOW_MISSING_DEVICES" = true ]; then options+=(--allow-missing-devices); fi
said="$RUNNER_TEMP/check.txt"
if ! node scripts/store-screenshots/local-set.mjs check "${options[@]}" > "$said" 2>&1; then
  cat "$said"
  {
    echo "## The set \`$SET\` was refused"
    echo
    echo '```'
    cat "$said"
    echo '```'
  } >> "$GITHUB_STEP_SUMMARY"
  echo "::error::The set $SET was refused: the run's summary says what is wrong with it. Nothing was sent."
  exit 1
fi
cat "$said"
version="the editable version"
if [ -n "$APP_VERSION" ]; then version="version $APP_VERSION"; fi
{
  echo "## The set \`$SET\`"
  echo
  echo "Going on $version of each platform, one locale per job:"
  echo
  echo '```'
  cat "$said"
  echo '```'
} >> "$GITHUB_STEP_SUMMARY"
