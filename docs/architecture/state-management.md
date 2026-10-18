## State Management (UDF)

Every screen is a shared `ViewModel` in `:feature:*` with up to three types in `presentation/model/`: `UiState`,
`UiEvent` and `UiAction`. State flows down to the UI, events flow up to the ViewModel, and the three native UIs only
draw and forward. A decision taken in a UI is a decision Android, Apple and the web will each take differently.

### UiState — what the screen shows

A `data class` whose fields never have default values: the ViewModel declares its initial state in full, and a
field added later can't silently default at call sites that never decided on it.

```kotlin
data class RestoreUiState(
    val code: String,
    val isCodeComplete: Boolean,
    val isOnline: Boolean,
    val isRestoring: Boolean,
    val connectingProvider: AccountProvider?,
    val error: RestoreError?,
) {
    val isBusy: Boolean
        get() = isRestoring || connectingProvider != null

    val areAccountButtonsEnabled: Boolean
        get() = isOnline && !isBusy

    val canRestore: Boolean
        get() = isOnline && isCodeComplete && !isBusy

    val showsOfflineNotice: Boolean
        get() = !isOnline
}
```

Rules derived from the state are **getters on the `UiState`**, never conditions in a UI: Compose, SwiftUI and the JS
presenter all read `areAccountButtonsEnabled`, so the three agree by construction.

UiState carries no copy. An error is an enum (`RestoreError`, `WelcomeError`) or a flag (`hasSignInFailed`), and a
status is an enum (`SyncBadge`); each UI maps it to its pt-BR text. Use a `sealed interface` only when the screen
has genuinely distinct shapes; an enum is enough for a status without data (`AppUiState.LOADING` / `READY`).

### UiEvent — what the person did

A `sealed interface` that extends the `UiEvent` marker of `:core:presentation`, named after what happened, not what
should happen:

```kotlin
sealed interface WelcomeUiEvent : UiEvent {
    data class OnProviderClicked(
        val provider: AccountProvider,
    ) : WelcomeUiEvent

    // Why: the platform's native button (Credential Manager, Sign in with Apple) returned a token.
    data class OnIdTokenReceived(
        val provider: AccountProvider,
        val idToken: String,
        val rawNonce: String?,
    ) : WelcomeUiEvent

    data object OnNativeSignInFailed : WelcomeUiEvent

    data object OnNativeSignInCancelled : WelcomeUiEvent
}
```

The result of a platform effect comes back as an event too (`OnIdTokenReceived`, `OnQrScanned`), so the ViewModel
stays the only place that reacts to it.

### UiAction — one-shot effects only a platform can perform

```kotlin
sealed interface RecoveryCodeUiAction {
    data class CopyToClipboard(
        val formattedCode: String,
    ) : RecoveryCodeUiAction

    // Why: a password manager entry on Android and Apple; on the web it becomes a file download.
    data class SaveToPasswordManager(
        val formattedCode: String,
    ) : RecoveryCodeUiAction

    data class LaunchNativeSignIn(
        val provider: AccountProvider,
    ) : RecoveryCodeUiAction
}
```

Clipboard, password manager, native sign in sheet, camera scanner, a transient "Em breve": things Kotlin can't do.
A screen with none has no `UiAction` (`ProfileSetupViewModel`). Navigation is **not** a `UiAction`: the ViewModel
calls the `Navigator` (see [navigation.md](navigation.md)).

### ViewModel structure

```kotlin
class WelcomeViewModel(
    private val nativeSignIn: NativeSignIn,
    private val connectWithIdToken: ConnectWithIdToken,
    private val connectWithBrowser: ConnectWithBrowser,
    private val observeAccountStatus: ObserveAccountStatus,
    private val completeSignIn: CompleteSignIn,
    private val connectivityObserver: NetworkConnectivityObserver,
    private val navigator: Navigator,
) : ViewModel() {
    val uiState: StateFlow<WelcomeUiState>
        field = MutableStateFlow(WelcomeUiState(isOnline = true, connectingProvider = null, error = null))

    val uiAction: SharedFlow<WelcomeUiAction>
        field = MutableSharedFlow<WelcomeUiAction>()

    val stateWatcher: FlowWatcher<WelcomeUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<WelcomeUiAction> = FlowWatcher(uiAction)

    init {
        observeConnectivity()
        observeBrowserSignIn()
    }

    fun onEvent(event: WelcomeUiEvent) {
        when (event) {
            is WelcomeUiEvent.OnProviderClicked -> handleProviderClicked(event.provider)
            is WelcomeUiEvent.OnIdTokenReceived -> handleIdToken(event)
            WelcomeUiEvent.OnNativeSignInFailed -> finishConnecting(error = WelcomeError.SIGN_IN_FAILED)
            WelcomeUiEvent.OnNativeSignInCancelled -> finishConnecting(error = null)
            WelcomeUiEvent.OnUseWithoutAccountClicked -> navigator.navigate(Route.ProfileSetup)
            WelcomeUiEvent.OnUsedOnAnotherDeviceClicked -> navigator.navigate(Route.Restore)
            WelcomeUiEvent.OnErrorDismissed -> uiState.update { it.copy(error = null) }
        }
    }

    private fun observeConnectivity() {
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
    }
}
```

- **Public class**, public `uiState`, `uiAction`, `onEvent`, `stateWatcher` and `actionWatcher`: Swift and JS see
  them. An `internal` ViewModel is invisible to the Apple framework.
- **Explicit backing fields** expose the read-only type while the class smart-casts to the mutable one
  (`quare:explicit-backing-field` flags the `_uiState` pattern). Keep the type argument on the constructor when the
  `UiState` is a sealed type, or the field infers the subtype of the initial value.
- **`stateWatcher` and `actionWatcher`** are declared right after the flows they wrap: they are what SwiftUI
  observes. A ViewModel without `uiAction` has no `actionWatcher`.
- **One `onEvent(event)`** with an exhaustive `when`. Each branch is one call; the work goes in a private function.
- Long-lived observation starts in `init` with `.onEach { }.launchIn(viewModelScope)`; one-shot work in
  `viewModelScope.launch { }`. Results come back as `Result` and are handled with `onSuccess` / `onFailure` (see
  [coroutine-error-handling.md](coroutine-error-handling.md)).
- Guards live in the ViewModel too: `handleProviderClicked` returns early when `areAccountButtonsEnabled` is false,
  so a double tap on any platform does nothing.

### How each UI observes it

**Android (Compose)**: the `XScreen` composable owns the ViewModel, collects the state, handles the actions in a
`LaunchedEffect` and hands the state to the stateless `XContent`:

```kotlin
@Composable
fun WelcomeScreen(viewModel: WelcomeViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val launchGoogleSignIn = rememberGoogleSignInLauncher { result -> viewModel.onEvent(/* result as event */) }
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                is WelcomeUiAction.LaunchNativeSignIn -> launchGoogleSignIn()
            }
        }
    }
    WelcomeContent(state = state, onEvent = viewModel::onEvent)
}
```

`XContent(state, onEvent, modifier)` never sees the ViewModel and carries the `@DevicePreviews` previews.

**Apple (SwiftUI)**: an `@Observable` model per screen holds the ViewModel through the `ViewModelHolder` that
`BlitzSplitApple` creates, mirrors the state through `stateWatcher`, reacts to `actionWatcher`, and clears the
holder when the view goes away:

```swift
@Observable
final class WelcomeModel {
    private(set) var state: WelcomeUiState

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createWelcomeViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! WelcomeUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            self?.handle(action)
        })
    }

    func send(_ event: any WelcomeUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}
```

The view keeps it in `@State private var model = WelcomeModel()`. Generics don't survive the Objective-C header,
so the first value is read with a cast; `WatchBag` cancels every `Watch` when the model is released.

**Web (React)**: React can't take Kotlin types, so `shared/src/jsMain/.../bridge` has one `@JsExport` presenter per
screen that maps the state to a plain `JsXState` (enums become their `code` or `name` string) and turns each event
into a method:

```kotlin
class RestorePresenter {
    private val holder = ViewModelHolder(RestoreViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsRestoreState) -> Unit): JsWatch { /* uiState.map { JsRestoreState(...) } */ }

    fun observeOpenQrScanner(onOpen: (Boolean) -> Unit): JsWatch { /* uiAction.filterIsInstance<...>() */ }

    fun restore() {
        viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)
    }

    fun dispose() {
        holder.clear()
    }
}
```

The screen holds it with the hooks of `webApp/src/bridge/usePresenter.ts`:

```tsx
const presenter = usePresenter(() => new RestorePresenter())
const state = useWatch(presenter, (p, onChange: (state: JsRestoreState) => void) => p.observeState(onChange))
useActions(presenter, (p, onOpen: (open: boolean) => void) => p.observeOpenQrScanner(onOpen), () => setScanning(true))
if (!presenter || !state) return null
```

`usePresenter` creates the presenter in an effect and disposes it on unmount (StrictMode-safe); `useWatch` mirrors a
flow into React state; `useActions` runs a callback per action. A text input bound to the state goes through
`useDraft`, so the asynchronous round trip doesn't move the caret.

`uiAction` is a `MutableSharedFlow` without replay: an action emitted while nothing collects is lost. Every UI
subscribes as the screen starts (Compose's `LaunchedEffect`, the Swift model's `init`, `useActions`).

### The bridge

`:core:presentation`'s `bridge` package is what lets Swift and JS use a `ViewModel` at all:

| Type | Why |
| --- | --- |
| `FlowWatcher<T>` | Swift and JS can't collect a `Flow`; `observe(onEach)` collects it on `MainScope()` and calls back on the main thread |
| `Watch` | What `observe` returns: `cancel()` stops the collection when the view goes away. JS wraps it in `JsWatch` |
| `ViewModelHolder<VM>` | Outside Compose nothing owns a `ViewModelStore`; the holder keeps one per screen and `clear()` cancels `viewModelScope`, as Android does |

### Why not SKIE

SKIE would turn flows into Swift `AsyncSequence`s and sealed types into Swift enums, but it supports Kotlin up to
2.4.10 and this project builds with 2.4.21 (the same toolchain as the Bible Planner). The bridge above is small,
works for Swift and JS alike, and has no compiler plugin to wait on. Swift therefore sees Kotlin's own names:
`WelcomeUiEventOnIdTokenReceived(provider: .apple, idToken:, rawNonce:)`, objects through `.shared`
(`WelcomeUiEventOnNativeSignInCancelled.shared`), `is` checks on actions, and `Int32` for a Kotlin `Int`.
