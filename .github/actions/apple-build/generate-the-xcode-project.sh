#!/usr/bin/env bash
#
# Generate the Xcode project. Run by .github/actions/apple-build/action.yml.
#
set -eo pipefail

./iosApp/generate-project.sh
if ! grep -q 'GoogleService-Info.plist in Resources' iosApp/BoraRachar.xcodeproj/project.pbxproj; then
  echo "::error::The generated project does not bundle GoogleService-Info.plist."
  exit 1
fi
