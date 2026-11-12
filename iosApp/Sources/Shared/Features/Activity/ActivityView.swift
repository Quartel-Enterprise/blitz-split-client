import Observation
import Shared
import SwiftUI

@Observable
final class ActivityModel {
    private(set) var state: ActivityUiState

    @ObservationIgnored private let holder: ViewModelHolder<ActivityViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteActivity) {
        holder = BlitzSplitApple.shared.createActivityViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! ActivityUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
    }

    /// The state as the ViewModel holds it right now, before the watcher brings it to `state`.
    var current: ActivityUiState {
        (holder.viewModel.uiState.value as? ActivityUiState) ?? state
    }

    func send(_ event: any ActivityUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    /// Pulled down: asks for a sync and returns once the shared code says it ended (at once offline).
    func refresh() async {
        send(ActivityUiEventOnRefresh.shared)
        while current.isRefreshing, !Task.isCancelled {
            try? await Task.sleep(for: .milliseconds(200))
        }
    }

    /// Filter open: closing it any other way than its buttons (a swipe down, Esc) keeps the filters as they were.
    var isFilterOpen: Binding<Bool> {
        Binding(
            get: { self.state.isFilterOpen },
            set: { isOpen in
                if !isOpen, self.current.isFilterOpen { self.send(ActivityUiEventOnFilterDismissed.shared) }
            },
        )
    }

    /// The alert of an item whose bill or group is gone; OK lets it go.
    var isDeletedNoticeShown: Binding<Bool> {
        Binding(
            get: { self.state.deletedNotice != nil },
            set: { isShown in
                if !isShown, self.current.deletedNotice != nil {
                    self.send(ActivityUiEventOnDeletedNoticeShown.shared)
                }
            },
        )
    }

    isolated deinit {
        holder.clear()
    }
}

/// Activity, which Home's bell pushes over it: what happened in every group, by day, the most recent on top, with the
/// changes this device hasn't sent yet above Today. On an iPhone, an iPad upright and a narrow Mac window, one column, where an edited
/// bill shows its changes inside its row. On an iPhone lying down the unread count and the filters move to a column
/// beside the feed. On an iPad lying down, an iPhone Duo open (the feed and the item on each side of its fold) and a
/// wide Mac window (the inspector) the item tapped shows beside the list, with what changed and the other events of
/// its bill.
struct ActivityView: View {
    let route: RouteActivity

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: ActivityModel?
    /// The whole screen, the column beside the list included, so showing it never changes what decides it.
    @State private var layout = ScreenLayout()
    #if os(macOS)
    @Environment(\.frontRoute) private var frontRoute
    @Environment(\.isWindowUncovered) private var isWindowUncovered
    #endif

    /// The item beside the list: lying down with an iPad's room (an iPad, an iPhone Duo open), a Mac window wide
    /// enough for the inspector.
    private var showsDetail: Bool {
        #if os(macOS)
        layout.size.width >= ActivityDetailPane.minimumScreenWidth
        #else
        layout.showsListDetail
        #endif
    }

    /// An iPhone lying down: the unread count and the filters in a column beside the feed.
    private var showsSideColumn: Bool {
        #if os(macOS)
        false
        #else
        !showsDetail && layout.usesTwoColumns
        #endif
    }

    /// What the screen tells the ViewModel, nil before the first layout, whose zero size would say there is no column.
    private var reportedDetail: Bool? {
        guard model != nil, layout.size != .zero else { return nil }
        return showsDetail
    }

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                content(model)
            } else {
                // The feed not read yet: its rows shimmer, never "Nothing here yet" before it
                ShimmerScreen {
                    ShimmerLine(width: 80, height: 10)
                    ShimmerList(count: 6, showsTrailing: false)
                }
                .background(Palette.groupedBackground.ignoresSafeArea())
            }
        }
        .measuringScreenLayout($layout)
        // Beside the list a tap picks the item instead of opening it
        .onChange(of: reportedDetail, initial: true) { _, isShown in
            guard let isShown else { return }
            model?.send(ActivityUiEventOnDetailPaneChanged(isShown: isShown))
        }
        .navigationTitle("Activity")
        // The same Notifications as the profile's: pushed over Activity on iPhone and iPad, Ajustes' section on the
        // Mac (`AppModel`), there whether or not the feed has anything to filter
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Notification settings", systemImage: "bell.badge") {
                    model?.send(ActivityUiEventOnNotificationSettingsClicked.shared)
                }
                .help(Text("Notification settings"))
            }
        }
        #if os(iOS)
        // Why: pushed over Home by its bell, the bar names it beside the back button.
        .toolbarTitleDisplayMode(.inline)
        .phoneMinimizesNavigationBar()
        .sheet(isPresented: model?.isFilterOpen ?? .constant(false)) {
            if let model {
                ActivityFilterSheet(model: model)
                    .foldSheetPlacement()
            }
        }
        #else
        .navigationSubtitle(model.map { ActivityMacCopy.subtitle($0.state) } ?? "")
        .toolbar {
            if let model, model.state.showsTools {
                ActivityMacToolbar(model: model, showsTitles: showsDetail)
            }
        }
        // Edit › Mark All as Read and View › Refresh, only while Activity is in front, never from under what it
        // opened nor under a sheet
        .focusedSceneValue(\.activityModel, isWindowUncovered && frontRoute == .activity(route) ? model : nil)
        #endif
        .alert(
            model?.state.deletedNotice.map { Text(ActivityCopy.deletedTitle($0)) } ?? Text(verbatim: ""),
            isPresented: model?.isDeletedNoticeShown ?? .constant(false),
            presenting: model?.state.deletedNotice,
        ) { _ in
            Button("OK") {}
        } message: { deletion in
            Text(verbatim: ActivityCopy.deleted(deletion))
        }
        .onAppear {
            if model == nil { model = ActivityModel(route: route) }
        }
    }

    @ViewBuilder
    private func content(_ model: ActivityModel) -> some View {
        if showsDetail {
            ActivityColumns {
                ActivityFeed(model: model, showsDetail: true, showsContext: true)
            } detail: {
                ActivityDetailPane(model: model)
            }
        } else if showsSideColumn {
            HStack(alignment: .top, spacing: 28) {
                ScrollView {
                    ActivityContext(model: model)
                        .padding(.top, 12)
                }
                .frame(width: 260)
                ActivityFeed(model: model, showsDetail: false, showsContext: false)
            }
            .background(Palette.groupedBackground)
        } else {
            ActivityFeed(model: model, showsDetail: false, showsContext: true)
                // An iPad upright keeps the canvas' 520 pt column, the scroll still the whole width. Narrower, nil
                // keeps the inset list's own margins, which 0 would take away
                .contentMargins(.horizontal, centeringMargin, for: .scrollContent)
        }
    }

    private var centeringMargin: CGFloat? {
        let margin = (layout.size.width - ScreenLayout.singleColumnMaxWidth) / 2
        return margin > 0 ? margin : nil
    }
}

/// The list and the item beside it. On an iPhone Duo (iOS 27.1) whose fold runs down the screen, the feed takes the
/// half before the fold and the item the other, the gap covering the fold (the boards' 412 | 48 | 360); elsewhere the
/// list keeps its column (400 pt on iPad, 360 on the Mac) and the item takes the rest.
private struct ActivityColumns<ListContent: View, Detail: View>: View {
    @ViewBuilder let list: ListContent
    @ViewBuilder let detail: Detail

    @State private var division: FoldDivision?

    #if os(macOS)
    private static var listWidth: CGFloat { 360 }
    #else
    private static var listWidth: CGFloat { ScreenLayout.listColumnWidth }
    #endif

    private var verticalFold: CGRect? {
        division?.verticalFrame
    }

    var body: some View {
        let gap = max(verticalFold?.width ?? 0, 48)
        HStack(spacing: 0) {
            list
                .frame(width: verticalFold.map { max(0, $0.midX - gap / 2) } ?? Self.listWidth)
            if verticalFold != nil {
                Color.clear.frame(width: gap)
            } else {
                Divider()
                    .ignoresSafeArea()
            }
            detail
                .frame(maxWidth: .infinity)
        }
        .readingFoldDivision($division)
    }
}

/// What sits above the feed: the sync status while it asks for attention, the note of a device that never synced,
/// the filters and the unread count with "Mark all as read". The Mac keeps the last two in its toolbar and lists
/// the filters in use instead.
struct ActivityContext: View {
    let model: ActivityModel

    private var state: ActivityUiState {
        model.state
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            if let status = state.status {
                HStack(spacing: 6) {
                    Image(systemName: ActivityCopy.statusIcon(status))
                        .accessibilityHidden(true)
                    Text(verbatim: ActivityCopy.status(status, queuedCount: state.queuedCount))
                    if status == .serverError {
                        Button("Try again") {
                            model.send(ActivityUiEventOnRefresh.shared)
                        }
                        .buttonStyle(.borderless)
                        .disabled(state.isRefreshing)
                    }
                }
                .font(.footnote)
                .foregroundStyle(.secondary)
            }
            if state.showsNotSyncedNotice {
                Label {
                    Text("This \(DeviceName.current) hasn't synced yet, so only what you did on it shows here. Group activity arrives on the first connection, with or without an account.")
                        .fixedSize(horizontal: false, vertical: true)
                } icon: {
                    Image(systemName: "info.circle")
                }
                .font(.footnote)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Palette.card, in: .rect(cornerRadius: 18))
            }
            #if os(iOS)
            if state.showsTools {
                ActivityFilterChips(model: model)
                if state.canMarkAllRead {
                    HStack {
                        Text(verbatim: ActivityCopy.unread(state.unreadCount))
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                        Spacer(minLength: 8)
                        Button("Mark all as read") {
                            model.send(ActivityUiEventOnMarkAllReadClicked.shared)
                        }
                        .buttonStyle(.borderless)
                    }
                }
            }
            #else
            if state.filter.isActive {
                ActivityFilterBar(model: model)
            }
            #endif
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// The feed: one section per day, "Today", "Yesterday" or the date, then "Loading more…" or, offline, why the older
/// ones wait. Before the first download a new device shows placeholders; with nothing to show, the empty state.
/// Pulling down syncs.
private struct ActivityFeed: View {
    let model: ActivityModel
    /// The item tapped shows beside the list: a row picks it, its detail keeps one line and its changes stay there.
    let showsDetail: Bool
    /// The status, the filters and the unread count above the days (else in their own column).
    let showsContext: Bool

    private var state: ActivityUiState {
        model.state
    }

    var body: some View {
        list
            #if os(iOS)
            .listStyle(.insetGrouped)
            .refreshable { await model.refresh() }
            #else
            .listStyle(.inset)
            .alternatingRowBackgrounds()
            // A double click or Return opens what the item is about
            .contextMenu(forSelectionType: String.self) { _ in
                EmptyView()
            } primaryAction: { ids in
                if let id = ids.first { model.send(ActivityUiEventOnOpenTargetClicked(itemId: id)) }
            }
            #endif
            .scrollContentBackground(.hidden)
            .background(Palette.groupedBackground)
    }

    @ViewBuilder
    private var list: some View {
        #if os(macOS)
        List(selection: selection) {
            sections
        }
        #else
        List {
            sections
        }
        #endif
    }

    /// The Mac's selection is the item beside the list; picking one tells the ViewModel, which opens it with no
    /// inspector.
    private var selection: Binding<String?> {
        Binding(
            get: { showsDetail ? state.selectedItem?.id : nil },
            set: { id in
                if let id { model.send(ActivityUiEventOnItemClicked(itemId: id)) }
            },
        )
    }

    @ViewBuilder
    private var sections: some View {
        if showsContext, state.status != nil || state.showsNotSyncedNotice || contextHasTools {
            Section {
                ActivityContext(model: model)
                    .listRowInsets(EdgeInsets(top: 4, leading: 4, bottom: 4, trailing: 4))
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
            }
        }
        // A new device's first sync, or older pages a filter waits for: the rows shimmer, never "Nothing here yet"
        if state.showsPlaceholders || state.isLoadingOlderItems {
            Section {
                ActivityPlaceholders()
            } footer: {
                if state.showsPlaceholders, state.isWaitingForInternet {
                    WaitingForInternetNote()
                }
            }
        }
        // Beside the list, the empty state takes the detail's column instead
        if let emptyState = state.emptyState, !showsDetail {
            Section {
                ActivityEmptyView(model: model, emptyState: emptyState)
            }
            .listRowBackground(Color.clear)
        }
        let lastId = state.days.last?.items.last?.id
        ForEach(state.days, id: \.dayStartMillis) { day in
            Section {
                ForEach(day.items, id: \.id) { item in
                    row(item)
                        .onAppear {
                            if item.id == lastId, state.hasMore { model.send(ActivityUiEventOnEndReached.shared) }
                        }
                }
            } header: {
                Text(verbatim: ActivityCopy.day(day))
            }
        }
        if state.isLoadingMore || state.showsOfflineEnd {
            Section {
                if state.isLoadingMore {
                    HStack(spacing: 8) {
                        ProgressView()
                            .controlSize(.small)
                        Text("Loading more…")
                    }
                    .frame(maxWidth: .infinity)
                }
                if state.showsOfflineEnd {
                    Text("The last 90 days stay on this \(DeviceName.current). Older ones load when the internet is back.")
                        .frame(maxWidth: .infinity)
                        .multilineTextAlignment(.center)
                }
            }
            .font(.footnote)
            .foregroundStyle(.secondary)
            .listRowBackground(Color.clear)
        }
    }

    /// The filters and the unread count live above the days on iPhone and iPad; the Mac lists the filters in use.
    private var contextHasTools: Bool {
        #if os(macOS)
        state.filter.isActive
        #else
        state.showsTools
        #endif
    }

    @ViewBuilder
    private func row(_ item: ActivityItem) -> some View {
        let isSelected = showsDetail && item.id == state.selectedItem?.id
        #if os(macOS)
        ActivityRow(model: model, item: item, showsDetail: showsDetail)
            .tag(item.id)
        #else
        ActivityRow(model: model, item: item, showsDetail: showsDetail)
            .listRowInsets(EdgeInsets(top: 12, leading: 5, bottom: 12, trailing: 16))
            .accessibilityAddTraits(isSelected ? .isSelected : [])
            .selectedRowBackground(isSelected)
        #endif
    }
}

/// One item: the face of who did it with what happened on its corner, the blue dot while unread, the headline, the
/// line under it and the group with the time. An edited bill shows "See changes" in the row when nothing is beside
/// the list.
private struct ActivityRow: View {
    let model: ActivityModel
    let item: ActivityItem
    let showsDetail: Bool

    #if os(macOS)
    private static let faceSize: CGFloat = 28
    #else
    private static let faceSize: CGFloat = 40
    #endif

    private var isExpanded: Bool {
        model.state.expandedItemId == item.id
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            #if os(macOS)
            summary
                .overlay(alignment: .topLeading) { actorButton }
                .accessibilityActions { actorAction }
            #else
            Button {
                model.send(ActivityUiEventOnItemClicked(itemId: item.id))
            } label: {
                summary
            }
            .buttonStyle(.plain)
            // Over the row's button rather than inside its label, so the face's tap never reaches the row
            .overlay(alignment: .topLeading) { actorButton }
            // On the button itself, not its label, so VoiceOver's Actions keep it
            .accessibilityActions { actorAction }
            #endif
            if item.hasChangesToShow, !showsDetail {
                ActivityRowChanges(model: model, item: item, isExpanded: isExpanded)
                    .padding(.leading, 11 + Self.faceSize + 12)
            }
        }
    }

    private var summary: some View {
        HStack(alignment: .top, spacing: 0) {
            Circle()
                .fill(item.isUnread ? Palette.accent : .clear)
                .frame(width: 8, height: 8)
                .padding(.top, Self.faceSize / 2 - 4)
                .accessibilityHidden(true)
            HStack(alignment: .top, spacing: 12) {
                ActivityFace(item: item, size: Self.faceSize)
                VStack(alignment: .leading, spacing: 3) {
                    ActivityCopy.headline(item)
                        .font(rowFont)
                        .fixedSize(horizontal: false, vertical: true)
                    if let detail = ActivityCopy.detail(item) {
                        Text(verbatim: detail)
                            .font(detailFont)
                            .foregroundStyle(.secondary)
                            // A comment's text keeps its two lines beside the detail too
                            .lineLimit(showsDetail && item.kind != .commentAdded ? 1 : 2)
                    }
                    ActivityRowMeta(item: item)
                }
                Spacer(minLength: 4)
                #if os(iOS)
                if !showsDetail {
                    Image(systemName: "chevron.right")
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(.tertiary)
                        .padding(.top, Self.faceSize / 2 - 8)
                        .accessibilityHidden(true)
                }
                #endif
            }
            .padding(.leading, 3)
        }
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
        .accessibilityValue(item.isUnread ? Text("Unread") : Text(verbatim: ""))
    }

    /// VoiceOver reaches the face's tap as an action of the row.
    @ViewBuilder
    private var actorAction: some View {
        if let openActor {
            Button(ActivityCopy.openActorLabel(item), action: openActor)
        }
    }

    /// A clear button over the face, which opens whoever did it apart from the row; VoiceOver has it as the row's
    /// action instead.
    @ViewBuilder
    private var actorButton: some View {
        if let openActor {
            Button(action: openActor) {
                Color.clear
                    .frame(width: Self.faceSize, height: Self.faceSize)
                    .contentShape(.circle)
            }
            .buttonStyle(.plain)
            // The unread dot and the face's inset come before it
            .padding(.leading, 11)
            .accessibilityHidden(true)
        }
    }

    /// The face opens whoever did it: their profile, or Profile for this device's user (Pierre's call, item 18).
    private var openActor: (() -> Void)? {
        guard item.actor.opensProfile else { return nil }
        return { model.send(ActivityUiEventOnActorClicked(itemId: item.id)) }
    }

    private var rowFont: Font {
        #if os(macOS)
        .body
        #else
        .subheadline
        #endif
    }

    private var detailFont: Font {
        #if os(macOS)
        .callout
        #else
        .subheadline
        #endif
    }
}

/// "Viagem Floripa · 8:15 PM", with the group's tint, or the amber dot and "Waiting to send" for a change not up yet.
private struct ActivityRowMeta: View {
    let item: ActivityItem

    var body: some View {
        HStack(spacing: 5) {
            GroupAvatar.shape(size: 10)
                .fill(item.groupColor?.data ?? Color.secondary.opacity(0.2))
                .frame(width: 10, height: 10)
                .accessibilityHidden(true)
            if let groupName = item.groupName {
                Text(verbatim: groupName)
                    .lineLimit(1)
                Text(verbatim: "·")
                    .accessibilityHidden(true)
            }
            if item.isPending {
                PendingDot()
            }
            Text(verbatim: ActivityCopy.time(item))
                .lineLimit(1)
        }
        .font(.caption)
        .foregroundStyle(.secondary)
    }
}

/// "See changes" opens, in the row, each field the edit changed before and after, with "Open bill".
private struct ActivityRowChanges: View {
    let model: ActivityModel
    let item: ActivityItem
    let isExpanded: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if isExpanded {
                VStack(alignment: .leading, spacing: 6) {
                    // Simplify debts lists what changed for this device's user, never the item's field changes (13.7b)
                    ForEach(Array(ActivityBeforeAfter.of(item).enumerated()), id: \.offset) { _, change in
                        ActivityChangeLine(change: change)
                    }
                    Button(ActivityCopy.openLabel(item)) {
                        model.send(ActivityUiEventOnOpenTargetClicked(itemId: item.id))
                    }
                    .buttonStyle(.borderless)
                    .font(.subheadline.weight(.semibold))
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Palette.groupedBackground, in: .rect(cornerRadius: 14))
                .accessibilityElement(children: .contain)
                .accessibilityLabel(Text("Changes in \(ActivityCopy.object(item))"))
            }
            Button {
                withAnimation { model.send(ActivityUiEventOnChangesToggled(itemId: item.id)) }
            } label: {
                HStack(spacing: 4) {
                    isExpanded ? Text("Hide changes") : Text("See changes")
                    Image(systemName: "chevron.down")
                        .font(.caption.weight(.bold))
                        .rotationEffect(.degrees(isExpanded ? 180 : 0))
                }
            }
            .buttonStyle(.borderless)
            .font(.subheadline)
            .accessibilityValue(isExpanded ? Text("Expanded") : Text("Collapsed"))
        }
    }
}

/// One line of what changed: what it is about (a field, or a person for Simplify debts), what it was and what it is.
struct ActivityBeforeAfter {
    let label: String
    let before: String
    let after: String

    /// "Amount  R$ 612,00 → R$ 642,00", a field an edit changed.
    init(change: ActivityChangeRow) {
        label = ActivityCopy.field(change.field)
        before = ActivityCopy.value(change.before, field: change.field)
        after = ActivityCopy.value(change.after, field: change.field)
    }

    /// "Diego  didn't owe you → owes you $64", what Simplify debts changed with someone (13.7b).
    init(change: ActivityDebtChange) {
        label = PaymentCopy.name(change.name)
        before = ActivityCopy.debtSide(change.beforeMinor, isBefore: true, currency: change.currency)
        after = ActivityCopy.debtSide(change.afterMinor, isBefore: false, currency: change.currency)
    }

    /// What an item lists under "See changes": what changed for this device's user for Simplify debts, never its
    /// field changes, and the fields an edit changed otherwise.
    static func of(_ item: ActivityItem) -> [ActivityBeforeAfter] {
        if let simplification = item.simplification {
            return simplification.myChanges.map { ActivityBeforeAfter(change: $0) }
        }
        return item.changes.map { ActivityBeforeAfter(change: $0) }
    }
}

/// "Amount  R$ 612,00 → R$ 642,00": what changed, what it was, struck through, and what it is now.
struct ActivityChangeLine: View {
    let change: ActivityBeforeAfter

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Text(verbatim: change.label)
                .foregroundStyle(.secondary)
                .frame(minWidth: 80, alignment: .leading)
            Text(verbatim: change.before)
                .strikethrough()
                .foregroundStyle(.secondary)
            Text(verbatim: "→")
                .foregroundStyle(.secondary)
                .accessibilityLabel(Text("to"))
            Text(verbatim: change.after)
                .fontWeight(.semibold)
        }
        .font(.subheadline)
        .accessibilityElement(children: .combine)
    }
}

/// Who did it, with what happened on its corner: a bill, a payment, a person, a trash can once it is gone.
struct ActivityFace: View {
    let item: ActivityItem
    let size: CGFloat

    var body: some View {
        let colors = ActivityCopy.iconColors(item)
        let badge = (size * 0.5).rounded()
        AvatarPhoto(initial: item.actor.initials, color: item.actor.color, photoUrl: item.actor.photoUrl, size: size)
            .overlay(alignment: .bottomTrailing) {
                Image(systemName: ActivityCopy.icon(item))
                    .font(.system(size: badge * 0.5, weight: .bold))
                    .foregroundStyle(colors.content)
                    .frame(width: badge, height: badge)
                    .background(colors.fill, in: .circle)
                    .overlay(Circle().stroke(Palette.card, lineWidth: 2))
                    .offset(x: 5, y: 5)
            }
            .padding(.trailing, 4)
            .accessibilityHidden(true)
    }
}

/// A new device waiting for the first page (with "Syncing…" in the status above), or the older pages a filter waits
/// for: the rows' shapes, shimmering.
private struct ActivityPlaceholders: View {
    var count = 5

    var body: some View {
        ShimmerList(count: count, showsTrailing: false)
            .shimmering(label: Text("Loading activity"))
    }
}

/// "Nothing here yet" leads to Groups; "Nothing with these filters" clears them.
struct ActivityEmptyView: View {
    let model: ActivityModel
    let emptyState: ActivityEmptyState

    private var isNoResults: Bool {
        emptyState == .noResults
    }

    private var title: LocalizedStringKey {
        isNoResults ? "Nothing with these filters" : "Nothing here yet"
    }

    private var message: LocalizedStringKey {
        isNoResults
            ? "Try another type or another group."
            : "When someone splits a bill, pays, comments or joins one of your groups, it shows up here."
    }

    private var action: LocalizedStringKey {
        isNoResults ? "Clear filters" : "See groups"
    }

    var body: some View {
        ContentUnavailableView {
            Label(title, systemImage: isNoResults ? "line.3.horizontal.decrease.circle" : "clock")
        } description: {
            Text(message)
        } actions: {
            Button(action) {
                if isNoResults {
                    model.send(ActivityUiEventOnClearFiltersClicked.shared)
                } else {
                    model.send(ActivityUiEventOnEmptyActionClicked.shared)
                }
            }
            .buttonStyle(.bordered)
            .buttonBorderShape(.capsule)
        }
    }
}
