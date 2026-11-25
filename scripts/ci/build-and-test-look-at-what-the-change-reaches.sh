#!/usr/bin/env bash
#
# Look at what the change reaches. Run by .github/workflows/build-and-test.yml.
#
set -euo pipefail
unit=false js=false android=false web=false
everything() { unit=true js=true android=true web=true; }
build='^(build-logic/|gradle/|[^/]+\.gradle\.kts$|gradle\.properties$|version\.properties$|gradlew(\.bat)?$)'
build="$build|^\.github/actions/gradle/|^\.github/workflows/(build-and-test|review-gate)\.yml$|^scripts/ci/build-and-test-"
# AnalyticsCatalogTest, a jvmTest, reads the analytics catalog and every main source set's Kotlin.
catalog='^docs/analytics/README\.md$|^(core|feature)/.*/src/[A-Za-z0-9]*Main/.*\.kt$'
none='\.md$|^docs/|^\.claude/|^iosApp/|^release-notes/|^store-listing/|^fastlane/|^\.github/'
none="$none|^scripts/ci/|^scripts/(release|store-screenshots|store-video|readme-hero)/|^scripts/(cpd|ktlint)\.sh$"
none="$none|^scripts/heavy-check\.|^scripts/check-android-strings\.py$|^\.jscpd|^\.editorconfig$"
if ! files=$(git -c core.quotePath=false diff --name-only --no-renames HEAD^1 HEAD) || [ -z "$files" ]; then
  everything
else
  while IFS= read -r file; do
    if [[ $file =~ $catalog ]]; then
      unit=true
    fi
    if [[ $file =~ $build ]]; then
      everything
    elif [[ $file =~ $none ]]; then
      :
    elif [[ $file =~ ^(core|feature|shared)/ ]]; then
      # A flag only turns on: a later file never turns off what an earlier one reached.
      tested=true
      [[ $file == shared/* ]] && tested=false
      if [[ $file =~ /src/(apple|ios|macos|watchos|native)[A-Za-z0-9]*/ ]]; then
        :
      elif [[ $file =~ /src/androidMain/ ]]; then
        android=true
      elif [[ $file =~ /src/jvm(Main|Test)/ ]]; then
        unit=true
      elif [[ $file =~ /src/jsMain/ ]]; then
        web=true
        [[ $tested == true ]] && js=true
      elif [[ $file =~ /src/jsTest/ ]]; then
        js=true
      elif [[ $file =~ /src/commonTest/ ]]; then
        unit=true js=true
      else
        unit=true android=true web=true
        [[ $tested == true ]] && js=true
      fi
    elif [[ $file =~ ^android/ ]]; then
      unit=true android=true
    elif [[ $file =~ ^(androidApp|wearApp)/ ]]; then
      android=true
    elif [[ $file =~ ^webApp/ ]]; then
      web=true
    elif [[ $file == scripts/modules-with-tests.sh ]]; then
      js=true
    else
      echo "$file reaches every job."
      everything
    fi
  done <<< "$files"
fi
{
  echo "unit=$unit"
  echo "js=$js"
  echo "android=$android"
  echo "web=$web"
} | tee -a "$GITHUB_OUTPUT"
