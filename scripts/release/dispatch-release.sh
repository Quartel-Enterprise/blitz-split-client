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

# A stable release submits the App Store versions, and a version waiting for review takes no screenshot. Dispatched
# while a set made on a Mac is still going up, one locale per job (app-store-screenshots.yml), it would leave the
# locales still to go without theirs. That workflow refuses to start during a release; this is the other way round,
# which the Actions tab's "Run workflow" doesn't check either. A beta submits nothing, so it goes on.
if [ "$workflow" = release-production.yml ]; then
  # Every status of a run that isn't over: between two of its jobs it reads as waiting, on the environment's rule.
  for status in in_progress queued waiting pending requested; do
    # A list that can't be read is not an empty one: dispatching then would be a guess.
    if ! uploading="$(gh run list --workflow=app-store-screenshots.yml --repo "$REPO" --status "$status" --json databaseId --jq length)"; then
      echo "Could not see whether App Store screenshots are going up (app-store-screenshots.yml). Run this again." >&2
      exit 1
    fi
    if [ "${uploading:-0}" -gt 0 ]; then
      echo "App Store screenshots are still going up (app-store-screenshots.yml has a run $status), and the release would submit the versions before the last locale landed. Wait for that run to end, then run this again." >&2
      exit 1
    fi
  done
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
