import Observation
import Shared
import SwiftUI

@Observable
final class SharedBillModel {
    private(set) var state: SharedBillUiState
    private(set) var commentErrorCount = 0
    private(set) var needsNameCount = 0
    private(set) var commentChangeErrorCount = 0
    private(set) var commentGoneCount = 0

    @ObservationIgnored private let holder: ViewModelHolder<SharedBillViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteSharedBill) {
        holder = BlitzSplitApple.shared.createSharedBillViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! SharedBillUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            switch action {
            case is SharedBillUiActionShowCommentError: self?.commentErrorCount += 1
            case is SharedBillUiActionShowNeedsName: self?.needsNameCount += 1
            case is SharedBillUiActionShowCommentChangeError: self?.commentChangeErrorCount += 1
            case is SharedBillUiActionShowCommentGone: self?.commentGoneCount += 1
            case let share as SharedBillUiActionShareComment: SystemShare.share(BillCopy.shareMessage(share.share))
            default: break
            }
        })
    }

    /// The state as the ViewModel holds it right now, before the watcher brings it to `state`: a sheet reports itself
    /// closed after the ViewModel already closed it.
    private var current: SharedBillUiState {
        (holder.viewModel.uiState.value as? SharedBillUiState) ?? state
    }

    func send(_ event: any SharedBillUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    /// The viewer, open while the ViewModel has an attachment to show; closing it any way tells the ViewModel.
    var isViewerOpen: Binding<Bool> {
        Binding(
            get: { self.state.viewedAttachmentId != nil },
            set: { isOpen in
                guard !isOpen, self.current.viewedAttachmentId != nil else { return }
                self.send(SharedBillUiEventOnViewerDismissed.shared)
            },
        )
    }

    /// "Delete comment?", open while the ViewModel has a comment to delete; closing it any way tells the ViewModel.
    var isCommentDeletionOpen: Binding<Bool> {
        Binding(
            get: { self.state.commentToDelete != nil },
            set: { isOpen in
                guard !isOpen, self.current.commentToDelete != nil else { return }
                self.send(SharedBillUiEventOnCommentDeleteDismissed.shared)
            },
        )
    }

    /// "Likes" on an iPhone and an iPad, open while the comment still has a like; closing it tells the ViewModel.
    var isLikesOpen: Binding<Bool> {
        Binding(
            get: { self.state.likesComment != nil },
            set: { isOpen in
                guard !isOpen, self.current.likesCommentId != nil else { return }
                self.send(SharedBillUiEventOnCommentLikesDismissed.shared)
            },
        )
    }

    var comments: BillCommentsValues {
        let send = self.send
        // "Reply" only where there is a composer to answer in
        let onReply: ((String) -> Void)? = state.showsComposer
            ? { send(SharedBillUiEventOnReplyClicked(commentId: $0)) }
            : nil
        return BillCommentsValues(
            header: BillCopy.commentsHeader(KotlinInt(int: state.commentCount)),
            showsNoComments: state.showsNoComments,
            isLoading: false,
            threads: state.threads,
            targetIds: [state.replyTarget?.commentId, state.editingCommentId],
            likesCommentId: state.likesComment?.id,
            actions: BillCommentActions(
                onReply: onReply,
                canChange: state.canChangeComments,
                onLike: { send(SharedBillUiEventOnCommentLikeClicked(commentId: $0)) },
                onLikes: { send(SharedBillUiEventOnCommentLikesClicked(commentId: $0)) },
                onLikesDismissed: { send(SharedBillUiEventOnCommentLikesDismissed.shared) },
                onShare: { send(SharedBillUiEventOnCommentShareClicked(commentId: $0)) },
                onEdit: { send(SharedBillUiEventOnCommentEditClicked(commentId: $0)) },
                onDelete: { send(SharedBillUiEventOnCommentDeleteClicked(commentId: $0)) },
            ),
        )
    }

    var composer: BillComposerValues {
        let send = self.send
        return BillComposerValues(
            draft: Binding(
                get: { self.current.commentDraft },
                set: { send(SharedBillUiEventOnCommentChanged(text: $0)) },
            ),
            isEditing: state.isEditingComment,
            replyTarget: state.replyTarget,
            canSend: state.canSendComment,
            onSend: { send(SharedBillUiEventOnSendCommentClicked.shared) },
            onEditCancelled: { send(SharedBillUiEventOnCommentEditCancelled.shared) },
            onReplyCancelled: { send(SharedBillUiEventOnReplyCancelled.shared) },
        )
    }

    var attachments: BillAttachmentsValues {
        let send = self.send
        return BillAttachmentsValues(
            tiles: state.attachments,
            onOpen: { send(SharedBillUiEventOnAttachmentClicked(id: $0)) },
        )
    }

    var viewer: BillViewerValues {
        let send = self.send
        return BillViewerValues(
            tiles: state.attachments,
            viewed: state.viewedAttachment,
            viewedIndex: state.viewedAttachmentIndex?.intValue,
            photoCount: state.photoCount,
            isOnline: state.isOnline,
            page: Binding(
                get: { self.state.viewedAttachmentId ?? "" },
                set: { id in
                    if id != self.current.viewedAttachmentId { send(SharedBillUiEventOnViewerPageChanged(id: id)) }
                },
            ),
            onClose: { send(SharedBillUiEventOnViewerDismissed.shared) },
            onDownload: { send(SharedBillUiEventOnDownloadClicked.shared) },
        )
    }

    isolated deinit {
        holder.clear()
    }
}

/// A bill's link opened (7.3h), blitzsplit.com/c/<code>: the bill's own page, pushed over wherever the app is (the
/// welcome without a profile), as an invite. It draws the bill as Bill does, read-only (no Share, Edit, Delete,
/// History nor adding or removing photos), with its attachments and comments under "This bill is from the group X";
/// whoever is signed in comments, in the group or not, and likes comments and edits and deletes their own, online
/// only. A link shared at a comment scrolls to it. "Open in the app" and "Sign in" sit in the bar, beside its back
/// button.
struct SharedBillView: View {
    let route: RouteSharedBill

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: SharedBillModel?

    #if os(iOS)
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    #endif

    var body: some View {
        ZStack {
            Palette.groupedBackground.ignoresSafeArea()
            if let model { content(model) }
        }
        .navigationTitle(Text(verbatim: ""))
        #if os(iOS)
        .toolbarTitleDisplayMode(.inline)
        // On iPhone the composer floats at the bottom, where the app's tabs would be, as on Bill
        .toolbar(horizontalSizeClass == .compact ? .hidden : .automatic, for: .tabBar)
        #endif
        .toolbar { toolbar }
        .billCommentDeletionAlert(
            isPresented: model?.isCommentDeletionOpen ?? .constant(false),
            onConfirm: { model?.send(SharedBillUiEventOnCommentDeleteConfirmed.shared) },
            onDismiss: { model?.send(SharedBillUiEventOnCommentDeleteDismissed.shared) },
        )
        .glassToast(BillCommentToasts.messages(
            goneCount: model?.commentGoneCount ?? 0,
            sendErrorCount: model?.commentErrorCount ?? 0,
            changeErrorCount: model?.commentChangeErrorCount ?? 0,
        ) + [GlassToastMessage(text: "Create your profile to comment.", trigger: model?.needsNameCount ?? 0)])
        .onAppear {
            if model == nil { model = SharedBillModel(route: route) }
        }
    }

    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        // Someone in the group with the bill on this device: Bill, over its group
        if model?.state.canOpenInApp == true {
            ToolbarItem(placement: .primaryAction) {
                Button("Open in the app") {
                    model?.send(SharedBillUiEventOnOpenInAppClicked.shared)
                }
            }
        }
        if model?.state.showsSignIn == true {
            ToolbarItem(placement: .primaryAction) {
                Button("Sign in") {
                    model?.send(SharedBillUiEventOnSignInClicked.shared)
                }
            }
        }
    }

    @ViewBuilder
    private func content(_ model: SharedBillModel) -> some View {
        let state = model.state
        let leave = { model.send(SharedBillUiEventOnCloseClicked.shared) }
        switch state.status {
        case .open:
            #if os(macOS)
            MacSharedBill(model: model)
            #else
            PhoneSharedBill(model: model)
            #endif
        case .notFound:
            SharedBillProblem(
                title: "This link doesn't open anymore",
                systemImage: "link",
                message: "Whoever shared it may have turned the link off or deleted the bill.",
                onLeave: leave,
            ) {}
        case .membersOnly:
            SharedBillProblem(
                title: "Only people in the group open this bill",
                systemImage: "lock",
                // Signed in with the group's own kind of account already: this one isn't in the group
                message: state.showsSignIn
                    ? "Sign in with the account you use in the group."
                    : "Your account isn't in this bill's group. Ask whoever shared it to invite you.",
                onLeave: leave,
            ) {
                if state.showsSignIn {
                    Button("Sign in") {
                        model.send(SharedBillUiEventOnSignInClicked.shared)
                    }
                    .buttonStyle(.glassProminent)
                    .prominentLabel()
                }
            }
        case .failed:
            SharedBillProblem(
                title: "Couldn't open the bill",
                systemImage: "icloud.slash",
                message: state.isOnline ? nil : "No internet.",
                onLeave: leave,
            ) {
                Button("Try again") {
                    model.send(SharedBillUiEventOnRetryClicked.shared)
                }
                .buttonStyle(.glassProminent)
                .prominentLabel()
            }
        default:
            BillLoadingShimmer()
        }
    }
}

/// Why the link doesn't open, what may help (`actions`), and "Go to Blitz Split", which leaves the page.
private struct SharedBillProblem<Actions: View>: View {
    let title: LocalizedStringKey
    let systemImage: String
    let message: LocalizedStringKey?
    let onLeave: () -> Void
    @ViewBuilder let actions: Actions

    var body: some View {
        ContentUnavailableView {
            Label(title, systemImage: systemImage)
        } description: {
            if let message {
                Text(message)
            }
        } actions: {
            actions
            Button("Go to Blitz Split", action: onLeave)
        }
    }
}

/// "This bill is from the group Trip. Whoever is in the group also sees their own part and the history."
private struct SharedBillGroupNotice: View {
    let groupName: String

    var body: some View {
        BillNotice(
            text: Text(
                "This bill is from the group \(groupName). Whoever is in the group also sees their own part and the history.",
            ),
            systemImage: "info.circle",
        )
    }
}

/// "Want to comment? Sign in with an account. You don't need to be in the group.", in the composer's place while
/// nobody is signed in.
private struct SharedBillSignInToComment: View {
    let onSignIn: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Want to comment? Sign in with an account. You don't need to be in the group.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
            Button("Sign in to comment", action: onSignIn)
                .buttonStyle(.bordered)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

#if os(iOS)
/// The page on an iPhone and an iPad, in Bill's own columns (`BillPhoneColumns`): the group's notice, the bill and
/// its Attachments, then how it splits and its comments, with the composer for whoever is signed in. A photo opens
/// full screen over it.
private struct PhoneSharedBill: View {
    let model: SharedBillModel

    @State private var layout = ScreenLayout()
    /// The comment the link was shared at, tinted for a moment.
    @State private var highlightedCommentId: String?
    @FocusState private var isComposing: Bool

    private var state: SharedBillUiState {
        model.state
    }

    var body: some View {
        ScrollViewReader { proxy in
            BillPhoneColumns(usesTwoColumns: layout.usesTwoColumns, showsComposer: state.showsComposer) { wraps in
                Section {
                    SharedBillGroupNotice(groupName: state.groupName)
                }
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())
                // Nobody opens a profile from a bill's link page, which is outside the app's groups
                BillHeadSection(bill: state, lonePayer: nil, onPerson: { _ in }) {}
                if !state.attachments.isEmpty {
                    BillAttachmentsSection(attachments: model.attachments, wraps: wraps) {}
                }
            } second: {
                BillSplitSection(bill: state, onPerson: { _ in })
                BillCommentsSection(comments: model.comments, highlightedId: highlightedCommentId) {
                    isComposing = true
                }
                if state.showsSignInToComment {
                    Section {
                        SharedBillSignInToComment {
                            model.send(SharedBillUiEventOnSignInClicked.shared)
                        }
                        .padding(.vertical, 4)
                    }
                }
            } composer: { isFloating in
                BillCommentComposer(composer: model.composer, isFloating: isFloating, focus: $isComposing)
            }
            .modifier(BillCommentFocus(
                focusedCommentId: state.focusedCommentId,
                proxy: proxy,
                highlighted: $highlightedCommentId,
                onShown: { model.send(SharedBillUiEventOnFocusedCommentShown.shared) },
            ))
        }
        .measuringScreenLayout($layout)
        .fullScreenCover(isPresented: model.isViewerOpen) {
            BillAttachmentViewer(viewer: model.viewer)
        }
        .sheet(isPresented: model.isLikesOpen) {
            BillLikesSheet(likers: state.likesComment?.likers ?? []) {
                model.send(SharedBillUiEventOnCommentLikesDismissed.shared)
            }
        }
    }
}
#endif

#if os(macOS)
/// The page in the Mac's window: one centered column of at most 720 pt, as the web's page, with the composer under it
/// for whoever is signed in. A photo opens in a sheet.
private struct MacSharedBill: View {
    let model: SharedBillModel

    /// The comment the link was shared at, tinted for a moment.
    @State private var highlightedCommentId: String?
    @FocusState private var isComposing: Bool

    private static let columnWidth: CGFloat = 720

    private var state: SharedBillUiState {
        model.state
    }

    var body: some View {
        VStack(spacing: 0) {
            ScrollViewReader { proxy in
                ScrollView {
                    column
                        .frame(maxWidth: Self.columnWidth, alignment: .leading)
                        .padding(20)
                        .frame(maxWidth: .infinity)
                }
                .modifier(BillCommentFocus(
                    focusedCommentId: state.focusedCommentId,
                    proxy: proxy,
                    highlighted: $highlightedCommentId,
                    onShown: { model.send(SharedBillUiEventOnFocusedCommentShown.shared) },
                ))
            }
            if state.showsComposer {
                Divider()
                BillCommentComposer(composer: model.composer, isFloating: false, focus: $isComposing)
                    .frame(maxWidth: Self.columnWidth)
                    .padding(16)
            }
        }
        .sheet(isPresented: model.isViewerOpen) {
            BillAttachmentViewer(viewer: model.viewer)
        }
    }

    private var column: some View {
        VStack(alignment: .leading, spacing: 20) {
            SharedBillGroupNotice(groupName: state.groupName)
            HStack(alignment: .top, spacing: 12) {
                CategoryBadge(category: state.category, size: 40)
                // The notice above already names the group
                MacBillTitles(bill: state, groupName: nil, isNarrow: false)
            }
            // Nobody opens a profile from a bill's link page, which is outside the app's groups
            MacBillTotal(bill: state, lonePayer: nil, isNarrow: false, onPerson: { _ in }) {}
            if !state.attachments.isEmpty {
                VStack(alignment: .leading, spacing: 6) {
                    BillAttachmentsHeader(attachments: model.attachments)
                        .font(.headline)
                    BillAttachmentStrip(attachments: model.attachments, wraps: true, metrics: .standard) {}
                }
            }
            MacBillSplit(bill: state, onPerson: { _ in })
            MacBillComments(comments: model.comments, showsTitle: true, highlightedId: highlightedCommentId) {
                isComposing = true
            }
            if state.showsSignInToComment {
                SharedBillSignInToComment {
                    model.send(SharedBillUiEventOnSignInClicked.shared)
                }
                .padding(12)
                .background(Palette.card, in: .rect(cornerRadius: 10))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
#endif
