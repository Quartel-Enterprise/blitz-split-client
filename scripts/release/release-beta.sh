#!/usr/bin/env bash
#
# Triggers the beta release workflow: no prompts, no menu.
#
# The workflow tags X.Y.Z-beta_N, publishes each platform to its channel (Android on Firebase App
# Distribution, the web on Cloudflare Pages, iOS and macOS on TestFlight, iOS also on Firebase App
# Distribution as an ad hoc build) and publishes a GitHub pre-release. See docs/release-process.md.
#
# Requires the GitHub CLI (gh), installed and authenticated: https://cli.github.com
#
# Usage: ./scripts/release/release-beta.sh [X.Y.Z]
#   X.Y.Z  optional: overrides the version that version.properties and the release notes give
set -euo pipefail

version="${1:-}"

echo "Bora Rachar: beta release"
echo "========================="

# Refuses while another release waits, dispatches on main and prints the run's link.
exec "$(dirname "$0")/dispatch-release.sh" release-beta.yml -f version="$version"
