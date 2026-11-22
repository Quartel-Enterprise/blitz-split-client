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
devices=(iphone_6_7 iphone_6_3 ipad_13 ipad_11)
if whole iphoneduo && whole iphoneduoopen; then devices+=(iphone_duo); fi
if whole mac; then devices+=(mac); fi
ART_DEVICES="$(IFS=,; echo "${devices[*]}")"
export ART_DEVICES

# What the tickets write on them, the same for every device: drawn once.
if [ ! -f "$ROOT/build/outputs/app-store-creative-asset-facts/facts.json" ]; then
  "$ROOT/scripts/heavy-check.sh" "$ROOT/gradlew" -p "$ROOT" :android:store_screenshots:testDebugUnitTest -PcreativeAssets --rerun -q
fi
node "$ART/compose.mjs"
python3 "$ART/stage-app-store.py"
