# Coroutine Error Handling

## The problem

Kotlin's `CancellationException` extends `Exception`. A generic `catch (e: Exception)` swallows it and breaks
structured concurrency: the caller sees the coroutine cancelled while its body keeps running. Stdlib `runCatching`
does the same.

```kotlin
// Wrong: cancellation is swallowed
suspend fun ensureRegistered(): Result<String> = runCatching { remote.signInAnonymously().userId }

// Wrong: the same, by hand
suspend fun ensureRegistered(): Result<String> = try {
    Result.success(remote.signInAnonymously().userId)
} catch (e: Exception) {
    Result.failure(e)
}
```

## The solution: `suspendRunCatching`

`core/utils` provides it. It rethrows `CancellationException` and wraps any other `Exception` in `Result.failure`
(an `Error` keeps propagating):

```kotlin
override suspend fun ensureRegistered(): Result<String> = suspendRunCatching {
    val user = remote.getCurrentUserOrNull() ?: remote.signInAnonymously()
    local.update { account -> account.toMirrorOf(user) }
    user.userId
}
```

It is `inline`, so suspend calls, `return@suspendRunCatching` for an early exit and `getOrThrow()` on another
`Result` all work inside it:

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

The custom ktlint rule `quare:suspend-run-catching` flags a `try` that rethrows `CancellationException` and catches
`Exception` or `Throwable`; `suspendRunCatching` itself is the one place that suppresses it.

## Where each layer handles errors

| Layer | Does |
| --- | --- |
| Remote data source | Calls Supabase and lets exceptions through, turning the ones the domain cares about into domain exceptions (`InvalidRecoveryCodeException`, `TooManyRecoveryAttemptsException`, `IdentityAlreadyLinkedException`, `SocialAccountHasNoRecoveryCodeException`) by status code or `AuthErrorCode` |
| Repository | Wraps every network operation in `suspendRunCatching` and returns `Result<T>`. A `try` that catches one specific domain exception inside it is a different intent and stays (`signInLinkingFirst` falls back on `IdentityAlreadyLinkedException`; `createRecoveryCode` settles the prompt and rethrows `SocialAccountHasNoRecoveryCodeException`) |
| Use case | Passes the `Result` through, or adds its rule |
| ViewModel | Never `try`s. Handles the `Result` with `onSuccess` / `onFailure` inside `viewModelScope.launch`, and maps the failure to UI state |
| UI | Maps the UI error to pt-BR text |

## Mapping errors to the UI

A screen shows an error as an enum in its `UiState`, never as a `Throwable` or a message:

```kotlin
enum class RestoreError {
    INVALID_CODE,
    TOO_MANY_ATTEMPTS,
    SIGN_IN_FAILED,
    UNKNOWN,
}

fun Throwable.toRestoreError(): RestoreError = when (this) {
    is InvalidRecoveryCodeException -> RestoreError.INVALID_CODE
    is TooManyRecoveryAttemptsException -> RestoreError.TOO_MANY_ATTEMPTS
    else -> RestoreError.UNKNOWN
}
```

```kotlin
viewModelScope.launch {
    restoreWithRecoveryCode(state.code)
        .onSuccess { finishSignIn() }
        .onFailure { throwable -> uiState.update { it.copy(error = throwable.toRestoreError()) } }
    uiState.update { it.copy(isRestoring = false) }
}
```

- When the screen distinguishes failures, map the `Throwable` in `presentation/mapper` (`toRestoreError()`).
- When it only needs "it failed", set the case directly (`WelcomeError.SIGN_IN_FAILED`) or a flag
  (`RecoveryCodeUiState.hasSignInFailed`).
- A cancelled native sign in is not an error: `OnNativeSignInCancelled` clears the busy state with `error = null`.
- Every error the UI shows has a dismiss event (`OnErrorDismissed`) or clears on the next input
  (`updateCode` resets `error`).

Each UI turns the enum into its words: Android with a `when` to `R.string`, Swift with a `switch`, the web with a
`Record` keyed by the name the presenter sends (`codeErrors[state.error]` in `RestoreScreen.tsx`).

## Do not use stdlib `runCatching`

`kotlin.runCatching` does not rethrow `CancellationException`. Never use it in a suspend context; outside one (pure
parsing in a test, for instance) it is fine.
