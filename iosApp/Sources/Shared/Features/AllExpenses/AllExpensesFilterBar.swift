import Shared
import SwiftUI

/// Over "All expenses"' bills (Pierre's call, beta feedback): the order and the filters as a row of menus that scrolls
/// sideways, a menu tinted while its filter narrows the list, and "Clear filters" once one does. The search is the
/// screen's own search field (`.searchable`).
struct AllExpensesFilterBar: View {
    let state: AllExpensesUiState
    let send: (any AllExpensesUiEvent) -> Void

    private var filters: AllExpensesFilters { state.filters }

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                sortMenu
                groupMenu
                categoryMenu
                shareMenu
                payerMenu
                if filters.isFiltering {
                    Button("Clear filters") { send(AllExpensesUiEventOnClearFiltersClicked.shared) }
                        .buttonStyle(.borderless)
                        .font(.subheadline)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 4)
        }
    }

    private var sortMenu: some View {
        Menu {
            ForEach(Self.sorts, id: \.self) { sort in
                checked(Self.title(sort), isOn: filters.sort == sort) {
                    send(AllExpensesUiEventOnSortPicked(sort: sort))
                }
            }
        } label: {
            chip(Self.text(filters.sort), systemImage: "arrow.up.arrow.down", isActive: false)
        }
        .accessibilityLabel(Text("Sort"))
    }

    private var groupMenu: some View {
        let picked = state.groupOptions.filter { filters.groupIds.contains($0.id) }
        return Menu {
            ForEach(state.groupOptions, id: \.id) { group in
                checked(verbatim: group.name, isOn: filters.groupIds.contains(group.id)) {
                    send(AllExpensesUiEventOnGroupToggled(groupId: group.id))
                }
            }
        } label: {
            chip(Self.picked(String(localized: "Group", bundle: .appLanguage), names: picked.map(\.name)), isActive: !picked.isEmpty)
        }
        #if os(iOS)
        // Several may be picked at once, so the menu stays open while they are; the Mac's menus close on each pick
        .menuActionDismissBehavior(.disabled)
        #endif
    }

    private var categoryMenu: some View {
        let picked = state.categoryOptions.filter { filters.categoryIds.contains($0.id) }
        return Menu {
            ForEach(state.categoryOptions, id: \.id) { category in
                checked(verbatim: CategoryCopy.name(category), isOn: filters.categoryIds.contains(category.id)) {
                    send(AllExpensesUiEventOnCategoryToggled(categoryId: category.id))
                }
            }
        } label: {
            chip(
                Self.picked(String(localized: "Category", bundle: .appLanguage), names: picked.map(CategoryCopy.name)),
                isActive: !picked.isEmpty,
            )
        }
        #if os(iOS)
        // Several may be picked at once, so the menu stays open while they are; the Mac's menus close on each pick
        .menuActionDismissBehavior(.disabled)
        #endif
    }

    private var shareMenu: some View {
        Menu {
            ForEach(Self.shares, id: \.self) { share in
                checked(Self.title(share), isOn: filters.share == share) {
                    send(AllExpensesUiEventOnShareFilterPicked(share: share))
                }
            }
        } label: {
            chip(
                filters.share == .all ? String(localized: "Your share", bundle: .appLanguage) : Self.text(filters.share),
                isActive: filters.share != .all,
            )
        }
    }

    private var payerMenu: some View {
        Menu {
            ForEach(Self.payers, id: \.self) { payer in
                checked(Self.title(payer), isOn: filters.payer == payer) {
                    send(AllExpensesUiEventOnPayerFilterPicked(payer: payer))
                }
            }
        } label: {
            chip(
                filters.payer == .all ? String(localized: "Who paid", bundle: .appLanguage) : Self.text(filters.payer),
                isActive: filters.payer != .all,
            )
        }
    }

    private func checked(_ title: LocalizedStringKey, isOn: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            if isOn {
                Label(title, systemImage: "checkmark")
            } else {
                Text(title)
            }
        }
    }

    private func checked(verbatim title: String, isOn: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            if isOn {
                Label(title, systemImage: "checkmark")
            } else {
                Text(verbatim: title)
            }
        }
    }

    private func chip(_ title: String, systemImage: String? = nil, isActive: Bool) -> some View {
        HStack(spacing: 4) {
            if let systemImage { Image(systemName: systemImage) }
            Text(verbatim: title)
                .lineLimit(1)
            Image(systemName: "chevron.down")
                .font(.caption2.weight(.semibold))
        }
        .font(.subheadline)
        .padding(.horizontal, 12)
        .padding(.vertical, 7)
        .foregroundStyle(isActive ? Palette.accent : .primary)
        .background(isActive ? Palette.accent.opacity(0.15) : Palette.card, in: .capsule)
        .contentShape(.capsule)
    }

    /// "Group" with nothing picked, the name of the one picked, "Group · 2" with several.
    private static func picked(_ label: String, names: [String]) -> String {
        if names.count == 1 { return names[0] }
        return names.count > 1 ? "\(label) · \(names.count)" : label
    }

    private static let sorts: [ExpenseSort] = [.newest, .oldest, .largest, .smallest]
    private static let shares: [ShareFilter] = [.all, .toReceive, .toPay]
    private static let payers: [PayerFilter] = [.all, .you, .others]

    private static func title(_ sort: ExpenseSort) -> LocalizedStringKey {
        switch sort {
        case .oldest: "Oldest first"
        case .largest: "Largest first"
        case .smallest: "Smallest first"
        default: "Newest first"
        }
    }

    private static func text(_ sort: ExpenseSort) -> String {
        switch sort {
        case .oldest: String(localized: "Oldest first", bundle: .appLanguage)
        case .largest: String(localized: "Largest first", bundle: .appLanguage)
        case .smallest: String(localized: "Smallest first", bundle: .appLanguage)
        default: String(localized: "Newest first", bundle: .appLanguage)
        }
    }

    private static func text(_ share: ShareFilter) -> String {
        switch share {
        case .toReceive: String(localized: "You get back", bundle: .appLanguage)
        case .toPay: String(localized: "You owe", bundle: .appLanguage)
        default: String(localized: "Any share", bundle: .appLanguage)
        }
    }

    private static func text(_ payer: PayerFilter) -> String {
        switch payer {
        case .you: String(localized: "You paid", bundle: .appLanguage)
        case .others: String(localized: "Others paid", bundle: .appLanguage)
        default: String(localized: "Anyone", bundle: .appLanguage)
        }
    }

    private static func title(_ share: ShareFilter) -> LocalizedStringKey {
        switch share {
        case .toReceive: "You get back"
        case .toPay: "You owe"
        default: "Any share"
        }
    }

    private static func title(_ payer: PayerFilter) -> LocalizedStringKey {
        switch payer {
        case .you: "You paid"
        case .others: "Others paid"
        default: "Anyone"
        }
    }
}
