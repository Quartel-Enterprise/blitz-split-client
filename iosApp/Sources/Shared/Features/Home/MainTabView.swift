import Shared
import SwiftUI

/// The tabs of the canvas: Home first, then Groups and Balances; Contacts and Activity answer "Coming soon". Search is
/// the iPhone's and the iPad's search tab, once there is a group, and looks in Groups; the Mac searches from Groups'
/// toolbar instead.
enum MainTab: Hashable {
    case home
    case groups
    case balances
    case contacts
    case activity
    case search
}

/// iPhone: the floating Liquid Glass tab bar. iPad and Mac: the glass sidebar (sidebarAdaptable). Home is the root of
/// the shared back stack and every other tab the entry right above it, so the first entry of `path` says which tab is
/// in front: Groups, Balances, or else Home, whose own stack is then the whole path.
struct MainTabView: View {
    /// The shared back stack above Home: Groups or Balances with what they opened, or what Home opened (a group, the
    /// profile, an invite opened from its link).
    @Binding var path: [AppRoute]

    @State private var home = HomeModel()
    @State private var model = GroupsModel()
    @State private var showsRecoveryCode = false

    var body: some View {
        TabView(selection: tabSelection) {
            Tab(value: MainTab.home) {
                NavigationStack(path: homePath) {
                    HomeView(model: home)
                        .navigationDestination(for: AppRoute.self) { Self.destination($0) }
                }
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
                #endif
            }
            // The shared back stack holds Balances right above Home, where its tab shows
            Tab("Balances", systemImage: "creditcard", value: MainTab.balances) {
                NavigationStack(path: balancesPath) {
                    if let route = balancesRoute {
                        GlobalBalancesView(route: route)
                            .id(route)
                            .navigationDestination(for: AppRoute.self) { Self.destination($0) }
                    } else {
                        Color.clear
                    }
                }
            }
            Tab("Contacts", systemImage: "person.crop.circle", value: MainTab.contacts) { Color.clear }
            Tab("Activity", systemImage: "list.bullet.rectangle", value: MainTab.activity) { Color.clear }
            #if os(iOS)
            // Nothing to look in before the first group
            if model.state.canSearch {
                Tab(value: MainTab.search, role: .search) {
                    // An iPad lying down opens the first result beside the results
                    GroupsColumns(model: model, path: stackPath(isSearch: true), isInFront: isInFront(isSearch: true)) {
                        GroupsSearchView(model: model)
                    }
                }
            }
            #endif
        }
        .tabViewStyle(.sidebarAdaptable)
        .glassToast(
            [
                GlassToastMessage(text: "Coming soon", trigger: model.comingSoonCount),
                GlassToastMessage(text: "Coming soon", trigger: home.comingSoonCount),
                GlassToastMessage(text: "Couldn't save the change. Try again.", trigger: home.changeErrorCount),
            ]
                + GroupExitCopy.errorToasts(
                    leave: model.leaveErrorCount,
                    delete: model.deleteErrorCount,
                    change: model.changeErrorCount,
                ),
        )
        // Their own toasts, for their longer duration; Groups' and Home's each wait until their screen is on top
        .glassToast(model.deletedGroup.text, trigger: model.deletedGroup.count, duration: DeletedGroupToast.duration)
        .glassToast(home.deletedGroup.text, trigger: home.deletedGroup.count, duration: DeletedGroupToast.duration)
        // Shown once the first sync issued a code, or from Home's "See code"; the sheet settles the prompt itself
        // ("I've saved it", "Remind me later" or a swipe down), which hides it here too.
        .onChange(of: home.state.showsRecoveryCodePrompt, initial: true) { _, isDue in
            showsRecoveryCode = isDue
        }
        .sheet(isPresented: $showsRecoveryCode) {
            RecoveryCodeSheet()
        }
    }

    /// Selecting a tab that is not built yet keeps the tab shown and says "Coming soon". Leaving Search forgets it
    /// and goes back to the groups, past what its results opened; Home goes back to itself, past what any tab opened.
    private var tabSelection: Binding<MainTab> {
        Binding(
            get: { selectedTab },
            set: { tab in
                switch selectedTab {
                case .home: selectFromHome(tab)
                default: selectFromGroups(tab)
                }
            },
        )
    }

    private var selectedTab: MainTab {
        if showsSearchTab { return .search }
        switch path.first {
        case .groups: return .groups
        case .globalBalances: return .balances
        default: return .home
        }
    }

    /// Home's tab events, which work over whatever Home pushed (a group, the profile): Groups and Balances take its
    /// place right above Home, while Contacts and Activity only say "Coming soon" and leave it open. Search lives in
    /// Groups, so it opens Groups first.
    private func selectFromHome(_ tab: MainTab) {
        switch tab {
        case .groups: home.send(HomeUiEventOnGroupsTabClicked.shared)
        case .balances: home.send(HomeUiEventOnBalancesTabClicked.shared)
        case .contacts: home.send(HomeUiEventOnContactsTabClicked.shared)
        case .activity: home.send(HomeUiEventOnActivityTabClicked.shared)
        case .search:
            home.send(HomeUiEventOnGroupsTabClicked.shared)
            model.send(GroupsUiEventOnSearchOpened.shared)
        case .home: break
        }
    }

    /// The groups' events, which also move the stack from Balances right above Home.
    private func selectFromGroups(_ tab: MainTab) {
        switch tab {
        case .home: model.send(GroupsUiEventOnHomeTabClicked.shared)
        case .balances: model.send(GroupsUiEventOnBalancesTabClicked.shared)
        case .contacts: model.send(GroupsUiEventOnContactsTabClicked.shared)
        case .activity: model.send(GroupsUiEventOnActivityTabClicked.shared)
        case .search: model.send(GroupsUiEventOnSearchOpened.shared)
        case .groups:
            if showsSearchTab {
                model.send(GroupsUiEventOnSearchTabLeft.shared)
            } else if balancesRoute != nil {
                model.send(GroupsUiEventOnGroupsTabClicked.shared)
            }
        }
    }

    /// What Home pushed over itself, while neither Groups nor Balances is above it.
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

    /// The search tab is selected: only the iPhone and the iPad have one, the Mac searches in the groups' toolbar. Read
    /// from the ViewModel at once, so a tap on the tab doesn't bounce back to Groups until the watcher catches up.
    private var showsSearchTab: Bool {
        #if os(iOS)
        model.current.isSearching
        #else
        false
        #endif
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

    /// What the tabs push over themselves: a group's expenses, balances, members or statistics, a bill, an invite
    /// opened from its link, the profile, its editor and, on iPhone, a key.
    @ViewBuilder
    static func destination(_ route: AppRoute) -> some View {
        switch route {
        case .editProfile: EditProfileView()
        case .notificationSettings: NotificationSettingsView()
        case let .paymentKey(route): PaymentKeyView(route: route)
        case let .members(route): MembersView(route: route)
        case let .groupDetail(route): GroupDetailView(route: route)
        case let .balances(route): BalancesView(route: route)
        case let .billDetail(route): BillDetailView(route: route)
        case let .groupStats(route): GroupStatsView(route: route)
        case let .joinInvite(route): JoinInviteView(route: route)
        case let .joinPick(route): JoinPickView(route: route)
        default: ProfileView()
        }
    }
}
