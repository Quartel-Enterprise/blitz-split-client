#!/usr/bin/env bash
#
# Run the lane. Run by .github/actions/app-store-lane/action.yml.
#
set -eo pipefail

# An empty one would name a version with no number.
if [ -z "$APP_VERSION" ]; then unset APP_VERSION; fi
# Unquoted on purpose: the lane and each of its options are words of their own. Without the shell's globbing,
# so an option reaches the lane as it is written.
set -f
# shellcheck disable=SC2086
fastlane $LANE
