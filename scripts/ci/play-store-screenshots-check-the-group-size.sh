#!/usr/bin/env bash
#
# Check the group size. Run by .github/workflows/play-store-screenshots.yml.
#
set -eo pipefail

# Before the hour of drawing: the upload refuses any other size only after it, and a blank one fails
# the fromJSON that hands it to the upload.
if ! [[ "$GROUP_SIZE" =~ ^[1-9][0-9]*$ ]]; then
  echo "::error::group_size is '$GROUP_SIZE': it is a whole number from 1 up, the locales each Play edit sends."
  exit 1
fi
