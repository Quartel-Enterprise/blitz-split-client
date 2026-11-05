#!/usr/bin/env bash
# Writes the app version into version.properties, the file every platform reads.
#
# Usage: scripts/release/bump-version.sh <versionName> <versionCode>
#   versionName  e.g. 3.0.0 or 3.0.0-beta_7
#   versionCode  e.g. 74
set -euo pipefail

version="${1:?versionName required (e.g. 3.0.0)}"
code="${2:?versionCode required (e.g. 74)}"

cd "$(dirname "$0")/../.."

sed -i.bak -E "s/^versionName=.*/versionName=${version}/" version.properties
sed -i.bak -E "s/^versionCode=.*/versionCode=${code}/" version.properties
rm -f version.properties.bak

echo "Bumped version to ${version} (versionCode ${code})"
