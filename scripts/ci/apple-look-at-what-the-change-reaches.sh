#!/usr/bin/env bash
#
# Look at what the change reaches. Run by .github/workflows/apple.yml.
#
set -e

kotlin=true
tests=true
simulator=true
shards='[1]'
own='^\.github/workflows/apple\.yml$|^scripts/ci/apple-(look-at|pick-this|run-the|fingerprint)|^scripts/modules-with-tests\.sh$'
apple='^(core|feature)/.*/src/(apple|ios|macos|native)[A-Za-z0-9]*/'
apple="$apple|^gradle/libs\.versions\.toml$|^gradle\.properties$"
logic='^build-logic/.*(Multiplatform|Simulator)[A-Za-z]*\.kt$'
apple="$apple|$logic|^\.github/actions/gradle/|$own"
ios='^(core|feature)/.*/src/ios[A-Za-z0-9]*/|^gradle/libs\.versions\.toml$'
ios="$ios|$logic|^\.github/workflows/apple\.yml$|^scripts/ci/apple-(look-at|pick-this|run-the|fingerprint)"
wide="^(core|gradle)/|^gradle\.properties$|$own"
not_apple='\.md$|/src/(js|jvm)(Main|Test)/|/src/androidMain/'
if files=$(git -c core.quotePath=false diff --name-only --no-renames HEAD^1 HEAD) &&
  [ -n "$files" ]; then
  simulator=false
  if ! grep -q -v -E '^(iosApp|release-notes)/|\.md$' <<< "$files"; then
    kotlin=false
    tests=false
  elif ! grep -q -E "$apple" <<< "$files"; then
    tests=false
  else
    if grep -q -E "$ios" <<< "$files"; then
      simulator=true
    fi
    if grep -v -E "$not_apple" <<< "$files" | grep -q -E "$wide"; then
      shards='[1, 2]'
    fi
  fi
fi
{
  echo "kotlin=$kotlin"
  echo "tests=$tests"
  echo "simulator=$simulator"
  echo "shards=$shards"
} >> "$GITHUB_OUTPUT"
