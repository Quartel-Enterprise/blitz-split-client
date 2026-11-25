#!/usr/bin/env bash
#
# Check the inputs. Run by .github/workflows/store-listing.yml.
#
set -e

if [ "$ANDROID" != true ] && [ "$IOS" != true ] && [ "$MACOS" != true ]; then
  echo "::error::No store is selected."
  exit 1
fi
if [ "$VALIDATE_ONLY" = true ] && [ "$ANDROID" != true ]; then
  echo "::error::validate_only checks Google Play alone, and android is off: the App Store has no such check."
  exit 1
fi
# Both reach a lane as a word of its command line, so nothing but a number goes through.
if [[ -n "$VERSION_CODE" && ! "$VERSION_CODE" =~ ^[1-9][0-9]*$ ]]; then
  echo "::error::version_code is a versionCode, a whole number, or blank."
  exit 1
fi
if [[ -n "$APP_VERSION" && ! "$APP_VERSION" =~ ^[0-9]+\.[0-9]+(\.[0-9]+)?$ ]]; then
  echo "::error::app_version is a version as the App Store writes it, X.Y.Z, or blank."
  exit 1
fi
