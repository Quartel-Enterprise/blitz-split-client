#!/usr/bin/env bash
#
# Triggers the workflow that releases a stable version to Google Play and the App Store: no prompts, no menu.
#
# The run waits for the approval of a reviewer of the Production environment before it builds anything. With no
# input it then publishes to everyone: the production track at 100% and a submission for App Store review. See
# docs/release-process.md#releasing-a-stable-version for the inputs.
#
# Requires the GitHub CLI (gh), installed and authenticated: https://cli.github.com
#
# Usage: ./scripts/release/release-production.sh [input=value ...]
#   ./scripts/release/release-production.sh                                          # every platform, to everyone
#   ./scripts/release/release-production.sh track=internal submit_for_review=false   # reaches no user
#   ./scripts/release/release-production.sh version=3.1.0 rollout=20                 # a staged rollout of 3.1.0
set -euo pipefail

inputs=()
for input in "$@"; do
  if [[ ! "$input" =~ ^[a-z_]+=.*$ ]]; then
    echo "Error: '$input' is not an input: write input=value (e.g. track=internal)" >&2
    exit 1
  fi
  inputs+=(-f "$input")
done

echo "Bora Rachar: stable release"
echo "==========================="
if [ "$#" -eq 0 ]; then
  echo "No input: once approved, the run publishes to everyone on Google Play and submits for App Store review."
fi

# Refuses while another release waits, dispatches on main and prints the run's link. The array may be empty,
# which bash before 4.4 takes for an unset variable.
exec "$(dirname "$0")/dispatch-release.sh" release-production.yml ${inputs[@]+"${inputs[@]}"}
