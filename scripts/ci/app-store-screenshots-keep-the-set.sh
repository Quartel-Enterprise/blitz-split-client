#!/usr/bin/env bash
#
# Keep the set. Run by .github/workflows/app-store-screenshots.yml.
#
set -eo pipefail

drop="\`gh release delete $SET_TAG --repo $GITHUB_REPOSITORY --yes\`"
# The repair's commands, for the versions this run wrote on: a set sent one version per run has lost the
# other's archive, and a version this run named is the one to write on again.
versions=""
if [ "$IOS" != true ]; then versions+=" -f ios=false"; fi
if [ "$MACOS" != true ]; then versions+=" -f macos=false"; fi
again="gh workflow run app-store-screenshots.yml --repo $GITHUB_REPOSITORY -f set=$SET$versions"
if [ -n "$APP_VERSION" ]; then again+=" -f app_version=$APP_VERSION"; fi
tidy="gh workflow run app-store-screenshots.yml --repo $GITHUB_REPOSITORY -f tidy_only=true$versions"
if [ "$LANDED" != true ]; then
  removal="and removes the set once every locale is up and the shelves are as staged"
  if [ -n "$LOCALES" ]; then removal="and keeps the set, as a run of chosen locales does"; fi
  echo "**The set was kept**, as the draft release \`$SET_TAG\`: a locale didn't land. Run its job again with the job's own re-run button; this job then runs again too, $removal. To drop it instead: $drop." >> "$GITHUB_STEP_SUMMARY"
  echo "::warning title=The set was kept::A locale didn't land, so the draft release $SET_TAG is still there. The run's summary names the locale."
elif [ "$TIDIED" != true ]; then
  echo "**The set was kept**, as the draft release \`$SET_TAG\`: every locale's job passed, and a shelf isn't as staged. The tidy-up job's summary names it. Send that shelf's locale again from this set, \`$again -f locales=\"<locale> <locale>\"\`, or, for a shelf that is only out of order, tidy up again with \`$tidy\` (docs/store-listing-screenshots.md#repairing-shelves). Don't delete one of a file's two entries by hand: both go. Once the shelves are right, drop the set: $drop." >> "$GITHUB_STEP_SUMMARY"
  echo "::warning title=The set was kept::A shelf isn't as staged, so the draft release $SET_TAG is still there to send its locale again from. The tidy-up job's summary names the shelf."
else
  echo "**The set was kept**, as the draft release \`$SET_TAG\`: this run sent chosen locales alone, and doesn't know whether the others are up. Once the shelves are right, drop it: $drop." >> "$GITHUB_STEP_SUMMARY"
fi
