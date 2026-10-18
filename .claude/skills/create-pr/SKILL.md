---
name: create-pr
description: "Create a pull request following all project conventions: ktlint formatting, AI review of the changes, the checks of every area the branch touches (shared Kotlin, Android, Apple, web), Conventional Commits title, description, assignee, and labels."
---

# Create PR

This skill formats the code, reviews the changes, runs the checks of every area the branch touches,
commits any uncommitted changes, pushes the branch, and opens a pull request with a clear title and
description inferred from the actual changes.

## Step-by-step workflow

### 1. Gather context

Run these in parallel:
- `git status -s` to check for uncommitted changes
- `git diff --stat` to see unstaged changes
- `git log --oneline -1` to get the latest commit
- `git rev-parse --abbrev-ref HEAD` to get the current branch name
- `git log --oneline main..HEAD` to see all commits on this branch
- `git diff main...HEAD --stat` to see total changes vs main

In worktree mode, run every command of this skill against the worktree: `git -C <worktree>`,
`./gradlew -p <worktree>`, `npm --prefix <worktree>/webApp`, `<worktree>/iosApp/...` (see step 7
of [`start-task`](../start-task/SKILL.md)).

### 2. Run ktlint formatter (if needed)

Check whether any `.kt` or `.kts` files are among the changed files gathered in step 1. If none are
present, skip this step entirely.

If there are Kotlin files changed, run from the project root:

```bash
./scripts/ktlint.sh --format
```

This is the ktlint CLI with the Quare ruleset
([ktlint-rules](https://github.com/Quartel-Enterprise/ktlint-rules)), pinned by
`quare-ktlint-rules` in `gradle/libs.versions.toml`. There is no Gradle ktlint task.

If the command exits with a non-zero code, stop and notify the user:
> "ktlint found issues it couldn't fix automatically. Please review and fix the reported errors, then try again."

Do not proceed until the formatter succeeds.

Formatting rewrites files in place, so re-run `git status -s` afterwards if you need an up-to-date
list of changed files for the steps below.

### 3. Check the current branch

```bash
git branch --show-current
```

Also check the upstream tracking branch:

```bash
git rev-parse --abbrev-ref --symbolic-full-name @{u} 2>/dev/null
```

**If the current branch is `main`**, or it has no type prefix and its upstream tracking branch is
`origin/main`, stop and ask whether the user wants to use the `start-task` skill instead: it's the
preferred way to start new work and will ask whether to use a worktree or work in-place. If they'd
rather branch directly here without going through that flow, continue to step 4 to determine the
prefix, then create the branch in step 5.

**If the current branch is already a feature/fix/enhancement/refactor/chore branch**, take the type
from its prefix and go directly to step 6. Its upstream may well be `origin/main`: that is how
`start-task` creates a branch, until step 10 pushes it.

### 4. Infer the change type

Inspect the uncommitted changes and any commits ahead of origin/main:

```bash
git remote update
git diff HEAD
git log origin/main..HEAD --oneline
```

Pick the `<type>` prefix using the table in [`branch-types.md`](../branch-types.md).

### 5. Create a new branch (if needed)

Derive the branch name from the changes; keep it short, lowercase, hyphen-separated:

```bash
git checkout -b <type>/<short-description>
```

Examples: `fix/offline-notice-overlaps-buttons`, `feature/create-group`,
`enhancement/restore-tablet-layout`, `refactor/welcome-viewmodel-sign-in`, `chore/xcodegen-schemes`.

### 6. Write the pull request description

Write the description now, before the review: the reviewers in step 7 don't see this conversation,
so the description is the only place they learn what the change is meant to do. Write it in English,
following [docs/architecture/pr-guidelines.md](../../../docs/architecture/pr-guidelines.md), at a
medium level of detail:

- Describe the problem being solved or the feature being added
- Describe the expected behavior, and the edge cases it covers (offline, no account vs Google or
  Apple, an anonymous user restoring on a new device, phone vs tablet or landscape) when they matter
- Say which UIs the change reaches: Android, iOS/iPadOS, macOS, web. The ViewModel is shared, so a
  UI left out on purpose is worth one line, or a reviewer will take it for a gap
- Mention the affected screens or areas of the app
- Explain the approach taken, and any decision a reviewer could take for a mistake
- Do NOT list every file changed or describe code line by line
- Do NOT include purely internal implementation details (e.g. which class was refactored)
- Keep it to 3–8 sentences or a short bullet list

The same text becomes the pull request body in step 11.

### 7. Review the changes

Skip this step when the user's request says so (e.g. "sem review", "pula a review"), or when every
file the branch changes, as gathered in step 1 (commits ahead of `main` and uncommitted changes
alike), is one of these:

- Markdown (`.md`), skills included
- `version.properties`, the only file a version bump touches
- `gradle/libs.versions.toml`, for a dependency bump with no code changes

Those changes have no code for the reviews to judge, and the two reviews take minutes. Say in one
line that the review was skipped and why, then go on to step 8.

Two reviews run on the whole change, the commits ahead of `main` and the uncommitted changes alike.
Both start with a fresh context, so they judge the change rather than the conversation that produced
it:

- `/code-review high --max-findings all` looks for correctness bugs and for cleanups of the changed
  code (reuse, simplification, efficiency).
- `review-conventions` checks the change against the docs under `docs/`, the product invariants of
  Bora Rachar, and the description from step 6.

`/code-review` reads `git diff @{upstream}...HEAD` plus `git diff HEAD`. The second leaves out files
git doesn't track yet, so mark new files as intended to be added first:

```bash
git add -N .
```

The first covers the whole branch only while its upstream is `origin/main`. Once the branch has been
pushed, its upstream is its own remote branch, and the pushed commits would drop out of the review.
In that case, commit any uncommitted changes now (step 9) and pass the branch name as the target.

Then invoke the `code-review` skill with `high --max-findings all` as its arguments
(`high <branch> --max-findings all` for a pushed branch): without the flag it keeps only the ten
most severe findings. The skill remembers the flag, so the user's own `/code-review` runs report
every finding too, until they pass `--max-findings default`. Invoke the `review-conventions` skill
with the description from step 6 as its argument. Wait for both results before going on.

If neither reports anything, continue to step 8. Otherwise stop and show the user every finding,
grouped by review, then use the `AskUserQuestion` tool to ask "Como quer seguir com os apontamentos
da review?", with these options:

- "Corrigir os apontamentos" (all of them, or the ones the user picks)
- "Seguir sem mudanças"

After fixing, run step 2 again if a Kotlin file changed, and update the description from step 6 if
what the branch does changed. Don't run the reviews again unless the user asks.

### 8. Run the checks of each area the branch touches

If every file the branch changes is Markdown (`.md`), as gathered in step 1 (commits ahead of
`main` and uncommitted changes alike), skip this step. Nothing builds those files, and the checks
take minutes.

Otherwise, sort the changed files into the areas below and run the checks of every area that has at
least one. Pass `--continue` to Gradle so one failure doesn't hide the next.

| Area | The branch changes | Checks |
|---|---|---|
| Shared Kotlin | `core/`, `feature/`, `shared/` | Tests and coverage first, then the full build |
| Build | `build-logic/`, `gradle/`, any `*.gradle.kts`, `gradle.properties`, `version.properties` | The full build and the module graph |
| Android UI | `android/`, `androidApp/` | The debug APK and Android lint, unless the full build already ran |
| Apple UI | `iosApp/`, or the API Swift sees: `shared/`, `core/{model,presentation,navigation}`, `feature/*/src/commonMain/**/presentation`, `version.properties` | Regenerate the Xcode project, build both schemes |
| Web UI | `webApp/`, or the API TypeScript sees: `shared/src/jsMain`, `feature/*/src/commonMain/**/presentation` | The production build of the web app |

**Shared Kotlin**, tests and coverage:

```bash
./gradlew jvmTest koverVerify --continue
```

`jvmTest` runs every module's `commonTest` on the JVM target, and `koverVerify` holds the merged
line coverage of `core/*` and `feature/*` at 80% (the `kover` block of the root `build.gradle.kts`
says what is left out: `*.di`, `*.bridge`, generated code, the `:testing` fakes and
`:core:provider:*`). If a test or the coverage rule fails, stop and report it: write the missing
tests rather than pushing a red branch, through the `write-tests` skill when the "Who writes the
unit tests" section of [docs/ai_agents.md](../../../docs/ai_agents.md) says so.

**Shared Kotlin and Build**, the full build:

```bash
./gradlew build --continue
```

It compiles every target of every module (Android, iOS, macOS, JS, JVM), links the `Shared`
framework and the JS library, runs every test suite (the JS ones in a headless browser, the Apple
ones on the simulator runtime), runs Android lint on `:androidApp` and the `:android:*` modules, and
the root `check` runs `koverVerify` again. For a **Build** change, also run the module graph rules
of the root `build.gradle.kts`, which `build` doesn't:

```bash
./gradlew assertModuleGraph
```

**Android UI**, when the full build didn't run:

```bash
./gradlew :androidApp:assembleDebug :androidApp:lintDebug :android:<module>:lintDebug --continue
```

with one `:android:<module>:lintDebug` for each changed module (`:android:designsystem`,
`:android:platform`, `:android:feature:<name>`).

**Apple UI**: regenerate the project, since `iosApp/BoraRachar.xcodeproj` is git-ignored and may
predate `project.yml`, then build both schemes. The project's build phase builds the `Shared`
framework with Gradle first.

```bash
./iosApp/generate-project.sh
xcodebuild -project iosApp/BoraRachar.xcodeproj -scheme BoraRachar-iOS \
  -destination 'generic/platform=iOS Simulator' -derivedDataPath iosApp/build/DerivedData build -quiet
xcodebuild -project iosApp/BoraRachar.xcodeproj -scheme BoraRachar-macOS \
  -destination 'platform=macOS' -derivedDataPath iosApp/build/DerivedData \
  CODE_SIGN_IDENTITY=- CODE_SIGN_STYLE=Manual DEVELOPMENT_TEAM= build -quiet
```

The iOS build targets the simulator, so it needs neither a device nor a signing identity. The macOS
build is signed ad hoc (`CODE_SIGN_IDENTITY=-`), so it doesn't need the team's certificate. If
signing still fails on the entitlements that require a provisioning profile (Sign in with Apple,
associated domains), build it again with `CODE_SIGNING_ALLOWED=NO` instead of the three signing
settings: it still compiles and links everything, which is what this check is for. If XcodeGen is
missing, ask the user before installing it (`brew install xcodegen`).

**Web UI**:

```bash
npm --prefix webApp run build
```

Its `prebuild` script builds the JS library of `:shared` with Gradle first, then `tsc -b` type
checks the app against the library's TypeScript definitions and Vite bundles it. If
`webApp/node_modules` is missing, run `npm --prefix webApp install` first.

If any check fails, stop and report which one and why. Fix it before going on; never push a branch
whose checks fail.

### 9. Commit all uncommitted changes

Stage everything and create a single commit:

```bash
git add -A
git commit -m "<type>: <short description>"
```

The commit message must:
- Start with the type prefix (`fix:`, `feature:`, `enhancement:`, `refactor:`, `chore:`)
- Be concise and in the imperative form (e.g. "fix: keep the account buttons below the offline notice")
- Not exceed 72 characters

If there are no uncommitted changes (the user already committed everything), skip this step.

### 10. Push the branch

```bash
git push -u origin HEAD
```

The repository is `Quartel-Enterprise/blitz-split-client`. If `origin` isn't set or the push is
rejected for lack of access, stop and tell the user. Never force push.

### 11. Create the pull request

Check first that the branch has no open pull request yet:

```bash
gh pr view --json number,state,url 2>/dev/null
```

If one is open, tell the user and give its URL instead of creating a duplicate. Otherwise:

```bash
gh pr create \
  --base main \
  --title "<type>: <short description>" \
  --body "<description>" \
  --assignee @me \
  --label "<label>"
```

Take the label from the table in [`branch-types.md`](../branch-types.md). `chore` has no label:
drop the `--label` flag for it. The repository is new, so the label may not exist yet
(`gh label list`): `gh pr create` then fails. Ask the user "O label `<label>` ainda não existe no
repositório. Quer que eu crie?" and, on a yes, create it with `gh label create <label>` before
creating the pull request. On a no, create the pull request without `--label`.

If `gh` is not installed, ask the user before installing it (`brew install gh`, then
`gh auth status || gh auth login`).

**Title:** Same format as the commit message: type prefix + short imperative description.

**Description:** The one written in step 6, updated if the review fixes changed what the branch
does.

## Example

Given uncommitted changes that move the offline notice of the welcome screen above the account
buttons in the Compose, SwiftUI and React UIs:

- **Branch:** `fix/offline-notice-overlaps-buttons`
- **Commit:** `fix: show the offline notice above the account buttons`
- **PR title:** `fix: show the offline notice above the account buttons`
- **PR description:**
  > Without internet, the welcome screen's "Sem conexão" notice was drawn over the Google and Apple
  > buttons on phones in landscape, hiding the disabled state. The notice now sits above the buttons
  > in the actions column on Android, iOS/iPadOS, macOS and the web, and the buttons keep their
  > disabled look until the device is back online. "Usar sem conta" stays enabled offline. No change
  > to `WelcomeViewModel`: `showsOfflineNotice` and `areAccountButtonsEnabled` already drove both.

### 12. Squash and merge (optional)

If the user's request already asked for the merge (e.g. "abre o PR e mergeia"), skip the question
and merge. Otherwise, after the PR is created, use the `AskUserQuestion` tool to ask "Quer que eu
faça o squash merge agora?", with these options:

- "Sim, faz o squash merge e limpa a task"
- "Não, para por aqui"

If yes, check whether the pull request has any checks to wait for. Right after the push GitHub may
not have registered them yet, so give it up to a minute:

```bash
for _ in $(seq 20); do gh pr checks 2>/dev/null | grep -q . && break; sleep 3; done
gh pr checks
```

- **The repository has checks** (workflows under `.github/workflows`): wait for the required ones,
  then merge.

  ```bash
  gh pr checks --watch --required --interval 10
  gh pr merge --squash
  ```

  If a required check fails, report it and stop.
- **It has none** ("no checks reported", e.g. before CI is set up): the checks of step 8 are the
  only gate. Merge only if they ran and passed in this session for the commit being merged;
  otherwise run them again first.

  ```bash
  gh pr merge --squash
  ```

Never bypass a branch protection rule with `--admin`. If `gh pr merge` refuses (a required review,
a ruleset, a conflict with `main`), notify the user with the reason and stop. Don't pass
`--delete-branch`: in worktree mode it would try to check out `main` inside the worktree.
`finish-task` deletes the branch, locally and on GitHub.

Once the squash merge succeeds, immediately run the `finish-task` skill in the same turn; don't
wait for the user to ask for it. Asking for the squash merge is also the request to clean up the
task. `finish-task` handles both worktree and in-place tasks and checks the merge before removing
the branch (and the worktree, the emulator and the simulator, if there are any), so don't duplicate
its logic here.

## Edge cases

- If `./scripts/ktlint.sh --format` fails, stop immediately; do not commit or push. The remaining
  errors are ones ktlint cannot autocorrect (e.g. custom `quare:*` rules such as
  `quare:comments-say-why` or the test structure rules), so they must be fixed by hand
- If there is nothing to commit and the branch is already pushed, still write the description and
  run the reviews (steps 6 and 7), then go directly to PR creation
- If the branch already has an open PR, notify the user instead of creating a duplicate
- Always target `main` as the base branch for the PR
- Never force push, and never rewrite commits that are already on `origin`
