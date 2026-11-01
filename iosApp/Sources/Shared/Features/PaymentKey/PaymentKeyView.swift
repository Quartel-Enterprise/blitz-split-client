import Observation
import Shared
import SwiftUI

@Observable
final class PaymentKeyModel {
    private(set) var state: PaymentKeyUiState

    @ObservationIgnored private let holder: ViewModelHolder<PaymentKeyViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RoutePaymentKey) {
        holder = BlitzSplitApple.shared.createPaymentKeyViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! PaymentKeyUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
    }

    func send(_ event: any PaymentKeyUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    /// The input as the ViewModel holds it right now, masked, before the watcher brings it to `state`.
    var currentInput: String {
        (holder.viewModel.uiState.value as? PaymentKeyUiState)?.input ?? state.input
    }

    /// The dialog closes itself on any of its buttons, so the close waits for the button's own event to
    /// reach the ViewModel first, and only dismisses a confirmation that is still showing.
    func deleteConfirmationClosed() {
        DispatchQueue.main.async { [weak self] in
            guard let self,
                  (holder.viewModel.uiState.value as? PaymentKeyUiState)?.isDeleteConfirmationShown == true
            else { return }
            send(PaymentKeyUiEventOnDeleteDismissed.shared)
        }
    }

    isolated deinit {
        holder.clear()
    }
}

/// A Pix or Bitcoin key, new or saved. The iPhone pushes it with Save in the bar, in two columns lying
/// down; the iPad opens it as a form sheet and the Mac as a sheet on its window, with Cancel and Save.
struct PaymentKeyView: View {
    let route: RoutePaymentKey

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws, and
    /// a ViewModel made for nothing would still read the keys, and close the screen if the key is gone.
    @State private var model: PaymentKeyModel?
    @State private var layout = ScreenLayout()
    /// The window under the sheet, not the sheet: whether this key is a sheet or a pushed screen.
    @Environment(\.windowLayout) private var windowLayout

    private var isSheet: Bool {
        AppRoute.paymentKey(route).presentsAsSheet(in: windowLayout)
    }

    private var isPix: Bool {
        route.method == .pix
    }

    private var title: LocalizedStringKey {
        switch (isPix, route.keyId != nil) {
        case (true, false): "New Pix key"
        case (true, true): "Edit Pix key"
        case (false, false): "New Bitcoin key"
        case (false, true): "Edit Bitcoin key"
        }
    }

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                PaymentKeyForm(
                    model: model,
                    title: title,
                    usesTwoColumns: layout.usesTwoColumns && !isSheet,
                )
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        .measuringScreenLayout($layout)
        .navigationTitle(title)
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            if isSheet {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { model?.send(PaymentKeyUiEventOnBackClicked.shared) }
                }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") { model?.send(PaymentKeyUiEventOnSaveClicked.shared) }
                    .disabled(model?.state.canSave != true)
            }
        }
        .interactiveDismissDisabled(model?.state.isSaving == true)
        #if os(macOS)
        .frame(minWidth: 460, idealWidth: 500, minHeight: 540, idealHeight: 680)
        #endif
        .onAppear {
            if model == nil { model = PaymentKeyModel(route: route) }
        }
    }
}

/// Upright, on iPad and on the Mac, one column. Lying down on the iPhone, the type on the left and the
/// key on the right, as the canvas draws it.
private struct PaymentKeyForm: View {
    let model: PaymentKeyModel
    let title: LocalizedStringKey
    let usesTwoColumns: Bool

    var body: some View {
        if usesTwoColumns {
            HStack(alignment: .top, spacing: 0) {
                Form {
                    TypeSection(model: model)
                    DisclaimerSection(method: model.state.method)
                }
                Form {
                    KeySections(model: model)
                }
            }
            .formStyle(.grouped)
            .frame(maxWidth: ScreenLayout.twoColumnsMaxWidth)
            .frame(maxWidth: .infinity)
            .background(Palette.groupedBackground)
        } else {
            Form {
                #if os(macOS)
                // A Mac sheet has no title bar, so the title heads the form
                Section {
                    Text(title).font(.title2.bold())
                }
                .listRowBackground(Color.clear)
                #endif
                TypeSection(model: model)
                KeySections(model: model)
                DisclaimerSection(method: model.state.method)
            }
            .formStyle(.grouped)
        }
    }
}

/// Pix: the four key types as a segmented control. Bitcoin: the two networks, each with what it is good for.
private struct TypeSection: View {
    let model: PaymentKeyModel

    var body: some View {
        let state = model.state
        if state.method == .pix {
            Section {
                Picker(selection: typeBinding) {
                    ForEach(state.types, id: \.self) { type in
                        type.segmentTitle.tag(type)
                    }
                } label: {
                    Text("Whoever pays you sees this key when settling up. Which type of key?")
                }
                .pickerStyle(.segmented)
                .labelsHidden()
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())
            } header: {
                Text("Whoever pays you sees this key when settling up. Which type of key?")
                    .font(.subheadline)
                    .textCase(nil)
                    .accessibilityHidden(true)
            }
        } else {
            Section {
                NetworkPicker(model: model)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
            } header: {
                Text("How do you want to receive?")
                    .font(.subheadline)
                    .textCase(nil)
                    .accessibilityHidden(true)
            } footer: {
                if state.hasSwitchedNetwork {
                    Label {
                        state.type == .onchain
                            ? Text("This is an on-chain address, so we switched to On-chain.")
                            : Text("This is a Lightning Address, so we switched to Lightning.")
                    } icon: {
                        Image(systemName: "arrow.left.arrow.right")
                    }
                }
            }
        }
    }

    private var typeBinding: Binding<PaymentKeyType> {
        Binding(
            get: { model.state.type },
            set: { model.send(PaymentKeyUiEventOnTypeSelected(type: $0)) },
        )
    }
}

/// Lightning or on-chain, a segmented control whose segments also say what each network is good for.
private struct NetworkPicker: View {
    let model: PaymentKeyModel

    var body: some View {
        let state = model.state
        HStack(spacing: 2) {
            ForEach(state.types, id: \.self) { type in
                let isSelected = type == state.type
                Button {
                    withAnimation(.snappy) { model.send(PaymentKeyUiEventOnTypeSelected(type: type)) }
                } label: {
                    VStack(spacing: 1) {
                        Label {
                            type.segmentTitle
                        } icon: {
                            Image(systemName: type == .lightning ? "bolt.fill" : "link")
                        }
                        .font(.subheadline.weight(isSelected ? .semibold : .medium))
                        .foregroundStyle(.primary)
                        Text(type == .lightning ? "fast, low fee" : "on the blockchain")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity, minHeight: 48)
                    .background {
                        if isSelected {
                            Capsule()
                                .fill(Palette.segmentThumb)
                                .shadow(color: .black.opacity(0.12), radius: 3, y: 2)
                        }
                    }
                    .contentShape(.capsule)
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(isSelected ? .isSelected : [])
            }
        }
        .padding(2)
        .background(.fill.tertiary, in: .capsule)
        .accessibilityElement(children: .contain)
        .accessibilityLabel("How do you want to receive?")
    }
}

/// The key, its helper, the CPF notice, what Bitcoin accepts, the default switch and, for a saved key,
/// its deletion.
private struct KeySections: View {
    let model: PaymentKeyModel

    @FocusState private var isFocused: Bool

    var body: some View {
        let state = model.state
        Section {
            KeyField(model: model, isFocused: $isFocused)
        } header: {
            state.type.fieldLabel
        } footer: {
            KeyHelper(state: state)
        }
        .onChange(of: isFocused) { wasFocused, isFocused in
            if wasFocused, !isFocused { model.send(PaymentKeyUiEventOnInputFocusLost.shared) }
        }
        if state.showsCpfNotice {
            Section {
                Label {
                    Text("Whoever pays you sees your full CPF. If you'd rather not share it, use a phone, email or random key.")
                } icon: {
                    Image(systemName: "info.circle")
                }
                .font(.subheadline)
                .foregroundStyle(Palette.onInfoContainer)
                .listRowBackground(Palette.infoContainer)
            }
        }
        if state.method == .bitcoin {
            Section("We accept") {
                if state.type == .lightning {
                    AcceptedRow(sample: Text(String(localized: "name@wallet.app")), name: "Lightning Address")
                    AcceptedRow(sample: Text(verbatim: "LNURL1DP68GURN…"), name: "LNURL")
                } else {
                    AcceptedRow(sample: Text(verbatim: "bc1q…"), name: "SegWit (bech32)")
                    AcceptedRow(sample: Text(verbatim: "bc1p…"), name: "Taproot (bech32m)")
                }
            }
        }
        Section {
            Toggle(isOn: defaultBinding) {
                state.method == .pix ? Text("Default Pix key") : Text("Default Bitcoin key")
            }
            .disabled(state.isDefaultLocked)
        } footer: {
            DefaultHelper(state: state)
        }
        if state.isEditing {
            DeleteSection(model: model)
        }
    }

    private var defaultBinding: Binding<Bool> {
        Binding(
            get: { model.state.isDefault },
            set: { model.send(PaymentKeyUiEventOnDefaultChanged(isDefault: $0)) },
        )
    }
}

/// The key as typed, masked by the ViewModel, with "Paste": a Pix key on one line, a Bitcoin key in a
/// monospaced field that wraps. That one is `UnhyphenatedTextField`: SwiftUI's own hyphenates an address
/// where it wraps, and the hyphen looks like part of it.
///
/// The field keeps its own draft, as the web's `useDraft` does: the state reaches Swift a moment after
/// each keystroke, and a field bound straight to it would show the stale input meanwhile and lose what
/// is typed fast. So each edit reads the masked input back from the ViewModel at once and puts the
/// cursor at the end, where the mask leaves it; a paste or a type switch arrives through the state.
private struct KeyField: View {
    let model: PaymentKeyModel
    let isFocused: FocusState<Bool>.Binding

    @State private var draft: String
    @State private var selection: TextSelection?
    /// The last edit sent: the field may report the same text again once the mask replaced it, which
    /// the ViewModel would take for a backspace over the mask.
    @State private var sentText: String?

    init(model: PaymentKeyModel, isFocused: FocusState<Bool>.Binding) {
        self.model = model
        self.isFocused = isFocused
        _draft = State(initialValue: model.state.input)
    }

    var body: some View {
        let state = model.state
        if state.method == .pix {
            HStack(spacing: 8) {
                textField
                    .font(state.type == .random ? .subheadline : .body)
                paste
            }
        } else {
            VStack(alignment: .leading, spacing: 10) {
                addressField
                paste
            }
            .padding(.vertical, 4)
        }
    }

    private var addressField: some View {
        let type = model.state.type
        return syncingDraft(
            UnhyphenatedTextField(
                text: $draft,
                isFocused: isFocused,
                accessibilityLabel: type.addressFieldLabel,
                lines: 2...4,
                onSubmit: { model.send(PaymentKeyUiEventOnSaveClicked.shared) },
            )
            #if os(iOS)
            .keyboard(type.keyboard, contentType: type.contentType)
            #endif
            .focused(isFocused)
            .overlay(alignment: .topLeading) {
                if draft.isEmpty {
                    type.fieldLabel
                        .font(.body.monospaced())
                        .foregroundStyle(UnhyphenatedTextField.placeholderColor)
                        .allowsHitTesting(false)
                        .accessibilityHidden(true)
                }
            },
        )
    }

    private var textField: some View {
        let type = model.state.type
        return syncingDraft(
            TextField(text: $draft, selection: $selection, prompt: type.fieldLabel) {
                type.fieldLabel
            }
            #if os(iOS)
            .keyboardType(type.keyboard)
            .textContentType(type.contentType)
            .textInputAutocapitalization(.never)
            #else
            .labelsHidden()
            #endif
            .autocorrectionDisabled()
            .focused(isFocused)
            .submitLabel(.done)
            .onSubmit { model.send(PaymentKeyUiEventOnSaveClicked.shared) },
        )
    }

    /// Sends each edit of the draft once and shows the masked input back, from the field or the state.
    private func syncingDraft(_ field: some View) -> some View {
        field
            .onChange(of: draft) { _, text in
                guard text != model.currentInput else { return }
                if text != sentText {
                    sentText = text
                    model.send(PaymentKeyUiEventOnInputChanged(input: text))
                }
                showCurrentInput()
            }
            // The live input, not the delivered one: a state a few keystrokes old must not undo them
            .onChange(of: model.state.input) {
                showCurrentInput()
            }
            // A new type replaces the input, so the next edit is new even if it repeats the last one sent
            .onChange(of: model.state.type) {
                sentText = nil
            }
    }

    private func showCurrentInput() {
        let input = model.currentInput
        guard input != draft else { return }
        draft = input
        selection = TextSelection(insertionPoint: input.endIndex)
    }

    // Why: the system's paste button reads the clipboard without the "Allow Paste" prompt, and says "Paste" in
    // the person's language.
    private var paste: some View {
        PasteButton(payloadType: String.self) { texts in
            guard let text = texts.first else { return }
            Task { @MainActor in
                // A paste replaces the input, so the next edit is new even if it repeats the last one sent
                sentText = nil
                model.send(PaymentKeyUiEventOnPasted(text: text))
            }
        }
        .labelStyle(.titleOnly)
        .buttonBorderShape(.capsule)
        .controlSize(.small)
        .tint(Palette.accent)
    }
}

/// Under the key: the error once there is one, what the valid key turned out to be, or how to type it.
private struct KeyHelper: View {
    let state: PaymentKeyUiState

    var body: some View {
        if let error = state.error {
            Label {
                error.message
            } icon: {
                Image(systemName: "exclamationmark.circle.fill")
            }
            .foregroundStyle(Palette.negative)
            .fontWeight(.medium)
        } else if let format = state.format {
            Label {
                format.validText
            } icon: {
                Image(systemName: "checkmark.circle.fill").foregroundStyle(Palette.accent)
            }
        } else {
            state.type.hint
        }
    }
}

/// "Shown first when settling up.", then which type is the default today, or why the switch is locked.
private struct DefaultHelper: View {
    let state: PaymentKeyUiState

    var body: some View {
        let lead = state.method == .pix
            ? Text("Shown first when settling up.")
            : Text("Shown first when someone pays in BTC.")
        if state.isDefaultLocked {
            let reason = state.isEditing
                ? Text("To change it, make another key the default.")
                : Text("Your first key of this kind is the default.")
            Text("\(lead) \(reason)")
        } else if let current = state.currentDefaultType {
            Text("\(lead) \(Text("Now: \(current.word)."))")
        } else {
            lead
        }
    }
}

private struct AcceptedRow: View {
    let sample: Text
    let name: String

    var body: some View {
        LabeledContent {
            Text(verbatim: name)
        } label: {
            sample.monospaced()
        }
    }
}

private struct DeleteSection: View {
    let model: PaymentKeyModel

    var body: some View {
        Section {
            Button("Delete key", role: .destructive) {
                model.send(PaymentKeyUiEventOnDeleteClicked.shared)
            }
            .disabled(model.state.isSaving)
            // Anchored to the button, so the iPad shows it as a popover over it
            .confirmationDialog(
                "Delete this key?",
                isPresented: Binding(
                    get: { model.state.isDeleteConfirmationShown },
                    set: { isPresented in
                        if !isPresented { model.deleteConfirmationClosed() }
                    },
                ),
                titleVisibility: .visible,
            ) {
                Button("Delete", role: .destructive) {
                    model.send(PaymentKeyUiEventOnDeleteConfirmed.shared)
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("People who owe you won't see it when settling up anymore.")
            }
        }
    }
}

/// The app never moves money: who pays does it in the bank app or the wallet.
private struct DisclaimerSection: View {
    let method: PaymentMethod

    var body: some View {
        Section {} footer: {
            Group {
                if method == .pix {
                    Text("Blitz Split doesn't move money: the payer pays in their bank app. Check that the key is in your name.")
                } else {
                    Text("When settling up, we show the amount in sats at the current rate. It's only an estimate: the payer confirms it in their wallet.")
                }
            }
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
        }
    }
}

private extension PaymentKeyType {
    var segmentTitle: Text {
        switch self {
        case .cpf: Text(verbatim: "CPF")
        case .phone: Text("Phone")
        case .email: Text("Email")
        case .random: Text("Random")
        case .lightning: Text(verbatim: "Lightning")
        default: Text(verbatim: "On-chain")
        }
    }

    var fieldLabel: Text {
        switch self {
        case .cpf: Text(verbatim: "CPF")
        case .phone: Text("Mobile number with area code")
        case .email: Text("Email")
        case .random: Text("Random key")
        case .lightning: Text("Lightning Address or LNURL")
        default: Text("Bitcoin address")
        }
    }

    var hint: Text {
        switch self {
        case .cpf: Text("Format 000.000.000-00")
        case .phone: Text("Format +55 (00) 90000-0000")
        case .email: Text(String(localized: "Format name@domain.com"))
        case .random: Text("32 letters and numbers, as your bank shows it")
        case .lightning: Text(String(localized: "Like name@wallet.com or LNURL1…"))
        default: Text("Mainnet address starting with bc1q or bc1p")
        }
    }

    /// The type in "Now: phone.", lowercase as it sits mid-sentence.
    var word: Text {
        switch self {
        case .cpf: Text(verbatim: "CPF")
        case .phone: Text("phone")
        case .email: Text("email")
        case .random: Text("random key")
        case .lightning: Text(verbatim: "Lightning")
        default: Text(verbatim: "on-chain")
        }
    }

    /// The Bitcoin field's label as VoiceOver reads it, for `UnhyphenatedTextField`.
    var addressFieldLabel: String {
        switch self {
        case .lightning: String(localized: "Lightning Address or LNURL")
        default: String(localized: "Bitcoin address")
        }
    }

    #if os(iOS)
    var keyboard: UIKeyboardType {
        switch self {
        case .cpf: .numberPad
        case .phone: .phonePad
        case .email: .emailAddress
        default: .asciiCapable
        }
    }

    var contentType: UITextContentType? {
        switch self {
        case .phone: .telephoneNumber
        case .email: .emailAddress
        default: nil
        }
    }
    #endif
}

private extension PaymentKeyFormat {
    var validText: Text {
        switch self {
        case .cpf: Text("Format 000.000.000-00 · check digits match")
        case .phone: Text("Format +55 (00) 90000-0000")
        case .email: Text(String(localized: "Format name@domain.com"))
        case .random: Text("Random key recognized (32 characters)")
        case .lightningAddress: Text("Valid Lightning Address")
        case .lnurl: Text("Valid LNURL")
        case .segwit: Text("SegWit (bc1q) · mainnet")
        default: Text("Taproot (bc1p) · mainnet")
        }
    }
}

private extension PaymentKeyError {
    var message: Text {
        switch self {
        case .invalidCpf: Text("Invalid CPF: the check digits don't match.")
        case .invalidPhone: Text("Incomplete mobile number: use the area code and 9 digits, starting with 9.")
        case .invalidEmail: Text("Incomplete email: the domain is missing, like .com or .com.br.")
        case .invalidRandom: Text("Incomplete random key: it has 32 letters and numbers.")
        case .invalidLightning: Text(String(localized: "This isn't a Lightning Address (name@wallet) or an LNURL."))
        case .lightningInvoice: Text("This is a Lightning invoice (lnbc…): it pays once and expires. Paste a Lightning Address or an LNURL.")
        case .invalidOnchain: Text("Invalid address. Use a mainnet bc1q or bc1p address.")
        case .testnetAddress: Text("This is a testnet address (tb1…). Only Bitcoin mainnet works here, with bc1… addresses.")
        default: Text("You already have this key.")
        }
    }
}
