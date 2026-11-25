#!/usr/bin/env bash
#
# Run the tests on the JS target. Run by .github/workflows/build-and-test.yml.
#
set -e

read -ra tasks <<< "$TASKS"
./gradlew "${tasks[@]}" --continue
