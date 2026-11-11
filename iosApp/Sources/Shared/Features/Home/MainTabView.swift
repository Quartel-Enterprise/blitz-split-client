import Shared
import SwiftUI

/// The tabs of the canvas: Home first, then Groups, Balances and Contacts. Search is the search tab at the end of the
/// bar, the room Activity left when it became Home's bell (Pierre's call), there from the first launch, as an item of
/// the navigation never comes and goes (Pierre's beta feedback); an iPad's and the Mac's sidebar hold it atop the
/// tabs, the Mac's opened by ⌘K too, as the web's "Search… ⌘K".
enum MainTab: Hashable {
    case home
    case groups
    case balances
    case contacts
    case search
}

/// iPhone: the floating Liquid Glass tab bar. iPad and Mac: the glass sidebar (sidebarAdaptable), on the Mac with the
/// open groups under the tabs. Home is the root of the shared back stack and every other tab the entry right above it,
/// so the first entry of `path` says which tab is in front: Groups, Balances, Contacts, or else Home, whose own stack
/// is then the whole path (Activity, which its bell pushes, included).
struct MainTabView: View {
    /// The shared back stack above Home: Groups, Balances or Contacts with what they opened, or what Home opened (a
    /// group, the profile, Activity, an invite opened from its link).
    @Binding var path: [AppRoute]

    @State private var home = HomeModel()
    @State private var model = GroupsModel()
    @State private var showsRecoveryCode = false
    /// The last Balances its tab showed, which it keeps drawing while another tab is in front.
    @State private var lastBalancesRoute: RouteGlobalBalances?
    #if os(macOS)
    @State private var sidebar = HomeSidebarModel()
    /// Bumped by ⌘K, which focuses the search tab's field again.
    @State private var searchFocusRequest = 0
    @Environment(\.isWindowUncovered) private var isWindowUncovered
    #endif
    init(path: Binding<[AppRoute]>) {
        _path = path
    }

    var body: some View {
        TabView(selection: tabSelection) {
            #if os(macOS)
            // Atop the sidebar's tabs, where the web's sidebar puts "Search… ⌘K"
            searchTab
            #endif
            Tab(value: MainTab.home) {
                SyncedNavigationStack(path: homePath) {
                    HomeView(model: home)
                        .navigationDestination(for: AppRoute.self) { Self.destination($0) }
                }
                #if os(macOS)
                .measuringNarrowWindow()
                #endif
            } label: {
                Label(HomeCopy.tabTitle, systemImage: "house")
            }
            Tab("Groups", systemImage: "person.3", value: MainTab.groups) {
                // An iPad lying down draws the group beside the list
                GroupsColumns(model: model, path: stackPath(isSearch: false), isInFront: isInFront(isSearch: false)) {
                    GroupsView(model: model)
                }
                #if os(macOS)
                // What the View menu's Grid Size reads and changes, only while the groups are in front
                .focusedSceneValue(\.groupsModel, isInFront(isSearch: false) && groupsPath.isEmpty ? model : nil)
                .measuringNarrowWindow()
                #endif
            }
            // The shared back stack holds Balances right above Home, where its tab shows. Once shown it stays, so
            // coming back to the tab finds it already read, on the filter its route names; only a route with another
            // filter opens it afresh. Its ViewModel then keeps reading the balances behind the other tabs for the
            // rest of the home, the price of coming back without a shimmer, as Home's and Groups' do.
            Tab("Balances", systemImage: "dollarsign.circle", value: MainTab.balances) {
                SyncedNavigationStack(path: balancesPath) {
                    if let route = shownBalancesRoute {
                        GlobalBalancesView(route: route)
                            .id(route)
                            .navigationDestination(for: AppRoute.self) { Self.destination($0) }
                    } else {
                        Color.clear
                    }
                }
                #if os(macOS)
                .measuringNarrowWindow()
                #endif
            }
            // Contacts sits right above Home too, and keeps what it opens in its own tab's stack. Its list stays
            // while another tab is in front, so coming back never reads it again.
            Tab("Contacts", systemImage: "person.crop.circle", value: MainTab.contacts) {
                ContactsColumns(path: tabStack(isShown: isContactsShown))
                    #if os(macOS)
                    .measuringNarrowWindow()
                    #endif
            }
            #if os(iOS)
            searchTab
            #endif
        }
        .tabViewStyle(.sidebarAdaptable)
        #if os(iOS)
        .searchTabApart()
        #endif
        #if os(macOS)
        // The sidebar's button at the detail's leading edge instead of the sidebar's header, as in Ajustes (the system's
        // is hidden, `RootView`'s `hidingSystemSidebarToggle()`)
        .toolbar {
            ToolbarItem(placement: .navigation) {
                SidebarToggleButton()
            }
        }
        // ⌘K (Edit › Search) opens the search tab, or brings the focus back to its field; never under a sheet, which
        // the tab's move would close
        .focusedSceneValue(\.openSearch, searchCommand)
        // Which screen publishes its menu commands: ⌘N and ⇧⌘N (`newItemCommands`), Activity's ⌘R
        .environment(\.frontRoute, path.last)
        // The open groups as Groups lists them, where the boards draw them: over Groups and a group's screens
        .tabViewSidebarFooter {
            if showsOpenGroups {
                SidebarOpenGroups(
                    groups: sidebar.state.openGroups,
                    selectedId: selectedGroupId,
                    onOpen: { sidebar.openGroup($0) },
                )
            }
        }
        #endif
        .glassToast(
            [
                GlassToastMessage(text: "Coming soon", trigger: model.comingSoonCount),
                GlassToastMessage(text: "Notifications on", trigger: home.notificationsOnCount),
                GlassToastMessage(text: "Couldn't save the change. Try again.", trigger: home.changeErrorCount),
            ]
                + GroupExitCopy.errorToasts(
                    leave: model.leaveErrorCount,
                    change: model.changeErrorCount,
                ),
        )
        // Their own toasts, for their longer duration; Groups' and Home's each wait until their screen is on top
        .glassToast(model.deletedGroup.text, trigger: model.deletedGroup.count, duration: DeletedGroupToast.duration)
        .glassToast(home.deletedGroup.text, trigger: home.deletedGroup.count, duration: DeletedGroupToast.duration)
        // Its own, long enough to read its two sentences
        .glassToast(
            "We saw you on your phone. Your groups are already there, and the invite to download the app won't show again.",
            trigger: home.usedOnPhoneCount,
            duration: .seconds(6),
        )
        // Shown once the first sync issued a code, or from Home's "See code"; the sheet settles the prompt itself
        // ("I've saved it", "Remind me later" or a swipe down), which hides it here too.
        .onChange(of: home.state.showsRecoveryCodePrompt, initial: true) { _, isDue in
            showsRecoveryCode = isDue
        }
        .sheet(isPresented: $showsRecoveryCode) {
            RecoveryCodeSheet()
        }
        .onChange(of: balancesRoute, initial: true) { _, route in
            if let route { lastBalancesRoute = route }
        }
    }

    /// Search, the last tab of an iPhone's bar and the first of the Mac's sidebar.
    private var searchTab: some TabContent<MainTab> {
        #if os(macOS)
        // A plain tab, which the sidebar keeps where it is declared; the search role would move it under the others
        Tab("Search", systemImage: "magnifyingglass", value: MainTab.search) {
            GroupsColumns(model: model, path: stackPath(isSearch: true), isInFront: isInFront(isSearch: true)) {
                GroupsSearchView(model: model)
            }
            .environment(\.searchFocusRequest, searchFocusRequest)
            .measuringNarrowWindow()
        }
        #else
        Tab(value: MainTab.search, role: .search) {
            // An iPad lying down opens the first result beside the results
            GroupsColumns(model: model, path: stackPath(isSearch: true), isInFront: isInFront(isSearch: true)) {
                GroupsSearchView(model: model)
            }
        }
        #endif
    }

    /// Selecting a tab opens it right above Home. Leaving Search forgets it and goes back to the groups, past what its
    /// results opened; Home goes back to itself, past what any tab opened.
    /// Home's tab events work from any tab, so every tab but Groups and Search selects through them.
    private var tabSelection: Binding<MainTab> {
        Binding(
            get: { selectedTab },
            set: { tab in
                switch selectedTab {
                case .groups, .search: selectFromGroups(tab)
                default: selectFromHome(tab)
                }
            },
        )
    }

    private var selectedTab: MainTab {
        // The search lives over Groups: once something replaced Groups (Activity from a notice), that wins
        if showsSearchTab, path.first == .groups { return .search }
        switch path.first {
        case .groups: return .groups
        case .globalBalances: return .balances
        case .contacts: return .contacts
        default: return .home
        }
    }

    /// Home's tab events, which work over whatever Home or another tab pushed: Groups, Balances and Contacts take its
    /// place right above Home. Search lives in Groups, so it opens Groups first; Home goes back to itself.
    private func selectFromHome(_ tab: MainTab) {
        switch tab {
        case .groups: home.send(HomeUiEventOnGroupsTabClicked.shared)
        case .balances: home.send(HomeUiEventOnBalancesTabClicked.shared)
        case .contacts: home.send(HomeUiEventOnContactsTabClicked.shared)
        case .search:
            home.send(HomeUiEventOnGroupsTabClicked.shared)
            model.send(GroupsUiEventOnSearchOpened.shared)
        case .home: if !path.isEmpty { path = [] }
        }
    }

    /// The groups' events while Groups or Search is in front, so leaving Search forgets it.
    private func selectFromGroups(_ tab: MainTab) {
        switch tab {
        case .home: model.send(GroupsUiEventOnHomeTabClicked.shared)
        case .balances: model.send(GroupsUiEventOnBalancesTabClicked.shared)
        case .contacts: model.send(GroupsUiEventOnContactsTabClicked.shared)
        case .search: model.send(GroupsUiEventOnSearchTabClicked.shared)
        case .groups:
            if showsSearchTab {
                model.send(GroupsUiEventOnSearchTabLeft.shared)
            } else {
                model.send(GroupsUiEventOnGroupsTabClicked.shared)
            }
        }
    }

    /// What Home pushed over itself (Activity among them), while no other tab is above it.
    private var homePath: Binding<[AppRoute]> {
        Binding(
            get: { selectedTab == .home ? path : [] },
            set: { newValue in
                guard selectedTab == .home else { return }
                path = newValue
            },
        )
    }

    /// What the groups pushed over themselves, while Groups is right above Home.
    private var groupsPath: [AppRoute] {
        path.first == .groups ? Array(path.dropFirst()) : []
    }

    /// Balances right above Home, which its tab shows; nil while another tab is in front.
    private var balancesRoute: RouteGlobalBalances? {
        if case let .globalBalances(route) = path.first { return route }
        return nil
    }

    /// The Balances its tab draws: the one right above Home, or else the last one shown, kept while another tab is in
    /// front. Its ViewModel is the route's, so a route with another filter draws a new one.
    private var shownBalancesRoute: RouteGlobalBalances? {
        balancesRoute ?? lastBalancesRoute
    }

    /// Contacts right above Home, which its tab shows.
    private var isContactsShown: Bool {
        if case .contacts = path.first { return true }
        return false
    }

    /// What Balances pushed over itself (the profile), shown in its own tab's stack.
    private var balancesPath: Binding<[AppRoute]> {
        tabStack(isShown: balancesRoute != nil)
    }

    /// The entries above the tab right above Home, while that tab is shown; a back gesture shortens them.
    private func tabStack(isShown: Bool) -> Binding<[AppRoute]> {
        Binding(
            get: { isShown ? Array(path.dropFirst()) : [] },
            set: { newValue in
                guard isShown, let first = path.first else { return }
                path = [first] + newValue
            },
        )
    }

    /// The search tab is selected. Read from the ViewModel at once, so a tap on the tab doesn't bounce back to Groups
    /// until the watcher catches up.
    private var showsSearchTab: Bool {
        model.current.isSearching
    }

    /// The groups' stack goes to the tab that pushed it: Search while it is open, else Groups. The other tabs' stacks
    /// stay at their root, so a screen never shows in two.
    private func stackPath(isSearch: Bool) -> Binding<[AppRoute]> {
        tabStack(isShown: isInFront(isSearch: isSearch))
    }

    /// Search while it is open, Groups while Groups is right above Home and Search isn't: the tab whose stack holds
    /// what the groups opened.
    private func isInFront(isSearch: Bool) -> Bool {
        path.first == .groups && showsSearchTab == isSearch
    }

    #if os(macOS)
    /// What ⌘K runs, nil while a sheet covers the window, the recovery code's prompt included (never searched over, as
    /// `HomeUiEvent.OnSearchClicked` refuses on the other platforms).
    private var searchCommand: (() -> Void)? {
        guard isWindowUncovered, !showsRecoveryCode else { return nil }
        return openSearch
    }

    /// ⌘K: the search tab, as a click on it in the sidebar, and its field focused; already open, back to its results
    /// from what a result pushed over them, as a click on the tab does (`selectFromGroups`).
    private func openSearch() {
        // An alert or a sheet a screen presents itself, which `isWindowUncovered` can't see
        guard !NSApplication.hasSheetOpen else { return }
        searchFocusRequest += 1
        tabSelection.wrappedValue = .search
    }

    /// "Open" shows while Groups or a group's screen is in front, whichever tab opened the group.
    private var showsOpenGroups: Bool {
        path.last == .groups || selectedGroupId != nil
    }

    /// The group whose screen is in front, which "Open" marks.
    private var selectedGroupId: String? {
        switch path.last {
        case let .groupDetail(route): route.groupId
        case let .groupStats(route): route.groupId
        case let .categoryExpenses(route): route.groupId
        default: nil
        }
    }
    #endif

    /// What the tabs push over themselves: Activity, a group's expenses, balances, members or statistics (and a
    /// category's bills they open), how
    /// simplifying debts works, a bill, an invite
    /// opened from its link, a group's invite with an iPad's room, the profile, its editor, its category map and, on
    /// iPhone, a key.
    /// Contacts also pushes a person's profile (which Balances pushes too), the agenda, and Balances opened from a
    /// profile's Request.
    @ViewBuilder
    static func destination(_ route: AppRoute) -> some View {
        switch route {
        case .editProfile: EditProfileView()
        case .notificationSettings: NotificationSettingsView()
        // The profile's "Category map"; the Mac opens it in Ajustes instead (`AppModel`)
        case .categoryMap: CategoryMapView()
        // The profile's "Privacy Policy" and "Terms of Service"; a sheet on the Mac's Ajustes instead
        case let .legalDocument(route): LegalDocumentView(route: route)
        case let .paymentKey(route): PaymentKeyView(route: route)
        // A new identity per route: a group swapped in place (the Mac's sidebar) must not keep the model of the last one
        case let .groupDetail(route): GroupDetailView(route: route).id(route)
        case let .billDetail(route): BillDetailView(route: route)
        case let .groupStats(route): GroupStatsView(route: route)
        case let .categoryExpenses(route): CategoryExpensesView(route: route)
        // "How it works" of Simplify debts, from the group's Balances; the Mac opens it as a sheet
        case let .simplifyDebtsInfo(route): SimplifyDebtsInfoView(route: route)
        case let .joinInvite(route): JoinInviteView(route: route)
        case let .joinPick(route): JoinPickView(route: route)
        case let .invite(route): InviteView(route: route)
        case let .contactProfile(route): ContactProfileView(route: route)
        case .contactsAgenda: AgendaView()
        case let .globalBalances(route): GlobalBalancesView(route: route).id(route)
        // Home's bell, or a contact's "See all", pushes Activity
        case let .activity(route): ActivityView(route: route).id(route)
        // The search over Home, Balances or Contacts; every platform here has the search tab instead
        case .search: SearchView()
        default:
            #if os(macOS)
            // The Mac's profile is Ajustes (⌘,), never pushed
            EmptyView()
            #else
            ProfileView()
            #endif
        }
    }
}

#if os(iOS)
private extension View {
    func searchTabApart() -> some View {
        modifier(SearchTabApart())
    }
}

/// The search tab as a round button of its own at the end of the bottom tab bar, on every tab (Pierre's call). iOS 26
/// draws it so already; iOS 27 keeps it inside the tabs' capsule unless selecting it activates the search, so there the
/// field takes the keyboard as the tab opens and its ✕ goes back to the tab it was opened from (lying down, the search
/// draws its own field beside the results, and another tab leaves it). The bar is at the bottom wherever either size
/// class is compact: an iPhone either way up and an iPad's narrow window; an iPad's top bar keeps its search tab.
private struct SearchTabApart: ViewModifier {
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.verticalSizeClass) private var verticalSizeClass

    func body(content: Content) -> some View {
        if #available(iOS 27, *), horizontalSizeClass == .compact || verticalSizeClass == .compact {
            content.tabViewSearchActivation(.searchTabSelection)
        } else {
            content
        }
    }
}
#endif

#if os(macOS)
extension FocusedValues {
    /// Opens the main window's search tab (⌘K).
    @Entry var openSearch: (() -> Void)?
}

/// Edit › Search (⌘K), as the web's "Search… ⌘K": the search tab atop the sidebar, its field focused.
struct SearchCommands: Commands {
    /// Profile › Language, whose change draws the menu again: the menu bar has no `locale` of the windows to follow.
    let language: AppLanguage
    @FocusedValue(\.openSearch) private var openSearch

    var body: some Commands {
        CommandGroup(after: .textEditing) {
            Button { openSearch?() } label: { Text("Search", bundle: .appLanguage) }
                .keyboardShortcut("k", modifiers: .command)
                .disabled(openSearch == nil)
        }
    }
}
#endif
