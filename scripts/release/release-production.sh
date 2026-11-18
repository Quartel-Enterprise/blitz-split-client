#!/usr/bin/env bash
#
# Triggers the workflow that releases a stable version to Google Play, the App Store and the web: no prompts, no
# menu.
#
# Nothing waits for an approval: the run starts at once, and only an administrator of the repository may dispatch
# it. With no input it publishes to everyone: the production track at 100%, a submission for App Store review and
# the web app at app.blitzsplit.com. See docs/release-process.md#releasing-a-stable-version for the inputs.
#
# Requires the GitHub CLI (gh), installed and authenticated: https://cli.github.com
#
# Usage: ./scripts/release/release-production.sh [input=value ...]
#   ./scripts/release/release-production.sh                                                    # every platform, to everyone
#   ./scripts/release/release-production.sh track=internal submit_for_review=false web=false   # reaches no user
#   ./scripts/release/release-production.sh version=3.1.0 rollout=20                           # a staged rollout of 3.1.0
#   ./scripts/release/release-production.sh screenshots=true                                   # and the listings' screenshots made again
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
  echo "No input: the run publishes to everyone on Google Play and the web, and submits for App Store review. Nothing waits for an approval."
  echo "The listings' screenshots stay as they are: pass screenshots=true to make them again (a new language, a screen that changed)."
fi

# Refuses while another release waits, dispatches on main and prints the run's link. The array may be empty,
# which bash before 4.4 takes for an unset variable.
exec "$(dirname "$0")/dispatch-release.sh" release-production.yml ${inputs[@]+"${inputs[@]}"}
