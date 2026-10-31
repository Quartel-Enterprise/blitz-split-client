import Observation
import Shared
import SwiftUI

@Observable
final class GroupsModel {
    private(set) var state: GroupsUiState
    private(set) var comingSoonCount = 0
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
            if action is GroupsUiActionShowComingSoon { self?.comingSoonCount += 1 }
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

/// The tabs of the canvas. Only Groups is built; the others answer "Coming soon".
enum MainTab: Hashable {
    case groups
    case balances
    case contacts
    case activity
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
                NavigationStack(path: $path) {
                    GroupsView(model: model)
                        .navigationDestination(for: AppRoute.self) { route in
                            switch route {
                            case .editProfile: EditProfileView()
                            case .notificationSettings: NotificationSettingsView()
                            case let .paymentKey(route): PaymentKeyView(route: route)
                            case let .members(route): MembersView(route: route)
                            case let .groupDetail(route): GroupDetailView(route: route)
                            case let .balances(route): BalancesView(route: route)
                            case let .billDetail(route): BillDetailView(route: route)
                            case let .joinInvite(route): JoinInviteView(route: route)
                            case let .joinPick(route): JoinPickView(route: route)
                            default: ProfileView()
                            }
                        }
                }
            }
            Tab("Balances", systemImage: "creditcard", value: MainTab.balances) { Color.clear }
            Tab("Contacts", systemImage: "person.crop.circle", value: MainTab.contacts) { Color.clear }
            Tab("Activity", systemImage: "list.bullet.rectangle", value: MainTab.activity) { Color.clear }
        }
        .tabViewStyle(.sidebarAdaptable)
        .glassToast("Coming soon", trigger: model.comingSoonCount)
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

    // Selecting a tab that is not built yet keeps Groups and says "Coming soon".
    private var tabSelection: Binding<MainTab> {
        Binding(
            get: { .groups },
            set: { tab in
                switch tab {
                case .balances: model.send(GroupsUiEventOnBalancesTabClicked.shared)
                case .contacts: model.send(GroupsUiEventOnContactsTabClicked.shared)
                case .activity: model.send(GroupsUiEventOnActivityTabClicked.shared)
                case .groups: break
                }
            },
        )
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
                GroupsTable(model: model)
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
        .toolbar {
            if !state.groups.isEmpty {
                ToolbarItem(placement: .principal) {
                    Picker("Status", selection: settledBinding) {
                        Text("Open").tag(false)
                        Text("Settled up").tag(true)
                    }
                    .pickerStyle(.segmented)
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
    /// Open or Settled up, the toolbar's segments.
    private var settledBinding: Binding<Bool> {
        Binding(
            get: { model.state.showsSettledGroups },
            set: { model.send(GroupsUiEventOnShowSettledChanged(isShown: $0)) },
        )
    }

    /// "Offline · 1 change queued · 4 open", "4 open" or "1 settled up".
    private var macSubtitle: Text {
        let state = model.state
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
