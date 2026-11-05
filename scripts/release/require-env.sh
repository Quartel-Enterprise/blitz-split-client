#!/usr/bin/env bash
#
# Fails, pointing to the docs, when any of the named environment variables is empty. The release workflow runs it at
# the start of a job, so a secret or a variable that was never set stops the job there and not after the build.
#
# Usage: scripts/release/require-env.sh NAME...
set -euo pipefail

for name in "$@"; do
  if [ -z "${!name:-}" ]; then
    echo "::error::The $name secret or variable is empty. See docs/release-process.md#secrets."
    exit 1
  fi
done
