import Shared
import SwiftUI

/// The group's header as Expenses or Balances last drew it. Switching between a group's tabs only changes what is
/// under them (`AppRoute.isGroupTabSwitch`), so the tab opened draws this header at once while it reads the group,
/// instead of an empty screen. Only right after a tab switch: any other move of the back stack forgets which group
/// may use it, so a group opened later never shows a header older than what the device holds.
enum GroupHeaderMemory {
    private static var headers: [String: RememberedHeader] = [:]
    private static var switchedGroupId: String?

    struct RememberedHeader {
        let header: GroupHeader
        let hasExpenses: Bool
    }

    static func remember(_ header: GroupHeader, hasExpenses: Bool, groupId: String) {
        headers[groupId] = RememberedHeader(header: header, hasExpenses: hasExpenses)
    }

    /// Called by `AppModel` on every move of the back stack: the group whose tab just took another's place, or nil.
    static func backStackMoved(switchingTabOf groupId: String?) {
        switchedGroupId = groupId
    }

    /// The header a tab's bars draw: its own once it has read the group, else the one the tab it replaced drew.
    static func shown(_ header: GroupHeader?, isLoaded: Bool, groupId: String) -> GroupHeader? {
        isLoaded ? header : self.header(of: groupId)?.header
    }

    static func header(of groupId: String) -> RememberedHeader? {
        guard switchedGroupId == groupId else { return nil }
        return headers[groupId]
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
        if let remembered = GroupHeaderMemory.header(of: groupId) {
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
    /// Keeps the header this tab draws, once it has read the group, for the next tab of the group to draw at once.
    func remembersGroupHeader(_ header: GroupHeader?, isLoaded: Bool, hasExpenses: Bool, groupId: String) -> some View {
        onChange(of: isLoaded ? header : nil, initial: true) { _, header in
            guard let header else { return }
            GroupHeaderMemory.remember(header, hasExpenses: hasExpenses, groupId: groupId)
        }
    }
}
