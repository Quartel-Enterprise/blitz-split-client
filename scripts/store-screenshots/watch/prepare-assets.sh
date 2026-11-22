#!/bin/bash

# The photos the watches' screenshots hand the sample trip (docs/store-listing-screenshots.md#the-watches-screenshots):
# the ones the iPhone's captures use, listed in assets.tsv (the faces and the three groups' photos SampleTrips names),
# copied into the folder given under the paths the seed reads (faces/person0.jpg, groups/<photo>.jpg). The folder is
# emptied first.
#
#   ./scripts/store-screenshots/watch/prepare-assets.sh build/store-watch-assets
set -eo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
BANK="$ROOT/scripts/store-screenshots"
ASSETS="${1:?Usage: prepare-assets.sh <folder>}"

rm -rf "$ASSETS"
mkdir -p "$ASSETS/faces" "$ASSETS/groups"
# name<TAB>file: a face is faces/person<n>.jpg, a group's photo groups/<photo>.jpg (the file of the bank, bank/groups/)
while IFS=$'\t' read -r name file; do
  case "$name" in
    person*) cp "$BANK/$file" "$ASSETS/faces/$name.jpg" ;;
    group_*) cp "$BANK/$file" "$ASSETS/groups/${name#group_}.jpg" ;;
  esac
done < <(grep -v '^#' "$BANK/assets.tsv")
