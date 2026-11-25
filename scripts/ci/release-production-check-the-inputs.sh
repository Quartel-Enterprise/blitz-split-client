#!/usr/bin/env bash
#
# Check the inputs. Run by .github/workflows/release-production.yml.
#
set -eo pipefail

if [ "$ANDROID" != true ] && [ "$IOS" != true ] && [ "$MACOS" != true ] && [ "$WEB" != true ]; then
  echo "::error::No platform is selected."
  exit 1
fi
if ! [[ "$ROLLOUT" =~ ^[0-9]+$ ]] || [ "$ROLLOUT" -lt 1 ] || [ "$ROLLOUT" -gt 100 ]; then
  echo "::error::rollout is the percentage of the users who get the release, from 1 to 100: '$ROLLOUT' is not."
  exit 1
fi
