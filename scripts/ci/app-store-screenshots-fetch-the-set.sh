#!/usr/bin/env bash
#
# Fetch the set. Run by .github/workflows/app-store-screenshots.yml.
#
set -eo pipefail

if ! release="$(gh release view "$SET_TAG" --json isDraft,assets)"; then
  echo "::error::No set named $SET: no draft release $SET_TAG. Send it from the Mac first, with node scripts/store-screenshots/local-set.mjs send."
  exit 1
fi
if [ "$(jq -r .isDraft <<< "$release")" != true ]; then
  echo "::error::$SET_TAG is a published release, not a set."
  exit 1
fi
set_dir="$RUNNER_TEMP/set"
mkdir -p "$set_dir"
fetch() {
  local archive="$1" folder="$2" input="$3"
  if ! jq -e --arg name "$archive" 'any(.assets[]; .name == $name)' <<< "$release" > /dev/null; then
    echo "::error::The set $SET has no $archive: it was sent without that platform. Dispatch with $input off."
    exit 1
  fi
  gh release download "$SET_TAG" --dir "$set_dir" --pattern "$archive"
  if ! grep -F "  $archive" "$set_dir/SHA256SUMS" > "$set_dir/$archive.sha256"; then
    echo "::error::The set's SHA256SUMS doesn't list $archive."
    exit 1
  fi
  (cd "$set_dir" && sha256sum --check "$archive.sha256")
  # Nothing but images in a locale's folder is written on the runner, which is all local-set.mjs packs:
  # no link, no folder of its own, no path that leaves the staged folder.
  odd="$(tar -tvf "$set_dir/$archive" | grep -v '^-' || true)"
  misplaced="$(tar -tf "$set_dir/$archive" | grep -vE '^[A-Za-z]+(-[A-Za-z]+)*/[A-Za-z0-9_]+\.png$' || true)"
  if [ -n "$odd" ] || [ -n "$misplaced" ]; then
    printf '%s\n' "$odd" "$misplaced"
    echo "::error::$archive holds something that isn't <locale>/<image>.png, so local-set.mjs didn't pack it. Nothing was extracted."
    exit 1
  fi
  mkdir -p "$folder"
  tar -xf "$set_dir/$archive" -C "$folder"
}
gh release download "$SET_TAG" --dir "$set_dir" --pattern SHA256SUMS
# A set sent with --devices holds those shelves alone: dispatched without them (allow_missing_devices) the
# upload would empty every other shelf, so the run names the same ones (DEVICES_LINE of local-set.mjs).
sent="$(sed -n 's/^# devices: //p' "$set_dir/SHA256SUMS")"
if [ "$sent" != "$DEVICES" ]; then
  echo "::error::The set $SET was sent for the shelves '${sent:-every one}', and the run names '${DEVICES:-every one}': dispatch it as local-set.mjs printed, with devices '$sent'."
  exit 1
fi
if [ "$IOS" = true ]; then fetch ios.tar fastlane/screenshots ios; fi
if [ "$MACOS" = true ]; then fetch mac.tar fastlane/screenshots-mac macos; fi
