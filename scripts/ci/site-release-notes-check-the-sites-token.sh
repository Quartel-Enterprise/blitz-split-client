#!/usr/bin/env bash
#
# Check the site's token. Run by .github/workflows/site-release-notes.yml.
#
set -e

./scripts/release/require-env.sh SITE_RELEASE_NOTES_TOKEN
if ! GH_TOKEN="$SITE_RELEASE_NOTES_TOKEN" gh api "repos/$SITE_REPOSITORY" --silent; then
  echo "::error::GitHub refused the SITE_RELEASE_NOTES_TOKEN secret: it expired or can't reach $SITE_REPOSITORY. See docs/release-process.md#secrets."
  exit 1
fi
