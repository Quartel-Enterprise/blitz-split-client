import Observation
import Shared
import SwiftUI

@Observable
final class WatchPairingModel {
    private(set) var state: WatchPairingUiState

    @ObservationIgnored private let holder = BlitzSplitWatch.shared.createWatchPairingViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! WatchPairingUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        // The code to the paired iPhone, which approves it with its own session (15.10)
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            guard let send = action as? WatchPairingUiActionSendCodeToPhone else { return }
            PhoneLink.shared.sendPairingCode(send.code) { reply in
                self?.send(WatchPairingUiEventOnPhoneReplied(reply: reply))
            }
        })
    }

    func send(_ event: any WatchPairingUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// Connecting the watch to the iPhone's account (15.9 to 15.13): one screen per step of `WatchPairingViewModel`, each
/// a message with its button at the end. The word for the phone is "iPhone" on the watch.
struct WatchPairingView: View {
    @State private var model = WatchPairingModel()

    var body: some View {
        let state = model.state
        Group {
            if state.isWaiting {
                WatchPairingWaitingView()
            } else {
                switch state.step {
                case .phoneOutOfReach: farView
                case .localOnly: localOnlyView
                case .code: codeView(state.code ?? "")
                case .connected: connectedView(state)
                case .signedOutOnPhone: signedOutView
                default: welcomeView(state.error)
                }
            }
        }
        .toolbar(.hidden, for: .navigationBar)
    }

    private func welcomeView(_ error: WatchPairingError?) -> some View {
        WatchMessageScreen {
            Image("Logo")
                .resizable()
                .scaledToFit()
                .padding(10)
                .frame(width: 44, height: 44)
                .foregroundStyle(WatchPalette.onAccent)
                .background(WatchPalette.accent, in: .circle)
                .accessibilityHidden(true)
            Text("Blitz Split")
                .font(.headline)
            Text("Use the same account as your iPhone here. Then the watch works on its own.")
                .messageBody()
            if let error {
                Text(errorText(error))
                    .font(.footnote)
                    .foregroundStyle(WatchPalette.negative)
                    .multilineTextAlignment(.center)
            }
        } actions: {
            prominentButton("Connect") { model.send(WatchPairingUiEventOnConnectClicked.shared) }
        }
    }

    private var farView: some View {
        WatchMessageScreen {
            Text("iPhone out of reach").messageTitle()
            Text("Bring your iPhone closer, or connect with a code.").messageBody()
        } actions: {
            prominentButton("Use code") { model.send(WatchPairingUiEventOnUseCodeClicked.shared) }
            glassButton("Try again") { model.send(WatchPairingUiEventOnTryAgainClicked.shared) }
        }
    }

    private var localOnlyView: some View {
        WatchMessageScreen {
            Text("Your data is only on your iPhone").messageTitle()
            Text("On your iPhone, connect an account in Profile and tap Connect here again.").messageBody()
        } actions: {
            glassButton("OK") { model.send(WatchPairingUiEventOnLocalOnlyAcknowledged.shared) }
        }
    }

    private func codeView(_ code: String) -> some View {
        WatchMessageScreen {
            Text("Your code")
                .font(.subheadline)
                .foregroundStyle(WatchPalette.secondary)
            // "K7P 4QX": two groups of three, read out one character at a time
            Text(verbatim: Self.grouped(code))
                .font(.system(size: 30, weight: .bold, design: .monospaced))
                .tracking(2)
                .foregroundStyle(WatchPalette.accent)
                .minimumScaleFactor(0.6)
                .lineLimit(1)
                .accessibilityLabel(Text("Code \(code.map(String.init).joined(separator: " "))"))
            Text("On your iPhone: Profile › Connect watch")
                .font(.subheadline)
                .multilineTextAlignment(.center)
            Text("Valid for 5 minutes")
                .font(.footnote)
                .foregroundStyle(WatchPalette.secondary)
        } actions: {
            glassButton("Cancel") { model.send(WatchPairingUiEventOnCodeCancelled.shared) }
        }
    }

    private func connectedView(_ state: WatchPairingUiState) -> some View {
        WatchMessageScreen {
            WatchAvatar(
                initials: state.profileInitials,
                color: state.profileColor,
                photoUrl: state.profilePhotoUrl,
                size: 48,
            )
            if let name = state.profileName, !name.isEmpty {
                Text("Ready, \(name)").messageTitle()
            } else {
                Text("Ready").messageTitle()
            }
            Text("The watch syncs on its own, even away from your iPhone.").messageBody()
        } actions: {
            prominentButton("Continue") { model.send(WatchPairingUiEventOnContinueClicked.shared) }
        }
    }

    private var signedOutView: some View {
        WatchMessageScreen {
            Text("You signed out on your iPhone").messageTitle()
            Text("The data left this watch too.").messageBody()
        } actions: {
            prominentButton("Connect again") { model.send(WatchPairingUiEventOnConnectAgainClicked.shared) }
        }
    }

    private func errorText(_ error: WatchPairingError) -> LocalizedStringKey {
        switch error {
        case .noInternet: "Connecting the watch needs the internet."
        case .tooManyAttempts: "Too many tries. Wait a few minutes and connect again."
        default: "Couldn't connect the watch. Try again."
        }
    }

    private func prominentButton(_ title: LocalizedStringKey, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title).frame(maxWidth: .infinity)
        }
        .buttonStyle(.glassProminent)
        .prominentLabel()
    }

    private func glassButton(_ title: LocalizedStringKey, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title).frame(maxWidth: .infinity)
        }
        .buttonStyle(.glass)
    }

    /// "K7P 4QX", as the code reads on every device.
    private static func grouped(_ code: String) -> String {
        guard code.count == 6 else { return code }
        return "\(code.prefix(3)) \(code.suffix(3))"
    }
}

/// 15.10: the code on its way to the iPhone, which approves it on its own.
private struct WatchPairingWaitingView: View {
    var body: some View {
        VStack(spacing: 12) {
            ProgressView()
                .controlSize(.large)
                .tint(WatchPalette.accent)
            Text("Getting your account from your iPhone…")
                .font(.headline)
                .multilineTextAlignment(.center)
        }
        .padding(.horizontal, 16)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .accessibilityElement(children: .combine)
    }
}

/// A message centred at the top with its buttons at the end, scrolling as one with the Digital Crown when the text is
/// long or the font large.
struct WatchMessageScreen<Content: View, Actions: View>: View {
    @ViewBuilder let content: Content
    @ViewBuilder let actions: Actions

    var body: some View {
        ScrollView {
            VStack(spacing: 4) {
                content
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 4)
            VStack(spacing: 6) {
                actions
            }
            .padding(.top, 10)
        }
        .scrollBounceBehavior(.basedOnSize)
    }
}

private extension Text {
    func messageTitle() -> some View {
        font(.headline).multilineTextAlignment(.center)
    }

    func messageBody() -> some View {
        font(.footnote)
            .foregroundStyle(WatchPalette.secondary)
            .multilineTextAlignment(.center)
    }
}
