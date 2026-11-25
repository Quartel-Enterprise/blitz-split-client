#!/usr/bin/env bash
#
# Check that no run is going. Run by .github/actions/no-run-going/action.yml.
#
set -eo pipefail

for workflow in $WORKFLOWS; do
  going="$(gh api "repos/$GITHUB_REPOSITORY/actions/workflows/$workflow/runs?per_page=30" \
    | jq -r --arg self "$GITHUB_RUN_ID" '[.workflow_runs[] | select(.status != "completed" and (.id | tostring) != $self)][0].status // empty')"
  if [ -n "$going" ]; then
    echo "::error::$workflow has a run that is $going: $THEN."
    exit 1
  fi
done
