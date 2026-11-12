import Observation
import Shared
import SwiftUI

@Observable
final class AllExpensesModel {
    private(set) var state: AllExpensesUiState
    /// "Couldn't delete the bill. Try again.", from a bill's "⋯".
    private(set) var billDeleteErrorCount = 0

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createAllExpensesViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! AllExpensesUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            if action is AllExpensesUiActionShowBillDeleteError { self?.billDeleteErrorCount += 1 }
        })
    }

    /// The state as the ViewModel holds it right now, before the watcher brings it to `state`.
    var current: AllExpensesUiState {
        (holder.viewModel.uiState.value as? AllExpensesUiState) ?? state
    }

    /// The search field's text, which finds bills by their description as the home's search does.
    var query: Binding<String> {
        Binding(
            get: { self.current.filters.query },
            set: { self.send(AllExpensesUiEventOnQueryChanged(query: $0)) },
        )
    }

    func send(_ event: any AllExpensesUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// "All expenses", which Home's "See everything" pushes over it: every bill you paid or take part in, across groups,
/// by day, drawn as Home's rows with their "⋯" (a long press, a right click on the Mac). A page on every size, one
/// column centered on an iPad and a wide Mac window; the next page is read as the last row comes into view.
struct AllExpensesView: View {
    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: AllExpensesModel?

    var body: some View {
        Group {
            if let model, !model.state.showsSkeleton {
                content(model)
            } else {
                ShimmerScreen {
                    ShimmerLine(width: 80, height: 10)
                    ShimmerList(count: 6, leading: .badge)
                }
                .background(Palette.groupedBackground.ignoresSafeArea())
            }
        }
        .searchable(text: model?.query ?? .constant(""), prompt: Text("Search expenses"))
        .navigationTitle(Text("All expenses"))
        #if os(iOS)
        .toolbarTitleDisplayMode(.inline)
        #endif
        .billDeletionAlert(
            model?.state.billToDelete,
            isDeleting: model?.state.isDeletingBill == true,
            onConfirm: { model?.send(BillMenuEventOnDeleteConfirmed.shared) },
            onDismiss: {
                if model?.current.billToDelete != nil { model?.send(BillMenuEventOnDeleteDismissed.shared) }
            },
        )
        .glassToast("Couldn't delete the bill. Try again.", trigger: model?.billDeleteErrorCount ?? 0)
        .onAppear {
            if model == nil { model = AllExpensesModel() }
        }
    }

    @ViewBuilder
    private func content(_ model: AllExpensesModel) -> some View {
        if model.state.showsEmpty {
            ContentUnavailableView {
                Label("No expenses yet", systemImage: "receipt")
            } description: {
                Text("Bills you paid or take part in, in any group, show up here.")
            }
            .background(Palette.groupedBackground.ignoresSafeArea())
        } else {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 0) {
                    AllExpensesFilterBar(state: model.state, send: model.send)
                        .padding(.horizontal, -16)
                    if model.state.showsNoResults {
                        ContentUnavailableView {
                            Label("No expenses match", systemImage: "magnifyingglass")
                        } description: {
                            Text("Try other words or clear the filters.")
                        } actions: {
                            Button("Clear filters") { model.send(AllExpensesUiEventOnClearFiltersClicked.shared) }
                        }
                    }
                    ForEach(model.state.days, id: \.dayStartMillis) { day in
                        // Sorted by amount the bills read as one list, with no day over them
                        if model.state.showsDayHeadings {
                            Text(verbatim: ExpenseFormat.day(day.dayStartMillis, relativeDay: day.relativeDay))
                                .font(.footnote)
                                .textCase(.uppercase)
                                .foregroundStyle(.secondary)
                                .padding(.horizontal, 20)
                                .padding(.top, 14)
                                .padding(.bottom, 6)
                                .accessibilityAddTraits(.isHeader)
                        }
                        dayCard(day, model: model)
                            .padding(.top, model.state.showsDayHeadings ? 0 : 12)
                    }
                    if model.state.showsMoreSkeleton {
                        ShimmerList(count: 2, leading: .badge)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 4)
                            .shimmering()
                            .background(Palette.card, in: .rect(cornerRadius: 26))
                            .padding(.top, 8)
                    }
                }
                .frame(maxWidth: ScreenLayout.singleColumnMaxWidth)
                .padding(.horizontal, 16)
                .padding(.bottom, 24)
                .frame(maxWidth: .infinity)
            }
            .background(Palette.groupedBackground.ignoresSafeArea())
        }
    }

    /// One day's bills in a card, a hairline between them, as on Home; the last bill read asks for the next page.
    private func dayCard(_ day: HomeBillDay, model: AllExpensesModel) -> some View {
        let lastId = model.state.days.last?.bills.last?.id
        return VStack(spacing: 0) {
            ForEach(Array(day.bills.enumerated()), id: \.element.id) { index, bill in
                HomeBillRow(
                    bill: bill,
                    onOpen: { model.send(AllExpensesUiEventOnBillClicked(groupId: bill.groupId, expenseId: bill.id)) },
                    sendMenu: { model.send($0) },
                )
                .onAppear {
                    if bill.id == lastId, model.state.hasMore { model.send(AllExpensesUiEventOnEndReached.shared) }
                }
                if index < day.bills.count - 1 {
                    // Under the text: 16 of padding, the 38 pt badge and 12 of spacing
                    Divider()
                        .padding(.leading, 66)
                }
            }
        }
        .background(Palette.card, in: .rect(cornerRadius: 26))
    }
}
