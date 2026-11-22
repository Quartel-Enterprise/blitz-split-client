#!/bin/bash

# Draws the Wear OS watch's Google Play screenshots (docs/store-listing-screenshots.md#the-watches-screenshots): four
# screens in every listing language, 456 px square, by Robolectric in :wearApp (WearStoreScreenshots), with the image
# bank's photos on the people. They land in build/outputs/store-screenshots-wear/<locale>/, where stage-play.mjs reads
# them. Run before it, by the lane of fastlane/Fastfile, the Play screenshots' action and generate.sh.
#
#   ./scripts/store-screenshots/watch/draw-wear.sh
#   ART_LOCALES=en-US,ja ./scripts/store-screenshots/watch/draw-wear.sh
set -eo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
OUTPUT="$ROOT/build/outputs/store-screenshots-wear"
ASSETS="$ROOT/build/store-watch-assets"

"$ROOT/scripts/store-screenshots/watch/prepare-assets.sh" "$ASSETS"
rm -rf "$OUTPUT"
# Every language when ART_LOCALES is empty: the test knows them
STORE_WATCH_ASSETS="$ASSETS" "$ROOT/scripts/heavy-check.sh" "$ROOT/gradlew" -p "$ROOT" :wearApp:testDebugUnitTest \
  -PstoreWatch "-PstoreWatchLocales=${ART_LOCALES:-}" --rerun -q
