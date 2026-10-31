import Observation
import Shared
import SwiftUI

@Observable
final class RegisterPaymentModel {
    private(set) var state: RegisterPaymentUiState
    private(set) var saveErrorCount = 0

    @ObservationIgnored private let holder: ViewModelHolder<RegisterPaymentViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteRegisterPayment) {
        holder = BlitzSplitApple.shared.createRegisterPaymentViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! RegisterPaymentUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            if action is RegisterPaymentUiActionShowSaveError { self?.saveErrorCount += 1 }
        })
    }

    /// The state as the ViewModel holds it right now, before the watcher brings it to `state`: the field reads its
    /// filtered text back at once, so a letter typed in the amount never shows for a frame.
    var current: RegisterPaymentUiState {
        (holder.viewModel.uiState.value as? RegisterPaymentUiState) ?? state
    }

    func send(_ event: any RegisterPaymentUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    var amount: Binding<String> {
        Binding(
            get: { self.current.amountText },
            set: { self.send(RegisterPaymentUiEventOnAmountChanged(text: $0)) },
        )
    }

    var method: Binding<SettlementMethod> {
        Binding(
            get: { self.state.method },
            set: { self.send(RegisterPaymentUiEventOnMethodPicked(method: $0)) },
        )
    }

    var paidAt: Binding<Date> {
        Binding(
            get: { ExpenseFormat.date(of: self.state.paidAt) },
            set: { self.send(RegisterPaymentUiEventOnPaidAtChanged(epochMillis: ExpenseFormat.millis(of: $0))) },
        )
    }

    /// A payment was made by now.
    var latestPaidAt: Date {
        ExpenseFormat.date(of: state.latestPaidAt)
    }

    func openPaidAt() {
        send(RegisterPaymentUiEventOnPaidAtOpened.shared)
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Record payment": a payment from one member to another made outside the app, which settles their debt. A sheet
/// over the group on iPhone, iPad and Mac, as Split a bill is; it closes by itself once saved, or when the debt is gone.
struct RegisterPaymentView: View {
    let route: RouteRegisterPayment

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: RegisterPaymentModel?

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                #if os(macOS)
                MacRegisterPayment(model: model)
                #else
                PhoneRegisterPayment(model: model)
                #endif
            } else {
                Palette.groupedBackground.ignoresSafeArea()
                    #if os(macOS)
                    .frame(width: 480, height: 420)
                    #endif
            }
        }
        .interactiveDismissDisabled(model?.state.isSaving == true)
        .glassToast("Couldn't record the payment. Try again.", trigger: model?.saveErrorCount ?? 0)
        .onAppear {
            if model == nil { model = RegisterPaymentModel(route: route) }
        }
    }
}

/// The words of the sheet, which follow who is recording the payment.
private enum RegisterPaymentCopy {
    static func from(_ state: RegisterPaymentUiState) -> String {
        PaymentCopy.name(state.from?.name)
    }

    static func to(_ state: RegisterPaymentUiState) -> String {
        PaymentCopy.name(state.to?.name)
    }

    /// "Did Ana pay you?", "Did you pay Bruno?" or "Did Diego pay Bruno?".
    static func title(_ state: RegisterPaymentUiState) -> String {
        switch state.kind {
        case .received: String(localized: "Did \(from(state)) pay you?")
        case .paid: String(localized: "Did you pay \(to(state))?")
        default: String(localized: "Did \(from(state)) pay \(to(state))?")
        }
    }

    /// "Ana owes you $162.30", "You owe Bruno $118.00" or "Diego owes Bruno $64.00".
    static func debt(_ state: RegisterPaymentUiState) -> String {
        let debt = state.currency.format(state.debtMinor)
        return switch state.kind {
        case .received: String(localized: "\(from(state)) owes you \(debt)")
        case .paid: String(localized: "You owe \(to(state)) \(debt)")
        default: String(localized: "\(from(state)) owes \(to(state)) \(debt)")
        }
    }

    /// Under the amount: more than the debt, or what is still owed after a partial payment.
    static func amountHelp(_ state: RegisterPaymentUiState) -> (text: String, isError: Bool)? {
        if state.showsOverDebtError {
            return (String(localized: "\(debt(state)). Use up to that amount."), true)
        }
        guard let remaining = state.remainingMinor?.int64Value else { return nil }
        let left = state.currency.format(remaining)
        let text = switch state.kind {
        case .received: String(localized: "\(from(state)) will still owe you \(left)")
        case .paid: String(localized: "You'll still owe \(to(state)) \(left)")
        default: String(localized: "\(from(state)) will still owe \(to(state)) \(left)")
        }
        return (text, false)
    }

    static func confirm(_ state: RegisterPaymentUiState) -> LocalizedStringKey {
        switch state.kind {
        case .received: "Confirm received"
        case .paid: "Confirm payment"
        default: "Record payment"
        }
    }
}

/// Whoever paid, an arrow, whoever received.
private struct PaymentFaces: View {
    let from: PaymentPerson?
    let to: PaymentPerson?
    let size: CGFloat

    var body: some View {
        HStack(spacing: 10) {
            face(from)
            Image(systemName: "arrow.right")
                .font(.headline)
                .foregroundStyle(.secondary)
            face(to)
        }
        .accessibilityHidden(true)
    }

    @ViewBuilder
    private func face(_ person: PaymentPerson?) -> some View {
        if let person {
            AvatarPhoto(initial: person.initials, color: person.avatarColor, photoUrl: nil, size: size)
        } else {
            // Someone no longer in the group, a blank face as Saldos draws them
            Circle()
                .fill(.fill.tertiary)
                .frame(width: size, height: size)
        }
    }
}

/// "How it was paid": Pix, Cash, Transfer or Other.
private struct MethodPicker: View {
    @Binding var selection: SettlementMethod

    var body: some View {
        Picker("How it was paid", selection: $selection) {
            ForEach(SettlementMethod.entries, id: \.self) { method in
                Text(verbatim: PaymentCopy.method(method)).tag(method)
            }
        }
        .pickerStyle(.segmented)
    }
}

/// "It becomes a payment in the group…", who it is recorded for when neither is this person, and the offline note.
private struct PaymentNotes: View {
    let state: RegisterPaymentUiState

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("It becomes a payment in the group: it shows in Expenses and can be undone.")
            if state.kind == .betweenOthers {
                Text("You're recording this for \(RegisterPaymentCopy.from(state)) and \(RegisterPaymentCopy.to(state)).")
            }
            if state.showsOfflineNotice {
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Circle()
                        .fill(.orange)
                        .frame(width: 8, height: 8)
                        .accessibilityHidden(true)
                    Text("No internet: it's saved on this device and syncs when you reconnect.")
                }
            }
        }
        .fixedSize(horizontal: false, vertical: true)
    }
}

/// The amount's help line, red when it goes past the debt.
private struct PaymentAmountHelp: View {
    let state: RegisterPaymentUiState

    var body: some View {
        if let help = RegisterPaymentCopy.amountHelp(state) {
            Text(verbatim: help.text)
                .foregroundStyle(help.isError ? Palette.negative : .secondary)
        }
    }
}

#if os(iOS)
private struct PhoneRegisterPayment: View {
    let model: RegisterPaymentModel

    @FocusState private var isAmountFocused: Bool

    private var state: RegisterPaymentUiState {
        model.state
    }

    var body: some View {
        Form {
            Section {
                VStack(spacing: 8) {
                    PaymentFaces(from: state.from, to: state.to, size: 52)
                    Text(verbatim: RegisterPaymentCopy.title(state))
                        .font(.title2.bold())
                        .multilineTextAlignment(.center)
                        .accessibilityAddTraits(.isHeader)
                    Text(verbatim: RegisterPaymentCopy.debt(state))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
            }
            .listRowBackground(Color.clear)
            Section {
                AmountField(currency: state.currency, text: model.amount, label: "Amount paid")
                    .focused($isAmountFocused)
                Button {
                    model.send(RegisterPaymentUiEventOnFullDebtClicked.shared)
                } label: {
                    Label("The whole debt", systemImage: state.isFullDebt ? "checkmark.circle.fill" : "circle")
                }
                .accessibilityAddTraits(state.isFullDebt ? .isSelected : [])
            } header: {
                Text("Amount paid")
            } footer: {
                PaymentAmountHelp(state: state)
            }
            Section("How it was paid") {
                MethodPicker(selection: model.method)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
            }
            Section {
                WhenPickerRows(
                    millis: state.paidAt,
                    relativeDay: state.paidDay,
                    selection: model.paidAt,
                    latest: model.latestPaidAt,
                    onOpen: model.openPaidAt,
                )
            } footer: {
                PaymentNotes(state: state)
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .safeAreaInset(edge: .bottom) {
            PrimaryButton(
                title: RegisterPaymentCopy.confirm(state),
                isEnabled: state.canConfirm,
                isLoading: state.isSaving,
            ) {
                model.send(RegisterPaymentUiEventOnConfirmClicked.shared)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 8)
        }
        .navigationTitle("Record payment")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel", systemImage: "xmark", role: .cancel) {
                    model.send(RegisterPaymentUiEventOnCloseClicked.shared)
                }
                .disabled(state.isSaving)
            }
        }
    }
}
#endif

#if os(macOS)
/// The sheet on the window: the faces and the question on top, a form with its labels on the left, and the buttons
/// at the bottom, as Split a bill draws them.
private struct MacRegisterPayment: View {
    let model: RegisterPaymentModel

    @FocusState private var isAmountFocused: Bool

    private var state: RegisterPaymentUiState {
        model.state
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 14) {
                PaymentFaces(from: state.from, to: state.to, size: 36)
                VStack(alignment: .leading, spacing: 2) {
                    Text(verbatim: RegisterPaymentCopy.title(state))
                        .font(.title2.bold())
                    Text(verbatim: RegisterPaymentCopy.debt(state))
                        .foregroundStyle(.secondary)
                }
            }
            Form {
                LabeledContent("Amount paid:") {
                    VStack(alignment: .leading, spacing: 4) {
                        HStack(spacing: 8) {
                            AmountField(currency: state.currency, text: model.amount, label: "Amount paid")
                                .focused($isAmountFocused)
                            Button {
                                model.send(RegisterPaymentUiEventOnFullDebtClicked.shared)
                            } label: {
                                Label("The whole debt", systemImage: state.isFullDebt ? "checkmark" : "arrow.uturn.backward")
                            }
                            .disabled(state.isFullDebt)
                        }
                        PaymentAmountHelp(state: state)
                            .font(.callout)
                    }
                }
                LabeledContent("How it was paid:") {
                    MethodPicker(selection: model.method)
                        .labelsHidden()
                        .fixedSize()
                }
                MacWhenField(
                    millis: state.paidAt,
                    relativeDay: state.paidDay,
                    selection: model.paidAt,
                    latest: model.latestPaidAt,
                    onOpen: model.openPaidAt,
                )
            }
            .formStyle(.columns)
            PaymentNotes(state: state)
                .font(.callout)
                .foregroundStyle(.secondary)
            HStack(spacing: 8) {
                Spacer()
                Button("Cancel") {
                    model.send(RegisterPaymentUiEventOnCloseClicked.shared)
                }
                .keyboardShortcut(.cancelAction)
                .disabled(state.isSaving)
                Button(RegisterPaymentCopy.confirm(state)) {
                    model.send(RegisterPaymentUiEventOnConfirmClicked.shared)
                }
                .buttonStyle(.borderedProminent)
                .keyboardShortcut(.defaultAction)
                .disabled(!state.canConfirm)
            }
            .controlSize(.large)
        }
        .padding(20)
        .frame(width: 480)
        .onAppear { isAmountFocused = true }
    }
}
#endif
