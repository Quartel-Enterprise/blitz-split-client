#!/usr/bin/env bash
#
# Bring the branch up to date with its base. Run by .github/workflows/merge-when-green.yml.
#
set -eo pipefail

# What the base gained since the branch left it: its commits (ahead_by of head...base) and the
# files they changed, as "<status> <path>". GitHub leaves the files out of a comparison it can't
# diff, which counts as too many to list.
gained=$(gh api "repos/$GH_REPO/compare/$HEAD_SHA...$BASE" \
  --jq '"\(.ahead_by) \(if .files == null then 300 else (.files | length) end)",
    ((.files // [])[] | "\(.status) \(.filename)")')
read -r behind gained_count <<< "$(head -n 1 <<< "$gained")"
base_files=$(tail -n +2 <<< "$gained")
if [ "$behind" -eq 0 ]; then
  echo "#$PR_NUMBER is up to date with $BASE."
  echo "current=true" >> "$GITHUB_OUTPUT"
  exit 0
fi
pr_files=$(gh api --paginate "repos/$GH_REPO/pulls/$PR_NUMBER/files?per_page=100" \
  --jq '.[] | "\(.status) \(.filename)"')

# A new Android module adds a values-xx/strings.xml per language too, so those say nothing.
language='^([a-z]+ ((.*/)?AppLanguage\.kt|iosApp/check-translations\.py)|added (webApp/src/i18n/[^/]+\.ts|store-listing/[^/]+))$'
if [ "$gained_count" -lt 300 ] && ! grep -Eq "$language" <<< "$base_files" &&
  ! grep -Eq "$language" <<< "$pr_files"; then
  echo "#$PR_NUMBER is $behind commit(s) behind $BASE, and neither side adds a language: merged as it is."
  echo "current=true" >> "$GITHUB_OUTPUT"
  exit 0
fi

echo "#$PR_NUMBER is $behind commit(s) behind $BASE, and one side adds a language: updating the branch."
echo "current=false" >> "$GITHUB_OUTPUT"
if answer="$(gh api --method PUT "repos/$GH_REPO/pulls/$PR_NUMBER/update-branch" \
  -f expected_head_sha="$HEAD_SHA" 2>&1 > /dev/null)"; then
  echo "Updated: the checks of the new head decide, and their completion starts this workflow again."
  exit 0
fi
echo "$answer"
reason="$(printf '%s\n' "$answer" | sed -n 's/^gh: //p' | head -n 1)"
case "$reason" in
  # A push meanwhile: its checks decide.
  *'expected head sha'*)
    echo "#$PR_NUMBER got a push meanwhile: its checks decide."
    exit 0
    ;;
  # Not a refusal: no answer at all, GitHub's own error, or a rate limit (the sweep's jobs share
  # the token). The label stays, and the next run tries again.
  '' | *'(HTTP 5'[0-9][0-9]')' | *'rate limit'*)
    echo "::warning::The update didn't go through (${reason:-no answer from GitHub}); the label stays for the next run."
    exit 1
    ;;
esac
# A conflict with the base, or a token GitHub won't let push to the branch, fails the same way on
# every later run, so the label goes, with a comment saying why.
body="Every check passed, but this branch is behind \`$BASE\` and one side adds a language, so it"
body+=" has to be brought up to date and checked again before it merges. GitHub refused to update it:"
body+=$'\n\n> '"$reason"$'\n\n'
body+="Merge \`$BASE\` into the branch (https://github.com/$GH_REPO/blob/main/docs/ci.md#merge-when-green),"
body+=" then add the \`merge-when-green\` label again."
gh pr comment "$PR_NUMBER" --body "$body"
gh api --method DELETE "repos/$GH_REPO/issues/$PR_NUMBER/labels/merge-when-green" > /dev/null
exit 1
