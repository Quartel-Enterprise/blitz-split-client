#!/usr/bin/env bash
#
# Publish the GitHub pre-release. Run by .github/workflows/release-beta.yml.
#
set -e

sent=""
if [ "$ANDROID" = success ]; then sent="android"; fi
if [ "$IOS_FIREBASE" = success ]; then sent="${sent:+$sent, }ios"; fi
sent="${sent:-none}"
if gh release view "$DISPLAY_VERSION" > /dev/null 2>&1; then
  gh release view "$DISPLAY_VERSION" --json body --jq .body | tr -d '\r' | awk \
    -v android="$ANDROID" -v web="$WEB" -v ios="$IOS" -v ios_firebase="$IOS_FIREBASE" -v macos="$MACOS" \
    -v pages="$CLOUDFLARE_PAGES_PROJECT" -v sent="$sent" '
    /^Sent to testers:/ { print "Sent to testers: " sent; next }
    /^\| Android \|/ { print "| Android | Firebase App Distribution, group `beta` | " android " |"; next }
    /^\| Web \|/     { print "| Web | Cloudflare Pages, https://" pages ".pages.dev | " web " |"; next }
    /^\| iOS \|/     { print "| iOS | TestFlight | " ios " |"; next }
    /^\| iOS, ad hoc \|/ { print "| iOS, ad hoc | Firebase App Distribution, group `ios-beta` | " ios_firebase " |"; next }
    /^\| macOS \|/   { print "| macOS | TestFlight | " macos " |"; next }
    { print }' > "$RUNNER_TEMP/notes.md"
  gh release edit "$DISPLAY_VERSION" --notes-file "$RUNNER_TEMP/notes.md"
  echo "::notice::$DISPLAY_VERSION is already published: its table of channels was refreshed."
  exit 0
fi
{
  echo "Build number: $VERSION_CODE"
  echo "Sent to testers: $sent"
  echo
  if [ -n "$NOTES" ]; then
    printf '%s\n\n' "$NOTES"
  fi
} > "$RUNNER_TEMP/channels.md"
cat >> "$RUNNER_TEMP/channels.md" <<EOF
## Channels

| Platform | Channel | Result |
| --- | --- | --- |
| Android | Firebase App Distribution, group \`beta\` | $ANDROID |
| Web | Cloudflare Pages, https://${CLOUDFLARE_PAGES_PROJECT}.pages.dev | $WEB |
| iOS | TestFlight | $IOS |
| iOS, ad hoc | Firebase App Distribution, group \`ios-beta\` | $IOS_FIREBASE |
| macOS | TestFlight | $MACOS |
EOF
gh release create "$DISPLAY_VERSION" \
  --target "$SHA" \
  --title "$DISPLAY_VERSION" \
  --notes-file "$RUNNER_TEMP/channels.md" \
  --generate-notes \
  --prerelease
