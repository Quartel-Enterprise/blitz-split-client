#!/usr/bin/env bash
# Merges what a release consumed back into main, through a pull request: the versionCode, the versionName when the
# release's is higher than main's, and for a stable version the release notes' fragments it told.
#
# Usage: scripts/release/merge-back.sh <version> <versionCode> <displayVersion> [<tag>]
#   version         X.Y.Z, the version the release used
#   versionCode     the build number it consumed
#   displayVersion  the release's name: X.Y.Z-beta_N for a beta, X.Y.Z for a stable version
#   tag             a stable version only: its tag, whose commit's fragments leave release-notes/unreleased/
#
# Run from a checkout of main with its history. Needs the GitHub CLI and, in the environment:
#   GH_TOKEN     creates the pull request. The workflow's GITHUB_TOKEN, so no pull request workflow runs for a bump
#   MERGE_TOKEN  optional, merges it: the MERGE_WHEN_GREEN_TOKEN, so the push to main triggers its workflows
#                (docs/ci.md, "Merge when green")
#   BRANCH       the merge-back branch, release/merge-back
#   ACTOR        optional, who dispatched the release: the pull request's assignee
#
# A re-run finishes the job wherever the last one stopped:
#   - nothing is ever lowered: a versionCode or a versionName someone raised on main meanwhile stays;
#   - main already holding everything is the end of it;
#   - the branch is rebuilt from the current main and pushed over the old one, so a pull request left open by a
#     failed run follows main instead of staying in conflict;
#   - the pull request is created only when none is open.
set -euo pipefail

version="${1:?version required (e.g. 3.1.0)}"
version_code="${2:?versionCode required (e.g. 84)}"
display_version="${3:?displayVersion required (e.g. 3.1.0-beta_2)}"
tag="${4:-}"
branch="${BRANCH:?BRANCH required (the merge-back branch)}"

cd "$(dirname "$0")/../.."

FRAGMENTS_DIR="release-notes/unreleased"

git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
git fetch --quiet origin "+refs/heads/$branch:refs/remotes/origin/$branch" 2> /dev/null || true
git checkout --quiet -B "$branch"

main_version="$(grep -E '^versionName=' version.properties | cut -d= -f2)"
main_code="$(grep -E '^versionCode=' version.properties | cut -d= -f2)"
# sort -V orders 3.10.0 after 3.9.0, as the version is read.
new_version="$(printf '%s\n' "$main_version" "$version" | sort -V | tail -1)"
new_code="$(( main_code > version_code ? main_code : version_code ))"
./scripts/release/bump-version.sh "$new_version" "$new_code"

# The fragments the version told are the ones its commit had, not the ones merged since. One a later pull request
# added a note to stays: the release already read its other notes, and its new one waits for the next version
# (docs/release-notes.md).
removed=0
if [ -n "$tag" ]; then
  git fetch --quiet origin "+refs/tags/$tag:refs/tags/$tag"
  commit="$(git rev-parse "refs/tags/$tag^{commit}")"
  while IFS= read -r path; do
    [ -f "$path" ] || continue
    if git diff --quiet "$commit" HEAD -- "$path"; then
      git rm --quiet "$path"
      removed=$(( removed + 1 ))
    else
      echo "::notice::$path changed since $tag: it stays for the next version."
    fi
  done < <(git ls-tree --name-only "$commit" "$FRAGMENTS_DIR/")
fi

if [ -z "$(git status --porcelain)" ]; then
  echo "::notice::main already holds version $main_version and versionCode $main_code, nothing to merge back."
  exit 0
fi

git commit --quiet -am "chore: bump the version to $new_version (versionCode $new_code) for $display_version"
body="Merges the version $new_version and the versionCode $new_code that $display_version used back into main."
if [ -n "$tag" ]; then
  body="$body Removes the $removed release notes' fragment(s) it told from \`$FRAGMENTS_DIR/\`."
fi
git push --quiet --force-with-lease origin "$branch"

title="chore: merge back $display_version into main"
if [ -z "$(gh pr list --head "$branch" --state open --json number --jq '.[0].number // empty')" ]; then
  gh pr create --base main --head "$branch" --title "$title" --body "$body"
else
  # Left open by an earlier release: the branch now holds this one's changes, so the text says so too.
  gh pr edit "$branch" --title "$title" --body "$body"
fi
# Best effort: GitHub refuses an assignee it can't assign (a bot, an app), and the merge must not depend on it.
if [ -n "${ACTOR:-}" ]; then
  gh pr edit "$branch" --add-assignee "$ACTOR" || true
fi

# GitHub recomputes mergeability asynchronously, so the merge right after the create can still read the pull
# request as blocked. Retry instead of failing the release on a race that resolves itself in seconds.
for _ in 1 2 3 4 5; do
  if GH_TOKEN="${MERGE_TOKEN:-$GH_TOKEN}" gh pr merge "$branch" --squash --delete-branch; then
    exit 0
  fi
  sleep "${MERGE_RETRY_SECONDS:-10}"
done
exit 1
