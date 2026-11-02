import Shared
import SwiftUI

/// The words of "Pay Ana" (4.5), what Record payment shows while this device's user pays someone with a key.
private enum PayWithKeyCopy {
    static func name(_ state: RegisterPaymentUiState) -> String {
        PaymentCopy.name(state.to?.name)
    }

    /// "Apê 302 · you owe", or "3 groups · you owe" across groups.
    static func source(_ state: RegisterPaymentUiState) -> String {
        if let group = state.groupName { return String(localized: "\(group) · you owe") }
        return String(localized: "\(state.groupParts.count) groups · you owe")
    }

    static func method(_ method: PaymentMethod) -> String {
        method == .pix ? String(localized: "Pix") : String(localized: "Bitcoin")
    }

    static func type(_ type: PaymentKeyType) -> String {
        switch type {
        case .cpf: String(localized: "CPF")
        case .phone: String(localized: "Phone")
        case .email: String(localized: "Email")
        case .random: String(localized: "Random key")
        case .lightning: String(localized: "Lightning")
        default: String(localized: "On-chain")
        }
    }

    /// Where to pay, and to check the name the bank shows: the app moves no money.
    static func note(_ state: RegisterPaymentUiState) -> String {
        if state.shownKeyMethod == .pix {
            return String(localized: "Pay in your bank's app and come back to mark it as paid. Check that the bank shows \(name(state))'s name.")
        }
        return String(localized: "Pay in your wallet and come back to mark it as paid.")
    }
}

extension RegisterPaymentModel {
    var keyMethod: Binding<PaymentMethod> {
        Binding(
            get: { self.state.shownKeyMethod },
            set: { self.send(RegisterPaymentUiEventOnKeyMethodPicked(method: $0)) },
        )
    }

    func markAsPaid() {
        send(RegisterPaymentUiEventOnMarkAsPaidClicked.shared)
    }
}

/// "Payment method": Pix or Bitcoin, only while the receiver has keys of both.
private struct KeyMethodPicker: View {
    let state: RegisterPaymentUiState
    @Binding var selection: PaymentMethod

    var body: some View {
        if state.showsKeyMethodPicker {
            Picker("Payment method", selection: $selection) {
                ForEach(state.keyMethods, id: \.self) { method in
                    Text(verbatim: PayWithKeyCopy.method(method)).tag(method)
                }
            }
            .pickerStyle(.segmented)
        }
    }
}

/// "Ana's default key", whole, with its type and "Copy key".
private struct DefaultKeyCard: View {
    let state: RegisterPaymentUiState
    let key: ReceiverKey
    let onCopy: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("\(PayWithKeyCopy.name(state))'s default key")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Spacer()
                Text(verbatim: PayWithKeyCopy.type(key.type))
                    .font(.caption.weight(.semibold))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(.tint.opacity(0.15), in: .capsule)
            }
            Text(verbatim: key.text)
                .font(.title3.bold())
                .textSelection(.enabled)
            Button {
                onCopy(key.value)
            } label: {
                Label("Copy key", systemImage: "doc.on.doc")
                    .frame(maxWidth: .infinity, minHeight: 32)
            }
            .buttonStyle(.borderedProminent)
        }
        .padding(16)
        .background(Palette.card, in: .rect(cornerRadius: 20))
    }
}

/// "Pix copy and paste", the default key with the whole debt in it, and its QR code for paying from another device.
private struct PixCodeCard: View {
    let state: RegisterPaymentUiState
    let code: String
    let qrSize: CGFloat
    let onCopy: (String) -> Void

    var body: some View {
        HStack(spacing: 14) {
            if let qrCode = state.pixQrCode {
                QrCodeImage(qrCode: qrCode)
                    .frame(width: qrSize, height: qrSize)
                    .background(.white, in: .rect(cornerRadius: 12))
                    .accessibilityLabel(Text("Pix QR code"))
            }
            VStack(alignment: .leading, spacing: 4) {
                Text("Pix copy and paste")
                    .font(.headline)
                Text("It already comes with \(state.currency.format(state.debtMinor)).")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                Button("Copy code") { onCopy(code) }
                    .buttonStyle(.bordered)
                    .padding(.top, 2)
            }
            Spacer(minLength: 0)
        }
        .padding(16)
        .background(Palette.card, in: .rect(cornerRadius: 20))
    }
}

/// "Ana's other keys", each with its own copy button.
private struct OtherKeysList: View {
    let state: RegisterPaymentUiState
    let onCopy: (String) -> Void

    var body: some View {
        if !state.otherKeys.isEmpty {
            VStack(alignment: .leading, spacing: 6) {
                Text("\(PayWithKeyCopy.name(state))'s other keys")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .padding(.leading, 4)
                VStack(spacing: 0) {
                    ForEach(Array(state.otherKeys.enumerated()), id: \.element.id) { index, key in
                        if index > 0 { Divider().padding(.leading, 16) }
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(verbatim: PayWithKeyCopy.type(key.type))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                Text(verbatim: key.text)
                                    .font(.body.weight(.semibold))
                            }
                            Spacer()
                            Button {
                                onCopy(key.value)
                            } label: {
                                Label("Copy", systemImage: "doc.on.doc")
                                    .labelStyle(.iconOnly)
                            }
                            .buttonStyle(.borderless)
                            .accessibilityLabel(Text("Copy \(PayWithKeyCopy.type(key.type)) of \(PayWithKeyCopy.name(state))"))
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                    }
                }
                .background(Palette.card, in: .rect(cornerRadius: 16))
            }
        }
    }
}

/// Where to pay, and the offline note: marking as paid waits on the device.
private struct PayNotes: View {
    let state: RegisterPaymentUiState

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(verbatim: PayWithKeyCopy.note(state))
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
        .font(.footnote)
        .foregroundStyle(.secondary)
        .fixedSize(horizontal: false, vertical: true)
    }
}

#if os(iOS)
/// "Pay Ana" in Record payment's sheet: one column upright, two lying down (who, how and the default key, then the
/// Pix code, the other keys and the note), "Mark as paid" always in sight.
struct PhonePayWithKey: View {
    let model: RegisterPaymentModel

    @State private var layout = ScreenLayout()
    @State private var copyCount = 0

    private var state: RegisterPaymentUiState {
        model.state
    }

    var body: some View {
        ScrollView {
            if layout.usesTwoColumns {
                TwoColumnsLayout(spacing: 24) {
                    VStack(spacing: 16) { who }
                } second: {
                    VStack(spacing: 16) { rest }
                }
                .padding(20)
            } else {
                VStack(spacing: 16) {
                    who
                    rest
                }
                .padding(20)
            }
        }
        .background(Palette.groupedBackground)
        .measuringScreenLayout($layout)
        .safeAreaInset(edge: .bottom) {
            PrimaryButton(
                title: "Mark as paid",
                isEnabled: state.canMarkAsPaid,
                isLoading: state.isSaving,
                action: model.markAsPaid,
            )
            .padding(.horizontal, 20)
            .padding(.bottom, 8)
        }
        .glassToast("Copied", trigger: copyCount)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Later", systemImage: "xmark", role: .cancel) {
                    model.send(RegisterPaymentUiEventOnCloseClicked.shared)
                }
                .disabled(state.isSaving)
            }
        }
    }

    @ViewBuilder
    private var who: some View {
        HStack(spacing: 14) {
            if let person = state.to {
                AvatarPhoto(initial: person.initials, color: person.avatarColor, photoUrl: nil, size: 48)
                    .accessibilityHidden(true)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text("Pay \(PayWithKeyCopy.name(state))")
                    .font(.title2.bold())
                    .accessibilityAddTraits(.isHeader)
                Text(verbatim: PayWithKeyCopy.source(state))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Text(verbatim: state.currency.format(state.debtMinor))
                .font(.title2.bold())
                .foregroundStyle(Palette.negative)
        }
        KeyMethodPicker(state: state, selection: model.keyMethod)
        if let key = state.defaultKey {
            DefaultKeyCard(state: state, key: key, onCopy: copy)
        }
    }

    @ViewBuilder
    private var rest: some View {
        if let code = state.pixCode {
            PixCodeCard(state: state, code: code, qrSize: 112, onCopy: copy)
        }
        OtherKeysList(state: state, onCopy: copy)
        PayNotes(state: state)
    }

    private func copy(_ text: String) {
        Pasteboard.copy(text)
        copyCount += 1
    }
}
#endif

#if os(macOS)
/// "Pay Ana · R$ 98,00" as a 440 pt sheet on the window: the method, the default key with "Copy key", the Pix code,
/// the other keys and the note scroll between the title and the buttons, so it fits a 560 pt window.
struct MacPayWithKey: View {
    let model: RegisterPaymentModel

    @State private var copyCount = 0

    private var state: RegisterPaymentUiState {
        model.state
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            VStack(alignment: .leading, spacing: 2) {
                Text("Pay \(PayWithKeyCopy.name(state)) · \(state.currency.format(state.debtMinor))")
                    .font(.title3.bold())
                    .accessibilityAddTraits(.isHeader)
                Text(verbatim: PayWithKeyCopy.source(state))
                    .foregroundStyle(.secondary)
            }
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    KeyMethodPicker(state: state, selection: model.keyMethod)
                        .labelsHidden()
                    if let key = state.defaultKey {
                        DefaultKeyCard(state: state, key: key, onCopy: copy)
                    }
                    if let code = state.pixCode {
                        PixCodeCard(state: state, code: code, qrSize: 88, onCopy: copy)
                    }
                    OtherKeysList(state: state, onCopy: copy)
                    PayNotes(state: state)
                }
            }
            .frame(maxHeight: 400)
            .fixedSize(horizontal: false, vertical: true)
            HStack(spacing: 8) {
                Spacer()
                Button("Later") {
                    model.send(RegisterPaymentUiEventOnCloseClicked.shared)
                }
                .keyboardShortcut(.cancelAction)
                .disabled(state.isSaving)
                Button("Mark as paid", action: model.markAsPaid)
                    .buttonStyle(.borderedProminent)
                    .keyboardShortcut(.defaultAction)
                    .disabled(!state.canMarkAsPaid)
            }
            .controlSize(.large)
        }
        .padding(20)
        .frame(width: 440)
        .glassToast("Copied", trigger: copyCount)
    }

    private func copy(_ text: String) {
        Pasteboard.copy(text)
        copyCount += 1
    }
}
#endif
