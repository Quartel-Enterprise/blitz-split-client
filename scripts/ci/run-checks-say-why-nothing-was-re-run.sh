#!/usr/bin/env bash
#
# Say why nothing was re-run. Run by .github/workflows/run-checks.yml.
#
set -e

body="$OPEN thread(s) of the Claude review are still unresolved, so the held checks weren't"
body+=" re-run. Fix each one, or reply with why it stays as it is, resolve them, then add the"
body+=" \`run-checks\` label again."
gh pr comment "$PR_NUMBER" --body "$body"
