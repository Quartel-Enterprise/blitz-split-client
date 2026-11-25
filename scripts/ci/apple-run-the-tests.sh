#!/usr/bin/env bash
#
# Run the tests. Run by .github/workflows/apple.yml.
#
set -e

status=0
read -ra tasks <<< "$TASKS"
./gradlew "${tasks[@]}" --continue || status=$?
echo "finished=true" >> "$GITHUB_OUTPUT"
exit "$status"
