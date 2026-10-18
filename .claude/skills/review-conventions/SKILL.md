---
name: review-conventions
description: "Review the branch's changes against the project's architecture and testing docs, the conventions of the shared Kotlin code and of the three native UIs (Compose, SwiftUI, React), the product invariants of Bora Rachar, and the intent of the pull request. Reports findings only, never edits files. Run by create-pr before the checks; pass the pull request description as the argument."
argument-hint: "[pull request description]"
context: fork
agent: general-purpose
background: false
disallowed-tools: Edit Write NotebookEdit
---

# Review conventions

You are reviewing someone else's change. You don't see the conversation that produced it: judge it
from the diff, the repository and the intent below, nothing else. Don't edit any file. Your output is
a list of findings for the author to decide on.

## The intent of the change

The author wrote this as the pull request description, before the review:

<intent>
$ARGUMENTS
</intent>

If the block above is empty, say so in your report and review only against the docs, the
conventions and the invariants: without an intent you can't tell whether the change does what it
should.

## 1. Collect the change

The change is everything the branch adds on top of `main`, committed or not:

```bash
git fetch origin main
base=$(git merge-base origin/main HEAD)
git diff "$base" --stat
git diff "$base"
git ls-files --others --exclude-standard
```

Untracked files are part of the change too: read each one in full. When a hunk isn't enough to judge
it, read the whole file, and read the code around it: the other UIs of the same feature, the callers
of a changed use case, the fakes of a changed repository.

## 2. Read the docs the change touches

The rules live in the docs that [docs/ai_agents.md](../../../docs/ai_agents.md) links. They are not
loaded into your context by default, so read the ones that apply to the files in the diff, always in
full:

| The diff touches | Read |
|---|---|
| Any Kotlin file | `docs/architecture/code-style.md` |
| A new or moved module, a `build.gradle.kts`, `settings.gradle.kts`, `build-logic/`, a dependency between modules | `docs/architecture/module-structure.md`, `docs/architecture/stack.md` |
| A `ViewModel`, `UiState`, `UiEvent`, `UiAction`, a `*Presenter` of `shared/src/jsMain`, `BlitzSplitApple`, a SwiftUI `*Model`, a React screen | `docs/architecture/state-management.md` |
| A use case, repository, data source, DTO, mapper | `docs/architecture/use-cases.md`, `docs/architecture/data-sources-and-sync.md` |
| A Room entity or DAO, `:core:provider:*`, `SyncCoordinator`, a Supabase call | `docs/architecture/data-sources-and-sync.md` |
| Koin modules, `single`/`factory`/`viewModel`, `AppModules.kt` | `docs/architecture/dependency-injection.md` |
| `Route`, `Navigator`, `AppNavDisplay`, `RootView`/`AppRoute`, `App.tsx` | `docs/architecture/navigation.md` |
| `launch`, `Flow`, `Result`, `try`/`catch`, `suspendRunCatching` | `docs/architecture/coroutine-error-handling.md` |
| A new screen or feature, or a change that reaches more than one UI | `docs/architecture/new-feature-checklist.md` |
| A test, a fake, a `:testing` module | `docs/testing/README.md` and the guides it links |
| `scripts/`, `.editorconfig`, `iosApp/project.yml`, `webApp/package.json`, setup steps | `docs/getting-started.md` |

Leave out anything ktlint already enforces: formatting, and the custom `quare:*` rules that
`./scripts/ktlint.sh` runs from [ktlint-rules](https://github.com/Quartel-Enterprise/ktlint-rules)
with the settings of `.editorconfig`. Among them: production comments only say why
(`quare:comments-say-why`), a data class has no default values, every data class and enum lives in
a file of its own, an interface with a single method is a `fun interface`, an interface and its
implementation live in separate files, `runCatching` is never used in suspend code
(`quare:suspend-run-catching`), a `StateFlow` exposed from a `MutableStateFlow` uses an explicit
backing field, DTO properties carry `@SerialName`, files stay under 600 lines, and the tests follow
Given/When/Then with `prepareScenario` (`quare:test-name-given-when-then`,
`quare:test-body-sections`, `quare:prepare-scenario-*`). Your job is what a linter can't see.

## 3. Check the conventions of the code

These hold across the repository today. A change that departs from them without a reason the
intent gives is a **Should fix**; one that breaks a platform or the module graph is a **Must fix**.

### Shared Kotlin (`core/*`, `feature/*`, `shared/`)

- **Shared up to the ViewModel.** Every rule of the app lives in Kotlin: a `:feature:<name>` module
  holds the ViewModel and its `presentation/model` types, `core/*` holds the domain and the data.
  The three UIs only draw a `UiState`, send `UiEvent`s and carry out the `UiAction`s that need the
  platform. A business rule written in Compose, Swift or TypeScript (a validation, a decision on
  what to show, a navigation choice) is a finding, even if all three UIs repeat it the same way.
- **ViewModel shape**, as in `WelcomeViewModel`, `RestoreViewModel`, `GroupsViewModel`:
  - `uiState: StateFlow<XUiState>` backed by a `MutableStateFlow` built with every field spelled
    out; `uiAction: SharedFlow<XUiAction>` for one-off effects only (launch the native sign in,
    copy, open the QR scanner), never for state a screen must still show after recreation.
  - `stateWatcher` and `actionWatcher` (`FlowWatcher`) next to them, for SwiftUI. A ViewModel
    without them can't be observed from Swift.
  - One public `fun onEvent(event: XUiEvent)` with an exhaustive `when` whose arms call private
    functions. `XUiEvent` is a sealed interface extending `core.presentation.UiEvent`, named after
    what the person did (`OnProviderClicked`, `OnCodeChanged`), not what the code does next.
  - Flags every UI needs are computed properties of the `UiState` (`areAccountButtonsEnabled`,
    `showsOfflineNotice`, `canRestore`), so no UI recomputes them.
  - Errors reach the state as an enum of the feature (`WelcomeError`, `RestoreError`), mapped from
    the domain exceptions by a function in `presentation/mapper` (`Throwable.toRestoreError()`),
    never as a raw message or `Throwable`.
  - It depends on use case interfaces, `Navigator` and `NetworkConnectivityObserver`; not on a
    repository, a data source, Supabase or Room.
- **Use cases**: a `fun interface X` with an `operator fun invoke` in `domain/usecase/`, its
  implementation `XUseCase` in `domain/usecase/impl/`, bound in the module's Koin module with
  `factoryOf(::XUseCase).bind<X>()`. Fallible work returns `Result`.
- **Data**: `XRepository` in `domain/repository/`, `XRepositoryImpl` in `data/repository/`, over
  `XLocalDataSource` (Room, `RoomX...`) and `XRemoteDataSource` (Supabase, `SupabaseX...`) in
  `data/datasource/`; DTOs in `data/dto/`, mappers in `data/mapper/`. Repositories, data sources and
  mappers are `singleOf(...).bind<...>()`. A Supabase error that the app reacts to becomes a domain
  exception in `domain/model/*Exceptions.kt`, thrown by the remote data source
  (`InvalidRecoveryCodeException`, `IdentityAlreadyLinkedException`), not checked by status code
  higher up.
- **Coroutines and errors**: `suspendRunCatching` (`:core:utils`) wraps fallible suspend work so a
  `CancellationException` is rethrown. A hand-written `catch (e: Exception)` or `catch (t: Throwable)`
  in suspend code that doesn't rethrow cancellation is a finding. ViewModels launch in
  `viewModelScope`; long-lived work outside a screen runs in an injected `CoroutineScope` (as
  `SyncCoordinator`), never `GlobalScope`. Work that blocks uses `ioDispatcher` from `:core:utils`,
  since the browser has no `Dispatchers.IO`.
- **Time**: timestamps come from the injected `CurrentTimeMillis`, so tests control them. A direct
  `Clock.System` or platform clock call outside `SystemCurrentTimeMillis` is a finding.
- **Platforms**: shared modules target Android, iOS, macOS, JS and JVM. A new `expect` needs an
  `actual` in `androidMain`, `appleMain`, `jsMain` and `jvmMain` (the JVM target exists for the
  tests), and an `actual` that throws or is left empty on one of them breaks that app.
- **Modules**: features never depend on each other, `core` never depends on a feature, and an
  `:android:feature:*` module sees only its own `:feature:*` module (the `moduleGraphAssert` rules of
  the root `build.gradle.kts`). What two features share moves down to `core`; what one screen needs
  from another feature comes in as a slot the app fills (`GroupsScreen(recoveryCodeSheet = ...)`).
  Production code never depends on a `:testing` module.
- **Wiring a new feature or ViewModel**: its Koin module (`di/XModule.kt`, `viewModelOf(::X)`) is
  added to `sharedModules` in `shared/src/commonMain/.../di/AppModules.kt`; its module to
  `exportedToApple` in `shared/build.gradle.kts`; a `createXViewModel()` to `BlitzSplitApple`; an
  `XPresenter` to `shared/src/jsMain/.../bridge`; and its `include` to `settings.gradle.kts`. A
  missing Koin binding only fails at runtime, on every platform, so check each `get()` resolves.
- **The JS bridge**: a `*Presenter` file is `@file:JsExport`, hands React a `JsXState` of plain
  values (strings, booleans, numbers, arrays; enums as their `code`), turns each `UiEvent` into a
  method, maps each `UiAction` the web acts on, and has `dispose()` clearing its `ViewModelHolder`.
  A Kotlin type the TypeScript side can't use (a sealed class, a `Flow`, a `Long`) crossing the
  bridge is a finding.

### Android UI (`android/*`, `androidApp/`)

- `XScreen(viewModel: XViewModel = koinViewModel())` collects `uiState` with
  `collectAsStateWithLifecycle()`, collects `uiAction` in a `LaunchedEffect(viewModel)`, and hands
  the state to a stateless `XContent(state, onEvent, modifier)` that never sees the ViewModel.
- Text comes from the module's string resources through `stringResource`: English in
  `res/values/strings.xml` and Portuguese in `res/values-pt/strings.xml`, both always complete. Colors and shapes come from `MaterialTheme` and
  `BlitzSplitTheme` (`:android:designsystem`), not literals in a feature.
- Layouts adapt through `AdaptiveScreen`/`DeviceLayout` (phone upright and lying down, tablet), with
  `@DevicePreviews` on the content, and respect insets (`safeDrawingPadding`, `imePadding` under the
  `adjustResize` activity).
- Platform APIs (Credential Manager, the password manager, the QR scanner) live in
  `:android:platform`, not in a feature module.
- A new route gets an `entry<Route.X>` in `androidApp/.../navigation/AppNavDisplay.kt`. Back goes
  through `navigator.navigateBack()`; no screen keeps a back stack of its own.

### Apple UI (`iosApp/`)

- A screen pairs an `@Observable final class XModel` with its view. The model holds the
  `ViewModelHolder` from `BlitzSplitApple.shared.createXViewModel()` and a `WatchBag`, reads the
  initial state from `uiState.value`, observes `stateWatcher`/`actionWatcher` with `[weak self]`,
  exposes `send(_ event:)`, and clears the holder in `isolated deinit`. A model that never clears the
  holder leaks the ViewModel and its coroutines.
- Shared code goes in `Sources/Shared` for iOS, iPadOS and macOS alike; what differs per platform is
  a file of the same type name in `Sources/iOS` and `Sources/macOS` (as `QRScannerView`), or
  `#if os(macOS)` for a small difference.
- Layouts adapt through `ScreenLayout` and `measuringScreenLayout` (one column, two columns on iPad
  and Mac), colors come from `Palette`, and text is an English literal with its pt-BR translation in `Localizable.xcstrings`.
- Navigation binds the shared back stack: `AppRoute` mirrors `Route`, and a native back gesture
  reaches Kotlin as `navigator.popTo(size:)`. A new `Route` needs its `AppRoute` case and its
  `screen(for:)` arm in `RootView`. On the sign in buttons, Apple comes first.
- Settings live in `iosApp/project.yml`, never only in the generated, git-ignored
  `BoraRachar.xcodeproj`, or the next `generate-project.sh` drops them. The deployment target is iOS
  and macOS 26, so the APIs used need no availability checks below it, and a change must keep both
  schemes, `BoraRachar-iOS` and `BoraRachar-macOS`, building.

### Web UI (`webApp/`)

- A screen in `src/features/<name>/` gets its presenter with `usePresenter(() => new XPresenter())`,
  its state with `useWatch`, its actions with `useActions`, and renders nothing until both exist.
  The presenter is created in the effect so React StrictMode never keeps a disposed one.
- Components come from `src/components/ui` (shadcn/ui) and `src/components/common.tsx`, styled with
  Tailwind classes and the theme tokens of `src/index.css`; imports use the `@/` alias. Text comes
  from `t()` with the key in both `src/i18n` dictionaries, and on an Apple device the Apple button
  comes before Google's (`src/lib/platform.ts`).
- `App.tsx` renders the last entry of the shared back stack and keeps the browser history in step.
  A new route needs its `RouteName` in the JS bridge and its case in `Screen`.
- Nothing calls Supabase or keeps app data in React state or `localStorage`: the data lives in Room,
  in the SQLite Web Worker (`src/sqlite/worker.js`).

## 4. Check the product invariants

These rules hold across the whole app. A change that breaks one is a finding even when the code is
otherwise clean.

- **Offline first.** Room is the source of truth on every platform (the web through the SQLite Web
  Worker); every screen observes it. A local write lands in Room first, with `pendingSync = true` and
  `updatedAt` from `CurrentTimeMillis`, and nothing the person does waits for the network to be
  saved. `SyncCoordinator` reconciles with Supabase when the device is online: it pulls, then pushes,
  Last-Write-Wins on `updatedAt`, and a local change made offline never loses to an older server
  value (`ProfileDao.applyRemote`).
- **The account is optional.** "Usar sem conta" works offline and without any network call.
  Registration (anonymous sign in) only happens once a local profile exists and the device is online.
  The Google and Apple buttons are disabled, with the offline notice, while there is no connection.
- **An anonymous user links, never forks.** Connecting Google or Apple links the identity to the
  current anonymous user, so its id and every row stay; only `identity_already_exists` signs in to
  the other user instead (`ConnectResult.SWITCHED`). A change that signs an anonymous user straight in
  with a new identity loses their data.
- **The recovery code.** Only an anonymous user gets one, issued after the first successful sync;
  a Google or Apple account settles the prompt for good (`RecoveryCodePrompt.SAVED`). "Lembrar
  depois" shows it again on the next launch, not right away. `RecoveryCodeFormat` mirrors
  `_shared/recovery_code.ts` of the API (12 Crockford base32 characters), so a change to one must
  change the other, and a mistyped code is rejected before any request.
- **Native sign in where the platform has it.** Google signs in natively on Android (Credential
  Manager) and Apple on iOS and macOS (`ASAuthorizationAppleIDButton`), declared by `NativeSignIn`;
  every other provider goes through the browser and comes back as `borarachar://auth-callback`,
  which each app hands to Supabase. A change to the scheme or host must change all of them: the
  Android manifest, `project.yml`, `DeepLinkConstants` and the Supabase redirect settings.
- **One back stack.** `:core:navigation` holds it and only the ViewModels move it; Navigation 3,
  `NavigationStack` and the browser history only draw it and report a native back.
- **Every platform runs.** Android, iPhone, iPad, Mac and the web all draw every screen of the
  shared flow. A change to a `UiState`, `UiEvent` or `UiAction` reaches every UI that uses it, or
  the intent says which UI is left for later and why. Phone and tablet, upright and lying down, keep
  working on each.
- **Published app versions keep working.** Bora Rachar replaces the legacy Blitz Split app in the
  stores: `applicationId` and the bundle id stay `com.quare.blitzsplit`, and `versionCode` in
  `version.properties` only grows. A change to a DTO or to an edge function contract of
  `blitz-split-api` must not break a version already installed; a field the client now requires
  needs the backend deployed first. Once a version is in the stores, a change to a Room entity bumps
  the database `version` with a migration and commits the exported schema under
  `core/provider/room/schemas/`; a destructive migration loses the user's offline data.

## 5. Check the change against its intent

With the intent in hand, look for:

- Behavior the intent promises that the diff doesn't deliver, or delivers only for some paths
  (another UI, offline, no account vs Google or Apple, a restored user, phone vs tablet, landscape).
- Behavior the diff changes that the intent doesn't mention: an unrelated screen, a shared use case
  whose other callers now behave differently, a `UiState` field whose other UIs now draw it wrong.
- Edge cases the intent lists that have no code or test covering them.
- A business rule that contradicts the invariants above, or another place in the code that already
  implements the same rule differently.

## 6. Report

Report only problems the change introduces, not ones that were already there. Prefer a short list of
real problems to a long list of maybes: if you are not confident, read more code until you are, or
leave it out.

For each finding, give:

- `path/to/File.kt:<line>`, the line in the new version of the file
- What is wrong, in one or two sentences
- The rule it breaks: a link to the doc section, the convention or invariant above, or the part of
  the intent
- What to do instead

Group the findings under **Must fix** (bugs, broken invariants, a platform that no longer builds or
runs, intent not met) and **Should fix** (conventions). End with one line saying whether the change
does what the intent describes. If there is nothing to report, say "No findings" and that line.
