#!/usr/bin/env bash
#
# Dispatches a release workflow on main and prints the link to its run. release-beta.sh and release-production.sh
# call it; see docs/release-process.md.
#
# The two release workflows share one concurrency group, since both take the next versionCode from the same place.
# GitHub keeps one pending run per group (one waiting behind the run in progress) and cancels it when another
# arrives, so a release dispatched behind a waiting one would silently cancel it. This refuses to dispatch then,
# which the Actions tab's "Run workflow" doesn't. A run that is only queued for a runner is not waiting on the
# group, and cancels nothing.
#
# Requires the GitHub CLI (gh), installed and authenticated: https://cli.github.com
#
# Usage: scripts/release/dispatch-release.sh <workflow file> [gh workflow run options...]
set -euo pipefail

REPO="Quartel-Enterprise/blitz-split-client"
GROUP_WORKFLOWS=("release-beta.yml" "release-production.yml")
workflow="${1:?workflow file required (e.g. release-beta.yml)}"
shift

for each in "${GROUP_WORKFLOWS[@]}"; do
  if ! waiting="$(gh run list --workflow="$each" --repo "$REPO" --status pending --json databaseId --jq length)"; then
    # gh fails on a workflow main doesn't have, which then has no run waiting; on the one asked for, it is an error.
    if [ "$each" = "$workflow" ]; then
      exit 1
    fi
    continue
  fi
  if [ "$waiting" -gt 0 ]; then
    echo "A release is already waiting behind a running one. Wait for it to start, then run this again." >&2
    exit 1
  fi
done

# Fails, saying why, while a workflow that sends a store's screenshots has a run that isn't over, whatever it is
# waiting for. A list that can't be read is not an empty one: dispatching then would be a guess. A workflow main
# doesn't have is the exception, as in the loop above: it has no run going, and a release can't wait for one.
refuse_while_going() {
  local sending="$1" what="$2" why="$3" going errors
  errors="$(mktemp)"
  if ! going="$(gh run list --workflow="$sending" --repo "$REPO" --limit 30 --json status --jq '[.[] | select(.status != "completed")] | length' 2> "$errors")"; then
    if grep -q "not found on the default branch" "$errors"; then
      rm -f "$errors"
      return 0
    fi
    cat "$errors" >&2
    rm -f "$errors"
    echo "Could not see whether $what are going up ($sending). Run this again." >&2
    exit 1
  fi
  rm -f "$errors"
  if [ "${going:-0}" -gt 0 ]; then
    echo "$what are still going up (a run of $sending isn't over), and $why. Wait for that run to end, then run this again." >&2
    exit 1
  fi
}

# A stable release submits the App Store versions, and a version waiting for review takes no screenshot. Dispatched
# while a set made on a Mac is still going up, one locale per job (app-store-screenshots.yml), it would leave the
# locales still to go without theirs. It also sends its bundle to Google Play through an edit, as each locale of the
# listing's screenshots does when they go alone (play-store-screenshots.yml), and the newer edit drops the one still
# open. The release's own first job refuses the run then; this says so before anything is dispatched, whatever the
# release's inputs: the upload jobs of both stop while any stable release is going, so one that reaches neither store
# would still fail the locales still to go. A beta is dispatched as it always was: it reaches neither Google Play's
# listing nor its tracks, so Google Play's upload goes on beside it, while the App Store's upload jobs do stop during
# one (app-store-screenshots-upload.yml), which this script doesn't look at.
if [ "$workflow" = release-production.yml ]; then
  refuse_while_going app-store-screenshots.yml "App Store screenshots" \
    "the release would submit the versions before the last locale landed"
  refuse_while_going play-store-screenshots.yml "Google Play screenshots" \
    "the release's bundle and a locale's screenshots would drop each other's Play edit"
fi

before="$(gh run list --workflow="$workflow" --repo "$REPO" --limit 1 --json databaseId --jq '.[0].databaseId // 0')"

# The workflows always build main, so the workflow file is read from main too: a script run from another
# branch would otherwise dispatch that branch's copy of it against main's code.
gh workflow run "$workflow" --repo "$REPO" --ref main "$@"

echo
echo "Workflow dispatched. Locating the run..."

url=""
for _ in $(seq 1 20); do
  sleep 3
  id="$(gh run list --workflow="$workflow" --repo "$REPO" --limit 1 --json databaseId --jq '.[0].databaseId // empty')"
  if [ -n "$id" ] && [ "$id" != "$before" ]; then
    url="$(gh run list --workflow="$workflow" --repo "$REPO" --limit 1 --json url --jq '.[0].url')"
    break
  fi
done

echo
if [ -n "$url" ]; then
  echo "Run: $url"
else
  echo "Dispatched. See: https://github.com/$REPO/actions/workflows/$workflow"
fi
