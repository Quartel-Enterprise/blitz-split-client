#!/usr/bin/env bash
#
# Says what a release does with the store listings' screenshots: the "Store screenshots" row of the plan's summary
# (release-production.yml) on stdout, and on stderr one GitHub warning per device marker that is on and adds no
# capture, which nothing else in the run would say. The slots come from app-store-capture-slots.sh, the list the
# capture job's matrix is made of, so the summary can't say something the matrix doesn't do.
#
# The App Store's screenshots are captured in a run with `screenshots` on that submits for review, on the slots that
# script lists (docs/release-process.md#the-devices-the-app-stores-captures-run-on).
#
# Usage: SCREENSHOTS=<true|false> SUBMIT=<true|false> IOS=<true|false> MACOS=<true|false> \
#   SCREENSHOTS_IPHONE_DUO=<true|false> SCREENSHOTS_MAC=<true|false> ./scripts/release/app-store-screenshots-plan.sh
set -euo pipefail

# The other four are checked by the script that lists the slots.
for name in SCREENSHOTS SUBMIT; do
  if [ "${!name:-}" != true ] && [ "${!name:-}" != false ]; then
    echo "Error: $name is '${!name:-}', neither true nor false" >&2
    exit 1
  fi
done
slots="$("$(dirname "$0")/app-store-capture-slots.sh")"

# What apple-screenshots-capture's `if` lets through: nothing without `screenshots`, or in a run that doesn't submit.
captured=""
if [ "$SCREENSHOTS" = true ] && [ "$SUBMIT" = true ]; then captured="$slots"; fi
captures() { [[ "$captured" == *"\"$1\""* ]]; }

row="left as they are"
if [ "$SCREENSHOTS" = true ]; then
  row="made again and uploaded with a release"
  if [ -n "$captured" ]; then
    row="$row. The App Store's are captured on $(jq -r 'map("`" + . + "`") | join(", ")' <<< "$captured")"
    # Only a device the run releases is one it leaves out.
    if [ "$IOS" = true ] && ! captures iphoneduo; then
      row="$row, without the iPhone Duo (\`screenshots_iphone_duo\`), so the iPhone's and the iPad's aren't uploaded"
    fi
    if [ "$MACOS" = true ] && ! captures mac; then
      row="$row, without the Mac (\`screenshots_mac\`)"
    fi
  elif [ "$IOS" = true ] || [ "$MACOS" = true ]; then
    if [ "$SUBMIT" != true ]; then
      row="$row. None of the App Store's is: they only go up in a run that submits for review"
    else
      # The one run that submits and could upload none of them: the Mac alone, without its marker.
      row="$row. None of the App Store's is: the Mac's capture is behind \`screenshots_mac\`"
    fi
  fi
elif [ "$SCREENSHOTS_IPHONE_DUO" = true ] || [ "$SCREENSHOTS_MAC" = true ]; then
  row="left as they are: \`screenshots\` is off, and the devices' markers only count with it"
fi

if [ "$SCREENSHOTS_IPHONE_DUO" = true ] && ! captures iphoneduo; then
  echo "::warning title=The iPhone Duo is not captured::screenshots_iphone_duo is on, but the iPhone Duo's screenshots are only made with screenshots on, in a run that submits iOS for review." >&2
fi
if [ "$SCREENSHOTS_MAC" = true ] && ! captures mac; then
  echo "::warning title=The Mac is not captured::screenshots_mac is on, but the Mac's screenshots are only made with screenshots on, in a run that submits macOS for review." >&2
fi

echo "$row"
