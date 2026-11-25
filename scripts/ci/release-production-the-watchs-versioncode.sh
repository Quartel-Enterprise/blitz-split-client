#!/usr/bin/env bash
#
# The watch's versionCode. Run by .github/workflows/release-production.yml.
#
set -eo pipefail

offset="$(sed -n 's/^val wearVersionCodeOffset = \([0-9_]*\)$/\1/p' wearApp/build.gradle.kts | tr -d _)"
if [ -z "$offset" ]; then
  echo "::error::No wearVersionCodeOffset in wearApp/build.gradle.kts."
  exit 1
fi
echo "version_code=$((VERSION_CODE + offset))" >> "$GITHUB_OUTPUT"
