import Observation
import Shared
import SwiftUI

@Observable
final class ProfileModel {
    private(set) var state: ProfileUiState
    private(set) var didCopy = false
    /// The JSON of "Export my data", until the save sheet closes.
    var exportJson: String?

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
        case let export as ProfileUiActionSaveExport:
            exportJson = export.json
        case is ProfileUiActionLaunchNativeSignIn:
            Task {
                switch await appleSignIn.signIn() {
                case let .success(idToken, rawNonce, fullName, authorizationCode):
                    send(ProfileUiEventOnIdTokenReceived(
                        provider: .apple,
                        idToken: idToken,
                        rawNonce: rawNonce,
                        fullName: fullName,
                        authorizationCode: authorizationCode,
                    ))
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
/// editor; the Mac edits name, photo and color right here, in Ajustes (⌘,), as the canvas draws it.
/// Lying down (an iPhone, an iPad, an iPhone Duo open) who you are, your account and the rest go in one column and
/// the keys and preferences in a second one, each scrolling on its own; an iPad lying down draws the first one as the
/// panel of a Settings split. Upright with an iPad's room it is one 520 pt column.
struct ProfileView: View {
    #if os(macOS)
    /// Ajustes' own (`SettingsView`), made once per profile: SwiftUI rebuilds this view's value whenever the window
    /// redraws.
    let model: ProfileModel
    /// The section Ajustes' sidebar picked, which the form scrolls to.
    var pane: ProfileSettingsPane?
    /// Bumped on every click in Ajustes' sidebar, the section already picked included, so the form scrolls back to it.
    var scrollRequest: Int
    /// Made once the settings appear, as the other screens make theirs: SwiftUI rebuilds this view's value
    /// whenever its parent redraws.
    @State private var notificationModel: NotificationSettingsModel?

    init(model: ProfileModel, pane: ProfileSettingsPane?, scrollRequest: Int) {
        self.model = model
        self.pane = pane
        self.scrollRequest = scrollRequest
    }
    #else
    @State private var model = ProfileModel()
    /// The screen's own size, which decides the columns.
    @State private var layout = ScreenLayout()
    /// The iPhone Duo's fold, where the two columns sit on each side of it rather than as a Settings split.
    @State private var division: FoldDivision?
    #endif

    var body: some View {
        content
            .navigationTitle("Profile")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button("Who sees your profile", systemImage: "info.circle") {
                        model.send(ProfileUiEventOnVisibilityInfoClicked.shared)
                    }
                }
            }
            .profileDialogs(model: model)
            #if os(macOS)
            .onAppear {
                if notificationModel == nil { notificationModel = NotificationSettingsModel() }
            }
            #endif
            .dataExporter(json: Binding(
                get: { model.exportJson },
                set: { model.exportJson = $0 },
            ))
    }

    #if os(macOS)
    private var content: some View {
        ScrollViewReader { proxy in
            Form {
                OfflineSection(model: model)
                IdentitySection(model: model)
                    .id(ProfileSettingsPane.profile)
                // The Mac's settings list the keys right under the profile, as the canvas draws them
                PaymentKeysSection(model: model)
                    .id(ProfileSettingsPane.keys)
                AccountSection(model: model)
                    .id(ProfileSettingsPane.account)
                PreferencesSection(model: model)
                    .id(ProfileSettingsPane.appearance)
                // The Mac's settings show the toggles right here, as the canvas draws them
                if let notificationModel {
                    NotificationSettingsSections(model: notificationModel)
                        .id(ProfileSettingsPane.notifications)
                }
                AboutSection(model: model)
                    .id(ProfileSettingsPane.privacy)
                SignOutSection(model: model)
                if model.state.showsDeleteAccount {
                    DeleteAccountSection(model: model)
                }
            }
            .formStyle(.grouped)
            // Also as the form comes back from Ajustes' Categories, which drew the map in its place
            .onChange(of: scrollRequest, initial: true) {
                guard let pane, scrollRequest > 0 else { return }
                withAnimation { proxy.scrollTo(pane, anchor: .top) }
            }
        }
    }
    #else
    private var content: some View {
        Group {
            if layout.usesTwoColumns {
                TwoColumnsLayout(
                    firstWidth: layout.isLarge ? 360 : 300,
                    spacing: layout.isLarge ? 48 : 28,
                    maxWidth: layout.isLarge ? 1048 : ScreenLayout.twoColumnsMaxWidth,
                ) {
                    identityForm
                } second: {
                    Form {
                        PaymentKeysSection(model: model)
                        PreferencesSection(model: model)
                    }
                    .formStyle(.grouped)
                }
            } else {
                Form {
                    OfflineSection(model: model)
                    IdentitySection(model: model)
                    AccountSection(model: model)
                    PaymentKeysSection(model: model)
                    PreferencesSection(model: model)
                    AboutSection(model: model)
                    SignOutSection(model: model)
                    if model.state.showsDeleteAccount {
                        DeleteAccountSection(model: model)
                    }
                }
                .formStyle(.grouped)
                .frame(maxWidth: layout.isLarge ? ScreenLayout.singleColumnMaxWidth : .infinity)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.groupedBackground)
        .measuringScreenLayout($layout)
        .readingFoldDivision($division)
    }

    /// The first column lying down: who you are, the account, privacy and signing out. With an iPad's room and no
    /// fold down the middle it is the Settings split's panel, on glass, as the canvas draws it.
    @ViewBuilder
    private var identityForm: some View {
        let form = Form {
            OfflineSection(model: model)
            IdentitySection(model: model)
            AccountSection(model: model)
            AboutSection(model: model)
            SignOutSection(model: model)
            if model.state.showsDeleteAccount {
                DeleteAccountSection(model: model)
            }
        }
        .formStyle(.grouped)
        if layout.showsListDetail, division == nil {
            form
                .scrollContentBackground(.hidden)
                .glassEffect(.regular, in: .rect(cornerRadius: 30))
                .padding(.bottom, 16)
        } else {
            form
        }
    }
    #endif
}

/// "No internet": what changes is kept on the device and goes up later.
private struct OfflineSection: View {
    let model: ProfileModel

    var body: some View {
        if model.state.showsOfflineNotice {
            Section {
                GlassNotice(
                    title: "No internet.",
                    message: "Edit freely: it stays saved on this \(DeviceName.current) and goes up when the connection is back.",
                )
            }
            .listRowInsets(EdgeInsets())
            .listRowBackground(Color.clear)
        }
    }
}

/// Appearance, and on iPhone and iPad the row that opens the notification settings (the Mac shows them inline).
private struct PreferencesSection: View {
    let model: ProfileModel

    var body: some View {
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
            #if os(iOS)
            Button {
                model.send(ProfileUiEventOnNotificationsClicked.shared)
            } label: {
                LabeledContent("Notifications") {
                    HStack(spacing: 6) {
                        Text("\(Int(model.state.activeNotificationCount)) active")
                        Image(systemName: "chevron.forward")
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(.tertiary)
                    }
                }
            }
            .foregroundStyle(.primary)
            #endif
            CategoryMapRow(model: model)
        }
    }
}

private struct SignOutSection: View {
    let model: ProfileModel

    var body: some View {
        let state = model.state
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
                ConnectedAccountRow(model: model, provider: provider)
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

/// The Google or Apple row. On iPhone and iPad the whole row opens the "Disconnect" alert; the Mac
/// has a "Disconnect…" button, as its settings rows do.
private struct ConnectedAccountRow: View {
    let model: ProfileModel
    let provider: AccountProvider

    var body: some View {
        let state = model.state
        #if os(macOS)
        LabeledContent {
            HStack(spacing: 12) {
                Text("Connected").foregroundStyle(Palette.accent)
                if state.isUnlinking {
                    ProgressView().controlSize(.small)
                } else {
                    Button("Disconnect…") { model.send(ProfileUiEventOnAccountClicked.shared) }
                }
            }
        } label: {
            label
        }
        #else
        Button {
            model.send(ProfileUiEventOnAccountClicked.shared)
        } label: {
            LabeledContent {
                if state.isUnlinking {
                    ProgressView().controlSize(.small)
                } else {
                    HStack(spacing: 8) {
                        Text("Connected").foregroundStyle(Palette.accent)
                        Image(systemName: "chevron.right")
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(.tertiary)
                            .accessibilityHidden(true)
                    }
                }
            } label: {
                label
            }
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityHint("Disconnect")
        #endif
    }

    @ViewBuilder
    private var label: some View {
        Text(verbatim: provider.displayName).foregroundStyle(.primary)
        if let email = model.state.maskedEmail {
            Text("\(email) · only you see it")
        }
    }
}

/// "Keys to receive payments": each key masked, its default marked, and a menu that asks Pix or Bitcoin
/// first. The Mac adds a key with the + under the list, as its settings lists do.
private struct PaymentKeysSection: View {
    let model: ProfileModel

    var body: some View {
        Section {
            ForEach(model.state.paymentKeys, id: \.id) { key in
                Button {
                    model.send(ProfileUiEventOnPaymentKeyClicked(keyId: key.id))
                } label: {
                    PaymentKeyRow(key: key)
                }
                .buttonStyle(.plain)
            }
            #if os(macOS)
            HStack {
                AddPaymentKeyMenu(model: model) {
                    Image(systemName: "plus")
                        .frame(width: 20, height: 20)
                        .contentShape(.rect)
                }
                .menuStyle(.borderlessButton)
                .menuIndicator(.hidden)
                .fixedSize()
                .accessibilityLabel("Add a Pix or Bitcoin key")
                Spacer()
            }
            #else
            AddPaymentKeyMenu(model: model) {
                Label("Add a Pix or Bitcoin key", systemImage: "plus.circle.fill")
            }
            #endif
        } header: {
            Text("Keys to receive payments")
        } footer: {
            Text("Whoever owes you sees your default key when settling up.")
        }
    }
}

private struct AddPaymentKeyMenu<MenuLabel: View>: View {
    let model: ProfileModel
    @ViewBuilder let label: MenuLabel

    var body: some View {
        Menu {
            Button("Pix key") {
                model.send(ProfileUiEventOnAddPaymentKeyClicked(method: .pix))
            }
            Button("Bitcoin key") {
                model.send(ProfileUiEventOnAddPaymentKeyClicked(method: .bitcoin))
            }
        } label: {
            label
        }
    }
}

/// A key as the profile lists it: its method and type, the masked value in a monospaced font, "Default".
private struct PaymentKeyRow: View {
    let key: PaymentKeyItem

    var body: some View {
        let isPix = key.type.method == .pix
        HStack(spacing: 12) {
            Image(systemName: key.type.symbol)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(isPix ? Palette.onPixTile : Palette.onBitcoinTile)
                .frame(width: 30, height: 30)
                .background(isPix ? Palette.pixTile : Palette.bitcoinTile, in: .rect(cornerRadius: 8))
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                key.type.rowTitle
                    .foregroundStyle(.primary)
                // Why: on a narrow row only the masked value gives way, so the format suffix stays readable.
                HStack(spacing: 0) {
                    Text(verbatim: key.maskedValue)
                        .truncationMode(.middle)
                    Text(verbatim: key.format.suffix)
                        .fixedSize()
                }
                .font(.footnote.monospaced())
                .foregroundStyle(.secondary)
                .lineLimit(1)
            }
            Spacer(minLength: 8)
            if key.isDefault {
                Text("Default")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Palette.accent)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(Palette.accent.opacity(0.14), in: .rect(cornerRadius: 8))
            }
            Image(systemName: "chevron.right")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.tertiary)
                .accessibilityHidden(true)
        }
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
    }
}

private extension PaymentKeyType {
    var rowTitle: Text {
        switch self {
        case .cpf: Text(verbatim: "Pix · CPF")
        case .phone: Text("Pix · Phone")
        case .email: Text("Pix · Email")
        case .random: Text("Pix · Random key")
        case .lightning: Text(verbatim: "Bitcoin · Lightning")
        default: Text(verbatim: "Bitcoin · On-chain")
        }
    }

    var symbol: String {
        switch self {
        case .lightning: "bolt.fill"
        case .onchain: "link"
        default: "rhombus"
        }
    }
}

private extension PaymentKeyFormat {
    /// "bc1p5d7r…sxg3297 · Taproot": the formats a type alone doesn't tell, the same words in every language.
    var suffix: String {
        switch self {
        case .segwit: " · SegWit"
        case .taproot: " · Taproot"
        case .lnurl: " · LNURL"
        default: ""
        }
    }
}

private struct AboutSection: View {
    let model: ProfileModel

    var body: some View {
        Section("Privacy and about") {
            ExportRow(model: model)
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

/// "Export my data": a JSON file the save sheet writes, which works offline from the local copy.
private struct ExportRow: View {
    let model: ProfileModel

    var body: some View {
        #if os(macOS)
        LabeledContent {
            Button("Export…") { model.send(ProfileUiEventOnExportClicked.shared) }
        } label: {
            Text("Export my data")
            Text("A JSON file with your profile and keys")
        }
        #else
        Button {
            model.send(ProfileUiEventOnExportClicked.shared)
        } label: {
            LabeledContent {
                Image(systemName: "square.and.arrow.up")
                    .foregroundStyle(Palette.accent)
                    .accessibilityHidden(true)
            } label: {
                Text("Export my data").foregroundStyle(.primary)
                Text("A JSON file with your profile and keys")
            }
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        #endif
    }
}

/// "Delete account and data", apart from Sign out at the very end, as the stores ask. The Mac shows it
/// as a settings row with a "Delete account…" button.
private struct DeleteAccountSection: View {
    let model: ProfileModel

    var body: some View {
        #if os(macOS)
        Section {
            LabeledContent {
                Button("Delete account…", role: .destructive) {
                    model.send(ProfileUiEventOnDeleteAccountClicked.shared)
                }
            } label: {
                Text("Delete account and data")
                    .fontWeight(.semibold)
                    .foregroundStyle(Palette.negative)
                Text("Deletes your Blitz Split account. This can't be undone.")
            }
        }
        #else
        Section {
            Button(role: .destructive) {
                model.send(ProfileUiEventOnDeleteAccountClicked.shared)
            } label: {
                Text("Delete account and data")
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity)
            }
            .listRowBackground(Palette.negative.opacity(0.12))
        } footer: {
            Text("Deletes your Blitz Split account. This can't be undone.")
                .frame(maxWidth: .infinity)
                .multilineTextAlignment(.center)
        }
        #endif
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

/// Preferences › "Category map" (flow 9): pushed on iPhone and iPad; on the Mac, whose profile is Ajustes, it picks
/// Ajustes' Categories section instead (`AppModel`).
private struct CategoryMapRow: View {
    let model: ProfileModel

    var body: some View {
        Button {
            model.send(ProfileUiEventOnCategoryMapClicked.shared)
        } label: {
            LabeledContent {
                Image(systemName: "chevron.forward")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.tertiary)
            } label: {
                Text("Category map")
                Text("The words that pick a bill's category")
            }
        }
        .foregroundStyle(.primary)
    }
}
