#!/usr/bin/env bash
#
# Read the Kotlin version. Run by .github/actions/gradle/action.yml.
#
set -eo pipefail

version=$(sed -nE 's/^kotlin = "(.*)"$/\1/p' gradle/libs.versions.toml)
if [ -z "$version" ]; then
  echo "::error::No kotlin = \"...\" line in gradle/libs.versions.toml, so the Kotlin/Native cache has no key."
  exit 1
fi
echo "version=$version" >> "$GITHUB_OUTPUT"
# The fixed word after the version keeps 2.5.0 from matching an entry of 2.5.0-RC.
echo "konan-key=konan-$RUNNER_OS-$RUNNER_ARCH-$version-libraries-" >> "$GITHUB_OUTPUT"
