import Observation
import Shared
import SwiftUI

@Observable
final class ProfileModel {
    private(set) var state: ProfileUiState
    private(set) var didCopy = false
    /// Bumped by each "Check for updates" with nothing to offer, which a toast says.
    private(set) var upToDateCount = 0
    private(set) var updateCheckFailedCount = 0
    /// Bumped by each "Copy link" of "Share profile", which a toast says.
    private(set) var linkCopiedCount = 0
    /// Bumped by each copy of "User ID", which a toast says.
    private(set) var userIdCopiedCount = 0
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
            RecoveryCodeSaver.copy(copy.code)
            didCopy = true
        case let open as ProfileUiActionOpenUrl:
            ExternalURL.open(open.url)
        case let export as ProfileUiActionSaveExport:
            exportJson = export.json
        case let share as ProfileUiActionShareProfileLink:
            SystemShare.share(String(
                localized: "Here's my profile on Blitz Split: \(share.link)",
                bundle: .appLanguage,
            ))
        case let copy as ProfileUiActionCopyProfileLink:
            Pasteboard.copy(copy.link)
            linkCopiedCount += 1
        case let copy as ProfileUiActionCopyUserId:
            Pasteboard.copy(copy.userId)
            userIdCopiedCount += 1
        case let result as ProfileUiActionShowUpdateCheckResult:
            switch result.result {
            case .upToDate: upToDateCount += 1
            case .failed: updateCheckFailedCount += 1
            // Only Google Play downloads inside the app
            default: break
            }
        // The windows already redraw in it (`AppLocale`)
        case is ProfileUiActionApplyLanguage:
            break
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
    /// The form's width, whose room past `ScreenLayout.macSettingsWidth` goes to its side margins.
    @State private var formWidth: CGFloat = 0

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
        Group {
            if model.state.isLoaded {
                content
            } else {
                // Who you are, the account and the theme not read yet: never "No account" with Connect, nor the
                // default theme, before them
                ShimmerScreen {
                    HStack(spacing: 14) {
                        ShimmerCircle(size: 64)
                        VStack(alignment: .leading, spacing: 8) {
                            ShimmerLine(width: 160, height: 18)
                            ShimmerLine(width: 110, height: 12)
                        }
                    }
                    ShimmerForm(fieldCount: 4)
                }
                .background(Palette.groupedBackground)
            }
        }
        .navigationTitle("Profile")
        .toolbar {
            // Only an account has a link. The share sheet has Copy on iPhone and iPad; the Mac's menu offers it beside
            // the sharing picker, as its share menus do
            if model.state.shareLink != nil {
                ToolbarItem(placement: .primaryAction) {
                    #if os(macOS)
                    Menu("Share profile", systemImage: "square.and.arrow.up") {
                        Button("Share profile", systemImage: "square.and.arrow.up") {
                            model.send(ProfileUiEventOnShareProfileClicked.shared)
                        }
                        Button("Copy link", systemImage: "doc.on.doc") {
                            model.send(ProfileUiEventOnCopyProfileLinkClicked.shared)
                        }
                    }
                    #else
                    Button("Share profile", systemImage: "square.and.arrow.up") {
                        model.send(ProfileUiEventOnShareProfileClicked.shared)
                    }
                    #endif
                }
            }
        }
        .profileDialogs(model: model)
        .glassToast([
            GlassToastMessage(text: "You already have the latest version.", trigger: model.upToDateCount),
            GlassToastMessage(text: "Link copied", trigger: model.linkCopiedCount),
            GlassToastMessage(text: "User ID copied", trigger: model.userIdCopiedCount),
            GlassToastMessage(
                text: "Couldn't check for updates. Check your connection and try again.",
                trigger: model.updateCheckFailedCount,
            ),
        ])
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
    /// Half the room past a settings pane's width, nil (the form's own margins) in a window narrower than that.
    private var settingsMargin: CGFloat? {
        let margin = (formWidth - ScreenLayout.macSettingsWidth) / 2
        return margin > 0 ? margin : nil
    }

    private var content: some View {
        ScrollViewReader { proxy in
            Form {
                OfflineSection(model: model)
                IdentitySection(model: model)
                    .id(ProfileSettingsPane.profile)
                // "Get the app" (14.4): always right under the profile, as the way to download later
                if let invite = model.state.appInvite {
                    AppInviteSection(model: model, invite: invite)
                        .id(ProfileSettingsPane.iphone)
                }
                // The Mac's settings list the keys right under the profile, as the canvas draws them
                PaymentKeysSection(model: model)
                    .id(ProfileSettingsPane.keys)
                PayingMethodsSection(model: model)
                    .id(ProfileSettingsPane.payingMethods)
                AccountSection(model: model)
                    .id(ProfileSettingsPane.account)
                // Right under the account it leaves, never at the end of the form (beta feedback 16)
                SignOutSection(model: model)
                PreferencesSection(model: model)
                    .id(ProfileSettingsPane.appearance)
                // The Mac's settings show the toggles right here, as the canvas draws them
                if let notificationModel {
                    NotificationSettingsSections(model: notificationModel)
                        .id(ProfileSettingsPane.notifications)
                }
                SupportSection(model: model)
                AboutSection(model: model)
                    .id(ProfileSettingsPane.privacy)
                if model.state.showsDeleteAccount {
                    DeleteAccountSection(model: model)
                }
                LegalFooter(model: model)
            }
            .formStyle(.grouped)
            // A wide Ajustes window (full screen) keeps the rows at a settings pane's width, centred, rather than
            // stretching them across it; the form itself, the scroll view, stays the window's width, so its scroll bar
            // sits at the edge and the margins scroll too
            .contentMargins(.horizontal, settingsMargin, for: .scrollContent)
            .onGeometryChange(for: CGFloat.self, of: \.size.width) { formWidth = $0 }
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
                        PayingMethodsSection(model: model)
                        PreferencesSection(model: model)
                    }
                    .formStyle(.grouped)
                }
            } else {
                Form {
                    OfflineSection(model: model)
                    IdentitySection(model: model)
                    AccountSection(model: model)
                    SignOutSection(model: model)
                    PaymentKeysSection(model: model)
                    PayingMethodsSection(model: model)
                    PreferencesSection(model: model)
                    SupportSection(model: model)
                    AboutSection(model: model)
                    if model.state.showsDeleteAccount {
                        DeleteAccountSection(model: model)
                    }
                    LegalFooter(model: model)
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

    /// The first column lying down: who you are, the account with signing out right under it, and privacy. With an iPad's room and no
    /// fold down the middle it is the Settings split's panel, on glass, as the canvas draws it.
    @ViewBuilder
    private var identityForm: some View {
        let form = Form {
            OfflineSection(model: model)
            IdentitySection(model: model)
            AccountSection(model: model)
            SignOutSection(model: model)
            // With the preferences in the other column, it still comes right before "Privacy and about"
            SupportSection(model: model)
            AboutSection(model: model)
            if model.state.showsDeleteAccount {
                DeleteAccountSection(model: model)
            }
            LegalFooter(model: model)
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

#if os(macOS)
/// Settings › iPhone (14.4): highlighted with ✕ while the invite stands, plain for good after "I downloaded it" or ✕
/// (14.4b), and "You already use the app on iPhone" once a sync found the account there (14.4c). "Show code…" opens
/// the code (14.2) as a sheet on this window.
private struct AppInviteSection: View {
    let model: ProfileModel
    let invite: ProfileAppInvite

    private var isActive: Bool { invite.status == .active }

    var body: some View {
        Section("iPhone") {
            HStack(spacing: 12) {
                Image("Logo")
                    .resizable()
                    .scaledToFit()
                    .padding(7)
                    .frame(width: 36, height: 36)
                    .foregroundStyle(isActive ? Palette.onAccent : .secondary)
                    .background(isActive ? AnyShapeStyle(Palette.accent) : AnyShapeStyle(.quaternary), in: .rect(cornerRadius: 9))
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 2) {
                    Text(isActive ? "Take Blitz Split on iPhone" : "Blitz Split on iPhone")
                        .fontWeight(isActive ? .semibold : .regular)
                    detail
                        .font(.callout)
                        .foregroundStyle(.secondary)
                }
                Spacer(minLength: 0)
                if isActive {
                    Button("Show code…") { model.send(ProfileUiEventOnAppInviteClicked.shared) }
                        .buttonStyle(.borderedProminent)
                        .prominentLabel()
                    Button("Don't show again", systemImage: "xmark") {
                        model.send(ProfileUiEventOnAppInviteDismissed.shared)
                    }
                    .labelStyle(.iconOnly)
                    .buttonStyle(.borderless)
                    .help("Don't show again")
                } else {
                    Button("Show code…") { model.send(ProfileUiEventOnAppInviteClicked.shared) }
                }
            }
        }
    }

    @ViewBuilder
    private var detail: some View {
        switch invite.status {
        case .active: Text("Add bills right away, wherever you are, even offline. And get payment notices.")
        case .inUse: Text(invite.usesIphone ? "You already use the app on iPhone." : "You already use the app on your phone.")
        default: Text("Code to download it on the App Store.")
        }
    }
}
#endif

/// Appearance, bitcoin in satoshis, and on iPhone and iPad the row that opens the notification settings (the Mac shows
/// them inline).
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
            Picker(
                selection: Binding(
                    get: { model.state.language },
                    set: { model.send(ProfileUiEventOnLanguageSelected(language: $0)) },
                ),
            ) {
                ForEach(model.state.languageOptions, id: \.self) { language in
                    LanguageName(language: language).tag(language)
                }
            } label: {
                Text("Language")
            } currentValueLabel: {
                // The row shows the pick by its own name alone, the menu's subtitle being for choosing
                LanguageName(language: model.state.language, showsTranslation: false)
            }
            Picker(
                selection: Binding(
                    get: { model.state.timeFormat },
                    set: { model.send(ProfileUiEventOnTimeFormatSelected(timeFormat: $0)) },
                ),
            ) {
                ForEach(TimeFormatPreference.entries, id: \.self) { timeFormat in
                    TimeFormatName(timeFormat: timeFormat).tag(timeFormat)
                }
            } label: {
                Text("Time format")
            } currentValueLabel: {
                // The row shows the pick alone, the menu's example being for choosing
                TimeFormatName(timeFormat: model.state.timeFormat, showsSample: false)
            }
            .disabled(!model.state.isLoaded)
            Toggle(
                isOn: Binding(
                    get: { model.state.showsBitcoinInSats },
                    set: { model.send(ProfileUiEventOnBitcoinInSatsToggled(isOn: $0)) },
                ),
            ) {
                Text("Bitcoin in satoshis")
                Text("Shows and types bitcoin amounts in sats (1 ₿ = 100,000,000 sats).")
            }
            .disabled(!model.state.isLoaded)
            #if os(iOS)
            Button {
                model.send(ProfileUiEventOnNotificationsClicked.shared)
            } label: {
                LabeledContent("Notifications") {
                    HStack(spacing: 6) {
                        // The choices of a restored account come with its first full sync: no count before them
                        if let count = model.state.activeNotificationCount.loaded(as: KotlinInt.self) {
                            Text("\(Int(count.int32Value)) active")
                        } else {
                            ShimmerLine(width: 56, height: 12)
                                .shimmering()
                        }
                        Image(systemName: "chevron.forward")
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(.tertiary)
                    }
                }
            }
            .foregroundStyle(.primary)
            #endif
            // The code a watch shows when its phone is out of reach (15.14); only with an account on the server
            if model.state.showsConnectWatch {
                ConnectWatchRow(model: model)
            }
            CategoryMapRow(model: model)
        }
    }
}

/// An option of Profile's "Time format", with how 2:30 in the afternoon reads in it under it ("14:30", "2:30 PM"; the
/// device's own clock for System), which a menu draws as the item's subtitle.
private struct TimeFormatName: View {
    let timeFormat: TimeFormatPreference
    var showsSample = true

    var body: some View {
        // One view, so the Picker takes it as one option, its second line as the menu item's subtitle
        VStack(alignment: .leading) {
            switch timeFormat {
            case .twentyFourHour: Text("24-hour")
            case .twelveHour: Text("12-hour")
            // Its own key: "System" agrees with the format, not the theme, in Russian and Ukrainian
            default: Text("System time format")
            }
            if showsSample {
                Text(verbatim: sample)
            }
        }
    }

    private var sample: String {
        let sample = Calendar.current.date(bySettingHour: 14, minute: 30, second: 0, of: .now) ?? .now
        let locale = ClockFormat.shared.locale(from: .autoupdatingCurrent, as: timeFormat)
        return sample.formatted(Date.FormatStyle(date: .omitted, time: .shortened, locale: locale))
    }
}

/// Each language in its own language, the way its speakers look for it (Pierre's call, beta feedback), with the region
/// spelled out for the ones written for a region ("Português (Brasil)", "Español (España)"), and under it its name in the app's language,
/// unless both read the same ("Italiano"). A menu draws the second line as the item's subtitle.
private struct LanguageName: View {
    let language: AppLanguage
    var showsTranslation = true

    var body: some View {
        // One view, so the Picker takes it as one option, its second line as the menu item's subtitle
        VStack(alignment: .leading) {
            if let native = nativeName, let tag = language.tag {
                Text(Self.spoken(native, in: tag))
                if showsTranslation, translatedName != native {
                    Text(verbatim: translatedName)
                }
            } else {
                Text("System")
            }
        }
    }

    /// The same in every language of the app, so not in the string catalog.
    private var nativeName: String? {
        switch language {
        case .english: "English (US)"
        case .englishUk: "English (UK)"
        case .englishAustralia: "English (Australia)"
        case .englishCanada: "English (Canada)"
        case .portuguese: "Português (Brasil)"
        case .portuguesePortugal: "Português (Portugal)"
        case .spanish: "Español (Latinoamérica)"
        case .spanishSpain: "Español (España)"
        case .french: "Français"
        case .german: "Deutsch"
        case .italian: "Italiano"
        case .polish: "Polski"
        case .dutch: "Nederlands"
        case .japanese: "日本語"
        case .turkish: "Türkçe"
        case .indonesian: "Bahasa Indonesia"
        case .russian: "Русский"
        case .ukrainian: "Українська"
        case .korean: "한국어"
        case .hindi: "हिन्दी"
        case .arabic: "العربية"
        case .chineseSimplified: "中文（简体）"
        case .hebrew: "עברית"
        case .thai: "ไทย"
        case .vietnamese: "Tiếng Việt"
        case .chineseTraditional: "中文（繁體）"
        case .swedish: "Svenska"
        case .czech: "Čeština"
        case .romanian: "Română"
        case .bengali: "বাংলা"
        case .malay: "Bahasa Melayu"
        case .greek: "Ελληνικά"
        case .croatian: "Hrvatski"
        case .hungarian: "Magyar"
        case .urdu: "اردو"
        case .filipino: "Filipino"
        case .serbian: "Srpski"
        case .slovak: "Slovenčina"
        case .persian: "فارسی"
        case .tamil: "தமிழ்"
        case .norwegian: "Norsk bokmål"
        case .telugu: "తెలుగు"
        case .bulgarian: "Български"
        case .marathi: "मराठी"
        case .swahili: "Kiswahili"
        case .catalan: "Català"
        case .danish: "Dansk"
        case .finnish: "Suomi"
        case .latvian: "Latviešu"
        case .estonian: "Eesti"
        case .gujarati: "ગુજરાતી"
        case .slovenian: "Slovenščina"
        case .lithuanian: "Lietuvių"
        case .punjabi: "ਪੰਜਾਬੀ"
        case .malayalam: "മലയാളം"
        default: nil
        }
    }

    private var translatedName: String {
        switch language {
        case .english: String(
            localized: "English (US)",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .englishUk: String(
            localized: "English (UK)",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .englishAustralia: String(
            localized: "English (Australia)",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .englishCanada: String(
            localized: "English (Canada)",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .portuguese: String(
            localized: "Portuguese (Brazil)",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .portuguesePortugal: String(
            localized: "Portuguese (Portugal)",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .spanish: String(
            localized: "Spanish (Latin America)",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .spanishSpain: String(
            localized: "Spanish (Spain)",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .french: String(
            localized: "French",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .german: String(
            localized: "German",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .italian: String(
            localized: "Italian",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .polish: String(
            localized: "Polish",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .dutch: String(
            localized: "Dutch",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .japanese: String(
            localized: "Japanese",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .turkish: String(
            localized: "Turkish",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .indonesian: String(
            localized: "Indonesian",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .russian: String(
            localized: "Russian",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .ukrainian: String(
            localized: "Ukrainian",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .korean: String(
            localized: "Korean",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .hindi: String(
            localized: "Hindi",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .arabic: String(
            localized: "Arabic",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .chineseSimplified: String(
            localized: "Chinese (Simplified)",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .hebrew: String(
            localized: "Hebrew",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .thai: String(
            localized: "Thai",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .vietnamese: String(
            localized: "Vietnamese",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .chineseTraditional: String(
            localized: "Chinese (Traditional)",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .swedish: String(
            localized: "Swedish",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .czech: String(
            localized: "Czech",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .romanian: String(
            localized: "Romanian",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .bengali: String(
            localized: "Bengali",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .malay: String(
            localized: "Malay",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .greek: String(
            localized: "Greek",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .croatian: String(
            localized: "Croatian",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .hungarian: String(
            localized: "Hungarian",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .urdu: String(
            localized: "Urdu",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .filipino: String(
            localized: "Filipino",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .serbian: String(
            localized: "Serbian",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .slovak: String(
            localized: "Slovak",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .persian: String(
            localized: "Persian",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .tamil: String(
            localized: "Tamil",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .norwegian: String(
            localized: "Norwegian Bokmål",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .telugu: String(
            localized: "Telugu",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .bulgarian: String(
            localized: "Bulgarian",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .marathi: String(
            localized: "Marathi",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .swahili: String(
            localized: "Swahili",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .catalan: String(
            localized: "Catalan",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .danish: String(
            localized: "Danish",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .finnish: String(
            localized: "Finnish",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .latvian: String(
            localized: "Latvian",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .estonian: String(
            localized: "Estonian",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .gujarati: String(
            localized: "Gujarati",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .slovenian: String(
            localized: "Slovenian",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .lithuanian: String(
            localized: "Lithuanian",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .punjabi: String(
            localized: "Punjabi",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        case .malayalam: String(
            localized: "Malayalam",
            bundle: .appLanguage,
            comment: "A language of Profile › Language, in the app's language",
        )
        default: String(localized: "System", bundle: .appLanguage)
        }
    }

    /// In its own language, so VoiceOver reads "日本語" as Japanese.
    private static func spoken(_ name: String, in tag: String) -> AttributedString {
        var text = AttributedString(name)
        text.languageIdentifier = tag
        return text
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
                // Only a profile nothing brings back is erased for good; any account signs out (beta feedback 17)
                Text(state.erasesProfileOnSignOut ? "Erase profile and sign out" : "Sign out")
                    .destructiveRowLabel()
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
            .photoCrop(editModel.state.photoCrop, frame: .circle) { editModel.send(EditProfileUiEventOnPhotoCrop(event: $0)) }
        #else
        let state = model.state
        Section {
            HStack(spacing: 14) {
                // The avatar opens the photo big, as Bible Planner's; the rest of the row edits the profile
                Button {
                    model.send(ProfileUiEventOnAvatarClicked.shared)
                } label: {
                    AvatarPhoto(
                        initial: state.avatarInitial,
                        color: state.avatarColor,
                        photoUrl: state.photoUrl,
                        size: 60,
                    )
                    .contentShape(.circle)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("View photo")
                Button {
                    model.send(ProfileUiEventOnEditClicked.shared)
                } label: {
                    HStack(spacing: 14) {
                        VStack(alignment: .leading, spacing: 3) {
                            Text(verbatim: state.displayName)
                                .font(.title3.bold())
                                .foregroundStyle(.primary)
                            if state.showsProfilePending {
                                Text("Changed · waiting to upload")
                                    .font(.subheadline)
                                    .foregroundStyle(Palette.pending)
                            } else {
                                Text("Name, photo and avatar color")
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        Spacer()
                        Image(systemName: "chevron.forward")
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(.tertiary)
                    }
                    .contentShape(.rect)
                }
                .buttonStyle(.plain)
                .accessibilityHint("Edit profile")
            }
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
                HStack(spacing: 12) {
                    Image(systemName: "person.slash")
                        .foregroundStyle(.secondary)
                        .frame(width: 24)
                        .accessibilityHidden(true)
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
                            Text("Deleted after 30 days without use")
                        } else {
                            Text("Saved only on this \(DeviceName.current) for now")
                        }
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
                        Image(systemName: "chevron.forward").foregroundStyle(.tertiary)
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
            if state.showsUserId {
                UserIdRow(model: model, shortUserId: state.shortUserId)
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

/// "User ID", a quiet last row: the id shortened, the whole one copied by a tap anywhere on the row, the
/// copy icon only marking the action, for a survey to tell who answered (Pierre's call, testers' feedback).
private struct UserIdRow: View {
    let model: ProfileModel
    let shortUserId: String

    var body: some View {
        Button {
            model.send(ProfileUiEventOnCopyUserIdClicked.shared)
        } label: {
            LabeledContent {
                Image(systemName: "doc.on.doc")
                    .foregroundStyle(.tint)
                    .accessibilityHidden(true)
            } label: {
                Text("User ID").foregroundStyle(.primary)
                Text(shortUserId)
            }
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text("User ID"))
        .accessibilityValue(Text(shortUserId))
        .accessibilityHint(Text("Copy user ID"))
    }
}

/// The Google or Apple row only says which account it is: leaving it is signing out or deleting the
/// account (Pierre's call, beta feedback), so it opens nothing. Its provider's logo and the whole email
/// say which account; the ⓘ next to it says only the person sees the email (testers' feedback).
private struct ConnectedAccountRow: View {
    let model: ProfileModel
    let provider: AccountProvider

    var body: some View {
        let email = model.state.email
        HStack(spacing: 12) {
            ProviderLogo(provider: provider, size: 20)
                .frame(width: 24)
                // Why: Google's logo is a decorative image, hidden from VoiceOver, so the label needs its own element.
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(Text(verbatim: provider.displayName))
                // Without an email the row's text already names the provider, which the logo would repeat
                .accessibilityHidden(email == nil)
            row(email: email)
        }
    }

    private func row(email: String?) -> some View {
        LabeledContent {
            if email != nil {
                Button {
                    model.send(ProfileUiEventOnEmailInfoClicked.shared)
                } label: {
                    Image(systemName: "info.circle")
                        .imageScale(.large)
                }
                .buttonStyle(.borderless)
                .accessibilityLabel(Text("About your email"))
                .popover(isPresented: model.isPresented(.emailInfo), arrowEdge: .top) {
                    EmailInfoPopover()
                }
            }
        } label: {
            Text(verbatim: email ?? provider.displayName)
                .foregroundStyle(.primary)
                .textSelection(.enabled)
        }
    }
}

private struct EmailInfoPopover: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Only you see your email")
                .font(.headline)
            Text("It's here so you know which account you're signed in with. The people in your groups see your name and photo, never your email.")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .padding(14)
        .frame(width: 280, alignment: .leading)
        .fixedSize(horizontal: false, vertical: true)
        .presentationCompactAdaptation(.popover)
    }
}

/// "Keys to receive payments": each key masked, its default marked, and a menu that asks Pix or Bitcoin
/// first, or one row for each where the Pix rule is off. The Mac adds a key with the + under the list, as its
/// settings lists do.
private struct PaymentKeysSection: View {
    let model: ProfileModel

    var body: some View {
        Section {
            if let keys = model.state.paymentKeys.loaded(as: [PaymentKeyItem].self) {
                ForEach(keys, id: \.id) { key in
                    Button {
                        model.send(ProfileUiEventOnPaymentKeyClicked(keyId: key.id))
                    } label: {
                        PaymentKeyRow(key: key)
                    }
                    .buttonStyle(.plain)
                    #if os(macOS)
                    // A right-click opens the key as its click does, where its default and Delete live
                    .contextMenu {
                        Button("Edit…", systemImage: "pencil") {
                            model.send(ProfileUiEventOnPaymentKeyClicked(keyId: key.id))
                        }
                    }
                    #endif
                }
            } else {
                // A restored account's keys come with its first full sync: never "no key" before them
                ShimmerList(count: 2, leading: .badge, showsTrailing: false)
                    .shimmering()
            }
            if model.state.canAddPaymentKey {
                if model.state.showsPix {
                    addKey
                } else {
                    // The Pix rule: no Pix signal, so Pix and Bitcoin each get a row and Pix stays one tap away
                    addKeyRows
                }
            }
        } header: {
            Text("Keys to receive payments")
        } footer: {
            Text("Whoever owes you sees your default key when settling up.")
        }
    }

    @ViewBuilder
    private var addKeyRows: some View {
        Button {
            model.send(ProfileUiEventOnAddPaymentKeyClicked(method: .bitcoin))
        } label: {
            Label("Add a Bitcoin key", systemImage: "plus.circle.fill")
        }
        Button {
            model.send(ProfileUiEventOnAddPaymentKeyClicked(method: .pix))
        } label: {
            Label("Add a Pix key", systemImage: "plus.circle.fill")
        }
    }

    @ViewBuilder
    private var addKey: some View {
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
    }
}

/// "Payment methods": how the person pays, the default first and marked, each opening its form, then "Add a payment
/// method". Private: only the person sees them, as the note under them says. The Mac adds one with the + under the
/// list, as its settings lists do.
private struct PayingMethodsSection: View {
    let model: ProfileModel

    var body: some View {
        Section {
            if let methods = model.state.payingMethods.loaded(as: [PayingMethod].self) {
                ForEach(methods, id: \.id) { method in
                    Button {
                        model.send(ProfileUiEventPayingMethodsOnClicked(methodId: method.id))
                    } label: {
                        PayingMethodRow(method: method)
                    }
                    .buttonStyle(.plain)
                    #if os(macOS)
                    // A right-click opens the method as its click does, where its default and Delete live
                    .contextMenu {
                        Button("Edit…", systemImage: "pencil") {
                            model.send(ProfileUiEventPayingMethodsOnClicked(methodId: method.id))
                        }
                    }
                    #endif
                }
            } else {
                // A restored account's methods come with its first full sync: never "none" before them
                ShimmerList(count: 2, leading: .badge, showsTrailing: false)
                    .shimmering()
            }
            addMethod
        } header: {
            Text("Payment methods")
        } footer: {
            Text("Only you see these. Mark which one you paid a bill with.")
        }
    }

    @ViewBuilder
    private var addMethod: some View {
        #if os(macOS)
        HStack {
            Button {
                model.send(ProfileUiEventPayingMethodsOnAddClicked.shared)
            } label: {
                Image(systemName: "plus")
                    .frame(width: 20, height: 20)
                    .contentShape(.rect)
            }
            .buttonStyle(.borderless)
            .accessibilityLabel("Add a payment method")
            Spacer()
        }
        #else
        Button {
            model.send(ProfileUiEventPayingMethodsOnAddClicked.shared)
        } label: {
            Label("Add a payment method", systemImage: "plus.circle.fill")
        }
        #endif
    }
}

/// A payment method as the profile lists it: its tile, its label over its type when named, "Default".
private struct PayingMethodRow: View {
    let method: PayingMethod

    var body: some View {
        HStack(spacing: 12) {
            PayingMethodLabel(method: method)
            Spacer(minLength: 8)
            if method.isDefault {
                DefaultBadge(title: Text(LocalizedStringResource(
                    "Default (a payment method's badge)",
                    defaultValue: "Default",
                )))
            }
            Image(systemName: "chevron.forward")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.tertiary)
                .accessibilityHidden(true)
        }
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
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
            PaymentKeyIcon(type: key.type)
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
                DefaultBadge(title: Text("Default"))
            }
            Image(systemName: "chevron.forward")
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

/// "Support and feedback", between the preferences and "Privacy and about": how to reach whoever makes the app.
private struct SupportSection: View {
    let model: ProfileModel

    var body: some View {
        Section("Support and feedback") {
            row("Contact support") {
                model.send(ProfileUiEventOnContactSupportClicked.shared)
            }
            // Only once the App Store lists the app: it opens its page at "Write a Review"
            if model.state.showsRateApp {
                row("Rate the app") {
                    model.send(ProfileUiEventOnRateAppClicked.shared)
                }
            }
        }
    }

    private func row(_ title: LocalizedStringKey, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            LabeledContent {
                Image(systemName: "chevron.forward")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.tertiary)
                    .accessibilityHidden(true)
            } label: {
                Text(title)
            }
            .contentShape(.rect)
        }
        .foregroundStyle(.primary)
    }
}

private struct AboutSection: View {
    let model: ProfileModel

    var body: some View {
        Section("Privacy and about") {
            ExportRow(model: model)
            // Analytics' opt out on this device, on by default under legitimate interest (docs/analytics/README.md)
            Toggle(
                isOn: Binding(
                    get: { model.state.sharesUsageData },
                    set: { model.send(ProfileUiEventOnUsageDataToggled(isOn: $0)) },
                ),
            ) {
                Text("Share usage data")
                Text("Screens and taps, to improve the app. Never names or amounts.")
            }
            .disabled(!model.state.isLoaded)
            ReleaseNotesRow(model: model)
            // The version itself sits in the legal footer, at the end of the profile (beta feedback 18)
            if model.state.showsCheckForUpdates {
                CheckForUpdatesRow(model: model)
            }
        }
    }
}

/// "Privacy Policy • Terms of Service" and "Version X" under them, discreet and centered at the very end of the
/// profile, as Bible Planner's (beta feedback 18).
private struct LegalFooter: View {
    let model: ProfileModel

    var body: some View {
        Section {
            VStack(spacing: 6) {
                ViewThatFits(in: .horizontal) {
                    HStack(spacing: 10) { links }
                    VStack(spacing: 6) { links }
                }
                Text("Version \(Bundle.appVersion)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity)
        }
        .listRowInsets(EdgeInsets())
        .listRowBackground(Color.clear)
    }

    @ViewBuilder
    private var links: some View {
        Button("Privacy Policy") {
            model.send(ProfileUiEventOnPrivacyPolicyClicked.shared)
        }
        .buttonStyle(.borderless)
        Text(verbatim: "•")
            .foregroundStyle(.secondary)
        Button("Terms of Service") {
            model.send(ProfileUiEventOnTermsClicked.shared)
        }
        .buttonStyle(.borderless)
    }
}

/// "Release notes": what each version changed, pushed on iPhone and iPad and a sheet on the Mac's Settings. The
/// whole row opens it, a chevron at its end, as a key's row does on both.
private struct ReleaseNotesRow: View {
    let model: ProfileModel

    var body: some View {
        Button {
            model.send(ProfileUiEventOnReleaseNotesClicked.shared)
        } label: {
            LabeledContent {
                Image(systemName: "chevron.forward")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.tertiary)
                    .accessibilityHidden(true)
            } label: {
                Text("Release notes").foregroundStyle(.primary)
            }
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
    }
}

/// "Check for updates" in the place of "Version", with the version under it, as Bible Planner's; the Mac keeps the
/// version as the row's label and the check as its button, as its Settings rows do.
private struct CheckForUpdatesRow: View {
    let model: ProfileModel

    private var isChecking: Bool {
        model.state.isCheckingForUpdates
    }

    var body: some View {
        #if os(macOS)
        LabeledContent {
            if isChecking {
                ProgressView().controlSize(.small)
            } else {
                Button("Check for updates") { check() }
            }
        } label: {
            Text("Version \(Bundle.appVersion)")
        }
        #else
        Button {
            check()
        } label: {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Check for updates")
                    Group {
                        if isChecking {
                            Text("Checking…")
                        } else {
                            Text("Version \(Bundle.appVersion)")
                        }
                    }
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                }
                Spacer()
                if isChecking {
                    ProgressView()
                }
            }
            .contentShape(.rect)
        }
        .disabled(isChecking)
        #endif
    }

    private func check() {
        model.send(ProfileUiEventOnCheckForUpdatesClicked.shared)
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
                // The brand's negative, not the system's red, which reads at 3:1 on this tint
                Text("Delete account and data")
                    .fontWeight(.semibold)
                    .destructiveRowLabel()
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
    /// The whole version, a beta's suffix too ("3.0.0-beta_1", beta feedback 19): Apple refuses a suffix in
    /// CFBundleShortVersionString, so the build writes it apart (iosApp/generate-project.sh).
    /// Read once: it can't change while the app runs.
    static let appVersion: String = {
        let info = Bundle.main.infoDictionary
        // Empty when xcodegen ran without generate-project.sh's environment
        let whole = info?["BlitzSplitVersionName"] as? String ?? ""
        return whole.isEmpty ? info?["CFBundleShortVersionString"] as? String ?? "" : whole
    }()
}

/// Preferences › "Category map" (flow 9): pushed on iPhone and iPad; on the Mac, whose profile is Ajustes, it picks
/// Ajustes' Categories section instead (`AppModel`).
private struct CategoryMapRow: View {
    let model: ProfileModel

    var body: some View {
        ChevronRow(title: "Category map", detail: "The words that pick a bill's category") {
            model.send(ProfileUiEventOnCategoryMapClicked.shared)
        }
    }
}

/// A row that opens another screen: its title over a detail, and the chevron.
private struct ChevronRow: View {
    let title: LocalizedStringKey
    let detail: LocalizedStringKey
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            LabeledContent {
                Image(systemName: "chevron.forward")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.tertiary)
            } label: {
                Text(title)
                // The button's primary style would reach it too
                Text(detail)
                    .foregroundStyle(.secondary)
            }
        }
        .foregroundStyle(.primary)
    }
}

/// Preferences › "Connect watch" (15.14): a sheet on iPhone and iPad, and on Settings' window on the Mac.
private struct ConnectWatchRow: View {
    let model: ProfileModel

    var body: some View {
        ChevronRow(title: "Connect watch", detail: "With the code your watch shows") {
            model.send(ProfileUiEventOnConnectWatchClicked.shared)
        }
    }
}

/// Pix's own mark, the bolt and the link of the two Bitcoin networks.
private struct PaymentKeyIcon: View {
    let type: PaymentKeyType

    var body: some View {
        switch type {
        case .lightning: Image(systemName: "bolt.fill")
        case .onchain: Image(systemName: "link")
        default: PixMark()
        }
    }
}
