import Observation
import Shared
import SwiftUI

@Observable
final class WatchBalancesModel {
    private(set) var state: WatchBalancesUiState

    @ObservationIgnored private let holder = BlitzSplitWatch.shared.createWatchBalancesViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! WatchBalancesUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
    }

    func send(_ event: any WatchBalancesUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Balances" (15.3), the watch's home once connected: "Offline · 1 change queued" on top while offline, one "Overall
/// balance" card per currency, each open group with your net in it, and "Updated at 14:32" at the end. With nothing
/// open, "Nothing open" (15.3b).
struct WatchBalancesView: View {
    @State private var model = WatchBalancesModel()

    var body: some View {
        let state = model.state
        Group {
            if !state.isLoaded {
                ProgressView()
                    .tint(WatchPalette.accent)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if state.isAllSettled {
                nothingOpen(state)
            } else {
                list(state)
            }
        }
        .navigationTitle("Balances")
    }

    private func list(_ state: WatchBalancesUiState) -> some View {
        ScrollView {
            LazyVStack(spacing: 6) {
                if state.showsOfflineNotice {
                    WatchOfflineNotice(queuedCount: Int(state.queuedChangeCount))
                }
                ForEach(state.totals, id: \.currency.code) { total in
                    WatchOverallCard(total: total)
                }
                ForEach(state.groups, id: \.rowId) { group in
                    Button {
                        model.send(WatchBalancesUiEventOnGroupClicked(groupId: group.groupId, currency: group.currency))
                    } label: {
                        WatchGroupCard(group: group)
                    }
                    .buttonStyle(.plain)
                }
                if let syncedAt = state.lastSyncedAt {
                    Text("Updated at \(ExpenseFormat.time(syncedAt.int64Value))")
                        .font(.caption2)
                        .foregroundStyle(WatchPalette.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.top, 2)
                }
            }
        }
    }

    private func nothingOpen(_ state: WatchBalancesUiState) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 6) {
                if state.showsOfflineNotice {
                    WatchOfflineNotice(queuedCount: Int(state.queuedChangeCount))
                }
                Image(systemName: "checkmark")
                    .font(.title2.weight(.semibold))
                    .foregroundStyle(WatchPalette.accent)
                    .accessibilityHidden(true)
                Text("Nothing open")
                    .font(.headline)
                Text("You owe nothing and nobody owes you. New groups and bills show up here on their own.")
                    .font(.subheadline)
                    .foregroundStyle(WatchPalette.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 6)
        }
    }
}

/// "Offline · 1 change queued" with the orange dot, or "Offline" with nothing queued (15.6b).
struct WatchOfflineNotice: View {
    let queuedCount: Int

    var body: some View {
        HStack(spacing: 4) {
            Circle()
                .fill(WatchPalette.pending)
                .frame(width: 6, height: 6)
                .accessibilityHidden(true)
            Group {
                if queuedCount > 0 {
                    Text("Offline · \(queuedCount) changes queued")
                } else {
                    Text("Offline")
                }
            }
            .font(.caption2)
            .foregroundStyle(WatchPalette.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 4)
        .accessibilityElement(children: .combine)
    }
}

/// "Overall balance", "+R$ 186,40", "to receive": one per currency, as Balances' totals.
private struct WatchOverallCard: View {
    let total: WatchTotal

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Overall balance")
                .font(.footnote)
                .foregroundStyle(WatchPalette.secondary)
            Text(verbatim: total.currency.formatSigned(total.netMinor))
                .font(.system(size: 24, weight: .bold))
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.6)
                .foregroundStyle(WatchPalette.balance(total.netMinor))
            if let word = Self.word(total.netMinor) {
                Text(word)
                    .font(.footnote)
                    .foregroundStyle(WatchPalette.balance(total.netMinor))
            }
        }
        .watchCard()
        .accessibilityElement(children: .combine)
    }

    private static func word(_ minor: Int64) -> LocalizedStringKey? {
        minor > 0 ? "to receive" : minor < 0 ? "you owe" : nil
    }
}

/// A group's row: its name over "+R$ 214,90 to receive" or "−R$ 98,00 you owe", and "waiting to send" while a
/// payment recorded here waits to go up.
private struct WatchGroupCard: View {
    let group: WatchGroupRow

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(verbatim: group.name)
                .font(.body)
                .lineLimit(1)
            amount
                .font(.subheadline.weight(.semibold))
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.7)
                .foregroundStyle(WatchPalette.balance(group.netMinor))
            if group.isQueued {
                WatchWaitingToSend()
            }
        }
        .watchCard()
        .contentShape(.rect(cornerRadius: 22))
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
    }

    @ViewBuilder
    private var amount: some View {
        let signed = group.currency.formatSigned(group.netMinor)
        if group.netMinor > 0 {
            Text("\(signed) to receive")
        } else if group.netMinor < 0 {
            Text("\(signed) you owe")
        } else {
            Text("Even overall")
        }
    }
}

/// The orange dot and "waiting to send" of a group with a payment not up yet.
struct WatchWaitingToSend: View {
    var body: some View {
        HStack(spacing: 4) {
            Circle()
                .fill(WatchPalette.pending)
                .frame(width: 6, height: 6)
                .accessibilityHidden(true)
            Text("waiting to send")
                .font(.caption2)
                .foregroundStyle(WatchPalette.secondary)
        }
    }
}

private extension WatchGroupRow {
    /// A group in two currencies is two rows.
    var rowId: String { "\(groupId):\(currency.code)" }
}
