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
# The device the simulator is made as, and the name of the file written: the store listing's watch screenshots
# (scripts/store-screenshots/watch/generate.sh) takes the Ultra 3, whose screen is the 422x514 App Store Connect asks for.
DEVICE_TYPE="${READMEHERO_WATCH_DEVICE_TYPE:-Apple-Watch-Series-12-46mm}"
SHOT_NAME="${READMEHERO_WATCH_SHOT:-watch-dark.png}"
# The screen the app walks to from Balances (WatchStoreScreenshots in BoraRacharWatchApp.swift): balances, group,
# confirm or recorded.
SCREEN="${READMEHERO_WATCH_SCREEN:-balances}"
# A folder with faces/ and groups/ (the image bank's photos, as the iPhone's captures hand them): the people have faces
# and the groups their photos. Without it they are initials.
ASSETS="${READMEHERO_WATCH_ASSETS:-}"
# The language the app runs in (its code, the language iOS is told and the locale), English unless the store listing's
# generate.sh asks for one of locales.tsv's.
APP_LANGUAGE="${READMEHERO_WATCH_LANGUAGE:-en}"
APPLE_LANGUAGE="${READMEHERO_WATCH_APPLE_LANGUAGE:-en}"
APPLE_LOCALE="${READMEHERO_WATCH_APPLE_LOCALE:-en_US}"
# One build and one simulator left booted for many captures (generate.sh's loop): READMEHERO_WATCH_REUSE=1 once the
# first has built.
REUSE="${READMEHERO_WATCH_REUSE:-}"
assets_arguments=()
if [ -n "$ASSETS" ]; then assets_arguments=(-StoreScreenshotsAssets "$ASSETS"); fi
BUNDLE_ID="com.quare.blitzsplit.watchkitapp"
HEAVY="$ROOT/scripts/heavy-check.sh"

udid="$(xcrun simctl list devices available | grep -F "    $NAME (" | head -1 | sed -E 's/.*\(([0-9A-F-]{36})\).*/\1/' || true)"
if [ -z "$udid" ]; then
  runtime="$(xcrun simctl list runtimes available | grep -i watchos | tail -1 | sed -E 's/.* - (com\.apple\.CoreSimulator\.SimRuntime\.[^ ]+)$/\1/')"
  [ -n "$runtime" ] || { echo "No watchOS runtime installed: add one in Xcode > Settings > Components" >&2; exit 1; }
  udid="$(xcrun simctl create "$NAME" "com.apple.CoreSimulator.SimDeviceType.$DEVICE_TYPE" "$runtime")"
fi

# Why: a failed build must not leave the simulator running, unless the caller shuts it down after its last capture
if [ -z "$REUSE" ]; then
  trap 'xcrun simctl shutdown "$udid" 2> /dev/null || true' EXIT
fi
xcrun simctl bootstatus "$udid" -b > /dev/null
if [ "$REUSE" != 2 ]; then
  # Why: the Xcode project is git-ignored and may predate project.yml
  "$ROOT/iosApp/generate-project.sh" > /dev/null
  "$HEAVY" xcodebuild -project "$ROOT/iosApp/BoraRachar.xcodeproj" -scheme BoraRachar-watchOS -configuration Debug \
    -destination "platform=watchOS Simulator,id=$udid" -derivedDataPath "$DERIVED_DATA" -quiet build
fi
app="$(find "$DERIVED_DATA/Build/Products" -maxdepth 2 -name 'BoraRachar.app' -path '*watchsimulator*' | head -1)"
[ -n "$app" ] || { echo "No watch app built in $DERIVED_DATA" >&2; exit 1; }

# Why: a prompt an earlier run left pending belongs to the installed app; a fresh install has none
xcrun simctl terminate "$udid" "$BUNDLE_ID" 2> /dev/null || true
xcrun simctl uninstall "$udid" "$BUNDLE_ID" 2> /dev/null || true
xcrun simctl install "$udid" "$app"
xcrun simctl launch "$udid" "$BUNDLE_ID" -StoreScreenshotsLanguage "$APP_LANGUAGE" -StoreScreenshotsScreen "$SCREEN" "${assets_arguments[@]}" -AppleLanguages "($APPLE_LANGUAGE)" -AppleLocale "$APPLE_LOCALE" > /dev/null
# Why: Kotlin starting, the seed and the balances take a few seconds, longer on a cold simulator: photograph until two
# shots two seconds apart are the same (nothing moving any more), for at most a minute, never after a fixed wait.
mkdir -p "$OUTPUT"
shot="$OUTPUT/$SHOT_NAME"
previous="$OUTPUT/.watch-previous.png"
rm -f "$previous"
sleep 4
for _ in $(seq 28); do
  xcrun simctl io "$udid" screenshot "$shot" > /dev/null 2>&1
  if [ -f "$previous" ] && cmp -s "$shot" "$previous"; then break; fi
  cp "$shot" "$previous"
  sleep 2
done
rm -f "$previous"
echo "Wrote $shot"
