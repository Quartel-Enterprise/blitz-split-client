#!/usr/bin/env bash
#
# Publish the GitHub release. Run by .github/workflows/release-production.yml.
#
set -eo pipefail

released=false
if [ "$TRACK" = production ] && [ "$ANDROID" = success ]; then released=true; fi
if [ "$IOS_SUBMIT" = success ] || [ "$MACOS_SUBMIT" = success ]; then released=true; fi
if [ "$WEB" = success ]; then released=true; fi
# Only a 404 says the version has no release: any other failure read as "not released" would skip the
# tag of a version that went out, or try to create a release that is there.
errors="$RUNNER_TEMP/release-errors.txt"
if release="$(gh api "repos/$GITHUB_REPOSITORY/releases/tags/$VERSION" 2> "$errors")"; then
  exists=true
elif grep -q "HTTP 404" "$errors"; then
  exists=false
else
  cat "$errors"
  echo "::error::Could not ask GitHub whether $VERSION has a release. Re-run this job."
  exit 1
fi
if [ "$released" = false ] && [ "$exists" = false ]; then
  echo "released=false" >> "$GITHUB_OUTPUT"
  echo "::notice::Nothing reached the production track, App Store review or the web: $VERSION gets no tag, and its fragments stay. Its versionCode is merged back, and its version when higher than main's."
  exit 0
fi
echo "released=true" >> "$GITHUB_OUTPUT"

android_channel="Google Play, \`$TRACK\` track"
if [ "$TRACK" = production ]; then android_channel="$android_channel, $ROLLOUT% of the users"; fi
# The watch's part only when its upload ran: a phone that failed never sent it.
if [ "$WEAR" != skipped ]; then android_channel="$android_channel, the watch on Wear OS's"; fi
# The phone's result decides the release; the watch's, sent after it, is told next to it when it differs.
android="$ANDROID"
if [ "$ANDROID" = success ] && [ "$WEAR" != success ]; then android="success (watch: $WEAR)"; fi
if [ "$SUBMIT" = true ]; then
  apple_channel="App Store, sent for review"
  ios="$IOS_SUBMIT"
  macos="$MACOS_SUBMIT"
  # A submission that never started because the upload failed is that failure, not a platform left out.
  if [ "$IOS_UPLOAD" != success ]; then ios="$IOS_UPLOAD"; fi
  if [ "$MACOS_UPLOAD" != success ]; then macos="$MACOS_UPLOAD"; fi
else
  apple_channel="TestFlight"
  ios="$IOS_UPLOAD"
  macos="$MACOS_UPLOAD"
fi

web_channel="Cloudflare Pages, $WEB_URL"
# The build number a store may hold. A run of the web alone has none to record: no store was asked to
# take a build, so the release names none and the next one doesn't skip a number for it.
code="$VERSION_CODE"
if [ "$STORES" != true ]; then code=0; fi

if [ "$exists" = true ]; then
  # A platform this run left out, or was cancelled on, keeps the row an earlier run wrote for it, and so
  # does one this run
  # only sent to a testing track or to TestFlight when the row says it was released. The web's keeps a
  # success a later deployment that failed didn't undo: the site still serves the one that worked. A
  # release from before the web was part of a stable version has no Web row: it goes after macOS's.
  jq -r '.body // ""' <<< "$release" | tr -d '\r' | awk \
    -v code="$code" -v android="$android" -v ios="$ios" -v macos="$macos" \
    -v android_channel="$android_channel" -v apple_channel="$apple_channel" \
    -v track="$TRACK" -v submit="$SUBMIT" -v web="$WEB" -v web_channel="$web_channel" '
    function left_out(result) { return result == "skipped" || result == "cancelled" }
    function keeps(result, releases) { return left_out(result) || (!releases && $0 ~ /production|sent for review/) }
    function web_row() { return "| Web | " web_channel " | " web " |" }
    after_macos { if ($0 !~ /^\| Web \|/ && !left_out(web)) print web_row(); after_macos = 0 }
    /^Build number: / { if ($3 + 0 < code + 0) print "Build number: " code; else print; next }
    /^\| Android \|/ && !keeps(android, track == "production") { print "| Android | " android_channel " | " android " |"; next }
    /^\| iOS \|/ && !keeps(ios, submit == "true")              { print "| iOS | " apple_channel " | " ios " |"; next }
    /^\| macOS \|/ { if (keeps(macos, submit == "true")) print; else print "| macOS | " apple_channel " | " macos " |"; after_macos = 1; next }
    /^\| Web \|/ && !left_out(web) && (web == "success" || $0 !~ /\| success \|$/) { print web_row(); next }
    { print }
    END { if (after_macos && !left_out(web)) print web_row() }' > "$RUNNER_TEMP/notes.md"
  # A release the web alone made names no build number: the first run that sends a store one adds it.
  if [ "$code" -gt 0 ] && ! grep -q '^Build number: ' "$RUNNER_TEMP/notes.md"; then
    { printf 'Build number: %s\n\n' "$code"; cat "$RUNNER_TEMP/notes.md"; } > "$RUNNER_TEMP/notes-with-build.md"
    mv "$RUNNER_TEMP/notes-with-build.md" "$RUNNER_TEMP/notes.md"
  fi
  gh release edit "$VERSION" --notes-file "$RUNNER_TEMP/notes.md"
  echo "::notice::$VERSION is already published: its table of channels was refreshed."
  exit 0
fi

{
  if [ "$code" -gt 0 ]; then
    echo "Build number: $code"
    echo
  fi
  if [ -n "$NOTES" ]; then
    printf '%s\n\n' "$NOTES"
  fi
  echo "## Channels"
  echo
  echo "| Platform | Channel | Result |"
  echo "| --- | --- | --- |"
  echo "| Android | $android_channel | $android |"
  echo "| iOS | $apple_channel | $ios |"
  echo "| macOS | $apple_channel | $macos |"
  echo "| Web | $web_channel | $WEB |"
} > "$RUNNER_TEMP/channels.md"
# The pull requests since the stable version before this one, not since its last beta.
since=()
# Not this version's own tag, which a release deleted since, or a tag pushed by hand, leaves behind.
previous="$(git tag --merged "$SHA" | grep -E '^[0-9]+\.[0-9]+\.[0-9]+$' | grep -vxF "$VERSION" | sort -V | tail -1 || true)"
if [ -n "$previous" ]; then since=(--notes-start-tag "$previous"); fi
gh release create "$VERSION" \
  --target "$SHA" \
  --title "$VERSION" \
  --notes-file "$RUNNER_TEMP/channels.md" \
  --generate-notes "${since[@]}" \
  --latest
