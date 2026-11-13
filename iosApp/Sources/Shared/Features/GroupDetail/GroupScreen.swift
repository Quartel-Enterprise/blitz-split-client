import Observation
import Shared
import SwiftUI

/// What the group's screen draws once over its tabs, sent to the group's ViewModel: the bars, the group's header, its
/// faces (which pick Members), "Your balance in this group" and the tabs.
struct GroupScreenActions {
    let select: (GroupTab) -> Void
    let invite: () -> Void
    let stats: () -> Void
    /// The group's avatar in its header, which opens the photo big.
    let photo: () -> Void
    let editGroup: () -> Void
    /// "Pin to Home" or "Unpin from Home", as the header says the group is.
    let togglePin: () -> Void
    let leaveGroup: () -> Void
    let deleteGroup: () -> Void
    let splitBill: () -> Void
    let settle: () -> Void
    /// A currency chip of the header and Balances, shown while the group has money in more than one.
    let selectCurrency: (GroupCurrency) -> Void
    /// The main button, the cancel and "Back to Groups" of the dialogs Leave and Delete open.
    let confirmExit: () -> Void
    let dismissExit: () -> Void
    let backToGroups: () -> Void

    /// Before a screen's model exists its bars are disabled, so these never run.
    static let disabled = GroupScreenActions(
        select: { _ in },
        invite: {},
        stats: {},
        photo: {},
        editGroup: {},
        togglePin: {},
        leaveGroup: {},
        deleteGroup: {},
        splitBill: {},
        settle: {},
        selectCurrency: { _ in },
        confirmExit: {},
        dismissExit: {},
        backToGroups: {},
    )
}

extension View {
    /// The bars of the group's screen: its name, "Statistics", "Invite people", the "⋯" menu and "Split a bill", the
    /// main button on every tab (the bottom toolbar's prominent glass item on iPhone, iOS has no FAB; the window
    /// toolbar's primary button on the Mac, with the tabs in the toolbar). `header` is nil until the screen is loaded,
    /// which keeps the buttons out of the bars rather than drawing them disabled. Offline, the menu's Leave and Delete
    /// need the internet; the dialogs they open come with the bars.
    func groupScreenBars(
        tab: GroupTab,
        host: AppRoute,
        header: GroupHeader?,
        hasExpenses: Bool,
        showsSplitBill: Bool,
        headerScroll: GroupHeaderScroll,
        exit: GroupExitState?,
        actions: GroupScreenActions,
    ) -> some View {
        modifier(
            GroupScreenBars(
                tab: tab,
                host: host,
                header: header,
                hasExpenses: hasExpenses,
                showsSplitBill: showsSplitBill,
                headerScroll: headerScroll,
                isOnline: exit?.isOnline ?? true,
                actions: actions,
            ),
        )
        .groupExitDialogs(exit: exit, actions: actions)
    }
}

private struct GroupScreenBars: ViewModifier {
    let tab: GroupTab
    /// The group's screen in the shared back stack, whose "Invite people" anchors the Mac's invite popover, except on
    /// Members, whose Share does.
    let host: AppRoute
    let header: GroupHeader?
    let hasExpenses: Bool
    let showsSplitBill: Bool
    let headerScroll: GroupHeaderScroll
    let isOnline: Bool
    let actions: GroupScreenActions

    #if os(iOS)
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.verticalSizeClass) private var verticalSizeClass
    #else
    @Environment(\.isNarrowWindow) private var isNarrowWindow
    @Environment(\.frontRoute) private var frontRoute
    #endif
    /// An iPhone Duo's vertical bar (iOS 27.1), which takes the main button as its plus alone.
    @State private var hasVerticalBar = false

    private var isLoaded: Bool {
        header != nil
    }

    func body(content: Content) -> some View {
        content
            .readingVerticalBar($hasVerticalBar)
            .navigationTitle(Text(verbatim: header?.groupName ?? ""))
            #if os(iOS)
            .toolbarTitleDisplayMode(.inline)
            // On iPhone the group's own bar replaces the app's tabs, as the canvas draws it
            .toolbar(horizontalSizeClass == .compact ? .hidden : .automatic, for: .tabBar)
            #else
            .navigationSubtitle(header.map { groupMeta($0, hasExpenses: hasExpenses) } ?? "")
            // File › Split a Bill (⌘N): a bill in this group, while this screen is the one in front
            .newItemCommands(
                isInFront: frontRoute == host,
                splitBill: isLoaded && showsSplitBill ? actions.splitBill : nil,
            )
            #endif
            .toolbar { toolbar }
    }

    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        #if os(iOS)
        // The header shows the avatar and the name; the bar only shows them small once the header scrolls away
        if let header {
            ToolbarItem(placement: .principal) {
                GroupInlineTitle(header: header, scroll: headerScroll, onPhoto: actions.photo)
            }
        }
        #else
        // A narrow window draws them below the toolbar instead (`MacGroupTop`)
        if !isNarrowWindow, isLoaded {
            ToolbarItem(placement: .principal) {
                GroupTabsPicker(selected: tab, onSelect: actions.select)
            }
        }
        #endif
        // The group not read yet: no button stands disabled in the bars, they come with it
        if isLoaded {
            ToolbarItem(placement: .primaryAction) {
                Button("Statistics", systemImage: "chart.bar", action: actions.stats)
            }
            ToolbarItem(placement: .primaryAction) {
                Button("Invite people", systemImage: "person.badge.plus", action: actions.invite)
                    .invitePopover(host: host, isAnchor: tab != .members)
            }
            ToolbarItem(placement: .primaryAction) { groupMenu }
        }
        if showsSplitBill {
            splitBillItem
        }
    }

    @ToolbarContentBuilder
    private var splitBillItem: some ToolbarContent {
        #if os(iOS)
        if usesBottomBar {
            ToolbarSpacer(.flexible, placement: .bottomBar)
            ToolbarItem(placement: .bottomBar) { splitBillButton }
        } else {
            ToolbarItem(placement: .primaryAction) { splitBillButton }
                .prefersVerticalBar()
        }
        #else
        ToolbarItem(placement: .primaryAction) {
            // File › Split a Bill (⌘N) runs it, as on Home and Groups: a bill in this group (`newItemCommands`)
            splitBillButton
                .help(Text("Split a bill (⌘N)"))
        }
        #endif
    }

    /// "⋯": only what changes the group itself (6.1i), Pin to Home first, which works offline; offline, Leave and
    /// Delete say they need the internet (6.1j). The Mac's items that open a dialog end in "…".
    private var groupMenu: some View {
        Menu("More group options", systemImage: "ellipsis") {
            if header?.isPinned == true {
                Button("Unpin from Home", systemImage: "pin.slash", action: actions.togglePin)
            } else {
                Button("Pin to Home", systemImage: "pin", action: actions.togglePin)
            }
            Button(editGroupTitle, systemImage: "pencil", action: actions.editGroup)
            Divider()
            Button(action: actions.leaveGroup) {
                menuLabel(leaveGroupTitle, systemImage: "rectangle.portrait.and.arrow.right")
            }
            .disabled(!isOnline)
            Button(role: .destructive, action: actions.deleteGroup) {
                menuLabel(deleteGroupTitle, systemImage: "trash")
            }
            .disabled(!isOnline)
        }
    }

    /// A menu shows a label's second line as its subtitle: "Needs internet" while offline.
    private func menuLabel(_ title: LocalizedStringKey, systemImage: String) -> some View {
        Label {
            Text(title)
            if !isOnline {
                Text("Needs internet")
            }
        } icon: {
            Image(systemName: systemImage)
        }
    }

    private var editGroupTitle: LocalizedStringKey {
        #if os(macOS)
        "Edit group…"
        #else
        "Edit group"
        #endif
    }

    private var leaveGroupTitle: LocalizedStringKey {
        #if os(macOS)
        "Leave group…"
        #else
        "Leave group"
        #endif
    }

    private var deleteGroupTitle: LocalizedStringKey {
        #if os(macOS)
        "Delete group…"
        #else
        "Delete group"
        #endif
    }

    private var splitBillButton: some View {
        SplitBillButton(showsTitle: !hasVerticalBar, action: actions.splitBill)
    }

    #if os(iOS)
    /// The upright iPhone: "Split a bill" goes to the bottom toolbar, under the thumb, except into an iPhone Duo's
    /// vertical bar, right under Back, as the boards draw it.
    private var usesBottomBar: Bool {
        horizontalSizeClass == .compact && verticalSizeClass == .regular && !hasVerticalBar
    }
    #endif
}

/// "Your balance in this group" once there is a bill; while the account's copy isn't whole the balance isn't known yet,
/// so the card shimmers in its slot.
struct GroupBalanceSlot: View {
    let header: GroupHeader
    let isMyBalanceLoading: Bool
    let onSettle: () -> Void
    let onSelectCurrency: (GroupCurrency) -> Void
    /// Members draws its own chips, with its own pick, so the header's stay out of that tab
    let showsCurrencyChips: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            // The header's amounts and the balance below are in the chip picked
            if showsCurrencyChips {
                CurrencyChips(currencies: header.currencies, selected: header.currency, onSelect: onSelectCurrency)
            }
            if let balance = header.myBalance {
                BalanceCard(
                    balance: balance,
                    currency: header.currency,
                    isEveryoneSettled: header.isEveryoneSettled,
                    onSettle: onSettle,
                )
            } else if isMyBalanceLoading {
                ShimmerCard(showsButtons: false)
                    .shimmering()
                    .background(Palette.card, in: .rect(cornerRadius: 26))
            }
        }
    }
}

/// Despesas' or Saldos' rows while the account's first full sync may still bring bills, in the empty state's place,
/// with "No internet. Your data shows up once you're back online." under them while that sync waits offline.
struct GroupLoadingRows: View {
    let isWaitingForInternet: Bool
    let leading: ShimmerRow.Leading

    var body: some View {
        Section {
            ShimmerList(count: 4, leading: leading)
                .shimmering()
        } footer: {
            if isWaitingForInternet {
                WaitingForInternetNote()
            }
        }
    }
}

/// "6 members · $2,418.60 in total" or "6 members · no expenses yet".
func groupMeta(_ header: GroupHeader, hasExpenses: Bool) -> String {
    let members = String(localized: "\(Int(header.memberCount)) members", bundle: .appLanguage)
    let spent = hasExpenses
        ? String(localized: "\(header.currency.format(header.totalSpentMinor)) in total", bundle: .appLanguage)
        : String(localized: "no expenses yet", bundle: .appLanguage)
    return "\(members) · \(spent)"
}

/// The faces of the group, "+N" for the rest, and "1 hasn't joined yet": a tap opens Members.
struct GroupFaces: View {
    let header: GroupHeader
    let size: CGFloat
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                HStack(spacing: -size * 0.25) {
                    ForEach(Array(header.faces.enumerated()), id: \.offset) { _, face in
                        AvatarPhoto(initial: face.initials, color: face.color, photoUrl: face.photoUrl, size: size)
                            .overlay(Circle().strokeBorder(Palette.groupedBackground, lineWidth: 2))
                    }
                    if header.extraMemberCount > 0 {
                        Text(verbatim: "+\(header.extraMemberCount)")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.secondary)
                            .frame(minWidth: size, minHeight: size)
                            .padding(.horizontal, 4)
                            .background(.fill.tertiary, in: .capsule)
                    }
                }
                if header.notJoinedCount > 0 {
                    Text("\(Int(header.notJoinedCount)) hasn't joined yet")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .contentShape(.rect)
            .fingerTarget(verticalOverlap: max(0, (44 - size) / 2))
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text("See members"))
        .accessibilityValue(facesValue)
        .accessibilityAddTraits(.isButton)
    }

    /// "6 members, 1 hasn't joined yet".
    private var facesValue: Text {
        let members = String(localized: "\(Int(header.memberCount)) members", bundle: .appLanguage)
        guard header.notJoinedCount > 0 else { return Text(verbatim: members) }
        return Text(verbatim: "\(members), \(String(localized: "\(Int(header.notJoinedCount)) hasn't joined yet", bundle: .appLanguage))")
    }
}

/// "Your balance in this group", over the tabs of Expenses, Balances and Members: "+$214.90 to get back" or
/// "−$118.00 you owe" with what makes it up, or "$0.00 · you're settled up"; "Settle up" while this person owes.
struct BalanceCard: View {
    let balance: MemberBalance
    let currency: GroupCurrency
    let isEveryoneSettled: Bool
    let onSettle: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Your balance in this group")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                amount
                Text(verbatim: details)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .accessibilityElement(children: .combine)
            Spacer(minLength: 8)
            if balance.canSettle {
                Button("Settle up", action: onSettle)
                    .fontWeight(.semibold)
                    #if os(iOS)
                    .buttonStyle(.glassProminent)
                    .prominentLabel()
                    #else
                    .buttonStyle(.borderedProminent)
                    .prominentLabel()
                    #endif
                    .buttonBorderShape(.capsule)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Palette.card, in: .rect(cornerRadius: 22))
    }

    @ViewBuilder
    private var amount: some View {
        if balance.isSettled {
            Text("\(currency.format(0)) · you're settled up")
                .font(.title3.weight(.semibold))
        } else {
            // What this person gets back and owes may cancel out: "$0.00", muted, with both in the details. Beside
            // "Settle up" in a narrow column (an iPhone Duo closed, with its vertical bar) the word goes under the
            // amount, which stays on one line and shrinks before it would break
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    netAmount
                    netWord
                }
                VStack(alignment: .leading, spacing: 0) {
                    netAmount
                    netWord
                }
            }
        }
    }

    private var netAmount: some View {
        Text(verbatim: currency.formatSigned(balance.netMinor))
            .font(.title2.bold())
            .monospacedDigit()
            .foregroundStyle(Palette.balance(balance.netMinor))
            .lineLimit(1)
            .minimumScaleFactor(0.7)
    }

    @ViewBuilder
    private var netWord: some View {
        if balance.netMinor > 0 {
            Text("to get back")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .lineLimit(1)
        } else if balance.netMinor < 0 {
            Text("you owe")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .lineLimit(1)
        }
    }

    /// "You get $332.90 and owe $118.00", without the part that is zero.
    private var details: String {
        if balance.isSettled {
            return isEveryoneSettled
                ? String(localized: "Nobody owes anybody anything here.", bundle: .appLanguage)
                : String(localized: "Others still have to settle up.", bundle: .appLanguage)
        }
        let receives = currency.format(balance.receivesMinor)
        let owes = currency.format(balance.owesMinor)
        if balance.owesMinor == 0 { return String(localized: "You get \(receives)", bundle: .appLanguage) }
        if balance.receivesMinor == 0 { return String(localized: "You owe \(owes)", bundle: .appLanguage) }
        return String(localized: "You get \(receives) and owe \(owes)", bundle: .appLanguage)
    }
}

#if os(iOS)
/// The group's screen on iPhone upright and iPad: one column (640 wide at most on iPad) with the header, the balance
/// card and the tabs over the picked tab's sections. iPhone lying down: the header and the card on the left, the tabs
/// and the sections on the right. An iPhone Duo open upright: the header and the card above the fold, the tabs and the
/// sections below it (3.3, 6.2), on every tab so the header never moves. Only the sections change with the tab.
struct PhoneGroupScaffold<Sections: View>: View {
    let header: GroupHeader
    /// The account's copy isn't whole: "Your balance in this group" shimmers in its place rather than leaving it out.
    let isMyBalanceLoading: Bool
    let hasExpenses: Bool
    let tab: GroupTab
    let usesTwoColumns: Bool
    /// Only the header's avatar and the bar's title read it, so a scroll never draws the rest of the screen again.
    let headerScroll: GroupHeaderScroll
    let actions: GroupScreenActions
    @ViewBuilder let sections: Sections

    var body: some View {
        Group {
            if usesTwoColumns {
                HStack(alignment: .top, spacing: 8) {
                    summary
                        .frame(width: 328)
                    list(withHeader: false)
                }
            } else {
                // An iPhone Duo open upright: the header and the card above the fold, the tabs below it
                FoldHalves(scrollsHalves: false) {
                    summary
                        .scrollBounceBehavior(.basedOnSize)
                        .frame(maxWidth: 640)
                        .frame(maxWidth: .infinity)
                } bottom: {
                    list(withHeader: false)
                        .frame(maxWidth: 640)
                        .frame(maxWidth: .infinity)
                } single: {
                    list(withHeader: true)
                        .frame(maxWidth: 640)
                        .frame(maxWidth: .infinity)
                }
            }
        }
        .background(Palette.groupedBackground)
    }

    /// The header and the card beside or above the tabs.
    private var summary: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                top(collapses: false)
                balanceCard
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)
        }
    }

    /// The group's avatar big, its name and "2 members · R$ 3,418.31 in total" under it, then its faces, centred.
    /// Over the list the avatar shrinks as the header scrolls under the bar (`collapses`); beside the tabs or above
    /// the fold it stays, a little smaller to leave the card its room.
    private func top(collapses: Bool) -> some View {
        VStack(spacing: 14) {
            VStack(spacing: 4) {
                GroupHeaderAvatar(
                    header: header,
                    size: collapses ? GroupHeaderScroll.avatarSize : 72,
                    scroll: collapses ? headerScroll : nil,
                    action: actions.photo,
                )
                .padding(.bottom, 8)
                Text(verbatim: header.groupName)
                    .font(.title.bold())
                    .lineLimit(2)
                    .accessibilityAddTraits(.isHeader)
                    .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { height in
                        if collapses { headerScroll.nameHeight = height }
                    }
                Text(verbatim: groupMeta(header, hasExpenses: hasExpenses))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .multilineTextAlignment(.center)
            GroupFaces(header: header, size: 28) { actions.select(.members) }
        }
        .frame(maxWidth: .infinity)
    }

    private var balanceCard: some View {
        GroupBalanceSlot(
            header: header,
            isMyBalanceLoading: isMyBalanceLoading,
            onSettle: actions.settle,
            onSelectCurrency: actions.selectCurrency,
            showsCurrencyChips: tab != .members
        )
    }

    private func list(withHeader: Bool) -> some View {
        List {
            if withHeader {
                Section {
                    top(collapses: true)
                    balanceCard
                }
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets(top: 4, leading: 0, bottom: 4, trailing: 0))
                .listRowSeparator(.hidden)
            }
            GroupTabsSection(selected: tab, onSelect: actions.select)
            sections
        }
        .listStyle(.insetGrouped)
        .listSectionSpacing(.compact)
        .scrollContentBackground(.hidden)
        // Tied to the scroll itself, frame by frame, as a large title folds into the bar; with the header beside the
        // tabs or above the fold the bar never names the group. Held at the fold's end, so a scroll further down the
        // bills never draws the header's avatar and the bar's title again
        .onScrollGeometryChange(for: CGFloat.self) { geometry in
            withHeader ? min(max(0, geometry.contentOffset.y + geometry.contentInsets.top), headerScroll.foldEnd) : 0
        } action: { _, offset in
            headerScroll.offset = offset
        }
    }
}
#endif

/// How far the group's list scrolled past its top. Only the header's avatar and the bar's title read it, so the
/// rest of the screen isn't drawn again on every frame of a scroll.
@Observable
final class GroupHeaderScroll {
    /// The header's avatar at the top of the list on iPhone and iPad (Pierre's beta feedback of 2026-11-12: bigger,
    /// shrinking as it goes to the top bar).
    static let avatarSize: CGFloat = 88
    /// The avatar beside the name in the bar once the header scrolled away.
    static let inlineAvatarSize: CGFloat = 30

    /// Scrolled past the top, in points, 0 at the top.
    var offset: CGFloat = 0

    /// The header's avatar shrinks to half its side over the first 88 pt of scroll, the time it takes to go under
    /// the bar, and fades as its last part goes.
    var avatarScale: CGFloat {
        1 - 0.5 * progress(from: 0, to: Self.avatarSize)
    }

    var avatarOpacity: CGFloat {
        1 - progress(from: 56, to: 96)
    }

    /// The header's name's height, measured: a second line or a larger text size pushes its last line further down.
    var nameHeight: CGFloat = 32

    /// The bar's avatar and name come in while the header's name's last line goes under the bar (after the avatar,
    /// its padding and the lines above it: 100 to 132 pt for one line), as the large title turns into the inline one.
    var titleProgress: CGFloat {
        progress(from: titleStart, to: foldEnd)
    }

    /// Where the header has gone and the bar's title is in: scrolling further changes nothing either draws.
    var foldEnd: CGFloat {
        titleStart + 32
    }

    private var titleStart: CGFloat {
        100 + max(0, nameHeight - 32)
    }

    private func progress(from start: CGFloat, to end: CGFloat) -> CGFloat {
        min(max((offset - start) / (end - start), 0), 1)
    }
}

#if os(iOS)
/// The header's avatar, shrinking towards the name as the list scrolls (`scroll`, nil where it stays). With Reduce
/// Motion on it only fades.
private struct GroupHeaderAvatar: View {
    let header: GroupHeader
    let size: CGFloat
    let scroll: GroupHeaderScroll?
    let action: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        GroupPhotoButton(header: header, size: size, action: action)
            .scaleEffect(reduceMotion ? 1 : scroll?.avatarScale ?? 1, anchor: .bottom)
            .opacity(Double(scroll?.avatarOpacity ?? 1))
    }
}

/// The group's avatar and name in the bar, coming in as the header goes under it. The avatar opens the photo big, as
/// the header's does.
private struct GroupInlineTitle: View {
    let header: GroupHeader
    let scroll: GroupHeaderScroll
    let onPhoto: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        let progress = scroll.titleProgress
        HStack(spacing: 8) {
            GroupPhotoButton(header: header, size: GroupHeaderScroll.inlineAvatarSize, action: onPhoto)
            Text(verbatim: header.groupName)
                .font(.headline)
                .lineLimit(1)
                .accessibilityAddTraits(.isHeader)
        }
        .opacity(progress)
        // Rises into place with the scroll, as the inline title does; Reduce Motion only fades it
        .offset(y: reduceMotion ? 0 : (1 - progress) * 8)
        .allowsHitTesting(progress > 0.5)
        .accessibilityHidden(progress < 0.5)
    }
}
#endif

#if os(macOS)
/// The top of the group's screen in a Mac window, over every tab: the group's strip (icon, faces, who hasn't joined) and the balance card.
/// A narrow window puts the tabs above them, below the toolbar.
struct MacGroupTop: View {
    let header: GroupHeader
    let isMyBalanceLoading: Bool
    let tab: GroupTab
    let actions: GroupScreenActions

    @Environment(\.isNarrowWindow) private var isNarrowWindow

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if isNarrowWindow {
                GroupTabsPicker(selected: tab, onSelect: actions.select)
                    .fixedSize()
            }
            HStack(spacing: 12) {
                GroupPhotoButton(header: header, size: 36, action: actions.photo)
                GroupFaces(header: header, size: 24) { actions.select(.members) }
                Spacer()
            }
            GroupBalanceSlot(
                header: header,
                isMyBalanceLoading: isMyBalanceLoading,
                onSettle: actions.settle,
                onSelectCurrency: actions.selectCurrency,
                showsCurrencyChips: tab != .members
            )
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
    }
}
#endif

/// The group's avatar in its header, a button that opens the photo big.
struct GroupPhotoButton: View {
    let header: GroupHeader
    let size: CGFloat
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            GroupAvatar(
                initials: header.groupInitials,
                color: header.groupColor,
                photoUrl: header.groupPhotoUrl,
                size: size
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel("View group photo")
    }
}
