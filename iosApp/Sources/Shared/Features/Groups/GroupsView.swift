import Observation
import Shared
import SwiftUI

@Observable
final class GroupsModel {
    private(set) var state: GroupsUiState
    private(set) var comingSoonCount = 0
    private(set) var leaveErrorCount = 0
    private(set) var deleteErrorCount = 0
    private(set) var changeErrorCount = 0
    private(set) var deletedGroupCount = 0
    /// "Carla deleted the group “Ana's birthday”.", kept while the toast shows.
    private(set) var deletedGroupText: LocalizedStringKey = ""
    /// The toast's own duration on the groups.
    static let deletedGroupToastDuration: Duration = .seconds(4)

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createGroupsViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! GroupsUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
            if !state.isRefreshing { self?.endRefreshes() }
            // State, not an action, so it waits until the home is on top; the next deleted group follows
            if let notice = state.deletedGroupNotice, notice.groupId != self?.shownDeletedGroupId {
                self?.showDeletedGroup(notice)
            }
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            switch action {
            case is GroupsUiActionShowComingSoon: self?.comingSoonCount += 1
            case is GroupsUiActionShowLeaveError: self?.leaveErrorCount += 1
            case is GroupsUiActionShowDeleteError: self?.deleteErrorCount += 1
            case is GroupsUiActionShowChangeError: self?.changeErrorCount += 1
            default: break
            }
        })
    }

    /// The notice on screen, so a state update meanwhile doesn't show it twice.
    @ObservationIgnored private var shownDeletedGroupId: String?

    private func showDeletedGroup(_ notice: DeletedGroupNotice) {
        shownDeletedGroupId = notice.groupId
        if let name = notice.deletedByName {
            deletedGroupText = "\(name) deleted the group “\(notice.groupName)”."
        } else {
            deletedGroupText = "The group “\(notice.groupName)” was deleted."
        }
        deletedGroupCount += 1
        // Shown once the toast is gone, as Android's snackbar does, so the next deleted group doesn't replace it
        Task { [weak self] in
            try? await Task.sleep(for: Self.deletedGroupToastDuration)
            self?.send(GroupsUiEventOnDeletedGroupNoticeShown.shared)
        }
    }

    /// The state as the ViewModel holds it right now, before the watcher brings it to `state`: the search field reads
    /// its text back at once, so the caret never jumps.
    var current: GroupsUiState {
        (holder.viewModel.uiState.value as? GroupsUiState) ?? state
    }

    func send(_ event: any GroupsUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    /// The pulls waiting for the sync they asked for, resumed by the state that says it ended.
    @ObservationIgnored private var pendingRefreshes: [CheckedContinuation<Void, Never>] = []

    /// Pulled down: asks for a sync and returns once the shared code says it ended (at once offline).
    func refresh() async {
        send(GroupsUiEventOnRefresh.shared)
        // The event marks the sync as started before it returns, and starts none offline
        guard (holder.viewModel.uiState.value as! GroupsUiState).isRefreshing else { return }
        await withCheckedContinuation { continuation in
            pendingRefreshes.append(continuation)
        }
    }

    private func endRefreshes() {
        let ended = pendingRefreshes
        pendingRefreshes = []
        ended.forEach { $0.resume() }
    }

    isolated deinit {
        endRefreshes()
        holder.clear()
    }
}

/// The tabs of the canvas. Groups and Balances are built; the others answer "Coming soon". Search is the iPhone's and
/// the iPad's search tab, once there is a group; the Mac searches from its toolbar instead.
enum MainTab: Hashable {
    case groups
    case balances
    case contacts
    case activity
    case search
}

/// iPhone: the floating Liquid Glass tab bar. iPad and Mac: the glass sidebar (sidebarAdaptable).
struct MainTabView: View {
    /// The shared back stack above the groups: a group's expenses, balances or members, an invite opened from its
    /// link, the profile, its editor and, on iPhone, a key.
    @Binding var path: [AppRoute]

    @State private var model = GroupsModel()
    @State private var showsRecoveryCode = false

    var body: some View {
        TabView(selection: tabSelection) {
            Tab("Groups", systemImage: "person.3", value: MainTab.groups) {
                // An iPad lying down draws the group beside the list
                GroupsColumns(model: model, path: stackPath(isSearch: false), isInFront: isInFront(isSearch: false)) {
                    GroupsView(model: model)
                }
                #if os(macOS)
                // What the View menu's Grid Size reads and changes, only while the groups are in front
                .focusedSceneValue(\.groupsModel, path.isEmpty ? model : nil)
                #endif
            }
            // The shared back stack holds Balances over the groups, so its tab shows while it is right above them
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
            [GlassToastMessage(text: "Coming soon", trigger: model.comingSoonCount)]
                + GroupExitCopy.errorToasts(
                    leave: model.leaveErrorCount,
                    delete: model.deleteErrorCount,
                    change: model.changeErrorCount,
                ),
        )
        // Its own toast, for its longer duration; the two seldom show at once
        .glassToast(
            model.deletedGroupText,
            trigger: model.deletedGroupCount,
            duration: GroupsModel.deletedGroupToastDuration,
        )
        // Shown once the first sync issued a code; the sheet settles the prompt itself ("I've saved it",
        // "Remind me later" or a swipe down), which hides it here too.
        .onChange(of: model.state.showsRecoveryCodePrompt, initial: true) { _, isDue in
            showsRecoveryCode = isDue
        }
        .sheet(isPresented: $showsRecoveryCode) {
            RecoveryCodeSheet()
        }
    }

    // Selecting a tab that is not built yet keeps the tab shown and says "Coming soon". Leaving Search forgets it
    // and goes back to the groups, past what its results opened; leaving Balances goes back to the groups too.
    private var tabSelection: Binding<MainTab> {
        Binding(
            get: { showsSearchTab ? .search : (balancesRoute != nil ? .balances : .groups) },
            set: { tab in
                switch tab {
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
            },
        )
    }

    /// Balances right above the groups, which its tab shows; nil while the Groups tab is in front.
    private var balancesRoute: RouteGlobalBalances? {
        if case let .globalBalances(route) = path.first { return route }
        return nil
    }

    /// What Balances pushed over itself (the profile), shown in its own tab's stack.
    private var balancesPath: Binding<[AppRoute]> {
        Binding(
            get: { balancesRoute != nil ? Array(path.dropFirst()) : [] },
            set: { newValue in
                guard balancesRoute != nil, let first = path.first else { return }
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

    /// The shared back stack above the groups goes to the tab that pushed it: Search while it is open, Balances while
    /// it is right above the groups, else Groups. The other tabs' stacks stay at their root, so a screen never shows in
    /// two.
    private func stackPath(isSearch: Bool) -> Binding<[AppRoute]> {
        Binding(
            get: { isInFront(isSearch: isSearch) ? path : [] },
            set: { newValue in
                guard isInFront(isSearch: isSearch) else { return }
                path = newValue
            },
        )
    }

    /// Search while it is open, Groups while neither Search nor Balances is: the tab whose stack holds the path.
    private func isInFront(isSearch: Bool) -> Bool {
        balancesRoute == nil && showsSearchTab == isSearch
    }

    /// What the groups and the search push over themselves: a group's expenses, balances, members or statistics, a
    /// bill, an invite opened from its link, a group's invite with an iPad's room, the profile, its editor and, on
    /// iPhone, a key.
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
        case let .invite(route): InviteView(route: route)
        default: ProfileView()
        }
    }
}

struct GroupsView: View {
    let model: GroupsModel

    var body: some View {
        let state = model.state
        Group {
            if !state.groups.isEmpty {
                #if os(iOS)
                GroupsList(model: model)
                #else
                // The toolbar's field: the results take the groups' place once something is typed
                if state.isSearching, state.search.hasQuery {
                    VStack(spacing: 0) {
                        Picker("Search in", selection: GroupsSearchCopy.scopeBinding(model)) {
                            ForEach(GroupsSearchCopy.scopes, id: \.self) { scope in
                                Text(GroupsSearchCopy.scopeTitle(scope)).tag(scope)
                            }
                        }
                        .pickerStyle(.segmented)
                        .labelsHidden()
                        .fixedSize()
                        .padding(.vertical, 8)
                        GroupsSearchResultsList(model: model)
                    }
                } else {
                    GroupsTable(model: model)
                }
                #endif
            } else if state.showsEmptyState {
                emptyState
                    #if os(iOS)
                    // The empty home has no list to hold the sync status, which only shows when it asks for attention
                    .safeAreaInset(edge: .top) {
                        if let status = GroupsCopy.sync(state.syncBadge, pendingChangeCount: state.pendingChangeCount) {
                            Text(verbatim: status)
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                                .padding(.top, 8)
                        }
                    }
                    #endif
            } else {
                // Nothing until the groups are read, so the empty state never flashes before the list
                Color.clear
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.groupedBackground)
        // What Leave and Delete open from a row, the same alerts as inside the group (a sheet on the Mac's window)
        .groupExitDialogs(
            exit: state.exit,
            currency: state.exitGroup?.currency,
            onConfirm: { model.send(GroupsUiEventOnGroupExitConfirmed.shared) },
            onDismiss: { model.send(GroupsUiEventOnGroupExitDismissed.shared) },
        )
        // Registrar pagamento, which a row's Settle up opened over the home, just closed
        .recordedPaymentNotice(
            state.recordedPayment,
            onUndo: { model.send(GroupsUiEventOnUndoPaymentClicked.shared) },
            onShown: { model.send(GroupsUiEventOnRecordedPaymentNoticeShown.shared) },
        )
        .navigationTitle("Groups")
        // Why: on iPhone and iPad the selected Groups tab already names the screen, so the bar shows no title. The
        // title still names the screen in the back stack, and VoiceOver still reads it as the header.
        #if os(iOS)
        .toolbarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                Color.clear
                    .frame(width: 1, height: 1)
                    .accessibilityElement()
                    .accessibilityLabel(Text("Groups"))
                    .accessibilityAddTraits(.isHeader)
            }
            .sharedBackgroundVisibility(.hidden)
            ToolbarItemGroup(placement: .primaryAction) {
                joinButton
                profileButton
            }
            ToolbarSpacer(.fixed, placement: .primaryAction)
            ToolbarItem(placement: .primaryAction) {
                Button("Split a bill", systemImage: "plus") {
                    model.send(GroupsUiEventOnSplitBillClicked.shared)
                }
                .buttonStyle(.glassProminent)
            }
        }
        #else
        .navigationSubtitle(macSubtitle)
        // Nothing to look in before the first group
        .toolbarSearch(macSearchBinding, isShown: !state.groups.isEmpty)
        .toolbar {
            if !state.groups.isEmpty {
                ToolbarItem(placement: .principal) {
                    Picker("Status", selection: settledBinding) {
                        Text("Open").tag(false)
                        Text("Settled up").tag(true)
                    }
                    .pickerStyle(.segmented)
                }
                // List or Grid, as in the Finder; the size is the slider under the grid and View › Grid Size. The
                // search's results stay a list
                if !state.isSearching {
                    ToolbarItem(placement: .principal) {
                        Picker("View", selection: displayBinding) {
                            Label("List", systemImage: GroupsDisplayCopy.displayIcon(.list)).tag(GroupsDisplay.list)
                            Label("Grid", systemImage: GroupsDisplayCopy.displayIcon(.grid)).tag(GroupsDisplay.grid)
                        }
                        .pickerStyle(.segmented)
                        .labelStyle(.iconOnly)
                    }
                }
            }
            if state.canRetrySync {
                ToolbarItem(placement: .primaryAction) {
                    Button("Try again", systemImage: "arrow.clockwise") {
                        model.send(GroupsUiEventOnRetrySyncClicked.shared)
                    }
                }
            }
            ToolbarItemGroup(placement: .primaryAction) {
                joinButton
                Button("New group", systemImage: "plus.square.on.square") {
                    model.send(GroupsUiEventOnCreateGroupClicked.shared)
                }
                profileButton
            }
            ToolbarItem(placement: .primaryAction) {
                Button {
                    model.send(GroupsUiEventOnSplitBillClicked.shared)
                } label: {
                    // A toolbar draws a Label as its icon only: the canvas spells "Split a bill" out
                    HStack(spacing: 6) {
                        Image(systemName: "plus")
                        Text("Split a bill")
                    }
                    .fontWeight(.semibold)
                }
                .buttonStyle(.borderedProminent)
            }
        }
        #endif
    }

    private var joinButton: some View {
        Button("Join with a link or QR code", systemImage: "qrcode") {
            model.send(GroupsUiEventOnJoinWithLinkClicked.shared)
        }
    }

    private var profileButton: some View {
        Button {
            model.send(GroupsUiEventOnProfileClicked.shared)
        } label: {
            AvatarPhoto(
                initial: model.state.avatarInitial,
                color: model.state.avatarColor,
                photoUrl: model.state.photoUrl,
                size: 32,
                font: .subheadline.weight(.semibold),
            )
        }
        .accessibilityLabel("Profile")
    }

    #if os(macOS)
    /// List or Grid, the toolbar's other segments.
    private var displayBinding: Binding<GroupsDisplay> {
        Binding(
            get: { model.state.layout.display },
            set: { model.send(GroupsUiEventOnDisplayChanged(display: $0)) },
        )
    }

    /// Open or Settled up, the toolbar's segments.
    private var settledBinding: Binding<Bool> {
        Binding(
            get: { model.state.showsSettledGroups },
            set: { model.send(GroupsUiEventOnShowSettledChanged(isShown: $0)) },
        )
    }

    /// The toolbar's field: typing opens the search, clearing it closes it. Only with the groups on top, where the
    /// field shows, so closing it never leaves a screen.
    private var macSearchBinding: Binding<String> {
        Binding(
            get: { model.current.search.query },
            set: { query in
                if query.isEmpty {
                    if model.current.search.isOpen { model.send(GroupsUiEventOnSearchClosed.shared) }
                    return
                }
                if !model.current.search.isOpen { model.send(GroupsUiEventOnSearchOpened.shared) }
                model.send(GroupsUiEventOnSearchQueryChanged(query: query))
            },
        )
    }

    /// "Offline · 1 change queued · 4 open", "4 open", "1 settled up", or "Searching groups, people and expenses".
    private var macSubtitle: Text {
        let state = model.state
        if state.isSearching, state.search.hasQuery {
            return Text("Searching groups, people and expenses")
        }
        guard !state.groups.isEmpty else {
            return Text(verbatim: GroupsCopy.sync(state.syncBadge, pendingChangeCount: state.pendingChangeCount) ?? "")
        }
        let count = state.showsSettledGroups
            ? String(localized: "\(state.settledGroups.count) settled up")
            : String(localized: "\(state.openGroups.count) open")
        guard let sync = GroupsCopy.sync(state.syncBadge, pendingChangeCount: state.pendingChangeCount) else {
            return Text(verbatim: count)
        }
        return Text(verbatim: "\(sync) · \(count)")
    }
    #endif

    private var emptyState: some View {
        ContentUnavailableView {
            Label("No groups yet", systemImage: "person.3")
        } description: {
            Text("Create a group for the trip, the house or the barbecue. You can start even without internet.")
        } actions: {
            VStack(spacing: 10) {
                PrimaryButton(title: "Create group") {
                    model.send(GroupsUiEventOnCreateGroupClicked.shared)
                }
                TintedButton(title: "Join with a link or QR code") {
                    model.send(GroupsUiEventOnJoinWithLinkClicked.shared)
                }
            }
            .frame(maxWidth: 320)
        }
    }
}

#if os(macOS)
private extension View {
    /// The Mac's toolbar search field, only while there is something to look in.
    @ViewBuilder
    func toolbarSearch(_ text: Binding<String>, isShown: Bool) -> some View {
        if isShown {
            searchable(text: text, placement: .toolbar, prompt: Text("Search"))
        } else {
            self
        }
    }
}
#endif
