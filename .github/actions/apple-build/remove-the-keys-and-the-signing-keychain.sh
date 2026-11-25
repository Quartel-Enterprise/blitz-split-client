#!/usr/bin/env bash
#
# Remove the keys and the signing keychain. Run by .github/actions/apple-build/action.yml.
#
set -eo pipefail

rm -f "${KEY_PATH:-}" "$RUNNER_TEMP/match_ssh"
# setup_ci made the temporary keychain the default and the only one searched: give both back to the login one
security default-keychain -s login.keychain-db 2> /dev/null || true
security list-keychains -d user -s login.keychain-db 2> /dev/null || true
security delete-keychain fastlane_tmp_keychain-db 2> /dev/null || true
