import Observation
import Shared
import SwiftUI

@Observable
final class EditGroupModel {
    private(set) var state: EditGroupUiState

    @ObservationIgnored private let holder: ViewModelHolder<EditGroupViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteEditGroup) {
        holder = BlitzSplitApple.shared.createEditGroupViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! EditGroupUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
    }

    /// The state as the ViewModel holds it right now, before the watcher brings it to `state`: the name field reads
    /// its capped text back at once, so a 41st character never shows for a frame.
    var current: EditGroupUiState {
        (holder.viewModel.uiState.value as? EditGroupUiState) ?? state
    }

    func send(_ event: any EditGroupUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    func photoPicked(_ data: Data?) {
        guard let data else {
            send(EditGroupUiEventOnPhotoUnreadable.shared)
            return
        }
        send(EditGroupUiEventOnPhotoPicked(source: BlitzSplitApple.shared.toByteArray(data: data)))
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Edit group": a sheet over the group on iPhone, iPad and Mac, with Cancel and Save (✓) in the bar, the canvas' 540 pt
/// form sheet with an iPad's room. The same fields as "New group": the icon is a color or a photo (EditGroupIconSection),
/// and the default currency stays editable, bills or not. "Simplify debts" (13.1) applies apart from Save: its confirmation
/// is a sheet over the form on iPhone and iPad, and takes the form's place on the Mac. An iPhone lying down draws two
/// columns and puts the avatar (its pencil is "Change icon or photo") in one compact row.
struct EditGroupView: View {
    let route: RouteEditGroup

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: EditGroupModel?

    var body: some View {
        Group {
            #if os(macOS)
            // The Mac's confirmation takes the place of the form in the same sheet, as its board draws it
            if let model, let confirmation = model.state.simplifyConfirmation {
                SimplifyDebtsConfirmationView(
                    confirmation: confirmation,
                    isChanging: model.state.isChangingSimplifyDebts,
                    onConfirm: { model.send(EditGroupUiEventOnSimplifyDebtsConfirmed.shared) },
                    onDismiss: { model.send(EditGroupUiEventOnSimplifyDebtsDismissed.shared) },
                )
            } else if let model, model.state.isLoaded {
                EditGroupForm(model: model)
            } else {
                shimmer
            }
            #else
            if let model, model.state.isLoaded {
                EditGroupForm(model: model)
            } else {
                shimmer
            }
            #endif
        }
        .navigationTitle(showsConfirmationInPlace ? "" : String(localized: "Edit group", bundle: .appLanguage))
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            if !showsConfirmationInPlace {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", systemImage: "xmark", role: .cancel) {
                        model?.send(EditGroupUiEventOnCloseClicked.shared)
                    }
                    .disabled(model?.state.isSaving == true)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save", systemImage: "checkmark") {
                        model?.send(EditGroupUiEventOnSaveClicked.shared)
                    }
                    .buttonStyle(.glassProminent)
                    .prominentLabel()
                    .disabled(model?.state.canSave != true)
                }
            }
        }
        .interactiveDismissDisabled(
            model?.state.isSaving == true || model?.state.isChangingSimplifyDebts == true
                || model?.state.hasUnsavedInput == true
        )
        // "Ana turned it off after you" (13.1e), over the form
        .simplifyConflictBanner(
            model?.state.simplifyConflict,
            onActivity: { model?.send(EditGroupUiEventOnSimplifyConflictActivityClicked.shared) },
            onDismiss: { model?.send(EditGroupUiEventOnSimplifyConflictDismissed.shared) },
        )
        #if os(iOS)
        .formSheetSizing()
        #endif
        #if os(macOS)
        .frame(minWidth: 460, idealWidth: 500, minHeight: 400, idealHeight: 560)
        #endif
        .onAppear {
            if model == nil { model = EditGroupModel(route: route) }
        }
    }

    /// The Mac draws the confirmation in the form's place, with its own buttons instead of the bar's.
    private var showsConfirmationInPlace: Bool {
        #if os(macOS)
        model?.state.simplifyConfirmation != nil
        #else
        false
        #endif
    }

    /// The group not read yet: its icon and fields shimmer in place.
    private var shimmer: some View {
        ShimmerScreen {
            ShimmerBlock(width: 88, height: 88, cornerRadius: 28)
                .frame(maxWidth: .infinity)
            ShimmerForm(fieldCount: 3)
        }
        .background(Palette.groupedBackground.ignoresSafeArea())
    }
}

/// One grouped form, or two in an iPhone's sheet lying down, as the boards draw it: the icon and the name, then the
/// type, currency, default split and "Simplify debts" (13.1), each column scrolling on its own.
private struct EditGroupForm: View {
    let model: EditGroupModel

    #if os(iOS)
    @Environment(\.verticalSizeClass) private var verticalSizeClass
    #endif
    /// The sheet's own size, which decides the columns.
    @State private var layout = ScreenLayout()

    private var state: EditGroupUiState {
        model.state
    }

    /// An iPhone lying down, whose sheet is too short for the big avatar over the fields.
    private var usesCompactIcon: Bool {
        #if os(iOS)
        verticalSizeClass == .compact
        #else
        false
        #endif
    }

    private var usesTwoColumns: Bool {
        #if os(iOS)
        layout.usesTwoColumns
        #else
        false
        #endif
    }

    var body: some View {
        Group {
            if usesTwoColumns {
                TwoColumnsLayout(spacing: 8) {
                    Form { groupSections }
                        .formStyle(.grouped)
                } second: {
                    Form { settingsSections }
                        .formStyle(.grouped)
                }
            } else {
                Form {
                    groupSections
                    settingsSections
                }
                .formStyle(.grouped)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.groupedBackground)
        .measuringScreenLayout($layout)
        .photoCrop(state.photoCrop, frame: .roundedSquare) { model.send(EditGroupUiEventOnPhotoCrop(event: $0)) }
        #if os(iOS)
        // "Simplify debts?" over the form; the Mac draws it in the form's place (EditGroupView)
        .sheet(isPresented: simplifyConfirmationShown) {
            if let confirmation = state.simplifyConfirmation {
                SimplifyDebtsConfirmationView(
                    confirmation: confirmation,
                    isChanging: state.isChangingSimplifyDebts,
                    onConfirm: { model.send(EditGroupUiEventOnSimplifyDebtsConfirmed.shared) },
                    onDismiss: { model.send(EditGroupUiEventOnSimplifyDebtsDismissed.shared) },
                )
                .presentationDetents(confirmation.people.isEmpty ? [.medium, .large] : [.large])
                // An iPhone Duo open: in the half before the fold, never over it
                .foldSheetPlacement()
                .presentationDragIndicator(.visible)
                .interactiveDismissDisabled(state.isChangingSimplifyDebts)
            }
        }
        #endif
    }

    /// The error, the avatar and the name.
    @ViewBuilder
    private var groupSections: some View {
        if state.showsSaveError {
            Section {
                Label("Couldn’t save the group. Try again.", systemImage: "exclamationmark.circle")
                    .foregroundStyle(Palette.negative)
            }
        }
        Section {
            EditGroupIconSection(model: model, usesCompactIcon: usesCompactIcon)
                .listRowBackground(Color.clear)
        }
        Section {
            HStack(spacing: 8) {
                // The prompt, not a label: the section's header already says "Group name", which the Mac's grouped
                // form would repeat on the field's left, the name pushed to its right
                TextField("Group name", text: nameBinding.firstLetterCapitalized, prompt: Text("Group name"))
                    .labelsHidden()
                    .capitalizesFirstLetter()
                    #if os(macOS)
                    .multilineTextAlignment(.leading)
                    #endif
                    .accessibilityHint(state.showsNameError ? Text(nameErrorText) : Text(""))
                Text(verbatim: "\(state.nameLength)/\(state.maxNameLength)")
                    .font(.subheadline)
                    .monospacedDigit()
                    .foregroundStyle(.secondary)
            }
        } header: {
            Text("Group name")
        } footer: {
            if state.showsNameError {
                Text(nameErrorText)
                    .fontWeight(.medium)
                    .foregroundStyle(Palette.negative)
            } else {
                Text("Everyone in the group sees the new name.")
            }
        }
    }

    /// The type, the currency, the default split, and "Payments" with "Simplify debts".
    @ViewBuilder
    private var settingsSections: some View {
        Section {
            Picker("Type", selection: typeBinding) {
                ForEach(state.types, id: \.self) { type in
                    Text(type.title).tag(type)
                }
            }
            Picker("Default currency", selection: currencyBinding) {
                CurrencyOptions(currencies: state.currencies)
            }
            // Pushes "Default split" inside this sheet, which keeps what is typed here
            Button {
                model.send(EditGroupUiEventOnDefaultSplitClicked.shared)
            } label: {
                HStack(spacing: 8) {
                    LabeledContent("Default split") {
                        Text(verbatim: DefaultSplitCopy.modeLabel(state.defaultSplitMode))
                    }
                    Image(systemName: "chevron.forward")
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(.tertiary)
                        .accessibilityHidden(true)
                }
                .contentShape(.rect)
            }
            .buttonStyle(.plain)
            .disabled(state.isSaving)
            .accessibilityHint("How new expenses split")
        } footer: {
            VStack(alignment: .leading, spacing: 12) {
                Text("New bills start in this currency; each bill can use another.")
                if state.showsOfflineNotice {
                    Text(
                        "No internet: the change holds on this \(DeviceName.current) now and reaches the others once it reconnects. A new photo goes up with it.",
                    )
                }
            }
        }
        EditGroupPaymentsSection(model: model)
    }

    private var nameErrorText: LocalizedStringKey {
        "Give the group a name, up to 40 characters."
    }

    private var nameBinding: Binding<String> {
        Binding(
            get: { model.current.name },
            set: { model.send(EditGroupUiEventOnNameChanged(name: $0)) },
        )
    }

    private var typeBinding: Binding<GroupType> {
        Binding(
            get: { model.state.type },
            set: { model.send(EditGroupUiEventOnTypeSelected(type: $0)) },
        )
    }

    private var currencyBinding: Binding<GroupCurrency> {
        Binding(
            get: { model.state.currency },
            set: { model.send(EditGroupUiEventOnCurrencySelected(currency: $0)) },
        )
    }

    /// Closing the confirmation any other way than its buttons (a swipe down) is "Not now".
    private var simplifyConfirmationShown: Binding<Bool> {
        Binding(
            get: { model.state.simplifyConfirmation != nil },
            set: { shown in
                if !shown, model.current.simplifyConfirmation != nil {
                    model.send(EditGroupUiEventOnSimplifyDebtsDismissed.shared)
                }
            },
        )
    }
}

/// "Payments" (13.1): "Simplify debts" with its switch, which only opens the confirmation and never flips by itself,
/// the line saying how it stands (the amber dot while it waits to go up), "How it works" and who may change it. It
/// applies at once, apart from Save. The Mac's switch is the small one of its forms.
private struct EditGroupPaymentsSection: View {
    let model: EditGroupModel

    private var state: EditGroupUiState {
        model.state
    }

    private var isAvailable: Bool {
        state.simplifyStatus != .needsThirdPerson
    }

    var body: some View {
        Section {
            HStack(alignment: .top, spacing: 12) {
                SimplifyDebtsTile(isEnabled: isAvailable)
                    .padding(.top, 1)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Simplify debts")
                        .foregroundStyle(isAvailable ? .primary : .secondary)
                    HStack(alignment: .firstTextBaseline, spacing: 6) {
                        if state.simplifyStatus == .pending {
                            PendingDot()
                        }
                        Text(verbatim: SimplifyDebtsCopy.status(state))
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityElement(children: .combine)
                .accessibilityHidden(true)
                Toggle(isOn: switchBinding) {
                    Text("Simplify debts")
                }
                .labelsHidden()
                .toggleStyle(.switch)
                #if os(macOS)
                .controlSize(.small)
                #endif
                .disabled(!state.canChangeSimplifyDebts)
                .accessibilityLabel(Text("Simplify debts"))
                .accessibilityHint(Text(verbatim: SimplifyDebtsCopy.status(state)))
            }
            Button("How it works") {
                model.send(EditGroupUiEventOnSimplifyDebtsInfoClicked.shared)
            }
            .disabled(!state.isLoaded)
        } header: {
            Text("Payments")
        } footer: {
            Text(verbatim: SimplifyDebtsCopy.note(state))
        }
    }

    /// Reads the group's setting; a tap only asks for the confirmation, so the switch stays as it was until it applies.
    private var switchBinding: Binding<Bool> {
        Binding(
            get: { model.state.simplifiesDebts },
            set: { _ in model.send(EditGroupUiEventOnSimplifyDebtsClicked.shared) },
        )
    }
}
