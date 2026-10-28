## Use Cases

Use cases and repositories live in the `:core:*` modules; a `:feature:*` module holds presentation only and injects
the use cases it needs. A ViewModel never touches a repository, a data source or the Supabase client.

### A `fun interface` plus an implementation in `impl/`

```kotlin
// core/account/.../domain/usecase/ConnectWithIdToken.kt
fun interface ConnectWithIdToken {
    suspend operator fun invoke(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
    ): Result<ConnectResult>
}

// core/account/.../domain/usecase/impl/ConnectWithIdTokenUseCase.kt
class ConnectWithIdTokenUseCase(
    private val repository: AccountRepository,
) : ConnectWithIdToken {
    override suspend fun invoke(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
    ): Result<ConnectResult> = repository.signInWithIdToken(provider = provider, idToken = idToken, rawNonce = rawNonce)
}
```

Koin binding, in the core module's `di/`:

```kotlin
factoryOf(::ConnectWithIdTokenUseCase).bind<ConnectWithIdToken>()
```

Every use case follows this shape, even a one-line delegation: the ViewModel depends on the interface, and a test
can pass a lambda (`completeSignIn = { outcome }`) or the real implementation over a shared fake
(`ConnectWithIdTokenUseCase(account)`).

The interface says what kind of call it is:

| Returns | Example | Meaning |
| --- | --- | --- |
| `Flow<T>` | `ObserveProfile`, `ObserveAccountStatus` | Reads Room and keeps emitting; the ViewModel collects it in `init` |
| `suspend … : Result<T>` | `RestoreWithRecoveryCode`, `ConnectWithBrowser`, `CompleteSignIn`, `SyncNow` | Can fail on the network; the ViewModel handles both outcomes |
| `suspend … : T` | `GetSessionUser` | Can't fail in a way the screen shows |
| `suspend …` (Unit) | `SaveProfile`, `UpdateRecoveryCodePrompt` | A local write |
| plain `T` | `ValidateDisplayName` | Pure logic, no I/O |

A use case with rules of its own keeps them in the implementation: `ValidateDisplayNameUseCase` checks
`DisplayNameRules`, `SaveProfileUseCase` trims the name, `CompleteSignInUseCase` pulls the profile and decides
between `SignInOutcome.PROFILE_RESTORED` and `NEEDS_PROFILE`.

### Use cases across core modules

A use case that reads two core modules lives in a core module above both. `:core:sync` depends on `:core:account`
and `:core:profile`, so `CompleteSignIn` (pull the profile after any sign in) and the `SyncCoordinator` wrappers
`StartSync` and `SyncNow` live there.

### Connecting Google or Apple

A screen that offers Google or Apple to a profile without them drives `ProviderConnector` (`:core:sync`, a Koin
factory, so each screen has its own) instead of the connect use cases: the provider click (native sheet or browser),
the ID token and `CompleteSignIn`, the browser sign in that returns after the web reload, and the one abandoned. The
ViewModel copies `connection` into its `UiState`, launches the calls in `viewModelScope` (and `observeBrowserSignIn`
with `launchIn`), and keeps only its own guards (offline, a sign out under way) and what counts as finished for
`observeBrowserSignIn`: the recovery code waits for the prompt to be settled, the profile for Google or Apple
connected.

### Many use cases in one ViewModel

ViewModels take each use case as its own constructor parameter (`RestoreViewModel` takes eight). If a constructor
ever reaches Koin's limit for `viewModelOf`, group the use cases in a `FeatureUseCases` data class bound with
`factoryOf(::FeatureUseCases)`.

## Repository pattern

The interface is in `domain/repository/`, the implementation in `data/repository/`, over one local and one remote
data source:

```kotlin
interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile?>

    suspend fun saveProfile(profile: UserProfile)

    suspend fun uploadPendingProfile(userId: String): Result<Unit>

    suspend fun downloadProfile(userId: String): Result<Unit>
}

class ProfileRepositoryImpl(
    private val local: ProfileLocalDataSource,
    private val remote: ProfileRemoteDataSource,
    private val mapper: ProfileMapper,
    private val currentTimeMillis: CurrentTimeMillis,
) : ProfileRepository {
    override fun observeProfile(): Flow<UserProfile?> = local.observe().map { entity -> entity?.let(mapper::toDomain) }

    override suspend fun saveProfile(profile: UserProfile) {
        local.upsert(mapper.toEntity(profile = profile, updatedAt = currentTimeMillis(), pendingSync = true))
    }
}
```

- Reads come from Room (`local`) only; the UI never waits on the network to draw.
- Writes go to Room first, marked pending; the sync pushes them later (see
  [data-sources-and-sync.md](data-sources-and-sync.md)).
- A function that reaches the network returns `Result<T>` built with `suspendRunCatching`.
- Data sources are interfaces too (`ProfileLocalDataSource` / `RoomProfileLocalDataSource`,
  `ProfileRemoteDataSource` / `SupabaseProfileRemoteDataSource`), so the repository tests fake them by hand.
- Time comes from the injected `CurrentTimeMillis`, so tests control the Last-Write-Wins timestamps.
