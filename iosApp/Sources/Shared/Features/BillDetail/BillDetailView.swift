import Observation
import Shared
import SwiftUI

@Observable
final class BillDetailModel {
    private(set) var state: BillDetailUiState
    private(set) var deleteErrorCount = 0
    private(set) var commentErrorCount = 0
    private(set) var undoErrorCount = 0

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
            case is BillDetailUiActionShowUndoError: self?.undoErrorCount += 1
            default: break
            }
        })
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

    /// The alert closed without "Delete"; the ViewModel keeps it while the bill is being deleted.
    func dismissDeletion() {
        if current.deletion != nil { send(BillDetailUiEventOnDeleteDismissed.shared) }
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Bill": one bill of the group, what it was, who paid and how it splits, then its Comments and History (7.3). Pushed
/// over the group on iPhone, a form sheet on iPad and a sheet on the Mac's window; Edit and Delete in the toolbar (on
/// the Mac, in the sheet's header).
struct BillDetailView: View {
    let route: RouteBillDetail

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: BillDetailModel?

    #if os(iOS)
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    /// The window under the sheet, not the sheet: whether this bill is a sheet that closes or a pushed screen.
    @Environment(\.windowLayout) private var windowLayout
    #endif

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                #if os(macOS)
                MacBillDetail(model: model)
                #else
                PhoneBillDetail(model: model)
                #endif
            } else {
                Palette.groupedBackground.ignoresSafeArea()
                    #if os(macOS)
                    .frame(width: 560, height: 480)
                    #endif
            }
        }
        #if os(iOS)
        .navigationTitle(Text(verbatim: model?.state.groupName ?? ""))
        .navigationBarTitleDisplayMode(.inline)
        // On iPhone the bill keeps the group's own bars, without the app's tabs
        .toolbar(horizontalSizeClass == .compact ? .hidden : .automatic, for: .tabBar)
        .toolbar { toolbar }
        // With an iPad's room the sheet is the canvas' 820×640 lying down, wide enough for the two columns; a pushed
        // screen takes the size it is given
        .billSheetSizing(in: windowLayout)
        #endif
        .billDeletionAlert(
            model?.state.deletion,
            currency: model?.state.currency,
            isDeleting: model?.state.isDeleting == true,
            onConfirm: { model?.send(BillDetailUiEventOnDeleteConfirmed.shared) },
            onDismiss: { model?.dismissDeletion() },
        )
        .glassToast([
            GlassToastMessage(text: "Couldn't send the comment. Try again.", trigger: model?.commentErrorCount ?? 0),
            GlassToastMessage(text: "Couldn't undo. Try again.", trigger: model?.undoErrorCount ?? 0),
            GlassToastMessage(text: "Couldn't delete the bill. Try again.", trigger: model?.deleteErrorCount ?? 0),
        ])
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
            Button("Edit") {
                model?.send(BillDetailUiEventOnEditClicked.shared)
            }
            .disabled(model?.state.areActionsEnabled != true)
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

/// "Bruno paid" next to Bruno's face, or "A former member paid".
private struct PayerLine: View {
    let payers: [BillPayer]
    let size: CGFloat

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
    }
}

/// "Queued · syncs when you're back online", with the orange dot of what is still queued.
private struct PendingLine: View {
    var body: some View {
        HStack(spacing: 8) {
            Circle()
                .fill(.orange)
                .frame(width: 8, height: 8)
                .accessibilityHidden(true)
            Text("Queued · syncs when you're back online")
        }
    }
}

/// "−R$ 118,00 you owe" in the negative color, "+R$ 249,60 to get back" in the accent, and who settles it.
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

/// One person of the split: their face, "Paid R$ 642,00" or "Hasn't joined yet", and their part.
private struct ShareRow: View {
    let row: BillShareRow
    let currency: GroupCurrency
    let avatarSize: CGFloat

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
/// Upright and on a narrow iPad sheet: one column, the bill, its split, Comments and History, with the composer
/// floating in glass at the bottom, above the keyboard. Lying down and on a wide iPad sheet: the bill on the left; how
/// it splits, Comments and History on the right with the composer under them.
private struct PhoneBillDetail: View {
    let model: BillDetailModel

    @State private var layout = ScreenLayout()
    @FocusState private var isComposing: Bool

    private var state: BillDetailUiState {
        model.state
    }

    var body: some View {
        Group {
            if layout.usesTwoColumns {
                TwoColumnsLayout(spacing: 0) {
                    list { billSections }
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
                    PayerLine(payers: state.payers, size: 24)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    if state.isPending {
                        PendingLine()
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
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

    private var splitSection: some View {
        Section {
            ForEach(state.people, id: \.memberId) { row in
                ShareRow(row: row, currency: state.currency, avatarSize: 32)
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
            ForEach(state.threads, id: \.comment.id) { thread in
                // The section's last row draws no line under the card
                let isLastThread = thread.comment.id == state.threads.last?.comment.id
                commentRow(thread.comment, isReply: false, endsThread: thread.replies.isEmpty && !isLastThread)
                ForEach(Array(thread.replies.enumerated()), id: \.element.id) { index, reply in
                    commentRow(reply, isReply: true, endsThread: index == thread.replies.count - 1 && !isLastThread)
                }
            }
        } header: {
            Text(verbatim: BillCopy.commentsHeader(state.commentCount))
        }
        if !state.history.isEmpty {
            Section {
                ForEach(state.history, id: \.dayStartMillis) { day in
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
        let isTarget = state.replyTarget?.commentId == comment.id
        return BillCommentView(
            comment: comment,
            isReply: isReply,
            metrics: .standard,
        ) {
            model.send(BillDetailUiEventOnReplyClicked(commentId: comment.id))
            isComposing = true
        }
        .padding(.vertical, 2)
        .listRowSeparator(endsThread ? .visible : .hidden, edges: .bottom)
        // The comment being answered, tinted while "Replying to" shows
        .listRowBackground(ZStack {
            Palette.card
            if isTarget { Palette.accent.opacity(0.12) }
        })
    }
}
#endif

#if os(macOS)
/// What the narrow window's sheet shows, one at a time.
private enum MacBillTab: Hashable {
    case bill, comments, history
}

/// The sheet on the window: the bill's header with Edit… and Delete, then, 720 pt wide, the bill and its split on the
/// left and Comments and History on the right with the composer under them. A window too narrow for that gets a
/// 330 pt sheet showing one of Bill, Comments and History at a time, the composer under Comments. Either fits the
/// window's 560 pt minimum height.
private struct MacBillDetail: View {
    let model: BillDetailModel

    @Environment(\.windowLayout) private var windowLayout
    @State private var tab = MacBillTab.bill
    @FocusState private var isComposing: Bool

    private var state: BillDetailUiState {
        model.state
    }

    /// The 720 pt sheet and the window's edge around it.
    private var isNarrow: Bool {
        windowLayout.size.width > 0 && windowLayout.size.width < 760
    }

    private var height: CGFloat {
        guard windowLayout.size.height > 0 else { return 520 }
        return min(640, max(400, windowLayout.size.height - 40))
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
                    // History hides while there is none, as everywhere else
                    if !state.history.isEmpty {
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
            Divider()
            HStack {
                Spacer()
                Button("Close") {
                    model.send(BillDetailUiEventOnBackClicked.shared)
                }
                .keyboardShortcut(.cancelAction)
            }
            .controlSize(.large)
            .padding(16)
        }
        .frame(width: isNarrow ? 330 : 720, height: height)
    }

    @ViewBuilder
    private var narrowContent: some View {
        switch tab == .history && state.history.isEmpty ? MacBillTab.bill : tab {
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

    private var header: some View {
        HStack(alignment: .top, spacing: 12) {
            CategoryBadge(category: state.category, size: isNarrow ? 32 : 40)
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
            Spacer(minLength: 8)
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
            .disabled(!state.areActionsEnabled)
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
        }
    }

    private var billContent: some View {
        VStack(alignment: .leading, spacing: 20) {
            VStack(alignment: .leading, spacing: 4) {
                Text(verbatim: state.currency.format(state.totalMinor))
                    .font(.system(size: isNarrow ? 28 : 34, weight: .bold))
                    .monospacedDigit()
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                PayerLine(payers: state.payers, size: 20)
                    .foregroundStyle(.secondary)
                if state.isPending {
                    PendingLine()
                        .foregroundStyle(.secondary)
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
                Text(verbatim: BillCopy.splitHeader(state.splitMode, count: state.participantCount))
                    .font(.headline)
                VStack(spacing: 0) {
                    ForEach(Array(state.people.enumerated()), id: \.element.memberId) { index, row in
                        if index > 0 { Divider() }
                        ShareRow(row: row, currency: state.currency, avatarSize: 24)
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
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    if showsComments { comments }
                    if showsHistory { history }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(isNarrow ? 16 : 20)
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
            ForEach(state.threads, id: \.comment.id) { thread in
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
        let isTarget = state.replyTarget?.commentId == comment.id
        return BillCommentView(comment: comment, isReply: isReply, metrics: .standard) {
            model.send(BillDetailUiEventOnReplyClicked(commentId: comment.id))
            isComposing = true
        }
        .padding(6)
        // The comment being answered, tinted while "Replying to" shows
        .background(isTarget ? Palette.accent.opacity(0.14) : .clear, in: .rect(cornerRadius: 8))
    }

    @ViewBuilder
    private var history: some View {
        if !state.history.isEmpty {
            VStack(alignment: .leading, spacing: 10) {
                if !isNarrow {
                    Text("History", comment: "The bill's history section")
                        .font(.headline)
                }
                ForEach(state.history, id: \.dayStartMillis) { day in
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
#endif
