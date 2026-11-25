#!/usr/bin/env bash
#
# Check fastlane. Run by .github/actions/fastlane/action.yml.
#
set -eo pipefail

if ! fastlane --version 2> /dev/null | grep -q "fastlane $FASTLANE_VERSION"; then
  gem install fastlane -v "$FASTLANE_VERSION" --no-document
fi
if ! fastlane --version 2> /dev/null | grep -q "fastlane $FASTLANE_VERSION"; then
  echo "::error::The fastlane on the PATH is not $FASTLANE_VERSION."
  exit 1
fi
