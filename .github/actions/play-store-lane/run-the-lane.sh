#!/usr/bin/env bash
#
# Run the lane. Run by .github/actions/play-store-lane/action.yml.
# Reads from the step's env: LANE, PLAY_STORE_JSON_KEY_PATH.
#
set -eo pipefail

# Unquoted on purpose: the lane and each of its options are words of their own. Without the shell's globbing,
# so a pattern in an option reaches the lane as it is.
set -f
# shellcheck disable=SC2086
fastlane $LANE
