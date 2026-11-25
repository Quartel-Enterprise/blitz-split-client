#!/usr/bin/env bash
#
# Make the screenshots. Run by .github/actions/play-screenshots-draw/action.yml.
#
set -eo pipefail

if [ -n "$ART" ]; then node scripts/store-screenshots/listing-art/compose.mjs; fi
node scripts/store-screenshots/listing-art/stage-play.mjs
