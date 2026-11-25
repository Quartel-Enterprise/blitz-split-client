#!/usr/bin/env bash
#
# Check for the ad hoc IPA. Run by .github/workflows/release-beta.yml.
#
set -e

if compgen -G "build/ios-adhoc/*.ipa" > /dev/null; then
  echo "ipa=true" >> "$GITHUB_OUTPUT"
else
  echo "ipa=false" >> "$GITHUB_OUTPUT"
fi
