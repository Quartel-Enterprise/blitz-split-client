#!/bin/bash

# Captures the Apple Watch of the README's image (scripts/readme-hero/generate.sh): builds the watch app's debug build for
# a watchOS simulator, launches it with -StoreScreenshotsLanguage, which makes it hold the sample trip instead of an
# account (BlitzSplitWatch.startForStoreScreenshots), and photographs Balances. Writes
#   <output folder>/watch-dark.png   (the watch is dark only; hero.html uses it in both themes)
#
#   ./scripts/readme-hero/capture-watch.sh <output folder>
#
# The simulator is made once, named "Store screenshots watch": an Apple Watch Series 12 (46mm).

set -eo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUTPUT="${1:?Usage: capture-watch.sh <output folder>}"
DERIVED_DATA="$ROOT/build/readme-hero-watch/DerivedData"
NAME="${READMEHERO_WATCH_SIMULATOR:-Store screenshots watch}"
BUNDLE_ID="com.quare.blitzsplit.watchkitapp"
HEAVY="$ROOT/scripts/heavy-check.sh"

udid="$(xcrun simctl list devices available | grep -F "    $NAME (" | head -1 | sed -E 's/.*\(([0-9A-F-]{36})\).*/\1/' || true)"
if [ -z "$udid" ]; then
  runtime="$(xcrun simctl list runtimes available | grep -i watchos | tail -1 | sed -E 's/.* - (com\.apple\.CoreSimulator\.SimRuntime\.[^ ]+)$/\1/')"
  [ -n "$runtime" ] || { echo "No watchOS runtime installed: add one in Xcode > Settings > Components" >&2; exit 1; }
  udid="$(xcrun simctl create "$NAME" "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-Series-12-46mm" "$runtime")"
fi

# Why: the Xcode project is git-ignored and may predate project.yml
"$ROOT/iosApp/generate-project.sh" > /dev/null
# Why: a failed build must not leave the simulator running
trap 'xcrun simctl shutdown "$udid" 2> /dev/null || true' EXIT
xcrun simctl bootstatus "$udid" -b > /dev/null
"$HEAVY" xcodebuild -project "$ROOT/iosApp/BoraRachar.xcodeproj" -scheme BoraRachar-watchOS -configuration Debug \
  -destination "platform=watchOS Simulator,id=$udid" -derivedDataPath "$DERIVED_DATA" -quiet build
app="$(find "$DERIVED_DATA/Build/Products" -maxdepth 2 -name 'BoraRachar.app' -path '*watchsimulator*' | head -1)"
[ -n "$app" ] || { echo "No watch app built in $DERIVED_DATA" >&2; exit 1; }

# Why: a prompt an earlier run left pending belongs to the installed app; a fresh install has none
xcrun simctl terminate "$udid" "$BUNDLE_ID" 2> /dev/null || true
xcrun simctl uninstall "$udid" "$BUNDLE_ID" 2> /dev/null || true
xcrun simctl install "$udid" "$app"
xcrun simctl launch "$udid" "$BUNDLE_ID" -StoreScreenshotsLanguage en -AppleLanguages "(en)" -AppleLocale en_US > /dev/null
# Why: the trip is seeded before the first screen, but the balances take a moment to compute and the list to settle
sleep 8
mkdir -p "$OUTPUT"
xcrun simctl io "$udid" screenshot "$OUTPUT/watch-dark.png"
xcrun simctl shutdown "$udid" 2> /dev/null || true
echo "Wrote $OUTPUT/watch-dark.png"
