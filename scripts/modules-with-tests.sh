#!/bin/bash

# Lists the modules that have tests in one of the given source sets, one folder per line, sorted:
#   ./scripts/modules-with-tests.sh commonTest jsTest
#
# The CI jobs that split a target's test suites across shards deal their modules from this list
# (js-tests of build-and-test.yml, apple-tests of apple.yml), so a new module joins a shard by itself
# and both deal the same modules. A folder settings.gradle.kts doesn't include is left out: its tasks
# don't exist, and one named on the command line fails the whole Gradle run before any test.

set -eo pipefail

if [ $# -eq 0 ]; then
  echo "usage: $0 <source set> [<source set>...]" >&2
  exit 2
fi

cd "$(dirname -- "${BASH_SOURCE[0]}")/.."

source_sets=()
for source_set in "$@"; do
  source_sets+=(-o -path "*/src/$source_set")
done

# The first -o has nothing before it to join.
find . \( -path ./build-logic -o -path ./.claude -o -name build -o -name node_modules \) -prune \
  -o -type d \( "${source_sets[@]:1}" \) -print |
  sed -E 's#^\./##; s#/src/[^/]+$##' | sort -u |
  while read -r module; do
    if grep -q -F "include(\":${module//\//:}\")" settings.gradle.kts; then
      echo "$module"
    fi
  done
