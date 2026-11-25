#!/usr/bin/env bash
#
# Delete the Gradle home the failed run saved. Run by .github/workflows/apple.yml.
#
set -e

ids=''
for attempt in 1 2 3; do
  ids=$(gh cache list --ref "$GITHUB_REF" --key gradle-home- --sort created_at --order desc \
    --limit 100 --json id,key | jq -r --arg sha "$GITHUB_SHA" '.[]
    | select((.key | contains("|apple-tests[")) and (.key | endswith("-" + $sha))) | .id')
  if [ -n "$ids" ] || [ "$attempt" -eq 3 ]; then
    break
  fi
  sleep 20
done
if [ -z "$ids" ]; then
  echo "::warning::The cache lists no Gradle home of this run's apple-tests. If the run saved one, it is still the newest: see \"Speed and timeouts\" in docs/ci.md."
fi
for id in $ids; do
  gh cache delete "$id"
done
