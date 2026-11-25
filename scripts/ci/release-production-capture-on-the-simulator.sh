#!/usr/bin/env bash
#
# Capture on the simulator. Run by .github/workflows/release-production.yml.
#
set -eo pipefail

# The art's screens: the nine of screens.tsv, then "Dividir" and, in Brazil's listing, "Pagar Carla".
# The iPhone Duo's two displays are captured as the listing's own list says (capture-ios.sh), the art
# taking the group, Balances and Statistics from the open one.
if [ "$STORE_SCREENSHOTS_SLOTS" = iphoneduoopen ]; then
  STORE_SCREENSHOTS_SCREENS=group,balances,statistics ./scripts/store-screenshots/capture-ios.sh
else
  ART_SLOTS="$STORE_SCREENSHOTS_SLOTS" ./scripts/store-screenshots/listing-art/capture.sh
fi
