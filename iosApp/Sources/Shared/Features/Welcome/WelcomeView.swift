import Observation
import Shared
import SwiftUI

@Observable
final class WelcomeModel {
    private(set) var state: WelcomeUiState

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createWelcomeViewModel()
    @ObservationIgnored private let watches = WatchBag()
    @ObservationIgnored private let appleSignIn = AppleSignIn()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! WelcomeUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            self?.handle(action)
        })
    }

    func send(_ event: any WelcomeUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    private func handle(_ action: any WelcomeUiAction) {
        guard action is WelcomeUiActionLaunchNativeSignIn else { return }
        Task {
            switch await appleSignIn.signIn() {
            case let .success(idToken, rawNonce):
                send(WelcomeUiEventOnIdTokenReceived(provider: .apple, idToken: idToken, rawNonce: rawNonce))
            case .cancelled:
                send(WelcomeUiEventOnNativeSignInCancelled.shared)
            case .failed:
                send(WelcomeUiEventOnNativeSignInFailed.shared)
            }
        }
    }

    isolated deinit {
        holder.clear()
    }
}

struct WelcomeView: View {
    @State private var model = WelcomeModel()
    @State private var layout = ScreenLayout()

    var body: some View {
        Group {
            if layout.usesTwoColumns {
                HStack(alignment: .center, spacing: 56) {
                    ViewThatFits(in: .vertical) {
                        WelcomePitch(alignment: .leading, isLarge: layout.isLarge)
                        ScrollView { WelcomePitch(alignment: .leading, isLarge: layout.isLarge) }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
                    // Centred while it fits; scrolls only when the window is too short
                    ViewThatFits(in: .vertical) {
                        WelcomeActions(model: model)
                        ScrollView { WelcomeActions(model: model) }
                    }
                    .frame(maxWidth: 400, maxHeight: .infinity)
                }
                .frame(maxWidth: ScreenLayout.twoColumnsMaxWidth)
                .padding(.horizontal, 32)
            } else {
                VStack(spacing: 0) {
                    ScrollView {
                        WelcomePitch(alignment: .center, isLarge: layout.isLarge)
                            .padding(.top, layout.isLarge ? 80 : 40)
                    }
                    .scrollBounceBehavior(.basedOnSize)
                    WelcomeActions(model: model)
                        .padding(.bottom, 8)
                }
                .frame(maxWidth: ScreenLayout.singleColumnMaxWidth)
                .padding(.horizontal, 24)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .measuringScreenLayout($layout)
        .toolbar(.hidden)
    }
}

private struct WelcomePitch: View {
    let alignment: HorizontalAlignment
    let isLarge: Bool

    var body: some View {
        VStack(alignment: alignment, spacing: 28) {
            VStack(alignment: alignment, spacing: 16) {
                CurrencyBadge(size: isLarge ? 88 : 72)
                Text("Racha a conta sem complicação")
                    .font(isLarge ? .largeTitle.bold() : .title.bold())
                    .multilineTextAlignment(alignment == .center ? .center : .leading)
            }
            VStack(alignment: .leading, spacing: 16) {
                BenefitRow(
                    systemImage: "wifi.slash",
                    title: "Funciona sem internet",
                    message: "Tudo fica no \(DeviceName.current) e sincroniza depois.",
                )
                BenefitRow(
                    systemImage: "checkmark.shield",
                    title: "Conta é opcional",
                    message: "Apple ou Google só servem para não perder nada ao trocar de aparelho.",
                )
                BenefitRow(
                    systemImage: "link",
                    title: "Convide por link ou QR",
                    message: "Os amigos entram no grupo sem criar conta.",
                )
            }
            .padding(.horizontal, alignment == .center ? 12 : 0)
        }
    }
}

private struct BenefitRow: View {
    let systemImage: String
    let title: String
    let message: String

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            Image(systemName: systemImage)
                .font(.title3)
                .foregroundStyle(Palette.accent)
                .frame(width: 28)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.subheadline.weight(.semibold))
                Text(message).font(.subheadline).foregroundStyle(.secondary)
            }
        }
        .accessibilityElement(children: .combine)
    }
}

private struct WelcomeActions: View {
    let model: WelcomeModel

    var body: some View {
        let state = model.state
        VStack(spacing: 10) {
            if state.showsOfflineNotice {
                GlassNotice(
                    title: "Sem conexão.",
                    message: "Entrar com conta precisa de internet. Use sem conta agora e conecte depois em "
                        + "\(DeviceName.accountPlace).",
                )
                .transition(.opacity)
            }
            // Apple first on iOS and macOS: the App Store asks for it when there is another social login
            AppleIDButton(label: .continueWith, isEnabled: state.areAccountButtonsEnabled) {
                model.send(WelcomeUiEventOnProviderClicked(provider: .apple))
            }
            GoogleButton(
                title: "Continuar com Google",
                isEnabled: state.areAccountButtonsEnabled,
                isLoading: state.connectingProvider == .google,
            ) {
                model.send(WelcomeUiEventOnProviderClicked(provider: .google))
            }
            if state.error != nil {
                Text("Não deu para entrar agora. Tente de novo.")
                    .font(.footnote)
                    .foregroundStyle(Palette.negative)
            }
            OrDivider()
            TintedButton(title: "Usar sem conta") {
                model.send(WelcomeUiEventOnUseWithoutAccountClicked.shared)
            }
            Button("Já usei em outro aparelho") {
                model.send(WelcomeUiEventOnUsedOnAnotherDeviceClicked.shared)
            }
            .buttonStyle(.borderless)
            .frame(minHeight: 36)
        }
        .animation(.default, value: state.showsOfflineNotice)
    }
}
