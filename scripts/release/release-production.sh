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
#   ./scripts/release/release-production.sh screenshots=true screenshots_iphone_duo=true       # the iPhone Duo's too, closed and open
#   ./scripts/release/release-production.sh screenshots=true screenshots_mac=true              # the Mac's too
#
# With screenshots=true the App Store's are captured on the iPhone and the iPad. The iPhone Duo and the Mac are each
# behind a marker of their own, off unless passed, since their captures fail on the runners today
# (docs/release-process.md#the-devices-the-app-stores-captures-run-on).
set -euo pipefail

inputs=()
screenshots_named=false
screenshots_on=false
device_named=false
device_on=false
for input in "$@"; do
  if [[ ! "$input" =~ ^[a-z_]+=.*$ ]]; then
    echo "Error: '$input' is not an input: write input=value (e.g. track=internal)" >&2
    exit 1
  fi
  inputs+=(-f "$input")
  case "$input" in
    screenshots=*)
      screenshots_named=true
      if [ "$input" = screenshots=true ]; then screenshots_on=true; fi
      ;;
    screenshots_iphone_duo=* | screenshots_mac=*)
      device_named=true
      if [[ "$input" == *=true ]]; then device_on=true; fi
      ;;
  esac
done

echo "Bora Rachar: stable release"
echo "==========================="
if [ "$#" -eq 0 ]; then
  echo "No input: the run publishes to everyone on Google Play and the web, and submits for App Store review. Nothing waits for an approval."
fi
# A device's marker without the switch it depends on reads as a release that makes its screenshots, and makes none.
if [ "$device_on" = true ] && [ "$screenshots_on" = false ]; then
  echo "No screenshot is made: screenshots_iphone_duo and screenshots_mac only choose the devices of a run with screenshots=true."
# Whatever else was passed: off is the default, and Google Play says nothing about a language left without its own.
elif [ "$screenshots_named" = false ]; then
  echo "The listings' screenshots stay as they are: pass screenshots=true to make them again (a new language, a screen that changed)."
elif [ "$screenshots_on" = true ] && [ "$device_named" = false ]; then
  echo "The App Store's screenshots leave the iPhone Duo and the Mac out: screenshots_iphone_duo=true and screenshots_mac=true add them, and their captures fail on the runners today."
fi

# Refuses while another release waits, dispatches on main and prints the run's link. The array may be empty,
# which bash before 4.4 takes for an unset variable.
exec "$(dirname "$0")/dispatch-release.sh" release-production.yml ${inputs[@]+"${inputs[@]}"}
