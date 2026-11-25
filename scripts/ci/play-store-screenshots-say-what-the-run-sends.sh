#!/usr/bin/env bash
#
# Say what the run sends. Run by .github/workflows/play-store-screenshots.yml.
#
set -eo pipefail

if [ -z "$LOCALES" ]; then
  echo "::error::No screenshot was staged, so there is nothing to send."
  exit 1
fi
going="Going to Google Play, a few locales per job, where they show to everyone as each job lands"
if [ "$VALIDATE_ONLY" = true ]; then
  going="Only checked against Google Play (\`validate_only\`): the listing doesn't change"
fi
{
  echo "## Google Play's screenshots"
  echo
  echo "$going: $(jq -r 'join(", ")' <<< "$LOCALES")."
  echo
  echo "The images are this run's artifact \`play-store-screenshots\`, to download and look at."
} >> "$GITHUB_STEP_SUMMARY"
