import Shared
import SwiftUI

/// What the group's tabs Expenses and Balances share, each sent to its own ViewModel: the bars, the group's header,
/// its faces, "Your balance in this group" and the tabs.
struct GroupScreenActions {
    let select: (GroupTab) -> Void
    let invite: () -> Void
    let members: () -> Void
    let editGroup: () -> Void
    let leaveGroup: () -> Void
    let deleteGroup: () -> Void
    let splitBill: () -> Void
    let settle: () -> Void

    /// Before a screen's model exists its bars are disabled, so these never run.
    static let disabled = GroupScreenActions(
        select: { _ in },
        invite: {},
        members: {},
        editGroup: {},
        leaveGroup: {},
        deleteGroup: {},
        splitBill: {},
        settle: {},
    )
}

extension View {
    /// The bars of a group's tab: its name, "Invite people", the "⋯" menu and "Split a bill" (the bottom toolbar's
    /// prominent glass item on iPhone, iOS has no FAB; the window toolbar's primary button on the Mac, with the tabs
    /// in the toolbar). `header` is nil until the screen is loaded, which disables the buttons.
    func groupScreenBars(
        tab: GroupTab,
        header: GroupHeader?,
        hasExpenses: Bool,
        showsSplitBill: Bool,
        showsInlineTitle: Bool,
        actions: GroupScreenActions,
    ) -> some View {
        modifier(
            GroupScreenBars(
                tab: tab,
                header: header,
                hasExpenses: hasExpenses,
                showsSplitBill: showsSplitBill,
                showsInlineTitle: showsInlineTitle,
                actions: actions,
            ),
        )
    }
}

private struct GroupScreenBars: ViewModifier {
    let tab: GroupTab
    let header: GroupHeader?
    let hasExpenses: Bool
    let showsSplitBill: Bool
    let showsInlineTitle: Bool
    let actions: GroupScreenActions

    #if os(iOS)
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.verticalSizeClass) private var verticalSizeClass
    #endif

    private var isLoaded: Bool {
        header != nil
    }

    func body(content: Content) -> some View {
        content
            .navigationTitle(Text(verbatim: header?.groupName ?? ""))
            #if os(iOS)
            .toolbarTitleDisplayMode(.inline)
            // On iPhone the group's own bar replaces the app's tabs, as the canvas draws it
            .toolbar(horizontalSizeClass == .compact ? .hidden : .automatic, for: .tabBar)
            #else
            .navigationSubtitle(header.map { groupMeta($0, hasExpenses: hasExpenses) } ?? "")
            #endif
            .toolbar { toolbar }
    }

    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        #if os(iOS)
        // The large title shows the name; the bar only names the group once it scrolls away
        ToolbarItem(placement: .principal) {
            Text(verbatim: header?.groupName ?? "")
                .font(.headline)
                .lineLimit(1)
                .opacity(showsInlineTitle ? 1 : 0)
                .accessibilityAddTraits(.isHeader)
        }
        #else
        ToolbarItem(placement: .principal) {
            GroupTabsPicker(selected: tab, onSelect: actions.select)
                .disabled(!isLoaded)
        }
        #endif
        ToolbarItem(placement: .primaryAction) {
            Button("Invite people", systemImage: "person.badge.plus", action: actions.invite)
                .disabled(!isLoaded)
        }
        ToolbarItem(placement: .primaryAction) { groupMenu }
        if showsSplitBill {
            #if os(iOS)
            if usesBottomBar {
                ToolbarSpacer(.flexible, placement: .bottomBar)
                ToolbarItem(placement: .bottomBar) { splitBillButton }
            } else {
                ToolbarItem(placement: .primaryAction) { splitBillButton }
            }
            #else
            ToolbarItem(placement: .primaryAction) { splitBillButton }
            #endif
        }
    }

    /// "⋯": only what changes the group itself (6.1i); Leave and Delete aren't built yet. The Mac's items that open
    /// a dialog end in "…".
    private var groupMenu: some View {
        Menu("More group options", systemImage: "ellipsis") {
            Button(editGroupTitle, systemImage: "pencil", action: actions.editGroup)
            Divider()
            Button("Leave group", systemImage: "rectangle.portrait.and.arrow.right", action: actions.leaveGroup)
            Button("Delete group", systemImage: "trash", role: .destructive, action: actions.deleteGroup)
        }
        .disabled(!isLoaded)
    }

    private var editGroupTitle: LocalizedStringKey {
        #if os(macOS)
        "Edit group…"
        #else
        "Edit group"
        #endif
    }

    private var splitBillButton: some View {
        Button(action: actions.splitBill) {
            // A toolbar draws a Label as its icon only: the canvas spells "Split a bill" out
            HStack(spacing: 6) {
                Image(systemName: "plus")
                Text("Split a bill")
            }
            .fontWeight(.semibold)
        }
        #if os(iOS)
        .buttonStyle(.glassProminent)
        #else
        .buttonStyle(.borderedProminent)
        #endif
    }

    #if os(iOS)
    /// The upright iPhone: "Split a bill" goes to the bottom toolbar, under the thumb.
    private var usesBottomBar: Bool {
        horizontalSizeClass == .compact && verticalSizeClass == .regular
    }
    #endif
}

/// "6 members · $2,418.60 in total" or "6 members · no expenses yet".
func groupMeta(_ header: GroupHeader, hasExpenses: Bool) -> String {
    let members = String(localized: "\(Int(header.memberCount)) members")
    let spent = hasExpenses
        ? String(localized: "\(header.currency.format(header.totalSpentMinor)) in total")
        : String(localized: "no expenses yet")
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
                        AvatarPhoto(initial: face.initials, color: face.color, photoUrl: nil, size: size)
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
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text("See members"))
        .accessibilityValue(facesValue)
        .accessibilityAddTraits(.isButton)
    }

    /// "6 members, 1 hasn't joined yet".
    private var facesValue: Text {
        let members = String(localized: "\(Int(header.memberCount)) members")
        guard header.notJoinedCount > 0 else { return Text(verbatim: members) }
        return Text(verbatim: "\(members), \(String(localized: "\(Int(header.notJoinedCount)) hasn't joined yet"))")
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
                    #else
                    .buttonStyle(.borderedProminent)
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
            // What this person gets back and owes may cancel out: "$0.00", neutral, with both in the details
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text(verbatim: currency.formatSigned(balance.netMinor))
                    .font(.title2.bold())
                    .monospacedDigit()
                    .foregroundStyle(netColor)
                if balance.netMinor > 0 {
                    Text("to get back")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                } else if balance.netMinor < 0 {
                    Text("you owe")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
        }
    }

    private var netColor: Color {
        if balance.netMinor > 0 { return Palette.accent }
        if balance.netMinor < 0 { return Palette.negative }
        return .primary
    }

    /// "You get $332.90 and owe $118.00", without the part that is zero.
    private var details: String {
        if balance.isSettled {
            return isEveryoneSettled
                ? String(localized: "Nobody owes anybody anything here.")
                : String(localized: "Others still have to settle up.")
        }
        let receives = currency.format(balance.receivesMinor)
        let owes = currency.format(balance.owesMinor)
        if balance.owesMinor == 0 { return String(localized: "You get \(receives)") }
        if balance.receivesMinor == 0 { return String(localized: "You owe \(owes)") }
        return String(localized: "You get \(receives) and owe \(owes)")
    }
}

#if os(iOS)
/// A group's tab on iPhone upright and iPad: one column (640 wide at most on iPad) with the header, the balance card
/// and the tabs over the tab's sections. iPhone lying down: the header and the card on the left, the tabs and the
/// sections on the right.
struct PhoneGroupScaffold<Sections: View>: View {
    let header: GroupHeader
    let hasExpenses: Bool
    let tab: GroupTab
    let usesTwoColumns: Bool
    @Binding var showsInlineTitle: Bool
    let actions: GroupScreenActions
    @ViewBuilder let sections: Sections

    var body: some View {
        Group {
            if usesTwoColumns {
                HStack(alignment: .top, spacing: 8) {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 16) {
                            top
                            balanceCard
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 12)
                    }
                    .frame(width: 328)
                    list(withHeader: false)
                }
            } else {
                list(withHeader: true)
                    .frame(maxWidth: 640)
                    .frame(maxWidth: .infinity)
            }
        }
        .background(Palette.groupedBackground)
    }

    private var top: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 14) {
                GroupAvatar(initials: header.groupInitials, color: header.groupColor, size: 56)
                VStack(alignment: .leading, spacing: 2) {
                    Text(verbatim: header.groupName)
                        .font(.title.bold())
                        .lineLimit(2)
                        .accessibilityAddTraits(.isHeader)
                    Text(verbatim: groupMeta(header, hasExpenses: hasExpenses))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            GroupFaces(header: header, size: 28, action: actions.members)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private var balanceCard: some View {
        if let balance = header.myBalance {
            BalanceCard(
                balance: balance,
                currency: header.currency,
                isEveryoneSettled: header.isEveryoneSettled,
                onSettle: actions.settle,
            )
        }
    }

    private func list(withHeader: Bool) -> some View {
        List {
            if withHeader {
                Section {
                    top
                    balanceCard
                }
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets(top: 4, leading: 20, bottom: 4, trailing: 20))
                .listRowSeparator(.hidden)
            }
            Section {
                GroupTabsPicker(selected: tab, onSelect: actions.select)
            }
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 0, trailing: 20))
            sections
        }
        .listStyle(.insetGrouped)
        .listSectionSpacing(.compact)
        .scrollContentBackground(.hidden)
        .onScrollGeometryChange(for: Bool.self) { geometry in
            withHeader && geometry.contentOffset.y + geometry.contentInsets.top > 64
        } action: { _, isPastHeader in
            withAnimation(.easeOut(duration: 0.15)) { showsInlineTitle = isPastHeader }
        }
    }
}
#endif

#if os(macOS)
/// The top of a group's tab in a Mac window: the group's strip (icon, faces, who hasn't joined) and the balance card.
struct MacGroupTop: View {
    let header: GroupHeader
    let actions: GroupScreenActions

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                GroupAvatar(initials: header.groupInitials, color: header.groupColor, size: 36)
                    .accessibilityHidden(true)
                GroupFaces(header: header, size: 24, action: actions.members)
                Spacer()
            }
            if let balance = header.myBalance {
                BalanceCard(
                    balance: balance,
                    currency: header.currency,
                    isEveryoneSettled: header.isEveryoneSettled,
                    onSettle: actions.settle,
                )
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
    }
}
#endif
