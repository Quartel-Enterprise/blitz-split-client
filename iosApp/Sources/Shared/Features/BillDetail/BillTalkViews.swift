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

/// "Offline. Comments and photos wait in the queue and go up once you're back online.", at the top of Bill.
struct BillOfflineBanner: View {
    var body: some View {
        Label {
            Text("Offline. Comments and photos wait in the queue and go up once you're back online.")
                .fixedSize(horizontal: false, vertical: true)
        } icon: {
            Image(systemName: "icloud.slash")
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

/// The orange dot of what is still queued, beside its words.
struct BillQueuedLine: View {
    let text: LocalizedStringKey

    var body: some View {
        HStack(spacing: 6) {
            Circle()
                .fill(.orange)
                .frame(width: 7, height: 7)
                .accessibilityHidden(true)
            Text(text)
        }
        .font(.caption)
        .foregroundStyle(.secondary)
    }
}

/// One comment or reply: the face, "Carla · Via link · Today, 8:40 AM", what was written, "Queued" while it waits
/// to go up, and "Reply".
struct BillCommentView: View {
    let comment: BillCommentRow
    let isReply: Bool
    let metrics: BillTalkMetrics
    let onReply: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: metrics.spacing) {
            AvatarPhoto(
                initial: comment.initials,
                color: comment.avatarColor,
                photoUrl: nil,
                size: isReply ? metrics.replyAvatar : metrics.avatar,
            )
            .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 3) {
                header
                BillCopy.body(comment)
                    .fixedSize(horizontal: false, vertical: true)
                    .textSelection(.enabled)
                if comment.isPending {
                    BillQueuedLine(text: "Queued · goes up once you're back online")
                }
                Button("Reply", action: onReply)
                    .buttonStyle(.borderless)
                    .font(.footnote.weight(.semibold))
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
/// changed ("Bar → Restaurant"), Created's note, and "Undo" on the latest edit.
struct BillHistoryEntryView: View {
    let entry: BillHistoryEntry
    let currency: GroupCurrency
    let categories: CategoryTree
    let isUndoing: Bool
    let metrics: BillTalkMetrics
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
                        Circle()
                            .fill(.orange)
                            .frame(width: 7, height: 7)
                            .accessibilityLabel(Text("Queued", comment: "A history line done offline and not up yet"))
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
        // "Undo" stays a button of its own
        .accessibilityElement(children: entry.canUndo ? .contain : .combine)
    }

    private var face: some View {
        AvatarPhoto(initial: entry.actorInitials, color: entry.actorColor, photoUrl: nil, size: metrics.avatar)
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
            .accessibilityHidden(true)
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

/// "Replying to Diego" with its ✕, over the composer.
struct BillReplyChip: View {
    let target: BillReplyTarget
    let isFloating: Bool
    let onCancel: () -> Void

    var body: some View {
        HStack(spacing: 4) {
            Text(verbatim: BillCopy.replying(target))
                .lineLimit(1)
            Button(action: onCancel) {
                Image(systemName: "xmark.circle.fill")
                    .symbolRenderingMode(.hierarchical)
            }
            .buttonStyle(.borderless)
            .accessibilityLabel(Text("Cancel reply"))
            .help(Text("Cancel reply"))
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

/// "Write a comment" and "Send comment", with "Replying to Diego" over them while a reply is being written. A glass
/// capsule floating over the bottom of an iPhone's page (`isFloating`), a plain one under the comments in two columns
/// and in the Mac's sheet.
struct BillCommentComposer: View {
    let model: BillDetailModel
    let isFloating: Bool
    let focus: FocusState<Bool>.Binding

    private var state: BillDetailUiState {
        model.state
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let target = state.replyTarget {
                BillReplyChip(target: target, isFloating: isFloating) {
                    model.send(BillDetailUiEventOnReplyCancelled.shared)
                }
            }
            #if os(macOS)
            HStack(alignment: .bottom, spacing: 8) {
                field
                    .textFieldStyle(.roundedBorder)
                Button("Send", action: send)
                    .buttonStyle(.borderedProminent)
                    .keyboardShortcut(.return, modifiers: .command)
                    .disabled(!state.canSendComment)
                    .help(Text("Send comment"))
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
            text: model.commentDraft,
            prompt: Text("Write a comment"),
            axis: .vertical,
        ) {
            Text("Comment", comment: "The bill's comment field, for a screen reader")
        }
        .lineLimit(1 ... 5)
        .focused(focus)
    }

    #if os(iOS)
    private var sendButton: some View {
        Button(action: send) {
            Image(systemName: "arrow.up")
                .font(.body.weight(.semibold))
                .frame(width: 36, height: 36)
                .foregroundStyle(state.canSendComment ? Palette.onAccent : .secondary)
                .background(state.canSendComment ? Palette.accent : Color(uiColor: .tertiarySystemFill), in: .circle)
        }
        .buttonStyle(.plain)
        .disabled(!state.canSendComment)
        .accessibilityLabel(Text("Send comment"))
    }
    #endif

    private func send() {
        guard model.current.canSendComment else { return }
        model.send(BillDetailUiEventOnSendCommentClicked.shared)
    }
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
