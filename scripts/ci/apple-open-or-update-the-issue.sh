#!/usr/bin/env bash
#
# Open or update the issue. Run by .github/workflows/apple.yml.
#
set -e

previous=$(gh api "repos/$GH_REPO/actions/workflows/apple.yml/runs?branch=main&event=push&status=completed&per_page=30" \
  --jq "[.workflow_runs[] | select(.id != $GITHUB_RUN_ID and (.conclusion == \"success\" or .conclusion == \"failure\"))][0].head_sha // empty" || true)
commits=''
# Only a commit id: on an error, gh prints GitHub's answer where the id would be.
if [[ "$previous" =~ ^[0-9a-f]{40}$ ]] && [ "$previous" != "$GITHUB_SHA" ]; then
  commits=" The run also stands for the pushes it replaced in the queue, so what broke is in [the commits since the last run that completed]($GITHUB_SERVER_URL/$GH_REPO/compare/$previous...$GITHUB_SHA)."
fi
body=$(printf '%s\n\n%s\n' \
  "[This run]($RUN_URL) of \`apple\` failed on \`main\` at $GITHUB_SHA: \`apple-tests\` $TESTS, \`apple-app\` $APP.$commits" \
  "A pull request runs the Kotlin tests on macOS only when it changes Apple code, so a test that fails on an Apple target alone first fails here, and the pull request that fixes it may not run them. The failed job's log names the task: on \`main\`, \`apple-tests\` runs every module's \`macosArm64Test\`, links \`Shared\` for the iOS simulator, compiles the device targets and builds the watchOS XCFramework. Run that task on the Mac, \`./scripts/heavy-check.sh ./gradlew :<module>:macosArm64Test\` for a test. See \"Speed and timeouts\" in docs/ci.md.")
issue=$(gh issue list --state open --limit 200 --json number,title \
  --jq "map(select(.title == \"$TITLE\"))[0].number // empty")
if [ -n "$issue" ]; then
  gh issue comment "$issue" --body "$body"
else
  gh issue create --title "$TITLE" --body "$body" --assignee "$ACTOR" ||
    gh issue create --title "$TITLE" --body "$body"
fi
