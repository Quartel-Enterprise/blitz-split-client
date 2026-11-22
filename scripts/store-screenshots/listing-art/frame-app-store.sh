#!/bin/bash

# Lays out the App Store's listing art from the captures in build/outputs/apple-screenshot-captures (capture.sh and
# capture-mac.sh write them) and stages it where deliver uploads from, fastlane/screenshots/ and fastlane/screenshots-mac/
# (docs/store-listing-screenshots.md). Only the devices that were captured are drawn: the iPhone's two sizes and the
# iPad's are required, the iPhone Duo's needs both its displays and the Mac's its window. A release's runner that
# captured fewer than that leaves the rest out.
#
#   ./scripts/store-screenshots/listing-art/frame-app-store.sh
#   ART_LOCALES=en-US,pt-BR ./scripts/store-screenshots/listing-art/frame-app-store.sh
#
# Needs Chrome (scripts/readme-hero/chrome.mjs drives it), Node, and Python with Pillow (pip3 install pillow).

set -eo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
CAPTURES="$ROOT/build/outputs/apple-screenshot-captures"
ART="$ROOT/scripts/store-screenshots/listing-art"

# The listing tags the art is drawn in: ART_LOCALES's, or every row of locales.tsv.
if [ -n "$ART_LOCALES" ]; then
  IFS=, read -r -a tags <<< "$ART_LOCALES"
else
  tags=($(grep -v '^#' "$ROOT/scripts/store-screenshots/locales.tsv" | cut -f1))
fi

captured() { [ -d "$CAPTURES/$1" ]; }
# A slot with a folder for every language. capture-ios.sh and capture-mac.sh leave the folder of a slot whose language
# failed in place, and compose.mjs stops at the missing one, so an optional device short of a language is left out.
whole() {
  captured "$1" || return 1
  for tag in "${tags[@]}"; do
    [ -d "$CAPTURES/$1/$tag" ] || { echo "Warning: $1 has no captures of $tag: its art is left out" >&2; return 1; }
  done
}

for slot in iphone63 iphone67 ipad13; do
  captured "$slot" || { echo "Error: no captures of $slot in $CAPTURES: run listing-art/capture.sh first" >&2; exit 1; }
done
# One run of compose.mjs for the devices the listing can't go without, so a capture missing there stops everything, and
# one for each optional device: a Duo or a Mac whose art can't be drawn (a screen missing from a capture, say) is left
# out with a warning and takes nothing else with it.
draw() { ART_DEVICES="$1" node "$ART/compose.mjs"; }

# What the tickets write on them, from the sample trip: drawn again every time, since a changed trip or language would
# leave stale names and amounts, or a tag missing from the file.
"$ROOT/scripts/heavy-check.sh" "$ROOT/gradlew" -p "$ROOT" :android:store_screenshots:testDebugUnitTest -PcreativeAssets --rerun -q

devices=(iphone_6_7 iphone_6_3 ipad_13 ipad_11)
draw "$(IFS=,; echo "${devices[*]}")"
if whole iphoneduo && whole iphoneduoopen; then
  if draw iphone_duo; then devices+=(iphone_duo); else echo "Warning: the iPhone Duo's art could not be drawn: it is left out" >&2; fi
fi
if whole mac; then
  if draw mac; then devices+=(mac); else echo "Warning: the Mac's art could not be drawn: it is left out" >&2; fi
fi
ART_DEVICES="$(IFS=,; echo "${devices[*]}")" python3 "$ART/stage-app-store.py"
