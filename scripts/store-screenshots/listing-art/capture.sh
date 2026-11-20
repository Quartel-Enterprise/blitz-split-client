#!/bin/bash

# Captures every screen the store listing's art shows (docs/store-listing-screenshots.md), one device at a time, each
# behind other worktrees' builds (scripts/heavy-check.sh):
#
#   ./scripts/store-screenshots/listing-art/capture.sh                          # every device and language
#   ART_SLOTS="iphone67 ipad13" ART_LOCALES=en-US,pt-BR ./scripts/store-screenshots/listing-art/capture.sh
#
# The screens of screens.tsv the art uses, then the two of art-screens.tsv: "Dividir" in every language, and "Pagar
# Carla" in Brazil's alone, the only listing whose art shows Pix. The open iPhone Duo (iphoneduoopen) is left out of
# the default: only Device Hub opens it, so capture-ios.sh waits for someone to press Open.

set -eo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
HEAVY="$ROOT/scripts/heavy-check.sh"
CAPTURE="$ROOT/scripts/store-screenshots/capture-ios.sh"
SLOTS="${ART_SLOTS:-iphone67 iphone63 ipad13 iphoneduo}"
LOCALES="${ART_LOCALES:-}"

for slot in $SLOTS; do
  screens=group,items,receipt,balances,invite,statistics,home,payers,light
  # The art takes the group, Balances and Statistics of the Duo from its open display (capture-duo-open.py).
  if [ "$slot" = iphoneduo ]; then screens=items,receipt,invite,home,payers,light; fi
  STORE_SCREENSHOTS_SLOTS="$slot" STORE_SCREENSHOTS_LOCALES="$LOCALES" STORE_SCREENSHOTS_SCREENS="$screens" \
    "$HEAVY" "$CAPTURE"
  STORE_SCREENSHOTS_SLOTS="$slot" STORE_SCREENSHOTS_LOCALES="$LOCALES" \
    STORE_SCREENSHOTS_TABLE=art-screens STORE_SCREENSHOTS_SCREENS=splitmodes \
    "$HEAVY" "$CAPTURE"
  if [ -z "$LOCALES" ] || [[ ",$LOCALES," == *",pt-BR,"* ]]; then
    STORE_SCREENSHOTS_SLOTS="$slot" STORE_SCREENSHOTS_LOCALES=pt-BR \
      STORE_SCREENSHOTS_TABLE=art-screens STORE_SCREENSHOTS_SCREENS=pay \
      "$HEAVY" "$CAPTURE"
  fi
done
