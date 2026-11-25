#!/usr/bin/env bash
#
# Pick this shard's modules. Run by .github/workflows/build-and-test.yml.
#
set -e

modules=$(./scripts/modules-with-tests.sh commonTest jsTest)
tasks=$(awk -v shard="$SHARD_INDEX" -v total="$SHARD_TOTAL" \
  '(NR - 1) % total == shard { gsub("/", ":"); printf ":%s:jsTest ", $0 }' <<< "$modules")
if [ -z "$tasks" ]; then
  echo "::error::No module with tests falls on shard $((SHARD_INDEX + 1)) of $SHARD_TOTAL."
  exit 1
fi
echo "$tasks" | tr ' ' '\n'
echo "tasks=$tasks" >> "$GITHUB_OUTPUT"
