#!/usr/bin/env bash
#
# Prepare SSH access to the certificates repository. Run by .github/actions/apple-build/action.yml.
#
set -eo pipefail

mkdir -p ~/.ssh
printf '%s\n' "$MATCH_SSH_PRIVATE_KEY" > "$RUNNER_TEMP/match_ssh"
chmod 600 "$RUNNER_TEMP/match_ssh"
ssh-keyscan -t ed25519 github.com >> ~/.ssh/known_hosts
