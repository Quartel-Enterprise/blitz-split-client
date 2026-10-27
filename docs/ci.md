# CI

The workflows under `.github/workflows`, in two groups: the checks that build and test the app, and the Claude
review that runs before them.

| Workflow | Runs on | What it does |
| --- | --- | --- |
| `build-and-test` | Pull requests and `main`, except docs, `.claude` and `iosApp` only changes | `jvmTest` and `:koverVerifyCi`, `:androidApp:assembleRelease`, `npm --prefix webApp run build` |
| `static-analysis` | Kotlin, Gradle scripts, `.editorconfig`, the version catalog | `./scripts/ktlint.sh` |
| `module-graph` | Gradle scripts, `build-logic`, the version catalog, the wrapper | `./gradlew assertModuleGraph` |
| `claude-review` | A pull request opened, or a draft marked ready | Reviews it, fixes what it found, posts the rest |
| `claude-review-on-request` | The `ai-review` label | Reviews it again, only commenting |
| `run-checks` | The `run-checks` label | Re-runs the checks the gate held, once the review's threads are resolved |

The Apple app isn't built here, since it needs a macOS runner: `create-pr` builds both schemes locally.

## AI review

Claude reviews a pull request at three points:

- **Before it opens**, `create-pr` runs `/code-review high --max-findings all` and `review-conventions`, on the
  author's machine.
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

The shared steps live in `.github/actions/claude-review`:

- `action.yml` hands the review the earlier findings, runs `anthropics/claude-code-action` with a read-only tool
  allowlist (plus `Edit` inside the checkout with `fix`, never under `.git`, `.github` or `.claude`), and posts the
  findings with the workflow's token when the session didn't.
- `stop-hook.sh`, the session's Stop hook, reads the findings JSON the model ends its reply with, runs
  `commit-fixes.sh`, then `post-review.sh`. It runs inside the session, so it posts and pushes as `claude[bot]` with
  the Claude GitHub App's token, and the push starts the pull request's workflows on the fix commit.
- `post-review.sh` posts every finding as one review, anchored to the reviewed commit; a finding outside the diff goes
  in the review body.

The action hides Claude's output, since this repository's logs are public. A tool the review was denied shows as a
warning on the run, with only its name: allow it in `action.yml` when the review needs it.

The runner's environment holds the Claude OAuth token and the GitHub tokens, and a prompt injection in a diff could
ask the review to quote one in a public comment, or to overwrite what runs with the App's token. So:

- The session reads files only through the Read, Grep and Glob tools, and only in the checkout and in a copy of the
  files the pull request changes, as of the reviewed commit (for the suggestions' lines). Every other path is
  denied, and the deny rules repeat it for `/proc`, `/sys`, `/etc`, `$RUNNER_TEMP` (where checkout keeps its
  credentials), the checkout's `.git` and the home's dot directories. Neither review persists checkout's
  credentials. The session's Bash commands are `gh`'s read-only ones, which take the diff from GitHub
  (`gh pr diff`), and git commands that print no file: `cat`, `grep`, `jq`, `git diff`, `git log`, `git show` and
  `git blame` all read or write a file they are given (`--no-index`, `--contents`, `--output`).
- The hook's scripts are copied to a read-only directory outside the checkout, and what the hook acts on (when the
  review started, whether it commits fixes) comes in its arguments. The session can't write there, and the markers
  are checked anyway: the posting step looks the review up on GitHub, on any commit, and only trusts `.posted` when
  GitHub can't be read; a fix commit named by `.pushed` only counts when it is the checkout's head on top of the
  reviewed commit.
- `post-review.sh` masks anything shaped like a token (Claude's `sk-ant-`, GitHub's `ghs_`, `ghp_`, `github_pat_`…,
  a JWT, a Basic or Bearer value with a digit) before posting, and the hook's log goes through the same mask.

An otherwise encoded token would still get through the mask: only branches of this repository are reviewed, never
forks.

### Setup

- The [Claude GitHub App](https://github.com/apps/claude) installed on the repository.
- The `CLAUDE_CODE_OAUTH_TOKEN` secret, created with `claude setup-token` on the maintainer's account, so the reviews
  count against that Claude subscription. When it expires the review fails on authentication: create a new one and
  update the secret.
- The `ai-review` and `run-checks` labels.
- Optionally `GRADLE_ENCRYPTION_KEY` (`openssl rand -base64 16`), which lets the Gradle configuration cache survive
  between runs.
- A ruleset on `main` with **Require conversation resolution before merging**. This is what keeps a pull request
  with open review threads from merging: the checks the gate holds are skipped, not failed, and GitHub counts a
  skipped required check as passing.

No check is required in that ruleset. Every check workflow is path filtered, so a required one would leave a pull
request it doesn't run on (docs only, `iosApp` only) waiting forever, and the gate's own `wait for review` job is
skipped on every push but the opening one, which also counts as passing. Without required checks, `create-pr`
merges only after `gh pr checks` shows every check of the head commit green.

## The review gate

The checks wait for the opening review instead of running next to it, so they don't run twice (once on the opening
commit, again on the fix commit):

- `build-and-test`, `static-analysis` and `module-graph` start with a `review-gate` job, the reusable
  `review-gate.yml`. On the commit a pull request opened with or a draft was marked ready on, and on the review's
  fix commit (a push by `claude[bot]`), it waits for the `claude-review` run of the reviewed commit to end. When the
  head moved meanwhile (the fix commit), it skips the checks here: they run on the new commit. Every other event (an
  author's push, a reopened pull request, `main`) skips the gate, and the checks run right away. A draft, a fork, a
  failed or cancelled review, one still running after 40 minutes or GitHub's API failing three times in a row let
  the checks run.
- Once the review ended, the gate counts its unresolved threads. With one open, the checks are skipped and the gate
  fails in its `Hold for the open threads` step, so the pull request shows why nothing ran. The author's next push
  runs them. Resolving the threads without a push (a finding answered instead of fixed) starts no workflow: add the
  `run-checks` label, which re-runs every held run of the head commit, or comments when a thread is still open.

`scripts/review_gate.sh` holds what these workflows agree on: which threads are the review's, and which runs were
held.

The fix commit isn't built before the push, since the review has no Gradle: a fix that breaks the build or ktlint
shows up in the checks of the fix commit. The local branch is one commit behind afterwards, so pull before pushing to
it again.

## Adding a workflow

- Scope it with `paths` or `paths-ignore`, so a docs-only change doesn't run a build.
- Use per-commit `concurrency` groups on `main` if it writes a Gradle cache, per-ref otherwise: a cancelled run saves
  no cache.
- If it checks the app on pull requests, start it with the `review-gate` job, with the same `if` as the other
  callers, make its other jobs need it, and add it to `GATED` in `scripts/review_gate.sh`, or `run-checks` won't
  re-run it when the gate held it.
- Set up Gradle through `./.github/actions/gradle` and pass `secrets.GRADLE_ENCRYPTION_KEY`.
