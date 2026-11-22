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
    /// Made once the screen appears: SwiftUI builds this view's value again whenever its parent redraws, and a model
    /// made in the initial value would start a Kotlin ViewModel each time.
    @State private var model: WatchBalancesModel?

    var body: some View {
        Group {
            if let model {
                WatchBalancesScreen(model: model)
            } else {
                Color.black.ignoresSafeArea()
            }
        }
        .onAppear {
            if model == nil { model = WatchBalancesModel() }
        }
    }
}

private struct WatchBalancesScreen: View {
    let model: WatchBalancesModel

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
        // The rows count too: the groups can come after the balances are loaded, and the walk must see them
        .onChange(of: state.isLoaded ? state.groups.count : 0, initial: true) { _, _ in
            // The store capture's walk (WatchStoreScreenshots): to the group you owe in alone, in one currency (the
            // soccer's: the trip is on the list twice), as a tap on its row
            let rows = state.groups
            guard state.isLoaded,
                  let group = rows.first(where: { row in row.netMinor < 0 && rows.filter { $0.groupId == row.groupId }.count == 1 })
            else { return }
            WatchStoreScreenshots.walk("group", for: ["group", "confirm", "recorded"]) {
                model.send(WatchBalancesUiEventOnGroupClicked(groupId: group.groupId, currency: group.currency))
            }
        }
        .sheet(isPresented: signOutBinding) {
            WatchSignOutQuestion(
                confirm: { model.send(WatchBalancesUiEventOnSignOutConfirmed.shared) },
                cancel: { model.send(WatchBalancesUiEventOnSignOutDismissed.shared) },
            )
        }
    }

    /// Open while "Sign out of this watch?" is asked; swiped away, it answers Cancel.
    private var signOutBinding: Binding<Bool> {
        Binding(
            get: { model.state.isConfirmingSignOut },
            set: { isShown in
                if !isShown, model.state.isConfirmingSignOut {
                    model.send(WatchBalancesUiEventOnSignOutDismissed.shared)
                }
            },
        )
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
                WatchSignOutFooter(state: state, send: model.send)
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
            WatchSignOutFooter(state: state, send: model.send)
                .padding(.top, 8)
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

/// The end of Balances: "Sign out", the one way to disconnect only the watch; once asked, "Sign out scheduled" with
/// Cancel until what the watch queued went up, as the phone's Profile shows it.
private struct WatchSignOutFooter: View {
    let state: WatchBalancesUiState
    let send: (any WatchBalancesUiEvent) -> Void

    var body: some View {
        if state.isSignOutScheduled {
            VStack(spacing: 6) {
                VStack(spacing: 0) {
                    Text("Sign out scheduled")
                        .font(.footnote.weight(.semibold))
                    Text("Signs out as soon as everything is uploaded")
                        .font(.caption2)
                        .foregroundStyle(WatchPalette.secondary)
                }
                .multilineTextAlignment(.center)
                .accessibilityElement(children: .combine)
                Button {
                    send(WatchBalancesUiEventOnCancelScheduledSignOutClicked.shared)
                } label: {
                    Text("Cancel").frame(maxWidth: .infinity)
                }
                .buttonStyle(.glass)
            }
        } else {
            Button(role: .destructive) {
                send(WatchBalancesUiEventOnSignOutClicked.shared)
            } label: {
                Text("Sign out").frame(maxWidth: .infinity)
            }
            .buttonStyle(.glass)
        }
    }
}

/// "Sign out of this watch?": the watch alone signs out, once what it queued went up; the iPhone stays signed in.
private struct WatchSignOutQuestion: View {
    let confirm: () -> Void
    let cancel: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 4) {
                Text("Sign out of this watch?")
                    .font(.headline)
                    .multilineTextAlignment(.center)
                Text("Its data is erased once what it queued is sent. Your iPhone stays signed in.")
                    .font(.footnote)
                    .foregroundStyle(WatchPalette.secondary)
                    .multilineTextAlignment(.center)
            }
            VStack(spacing: 6) {
                Button(role: .destructive, action: confirm) {
                    Text("Sign out").frame(maxWidth: .infinity)
                }
                .buttonStyle(.glass)
                Button(action: cancel) {
                    Text("Cancel").frame(maxWidth: .infinity)
                }
                .buttonStyle(.glass)
            }
            .padding(.top, 10)
        }
        .toolbar(.hidden, for: .navigationBar)
    }
}
