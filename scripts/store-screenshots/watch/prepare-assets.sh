#!/bin/bash

# The photos the watches' screenshots hand the sample trip (docs/store-listing-screenshots.md#the-watches-screenshots): the
# five faces and the three groups' photos the iPhone's captures use (assets.tsv, SampleTrips), as faces/ and groups/ of
# the folder given, which is emptied first.
#
#   ./scripts/store-screenshots/watch/prepare-assets.sh build/store-watch-assets
set -eo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
BANK="$ROOT/scripts/store-screenshots"
ASSETS="${1:?Usage: prepare-assets.sh <folder>}"

rm -rf "$ASSETS"
mkdir -p "$ASSETS/faces" "$ASSETS/groups"
cp "$BANK"/faces/person*.jpg "$ASSETS/faces/"
for photo in trip-lisbon-tram home-kitchen event-soccer; do cp "$BANK/bank/groups/$photo.jpg" "$ASSETS/groups/"; done
