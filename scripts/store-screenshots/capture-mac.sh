#!/bin/bash

# Captures the Mac App Store screenshots' windows (docs/store-listing-screenshots.md): builds the debug Mac app, then
# launches it once per language and listing screen, seeded with the sample trip, and photographs its window into
#   build/outputs/apple-screenshot-captures/mac/<locale>/<screen>.png
# for the Robolectric generators to frame. No UI test drives it, as one does on the simulators (capture-ios.sh): a
# Mac's test runner is sandboxed, so it can't write the captures out. The app opens its window by itself, hides the
# other apps' windows and prints the window's number (StoreScreenshotsRun), and `screencapture` photographs that
# window alone, which takes the Screen Recording permission for whatever runs this script.
#
# The app takes this Mac's screen for the whole run, about ten seconds per capture: leave the keyboard and mouse
# alone until it ends. The run stops at the first capture when the window is on a display that isn't Retina, whose
# captures the frame would blur. STORE_SCREENSHOTS_LOCALES and STORE_SCREENSHOTS_SCREENS narrow it as in capture-ios.sh.
# STORE_SCREENSHOTS_BUILD_SETTINGS adds build settings to the build, for a machine with no signing account (the
# release's runner: release-production.yml).

set -eo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
TABLES="$ROOT/scripts/store-screenshots"
# shellcheck source=scripts/store-screenshots/tables.sh
. "$TABLES/tables.sh"
OUTPUT="${STORE_SCREENSHOTS_OUTPUT:-$ROOT/build/outputs/apple-screenshot-captures}"
DERIVED_DATA="$ROOT/build/apple-screenshots/DerivedData-mac"
APP="$DERIVED_DATA/Build/Products/Debug/BoraRachar.app"
# The app's processes, for pkill and pgrep, which read a regular expression: the path's own characters are escaped,
# or a "+" or a "(" in a folder's name would match nothing and leave every launch running.
APP_PROCESS="$(printf '%s' "$APP/Contents/MacOS/" | sed 's/[][\\.^$*+?(){}|]/\\&/g')"
APP_OUTPUT="$DERIVED_DATA/store-screenshots-app-output.txt"
# How long the app may take to open its window, and how long a screen takes to seed, open and settle after that.
WINDOW_TIMEOUT=30
SETTLE=4

# Stops on a row that hasn't the table's number of tab-separated fields (spaces typed for a tab), naming it: read as
# it is, it would launch the app on the wrong screen and write the capture under an empty name.
check_table() {
  local bad
  bad="$(rows "$1" | awk -F'\t' -v fields="$2" 'NF != fields { print; exit }')"
  if [ -n "$bad" ]; then
    echo "$1.tsv: \"$bad\" has not $2 fields" >&2
    exit 1
  fi
}

# Whether a comma-separated list (empty: everything) has the item.
wanted() { [ -z "$1" ] || [[ ",$1," == *",$2,"* ]]; }

# Ends the app as the iOS test's terminate() does, and kills it if it is still there five seconds later, so a stuck
# launch can't hold the run. Every launch starts from a fresh database, so nothing of the one ended is read again.
quit_app() {
  pkill -f "$APP_PROCESS" 2>/dev/null || true
  for _ in $(seq 25); do
    pgrep -f "$APP_PROCESS" > /dev/null || return 0
    sleep 0.2
  done
  pkill -9 -f "$APP_PROCESS" 2>/dev/null || true
}

check_table locales 4
check_table screens 3
check_table assets 2
# Stops on a locale or a screen asked for that no row has, which the loops below would pass over.
picked locales 1 "$STORE_SCREENSHOTS_LOCALES" > /dev/null
picked screens 1 "$STORE_SCREENSHOTS_SCREENS" > /dev/null

"$ROOT/iosApp/generate-project.sh"

echo "Building the Mac app"
# Unquoted on purpose: several settings, each a word of its own.
# shellcheck disable=SC2086
xcodebuild build \
  -project "$ROOT/iosApp/BoraRachar.xcodeproj" \
  -scheme BoraRachar-macOS \
  -configuration Debug \
  -destination "platform=macOS,arch=arm64" \
  -derivedDataPath "$DERIVED_DATA" \
  $STORE_SCREENSHOTS_BUILD_SETTINGS \
  -quiet

# The faces, the groups' photos and the receipt's photo, base64 in the launch environment as the iOS test passes
# them: the Mac app's sandbox can't read the repository, so the app writes them to a folder of its own.
assets=()
while IFS=$'\t' read -r name file; do
  assets+=(--env "STORE_SCREENSHOTS_ASSET_$name=$(base64 -i "$TABLES/$file")")
done < <(rows assets)

# Only the locales and screens asked for are captured again: a narrowed run keeps the others' PNGs.
if [ -z "$STORE_SCREENSHOTS_LOCALES" ] && [ -z "$STORE_SCREENSHOTS_SCREENS" ]; then rm -rf "${OUTPUT:?}/mac"; fi

quit_app
# The app's own language comes back once the captures end, whatever they stopped on.
restore_language() {
  quit_app
  open -n "$APP" --args -StoreScreenshotsRestore YES
  sleep "$SETTLE"
  quit_app
}
trap restore_language EXIT

last_capture=""
while IFS=$'\t' read -r tag language apple_language apple_locale; do
  wanted "$STORE_SCREENSHOTS_LOCALES" "$tag" || continue
  mkdir -p "$OUTPUT/mac/$tag"
  while IFS=$'\t' read -r code file_name theme; do
    wanted "$STORE_SCREENSHOTS_SCREENS" "$code" || continue
    : > "$APP_OUTPUT"
    open -n "${assets[@]}" --stdout "$APP_OUTPUT" "$APP" --args \
      -StoreScreenshotsLanguage "$language" -StoreScreenshotsTheme "$theme" -StoreScreenshotsScreen "$code" \
      -AppleLanguages "($apple_language)" -AppleLocale "$apple_locale" < /dev/null
    window=""
    for _ in $(seq $((WINDOW_TIMEOUT * 5))); do
      window="$(sed -n 's/^store-screenshots-window \([0-9][0-9]*\)$/\1/p' "$APP_OUTPUT" | tail -1)"
      [ -n "$window" ] && break
      sleep 0.2
    done
    if [ -z "$window" ]; then
      echo "The app opened no window for $tag $code in $WINDOW_TIMEOUT seconds" >&2
      exit 1
    fi
    sleep "$SETTLE"
    # The window alone, without its shadow, at the size StoreScreenshotsRun gives it, which the Mac's frame draws
    # on its own canvas
    capture="$OUTPUT/mac/$tag/$file_name.png"
    rm -f "$capture"
    screencapture -x -o -l "$window" "$capture" || true
    quit_app
    if [ ! -s "$capture" ]; then
      echo "screencapture wrote no image of the window for $tag $code: whatever runs this script needs the Screen" \
        "Recording permission (System Settings > Privacy & Security)" >&2
      exit 1
    fi
    # The frame draws the window at twice its points: a capture from a display that isn't Retina would go to the
    # listing blurred. Checked on the first capture, which stays there to look at, not after half an hour of them.
    if [ -z "$last_capture" ]; then
      width="$(sips -g pixelWidth "$capture" | awk '/pixelWidth/ { print $2 }')"
      if [ "$width" -lt 2880 ]; then
        echo "The Mac capture is $width px wide and the frame takes 2880: its display isn't Retina" >&2
        exit 1
      fi
    fi
    last_capture="$capture"
  done < <(rows screens)
  echo "Captured $tag"
done < <(rows locales)

if [ -z "$last_capture" ]; then
  echo "Nothing captured: no row of locales.tsv and screens.tsv matches what was asked for" >&2
  exit 1
fi
echo "Captured into $OUTPUT/mac"
