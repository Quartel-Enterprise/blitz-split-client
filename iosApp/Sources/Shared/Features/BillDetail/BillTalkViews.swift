import Shared
import SwiftUI

/// The sizes of Comments and History: an iPhone's and iPad's list, or the Mac's denser sheet.
struct BillTalkMetrics {
    let avatar: CGFloat
    let replyAvatar: CGFloat
    let replyIndent: CGFloat
    let spacing: CGFloat

    #if os(macOS)
    static let standard = BillTalkMetrics(avatar: 22, replyAvatar: 18, replyIndent: 30, spacing: 8)
    #else
    static let standard = BillTalkMetrics(avatar: 32, replyAvatar: 28, replyIndent: 44, spacing: 12)
    #endif
}

/// A line the page says about the bill as a whole, on a card over it: Bill's offline notice, and the group a bill's
/// link page belongs to.
struct BillNotice: View {
    let text: Text
    let systemImage: String

    var body: some View {
        Label {
            text
                .fixedSize(horizontal: false, vertical: true)
        } icon: {
            Image(systemName: systemImage)
        }
        .font(.footnote)
        .foregroundStyle(.secondary)
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Palette.card, in: .rect(cornerRadius: 16))
        .accessibilityElement(children: .combine)
    }
}

/// "Offline. Comments and photos wait in the queue and go up once you're back online.", at the top of Bill.
struct BillOfflineBanner: View {
    var body: some View {
        BillNotice(
            text: Text("Offline. Comments and photos wait in the queue and go up once you're back online."),
            systemImage: "icloud.slash",
        )
    }
}

/// What a comment's buttons do, on Bill and on a bill's link page.
struct BillCommentActions {
    /// The author's face and name; nil on a bill's link page, which opens nobody's profile.
    var onPerson: ((String) -> Void)?
    /// "Reply"; nil where there is no composer to answer in, so it doesn't show.
    var onReply: ((String) -> Void)?
    /// The heart, Edit and Delete wait while false: a bill's link page sends each straight to the server.
    var canChange = true
    let onLike: (String) -> Void
    let onLikes: (String) -> Void
    let onLikesDismissed: () -> Void
    let onShare: (String) -> Void
    let onEdit: (String) -> Void
    let onDelete: (String) -> Void
}

/// A bill's comments as its page draws them, on Bill and on a bill's link page.
struct BillCommentsValues {
    /// "Comments · 3"
    let header: String
    let showsNoComments: Bool
    let isLoading: Bool
    let threads: [BillCommentThread]
    /// The comment being answered or edited, tinted while "Replying to" or "Editing comment" shows.
    let targetIds: [String?]
    /// The comment whose "Likes" is open, which the Mac shows as a popover on its heart.
    let likesCommentId: String?
    let actions: BillCommentActions
}

/// What the composer holds and does, on Bill and on a bill's link page.
struct BillComposerValues {
    /// "Write a comment", read back from the ViewModel at once so a keystroke never shows the text before it.
    let draft: Binding<String>
    let isEditing: Bool
    let replyTarget: BillReplyTarget?
    let canSend: Bool
    let onSend: () -> Void
    let onEditCancelled: () -> Void
    let onReplyCancelled: () -> Void
}

/// The amber dot of what is still queued, beside its words. The attachment viewer, black in both appearances, passes
/// `Palette.pendingOnDark`.
struct BillQueuedLine: View {
    let text: LocalizedStringKey
    var dotColor: Color = Palette.pending

    var body: some View {
        HStack(spacing: 6) {
            PendingDot(size: 7, color: dotColor)
            Text(text)
        }
        .font(.caption)
        .foregroundStyle(.secondary)
    }
}

/// One comment or reply: the face, "Carla · Via link · Today, 8:40 AM · edited", what was written, "Queued" while
/// it waits to go up, then "Reply", the heart with how many liked it, and ⋯ with Share, and Edit and Delete on the
/// user's own (the same as the row's context menu, a right click on the Mac). The author's face and name open their
/// profile while they are in the group. A deleted comment kept over its replies reads "Comment deleted", with
/// nothing to tap.
struct BillCommentView: View {
    let comment: BillCommentRow
    let isReply: Bool
    let metrics: BillTalkMetrics
    /// Whether "Likes" is open on this comment: the Mac shows it as a popover on the heart.
    let showsLikes: Bool
    let actions: BillCommentActions
    /// "Reply" and "Edit" put the cursor in the composer.
    let focusComposer: () -> Void

    private var openAuthor: (() -> Void)? {
        guard let id = comment.authorMemberId, let onPerson = actions.onPerson else { return nil }
        return { onPerson(id) }
    }

    var body: some View {
        if comment.isDeleted {
            Text("Comment deleted", comment: "A deleted comment kept over its replies")
                .font(.subheadline)
                .italic()
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.vertical, 4)
        } else {
            content
                .contextMenu {
                    menuActions
                    if comment.likeCount > 0 {
                        Button("Likes", systemImage: "heart", action: openLikes)
                    }
                }
        }
    }

    private var content: some View {
        HStack(alignment: .top, spacing: metrics.spacing) {
            AvatarPhoto(
                initial: comment.initials,
                color: comment.avatarColor,
                photoUrl: comment.photoUrl,
                size: isReply ? metrics.replyAvatar : metrics.avatar,
            )
            // The name is the one stop for VoiceOver; the face is only a bigger target for the finger
            .opensProfile(openAuthor)
            .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 3) {
                header
                BillCopy.body(comment)
                    .fixedSize(horizontal: false, vertical: true)
                    .textSelection(.enabled)
                if comment.isPending {
                    BillQueuedLine(text: "Queued · goes up once you're back online")
                }
                footer
                    .padding(.top, 1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.leading, isReply ? metrics.replyIndent : 0)
        .accessibilityElement(children: .contain)
    }

    private var header: some View {
        HStack(alignment: .firstTextBaseline, spacing: 6) {
            Text(verbatim: BillCopy.author(comment))
                .fontWeight(.semibold)
                .lineLimit(1)
                .opensProfile(openAuthor)
            if comment.viaLink {
                Text("Via link", comment: "A comment written through the bill's open link, by someone out of the group")
                    .font(.caption2.weight(.semibold))
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(Palette.segmentThumb, in: .capsule)
                    .overlay(Capsule().strokeBorder(.separator, lineWidth: 0.5))
            }
            Text(verbatim: BillCopy.posted(comment))
                .font(.footnote)
                .foregroundStyle(.secondary)
                .lineLimit(1)
        }
        .font(.subheadline)
    }

    /// "Reply", the heart and its count, and ⋯ at the end; each a button of its own, never the whole row.
    private var footer: some View {
        HStack(spacing: 14) {
            if let onReply = actions.onReply {
                Button("Reply") {
                    onReply(comment.id)
                    focusComposer()
                }
                .font(.footnote.weight(.semibold))
            }
            likes
            Spacer(minLength: 0)
            Menu {
                menuActions
            } label: {
                Label("More options", systemImage: "ellipsis")
                    .labelStyle(.iconOnly)
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.secondary)
                    .frame(minWidth: 28, minHeight: 22)
                    .contentShape(.rect)
                    .fingerTarget()
            }
            .menuIndicator(.hidden)
            #if os(macOS)
            .menuStyle(.borderlessButton)
            #endif
            .fixedSize()
            .help(Text("More options"))
        }
        .buttonStyle(.borderless)
    }

    /// The heart, filled once the user liked it, and how many did, which lists them.
    private var likes: some View {
        HStack(spacing: 4) {
            Button {
                actions.onLike(comment.id)
            } label: {
                Image(systemName: comment.isLikedByMe ? "heart.fill" : "heart")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(comment.isLikedByMe ? Color.red : Color.secondary)
                    .contentTransition(.symbolEffect(.replace))
                    .fingerTarget()
            }
            .disabled(!actions.canChange)
            .accessibilityLabel(comment.isLikedByMe ? Text("Unlike") : Text("Like"))
            .help(comment.isLikedByMe ? Text("Unlike") : Text("Like"))
            if comment.likeCount > 0 {
                Button(action: openLikes) {
                    Text(verbatim: "\(comment.likeCount)")
                        .font(.footnote.weight(.semibold))
                        .monospacedDigit()
                        .foregroundStyle(.secondary)
                }
                .accessibilityLabel(Text(verbatim: BillCopy.likes(comment.likeCount)))
                // Who liked it, on hover over the Mac's count
                .help(Text(verbatim: ExpenseFormat.names(comment.likers.map(BillCopy.liker))))
            }
        }
        #if os(macOS)
        .popover(isPresented: likesPopover, arrowEdge: .bottom) {
            BillLikesList(likers: comment.likers)
                .frame(width: 240)
                .frame(maxHeight: 320)
        }
        #endif
    }

    #if os(macOS)
    /// Closing the popover any way tells the ViewModel.
    private var likesPopover: Binding<Bool> {
        Binding(
            get: { showsLikes },
            set: { isShown in
                if !isShown, showsLikes { actions.onLikesDismissed() }
            },
        )
    }
    #endif

    /// Share, then Edit and Delete on the user's own comment.
    @ViewBuilder
    private var menuActions: some View {
        Button("Share", systemImage: "square.and.arrow.up") {
            actions.onShare(comment.id)
        }
        if comment.canChange, actions.canChange {
            Button("Edit", systemImage: "pencil") {
                actions.onEdit(comment.id)
                focusComposer()
            }
            Button(role: .destructive) {
                actions.onDelete(comment.id)
            } label: {
                Label {
                    Text(BillCopy.delete)
                } icon: {
                    Image(systemName: "trash")
                }
            }
        }
    }

    private func openLikes() {
        actions.onLikes(comment.id)
    }
}

/// "Likes": whoever liked a comment, in the order they did, "You" for the user and "Someone" for a name the server
/// didn't keep. A medium sheet on an iPhone and an iPad, a popover on the heart on the Mac.
struct BillLikesList: View {
    let likers: [BillCommentLiker]

    var body: some View {
        List {
            Section {
                ForEach(Array(likers.enumerated()), id: \.offset) { _, liker in
                    BillLikerRow(liker: liker)
                }
            } header: {
                Text("Likes", comment: "Who liked a comment")
            }
        }
        #if os(macOS)
        .listStyle(.inset)
        #endif
    }
}

/// One name of "Likes", beside a heart.
private struct BillLikerRow: View {
    let liker: BillCommentLiker

    var body: some View {
        Label {
            Text(verbatim: BillCopy.liker(liker))
        } icon: {
            Image(systemName: "heart.fill")
                .foregroundStyle(.red)
        }
    }
}

#if os(iOS)
/// The iPhone's and iPad's "Likes", a medium sheet with Close.
struct BillLikesSheet: View {
    let likers: [BillCommentLiker]
    let onClose: () -> Void

    var body: some View {
        NavigationStack {
            List {
                ForEach(Array(likers.enumerated()), id: \.offset) { _, liker in
                    BillLikerRow(liker: liker)
                }
            }
            .navigationTitle(Text("Likes", comment: "Who liked a comment"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", systemImage: "xmark", role: .cancel, action: onClose)
                }
            }
        }
        .presentationDetents([.medium, .large])
        // An iPhone Duo open: in the half before the fold, never over it
        .foldSheetPlacement()
    }
}
#endif

/// Bill's comments, the read of a restored account's copy, or History, which online waits for the server: until they
/// come, a few lines shimmer in their place, never "No comments yet".
struct BillTalkPlaceholder: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ShimmerList(count: 2, showsTrailing: false)
        }
        .shimmering()
        .padding(.vertical, 4)
    }
}

extension BillDetailUiState {
    /// The comments once read; nil while they load.
    var loadedThreads: [BillCommentThread]? {
        threads.loaded(as: [BillCommentThread].self)
    }

    /// History's days once known; nil while they load.
    var loadedHistory: [BillHistoryDay]? {
        history.loaded(as: [BillHistoryDay].self)
    }

    /// The receipt and the photos once read; nil while they load.
    var loadedAttachments: [BillAttachmentTile]? {
        attachments.loaded(as: [BillAttachmentTile].self)
    }

    /// History shows while it loads (shimmering) and once it holds a line; never empty.
    var showsHistory: Bool {
        history.isLoading || loadedHistory?.isEmpty == false
    }
}

/// "No comments yet. Ask or agree on something about this bill."
struct BillNoComments: View {
    var body: some View {
        Text("No comments yet. Ask or agree on something about this bill.")
            .font(.subheadline)
            .foregroundStyle(.secondary)
            .fixedSize(horizontal: false, vertical: true)
    }
}

/// One line of History: the face with what was done on its corner, "**Ana** changed the category · 9:12 AM", what
/// changed ("Bar → Restaurant"), Created's note, and "Undo" on the latest edit. The face opens the actor's profile
/// while they are in the group.
struct BillHistoryEntryView: View {
    let entry: BillHistoryEntry
    let currency: GroupCurrency
    let categories: CategoryTree
    let isUndoing: Bool
    let metrics: BillTalkMetrics
    let onPerson: (String) -> Void
    let onUndo: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: metrics.spacing) {
            face
            VStack(alignment: .leading, spacing: 3) {
                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    BillCopy.headline(entry)
                        .fixedSize(horizontal: false, vertical: true)
                    Spacer(minLength: 4)
                    if entry.isPending {
                        PendingDot(size: 7, label: Text("Queued", comment: "A history line done offline and not up yet"))
                    }
                    Text(verbatim: ExpenseFormat.time(entry.occurredAt))
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .monospacedDigit()
                }
                .font(.subheadline)
                ForEach(BillCopy.diffs(entry, currency: currency, categories: categories), id: \.self) { diff in
                    BillHistoryDiffLine(diff: diff)
                }
                if let note = BillCopy.createdNote(entry, currency: currency) {
                    Text(verbatim: note)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                if entry.canUndo {
                    Button("Undo", action: onUndo)
                        .buttonStyle(.borderless)
                        .font(.footnote.weight(.semibold))
                        .disabled(isUndoing)
                        .padding(.top, 1)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        // "Undo" and the face that opens the profile stay buttons of their own
        .accessibilityElement(children: entry.canUndo || entry.actorMemberId != nil ? .contain : .combine)
    }

    private var face: some View {
        AvatarPhoto(
            initial: entry.actorInitials,
            color: entry.actorColor,
            photoUrl: entry.actorPhotoUrl,
            size: metrics.avatar,
        )
            .overlay(alignment: .bottomTrailing) {
                let badge = metrics.avatar * 0.5
                Image(systemName: BillCopy.historyIcon(entry))
                    .font(.system(size: badge * 0.55, weight: .semibold))
                    .foregroundStyle(Palette.onPixTile)
                    .frame(width: badge, height: badge)
                    .background(Palette.pixTile, in: .circle)
                    .overlay(Circle().strokeBorder(Palette.card, lineWidth: 1.5))
                    .offset(x: badge * 0.25, y: badge * 0.25)
            }
            .accessibilityLabel(Text(verbatim: BillCopy.actor(entry)))
            .opensProfile(entry.actorMemberId.map { id in { onPerson(id) } })
            .accessibilityHidden(entry.actorMemberId == nil)
    }
}

/// "Carla R$ 112.00 → R$ 98.00", the field in its own color, or the field alone over the parts.
private struct BillHistoryDiffLine: View {
    let diff: BillHistoryDiff

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 6) {
            if let field = diff.field {
                Text(verbatim: field)
                    .foregroundStyle(.secondary)
            }
            if let before = diff.before, let after = diff.after {
                Text(verbatim: before)
                    .foregroundStyle(.secondary)
                Image(systemName: "arrow.right")
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(.secondary)
                    .accessibilityLabel(Text("to", comment: "Bill history: before, to after"))
                Text(verbatim: after)
            }
        }
        .font(.footnote)
        .monospacedDigit()
        .lineLimit(2)
    }
}

/// "Replying to Diego" or "Editing comment" with its ✕, over the composer.
struct BillComposerChip: View {
    let text: String
    let cancelLabel: LocalizedStringKey
    let isFloating: Bool
    let onCancel: () -> Void

    var body: some View {
        HStack(spacing: 4) {
            Text(verbatim: text)
                .lineLimit(1)
            Button(action: onCancel) {
                Image(systemName: "xmark.circle.fill")
                    .symbolRenderingMode(.hierarchical)
            }
            .buttonStyle(.borderless)
            .accessibilityLabel(Text(cancelLabel))
            .help(Text(cancelLabel))
        }
        .font(.footnote)
        .foregroundStyle(.secondary)
        .padding(.leading, 12)
        .padding(.trailing, 6)
        .padding(.vertical, 5)
        .modifier(ChipBackground(isFloating: isFloating))
    }
}

/// Glass over the scrolling page, like the composer under it; a card's fill elsewhere.
private struct ChipBackground: ViewModifier {
    let isFloating: Bool

    func body(content: Content) -> some View {
        #if os(iOS)
        if isFloating {
            content.glassEffect(.regular, in: .capsule)
        } else {
            content.background(Palette.card, in: .capsule)
        }
        #else
        content.background(Palette.card, in: .capsule)
        #endif
    }
}

/// "Write a comment" and "Send comment", with "Replying to Diego" over them while a reply is being written, or
/// "Editing comment" and Save while the field holds the user's own comment. A glass
/// capsule floating over the bottom of an iPhone's page (`isFloating`), a plain one under the comments in two columns
/// and in the Mac's sheet.
struct BillCommentComposer: View {
    let composer: BillComposerValues
    let isFloating: Bool
    let focus: FocusState<Bool>.Binding

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // The ViewModel never keeps both: editing drops the reply, and replying drops the edit
            if composer.isEditing {
                BillComposerChip(
                    text: BillCopy.editing,
                    cancelLabel: "Cancel editing",
                    isFloating: isFloating,
                    onCancel: composer.onEditCancelled,
                )
            } else if let target = composer.replyTarget {
                BillComposerChip(
                    text: BillCopy.replying(target),
                    cancelLabel: "Cancel reply",
                    isFloating: isFloating,
                    onCancel: composer.onReplyCancelled,
                )
            }
            #if os(macOS)
            HStack(alignment: .bottom, spacing: 8) {
                field
                    .textFieldStyle(.roundedBorder)
                Button(action: composer.onSend) {
                    composer.isEditing ? Text("Save") : Text("Send")
                }
                .buttonStyle(.borderedProminent)
                .prominentLabel()
                .keyboardShortcut(.return, modifiers: .command)
                .disabled(!composer.canSend)
                .help(composer.isEditing ? Text("Save") : Text("Send comment"))
            }
            #else
            HStack(alignment: .bottom, spacing: 8) {
                field
                    .padding(.vertical, 13)
                sendButton
                    .padding(.vertical, 6)
            }
            .padding(.leading, 18)
            .padding(.trailing, 6)
            .modifier(ComposerCapsule(isFloating: isFloating))
            #endif
        }
    }

    private var field: some View {
        TextField(
            text: composer.draft.firstLetterCapitalized,
            prompt: Text("Write a comment"),
            axis: .vertical,
        ) {
            Text("Comment", comment: "The bill's comment field, for a screen reader")
        }
        .lineLimit(1 ... 5)
        .capitalizesFirstLetter()
        .focused(focus)
    }

    #if os(iOS)
    private var sendButton: some View {
        Button(action: composer.onSend) {
            // Saving an edit is a check, sending a new comment an arrow
            Image(systemName: composer.isEditing ? "checkmark" : "arrow.up")
                .font(.body.weight(.semibold))
                .frame(width: 36, height: 36)
                .foregroundStyle(composer.canSend ? Palette.onAccent : .secondary)
                .background(composer.canSend ? Palette.accent : Color(uiColor: .tertiarySystemFill), in: .circle)
        }
        .buttonStyle(.plain)
        .disabled(!composer.canSend)
        .accessibilityLabel(composer.isEditing ? Text("Save") : Text("Send comment"))
    }
    #endif
}

#if os(iOS)
/// Liquid Glass floating over the page, a card's fill where it sits in a column.
private struct ComposerCapsule: ViewModifier {
    let isFloating: Bool

    func body(content: Content) -> some View {
        if isFloating {
            content.glassEffect(.regular.interactive(), in: .rect(cornerRadius: 24))
        } else {
            content.background(Palette.card, in: .rect(cornerRadius: 24))
        }
    }
}
#endif

/// The comment a link or Activity opened Bill at: once it is drawn, scrolled to the middle and tinted for a moment
/// (`highlighted`, fading out over about two seconds), then the ViewModel is told, once. Only on the scroll view that
/// holds the comments (`isEnabled`): the Mac's narrow inspector shows History in another.
struct BillCommentFocus: ViewModifier {
    let focusedCommentId: String?
    let proxy: ScrollViewProxy
    @Binding var highlighted: String?
    var isEnabled = true
    /// Tells the ViewModel the comment was shown, which clears it.
    let onShown: () -> Void

    func body(content: Content) -> some View {
        content.task(id: focusedCommentId) {
            guard isEnabled, let id = focusedCommentId else { return }
            // Why: the rows lay out first, the comments' tab included, or there is nothing to scroll to yet
            try? await Task.sleep(for: .milliseconds(350))
            guard !Task.isCancelled else { return }
            withAnimation { proxy.scrollTo(id, anchor: .center) }
            highlighted = id
            // Why: not this task, which telling the ViewModel ends, as it clears the comment it runs for
            Task {
                try? await Task.sleep(for: .milliseconds(700))
                withAnimation(.easeOut(duration: 1.3)) {
                    if highlighted == id { highlighted = nil }
                }
            }
            onShown()
        }
    }
}

/// What went wrong with a comment, said in the glass toast of Bill and of a bill's link page.
enum BillCommentToasts {
    static func messages(goneCount: Int, sendErrorCount: Int, changeErrorCount: Int) -> [GlassToastMessage] {
        [
            GlassToastMessage(text: "That comment was deleted.", trigger: goneCount),
            GlassToastMessage(text: "Couldn't send the comment. Try again.", trigger: sendErrorCount),
            GlassToastMessage(text: "Couldn't save the change to the comment.", trigger: changeErrorCount),
        ]
    }
}

extension View {
    /// "Delete comment?", open while the ViewModel has a comment to delete.
    func billCommentDeletionAlert(
        isPresented: Binding<Bool>,
        onConfirm: @escaping () -> Void,
        onDismiss: @escaping () -> Void,
    ) -> some View {
        alert("Delete comment?", isPresented: isPresented) {
            Button(role: .destructive, action: onConfirm) {
                Text(BillCopy.delete)
            }
            Button("Cancel", role: .cancel, action: onDismiss)
        } message: {
            Text("It leaves the bill for everyone. Its replies stay.")
        }
    }
}

#if os(iOS)
/// Comments in a bill's list, each with its replies under it: a thread reads as one block, with no line between a
/// comment and its replies.
struct BillCommentsSection: View {
    let comments: BillCommentsValues
    /// The comment a link opened the page at, tinted for a moment.
    let highlightedId: String?
    let focusComposer: () -> Void

    var body: some View {
        Section {
            if comments.showsNoComments {
                BillNoComments()
                    .padding(.vertical, 4)
            }
            if comments.isLoading {
                BillTalkPlaceholder()
            }
            ForEach(comments.threads, id: \.comment.id) { thread in
                // The section's last row draws no line under the card
                let isLastThread = thread.comment.id == comments.threads.last?.comment.id
                row(thread.comment, isReply: false, endsThread: thread.replies.isEmpty && !isLastThread)
                ForEach(Array(thread.replies.enumerated()), id: \.element.id) { index, reply in
                    row(reply, isReply: true, endsThread: index == thread.replies.count - 1 && !isLastThread)
                }
            }
        } header: {
            Text(verbatim: comments.header)
        }
    }

    private func row(_ comment: BillCommentRow, isReply: Bool, endsThread: Bool) -> some View {
        BillCommentView(
            comment: comment,
            isReply: isReply,
            metrics: .standard,
            showsLikes: false,
            actions: comments.actions,
            focusComposer: focusComposer,
        )
        .padding(.vertical, 2)
        .id(comment.id)
        .listRowSeparator(endsThread ? .visible : .hidden, edges: .bottom)
        .listRowBackground(ZStack {
            Palette.card
            if comments.targetIds.contains(comment.id) { Palette.accent.opacity(0.12) }
            // The comment a link opened at, fading out
            Palette.accent.opacity(highlightedId == comment.id ? 0.22 : 0)
        })
    }
}
#endif

#if os(macOS)
/// Comments on the Mac, each with its replies under it; `showsTitle` is false where a segment already says
/// "Comments".
struct MacBillComments: View {
    let comments: BillCommentsValues
    let showsTitle: Bool
    /// The comment a link opened the page at, tinted for a moment.
    let highlightedId: String?
    let focusComposer: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if showsTitle {
                Text(verbatim: comments.header)
                    .font(.headline)
            }
            if comments.showsNoComments {
                BillNoComments()
            }
            if comments.isLoading {
                BillTalkPlaceholder()
            }
            ForEach(comments.threads, id: \.comment.id) { thread in
                VStack(alignment: .leading, spacing: 2) {
                    row(thread.comment, isReply: false)
                    ForEach(thread.replies, id: \.id) { reply in
                        row(reply, isReply: true)
                    }
                }
                .padding(.top, 4)
            }
        }
    }

    private func row(_ comment: BillCommentRow, isReply: Bool) -> some View {
        let isTarget = comments.targetIds.contains(comment.id)
        let isHighlighted = highlightedId == comment.id
        return BillCommentView(
            comment: comment,
            isReply: isReply,
            metrics: .standard,
            showsLikes: comments.likesCommentId == comment.id,
            actions: comments.actions,
            focusComposer: focusComposer,
        )
        .padding(6)
        .background(
            Palette.accent.opacity(isHighlighted ? 0.24 : isTarget ? 0.14 : 0),
            in: .rect(cornerRadius: 8),
        )
        .id(comment.id)
    }
}
#endif
