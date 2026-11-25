#!/usr/bin/env bash
#
# Check where the listing comes from. Run by .github/workflows/store-listing.yml.
#
set -e

if [ "$REF" != refs/heads/main ]; then
  echo "::error::A listing is sent from main, and this run was dispatched on $REF. See docs/store-listing.md."
  exit 1
fi
