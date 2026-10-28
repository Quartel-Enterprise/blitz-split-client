# The `prepareScenario` factory

Each test class ends with a single `private fun prepareScenario(...)` that assembles the system under test and its
fakes. Tests call it in their **Given** section instead of repeating the construction, so the wiring lives in one
place.

Rules:

- **`prepareScenario` never returns anything.** It assigns the system under test, and every collaborator a test
  drives or asserts on, to `lateinit var` fields declared at the top of the class. The **When** and **Then** read
  named fields (`viewModel`, `account`, `navigator`), never a holder object.
- It is the **last member of the class**; only a companion object may follow it. Private fakes used by this file
  only go after the class.
- Tests call it under `// Given`, never under `// When` or `// Then`.
- Parameterize it with what varies per test (whether the device is online, the fake's failure, the providers that
  are native here), with defaults for the values most tests share. A parameter every test passes the same value
  to is not a parameter.
- When the setup needs a coroutine scope (to collect `uiAction`, or to give `SyncCoordinator` its scope), make it
  an extension on `TestScope` and use `backgroundScope` (see [fakes-and-coroutines.md](fakes-and-coroutines.md)).
- **Nothing varies per test → no `prepareScenario`.** A stateless system under test becomes a field initializer
  (`private val useCase = ValidateDisplayNameUseCase()`), and the tests keep only their inputs under `// Given`.
- Fixtures several tests share are `private val`s of the class (`private val anonymousUser = SessionUser(...)`),
  declared before `prepareScenario`.

## A ViewModel, its fakes and its actions

```kotlin
class RestoreViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var account: FakeAccountRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: RestoreViewModel
    private val actions = mutableListOf<RestoreUiAction>()

    // ... @BeforeTest / @AfterTest set and reset Dispatchers.Main, then the tests ...

    private fun TestScope.prepareScenario(
        online: Boolean = true,
        outcome: SignInOutcome = SignInOutcome.PROFILE_RESTORED,
        restoreError: Throwable? = null,
        native: Set<AccountProvider> = emptySet(),
        connectResult: Result<ConnectResult> = Result.success(ConnectResult.LINKED),
    ) {
        account = FakeAccountRepository().also {
            it.restoreError = restoreError
            it.connectResult = connectResult
        }
        navigator = BackStackNavigator(startRoute = Route.Welcome).also { it.navigate(Route.Restore) }
        viewModel = RestoreViewModel(
            providerSignIn = ProviderSignInImpl(
                nativeSignIn = NativeSignIn(native),
                connectWithIdToken = ConnectWithIdTokenUseCase(account),
                connectWithBrowser = ConnectWithBrowserUseCase(account, navigator),
                completeSignIn = { Result.success(outcome) },
                observeAccountStatus = ObserveAccountStatusUseCase(account),
                observeReturningBrowserSignIn = ObserveReturningBrowserSignInUseCase(account),
                observeAbandonedBrowserSignIn = ObserveAbandonedBrowserSignInUseCase(account),
            ),
            restoreWithRecoveryCode = RestoreWithRecoveryCodeUseCase(account),
            navigator = navigator,
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(online) },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
```

A ViewModel that drives `ProviderSignIn` or `ProviderConnector` gets the real implementation over the fakes, so its
tests walk the whole sign in. The navigator starts where the screen really is in the flow (`Welcome`, then `Restore`),
so a test can assert that back returns to `listOf(Route.Welcome)` and that a restore leaves only `listOf(Route.Groups)`.

## A repository and its data sources

```kotlin
class AccountRepositoryImplTest {
    private lateinit var local: FakeAccountLocalDataSource
    private lateinit var remote: FakeAccountRemoteDataSource
    private lateinit var repository: AccountRepositoryImpl

    // ... tests ...

    private val anonymousUser = SessionUser("anonymous", socialProvider = null, displayName = null, photoUrl = null)

    private fun prepareScenario(
        currentUser: SessionUser?,
        linkError: Throwable? = null,
        issueError: Throwable? = null,
    ) {
        local = FakeAccountLocalDataSource()
        remote = FakeAccountRemoteDataSource(currentUser, linkError, issueError)
        repository = AccountRepositoryImpl(local = local, remote = remote)
    }
}

private class FakeAccountLocalDataSource : AccountLocalDataSource { ... }

private class FakeAccountRemoteDataSource(...) : AccountRemoteDataSource { ... }
```

## Enforcement

Three custom ktlint rules (in [ktlint-rules](https://github.com/Quartel-Enterprise/ktlint-rules)) check every test
source set:

- `quare:prepare-scenario-returns-unit`: `prepareScenario` has a block body and no return type.
- `quare:prepare-scenario-last-member`: nothing but its overloads and the companion object comes after it in the
  class.
- `quare:prepare-scenario-in-given`: a test calls it under `// Given`.
