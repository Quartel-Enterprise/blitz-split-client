import Observation
import Shared
import SwiftUI

@Observable
final class HomeModel {
    private(set) var state: HomeUiState
    private(set) var comingSoonCount = 0
    private(set) var changeErrorCount = 0
    let deletedGroup = DeletedGroupToast()

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createHomeViewModel()
    @ObservationIgnored private let watches = WatchBag()
    /// The pulls waiting for the sync they asked for, resumed by the state that says it ended.
    @ObservationIgnored private let refreshes = PendingRefreshes()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! HomeUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
            if !state.isRefreshing { self?.refreshes.endAll() }
            self?.deletedGroup.show(state.deletedGroupNotice) {
                self?.send(HomeUiEventOnDeletedGroupNoticeShown.shared)
            }
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            switch action {
            case is HomeUiActionShowComingSoon: self?.comingSoonCount += 1
            case is HomeUiActionShowChangeError: self?.changeErrorCount += 1
            default: break
            }
        })
    }

    func send(_ event: any HomeUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    /// Pulled down: asks for a sync and returns once the shared code says it ended (at once offline).
    func refresh() async {
        send(HomeUiEventOnRefresh.shared)
        // The event marks the sync as started before it returns, and starts none offline
        await refreshes.wait(isRefreshing: (holder.viewModel.uiState.value as! HomeUiState).isRefreshing)
    }

    isolated deinit {
        refreshes.endAll()
        holder.clear()
    }
}

/// Home (flow 10), the first tab and where the app opens: the balances with people, the overall balance, the
/// shortcuts, your groups and the latest bills. Every layout is the window's size, never the device: one column on an
/// iPhone (an iPhone Duo closed too), a centred column on an iPad upright, two columns lying down (an iPhone, an iPad,
/// an iPhone Duo open, each column on its side of the fold), the four quarters of an iPhone Duo upright, whose fold
/// runs across; on the Mac, two rows of two columns in a wide window and one column in a narrow one.
struct HomeView: View {
    let model: HomeModel

    @State private var layout = ScreenLayout()
    @State private var division: FoldDivision?

    #if os(iOS)
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.verticalSizeClass) private var verticalSizeClass
    #endif

    private var state: HomeUiState {
        model.state
    }

    var body: some View {
        Group {
            if state.showsFirstUse {
                ScrollView {
                    VStack(spacing: 16) {
                        syncStatus
                        HomeFirstUseCard(send: model.send)
                    }
                    .padding(16)
                    .padding(.top, 40)
                    .frame(maxWidth: .infinity)
                }
            } else if state.showsHome {
                content
            } else {
                // Nothing until Room is read, so the first use's card never flashes before the groups
                Color.clear
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.groupedBackground)
        .measuringScreenLayout($layout)
        .readingFoldDivision($division)
        // Record payment, which "Who paid whom?" or a face's settling up opened over Home, just closed
        .recordedPaymentNotice(
            state.recordedPayment,
            onUndo: { model.send(HomeUiEventOnUndoPaymentClicked.shared) },
            onShown: { model.send(HomeUiEventOnRecordedPaymentNoticeShown.shared) },
        )
        .navigationTitle(HomeCopy.tabTitle)
        #if os(iOS)
        .toolbarTitleDisplayMode(.inline)
        .toolbar {
            HiddenTabTitle(title: HomeCopy.tabTitle)
            ToolbarItem(placement: .primaryAction) { profileButton }
            if state.showsSplitBillButton, !usesBottomButton {
                ToolbarSpacer(.fixed, placement: .primaryAction)
                ToolbarItem(placement: .primaryAction) { splitBillButton }
            }
        }
        // The upright iPhone: "Split a bill" floats over the tab bar, under the thumb, as the canvas draws it
        .safeAreaInset(edge: .bottom) {
            if state.showsSplitBillButton, usesBottomButton {
                HStack {
                    Spacer()
                    splitBillButton
                        .controlSize(.large)
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
            }
        }
        #else
        .navigationSubtitle(Text(verbatim: syncText ?? ""))
        .toolbar { macToolbar }
        #endif
    }

    // MARK: - Layouts

    @ViewBuilder
    private var content: some View {
        #if os(iOS)
        Group {
            if let fold = acrossFold {
                FoldQuarters(fold: fold, spacing: 24) {
                    VStack(spacing: 14) {
                        syncStatus
                        people
                        summary(stacksButtons: true)
                    }
                } topTrailing: {
                    groups(cardWidth: 170)
                } bottomLeading: {
                    VStack(spacing: 10) {
                        shortcuts
                        recoveryCard
                    }
                } bottomTrailing: {
                    bills
                }
            } else {
                ScrollView {
                    columns
                        .padding(.horizontal, 16)
                        .padding(.top, 4)
                        .padding(.bottom, 24)
                }
            }
        }
        .refreshable { await model.refresh() }
        #else
        ScrollView {
            if layout.size.width >= Self.macWideWidth {
                macWide
            } else {
                macNarrow
            }
        }
        #endif
    }

    #if os(iOS)
    /// One column on an iPhone (620 pt at most with an iPad's room), the two of the canvas lying down: the balances on
    /// the left, the groups and the bills on the right.
    @ViewBuilder
    private var columns: some View {
        if layout.usesTwoColumns {
            TwoColumnsLayout(firstWidth: layout.isLarge ? 400 : 300, spacing: layout.isLarge ? 24 : 28) {
                VStack(spacing: layout.isLarge ? 14 : 8) {
                    firstColumn(stacksButtons: !layout.isLarge)
                }
            } second: {
                VStack(spacing: layout.isLarge ? 14 : 8) {
                    secondColumn
                }
            }
        } else {
            VStack(spacing: 14) {
                firstColumn(stacksButtons: false)
                secondColumn
            }
            .frame(maxWidth: layout.isLarge ? 620 : .infinity)
            .frame(maxWidth: .infinity)
        }
    }

    /// An iPhone Duo upright (648×920): its fold runs across the screen, so the screen splits into its four quarters
    /// instead of one long column.
    private var acrossFold: CGRect? {
        guard let division, !division.isVertical, layout.isLarge, layout.size.height > layout.size.width else {
            return nil
        }
        return division.frame
    }

    /// The upright iPhone keeps "Split a bill" under the thumb; lying down and with an iPad's room it is the bar's
    /// tinted glass item.
    private var usesBottomButton: Bool {
        horizontalSizeClass == .compact && verticalSizeClass == .regular
    }
    #endif

    @ViewBuilder
    private func firstColumn(stacksButtons: Bool) -> some View {
        syncStatus
        people
        summary(stacksButtons: stacksButtons)
        shortcuts
        recoveryCard
    }

    @ViewBuilder
    private var secondColumn: some View {
        groups(cardWidth: layout.isLarge ? 200 : 170)
        bills
    }

    #if os(macOS)
    /// From this width the Mac's window holds the canvas' two rows of two columns.
    private static let macWideWidth: CGFloat = 860

    /// The overall balance beside the people, then the latest bills beside your groups.
    private var macWide: some View {
        VStack(alignment: .leading, spacing: 12) {
            recoveryCard
            HStack(alignment: .top, spacing: 12) {
                summary(stacksButtons: true)
                    .frame(width: 300)
                people
            }
            HStack(alignment: .top, spacing: 12) {
                bills
                if !state.groups.isEmpty {
                    HomeGroupsSection(groups: state.groups, style: .rows, send: model.send)
                        .frame(width: 300)
                }
            }
        }
        .padding(20)
    }

    /// One column: the overall balance, the people, then your groups as a grid over the bills.
    private var macNarrow: some View {
        VStack(alignment: .leading, spacing: 12) {
            recoveryCard
            summary(stacksButtons: false)
            people
            if !state.groups.isEmpty {
                HomeGroupsSection(groups: state.groups, style: .grid, send: model.send)
            }
            bills
        }
        .padding(20)
    }
    #endif

    // MARK: - Sections

    /// "Offline · 1 change queued", "No response from the server" with "Try again", or "Syncing…" with a bar while
    /// the first download fills the device: only when it asks for attention. The Mac says it as the window's subtitle.
    @ViewBuilder
    private var syncStatus: some View {
        #if os(iOS)
        if let status = syncText {
            VStack(alignment: .leading, spacing: 8) {
                SyncStatusRow(status: status, badge: state.syncBadge, canRetry: state.canRetrySync) {
                    model.send(HomeUiEventOnRetrySyncClicked.shared)
                }
                if state.isFirstDownload {
                    ProgressView()
                        .progressViewStyle(.linear)
                }
            }
            .padding(.horizontal, 4)
        }
        #endif
    }

    private var syncText: String? {
        GroupsCopy.sync(state.syncBadge, pendingChangeCount: state.pendingChangeCount)
    }

    @ViewBuilder
    private var people: some View {
        if state.showsPeople || state.isFirstDownload {
            #if os(macOS)
            HomePeopleSection(people: state.people, isLoading: state.isFirstDownload, faceSize: 44, send: model.send)
            #else
            HomePeopleSection(people: state.people, isLoading: state.isFirstDownload, faceSize: 56, send: model.send)
            #endif
        }
    }

    private func summary(stacksButtons: Bool) -> some View {
        HomeSummaryCard(
            summary: state.summary,
            isFirstDownload: state.isFirstDownload,
            stacksButtons: stacksButtons,
            send: model.send,
        )
    }

    @ViewBuilder
    private var shortcuts: some View {
        #if os(iOS)
        HomeShortcuts(canScanReceipt: state.canScanReceipt, send: model.send)
        #endif
    }

    @ViewBuilder
    private var recoveryCard: some View {
        if state.showsRecoveryCodeCard {
            HomeRecoveryCard(send: model.send)
        }
    }

    @ViewBuilder
    private func groups(cardWidth: CGFloat) -> some View {
        if !state.groups.isEmpty {
            HomeGroupsSection(groups: state.groups, style: .cards(width: cardWidth), send: model.send)
        }
    }

    @ViewBuilder
    private var bills: some View {
        if !state.billDays.isEmpty || state.isFirstDownload {
            HomeBillsSection(days: state.billDays, isLoading: state.isFirstDownload, send: model.send)
        }
    }

    // MARK: - Bars

    private var profileButton: some View {
        ProfileToolbarButton(initial: state.avatarInitial, color: state.avatarColor, photoUrl: state.photoUrl) {
            model.send(HomeUiEventOnProfileClicked.shared)
        }
    }

    private var splitBillButton: some View {
        SplitBillButton {
            model.send(HomeUiEventOnSplitBillClicked.shared)
        }
    }

    #if os(macOS)
    /// "Try again" while the server doesn't answer, the shortcuts grouped in glass, the profile, and "Split a bill"
    /// as the prominent item, ⌘N.
    @ToolbarContentBuilder
    private var macToolbar: some ToolbarContent {
        if state.canRetrySync {
            ToolbarItem(placement: .primaryAction) {
                Button("Try again", systemImage: "arrow.clockwise") {
                    model.send(HomeUiEventOnRetrySyncClicked.shared)
                }
            }
        }
        if state.showsHome {
            ToolbarItem(placement: .primaryAction) {
                ControlGroup {
                    Button("Scan a receipt", systemImage: "doc.text.viewfinder") {
                        model.send(HomeUiEventOnScanReceiptClicked.shared)
                    }
                    .disabled(!state.canScanReceipt)
                    .help(state.canScanReceipt ? Text("Scan a receipt") : Text("Scan a receipt · needs internet"))
                    Button("Record payment", systemImage: "banknote") {
                        model.send(HomeUiEventOnRecordPaymentClicked.shared)
                    }
                    .help(Text("Record payment"))
                    Button("New group", systemImage: "person.badge.plus") {
                        model.send(HomeUiEventOnCreateGroupClicked.shared)
                    }
                    .help(Text("New group"))
                }
            }
        }
        ToolbarItem(placement: .primaryAction) { profileButton }
        if state.showsSplitBillButton {
            ToolbarItem(placement: .primaryAction) {
                splitBillButton
                    .keyboardShortcut("n", modifiers: .command)
                    .help(Text("Split a bill (⌘N)"))
            }
        }
    }
    #endif
}

#if os(iOS)
/// The four quarters of a screen whose fold runs across it (an iPhone Duo upright): two columns above the fold and
/// two below, the gap covering the fold whole, each quarter scrolling on its own.
private struct FoldQuarters<TopLeading: View, TopTrailing: View, BottomLeading: View, BottomTrailing: View>: View {
    let fold: CGRect
    let spacing: CGFloat
    @ViewBuilder let topLeading: TopLeading
    @ViewBuilder let topTrailing: TopTrailing
    @ViewBuilder let bottomLeading: BottomLeading
    @ViewBuilder let bottomTrailing: BottomTrailing

    var body: some View {
        let gapStart = max(0, min(fold.midY - 24, fold.minY))
        let gapEnd = max(gapStart, fold.midY + 24, fold.maxY)
        VStack(spacing: 0) {
            HStack(alignment: .top, spacing: spacing) {
                quarter { topLeading }
                quarter { topTrailing }
            }
            .frame(height: gapStart)
            Color.clear
                .frame(height: gapEnd - gapStart)
            HStack(alignment: .top, spacing: spacing) {
                quarter { bottomLeading }
                quarter { bottomTrailing }
            }
            .frame(maxHeight: .infinity)
        }
        .padding(.horizontal, 24)
    }

    private func quarter(@ViewBuilder _ content: () -> some View) -> some View {
        ScrollView {
            VStack(spacing: 14) {
                content()
            }
            .padding(.vertical, 24)
        }
        .frame(maxWidth: .infinity)
    }
}
#endif
