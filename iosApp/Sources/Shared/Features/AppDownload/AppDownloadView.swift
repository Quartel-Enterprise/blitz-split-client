import Observation
import Shared
import SwiftUI

@Observable
final class AppDownloadModel {
    private(set) var state: AppDownloadUiState
    /// "Link copied", each time the link goes to the clipboard.
    private(set) var copiedCount = 0

    @ObservationIgnored private let holder: ViewModelHolder<AppDownloadViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteAppDownload) {
        holder = BlitzSplitApple.shared.createAppDownloadViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! AppDownloadUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            guard let copy = action as? AppDownloadUiActionCopyLink else { return }
            #if os(macOS)
            NSPasteboard.general.clearContents()
            NSPasteboard.general.setString(copy.link, forType: .string)
            #else
            UIPasteboard.general.string = copy.link
            #endif
            self?.copiedCount += 1
        })
    }

    func send(_ event: any AppDownloadUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Blitz Split on iPhone" (14.2), the Mac's sheet from Home's card (on the main window) and Settings' iPhone row (on
/// Settings' window): the QR code big, which opens the App Store, what only the iPhone gives, and "I downloaded it"
/// while the invite stands. Once the account uses the app on a phone it says since when (14.2b). The Mac app comes
/// from the Mac App Store, so it only ever names the iPhone and the App Store.
struct AppDownloadView: View {
    let route: RouteAppDownload

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: AppDownloadModel?

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                AppDownloadContent(model: model)
            } else {
                Color.clear.frame(width: 520, height: 320)
            }
        }
        .onAppear {
            if model == nil { model = AppDownloadModel(route: route) }
        }
    }
}

private struct AppDownloadContent: View {
    let model: AppDownloadModel

    private var state: AppDownloadUiState { model.state }

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Blitz Split on iPhone")
                    .font(.title2.weight(.semibold))
                Text("Point your iPhone's camera at the code. It opens the App Store.")
                    .foregroundStyle(.secondary)
            }
            HStack(alignment: .top, spacing: 20) {
                if let qrCode = state.qrCode {
                    QrCodeImage(qrCode: qrCode, accessibilityLabel: "QR code that opens Blitz Split on the App Store")
                        .frame(width: 176, height: 176)
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(.separator))
                }
                VStack(alignment: .leading, spacing: 14) {
                    AppDownloadPerk(
                        systemImage: "mappin.and.ellipse",
                        title: "Right away, wherever you are",
                        message: "Add the bill while you're still at the table, even offline. It syncs once you're back online.",
                    )
                    AppDownloadPerk(
                        systemImage: "bell",
                        title: "Payment notices",
                        message: "Know when someone records a payment or asks you to pay.",
                    )
                }
            }
            if let since = state.phoneUseSinceMillis {
                Label {
                    Text("You've used the app on your phone since \(Self.day(since)). This code always stays in Settings, for when you change iPhones.")
                } icon: {
                    Image(systemName: "info.circle")
                }
                .font(.callout)
                .padding(10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(.quaternary.opacity(0.5), in: .rect(cornerRadius: 10))
            }
            accountNote
                .font(.callout)
                .foregroundStyle(.secondary)
            HStack {
                Button("Copy link", systemImage: "doc.on.doc") {
                    model.send(AppDownloadUiEventOnCopyLinkClicked.shared)
                }
                Spacer()
                if state.showsDownloadedButton {
                    Button("I downloaded it") {
                        model.send(AppDownloadUiEventOnDownloadedClicked.shared)
                    }
                }
                Button("OK") {
                    model.send(AppDownloadUiEventOnCloseClicked.shared)
                }
                .keyboardShortcut(.defaultAction)
            }
        }
        .padding(24)
        .frame(width: 560)
        #if os(macOS)
        .onExitCommand { model.send(AppDownloadUiEventOnCloseClicked.shared) }
        #endif
        .glassToast("Link copied", trigger: model.copiedCount)
    }

    @ViewBuilder
    private var accountNote: some View {
        switch state.accountProvider {
        case .google: Text("Sign in to the app with the same Google account and your groups will be there.")
        case .apple: Text("Sign in to the app with the same Apple account and your groups will be there.")
        default: Text("Without an account, the app on iPhone starts empty. Connect an Apple or Google account in Settings to take your groups.")
        }
    }

    /// "Oct 12", the day the account first signed in on a phone app.
    static func day(_ millis: KotlinLong) -> String {
        Date(timeIntervalSince1970: Double(millis.int64Value) / 1000).formatted(.dateTime.day().month(.abbreviated))
    }
}

private struct AppDownloadPerk: View {
    let systemImage: String
    let title: LocalizedStringKey
    let message: LocalizedStringKey

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: systemImage)
                .foregroundStyle(Palette.accent)
                .frame(width: 32, height: 32)
                .background(Palette.accent.opacity(0.12), in: .rect(cornerRadius: 9))
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).fontWeight(.semibold)
                Text(message)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}
