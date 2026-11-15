#!/bin/bash

# Captures the App Store screenshots' screens on the iOS simulator (docs/store-listing-screenshots.md): builds the
# BoraRachar-iOS-StoreScreenshots UI tests once, then runs them on one simulator per App Store slot, which write
#   build/outputs/apple-screenshot-captures/<slot>/<locale>/<screen>.png
# for the Robolectric generators of :android:store_screenshots to frame. `fastlane screenshots` runs it.
#
#   ./scripts/store-screenshots/capture-ios.sh                  # every slot, language and screen
#   STORE_SCREENSHOTS_SLOTS=iphone63 STORE_SCREENSHOTS_LOCALES=en-US,ja STORE_SCREENSHOTS_SCREENS=group,items \
#     ./scripts/store-screenshots/capture-ios.sh                # a quick look at a few
#
# Each slot's simulator is the one whose screen matches what the frame draws (AppleScreenshotSlot): Dynamic Island
# (medium), the size App Store Connect requires, from an iPhone 17 Pro (1206x2622); Dynamic Island (large) from an
# iPhone 16 Plus (1290x2796); the 13" iPad from an iPad Air 13" (2048x2732), switched to full-screen apps by the test;
# the iPhone Duo, on the runtime that has it, twice: closed, as it boots, its outer display (1398x2034), and open, its
# inner display lying down (2853x2007, which XCTest hands as 2852x2006). Only Device Hub can open it (open_duo below).
# The simulators are made once, named "Store screenshots <slot>", and their status bar shows Apple's 9:41.

set -eo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUTPUT="${STORE_SCREENSHOTS_OUTPUT:-$ROOT/build/outputs/apple-screenshot-captures}"
DERIVED_DATA="$ROOT/build/apple-screenshots/DerivedData"
SLOTS="${STORE_SCREENSHOTS_SLOTS:-iphone63 iphone67 ipad13 iphoneduo iphoneduoopen}"
DEVICE_HUB="com.apple.dt.Devices"
# How long the Duo may take to open, by the script's press or by a person's, counted once the script's own attempt at
# the press is over (which waits up to a minute for Device Hub's window and half a minute for its Open button).
OPEN_TIMEOUT="${STORE_SCREENSHOTS_OPEN_TIMEOUT:-120}"
# The Duo's inner display, as `simctl io enumerate` numbers it.
DUO_INNER_DISPLAY=3

device_type() {
  case "$1" in
    iphone63) echo "com.apple.CoreSimulator.SimDeviceType.iPhone-17-Pro" ;;
    iphone67) echo "com.apple.CoreSimulator.SimDeviceType.iPhone-16-Plus" ;;
    ipad13) echo "com.apple.CoreSimulator.SimDeviceType.iPad-Air-13-inch-M3" ;;
    iphoneduo | iphoneduoopen) echo "com.apple.CoreSimulator.SimDeviceType.iPhone-Duo" ;;
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

# Whether the Duo is open: its inner display, off and upright while closed, then draws the screen lying down. simctl
# writes it as it is seen, 2853x2007, with no rotation tag (XCTest's capture is the one that has it).
duo_is_open() {
  local probe width height
  probe="$(mktemp -d)"
  xcrun simctl io "$1" screenshot --display="$DUO_INNER_DISPLAY" "$probe/inner.png" > /dev/null 2>&1 || true
  width="$(sips -g pixelWidth "$probe/inner.png" 2> /dev/null | awk '/pixelWidth/ { print $2 }')"
  height="$(sips -g pixelHeight "$probe/inner.png" 2> /dev/null | awk '/pixelHeight/ { print $2 }')"
  rm -rf "$probe"
  [ "${width:-0}" -gt "${height:-0}" ]
}

# Opens the booted Duo. Its hinge only moves from Device Hub, whose "Open" button sends the simulator a HID event no
# command line tool sends, so the script selects the simulator there and presses the button through accessibility.
# Where the terminal isn't allowed to (System Settings › Privacy & Security › Accessibility), it waits for a person
# to press Open in Device Hub instead, and fails after OPEN_TIMEOUT seconds, which skips the slot.
open_duo() {
  local udid="$1" name="$2" hub
  # The Device Hub of the Xcode in use, which is the one that knows its simulators: a runner has several Xcodes, and
  # the bundle identifier alone lets macOS pick any of theirs.
  hub="$(dirname "$(xcode-select -p)")/Applications/DeviceHub.app"
  if [ -d "$hub" ]; then
    open -g "$hub"
  else
    echo "No Device Hub at $hub: opening whichever Xcode's $DEVICE_HUB macOS picks" >&2
    open -g -b "$DEVICE_HUB"
  fi
  if ! osascript - "$name" << 'APPLESCRIPT' > /dev/null 2>&1; then
on run arguments
  set simulatorName to item 1 of arguments
  -- Just launched, Device Hub may be neither a process nor a window yet.
  tell application "System Events"
    repeat 30 times
      if exists (first process whose bundle identifier is "com.apple.dt.Devices") then exit repeat
      delay 1
    end repeat
  end tell
  tell application "System Events" to tell (first process whose bundle identifier is "com.apple.dt.Devices")
    repeat 30 times
      if exists window 1 then exit repeat
      delay 1
    end repeat
    -- The sidebar is an outline: the simulator's row is the one holding its name, selected as a click would.
    set isSelected to false
    repeat with element in entire contents of window 1
      if class of element is row then
        if (value of every static text of entire contents of element) contains simulatorName then
          set value of attribute "AXSelected" of element to true
          set isSelected to true
          exit repeat
        end if
      end if
    end repeat
    -- Open acts on the device selected: without this one's row, it would open another session's Duo.
    if not isSelected then error "No " & simulatorName & " in Device Hub"
    -- The bar under the device is only there once Device Hub has connected to its display ("Connecting display…").
    -- Its buttons have no words, so "Open" is the button's accessibility label, read both ways an app can give one.
    repeat 15 times
      delay 2
      repeat with element in entire contents of window 1
        if class of element is button and (description of element is "Open" or name of element is "Open") then
          click element
          return
        end if
      end repeat
    end repeat
    error "No Open button in Device Hub"
  end tell
end run
APPLESCRIPT
    echo "Couldn't press Open in Device Hub (is this terminal allowed in Accessibility?)." >&2
    echo "Select \"$name\" in Device Hub and press Open, in the bar under the device: waiting ${OPEN_TIMEOUT}s." >&2
  fi
  local waited=0
  until duo_is_open "$udid"; do
    if [ "$waited" -ge "$OPEN_TIMEOUT" ]; then
      return 1
    fi
    sleep 2
    waited=$((waited + 2))
  done
  # The fold's animation, and the home screen moving to the inner display.
  sleep 3
}

# The Duo's two slots share one simulator, closed for the first and open for the second.
simulator_name() {
  case "$1" in
    iphoneduoopen) echo "Store screenshots iphoneduo" ;;
    *) echo "Store screenshots $1" ;;
  esac
}

simulator_for() {
  local name
  name="$(simulator_name "$1")"
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

captured=0
for slot in $SLOTS; do
  udid="$(simulator_for "$slot")"
  echo "Capturing $slot on $udid"
  # The Duo boots closed, whatever a run that stopped halfway left it as: its two slots share the simulator.
  case "$slot" in iphoneduo*) xcrun simctl shutdown "$udid" 2> /dev/null || true ;; esac
  xcrun simctl boot "$udid" 2> /dev/null || true
  xcrun simctl bootstatus "$udid" -b > /dev/null
  xcrun simctl status_bar "$udid" override --time "9:41" --dataNetwork wifi --wifiMode active --wifiBars 3 \
    --cellularMode active --cellularBars 4 --batteryState discharging --batteryLevel 100
  # Only the locales and screens asked for are captured again: a narrowed run keeps the others' PNGs. A whole run
  # starts the slot over, so one it then skips leaves no older capture to frame.
  if [ -z "$STORE_SCREENSHOTS_LOCALES" ] && [ -z "$STORE_SCREENSHOTS_SCREENS" ]; then rm -rf "${OUTPUT:?}/$slot"; fi
  if [ "$slot" = iphoneduoopen ] && ! open_duo "$udid" "$(simulator_name "$slot")"; then
    echo "The iPhone Duo didn't open: skipping $slot, the other slots go on" >&2
    xcrun simctl shutdown "$udid" || true
    continue
  fi
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
  captured=$((captured + 1))
done

# A run of the open iPhone Duo alone, as a release's job is, fails here rather than at whatever reads its captures.
if [ "$captured" -eq 0 ]; then
  echo "No slot was captured" >&2
  exit 1
fi
echo "Captured into $OUTPUT"
