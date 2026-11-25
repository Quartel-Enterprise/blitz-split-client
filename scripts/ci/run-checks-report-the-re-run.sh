#!/usr/bin/env bash
#
# Report the re-run. Run by .github/workflows/run-checks.yml.
#
set -e

if [ "$RERUN" -eq 0 ]; then
  echo "::notice::No check of $HEAD_SHA was held, so nothing was re-run."
else
  echo "Re-ran $RERUN held run(s) of $HEAD_SHA."
fi
