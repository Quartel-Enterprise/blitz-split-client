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
                // The pairs not read yet: never "Nobody owes anything" before them
                ShimmerScreen(isWaitingForInternet: model?.state.isWaitingForInternet == true) {
                    ShimmerLine(width: 160, height: 20)
                    ShimmerList(count: 4)
                }
                .background(Palette.groupedBackground.ignoresSafeArea())
            }
        }
        #if os(macOS)
        .frame(width: 480, height: 520)
        #else
        // Record payment's 540×620 form sheet with an iPad's room, which takes this sheet's place
        .formSheetSizing()
        #endif
        .onAppear {
            if model == nil { model = PickPaymentModel() }
        }
    }
}

private struct PickPaymentList: View {
    let model: PickPaymentModel

    #if os(macOS)
    /// The pair the arrow keys moved to; Return or a double click records it, as a click does.
    @State private var selection: PairChoice?
    #endif

    var body: some View {
        let state = model.state
        list {
            ForEach(state.groups, id: \.self) { group in
                Section {
                    ForEach(group.pairs, id: \.self) { pair in
                        let choice = PairChoice(
                            groupId: group.groupId,
                            fromMemberId: pair.fromMemberId,
                            toMemberId: pair.toMemberId,
                            currency: group.currency,
                        )
                        PairRow(pair: pair, currency: group.currency) { pick(choice) }
                            .tag(choice)
                    }
                } header: {
                    HStack(spacing: 8) {
                        GroupAvatar(initials: group.initials, color: group.color, size: 22)
                            .accessibilityHidden(true)
                        Text(verbatim: group.name)
                        // A group owing in two currencies shows twice: the code tells the two apart
                        if state.groups.count(where: { $0.groupId == group.groupId }) > 1 {
                            Text(verbatim: group.currency.code)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
        }
        #if os(macOS)
        .contextMenu(forSelectionType: PairChoice.self) { _ in
            EmptyView()
        } primaryAction: { choices in
            if let choice = choices.first { pick(choice) }
        }
        #endif
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
            }
            .padding(20)
        }
        #endif
    }
}

private extension PickPaymentList {
    #if os(macOS)
    func list(@ViewBuilder content: () -> some View) -> some View {
        List(selection: $selection, content: content)
            // The arrows work before any click
            .focusedOnAppear()
    }
    #else
    func list(@ViewBuilder content: () -> some View) -> some View {
        List(content: content)
    }
    #endif

    func pick(_ choice: PairChoice) {
        model.send(PickPaymentUiEventOnPairClicked(
            groupId: choice.groupId,
            fromMemberId: choice.fromMemberId,
            toMemberId: choice.toMemberId,
            currency: choice.currency,
        ))
    }
}

/// A pair of a group, which the Mac's list selects by.
private struct PairChoice: Hashable {
    let groupId: String
    let fromMemberId: String
    let toMemberId: String
    let currency: GroupCurrency
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
        if pair.to?.isMe == true { return String(
            localized: "\(name(pair.from)) owes you \(amount)",
            bundle: .appLanguage,
        ) }
        if pair.from?.isMe == true { return String(
            localized: "You owe \(name(pair.to)) \(amount)",
            bundle: .appLanguage,
        ) }
        return String(localized: "\(name(pair.from)) owes \(name(pair.to)) \(amount)", bundle: .appLanguage)
    }

    private func name(_ person: PaymentPerson?) -> String {
        PaymentCopy.name(person?.name)
    }
}
