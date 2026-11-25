#!/usr/bin/env bash
#
# Say what the run publishes. Run by .github/workflows/release-production.yml.
#
set -eo pipefail

android="not in this run"
if [ "$ANDROID" = true ]; then
  android="Google Play, \`$TRACK\` track"
  if [ "$TRACK" = production ]; then
    android="$android, $ROLLOUT% of the users"
  fi
  android="$android; the watch to the same Wear OS track"
fi
app_store="TestFlight only"
if [ "$SUBMIT" = true ]; then
  app_store="App Store review, then released once approved"
  if [ "$PHASED" = true ]; then
    app_store="$app_store, over seven days"
  fi
fi
ios="not in this run"
macos="not in this run"
if [ "$IOS" = true ]; then ios="$app_store"; fi
if [ "$MACOS" = true ]; then macos="$app_store"; fi
web="not in this run"
if [ "$WEB" = true ]; then web="$WEB_URL, for everyone"; fi
# The web has no testing channel: deployed, it is out for everyone and the version is released, whatever
# the stores get. Said at once, while the web job still builds: a run meant to try the stores would
# otherwise do more than its plan reads, and nothing waits for anyone to read it.
trial=false
if [ "$ANDROID" = true ] && [ "$TRACK" != production ]; then trial=true; fi
if { [ "$IOS" = true ] || [ "$MACOS" = true ]; } && [ "$SUBMIT" != true ]; then trial=true; fi
note=""
if [ "$WEB" = true ] && [ "$trial" = true ]; then
  note="The stores only get a trial (a testing track, or TestFlight), but the web app is deployed for everyone: the version gets its tag and its notes are published. For a run that reaches no user, cancel this one now, before the web job deploys, and dispatch again with \`web\` off. What a store already took stays there, and the run still records it."
  echo "::warning title=The web is released::${note//\`/}"
fi
# The row about the screenshots, and a warning for each device marker that is on and adds no capture, from
# a script with tests, which reads the list apple-screenshots-capture's matrix is made of. The warnings
# come on stderr: kept apart from the row, then printed for the run's page.
warnings="$RUNNER_TEMP/screenshots-warnings.txt"
if ! screenshots="$(./scripts/release/app-store-screenshots-plan.sh 2> "$warnings")"; then
  cat "$warnings"
  exit 1
fi
cat "$warnings"
{
  echo ""
  echo "| Platform | Where it goes |"
  echo "|---|---|"
  echo "| Android | $android |"
  echo "| iOS | $ios |"
  echo "| macOS | $macos |"
  echo "| Web | $web |"
  echo "| Store screenshots | $screenshots |"
  echo ""
  if [ -n "$note" ]; then
    echo "**$note**"
    echo ""
  fi
} >> "$GITHUB_STEP_SUMMARY"
