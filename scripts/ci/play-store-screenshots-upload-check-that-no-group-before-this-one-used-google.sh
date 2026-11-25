#!/usr/bin/env bash
#
# Check that no group before this one used Google Play's save quota up. Run by .github/workflows/play-store-screenshots-upload.yml.
#
set -eo pipefail

script=scripts/release/play-screenshot-groups.mjs
reached="$(node "$script" quota-reached)"
echo "reached=$reached" >> "$GITHUB_OUTPUT"
if [ "$reached" = true ]; then
  # Read into a variable first, so a script that fails stops the step instead of leaving a warning with
  # no words.
  warning="$(node "$script" warning --groups="$GROUP_MATRIX" --group="$GROUP")"
  echo "::warning title=Google Play's daily save quota is used up::$warning"
fi
