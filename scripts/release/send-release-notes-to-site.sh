#!/usr/bin/env bash
# Sends a version's release notes to blitz-split-site: writes release-notes/<language>/<version>.json in a checkout
# of it, from the fragments of the commit the version was built from, and merges them through a pull request. The
# site deploys on that merge, which is what puts the notes on its "What's new" page and in the feed the apps read.
#
# Usage: scripts/release/send-release-notes-to-site.sh <version> <commit> <site checkout>
#   version        e.g. 3.1.0 or 3.1.0-beta_2
#   commit         the commit the version was built from
#   site checkout  a clone of blitz-split-site on its main, whose `origin` can be pushed to
#
# Needs the GitHub CLI and GH_TOKEN, a token that can write to blitz-split-site (the SITE_RELEASE_NOTES_TOKEN secret:
# docs/release-process.md#secrets). It can be run again: a version the site already has is left as it is, whether an
# earlier run sent it or someone wrote it by hand, and a pull request an earlier run left open is merged.
set -euo pipefail

version="${1:?version required (e.g. 3.1.0-beta_2)}"
sha="${2:?the commit the version was built from}"
site="$(cd "${3:?path of the blitz-split-site checkout}" && pwd)"

cd "$(dirname "$0")/../.."

branch="release-notes/${version}"

# Only a version that was released: finalize tags it. Notes sent for a version with no tag would make the site
# refuse the same version's notes later, when the release is made after all, with other notes.
if ! git rev-parse --quiet --verify "refs/tags/${version}" > /dev/null; then
  echo "::error::${version} has no tag: it was never released. Re-run finalize first, then this job."
  exit 1
fi

if [ -f "$site/release-notes/en/${version}.json" ]; then
  echo "::notice::The site already has the release notes of ${version}: nothing to send."
  exit 0
fi

node scripts/release/release-notes.mjs collect "$version" --ref "$sha" --site "$site"

if [ -z "$(git -C "$site" status --porcelain -- release-notes)" ]; then
  echo "::notice::${version} has no release note to send."
  exit 0
fi

# The site's own check, before anything is pushed: the same one its pull requests run. From its folder, so the
# files it reads are the site's whatever the script takes its paths from.
(cd "$site" && node scripts/build-release-notes.mjs --check)

git -C "$site" config user.name "github-actions[bot]"
git -C "$site" config user.email "41898282+github-actions[bot]@users.noreply.github.com"
git -C "$site" checkout -B "$branch"
git -C "$site" add release-notes
git -C "$site" commit -m "chore: release notes of ${version}"
# Over a branch an earlier run left behind: its notes are these, written again.
git -C "$site" push --force origin "$branch"

cd "$site"
title="chore: release notes of ${version}"
body="What ${version} changed, in every language, written by the release of blitz-split-client from the fragments its pull requests left in \`release-notes/unreleased/\`. Merging it deploys the site, which puts the notes on the \"What's new\" page and in the feed the apps read."
if [ -z "$(gh pr list --head "$branch" --state open --json number --jq '.[0].number // empty')" ]; then
  gh pr create --base main --head "$branch" --title "$title" --body "$body"
fi

# GitHub recomputes mergeability asynchronously, so the merge right after the create can still read the pull
# request as blocked. Retry instead of failing on a race that resolves itself in seconds.
for _ in 1 2 3 4 5; do
  if gh pr merge "$branch" --squash --delete-branch; then
    exit 0
  fi
  sleep 10
done
exit 1
