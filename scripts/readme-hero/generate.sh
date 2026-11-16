#!/bin/bash

# Draws the README's opening image, docs/images/readme-hero-{dark,light}.webp: the group of the sample trip (the one the
# store screenshots tell, docs/store-listing-screenshots.md) on Android, the iPhone, the Mac and the web, each drawn by
# its own native UI, side by side. Each capture is a real screen of the app, never a mockup, so running this again
# after a UI change brings the image up to date.
#
#   ./scripts/readme-hero/generate.sh                           # capture all four, then draw the image
#   READMEHERO_CAPTURES=android,web ./scripts/readme-hero/generate.sh   # capture again only those, keep the others
#   READMEHERO_CAPTURES=none ./scripts/readme-hero/generate.sh          # draw the image again from the captures
#
# The captures, dark and light, land in build/outputs/readme-hero/:
# - android: Robolectric draws the Compose screen (ReadmeHeroScreens in :android:store_screenshots). Any machine.
# - ios: the store screenshots' UI test on the iPhone 17 Pro simulator (capture-ios.sh). Needs Xcode.
# - mac: the store screenshots' Mac capture (capture-mac.sh). It takes this Mac's screen for about half a minute and
#   needs the Screen Recording permission.
# - web: the dev server in a headless Chrome of its own (capture-web.mjs), which seeds the trip in an empty profile.
# Then compose.mjs lays them out (hero.html) and photographs the page. Every heavy step waits its turn behind other
# worktrees' builds (scripts/heavy-check.sh).

set -eo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
HERE="$ROOT/scripts/readme-hero"
CAPTURES="$ROOT/build/outputs/readme-hero"
APPLE_CAPTURES="$ROOT/build/outputs/readme-hero-apple"
ANDROID_CAPTURES="$ROOT/build/outputs/readme-hero-android"
IMAGES="$ROOT/docs/images"
WANTED="${READMEHERO_CAPTURES:-android,ios,mac,web}"
HEAVY="$ROOT/scripts/heavy-check.sh"

wanted() { [[ ",$WANTED," == *",$1,"* ]]; }

mkdir -p "$CAPTURES" "$IMAGES"

if wanted android; then
  echo "Capturing Android"
  "$HEAVY" "$ROOT/gradlew" -p "$ROOT" :android:store_screenshots:testDebugUnitTest -PreadmeHero --rerun -q
  # Why: the test task owns a folder of its own, which Gradle may wipe as a stale output; the other captures stay out
  cp "$ANDROID_CAPTURES/android-dark.png" "$ANDROID_CAPTURES/android-light.png" "$CAPTURES/"
fi

# The store screenshots' 01_group and 10_light_theme are the group, dark and light: the README takes them as they are.
# The folder each capture script writes its slot to: the iPhone 17 Pro's (iphone63) and the Mac's.
apple_capture() {
  local platform="$1" script="$2" folder="$3"
  echo "Capturing $platform"
  STORE_SCREENSHOTS_OUTPUT="$APPLE_CAPTURES" STORE_SCREENSHOTS_LOCALES=en-US STORE_SCREENSHOTS_SCREENS=group,light \
    "$HEAVY" "$script"
  cp "$APPLE_CAPTURES/$folder/en-US/01_group.png" "$CAPTURES/$platform-dark.png"
  cp "$APPLE_CAPTURES/$folder/en-US/10_light_theme.png" "$CAPTURES/$platform-light.png"
}
if wanted ios; then
  STORE_SCREENSHOTS_SLOTS=iphone63 apple_capture ios "$ROOT/scripts/store-screenshots/capture-ios.sh" iphone63
fi
if wanted mac; then apple_capture mac "$ROOT/scripts/store-screenshots/capture-mac.sh" mac; fi

if wanted web; then
  echo "Capturing the web"
  "$HEAVY" npm --prefix "$ROOT/webApp" run shared
  # A fresh worktree has none yet; the shared library must be built first, as package.json takes it from a folder
  [ -d "$ROOT/webApp/node_modules" ] || npm --prefix "$ROOT/webApp" install
  port="$(node -e 'const s = require("net").createServer().listen(0, () => { console.log(s.address().port); s.close() })')"
  log="$CAPTURES/vite.log"
  # Vite alone (not `npm run dev`), since the shared library was just built
  (cd "$ROOT/webApp" && exec npx vite --port "$port" --strictPort > "$log" 2>&1) &
  vite=$!
  trap 'kill "$vite" 2> /dev/null || true' EXIT
  is_up=false
  for _ in $(seq 120); do
    if curl -sf "http://localhost:$port/" > /dev/null; then
      is_up=true
      break
    fi
    sleep 0.5
  done
  if [ "$is_up" != true ]; then
    echo "The Vite dev server didn't answer on port $port in a minute; its log:" >&2
    cat "$log" >&2
    exit 1
  fi
  node "$HERE/capture-web.mjs" "http://localhost:$port/" "$CAPTURES"
  kill "$vite" 2> /dev/null || true
fi

for platform in android ios mac web; do
  for theme in dark light; do
    if [ ! -s "$CAPTURES/$platform-$theme.png" ]; then
      echo "No $platform-$theme.png in $CAPTURES: capture $platform first (READMEHERO_CAPTURES=$platform)" >&2
      exit 1
    fi
  done
done

node "$HERE/compose.mjs" "$CAPTURES" "$IMAGES"
