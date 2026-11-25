#!/usr/bin/env bash
#
# Pick the release notes. Run by .github/workflows/release-beta.yml.
#
set -e

node scripts/release/release-notes.mjs collect "$DISPLAY_VERSION" \
  --out "$RUNNER_TEMP/release-notes" --releases "$RUNNER_TEMP/releases.json"
# A delimiter no note can hold, since the notes are text the pull requests wrote.
delimiter="notes_$(openssl rand -hex 16)"
{
  echo "github_notes<<${delimiter}"
  cat "$RUNNER_TEMP/release-notes/github.md"
  echo "$delimiter"
} >> "$GITHUB_OUTPUT"
for platform in android ios; do
  {
    echo "testers_${platform}<<${delimiter}"
    echo "$DISPLAY_VERSION (build $VERSION_CODE)"
    if [ -s "$RUNNER_TEMP/release-notes/testers/${platform}.txt" ]; then
      echo
      cat "$RUNNER_TEMP/release-notes/testers/${platform}.txt"
      echo
    fi
    echo "$delimiter"
  } >> "$GITHUB_OUTPUT"
done
if [ -s "$RUNNER_TEMP/release-notes/github.md" ]; then
  cat "$RUNNER_TEMP/release-notes/github.md" >> "$GITHUB_STEP_SUMMARY"
fi
