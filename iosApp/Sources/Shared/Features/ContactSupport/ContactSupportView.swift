import Observation
import Shared
import SwiftUI
#if os(iOS)
import UIKit
#else
import AppKit
#endif

@Observable
final class ContactSupportModel {
    private(set) var state: ContactSupportUiState

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createContactSupportViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! ContactSupportUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            if let copy = action as? ContactSupportUiActionCopyToClipboard {
                Pasteboard.copy(copy.email)
            } else if let open = action as? ContactSupportUiActionOpenEmail {
                self?.openEmail(open.mailtoLink)
            }
        })
    }

    func send(_ event: any ContactSupportUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    /// Hands the mailto link to the system. A device with no email app set up says so, and the shared code then
    /// copies the address instead.
    private func openEmail(_ mailtoLink: String) {
        guard let url = URL(string: mailtoLink) else {
            send(ContactSupportUiEventOnOpenEmailFailed.shared)
            return
        }
        #if os(iOS)
        UIApplication.shared.open(url) { [weak self] isOpened in
            if !isOpened { self?.send(ContactSupportUiEventOnOpenEmailFailed.shared) }
        }
        #else
        if !NSWorkspace.shared.open(url) {
            send(ContactSupportUiEventOnOpenEmailFailed.shared)
        }
        #endif
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Contact support", from Profile's "Support and feedback": a sheet on iPhone, iPad and Mac with the address, what
/// the email will carry and "Open email", which writes the message's subject and diagnostics for the person.
struct ContactSupportView: View {
    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: ContactSupportModel?

    var body: some View {
        Group {
            if let model {
                ContactSupportContent(model: model)
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        .navigationTitle("Contact support")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Close", systemImage: "xmark", role: .cancel) {
                    model?.send(ContactSupportUiEventOnDismissed.shared)
                }
            }
        }
        #endif
        .onAppear {
            if model == nil { model = ContactSupportModel() }
        }
    }
}

private struct ContactSupportContent: View {
    let model: ContactSupportModel

    private var state: ContactSupportUiState {
        model.state
    }

    private var diagnostics: SupportDiagnostics? {
        state.diagnostics.loaded(as: SupportDiagnostics.self)
    }

    /// What the last tap did, in the app's language; nil before any.
    private var notice: String? {
        guard let notice = state.notice else { return nil }
        return notice == .noEmailApp
            ? String(localized: "No email app found. The address was copied instead.", bundle: .appLanguage)
            : String(localized: "Email address copied.", bundle: .appLanguage)
    }

    var body: some View {
        #if os(macOS)
        macBody
        #else
        iOSBody
        #endif
    }

    #if os(iOS)
    private var iOSBody: some View {
        Form {
            Section {
                VStack(spacing: 10) {
                    EmailTile(size: 52, shape: .circle)
                    Text(description)
                        .font(.callout)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
            }
            .listRowInsets(EdgeInsets())
            .listRowBackground(Color.clear)
            Section {
                HStack(spacing: 12) {
                    addressLabel
                    Spacer(minLength: 8)
                    copyIconButton
                }
            }
            Section("Will be included in the email") {
                diagnosticsRows
            }
        }
        .formStyle(.grouped)
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: 10) {
                if let notice {
                    Text(verbatim: notice)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
                PrimaryButton(title: "Open email", systemImage: "envelope", isEnabled: state.canOpenEmail) {
                    model.send(ContactSupportUiEventOnOpenEmailClicked.shared)
                }
                Button("Copy email address") {
                    model.send(ContactSupportUiEventOnCopyEmailClicked.shared)
                }
                .buttonStyle(.borderless)
                .frame(minHeight: 36)
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 8)
            .frame(maxWidth: ScreenLayout.singleColumnMaxWidth)
            .frame(maxWidth: .infinity)
            .background(Palette.groupedBackground)
        }
    }
    #endif

    #if os(macOS)
    /// A compact sheet on Settings' window: the tile and the title side by side, the address and what the email
    /// carries in a box each, and the buttons in a row with the copy on the left.
    private var macBody: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .center, spacing: 14) {
                EmailTile(size: 44, shape: .rect(cornerRadius: 12))
                VStack(alignment: .leading, spacing: 2) {
                    Text("Contact support")
                        .font(.headline)
                        .accessibilityAddTraits(.isHeader)
                    Text(description)
                        .font(.callout)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            // Plain boxes, never a Form: a Form scrolls, and a scroll view has no height of its own for the sheet to fit
            HStack(spacing: 12) {
                addressLabel
                Spacer(minLength: 8)
                copyIconButton
            }
            .padding(10)
            .background(.quaternary.opacity(0.5), in: .rect(cornerRadius: 10))
            VStack(alignment: .leading, spacing: 8) {
                Text("Will be included in the email")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .accessibilityAddTraits(.isHeader)
                diagnosticsRows
            }
            .padding(10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(.quaternary.opacity(0.5), in: .rect(cornerRadius: 10))
            if let notice {
                Text(verbatim: notice)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            HStack(spacing: 8) {
                Button("Copy email address") {
                    model.send(ContactSupportUiEventOnCopyEmailClicked.shared)
                }
                Spacer(minLength: 8)
                Button("Close", role: .cancel) {
                    model.send(ContactSupportUiEventOnDismissed.shared)
                }
                .keyboardShortcut(.cancelAction)
                Button("Open email") {
                    model.send(ContactSupportUiEventOnOpenEmailClicked.shared)
                }
                .buttonStyle(.borderedProminent)
                .prominentLabel()
                .keyboardShortcut(.defaultAction)
                .disabled(!state.canOpenEmail)
            }
        }
        .padding(20)
        .frame(width: 460)
        .fixedSize(horizontal: false, vertical: true)
    }
    #endif

    private var description: LocalizedStringKey {
        "We'll open your email app with a message ready to go. Describe what happened and send it: real people read every message."
    }

    /// "To" over the address, which can be selected to write the email somewhere else.
    private var addressLabel: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("To (an email's recipient)")
                .font(.footnote)
                .foregroundStyle(.secondary)
            Text(verbatim: state.email)
                .textSelection(.enabled)
        }
        .accessibilityElement(children: .combine)
    }

    private var copyIconButton: some View {
        Button("Copy email address", systemImage: "doc.on.doc") {
            model.send(ContactSupportUiEventOnCopyEmailClicked.shared)
        }
        .labelStyle(.iconOnly)
        .buttonStyle(.borderless)
    }

    /// The rows shimmer until the account, the language and the profile's link were read.
    @ViewBuilder
    private var diagnosticsRows: some View {
        if let diagnostics {
            LabeledContent("App version") { Text(verbatim: diagnostics.appVersion) }
            LabeledContent("Platform") { Text(verbatim: platformName(diagnostics.platform)) }
            LabeledContent("Language") {
                if let tag = diagnostics.language.tag {
                    Text(verbatim: tag)
                } else {
                    // A pick of System alone would not say which language the app speaks
                    Text("System") + Text(verbatim: " (\(diagnostics.spokenLanguage))")
                }
            }
            LabeledContent("Account") { accountText(diagnostics.account) }
            if let link = diagnostics.profileLink {
                LabeledContent("Profile") {
                    Text(verbatim: link)
                        .lineLimit(1)
                        .truncationMode(.middle)
                }
            }
        } else {
            ForEach(0 ..< 4, id: \.self) { _ in
                ShimmerLine(width: 220, height: 12)
                    .shimmering()
            }
        }
    }

    /// The names of the platforms are the same in every language.
    private func platformName(_ platform: SupportPlatform) -> String {
        switch platform {
        case .android: "Android"
        case .ios: "iOS"
        case .macos: "macOS"
        default: "Web"
        }
    }

    @ViewBuilder
    private func accountText(_ account: SupportAccount) -> some View {
        switch account {
        case .localOnly: Text("This device only")
        case .anonymous: Text("Not connected")
        case .google: Text(verbatim: "Google")
        default: Text(verbatim: "Apple")
        }
    }
}

/// The envelope on the accent tint that heads the sheet.
private struct EmailTile<TileShape: Shape>: View {
    let size: CGFloat
    let shape: TileShape

    var body: some View {
        Image(systemName: "envelope")
            .font(.system(size: size * 0.46, weight: .semibold))
            .foregroundStyle(Palette.accent)
            .frame(width: size, height: size)
            .background(Palette.accent.opacity(0.14), in: shape)
            .accessibilityHidden(true)
    }
}
