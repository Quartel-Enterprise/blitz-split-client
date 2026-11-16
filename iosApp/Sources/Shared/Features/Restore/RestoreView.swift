import Observation
import Shared
import SwiftUI

@Observable
final class RestoreModel {
    private(set) var state: RestoreUiState
    var isScanningQr = false
    /// The word field the ViewModel moves the keyboard to, once a word is completed or picked.
    private(set) var focusRequest: Int?

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
        case let focus as RestoreUiActionFocusWord:
            focusRequest = Int(focus.index)
        case is RestoreUiActionLaunchNativeSignIn:
            Task {
                switch await appleSignIn.signIn() {
                case let .success(idToken, rawNonce, fullName, authorizationCode):
                    send(RestoreUiEventOnIdTokenReceived(
                        provider: .apple,
                        idToken: idToken,
                        rawNonce: rawNonce,
                        fullName: fullName,
                        authorizationCode: authorizationCode,
                    ))
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
                // An iPhone Duo open splits on its fold: the accounts on one side, the code on the other
                TwoColumnsLayout {
                    ScrollView {
                        RestoreIntro(model: model, isLarge: layout.isLarge)
                    }
                    .scrollBounceBehavior(.basedOnSize)
                } second: {
                    VStack(spacing: 16) {
                        ScrollView {
                            RestoreCodeSection(model: model)
                        }
                        .scrollBounceBehavior(.basedOnSize)
                        RestoreActions(model: model)
                    }
                }
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
                    // "Recover" stays on screen, above the keyboard when it is open
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
            .foldSheetPlacement()
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
                Text("Bring your groups back")
                    .font(isLarge ? .largeTitle.bold() : .title.bold())
                Text("Signed in with an account before? Just sign in again. If not, use the recovery code or the QR code from your old device.")
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
                Text("Couldn't sign in right now. Try again.")
                    .font(.footnote)
                    .foregroundStyle(Palette.negative)
            }
            if state.showsOfflineNotice {
                ConnectionCard(
                    title: "No connection",
                    message: "Signing in with an account or a code needs the internet to download your groups. What you typed is kept.",
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
            SectionCaption(text: "Or use the recovery code")
            GroupedCard {
                RestoreWordFields(model: model)
                    .padding(12)
            }
            if !state.suggestions.isEmpty {
                RestoreWordSuggestions(model: model)
                    .padding(.horizontal, 4)
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

    private var scanTitle: LocalizedStringKey {
        #if os(iOS)
        "Scan the QR code from your old device"
        #else
        "Scan the QR code with the camera"
        #endif
    }

    private var scanHint: LocalizedStringKey {
        #if os(iOS)
        "There: Profile › Recovery › Show QR code"
        #else
        "On the old device: Profile › Recovery › Show QR code"
        #endif
    }

    private func isCodeError(_ error: RestoreError?) -> Bool {
        error != nil && error != .signInFailed
    }

    private func codeHint(for error: RestoreError?) -> LocalizedStringKey {
        switch error {
        case .invalidCode: "Code not found. Check the words and their order, then try again."
        case .tooManyAttempts: "Too many attempts. Wait a few minutes and try again."
        case .unknown: "Something went wrong. Try again."
        default: "The 12 words in order. Paste them all into one field to fill every field."
        }
    }
}

/// One field per word, so a wrong word is pointed at instead of the whole code. Pasting the words into any field fills
/// them all, and the keyboard's next moves on. Two columns on an iPhone, more where there is room.
private struct RestoreWordFields: View {
    let model: RestoreModel

    @FocusState private var focusedWord: Int?

    var body: some View {
        let words = model.state.words
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 132), spacing: 8)], spacing: 8) {
            ForEach(words.indices, id: \.self) { index in
                field(at: index, isDeadEnd: words[index].isDeadEnd, isLast: index == words.count - 1)
            }
        }
        .onChange(of: focusedWord) { _, index in
            if let index {
                model.send(RestoreUiEventOnWordFocused(index: Int32(index)))
            }
        }
        .onChange(of: model.focusRequest) { _, index in
            focusedWord = index
        }
    }

    private func field(at index: Int, isDeadEnd: Bool, isLast: Bool) -> some View {
        HStack(spacing: 6) {
            Text(verbatim: "\(index + 1)")
                .font(.subheadline)
                .monospacedDigit()
                .foregroundStyle(.secondary)
                .frame(minWidth: 20, alignment: .trailing)
                .accessibilityHidden(true)
            TextField(
                "",
                text: Binding(
                    get: { model.state.words[index].text },
                    set: { model.send(RestoreUiEventOnWordChanged(index: Int32(index), text: $0)) },
                ),
            )
            .font(.system(.body, design: .monospaced))
            .autocorrectionDisabled()
            #if os(iOS)
            .textInputAutocapitalization(.never)
            .keyboardType(.asciiCapable)
            #endif
            .focused($focusedWord, equals: index)
            .submitLabel(isLast ? .go : .next)
            .onSubmit {
                if isLast {
                    model.send(RestoreUiEventOnRestoreClicked.shared)
                } else {
                    focusedWord = index + 1
                }
            }
            .accessibilityLabel("Word \(index + 1)")
        }
        .padding(.horizontal, 10)
        .frame(minHeight: 44)
        .background(
            RoundedRectangle(cornerRadius: 10)
                .strokeBorder(isDeadEnd ? Palette.negative : Color.secondary.opacity(0.35)),
        )
    }
}

/// The words of the list that start with what the focused field holds, to tap instead of typing the rest.
private struct RestoreWordSuggestions: View {
    let model: RestoreModel

    var body: some View {
        HStack(spacing: 8) {
            ForEach(model.state.suggestions, id: \.self) { word in
                Button {
                    model.send(RestoreUiEventOnSuggestionClicked(word: word))
                } label: {
                    Text(verbatim: word)
                        .font(.system(.subheadline, design: .monospaced))
                }
                .buttonStyle(.bordered)
                .buttonBorderShape(.capsule)
            }
        }
    }
}

private struct RestoreActions: View {
    let model: RestoreModel

    var body: some View {
        let state = model.state
        VStack(spacing: 10) {
            if state.isOnline {
                PrimaryButton(title: "Recover", isEnabled: state.canRestore, isLoading: state.isRestoring) {
                    model.send(RestoreUiEventOnRestoreClicked.shared)
                }
            } else {
                // The connectivity observer flips the screen back by itself; the button reassures
                PrimaryButton(title: "Try again", systemImage: "arrow.clockwise") {
                    model.send(RestoreUiEventOnRestoreClicked.shared)
                }
                Button("Start fresh on this \(DeviceName.current)") {
                    model.send(RestoreUiEventOnStartFreshClicked.shared)
                }
                .buttonStyle(.borderless)
                .frame(minHeight: 36)
            }
        }
        .frame(maxWidth: 400)
    }
}
