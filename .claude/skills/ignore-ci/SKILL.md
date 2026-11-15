---
name: ignore-ci
description: "Merge this session's open pull request right away without waiting for its checks, then run finish-task to clean up the task. Use when the user runs /ignore-ci or says to merge the PR now and skip, ignore or not wait for the CI (\"mergeia sem esperar o CI\", \"ignora o CI\")."
---

# Ignore CI

Merges the pull request of the task in this session now, whatever its checks say (pending, failed or
cancelled), and then cleans the task up with [`finish-task`](../finish-task/SKILL.md). Running this
skill is the user's authorization to skip the checks, to push the branch's local commits and to
merge: don't ask again for any of these. It is not an authorization to bypass anything GitHub itself
enforces: never pass `--admin`, never disable or edit a ruleset.

Checks are not required on `main` (every check workflow is path filtered, see
[docs/ci.md](../../../docs/ci.md#setup)), so `gh pr merge` merges at once. What the ruleset does
enforce is that every review conversation is resolved and that the branch merges cleanly; step 4
handles both.

## 1. Find the pull request

The pull request is the one of the current task's branch: the branch checked out in this session's
working directory (a worktree under `.claude/worktrees/`, or the main checkout in in-place mode).

```bash
branch=$(git branch --show-current)
gh pr view "$branch" --json number,url,state,headRefOid,mergeStateStatus,labels
```

- On `main`, or with no pull request for the branch, or more than one task this session could mean:
  ask the user which pull request to merge.
- Already `MERGED`: skip to step 5.
- `CLOSED`: tell the user and stop.

## 2. Stop the wait for green

If this session was driving the pull request (as `create-pr` step 12 leaves it), stop that first,
so nothing pushes a fix or re-adds a label after the merge:

- Stop the background wait loop of `create-pr`, if one is running (`TaskStop`).
- With the `ccd_pr` tools, turn Auto-fix off: `set_monitor` with `auto_fix: false` and the pull
  request's URL. With `subscribe_pr_activity`, call `unsubscribe_pr_activity`.
- Remove the `merge-when-green` label if it is on, so its workflow doesn't race the merge:

  ```bash
  gh pr edit "$branch" --remove-label merge-when-green
  ```

## 3. Push what the pull request is missing

```bash
git status --porcelain
git fetch origin "$branch"
git log "origin/$branch..HEAD" --oneline
```

- **Uncommitted changes**: stop and ask the user whether they belong in the pull request. If they
  do, commit them with `create-pr`'s commit message rules and push; never discard them.
- **Local commits not on `origin`**: push them (`git push`), since the merge takes only what is on
  GitHub and `finish-task` would then refuse a branch with commits the pull request lacks. Don't
  wait for the checks this push starts.
- **Commits on `origin` not in the local branch** (the opening `claude-review`'s fix commit, for
  one): they are part of the pull request and go in with the merge. Nothing to do.

## 4. Merge

```bash
gh pr merge "$branch" --squash
```

Squash, as every merge here, and without `--delete-branch`: in worktree mode it would try to check
out `main` inside the worktree, and `finish-task` deletes the branch anyway. Don't use
`gh pr merge --auto` or `set_auto_merge`: they would wait for the checks this skill is skipping.

If GitHub refuses the merge:

- **Unresolved review conversations**: list them (the query is in `create-pr`, outcome 3 of step 12)
  and show them to the user. Skipping the CI doesn't extend to dismissing a review, so ask whether
  to resolve them as they are, fix them first or keep the pull request open. Resolve them only on
  the user's yes, then merge again.
- **A conflict with `main`**: merge `origin/main` into the branch (a merge commit, never a rebase),
  resolve the conflicts keeping what both sides meant, push and merge again without waiting for the
  checks. Ask the user only when a conflict needs a product choice nobody wrote down yet.
- **Anything else** (a required review, a ruleset): tell the user GitHub's reason and stop. Never
  retry with `--admin`.

## 5. Finish the task

Once the merge succeeds, run the [`finish-task`](../finish-task/SKILL.md) skill right away in the same
turn, from the main checkout. It checks the merge again, then removes the worktree or switches back
to `main`, deletes the branch locally and on GitHub, and deletes the task's devices. Don't
duplicate its steps here.

Report to the user that the pull request was merged without waiting for the CI, with its link and
which checks were still pending or had failed at the merge (`gh pr checks <pr>`), since they now land
on `main` unverified. The checks that run on `main` after every merge (the iOS simulator tests, see
[docs/ci.md](../../../docs/ci.md)) still run there.
