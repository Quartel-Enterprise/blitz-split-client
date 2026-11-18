#!/bin/bash

# Captures the App Store screenshots' screens on the iOS simulator (docs/store-listing-screenshots.md): builds the
# BoraRachar-iOS-StoreScreenshots UI tests once, then runs them on one simulator per App Store slot, one language at
# a time, which write
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
# STORE_SCREENSHOTS_SIMULATOR names another one to use or make, for a run of one device beside another run of this
# script (a task's own simulator: two runs can't share one).
#
# Progress. Each language is a run of the UI test of its own (capture_language below), so the log says which one is
# being captured, how long each took and about how long the slot still takes:
#   Capturing iphone67: pt-BR (2 of 26)
#   Captured iphone67: pt-BR in 2m 05s (2 of 26 done, about 50 min left)
# xcodebuild's own output goes to build/apple-screenshots/logs/<slot>/<locale>.log. On GitHub Actions it is in the
# log too, folded under the language's line, and the job's summary gets a table of the languages, written as each one
# ends.
#
# A language that fails. It is tried once more, since a launch that hangs or a runner that dies is rarely the
# language's fault. Failing again, what it was to capture is deleted: its folder, so the frame sees a slot short of a
# language, or, in a run narrowed to some screens, those screens alone, which the frame then stops at by name
# rather than draw an earlier run's. The script goes on with the other languages and slots: one bad language doesn't
# hide whether the others work, and their captures stay to look at or to frame once that language is taken again
# (STORE_SCREENSHOTS_SLOTS and STORE_SCREENSHOTS_LOCALES, which keep the others' PNGs). A third failed language ends
# the slot, though: by then it is the slot that fails, and a failure that hangs for minutes, twice per language,
# would use up a release job's time. The script then fails at the end, naming every slot and language that failed.
# The frame (`./gradlew stageAppStoreScreenshots`) takes a slot only with every language: it stops without one of
# the iPhone's or the iPad's, and doesn't frame an iPhone Duo's or the Mac's slot that misses one.

set -eo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
TABLES="$ROOT/scripts/store-screenshots"
# shellcheck source=scripts/store-screenshots/tables.sh
. "$TABLES/tables.sh"
OUTPUT="${STORE_SCREENSHOTS_OUTPUT:-$ROOT/build/outputs/apple-screenshot-captures}"
DERIVED_DATA="$ROOT/build/apple-screenshots/DerivedData"
LOGS="$ROOT/build/apple-screenshots/logs"
SLOTS="${STORE_SCREENSHOTS_SLOTS:-iphone63 iphone67 ipad13 iphoneduo iphoneduoopen}"
# How often a language is tried before it counts as failed.
ATTEMPTS=2
# How many failed languages end a slot.
FAILED_LANGUAGES=3
APP_ID="com.quare.blitzsplit"
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
  if [ -n "$STORE_SCREENSHOTS_SIMULATOR" ]; then
    echo "$STORE_SCREENSHOTS_SIMULATOR"
    return
  fi
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
  # By its name and its device: a simulator STORE_SCREENSHOTS_SIMULATOR names may be another slot's.
  udid="$(xcrun simctl list devices -j | /usr/bin/python3 -I -c "
import json, sys
name, runtime, device_type = sys.argv[1], sys.argv[2], sys.argv[3]
devices = json.load(sys.stdin)['devices'].get(runtime, [])
fits = [d for d in devices if d['name'] == name and d['isAvailable'] and d['deviceTypeIdentifier'] == device_type]
print(next((d['udid'] for d in fits), ''))
" "$name" "$runtime" "$(device_type "$1")")"
  if [ -z "$udid" ]; then
    udid="$(xcrun simctl create "$name" "$(device_type "$1")" "$runtime")"
  fi
  echo "$udid"
}

# "2m 05s", of a number of seconds.
duration() { printf '%dm %02ds' $(($1 / 60)) $(($1 % 60)); }

# A line of the job's summary on GitHub Actions, which shows on the run's page without opening the log.
summary() {
  if [ -n "$GITHUB_STEP_SUMMARY" ]; then echo "$1" >> "$GITHUB_STEP_SUMMARY"; fi
}

# A failure, as an annotation on the run's page on GitHub Actions.
failure() {
  if [ "$GITHUB_ACTIONS" = true ]; then echo "::error::$1"; else echo "$1" >&2; fi
}

# One run of the UI test, narrowed to one language. Its output goes to a file, and to the log too on GitHub Actions,
# where the language's line folds it. It runs the built tests from their .xctestrun file, which loads no project and
# resolves no Swift package: three seconds more per language than one run for them all, where a run through the
# project and its scheme took eighteen (an iPhone 17 Pro's simulator, November 2026).
run_ui_test() {
  local slot="$1" udid="$2" tag="$3" log="$4"
  # A language takes two to three minutes: a test that hangs fails after ten and is tried again, instead of holding
  # the release job until its time is out, which uploads none of the languages already captured.
  local run=(
    env
    "TEST_RUNNER_STORE_SCREENSHOTS_OUTPUT=$OUTPUT"
    "TEST_RUNNER_STORE_SCREENSHOTS_SLOT=$slot"
    "TEST_RUNNER_STORE_SCREENSHOTS_ASSETS=$TABLES"
    "TEST_RUNNER_STORE_SCREENSHOTS_LOCALES=$tag"
    "TEST_RUNNER_STORE_SCREENSHOTS_SCREENS=$STORE_SCREENSHOTS_SCREENS"
    "TEST_RUNNER_STORE_SCREENSHOTS_DEVICE_IS_SET_UP=$device_is_set_up"
    "TEST_RUNNER_STORE_SCREENSHOTS_CALLER_RESTORES_LANGUAGE=1"
    xcodebuild test-without-building
    -xctestrun "$xctestrun"
    -destination "id=$udid"
    -derivedDataPath "$DERIVED_DATA"
    -test-timeouts-enabled YES
    -default-test-execution-time-allowance 600
  )
  if [ "$GITHUB_ACTIONS" = true ]; then
    "${run[@]}" 2>&1 | tee "$log"
  else
    "${run[@]}" > "$log" 2>&1
  fi
}

# The first screen the language has no capture of, in the shelf's order, which is the order they are captured in:
# after a run that failed, the screen it failed on. Nothing when every screen is there.
first_missing_screen() {
  local directory="$1" file
  for file in "${screen_files[@]}"; do
    if [ ! -s "$directory/$file.png" ]; then
      echo "$file"
      return
    fi
  done
}

# Deletes what a run of the language is to write: the screens asked for, and never another screen an earlier run
# captured.
forget_captures() {
  local directory="$1" file
  for file in "${screen_files[@]}"; do rm -f "$directory/$file.png"; done
}

# Captures one language of a slot, trying ATTEMPTS times, and says how it went: in the log, and as a row of the
# summary's table. Returns 1 for a language that failed every time, with nothing of it left behind.
capture_language() {
  local slot="$1" udid="$2" tag="$3" position="$4" total="$5"
  local directory="$OUTPUT/$slot/$tag" attempt log started seconds status missing result spent=0
  local line="Capturing $slot: $tag ($position of $total)"
  for attempt in $(seq "$ATTEMPTS"); do
    log="$LOGS/$slot/$tag.log"
    if [ "$attempt" -gt 1 ]; then
      log="$LOGS/$slot/$tag.$attempt.log"
      line="Capturing $slot: $tag again ($position of $total, try $attempt of $ATTEMPTS)"
    fi
    # What this run is to write goes first, so a capture that is there afterwards is this run's.
    forget_captures "$directory"
    if [ "$GITHUB_ACTIONS" = true ]; then echo "::group::$line"; else echo "$line"; fi
    started="$(date +%s)"
    status=0
    run_ui_test "$slot" "$udid" "$tag" "$log" || status=$?
    seconds=$(($(date +%s) - started))
    # Every try counts toward the language's time, as it does toward the slot's.
    spent=$((spent + seconds))
    if [ "$GITHUB_ACTIONS" = true ]; then echo "::endgroup::"; fi
    missing="$(first_missing_screen "$directory")"
    if [ "$status" -eq 0 ] && [ -z "$missing" ]; then
      result="captured"
      if [ "$attempt" -gt 1 ]; then result="captured on try $attempt"; fi
      summary "| $tag | ✅ $result | $(duration "$spent") |"
      language_seconds="$spent"
      # What the test does once per simulator (the iPad's full-screen apps) is done: the next runs skip it.
      device_is_set_up=1
      return 0
    fi
    result="failed"
    if [ -n "$missing" ]; then result="failed at $missing"; fi
    echo "Failed $slot: $tag after $(duration "$seconds"), at ${missing:-the end of the run} (try $attempt of $ATTEMPTS)"
    # The test's own words for it, which xcodebuild's last lines ("Failing tests:") leave out.
    grep -E ' error: |Fatal error|Restarting after unexpected exit' "$log" | head -5 || true
    echo "  xcodebuild's output: $log"
  done
  # Nothing of what it was to capture stays: its whole folder, unless the run was narrowed to some screens, whose
  # others are an earlier run's.
  forget_captures "$directory"
  if [ -z "$STORE_SCREENSHOTS_SCREENS" ]; then rm -rf "${directory:?}"; fi
  summary "| $tag | ❌ $result, $ATTEMPTS tries | $(duration "$spent") |"
  return 1
}

# Every language of a slot, one after the other. A language that failed doesn't stop the others, but the slot's
# FAILED_LANGUAGES-th does: by then it is the slot that fails, and each one more costs two tries. failed_languages
# names the slot's, and counts the ones it then left untried.
capture_slot() {
  local slot="$1" udid="$2" total="${#locales[@]}" position=0 captured_count=0 failed_count=0 done_seconds=0
  local tag left started untried
  started="$(date +%s)"
  failed_languages=""
  device_is_set_up=""
  mkdir -p "$LOGS/$slot"
  rm -f "$LOGS/$slot"/*.log
  summary "### App Store captures: $slot"
  summary ""
  summary "| Language | Result | Time |"
  summary "| --- | --- | --- |"
  for tag in "${locales[@]}"; do
    position=$((position + 1))
    if capture_language "$slot" "$udid" "$tag" "$position" "$total"; then
      captured_count=$((captured_count + 1))
      done_seconds=$((done_seconds + language_seconds))
      # What is left, at the pace of the languages captured so far.
      left="$position of $total done, about $(((total - position) * done_seconds / captured_count / 60)) min left"
      if [ "$position" -eq "$total" ]; then left="$position of $total done"; fi
      echo "Captured $slot: $tag in $(duration "$language_seconds") ($left)"
      continue
    fi
    failed_languages="$failed_languages $tag"
    failed_count=$((failed_count + 1))
    untried=$((total - position))
    if [ "$failed_count" -ge "$FAILED_LANGUAGES" ] && [ "$untried" -gt 0 ]; then
      echo "$failed_count languages of $slot failed: the other $untried are left untried"
      summary "| the other $untried | ⏭️ not tried, after $failed_count languages failed | |"
      failed_languages="$failed_languages and $untried left untried"
      break
    fi
  done
  failed_languages="${failed_languages# }"
  summary ""
  if [ -z "$failed_languages" ]; then
    summary "$total of $total languages captured in $(duration $(($(date +%s) - started)))."
    return 0
  fi
  summary "**Failed: $failed_languages.** $captured_count of $total were captured, in $(duration $(($(date +%s) - started)))."
  return 1
}

# A malformed row would fail every language's run alike, after two tries each: stop before the build instead.
check_table locales 4
check_table screens 3
check_table assets 2
# The app's own language comes back once a slot's captures end, whatever they stopped on: one launch without the seed
# (StoreScreenshotsRun), here, where the test's own would be one more launch per language. Nothing to restore, and
# nothing launched, on a simulator the app never reached.
restore_app_language() {
  local udid="$1"
  if xcrun simctl launch "$udid" "$APP_ID" -StoreScreenshotsRestore YES > /dev/null 2>&1; then
    sleep 2
    xcrun simctl terminate "$udid" "$APP_ID" > /dev/null 2>&1 || true
  fi
}

chosen="$(picked locales 1 "$STORE_SCREENSHOTS_LOCALES")"
# Split on purpose: one tag or file name per line, none with a space.
# shellcheck disable=SC2206
locales=($chosen)
chosen="$(picked screens 2 "$STORE_SCREENSHOTS_SCREENS")"
# shellcheck disable=SC2206
screen_files=($chosen)

# One name is one simulator, and Device Hub picks the Duo to open by its name: a simulator named by hand serves the
# slots of one device (the Duo's two share theirs).
if [ -n "$STORE_SCREENSHOTS_SIMULATOR" ]; then
  devices="$(for slot in $SLOTS; do device_type "$slot"; done | sort -u | wc -l)"
  if [ "$devices" -gt 1 ]; then
    echo "STORE_SCREENSHOTS_SIMULATOR names one simulator: pass the slots of one device in STORE_SCREENSHOTS_SLOTS" >&2
    exit 2
  fi
fi

"$ROOT/iosApp/generate-project.sh"

echo "Building the store screenshots' UI tests"
# The build writes the file the runs read, named after the SDK: an older Xcode's would be a second one.
rm -f "$DERIVED_DATA"/Build/Products/*.xctestrun
xcodebuild build-for-testing \
  -project "$ROOT/iosApp/BoraRachar.xcodeproj" \
  -scheme BoraRachar-iOS-StoreScreenshots \
  -destination "generic/platform=iOS Simulator" \
  -derivedDataPath "$DERIVED_DATA" \
  ARCHS=arm64 \
  -quiet
xctestruns=("$DERIVED_DATA"/Build/Products/*.xctestrun)
xctestrun="${xctestruns[0]}"
if [ "${#xctestruns[@]}" -ne 1 ] || [ ! -f "$xctestrun" ]; then
  echo "The build left not one .xctestrun file in $DERIVED_DATA/Build/Products: ${xctestruns[*]}" >&2
  exit 1
fi

captured=0
failed=""
for slot in $SLOTS; do
  udid="$(simulator_for "$slot")"
  echo "Capturing $slot on $udid: ${#locales[@]} language(s), ${#screen_files[@]} screen(s) each"
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
  if capture_slot "$slot" "$udid"; then
    captured=$((captured + 1))
  else
    failed="$failed, $slot ($failed_languages)"
  fi
  restore_app_language "$udid"
  xcrun simctl shutdown "$udid" || true
done

if [ -n "$failed" ]; then
  failure "The App Store captures failed for ${failed#, }: the frame takes a slot only with every language"
  exit 1
fi
# A run of the open iPhone Duo alone, as a release's job is, fails here rather than at whatever reads its captures.
if [ "$captured" -eq 0 ]; then
  failure "No slot was captured"
  exit 1
fi
echo "Captured into $OUTPUT"
