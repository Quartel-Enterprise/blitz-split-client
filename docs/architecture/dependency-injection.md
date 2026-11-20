## Dependency Injection (Koin)

Every Gradle module that provides something has exactly one Koin module, in its `di/` package.

A core module binds its data sources and repository as singletons and its use cases as factories:

```kotlin
// core/profile/.../di/ProfileModule.kt
val profileModule = module {
    singleOf(::RoomProfileLocalDataSource).bind<ProfileLocalDataSource>()
    singleOf(::SupabaseProfileRemoteDataSource).bind<ProfileRemoteDataSource>()
    singleOf(::ProfileMapperImpl).bind<ProfileMapper>()
    singleOf(::ProfileRepositoryImpl).bind<ProfileRepository>()
    factoryOf(::ObserveProfileUseCase).bind<ObserveProfile>()
    factoryOf(::SaveProfileUseCase).bind<SaveProfile>()
    factoryOf(::ValidateDisplayNameUseCase).bind<ValidateDisplayName>()
}
```

A feature module only declares its ViewModel, as a factory:

```kotlin
// feature/restore/.../di/RestoreModule.kt
val restoreModule = module {
    factoryOf(::RestoreViewModel)
}
```

Why `factoryOf` and not Koin's `viewModelOf`: `viewModelOf` lives in `koin-core-viewmodel`, which publishes no watchOS
target, and it only wraps `factory { }`. Android's `koinViewModel()` resolves a factory the same way, keeping the
instance in the screen's `ViewModelStore`, and Swift and React hold theirs through the bridge.

Write the constructor out (`single { … }`) only when a parameter is not in the graph, as `syncModule` does to give
`SyncCoordinator` its own `CoroutineScope(SupervisorJob() + Dispatchers.Default)`.

## The composition root: `:shared`

`shared/src/commonMain/.../di/AppModules.kt` lists every module once and starts Koin:

```kotlin
val sharedModules: List<Module> = listOf(
    appModule,
    platformModule,
    connectivityModule,
    navigationModule,
    roomModule,
    supabaseModule,
    profileModule,
    accountModule,
    syncModule,
    welcomeModule,
    profileSetupModule,
    restoreModule,
    recoveryCodeModule,
    groupsModule,
)

// Why: each app adds what only it can provide (the Android Context, the browser's SQLite Worker).
fun startBlitzSplitKoin(
    extraModules: List<Module> = emptyList(),
    configure: KoinApplication.() -> Unit = {},
): KoinApplication = startKoin {
    configure()
    modules(sharedModules + extraModules)
}
```

**Register a new Koin module in `sharedModules`.** Nothing else lists them.

### What each platform adds

| Platform | Starts Koin from | Adds |
| --- | --- | --- |
| Android | `BlitzSplitApplication.onCreate` | `startBlitzSplitKoin { androidContext(this@BlitzSplitApplication) }`: the `Context` the Android `platformRoomModule` needs for the database path |
| Apple | `BoraRacharApp.init` → `BlitzSplitApple.shared.start()` | Nothing: the database goes to Application Support |
| Web | `webApp/src/bridge/shared.ts` → `startBlitzSplit(worker, uiTag)` | A module with the SQLite Web Worker the JS `platformRoomModule` hands to `WebWorkerSQLiteDriver` (React creates the worker so Vite bundles it) and `DeviceDecimalSeparator` and `ObserveSpokenLanguage` from the UI's language tag, so the amount fields type, and the shared links say, the language `t()` and `formatMoney` speak: Chrome's `navigator.language` is its menus' language, which may differ |

### `expect` modules

What differs by platform is an `internal expect val … : Module` with one `actual` per target:

- `platformModule` (`:shared`) binds `NativeSignIn`, the providers whose native button returns an ID token:
  `GOOGLE` on Android, `APPLE` on Apple, none on JS and jvm. `ProviderSignIn` (the welcome and the restore) and
  `ProviderConnector` (the recovery code and the profile) read it to choose between the native sheet
  (`LaunchNativeSignIn`) and the browser sign in.
- `platformRoomModule` (`:core:provider:room`) binds the `RoomDatabase.Builder<AppDatabase>` of each platform
  (see [data-sources-and-sync.md](data-sources-and-sync.md#room-the-source-of-truth)).

## Getting a ViewModel

| UI | How |
| --- | --- |
| Compose | `viewModel: XViewModel = koinViewModel()` as the `XScreen` default parameter; `rememberViewModelStoreNavEntryDecorator()` in `AppNavDisplay` scopes it to the back stack entry; a piece composed only for a while gets its own scope (see below) |
| SwiftUI | `BlitzSplitApple.shared.createXViewModel()`, which returns a `ViewModelHolder` built with `get()`: Swift can't call Koin's reified functions |
| React | the presenter builds `ViewModelHolder(XViewModel::class) { KoinPlatform.getKoin().get() }` |

### Which scope a Compose ViewModel takes

A ViewModel lives as long as the `ViewModelStoreOwner` it was created in, and `koinViewModel()` takes the nearest
one from `LocalViewModelStoreOwner`. Pick the scope by how long the state and the work must outlive what draws
them:

| What draws it | Scope | How |
| --- | --- | --- |
| A screen, a tab, a dialog or sheet that is a route | its back stack entry (the default) | nothing: `rememberViewModelStoreNavEntryDecorator()` provides it |
| A piece that may start over each time it shows: a lazy list item, a pager page that may load again once swiped away | that composable | `rememberViewModelStoreOwner()` provided through `LocalViewModelStoreOwner` |
| Pieces that each keep their own ViewModel while their container stays composed (pager pages kept across swipes), or a pick shown in a pane, whose store must go even when a rotation leaves no pane | one store per key | `rememberViewModelStoreProvider()` once in the container, then `rememberViewModelStoreOwner(key = pick, provider = provider)` per piece, and `clearKey` for a key no longer shown, as `rememberPaneStores` and `PickScopedPane` do in Contacts |

- `rememberViewModelStoreOwner()` (lifecycle-viewmodel-compose, `commonMain`) clears its ViewModels when the
  composable leaves the composition and keeps them across a rotation, since it hangs off the entry's owner. It finds
  its store again by its position in the composition: give lazy items a stable `key`, or after a reorder and a
  rotation an item picks up another one's ViewModel. Across a configuration change the store waits for the same
  call site to come back, so when the new layout doesn't draw it (a pane gone after turning a tablet upright), it
  stays in the entry's store, running, until that call site is drawn and left again. A pane that comes and goes
  with the window takes the provider row instead, clearing the key it last showed (saved with `rememberSaveable`)
  once it shows another pick or none.
- The provider's stores go once the container leaves the composition, a route pushed over the entry included.
- Never hand-roll a `ViewModelStoreOwner` with `remember` and `DisposableEffect`: its store is cleared on every
  configuration change.
- Wrap it in `key(...)` when one call site draws different things over time (`PickScopedPane(pick)`), so each one
  gets a fresh store.
- Keep a ViewModel on the entry when its own work can remove the composable it is drawn in. `RecoveryCodeSheet` is
  composed only while the prompt shows, but linking Google from it settles the prompt and hides the sheet while
  `completeSignIn()` still runs in `viewModelScope`; a sheet-scoped ViewModel would cancel it halfway.
- A ViewModel that two composables of one entry share (a list and its detail pane talking to each other) stays on
  the entry, too.

Besides these and the platform entry points (`MainActivity` injects the `SupabaseClient` for the auth deep link,
`BlitzSplitApple.handleDeepLink` gets it the same way), nothing reaches into Koin: classes take their dependencies
as constructor parameters.
