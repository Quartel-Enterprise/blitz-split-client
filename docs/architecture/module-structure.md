## Module Structure

The project is a multi-module Gradle build plus two non-Gradle apps. Every new feature gets a shared module under
`feature/` and an Android module under `android/feature/`.

```
shared/                 # Composition root: Koin, AppViewModel, the Apple framework and the JS library
core/
├── model/              # Plain models every layer shares (UserProfile, AccountStatus, AvatarColor…)
├── utils/              # suspendRunCatching, ioDispatcher, CurrentTimeMillis, RecoveryCodeFormat
├── presentation/       # The UiEvent marker and the Swift/JS bridge (FlowWatcher, Watch, ViewModelHolder)
├── navigation/         # Route, Navigator, BackStackNavigator: the one back stack
├── profile/            # Profile domain + data
│   └── testing/        # FakeProfileRepository, for other modules' tests
├── account/            # Account domain + data: session mirror, sign in, recovery code
│   └── testing/        # FakeAccountRepository
├── groups/             # Groups domain + data, synced like the keys (not `group`: Gradle's getGroup() hides it)
│   └── testing/        # FakeGroupRepository
├── sync/               # SyncCoordinator, ProviderConnector, ProviderSignIn, use cases across profile and account
└── provider/
    ├── room/           # AppDatabase, DAOs, entities, the database file of each platform
    ├── supabase/       # SupabaseClient, Auth / Postgrest / Functions / Storage, SupabaseConfig (BuildKonfig)
    └── image/          # AvatarImageEncoder: any picked image to a 512x512 JPEG, per platform
feature/                # Presentation only: ViewModel, UiState, UiEvent, UiAction, Koin module
├── welcome/
├── profile_setup/
├── restore/
├── recovery_code/
├── groups/
└── create_group/
android/
├── designsystem/       # BlitzSplitTheme, DeviceLayout, FoldLayout, DevicePreviews, shared composables
├── platform/           # Credential Manager (Google), password manager, QR scanner
└── feature/            # The Compose UI of each feature: XScreen + XContent
    ├── welcome/
    ├── profile_setup/
    ├── restore/
    ├── recovery_code/
    ├── groups/
    └── create_group/
androidApp/             # Application, MainActivity, AppNavDisplay
iosApp/                 # XcodeGen project: Sources/Shared (both platforms), Sources/iOS, Sources/macOS
webApp/                 # Vite + React app: src/app, src/bridge, src/features, src/components, src/sqlite
build-logic/            # Convention plugins
```

## Two kinds of Gradle module

| Plugin | Applied to | What it sets up |
| --- | --- | --- |
| `blitzsplit.kotlin.multiplatform` | `:core:*`, `:feature:*`, `:shared` | `android` (`com.android.kotlin.multiplatform.library`), `iosArm64`, `iosSimulatorArm64`, `macosArm64`, `js` (browser, ES modules), `jvm`; the default hierarchy (`appleMain` for the three Apple targets); opt-ins for `ExperimentalTime`, `ExperimentalUuidApi`, `ExperimentalJsExport` and `ExperimentalCoroutinesApi`; `kotlin("test")` in `commonTest`; Kover (except on `:testing` modules) |
| `blitzsplit.android.compose` | `:android:*` | `com.android.library` + the Compose compiler, the Compose BOM, Material 3, lifecycle Compose, `koin-compose-viewmodel`, and `:android:designsystem` on every module but itself |

`:androidApp` applies `com.android.application` directly. Both plugins name the module with `createNamespace()`:
`:core:provider:room` becomes `com.quare.blitzsplit.core.provider.room` and underscores drop
(`:feature:profile_setup` → `com.quare.blitzsplit.feature.profilesetup`), so the namespace matches the package.

The `jvm` target exists only so `commonTest` runs fast on the JVM and Kover can measure it; no app ships it. Its
`actual`s are the minimum the tests need (`platformRoomModule` is an in-memory database there).

## What each UI consumes

| UI | Consumes | How |
| --- | --- | --- |
| Android | `:android:feature:x` → `:feature:x` | Plain Gradle dependency; the screen gets its ViewModel with `koinViewModel()` |
| Android app | `:shared`, `:core:navigation`, `:core:provider:supabase`, every `:android:*` | `startBlitzSplitKoin`, `AppViewModel`, `handleDeeplinks` |
| SwiftUI | the static framework `Shared` | `:shared` exports the modules listed in `exportedToApple` (`shared/build.gradle.kts`): `core:model`, `core:presentation`, `core:navigation` and every `feature:*`. Swift reaches the ViewModels through `BlitzSplitApple` |
| React | the npm package `blitzsplit-shared` | `:shared`'s JS library with TypeScript definitions. Only the `@JsExport` declarations of `shared/src/jsMain/.../bridge` cross: `startBlitzSplit`, `AppPresenter`, one `XPresenter` per screen and their plain `JsXState` classes |

## Dependency rules

`assertModuleGraph` (modules-graph-assert, the `moduleGraphAssert` block of the root `build.gradle.kts`) checks the
production configurations only (`api`, `implementation`, `commonMain*`, `androidMain*`, `appleMain*`, `jsMain*`,
`jvmMain*`); test dependencies are not part of the architecture.

| Rule | Meaning |
| --- | --- |
| `:feature:.* -X> :feature:.*` | A feature never depends on another feature |
| `:core:.* -X> :(feature\|android\|shared).*` | Core sits below everything |
| `:feature:.* -X> :(android\|shared).*` | Shared presentation never sees a UI or the composition root |
| `:android:feature:.* -X> :android:feature:.*` | Android feature UIs never see each other |
| `:android:(designsystem\|feature:.*) -X> :shared` | Only `:androidApp` reaches the composition root |
| `:(?!\S*:testing ).* -X> :.*:testing` | A `:testing` module is only ever a test dependency |

An `:android:feature:x` module depends on its own `:feature:x` only (plus `:android:platform` when it needs a
native sign in, the password manager, the scanner, the camera or a picked photo's bytes). The graph check does not catch an edge to another feature's
presentation module, so the review does.

When two features need the same thing, it goes down, never sideways:

| Need | Where it goes | Example |
| --- | --- | --- |
| Domain or data a feature reads | A `:core:*` module with `domain/` and `data/`; the feature keeps `presentation/` | `core/account`, `core/profile` |
| Logic that crosses two core modules | A core module above them | `core/sync` (`CompleteSignIn` reads account and profile) |
| A model several layers or UIs draw | `core/model` | `AvatarColor`, `AccountProvider` |
| A Compose component two Android screens show | `android/designsystem` | `GoogleSignInButton`, `OfflineNoticeBanner` |
| A feature's UI drawn inside another's | The host takes a slot; the app fills it | `GroupsScreen(recoveryCodeSheet = { RecoveryCodeSheet() })` |

A new dependency that breaks a rule means the code is in the wrong module: move the shared piece down, or have the
app hand it over.

```bash
./gradlew assertModuleGraph
```

### Within a `core/` module

```
src/commonMain/kotlin/com/quare/blitzsplit/core/<name>/
├── data/
│   ├── datasource/      # XLocalDataSource (Room) and XRemoteDataSource (Supabase), interface + impl
│   ├── dto/             # @Serializable wire models
│   ├── mapper/          # DTO / entity ↔ domain
│   └── repository/      # XRepositoryImpl
├── connect/             # :core:sync only: ProviderConnector, the Google or Apple link state every screen shares
├── signin/              # :core:sync only: ProviderSignIn and ProviderSignInController, the Google or Apple sign in every screen shares
├── exit/                # :core:sync only: GroupExitController, "Sair do grupo" and "Apagar grupo" from the group and from the home
├── photo/               # :core:profile only: ProfilePhotoPicker, the photo picked in the profile setup and "Editar perfil"
├── di/                  # One Koin module
└── domain/
    ├── model/           # Domain models and exceptions
    ├── repository/      # XRepository interface
    └── usecase/         # fun interface per use case + impl/XUseCase
```

### Within a `feature/` module

```
src/commonMain/kotlin/com/quare/blitzsplit/feature/<name>/
├── di/                  # One Koin module: viewModelOf(::XViewModel)
└── presentation/
    ├── model/           # XUiState, XUiEvent, XUiAction, XError and other UI enums
    ├── mapper/          # Throwable → UI error, input parsing (if needed)
    └── viewmodel/       # XViewModel
src/commonTest/kotlin/com/quare/blitzsplit/feature/<name>/XViewModelTest.kt
```

## Naming conventions

| Artifact | Convention | Example |
| --- | --- | --- |
| Gradle module / package / web folder | `snake_case` / no separator / `kebab-case` | `:feature:profile_setup` / `feature.profilesetup` / `features/profile-setup` |
| Use case interface | `VerbNoun` | `ConnectWithIdToken` |
| Use case impl | Same name + `UseCase`, in `impl/` | `ConnectWithIdTokenUseCase` |
| Repository | `NounRepository` / `NounRepositoryImpl` | `AccountRepository` / `AccountRepositoryImpl` |
| Data source | `NounLocalDataSource` / `RoomNounLocalDataSource`, `NounRemoteDataSource` / `SupabaseNounRemoteDataSource` | `RoomAccountLocalDataSource` |
| DTO / entity / mapper | `NounDto` / `NounEntity` / `NounMapper` + `NounMapperImpl` | `ProfileDto` / `ProfileEntity` / `ProfileMapperImpl` |
| ViewModel and its types | `FeatureViewModel`, `FeatureUiState`, `FeatureUiEvent`, `FeatureUiAction`, `FeatureError` | `RestoreViewModel`, `RestoreError` |
| Koin module | `featureModule` | `restoreModule` |
| Route | a `data object` in `Route` | `Route.Restore` |
| Android UI | `FeatureScreen` (stateful) + `FeatureContent` (stateless) | `RestoreScreen` / `RestoreContent` |
| Swift UI | `FeatureModel` (`@Observable`) + `FeatureView` | `RestoreModel` / `RestoreView` |
| JS bridge | `FeaturePresenter` + `JsFeatureState` | `RestorePresenter` / `JsRestoreState` |
| React UI | `FeatureScreen` | `RestoreScreen` |

The recovery code is not a route but a sheet over the groups, so its UIs are `RecoveryCodeSheet` (Android, Swift)
and `RecoveryCodeDialog` (web).
