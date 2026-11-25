#!/usr/bin/env bash
#
# Report what was re-run. Run by .github/workflows/merge-when-green.yml.
#
set -e

if [ "$RERUN" -gt 0 ]; then
  echo "Re-ran $RERUN run(s) of #$PR_NUMBER held for review conversations that are resolved now."
  exit 0
fi
echo "::warning::A review-gate check of #$PR_NUMBER failed, but no run was held for review threads."
body="A \`review-gate\` check failed without holding the checks for review threads, so this"
body+=" was not merged. Re-run the failed \`review-gate\` job, then add the"
body+=" \`merge-when-green\` label again."
gh pr comment "$PR_NUMBER" --body "$body"
gh api --method DELETE "repos/$GH_REPO/issues/$PR_NUMBER/labels/merge-when-green" > /dev/null
