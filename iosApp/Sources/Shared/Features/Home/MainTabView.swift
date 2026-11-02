import Shared
import SwiftUI

/// The tabs of the canvas, all built: Home first, then Groups, Balances, Contacts and Activity. Search, once there is a
/// group, is a search tab with an iPad's room (`hasSearchTabRoom`) and opens in Groups' place from its magnifier on an
/// iPhone; the Mac searches from Groups' toolbar instead.
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
/// in front: Groups, Balances, Contacts, Activity, or else Home, whose own stack is then the whole path.
struct MainTabView: View {
    /// The shared back stack above Home: Groups, Balances, Contacts or Activity with what they opened, or what Home
    /// opened (a group, the profile, an invite opened from its link).
    @Binding var path: [AppRoute]

    @State private var home = HomeModel()
    @State private var model = GroupsModel()
    @State private var showsRecoveryCode = false
    #if os(iOS)
    @Environment(\.hasSearchTabRoom) private var hasSearchTabRoom
    #endif

    init(path: Binding<[AppRoute]>) {
        _path = path
        #if os(iOS)
        Self.tintsBadges()
        #endif
    }

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
                    #if os(iOS)
                    // A compact tab bar has no search tab: the search takes the groups' place
                    if showsSearchInGroups {
                        GroupsSearchView(model: model)
                    } else {
                        GroupsView(model: model)
                    }
                    #else
                    GroupsView(model: model)
                    #endif
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
            // Contacts sits right above Home too, and keeps what it opens in its own tab's stack
            Tab("Contacts", systemImage: "person.crop.circle", value: MainTab.contacts) {
                if isContactsShown {
                    ContactsColumns(path: tabStack(isShown: isContactsShown))
                } else {
                    Color.clear
                }
            }
            // Activity sits right above Home as Balances does; the badge counts every unread item, in the accent
            Tab("Activity", systemImage: "list.bullet.rectangle", value: MainTab.activity) {
                NavigationStack(path: tabStack(isShown: activityRoute != nil)) {
                    if let route = activityRoute {
                        ActivityView(route: route)
                            .id(route)
                            .navigationDestination(for: AppRoute.self) { Self.destination($0) }
                    } else {
                        Color.clear
                    }
                }
            }
            .badge(Int(home.state.activityUnreadCount))
            #if os(iOS)
            // Nothing to look in before the first group; a compact tab bar opens it from Groups' magnifier instead
            if hasSearchTab {
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
        if showsSearchTab { return .search }
        switch path.first {
        case .groups: return .groups
        case .globalBalances: return .balances
        case .contacts: return .contacts
        case .activity: return .activity
        default: return .home
        }
    }

    /// Home's tab events, which work over whatever Home or another tab pushed: Groups, Balances, Contacts and Activity
    /// take its place right above Home. Search lives in Groups, so it opens Groups first; Home goes back to itself.
    private func selectFromHome(_ tab: MainTab) {
        switch tab {
        case .groups: home.send(HomeUiEventOnGroupsTabClicked.shared)
        case .balances: home.send(HomeUiEventOnBalancesTabClicked.shared)
        case .contacts: home.send(HomeUiEventOnContactsTabClicked.shared)
        case .activity: home.send(HomeUiEventOnActivityTabClicked.shared)
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
        case .activity: model.send(GroupsUiEventOnActivityTabClicked.shared)
        case .search: model.send(GroupsUiEventOnSearchOpened.shared)
        case .groups:
            if showsSearchTab || showsSearchInGroups {
                model.send(GroupsUiEventOnSearchTabLeft.shared)
            } else {
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

    /// Activity right above Home, which its tab shows; nil while another tab is in front.
    private var activityRoute: RouteActivity? {
        if case let .activity(route) = path.first { return route }
        return nil
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

    /// The search tab is selected: only an iPad's room has one, the Mac searches in the groups' toolbar. Read
    /// from the ViewModel at once, so a tap on the tab doesn't bounce back to Groups until the watcher catches up.
    private var showsSearchTab: Bool {
        #if os(iOS)
        hasSearchTab && model.current.isSearching
        #else
        false
        #endif
    }

    /// The five tabs stay, and with no room for a sixth (`hasSearchTabRoom`) the search opens from Groups' magnifier
    /// in the groups' place; an iPad's room keeps the tab.
    private var hasSearchTab: Bool {
        #if os(iOS)
        hasSearchTabRoom && model.state.canSearch
        #else
        false
        #endif
    }

    /// The search open in Groups' place, where the tab bar is compact. Read from the state, which the tab view
    /// observes: nothing else redraws it as the search opens.
    private var showsSearchInGroups: Bool {
        #if os(iOS)
        !hasSearchTabRoom && model.state.isSearching
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

    #if os(iOS)
    /// The tab bar's badges in the accent, never the system's red: Activity's unread count is news, not an error.
    private static func tintsBadges() {
        let appearance = UITabBarItem.appearance()
        appearance.badgeColor = UIColor(named: "AccentColor")
        appearance.setBadgeTextAttributes([.foregroundColor: UIColor(Palette.onAccent)], for: .normal)
    }
    #endif

    /// What the tabs push over themselves: a group's expenses, balances, members or statistics, a bill, an invite
    /// opened from its link, a group's invite with an iPad's room, the profile, its editor, its category map and, on
    /// iPhone, a key.
    /// Contacts also pushes a person's profile, the agenda, and Balances opened from a profile's Request.
    @ViewBuilder
    static func destination(_ route: AppRoute) -> some View {
        switch route {
        case .editProfile: EditProfileView()
        case .notificationSettings: NotificationSettingsView()
        // The profile's "Category map"; the Mac opens it in Ajustes instead (`AppModel`)
        case .categoryMap: CategoryMapView()
        case let .paymentKey(route): PaymentKeyView(route: route)
        case let .groupDetail(route): GroupDetailView(route: route)
        case let .billDetail(route): BillDetailView(route: route)
        case let .groupStats(route): GroupStatsView(route: route)
        case let .joinInvite(route): JoinInviteView(route: route)
        case let .joinPick(route): JoinPickView(route: route)
        case let .invite(route): InviteView(route: route)
        case let .contactProfile(route): ContactProfileView(route: route)
        case .contactsAgenda: AgendaView()
        case let .globalBalances(route): GlobalBalancesView(route: route).id(route)
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
extension EnvironmentValues {
    /// Only an iPad's room, both size classes regular, has a search tab. A compact tab bar holds five items and
    /// puts the rest under "More", and a Plus or Max iPhone lying down, regular in width, still draws that bar, so
    /// the width alone would flip the search between a tab and Groups' magnifier as it turns.
    var hasSearchTabRoom: Bool {
        horizontalSizeClass == .regular && verticalSizeClass == .regular
    }
}
#endif
