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

A feature module only declares its ViewModel:

```kotlin
// feature/restore/.../di/RestoreModule.kt
val restoreModule = module {
    viewModelOf(::RestoreViewModel)
}
```

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
| Web | `webApp/src/bridge/shared.ts` → `startBlitzSplit(worker)` | `module { single<Worker> { sqliteWorker } }`: the SQLite Web Worker the JS `platformRoomModule` hands to `WebWorkerSQLiteDriver`. React creates the worker so Vite bundles it |

### `expect` modules

What differs by platform is an `internal expect val … : Module` with one `actual` per target:

- `platformModule` (`:shared`) binds `NativeSignIn`, the providers whose native button returns an ID token:
  `GOOGLE` on Android, `APPLE` on Apple, none on JS and jvm. The Welcome and Restore ViewModels and
  `ProviderConnector` read it to choose between the native sheet (`LaunchNativeSignIn`) and the browser sign in.
- `platformRoomModule` (`:core:provider:room`) binds the `RoomDatabase.Builder<AppDatabase>` of each platform
  (see [data-sources-and-sync.md](data-sources-and-sync.md#room-the-source-of-truth)).

## Getting a ViewModel

| UI | How |
| --- | --- |
| Compose | `viewModel: XViewModel = koinViewModel()` as the `XScreen` default parameter; `rememberViewModelStoreNavEntryDecorator()` in `AppNavDisplay` scopes it to the back stack entry |
| SwiftUI | `BlitzSplitApple.shared.createXViewModel()`, which returns a `ViewModelHolder` built with `get()`: Swift can't call Koin's reified functions |
| React | the presenter builds `ViewModelHolder(XViewModel::class) { KoinPlatform.getKoin().get() }` |

Besides these and the platform entry points (`MainActivity` injects the `SupabaseClient` for the auth deep link,
`BlitzSplitApple.handleDeepLink` gets it the same way), nothing reaches into Koin: classes take their dependencies
as constructor parameters.
