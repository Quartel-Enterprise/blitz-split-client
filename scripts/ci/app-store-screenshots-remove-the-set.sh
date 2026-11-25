#!/usr/bin/env bash
#
# Remove the set. Run by .github/workflows/app-store-screenshots.yml.
#
set -eo pipefail

errors="$RUNNER_TEMP/release-view-errors.txt"
if ! release="$(gh release view "$SET_TAG" --json isDraft,assets 2> "$errors")"; then
  if ! grep -qi "release not found" "$errors"; then
    cat "$errors"
    echo "::error::Could not read the draft release $SET_TAG, so nothing says whether it is still there. Run this job again."
    exit 1
  fi
  echo "The draft release \`$SET_TAG\` is already gone." >> "$GITHUB_STEP_SUMMARY"
elif [ "$(jq -r .isDraft <<< "$release")" != true ]; then
  echo "::error::$SET_TAG is a published release, not a set: left alone."
  exit 1
else
  sent=()
  if [ "$IOS" = true ]; then sent+=(ios.tar); fi
  if [ "$MACOS" = true ]; then sent+=(mac.tar); fi
  unsent="$(jq -r '[.assets[].name | select(endswith(".tar"))] - $ARGS.positional | join(" and ")' --args "${sent[@]}" <<< "$release")"
  if [ -z "$unsent" ]; then
    gh release delete "$SET_TAG" --yes
    echo "Every locale landed, so the set was removed: the draft release \`$SET_TAG\` is deleted." >> "$GITHUB_STEP_SUMMARY"
  else
    for archive in "${sent[@]}"; do
      if jq -e --arg name "$archive" 'any(.assets[]; .name == $name)' <<< "$release" > /dev/null; then
        gh release delete-asset "$SET_TAG" "$archive" --yes
      fi
    done
    echo "Every locale landed, and what went up was removed from the set. **The draft release \`$SET_TAG\` was kept**: it still holds $unsent, the version this run left out. Dispatch again for that one alone, or drop it with \`gh release delete $SET_TAG --yes\`." >> "$GITHUB_STEP_SUMMARY"
  fi
fi
{
  echo
  echo "**Next:** look at the shelves in App Store Connect, then dispatch the stable release with \`screenshots\` off: it submits these versions with the screenshots they now have (docs/release-process.md#screenshots-made-on-a-mac)."
} >> "$GITHUB_STEP_SUMMARY"
