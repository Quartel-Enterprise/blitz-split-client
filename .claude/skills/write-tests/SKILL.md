---
name: write-tests
description: "Write the unit tests for a change from a fresh context, against what the change is meant to do rather than what the code happens to do. Use when the change needs 3 or more new unit test cases in core/* or feature/*, as the \"Who writes the unit tests\" section of docs/ai_agents.md counts them; not for a bug's regression test. Writes only test code and reports suspected bugs in the production code instead of fixing them. Pass the intent, the classes to cover and the worktree path as the argument."
argument-hint: "[intent, classes to cover, worktree path]"
context: fork
agent: general-purpose
background: false
---

# Write tests

You are writing the tests for someone else's change. You don't see the conversation that produced
it, and that is the point: the author's tests tend to restate their own code, bugs included. Derive
the cases from what the change is meant to do, then check the code against them.

## The request

<request>
$ARGUMENTS
</request>

It should give you:

- **The intent**: what the change is meant to do, and the edge cases that matter (offline, no
  account vs Google or Apple, an anonymous user, a restored user, an error from Supabase, an empty
  or mistyped input, a second sync).
- **The scope**: the classes to cover.
- **The worktree path**, when the change lives in one. Point every file tool at it, and run Gradle
  and git with `./gradlew -p <worktree>` and `git -C <worktree>`: a `cd` doesn't persist between
  Bash calls, so a bare `./gradlew` builds the main checkout.

If the intent is missing, stop and say so: without it, the tests can only restate the code. If the
scope is missing, take the production classes the branch adds or changes
(`git diff $(git merge-base origin/main HEAD) --stat`, plus `git ls-files --others --exclude-standard`).

Unit tests cover the shared Kotlin that Kover measures: `core/*` and `feature/*`, except the Koin
modules (`*.di`), the Swift and JS bridges (`*.bridge`), the `:testing` fakes and
`:core:provider:*`. The Compose, SwiftUI and React UIs have no unit tests: if the scope is only UI,
say so and stop.

## 1. Read the conventions

Read [docs/testing/README.md](../../../docs/testing/README.md) and, in full,
[given-when-then.md](../../../docs/testing/given-when-then.md),
[prepare-scenario.md](../../../docs/testing/prepare-scenario.md) and
[fakes-and-coroutines.md](../../../docs/testing/fakes-and-coroutines.md).

Before writing a fake, look for one in the `:testing` modules (`core/account/testing`,
`core/profile/testing`, and any other `core/*/testing` the branch adds), and read one or two
existing test files of the same module to match their style. The ones in the repository today
follow these patterns, which ktlint partly enforces in the test source sets
(`quare:test-name-given-when-then`, `quare:test-body-sections`, `quare:prepare-scenario-*`):

- **Place and tools**: `src/commonTest/kotlin/com/quare/blitzsplit/<module path>/...Test.kt`, with
  `kotlin.test` (`@Test`, `@BeforeTest`, `@AfterTest`, `assertEquals`, `assertIs`, `assertTrue`…)
  and `kotlinx.coroutines.test.runTest`. There is no mocking library: dependencies are fakes or the
  real implementations.
- **Names and body**: a backtick name `` `GIVEN <situation> WHEN <action> THEN <outcome>` `` in plain
  words, and a body split by `// Given`, `// When`, `// Then` comments.
- **`prepareScenario`**: a private function, the last member of the class, returning `Unit`, called
  in the Given section. Its parameters (with defaults) are the knobs a case turns
  (`online = false`, `storedProfile = null`, `connectResult = Result.failure(...)`); it builds the
  fakes and the class under test into `lateinit var`s. It is a `TestScope.prepareScenario` when it
  needs `backgroundScope`. Fixed values the cases share (`pierre`, `anonymousUser`) are `private val`s
  of the class.
- **ViewModels** (`WelcomeViewModelTest`, `RestoreViewModelTest`): a
  `private val testDispatcher = UnconfinedTestDispatcher()`, `Dispatchers.setMain(testDispatcher)`
  in `@BeforeTest` and `Dispatchers.resetMain()` in `@AfterTest`, and `runTest(testDispatcher)`. The
  use cases are their real implementations over the `:testing` fakes
  (`ConnectWithIdTokenUseCase(account)`); a `fun interface` whose outcome is all the case needs is a
  lambda (`completeSignIn = { outcome }`). Connectivity is
  `NetworkConnectivityObserver { isOnline }` over a `MutableStateFlow<Boolean>` the case can flip.
  Navigation is a real `BackStackNavigator(startRoute = ...)`, asserted through
  `navigator.backStack.value`. Actions are collected into a list in `prepareScenario` with
  `backgroundScope.launch { viewModel.uiAction.collect(actions::add) }`. Assert `uiState.value`, the
  actions, the back stack and the calls the fakes recorded.
- **Classes with their own scope** (`SyncCoordinatorTest`): pass `backgroundScope` as the scope and
  advance with `runCurrent()`.
- **Repositories** (`AccountRepositoryImplTest`, `ProfileRepositoryImplTest`): the real repository
  over fake data sources. A fake only one test file needs is a `private class` at the bottom of that
  file (`FakeAccountLocalDataSource`, `FakeProfileLocalDataSource`). Test data that varies per case
  comes from a fixture function named after the value it builds, with defaults for the rest
  (`entity(pendingSync = true, updatedAt = 42L)`).
- **Shared fakes** (`FakeAccountRepository`, `FakeProfileRepository`): in-memory, with the state as
  public `MutableStateFlow`s a test sets and reads, every call recorded in public lists or counters
  (`browserConnections`, `registrationCount`), the failures as public vars (`connectResult`,
  `pushError`), and the ids they hand out as `companion object` constants. Extend one when a case
  needs a knob it lacks, keeping its existing behavior for the tests that already use it.
- **Time**: pass a `CurrentTimeMillis` lambda returning fixed values, so Last-Write-Wins cases are
  deterministic.

## 2. List the cases before reading the implementation

Read the public API of each class in scope (constructor, public functions, the `UiState`,
`UiEvent` and `UiAction` of a ViewModel), not its bodies yet. From the intent alone, write down the
cases: each behavior it promises, and each edge case it names or clearly implies.

Then read the implementation and add a case for each branch the list doesn't reach yet: an error
path, an early return, a `when` arm, an `onFailure`. A branch the intent doesn't explain is worth a
line in the report (step 5), since it may be behavior nobody asked for.

## 3. Write the tests

- Write only test code: files under `src/commonTest/` (or `src/jvmTest/` for something only the JVM
  can run), new or extended shared fakes in a `core/*/testing` module when more than one module needs
  them, and the `commonTest.dependencies` of the module under test when the tests need a `:testing`
  module or `libs.kotlinx.coroutines.test`. A new `:testing` module also needs its `include` in
  `settings.gradle.kts`; say so in the report. Never edit production code, not even to make it
  testable. Report what blocks a test instead.
- One test per case, named and structured as the docs say.
- Assert what the intent promises. Never weaken an assertion until it matches what the code does.

## 4. Run them

```bash
<worktree>/scripts/ktlint.sh --format
./gradlew -p <worktree> :<module>:jvmTest --continue
```

Run `jvmTest` for each module you wrote tests in (e.g. `:feature:restore:jvmTest`,
`:core:account:jvmTest`). If ktlint reports something it can't fix, fix it by hand and run it again.

When a test fails, decide which side is wrong:

- **The test** (a wrong fake, a flow collected too late, a missing `runCurrent()`, a misread API):
  fix the test.
- **The code** (it contradicts the intent): keep the test as it is, failing, and report it as a
  suspected bug. Don't `@Ignore` it and don't change the code: the author decides.

Leave out `:koverVerifyCi` and the other platforms: `create-pr` checks the coverage and compiles
`commonTest` for Kotlin/Native and JS for the whole branch.

## 5. Report

- **Tests written**: each file, with one line per case it covers.
- **Suspected bugs**: for each, `path/to/File.kt:<line>`, what the code does, what the intent says
  instead, and the failing test that shows it.
- **Behavior the intent doesn't mention**: branches you tested as the code has them, but that may be
  unintended.
- **Left out**: cases you couldn't test and why (a platform boundary such as an `expect`/`actual`, a
  Supabase or Room class without an interface to fake, UI code).
- **Other files touched**: fakes extended or added, build scripts changed.
- The last `jvmTest` result for each module: passed, or which tests fail.
