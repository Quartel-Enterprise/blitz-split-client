## Checklist for a New Feature

A feature is one shared presentation module and three UIs. Build the shared part first, test it, then draw it on
each platform.

### Shared (Kotlin)

- [ ] Domain and data the feature needs go in a `:core:*` module (new or existing), never in the feature: repository
      interface + `Impl`, data sources, use cases as `fun interface` + `impl/XUseCase`, bound in that module's Koin
      module (see [use-cases.md](use-cases.md))
- [ ] A synced table follows [the sync rules](data-sources-and-sync.md#the-sync-synccoordinator): `updatedAt`,
      `pendingSync`, `applyRemote`, `markSynced`, and a step in `SyncCoordinator.syncNow()`
- [ ] Create `feature/<name>` with `blitzsplit.kotlin.multiplatform` and `blitzsplit.kotlin.multiplatform.watchos`
      (`:shared` builds for the watch and depends on every feature), and include it in `settings.gradle.kts`. It may
      not depend on another feature (see [Dependency rules](module-structure.md#dependency-rules))
- [ ] `XUiState` (data class, no defaults, derived rules as getters), `XUiEvent : UiEvent` with each case's
      `analytics` and its rows in the catalog (the `add-analytics-event` skill, [analytics](../analytics/README.md)), `XUiAction` only if a
      platform effect is needed, and an `XError` enum if the screen shows errors (see
      [state-management.md](state-management.md))
- [ ] `XViewModel`: public, extends `TrackedViewModel<XUiEvent>(trackEvent)`, `uiState` / `uiAction` with explicit
      backing fields, `stateWatcher` / `actionWatcher`, `handleEvent`; navigation through `Navigator`
- [ ] `di/XModule.kt` with `factoryOf(::XViewModel)`, added to `sharedModules` in `shared/.../di/AppModules.kt`
- [ ] If it is a screen of its own: a `Route` (see [Adding a route](navigation.md#adding-a-route))
- [ ] `XViewModelTest` in `commonTest`, and tests for the new use cases, repository and mapper (see
      [docs/testing](../testing/README.md)); `./gradlew jvmTest :koverVerifyCi` passes

### Android

- [ ] Create `android/feature/<name>` with `blitzsplit.android.compose`, depending on `:feature:<name>` (and
      `:android:platform` if it needs a native sign in, the password manager or the scanner); include it in
      `settings.gradle.kts` and add it to `androidApp/build.gradle.kts`
- [ ] `XScreen(viewModel: XViewModel = koinViewModel())`: collects `uiState` with `collectAsStateWithLifecycle()`,
      handles `uiAction` in a `LaunchedEffect(viewModel)`, calls `XContent`
- [ ] A ViewModel created inside part of a screen that comes and goes takes its own scope, not the entry's: a pane
      per pick or pager pages kept across swipes get `rememberViewModelStoreProvider()` with a key each, a piece that
      may start over (a lazy item with a stable `key`) gets `rememberViewModelStoreOwner()` (see
      [dependency-injection.md](dependency-injection.md#which-scope-a-compose-viewmodel-takes))
- [ ] `XContent(state, onEvent, modifier)`: stateless, adaptive through `AdaptiveScreen` / `DeviceLayout` and, where
      the boards draw a foldable of their own, `FormFactor` and the hinge layouts (see
      [responsive-layouts.md](responsive-layouts.md)), with `@DevicePreviews` previews wrapped in `BlitzSplitTheme`
- [ ] The `entry<Route.X>` in `AppNavDisplay`, or a slot the host screen takes when it is drawn inside another
      feature (as `GroupsScreen(recoveryCodeSheet = …)`)
- [ ] Strings in English in the module's `res/values/strings.xml`, in Portuguese in `res/values-pt/strings.xml` (`values-pt-rPT` only for what Portugal says differently), in
      Spanish in `res/values-es/strings.xml` (`values-es-rES` only for what Spain says differently), in French in
      `res/values-fr/strings.xml`, in German in `res/values-de/strings.xml`, in Italian in `res/values-it/strings.xml`,
      in Polish in `res/values-pl/strings.xml` (with Polish's few and many plurals), in Dutch in
      `res/values-nl/strings.xml`, in Japanese in `res/values-ja/strings.xml`, in Turkish in
      `res/values-tr/strings.xml`, in Indonesian in `res/values-in/strings.xml` (Android's legacy code for `id`), in
      Russian in `res/values-ru/strings.xml` and in Ukrainian in `res/values-uk/strings.xml` (both with the few and
      many plurals, as Polish), in Korean in `res/values-ko/strings.xml` (no particle glued to a placeholder), in Hindi in
      `res/values-hi/strings.xml`, in Simplified Chinese in `res/values-b+zh+Hans/strings.xml`, and in Arabic in `res/values-ar/strings.xml` (all six plurals: zero, one, two, few,
      many, other), in Hebrew in `res/values-iw/strings.xml` (one, two and other), in Thai in
      `res/values-th/strings.xml`, in Vietnamese in `res/values-vi/strings.xml`, in Traditional Chinese in
      `res/values-b+zh+Hant/strings.xml`, in Swedish in `res/values-sv/strings.xml`, in Czech in `res/values-cs/strings.xml`,
      in Romanian in `res/values-ro/strings.xml`, in Bengali in `res/values-bn/strings.xml`, in Malay in
      `res/values-ms/strings.xml`, in Greek in `res/values-el/strings.xml` and in Croatian in
      `res/values-hr/strings.xml` (one, few and other)
- [ ] Any resource of a `values-pt-rPT` or `values-es-rES` (a string, a bool) also in `values-pt` or `values-es`:
      Android 7 and later fall back to a child of the language when its bare folder lacks a resource, so a pt-BR
      device read Portugal's `designsystem_portugal_portuguese` and wrote "Você criaste a conta"

### Apple

- [ ] Add `":feature:<name>"` to `exportedToApple` in `shared/build.gradle.kts`, so Swift sees its types
- [ ] `fun createXViewModel(): ViewModelHolder<XViewModel>` in `BlitzSplitApple`
- [ ] `iosApp/Sources/Shared/Features/<Name>/XView.swift`: an `@Observable final class XModel` (holder, `WatchBag`,
      `stateWatcher`, `actionWatcher`, `send`, `isolated deinit { holder.clear() }`) and the `XView`; code that
      differs by platform goes behind `#if os(macOS)` or into `Sources/iOS` / `Sources/macOS`; layout follows the
      window's size (`ScreenLayout`), never the idiom (see [responsive-layouts.md](responsive-layouts.md))
- [ ] The route in `AppRoute` and `RootView`, or the sheet where its host presents it
- [ ] Strings as English SwiftUI literals with their pt-BR, (where Portugal differs) pt-PT, es, (where Spain differs) es-ES, fr, de, it, pl, nl, ja, tr and id translations in `Localizable.xcstrings`; rerun `./iosApp/generate-project.sh` after adding files, then build
      both `BoraRachar-iOS` and `BoraRachar-macOS`

### Web

- [ ] `shared/src/jsMain/.../bridge/XPresenter.kt` with `@file:JsExport`: a `JsXState` of plain types (strings,
      booleans, arrays, nullable strings), `observeState`, an `observe…` per action the web handles, one method per
      event, `dispose()`
- [ ] The route name in `RouteName.kt` and the case in `Screen` of `App.tsx`
- [ ] `webApp/src/features/<name>/XScreen.tsx` with `usePresenter`, `useWatch`, `useActions` (and `useDraft` for
      text inputs), shadcn/ui components and Tailwind
- [ ] Strings through `t()` with the key in `src/i18n/en.ts` and every other dictionary (`pt`, `es`, `fr`, `de`, `it`,
      `pl`, `nl`, `ja`, `tr`, `id`, a Polish `…Other` with a `{count}` holding its few and many forms as "few|many"); `npm --prefix webApp run
      build` passes (it type-checks against the generated `.d.ts`)

### Before the PR

- [ ] `./scripts/ktlint.sh`, `./scripts/cpd.sh`, `./gradlew assertModuleGraph`, and a run of the flow on Android, iOS, macOS and the web
      (see [pr-guidelines.md](pr-guidelines.md))
