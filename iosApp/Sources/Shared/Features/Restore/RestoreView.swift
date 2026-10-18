import Observation
import Shared
import SwiftUI

@Observable
final class RestoreModel {
    private(set) var state: RestoreUiState
    var isScanningQr = false

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createRestoreViewModel()
    @ObservationIgnored private let watches = WatchBag()
    @ObservationIgnored private let appleSignIn = AppleSignIn()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! RestoreUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            self?.handle(action)
        })
    }

    func send(_ event: any RestoreUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    private func handle(_ action: any RestoreUiAction) {
        switch action {
        case is RestoreUiActionOpenQrScanner:
            isScanningQr = true
        case is RestoreUiActionLaunchNativeSignIn:
            Task {
                switch await appleSignIn.signIn() {
                case let .success(idToken, rawNonce):
                    send(RestoreUiEventOnIdTokenReceived(provider: .apple, idToken: idToken, rawNonce: rawNonce))
                case .cancelled:
                    send(RestoreUiEventOnNativeSignInCancelled.shared)
                case .failed:
                    send(RestoreUiEventOnNativeSignInFailed.shared)
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

struct RestoreView: View {
    @State private var model = RestoreModel()
    @State private var layout = ScreenLayout()

    var body: some View {
        Group {
            if layout.usesTwoColumns {
                HStack(alignment: .top, spacing: 48) {
                    ScrollView {
                        RestoreIntro(model: model, isLarge: layout.isLarge)
                    }
                    .scrollBounceBehavior(.basedOnSize)
                    VStack(spacing: 16) {
                        ScrollView {
                            RestoreCodeSection(model: model)
                        }
                        .scrollBounceBehavior(.basedOnSize)
                        RestoreActions(model: model)
                    }
                }
                .frame(maxWidth: ScreenLayout.twoColumnsMaxWidth)
                .padding(.horizontal, 32)
                .padding(.bottom, 12)
            } else {
                VStack(spacing: 0) {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 18) {
                            RestoreIntro(model: model, isLarge: false)
                            RestoreCodeSection(model: model)
                        }
                        .padding(.horizontal, 16)
                        .padding(.bottom, 24)
                    }
                    .scrollBounceBehavior(.basedOnSize)
                    // "Recuperar" stays on screen, above the keyboard when it is open
                    RestoreActions(model: model)
                        .padding(.horizontal, 24)
                        .padding(.vertical, 8)
                }
                .frame(maxWidth: ScreenLayout.singleColumnMaxWidth)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.groupedBackground)
        .measuringScreenLayout($layout)
        .sheet(isPresented: $model.isScanningQr) {
            QRScannerView { content in
                model.send(RestoreUiEventOnQrScanned(content: content))
            }
        }
    }
}

private struct RestoreIntro: View {
    let model: RestoreModel
    let isLarge: Bool

    var body: some View {
        let state = model.state
        VStack(alignment: .leading, spacing: 18) {
            VStack(alignment: .leading, spacing: 6) {
                Text("Trazer seus grupos")
                    .font(isLarge ? .largeTitle.bold() : .title.bold())
                Text("Entrou com uma conta antes? É só entrar de novo. Se não, use o código de recuperação ou o QR do aparelho antigo.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .padding(.horizontal, 4)
            HStack(spacing: 10) {
                AppleIDButton(label: .short, isEnabled: state.areAccountButtonsEnabled) {
                    model.send(RestoreUiEventOnProviderClicked(provider: .apple))
                }
                GoogleButton(
                    title: "Google",
                    isEnabled: state.areAccountButtonsEnabled,
                    isLoading: state.connectingProvider == .google,
                ) {
                    model.send(RestoreUiEventOnProviderClicked(provider: .google))
                }
            }
            if state.error == .signInFailed {
                Text("Não deu para entrar agora. Tente de novo.")
                    .font(.footnote)
                    .foregroundStyle(Palette.negative)
            }
            if state.showsOfflineNotice {
                ConnectionCard(
                    title: "Sem conexão",
                    message: "Entrar com conta ou código precisa de internet para baixar seus grupos. O que você digitou fica guardado.",
                )
                .transition(.opacity)
            }
        }
        .animation(.default, value: state.showsOfflineNotice)
    }
}

private struct RestoreCodeSection: View {
    let model: RestoreModel

    var body: some View {
        let state = model.state
        VStack(alignment: .leading, spacing: 7) {
            SectionCaption(text: "Ou use o código de recuperação")
            GroupedCard {
                TextField(
                    "K7QM-4XPA-9TRE",
                    text: Binding(
                        get: { model.state.code },
                        set: { model.send(RestoreUiEventOnCodeChanged(code: $0)) },
                    ),
                )
                .font(.system(.title2, design: .monospaced).weight(.semibold))
                .tracking(2)
                .autocorrectionDisabled()
                #if os(iOS)
                .textInputAutocapitalization(.characters)
                .keyboardType(.asciiCapable)
                #endif
                .submitLabel(.go)
                .onSubmit { model.send(RestoreUiEventOnRestoreClicked.shared) }
                .accessibilityLabel("Código de recuperação")
                .padding(.horizontal, 16)
                .frame(minHeight: 56)
            }
            Text(codeHint(for: state.error))
                .font(.footnote)
                .foregroundStyle(isCodeError(state.error) ? Palette.negative : .secondary)
                .padding(.horizontal, 20)
            GroupedCard {
                Button {
                    model.send(RestoreUiEventOnScanQrClicked.shared)
                } label: {
                    HStack(spacing: 12) {
                        Image(systemName: "qrcode.viewfinder")
                            .font(.title2)
                            .foregroundStyle(Palette.accent)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(scanTitle)
                            Text(scanHint).font(.footnote).foregroundStyle(.secondary)
                        }
                        Spacer()
                        Image(systemName: "chevron.forward")
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(.tertiary)
                    }
                    .padding(.horizontal, 16)
                    .frame(minHeight: 60)
                    .contentShape(.rect)
                }
                .buttonStyle(.plain)
            }
            .padding(.top, 9)
        }
    }

    private var scanTitle: String {
        #if os(iOS)
        "Escanear QR do aparelho antigo"
        #else
        "Escanear QR com a câmera"
        #endif
    }

    private var scanHint: String {
        #if os(iOS)
        "Lá: Perfil › Recuperação › Mostrar QR"
        #else
        "No aparelho antigo: Perfil › Recuperação › Mostrar QR"
        #endif
    }

    private func isCodeError(_ error: RestoreError?) -> Bool {
        error != nil && error != .signInFailed
    }

    private func codeHint(for error: RestoreError?) -> String {
        switch error {
        case .invalidCode: "Código não encontrado. Confira as letras e tente de novo."
        case .tooManyAttempts: "Muitas tentativas. Espere alguns minutos e tente de novo."
        case .unknown: "Algo deu errado. Tente de novo."
        default: "12 letras e números. Traços são opcionais."
        }
    }
}

private struct RestoreActions: View {
    let model: RestoreModel

    var body: some View {
        let state = model.state
        VStack(spacing: 10) {
            if state.isOnline {
                PrimaryButton(title: "Recuperar", isEnabled: state.canRestore, isLoading: state.isRestoring) {
                    model.send(RestoreUiEventOnRestoreClicked.shared)
                }
            } else {
                // The connectivity observer flips the screen back by itself; the button reassures
                PrimaryButton(title: "Tentar de novo", systemImage: "arrow.clockwise") {
                    model.send(RestoreUiEventOnRestoreClicked.shared)
                }
                Button("Começar do zero neste \(DeviceName.current)") {
                    model.send(RestoreUiEventOnStartFreshClicked.shared)
                }
                .buttonStyle(.borderless)
                .frame(minHeight: 36)
            }
        }
        .frame(maxWidth: 400)
    }
}
