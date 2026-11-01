import Observation
import Shared
import SwiftUI

@Observable
final class PickPaymentModel {
    private(set) var state: PickPaymentUiState

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createPickPaymentViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! PickPaymentUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
    }

    func send(_ event: any PickPaymentUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Who paid whom?", what Home's "Record payment" asks first: every open pair of every group, under the group's name,
/// the most recently used group first. A pair opens Record payment in this sheet's place. A sheet over Home on iPhone,
/// iPad and Mac, as Record payment is; it works offline on the device's data.
struct PickPaymentView: View {
    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: PickPaymentModel?

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                PickPaymentList(model: model)
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        #if os(macOS)
        .frame(width: 480, height: 520)
        #endif
        .onAppear {
            if model == nil { model = PickPaymentModel() }
        }
    }
}

private struct PickPaymentList: View {
    let model: PickPaymentModel

    var body: some View {
        let state = model.state
        List {
            ForEach(state.groups, id: \.groupId) { group in
                Section {
                    ForEach(group.pairs, id: \.self) { pair in
                        PairRow(pair: pair, currency: group.currency) {
                            model.send(PickPaymentUiEventOnPairClicked(
                                groupId: group.groupId,
                                fromMemberId: pair.fromMemberId,
                                toMemberId: pair.toMemberId,
                            ))
                        }
                    }
                } header: {
                    HStack(spacing: 8) {
                        GroupAvatar(initials: group.initials, color: group.color, size: 22)
                            .accessibilityHidden(true)
                        Text(verbatim: group.name)
                    }
                }
            }
        }
        .overlay {
            if state.showsEmpty {
                ContentUnavailableView {
                    Label("Nobody owes anything right now", systemImage: "checkmark.circle")
                } description: {
                    Text("When someone owes, just pick here who paid.")
                }
            }
        }
        .navigationTitle("Who paid whom?")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel", systemImage: "xmark", role: .cancel) {
                    model.send(PickPaymentUiEventOnDismissed.shared)
                }
            }
        }
        #else
        // The Mac's sheet has no bar: its title on top and Cancel at the bottom, as Record payment's
        .safeAreaInset(edge: .top) {
            Text("Who paid whom?")
                .font(.title2.bold())
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding([.horizontal, .top], 20)
                .accessibilityAddTraits(.isHeader)
        }
        .safeAreaInset(edge: .bottom) {
            HStack {
                Spacer()
                Button("Cancel") {
                    model.send(PickPaymentUiEventOnDismissed.shared)
                }
                .keyboardShortcut(.cancelAction)
                .controlSize(.large)
            }
            .padding(20)
        }
        #endif
    }
}

/// "Ana → You": both faces, then "Ana owes you $162.30", "You owe Bruno $98.00" or "Carla owes Bruno $30.00".
private struct PairRow: View {
    let pair: PickPaymentPair
    let currency: GroupCurrency
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                PaymentFaces(from: pair.from, to: pair.to, size: 32)
                Text(verbatim: debt)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.tertiary)
                    .accessibilityHidden(true)
            }
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityHint(Text("Record payment"))
    }

    private var debt: String {
        let amount = currency.format(pair.amountMinor)
        if pair.to?.isMe == true { return String(localized: "\(name(pair.from)) owes you \(amount)") }
        if pair.from?.isMe == true { return String(localized: "You owe \(name(pair.to)) \(amount)") }
        return String(localized: "\(name(pair.from)) owes \(name(pair.to)) \(amount)")
    }

    private func name(_ person: PaymentPerson?) -> String {
        PaymentCopy.name(person?.name)
    }
}
