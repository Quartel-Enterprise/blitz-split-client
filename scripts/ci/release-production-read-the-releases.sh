#!/usr/bin/env bash
#
# Read the releases. Run by .github/workflows/release-production.yml.
#
set -eo pipefail

gh api "repos/$GITHUB_REPOSITORY/releases" --paginate \
  --jq '.[] | {tag: .tag_name, body: .body, prerelease: .prerelease}' | jq -s . > "$RUNNER_TEMP/releases.json"
