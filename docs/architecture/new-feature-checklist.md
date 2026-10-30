## Checklist for a New Feature

A feature is one shared presentation module and three UIs. Build the shared part first, test it, then draw it on
each platform.

### Shared (Kotlin)

- [ ] Domain and data the feature needs go in a `:core:*` module (new or existing), never in the feature: repository
      interface + `Impl`, data sources, use cases as `fun interface` + `impl/XUseCase`, bound in that module's Koin
      module (see [use-cases.md](use-cases.md))
- [ ] A synced table follows [the sync rules](data-sources-and-sync.md#the-sync-synccoordinator): `updatedAt`,
      `pendingSync`, `applyRemote`, `markSynced`, and a step in `SyncCoordinator.syncNow()`
- [ ] Create `feature/<name>` with `blitzsplit.kotlin.multiplatform`, and include it in `settings.gradle.kts`. It may
      not depend on another feature (see [Dependency rules](module-structure.md#dependency-rules))
- [ ] `XUiState` (data class, no defaults, derived rules as getters), `XUiEvent : UiEvent`, `XUiAction` only if a
      platform effect is needed, and an `XError` enum if the screen shows errors (see
      [state-management.md](state-management.md))
- [ ] `XViewModel`: public, `uiState` / `uiAction` with explicit backing fields, `stateWatcher` / `actionWatcher`,
      `onEvent`; navigation through `Navigator`
- [ ] `di/XModule.kt` with `viewModelOf(::XViewModel)`, added to `sharedModules` in `shared/.../di/AppModules.kt`
- [ ] If it is a screen of its own: a `Route` (see [Adding a route](navigation.md#adding-a-route))
- [ ] `XViewModelTest` in `commonTest`, and tests for the new use cases, repository and mapper (see
      [docs/testing](../testing/README.md)); `./gradlew jvmTest :koverVerifyCi` passes

### Android

- [ ] Create `android/feature/<name>` with `blitzsplit.android.compose`, depending on `:feature:<name>` (and
      `:android:platform` if it needs a native sign in, the password manager or the scanner); include it in
      `settings.gradle.kts` and add it to `androidApp/build.gradle.kts`
- [ ] `XScreen(viewModel: XViewModel = koinViewModel())`: collects `uiState` with `collectAsStateWithLifecycle()`,
      handles `uiAction` in a `LaunchedEffect(viewModel)`, calls `XContent`
- [ ] `XContent(state, onEvent, modifier)`: stateless, adaptive through `AdaptiveScreen` / `DeviceLayout`, with
      `@DevicePreviews` previews wrapped in `BlitzSplitTheme`
- [ ] The `entry<Route.X>` in `AppNavDisplay`, or a slot the host screen takes when it is drawn inside another
      feature (as `GroupsScreen(recoveryCodeSheet = …)`)
- [ ] Strings in English in the module's `res/values/strings.xml` and in Portuguese in `res/values-pt/strings.xml`

### Apple

- [ ] Add `projects.feature.<name>` to `exportedToApple` in `shared/build.gradle.kts`, so Swift sees its types
- [ ] `fun createXViewModel(): ViewModelHolder<XViewModel>` in `BlitzSplitApple`
- [ ] `iosApp/Sources/Shared/Features/<Name>/XView.swift`: an `@Observable final class XModel` (holder, `WatchBag`,
      `stateWatcher`, `actionWatcher`, `send`, `isolated deinit { holder.clear() }`) and the `XView`; code that
      differs by platform goes behind `#if os(macOS)` or into `Sources/iOS` / `Sources/macOS`
- [ ] The route in `AppRoute` and `RootView`, or the sheet where its host presents it
- [ ] Strings as English SwiftUI literals with their pt-BR translation in `Localizable.xcstrings`; rerun `./iosApp/generate-project.sh` after adding files, then build
      both `BoraRachar-iOS` and `BoraRachar-macOS`

### Web

- [ ] `shared/src/jsMain/.../bridge/XPresenter.kt` with `@file:JsExport`: a `JsXState` of plain types (strings,
      booleans, arrays, nullable strings), `observeState`, an `observe…` per action the web handles, one method per
      event, `dispose()`
- [ ] The route name in `RouteName.kt` and the case in `Screen` of `App.tsx`
- [ ] `webApp/src/features/<name>/XScreen.tsx` with `usePresenter`, `useWatch`, `useActions` (and `useDraft` for
      text inputs), shadcn/ui components and Tailwind
- [ ] Strings through `t()` with the key in both `src/i18n/en.ts` and `pt.ts`; `npm --prefix webApp run build` passes (it type-checks against the generated
      `.d.ts`)

### Before the PR

- [ ] `./scripts/ktlint.sh`, `./scripts/cpd.sh`, `./gradlew assertModuleGraph`, and a run of the flow on Android, iOS, macOS and the web
      (see [pr-guidelines.md](pr-guidelines.md))
