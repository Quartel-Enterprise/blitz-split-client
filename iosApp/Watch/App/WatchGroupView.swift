import Observation
import Shared
import SwiftUI
import WatchKit

@Observable
final class WatchGroupModel {
    private(set) var state: WatchGroupUiState
    /// "Payment recorded" (15.6) on screen, until it ends and the watch goes back to the balances.
    var showsRecorded = false

    @ObservationIgnored private let holder: ViewModelHolder<WatchGroupViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteWatchGroup) {
        holder = BlitzSplitWatch.shared.createWatchGroupViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! WatchGroupUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            if action is WatchGroupUiActionShowPaymentRecorded { self?.showsRecorded = true }
        })
    }

    func send(_ event: any WatchGroupUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// A group on the watch (15.4): your net in it in big, then each person with you, the largest first. "I paid" is the
/// toolbar's tinted item at the bottom while you owe a single person; owing two or more, each of their rows is its own
/// "I paid". Its question (15.5) is a sheet, and "Payment recorded" (15.6) covers the screen.
struct WatchGroupView: View {
    @State private var model: WatchGroupModel

    init(route: RouteWatchGroup) {
        _model = State(initialValue: WatchGroupModel(route: route))
    }

    var body: some View {
        let state = model.state
        ScrollView {
            VStack(spacing: 6) {
                if state.isLoaded {
                    netCard(state)
                    ForEach(state.debts.dropFirst(state.debts.count == 1 ? 1 : 0), id: \.memberId) { debt in
                        debtRow(debt, isClickable: state.areDebtsClickable)
                    }
                }
            }
        }
        .navigationTitle(Text(verbatim: state.groupName ?? ""))
        .toolbar {
            if state.showsIPaid, let debt = state.singleDebtToPay {
                ToolbarItem(placement: .bottomBar) {
                    Button {
                        model.send(WatchGroupUiEventOnIPaidClicked(memberId: debt.memberId))
                    } label: {
                        Text("I paid").padding(.horizontal, 10)
                    }
                    .buttonStyle(.glassProminent)
                    .prominentLabel()
                }
            }
        }
        .sheet(isPresented: confirmingBinding) {
            if let debt = state.confirming {
                WatchConfirmPaymentView(
                    debt: debt,
                    currency: state.currency,
                    groupName: state.groupName ?? "",
                    isRecording: state.isRecording,
                    confirm: { model.send(WatchGroupUiEventOnPaymentConfirmed.shared) },
                    cancel: { model.send(WatchGroupUiEventOnConfirmDismissed.shared) },
                )
                .interactiveDismissDisabled(state.isRecording)
            }
        }
        // Over the screen rather than presented: it shows while the question's sheet is still closing
        .overlay {
            if model.showsRecorded {
                WatchPaymentRecordedView {
                    model.showsRecorded = false
                    model.send(WatchGroupUiEventOnRecordedShown.shared)
                }
                .ignoresSafeArea()
            }
        }
        .toolbar(model.showsRecorded ? .hidden : .automatic, for: .navigationBar)
    }

    /// Open while a pair is asked about; swiped away, it answers ✕.
    private var confirmingBinding: Binding<Bool> {
        Binding(
            get: { model.state.confirming != nil },
            set: { isShown in
                if !isShown, model.state.confirming != nil, !model.state.isRecording {
                    model.send(WatchGroupUiEventOnConfirmDismissed.shared)
                }
            },
        )
    }

    /// "−R$ 98,00" over "you owe Carla" with her face: with a single pair, the card says whom; with more, the rows do.
    private func netCard(_ state: WatchGroupUiState) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(verbatim: state.currency.formatSigned(state.netMinor))
                .font(.system(size: 24, weight: .bold))
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.6)
                .foregroundStyle(WatchPalette.balance(state.netMinor))
            if state.debts.count == 1, let debt = state.debts.first {
                WatchDebtLine(debt: debt, showsAmount: false)
            }
            if state.isQueued {
                WatchWaitingToSend()
            }
        }
        .watchCard()
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder
    private func debtRow(_ debt: WatchDebt, isClickable: Bool) -> some View {
        if isClickable, debt.isOwedByMe {
            Button {
                model.send(WatchGroupUiEventOnIPaidClicked(memberId: debt.memberId))
            } label: {
                WatchDebtLine(debt: debt, showsAmount: true, currency: model.state.currency)
                    .watchCard()
                    .contentShape(.rect(cornerRadius: 22))
            }
            .buttonStyle(.plain)
            .accessibilityHint(Text("I paid"))
        } else {
            WatchDebtLine(debt: debt, showsAmount: true, currency: model.state.currency)
                .watchCard()
        }
    }
}

/// "you owe Carla" or "Ana owes you" with their face, and the amount when the row stands alone.
private struct WatchDebtLine: View {
    let debt: WatchDebt
    let showsAmount: Bool
    var currency: GroupCurrency?

    var body: some View {
        HStack(spacing: 6) {
            WatchAvatar(initials: debt.initials, color: debt.color, photoUrl: debt.photoUrl)
            VStack(alignment: .leading, spacing: 0) {
                sentence
                    .font(.subheadline)
                    .foregroundStyle(WatchPalette.secondary)
                    .lineLimit(2)
                if showsAmount, let currency {
                    Text(verbatim: currency.formatSigned(debt.amountMinor))
                        .font(.subheadline.weight(.semibold))
                        .monospacedDigit()
                        .foregroundStyle(WatchPalette.balance(debt.amountMinor))
                }
            }
        }
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder
    private var sentence: some View {
        let name = debt.name ?? String(localized: "Former member", bundle: .appLanguage)
        if debt.isOwedByMe {
            Text("you owe \(name)")
        } else {
            Text("\(name) owes you")
        }
    }
}

/// 15.5: "Paid R$ 98,00 to Carla?", the group and what happens offline, with "I paid" and "Cancel".
private struct WatchConfirmPaymentView: View {
    let debt: WatchDebt
    let currency: GroupCurrency
    let groupName: String
    let isRecording: Bool
    let confirm: () -> Void
    let cancel: () -> Void

    var body: some View {
        let amount = currency.format(-debt.amountMinor)
        let name = debt.name ?? String(localized: "Former member", bundle: .appLanguage)
        ScrollView {
            VStack(spacing: 4) {
                Text("Paid \(amount) to \(name)?")
                    .font(.headline)
                    .multilineTextAlignment(.center)
                Text("\(groupName). The group sees it right away, or when the internet is back.")
                    .font(.subheadline)
                    .foregroundStyle(WatchPalette.secondary)
                    .multilineTextAlignment(.center)
            }
            VStack(spacing: 6) {
                Button(action: confirm) {
                    Text("I paid").frame(maxWidth: .infinity)
                }
                .buttonStyle(.glassProminent)
                .prominentLabel()
                Button(action: cancel) {
                    Text("Cancel").frame(maxWidth: .infinity)
                }
                .buttonStyle(.glass)
            }
            .disabled(isRecording)
            .padding(.top, 10)
        }
        .toolbar(.hidden, for: .navigationBar)
    }
}

/// 15.6: the accent's check and "Payment recorded", for a moment, then back to the balances.
private struct WatchPaymentRecordedView: View {
    let done: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "checkmark")
                .font(.system(size: 40, weight: .bold))
                .foregroundStyle(WatchPalette.onAccent)
                .frame(width: 84, height: 84)
                .background(WatchPalette.accent, in: .circle)
                .accessibilityHidden(true)
            Text("Payment recorded")
                .font(.headline)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(.black)
        .toolbar(.hidden, for: .navigationBar)
        .task {
            WKInterfaceDevice.current().play(.success)
            try? await Task.sleep(for: .seconds(1.5))
            done()
        }
    }
}
