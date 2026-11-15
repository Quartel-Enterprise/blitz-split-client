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
# alone until it ends. The run fails when the window was on a display that isn't Retina, whose captures the frame
# would blur. STORE_SCREENSHOTS_LOCALES and STORE_SCREENSHOTS_SCREENS narrow it as in capture-ios.sh.
# STORE_SCREENSHOTS_BUILD_SETTINGS adds build settings to the build, for a machine with no signing account (the
# release's runner: release-production.yml).

set -eo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
TABLES="$ROOT/scripts/store-screenshots"
OUTPUT="${STORE_SCREENSHOTS_OUTPUT:-$ROOT/build/outputs/apple-screenshot-captures}"
DERIVED_DATA="$ROOT/build/apple-screenshots/DerivedData-mac"
APP="$DERIVED_DATA/Build/Products/Debug/BoraRachar.app"
APP_OUTPUT="$DERIVED_DATA/store-screenshots-app-output.txt"
# How long the app may take to open its window, and how long a screen takes to seed, open and settle after that.
WINDOW_TIMEOUT=30
SETTLE=4

# The rows of a table of scripts/store-screenshots, without its # comment lines.
rows() { grep -v '^#' "$TABLES/$1.tsv"; }

# Whether a comma-separated list (empty: everything) has the item.
wanted() { [ -z "$1" ] || [[ ",$1," == *",$2,"* ]]; }

quit_app() {
  pkill -f "$APP/Contents/MacOS/" 2>/dev/null || true
  while pgrep -f "$APP/Contents/MacOS/" > /dev/null; do sleep 0.2; done
}

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
    screencapture -x -o -l "$window" "$OUTPUT/mac/$tag/$file_name.png"
    quit_app
  done < <(rows screens)
  echo "Captured $tag"
done < <(rows locales)

echo "Captured into $OUTPUT/mac"

# The frame draws the window at twice its points: a capture from a display that isn't Retina would go to the listing
# blurred, so the run fails and whatever called it stops before framing or uploading.
capture="$(find "$OUTPUT/mac" -name '*.png' -print -quit)"
width="$(sips -g pixelWidth "$capture" | awk '/pixelWidth/ { print $2 }')"
if [ "$width" -lt 2880 ]; then
  echo "The Mac captures are $width px wide, the frame takes 2880: the window was on a display that isn't Retina" >&2
  exit 1
fi
