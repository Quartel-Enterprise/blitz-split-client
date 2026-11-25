#!/usr/bin/env bash
#
# Fingerprint the Kotlin/Native libraries. Run by .github/workflows/apple.yml.
#
set -e

if [ -n "$KEY_PREFIX" ] && ls -d ~/.konan/kotlin-native-prebuilt-*/klib/cache > /dev/null 2>&1; then
  libraries=$(find ~/.konan/kotlin-native-prebuilt-*/klib/cache -mindepth 3 -maxdepth 3 -type d |
    sort | shasum -a 256 | cut -c1-16)
  case "$NEWEST_KEY" in
    "$KEY_PREFIX$libraries"*) echo "The newest entry already holds these libraries." ;;
    *) echo "key=$KEY_PREFIX$libraries-$GITHUB_RUN_ID-$GITHUB_RUN_ATTEMPT" >> "$GITHUB_OUTPUT" ;;
  esac
fi
