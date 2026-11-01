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

    isolated deinit {
        holder.clear()
    }
}

/// "Edit group": a sheet over the group on iPhone, iPad and Mac, with Cancel and Save (✓) in the bar. The same
/// fields as "New group": the avatar changes color on a tap, and the currency locks once the group has a bill.
struct EditGroupView: View {
    let route: RouteEditGroup

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: EditGroupModel?

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                EditGroupForm(model: model)
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        .navigationTitle("Edit group")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
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
                .disabled(model?.state.canSave != true)
            }
        }
        .interactiveDismissDisabled(model?.state.isSaving == true)
        #if os(macOS)
        .frame(minWidth: 460, idealWidth: 500, minHeight: 520, idealHeight: 600)
        #endif
        .onAppear {
            if model == nil { model = EditGroupModel(route: route) }
        }
    }
}

private struct EditGroupForm: View {
    let model: EditGroupModel

    private var state: EditGroupUiState {
        model.state
    }

    var body: some View {
        Form {
            #if os(macOS)
            // A Mac sheet has no title bar, so the title heads the form
            Section {
                Text("Edit group").font(.title2.bold())
            }
            .listRowBackground(Color.clear)
            #endif
            if state.showsSaveError {
                Section {
                    Label("Couldn’t save the group. Try again.", systemImage: "exclamationmark.circle")
                        .foregroundStyle(Palette.negative)
                }
            }
            Section {
                VStack(spacing: 8) {
                    Button {
                        model.send(EditGroupUiEventOnColorClicked.shared)
                    } label: {
                        GroupAvatar(initials: state.initials, color: state.color, size: 96)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Change the group's color")
                    Button("Change color") {
                        model.send(EditGroupUiEventOnColorClicked.shared)
                    }
                    .buttonStyle(.borderless)
                }
                .frame(maxWidth: .infinity)
                .listRowBackground(Color.clear)
            }
            Section {
                HStack(spacing: 8) {
                    TextField("Group name", text: nameBinding)
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
            Section {
                Picker("Type", selection: typeBinding) {
                    ForEach(state.types, id: \.self) { type in
                        Text(type.title).tag(type)
                    }
                }
                if state.isCurrencyLocked {
                    LabeledContent("Currency") {
                        HStack(spacing: 6) {
                            Text(verbatim: state.currency.label)
                            Image(systemName: "lock")
                                .accessibilityLabel("Locked")
                        }
                    }
                } else {
                    Picker("Currency", selection: currencyBinding) {
                        ForEach(state.currencies, id: \.self) { currency in
                            Text(verbatim: currency.label).tag(currency)
                        }
                    }
                }
            } footer: {
                VStack(alignment: .leading, spacing: 12) {
                    Text("The currency doesn’t change after the first expense.")
                    if state.showsOfflineNotice {
                        Text("No internet: the change holds on this \(DeviceName.current) now and reaches the others once it reconnects.")
                    } else {
                        Text("Anyone in the group can change the name, the icon and the type.")
                    }
                }
            }
        }
        .formStyle(.grouped)
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
}
