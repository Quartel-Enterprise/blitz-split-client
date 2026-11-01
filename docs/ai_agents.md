# Bora Rachar — Architecture Guide for AI Assistants

This document describes the architecture conventions of this project. Follow these patterns exactly when creating
new features or modifying existing ones.

- [@docs/architecture/stack.md](architecture/stack.md)
- [@docs/architecture/module-structure.md](architecture/module-structure.md)
- [@docs/architecture/state-management.md](architecture/state-management.md)
- [@docs/architecture/use-cases.md](architecture/use-cases.md)
- [@docs/architecture/dependency-injection.md](architecture/dependency-injection.md)
- [@docs/architecture/navigation.md](architecture/navigation.md)
- [@docs/architecture/responsive-layouts.md](architecture/responsive-layouts.md)
- [@docs/architecture/data-sources-and-sync.md](architecture/data-sources-and-sync.md)
- [@docs/architecture/coroutine-error-handling.md](architecture/coroutine-error-handling.md)
- [@docs/architecture/new-feature-checklist.md](architecture/new-feature-checklist.md)
- [@docs/architecture/pr-guidelines.md](architecture/pr-guidelines.md)
- [@docs/architecture/code-style.md](architecture/code-style.md)
- [@docs/testing/README.md](testing/README.md)
- [@docs/getting-started.md](getting-started.md)
- [@docs/ci.md](ci.md)

The one idea behind all of them: Kotlin Multiplatform is shared **up to the ViewModel**. Every `ViewModel`,
`UiState`, `UiEvent` and `UiAction` lives in `:feature:*`, and three native UIs draw them: Jetpack Compose
(`:android:*`), SwiftUI (`iosApp/`) and React (`webApp/`). Logic goes in the ViewModel or below it, never in a UI,
so the three platforms walk the same flow.

Write code, comments, docs, commit messages and pull requests in English, even when the conversation is in
another language. User-facing strings are the exception: each one exists in English (the fallback for every
language but Portuguese) and Portuguese, in each platform's own place for them — `res/values/strings.xml` and
`res/values-pt/strings.xml` of the `:android:*` module, the English SwiftUI literals of the Apple app with their
pt-BR translation in `Localizable.xcstrings`, and the `en` / `pt` dictionaries of `webApp/src/i18n` on the web. The
product is "Bora Rachar" in Portuguese and "Blitz Split" in English. Never send UI copy from Kotlin: the
shared code hands the UIs enums and booleans (`RestoreError`, `SyncBadge`, `showsOfflineNotice`) and each platform
picks its words.

## Who writes the unit tests

Once the production code compiles, and before opening the PR, decide who writes the unit tests. The `write-tests`
skill writes them in a subagent that doesn't see this conversation: it lists the cases from the intent you hand it,
not from the code you just wrote, so it catches what your own tests would restate. Reading the testing docs, the
fakes and the module's style costs it a few minutes, which only pays off when there are enough cases. Both follow
[docs/testing](testing/README.md).

Look only at the production code the change adds or changes that Kover measures (see
[What Kover measures](testing/README.md#what-kover-measures)): the `jvm` target of `:core:*` and `:feature:*`. Not
the `di` and `bridge` packages, generated code (Room, kotlinx.serialization, BuildKonfig), the `:testing` fake
modules, `:core:provider:*`, `:shared`, interfaces, models without logic, nor the Android, Swift and React UIs,
which are not unit tested. List the cases that code needs and no existing test covers yet: a new class with logic
(use case, repository, mapper, data source, coordinator, ViewModel), a new public function, a newly handled
`UiEvent`, a new `if`/`when`/`catch`/early-return branch that changes the result.

| The change | Who writes the tests |
| --- | --- |
| 3 or more new cases | `write-tests` |
| 1 or 2 new cases | you |
| A bug fix | you write the regression test, first, failing before the fix: you reproduced the bug. New behavior the fix adds beyond that follows the rows above |
| A behavior change that existing tests now contradict | you update those assertions, since you know what changed on purpose. New cases follow the rows above |
| No new case: a refactor whose existing tests pass unchanged, or only code Kover leaves out | nobody: the existing tests are the spec |

Calling `write-tests`:

- One call per module, so each run reads only that module's fakes and style. Run them one after the other: they
  build the same checkout.
- Pass the intent as a reviewer would need it: what the change is meant to do, the edge cases that matter (offline,
  local only vs anonymous vs connected, a Google or Apple identity that already belongs to another user, a server
  error, a platform whose sign in is native vs through the browser), and any decision taken in this conversation.
  Not how the code does it: that is what the tests check.
- Pass the classes to cover and the worktree path, if any.
- For each suspected bug it reports, fix the code. If the intent you passed was wrong instead, fix the test and tell
  the user.
