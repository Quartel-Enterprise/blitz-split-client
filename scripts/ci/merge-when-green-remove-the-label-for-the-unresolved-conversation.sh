#!/usr/bin/env bash
#
# Remove the label for the unresolved conversations. Run by .github/workflows/merge-when-green.yml.
#
set -e

echo "#$PR_NUMBER is green but has $UNRESOLVED unresolved review conversation(s)."
if [ "$HELD" -gt 0 ]; then
  body="The checks are held for the Claude review's threads, and $UNRESOLVED review"
  body+=" conversation(s) are still unresolved, so this was not merged. Fix each one, or reply"
  body+=" with why it stays as it is, resolve them, then add the \`merge-when-green\` label"
  body+=" again: the held checks run then, and this merges once they pass."
else
  body="Every check passed, but $UNRESOLVED review conversation(s) are still unresolved, so this"
  body+=" was not merged. Fix each one, or reply with why it stays as it is, resolve them, then"
  body+=" add the \`merge-when-green\` label again."
fi
gh pr comment "$PR_NUMBER" --body "$body"
gh api --method DELETE "repos/$GH_REPO/issues/$PR_NUMBER/labels/merge-when-green" > /dev/null
