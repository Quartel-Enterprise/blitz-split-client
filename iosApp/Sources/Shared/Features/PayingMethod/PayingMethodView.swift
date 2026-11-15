import Observation
import Shared
import SwiftUI

@Observable
final class PayingMethodModel {
    private(set) var state: PayingMethodUiState

    @ObservationIgnored private let holder: ViewModelHolder<PayingMethodViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RoutePayingMethod) {
        holder = BlitzSplitApple.shared.createPayingMethodViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! PayingMethodUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
    }

    /// The ViewModel's state right now, which "New color"'s fields read before the next state reaches Swift.
    var current: PayingMethodUiState {
        holder.viewModel.uiState.value as! PayingMethodUiState
    }

    func send(_ event: any PayingMethodUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    /// The name as typed, capped at what the method takes. Read from the ViewModel right away: the state reaches
    /// Swift a moment after each keystroke, and a field reading the stale name would undo what was typed fast.
    var name: Binding<String> {
        Binding(
            get: { [holder] in (holder.viewModel.uiState.value as? PayingMethodUiState)?.name ?? "" },
            set: { [weak self] name in
                guard let self else { return }
                send(PayingMethodUiEventOnNameChanged(name: String(name.prefix(Int(state.nameMaxLength)))))
            },
        )
    }

    /// The dialog closes itself on any of its buttons, so the close waits for the button's own event to
    /// reach the ViewModel first, and only dismisses a confirmation that is still showing.
    func deleteConfirmationClosed() {
        DispatchQueue.main.async { [weak self] in
            guard let self,
                  (holder.viewModel.uiState.value as? PayingMethodUiState)?.isDeleteConfirmationShown == true
            else { return }
            send(PayingMethodUiEventOnDeleteDismissed.shared)
        }
    }

    isolated deinit {
        holder.clear()
    }
}

/// A payment method of the person's own, new or saved: a sheet on every device (Pierre's call, feedback of November
/// 15, 2026), over the profile or over Split a bill's "Add a payment method", with Cancel and Save. The iPhone's takes
/// the whole height its seven types need, the iPad's is a form sheet and the Mac's a sheet on Settings' window or the
/// bill's. A key, unlike it, is still pushed on an iPhone.
struct PayingMethodView: View {
    let route: RoutePayingMethod

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws, and a ViewModel
    /// made for nothing would still read the methods, and close the screen if the method is gone.
    @State private var model: PayingMethodModel?
    @State private var layout = ScreenLayout()
    /// The window under the sheet, not the sheet: whether this method is a sheet or a pushed screen.
    @Environment(\.windowLayout) private var windowLayout

    private var isSheet: Bool {
        AppRoute.payingMethod(route).presentsAsSheet(isLargeWindow: windowLayout.isLarge)
    }

    private var title: Text {
        model?.state.isEditing ?? (route.methodId != nil)
            ? Text("Edit payment method")
            : Text("New payment method")
    }

    var body: some View {
        SavingFormScreen(
            title: title,
            layout: $layout,
            isSheet: isSheet,
            canSave: model?.state.canSave == true,
            isSaving: model?.state.isSaving == true,
            onCancel: { model?.send(PayingMethodUiEventOnBackClicked.shared) },
            onSave: { model?.send(PayingMethodUiEventOnSaveClicked.shared) },
            isLoaded: model?.state.isLoaded == true,
        ) {
            if let model {
                PayingMethodForm(model: model)
            }
        }
        // A swipe down waits while it saves, and over unsaved input Cancel asks "Discard changes?" instead; one modifier,
        // since an outer one would override SavingFormScreen's
        .interactiveDismissDisabled(model.map { $0.state.isSaving || $0.state.hasUnsavedInput } ?? false)
        #if os(iOS)
        .presentationDetents([.large])
        .formSheetSizing()
        #endif
        .onAppear {
            if model == nil { model = PayingMethodModel(route: route) }
        }
    }
}

/// The type, then the name, a card's color and the default, in one column: a sheet on every device.
private struct PayingMethodForm: View {
    let model: PayingMethodModel

    var body: some View {
        Form {
            TypeSection(model: model)
            DetailSections(model: model)
        }
        .formStyle(.grouped)
    }
}

/// "Type": one row per type the form offers, in its order, each with its tile, the picked one checked.
private struct TypeSection: View {
    let model: PayingMethodModel

    var body: some View {
        let state = model.state
        Section {
            ForEach(state.types, id: \.self) { type in
                let isSelected = type == state.type
                Button {
                    model.send(PayingMethodUiEventOnTypeSelected(type: type))
                } label: {
                    HStack(spacing: 12) {
                        PayingMethodTile(type: type, cardColor: state.cardColor)
                        Text(verbatim: type.label)
                            .foregroundStyle(.primary)
                        Spacer(minLength: 8)
                        if isSelected {
                            Image(systemName: "checkmark")
                                .fontWeight(.semibold)
                                .foregroundStyle(Palette.accent)
                        }
                    }
                    .contentShape(.rect)
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(isSelected ? .isSelected : [])
            }
        } header: {
            Text(LocalizedStringResource("Type (a payment method's)", defaultValue: "Type"))
        }
    }
}

/// The name, a card's color, the default switch and, for a saved method, its deletion.
private struct DetailSections: View {
    let model: PayingMethodModel

    var body: some View {
        let state = model.state
        let nameLabel = state.isNameRequired
            ? Text(LocalizedStringResource("Name (a payment method's)", defaultValue: "Name"))
            : Text("Name (optional)")
        Section {
            KeptTextField(text: model.name, prompt: nameLabel) {
                nameLabel
            }
            #if os(iOS)
            .textInputAutocapitalization(.words)
            #else
            .labelsHidden()
            #endif
            .submitLabel(.done)
            .onSubmit { model.send(PayingMethodUiEventOnSaveClicked.shared) }
        } header: {
            nameLabel
        }
        if state.showsCardColors {
            Section {
                // A category's picker (Pierre's call): the twelve named colors, the person's own and "+" for "New color"
                CategoryColorGrid(
                    selected: state.cardColor,
                    customColors: state.customColors,
                    currentCustomColors: { model.current.customColors },
                    pick: { model.send(PayingMethodUiEventOnCardColorSelected(color: $0)) },
                    send: { model.send(PayingMethodUiEventOnCustomColor(event: $0)) },
                )
                .padding(.vertical, 4)
            } header: {
                Text("Color")
            }
        }
        Section {
            Toggle(isOn: defaultBinding) {
                Text("Default method")
            }
        } footer: {
            Text("New bills you pay start with it.")
        }
        if state.isEditing {
            ConfirmedDeleteSection(
                label: Text("Delete payment method"),
                question: Text("Delete this payment method?"),
                message: Text("Bills you marked with it will no longer show it."),
                isDisabled: state.isSaving,
                isConfirmationShown: state.isDeleteConfirmationShown,
                onDelete: { model.send(PayingMethodUiEventOnDeleteClicked.shared) },
                onConfirm: { model.send(PayingMethodUiEventOnDeleteConfirmed.shared) },
                onClosed: model.deleteConfirmationClosed,
            )
        }
    }

    private var defaultBinding: Binding<Bool> {
        Binding(
            get: { model.state.isDefault },
            set: { model.send(PayingMethodUiEventOnDefaultChanged(isDefault: $0)) },
        )
    }
}
