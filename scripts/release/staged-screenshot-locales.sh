#!/usr/bin/env bash
#
# Prints the locales that have screenshots staged under a store's folder, as a JSON array (["de-DE","en-US"]): what
# the release's upload jobs are made of, one job per locale on the App Store and a few locales per job on Google
# Play, which play-screenshot-groups.mjs groups (release-production.yml). A locale is a folder right under
# the store's folder that holds a PNG at any depth, which fits the three layouts: fastlane/metadata/android
# (<locale>/images/<type>/), fastlane/screenshots and fastlane/screenshots-mac (<locale>/).
#
# Prints nothing when no locale is staged or the folder is missing: an empty array would fail the matrix, and the
# jobs skip on an empty answer.
#
# Usage: ./scripts/release/staged-screenshot-locales.sh <folder>
set -euo pipefail

folder="${1:?usage: staged-screenshot-locales.sh <folder>}"
if [ ! -d "$folder" ]; then
  exit 0
fi

locales="$(cd "$folder" && find . -mindepth 2 -type f -name '*.png' | cut -d/ -f2 | sort -u | jq -Rcn '[inputs]')"
if [ "$locales" != "[]" ]; then
  echo "$locales"
fi
