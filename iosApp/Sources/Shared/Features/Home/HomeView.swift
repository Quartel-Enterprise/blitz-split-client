import Observation
import Shared
import SwiftUI

@Observable
final class HomeModel {
    private(set) var state: HomeUiState
    private(set) var changeErrorCount = 0
    /// "Notifications on", once the system allows them after not allowing them.
    private(set) var notificationsOnCount = 0
    /// "We saw you on your phone.", once a sync found the account on a phone app while the Mac's invite showed (14.1c).
    private(set) var usedOnPhoneCount = 0
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
            case is HomeUiActionShowChangeError: self?.changeErrorCount += 1
            case is HomeUiActionShowNotificationsOn: self?.notificationsOnCount += 1
            case is HomeUiActionShowUsedOnPhone: self?.usedOnPhoneCount += 1
            case is HomeUiActionRequestNotificationPermission:
                Task {
                    await PushNotifications.requestPermission()
                    self?.checkNotificationPermission()
                }
            case is HomeUiActionRequestQuietNotifications:
                Task {
                    await PushNotifications.requestQuietPermission()
                    // Its own event, so a read started before the request isn't taken for its answer
                    let permission = await PushNotifications.readPermission()
                    self?.send(HomeUiEventOnQuietNotificationsAnswered(permission: permission))
                }
            case is HomeUiActionOpenNotificationSettings: PushNotifications.openSettings()
            default: break
            }
        })
    }

    func send(_ event: any HomeUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    /// On appear, on every return to the app and after the system's prompt: the card follows what the system says.
    func checkNotificationPermission() {
        Task {
            let permission = await PushNotifications.readPermission()
            // Apple can always deliver quietly first, with no prompt
            send(HomeUiEventOnNotificationPermissionChecked(permission: permission, canAskQuietly: true))
        }
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
    /// An iPhone Duo's vertical bar (iOS 27.1), which takes "Split a bill" under the profile.
    @State private var hasVerticalBar = false

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
                        downloadStatus
                        HomeFirstUseCard(send: model.send)
                    }
                    .padding(16)
                    .padding(.top, 40)
                    .frame(maxWidth: .infinity)
                }
            } else if state.showsHome {
                content
            } else {
                // Room not read yet: Início's shape shimmers, so the first use's card never flashes before the groups
                ShimmerScreen {
                    HomeFacesPlaceholder(faceSize: 56)
                    ShimmerCard()
                        .background(Palette.card, in: .rect(cornerRadius: 26))
                    ShimmerList(count: 3, leading: .badge)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.groupedBackground)
        .measuringScreenLayout($layout)
        .readingFoldDivision($division)
        .readingVerticalBar($hasVerticalBar)
        // The notification card asks, never Home on its own: it only reads what the system says
        .checkingNotificationPermission { model.checkNotificationPermission() }
        // Record payment, which "Who paid whom?" or a face's settling up opened over Home, just closed
        .recordedPaymentNotice(
            state.recordedPayment,
            onUndo: { model.send(HomeUiEventOnUndoPaymentClicked.shared) },
            onShown: { model.send(HomeUiEventOnRecordedPaymentNoticeShown.shared) },
        )
        .navigationTitle(HomeCopy.tabTitle)
        #if os(iOS)
        .tabTitleDisplayMode()
        .toolbar {
            TabTitle(title: HomeCopy.tabTitle)
            // The sync status takes the bar's empty leading side, so it comes and goes without moving the content
            if let status = syncText {
                ToolbarItem(placement: .topBarLeading) {
                    SyncStatusCapsule(status: status, badge: state.syncBadge, canRetry: state.canRetrySync) {
                        model.send(HomeUiEventOnRetrySyncClicked.shared)
                    }
                }
            }
            TabTrailingItems(
                showsSplitBill: state.showsSplitBillButton,
                showsSplitBillTitle: !hasVerticalBar && !isUprightIPhone,
                onSplitBill: { model.send(HomeUiEventOnSplitBillClicked.shared) },
            ) {
                bellButton
                profileButton
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
                        downloadStatus
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

    /// An iPhone upright, whose bar has the room for "Split a bill"'s icon alone beside the bell and the profile, as
    /// Groups draws it.
    private var isUprightIPhone: Bool {
        horizontalSizeClass == .compact && verticalSizeClass == .regular
    }
    #endif

    @ViewBuilder
    private func firstColumn(stacksButtons: Bool) -> some View {
        downloadStatus
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

    /// The overall balance over the shortcuts beside the people, then the latest bills beside your groups.
    private var macWide: some View {
        VStack(alignment: .leading, spacing: 12) {
            recoveryCard
            HStack(alignment: .top, spacing: 12) {
                VStack(spacing: 12) {
                    summary(stacksButtons: true)
                    shortcuts
                }
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

    /// One column: the overall balance, the shortcuts, the people, then your groups as a grid over the bills.
    private var macNarrow: some View {
        VStack(alignment: .leading, spacing: 12) {
            recoveryCard
            summary(stacksButtons: false)
            shortcuts
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

    /// The first download's bar while it fills the device ("Syncing… 3 of 7 groups" is in the bar's leading capsule,
    /// as "Offline · 1 change queued" and "No response from the server" are). The Mac says it as the window's subtitle.
    @ViewBuilder
    private var downloadStatus: some View {
        #if os(iOS)
        if let progress = state.firstDownload {
            DownloadProgressBar(progress: progress)
                .padding(.horizontal, 4)
        }
        #endif
    }

    private var syncText: String? {
        GroupsCopy.sync(
            state.syncBadge,
            pendingChangeCount: state.pendingChangeCount,
            downloadProgress: state.firstDownload,
        )
    }

    /// The faces shimmer until they are read, and while the first download brought nobody yet.
    @ViewBuilder
    private var people: some View {
        let people = state.people.loaded(as: [HomePerson].self)
        if state.showsPeople || state.isLoadingPeople {
            #if os(macOS)
            HomePeopleSection(people: people, isLoading: state.isLoadingPeople, faceSize: 44, send: model.send)
            #else
            HomePeopleSection(people: people, isLoading: state.isLoadingPeople, faceSize: 56, send: model.send)
            #endif
        }
    }

    /// "Overall balance" shimmers with its two buttons until it is read: never "Nothing to receive" before it.
    @ViewBuilder
    private func summary(stacksButtons: Bool) -> some View {
        if let summary = state.summary.loaded(as: HomeSummary.self) {
            HomeSummaryCard(
                summary: summary,
                isFirstDownload: state.isFirstDownload,
                stacksButtons: stacksButtons,
                send: model.send,
            )
        } else {
            ShimmerCard()
                .shimmering()
                .background(Palette.card, in: .rect(cornerRadius: 26))
        }
    }

    private var shortcuts: some View {
        HomeShortcuts(canScanReceipt: state.canScanReceipt, send: model.send)
    }

    /// The recovery code's card, or the notifications' once there is a group: never both at once.
    @ViewBuilder
    private var recoveryCard: some View {
        if state.showsRecoveryCodeCard {
            HomeRecoveryCard(send: model.send)
        }
        if let prompt = state.notificationPrompt {
            HomeNotificationCard(prompt: prompt, send: model.send)
        }
        #if os(macOS)
        // "Get the app" (page 14): only the Mac and the web invite, after the first bill, one card at a time
        if state.showsAppInviteCard, let invite = state.appInvite {
            HomeAppInviteCard(
                invite: invite,
                needsAccount: state.appInviteNeedsAccount,
                accountProvider: state.accountProvider,
                send: model.send,
            )
        }
        #endif
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

    private var bellButton: some View {
        ActivityBellButton(unreadCount: Int(state.activityUnreadCount)) {
            model.send(HomeUiEventOnActivityClicked.shared)
        }
    }

    private var profileButton: some View {
        ProfileToolbarButton(initial: state.avatarInitial, color: state.avatarColor, photoUrl: state.photoUrl) {
            model.send(HomeUiEventOnProfileClicked.shared)
        }
    }

    #if os(macOS)
    private var splitBillButton: some View {
        SplitBillButton {
            model.send(HomeUiEventOnSplitBillClicked.shared)
        }
    }

    /// "Try again" while the server doesn't answer, search, the bell, the profile, and "Split a bill" as the
    /// prominent item, ⌘N. The shortcuts sit in the content, spelled out, as on the other platforms.
    @ToolbarContentBuilder
    private var macToolbar: some ToolbarContent {
        if state.canRetrySync {
            ToolbarItem(placement: .primaryAction) {
                Button("Try again", systemImage: "arrow.clockwise") {
                    model.send(HomeUiEventOnRetrySyncClicked.shared)
                }
            }
        }
        ToolbarItem(placement: .primaryAction) { bellButton }
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
