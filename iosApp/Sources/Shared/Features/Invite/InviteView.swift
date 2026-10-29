import Observation
import Shared
import SwiftUI

@Observable
final class InviteModel {
    private(set) var state: InviteUiState

    @ObservationIgnored private let holder: ViewModelHolder<InviteViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteInvite) {
        holder = BlitzSplitApple.shared.createInviteViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! InviteUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { action in
            if let copy = action as? InviteUiActionCopyLink { Pasteboard.copy(copy.link) }
        })
    }

    func send(_ event: any InviteUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Invite", right after a group is created: a sheet on iPhone, iPad and Mac, taking the new group's place.
/// The QR code and the link come once the group's first sync brings its code; until then a dashed box says why.
struct InviteView: View {
    let route: RouteInvite

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: InviteModel?

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                InviteContent(model: model)
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Close", systemImage: "xmark", role: .cancel) {
                    model?.send(InviteUiEventOnCloseClicked.shared)
                }
            }
        }
        #if os(macOS)
        .frame(minWidth: 420, idealWidth: 460, minHeight: 600, idealHeight: 680)
        #endif
        .onAppear {
            if model == nil { model = InviteModel(route: route) }
        }
    }
}

private struct InviteContent: View {
    let model: InviteModel

    private var state: InviteUiState {
        model.state
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                VStack(alignment: .leading, spacing: 12) {
                    Label("Group created", systemImage: "checkmark")
                        .font(.footnote.weight(.semibold))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .foregroundStyle(Palette.accent)
                        .background(Palette.accent.opacity(0.14), in: .capsule)
                    Text("Invite everyone to \(state.groupName)")
                        .font(.title2.bold())
                }
                if let link = state.link, let displayLink = state.displayLink, let qrCode = state.qrCode {
                    LinkCard(model: model, displayLink: displayLink, qrCode: qrCode)
                    WhoPicksText(names: state.notJoinedNames)
                    ShareLink(item: link, message: Text("Let's split the bills of \(state.groupName) on Blitz Split. Join with this link:")) {
                        Label("Share invite", systemImage: "square.and.arrow.up")
                            .font(.headline)
                            .frame(maxWidth: .infinity, minHeight: 36)
                    }
                    .buttonStyle(.glassProminent)
                    .buttonBorderShape(.capsule)
                    .controlSize(.large)
                    TintedButton(title: "See who joined") {
                        model.send(InviteUiEventOnSeeWhoJoinedClicked.shared)
                    }
                } else {
                    PendingLinkBox(state: state) {
                        model.send(InviteUiEventOnRetryClicked.shared)
                    }
                    WhoPicksText(names: state.notJoinedNames)
                    Button {} label: {
                        Label("Share invite", systemImage: "square.and.arrow.up")
                            .font(.headline)
                            .frame(maxWidth: .infinity, minHeight: 36)
                    }
                    .buttonStyle(.glassProminent)
                    .buttonBorderShape(.capsule)
                    .controlSize(.large)
                    .disabled(true)
                    TintedButton(title: "Go to the group") {
                        model.send(InviteUiEventOnGoToGroupClicked.shared)
                    }
                }
            }
            .padding(20)
            .frame(maxWidth: ScreenLayout.singleColumnMaxWidth)
            .frame(maxWidth: .infinity)
        }
        .background(Palette.groupedBackground)
    }
}

private struct LinkCard: View {
    let model: InviteModel
    let displayLink: String
    let qrCode: QrCode

    var body: some View {
        VStack(spacing: 16) {
            QrCodeImage(qrCode: qrCode)
                .frame(maxWidth: 240)
            HStack(spacing: 12) {
                Text(verbatim: displayLink)
                    .font(.body.monospaced())
                    .lineLimit(1)
                    .truncationMode(.middle)
                    .textSelection(.enabled)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Button {
                    model.send(InviteUiEventOnCopyClicked.shared)
                } label: {
                    if model.state.isLinkCopied {
                        Label("Copied", systemImage: "checkmark")
                    } else {
                        Label("Copy", systemImage: "doc.on.doc")
                    }
                }
                .buttonStyle(.bordered)
                .buttonBorderShape(.capsule)
                .accessibilityAddTraits(.updatesFrequently)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .glassEffect(.regular, in: .rect(cornerRadius: 28))
    }
}

/// Black on white in both appearances, as scanners read it best, with the quiet zone around the modules.
private struct QrCodeImage: View {
    let qrCode: QrCode

    /// The blank border a scanner needs around the code, in modules.
    private let quietZone = 2

    var body: some View {
        Canvas { context, size in
            let count = Int(qrCode.size)
            let module = min(size.width, size.height) / CGFloat(count + quietZone * 2)
            var path = Path()
            for row in 0..<count {
                for column in 0..<count where qrCode.isDark(row: Int32(row), column: Int32(column)) {
                    path.addRect(CGRect(
                        x: CGFloat(column + quietZone) * module,
                        y: CGFloat(row + quietZone) * module,
                        width: module,
                        height: module
                    ))
                }
            }
            context.fill(path, with: .color(.black))
        }
        .aspectRatio(1, contentMode: .fit)
        .background(.white, in: .rect(cornerRadius: 16))
        .accessibilityElement()
        .accessibilityLabel("QR code of the group's invite link")
        .accessibilityAddTraits(.isImage)
    }
}

/// On its way while online, waiting for the internet while offline, or failed with a retry after a failed sync.
private struct PendingLinkBox: View {
    let state: InviteUiState
    let onRetry: () -> Void

    private var isCreating: Bool {
        state.isOnline && !state.isLinkFailed
    }

    var body: some View {
        VStack(spacing: 6) {
            if isCreating {
                ProgressView()
                    .padding(.bottom, 6)
            }
            Group {
                if state.isLinkFailed {
                    Text("Couldn't create the link")
                } else {
                    Text("Link pending")
                }
            }
            .font(.headline)
            Group {
                if state.isLinkFailed {
                    Text("Something went wrong while syncing the group.")
                } else if state.isOnline {
                    Text("Creating the link…")
                } else {
                    Text("It's created as soon as the \(DeviceName.current) is online.")
                }
            }
            .font(.subheadline)
            .foregroundStyle(.secondary)
            .multilineTextAlignment(.center)
            if state.isLinkFailed {
                Button("Try again", action: onRetry)
                    .buttonStyle(.bordered)
                    .buttonBorderShape(.capsule)
                    .padding(.top, 8)
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity, minHeight: 160)
        .overlay {
            RoundedRectangle(cornerRadius: 28)
                .strokeBorder(.secondary, style: StrokeStyle(lineWidth: 1.5, dash: [8, 6]))
        }
        .accessibilityElement(children: .contain)
    }
}

/// "Quem abrir o link escolhe quem é na lista: Ana, Bruno ou Carla", with the names in bold.
private struct WhoPicksText: View {
    let names: [String]

    var body: some View {
        Group {
            if names.isEmpty {
                Text("Whoever opens the link joins the group. No account needed.")
            } else {
                Text("Whoever opens the link picks who they are on the list: \(Text(names.formatted(.list(type: .or))).bold()). No account needed.")
            }
        }
        .font(.callout)
        .foregroundStyle(.secondary)
    }
}
