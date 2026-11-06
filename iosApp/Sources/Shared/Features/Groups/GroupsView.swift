import Observation
import Shared
import SwiftUI

@Observable
final class GroupsModel {
    private(set) var state: GroupsUiState
    private(set) var comingSoonCount = 0
    private(set) var leaveErrorCount = 0
    private(set) var changeErrorCount = 0
    let deletedGroup = DeletedGroupToast()

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createGroupsViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! GroupsUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
            if !state.isRefreshing { self?.endRefreshes() }
            self?.deletedGroup.show(state.deletedGroupNotice) {
                self?.send(GroupsUiEventOnDeletedGroupNoticeShown.shared)
            }
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            switch action {
            case is GroupsUiActionShowComingSoon: self?.comingSoonCount += 1
            case is GroupsUiActionShowLeaveError: self?.leaveErrorCount += 1
            case is GroupsUiActionShowChangeError: self?.changeErrorCount += 1
            default: break
            }
        })
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
    @ObservationIgnored private let refreshes = PendingRefreshes()

    /// Pulled down: asks for a sync and returns once the shared code says it ended (at once offline).
    func refresh() async {
        send(GroupsUiEventOnRefresh.shared)
        // The event marks the sync as started before it returns, and starts none offline
        await refreshes.wait(isRefreshing: (holder.viewModel.uiState.value as! GroupsUiState).isRefreshing)
    }

    private func endRefreshes() {
        refreshes.endAll()
    }

    isolated deinit {
        endRefreshes()
        holder.clear()
    }
}

struct GroupsView: View {
    let model: GroupsModel
    #if os(macOS)
    @Environment(\.isNarrowWindow) private var isNarrowWindow
    #endif

    var body: some View {
        let state = model.state
        Group {
            #if os(iOS)
            // The first download draws the list before its first group, with what is still coming in grey
            let showsList = !state.groups.isEmpty || state.firstDownload != nil
            #else
            let showsList = !state.groups.isEmpty
            #endif
            if showsList {
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
                // The groups not read yet (or, on the Mac, a first download before its first group): their shape
                // shimmers, so the empty state never flashes before the list
                ShimmerScreen(isWaitingForInternet: state.firstDownload?.isWaitingForInternet == true, isAboutGroups: true) {
                    ShimmerCard()
                        .background(Palette.card, in: .rect(cornerRadius: 28))
                    ShimmerList(count: 4, leading: .badge)
                }
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
        #if os(iOS)
        .tabTitleDisplayMode()
        .toolbar {
            TabTitle(title: String(localized: "Groups", bundle: .appLanguage))
            TabTrailingItems(
                showsSplitBill: state.showsSplitBillButton,
                showsSplitBillTitle: false,
                onSplitBill: { model.send(GroupsUiEventOnSplitBillClicked.shared) },
            ) {
                joinButton
                profileButton
            }
        }
        #else
        .navigationSubtitle(macSubtitle)
        .toolbar {
            if !state.groups.isEmpty {
                // A narrow window draws them above the table instead
                if !isNarrowWindow {
                    ToolbarItem(placement: .principal) {
                        GroupsStatusPicker(model: model)
                    }
                }
                // List or Grid, as in the Finder; the size is the slider under the grid and View › Grid Size
                ToolbarItem(placement: .principal) {
                    Picker("View", selection: displayBinding) {
                        Label("List", systemImage: GroupsDisplayCopy.displayIcon(.list)).tag(GroupsDisplay.list)
                        Label("Grid", systemImage: GroupsDisplayCopy.displayIcon(.grid)).tag(GroupsDisplay.grid)
                    }
                    .pickerStyle(.segmented)
                    .labelStyle(.iconOnly)
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
                // File › New Group (⇧⌘N) runs it (`NewItemCommands`)
                .help(Text("New group (⇧⌘N)"))
                profileButton
            }
            if state.showsSplitBillButton {
                ToolbarItem(placement: .primaryAction) {
                    SplitBillButton {
                        model.send(GroupsUiEventOnSplitBillClicked.shared)
                    }
                    // File › Split a Bill (⌘N) runs it (`NewItemCommands`)
                    .help(Text("Split a bill (⌘N)"))
                }
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
        ProfileToolbarButton(
            initial: model.state.avatarInitial,
            color: model.state.avatarColor,
            photoUrl: model.state.photoUrl,
        ) {
            model.send(GroupsUiEventOnProfileClicked.shared)
        }
    }

    #if os(macOS)
    /// List or Grid, the toolbar's other segments.
    private var displayBinding: Binding<GroupsDisplay> {
        Binding(
            get: { model.state.layout.display },
            set: { model.send(GroupsUiEventOnDisplayChanged(display: $0)) },
        )
    }

    /// "Offline · 1 change queued · 4 open", "4 open" or "1 settled up"; a first download's "Syncing… 3 of 7 groups"
    /// in front, the Mac's place for it, as Home's.
    private var macSubtitle: Text {
        let state = model.state
        let sync = GroupsCopy.sync(
            state.syncBadge,
            pendingChangeCount: state.pendingChangeCount,
            downloadProgress: state.firstDownload,
        )
        guard !state.groups.isEmpty else {
            return Text(verbatim: sync ?? "")
        }
        let count = state.showsSettledGroups
            ? String(localized: "\(state.settledGroups.count) settled up", bundle: .appLanguage)
            : String(localized: "\(state.openGroups.count) open", bundle: .appLanguage)
        guard let sync else {
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
