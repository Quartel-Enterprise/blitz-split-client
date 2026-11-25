#!/usr/bin/env bash
#
# Check that every claude-review ref is the same. Run by .github/workflows/claude-review-refs.yml.
#
set -e

refs=$(grep -rhoE 'Quartel-Enterprise/claude-review/[a-z-]+@[^[:space:]]+' .github/workflows | sed 's/.*@//' | sort -u)
if [ "$(grep -c . <<< "$refs")" -gt 1 ]; then
  echo "::error::The workflows pin different releases of Quartel-Enterprise/claude-review: $(tr '\n' ' ' <<< "$refs")Bump every uses: to the same one."
  grep -rnE 'Quartel-Enterprise/claude-review/[a-z-]+@' .github/workflows
  exit 1
fi
echo "Every claude-review ref is ${refs:-absent}."
