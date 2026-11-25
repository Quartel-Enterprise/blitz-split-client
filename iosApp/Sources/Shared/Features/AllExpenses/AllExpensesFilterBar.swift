import Shared
import SwiftUI

/// Over "All expenses"' bills (Pierre's call, beta feedback): the order and the filters as a row of menus that scrolls
/// sideways, in `filterOrder` (what the statistics picked first), a menu tinted while its filter narrows the list, and
/// "Clear filters" once one does. The search is the screen's own search field (`.searchable`).
struct AllExpensesFilterBar: View {
    let state: AllExpensesUiState
    let send: (any AllExpensesUiEvent) -> Void

    @State private var isPickingDates = false

    private var filters: AllExpensesFilters { state.filters }

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(state.filterOrder, id: \.self) { filter in
                    menu(filter)
                }
                if filters.isFiltering {
                    Button("Clear filters") { send(AllExpensesUiEventOnClearFiltersClicked.shared) }
                        .buttonStyle(.borderless)
                        .font(.subheadline)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 4)
        }
        .sheet(isPresented: $isPickingDates) {
            let now = ExpenseFormat.millis(of: .now)
            DateRangeSheet(
                startMillis: filters.customStartMillis?.int64Value ?? now,
                endMillis: filters.customEndMillis?.int64Value ?? now,
            ) { start, end in
                isPickingDates = false
                send(AllExpensesUiEventOnCustomPeriodPicked(startMillis: start, endMillis: end))
            }
        }
    }

    @ViewBuilder
    private func menu(_ filter: AllExpensesFilter) -> some View {
        switch filter {
        case .sort: sortMenu
        case .group: groupMenu
        case .category: categoryMenu
        case .period: periodMenu
        case .share: shareMenu
        default: payerMenu
        }
    }

    /// The statistics' periods; "Pick dates…" opens the sheet, and a custom period names its days.
    private var periodMenu: some View {
        Menu {
            ForEach(Self.periods, id: \.self) { period in
                checked(Self.title(period), isOn: filters.period == period) {
                    if period == .custom {
                        isPickingDates = true
                    } else {
                        send(AllExpensesUiEventOnPeriodPicked(period: period))
                    }
                }
            }
        } label: {
            chip(periodText, isActive: filters.period != .all)
        }
    }

    private var periodText: String {
        if filters.period == .custom, let start = filters.customStartMillis, let end = filters.customEndMillis {
            let range = ExpenseFormat.date(of: start.int64Value)..<ExpenseFormat.date(of: end.int64Value)
            return range.formatted(.interval.day().month(.abbreviated).year().appDateFormat())
        }
        return switch filters.period {
        case .thisYear: String(localized: "This year", bundle: .appLanguage)
        case .thisMonth: String(localized: "This month", bundle: .appLanguage)
        case .lastMonth: String(localized: "Last month", bundle: .appLanguage)
        default: String(localized: "Period", bundle: .appLanguage)
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

    /// "Anyone", then the people (Pierre's call, 2026-11-24): the user first, as "You", then everyone of the groups;
    /// several may be picked at once. One picked names the menu ("You paid", "Paid by Ana").
    private var payerMenu: some View {
        let picked = state.payerOptions.filter { filters.payerKeys.contains($0.key) }
        return Menu {
            checked("Anyone", isOn: filters.payerKeys.isEmpty) {
                send(AllExpensesUiEventOnAnyPayerPicked.shared)
            }
            #if os(iOS)
            // "Anyone" lets go of everyone at once, so it closes the menu, as on Android and the web
            .menuActionDismissBehavior(.enabled)
            #endif
            Divider()
            ForEach(state.payerOptions, id: \.key) { person in
                if person.isMe {
                    checked("You", isOn: filters.payerKeys.contains(person.key)) {
                        send(AllExpensesUiEventOnPayerToggled(key: person.key))
                    }
                } else {
                    checked(verbatim: person.name, isOn: filters.payerKeys.contains(person.key)) {
                        send(AllExpensesUiEventOnPayerToggled(key: person.key))
                    }
                }
            }
        } label: {
            chip(payerText(picked), isActive: !filters.payerKeys.isEmpty)
        }
        #if os(iOS)
        // Several may be picked at once, so the menu stays open while they are; the Mac's menus close on each pick
        .menuActionDismissBehavior(.disabled)
        #endif
    }

    /// "Who paid" with nobody picked, "You paid" or "Paid by Ana" with one, "Who paid · 2" with several.
    private func payerText(_ picked: [FilterPayerOption]) -> String {
        let count = filters.payerKeys.count
        if count == 1, let person = picked.first {
            return person.isMe
                ? String(localized: "You paid", bundle: .appLanguage)
                : String(localized: "Paid by \(person.name)", bundle: .appLanguage)
        }
        let label = String(localized: "Who paid", bundle: .appLanguage)
        return count > 1 ? "\(label) · \(count)" : label
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

    private static let periods: [ExpensePeriod] = [.all, .thisYear, .thisMonth, .lastMonth, .custom]
    private static let sorts: [ExpenseSort] = [.newest, .oldest, .largest, .smallest]
    private static let shares: [ShareFilter] = [.all, .toReceive, .toPay]

    private static func title(_ sort: ExpenseSort) -> LocalizedStringKey {
        switch sort {
        case .oldest: "Oldest first"
        case .largest: "Largest first"
        case .smallest: "Smallest first"
        default: "Newest first"
        }
    }

    private static func title(_ period: ExpensePeriod) -> LocalizedStringKey {
        switch period {
        case .thisYear: "This year"
        case .thisMonth: "This month"
        case .lastMonth: "Last month"
        case .custom: "Pick dates…"
        default: "All time"
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

    private static func title(_ share: ShareFilter) -> LocalizedStringKey {
        switch share {
        case .toReceive: "You get back"
        case .toPay: "You owe"
        default: "Any share"
        }
    }
}
