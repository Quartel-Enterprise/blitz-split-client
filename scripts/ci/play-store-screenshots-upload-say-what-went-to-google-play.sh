#!/usr/bin/env bash
#
# Say what went to Google Play. Run by .github/workflows/play-store-screenshots-upload.yml.
#
set -eo pipefail

script=scripts/release/play-screenshot-groups.mjs
node "$script" summary --groups="$GROUP_MATRIX" --group="$GROUP" --sent="$SENT" --unchanged="$UNCHANGED" \
  --not-sent="$NOT_SENT" --quota="$SAVE_QUOTA" --validate-only="$VALIDATE_ONLY" >> "$GITHUB_STEP_SUMMARY"
if [ "$SAVE_QUOTA" = reached ]; then
  warning="$(node "$script" warning --groups="$GROUP_MATRIX" --group="$GROUP" --not-sent="$NOT_SENT")"
  echo "::warning title=Google Play's daily save quota is used up::$warning"
  mkdir -p "$RUNNER_TEMP/play-save-quota"
  echo "$GROUP" > "$RUNNER_TEMP/play-save-quota/group"
fi
