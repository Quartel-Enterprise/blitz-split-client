#!/usr/bin/env bash
#
# Triggers the beta release workflow: no prompts, no menu.
#
# The workflow tags X.Y.Z-beta_N, publishes each platform to its channel (Android on Firebase App
# Distribution, the web on Cloudflare Pages, iOS and macOS on TestFlight) and publishes a GitHub
# pre-release. See docs/release-process.md.
#
# Requires the GitHub CLI (gh), installed and authenticated: https://cli.github.com
#
# Usage: ./scripts/release/release-beta.sh [X.Y.Z]
#   X.Y.Z  optional: overrides the versionName of version.properties
set -euo pipefail

REPO="Quartel-Enterprise/blitz-split-client"
WORKFLOW="release-beta.yml"
version="${1:-}"

echo "Bora Rachar: beta release"
echo "========================="

# GitHub keeps one pending run per concurrency group (one waiting behind the run in progress) and cancels it when
# another arrives, so a beta dispatched behind a waiting one would silently cancel it, after this script printed its
# link. A run that is only queued for a runner is not waiting on the group, and cancels nothing.
waiting="$(gh run list --workflow="$WORKFLOW" --repo "$REPO" --status pending --json databaseId --jq length)"
if [ "$waiting" -gt 0 ]; then
  echo "A beta is already waiting behind a running one. Wait for it to start, then run this again." >&2
  exit 1
fi

before="$(gh run list --workflow="$WORKFLOW" --repo "$REPO" --limit 1 --json databaseId --jq '.[0].databaseId // 0')"

# The workflow always builds main, so the workflow file is read from main too: a script run from another
# branch would otherwise dispatch that branch's copy of it against main's code.
gh workflow run "$WORKFLOW" --repo "$REPO" --ref main -f version="$version"

echo
echo "Workflow dispatched. Locating the run..."

url=""
for _ in $(seq 1 20); do
  sleep 3
  id="$(gh run list --workflow="$WORKFLOW" --repo "$REPO" --limit 1 --json databaseId --jq '.[0].databaseId // empty')"
  if [ -n "$id" ] && [ "$id" != "$before" ]; then
    url="$(gh run list --workflow="$WORKFLOW" --repo "$REPO" --limit 1 --json url --jq '.[0].url')"
    break
  fi
done

echo
if [ -n "$url" ]; then
  echo "Run: $url"
else
  echo "Dispatched. See: https://github.com/$REPO/actions/workflows/$WORKFLOW"
fi
