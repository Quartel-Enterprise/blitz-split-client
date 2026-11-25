#!/usr/bin/env bash
#
# Build the signed app. Run by .github/actions/android-build/action.yml.
#
set -eo pipefail

read -ra tasks <<< "$TASK"
./gradlew "${tasks[@]}"
