#!/usr/bin/env bash
#
# Prints the App Store slots a release captures its screenshots on, as a JSON array
# (["iphone63","iphone67","ipad13"]): the matrix of release-production.yml's apple-screenshots-capture, one Mac runner
# per slot, so a device left out starts no runner at all. The slots are scripts/store-screenshots/capture-ios.sh's,
# plus `mac` for capture-mac.sh.
#
# A slot is captured only when the run can upload its images, and each device the runners can't capture yet is
# behind a marker of the workflow, off unless asked for
# (docs/release-process.md#the-devices-the-app-stores-captures-run-on):
#   - the iPhone Duo's two, closed and open, with `screenshots_iphone_duo`, in a run that releases iOS, whose listing
#     they are part of;
#   - the Mac's with `screenshots_mac`, in a run that releases macOS;
#   - the iPhone's two sizes and the iPad's whenever anything of the App Store's is made, with no marker: they are
#     the iOS listing's own, and the art of every device, the Mac's too, can't be drawn without them
#     (listing-art/frame-app-store.sh refuses).
#
# Prints nothing when no slot is captured, a run of the Mac alone without its marker: an empty array would fail the
# matrix, and the job skips on an empty answer.
#
# Usage: IOS=<true|false> MACOS=<true|false> SCREENSHOTS_IPHONE_DUO=<true|false> SCREENSHOTS_MAC=<true|false> \
#   ./scripts/release/app-store-capture-slots.sh
set -euo pipefail

# Anything but true or false is a typo that would read as off, and the release would go without a device it asked for.
for name in IOS MACOS SCREENSHOTS_IPHONE_DUO SCREENSHOTS_MAC; do
  if [ "${!name:-}" != true ] && [ "${!name:-}" != false ]; then
    echo "Error: $name is '${!name:-}', neither true nor false" >&2
    exit 1
  fi
done

slots=()
if [ "$IOS" = true ] && [ "$SCREENSHOTS_IPHONE_DUO" = true ]; then slots+=(iphoneduo iphoneduoopen); fi
if [ "$MACOS" = true ] && [ "$SCREENSHOTS_MAC" = true ]; then slots+=(mac); fi
if [ "$IOS" = true ] || [ "${#slots[@]}" -gt 0 ]; then slots=(iphone63 iphone67 ipad13 ${slots[@]+"${slots[@]}"}); fi

if [ "${#slots[@]}" -gt 0 ]; then
  printf '%s\n' "${slots[@]}" | jq -Rcn '[inputs]'
fi
