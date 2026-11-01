import SwiftUI

/// The sections of a group: Expenses, Balances and Members. Each is its own route, so picking one only tells the
/// ViewModel, which replaces the screen in place, with no push (`AppRoute.isGroupTabSwitch`); the picker never moves
/// by itself.
enum GroupTab: Hashable, CaseIterable {
    case expenses
    case balances
    case members

    var title: LocalizedStringKey {
        switch self {
        case .expenses: "Expenses"
        case .balances: "Balances"
        case .members: "Members"
        }
    }
}

/// The segmented control of the canvas, under the group's header on iPhone and iPad and in the toolbar on the Mac.
struct GroupTabsPicker: View {
    let selected: GroupTab
    let onSelect: (GroupTab) -> Void

    var body: some View {
        Picker("Group sections", selection: selection) {
            ForEach(GroupTab.allCases, id: \.self) { tab in
                Text(tab.title).tag(tab)
            }
        }
        .pickerStyle(.segmented)
        .labelsHidden()
    }

    private var selection: Binding<GroupTab> {
        Binding(
            get: { selected },
            set: { tab in
                if tab != selected { onSelect(tab) }
            },
        )
    }
}

#if os(iOS)
/// The picker as its own section of a group's list on iPhone and iPad, with no row inset of its own, so it lines up
/// with the cards below at the list's margin on Expenses, Balances and Members alike.
struct GroupTabsSection: View {
    let selected: GroupTab
    let onSelect: (GroupTab) -> Void

    var body: some View {
        Section {
            GroupTabsPicker(selected: selected, onSelect: onSelect)
        }
        .listRowBackground(Color.clear)
        .listRowInsets(EdgeInsets())
    }
}
#endif
