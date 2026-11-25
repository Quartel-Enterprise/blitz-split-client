#!/usr/bin/env bash
#
# Check where the release comes from. Run by .github/workflows/release-production.yml.
#
set -eo pipefail

if [ "$REF" != refs/heads/main ]; then
  echo "::error::A stable version is released from main, and this run was dispatched on $REF. See docs/release-process.md#who-releases."
  exit 1
fi
