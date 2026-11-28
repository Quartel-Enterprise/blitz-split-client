#!/usr/bin/env bash
#
# Check whether every check passed. Run by .github/workflows/merge-when-green.yml.
#
set -e

if [ -z "$GH_TOKEN" ]; then
  echo "::error::The MERGE_WHEN_GREEN_TOKEN secret is not set."
  exit 1
fi

# An empty --head filters nothing, and would judge any labeled pull request.
if [ -z "$HEAD_BRANCH" ]; then
  echo "::error::No head branch to check."
  exit 1
fi

# No pull request workflow runs on every push here, so none can tell that the push's workflows have
# registered their runs. A label added, or a sweep run, right after a push gives them time
# to; a completed run of the head commit (workflow_run) means they already have.
if [ "$GITHUB_EVENT_NAME" != workflow_run ]; then
  sleep 30
fi

# The label is filtered here rather than with --label, which goes through the search index:
# it lags behind a label just added or removed, and behind a merge.
if ! prs=$(gh pr list --state open --head "$HEAD_BRANCH" \
  --json number,headRefOid,baseRefName,isDraft,isCrossRepository,labels); then
  echo "::error::Reading the pull requests with the MERGE_WHEN_GREEN_TOKEN secret failed. If the token expired, create a new one and update the secret."
  exit 1
fi

# The pull request's current head is what gets judged, not the commit of the run that
# started this one: ai-review's run ends on the opening commit after it pushed its fix
# commit, and on a pull request whose changes run no check workflow (docs only) only
# ai-review's own short run of the push completes on the fix commit, possibly before the
# review's run ends. A draft can't be merged; marking it ready runs its
# checks again. Two labeled pull requests can share a branch (one into two bases): one is
# handled per run, and its merge or label change starts this workflow again for the other.
match=$(jq -c '[.[] | select((.isCrossRepository | not) and (.isDraft | not)
  and any(.labels[]; .name == "merge-when-green"))] | first // empty' <<< "$prs")
if [ -z "$match" ]; then
  echo "No open pull request of $HEAD_BRANCH is labeled merge-when-green and ready for review."
  exit 0
fi
pr=$(jq -r .number <<< "$match")
head=$(jq -r .headRefOid <<< "$match")
base=$(jq -r .baseRefName <<< "$match")

# Some checks turn green with nothing to start this workflow then. The check code scanning
# adds (CodeQL) is no workflow run: it turns green a little after the codeql workflow's run
# completes. The "Analyze" checks of GitHub's Code Quality are jobs of a run GitHub starts on
# its own on every push (event "dynamic": its workflow is no file of the repository, and it
# is named after the pull request), which no workflow_run announces; on a pull request whose
# changes run no check workflow (docs only) they are the last to finish. So while the only
# checks left are pending ones of those two kinds, the checks are read again:
# - for 15 minutes while a dynamic run that is still going owns one of them: a Code Quality
#   run takes a minute as a rule, and took 13 at its longest;
# - for 2 minutes otherwise: a check no run owns, a dynamic run of the head commit with no
#   check on it yet (its jobs may not have theirs), or a read GitHub's API failed.
# Every 20 seconds for the first 2 minutes and every minute after that, so a long wait costs
# few API calls.
long_wait=900
short_wait=120
waited=0
stalled=0
unread_for=0
pause() {
  local step=20
  if [ "$waited" -ge "$short_wait" ]; then step=60; fi
  sleep "$step"
  waited=$((waited + step))
  if [ "$1" = stalled ]; then stalled=$((stalled + step)); fi
  if [ "$1" = unread ]; then unread_for=$((unread_for + step)); else unread_for=0; fi
}
while :; do
  unread=false

  # A wait can be long, and the pull request may no longer be the one read before it: merged
  # or closed by someone, made a draft, its label taken off by whoever changed their mind, or
  # pushed to, in which case that commit's own runs decide. Read again on every round after
  # the first, so the wait ends as soon as that happens.
  if [ "$waited" -gt 0 ]; then
    if now=$(gh pr view "$pr" --json state,isDraft,labels,headRefOid \
      --jq '"\(.state) \(.isDraft) \(any(.labels[]; .name == "merge-when-green")) \(.headRefOid)"'); then
      if [ "$now" != "OPEN false true $head" ]; then
        echo "#$pr changed during the wait (state, draft, labeled, head: $now): not merged by this run."
        exit 0
      fi
    else
      unread=true
    fi
  fi

  # The head commit's runs GitHub started on its own that are still going: one whose jobs
  # have no check yet shows nowhere else. A read that fails is no answer, never "none".
  if ! going=$(GH_TOKEN="$GITHUB_TOKEN" gh api --method GET "repos/$GH_REPO/actions/runs" \
    -f head_sha="$head" -f event=dynamic -f per_page=100 \
    --jq '[.workflow_runs[] | select(.status != "completed") | (.id | tostring)]'); then
    unread=true
    going='[]'
  fi

  # gh exits with 8 while checks are pending and with 1 when one failed or none exists. Those
  # are answers here, not errors, so the JSON it prints is what gets inspected. A pull request
  # whose changes run no check workflow (docs only) has nothing to wait for. GITHUB_TOKEN reads
  # them, since it is granted checks: read and the personal token isn't. An error on the first
  # read fails the job, as it always did; one in the middle of a wait is read again, for 2
  # minutes, since giving up there would leave a green pull request with nothing to merge it.
  checks=$(GH_TOKEN="$GITHUB_TOKEN" gh pr checks "$pr" --json name,workflow,bucket,link 2> "$RUNNER_TEMP/checks.err") || true
  if ! jq -e 'type == "array"' <<< "$checks" > /dev/null 2>&1; then
    if ! grep -q 'no checks reported' "$RUNNER_TEMP/checks.err"; then
      cat "$RUNNER_TEMP/checks.err"
      if [ "$waited" -eq 0 ] || [ "$unread_for" -ge "$short_wait" ]; then
        exit 1
      fi
      pause unread
      continue
    fi
    checks='[]'
  fi
  others=$(jq '[.[] | select(.workflow != "merge-when-green")]' <<< "$checks")

  # GitHub sometimes leaves a job's check in progress after its workflow run completed: the
  # job has no steps and never ends, so waiting on it would wait forever. A pending check
  # whose run completed takes the run's conclusion instead: a run succeeds only when every
  # job of it did. A run still going that GitHub started on its own joins the ones listed
  # above, which may have been read before it existed. One that can't be read may be either.
  unknown='[]'
  pending_runs=$(jq -r '.[] | select(.bucket == "pending") | .link // ""
    | capture("/actions/runs/(?<id>[0-9]+)/job/")? | .id' <<< "$others" | sort -u)
  for run_id in $pending_runs; do
    if ! run=$(GH_TOKEN="$GITHUB_TOKEN" gh api "repos/$GH_REPO/actions/runs/$run_id" \
      --jq '"\(.status) \(.conclusion // "none") \(.event)"'); then
      unknown=$(jq -c --arg id "$run_id" '. + [$id]' <<< "$unknown")
      continue
    fi
    read -r status conclusion event <<< "$run"
    if [ "$status" != completed ]; then
      if [ "$event" = dynamic ]; then
        going=$(jq -c --arg id "$run_id" '. + [$id] | unique' <<< "$going")
      fi
      continue
    fi
    case "$conclusion" in
      success) bucket=pass ;;
      skipped) bucket=skipping ;;
      cancelled) bucket=cancel ;;
      *) bucket=fail ;;
    esac
    echo "::warning::Run $run_id completed ($conclusion) with a check still in progress: counted as $bucket."
    others=$(jq --arg id "$run_id" --arg bucket "$bucket" 'map(if .bucket == "pending"
      and ((.link // "") | contains("/actions/runs/\($id)/job/")) then .bucket = $bucket else . end)' <<< "$others")
  done

  # A gate that held its workflow's checks for ai-review's open threads is the only
  # review-gate check that fails. Its run completes failed, which starts this workflow too.
  held=$(jq '[.[] | select((.name | startswith("review-gate")) and .bucket == "fail")] | length' <<< "$others")
  unfinished=$(jq -r '.[] | select(.bucket != "pass" and .bucket != "skipping")
    | select(((.name | startswith("review-gate")) and .bucket == "fail") | not)
    | "\(.name): \(.bucket)"' <<< "$others")

  # What each check left says: a failed one, or a pending one of a run of the repository's
  # own workflows, ends this run at once, since that run's completion starts another. The
  # others start nothing when they turn green: no run owns the check, a dynamic run does, or
  # its run couldn't be read.
  left=$(jq -c --argjson going "$going" --argjson unknown "$unknown" '[.[]
    | select(.bucket != "pass" and .bucket != "skipping")
    | select(((.name | startswith("review-gate")) and .bucket == "fail") | not)
    | (((.link // "") | capture("/actions/runs/(?<id>[0-9]+)").id) // null) as $run
    | if .bucket != "pending" then "blocking"
      elif $run == null then "unowned"
      elif any($going[]; . == $run) then "dynamic"
      elif any($unknown[]; . == $run) then "unowned"
      else "blocking" end]' <<< "$others")
  if jq -e 'any(.[]; . == "blocking")' <<< "$left" > /dev/null; then
    echo "#$pr is not green yet:"
    echo "$unfinished"
    exit 0
  fi

  if [ -n "$unfinished" ]; then
    limit=$short_wait
    if jq -e 'any(.[]; . == "dynamic")' <<< "$left" > /dev/null; then limit=$long_wait; fi
    if [ "$waited" -ge "$limit" ]; then
      echo "#$pr is not green yet, after $((waited / 60)) minutes of waiting:"
      echo "$unfinished"
      exit 0
    fi
    echo "Waiting for the checks whose completion starts no workflow:"
    echo "$unfinished"
    pause
    continue
  fi

  # Every check there is passed. Merging still takes this round's reads to have answered.
  if [ "$unread" = true ]; then
    if [ "$unread_for" -ge "$short_wait" ]; then
      echo "::warning::#$pr looks green, but GitHub's API failed to say whether it still is, or which runs are going: not merged by this run."
      exit 0
    fi
    echo "A read of GitHub's API failed: reading again."
    pause unread
    continue
  fi

  # A run GitHub started on its own may not have given its jobs their checks yet, so it gets
  # 2 minutes to. Past that it is a run with no check on the commit (a run stuck in progress,
  # or one that never checks anything), which holds no merge: it is named and left behind.
  if [ "$going" != '[]' ]; then
    if [ "$stalled" -lt "$short_wait" ]; then
      echo "Waiting for the checks of run(s) $going, which GitHub started on its own."
      pause stalled
      continue
    fi
    echo "::warning::Run(s) $going of #$pr, which GitHub started on its own, are still going with no check on the commit: not waited for."
  fi
  break
done

# A job waiting on another (the checks behind review-gate) has no check until it starts, so
# while review-gate waits for the review, or right after it passes, every check can be green
# while its workflow still has jobs to run. A run of the head commit counts as unfinished
# until it completes; its completion starts this workflow again. So does a ai-review run
# of an earlier commit: it ends on the opening commit after pushing its fix commit, and its
# threads come last. Runs started by a label (ai-review-on-request, run-checks, this
# one) don't count: their job is skipped for any other label, and a skipped run starts
# nothing; a review on request shows as a pending check instead. The run that started this
# one counts as complete whatever the listing says, which can lag behind the event.
running=$(GH_TOKEN="$GITHUB_TOKEN" HEAD="$head" gh api --method GET "repos/$GH_REPO/actions/runs" \
  -f branch="$HEAD_BRANCH" -f event=pull_request -f per_page=100 \
  --jq '.workflow_runs[] | select(.status != "completed" and .id != (env.TRIGGER_RUN_ID | tonumber))
    | select(.path | test("^\\.github/workflows/(ai-review-on-request|run-checks|merge-when-green)\\.yml$") | not)
    | select(.head_sha == env.HEAD or .path == ".github/workflows/ai-review.yml")
    | "\(.name) (\(.head_sha[:7]))"')
if [ -n "$running" ]; then
  echo "#$pr still has workflows running:"
  echo "$running"
  exit 0
fi

{
  echo "pr=$pr"
  echo "head=$head"
  echo "base=$base"
  echo "held=$held"
} >> "$GITHUB_OUTPUT"
