## Navigation

There is **one back stack**, in `:core:navigation`, and the shared ViewModels move it, directly or through a
controller they hand their `Navigator` to. Each UI only draws it: Navigation 3 on Android, a `NavigationStack` path
on Apple, the last entry plus the browser history on the web. So navigation logic is tested once, in the ViewModel
tests, and every platform walks the same flow.

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
| `replaceTop(route)` | Puts `route` in the top entry's place, so back skips the screen it replaced | `navigator.replaceTop(Route.Invite(groupId, isNewGroup = true))` once "Novo grupo" saved the group; Convidar's "Ver quem já entrou" and "Ir para o grupo", and the group's tabs (Despesas, Saldos, Membros), go through `openGroupTab`, which treats the three tabs as one group screen: it goes back to the tab asked for when it is right below, puts it in the other tab's place, or replaces the top |
| `replaceAll(route)` | Leaves only `route`, so back can't return to the first access flow | `navigator.replaceAll(Route.Home)` after a profile is saved or restored |
| `popTo(size)` | Keeps the first `size` entries (at least one) | Called by the UIs, not the ViewModels: see below |

A flow two screens share takes the ViewModel's `Navigator` along: `ProviderSignInController` (`:core:sync`),
created by the Welcome and Restore ViewModels, opens the next screen once a Google or Apple sign in is done.

```kotlin
is SignInStep.SignedIn -> {
    showConnecting(null, false)
    when (step.outcome) {
        SignInOutcome.PROFILE_RESTORED -> navigator.replaceAll(Route.Home)
        SignInOutcome.NEEDS_PROFILE -> navigator.navigate(Route.ProfileSetup)
    }
}
```

`BackStackNavigator(startRoute = Route.Welcome)` is the single implementation (`navigationModule`). On launch,
`AppViewModel` starts the sync and replaces the stack with `Route.Home` (Início) when Room already holds a profile, or
`Route.Welcome` otherwise; until then its `uiState` is `AppUiState.LOADING` and every UI draws an empty surface.

### Links opened from outside: `LinkInbox`

An invite link (`https://blitzsplit.com/g/<code>`, or a member's personal one, `.../g/<code>/<member code>`) can arrive
before the app knows where it starts: Android's `MainActivity` gets it from the intent (App Links, `autoVerify` on
`blitzsplit.com/g/`), Apple from `onOpenURL` / Universal Links through `BlitzSplitApple.handleDeepLink`, and the web
from the page's own URL (`BlitzSplitWeb.openInviteLink`, which then puts `/` back in the address bar). Each one only
drops the URL into the `LinkInbox` singleton of `:shared`. `AppViewModel` waits until its `uiState` is `READY`, takes
the link and pushes `Route.JoinInvite(code, memberCode, inviterCode)` over whatever started (the groups, or the welcome
before there is a profile), so the start route logic stays in one place. `InviteLink.targetOf` reads the codes from any
host, so `borarachar.com` keeps working once it redirects there. Joining replaces the stack with `[Groups,
Members(groupId, joinedAs)]`, or `[Groups, GroupDetail(groupId)]` when there is no member name to greet.

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
entries remain with `popTo`. When the root is `.home` (Início), `RootView` swaps the stack for `MainTabView`, the
`TabView` with `.tabViewStyle(.sidebarAdaptable)` (a tab bar on iPhone, a sidebar on iPad and Mac).

Some routes open as a sheet instead of a push (`AppRoute.presentsAsSheet`: a payment key, and a bill on iPad and
Mac). `AppModel.sheet` shows the top entry when it is one, and `path` leaves it out. When the two top entries are
both sheets ("Editar conta" over a bill's "Conta"), `AppModel.stackedSheet` presents the second over the first
instead of replacing it, and closing either pops the shared stack down to what is still shown. On the Mac, a group's
Convidar over one of its tabs is `AppModel.invitePopover` instead: the root hands it down (`\.invitePopover`) and the
tab's button draws it (`.invitePopover(host:)`); closing it pops it the same way.

#### The Mac's Ajustes: two windows, one back stack

The Mac's profile is Ajustes, the app's `Settings` window, which draws the same back stack as the main window: a
`Route.Profile` the shared code pushes, from whichever tab (Início's or Grupos' avatar, Atividade's, "Proteja seu
acesso"), is popped at once and opens Ajustes instead, and what Ajustes opens
(`AppRoute.opensInSettings`: a key, deleting the account) is pushed on top as usual. Each window reads only its own
entries, so a sheet open in one never changes the other's:

- `AppModel.windowStack`, the back stack without the trailing `opensInSettings` entries, is all the main window's
  `path`, `sheet`, `stackedSheet`, `invitePopover` and `cover` read, and what their setters pop to. A key opened in
  Ajustes over the main window's "Editar grupo" leaves that sheet where it is.
- `AppModel.settingsSheet` is the top entry while it is one of Ajustes'; closing it pops down to `windowStack`.
- Ajustes' entries only ever sit on top, since a ViewModel's `navigateBack()` pops the top whichever window drew
  it: while `settingsSheet` is open the main window is disabled, and a screen the shared code still opens over it (a
  link opened from outside, a menu command) closes Ajustes' entries, `AppModel` keeping what was opened above them
  (`replaceAbove`).

Ajustes draw the profile while there is one, once Início is the root (`AppModel.hasProfile`), and close once none
is left. Since the profile is never on the back stack there, a screen never learns where it is from the top entry: the
profile tells `ProviderConnector.connect` that a browser sign in leaves from it (`isFromProfile`), so the browser
coming back reopens it, which on the Mac opens Ajustes.

### Web: last entry and browser history

`AppPresenter.observeBackStack` sends the stack as `JsRoute`s: a route name (`RouteName.kt`: `welcome`,
`profile-setup`, `restore`, `groups`…) plus the arguments of the routes that carry them (`payment-key`'s method and
key id), with the route itself kept inside so `PaymentKeyPresenter` opens its ViewModel with it. `App.tsx` renders
only the last one, except `payment-key` and `delete-account`, dialogs drawn over the entry under them (the profile), and
`useBrowserHistory` keeps the history in step (`create-group` and `invite` are dialogs over the home the same way): a
longer stack `pushState`s an entry holding its size, and a `popstate` (the browser's back button) calls
`app.popTo(size)`, the same catch-up as SwiftUI's.

### Sheets and tabs are state, not routes

The recovery code is not a route. `HomeUiState.showsRecoveryCodePrompt` and `GroupsUiState.showsRecoveryCodePrompt`
say when it is due, and each UI shows the recovery code feature over Início and the groups: Android through the `recoveryCodeSheet` slot that `AppNavDisplay`
fills (features never see each other), SwiftUI with `.sheet(isPresented:)` in `MainTabView`, the web with
`RecoveryCodeDialog`. Closing it without "Já guardei" sends `OnRemindLaterClicked`, so it comes back on the next
launch.

The home has five tabs: Início, Grupos, Saldos, Contatos and Atividade. Início (`Route.Home`) is the root of the
back stack and where the app opens; every other tab is one route right above it, and the tabs take each other's place
there, as a tab bar does: `[Home]`, `[Home, Groups]`, `[Home, GlobalBalances(filter, charge)]`, `[Home, Contacts]`,
`[Home, Activity(personKey)]`. `Navigator.openHomeTab(tab)` (`HomeTabs.kt`) goes back to the tab when it is already in
the stack, closing whatever it opened, and otherwise puts it right above Início; without Início in the stack (the
welcome, the restore) it does nothing. `popToGroups()` is `openHomeTab(Route.Groups)`. Each tab's ViewModel has an
`OnHomeTabClicked`, and on Apple the `TabView` selects the tab from the route right above Início. Back from any tab
returns to Início. Contatos' profile (`Route.ContactProfile`), agenda (`Route.ContactsAgenda`) and invite
(`Route.ContactInvite`) open over it, or in its pane on wide windows (Contatos keeps the pick in its ViewModel, like
Saldos). Atividade opens on a person from a contact's history ("Ver tudo"), and on everyone from Início's "Ver tudo".

Início's "Registrar pagamento" belongs to no group, so it asks who paid whom first: `Route.PickPayment` lists every
open pair of every group, and the pair tapped opens `Route.RegisterPayment` in its place (`replaceTop`), so closing
the form returns to Início, which shows its notice.

### List and detail on wide windows

A wide window draws some lists with what they opened beside them (the canvas' list and detail). The back stack stays
the only source of truth: nothing about the window reaches it, and a phone walks the same entries.

**A route in the pane** (Grupos, 5.1). The group's tabs (`GroupDetail`, `Balances`, `Members`) stay routes right
above `Route.Groups`. Each UI decides from its own window whether it draws that entry beside the list or over it:

- Android: `AppNavDisplay` adds Material's `ListDetailSceneStrategy` (`adaptive-navigation3`) after the dialog and
  sheet strategies. `Route.Groups` carries `listPane` metadata (with its width and a placeholder), the group's tabs
  `detailPane`. `rememberHomeListDetailStrategy` caps the directive at two panes only where `rememberListDetailLayout`
  says so (a tablet lying down and a book, the halves split on the hinge the device reports: see
  [responsive-layouts.md](responsive-layouts.md)), else at one, which leaves the strategy out and the screens as on a phone. The strategy only claims the pane entries
  on top, so any other route above the groups (Estatísticas, the profile) is a scene of its own, full screen, and a
  dialog or sheet over the group keeps the scene drawn under it. `LocalInDetailPane` tells a screen it sits in the
  pane: `InGroupPane` reads it from the scene itself (`LocalListDetailSceneScope`, which Material provides to the
  panes it draws, while the list pane shows), never from the window, so a tab drawn alone keeps its back arrow.
  `rememberDeviceLayout` gives it a tablet's upright column, and it drops its back arrow and "Rachar conta", which
  the list's rail has.
- Apple: `GroupsColumns` (the Groups and Search tabs) keeps the list's `NavigationStack` and, lying down with an
  iPad's room (`ScreenLayout.showsListDetail`: an iPad, an iPhone Duo open) while the path holds nothing but the group's tabs (or nothing), roots a second
  one beside it with the first of them (else a placeholder). Any other route above the groups goes back to the one
  stack, which pushes the whole path over the list as upright, the way Android's strategy decides; the sheets, which
  the path leaves out, keep the columns under them. Only the tab in front reports the pane, once it has a size. The
  Mac's groups are a table and keep one stack.
- The web's Grupos is a table on every width; it draws no pane.

The UI tells the ViewModel whether it draws the pane (`GroupsUiEvent.OnDetailPaneChanged`), since that changes
what a tap means: with the pane, the list is in sight while only the pane is above it (`showsOnlyPaneAboveGroups`),
so a row opens its group in the place of the one shown (`Navigator.openBesideGroups`, one `replaceAbove` step);
without it, only the groups on top take taps, so a late tap on a phone never stacks a second group. The pane's group
comes back as state (`GroupsUiState.selectedGroupId`, from `groupBesideGroupsOrNull`), which the list highlights only
while it draws the pane. With the pane, the search opens its first result there as it is typed
(`GroupsDetailPane.preview`). Back from the pane closes it to the placeholder; turning the device upright leaves the
group open, drawn over the list.

**A selection in the ViewModel** (Saldos, flow 8). When the detail is not a screen of its own, it is state: the
ViewModel keeps what was picked (`GlobalBalancesUiEvent.OnPersonSelected`, ignored for someone the filter doesn't
list) and says who the pane shows (`GlobalBalancesUiState.selectedPerson`: the pick while the filter lists it, else
the first row), which every UI draws as it comes, the web's presenter handing over that row. Each UI draws the
pane where its window has room (Android's `rememberListDetailLayout`, the iPad lying down, a Mac window from 700 pt
for the inspector column, the web from 1024 px), and the rows inline everywhere else. No route, so nothing changes
on the back stack, and nothing to tell the ViewModel about the window.

Atividade (page 12) keeps its pick the same way (`ActivityUiState.pickedItemId`, `selectedItem`), with one
difference: a tap on a row opens what it is about where there is no pane, and only picks it where there is one. So
its UIs do tell the ViewModel whether they draw the pane (`ActivityUiEvent.OnDetailPaneChanged`), and the pane's own
button opens it (`OnOpenTargetClicked`).

Pick the first when the detail is a screen that also opens on its own (it already has a route); the second when it
only exists beside its list.

### Adding a route

1. Add a `data object` (or `data class`) to `Route`.
2. Android: an `entry<Route.X> { XScreen() }` in `AppNavDisplay`. A route drawn over the screen below it passes
   metadata: `DialogSceneStrategy.dialog(...)` for a dialog (Conta and Registrar pagamento on a tablet), or
   `BottomSheetSceneStrategy.bottomSheet` for a modal bottom sheet (Registrar pagamento on a phone), whose dismissal
   pops the back stack.
3. Apple: a case in `AppRoute`, its mapping in `AppRoute.init` (an unknown route falls back to `.welcome`, so a
   missing case fails silently) and in `RootView.screen(for:)`. A route pushed over the groups also needs its case
   in `MainTabView`'s `navigationDestination`, whose `default` is the profile.
4. Web: a name in `toRouteName` (`RouteName.kt`, an exhaustive `when`) and a case in `Screen` of `App.tsx`.
