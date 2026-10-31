#if os(iOS)
import Shared
import SwiftUI

/// The home's list on iPhone and iPad: the sync status while it asks for attention, "Overall balance", the open
/// groups by their latest activity and, collapsed, the settled ones. Pulling down syncs. A row opens the group; its
/// ⋯ menu, the grid and the search come with their own flows.
struct GroupsList: View {
    let model: GroupsModel

    var body: some View {
        let state = model.state
        List {
            if let status = GroupsCopy.sync(state.syncBadge, pendingChangeCount: state.pendingChangeCount) {
                Section {
                    SyncStatusRow(status: status, badge: state.syncBadge, canRetry: state.canRetrySync) {
                        model.send(GroupsUiEventOnRetrySyncClicked.shared)
                    }
                }
            }
            Section {
                GroupsSummaryCard(summary: state.summary, send: { model.send($0) })
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
            }
            Section {
                // Every group is settled up: the section says so rather than standing empty
                if state.showsNoOpenGroups {
                    Text("No open groups")
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity)
                }
                ForEach(state.openGroups, id: \.id) { group in
                    row(group)
                }
            } header: {
                HStack {
                    Text("Open · \(state.openGroups.count)")
                    Spacer()
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
        Button {
            model.send(GroupsUiEventOnGroupClicked(groupId: group.id))
        } label: {
            GroupListRow(group: group)
        }
        .buttonStyle(.plain)
    }
}

/// "Offline · 1 change queued", "Syncing…", or "No response from the server" with "Try again".
private struct SyncStatusRow: View {
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

/// "Overall balance": the sum of your net in each group, one line per currency. "To get back" and "To pay" will
/// open the global Balances already filtered, and stay disabled while there is nothing in them.
private struct GroupsSummaryCard: View {
    let summary: GroupsSummary
    let send: (any GroupsUiEvent) -> Void

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text("Overall balance")
                    .font(.footnote.weight(.medium))
                    .foregroundStyle(.secondary)
                ForEach(GroupsCopy.amounts(summary.totals) { $0.netMinor }, id: \.self) { amount in
                    Text(verbatim: amount)
                        .font(summary.totals.count > 1 ? .title2.bold() : .largeTitle.bold())
                        .monospacedDigit()
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                }
                Text(verbatim: GroupsCopy.summaryHint(summary.kind))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .accessibilityElement(children: .combine)
            Spacer(minLength: 0)
            VStack(spacing: 6) {
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
            .fixedSize(horizontal: true, vertical: false)
        }
        .padding(20)
        .background(Palette.card, in: .rect(cornerRadius: 28))
    }
}

private struct SummaryButton: View {
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
                    HStack(spacing: -6) {
                        ForEach(Array(group.faces.enumerated()), id: \.offset) { _, face in
                            AvatarPhoto(initial: face.initial, color: face.color, photoUrl: nil, size: 20)
                        }
                        if group.extraMemberCount > 0 {
                            Text(verbatim: "+\(group.extraMemberCount)")
                                .font(.caption2.weight(.semibold))
                                .padding(.leading, 10)
                        }
                    }
                    .accessibilityElement()
                    .accessibilityLabel(Text(verbatim: GroupsCopy.members(group.memberCount)))
                    if group.isPending {
                        Circle()
                            .fill(.orange)
                            .frame(width: 8, height: 8)
                            .accessibilityHidden(true)
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
