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
- The list tables (`payment_key`, `split_group`, `group_member`, `expense`, `settlement`) are keyed by an id the device makes (`Uuid.random()`), so a row created offline
  keeps its id on the server.
- A local data source wraps each DAO (`RoomProfileLocalDataSource`, `RoomAccountLocalDataSource`,
  `RoomPaymentKeyLocalDataSource`, `RoomGroupLocalDataSource`, `RoomExpenseLocalDataSource`,
  `RoomSettlementLocalDataSource`); the repository maps entities to domain models.

### Supabase: the remote

`:core:provider:supabase` builds one `SupabaseClient` (`createBlitzSplitSupabaseClient()`) with Auth, Postgrest,
Functions and Storage, and binds each plugin on its own so data sources depend on `Auth`, `Postgrest`, `Functions`
or `Storage`, never on the client. The URL, the publishable key and the Google web client id come from the BuildKonfig object
`SupabaseConfig` (see [getting-started.md](../getting-started.md#supabase)).

| Remote data source | Talks to |
| --- | --- |
| `SupabaseProfileRemoteDataSource` | the `profiles` table (Postgrest) |
| `SupabasePaymentKeyRemoteDataSource` | the `payment_keys` table (Postgrest): the Pix and Bitcoin keys of the profile |
| `SupabaseGroupRemoteDataSource` | the `groups` and `group_members` tables (Postgrest): the groups the user created or joined, and their members; the `get_invite` and `join_group` functions of the invite link |
| `SupabaseExpenseRemoteDataSource` | the `expenses` table (Postgrest): the bills of the user's groups, with their payers and shares as jsonb |
| `SupabaseSettlementRemoteDataSource` | the `settlements` table (Postgrest): the payments recorded in the user's groups |
| `SupabaseProfilePhotoRemoteDataSource` | the `avatars` Storage bucket: one `{userId}/avatar.jpg` per user, overwritten with upsert |
| `SupabaseAccountRemoteDataSource` | Auth (session, anonymous sign in, ID token, OAuth, email + password) and the `recover`, `recovery-code`, `unlink-identity`, `delete-account` and `apple-token` edge functions |

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
`StartSync` on launch, and `start()` (idempotent) launches five things:

1. `accountRepository.mirrorSession()`: copies every settled Supabase session (a user, or none after a sign out)
   into the `account` row, so the UI knows the account state offline.
2. `reviveSnoozedPrompt()`: a recovery code snoozed with "Lembrar depois" becomes due again on the next launch.
3. `syncOnUserSwitch()`: when the session moves to another user, it syncs right away under the new one, whose
   profile replaces this device's copy (see below).
4. `syncWhenDue()`: a collector of connectivity, the local profile, its payment keys, notification preferences, groups
   and bills, and
   their pending changes. It syncs when the
   device becomes able to (online **and** a profile exists) and whenever a change is queued while it already is.
   Each round repeats while it succeeds and a change saved during the push is still pending (`syncUntilSettled()`).
5. `accountRepository.keepAppleTokens()`: hands the server Apple's refresh token after each browser sign in with
   Apple, so deleting the account can revoke it, as the App Store asks. The native sheet sends its authorization
   code at sign in instead.

`syncNow()` runs under a `Mutex`, inside `suspendRunCatching`, and records what the sync state needs:

```kotlin
suspend fun syncNow(): Result<Unit> {
    requestedSyncs.update { it + 1 }
    return try {
        syncLock.withLock {
            suspendRunCatching {
                profileRepository.observeProfile().first() ?: return@suspendRunCatching
                val userId = accountRepository.ensureRegistered().getOrThrow()
                profileRepository.downloadProfile(userId).getOrThrow()
                profileRepository.uploadPendingProfile(userId).getOrThrow()
                // Each list syncs on its own, so one failing doesn't stop the others; any failure fails the sync
                val keysSynced = suspendRunCatching {
                    paymentKeyRepository.downloadKeys(userId).getOrThrow()
                    paymentKeyRepository.uploadPendingKeys(userId).getOrThrow()
                }
                val preferencesSynced = suspendRunCatching { /* download, then upload */ }
                val groupsSynced = suspendRunCatching {
                    groupRepository.downloadGroups(userId).getOrThrow()
                    groupRepository.uploadPendingGroups(userId).getOrThrow()
                    // A bill needs its group and members on both sides first
                    expenseRepository.downloadExpenses(userId).getOrThrow()
                    expenseRepository.uploadPendingExpenses(userId).getOrThrow()
                    settlementRepository.downloadSettlements(userId).getOrThrow()
                    settlementRepository.uploadPendingSettlements(userId).getOrThrow()
                }
                offerRecoveryCodeIfDue()
                signOutIfScheduledAndSynced()
                keysSynced.getOrThrow()
                preferencesSynced.getOrThrow()
                groupsSynced.getOrThrow()
            }.also { result -> lastSyncFailed.value = result.isFailure }
        }
    } finally {
        requestedSyncs.update { it - 1 }
    }
}
```

`observeState()` (the `ObserveSyncState` use case) turns that into a `SyncState`: `SYNCING` while any sync is
running or waiting on the lock, `FAILED` when the last one failed, `PENDING` when a local change is not pushed yet
(`ProfileRepository`, `PaymentKeyRepository`, `NotificationPreferencesRepository` or
`GroupRepository.observeHasPendingChanges()`),
and `SYNCED` otherwise. The home draws a badge only for the states that ask for attention, and its "try again" goes
through `RetrySync`, which runs in the coordinator's scope so it outlives the screen.

- **Register**: `ensureRegistered()` reuses the current session or signs in anonymously. The sync never registers
  an install that has no profile yet.
- **Pull, then push, Last-Write-Wins by `updatedAt`.** Every synced row carries `updatedAt` and `pendingSync`. A
  local write sets `pendingSync = true` with the injected clock. The pull applies a server row only when the local
  one is **not pending and strictly older** (`ProfileDao.applyRemote`), so a change made offline never loses to an
  older server value. The push upserts the pending row and `markSynced(syncedUpdatedAt)` clears the flag only if
  the row was not touched again while the push was in flight.
- **Profile photo**: a photo picked in "Adicionar foto" is already a 512x512 JPEG (`PrepareProfilePhoto`, through
  the `AvatarImageEncoder` of `:core:provider:image`) and waits in `profile.pendingPhoto` until the push. The push
  uploads it first, sends its public URL (with `?v=<updatedAt>`, so caches fetch the new photo) as `avatar_url`,
  and `markSynced` swaps the bytes for that URL in the same guarded update. Until then the UIs draw the bytes as a
  data URI (`ProfileSetupUiState.photoUrl`).
- **Payment keys**: each Pix or Bitcoin key is a row of `payment_key`, synced by `updatedAt` like the profile,
  with two differences in `PaymentKeyDao.applyRemote`: a newer server row wins even over a change not pushed
  yet, since the server would refuse that older push anyway, and a deletion is final on both sides (a tombstone
  wins whatever its age, and the server's trigger never lets a write bring a deleted key back). A delete is a tombstone (`isDeleted`) with an empty value, so a deleted CPF or
  wallet doesn't stay on the server; it is pushed like any change and removed once synced, since nothing is ever
  deleted on the server. Deleting the default writes the key that takes over in the same upsert. Two devices may each mark a default offline, so readers
  take the flagged key with the newest `updatedAt` (`PaymentKeyMapper`). The server keeps the newer row too: a
  trigger ignores an upsert older than the stored one.
- **Groups**: a group (`split_group`) and its members (`group_member`) sync by `updatedAt` like the keys: a newer
  server row wins (`GroupDao.applyRemote*`), and the push sends the groups before their members, since the server
  only lets the creator or a member write a group's members. A new group belongs to the profile's account, like a
  key; one created before any account has no owner until the next sync claims it, with its creator's member row
  (`claimUnowned`). A member is just a name: `userId` stays null until someone picks that member from the invite
  link, and the server never lets a client take over a member that has an account. The server keeps the newer row
  too, with the same trigger as the keys. The download reads every group and member page by page and deletes the
  synced ones the server no longer has (`GroupDao.deleteGoneFromServer`); pending ones stay. When an account is
  deleted, the server unlinks it from its groups and deletes the groups nobody with an account is left in.
  - **Removing a member**: only a member nobody has joined as can be removed, on the server too (its delete policy
    asks for a null `user_id`). The device marks the row `isRemoved` and pending, so the UIs hide it at once; the
    push deletes the removed members on the server after the upserts, then drops their rows
    (`GroupDao.deleteRemovedMembers`). A newer server row in the meantime (someone joined as that member, or
    renamed it) brings the member back, as Last-Write-Wins says, and the other devices lose it through
    `deleteGoneFromServer`.
  - **Invite code**: the server makes each group's code (`invite_code`, the `blitzsplit.com/g/<code>` of "Convidar")
    when the group first goes up, and no client sends or changes it. The push reads it back from the upsert's answer,
    since the sync downloads before it uploads and a new group would otherwise wait a whole sync for its link. Both
    the push and the download store it with `GroupDao.updateInviteCode`, outside Last-Write-Wins: a group this
    device created comes back with the same `updatedAt`, which `updateGroupFromRemote` leaves alone. A group created
    offline has no code until then, and `InviteUiState.isLinkPending` says so. Each member's code (`invite_code` of
    `group_members`, the `<member code>` of a personal link `blitzsplit.com/g/<code>/<member code>`) follows the same
    path: read back from the members' upsert and stored with `GroupDao.updateMemberInviteCode`, outside
    Last-Write-Wins, so a member added offline has no personal link until it goes up.
  - **Joining through the invite link**: a group the user is not in yet can't be read through the tables, so the
    invite screen asks `get_invite` (open to `anon` too, so only looking at an invite creates no account) and the
    join asks `join_group`, both security definer functions on the server. `JoinGroupUseCase` (in `:core:sync`)
    signs in anonymously first when there is no account (`ensureRegistered`), sends the profile's avatar color (the
    member's, or the least used one, when the device has no profile yet), and on `Joined` or `AlreadyMember` saves a
    profile from the name joined as when there is none, then runs `syncNow()` so the group is in Room before
    Membros opens it. Joining is online only: the link waits on the invite screen until the device is online. A
    member someone else took first (`MemberTaken`) or a name already in the group (`NameTaken`) comes back as an
    outcome, not an error, and the pick screen reloads the list. A personal link asks `get_invite` with the member's
    code and the sharer's too, which answer `personal_member_id` and `inviter_name`, and joins with `join_group` as
    that member right from the invite screen.
- **Bills** (`:core:expenses`): a bill (`expense`) is one Last-Write-Wins row, its payers and shares inside it as
  JSON (jsonb on the server), so an edit never mixes two versions of a split. It belongs to its group through a
  foreign key with `ON DELETE CASCADE`: whatever deletes a group on the device (a sign out, an account switch, the
  server no longer having it) takes its bills, and upserting the group keeps them. The sync moves bills after the
  groups and members, down then up, since the server checks the bill's group (`can_access_group`) and the device
  needs the group to store a bill (`applyRemote` skips a bill whose group isn't there yet). The server checks that
  the shares and payers add up to the amount (`expense_adds_up`) and keeps the newer row with the same kind of
  trigger, the creator included. The download pages through every bill and deletes the synced ones the server no
  longer has (`ExpenseDao.deleteGoneFromServer`); pending ones stay. A bill with a split mode this version doesn't
  know stays in Room but out of the lists. The split math (`BillSplit`) is shared by every UI: the cents a split
  can't share evenly go to whoever paid while they are in the split, otherwise one each to the first members.
  An edit stamps `updatedAt` with `MAX(now, updatedAt + 1)`, so it wins over the row it read even when the device
  clock is behind, and a delete is a tombstone (`isDeleted`) that syncs like an edit; `observeExpense` emits `null`
  for a deleted or gone bill. The server records who last saved each bill in `updated_by` (its triggers set it from
  `auth.uid()` and never bump `updated_at`). Room clears `updatedBy` on any local write, since the edit is nobody
  else's until it syncs, and `markSynced` sets it to whoever pushed; `ExpenseWithEditor` resolves it to the
  member's name for the "someone changed this bill" banner.
- **Payments** (`:core:expenses`): a payment recorded in "Registrar pagamento" (`settlement`) is one
  Last-Write-Wins row naming two members, like a bill's payers, and belongs to its group with the same
  `ON DELETE CASCADE`. It syncs right after the bills, down then up, with the same paging and
  `deleteGoneFromServer`. It is never edited: undoing it is a tombstone (`isDeleted`, `updatedAt` stamped with
  `MAX(now, updatedAt + 1)`), and the server's trigger keeps a deleted payment deleted whatever another device
  pushes. The server checks an upsert against its insert policy with the row as sent, where `created_by` must be the
  user pushing, so the client always sends itself as `created_by` and the trigger keeps who recorded it. Balances
  (`GroupBalances`) are worked out on the device from the bills and payments in Room, never stored.
- **Recovery code**: after the first successful sync of an anonymous user whose prompt is `NOT_DUE`, it asks the
  server for a recovery code.

A new synced table follows the same shape: `updatedAt` and `pendingSync` columns, an `applyRemote` query with the
same guard, a `markSynced` that compares `updatedAt`, and a pull and push step in `syncNow()`. Its pending flag
also joins `observeHasPendingChanges()` in `observeState()` and `syncWhenDue()`, or its changes neither show as
pending nor trigger a sync.

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
web. `ProviderSignIn` (for the Welcome and Restore ViewModels) finishes a browser sign in when it sees
`AccountStatus.Connected` while the screen is on top of the stack, since on the web the page reloaded in between.

A browser link follows the ID token rule too, but supabase-kt drops the callback's error while a session is signed
in, as the anonymous one is during a link. So each platform also hands the callback URL to `HandleBrowserCallback`
(after `handleDeeplinks` on the native apps, the URL the page loaded with on the web), and
`AccountRepositoryImpl.handleBrowserCallback` reads it itself (`SupabaseCallbackUrl`). The callback doesn't say
which provider failed, so `signInWithBrowser` keeps the provider being linked in the account row
(`pendingLinkProvider`), which survives the page reload and a process death. On `identity_already_exists` it signs
in to the user that owns the identity with a second browser trip, started right away with no prompt, the same
silent switch as the ID token path. The pending link is cleared once the callback is handled (after the second trip
started), when the link can't start, and on sign out; a URL with none pending is ignored, so a reload never starts
the trip twice. On the web the callback is the page itself, so only Supabase's full shape counts as one (`error`
with `error_description`, or `access_token` with `refresh_token`): an unrelated `?code=` or `?error=` is not.

A browser closed without finishing sends no callback at all, so the account layer also remembers, in memory, that a
browser trip is open, and each native app reports leaving and coming back to the foreground: `MainActivity.onPause`
and `onResume` on Android, `willResignActive` and `didBecomeActive` in `BoraRacharApp` on iOS and macOS
(`HandleAppPaused`, `HandleAppResumed`). A trip that saw the app leave and is still open when the app is back was
abandoned: `ObserveAbandonedBrowserSignIn` says `CANCELLED`, `ProviderSignIn` reports it as an `Ended` that didn't
fail to the welcome or the restore, whichever is on top of the stack (the one that started the trip, not one left
under it), and they and `ProviderConnector` (for the recovery code and the profile) turn the spinner off with no
error, as for a cancelled native sheet. This relies on the callback arriving before the app is reported active
(Android calls `onNewIntent` before `onResume`, SwiftUI `onOpenURL` before `didBecomeActive`), and on `handleBrowserCallback` closing the trip
before its first suspension, which is why both bridges run it on `Dispatchers.Main.immediate`. A callback that can't finish a sign in ends the wait too: a cancel on
the provider's page (`access_denied`, Apple's `user_cancelled_authorize`) as `CANCELLED`, any other error, or a second
trip that can't start, as `FAILED`, which shows the screen's sign in error. The web never reports either: the page
reloads, so no trip survives it.

A switch can land on any screen, the recovery code sheet included, which closes on that same update. So
`SyncCoordinator` syncs again whenever the session moves from one user id to another, and the account's profile
wins over the one on the device, even over a change not pushed yet: the copy on the device was the previous
user's. The profile row stores whose copy it is (`ProfileEntity.ownerUserId`), and every sync checks it against
the user it runs under, rather than a switch seen in memory, so a sync already in flight, a switch the app died in
or a pull that failed can't push another user's copy:

- A copy of this user, or one made before its account (`NULL`, claimed by the first sync), follows Last-Write-Wins.
- Another user's copy is replaced by the account's profile whatever its age, a change or a photo not pushed yet
  included (`ProfileDao.replaceOtherUsersCopy`). An edit made to it keeps its owner, so it gives way too.
- If the account has no profile on the server, another user's copy goes up as the account's
  (`ProfileDao.reclaimOtherUsersCopy`). A photo already uploaded is the previous user's storage object, so it is
  dropped (`ProfilePhotoRemoteDataSource.isUploaded`); a Google or Apple picture, or a photo not uploaded yet,
  goes with it.
- `uploadPendingProfile` pushes only a copy of the user it pushes for.
- Payment keys never move to another account: a key holds a CPF or a wallet, so another user's keys are deleted
  whatever their state (`PaymentKeyDao.deleteOtherUsersKeys`), keys made before the account are claimed
  (`claimUnowned`), and `uploadPendingKeys` pushes only this user's. Groups follow the same rule on this device:
  another user's local groups are deleted (`GroupDao.deleteOtherUsersGroups`); they stay on the server for their
  members.

A switch also drops a sign out scheduled with "Sair ao sincronizar", in the same write as the session's new user
(`AccountRepositoryImpl.toMirrorOf`): it was for the previous account. A link keeps the id and doesn't trigger any
of this.

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
