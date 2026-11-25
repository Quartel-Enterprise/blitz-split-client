#!/usr/bin/env bash
#
# List the labeled pull requests. Run by .github/workflows/merge-when-green.yml.
#
set -eo pipefail

# As in the merge job, the label is filtered here rather than with --label, which goes
# through the search index and lags behind a label just added.
fields=headRefName,labels,state,isDraft,isCrossRepository
if [ -n "$PR_NUMBER" ]; then
  prs=$(gh pr view "$PR_NUMBER" --json "$fields" | jq -c '[.]')
else
  prs=$(gh pr list --state open --limit 200 --json "$fields")
fi
branches=$(jq -c '[.[] | select(.state == "OPEN" and (.isDraft | not) and (.isCrossRepository | not)
  and any(.labels[]; .name == "merge-when-green")) | .headRefName] | unique' <<< "$prs")
echo "Labeled pull request branches: $branches"
echo "branches=$branches" >> "$GITHUB_OUTPUT"
