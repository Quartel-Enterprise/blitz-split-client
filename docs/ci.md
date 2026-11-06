# CI

The workflows under `.github/workflows`: the checks that build and test the app, the Claude review that runs before
them, and `merge-when-green`, which merges a pull request once they pass.

| Workflow | Runs on | What it does |
| --- | --- | --- |
| `build-and-test` | Pull requests and `main`, except docs, `.claude` and `iosApp` only changes | `jvmTest` and `:koverVerifyCi`, `jsTest` split across shards, `:androidApp:assembleRelease`, `lintDebug`, `npm --prefix webApp run build`, each in a job of its own |
| `apple` | The shared Kotlin, `iosApp`, the build (on a macOS runner) | `iosSimulatorArm64Test`, `macosArm64Test` and `compileKotlinIosArm64`; both schemes of the Apple app, which link the `Shared` framework |
| `static-analysis` | Kotlin, Gradle scripts, `.editorconfig`, the version catalog | `./scripts/ktlint.sh` |
| `duplication` | Kotlin, Swift, `webApp/src`, the jscpd config and baseline, `scripts/cpd.sh` | `./scripts/cpd.sh`, one warning when the baseline takes in copied code against the base branch, with each file in the job summary |
| `module-graph` | Gradle scripts, `build-logic`, the version catalog, the wrapper | `./gradlew assertModuleGraph` |
| `claude-review` | A pull request opened, or a draft marked ready | Reviews it, fixes what it found, posts the rest |
| `claude-review-on-request` | The `ai-review` label | Reviews it again, only commenting |
| `run-checks` | The `run-checks` label | Re-runs the checks the gate held, once the review's threads are resolved |
| `claude-review-refs` | Pull requests that change a workflow | Fails when the workflows pin different releases of the shared review |
| `merge-when-green` | The `merge-when-green` label, each pull request workflow that completes, every push to `main`, `build-and-test` completing on `main`, every 15 minutes (when GitHub fires it), and by hand | Squash merges the pull request once every check passed and every conversation is resolved |
| `release-beta` | By hand, with `./scripts/release/release-beta.sh` | Ships a beta to the closed group: Android on Firebase App Distribution, the web on Cloudflare Pages, iOS and macOS on TestFlight. Merges the versionCode back into `main` and publishes the `X.Y.Z-beta_N` GitHub pre-release; see [release-process.md](release-process.md) |

Before a pull request opens, `create-pr` runs only the fast part of these on the author's Mac: the JVM tests and
coverage, a compile of the shared code for the iOS simulator and JS, the debug APK, and the UI the branch touches (the
web build, or the Xcode scheme a change to `iosApp` reaches). The tests on the JS, iOS simulator and macOS targets, Android
lint, the release APK and the other Apple scheme are left to these workflows, and `merge-when-green` waits for them:
on one Mac, queued behind the other worktrees' builds, they were what made a branch slow to open.

## Speed and timeouts

`build-and-test` runs its jobs side by side, so its wall time is its slowest job's. Two of them are split to keep that
short:

- `js-tests` is the slowest: each module with tests links a test executable of its own and runs it in its own Karma
  and headless Chrome, and a change to a core module recompiles every module after it. Its matrix splits the modules
  across its shards, dealt round-robin from the sorted list of the modules that have `commonTest` or `jsTest` sources,
  found when the job runs, so a new module joins a shard on its own. Each shard compiles the modules its own depend on again, side by
  side with the others. Adding a shard is adding a number to the matrix.
- `android` builds the release APK (R8 runs only there) and `android-lint` runs `lintDebug`. They compile different
  variants and share no work, so in one job they ran one after the other.

Every job has a `timeout-minutes`; the ones that build get about three times what they take on a pull request that
recompiles everything. A runner that hangs then fails its check instead of holding it, and `merge-when-green` with it, for GitHub's
default of six hours.

Only `main` saves the Gradle caches, one set per job and shard, and pull requests read them. The repository's Actions
cache has 10 GB and drops the least recently used entries first, so it keeps the newest `main` commits' entries, the
ones pull requests read.

The `apple` jobs run on `xcode-27`, GitHub's macOS image with Xcode 27 (a public preview, since the app uses the iOS 27
SDK's APIs and `macos-26` only has Xcode 26), free for a public repository. Its tests and its app build are
two jobs side by side, each compiling the Kotlin/Native code on its own, since two runners finish sooner than one.
The Gradle action fits them to the runner: it selects the image's newest Xcode, as the Mac the app is developed on
runs, and it shrinks the Gradle and Kotlin daemons to its 7 GB of memory. Only
`main` saves their caches, and only `apple-tests` saves the Kotlin/Native toolchain and the Gradle home, so the two jobs
don't store the same entries twice.

## AI review

Claude reviews a pull request at three points:

- **Before it opens**, `create-pr` runs `/code-review high --max-findings all` and `review-conventions`, on the
  author's machine, and fixes their findings before pushing.
- **On GitHub, when it opens**, `claude-review` runs the same `/code-review high --max-findings all`, on Opus, with
  `--fix`: it applies the findings that are clear and contained, pushes them to the branch as a commit of
  `claude[bot]`, and posts one review. The fixed findings are listed in its body, under the fix commit; each one left
  for the author is an inline comment, labelled 🔴 **Must fix**, 🟡 **Should fix** or ⚪ **Nitpick**, with a
  suggestion to commit when the fix fits the commented lines. A draft is reviewed when it is marked ready. Later
  pushes, the fix commit included, aren't reviewed again.
- **On request**, the `ai-review` label runs `claude-review-on-request`: the same review, only commenting, told what
  the earlier reviews found so it doesn't repeat them. The label is removed when it ends. Use it after new pushes.

Both fail only when the run itself does (an expired token, the 30-minute timeout), never because of what they found.
They are skipped on pull requests from forks, which get no secrets. The action refuses to run when the pull request
changes the workflow file that started it, so a change to either review workflow can only be tried after it merges.

The review, the gate and the threads lookups are the `review`, `gate` and `threads` actions of
[Quartel-Enterprise/claude-review](https://github.com/Quartel-Enterprise/claude-review), shared with
bible-planner-mobile-client. Every `uses:` of them (`claude-review.yml`, `claude-review-on-request.yml`,
`review-gate.yml`, `run-checks.yml`, `merge-when-green.yml`) pins the same exact release: a fix there ships as a release, then a bump of all
of them here, which `claude-review-refs` checks. Dependabot opens that bump as one pull request the day after a
release (`.github/dependabot.yml`), and the review skips it. What runs comes from that release, never from the pull request under
review, so a pull request can't change the review that judges it. How the review works, which Bash commands it may
run and how it keeps the tokens out of its comments are in that repository's README.

The action hides Claude's output, since this repository's logs are public. A tool the review was denied, or a Bash
command its guard refused, shows as a warning on the run; allowing one is a change to the shared repository (see its
README, "Security"). Only someone who can push a branch to this repository can try a prompt injection, since forks
are never reviewed. If a token ever shows up in a comment, a commit or a log, revoke it: create a new OAuth token
with `claude setup-token` and update `CLAUDE_CODE_OAUTH_TOKEN`; the GitHub tokens expire with the job.

### Setup

- The [Claude GitHub App](https://github.com/apps/claude) installed on the repository.
- The `CLAUDE_CODE_OAUTH_TOKEN` secret, created with `claude setup-token` on the maintainer's account, so the reviews
  count against that Claude subscription. When it expires the review fails on authentication: create a new one and
  update the secret.
- The `ai-review`, `run-checks` and `merge-when-green` labels.
- The `MERGE_WHEN_GREEN_TOKEN` secret, for `merge-when-green` (see [Merge when green](#merge-when-green)).
- Optionally `GRADLE_ENCRYPTION_KEY` (`openssl rand -base64 16`), which lets the Gradle configuration cache survive
  between runs.
- A ruleset on `main` with **Require conversation resolution before merging**. This is what keeps a pull request
  with open review threads from merging: the checks the gate holds are skipped, not failed, and GitHub counts a
  skipped required check as passing.

No check is required in that ruleset. Every check workflow is path filtered, so a required one would leave a pull
request it doesn't run on (docs only, `iosApp` only) waiting forever, and the gate's own `wait for review` job is
skipped on every push but the opening one, which also counts as passing. Without required checks, `create-pr`
adds the `merge-when-green` label, which merges only once every check of the head commit is green, and waits with
`gh pr checks` itself only when the label can't merge (no `MERGE_WHEN_GREEN_TOKEN`) or when asked to merge right
away.

## The review gate

The checks wait for the opening review instead of running next to it, so they don't run twice (once on the opening
commit, again on the fix commit):

- `build-and-test`, `apple`, `static-analysis`, `duplication` and `module-graph` start with a `review-gate` job, the reusable
  `review-gate.yml`. On the commit a pull request opened with or a draft was marked ready on, and on the review's
  fix commit (a push by `claude[bot]`), it waits for the `claude-review` run of the reviewed commit to end (on a draft
  marked ready, only a run that event created, never the one skipped while it was a draft). When the
  head moved meanwhile (the fix commit), it skips the checks here: they run on the new commit. Every other event (an
  author's push, a reopened pull request, `main`) skips the gate, and the checks run right away. A draft, a fork, a
  failed or cancelled review, one still running after 40 minutes or GitHub's API failing three times in a row let
  the checks run.
- Once the review ended, the gate counts its unresolved threads. With one open, the checks are skipped and the gate
  fails in its `Hold for the open threads` step, so the pull request shows why nothing ran. The author's next push
  runs them. Resolving the threads without a push (a finding answered instead of fixed) starts no workflow: add the
  `run-checks` label, which re-runs every held run of the head commit, or comments when a thread is still open.

`review-gate.yml` is a thin wrapper of the shared `gate` action, which waits and counts the threads; the wrapper
only fails the hold, in its `Hold for the open threads` step. The shared `threads` action holds what `run-checks`
agrees on with the gate: which threads are the review's, and which runs were held (any failed run of the commit whose
`Hold for the open threads` step failed, so no list of the gated workflows has to be kept).

The fix commit isn't built before the push, since the review has no Gradle: a fix that breaks the build or ktlint
shows up in the checks of the fix commit. The local branch is one commit behind afterwards, so pull before pushing to
it again.

## Merge when green

Adding the `merge-when-green` label to a pull request makes the `merge-when-green` workflow squash merge it once
every check on its head commit has passed, then remove the label. GitHub's own auto-merge can't do this: it waits
only for the required checks, and none is required here (see [Setup](#setup)).

The workflow runs when the label is added and each time a pull request workflow completes, so the last check to
finish is the one that triggers the merge. GitHub loses some of those events: a run of this workflow that fails to start
(`startup_failure`), or a `workflow_run` never sent. So it also runs a `sweep`: a job that lists the open pull requests
with the label (drafts and forks left out) and checks each one the same way. GitHub's scheduler can't be what drives it:
it fired about one run in 26 of the `schedule` here (GitHub drops scheduled runs under load, and the workflow has no
`concurrency` setting that could cancel them). The sweep therefore runs on every push to `main` (each
merge), when `build-and-test` completes on `main`, every 15 minutes when GitHub does fire the schedule, and by hand.
Those only fire when something lands on `main`: with `main` idle, a pull request that missed its event waits for the
schedule or for someone to dispatch the workflow. To check them right away, run
the workflow by hand (**Actions**, `merge-when-green`, **Run workflow**), with a pull request number to check only that
one. The sweep only sees pull requests that still have the label: one it removed for an unresolved conversation needs
the label added again, as below. A push to the pull request moves its head commit, and the new checks have
to pass again. A failed check leaves the pull request open with the label on; re-running the check and getting it
green merges it. The pull request's current head is the one judged, whichever commit's run started the workflow, so
a pull request whose changes run no check workflow (docs only, `iosApp` only) merges once the review ends, even after
the review pushed its fix commit. No pull request workflow runs on every push here, so a label added, or a sweep run, right after a
push waits 30 seconds first, for the push's workflows to register their runs. Drafts wait until they are marked
ready, and pull requests from forks are never merged: their author could push after a maintainer added the label.

A job waiting on another has no check until it starts, so while `review-gate` waits for the review, or right after it
passes, every check of a pull request can be green while its workflows still have jobs to start. Every run of the
head commit therefore counts as unfinished until it completes, and its completion starts `merge-when-green` again. So
does a `claude-review` run of an earlier commit, which ends on the opening commit after pushing its fix commit. Only the
runs a label starts (`claude-review-on-request`, `run-checks`, `merge-when-green`) don't count.

GitHub sometimes leaves a job's check in progress after its workflow run completed: the job has no steps and never
ends. A check still pending in a completed run takes the run's conclusion instead (a run succeeds only when every job of
it did), with a warning on the `merge-when-green` run that names the run.

The `Main rules` ruleset requires every review conversation to be resolved before a pull request merges into `main`,
with no bypass, admins included, and `merge-when-green` never merges with `--admin`. When everything else is green
and a conversation is still open, it removes its label and comments on the pull request instead. Fix each finding,
or reply with why the code stays as it is, resolve the conversation, then add the label again: resolving one starts
no workflow. When the opening review's threads held the checks (see [The review gate](#the-review-gate)), the failed
gates don't count as failed checks: once every conversation is resolved, `merge-when-green` re-runs the held runs,
keeps its label and merges when they pass. A merge GitHub refuses with everything green (a conflict with `main`) also
removes the label, with a comment saying why.

The merge uses the `MERGE_WHEN_GREEN_TOKEN` secret, not `GITHUB_TOKEN`: a merge made with `GITHUB_TOKEN` triggers no
other workflow, so the push to `main` would run none of its checks. The merge shows up as made by the token's owner.
The held runs are re-run with it too, since a re-run made with `GITHUB_TOKEN` may not start `merge-when-green` when it
completes. Before the secret is set, or once the token expires, the workflow fails with an error that names the
secret. The checks are read with `GITHUB_TOKEN`, which the job grants `checks: read` and `statuses: read`.

**Manual setup** (Pierre): create a fine-grained personal access token restricted to this repository, with Actions,
Contents and Pull requests set to Read and write, and save it as the `MERGE_WHEN_GREEN_TOKEN` repository secret. Until then,
the label does nothing but fail the workflow.

When a workflow is added to or renamed in the pull request checks, update the `workflow_run` list in
`merge-when-green.yml` too. A workflow missing from it still blocks the merge while it runs, but its completion does
not trigger one. A new workflow started by a label goes in the list of label workflows of its `Check whether every
check passed` step instead: a run of it still queued would hold the merge, and its skipped completion would start
nothing.

## Adding a workflow

- Scope it with `paths` or `paths-ignore`, so a docs-only change doesn't run a build.
- Give every job a `timeout-minutes` (see [Speed and timeouts](#speed-and-timeouts)).
- Use per-commit `concurrency` groups on `main` if it writes a Gradle cache, per-ref otherwise: a cancelled run saves
  no cache.
- If it checks the app on pull requests, start it with the `review-gate` job, with the same `pull_request` types
  and `if` as the other callers, and make its other jobs need it.
- Set up Gradle through `./.github/actions/gradle` and pass `secrets.GRADLE_ENCRYPTION_KEY`.
- If it runs on pull requests, add it to the `workflow_run` list of `merge-when-green.yml` (see [Merge when green](#merge-when-green)).
