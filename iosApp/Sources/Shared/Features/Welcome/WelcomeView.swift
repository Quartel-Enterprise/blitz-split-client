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
            case let .success(idToken, rawNonce, fullName):
                send(WelcomeUiEventOnIdTokenReceived(provider: .apple, idToken: idToken, rawNonce: rawNonce, fullName: fullName))
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
                Text("Split the bill, no hassle")
                    .font(isLarge ? .largeTitle.bold() : .title.bold())
                    .multilineTextAlignment(alignment == .center ? .center : .leading)
            }
            VStack(alignment: .leading, spacing: 16) {
                BenefitRow(
                    systemImage: "wifi.slash",
                    title: "Works offline",
                    message: "Everything stays on your \(DeviceName.current) and syncs later.",
                )
                BenefitRow(
                    systemImage: "checkmark.shield",
                    title: "An account is optional",
                    message: "Apple or Google only keep you from losing anything when you switch devices.",
                )
                BenefitRow(
                    systemImage: "link",
                    title: "Invite with a link or QR code",
                    message: "Friends join the group without creating an account.",
                )
            }
            .padding(.horizontal, alignment == .center ? 12 : 0)
        }
    }
}

private struct BenefitRow: View {
    let systemImage: String
    let title: LocalizedStringKey
    let message: LocalizedStringKey

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
                    title: "No connection.",
                    message: "Signing in with an account needs the internet. Use it without an account now and connect later in \(DeviceName.accountPlace).",
                )
                .transition(.opacity)
            }
            // Apple first on iOS and macOS: the App Store asks for it when there is another social login
            AppleIDButton(label: .continueWith, isEnabled: state.areAccountButtonsEnabled) {
                model.send(WelcomeUiEventOnProviderClicked(provider: .apple))
            }
            GoogleButton(
                title: "Continue with Google",
                isEnabled: state.areAccountButtonsEnabled,
                isLoading: state.connectingProvider == .google,
            ) {
                model.send(WelcomeUiEventOnProviderClicked(provider: .google))
            }
            if state.error != nil {
                Text("Couldn't sign in right now. Try again.")
                    .font(.footnote)
                    .foregroundStyle(Palette.negative)
            }
            OrDivider()
            TintedButton(title: "Use without an account") {
                model.send(WelcomeUiEventOnUseWithoutAccountClicked.shared)
            }
            Button("I've used it on another device") {
                model.send(WelcomeUiEventOnUsedOnAnotherDeviceClicked.shared)
            }
            .buttonStyle(.borderless)
            .frame(minHeight: 36)
        }
        .animation(.default, value: state.showsOfflineNotice)
    }
}
