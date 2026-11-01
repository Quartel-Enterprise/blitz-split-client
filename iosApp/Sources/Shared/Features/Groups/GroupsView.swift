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
            case is GroupsUiActionShowDeleteError: self?.deleteErrorCount += 1
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
            HiddenTabTitle(title: String(localized: "Groups"))
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
                SplitBillButton {
                    model.send(GroupsUiEventOnSplitBillClicked.shared)
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
