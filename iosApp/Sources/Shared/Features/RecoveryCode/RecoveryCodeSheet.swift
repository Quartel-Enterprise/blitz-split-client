import Observation
import Shared
import SwiftUI

@Observable
final class RecoveryCodeModel {
    private(set) var state: RecoveryCodeUiState
    private(set) var didCopy = false

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createRecoveryCodeViewModel()
    @ObservationIgnored private let watches = WatchBag()
    @ObservationIgnored private let appleSignIn = AppleSignIn()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! RecoveryCodeUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            self?.handle(action)
        })
    }

    func send(_ event: any RecoveryCodeUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    private func handle(_ action: any RecoveryCodeUiAction) {
        switch action {
        case let copy as RecoveryCodeUiActionCopyToClipboard:
            RecoveryCodeSaver.copy(copy.formattedCode)
            didCopy = true
        case let save as RecoveryCodeUiActionSaveToPasswordManager:
            Task { _ = await RecoveryCodeSaver.saveToPasswords(save.formattedCode) }
        case is RecoveryCodeUiActionLaunchNativeSignIn:
            Task {
                switch await appleSignIn.signIn() {
                case let .success(idToken, rawNonce, fullName):
                    send(RecoveryCodeUiEventOnIdTokenReceived(provider: .apple, idToken: idToken, rawNonce: rawNonce, fullName: fullName))
                case .cancelled:
                    send(RecoveryCodeUiEventOnNativeSignInCancelled.shared)
                case .failed:
                    send(RecoveryCodeUiEventOnNativeSignInFailed.shared)
                }
            }
        default:
            break
        }
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Save your recovery code": a glass sheet on iPhone (floating when lying down), a form
/// sheet on iPad and a sheet attached to the window top on the Mac. Dismissing it counts as
/// "Remind me later", so the code comes back on the next launch.
struct RecoveryCodeSheet: View {
    @State private var model = RecoveryCodeModel()
    @State private var contentHeight: CGFloat = 560

    var body: some View {
        ScrollView {
            RecoveryCodeContent(model: model)
                .padding(.horizontal, 22)
                .padding(.top, 24)
                .padding(.bottom, 16)
                .onGeometryChange(for: CGFloat.self, of: \.size.height) { height in
                    contentHeight = height
                }
        }
        .scrollBounceBehavior(.basedOnSize)
        #if os(iOS)
        .presentationDetents([.height(contentHeight)])
        .presentationSizing(.form)
        #else
        .frame(width: 480)
        .frame(minHeight: 420)
        #endif
        .interactiveDismissDisabled(false)
        .onDisappear {
            if model.state.isPromptDue {
                model.send(RecoveryCodeUiEventOnRemindLaterClicked.shared)
            }
        }
    }
}

private struct RecoveryCodeContent: View {
    let model: RecoveryCodeModel

    var body: some View {
        let state = model.state
        VStack(spacing: 16) {
            VStack(spacing: 8) {
                Image(systemName: "key.fill")
                    .font(.system(size: 34))
                    .foregroundStyle(Palette.accent)
                    .accessibilityHidden(true)
                Text("Save your recovery code")
                    .font(.title2.bold())
                    .multilineTextAlignment(.center)
                Text("You're using it without an account. This code brings your groups back if you switch or lose your \(DeviceName.current).")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
            GroupedCard {
                VStack(spacing: 0) {
                    HStack {
                        Text(state.formattedCode ?? "")
                            .font(.system(.title2, design: .monospaced).weight(.semibold))
                            .tracking(2)
                            .textSelection(.enabled)
                        Spacer()
                        Button(model.didCopy ? "Copied" : "Copy" as LocalizedStringKey) {
                            model.send(RecoveryCodeUiEventOnCopyClicked.shared)
                        }
                        .buttonStyle(.borderless)
                    }
                    .padding(.horizontal, 16)
                    .frame(minHeight: 64)
                    Divider()
                    OptionRow(systemImage: "key.viewfinder", title: "Save to the Passwords app") {
                        model.send(RecoveryCodeUiEventOnSaveToPasswordManagerClicked.shared)
                    }
                    Divider()
                    if state.isConnectExpanded {
                        // Apple first on iOS and macOS, as the design asks
                        VStack(spacing: 10) {
                            AppleIDButton(label: .continueWith, isEnabled: state.areAccountButtonsEnabled) {
                                model.send(RecoveryCodeUiEventOnProviderClicked(provider: .apple))
                            }
                            GoogleButton(
                                title: "Continue with Google",
                                isEnabled: state.areAccountButtonsEnabled,
                                isLoading: state.connectingProvider == .google,
                            ) {
                                model.send(RecoveryCodeUiEventOnProviderClicked(provider: .google))
                            }
                        }
                        .padding(12)
                    } else {
                        OptionRow(
                            systemImage: "checkmark.shield",
                            title: "Connect Apple or Google instead",
                            subtitle: "No code needed",
                            isEnabled: state.isOnline,
                        ) {
                            withAnimation { model.send(RecoveryCodeUiEventOnConnectInsteadClicked.shared) }
                        }
                    }
                }
            }
            if state.hasSignInFailed {
                Text("Couldn't connect right now. Try again.")
                    .font(.footnote)
                    .foregroundStyle(Palette.negative)
            }
            Text("It's also in \(DeviceName.recoveryPlace).")
                .font(.footnote)
                .foregroundStyle(.secondary)
            actions
        }
    }

    @ViewBuilder
    private var actions: some View {
        #if os(iOS)
        VStack(spacing: 4) {
            PrimaryButton(title: "I've saved it") {
                model.send(RecoveryCodeUiEventOnSavedClicked.shared)
            }
            Button("Remind me later") {
                model.send(RecoveryCodeUiEventOnRemindLaterClicked.shared)
            }
            .buttonStyle(.borderless)
            .frame(minHeight: 36)
        }
        #else
        HStack {
            Spacer()
            Button("Remind me later") {
                model.send(RecoveryCodeUiEventOnRemindLaterClicked.shared)
            }
            .keyboardShortcut(.cancelAction)
            Button("I've saved it") {
                model.send(RecoveryCodeUiEventOnSavedClicked.shared)
            }
            .keyboardShortcut(.defaultAction)
            .buttonStyle(.glassProminent)
        }
        #endif
    }
}

private struct OptionRow: View {
    let systemImage: String
    let title: LocalizedStringKey
    var subtitle: LocalizedStringKey?
    var isEnabled = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: systemImage)
                    .frame(width: 24)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                    if let subtitle {
                        Text(subtitle).font(.footnote).foregroundStyle(Color.secondary)
                    }
                }
                Spacer()
            }
            .foregroundStyle(Palette.accent)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .frame(minHeight: 48)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .disabled(!isEnabled)
        .opacity(isEnabled ? 1 : 0.4)
    }
}
