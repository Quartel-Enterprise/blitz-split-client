#!/usr/bin/env bash
# Resolves the version of a beta release.
#
# Usage: scripts/release/suggest-beta-version.sh [versionName]
#
# The version is the versionName of version.properties, the single source shared by every platform,
# unless the argument (X.Y.Z) overrides it. The beta number is one past the highest existing
# X.Y.Z-beta_N tag, and the versionCode is the file's plus one: each beta consumes a code that the
# release workflow merges back into main, so the next beta, and the store build after it, are always
# higher.
#
# Inside GitHub Actions, writes `version`, `display_version`, `version_code` and `branch` to
# $GITHUB_OUTPUT and a human-readable plan to $GITHUB_STEP_SUMMARY.
set -euo pipefail

cd "$(dirname "$0")/../.."

VERSION_FILE="version.properties"
input_version="${1:-}"

if [[ -n "$input_version" && ! "$input_version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Error: version '$input_version' must be in X.Y.Z format (e.g. 3.0.0)" >&2
  exit 1
fi

current_version="$(grep -E '^versionName=' "$VERSION_FILE" | cut -d= -f2 || true)"
current_code="$(grep -E '^versionCode=' "$VERSION_FILE" | cut -d= -f2 || true)"

if [[ -z "$current_version" || -z "$current_code" ]]; then
  echo "Error: could not read versionName/versionCode from ${VERSION_FILE}" >&2
  exit 1
fi

version="${input_version:-$current_version}"
version_code=$(( current_code + 1 ))

# Apple refuses a suffixed version string, so only the tag, the GitHub release and the Android
# versionName carry the suffix.
last_beta="$(git tag --list "${version}-beta_*" | sed "s/^${version}-beta_//" | grep -E '^[0-9]+$' | sort -n | tail -1 || true)"
display_version="${version}-beta_$(( ${last_beta:-0} + 1 ))"
branch="release/${display_version}"

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
    echo "## Beta release plan"
    echo ""
    echo "| Field | Value |"
    echo "|---|---|"
    echo "| Tag and release | \`${display_version}\` |"
    echo "| Version name | \`${version}\` (Android: \`${display_version}\`) |"
    echo "| Version code | \`${version_code}\` |"
    echo "| Merge-back branch | \`${branch}\` |"
  } >> "$GITHUB_STEP_SUMMARY"
fi
