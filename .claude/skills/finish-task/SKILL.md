---
name: finish-task
description: "Tear down a finished task once its PR has merged: remove its worktree, or switch back to main in-place, then delete the branch locally and on GitHub, delete the task's Android emulator and iOS simulator and any left behind by dropped tasks, and sync origin."
---

# Finish Task

This skill cleans up after `start-task`, in either mode it can create a task in:

- **Worktree mode**: removes the worktree under `.claude/worktrees/` and deletes its branch.
- **In-place mode**: switches the main checkout back to `main` and deletes the branch that was
  checked out there.

In both modes it also deletes the Android emulator and the iOS simulator `start-task` created for
the task, if there are any, and the ones of tasks that were dropped without this skill.

In both modes this only runs after confirming the branch's PR has actually merged. Running this
skill (or the user asking to clean up / finish the task) is itself the user's authorization to
remove the worktree, the branch (local and remote) and the task's devices once that merge is
verified: do not ask for a second confirmation before step 4's cleanup. Still stop and ask before
anything not covered by that authorization: an unmerged branch (step 3), or discarding uncommitted
changes (see Edge cases).

All commands in this skill must run from the **main repo checkout**, not from inside a worktree
being removed: a worktree cannot remove itself, and `git branch -D` on a branch checked out
elsewhere will fail anyway.

## Step-by-step workflow

### 1. Identify the task, branch, and mode

```bash
git worktree list
git branch --show-current
```

- If the user named a task that matches a path under `.claude/worktrees/`, or the current working
  directory is inside one, that's **worktree mode**. Get its branch:
  ```bash
  git -C .claude/worktrees/<name> branch --show-current
  ```
- Otherwise, if the main checkout is currently on a branch other than `main`, that's **in-place
  mode**: the branch to clean up is that current branch.
- If it's ambiguous (e.g. more than one worktree exists and no task was named, or the main checkout
  is also mid-task), ask the user which task to finish.

### 2. Sync with origin

```bash
git remote update origin
```

### 3. Verify the branch is actually merged

Prefer checking the PR directly:

```bash
gh pr view <branch> --json state,mergedAt,headRefOid
```

If `gh` isn't available or there's no PR, fall back to:

```bash
git log origin/main..<branch> --oneline
```

An empty result means every commit on the branch is already in `origin/main`. PRs here are squash
merged, so a merged branch usually still lists its commits in this fallback: trust the PR state
whenever there is a PR.

**If the branch is not merged** (PR still open, no PR at all, or commits not in `origin/main`),
stop. Tell the user exactly what's unmerged and ask for explicit confirmation before proceeding:
never delete an unmerged task silently.

### 4. Clean up

State exactly what's about to happen (the worktree path and branch being removed, or "switch the
current checkout back to `main` and delete `<branch>`" for in-place, plus the devices being
deleted), then proceed without waiting for a further confirmation; the merge check in step 3 plus
the user's request to finish the task already authorize this.

If the task left a device running (an emulator, a simulator, the web dev server of the worktree),
shut it down first, as the "Task devices" section of
[`start-task`](../start-task/SKILL.md#task-devices) describes.

**Worktree mode:**

```bash
git worktree remove .claude/worktrees/<name>
git branch -D <branch>
```

**In-place mode:**

```bash
git checkout main
git pull
git branch -D <branch>
```

A squash merge replaces the branch's commits with a single new one on `main`, so git never sees the
local branch as merged and `git branch -d` refuses it. Use `-D` only here, after step 3 confirmed
the PR merged.

**The remote branch, in both modes:** `create-pr` merges without `--delete-branch`, so unless the
repository deletes head branches on merge, the branch is still on GitHub. Delete it there too, then
prune:

```bash
git ls-remote --exit-code --heads origin <branch> > /dev/null && git push origin --delete <branch>
git remote update origin --prune
```

**The task's devices, in both modes:** after the branch is deleted, delete every
`BoraRachar_*` emulator and simulator whose branch no longer exists: this task's, now that its branch
is gone, and any left behind by a dropped task. Every task device has a local branch while its task
is alive, because a device is only ever created after its task's branch. The name is the branch's
short description with `-` turned into `_` (see the "Name" section of
[`start-task`](../start-task/SKILL.md#name)).

Android emulators. After its first boot an AVD takes about 2.3 GB of disk, freed only when the AVD
is deleted:

```bash
setopt null_glob 2>/dev/null
for INI in ~/.android/avd/BoraRachar_*.ini; do
  [ -e "$INI" ] || continue
  AVD=$(basename "$INI" .ini)
  git for-each-ref --format='%(refname:short)' refs/heads \
    | sed 's#^[^/]*/##; s#-#_#g' | grep -qx "${AVD#BoraRachar_}" && continue
  pkill -f -- "-avd $AVD( |$)"
  while pgrep -f -- "-avd $AVD( |$)" > /dev/null; do sleep 1; done
  rm -rf ~/.android/avd/"$AVD".avd "$INI"
  echo "deleted $AVD"
done
```

iOS simulators:

```bash
xcrun simctl list devices | sed -nE 's/^ +(BoraRachar_[A-Za-z0-9_]+) \(([0-9A-F-]{36})\).*/\1 \2/p' \
  | while read -r SIM UDID; do
      git for-each-ref --format='%(refname:short)' refs/heads \
        | sed 's#^[^/]*/##; s#-#_#g' | grep -qx "${SIM#BoraRachar_}" && continue
      xcrun simctl shutdown "$UDID" 2>/dev/null
      xcrun simctl delete "$UDID" && echo "deleted $SIM"
    done
```

- `setopt null_glob` is for zsh, the default shell here: without it, a glob with no match aborts the
  whole command with "no matches found". Bash has no `setopt`, and there the `[ -e "$INI" ]` guard
  skips the unmatched pattern instead.
- Only `BoraRachar_*` devices are ever deleted. `Medium_Phone`, `Pixel_9`, `Pixel_Tablet`, the stock
  iPhone and iPad simulators and every other project's devices (`BiblePlanner_*`, `TuneScout_*`)
  belong to the user or to another project.
- A task whose branch still exists keeps its devices, even if nobody is working on it. Finishing or
  deleting that branch is what frees them.

### 5. Verify cleanup

```bash
git worktree list
git branch --list <branch>
git ls-remote --heads origin <branch>
~/Library/Android/sdk/emulator/emulator -list-avds | grep '^BoraRachar_'
xcrun simctl list devices | grep 'BoraRachar_'
```

None of them should show the removed task anymore, and every `BoraRachar_*` emulator or simulator
left should belong to a branch that still exists. Report the result to the user, including which
devices were deleted.

## Edge cases

- **Worktree mode:** if `git worktree remove` fails because of uncommitted changes (modified or
  untracked files) in the worktree, stop and show what's there. Only pass `--force` after the user
  explicitly confirms discarding those changes. Git-ignored files (`node_modules`, `build/`, the
  generated Xcode project) don't block the removal and go with the worktree.
- **In-place mode:** if `git checkout main` fails because of uncommitted changes, stop and ask the
  user how to proceed (commit, stash, or discard); don't discard anything without asking.
- If the local branch has commits that weren't in the merged PR (`git rev-parse <branch>` differs
  from `gh pr view <branch> --json headRefOid`), stop and show them with
  `git log <headRefOid>..<branch> --oneline` before deleting.
- If the remote branch has commits that weren't in the merged PR
  (`git rev-parse origin/<branch>` differs from the PR's `headRefOid`), don't delete it: tell the
  user, since someone pushed to it after the merge.
- If a worktree directory was already deleted from disk without `git worktree remove` (it will show
  as `prunable` in `git worktree list`), just run `git worktree prune` and then delete the branch as
  in step 4.
