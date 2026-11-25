#!/usr/bin/env bash
#
# Say which locales landed. Run by .github/workflows/app-store-screenshots.yml.
#
set -eo pipefail

jobs="$RUNNER_TEMP/jobs.json"
gh api --paginate "repos/$GITHUB_REPOSITORY/actions/runs/$GITHUB_RUN_ID/jobs?filter=latest&per_page=100" \
  --jq '.jobs[] | {name, conclusion}' | jq -s . > "$jobs"
options=(--jobs "$jobs")
if [ "$TIDY_ONLY" = true ]; then
  # No locale was sent: the report is the tidy-up's alone.
  options+=(--tidy-only)
  IOS_LOCALES='[]'
  MAC_LOCALES='[]'
else
  options+=(--set "$SET")
fi
if [ -n "$LOCALES" ]; then options+=(--only "$LOCALES"); fi
if [ -n "$APP_VERSION" ]; then options+=(--version "$APP_VERSION"); fi
if [ "$IOS" = true ]; then options+=(--ios-job "iPhone and iPad" --ios-locales "$IOS_LOCALES" --ios-shelves "$IOS_SHELVES"); fi
if [ "$MACOS" = true ]; then options+=(--mac-job "Mac" --mac-locales "$MAC_LOCALES" --mac-shelves "$MAC_SHELVES"); fi
node scripts/store-screenshots/local-set.mjs report "${options[@]}" | tee -a "$GITHUB_STEP_SUMMARY"
