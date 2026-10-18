## Data Sources and Sync

Bora Rachar is **offline first**. Room is the source of truth on every platform: every screen observes Room, every
write lands in Room first, and `SyncCoordinator` reconciles Room with Supabase whenever the device is online. An
account is optional: the server only learns about an install on its first connection.

### Room: the source of truth

`:core:provider:room` holds `AppDatabase` (Room 3, `androidx.room3`), its DAOs and entities. Each platform opens
the file its own way in `platformRoomModule`:

| Platform | Driver | Where the database lives |
| --- | --- | --- |
| Android | `AndroidSQLiteDriver` (`sqlite-framework`: the OS SQLite, no native library shipped) | `context.getDatabasePath(DATABASE_NAME)` |
| iOS, macOS | `BundledSQLiteDriver` | Application Support (private to the app, inside the sandbox on macOS) |
| Web | `WebWorkerSQLiteDriver(get<Worker>())` | OPFS, through `webApp/src/sqlite/worker.js` (SQLite WASM with the `opfs-sahpool` VFS) |
| jvm (tests only) | `BundledSQLiteDriver` | In memory |

`roomModule` builds it with `setQueryCoroutineContext(ioDispatcher)` (`Dispatchers.IO`, or `Dispatchers.Default`
in the browser) and exposes each DAO as a singleton. Schemas are exported to `core/provider/room/schemas`.

Today the builder also sets `fallbackToDestructiveMigration(true)`: a version bump drops the database, pending
changes included. Once a schema has shipped, change it with a migration instead.

- DAOs expose `Flow<T>` for what the UI observes and `suspend` functions for one-shot reads and writes.
- The single-row tables (`profile`, `account`) are keyed by `SINGLE_ROW_ID` (`"me"`): the row exists before the
  server knows the user, so it can't be keyed by the user id.
- A local data source wraps each DAO (`RoomProfileLocalDataSource`, `RoomAccountLocalDataSource`); the repository
  maps entities to domain models.

### Supabase: the remote

`:core:provider:supabase` builds one `SupabaseClient` (`createBlitzSplitSupabaseClient()`) with Auth, Postgrest and
Functions, and binds each plugin on its own so data sources depend on `Auth`, `Postgrest` or `Functions`, never on
the client. The URL, the publishable key and the Google web client id come from the BuildKonfig object
`SupabaseConfig` (see [getting-started.md](../getting-started.md#supabase)).

| Remote data source | Talks to |
| --- | --- |
| `SupabaseProfileRemoteDataSource` | the `profiles` table (Postgrest) |
| `SupabaseAccountRemoteDataSource` | Auth (session, anonymous sign in, ID token, OAuth, email + password) and the `recover` and `recovery-code` edge functions |

The tables, functions and dashboard settings are documented in the `blitz-split-api` repository (`docs/auth.md`,
`docs/database.md`).

A remote data source turns server errors into domain exceptions (`core/account/.../domain/model/AccountExceptions.kt`),
so nothing above it sees an HTTP status:

| Server answer | Exception |
| --- | --- |
| `recover` 400 or 404 | `InvalidRecoveryCodeException` |
| `recover` 429 | `TooManyRecoveryAttemptsException` |
| `recovery-code` 409 (the user has Google or Apple) | `SocialAccountHasNoRecoveryCodeException` |
| `AuthErrorCode.IdentityAlreadyExists` on linking | `IdentityAlreadyLinkedException` |

### The sync: `SyncCoordinator`

`SyncCoordinator` (`:core:sync`) is a Koin singleton with its own `SupervisorJob` scope. `AppViewModel` calls
`StartSync` on launch, and `start()` (idempotent) launches three things:

1. `accountRepository.mirrorSession()`: copies every settled Supabase session (a user, or none after a sign out)
   into the `account` row, so the UI knows the account state offline.
2. `reviveSnoozedPrompt()`: a recovery code snoozed with "Lembrar depois" becomes due again on the next launch.
3. A collector of connectivity combined with the local profile: each time the device is online **and** a profile
   exists, it runs `syncNow()`.

`syncNow()` runs under a `Mutex`, inside `suspendRunCatching`:

```kotlin
suspend fun syncNow(): Result<Unit> = syncLock.withLock {
    suspendRunCatching {
        profileRepository.observeProfile().first() ?: return@suspendRunCatching
        val userId = accountRepository.ensureRegistered().getOrThrow()
        profileRepository.downloadProfile(userId).getOrThrow()
        profileRepository.uploadPendingProfile(userId).getOrThrow()
        offerRecoveryCodeIfDue()
    }
}
```

- **Register**: `ensureRegistered()` reuses the current session or signs in anonymously. The sync never registers
  an install that has no profile yet.
- **Pull, then push, Last-Write-Wins by `updatedAt`.** Every synced row carries `updatedAt` and `pendingSync`. A
  local write sets `pendingSync = true` with the injected clock. The pull applies a server row only when the local
  one is **not pending and strictly older** (`ProfileDao.applyRemote`), so a change made offline never loses to an
  older server value. The push upserts the pending row and `markSynced(syncedUpdatedAt)` clears the flag only if
  the row was not touched again while the push was in flight.
- **Recovery code**: after the first successful sync of an anonymous user whose prompt is `NOT_DUE`, it asks the
  server for a recovery code.

A new synced table follows the same shape: `updatedAt` and `pendingSync` columns, an `applyRemote` query with the
same guard, a `markSynced` that compares `updatedAt`, and a pull and push step in `syncNow()`.

### Accounts: anonymous, linked, switched

The account is optional. Every install starts `AccountStatus.LocalOnly`, becomes `Anonymous` on its first sync and
`Connected` once Google or Apple is attached.

| Platform | Google | Apple |
| --- | --- | --- |
| Android | native: Credential Manager ID token (`rememberGoogleSignInLauncher`) | browser OAuth |
| iOS, macOS | browser OAuth | native: Sign in with Apple ID token with a SHA-256 nonce (`AppleSignIn`) |
| Web | browser OAuth | browser OAuth |

`NativeSignIn` (see [dependency-injection.md](dependency-injection.md#expect-modules)) tells the ViewModel which
path to take. With an ID token, `AccountRepositoryImpl.signInWithIdToken` decides:

| Current session | Does | `ConnectResult` |
| --- | --- | --- |
| none | signs in with the token | `SIGNED_IN` |
| Google or Apple | signs in with the token (another account) | `SWITCHED` |
| anonymous | **links** the identity, so the user id and every row stay the same | `LINKED` |
| anonymous, and the link fails with `identity_already_exists` | the identity already belongs to a user from another device: signs in to that user instead | `SWITCHED` |

With the browser, an anonymous user links (`linkIdentity`) and anyone else signs in. The OAuth callback comes back
as `borarachar://auth-callback` on the native apps (`MainActivity.handleAuthCallback`,
`BlitzSplitApple.handleDeepLink` from `onOpenURL`, both calling `handleDeeplinks`) and as the page itself on the
web. The Welcome and Restore ViewModels finish a browser sign in when they see `AccountStatus.Connected` while on
top of the stack, since on the web the page reloaded in between.

After any sign in, `CompleteSignIn` pulls the account's profile: `PROFILE_RESTORED` goes straight to the groups,
`NEEDS_PROFILE` to the profile setup. A failed pull fails the sign in and the screen shows its error: sending a
returning user to the profile setup would make a new local profile that overwrites theirs on the next push.

### Recovery code

For people without Google or Apple: 12 Crockford base32 characters, hashed server side. `RecoveryCodeFormat` mirrors
the API's rules so a mistyped code is caught before any request: `normalize` uppercases, drops dashes and spaces and
reads `O` as `0` and `I`/`L` as `1`; `format` groups by four (`K7QM-4XPA-9TRE`).

The `account` row tracks a `RecoveryCodePrompt`:

| Prompt | When |
| --- | --- |
| `NOT_DUE` | Before the first sync |
| `DUE` | `createRecoveryCode()` stored a code; the groups show the sheet |
| `SNOOZED` | "Lembrar depois", or the sheet dismissed; due again on the next launch |
| `SAVED` | "Já guardei", a restore, or a Google / Apple account (which never needs one) |

Restoring (`restoreWithRecoveryCode`) normalizes the code, trades it at the `recover` function for one-time email
and password credentials, signs in with them and marks the prompt `SAVED`; then `CompleteSignIn` pulls the profile.
The old device's QR holds `borarachar://recover?code=...` and `toRecoveryCodeFromQr` also accepts a bare code.
