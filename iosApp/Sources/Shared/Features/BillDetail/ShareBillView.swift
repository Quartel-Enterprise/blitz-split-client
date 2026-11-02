import Observation
import Shared
import SwiftUI

@Observable
final class ShareBillModel {
    private(set) var state: ShareBillUiState
    private(set) var updateErrorCount = 0
    private(set) var disableErrorCount = 0

    @ObservationIgnored private let holder: ViewModelHolder<ShareBillViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteShareBill) {
        holder = BlitzSplitApple.shared.createShareBillViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! ShareBillUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            // Send link is a ShareLink: a share sheet can't be opened from code
            switch action {
            case let copy as ShareBillUiActionCopyLink: Pasteboard.copy(copy.link)
            case let error as ShareBillUiActionShowError: self?.countError(error.error)
            default: break
            }
        })
    }

    private func countError(_ error: ShareBillError) {
        switch error {
        case .disableFailed: disableErrorCount += 1
        default: updateErrorCount += 1
        }
    }

    func send(_ event: any ShareBillUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Share bill" (7.3), over the bill: its link, who can open it, Copy, Turn off link and Send link. The server keeps
/// the link, so changing it needs the internet; a link already known still copies and sends offline. A sheet with
/// medium and large detents on an iPhone, a 540 pt form sheet with an iPad's room (over the bill's own sheet), and a
/// sheet on the Mac over the window that shows the bill in its inspector, as Contacts' invite.
struct ShareBillView: View {
    let route: RouteShareBill

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: ShareBillModel?

    /// The window under the sheet: an iPad's room centres the form sheet, which a medium detent would pin to the bottom.
    @Environment(\.windowLayout) private var windowLayout

    var body: some View {
        Group {
            if let model {
                ShareBillContent(model: model)
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        .navigationTitle(Text("Share bill"))
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Close", systemImage: "xmark", role: .cancel) {
                    model?.send(ShareBillUiEventOnCloseClicked.shared)
                }
            }
        }
        .glassToast([
            GlassToastMessage(text: "Couldn't change who can open it. Try again.", trigger: model?.updateErrorCount ?? 0),
            GlassToastMessage(text: "Couldn't turn off the link. Try again.", trigger: model?.disableErrorCount ?? 0),
        ])
        #if os(iOS)
        // An iPhone lying down has a compact height, where the sheet takes the whole screen whatever the detents
        .presentationDetents(windowLayout.isLarge ? [.large] : [.medium, .large])
        .formSheetSizing()
        #else
        .frame(minWidth: 440, idealWidth: 480, minHeight: 420, idealHeight: 500)
        #endif
        .onAppear {
            if model == nil { model = ShareBillModel(route: route) }
        }
    }
}

private struct ShareBillContent: View {
    let model: ShareBillModel

    private var state: ShareBillUiState {
        model.state
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                VStack(alignment: .leading, spacing: 2) {
                    #if os(macOS)
                    // A Mac sheet shows no navigation title
                    Text("Share bill")
                        .font(.title2.bold())
                        .accessibilityAddTraits(.isHeader)
                    #endif
                    Text(verbatim: "\(state.description_) · \(state.currency.format(state.totalMinor))")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
                ShareBillLinkRow(model: model)
                ShareBillVisibilityPicker(model: model)
                Label {
                    Text("The link shows the bill, who paid, each one's part, the attachments and the comments. Balances and the group's other bills don't show.")
                        .fixedSize(horizontal: false, vertical: true)
                } icon: {
                    Image(systemName: "info.circle")
                }
                .font(.footnote)
                .foregroundStyle(.secondary)
                if state.showsOfflineNotice {
                    Label("Sharing needs the internet.", systemImage: "wifi.slash")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(20)
            .frame(maxWidth: ScreenLayout.singleColumnMaxWidth)
            .frame(maxWidth: .infinity)
        }
        // Always in sight, the medium detent included
        .safeAreaInset(edge: .bottom) {
            ShareBillButtons(model: model)
                .padding(.horizontal, 20)
                .padding(.vertical, 8)
        }
        .background(Palette.groupedBackground)
    }
}

/// The link in a field-like row with Copy; a progress while it is read or made, and Try again once that failed.
private struct ShareBillLinkRow: View {
    let model: ShareBillModel

    private var state: ShareBillUiState {
        model.state
    }

    var body: some View {
        HStack(spacing: 12) {
            if state.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity)
            } else if state.hasFailed {
                Text("Couldn't make the link.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Button("Try again") {
                    model.send(ShareBillUiEventOnRetryClicked.shared)
                }
                .buttonStyle(.bordered)
                .buttonBorderShape(.capsule)
                .disabled(!state.isOnline)
            } else {
                Text(verbatim: state.displayLink ?? "—")
                    .font(.subheadline.monospaced())
                    .foregroundStyle(state.displayLink == nil ? .secondary : .primary)
                    .lineLimit(1)
                    .truncationMode(.middle)
                    .textSelection(.enabled)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Button {
                    model.send(ShareBillUiEventOnCopyClicked.shared)
                } label: {
                    if state.isLinkCopied {
                        Label("Copied", systemImage: "checkmark")
                    } else {
                        Label("Copy", systemImage: "doc.on.doc")
                    }
                }
                .buttonStyle(.bordered)
                .buttonBorderShape(.capsule)
                .accessibilityAddTraits(.updatesFrequently)
                .disabled(!state.canSendLink)
            }
        }
        .padding(.leading, 16)
        .padding(.trailing, 8)
        .frame(minHeight: 52)
        .background(Palette.card, in: .rect(cornerRadius: 16))
    }
}

/// "Who can open it": only the people in the bill, or anyone with the link.
private struct ShareBillVisibilityPicker: View {
    let model: ShareBillModel

    private var state: ShareBillUiState {
        model.state
    }

    /// Who joined, this device's user aside, in the language's list ("Ana, Bruno e Carla").
    private var membersDetail: String {
        let names = state.memberNames
        let list = names.formatted(.list(type: .and))
        switch names.count {
        case 0: return String(localized: "Whoever is in the group opens it with their own account to see and comment.")
        case 1: return String(localized: "\(list) opens it with their own account to see and comment.")
        default: return String(localized: "\(list) open it with their own account to see and comment.")
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Who can open it")
                .font(.headline)
                .accessibilityAddTraits(.isHeader)
            VStack(spacing: 0) {
                option(
                    .members,
                    title: Text("Only people in the bill"),
                    detail: Text(verbatim: membersDetail),
                )
                Divider()
                    .padding(.leading, 48)
                option(
                    .anyone,
                    title: Text("Anyone with the link"),
                    detail: Text("Sees it without signing in. To comment, they sign in with an account, even outside the group."),
                )
            }
            .background(Palette.card, in: .rect(cornerRadius: 16))
            .disabled(!state.canChangeLink)
        }
    }

    private func option(_ visibility: BillLinkVisibility, title: Text, detail: Text) -> some View {
        let isSelected = visibility == state.visibility
        return Button {
            model.send(ShareBillUiEventOnVisibilityPicked(visibility: visibility))
        } label: {
            HStack(alignment: .top, spacing: 14) {
                Image(systemName: isSelected ? "largecircle.fill.circle" : "circle")
                    .font(.title3)
                    .foregroundStyle(isSelected ? Palette.accent : .secondary)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 2) {
                    title
                        .font(.body.weight(.medium))
                    detail
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

/// Turn off link, then Send link: the system's share with the bill's name before the link.
private struct ShareBillButtons: View {
    let model: ShareBillModel

    private var state: ShareBillUiState {
        model.state
    }

    var body: some View {
        HStack(spacing: 10) {
            Button(role: .destructive) {
                model.send(ShareBillUiEventOnDisableClicked.shared)
            } label: {
                Text("Turn off link")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
            .disabled(!state.canChangeLink)
            if let link = state.link {
                ShareLink(item: link, message: Text("Bill “\(state.description_)” on Blitz Split:")) {
                    Label("Send link", systemImage: "square.and.arrow.up")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
            } else {
                Button {} label: {
                    Label("Send link", systemImage: "square.and.arrow.up")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .disabled(true)
            }
        }
        .controlSize(.large)
        .buttonBorderShape(.capsule)
    }
}
