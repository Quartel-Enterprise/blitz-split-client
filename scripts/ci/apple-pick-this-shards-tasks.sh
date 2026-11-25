#!/usr/bin/env bash
#
# Pick this shard's tasks. Run by .github/workflows/apple.yml.
#
set -e

simulator=''
if [ "$SIMULATOR" != false ]; then
  simulator=iosSimulatorArm64Test
fi
if [ "$EVENT_NAME" != pull_request ]; then
  tasks='macosArm64Test :shared:linkDebugFrameworkIosSimulatorArm64'
  tasks="$tasks compileKotlinIosArm64 compileKotlinWatchosArm64 compileAppleMainKotlinMetadata"
elif [ "$SHARD_TOTAL" -eq 1 ]; then
  tasks="${simulator:+$simulator }macosArm64Test"
else
  modules=$(./scripts/modules-with-tests.sh commonTest)
  tasks=$(awk -v shard="$SHARD_INDEX" -v total="$SHARD_TOTAL" -v simulator="$simulator" '
    (NR - 1) % total == shard {
      gsub("/", ":")
      printf ":%s:macosArm64Test ", $0
      if (simulator != "") printf ":%s:%s ", $0, simulator
    }' <<< "$modules")
  if [ -z "$tasks" ]; then
    echo "::error::No module with tests falls on shard $((SHARD_INDEX + 1)) of $SHARD_TOTAL."
    exit 1
  fi
fi
echo "$tasks" | tr ' ' '\n'
echo "tasks=$tasks" >> "$GITHUB_OUTPUT"
