import Observation
import Shared
import SwiftUI

@Observable
final class DefaultSplitModel {
    private(set) var state: DefaultSplitUiState

    @ObservationIgnored private let holder: ViewModelHolder<DefaultSplitViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteDefaultSplit) {
        holder = BlitzSplitApple.shared.createDefaultSplitViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! DefaultSplitUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
    }

    /// The state as the ViewModel holds it right now, before the watcher brings it to `state`: a field reads what the
    /// ViewModel kept of a keystroke back at once.
    var current: DefaultSplitUiState {
        (holder.viewModel.uiState.value as? DefaultSplitUiState) ?? state
    }

    func send(_ event: any DefaultSplitUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    func percent(of memberId: String) -> Binding<String> {
        Binding(
            get: { self.current.people.first { $0.id == memberId }?.percentText ?? "" },
            set: { self.send(DefaultSplitUiEventOnPercentChanged(memberId: memberId, text: $0)) },
        )
    }

    func income(of memberId: String) -> Binding<String> {
        Binding(
            get: { self.current.people.first { $0.id == memberId }?.incomeText ?? "" },
            set: { self.send(DefaultSplitUiEventOnIncomeChanged(memberId: memberId, text: $0)) },
        )
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Default split" (6.7), pushed inside Edit group's sheet on iPhone, iPad and Mac, with Save (✓) in the bar. Lying
/// down, on an iPad lying down and in the Mac's sheet: the ways to split on the left, the people and the example bill
/// on the right.
struct DefaultSplitView: View {
    let route: RouteDefaultSplit

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: DefaultSplitModel?
    @State private var layout = ScreenLayout()

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                if layout.usesTwoColumns {
                    HStack(alignment: .top, spacing: 0) {
                        Form { DefaultSplitOverview(model: model) }
                        Form { DefaultSplitPeople(model: model) }
                    }
                    .formStyle(.grouped)
                } else {
                    Form {
                        DefaultSplitOverview(model: model)
                        DefaultSplitPeople(model: model)
                    }
                    .formStyle(.grouped)
                }
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .measuringScreenLayout($layout)
        .navigationTitle("Default split")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #else
        .navigationBarBackButtonHidden(true)
        #endif
        .toolbar {
            #if os(macOS)
            ToolbarItem(placement: .cancellationAction) {
                Button("Back") {
                    model?.send(DefaultSplitUiEventOnBackClicked.shared)
                }
                .disabled(model?.state.isSaving == true)
            }
            #endif
            ToolbarItem(placement: .confirmationAction) {
                Button("Save", systemImage: "checkmark") {
                    model?.send(DefaultSplitUiEventOnSaveClicked.shared)
                }
                .buttonStyle(.glassProminent)
                .disabled(model?.state.canSave != true)
            }
        }
        #if os(macOS)
        .frame(minWidth: 640, idealWidth: 760, minHeight: 520, idealHeight: 640)
        #endif
        .onAppear {
            if model == nil { model = DefaultSplitModel(route: route) }
        }
    }
}

/// The group it is for, the notices and the ways to split.
private struct DefaultSplitOverview: View {
    let model: DefaultSplitModel

    private var state: DefaultSplitUiState {
        model.state
    }

    var body: some View {
        Section {
            VStack(alignment: .leading, spacing: 2) {
                #if os(macOS)
                // A Mac sheet has no title bar, so the title heads the form
                Text("Default split").font(.title2.bold())
                #endif
                Text("\(state.groupName) · for new expenses")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .listRowBackground(Color.clear)
        if state.showsSaveError {
            Section {
                Label("Couldn’t save the split. Try again.", systemImage: "exclamationmark.circle")
                    .foregroundStyle(Palette.negative)
            }
        }
        if !state.joinedPeople.isEmpty {
            Section {
                Label(
                    DefaultSplitCopy.joined(state.joinedPeople.map(\.name), mode: state.mode),
                    systemImage: "person.badge.plus",
                )
                .foregroundStyle(Palette.onInfoContainer)
                .listRowBackground(Palette.infoContainer)
            }
        }
        Section("How to split") {
            ForEach(state.modes, id: \.self) { mode in
                Button {
                    model.send(DefaultSplitUiEventOnModeSelected(mode: mode))
                } label: {
                    HStack(spacing: 12) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(verbatim: DefaultSplitCopy.modeLabel(mode))
                                .foregroundStyle(.primary)
                            Text(verbatim: DefaultSplitCopy.modeHint(mode))
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                        Spacer(minLength: 8)
                        if mode == state.mode {
                            Image(systemName: "checkmark")
                                .fontWeight(.semibold)
                                .foregroundStyle(Palette.accent)
                        }
                    }
                    .contentShape(.rect)
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(mode == state.mode ? .isSelected : [])
            }
        }
    }
}

/// Each person's part, the example bill and what the screen saves.
private struct DefaultSplitPeople: View {
    let model: DefaultSplitModel

    private var state: DefaultSplitUiState {
        model.state
    }

    var body: some View {
        Section {
            ForEach(state.rows, id: \.person.id) { row in
                DefaultSplitPersonRow(row: row, model: model)
            }
        } header: {
            Text(verbatim: DefaultSplitCopy.listLabel(state.mode))
        } footer: {
            if state.mode == .income {
                Label("Incomes stay on this \(DeviceName.current) only. The group sees only the percentages.", systemImage: "lock")
            }
        }
        Section {
            DefaultSplitExample(state: state)
                .listRowBackground(state.showsPercentError ? Palette.errorContainer : Palette.card)
        } footer: {
            if state.showsOfflineNotice {
                Text("No internet: the new split holds on this \(DeviceName.current) now and reaches the others once it reconnects.")
            } else {
                Text("Counts for new expenses. Each expense can change it on the spot. Expenses already added don't change.")
            }
        }
    }
}

private struct DefaultSplitPersonRow: View {
    let row: DefaultSplitRow
    let model: DefaultSplitModel

    private var person: DefaultSplitPerson {
        row.person
    }

    private var mode: DefaultSplitMode {
        model.state.mode
    }

    var body: some View {
        if mode == .equal {
            Button {
                model.send(DefaultSplitUiEventOnPersonToggled(memberId: person.id))
            } label: {
                content
            }
            .buttonStyle(.plain)
            .accessibilityAddTraits(person.isIncluded ? .isSelected : [])
        } else {
            content
        }
    }

    private var name: String {
        person.isMe ? String(localized: "\(person.name) (you)") : person.name
    }

    private var content: some View {
        HStack(spacing: 12) {
            if mode == .equal {
                Image(systemName: person.isIncluded ? "checkmark.circle.fill" : "circle")
                    .font(.title2)
                    .foregroundStyle(person.isIncluded ? AnyShapeStyle(Palette.accent) : AnyShapeStyle(.tertiary))
                    .accessibilityHidden(true)
            }
            AvatarPhoto(initial: person.initials, color: person.avatarColor, photoUrl: nil, size: 32)
            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: name)
                Text(verbatim: subtitle)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .monospacedDigit()
            }
            Spacer(minLength: 8)
            trailing
        }
        .contentShape(.rect)
    }

    /// "1 of 6 · 16.7%", "75% of each bill", "R$ 75,00 of each R$ 100,00", "Income per month · 75% of each bill".
    private var subtitle: String {
        let state = model.state
        let share = Double(row.basisPoints) / 10000
        let text: String = switch mode {
        case .shares:
            String(localized: "\(share.formatted(.percent.precision(.fractionLength(0)))) of each bill")
        case .percent:
            String(
                localized: "\(state.currency.format(row.exampleMinor?.int64Value ?? 0)) of each \(state.currency.format(state.exampleTotalMinor))",
            )
        case .income:
            String(localized: "Income per month · \(SplitBillCopy.percent(row.weight)) of each bill")
        default:
            row.weight > 0
                ? String(localized: "1 of \(Int(state.includedCount)) · \(share.formatted(.percent.precision(.fractionLength(1))))")
                : String(localized: "Out of the split by default")
        }
        return person.isNew ? String(localized: "\(text) · just joined") : text
    }

    @ViewBuilder
    private var trailing: some View {
        switch mode {
        case .shares:
            HStack(spacing: 8) {
                Text(verbatim: "\(person.parts)")
                    .monospacedDigit()
                    .frame(minWidth: 24, alignment: .trailing)
                Stepper {
                    Text("Shares of \(person.name)")
                } onIncrement: {
                    model.send(DefaultSplitUiEventOnPartsIncreased(memberId: person.id))
                } onDecrement: {
                    model.send(DefaultSplitUiEventOnPartsDecreased(memberId: person.id))
                }
                .labelsHidden()
            }
        case .percent:
            HStack(spacing: 4) {
                numberField(model.percent(of: person.id), prompt: "0", width: 52) {
                    Text("Percentage of \(person.name)")
                }
                Text(verbatim: "%")
                    .foregroundStyle(.secondary)
                    .accessibilityHidden(true)
            }
        case .income:
            HStack(spacing: 4) {
                Text(verbatim: model.state.currency.symbol)
                    .foregroundStyle(.secondary)
                    .accessibilityHidden(true)
                numberField(model.income(of: person.id), prompt: model.state.currency.zeroPlaceholder, width: 110) {
                    Text("Income of \(person.name)")
                }
            }
        default:
            EmptyView()
        }
    }

    private func numberField(
        _ text: Binding<String>,
        prompt: String,
        width: CGFloat,
        @ViewBuilder label: () -> Text,
    ) -> some View {
        let isError = model.state.showsPercentError && mode == .percent
        return TextField(text: text, prompt: Text(verbatim: prompt), label: label)
            #if os(iOS)
            .keyboardType(mode == .income ? .decimalPad : .numberPad)
            #endif
            .inlineValueFieldStyle(width: width, isError: isError)
    }
}

/// "On a R$ 100,00 bill:" with each one's part and the bar in their colors, or what is missing while the
/// percentages don't add up to 100.
private struct DefaultSplitExample: View {
    let state: DefaultSplitUiState

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label {
                Text(verbatim: title)
                    .font(.headline)
                    .monospacedDigit()
            } icon: {
                if state.showsPercentError {
                    Image(systemName: "exclamationmark.circle")
                }
            }
            .labelStyle(.titleAndIcon)
            ShareBar(state: state)
                .frame(height: 8)
                .accessibilityHidden(true)
            Text(verbatim: detail)
                .font(.footnote)
                .foregroundStyle(state.showsPercentError ? AnyShapeStyle(Palette.onErrorContainer) : AnyShapeStyle(.secondary))
                .monospacedDigit()
        }
        .foregroundStyle(state.showsPercentError ? Palette.onErrorContainer : Color.primary)
        .accessibilityElement(children: .combine)
    }

    private var title: String {
        let currency = state.currency
        if state.showsPercentError {
            let sum = SplitBillCopy.percent(state.percentSum)
            let gap = SplitBillCopy.percent(abs(state.remainingPercent))
            return state.remainingPercent > 0
                ? String(localized: "The sum is \(sum). \(gap) missing.")
                : String(localized: "The sum is \(sum). \(gap) over.")
        }
        let total = currency.format(state.exampleTotalMinor)
        if state.mode == .equal, state.includedCount > 0 {
            return String(localized: "On a \(total) bill: \(currency.format(state.exampleEachMinor)) each")
        }
        return String(localized: "On a \(total) bill:")
    }

    private var detail: String {
        if state.showsPercentError {
            return String(localized: "Adjust the percentages until they add up to \(SplitBillCopy.percent(100)) to save.")
        }
        if state.mode == .equal {
            return String(localized: "Whoever isn't in an expense leaves it when splitting.")
        }
        return state.rows
            .filter { $0.weight > 0 }
            .map { "\($0.person.name) \(state.currency.format($0.exampleMinor?.int64Value ?? 0))" }
            .joined(separator: " · ")
    }
}

/// Each one's slice of every bill in their avatar's color; the part still missing stays empty.
private struct ShareBar: View {
    let state: DefaultSplitUiState

    var body: some View {
        GeometryReader { proxy in
            let rows = state.rows.filter { $0.basisPoints > 0 }
            let missing = state.showsPercentError && state.remainingPercent > 0 ? Double(state.remainingPercent) * 100 : 0
            let total = Double(rows.reduce(0) { $0 + $1.basisPoints }) + missing
            let gaps = CGFloat(max(rows.count - (missing > 0 ? 0 : 1), 0)) * 2
            let width = max(proxy.size.width - gaps, 0)
            HStack(spacing: 2) {
                ForEach(rows, id: \.person.id) { row in
                    RoundedRectangle(cornerRadius: 4)
                        .fill(row.person.avatarColor.container)
                        .frame(width: total > 0 ? width * Double(row.basisPoints) / total : 0)
                }
                if missing > 0 {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(.fill.tertiary)
                        .frame(width: total > 0 ? width * missing / total : 0)
                }
            }
        }
    }
}
