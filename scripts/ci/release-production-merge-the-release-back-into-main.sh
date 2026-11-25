#!/usr/bin/env bash
#
# Merge the release back into main. Run by .github/workflows/release-production.yml.
#
set -eo pipefail

tag=()
if [ "$RELEASED" = true ]; then tag=("$VERSION"); fi
code="$VERSION_CODE"
if [ "$STORES" != true ]; then code=0; fi
./scripts/release/merge-back.sh "$VERSION" "$code" "$VERSION" "${tag[@]}"
