#if os(macOS)
import Shared
import SwiftUI

/// The Mac's home: "Overall balance", "To get back" and "To pay" over a zebra table of the groups that the
/// toolbar's Open / Settled up segments pick, or the open groups as the grid's cards, with the size slider below
/// them as in the Finder. Clicking a row or a card opens the group; its ⋯ and its right click hold the group's
/// actions. Settled up stays a table.
struct GroupsTable: View {
    let model: GroupsModel

    @State private var selection: String?

    var body: some View {
        let state = model.state
        let rows = (state.showsSettledGroups ? state.settledGroups : state.openGroups).map(GroupItem.init)
        VStack(alignment: .leading, spacing: 16) {
            GroupsSummaryCards(summary: state.summary, send: { model.send($0) })
            if state.showsGrid, !state.showsSettledGroups {
                GroupsGridPane(model: model)
            } else {
                table(rows)
            }
        }
        .padding(20)
    }

    private func table(_ rows: [GroupItem]) -> some View {
        let state = model.state
        return Table(rows, selection: $selection) {
            TableColumn("Group") { item in
                HStack(spacing: 8) {
                    GroupAvatar(initials: item.row.initials, color: item.row.color, size: 22)
                    Text(verbatim: item.row.name)
                        .fontWeight(.medium)
                        .lineLimit(1)
                    if item.row.isPending {
                        Text("queued")
                            .font(.caption)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 1)
                            .background(.orange.opacity(0.18), in: .rect(cornerRadius: 5))
                            .foregroundStyle(.orange)
                    }
                }
            }
            .width(min: 180, ideal: 260)
            TableColumn("Members") { item in
                Text(verbatim: GroupsCopy.memberNames(item.row))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            .width(min: 120, ideal: 200)
            TableColumn("Last activity") { item in
                Text(verbatim: GroupsCopy.activityColumn(item.row))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            .width(min: 120, ideal: 220)
            TableColumn("Your balance") { item in
                Text(verbatim: item.balance)
                    .fontWeight(.semibold)
                    .monospacedDigit()
                    .foregroundStyle(Palette.balance(item.row.isSettled ? 0 : item.row.netMinor))
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
            .width(min: 110, ideal: 130)
            TableColumn(String()) { item in
                GroupRowMenuButton(group: item.row, isOnline: state.exit.isOnline, send: { model.send($0) })
            }
            .width(32)
        }
        .contextMenu(forSelectionType: String.self) { groupIds in
            if let item = rows.first(where: { groupIds.contains($0.id) }) {
                GroupRowActions(group: item.row, isOnline: state.exit.isOnline, send: { model.send($0) })
            }
        }
        .alternatingRowBackgrounds(.enabled)
        // The selected segment with no group, once every group is on the other one
        .overlay {
            if state.showsSettledGroups, state.showsNoSettledGroups {
                Text("No settled groups")
                    .foregroundStyle(.secondary)
            } else if !state.showsSettledGroups, state.showsNoOpenGroups {
                Text("No open groups")
                    .foregroundStyle(.secondary)
            }
        }
        .onChange(of: selection) { _, groupId in
            guard let groupId else { return }
            model.send(GroupsUiEventOnGroupClicked(groupId: groupId))
            selection = nil
        }
    }
}

/// The open groups as cards, and under them the count and the grid's size slider: Compact at the small grid, Large at
/// the big square.
private struct GroupsGridPane: View {
    let model: GroupsModel

    var body: some View {
        let state = model.state
        VStack(spacing: 0) {
            ScrollView {
                GroupsGrid(
                    groups: state.openGroups,
                    size: state.layout.gridSize,
                    isOnline: state.exit.isOnline,
                    send: { model.send($0) },
                )
            }
            .overlay {
                if state.showsNoOpenGroups {
                    Text("No open groups")
                        .foregroundStyle(.secondary)
                }
            }
            HStack(spacing: 8) {
                Text("\(state.openGroups.count) groups")
                Spacer()
                Image(systemName: "square.grid.3x3")
                    .imageScale(.small)
                    .accessibilityHidden(true)
                Slider(value: sizeIndex, in: 0 ... 2, step: 1)
                    .controlSize(.mini)
                    .frame(width: 96)
                    .accessibilityLabel(Text("Grid size"))
                    .accessibilityValue(Text(GroupsDisplayCopy.gridSizeName(state.layout.gridSize)))
                Image(systemName: "square")
                    .accessibilityHidden(true)
            }
            .font(.caption)
            .foregroundStyle(.secondary)
            .frame(height: 28)
            .padding(.horizontal, 6)
        }
    }

    /// 0 for Compact, 1 for Standard and 2 for Large.
    private var sizeIndex: Binding<Double> {
        Binding(
            get: { Double(GroupsDisplayCopy.gridSizes.firstIndex(of: model.state.layout.gridSize) ?? 1) },
            set: { value in
                let index = min(max(Int(value.rounded()), 0), GroupsDisplayCopy.gridSizes.count - 1)
                model.send(GroupsUiEventOnGridSizeChanged(size: GroupsDisplayCopy.gridSizes[index]))
            },
        )
    }
}

/// A row of the table, which needs an Identifiable value.
private struct GroupItem: Identifiable {
    let row: GroupRow

    var id: String { row.id }

    /// "+R$ 214,90", "−R$ 98,00", "R$ 0,00" or "Settled up".
    var balance: String {
        row.isSettled ? String(localized: "Settled up") : row.currency.formatSigned(row.netMinor)
    }
}

/// "Overall balance", "To get back" and "To pay" as three buttons, one line per currency. The last two will open the
/// global Balances already filtered, and stay disabled while there is nothing in them.
private struct GroupsSummaryCards: View {
    let summary: GroupsSummary
    let send: (any GroupsUiEvent) -> Void

    var body: some View {
        HStack(spacing: 12) {
            SummaryCard(
                label: "Overall balance",
                amounts: GroupsCopy.amounts(summary.totals) { $0.netMinor },
                caption: GroupsCopy.summaryHint(summary.kind),
                color: Palette.balance(summary.totals.first { $0.netMinor != 0 }?.netMinor ?? 0),
                isEnabled: true,
            ) {
                send(GroupsUiEventOnBalancesTabClicked.shared)
            }
            SummaryCard(
                label: "To get back",
                amounts: GroupsCopy.amounts(summary.totals) { $0.toReceiveMinor },
                caption: String(localized: "in \(Int(summary.toReceiveGroupCount)) groups"),
                color: Palette.accent,
                isEnabled: summary.canOpenToReceive,
            ) {
                send(GroupsUiEventOnToReceiveClicked.shared)
            }
            SummaryCard(
                label: "To pay",
                amounts: GroupsCopy.amounts(summary.totals) { -$0.toPayMinor },
                caption: String(localized: "in \(Int(summary.toPayGroupCount)) groups"),
                color: Palette.negative,
                isEnabled: summary.canOpenToPay,
            ) {
                send(GroupsUiEventOnToPayClicked.shared)
            }
        }
    }
}

private struct SummaryCard: View {
    let label: LocalizedStringKey
    let amounts: [String]
    let caption: String
    let color: Color
    let isEnabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 4) {
                Text(label)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(.secondary)
                ForEach(amounts, id: \.self) { amount in
                    Text(verbatim: amount)
                        .font(.title.bold())
                        .monospacedDigit()
                        .foregroundStyle(isEnabled ? color : .secondary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                }
                Text(verbatim: caption)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Palette.card, in: .rect(cornerRadius: 12))
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .disabled(!isEnabled)
    }
}
#endif
