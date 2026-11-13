#!/bin/bash
# Relaunch the debug iOS app on a seeded screen (the App Store screenshots' sample trip) and screenshot it.
#
#   ios-seeded.sh <udid> <screen> <light|dark> <output.png> [display]
#
# <screen> is a code of StoreScreenshotsScreen: home, group, balances, statistics, activity, payers, items, invite,
# receipt. The seed needs a debug build already installed (docs/store-listing-screenshots.md). The iPhone Duo has two
# displays, so it needs [display]: 1 is the outer one (closed), 3 the inner one, which is black until the simulator is
# unfolded by hand in Device Hub (no tool can unfold it). Other simulators take no [display].
set -eo pipefail
UDID="$1"; SCREEN="$2"; THEME="$3"; OUTPUT="$4"; DISPLAY_ID="$5"
xcrun simctl terminate "$UDID" com.quare.blitzsplit >/dev/null 2>&1 || true
xcrun simctl ui "$UDID" appearance "$THEME"
xcrun simctl launch "$UDID" com.quare.blitzsplit -StoreScreenshotsLanguage "${SEED_LANGUAGE:-en}" \
  -StoreScreenshotsTheme "$THEME" -StoreScreenshotsScreen "$SCREEN" >/dev/null
sleep "${SEED_WAIT:-5}"
if [ -n "$DISPLAY_ID" ]; then
  xcrun simctl io "$UDID" screenshot "--display=$DISPLAY_ID" "$OUTPUT" >/dev/null
else
  xcrun simctl io "$UDID" screenshot "$OUTPUT" >/dev/null
fi
