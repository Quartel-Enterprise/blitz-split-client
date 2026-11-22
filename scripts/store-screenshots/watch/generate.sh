#!/bin/bash

# The watches' store screenshots (docs/store-listing-screenshots.md#the-watches-screenshots): four screens of each
# watch app (balances, group, confirm, recorded), in every listing language of locales.tsv, at the size its store
# takes, written to store_listings/watch/<store>/<locale>/ to look at. Google Play's are also what stage-play.mjs
# stages (draw-wear.sh draws those alone); the Apple Watch's are what stage-app-store.py stages with the iPhone's.
#
#   ./scripts/store-screenshots/watch/generate.sh
#   WATCH_STORES=apple ./scripts/store-screenshots/watch/generate.sh     # or wear
#   ART_LOCALES=en-US,ja ./scripts/store-screenshots/watch/generate.sh  # a few languages
#
# - Apple Watch: App Store Connect takes one watch size for every language, 422x514 (Ultra 3) here, captured as the
#   simulator draws it. The debug app walks to each screen by acting as a person would (WatchStoreScreenshots). About
#   half a minute a screen, two minutes a language: an hour or more for every language.
# - Wear OS: Google Play takes square images of at least 384 px, with no watch frame, no text and no background
#   around them. Robolectric draws the app's own screens (WearStoreScreenshots), 456 px square.
set -eo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
OUT="$ROOT/store_listings/watch"
WANTED="${WATCH_STORES:-apple,wear}"
HEAVY="$ROOT/scripts/heavy-check.sh"
LOCALES_TSV="$ROOT/scripts/store-screenshots/locales.tsv"
SCREENS=(balances group confirm recorded)

# The listing tags to draw: ART_LOCALES's, or every row of locales.tsv.
if [ -n "$ART_LOCALES" ]; then
  IFS=, read -r -a tags <<< "$ART_LOCALES"
else
  tags=($(grep -v '^#' "$LOCALES_TSV" | cut -f1))
fi

ASSETS="$ROOT/build/store-watch-assets"
"$ROOT/scripts/store-screenshots/watch/prepare-assets.sh" "$ASSETS"

check_size() {
  local image="$1" expected="$2" size
  size="$(sips -g pixelWidth -g pixelHeight "$image" | awk '/pixel/ {printf "%s ", $2}')"
  [ "$size" = "$expected " ] || { echo "$image is $size, the store takes $expected" >&2; exit 1; }
}

if [[ ",$WANTED," == *",apple,"* ]]; then
  simulator="Store screenshots watch ultra 3"
  reuse=""
  failed=()
  # capture-watch.sh leaves the simulator booted for the next capture: whatever stops this run shuts it down
  shut_down_simulator() {
    xcrun simctl shutdown "$(xcrun simctl list devices available | grep -F "    $simulator (" | head -1 | sed -E 's/.*\(([0-9A-F-]{36})\).*/\1/')" 2> /dev/null || true
  }
  trap shut_down_simulator EXIT
  for tag in "${tags[@]}"; do
    row="$(grep -v '^#' "$LOCALES_TSV" | awk -F'\t' -v tag="$tag" '$1 == tag')"
    [ -n "$row" ] || { echo "locales.tsv has no $tag" >&2; exit 1; }
    IFS=$'\t' read -r _ language apple_language apple_locale <<< "$row"
    directory="$OUT/app-store-ultra-3/$tag"
    mkdir -p "$directory"
    rm -f "$directory"/*.png
    count=0
    for screen in "${SCREENS[@]}"; do
      number=$((++count))
      file="0${number}_${screen}.png"
      # The first capture builds the app and the others reuse the build and the booted simulator
      if READMEHERO_WATCH_SIMULATOR="$simulator" READMEHERO_WATCH_DEVICE_TYPE="Apple-Watch-Ultra-3-49mm" \
        READMEHERO_WATCH_ASSETS="$ASSETS" READMEHERO_WATCH_SCREEN="$screen" READMEHERO_WATCH_SHOT="$file" \
        READMEHERO_WATCH_LANGUAGE="$language" READMEHERO_WATCH_APPLE_LANGUAGE="$apple_language" \
        READMEHERO_WATCH_APPLE_LOCALE="$apple_locale" READMEHERO_WATCH_REUSE="${reuse:-1}" \
        "$ROOT/scripts/readme-hero/capture-watch.sh" "$directory"; then
        check_size "$directory/$file" "422 514"
      else
        echo "Failed: $tag $screen" >&2
        failed+=("$tag/$screen")
      fi
      # Only a capture that built the app lets the others reuse it: a failed build would leave them an old app
      if [ -z "$reuse" ] && [ "${#failed[@]}" -gt 0 ]; then
        echo "The first capture failed, so the others would photograph an old build: stopping" >&2
        exit 1
      fi
      reuse=2
    done
  done
  shut_down_simulator
  trap - EXIT
  if [ "${#failed[@]}" -gt 0 ]; then echo "Apple Watch captures that failed: ${failed[*]}" >&2; exit 1; fi
fi

if [[ ",$WANTED," == *",wear,"* ]]; then
  "$ROOT/scripts/store-screenshots/watch/draw-wear.sh"
  rm -rf "$OUT/play-wear"
  mkdir -p "$OUT/play-wear"
  cp -R "$ROOT"/build/outputs/store-screenshots-wear/. "$OUT/play-wear/"
  for image in "$OUT"/play-wear/*/*.png; do check_size "$image" "456 456"; done
fi
echo "Wrote $OUT"
