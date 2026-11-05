import Shared
import SwiftUI

/// The tabs of the canvas: Home first, then Groups, Balances and Contacts. Search, once there is a group, is the search
/// tab at the end of the bar, the room Activity left when it became Home's bell (Pierre's call); the Mac searches from
/// Groups' toolbar instead.
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
    #if os(macOS)
    @State private var sidebar = HomeSidebarModel()
    #endif
    init(path: Binding<[AppRoute]>) {
        _path = path
    }

    var body: some View {
        TabView(selection: tabSelection) {
            Tab(value: MainTab.home) {
                NavigationStack(path: homePath) {
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
            // The shared back stack holds Balances right above Home, where its tab shows
            Tab("Balances", systemImage: "dollarsign.circle", value: MainTab.balances) {
                NavigationStack(path: balancesPath) {
                    if let route = balancesRoute {
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
            // Contacts sits right above Home too, and keeps what it opens in its own tab's stack
            Tab("Contacts", systemImage: "person.crop.circle", value: MainTab.contacts) {
                if isContactsShown {
                    ContactsColumns(path: tabStack(isShown: isContactsShown))
                        #if os(macOS)
                        .measuringNarrowWindow()
                        #endif
                } else {
                    Color.clear
                }
            }
            #if os(iOS)
            // Nothing to look in before the first group
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
        #if os(iOS)
        .searchTabApart()
        #endif
        #if os(macOS)
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

    /// The search tab is selected; the Mac has none and searches in the groups' toolbar. Read
    /// from the ViewModel at once, so a tap on the tab doesn't bounce back to Groups until the watcher catches up.
    private var showsSearchTab: Bool {
        #if os(iOS)
        hasSearchTab && model.current.isSearching
        #else
        false
        #endif
    }

    /// Four tabs leave room for the search tab on an iPhone too, as Apple's tab bar sets it apart at its end.
    private var hasSearchTab: Bool {
        #if os(iOS)
        model.state.canSearch
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

    #if os(macOS)
    /// "Open" shows while Groups or a group's screen is in front, whichever tab opened the group.
    private var showsOpenGroups: Bool {
        path.last == .groups || selectedGroupId != nil
    }

    /// The group whose screen is in front, which "Open" marks.
    private var selectedGroupId: String? {
        switch path.last {
        case let .groupDetail(route): route.groupId
        case let .groupStats(route): route.groupId
        default: nil
        }
    }
    #endif

    /// What the tabs push over themselves: Activity, a group's expenses, balances, members or statistics, how
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
        case let .paymentKey(route): PaymentKeyView(route: route)
        case let .groupDetail(route): GroupDetailView(route: route)
        case let .billDetail(route): BillDetailView(route: route)
        case let .groupStats(route): GroupStatsView(route: route)
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
        // Search over Home, Balances or Contacts on the Mac, as a sheet; an iPhone and an iPad have the search tab
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
