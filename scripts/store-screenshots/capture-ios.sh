#!/bin/bash

# Captures the App Store screenshots' screens on the iOS simulator (docs/store-listing-screenshots.md): builds the
# BoraRachar-iOS-StoreScreenshots UI tests once, then runs them on one simulator per App Store slot, which write
#   build/outputs/apple-screenshot-captures/<slot>/<locale>/<screen>.png
# for the Robolectric generators of :android:store_screenshots to frame. Gradle's captureAppleScreenshots runs it.
#
#   ./scripts/store-screenshots/capture-ios.sh                  # every slot, language and screen
#   STORE_SCREENSHOTS_SLOTS=iphone63 STORE_SCREENSHOTS_LOCALES=en-US,ja STORE_SCREENSHOTS_SCREENS=group,items \
#     ./scripts/store-screenshots/capture-ios.sh                # a quick look at a few
#
# Each slot's simulator is the one whose screen matches what the frame draws (AppleScreenshotSlot): Dynamic Island
# (medium), the size App Store Connect requires, from an iPhone 17 Pro (1206x2622); Dynamic Island (large) from an
# iPhone 16 Plus (1290x2796); the 13" iPad from an iPad Air 13" (2048x2732), switched to full-screen apps by the test;
# the iPhone Duo, on the runtime that has it, which boots closed: its outer display (1398x2034). No tool can unfold it,
# so the inner display isn't captured. The simulators are made once, named "Store screenshots <slot>", and their
# status bar shows Apple's 9:41.

set -eo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUTPUT="${STORE_SCREENSHOTS_OUTPUT:-$ROOT/build/outputs/apple-screenshot-captures}"
DERIVED_DATA="$ROOT/build/apple-screenshots/DerivedData"
SLOTS="${STORE_SCREENSHOTS_SLOTS:-iphone63 iphone67 ipad13 iphoneduo}"

device_type() {
  case "$1" in
    iphone63) echo "com.apple.CoreSimulator.SimDeviceType.iPhone-17-Pro" ;;
    iphone67) echo "com.apple.CoreSimulator.SimDeviceType.iPhone-16-Plus" ;;
    ipad13) echo "com.apple.CoreSimulator.SimDeviceType.iPad-Air-13-inch-M3" ;;
    iphoneduo) echo "com.apple.CoreSimulator.SimDeviceType.iPhone-Duo" ;;
    *) echo "unknown slot $1" >&2; exit 2 ;;
  esac
}

# The newest iOS runtime that can run the slot's device: the Duo has a runtime of its own.
runtime_for() {
  xcrun simctl list runtimes iOS -j | /usr/bin/python3 -I -c "
import json, sys
device_type = sys.argv[1]
runtimes = [r for r in json.load(sys.stdin)['runtimes'] if r['isAvailable']]
fits = [r['identifier'] for r in runtimes if any(t['identifier'] == device_type for t in r.get('supportedDeviceTypes', []))]
print(fits[-1] if fits else '')
" "$(device_type "$1")"
}

simulator_for() {
  local name="Store screenshots $1"
  local runtime
  runtime="$(runtime_for "$1")"
  if [ -z "$runtime" ]; then
    echo "No installed iOS runtime runs $(device_type "$1")" >&2
    exit 1
  fi
  local udid
  udid="$(xcrun simctl list devices -j | /usr/bin/python3 -I -c "
import json, sys
name, runtime = sys.argv[1], sys.argv[2]
devices = json.load(sys.stdin)['devices'].get(runtime, [])
print(next((d['udid'] for d in devices if d['name'] == name and d['isAvailable']), ''))
" "$name" "$runtime")"
  if [ -z "$udid" ]; then
    udid="$(xcrun simctl create "$name" "$(device_type "$1")" "$runtime")"
  fi
  echo "$udid"
}

"$ROOT/iosApp/generate-project.sh"

echo "Building the store screenshots' UI tests"
xcodebuild build-for-testing \
  -project "$ROOT/iosApp/BoraRachar.xcodeproj" \
  -scheme BoraRachar-iOS-StoreScreenshots \
  -destination "generic/platform=iOS Simulator" \
  -derivedDataPath "$DERIVED_DATA" \
  ARCHS=arm64 \
  -quiet

for slot in $SLOTS; do
  udid="$(simulator_for "$slot")"
  echo "Capturing $slot on $udid"
  xcrun simctl boot "$udid" 2> /dev/null || true
  xcrun simctl bootstatus "$udid" -b > /dev/null
  xcrun simctl status_bar "$udid" override --time "9:41" --dataNetwork wifi --wifiMode active --wifiBars 3 \
    --cellularMode active --cellularBars 4 --batteryState discharging --batteryLevel 100
  # Only the locales asked for are captured again: the others keep their PNGs.
  if [ -z "$STORE_SCREENSHOTS_LOCALES" ]; then rm -rf "${OUTPUT:?}/$slot"; fi
  TEST_RUNNER_STORE_SCREENSHOTS_OUTPUT="$OUTPUT" \
    TEST_RUNNER_STORE_SCREENSHOTS_SLOT="$slot" \
    TEST_RUNNER_STORE_SCREENSHOTS_ASSETS="$ROOT/scripts/store-screenshots" \
    TEST_RUNNER_STORE_SCREENSHOTS_LOCALES="$STORE_SCREENSHOTS_LOCALES" \
    TEST_RUNNER_STORE_SCREENSHOTS_SCREENS="$STORE_SCREENSHOTS_SCREENS" \
    xcodebuild test-without-building \
    -project "$ROOT/iosApp/BoraRachar.xcodeproj" \
    -scheme BoraRachar-iOS-StoreScreenshots \
    -destination "id=$udid" \
    -derivedDataPath "$DERIVED_DATA" \
    -quiet
  xcrun simctl shutdown "$udid" || true
done

echo "Captured into $OUTPUT"
