#!/usr/bin/env bash
#
# Build the app. Run by .github/workflows/apple.yml.
#
set -e

case "$PLATFORM" in
  iOS) target=(-destination 'generic/platform=iOS Simulator' ARCHS=arm64) ;;
  macOS) target=(-destination 'platform=macOS') ;;
esac
xcodebuild -project iosApp/BoraRachar.xcodeproj -scheme "BoraRachar-$PLATFORM" "${target[@]}" \
  -derivedDataPath iosApp/build/DerivedData -clonedSourcePackagesDirPath iosApp/build/SourcePackages \
  CODE_SIGNING_ALLOWED=NO build -quiet
