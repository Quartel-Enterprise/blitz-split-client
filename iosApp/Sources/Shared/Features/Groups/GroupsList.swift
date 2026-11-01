import Shared
import SwiftUI

#if os(iOS)

/// The home's list on iPhone and iPad: the sync status while it asks for attention, "Overall balance", the open
/// groups by their latest activity, as rows or as the grid's cards, and, collapsed, the settled ones, always rows.
/// Pulling down syncs. A row opens the group; its ⋯ menu and its long press hold the group's actions. The search
/// is its own tab (GroupsSearchView). An iPhone lying down (5.1) keeps the sync status and "Overall balance" still in a
/// 300 pt column on the left, the list scrolling on the right.
struct GroupsList: View {
    let model: GroupsModel
    @Environment(\.showsGroupBesideList) private var showsGroupBesideList
    @State private var layout = ScreenLayout()

    /// Lying down short of an iPad's room, the list alone: an iPhone lying down (an iPad's list column is upright).
    private var usesSideColumn: Bool {
        layout.usesTwoColumns && !layout.isLarge && !showsGroupBesideList
    }

    /// The group open beside the list on an iPad lying down, whose row or card is highlighted.
    private var selectedGroupId: String? {
        showsGroupBesideList ? model.state.selectedGroupId : nil
    }

    var body: some View {
        // The list keeps its place whether the column shows, so turning the iPhone keeps its scroll
        HStack(alignment: .top, spacing: usesSideColumn ? 28 : 0) {
            if usesSideColumn {
                sideColumn
                    .frame(width: 300)
            }
            list(showsSummary: !usesSideColumn)
        }
        .measuringScreenLayout($layout)
    }

    /// The sync status while it asks for attention and "Overall balance", which stay still lying down.
    private var sideColumn: some View {
        let state = model.state
        return ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                if let status = GroupsCopy.sync(state.syncBadge, pendingChangeCount: state.pendingChangeCount) {
                    syncStatusRow(status)
                }
                GroupsSummaryCard(summary: state.summary, isStacked: true, send: { model.send($0) })
            }
            .padding(.leading, 20)
            .padding(.vertical, 12)
        }
        .scrollBounceBehavior(.basedOnSize)
    }

    /// The sync status with its retry, in the list upright and in the column lying down.
    private func syncStatusRow(_ status: String) -> some View {
        let state = model.state
        return SyncStatusRow(status: status, badge: state.syncBadge, canRetry: state.canRetrySync) {
            model.send(GroupsUiEventOnRetrySyncClicked.shared)
        }
    }

    private func list(showsSummary: Bool) -> some View {
        let state = model.state
        return List {
            if showsSummary {
                if let status = GroupsCopy.sync(state.syncBadge, pendingChangeCount: state.pendingChangeCount) {
                    Section {
                        syncStatusRow(status)
                    }
                }
                Section {
                    GroupsSummaryCard(summary: state.summary, isStacked: false, send: { model.send($0) })
                        .listRowInsets(EdgeInsets())
                        .listRowBackground(Color.clear)
                }
            }
            Section {
                // Every group is settled up: the section says so rather than standing empty
                if state.showsNoOpenGroups {
                    Text("No open groups")
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity)
                }
                if state.showsGrid {
                    // One row holds the whole grid, drawn straight on the background
                    if !state.openGroups.isEmpty {
                        GroupsGrid(
                            groups: state.openGroups,
                            size: state.layout.gridSize,
                            isOnline: state.exit.isOnline,
                            selectedGroupId: selectedGroupId,
                            send: { model.send($0) },
                        )
                        .listRowInsets(EdgeInsets())
                        .listRowBackground(Color.clear)
                    }
                } else {
                    ForEach(state.openGroups, id: \.id) { group in
                        row(group)
                    }
                }
            } header: {
                HStack {
                    Text("Open · \(state.openGroups.count)")
                    Spacer()
                    GroupsDisplayMenu(state: state, send: { model.send($0) })
                        .textCase(nil)
                    Button("New group", systemImage: "plus") {
                        model.send(GroupsUiEventOnCreateGroupClicked.shared)
                    }
                    .buttonStyle(.bordered)
                    .textCase(nil)
                }
            }
            let settled = state.settledGroups
            if !settled.isEmpty {
                Section {
                    if state.showsSettledGroups {
                        ForEach(settled, id: \.id) { group in
                            row(group)
                        }
                    }
                } header: {
                    Button {
                        model.send(GroupsUiEventOnShowSettledChanged(isShown: !state.showsSettledGroups))
                    } label: {
                        HStack {
                            Text("Settled up · \(settled.count)")
                            Spacer()
                            Image(systemName: "chevron.down")
                                .rotationEffect(.degrees(state.showsSettledGroups ? 180 : 0))
                        }
                        .contentShape(.rect)
                    }
                    .buttonStyle(.plain)
                    .accessibilityValue(state.showsSettledGroups ? Text("Expanded") : Text("Collapsed"))
                }
            }
        }
        .listStyle(.insetGrouped)
        .refreshable { await model.refresh() }
    }

    private func row(_ group: GroupRow) -> some View {
        let isOnline = model.state.exit.isOnline
        return HStack(spacing: 0) {
            Button {
                model.send(GroupsUiEventOnGroupClicked(groupId: group.id))
            } label: {
                GroupListRow(group: group)
            }
            .buttonStyle(.plain)
            GroupRowMenuButton(group: group, isOnline: isOnline, send: { model.send($0) })
        }
        .contextMenu {
            GroupRowActions(group: group, isOnline: isOnline, send: { model.send($0) })
        }
        .accessibilityAddTraits(group.id == selectedGroupId ? .isSelected : [])
        .selectedRowBackground(group.id == selectedGroupId)
    }
}

/// "Overall balance": the sum of your net in each group, one line per currency. "To get back" and "To pay" will
/// open the global Balances already filtered, and stay disabled while there is nothing in them. In an iPhone lying
/// down's column the buttons go side by side under the amount, as the canvas draws it.
private struct GroupsSummaryCard: View {
    let summary: GroupsSummary
    let isStacked: Bool
    let send: (any GroupsUiEvent) -> Void

    var body: some View {
        Group {
            if isStacked {
                VStack(alignment: .leading, spacing: 10) {
                    amount
                    HStack(spacing: 10) {
                        buttons
                    }
                }
                .padding(16)
            } else {
                HStack(alignment: .center, spacing: 12) {
                    amount
                    Spacer(minLength: 0)
                    VStack(spacing: 6) {
                        buttons
                    }
                    .fixedSize(horizontal: true, vertical: false)
                }
                .padding(20)
            }
        }
        .background(Palette.card, in: .rect(cornerRadius: isStacked ? 26 : 28))
    }

    private var amount: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("Overall balance")
                .font(.footnote.weight(.medium))
                .foregroundStyle(.secondary)
            ForEach(GroupsCopy.amounts(summary.totals) { $0.netMinor }, id: \.self) { amount in
                Text(verbatim: amount)
                    .font(summary.totals.count > 1 ? .title2.bold() : isStacked ? .title.bold() : .largeTitle.bold())
                    .monospacedDigit()
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
            }
            Text(verbatim: GroupsCopy.summaryHint(summary.kind))
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder
    private var buttons: some View {
        SummaryButton(
            label: "To get back",
            amounts: GroupsCopy.amounts(summary.totals) { $0.toReceiveMinor },
            color: Palette.accent,
            isEnabled: summary.canOpenToReceive,
            hint: "See who owes you",
        ) {
            send(GroupsUiEventOnToReceiveClicked.shared)
        }
        SummaryButton(
            label: "To pay",
            amounts: GroupsCopy.amounts(summary.totals) { -$0.toPayMinor },
            color: Palette.negative,
            isEnabled: summary.canOpenToPay,
            hint: "See whom you owe",
        ) {
            send(GroupsUiEventOnToPayClicked.shared)
        }
    }
}

private struct GroupListRow: View {
    let group: GroupRow

    var body: some View {
        HStack(spacing: 14) {
            GroupAvatar(initials: group.initials, color: group.color, size: 44)
            VStack(alignment: .leading, spacing: 4) {
                Text(verbatim: group.name)
                    .font(.body.weight(.semibold))
                    .lineLimit(group.usesTwoNameLines ? 2 : 1)
                HStack(spacing: 6) {
                    MemberFaces(group: group)
                    if group.isPending {
                        PendingIndicator()
                    }
                    Text(verbatim: GroupsCopy.detail(group))
                        .lineLimit(1)
                }
                .font(.footnote)
                .foregroundStyle(.secondary)
            }
            Spacer(minLength: 8)
            if group.isSettled {
                Label("Settled up", systemImage: "checkmark")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.secondary)
            } else {
                VStack(alignment: .trailing, spacing: 2) {
                    Text(verbatim: group.currency.formatSigned(group.netMinor))
                        .font(.body.bold())
                        .monospacedDigit()
                        .foregroundStyle(Palette.balance(group.netMinor))
                    Text(verbatim: GroupsCopy.balanceLabel(group.balanceKind))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .contentShape(.rect)
    }
}
#endif

/// The orange dot of a group with a change waiting to go up ("na fila"), on its row and its card.
struct PendingIndicator: View {
    var body: some View {
        Circle()
            .fill(.orange)
            .frame(width: 8, height: 8)
            .accessibilityHidden(true)
    }
}

/// "Offline · 1 change queued", "Syncing…", or "No response from the server" with "Try again".
struct SyncStatusRow: View {
    let status: String
    let badge: SyncBadge?
    let canRetry: Bool
    let onRetry: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: badge == .offline ? "icloud.slash" : canRetry ? "exclamationmark.icloud" : "arrow.triangle.2.circlepath.icloud")
                .accessibilityHidden(true)
            Text(verbatim: status)
                .font(.subheadline)
            Spacer(minLength: 8)
            if canRetry {
                Button("Try again", action: onRetry)
                    .font(.subheadline.weight(.semibold))
            }
        }
        .foregroundStyle(.secondary)
    }
}

/// "To get back" or "To pay" beside an overall balance (Groups' and Home's): the amount in its color and a chevron,
/// disabled while there is nothing in it.
struct SummaryButton: View {
    let label: LocalizedStringKey
    let amounts: [String]
    let color: Color
    let isEnabled: Bool
    let hint: LocalizedStringKey
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                VStack(alignment: .leading, spacing: 0) {
                    Text(label)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    ForEach(amounts, id: \.self) { amount in
                        Text(verbatim: amount)
                            .font(.subheadline.bold())
                            .monospacedDigit()
                            .foregroundStyle(isEnabled ? color : .secondary)
                    }
                }
                Spacer(minLength: 4)
                if isEnabled {
                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.secondary)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .frame(minHeight: 44)
            .background(Palette.groupedBackground, in: .rect(cornerRadius: 16))
        }
        .buttonStyle(.plain)
        .disabled(!isEnabled)
        .accessibilityHint(isEnabled ? Text(hint) : Text(verbatim: ""))
    }
}
