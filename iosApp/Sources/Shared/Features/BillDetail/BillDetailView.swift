import Observation
import Shared
import SwiftUI

@Observable
final class BillDetailModel {
    private(set) var state: BillDetailUiState
    private(set) var deleteErrorCount = 0
    private(set) var commentErrorCount = 0
    private(set) var commentChangeErrorCount = 0
    private(set) var commentGoneCount = 0
    private(set) var undoErrorCount = 0
    private(set) var unreadablePhotoCount = 0
    private(set) var tooLargePhotoCount = 0
    private(set) var noRoomPhotoCount = 0
    private(set) var failedPhotoCount = 0
    private(set) var removePhotoErrorCount = 0

    @ObservationIgnored private let holder: ViewModelHolder<BillDetailViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteBillDetail) {
        holder = BlitzSplitApple.shared.createBillDetailViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! BillDetailUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            switch action {
            case is BillDetailUiActionShowDeleteError: self?.deleteErrorCount += 1
            case is BillDetailUiActionShowCommentError: self?.commentErrorCount += 1
            case is BillDetailUiActionShowCommentChangeError: self?.commentChangeErrorCount += 1
            case is BillDetailUiActionShowCommentGone: self?.commentGoneCount += 1
            case let share as BillDetailUiActionShareComment: SystemShare.share(BillCopy.shareMessage(share.share))
            case is BillDetailUiActionShowUndoError: self?.undoErrorCount += 1
            case let photo as BillDetailUiActionShowPhotoError: self?.countPhotoError(photo.error)
            case is BillDetailUiActionShowRemovePhotoError: self?.removePhotoErrorCount += 1
            default: break
            }
        })
    }

    private func countPhotoError(_ error: BillPhotoError) {
        switch error {
        case .unreadable: unreadablePhotoCount += 1
        case .tooLarge: tooLargePhotoCount += 1
        case .noRoom: noRoomPhotoCount += 1
        default: failedPhotoCount += 1
        }
    }

    /// The state as the ViewModel holds it right now, before the watcher brings it to `state`: "Delete" has
    /// already started deleting by the time the alert reports itself closed.
    var current: BillDetailUiState {
        (holder.viewModel.uiState.value as? BillDetailUiState) ?? state
    }

    func send(_ event: any BillDetailUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    /// "Write a comment", read back from the ViewModel at once so a keystroke never shows the text before it.
    var commentDraft: Binding<String> {
        Binding(
            get: { self.current.commentDraft },
            set: { self.send(BillDetailUiEventOnCommentChanged(text: $0)) },
        )
    }

    /// A photo taken or picked, as the file came; nil when the platform couldn't read it.
    func photoPicked(_ data: Data?) {
        guard let data else {
            send(BillDetailUiEventOnPhotoUnreadable.shared)
            return
        }
        send(BillDetailUiEventOnPhotoPicked(source: BlitzSplitApple.shared.toByteArray(data: data)))
    }

    /// The viewer, open while the ViewModel has an attachment to show; closing it any way tells the ViewModel.
    var isViewerOpen: Binding<Bool> {
        Binding(
            get: { self.state.viewedAttachmentId != nil },
            set: { isOpen in
                if !isOpen, self.current.viewedAttachmentId != nil { self.send(BillDetailUiEventOnViewerDismissed.shared) }
            },
        )
    }

    /// The viewer's page, which a swipe or an arrow moves.
    var viewedAttachmentId: Binding<String> {
        Binding(
            get: { self.state.viewedAttachmentId ?? "" },
            set: { id in
                if id != self.current.viewedAttachmentId { self.send(BillDetailUiEventOnViewerPageChanged(id: id)) }
            },
        )
    }

    /// "Delete comment?", open while the ViewModel has a comment to delete; closing it any way tells the ViewModel.
    var isCommentDeletionOpen: Binding<Bool> {
        Binding(
            get: { self.state.commentToDelete != nil },
            set: { isOpen in
                guard !isOpen, self.current.commentToDelete != nil else { return }
                self.send(BillDetailUiEventOnCommentDeleteDismissed.shared)
            },
        )
    }

    /// "Likes" on an iPhone and an iPad, open while the comment still has a like; closing it tells the ViewModel.
    var isLikesOpen: Binding<Bool> {
        Binding(
            get: { self.state.likesComment != nil },
            set: { isOpen in
                guard !isOpen, self.current.likesCommentId != nil else { return }
                self.send(BillDetailUiEventOnCommentLikesDismissed.shared)
            },
        )
    }

    /// The alert closed without "Delete"; the ViewModel keeps it while the bill is being deleted.
    func dismissDeletion() {
        if current.deletion != nil { send(BillDetailUiEventOnDeleteDismissed.shared) }
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Bill": one bill of the group, what it was, who paid, its Attachments and how it splits, then its Comments and
/// History (7.3). Pushed
/// over the group on iPhone, a sheet on iPad (820×640 lying down, 620×860 upright) and the window's inspector on the
/// Mac; Share, Edit and Delete in the toolbar (on the Mac, in the inspector's header).
struct BillDetailView: View {
    let route: RouteBillDetail

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: BillDetailModel?

    #if os(iOS)
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    #endif
    /// The window under the sheet or the inspector, not them: whether this bill is a sheet that closes or a pushed
    /// screen, and how wide the Mac's inspector is.
    @Environment(\.windowLayout) private var windowLayout

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                #if os(macOS)
                MacBillDetail(model: model)
                #else
                PhoneBillDetail(model: model)
                #endif
            } else {
                // The bill not read yet: its title, amount and split shimmer in place
                ShimmerScreen {
                    HStack(spacing: 12) {
                        ShimmerBlock(width: 40, height: 40, cornerRadius: 12)
                        VStack(alignment: .leading, spacing: 8) {
                            ShimmerLine(width: 120, height: 10)
                            ShimmerLine(width: 200, height: 20)
                        }
                    }
                    ShimmerLine(width: 140, height: 28)
                    ShimmerList(count: 4)
                }
                .background(Palette.groupedBackground.ignoresSafeArea())
            }
        }
        #if os(iOS)
        .navigationTitle(Text(verbatim: model?.state.groupName ?? ""))
        .navigationBarTitleDisplayMode(.inline)
        // On iPhone the bill keeps the group's own bars, without the app's tabs
        .toolbar(horizontalSizeClass == .compact ? .hidden : .automatic, for: .tabBar)
        .toolbar { toolbar }
        // A sheet with an iPad's room, 820×640 lying down and 620×860 upright as the canvas draws it, wide enough for the
        // two columns lying down; a pushed screen takes the size it is given
        .billSheetSizing(in: windowLayout)
        #else
        // Set while the bill loads too, so the inspector opens at its width rather than jumping to it
        .inspectorColumnWidth(
            min: MacBillInspector.narrowWidth,
            ideal: MacBillInspector.width(in: windowLayout),
            max: MacBillInspector.wideWidth,
        )
        #endif
        .billDeletionAlert(
            model?.state.deletion,
            isDeleting: model?.state.isDeleting == true,
            onConfirm: { model?.send(BillDetailUiEventOnDeleteConfirmed.shared) },
            onDismiss: { model?.dismissDeletion() },
        )
        .alert("Delete comment?", isPresented: model?.isCommentDeletionOpen ?? .constant(false)) {
            Button(role: .destructive) {
                model?.send(BillDetailUiEventOnCommentDeleteConfirmed.shared)
            } label: {
                Text(BillCopy.delete)
            }
            Button("Cancel", role: .cancel) {
                model?.send(BillDetailUiEventOnCommentDeleteDismissed.shared)
            }
        } message: {
            Text("It leaves the bill for everyone. Its replies stay.")
        }
        .glassToast([
            GlassToastMessage(text: "That comment was deleted.", trigger: model?.commentGoneCount ?? 0),
            GlassToastMessage(text: "Couldn't send the comment. Try again.", trigger: model?.commentErrorCount ?? 0),
            GlassToastMessage(
                text: "Couldn't save the change to the comment.",
                trigger: model?.commentChangeErrorCount ?? 0,
            ),
            GlassToastMessage(text: "Couldn't undo. Try again.", trigger: model?.undoErrorCount ?? 0),
            GlassToastMessage(text: "Couldn't delete the bill. Try again.", trigger: model?.deleteErrorCount ?? 0),
        ] + BillAttachmentToasts.messages(model))
        .onAppear {
            if model == nil { model = BillDetailModel(route: route) }
        }
    }

    #if os(iOS)
    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        // A form sheet on iPad has no back button: it closes
        if AppRoute.billDetail(route).presentsAsSheet(isLargeWindow: windowLayout.isLarge) {
            ToolbarItem(placement: .cancellationAction) {
                Button("Close", systemImage: "xmark", role: .cancel) {
                    model?.send(BillDetailUiEventOnBackClicked.shared)
                }
            }
        }
        ToolbarItem(placement: .primaryAction) {
            Button("Share", systemImage: "square.and.arrow.up") {
                model?.send(BillDetailUiEventOnShareClicked.shared)
            }
            .disabled(model?.state.areActionsEnabled != true)
        }
        ToolbarItem(placement: .primaryAction) {
            Button("Edit") {
                model?.send(BillDetailUiEventOnEditClicked.shared)
            }
            .disabled(model?.state.canEdit != true)
        }
        ToolbarItem(placement: .primaryAction) {
            Button(role: .destructive) {
                model?.send(BillDetailUiEventOnDeleteClicked.shared)
            } label: {
                Label {
                    Text(BillCopy.delete)
                } icon: {
                    Image(systemName: "trash")
                }
            }
            .disabled(model?.state.areActionsEnabled != true)
        }
    }
    #endif
}

/// "Bruno paid" next to Bruno's face, or "A former member paid". A lone payer opens their profile from here; several
/// each have their row in the split, which opens them.
private struct PayerLine: View {
    let payers: [BillPayer]
    let lonePayer: BillPayer?
    let size: CGFloat
    let onPerson: (String) -> Void

    var body: some View {
        HStack(spacing: 8) {
            // Whoever left the group has no face
            let faces = payers.filter { $0.name != nil }.prefix(3)
            if !faces.isEmpty {
                HStack(spacing: -size * 0.3) {
                    ForEach(Array(faces.enumerated()), id: \.offset) { _, payer in
                        AvatarPhoto(initial: payer.initials, color: payer.avatarColor, photoUrl: payer.photoUrl, size: size)
                    }
                }
                .accessibilityHidden(true)
            }
            Text(verbatim: BillCopy.paid(payers))
        }
        .opensProfile(lonePayer.map { payer in { onPerson(payer.memberId) } })
    }
}

/// "Queued · syncs when you're back online", with the amber dot of what is still queued.
private struct PendingLine: View {
    var body: some View {
        HStack(spacing: 8) {
            PendingDot()
            Text("Queued · syncs when you're back online")
        }
    }
}

/// Under a bill in a currency only a newer app knows, whose Edit is off.
private struct UnknownCurrencyNote: View {
    var body: some View {
        Text("This bill uses a currency this app version doesn't know. Update the app to edit it.")
            .font(.subheadline)
            .foregroundStyle(.secondary)
    }
}

/// "−R$ 118,00 you owe" in the negative color, "+R$ 249,60 to get back" in the positive green, zero muted, and who
/// settles it.
private struct MyPartContent: View {
    let state: BillDetailUiState

    var body: some View {
        if let minor = state.myBalanceMinor?.int64Value {
            VStack(alignment: .leading, spacing: 4) {
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text(verbatim: BillCopy.myPartAmount(minor, currency: state.currency))
                        .font(.title2.bold())
                        .monospacedDigit()
                    Text(verbatim: BillCopy.myPartWord(minor))
                        .font(.body.weight(.semibold))
                }
                .foregroundStyle(Palette.balance(minor))
                if let note = BillCopy.myPartNote(state.myPartNote, currency: state.currency) {
                    Text(verbatim: note)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .combine)
        }
    }
}

/// One person of the split: their face, "Paid R$ 642,00" or "Hasn't joined yet", and their part. The row opens their
/// profile, which someone no longer in the group doesn't have.
private struct ShareRow: View {
    let row: BillShareRow
    let currency: GroupCurrency
    let avatarSize: CGFloat
    let onPerson: (String) -> Void

    var body: some View {
        HStack(spacing: 12) {
            ShareAvatar(row: row, size: avatarSize)
            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: BillCopy.name(row))
                    .lineLimit(1)
                if let subtitle = BillCopy.subtitle(row, currency: currency) {
                    Text(verbatim: subtitle)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer(minLength: 8)
            Text(verbatim: BillCopy.share(row, currency: currency))
                .monospacedDigit()
                .foregroundStyle(row.shareMinor == nil ? .secondary : .primary)
        }
        .accessibilityElement(children: .combine)
        .opensProfile(row.opensProfile ? { onPerson(row.memberId) } : nil)
    }
}

/// Someone who left the group has no name nor color left: a plain person.
private struct ShareAvatar: View {
    let row: BillShareRow
    let size: CGFloat

    var body: some View {
        Group {
            if row.name == nil, !row.isMe {
                Image(systemName: "person.fill")
                    .font(.system(size: size * 0.45))
                    .foregroundStyle(.secondary)
                    .frame(width: size, height: size)
                    .background(.fill.tertiary, in: .circle)
            } else {
                AvatarPhoto(initial: row.initials, color: row.avatarColor, photoUrl: row.photoUrl, size: size)
            }
        }
        .accessibilityHidden(true)
    }
}

#if os(iOS)
/// Upright and on a narrow iPad sheet: one column, the bill, its Attachments, its split, Comments and History, with the
/// composer floating in glass at the bottom, above the keyboard. Lying down and on a wide iPad sheet: the bill and its
/// Attachments on the left; how it splits, Comments and History on the right with the composer under them. A photo
/// opens full screen over it.
private struct PhoneBillDetail: View {
    let model: BillDetailModel

    @State private var layout = ScreenLayout()
    /// The comment a link opened Bill at, tinted for a moment.
    @State private var highlightedCommentId: String?
    @FocusState private var isComposing: Bool

    private var state: BillDetailUiState {
        model.state
    }

    /// A person's face, name or row: their profile, or Profile for this device's user.
    private func openPerson(_ memberId: String) {
        model.send(BillDetailUiEventOnPersonClicked(memberId: memberId))
    }

    var body: some View {
        ScrollViewReader { proxy in
            columns
                .modifier(BillCommentFocus(model: model, proxy: proxy, highlighted: $highlightedCommentId))
        }
        .safeAreaInset(edge: .top, spacing: 0) {
            if !state.isOnline {
                BillOfflineBanner()
                    .frame(maxWidth: 640)
                    .padding(.horizontal, 20)
                    .padding(.top, 4)
                    .padding(.bottom, 4)
            }
        }
        .measuringScreenLayout($layout)
        .background(Palette.groupedBackground)
        .fullScreenCover(isPresented: model.isViewerOpen) {
            BillAttachmentViewer(model: model)
        }
        .sheet(isPresented: model.isLikesOpen) {
            BillLikesSheet(likers: state.likesComment?.likers ?? []) {
                model.send(BillDetailUiEventOnCommentLikesDismissed.shared)
            }
        }
    }

    @ViewBuilder
    private var columns: some View {
        Group {
            if layout.usesTwoColumns {
                TwoColumnsLayout(spacing: 0) {
                    list {
                        billSections
                        attachmentsSection(wraps: true)
                    }
                } second: {
                    VStack(spacing: 0) {
                        list {
                            splitSection
                            talkSections
                        }
                        BillCommentComposer(model: model, isFloating: false, focus: $isComposing)
                            .padding(.horizontal, 20)
                            .padding(.top, 8)
                            .padding(.bottom, 12)
                    }
                }
            } else {
                list {
                    billSections
                    attachmentsSection(wraps: false)
                    splitSection
                    talkSections
                }
                .frame(maxWidth: 640)
                .frame(maxWidth: .infinity)
                .safeAreaInset(edge: .bottom, spacing: 0) {
                    BillCommentComposer(model: model, isFloating: true, focus: $isComposing)
                        .frame(maxWidth: 640)
                        .padding(.horizontal, 16)
                        .padding(.top, 8)
                        .padding(.bottom, 8)
                }
            }
        }
    }

    private func list<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        List(content: content)
            .listStyle(.insetGrouped)
            .listSectionSpacing(.compact)
            .scrollContentBackground(.hidden)
            // Scrolling back up to the bill puts the keyboard away
            .scrollDismissesKeyboard(.interactively)
    }

    @ViewBuilder
    private var billSections: some View {
        Section {
            VStack(alignment: .leading, spacing: 18) {
                HStack(spacing: 12) {
                    CategoryBadge(category: state.category, size: 52)
                    VStack(alignment: .leading, spacing: 2) {
                        // The bill's category over its title, "Food › Groceries" (7.3)
                        Text(verbatim: CategoryCopy.path(state.category, parent: state.categoryParent))
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        Text(verbatim: state.description_)
                            .font(.title.bold())
                            .lineLimit(3)
                            .accessibilityAddTraits(.isHeader)
                        Text(verbatim: ExpenseFormat.dayTime(state.spentAt, relativeDay: state.spentDay))
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text(verbatim: state.currency.format(state.totalMinor))
                        .font(.system(size: 40, weight: .bold))
                        .monospacedDigit()
                        .lineLimit(1)
                        .minimumScaleFactor(0.5)
                    PayerLine(payers: state.payers, lonePayer: state.lonePayer, size: 24, onPerson: openPerson)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    if state.isPending {
                        PendingLine()
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    if state.showsUnknownCurrencyNote {
                        UnknownCurrencyNote()
                    }
                }
                // The row clips to its rounded corner, which would cut the payer's face at its edge
                .padding(.horizontal, 4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .listRowBackground(Color.clear)
        .listRowInsets(EdgeInsets(top: 8, leading: 0, bottom: 4, trailing: 0))
        if state.showsMyPart {
            Section {
                MyPartContent(state: state)
                    .padding(.vertical, 4)
            } header: {
                Text("Your share")
            }
        }
    }

    /// Attachments under Your share, as the canvas draws them: a strip in one column, wrapping in the bill's own one.
    private func attachmentsSection(wraps: Bool) -> some View {
        Section {
            BillAttachmentStrip(model: model, wraps: wraps, metrics: .standard)
                .padding(.vertical, 4)
        } header: {
            BillAttachmentsHeader(state: state)
        }
        .listRowBackground(Color.clear)
        .listRowInsets(EdgeInsets(top: 4, leading: 4, bottom: 4, trailing: wraps ? 4 : 0))
    }

    private var splitSection: some View {
        Section {
            ForEach(state.people, id: \.memberId) { row in
                ShareRow(row: row, currency: state.currency, avatarSize: 32, onPerson: openPerson)
            }
        } header: {
            Text(verbatim: BillCopy.splitHeader(state.splitMode, count: state.participantCount))
        }
    }

    /// Comments, each with its replies under it, then History by day (hidden while there is none).
    @ViewBuilder
    private var talkSections: some View {
        Section {
            if state.showsNoComments {
                BillNoComments()
                    .padding(.vertical, 4)
            }
            if state.threads.isLoading {
                BillTalkPlaceholder()
            }
            ForEach(state.loadedThreads ?? [], id: \.comment.id) { thread in
                // The section's last row draws no line under the card
                let isLastThread = thread.comment.id == state.loadedThreads?.last?.comment.id
                commentRow(thread.comment, isReply: false, endsThread: thread.replies.isEmpty && !isLastThread)
                ForEach(Array(thread.replies.enumerated()), id: \.element.id) { index, reply in
                    commentRow(reply, isReply: true, endsThread: index == thread.replies.count - 1 && !isLastThread)
                }
            }
        } header: {
            Text(verbatim: BillCopy.commentsHeader(state.commentCount))
        }
        if state.history.isLoading {
            Section {
                BillTalkPlaceholder()
            } header: {
                Text("History", comment: "The bill's history section")
            }
        } else if let history = state.loadedHistory, !history.isEmpty {
            Section {
                ForEach(history, id: \.dayStartMillis) { day in
                    ForEach(Array(day.entries.enumerated()), id: \.element.id) { index, entry in
                        // The day heads its first line, in the same row, so it takes no row's height of its own
                        VStack(alignment: .leading, spacing: 8) {
                            if index == 0 {
                                Text(verbatim: BillCopy.historyDay(day))
                                    .font(.footnote.weight(.semibold))
                                    .foregroundStyle(.secondary)
                                    .accessibilityAddTraits(.isHeader)
                            }
                            BillHistoryEntryView(
                                entry: entry,
                                currency: state.currency,
                                categories: state.categories,
                                isUndoing: state.isUndoing,
                                metrics: .standard,
                                onPerson: openPerson,
                            ) {
                                model.send(BillDetailUiEventOnUndoClicked.shared)
                            }
                        }
                        .padding(.vertical, 2)
                    }
                }
            } header: {
                Text("History", comment: "The bill's history section")
            }
        }
    }

    /// A thread reads as one block: no line between a comment and its replies.
    private func commentRow(_ comment: BillCommentRow, isReply: Bool, endsThread: Bool) -> some View {
        // The comment being answered or edited, tinted while "Replying to" or "Editing comment" shows
        let isTarget = state.replyTarget?.commentId == comment.id || state.editingCommentId == comment.id
        return BillCommentView(
            comment: comment,
            isReply: isReply,
            metrics: .standard,
            showsLikes: false,
            send: model.send,
        ) {
            isComposing = true
        }
        .padding(.vertical, 2)
        .id(comment.id)
        .listRowSeparator(endsThread ? .visible : .hidden, edges: .bottom)
        .listRowBackground(ZStack {
            Palette.card
            if isTarget { Palette.accent.opacity(0.12) }
            // The comment a link opened at, fading out
            Palette.accent.opacity(highlightedCommentId == comment.id ? 0.22 : 0)
        })
    }
}
#endif

#if os(macOS)
/// What the narrow inspector shows, one at a time.
private enum MacBillTab: Hashable {
    case bill, comments, history
}

/// The window's inspector on the right (decision 3, F7Mac): the bill's header with Share, Edit…, Delete and ✕, then, in the
/// standard window's 720 pt, the bill, its Attachments and its split on the left and Comments and History on the right with the
/// composer under them. A narrow window's 330 pt inspector shows one of Bill, Comments and History at a time, the
/// composer under Comments.
private struct MacBillDetail: View {
    let model: BillDetailModel

    @State private var layout = ScreenLayout()
    @State private var tab = MacBillTab.bill
    /// The comment a link opened Bill at, tinted for a moment.
    @State private var highlightedCommentId: String?
    @FocusState private var isFocused: Bool
    @FocusState private var isComposing: Bool

    private var state: BillDetailUiState {
        model.state
    }

    /// A person's face, name or row: their profile, or Profile for this device's user.
    private func openPerson(_ memberId: String) {
        model.send(BillDetailUiEventOnPersonClicked(memberId: memberId))
    }

    /// The inspector itself, not the window: two columns from 600 pt, as the 720 pt inspector draws them.
    private var isNarrow: Bool {
        layout.size.width > 0 && layout.size.width < MacBillInspector.twoColumnsWidth
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
                .padding(isNarrow ? 16 : 20)
            if !state.isOnline {
                BillOfflineBanner()
                    .padding(.horizontal, isNarrow ? 16 : 20)
                    .padding(.bottom, 12)
            }
            if isNarrow {
                Picker(selection: $tab) {
                    Text("Bill", comment: "The narrow Mac sheet's segment with the bill itself").tag(MacBillTab.bill)
                    Text(verbatim: BillCopy.commentsHeader(state.commentCount)).tag(MacBillTab.comments)
                    // History hides while there is none, as everywhere else (it shimmers while it loads)
                    if state.showsHistory {
                        Text("History", comment: "The bill's history section").tag(MacBillTab.history)
                    }
                } label: {
                    Text("Show", comment: "What the bill's narrow Mac sheet shows: the bill, its comments or history")
                }
                .pickerStyle(.segmented)
                .labelsHidden()
                .padding(.horizontal, 16)
                .padding(.bottom, 12)
            }
            Divider()
            if isNarrow {
                narrowContent
            } else {
                HStack(spacing: 0) {
                    ScrollView {
                        billContent
                            .padding(20)
                    }
                    .frame(maxWidth: .infinity)
                    Divider()
                    talkColumn(showsComments: true, showsHistory: true)
                        .frame(maxWidth: .infinity)
                }
            }
        }
        .measuringScreenLayout($layout)
        .background(Palette.groupedBackground)
        .sheet(isPresented: model.isViewerOpen) {
            BillAttachmentViewer(model: model)
        }
        // Esc closes it while the focus is in it, which it takes as it opens, so a field elsewhere keeps its own Esc
        .focusable()
        .focusEffectDisabled()
        .focused($isFocused)
        .onAppear { isFocused = true }
        // The narrow inspector shows the comments, so the one a link opened at can be scrolled to
        .onChange(of: state.focusedCommentId, initial: true) { _, id in
            if id != nil { tab = .comments }
        }
        .onExitCommand {
            model.send(BillDetailUiEventOnBackClicked.shared)
        }
    }

    @ViewBuilder
    private var narrowContent: some View {
        switch tab == .history && !state.showsHistory ? MacBillTab.bill : tab {
        case .bill:
            ScrollView {
                billContent
                    .padding(16)
            }
        case .comments:
            talkColumn(showsComments: true, showsHistory: false)
        case .history:
            talkColumn(showsComments: false, showsHistory: true)
        }
    }

    /// The bill's name beside its buttons; the narrow inspector (330 pt) puts the buttons in a row above it, which
    /// beside them would leave the name a hundred points and cut the group's line to a few letters.
    @ViewBuilder
    private var header: some View {
        if isNarrow {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 12) {
                    Spacer(minLength: 0)
                    headerButtons
                }
                HStack(alignment: .top, spacing: 12) {
                    CategoryBadge(category: state.category, size: 32)
                    headerTitles
                }
            }
        } else {
            HStack(alignment: .top, spacing: 12) {
                CategoryBadge(category: state.category, size: 40)
                headerTitles
                Spacer(minLength: 8)
                headerButtons
            }
        }
    }

    private var headerTitles: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(verbatim: CategoryCopy.path(state.category, parent: state.categoryParent))
                .font(.callout)
                .foregroundStyle(.secondary)
                .lineLimit(1)
            Text(verbatim: state.description_)
                .font(isNarrow ? .title3.bold() : .title2.bold())
                .lineLimit(2)
                .accessibilityAddTraits(.isHeader)
            Text(verbatim: "\(state.groupName) · \(ExpenseFormat.dayTime(state.spentAt, relativeDay: state.spentDay))")
                .foregroundStyle(.secondary)
                .lineLimit(1)
        }
    }

    /// Share, Edit…, Delete and Close.
    @ViewBuilder
    private var headerButtons: some View {
        Button("Share", systemImage: "square.and.arrow.up") {
            model.send(BillDetailUiEventOnShareClicked.shared)
        }
        .labelStyle(.iconOnly)
        .help(Text("Share"))
        .disabled(!state.areActionsEnabled)
        Button {
            model.send(BillDetailUiEventOnEditClicked.shared)
        } label: {
            if isNarrow {
                Label("Edit…", systemImage: "pencil")
                    .labelStyle(.iconOnly)
            } else {
                Text("Edit…")
            }
        }
        .help(Text("Edit…"))
        .disabled(!state.canEdit)
        Button(role: .destructive) {
            model.send(BillDetailUiEventOnDeleteClicked.shared)
        } label: {
            Label {
                Text(BillCopy.delete)
            } icon: {
                Image(systemName: "trash")
            }
            .labelStyle(.iconOnly)
        }
        .help(Text(BillCopy.delete))
        .disabled(!state.areActionsEnabled)
        Button("Close", systemImage: "xmark") {
            model.send(BillDetailUiEventOnBackClicked.shared)
        }
        .labelStyle(.iconOnly)
        .buttonStyle(.borderless)
        .help(Text("Close"))
    }

    private var billContent: some View {
        VStack(alignment: .leading, spacing: 20) {
            VStack(alignment: .leading, spacing: 4) {
                Text(verbatim: state.currency.format(state.totalMinor))
                    .font(.system(size: isNarrow ? 28 : 34, weight: .bold))
                    .monospacedDigit()
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                PayerLine(payers: state.payers, lonePayer: state.lonePayer, size: 20, onPerson: openPerson)
                    .foregroundStyle(.secondary)
                if state.isPending {
                    PendingLine()
                        .foregroundStyle(.secondary)
                }
                if state.showsUnknownCurrencyNote {
                    UnknownCurrencyNote()
                }
            }
            if state.showsMyPart {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Your share")
                        .font(.headline)
                    MyPartContent(state: state)
                        .padding(12)
                        .background(Palette.card, in: .rect(cornerRadius: 10))
                }
            }
            VStack(alignment: .leading, spacing: 6) {
                BillAttachmentsHeader(state: state)
                    .font(.headline)
                BillAttachmentStrip(model: model, wraps: true, metrics: isNarrow ? .narrow : .standard)
            }
            VStack(alignment: .leading, spacing: 6) {
                Text(verbatim: BillCopy.splitHeader(state.splitMode, count: state.participantCount))
                    .font(.headline)
                VStack(spacing: 0) {
                    ForEach(Array(state.people.enumerated()), id: \.element.memberId) { index, row in
                        if index > 0 { Divider() }
                        ShareRow(row: row, currency: state.currency, avatarSize: 24, onPerson: openPerson)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                    }
                }
                .background(Palette.card, in: .rect(cornerRadius: 10))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    /// Comments and History scroll together over the composer, which shows with the comments.
    private func talkColumn(showsComments: Bool, showsHistory: Bool) -> some View {
        VStack(spacing: 0) {
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        if showsComments { comments }
                        if showsHistory { history }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(isNarrow ? 16 : 20)
                }
                .modifier(BillCommentFocus(
                    model: model,
                    proxy: proxy,
                    highlighted: $highlightedCommentId,
                    isEnabled: showsComments,
                ))
            }
            if showsComments {
                Divider()
                BillCommentComposer(model: model, isFloating: false, focus: $isComposing)
                    .padding(isNarrow ? 12 : 16)
            }
        }
    }

    private var comments: some View {
        VStack(alignment: .leading, spacing: 6) {
            // The narrow sheet's segment already says it
            if !isNarrow {
                Text(verbatim: BillCopy.commentsHeader(state.commentCount))
                    .font(.headline)
            }
            if state.showsNoComments {
                BillNoComments()
            }
            if state.threads.isLoading {
                BillTalkPlaceholder()
            }
            ForEach(state.loadedThreads ?? [], id: \.comment.id) { thread in
                VStack(alignment: .leading, spacing: 2) {
                    commentRow(thread.comment, isReply: false)
                    ForEach(thread.replies, id: \.id) { reply in
                        commentRow(reply, isReply: true)
                    }
                }
                .padding(.top, 4)
            }
        }
    }

    private func commentRow(_ comment: BillCommentRow, isReply: Bool) -> some View {
        // The comment being answered or edited, tinted while "Replying to" or "Editing comment" shows; the one a link
        // opened at, fading out
        let isTarget = state.replyTarget?.commentId == comment.id || state.editingCommentId == comment.id
        let isHighlighted = highlightedCommentId == comment.id
        return BillCommentView(
            comment: comment,
            isReply: isReply,
            metrics: .standard,
            showsLikes: state.likesComment?.id == comment.id,
            send: model.send,
        ) {
            isComposing = true
        }
        .padding(6)
        .background(
            Palette.accent.opacity(isHighlighted ? 0.24 : isTarget ? 0.14 : 0),
            in: .rect(cornerRadius: 8),
        )
        .id(comment.id)
    }

    @ViewBuilder
    private var history: some View {
        if state.showsHistory {
            VStack(alignment: .leading, spacing: 10) {
                if !isNarrow {
                    Text("History", comment: "The bill's history section")
                        .font(.headline)
                }
                if state.history.isLoading {
                    BillTalkPlaceholder()
                }
                ForEach(state.loadedHistory ?? [], id: \.dayStartMillis) { day in
                    Text(verbatim: BillCopy.historyDay(day))
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.secondary)
                        .accessibilityAddTraits(.isHeader)
                    ForEach(day.entries, id: \.id) { entry in
                        BillHistoryEntryView(
                            entry: entry,
                            currency: state.currency,
                            categories: state.categories,
                            isUndoing: state.isUndoing,
                            metrics: .standard,
                            onPerson: openPerson,
                        ) {
                            model.send(BillDetailUiEventOnUndoClicked.shared)
                        }
                        .padding(.horizontal, 6)
                    }
                }
            }
        }
    }
}

/// The inspector's widths the canvas draws: 720 pt in the standard window, 330 pt in a narrow one, where the window
/// would leave the groups too little room beside 720 pt.
private enum MacBillInspector {
    static let wideWidth: CGFloat = 720
    static let narrowWidth: CGFloat = 330
    /// The bill and its split beside Comments and History from here, as the 720 pt inspector draws them.
    static let twoColumnsWidth: CGFloat = 600
    /// The narrowest window that keeps the sidebar and a list beside the 720 pt inspector.
    static let wideWindowWidth: CGFloat = 1180

    static func width(in window: ScreenLayout) -> CGFloat {
        window.size.width >= wideWindowWidth ? wideWidth : narrowWidth
    }
}
#endif
