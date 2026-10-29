## Navigation

There is **one back stack**, in `:core:navigation`, and the shared ViewModels move it. Each UI only draws it:
Navigation 3 on Android, a `NavigationStack` path on Apple, the last entry plus the browser history on the web. So
navigation logic is tested once, in the ViewModel tests, and every platform walks the same flow.

### Routes

```kotlin
sealed interface Route {
    data object Welcome : Route

    data object ProfileSetup : Route

    data object Restore : Route

    data object Groups : Route
}
```

Routes are plain `data object`s (or `data class`es when a screen needs an argument). They are not `@Serializable`
Navigation 3 keys: the back stack lives in the `Navigator` singleton, and after process death `AppViewModel` resolves
the start route again.

### The `Navigator`

Inject `Navigator` into the ViewModel and call it from `onEvent` or after a result:

| Call | Does | Example |
| --- | --- | --- |
| `navigate(route)` | Pushes `route` | `OnUsedOnAnotherDeviceClicked -> navigator.navigate(Route.Restore)` |
| `navigateBack()` | Pops the top entry, never the last one | `OnBackClicked -> navigator.navigateBack()` |
| `replaceTop(route)` | Puts `route` in the top entry's place, so back skips the screen it replaced | `navigator.replaceTop(Route.Invite(groupId))` once "Novo grupo" saved the group |
| `replaceAll(route)` | Leaves only `route`, so back can't return to the first access flow | `navigator.replaceAll(Route.Groups)` after a profile is saved or restored |
| `popTo(size)` | Keeps the first `size` entries (at least one) | Called by the UIs, not the ViewModels: see below |

```kotlin
private suspend fun finishSignIn() {
    completeSignIn()
        .onSuccess { outcome ->
            finishConnecting(error = null)
            when (outcome) {
                SignInOutcome.PROFILE_RESTORED -> navigator.replaceAll(Route.Groups)
                SignInOutcome.NEEDS_PROFILE -> navigator.navigate(Route.ProfileSetup)
            }
        }.onFailure { finishConnecting(error = WelcomeError.SIGN_IN_FAILED) }
}
```

`BackStackNavigator(startRoute = Route.Welcome)` is the single implementation (`navigationModule`). On launch,
`AppViewModel` starts the sync and replaces the stack with `Route.Groups` when Room already holds a profile, or
`Route.Welcome` otherwise; until then its `uiState` is `AppUiState.LOADING` and every UI draws an empty surface.

**Navigation never belongs in a `UiAction`.** `UiAction` is for what only a platform can do (see
[state-management.md](state-management.md#uiaction--one-shot-effects-only-a-platform-can-perform)).

### Android: Navigation 3

`androidApp/.../navigation/AppNavDisplay.kt` collects the back stack and hands it to `NavDisplay`:

```kotlin
NavDisplay(
    backStack = backStack,
    onBack = { appViewModel.navigator.navigateBack() },
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(rememberSaveableStateHolder()),
        rememberViewModelStoreNavEntryDecorator(),
    ),
    entryProvider = entryProvider {
        entry<Route.Welcome> { WelcomeScreen() }
        entry<Route.ProfileSetup> { ProfileSetupScreen() }
        entry<Route.Restore> { RestoreScreen() }
        entry<Route.Groups> { GroupsScreen(recoveryCodeSheet = { RecoveryCodeSheet() }) }
    },
)
```

The ViewModel store decorator gives each entry its own ViewModel, cleared when the entry is popped. The system back
gesture goes through `navigateBack()`, so the shared stack stays the source of truth.

### Apple: `NavigationStack` path

`AppModel` mirrors the back stack through `backStackWatcher` into `[AppRoute]` (a Swift `Hashable` enum, mapped from
the Kotlin `Route` in `AppRoute.init`). `RootView` shows the first entry as the stack's root and binds the rest as
its path:

```swift
var path: [AppRoute] {
    get { Array(backStack.dropFirst()) }
    set {
        guard newValue.count < backStack.count - 1 else { return }
        holder.viewModel.navigator.popTo(size: Int32(newValue.count + 1))
    }
}
```

A push always starts in a ViewModel and reaches SwiftUI through the watcher. A pop can start in SwiftUI (the back
button, the edge swipe): the `NavigationStack` already moved, so the setter only tells the shared stack how many
entries remain with `popTo`. When the root is `.groups`, `RootView` swaps the stack for `MainTabView`, the
`TabView` with `.tabViewStyle(.sidebarAdaptable)` (a tab bar on iPhone, a sidebar on iPad and Mac).

### Web: last entry and browser history

`AppPresenter.observeBackStack` sends the stack as `JsRoute`s: a route name (`RouteName.kt`: `welcome`,
`profile-setup`, `restore`, `groups`…) plus the arguments of the routes that carry them (`payment-key`'s method and
key id), with the route itself kept inside so `PaymentKeyPresenter` opens its ViewModel with it. `App.tsx` renders
only the last one, except `payment-key` and `delete-account`, dialogs drawn over the entry under them (the profile), and
`useBrowserHistory` keeps the history in step (`create-group` and `invite` are dialogs over the home the same way): a
longer stack `pushState`s an entry holding its size, and a `popstate` (the browser's back button) calls
`app.popTo(size)`, the same catch-up as SwiftUI's.

### Sheets and tabs are state, not routes

The recovery code is not a route. `GroupsUiState.showsRecoveryCodePrompt` says when it is due, and each UI shows
the recovery code feature over the groups: Android through the `recoveryCodeSheet` slot that `AppNavDisplay`
fills (features never see each other), SwiftUI with `.sheet(isPresented:)` in `MainTabView`, the web with
`RecoveryCodeDialog`. Closing it without "Já guardei" sends `OnRemindLaterClicked`, so it comes back on the next
launch.

The tabs Saldos, Contatos and Atividade are drawn on every platform but not built yet: their events emit
`GroupsUiAction.ShowComingSoon` and the selection stays on Grupos.

### Adding a route

1. Add a `data object` (or `data class`) to `Route`.
2. Android: an `entry<Route.X> { XScreen() }` in `AppNavDisplay`.
3. Apple: a case in `AppRoute`, its mapping in `AppRoute.init` (an unknown route falls back to `.welcome`, so a
   missing case fails silently) and in `RootView.screen(for:)`.
4. Web: a name in `toRouteName` (`RouteName.kt`, an exhaustive `when`) and a case in `Screen` of `App.tsx`.
