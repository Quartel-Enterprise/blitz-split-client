#!/bin/bash

# Captures the Mac App Store screenshots' windows (docs/store-listing-screenshots.md): builds the
# BoraRachar-macOS-StoreScreenshots UI tests and runs them on this Mac, which write
#   build/outputs/apple-screenshot-captures/mac/<locale>/<screen>.png
# for the Robolectric generators to frame. The test drives the real app on this Mac's screen for a few minutes: leave
# the keyboard and mouse alone until it ends. STORE_SCREENSHOTS_LOCALES and STORE_SCREENSHOTS_SCREENS narrow it as
# in capture-ios.sh.

set -eo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUTPUT="${STORE_SCREENSHOTS_OUTPUT:-$ROOT/build/outputs/apple-screenshot-captures}"
DERIVED_DATA="$ROOT/build/apple-screenshots/DerivedData-mac"

"$ROOT/iosApp/generate-project.sh"

echo "Building the Mac store screenshots' UI tests"
xcodebuild build-for-testing \
  -project "$ROOT/iosApp/BoraRachar.xcodeproj" \
  -scheme BoraRachar-macOS-StoreScreenshots \
  -destination "platform=macOS,arch=arm64" \
  -derivedDataPath "$DERIVED_DATA" \
  -quiet

rm -rf "${OUTPUT:?}/mac"
TEST_RUNNER_STORE_SCREENSHOTS_OUTPUT="$OUTPUT" \
  TEST_RUNNER_STORE_SCREENSHOTS_SLOT="mac" \
  TEST_RUNNER_STORE_SCREENSHOTS_ASSETS="$ROOT/scripts/store-screenshots" \
  TEST_RUNNER_STORE_SCREENSHOTS_LOCALES="$STORE_SCREENSHOTS_LOCALES" \
  TEST_RUNNER_STORE_SCREENSHOTS_SCREENS="$STORE_SCREENSHOTS_SCREENS" \
  xcodebuild test-without-building \
  -project "$ROOT/iosApp/BoraRachar.xcodeproj" \
  -scheme BoraRachar-macOS-StoreScreenshots \
  -destination "platform=macOS,arch=arm64" \
  -derivedDataPath "$DERIVED_DATA" \
  -quiet

echo "Captured into $OUTPUT/mac"
