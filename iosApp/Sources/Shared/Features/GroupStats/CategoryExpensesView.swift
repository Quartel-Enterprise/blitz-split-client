import Observation
import Shared
import SwiftUI

@Observable
final class CategoryExpensesModel {
    private(set) var state: CategoryExpensesUiState
    /// "Couldn't delete the bill. Try again.", from a bill's "⋯".
    private(set) var billDeleteErrorCount = 0

    @ObservationIgnored private let holder: ViewModelHolder<CategoryExpensesViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteCategoryExpenses) {
        holder = BlitzSplitApple.shared.createCategoryExpensesViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! CategoryExpensesUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            if action is CategoryExpensesUiActionShowBillDeleteError { self?.billDeleteErrorCount += 1 }
        })
    }

    /// The state as the ViewModel holds it right now, before the watcher brings it to `state`.
    var current: CategoryExpensesUiState {
        (holder.viewModel.uiState.value as? CategoryExpensesUiState) ?? state
    }

    func send(_ event: any CategoryExpensesUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Expenses by category", from a category of the statistics' "By category": the group's bills of the period the
/// statistics showed, of the main categories its chips pick (all of them while none is), by day as the group's
/// Expenses lists them. Pushed over the statistics, in the same column (the iPad's beside the groups too); a tap opens
/// the bill as from Expenses, and its "⋯" (a long press, a right click on the Mac) offers Bill's options as there.
/// Lying down and with an iPad's room the chips and the total keep a column of their own beside the bills.
struct CategoryExpensesView: View {
    let route: RouteCategoryExpenses

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: CategoryExpensesModel?
    @State private var layout = ScreenLayout()
    @Environment(\.windowLayout) private var windowLayout

    #if os(iOS)
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    #endif

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                CategoryExpensesList(model: model, usesSideColumn: usesSideColumn)
            } else {
                ShimmerScreen {
                    VStack(spacing: 12) {
                        ShimmerRow()
                        ShimmerRow()
                        ShimmerRow()
                    }
                }
                .background(Palette.groupedBackground.ignoresSafeArea())
            }
        }
        .measuringScreenLayout($layout)
        .billDeletionAlert(
            model?.state.billToDelete,
            isDeleting: model?.state.isDeletingBill == true,
            onConfirm: { model?.send(CategoryExpensesUiEventOnExpenseDeleteConfirmed.shared) },
            onDismiss: {
                if model?.current.billToDelete != nil { model?.send(CategoryExpensesUiEventOnExpenseDeleteDismissed.shared) }
            },
        )
        .glassToast("Couldn't delete the bill. Try again.", trigger: model?.billDeleteErrorCount ?? 0)
        .navigationTitle("Expenses by category")
        .navigationSubtitle(subtitle)
        #if os(iOS)
        .toolbarTitleDisplayMode(.inline)
        .toolbar(horizontalSizeClass == .compact ? .hidden : .automatic, for: .tabBar)
        #endif
        .onAppear {
            if model == nil { model = CategoryExpensesModel(route: route) }
        }
    }

    /// Lying down or with an iPad's room; the window's until this screen has measured itself, so it never opens in one
    /// column and then splits.
    private var usesSideColumn: Bool {
        let measured = layout.or(windowLayout)
        return measured.usesTwoColumns || measured.isLarge
    }

    /// "Viagem Floripa · October 2026", or "Viagem Floripa · The whole trip" for the whole history.
    private var subtitle: String {
        guard let state = model?.state, state.isLoaded else { return "" }
        let period: String = if let start = state.periodStartMillis?.int64Value, let end = state.periodEndMillis?.int64Value {
            GroupStatsCopy.range(state.periodRangeStyle, start: start, end: end)
        } else if state.isTrip {
            String(localized: "The whole trip", bundle: .appLanguage)
        } else {
            String(localized: "All time", bundle: .appLanguage)
        }
        return "\(state.groupName) · \(period)"
    }
}

private struct CategoryExpensesList: View {
    let model: CategoryExpensesModel
    /// The chips and the total in a column of their own, beside the bills.
    let usesSideColumn: Bool

    private var state: CategoryExpensesUiState {
        model.state
    }

    var body: some View {
        if usesSideColumn {
            TwoColumnsLayout(firstWidth: 300, spacing: 8, maxWidth: 1100) {
                ScrollView {
                    VStack(alignment: .leading, spacing: 14) {
                        CategoryChipsFlow(model: model)
                        total
                    }
                    .padding(.top, 20)
                    .padding(.leading, 20)
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            } second: {
                list {
                    bills
                }
            }
            .background(Palette.groupedBackground.ignoresSafeArea())
        } else {
            list {
                Section {
                    CategoryChipsFlow(model: model)
                        .listRowBackground(Color.clear)
                        .listRowInsets(EdgeInsets(top: 4, leading: 4, bottom: 4, trailing: 4))
                } footer: {
                    total
                }
                bills
            }
        }
    }

    private func list(@ViewBuilder content: () -> some View) -> some View {
        List {
            content()
        }
        #if os(iOS)
        .listStyle(.insetGrouped)
        #else
        .listStyle(.inset)
        #endif
        .animation(.smooth(duration: 0.25), value: state.days.map(\.dayStartMillis))
    }

    /// "R$ 610 · 4 expenses": what the bills shown add up to.
    private var total: some View {
        Text(verbatim: "\(state.currency.format(state.totalMinor)) · \(GroupStatsCopy.expenseCount(state.expenseCount))")
            .font(.headline)
            .foregroundStyle(.primary)
            .monospacedDigit()
            .contentTransition(.numericText())
            .accessibilityAddTraits(.updatesFrequently)
    }

    /// The skeletons, an empty state, or the bills under their day.
    @ViewBuilder
    private var bills: some View {
        if state.isLoadingBills {
            // A new device's first download may still bring these bills: never an empty state before
            GroupLoadingRows(isWaitingForInternet: false, leading: .badge)
        }
        if state.isEmpty {
            Section {
                ContentUnavailableView {
                    Label("No expenses in these categories", systemImage: "line.3.horizontal.decrease.circle")
                } description: {
                    Text("Pick other categories above, or clear them to see every expense of the period.")
                }
            }
            .listRowBackground(Color.clear)
        }
        if state.isPeriodEmpty {
            Section {
                ContentUnavailableView("No expenses in this period", systemImage: "receipt")
            }
            .listRowBackground(Color.clear)
        }
        ForEach(state.days, id: \.dayStartMillis) { day in
            Section(ExpenseFormat.day(day.dayStartMillis, relativeDay: day.relativeDay)) {
                ForEach(day.expenses, id: \.id) { expense in
                    CategoryExpenseLine(expense: expense, currency: state.currency, model: model)
                }
            }
        }
    }
}

/// A bill as the group's Expenses draws it: a tap opens it, its "⋯" and a long press (a right click on the Mac) offer
/// Bill's options without opening it, as on the group's Expenses and the home's latest expenses (beta feedback).
private struct CategoryExpenseLine: View {
    let expense: CategoryExpenseRow
    let currency: GroupCurrency
    let model: CategoryExpensesModel

    var body: some View {
        HStack(spacing: 4) {
            Button {
                model.send(CategoryExpensesUiEventOnExpenseClicked(expenseId: expense.id))
            } label: {
                DayEntryLine(
                    systemImage: "receipt",
                    category: expense.category,
                    title: expense.description_,
                    meta: Self.meta(expense),
                    isPending: expense.isPending,
                ) {
                    Text(verbatim: currency.format(expense.totalMinor))
                        .monospacedDigit()
                    MyBalanceText(minor: expense.myBalanceMinor?.int64Value, currency: currency)
                        .font(.footnote.weight(.semibold))
                }
            }
            .buttonStyle(.plain)
            RowMenuButton(title: expense.description_) { items }
        }
        .contextMenu { items }
    }

    private var items: some View {
        BillMenuItems(
            onEdit: { model.send(CategoryExpensesUiEventOnExpenseEditClicked(expenseId: expense.id)) },
            onDuplicate: { model.send(CategoryExpensesUiEventOnExpenseDuplicateClicked(expenseId: expense.id)) },
            onShare: { model.send(CategoryExpensesUiEventOnExpenseShareClicked(expenseId: expense.id)) },
            onDelete: { model.send(CategoryExpensesUiEventOnExpenseDeleteClicked(expenseId: expense.id)) },
        )
    }

    /// "21:30 · Bruno paid · Restaurant", "17:40 · queued · You paid · Food": the subcategory says which part of the
    /// main category the bill is.
    private static func meta(_ expense: CategoryExpenseRow) -> String {
        let bill = billMeta(
            spentAt: expense.spentAt,
            isPending: expense.isPending,
            payers: expense.payers.map { (name: $0.name, isMe: $0.isMe) },
        )
        return "\(bill) · \(CategoryCopy.name(expense.category))"
    }
}

/// The period's main categories as toggles, the picked ones filled with a check, then "Clear" while one is picked.
private struct CategoryChipsFlow: View {
    let model: CategoryExpensesModel

    var body: some View {
        TermFlowLayout(spacing: 8) {
            ForEach(model.state.chips, id: \.category.id) { chip in
                Button {
                    model.send(CategoryExpensesUiEventOnCategoryToggled(categoryId: chip.category.id))
                } label: {
                    HStack(spacing: 6) {
                        if chip.isSelected {
                            Image(systemName: "checkmark")
                                .font(.caption.weight(.bold))
                        } else {
                            Circle()
                                .fill(chip.category.color.data)
                                .frame(width: 10, height: 10)
                        }
                        Text(verbatim: CategoryCopy.name(chip.category))
                            .font(.subheadline)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 7)
                    .foregroundStyle(chip.isSelected ? Palette.accent : .primary)
                    .background(chip.isSelected ? Palette.accent.opacity(0.16) : Palette.card, in: .capsule)
                    .overlay {
                        Capsule().strokeBorder(chip.isSelected ? Color.clear : Color.secondary.opacity(0.3))
                    }
                    .contentShape(.capsule)
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(chip.isSelected ? .isSelected : [])
            }
            if model.state.showsClear {
                Button("Clear") {
                    model.send(CategoryExpensesUiEventOnClearClicked.shared)
                }
                .font(.subheadline.weight(.semibold))
                .padding(.vertical, 7)
                .buttonStyle(.borderless)
            }
        }
        .animation(.smooth(duration: 0.2), value: model.state.chips.map(\.isSelected))
    }
}
