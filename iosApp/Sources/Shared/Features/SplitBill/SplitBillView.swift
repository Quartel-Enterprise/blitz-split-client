import Observation
import Shared
import SwiftUI

@Observable
final class SplitBillModel {
    private(set) var state: SplitBillUiState
    /// The split being edited, kept after "Done" or "Back" so "Split" still draws while it slides away.
    private(set) var shownEditor: SplitEditor?
    /// "Several people paid" as last open, kept after "Done" or "Close" so it still draws while the sheet slides
    /// away; "Who paid" opening again forgets it.
    private(set) var shownPayerAmounts: PayerAmounts?
    private(set) var saveErrorCount = 0
    private(set) var deleteErrorCount = 0

    @ObservationIgnored private let holder: ViewModelHolder<SplitBillViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteSplitBill) {
        holder = BlitzSplitApple.shared.createSplitBillViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! SplitBillUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
            if let editor = state.splitEditor { self?.shownEditor = editor }
            if let amounts = state.payerAmounts {
                self?.shownPayerAmounts = amounts
            } else if state.payerSelection != nil {
                self?.shownPayerAmounts = nil
            }
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            switch action {
            case is SplitBillUiActionShowSaveError: self?.saveErrorCount += 1
            case is SplitBillUiActionShowDeleteError: self?.deleteErrorCount += 1
            default: break
            }
        })
    }

    /// The state as the ViewModel holds it right now, before the watcher brings it to `state`: a field reads its
    /// filtered text back at once, so a letter typed in an amount never shows for a frame.
    var current: SplitBillUiState {
        (holder.viewModel.uiState.value as? SplitBillUiState) ?? state
    }

    func send(_ event: any SplitBillUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    /// "Delete this bill?" closed without "Delete"; the ViewModel keeps it while the bill is being deleted.
    func dismissDeletion() {
        if current.deletion != nil { send(SplitBillUiEventOnDeleteDismissed.shared) }
    }

    var amount: Binding<String> {
        Binding(
            get: { self.current.amountText },
            set: { self.send(SplitBillUiEventOnAmountChanged(text: $0)) },
        )
    }

    var billDescription: Binding<String> {
        Binding(
            get: { self.current.description_ },
            set: { self.send(SplitBillUiEventOnDescriptionChanged(text: $0)) },
        )
    }

    var spentAt: Binding<Date> {
        Binding(
            get: { ExpenseFormat.date(of: self.state.spentAt) },
            set: { self.send(SplitBillUiEventOnSpentAtChanged(epochMillis: ExpenseFormat.millis(of: $0))) },
        )
    }

    /// The latest "When" a bill takes: a bill is spent by now.
    var latestSpentAt: Date {
        ExpenseFormat.date(of: state.latestSpentAt)
    }

    /// A row's field in "Split", by amount or percentage.
    func value(of memberId: String) -> Binding<String> {
        Binding(
            get: { self.current.splitEditor?.rows.first { $0.person.id == memberId }?.valueText ?? "" },
            set: { self.send(SplitBillUiEventOnPersonValueChanged(memberId: memberId, text: $0)) },
        )
    }

    /// "Several people paid", from "Who paid" in one go: the pop-up and the Mac's button open it straight away.
    func openSeveralPayers() {
        send(SplitBillUiEventOnPayerClicked.shared)
        send(SplitBillUiEventOnSeveralPayersClicked.shared)
    }

    /// What one person paid, in "Several people paid".
    func payerAmount(of memberId: String) -> Binding<String> {
        Binding(
            get: { self.current.payerAmounts?.rows.first { $0.memberId == memberId }?.amountText ?? "" },
            set: { self.send(SplitBillUiEventOnPayerAmountChanged(memberId: memberId, text: $0)) },
        )
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Split a bill", or "Edit bill" for a bill that exists: a sheet over the group (over the bill on iPad and Mac) on
/// iPhone, iPad and Mac. On iPhone and iPad "Who paid" is a sheet of its own and "Split" is pushed inside this one;
/// on the Mac "Who paid" is a pop-up button and "Split" takes the sheet's place, as the canvas draws them. Editing
/// adds who changed the bill meanwhile on top and "Delete bill" under the form.
struct SplitBillView: View {
    let route: RouteSplitBill

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: SplitBillModel?

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                #if os(macOS)
                MacSplitBill(model: model)
                #else
                PhoneSplitBill(model: model)
                #endif
            } else {
                Palette.groupedBackground.ignoresSafeArea()
                    #if os(macOS)
                    .frame(width: 520, height: 480)
                    #endif
            }
        }
        .interactiveDismissDisabled(model?.state.isSaving == true || model?.state.isDeleting == true)
        .billDeletionAlert(
            model?.state.deletion,
            currency: model?.state.currency,
            isDeleting: model?.state.isDeleting == true,
            onConfirm: { model?.send(SplitBillUiEventOnDeleteConfirmed.shared) },
            onDismiss: { model?.dismissDeletion() },
        )
        .glassToast("Couldn't save the bill. Try again.", trigger: model?.saveErrorCount ?? 0)
        .glassToast("Couldn't delete the bill. Try again.", trigger: model?.deleteErrorCount ?? 0)
        .onAppear {
            if model == nil { model = SplitBillModel(route: route) }
        }
    }
}

/// Under the amount: what's wrong with it or what the bill means for this device's user, then what changing it does
/// to a split by amount being edited.
private struct AmountHelp: View {
    let state: SplitBillUiState

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            if state.showsAmountError {
                Text("Enter how much the bill was.")
                    .foregroundStyle(Palette.negative)
            } else if let refund = state.refundMinor {
                Text("You paid and get \(state.currency.format(refund.int64Value)) back.")
            } else if let mine = state.myShareMinor {
                Text("Your share: \(state.currency.format(mine.int64Value))")
            }
            if state.showsProportionalAmountHint {
                Text("Changing the total redoes a by-amount split proportionally.")
            }
        }
    }
}

/// "No internet: the bill is saved on this device…", with the orange dot of what is still queued.
private struct OfflineBillNote: View {
    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Circle()
                .fill(.orange)
                .frame(width: 8, height: 8)
                .accessibilityHidden(true)
            Text("No internet: the bill is saved on this device and counts in the balances now. It syncs when you reconnect.")
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

/// "Ana changed this bill 2 minutes ago. The last saved edit wins: …", kept up to date as the minutes pass.
private struct ConflictText: View {
    let conflict: BillConflict
    /// The Mac's alert sets the first sentence in bold, as the canvas draws it.
    var boldsLead = false

    var body: some View {
        TimelineView(.everyMinute) { context in
            let text = BillCopy.conflict(conflict, now: context.date)
            Text("\(Text(verbatim: text.lead).fontWeight(boldsLead ? .semibold : .regular)) \(Text(verbatim: text.detail))")
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

/// Up to four faces of who is in the split.
private struct SplitFaces: View {
    let people: [BillPerson]
    var size: CGFloat = 22

    var body: some View {
        HStack(spacing: -size * 0.3) {
            ForEach(people.prefix(4), id: \.id) { person in
                AvatarPhoto(initial: person.initials, color: person.avatarColor, photoUrl: nil, size: size)
                    .overlay(Circle().strokeBorder(Palette.card, lineWidth: 1.5))
            }
        }
        .accessibilityHidden(true)
    }
}

#if os(iOS)
private struct PhoneSplitBill: View {
    let model: SplitBillModel

    @State private var layout = ScreenLayout()
    @FocusState private var focusedField: Field?

    private enum Field {
        case amount
        case description
    }

    private var state: SplitBillUiState {
        model.state
    }

    var body: some View {
        Group {
            // Lying down and on iPad: what to read on the left, the details on the right
            if layout.usesTwoColumns {
                HStack(alignment: .top, spacing: 0) {
                    Form { whatSections }
                    Form {
                        detailsSection
                        deleteSection
                    }
                }
            } else {
                Form {
                    whatSections
                    detailsSection
                    deleteSection
                }
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .measuringScreenLayout($layout)
        .navigationTitle(state.isEditing ? "Edit bill" : "Split a bill")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Close", systemImage: "xmark", role: .cancel) {
                    model.send(SplitBillUiEventOnCloseClicked.shared)
                }
                .disabled(state.isSaving)
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Save", systemImage: "checkmark") {
                    model.send(SplitBillUiEventOnSaveClicked.shared)
                }
                .buttonStyle(.glassProminent)
                .disabled(!state.canSave)
            }
        }
        .navigationDestination(isPresented: splitShown) {
            PhoneSplitEditor(model: model)
        }
        .sheet(isPresented: payerShown) {
            // Why: "Several people" swaps the sheet's content, since a second sheet can't open while one closes.
            if model.state.payerSelection == nil, model.shownPayerAmounts != nil {
                SeveralPayersSheet(model: model)
            } else {
                PayerSheet(model: model)
            }
        }
        .onAppear {
            if state.amountText.isEmpty { focusedField = .amount }
        }
    }

    @ViewBuilder
    private var whatSections: some View {
        if let conflict = state.conflict {
            Section {
                ConflictText(conflict: conflict)
                    .font(.subheadline)
            }
            .listRowBackground(Palette.accent.opacity(0.12))
        }
        Section {
            AmountField(currency: state.currency, text: model.amount, label: "Amount")
                .focused($focusedField, equals: .amount)
        } header: {
            Text("Amount")
        } footer: {
            AmountHelp(state: state)
        }
        Section {
            TextField("What was it? E.g. Groceries", text: model.billDescription)
                .textInputAutocapitalization(.sentences)
                .submitLabel(.done)
                .focused($focusedField, equals: .description)
        } header: {
            Text("Description")
        } footer: {
            if state.showsDescriptionError {
                Text("Say what it was, for example Groceries.")
                    .foregroundStyle(Palette.negative)
            }
        }
    }

    private var detailsSection: some View {
        Section {
            LabeledContent("Group") {
                HStack(spacing: 8) {
                    GroupAvatar(initials: state.groupInitials, color: state.groupColor, size: 24)
                    Text(verbatim: state.groupName)
                        .lineLimit(1)
                }
            }
            Button {
                model.send(SplitBillUiEventOnPayerClicked.shared)
            } label: {
                HStack(spacing: 8) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Who paid")
                            .foregroundStyle(.primary)
                        if let several = state.severalPayers, several.showsMismatch {
                            Text(verbatim: SplitBillCopy.payersMismatch(several))
                                .font(.footnote)
                                .foregroundStyle(Palette.negative)
                        }
                    }
                    Spacer()
                    if let several = state.severalPayers {
                        SplitFaces(people: several.paying.compactMap(\.person))
                        Text(verbatim: ExpenseFormat.payerNames(SplitBillCopy.payerNames(several)))
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                    } else if let payer = state.payer {
                        AvatarPhoto(initial: payer.initials, color: payer.avatarColor, photoUrl: nil, size: 24)
                        Text(verbatim: SplitBillCopy.name(payer))
                            .foregroundStyle(.secondary)
                    } else if state.isPayerFormerMember {
                        // An edited bill keeps whoever paid it after they left the group
                        Text("Former member")
                            .foregroundStyle(.secondary)
                    }
                    DisclosureIndicator()
                }
                .contentShape(.rect)
            }
            .buttonStyle(.plain)
            WhenPickerRows(
                millis: state.spentAt,
                relativeDay: state.spentDay,
                selection: model.spentAt,
                latest: model.latestSpentAt,
                onOpen: { model.send(SplitBillUiEventOnSpentAtOpened.shared) },
            )
            Button {
                model.send(SplitBillUiEventOnSplitClicked.shared)
            } label: {
                HStack(spacing: 10) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Split")
                            .foregroundStyle(.primary)
                        Text(verbatim: SplitBillCopy.splitSubtitle(state))
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                    Spacer(minLength: 8)
                    SplitFaces(people: state.split.included)
                    Text(verbatim: SplitBillCopy.modeSummary(state.split.mode))
                        .foregroundStyle(.secondary)
                    DisclosureIndicator()
                }
                .contentShape(.rect)
            }
            .buttonStyle(.plain)
        } header: {
            Text("Details")
        } footer: {
            VStack(alignment: .leading, spacing: 8) {
                if state.showsOfflineNotice {
                    OfflineBillNote()
                }
                if state.showsAnyoneCanEditHint {
                    Text("Anyone in the group can edit or delete it.")
                }
            }
        }
    }

    @ViewBuilder
    private var deleteSection: some View {
        if state.isEditing {
            Section {
                Button(role: .destructive) {
                    model.send(SplitBillUiEventOnDeleteClicked.shared)
                } label: {
                    Text("Delete bill")
                        .frame(maxWidth: .infinity)
                }
                .disabled(!state.canDelete)
            }
        }
    }

    /// Back on "Split" (the button, a swipe) leaves the bill's split as it was.
    private var splitShown: Binding<Bool> {
        Binding(
            get: { model.state.splitEditor != nil },
            set: { shown in
                if !shown, model.current.splitEditor != nil { model.send(SplitBillUiEventOnSplitDismissed.shared) }
            },
        )
    }

    /// Dismissing the sheet (a swipe down), on "Who paid" or "Several people paid", keeps whoever paid before.
    private var payerShown: Binding<Bool> {
        Binding(
            get: { model.state.payerSelection != nil || model.state.payerAmounts != nil },
            set: { shown in
                guard !shown else { return }
                if model.current.payerSelection != nil { model.send(SplitBillUiEventOnPayerDismissed.shared) }
                if model.current.payerAmounts != nil { model.send(SplitBillUiEventOnSeveralPayersDismissed.shared) }
            },
        )
    }
}

private struct DisclosureIndicator: View {
    var body: some View {
        Image(systemName: "chevron.forward")
            .font(.footnote.weight(.semibold))
            .foregroundStyle(.tertiary)
            .accessibilityHidden(true)
    }
}

/// "Who paid": one row per person, the one marked until "Done", and "Several people", which opens their amounts.
private struct PayerSheet: View {
    let model: SplitBillModel

    private var state: SplitBillUiState {
        model.state
    }

    var body: some View {
        NavigationStack {
            List {
                ForEach(state.people, id: \.id) { person in
                    let isSelected = state.payerSelection?.memberId == person.id
                    Button {
                        model.send(SplitBillUiEventOnPayerPicked(memberId: person.id))
                    } label: {
                        HStack(spacing: 12) {
                            AvatarPhoto(initial: person.initials, color: person.avatarColor, photoUrl: nil, size: 32)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(verbatim: SplitBillCopy.name(person))
                                if !person.hasJoined {
                                    Text("Hasn't joined yet")
                                        .font(.footnote)
                                        .foregroundStyle(.secondary)
                                }
                            }
                            Spacer()
                            if isSelected {
                                Image(systemName: "checkmark")
                                    .font(.body.weight(.semibold))
                                    .foregroundStyle(Palette.accent)
                            }
                        }
                        .contentShape(.rect)
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(isSelected ? .isSelected : [])
                }
                // Marked while the bill has several payers and nobody else was picked
                let isSeveralSelected = state.payerSelection != nil && state.payerSelection?.memberId == nil
                Button {
                    model.send(SplitBillUiEventOnSeveralPayersClicked.shared)
                } label: {
                    HStack(spacing: 12) {
                        Image(systemName: "plus")
                            .font(.body.weight(.semibold))
                            .foregroundStyle(.secondary)
                            .frame(width: 32, height: 32)
                            .background(.fill.tertiary, in: .circle)
                            .accessibilityHidden(true)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Several people")
                            Text("Each one says how much they paid")
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        if isSeveralSelected {
                            Image(systemName: "checkmark")
                                .font(.body.weight(.semibold))
                                .foregroundStyle(Palette.accent)
                        } else {
                            DisclosureIndicator()
                        }
                    }
                    .contentShape(.rect)
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(isSeveralSelected ? .isSelected : [])
            }
            .navigationTitle("Who paid")
            .navigationSubtitle(SplitBillCopy.payerSubtitle(state))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", systemImage: "xmark", role: .cancel) {
                        model.send(SplitBillUiEventOnPayerDismissed.shared)
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done", systemImage: "checkmark") {
                        model.send(SplitBillUiEventOnPayerDone.shared)
                    }
                    .buttonStyle(.glassProminent)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}

/// "Several people paid": what each one paid, and whether it adds up to the total; "Done" waits until it does.
private struct SeveralPayersSheet: View {
    let model: SplitBillModel

    var body: some View {
        if let amounts = model.state.payerAmounts ?? model.shownPayerAmounts {
            NavigationStack {
                List {
                    Section {
                        ForEach(amounts.rows, id: \.memberId) { row in
                            HStack(spacing: 12) {
                                PayerAmountFace(row: row, size: 32)
                                Text(verbatim: SplitBillCopy.payerName(row))
                                    .lineLimit(1)
                                Spacer(minLength: 8)
                                PayerAmountField(model: model, row: row, currency: amounts.currency)
                            }
                        }
                    } footer: {
                        if let status = SplitBillCopy.payersStatus(amounts) {
                            Text(verbatim: status)
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(amounts.addsUp ? Palette.accent : Palette.negative)
                        }
                    }
                }
                .navigationTitle("Several people paid")
                .navigationSubtitle(SplitBillCopy.payersLead(amounts))
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Close", systemImage: "xmark", role: .cancel) {
                            model.send(SplitBillUiEventOnSeveralPayersDismissed.shared)
                        }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Done", systemImage: "checkmark") {
                            model.send(SplitBillUiEventOnSeveralPayersDone.shared)
                        }
                        .buttonStyle(.glassProminent)
                        .disabled(!amounts.addsUp)
                    }
                }
            }
            .presentationDetents([.medium, .large])
        }
    }
}
#endif

/// A face in "Several people paid", blank for someone who left the group.
private struct PayerAmountFace: View {
    let row: PayerAmountRow
    let size: CGFloat

    var body: some View {
        if let person = row.person {
            AvatarPhoto(initial: person.initials, color: person.avatarColor, photoUrl: nil, size: size)
        } else {
            Circle()
                .fill(.fill.tertiary)
                .frame(width: size, height: size)
                .accessibilityHidden(true)
        }
    }
}

/// How much one person paid: what is typed goes to the ViewModel, which filters it and hands it back.
private struct PayerAmountField: View {
    let model: SplitBillModel
    let row: PayerAmountRow
    let currency: GroupCurrency

    var body: some View {
        HStack(spacing: 4) {
            Text(verbatim: currency.symbol)
                .foregroundStyle(.secondary)
                .accessibilityHidden(true)
            // "How much Pierre paid" reads better to VoiceOver than "How much You paid"
            TextField(text: model.payerAmount(of: row.memberId), prompt: Text(verbatim: currency.zeroPlaceholder)) {
                Text("How much \(row.person?.name ?? String(localized: "Former member")) paid")
            }
            // A form in columns would draw that label beside the field on macOS
            .labelsHidden()
            .multilineTextAlignment(.trailing)
            .monospacedDigit()
            #if os(iOS)
            .keyboardType(.decimalPad)
            #endif
        }
        #if os(iOS)
        .padding(.horizontal, 10)
        .frame(width: 128, height: 36)
        .background(.fill.tertiary, in: .rect(cornerRadius: 10))
        #else
        .frame(width: 128)
        #endif
    }
}

#if os(macOS)
/// The sheet on the window: a form with its labels on the left, "Who paid" as a pop-up button and "When" as a
/// button that opens the calendar. "Split" takes the sheet's place while it is open.
private struct MacSplitBill: View {
    let model: SplitBillModel

    var body: some View {
        Group {
            if model.state.splitEditor != nil {
                MacSplitEditor(model: model)
            } else {
                MacBillForm(model: model)
            }
        }
        .frame(width: 520)
    }
}

private struct MacBillForm: View {
    let model: SplitBillModel

    @FocusState private var isAmountFocused: Bool

    private var state: SplitBillUiState {
        model.state
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            VStack(alignment: .leading, spacing: 2) {
                Group {
                    if state.isEditing {
                        Text("Edit Bill")
                    } else {
                        Text("Split a Bill")
                    }
                }
                .font(.title2.bold())
                if state.isEditing {
                    Text(verbatim: "\(state.description_) · \(state.groupName)")
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                } else {
                    Text("A new expense in the group. Works without internet.")
                        .foregroundStyle(.secondary)
                }
            }
            if let conflict = state.conflict {
                ConflictText(conflict: conflict, boldsLead: true)
                    .font(.callout)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Palette.accent.opacity(0.12), in: .rect(cornerRadius: 10))
            }
            Form {
                LabeledContent("Group:") {
                    HStack(spacing: 6) {
                        GroupAvatar(initials: state.groupInitials, color: state.groupColor, size: 18)
                        Text(verbatim: state.groupName)
                    }
                }
                LabeledContent("Amount:") {
                    VStack(alignment: .leading, spacing: 4) {
                        AmountField(currency: state.currency, text: model.amount, label: "Amount")
                            .focused($isAmountFocused)
                        AmountHelp(state: state)
                            .font(.callout)
                            .foregroundStyle(.secondary)
                    }
                }
                LabeledContent("Description:") {
                    VStack(alignment: .leading, spacing: 4) {
                        TextField(text: model.billDescription, prompt: Text("What was it? E.g. Groceries")) {
                            Text("Description")
                        }
                        if state.showsDescriptionError {
                            Text("Say what it was, for example Groceries.")
                                .font(.callout)
                                .foregroundStyle(Palette.negative)
                        }
                    }
                }
                Picker("Who paid:", selection: payer) {
                    ForEach(state.people, id: \.id) { person in
                        Text(verbatim: SplitBillCopy.name(person)).tag(person.id)
                    }
                    Divider()
                    // Why: the empty id stands for several payers, the payer id a bill several people paid has.
                    if let several = state.severalPayers {
                        Text(verbatim: ExpenseFormat.payerNames(SplitBillCopy.payerNames(several))).tag("")
                    } else {
                        Text("Several People…").tag("")
                    }
                }
                .fixedSize()
                if let several = state.severalPayers {
                    LabeledContent {
                        HStack(spacing: 8) {
                            Button("Change Amounts…") {
                                model.openSeveralPayers()
                            }
                            if several.showsMismatch {
                                Text(verbatim: SplitBillCopy.payersMismatch(several))
                                    .foregroundStyle(Palette.negative)
                            }
                        }
                    } label: {
                        Text(verbatim: "")
                    }
                }
                MacWhenField(
                    millis: state.spentAt,
                    relativeDay: state.spentDay,
                    selection: model.spentAt,
                    latest: model.latestSpentAt,
                    onOpen: { model.send(SplitBillUiEventOnSpentAtOpened.shared) },
                )
                LabeledContent("Split:") {
                    HStack(alignment: .firstTextBaseline, spacing: 12) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(verbatim: SplitBillCopy.modeSummary(state.split.mode))
                            Text(verbatim: SplitBillCopy.splitSubtitle(state))
                                .font(.callout)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        Button("Change…") {
                            model.send(SplitBillUiEventOnSplitClicked.shared)
                        }
                    }
                }
            }
            .formStyle(.columns)
            if state.showsOfflineNotice {
                OfflineBillNote()
                    .font(.callout)
                    .foregroundStyle(.secondary)
            }
            if state.showsAnyoneCanEditHint {
                Text("Anyone in the group can edit or delete it.")
                    .font(.callout)
                    .foregroundStyle(.secondary)
            }
            HStack(spacing: 8) {
                if state.isEditing {
                    Button("Delete Bill…", role: .destructive) {
                        model.send(SplitBillUiEventOnDeleteClicked.shared)
                    }
                    .foregroundStyle(Palette.negative)
                    .disabled(!state.canDelete)
                }
                Spacer()
                Button("Cancel") {
                    model.send(SplitBillUiEventOnCloseClicked.shared)
                }
                .keyboardShortcut(.cancelAction)
                .disabled(state.isSaving)
                Button("Save") {
                    model.send(SplitBillUiEventOnSaveClicked.shared)
                }
                .buttonStyle(.borderedProminent)
                .keyboardShortcut(.defaultAction)
                .disabled(!state.canSave)
            }
            .controlSize(.large)
        }
        .padding(20)
        .onAppear { isAmountFocused = true }
        .sheet(isPresented: severalPayersShown) {
            MacSeveralPayers(model: model)
        }
    }

    /// The pop-up picks whoever paid in one go: the ViewModel's selection opens, marks and closes at once. "Several
    /// People…" opens their amounts instead.
    private var payer: Binding<String> {
        Binding(
            get: { model.state.payerId },
            set: { memberId in
                if memberId.isEmpty {
                    model.openSeveralPayers()
                    return
                }
                model.send(SplitBillUiEventOnPayerClicked.shared)
                model.send(SplitBillUiEventOnPayerPicked(memberId: memberId))
                model.send(SplitBillUiEventOnPayerDone.shared)
            },
        )
    }

    /// Closing the sheet any way but "Done" keeps whoever paid before.
    private var severalPayersShown: Binding<Bool> {
        Binding(
            get: { model.state.payerAmounts != nil },
            set: { shown in
                if !shown, model.current.payerAmounts != nil {
                    model.send(SplitBillUiEventOnSeveralPayersDismissed.shared)
                }
            },
        )
    }
}

/// "Several People Paid", a sheet on the bill's sheet: one field per person, and whether they add up to the total.
private struct MacSeveralPayers: View {
    let model: SplitBillModel

    var body: some View {
        if let amounts = model.state.payerAmounts ?? model.shownPayerAmounts {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Several People Paid")
                        .font(.title2.bold())
                    Text(verbatim: SplitBillCopy.payersLead(amounts))
                        .foregroundStyle(.secondary)
                }
                Form {
                    ForEach(amounts.rows, id: \.memberId) { row in
                        LabeledContent {
                            PayerAmountField(model: model, row: row, currency: amounts.currency)
                        } label: {
                            HStack(spacing: 6) {
                                PayerAmountFace(row: row, size: 18)
                                Text(verbatim: SplitBillCopy.payerName(row))
                            }
                        }
                    }
                }
                .formStyle(.columns)
                if let status = SplitBillCopy.payersStatus(amounts) {
                    Text(verbatim: status)
                        .font(.callout.weight(.semibold))
                        .foregroundStyle(amounts.addsUp ? Palette.accent : Palette.negative)
                }
                HStack(spacing: 8) {
                    Spacer()
                    Button("Cancel") {
                        model.send(SplitBillUiEventOnSeveralPayersDismissed.shared)
                    }
                    .keyboardShortcut(.cancelAction)
                    Button("Done") {
                        model.send(SplitBillUiEventOnSeveralPayersDone.shared)
                    }
                    .buttonStyle(.borderedProminent)
                    .keyboardShortcut(.defaultAction)
                    .disabled(!amounts.addsUp)
                }
                .controlSize(.large)
            }
            .padding(20)
            .frame(width: 420)
        }
    }
}
#endif
