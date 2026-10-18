# Fakes & coroutine testing

## Fakes, not mocks

There is no mocking library. Collaborators are faked by hand, which works because everything a class depends on is
an interface: repositories, data sources, use cases (`fun interface`), `CurrentTimeMillis`, and Quare's
`NetworkConnectivityObserver`.

Pick the lightest fake that works:

| Collaborator | Fake | Example |
| --- | --- | --- |
| A use case whose result is all that matters | a lambda | `completeSignIn = { outcome }` |
| A use case over a repository | the real `impl/` class over the shared fake repository | `ConnectWithIdTokenUseCase(account)` |
| Connectivity | a lambda over a `MutableStateFlow` the test flips | `NetworkConnectivityObserver { isOnline }`, then `isOnline.value = true` |
| The clock | a lambda | `currentTimeMillis = { now }` |
| Navigation | the real `BackStackNavigator` | assert `navigator.backStack.value` |
| A data source of the class under test | a `private class FakeX : X` at the end of the test file | `FakeAccountRemoteDataSource` |

A private fake only needs real behavior for what the tests use; stub the rest with `error("unused")`.

## Shared fakes

A fake of a core repository that several modules' tests need lives in that module's `:testing` submodule instead of
being copied into each test source set:

| Module | Fakes |
| --- | --- |
| `:core:account:testing` | `FakeAccountRepository` |
| `:core:profile:testing` | `FakeProfileRepository` |

Depend on it from a test source set only:

```kotlin
commonTest.dependencies {
    implementation(libs.kotlinx.coroutines.test)
    implementation(projects.core.account.testing)
    implementation(projects.core.profile.testing)
}
```

`assertModuleGraph` rejects a production dependency on a `:testing` module, and Kover does not measure them.

A shared fake is not written for one test, so it behaves like an in-memory version of the real thing instead of
stubbing methods:

- Its state is a public `MutableStateFlow` a test sets or reads: `status`, `recoveryCode`, `recoveryCodePrompt`,
  `profile`.
- Every call is recorded in a public list or counter: `idTokenConnections`, `browserConnections`, `restoredCodes`,
  `registrationCount`, `recoveryCodeCreations`, `savedProfiles`, `uploadedUserIds`, `downloadedUserIds`.
- Writes change the state the way the real repository would: `ensureRegistered()` turns `LocalOnly` into
  `Anonymous(ANONYMOUS_USER_ID)`, a successful `signInWithIdToken` makes the status `Connected`,
  `createRecoveryCode()` stores the code and makes the prompt `DUE`.
- Failure paths are public `var`s: `connectResult`, `registerError`, `restoreError`, `createRecoveryCodeResult`,
  `pushError`, `pullError`.
- The ids it hands out are constants of its companion (`FakeAccountRepository.ANONYMOUS_USER_ID`), so tests assert
  on them by name.

When a repository method changes, update its shared fake in the same change. A fake used by one module only stays a
`private class` in that module's tests.

## Coroutines

- Wrap test bodies in `runTest { }`. Virtual time auto-advances, so `delay` and timeouts resolve without waiting.
- No `@OptIn(ExperimentalCoroutinesApi::class)`: `blitzsplit.kotlin.multiplatform` opts every module in.
- **ViewModels** run on `viewModelScope`, which uses `Dispatchers.Main`. Set it to an `UnconfinedTestDispatcher` in
  `@BeforeTest`, reset it in `@AfterTest`, and run the test on the same dispatcher:

  ```kotlin
  class GroupsViewModelTest {
      private val testDispatcher = UnconfinedTestDispatcher()

      @BeforeTest
      fun setUp() {
          Dispatchers.setMain(testDispatcher)
      }

      @AfterTest
      fun tearDown() {
          Dispatchers.resetMain()
      }

      @Test
      fun `GIVEN no internet WHEN the groups open THEN shows the offline badge`() = runTest(testDispatcher) { ... }
  }
  ```

  The unconfined dispatcher runs `init`'s collectors and each `viewModelScope.launch` eagerly, so the state is
  already settled when `onEvent` returns and the **Then** reads `viewModel.uiState.value` directly.

- **`UiAction`s** are a `SharedFlow` without replay: collect them into a list from `backgroundScope` *before* the
  action happens, in `prepareScenario` or in the test's **Given**:

  ```kotlin
  backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
  ```

  `backgroundScope` is cancelled when the test ends.

- **Long-running collectors** (`SyncCoordinator.start()`, `mirrorSession()`): give them `backgroundScope` as their
  scope and step with `runCurrent()`:

  ```kotlin
  // Given
  prepareScenario(storedProfile = pierre, online = false)
  coordinator.start()
  runCurrent()

  // When
  isOnline.value = true
  runCurrent()
  ```

## Gotcha: exception identity

`kotlinx-coroutines` stack trace recovery may **copy** an exception as it crosses coroutine boundaries, so the caught
instance is not always the object thrown. Assert the type (and the message when it matters), not identity:

```kotlin
assertIs<SocialAccountHasNoRecoveryCodeException>(result.exceptionOrNull())
```
