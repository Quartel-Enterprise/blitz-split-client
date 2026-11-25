#!/usr/bin/env bash
#
# Merge the pull request. Run by .github/workflows/merge-when-green.yml.
#
set -e

# Only gh's first line is kept, "gh: <GitHub's message> (HTTP <status>)": the answer's JSON
# goes, and so does a hint about scopes gh may add after it.
if ! answer="$(gh api --method PUT "repos/$GH_REPO/pulls/$PR_NUMBER/merge" \
  -f merge_method=squash -f sha="$HEAD_SHA" 2>&1 > /dev/null)"; then
  echo "$answer"
  reason="$(printf '%s\n' "$answer" | sed -n 's/^gh: //p' | head -n 1)"
  # Not a refusal: no answer at all, GitHub's own error, a rate limit, or main moved under the
  # merge (another pull request merged first). The label stays, and the next run tries again.
  case "$reason" in
    '' | 'Base branch was modified'* | *'(HTTP 5'[0-9][0-9]')' | *'rate limit'*)
      echo "::warning::The merge didn't go through (${reason:-no answer from GitHub}); the label stays for the next run."
      exit 1
      ;;
  esac
  # Nor is a pull request someone merged or closed meanwhile, or one that got a push: its checks decide.
  if [ "$(gh pr view "$PR_NUMBER" --json state,headRefOid --jq '"\(.state) \(.headRefOid)"')" = "OPEN $HEAD_SHA" ]; then
    body="Every check passed and every conversation is resolved, but GitHub refused the merge:"
    body+=$'\n\n> '"$reason"$'\n\n'
    body+="Fix what it reports (https://github.com/$GH_REPO/blob/main/docs/ci.md#merge-when-green), then add the"
    body+=" \`merge-when-green\` label again."
    gh pr comment "$PR_NUMBER" --body "$body"
    gh api --method DELETE "repos/$GH_REPO/issues/$PR_NUMBER/labels/merge-when-green" > /dev/null
  fi
  exit 1
fi

# The label only means "merge this once it is green", so it goes once that is done.
gh api --method DELETE "repos/$GH_REPO/issues/$PR_NUMBER/labels/merge-when-green" > /dev/null
