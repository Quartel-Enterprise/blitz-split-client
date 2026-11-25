#!/usr/bin/env bash
#
# Check where the run comes from. Run by .github/workflows/play-store-screenshots.yml.
#
set -eo pipefail

if [ "$REF" != refs/heads/main ]; then
  echo "::error::Screenshots are sent from main, and this run was dispatched on $REF. See docs/store-listing-screenshots.md."
  exit 1
fi
