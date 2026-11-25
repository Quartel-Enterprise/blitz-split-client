#!/usr/bin/env bash
#
# Check the secrets and the Pages project. Run by .github/actions/web-deploy/action.yml.
#
set -eo pipefail

./scripts/release/require-env.sh CLOUDFLARE_API_TOKEN CLOUDFLARE_ACCOUNT_ID
project="$RUNNER_TEMP/pages-project.json"
# A curl that fails (no answer after its retries) leaves 000, which the last case below explains.
status="$(curl --silent --show-error --retry 3 --max-time 30 --output "$project" --write-out '%{http_code}' \
  --header "Authorization: Bearer $CLOUDFLARE_API_TOKEN" \
  "https://api.cloudflare.com/client/v4/accounts/$CLOUDFLARE_ACCOUNT_ID/pages/projects/$PAGES_PROJECT")" || true
case "$status" in
  200) ;;
  404)
    echo "::error::The Cloudflare Pages project '$PAGES_PROJECT' doesn't exist in the account of CLOUDFLARE_ACCOUNT_ID. Create it once, then re-run this job: see docs/release-process.md#the-production-web-app."
    exit 1
    ;;
  400 | 401 | 403)
    jq -r '.errors[]?.message' "$project" 2> /dev/null || true
    echo "::error::Cloudflare refused the request for the Pages project '$PAGES_PROJECT' (HTTP $status): check the CLOUDFLARE_API_TOKEN and CLOUDFLARE_ACCOUNT_ID secrets. See docs/release-process.md#secrets."
    exit 1
    ;;
  *)
    jq -r '.errors[]?.message' "$project" 2> /dev/null || true
    echo "::error::Cloudflare didn't say whether the Pages project '$PAGES_PROJECT' exists (HTTP ${status:-000}, 000 being no answer at all), which says nothing about the secrets: re-run this job."
    exit 1
    ;;
esac
# The deploy below goes to main: on a project with another production branch it would only be a preview,
# which no domain serves, and the job would still pass.
branch="$(jq -r '.result.production_branch // empty' "$project")"
if [ "$branch" != main ]; then
  echo "::error::The Pages project '$PAGES_PROJECT' has '$branch' as its production branch, and a release deploys to main: it would only be a preview. Set the project's production branch to main: see docs/release-process.md#the-production-web-app."
  exit 1
fi
# Only a warning: the deployment is still made, on the project's own pages.dev address, which is counted
# with its domains in case Cloudflare lists only the custom ones.
host="${URL#https://}"
host="${host%%/*}"
domains='((.result.domains // []) + [.result.subdomain // empty]) | unique'
if ! jq -e --arg host "$host" "$domains | index(\$host)" "$project" > /dev/null; then
  echo "::warning title=Web::$host is not one of the domains of the Pages project '$PAGES_PROJECT' ($(jq -r "$domains | join(\", \")" "$project")): the deployment isn't served there until it is added. See docs/release-process.md#the-production-web-app."
fi
