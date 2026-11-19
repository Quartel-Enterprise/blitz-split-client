#!/bin/bash

# Draws the App Store's creative assets, the product page's header and the search result, in every listing language
# (docs/store-listing-screenshots.md). They show real screens of the app, so run this again after a change to one of
# the four screens they show, the sample trip or copy.tsv.
#
#   ./scripts/store-screenshots/creative-assets/generate.sh                # capture the screens again, then draw
#   CREATIVE_ASSETS_CAPTURE=none ./scripts/store-screenshots/creative-assets/generate.sh   # draw from the captures there
#   CREATIVE_ASSETS_LOCALES=en-US,ja ./scripts/store-screenshots/creative-assets/generate.sh   # only these languages
#
# The images land in store_listings/creative-assets/<locale>/{header,search-results}.png. Nothing reaches a store.

set -eo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
HEAVY="$ROOT/scripts/heavy-check.sh"

if [ "${CREATIVE_ASSETS_CAPTURE:-all}" != none ]; then
  STORE_SCREENSHOTS_SLOTS=iphone67 \
    STORE_SCREENSHOTS_LOCALES="${CREATIVE_ASSETS_LOCALES:-}" \
    STORE_SCREENSHOTS_SCREENS=group,items,statistics,home \
    "$HEAVY" "$ROOT/scripts/store-screenshots/capture-ios.sh"
fi

"$HEAVY" "$ROOT/gradlew" -p "$ROOT" :android:store_screenshots:testDebugUnitTest -PcreativeAssets --rerun -q
node "$ROOT/scripts/store-screenshots/creative-assets/compose.mjs"
