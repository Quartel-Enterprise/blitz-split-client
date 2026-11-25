#!/usr/bin/env bash
#
# Read the releases. Run by .github/workflows/release-beta.yml.
#
set -eo pipefail

# pipefail, so a gh that fails stops the step: without it jq -s would write an empty list, and
# suggest-version.sh would resolve the versionCode as if no release had recorded one.
gh api "repos/$GITHUB_REPOSITORY/releases" --paginate \
  --jq '.[] | {tag: .tag_name, body: .body, prerelease: .prerelease}' | jq -s . > "$RUNNER_TEMP/releases.json"
