#!/usr/bin/env bash
#
# Pick the release notes. Run by .github/workflows/release-production.yml.
#
set -eo pipefail

node scripts/release/release-notes.mjs collect "$VERSION" --ref "$SHA" --site site \
  --out "$RUNNER_TEMP/release-notes"
# A delimiter no note can hold, since the notes are text the pull requests wrote.
delimiter="notes_$(openssl rand -hex 16)"
{
  echo "github_notes<<${delimiter}"
  cat "$RUNNER_TEMP/release-notes/github.md"
  echo "$delimiter"
} >> "$GITHUB_OUTPUT"
if [ -s "$RUNNER_TEMP/release-notes/github.md" ]; then
  cat "$RUNNER_TEMP/release-notes/github.md" >> "$GITHUB_STEP_SUMMARY"
fi
{
  echo ""
  echo "### What's new on Google Play, in English"
  echo ""
  echo '```'
  cat "$RUNNER_TEMP/release-notes/stores/play/en.txt"
  echo ""
  echo '```'
} >> "$GITHUB_STEP_SUMMARY"
