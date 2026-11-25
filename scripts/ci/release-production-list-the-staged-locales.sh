#!/usr/bin/env bash
#
# List the staged locales. Run by .github/workflows/release-production.yml.
#
set -eo pipefail

ios_locales="$(./scripts/release/staged-screenshot-locales.sh fastlane/screenshots)"
# The upload refuses a set without the iPhone Duo (the Fastfile), which would empty its shelf: rather than a
# failed job per locale, the iPhone's and the iPad's are left as they are
if [ -n "$ios_locales" ] && ! compgen -G "fastlane/screenshots/*/*_iphone_duo*.png" > /dev/null; then
  echo "::warning::No iPhone Duo screenshots staged: the iPhone's and the iPad's App Store screenshots are left as they are (screenshots_iphone_duo, or a set sent from a Mac)"
  ios_locales=""
fi
mac_locales="$(./scripts/release/staged-screenshot-locales.sh fastlane/screenshots-mac)"
{
  echo "ios_locales=$ios_locales"
  echo "mac_locales=$mac_locales"
} >> "$GITHUB_OUTPUT"
