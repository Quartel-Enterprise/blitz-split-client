#!/usr/bin/env bash
# Resolves the version of a release: a beta's, or with --stable a stable version's.
#
# Usage: scripts/release/suggest-version.sh [--stable] [versionName]
#
# The version is the higher of two: the versionName of version.properties, the file every platform reads, and the
# one the release notes lead to (scripts/release/release-notes.mjs next-version: the last stable version, a patch
# higher when its fragments only fix, a minor higher when one of them is new or improved). The second is empty until
# a stable version is tagged, and the first is how a major version is declared, by hand. The argument (X.Y.Z)
# overrides both. Either way it has to be X.Y.Z, since it goes into the tag, the branch name and the Apple version
# string. A beta's number is one past the highest existing X.Y.Z-beta_N tag; a stable version is X.Y.Z as it is.
#
# The versionCode is one past the highest of three numbers: the one in version.properties, the "Build number: N"
# line every release, beta or stable, records in its notes, and the one the merge-back branch holds on the remote.
# The last two are what keep a release from reusing a code when the merge-back of the previous one did not reach
# main: a stable run that only reached a testing track or TestFlight publishes no release, and its code is then
# only on that branch, which stays until its pull request merges. The workflow hands the releases in a file
# (RELEASES_FILE: a JSON list, each with its `body`); without it they are read with the GitHub CLI and a token
# (GH_TOKEN), and run locally with neither, only the file and the branch count.
#
# Inside GitHub Actions, writes `version`, `display_version`, `version_code` and `branch` to $GITHUB_OUTPUT and a
# human-readable plan to $GITHUB_STEP_SUMMARY, which says so when a stable version is already tagged: the run then
# adds its builds to a version already released.
set -euo pipefail

cd "$(dirname "$0")/../.."

VERSION_FILE="version.properties"
SEMVER='^[0-9]+\.[0-9]+\.[0-9]+$'
stable=false
if [[ "${1:-}" == "--stable" ]]; then
  stable=true
  shift
fi
input_version="${1:-}"

if [[ -n "$input_version" && ! "$input_version" =~ $SEMVER ]]; then
  echo "Error: version '$input_version' must be in X.Y.Z format (e.g. 3.0.0)" >&2
  exit 1
fi

current_version="$(grep -E '^versionName=' "$VERSION_FILE" | cut -d= -f2 || true)"
current_code="$(grep -E '^versionCode=' "$VERSION_FILE" | cut -d= -f2 || true)"

if [[ -z "$current_version" || -z "$current_code" ]]; then
  echo "Error: could not read versionName/versionCode from ${VERSION_FILE}" >&2
  exit 1
fi

# A versionName someone left with a suffix (a local bump-version.sh run) would end up as 3.0.0-beta_7-beta_1.
if [[ ! "$current_version" =~ $SEMVER ]]; then
  echo "Error: the versionName '${current_version}' of ${VERSION_FILE} must be in X.Y.Z format (e.g. 3.0.0)" >&2
  exit 1
fi

# Captured first, so a fragment the script can't read stops the release instead of leaving the version at the file's.
from_notes="$(node scripts/release/release-notes.mjs next-version)"
# sort -V orders 3.10.0 after 3.9.0, as the version is read.
version="${input_version:-$(printf '%s\n' "$current_version" "$from_notes" | sort -V | tail -1)}"

version_code=$(( current_code + 1 ))

recorded=0
# Every release's notes, captured first, so a file that can't be read or an API or auth error stops the script
# instead of leaving `recorded` at 0 and the versionCode at the file's.
notes=""
if [[ -n "${RELEASES_FILE:-}" ]]; then
  notes="$(jq -r '.[] | .body // empty' "$RELEASES_FILE")"
elif command -v gh > /dev/null && [[ -n "${GH_TOKEN:-${GITHUB_TOKEN:-}}" ]]; then
  notes="$(gh api "repos/{owner}/{repo}/releases" --paginate --jq '.[] | .body // empty')"
fi
if [[ -n "$notes" ]]; then
  # The notes of a release edited on github.com have CRLF line ends.
  while read -r code; do
    if [[ "$code" =~ ^[0-9]+$ ]] && (( code > recorded )); then
      recorded="$code"
    fi
  done < <(tr -d '\r' <<< "$notes" | sed -n 's/^Build number: *//p')
fi
# One branch for every merge-back: a pull request a failed run left open is rebuilt by the next one, not
# left behind next to it.
branch="release/merge-back"
# Fetched now, so a branch deleted since (its pull request merged) isn't read from an old copy of it.
if git fetch --quiet origin "+refs/heads/$branch:refs/remotes/origin/$branch" 2> /dev/null; then
  pending="$(git show "refs/remotes/origin/$branch:$VERSION_FILE" | grep -E '^versionCode=' | cut -d= -f2 || true)"
  if [[ "$pending" =~ ^[0-9]+$ ]] && (( pending > recorded )); then
    recorded="$pending"
  fi
fi
if (( recorded >= version_code )); then
  version_code=$(( recorded + 1 ))
fi

tagged=false
if [[ "$stable" == true ]]; then
  display_version="$version"
  if [[ -n "$(git tag --list "$version")" ]]; then
    tagged=true
  fi
else
  # The beta number is one past the highest X.Y.Z-beta_N tag. The suffixed version goes into the tag, the GitHub
  # release and every platform's versionName; iosApp/generate-project.sh drops the suffix from Apple's version string.
  last_beta="$(git tag --list "${version}-beta_*" | sed "s/^${version}-beta_//" | grep -E '^[0-9]+$' | sort -n | tail -1 || true)"
  display_version="${version}-beta_$(( ${last_beta:-0} + 1 ))"
fi

echo "Resolved version: ${display_version} (versionCode ${version_code})"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "version=${version}"
    echo "display_version=${display_version}"
    echo "version_code=${version_code}"
    echo "branch=${branch}"
  } >> "$GITHUB_OUTPUT"
fi

if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  {
    if [[ "$stable" == true ]]; then
      echo "## Release plan"
    else
      echo "## Beta release plan"
    fi
    echo ""
    echo "| Field | Value |"
    echo "|---|---|"
    echo "| Tag and release | \`${display_version}\` |"
    if [[ "$stable" == true ]]; then
      echo "| Version name | \`${version}\` |"
    else
      echo "| Version name | \`${display_version}\` (Apple version string: \`${version}\`) |"
    fi
    echo "| Version code | \`${version_code}\` |"
    echo "| Merge-back branch | \`${branch}\` |"
    if [[ "$tagged" == true ]]; then
      echo ""
      echo "\`${version}\` is already tagged: this run adds its builds to the version already released."
    fi
  } >> "$GITHUB_STEP_SUMMARY"
fi
