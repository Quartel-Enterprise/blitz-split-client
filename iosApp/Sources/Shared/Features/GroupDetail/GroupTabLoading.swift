import Shared
import SwiftUI

/// What Expenses or Balances draw around their content once they have read the group: the header and the bars' state.
struct GroupTabPage: Equatable {
    let header: GroupHeader
    let hasExpenses: Bool
    let showsSplitBill: Bool
}

/// The page Expenses or Balances last drew for each group. Switching between a group's tabs only changes what is
/// under them (`AppRoute.isGroupTabSwitch`), so the tab opened draws the page the tab left drew at once while it reads
/// the group, instead of an empty screen. Only right after a switch from Expenses or Balances, the tabs that draw this
/// page (Members draws its own), and only while a tab of the group is on the back stack, so a group opened later never
/// shows a page older than what the device holds.
enum GroupHeaderMemory {
    private static var pages: [String: GroupTabPage] = [:]
    private static var switchedGroupId: String?

    static func remember(_ page: GroupTabPage, groupId: String) {
        pages[groupId] = page
    }

    /// Called by `AppModel` on every move of the back stack, from the entry that was on top to the new entries.
    static func backStackMoved(from left: AppRoute?, to routes: [AppRoute]) {
        switchedGroupId = left.flatMap { left in
            routes.last.flatMap { top in
                drawsPage(left) && left.isGroupTabSwitch(to: top) ? top.tabGroupId : nil
            }
        }
        let groupIds = Set(routes.compactMap(\.tabGroupId))
        pages = pages.filter { groupIds.contains($0.key) }
    }

    static func page(of groupId: String) -> GroupTabPage? {
        guard switchedGroupId == groupId else { return nil }
        return pages[groupId]
    }

    private static func drawsPage(_ route: AppRoute) -> Bool {
        switch route {
        case .groupDetail, .balances: true
        default: false
        }
    }
}

/// What Expenses and Balances draw until they have read the group: the header of the tab they replaced, with this
/// tab picked and nothing under it, or the empty background when they were opened any other way.
struct GroupTabLoading: View {
    let groupId: String
    let tab: GroupTab

    #if os(iOS)
    @Environment(\.verticalSizeClass) private var verticalSizeClass
    #endif

    var body: some View {
        if let remembered = GroupHeaderMemory.page(of: groupId) {
            #if os(macOS)
            VStack(alignment: .leading, spacing: 0) {
                MacGroupTop(header: remembered.header, actions: .disabled)
                Spacer(minLength: 0)
            }
            .background(Palette.groupedBackground)
            #else
            PhoneGroupScaffold(
                header: remembered.header,
                hasExpenses: remembered.hasExpenses,
                tab: tab,
                usesTwoColumns: verticalSizeClass == .compact,
                showsInlineTitle: .constant(false),
                actions: .disabled,
            ) {
                EmptyView()
            }
            #endif
        } else {
            Palette.groupedBackground.ignoresSafeArea()
        }
    }
}

extension View {
    /// The group screen's bars for Expenses or Balances: drawn from the tab's own page once it has read the group, else
    /// from the page the tab it replaced drew, which it keeps for the next tab of the group once it has its own.
    func groupTabBars(
        tab: GroupTab,
        host: AppRoute,
        loaded: GroupTabPage?,
        groupId: String,
        showsInlineTitle: Bool,
        exit: GroupExitState?,
        actions: GroupScreenActions,
    ) -> some View {
        let page = loaded ?? GroupHeaderMemory.page(of: groupId)
        return onChange(of: loaded, initial: true) { _, loaded in
            guard let loaded else { return }
            GroupHeaderMemory.remember(loaded, groupId: groupId)
        }
        .groupScreenBars(
            tab: tab,
            host: host,
            header: page?.header,
            hasExpenses: page?.hasExpenses == true,
            showsSplitBill: page?.showsSplitBill == true,
            showsInlineTitle: showsInlineTitle,
            exit: exit,
            actions: actions,
        )
    }
}
