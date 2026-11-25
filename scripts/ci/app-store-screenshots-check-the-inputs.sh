#!/usr/bin/env bash
#
# Check the inputs. Run by .github/workflows/app-store-screenshots.yml.
#
set -eo pipefail

if [ "$IOS" != true ] && [ "$MACOS" != true ]; then
  echo "::error::No platform is selected."
  exit 1
fi
# The tidy-up works on the version that is open for editing, and sends nothing: an input that names
# something to send, or a version to write on, was meant for another run.
if [ "$TIDY_ONLY" = true ]; then
  if [ -n "$SET$LOCALES$APP_VERSION$DEVICES" ]; then
    echo "::error::tidy_only sends nothing: leave set, locales, devices and app_version blank."
    exit 1
  fi
  exit 0
fi
# The name becomes the tag of the release that is read and then deleted: nothing but a set's name goes
# through (SET_NAME of scripts/store-screenshots/local-set.mjs).
if [[ ! "$SET" =~ ^[A-Za-z0-9][A-Za-z0-9._-]{0,59}$ ]]; then
  echo "::error::set is the name local-set.mjs printed: letters, digits, dots, dashes and underscores."
  exit 1
fi
# It reaches the lane's command line, where a space would split it: shelf names and commas alone.
if [[ -n "$DEVICES" && ! "$DEVICES" =~ ^[a-z0-9_]+(,[a-z0-9_]+)*$ ]]; then
  echo "::error::devices is shelf names with commas and no spaces, as local-set.mjs printed: iphone_duo,watch_ultra_3."
  exit 1
fi
# It reaches a lane's environment, so nothing but a version goes through.
if [[ -n "$APP_VERSION" && ! "$APP_VERSION" =~ ^[0-9]+\.[0-9]+(\.[0-9]+)?$ ]]; then
  echo "::error::app_version is a version as the App Store writes it, X.Y.Z, or blank."
  exit 1
fi
