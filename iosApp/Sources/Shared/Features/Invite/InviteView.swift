import Observation
import Shared
import SwiftUI

@Observable
final class InviteModel {
    private(set) var state: InviteUiState
    private(set) var changeLinkErrorCount = 0

    @ObservationIgnored private let holder: ViewModelHolder<InviteViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteInvite) {
        holder = BlitzSplitApple.shared.createInviteViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! InviteUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            switch action {
            case let copy as InviteUiActionCopyLink: Pasteboard.copy(copy.link)
            case is InviteUiActionShowChangeLinkFailed: self?.changeLinkErrorCount += 1
            default: break
            }
        })
    }

    func send(_ event: any InviteUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Invite", right after a group is created, taking the new group's place, or from the group (2.2). A sheet on an
/// iPhone, two columns inside it lying down; pushed full screen with an iPad's room (decision 1: an iPad, an iPhone
/// Duo open or upright), two columns lying down and the QR card first upright; on the Mac a sheet right after
/// creating, and a popover on the group's "Invite people" once the group is behind it (F2Mac, `InvitePopover`).
/// The QR code and the link come once the group's first sync brings its code; until then a dashed box says why.
struct InviteView: View {
    let route: RouteInvite
    /// The Mac's popover over the group's "Invite people", which has no toolbar of its own.
    var isPopover = false

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: InviteModel?
    @Environment(\.windowLayout) private var window

    /// Pushed full screen with an iPad's room, where the system's back goes back to the group; the new group's
    /// invite keeps its close button, as the board draws it.
    private var isPushed: Bool {
        #if os(iOS)
        window.isLarge
        #else
        false
        #endif
    }

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                #if os(macOS)
                if isPopover {
                    InvitePopoverContent(model: model)
                } else {
                    InviteContent(model: model)
                }
                #else
                InviteContent(model: model)
                #endif
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(isPushed && route.isNewGroup)
        #endif
        .toolbar {
            if !isPopover, !isPushed || route.isNewGroup {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", systemImage: "xmark", role: .cancel) {
                        model?.send(InviteUiEventOnCloseClicked.shared)
                    }
                }
            }
            if let model, model.state.showsChangeLink, !isPopover {
                ToolbarItem(placement: .primaryAction) { changeLinkButton(model: model) }
            }
        }
        .alert("Change the invite link?", isPresented: changeLinkShown) {
            Button("Change link", role: .destructive) {
                model?.send(InviteUiEventOnChangeLinkConfirmed.shared)
            }
            Button("Cancel", role: .cancel) {
                model?.send(InviteUiEventOnChangeLinkDismissed.shared)
            }
        } message: {
            Text("The current link and QR code stop working, unused personal invites included. Whoever already joined stays in the group.")
        }
        // The count only grows on an action: `?? 0` keeps the model's arrival (nil to 0) from showing a toast.
        .glassToast("Couldn't change the link. Try again.", trigger: model?.changeLinkErrorCount ?? 0)
        #if os(macOS)
        .frame(
            minWidth: isPopover ? 340 : 420,
            idealWidth: isPopover ? 340 : 460,
            minHeight: isPopover ? nil : 600,
            idealHeight: isPopover ? nil : 680,
        )
        #endif
        .onAppear {
            if model == nil { model = InviteModel(route: route) }
        }
    }

    /// "Trocar link", the group's creator only: at the top on iPhone and iPad, in the "⋯" menu of the Mac's sheet,
    /// whose items that open a dialog end in "…" (its popover has the link under the share button).
    @ViewBuilder
    private func changeLinkButton(model: InviteModel) -> some View {
        #if os(macOS)
        Menu("More invite options", systemImage: "ellipsis") {
            Button("Change link…", systemImage: "arrow.triangle.2.circlepath") {
                model.send(InviteUiEventOnChangeLinkClicked.shared)
            }
        }
        .disabled(model.state.isChangingLink)
        #else
        Button("Change link") {
            model.send(InviteUiEventOnChangeLinkClicked.shared)
        }
        .disabled(model.state.isChangingLink)
        #endif
    }

    /// Dismissing the alert (Esc, a tap outside on iPad) closes the confirmation in the ViewModel too.
    private var changeLinkShown: Binding<Bool> {
        Binding(
            get: { model?.state.isConfirmingLinkChange ?? false },
            set: { shown in
                if !shown { model?.send(InviteUiEventOnChangeLinkDismissed.shared) }
            },
        )
    }
}

/// One column on an iPhone upright, the QR card under the title; lying down the text and buttons on the left and the
/// QR card on the right (330 pt on an iPhone, 400 pt in a 920 pt container with an iPad's room); upright with an
/// iPad's room one 520 pt column with the QR card first, the text centered under it.
private struct InviteContent: View {
    let model: InviteModel

    /// The screen's own size: a sheet's on an iPhone, the window's when pushed.
    @State private var layout = ScreenLayout()

    private var state: InviteUiState {
        model.state
    }

    var body: some View {
        Group {
            if layout.usesTwoColumns {
                TwoColumnsLayout(
                    secondWidth: layout.isLarge ? 400 : 330,
                    spacing: layout.isLarge ? 64 : 40,
                    alignment: .center,
                ) {
                    ScrollView {
                        InviteText(model: model, isCentered: false, isLarge: layout.isLarge)
                            .padding(.vertical, 20)
                    }
                    .scrollBounceBehavior(.basedOnSize)
                    .defaultScrollAnchor(.center, for: .alignment)
                } second: {
                    ScrollView {
                        card(qrMaxWidth: layout.isLarge ? 240 : 180)
                            .padding(.vertical, 20)
                    }
                    .scrollBounceBehavior(.basedOnSize)
                    .defaultScrollAnchor(.center, for: .alignment)
                }
                .padding(.horizontal, layout.isLarge ? 64 : 20)
                .frame(maxHeight: .infinity)
            } else if layout.isLarge {
                ScrollView {
                    VStack(spacing: 28) {
                        card(qrMaxWidth: 240)
                        InviteText(model: model, isCentered: true, isLarge: true)
                    }
                    .padding(24)
                    .frame(maxWidth: ScreenLayout.singleColumnMaxWidth)
                    .frame(maxWidth: .infinity)
                }
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        InviteHeading(state: state, isLarge: false)
                        card(qrMaxWidth: 240)
                        WhoPicksText(names: state.notJoinedNames)
                        InviteButtons(model: model, spacing: 20)
                    }
                    .padding(20)
                    .frame(maxWidth: ScreenLayout.singleColumnMaxWidth)
                    .frame(maxWidth: .infinity)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.groupedBackground)
        .measuringScreenLayout($layout)
    }

    /// The QR code and the link, or the dashed box while the link waits. An iPhone lying down has room for a smaller
    /// code only.
    @ViewBuilder
    private func card(qrMaxWidth: CGFloat) -> some View {
        if let displayLink = state.displayLink, let qrCode = state.qrCode {
            LinkCard(model: model, displayLink: displayLink, qrCode: qrCode, qrMaxWidth: qrMaxWidth)
        } else {
            PendingLinkBox(state: state) {
                model.send(InviteUiEventOnRetryClicked.shared)
            }
        }
    }
}

/// "Grupo criado" and the title.
private struct InviteHeading: View {
    let state: InviteUiState
    let isLarge: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if state.isNewGroup {
                Label("Group created", systemImage: "checkmark")
                    .font(.footnote.weight(.semibold))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .foregroundStyle(Palette.accent)
                    .background(Palette.accent.opacity(0.14), in: .capsule)
            }
            Text("Invite everyone to \(state.groupName)")
                .font(isLarge ? .largeTitle.bold() : .title2.bold())
        }
    }
}

/// The column the wide boards set beside (or under) the QR card: the title, who picks and the buttons.
private struct InviteText: View {
    let model: InviteModel
    let isCentered: Bool
    let isLarge: Bool

    var body: some View {
        VStack(alignment: isCentered ? .center : .leading, spacing: isLarge ? 16 : 12) {
            InviteHeading(state: model.state, isLarge: isLarge)
                .multilineTextAlignment(isCentered ? .center : .leading)
            WhoPicksText(names: model.state.notJoinedNames)
                .multilineTextAlignment(isCentered ? .center : .leading)
            InviteButtons(model: model)
                .padding(.top, isLarge ? 8 : 4)
        }
        .frame(maxWidth: .infinity, alignment: isCentered ? .center : .leading)
    }
}

/// "Compartilhar convite" and "Ver quem já entrou", or a disabled share and "Ir para o grupo" while the link waits.
private struct InviteButtons: View {
    let model: InviteModel
    /// An iPhone upright spaces them like the rest of its column.
    var spacing: CGFloat = 8

    var body: some View {
        let state = model.state
        VStack(spacing: spacing) {
            if let link = state.link, state.displayLink != nil, state.qrCode != nil {
                ShareLink(item: link, message: inviteShareMessage(groupName: state.groupName)) {
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
    }
}

/// The message the share sheet sends with the invite link, from the screen and from the Mac's popover.
private func inviteShareMessage(groupName: String) -> Text {
    Text("Let's split the bills of \(groupName) on Blitz Split. Join with this link:")
}

#if os(macOS)
/// The Mac's popover over the group's "Invite people" (F2Mac): the code beside who picks, the link with Copy,
/// "Share…" and, for the group's creator, "Change link…".
private struct InvitePopoverContent: View {
    let model: InviteModel

    var body: some View {
        let state = model.state
        VStack(alignment: .leading, spacing: 12) {
            if let link = state.link, let displayLink = state.displayLink, let qrCode = state.qrCode {
                HStack(spacing: 14) {
                    QrCodeImage(qrCode: qrCode)
                        .frame(width: 120, height: 120)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Invite everyone to \(state.groupName)")
                            .font(.headline)
                        WhoPicksText(names: state.notJoinedNames, font: .caption)
                    }
                }
                HStack(spacing: 6) {
                    Text(verbatim: displayLink)
                        .font(.callout.monospaced())
                        .lineLimit(1)
                        .truncationMode(.middle)
                        .textSelection(.enabled)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Button {
                        model.send(InviteUiEventOnCopyClicked.shared)
                    } label: {
                        if state.isLinkCopied {
                            Label("Copied", systemImage: "checkmark")
                        } else {
                            Text("Copy")
                        }
                    }
                    .controlSize(.small)
                    .accessibilityAddTraits(.updatesFrequently)
                }
                .padding(.leading, 10)
                .padding(.trailing, 4)
                .padding(.vertical, 4)
                .background(Palette.card, in: .rect(cornerRadius: 8))
                ShareLink(item: link, message: inviteShareMessage(groupName: state.groupName)) {
                    Label("Share…", systemImage: "square.and.arrow.up")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.glassProminent)
                .buttonBorderShape(.capsule)
                if state.showsChangeLink {
                    Button("Change link…") {
                        model.send(InviteUiEventOnChangeLinkClicked.shared)
                    }
                    .buttonStyle(.link)
                    .disabled(state.isChangingLink)
                    .frame(maxWidth: .infinity)
                }
            } else {
                PendingLinkBox(state: state) {
                    model.send(InviteUiEventOnRetryClicked.shared)
                }
                WhoPicksText(names: state.notJoinedNames, font: .caption)
            }
        }
        .padding(16)
    }
}
#endif

private struct LinkCard: View {
    let model: InviteModel
    let displayLink: String
    let qrCode: QrCode
    var qrMaxWidth: CGFloat = 240

    var body: some View {
        VStack(spacing: 16) {
            QrCodeImage(qrCode: qrCode)
                .frame(maxWidth: qrMaxWidth)
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
struct QrCodeImage: View {
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
    var font: Font = .callout

    var body: some View {
        Group {
            if names.isEmpty {
                Text("Whoever opens the link joins the group. No account needed.")
            } else {
                Text("Whoever opens the link picks who they are on the list: \(Text(names.formatted(.list(type: .or))).bold()). No account needed.")
            }
        }
        .font(font)
        .foregroundStyle(.secondary)
    }
}
