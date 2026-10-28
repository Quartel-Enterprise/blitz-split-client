import Observation
import Shared
import SwiftUI

@Observable
final class ProfileModel {
    private(set) var state: ProfileUiState
    private(set) var didCopy = false

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createProfileViewModel()
    @ObservationIgnored private let watches = WatchBag()
    @ObservationIgnored private let appleSignIn = AppleSignIn()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! ProfileUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            self?.handle(action)
        })
    }

    func send(_ event: any ProfileUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    private func handle(_ action: any ProfileUiAction) {
        switch action {
        case let copy as ProfileUiActionCopyToClipboard:
            RecoveryCodeSaver.copy(copy.formattedCode)
            didCopy = true
        case let open as ProfileUiActionOpenUrl:
            guard let url = URL(string: open.url) else { return }
            #if os(iOS)
            UIApplication.shared.open(url)
            #else
            NSWorkspace.shared.open(url)
            #endif
        case is ProfileUiActionLaunchNativeSignIn:
            Task {
                switch await appleSignIn.signIn() {
                case let .success(idToken, rawNonce, fullName):
                    send(ProfileUiEventOnIdTokenReceived(provider: .apple, idToken: idToken, rawNonce: rawNonce, fullName: fullName))
                case .cancelled:
                    send(ProfileUiEventOnNativeSignInCancelled.shared)
                case .failed:
                    send(ProfileUiEventOnNativeSignInFailed.shared)
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

/// The profile: a grouped form pushed over the groups. On iPhone and iPad the identity row opens the
/// editor; the Mac edits name, photo and color right here, as the canvas draws it.
struct ProfileView: View {
    @State private var model = ProfileModel()

    var body: some View {
        let state = model.state
        Form {
            if state.showsOfflineNotice {
                Section {
                    GlassNotice(
                        title: "No internet.",
                        message: "Edit freely: it stays saved on this \(DeviceName.current) and goes up when the connection is back.",
                    )
                }
                .listRowInsets(EdgeInsets())
                .listRowBackground(Color.clear)
            }
            IdentitySection(model: model)
            AccountSection(model: model)
            Section("Preferences") {
                Picker(
                    "Appearance",
                    selection: Binding(
                        get: { model.state.theme },
                        set: { model.send(ProfileUiEventOnThemeSelected(theme: $0)) },
                    ),
                ) {
                    Text("System").tag(ThemePreference.system)
                    Text("Light").tag(ThemePreference.light)
                    Text("Dark").tag(ThemePreference.dark)
                }
            }
            AboutSection(model: model)
            Section {
                Button(role: .destructive) {
                    model.send(ProfileUiEventOnSignOutClicked.shared)
                } label: {
                    Text(state.hasAccount ? "Sign out" : "Erase profile and sign out")
                        .frame(maxWidth: .infinity)
                }
                .disabled(!state.canSignOut)
            }
        }
        .formStyle(.grouped)
        .navigationTitle("Profile")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Who sees your profile", systemImage: "info.circle") {
                    model.send(ProfileUiEventOnVisibilityInfoClicked.shared)
                }
            }
        }
        .profileDialogs(model: model)
    }
}

private struct IdentitySection: View {
    let model: ProfileModel

    #if os(macOS)
    @State private var editModel = EditProfileModel()
    #endif

    var body: some View {
        #if os(macOS)
        EditProfileSections(model: editModel, isInline: true)
        #else
        let state = model.state
        Section {
            Button {
                model.send(ProfileUiEventOnEditClicked.shared)
            } label: {
                HStack(spacing: 14) {
                    AvatarPhoto(
                        initial: state.avatarInitial,
                        color: state.avatarColor,
                        photoUrl: state.photoUrl,
                        size: 60,
                    )
                    .accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: 3) {
                        Text(verbatim: state.displayName)
                            .font(.title3.bold())
                            .foregroundStyle(.primary)
                        if state.showsProfilePending {
                            Text("Changed · waiting to upload")
                                .font(.subheadline)
                                .foregroundStyle(Palette.negative)
                        } else {
                            Text("Name, photo and avatar color")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                    }
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(.tertiary)
                }
                .contentShape(.rect)
            }
            .buttonStyle(.plain)
            .accessibilityHint("Edit profile")
        }
        #endif
    }
}

private struct AccountSection: View {
    let model: ProfileModel

    var body: some View {
        let state = model.state
        Section("Account and sync") {
            if let provider = state.accountProvider {
                LabeledContent {
                    Text("Connected").foregroundStyle(Palette.accent)
                } label: {
                    Text(verbatim: provider.displayName)
                    if let email = state.maskedEmail {
                        Text("\(email) · only you see it")
                    }
                }
            } else {
                LabeledContent {
                    if !state.isConnectExpanded {
                        Button("Connect") {
                            withAnimation { model.send(ProfileUiEventOnConnectClicked.shared) }
                        }
                        .buttonStyle(.bordered)
                        .disabled(!state.canConnect)
                    }
                } label: {
                    Text("No account")
                    if state.hasAnonymousId {
                        Text("Anonymous ID of this \(DeviceName.current)")
                    } else {
                        Text("Saved only on this \(DeviceName.current) for now")
                    }
                }
                if state.showsConnectButtons {
                    // Apple first on iOS and macOS, as the design asks
                    VStack(spacing: 10) {
                        AppleIDButton(label: .continueWith, isEnabled: state.areAccountButtonsEnabled) {
                            model.send(ProfileUiEventOnProviderClicked(provider: .apple))
                        }
                        GoogleButton(
                            title: "Continue with Google",
                            isEnabled: state.areAccountButtonsEnabled,
                            isLoading: state.connectingProvider == .google,
                        ) {
                            model.send(ProfileUiEventOnProviderClicked(provider: .google))
                        }
                        if state.hasSignInFailed {
                            Text("Couldn't connect right now. Try again.")
                                .font(.footnote)
                                .foregroundStyle(Palette.negative)
                        }
                    }
                    .padding(.vertical, 6)
                }
            }
            if state.showsRecoveryCodeRow {
                Button {
                    model.send(ProfileUiEventOnRecoveryCodeClicked.shared)
                } label: {
                    LabeledContent {
                        Image(systemName: "chevron.right").foregroundStyle(.tertiary)
                    } label: {
                        Text("Recovery code").foregroundStyle(.primary)
                        Text("View and keep · restores on another device")
                    }
                    .contentShape(.rect)
                }
                .buttonStyle(.plain)
            }
            if let syncStatus = state.syncStatus {
                LabeledContent {
                    if syncStatus == .syncing {
                        ProgressView().controlSize(.small)
                    } else if state.canRetrySync {
                        Button("Try again") { model.send(ProfileUiEventOnRetrySyncClicked.shared) }
                            .buttonStyle(.borderless)
                    }
                } label: {
                    Text("Sync")
                    syncText(syncStatus, hasPendingChanges: state.hasPendingChanges, count: Int(state.pendingChangeCount))
                }
            }
            if state.isSignOutScheduled {
                LabeledContent {
                    Button("Cancel") { model.send(ProfileUiEventOnCancelScheduledSignOutClicked.shared) }
                        .buttonStyle(.borderless)
                } label: {
                    Text("Sign out scheduled")
                    Text("Signs out as soon as everything is uploaded")
                }
            }
        }
    }

    private func syncText(_ status: ProfileSyncStatus, hasPendingChanges: Bool, count: Int) -> Text {
        switch status {
        case .syncing: Text("Syncing…")
        case .failed: Text("No response from the server")
        default: hasPendingChanges ? Text("\(count) changes in the queue") : Text("Waiting to sync")
        }
    }
}

private struct AboutSection: View {
    let model: ProfileModel

    var body: some View {
        Section("Privacy and about") {
            Button("Who sees your profile") {
                model.send(ProfileUiEventOnVisibilityInfoClicked.shared)
            }
            Button {
                model.send(ProfileUiEventOnPrivacyPolicyClicked.shared)
            } label: {
                Label("Privacy policy", systemImage: "arrow.up.right").labelStyle(TrailingIconLabelStyle())
            }
            Button {
                model.send(ProfileUiEventOnTermsClicked.shared)
            } label: {
                Label("Terms of use", systemImage: "arrow.up.right").labelStyle(TrailingIconLabelStyle())
            }
            LabeledContent("Version", value: Bundle.main.appVersion)
        }
    }
}

private struct TrailingIconLabelStyle: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack {
            configuration.title
            Spacer()
            configuration.icon.foregroundStyle(.tertiary)
        }
        .contentShape(.rect)
    }
}

extension AccountProvider {
    /// A product name, the same in every language.
    var displayName: String {
        self == .apple ? "Apple" : "Google"
    }
}

private extension Bundle {
    var appVersion: String {
        infoDictionary?["CFBundleShortVersionString"] as? String ?? ""
    }
}
