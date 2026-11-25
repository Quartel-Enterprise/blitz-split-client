#!/usr/bin/env bash
#
# Deploy to Cloudflare Pages. Run by .github/actions/web-deploy/action.yml.
#
set -eo pipefail

npx --yes "wrangler@${WRANGLER_VERSION}" pages deploy webApp/dist \
  --project-name "$PAGES_PROJECT" \
  --branch main \
  --commit-dirty=true \
  --commit-hash "$SHA" \
  --commit-message "$VERSION"
{
  echo "### Web"
  echo ""
  echo "$URL"
} >> "$GITHUB_STEP_SUMMARY"
