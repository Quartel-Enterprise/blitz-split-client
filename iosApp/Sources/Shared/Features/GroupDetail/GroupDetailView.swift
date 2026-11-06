import Observation
import Shared
import SwiftUI

@Observable
final class GroupDetailModel {
    private(set) var state: GroupDetailUiState
    private(set) var leaveErrorCount = 0
    private(set) var changeErrorCount = 0
    private(set) var billDeletedCount = 0
    private(set) var teachErrorCount = 0
    private(set) var billDeleteErrorCount = 0

    @ObservationIgnored private let holder: ViewModelHolder<GroupDetailViewModel>
    @ObservationIgnored private let watches = WatchBag()
    /// The payment the details sheet showed last, drawn while the sheet slides away after the ViewModel closed it.
    @ObservationIgnored private(set) var lastShownPayment: PaymentRow?
    /// The payment whose details' "Delete" was tapped, asked about once the details sheet is gone.
    @ObservationIgnored private var paymentToAskAfterDetails: String?

    init(route: RouteGroupDetail) {
        holder = BlitzSplitApple.shared.createGroupDetailViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! GroupDetailUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
            if let shown = state.paymentToView { self?.lastShownPayment = shown }
            // State, not an action, so it waits for this screen while the bill's screen still covers it
            if state.showsBillDeletedNotice { self?.showBillDeleted() }
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            switch action {
            case is GroupDetailUiActionShowLeaveError: self?.leaveErrorCount += 1
            case is GroupDetailUiActionShowChangeError: self?.changeErrorCount += 1
            case is GroupDetailUiActionShowTeachError: self?.teachErrorCount += 1
            case is GroupDetailUiActionShowBillDeleteError: self?.billDeleteErrorCount += 1
            default: break
            }
        })
    }

    /// The state as the ViewModel holds it right now, before the watcher brings it to `state`: "Delete" has
    /// already closed the confirmation by the time the alert reports itself closed.
    var current: GroupDetailUiState {
        (holder.viewModel.uiState.value as? GroupDetailUiState) ?? state
    }

    private func showBillDeleted() {
        billDeletedCount += 1
        send(GroupDetailUiEventOnBillDeletedNoticeShown.shared)
    }

    func send(_ event: any GroupDetailUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    /// The tab picked, which the ViewModel keeps: the back stack never moves with it.
    var tab: GroupTab {
        GroupTab(state.tab)
    }

    var actions: GroupScreenActions {
        GroupScreenActions(
            select: { [weak self] in self?.send(GroupDetailUiEventOnTabSelected(tab: $0.shared)) },
            invite: { [weak self] in self?.send(GroupDetailUiEventOnInviteClicked.shared) },
            stats: { [weak self] in self?.send(GroupDetailUiEventOnStatsClicked.shared) },
            editGroup: { [weak self] in self?.send(GroupDetailUiEventOnEditGroupClicked.shared) },
            togglePin: { [weak self] in self?.send(GroupDetailUiEventOnPinToggled.shared) },
            leaveGroup: { [weak self] in self?.send(GroupDetailUiEventOnLeaveGroupClicked.shared) },
            deleteGroup: { [weak self] in self?.send(GroupDetailUiEventOnDeleteGroupClicked.shared) },
            splitBill: { [weak self] in self?.send(GroupDetailUiEventOnSplitBillClicked.shared) },
            settle: { [weak self] in self?.send(GroupDetailUiEventOnSettleClicked.shared) },
            selectCurrency: { [weak self] in self?.send(GroupDetailUiEventOnCurrencySelected(currency: $0)) },
            confirmExit: { [weak self] in self?.send(GroupDetailUiEventOnGroupExitConfirmed.shared) },
            dismissExit: { [weak self] in self?.send(GroupDetailUiEventOnGroupExitDismissed.shared) },
            backToGroups: { [weak self] in self?.send(GroupDetailUiEventOnBackToGroupsClicked.shared) },
        )
    }

    /// "Delete this payment?" closed without "Delete" (Cancel, Esc, a tap outside on iPad).
    func dismissPaymentDeletion() {
        if current.paymentToDelete != nil { send(GroupDetailUiEventOnPaymentDeleteDismissed.shared) }
    }

    /// A payment's details closed (Done, a swipe down, a tap outside on iPad).
    func dismissPaymentDetails() {
        if current.paymentToView != nil { send(GroupDetailUiEventOnPaymentDetailsDismissed.shared) }
    }

    /// "Delete" in a payment's details: the sheet closes first and the question comes once it is gone
    /// (`paymentDetailsDidDismiss`), since an alert asked while a sheet is still leaving the screen may never show.
    func deletePaymentFromDetails(_ payment: PaymentRow) {
        paymentToAskAfterDetails = payment.id
        dismissPaymentDetails()
    }

    /// The details sheet finished leaving the screen: asks "Delete this payment?" if its "Delete" closed it.
    func paymentDetailsDidDismiss() {
        guard let paymentId = paymentToAskAfterDetails else { return }
        paymentToAskAfterDetails = nil
        send(GroupDetailUiEventOnPaymentDeleteClicked(paymentId: paymentId))
    }

    /// "Delete this bill?" closed without "Delete", from a bill's options on the list.
    func dismissBillDeletion() {
        if current.billToDelete != nil { send(GroupDetailUiEventOnExpenseDeleteDismissed.shared) }
    }

    /// A line's "Delete": a payment asks "Delete this payment?", a bill "Delete this bill?".
    func delete(_ entry: any DayEntry) {
        if entry is PaymentRow {
            send(GroupDetailUiEventOnPaymentDeleteClicked(paymentId: entry.id))
        } else {
            send(GroupDetailUiEventOnExpenseDeleteClicked(expenseId: entry.id))
        }
    }

    func open(_ entry: any DayEntry) {
        if entry is PaymentRow {
            send(GroupDetailUiEventOnPaymentClicked(paymentId: entry.id))
        } else {
            send(GroupDetailUiEventOnExpenseClicked(expenseId: entry.id))
        }
    }

    isolated deinit {
        holder.clear()
    }
}

/// A group, pushed over the groups: its header, its balance card and its tabs drawn once, over the tab picked:
/// Expenses (the bills and payments by day), Balances (who owes whom) or Members. Picking a tab only swaps the
/// sections under the tabs. On the Mac the tabs are in the toolbar and the bills in a table.
struct GroupDetailView: View {
    let route: RouteGroupDetail

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: GroupDetailModel?
    /// Members' own ViewModel, made with the group's, so the people are already read when the tab is picked.
    @State private var members: MembersModel?
    @State private var showsInlineTitle = false

    #if os(iOS)
    @Environment(\.verticalSizeClass) private var verticalSizeClass
    #endif

    private var host: AppRoute {
        .groupDetail(route)
    }

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                #if os(macOS)
                MacGroupScreen(model: model, members: members, host: host)
                #else
                PhoneGroupScaffold(
                    header: model.state.header,
                    isMyBalanceLoading: model.state.isMyBalanceLoading,
                    hasExpenses: model.state.hasBills,
                    tab: model.tab,
                    usesTwoColumns: verticalSizeClass == .compact,
                    showsInlineTitle: $showsInlineTitle,
                    actions: model.actions,
                ) {
                    switch model.tab {
                    case .expenses: PhoneExpenseSections(model: model)
                    case .balances: BalancesSections(model: model)
                    case .members:
                        if let members { MembersSections(model: members, host: host) }
                    }
                }
                #endif
            } else {
                // The group not read yet: its header, its balance and its bills shimmer in place
                ShimmerScreen {
                    HStack(spacing: 14) {
                        ShimmerBlock(width: 56, height: 56, cornerRadius: 18)
                        VStack(alignment: .leading, spacing: 8) {
                            ShimmerLine(width: 180, height: 22)
                            ShimmerLine(width: 120, height: 12)
                        }
                    }
                    ShimmerCard(showsButtons: false)
                        .background(Palette.card, in: .rect(cornerRadius: 26))
                    ShimmerBlock(height: 32, cornerRadius: 16)
                    ShimmerList(count: 4, leading: .badge)
                }
                .background(Palette.groupedBackground.ignoresSafeArea())
            }
        }
        .groupScreenBars(
            tab: model?.tab ?? GroupTab(route.tab),
            host: host,
            header: model?.state.isLoaded == true ? model?.state.header : nil,
            hasExpenses: model?.state.hasBills == true,
            showsSplitBill: model?.state.showsSplitBillButton == true,
            showsInlineTitle: showsInlineTitle,
            exit: model?.state.exit,
            actions: model?.actions ?? .disabled,
        )
        .membersOverlays(members)
        .alert("Delete this payment?", isPresented: paymentDeletionShown) {
            Button(role: .destructive) {
                model?.send(GroupDetailUiEventOnPaymentDeleteConfirmed.shared)
            } label: {
                Text(BillCopy.delete)
            }
            Button("Cancel", role: .cancel) {
                model?.dismissPaymentDeletion()
            }
        } message: {
            Text("The payment leaves the group and the debt is back in the balances.")
        }
        // A bill's "Delete bill" on the list asks what Bill asks, in the same words
        .billDeletionAlert(
            model?.state.billToDelete,
            isDeleting: model?.state.isDeletingBill == true,
            onConfirm: { model?.send(GroupDetailUiEventOnExpenseDeleteConfirmed.shared) },
            onDismiss: { model?.dismissBillDeletion() },
        )
        .glassToast([
            GlassToastMessage(text: "This bill was deleted", trigger: model?.billDeletedCount ?? 0),
            GlassToastMessage(text: "Couldn't delete the bill. Try again.", trigger: model?.billDeleteErrorCount ?? 0),
        ] + GroupExitCopy.errorToasts(
            leave: model?.leaveErrorCount ?? 0,
            change: model?.changeErrorCount ?? 0,
        ))
        .recordedPaymentNotice(
            model?.state.recordedPayment,
            onUndo: { model?.send(GroupDetailUiEventOnUndoPaymentClicked.shared) },
            onShown: { model?.send(GroupDetailUiEventOnRecordedPaymentNoticeShown.shared) },
        )
        .glassToast("Couldn't teach the map. Try again.", trigger: model?.teachErrorCount ?? 0)
        // "Ana turned it off after you" (13.1e), on whichever of the group's tabs is in front
        .simplifyConflictBanner(
            model?.state.simplifyConflict,
            onActivity: { model?.send(GroupDetailUiEventOnSimplifyConflictActivityClicked.shared) },
            onDismiss: { model?.send(GroupDetailUiEventOnSimplifyConflictDismissed.shared) },
        )
        .safeAreaInset(edge: .bottom) {
            // "Teach the map?" belongs to Expenses, the tab the bill landed on
            if let model, model.tab == .expenses, let teach = model.state.teach, !teach.isPickingWord {
                CategoryTeachBanner(teach: teach, model: model)
            }
        }
        .sheet(isPresented: teachShown) {
            if let model, let teach = model.state.teach {
                NavigationStack { CategoryTeachSheet(teach: teach, model: model) }
                    .formSheetSizing()
            }
        }
        .sheet(isPresented: paymentDetailsShown, onDismiss: { model?.paymentDetailsDidDismiss() }) {
            if let model, let payment = model.state.paymentToView ?? model.lastShownPayment {
                NavigationStack {
                    PaymentDetailsSheet(payment: payment, model: model)
                }
                .formSheetSizing()
            }
        }
        .onAppear {
            if model == nil { model = GroupDetailModel(route: route) }
            if members == nil { members = MembersModel(route: route) }
        }
    }

    /// "Teach the map" (9.3f) while the person picks the word; closing it any way is "Not now".
    private var teachShown: Binding<Bool> {
        Binding(
            get: { model?.state.teach?.isPickingWord == true },
            set: { shown in
                if !shown, model?.current.teach != nil { model?.send(GroupDetailUiEventOnTeachDismissed.shared) }
            },
        )
    }

    /// A payment's details, closed any way in the ViewModel too.
    private var paymentDetailsShown: Binding<Bool> {
        Binding(
            get: { model?.state.paymentToView != nil },
            set: { shown in
                if !shown { model?.dismissPaymentDetails() }
            },
        )
    }

    /// Dismissing the alert any other way than "Delete" closes the confirmation in the ViewModel too.
    private var paymentDeletionShown: Binding<Bool> {
        Binding(
            get: { model?.state.paymentToDelete != nil },
            set: { shown in
                if !shown { model?.dismissPaymentDeletion() }
            },
        )
    }
}

/// Right after joining through the link (3.3), over the group's bills: "You joined as Bruno", and "Protect your
/// access" while the account has no Google or Apple, which opens the profile. A list's section shows them as two rows.
private struct JoinedNotice: View {
    let name: String
    let showsProtectAccess: Bool
    let onProtectAccess: () -> Void

    var body: some View {
        Label {
            VStack(alignment: .leading, spacing: 2) {
                Text("You joined as \(name)")
                    .font(.headline)
                Text("Everything added for you shows up here.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        } icon: {
            Image(systemName: "checkmark.circle.fill")
                .foregroundStyle(Palette.accent)
        }
        .accessibilityElement(children: .combine)
        if showsProtectAccess {
            Button(action: onProtectAccess) {
                Label {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Protect your access")
                            .font(.headline)
                        Text("You joined without an account. Save the code or connect Google or Apple.")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                } icon: {
                    Image(systemName: "lock.shield")
                        .foregroundStyle(Palette.accent)
                }
                .foregroundStyle(.primary)
            }
            #if os(macOS)
            .buttonStyle(.plain)
            #endif
        }
    }
}

/// The bill's line on the list: "21:30 · Bruno paid", "17:40 · queued · You paid", "21:30 · You and Ana paid", or
/// just the time once whoever paid alone left the group.
private func expenseMeta(_ row: ExpenseRow) -> String {
    billMeta(spentAt: row.spentAt, isPending: row.isPending, payers: row.payers.map { (name: $0.name, isMe: $0.isMe) })
}

private func payerText(_ row: ExpenseRow) -> String? {
    ExpenseFormat.paid(row.payers.map { (name: $0.name, isMe: $0.isMe) }, loneFormer: nil)
}

/// A bill's meta on a list of bills by day (the group's Expenses, a category's from the statistics).
func billMeta(spentAt: Int64, isPending: Bool, payers: [(name: String?, isMe: Bool)]) -> String {
    let time = ExpenseFormat.time(spentAt)
    let when = isPending ? String(localized: "\(time) · queued", bundle: .appLanguage) : time
    guard let paid = ExpenseFormat.paid(payers, loneFormer: nil) else { return when }
    return "\(when) · \(paid)"
}

/// A payment's banknote (a bill draws its category's badge instead).
struct ExpenseIcon: View {
    var size: CGFloat = 38
    var systemName = "receipt"

    var body: some View {
        Image(systemName: systemName)
            .font(.system(size: size * 0.45, weight: .medium))
            .foregroundStyle(Palette.accent)
            .frame(width: size, height: size)
            .background(Palette.accent.opacity(0.12), in: .rect(cornerRadius: size * 0.3))
            .accessibilityHidden(true)
    }
}

/// The small "payment" tag next to a payment's amount.
private struct PaymentTag: View {
    var body: some View {
        Text("payment")
            .font(.caption2.weight(.semibold))
            .foregroundStyle(Palette.accent)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(Palette.accent.opacity(0.12), in: .capsule)
    }
}

/// "+R$ 600,00" in the positive green, "−R$ 118,00" in the negative color, zero muted; nothing when the bill leaves
/// this person out.
struct MyBalanceText: View {
    let minor: Int64?
    let currency: GroupCurrency

    var body: some View {
        if let minor {
            Text(verbatim: currency.formatSigned(minor))
                .monospacedDigit()
                .foregroundStyle(Palette.balance(minor))
        }
    }
}

private struct EmptyExpenses: View {
    let send: (any GroupDetailUiEvent) -> Void

    var body: some View {
        ContentUnavailableView {
            Label("No expenses yet", systemImage: "receipt")
        } description: {
            Text("Add the first expense or invite who's splitting with you. People without the app count too.")
        } actions: {
            VStack(spacing: 10) {
                PrimaryButton(title: "Split a bill") {
                    send(GroupDetailUiEventOnSplitBillClicked.shared)
                }
                TintedButton(title: "Invite people") {
                    send(GroupDetailUiEventOnInviteClicked.shared)
                }
            }
            // A list row only offers the buttons their ideal width: they keep the empty state's own
            .frame(minWidth: 260, maxWidth: 320)
        }
    }
}

/// An entry of the day (the group's Expenses, a category's bills from the statistics): its icon, a title and a meta line (with the amber dot while queued), and its amounts.
struct DayEntryLine<Amounts: View>: View {
    let systemImage: String
    /// A bill's category, drawn in place of the icon (6.1); nil for a payment.
    var category: Shared.Category?
    let title: String
    let meta: String
    let isPending: Bool
    @ViewBuilder let amounts: Amounts

    var body: some View {
        HStack(spacing: 12) {
            if let category {
                CategoryBadge(category: category, size: 38)
            } else {
                ExpenseIcon(systemName: systemImage)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: title)
                    .lineLimit(1)
                HStack(spacing: 6) {
                    if isPending { PendingDot() }
                    Text(verbatim: meta)
                        .lineLimit(1)
                }
                .font(.footnote)
                .foregroundStyle(.secondary)
            }
            Spacer(minLength: 8)
            VStack(alignment: .trailing, spacing: 2) {
                amounts
            }
        }
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
    }
}

#if os(iOS)
/// The empty state, or the bills and payments under their day.
private struct PhoneExpenseSections: View {
    let model: GroupDetailModel

    private var state: GroupDetailUiState {
        model.state
    }

    var body: some View {
        if let joinedAs = state.joinedAs {
            Section {
                JoinedNotice(name: joinedAs, showsProtectAccess: state.showsProtectAccess) {
                    model.send(GroupDetailUiEventOnProtectAccessClicked.shared)
                }
            }
        }
        if state.isEmpty {
            Section {
                EmptyExpenses { model.send($0) }
            }
            .listRowBackground(Color.clear)
        }
        if state.isLoadingBills {
            GroupLoadingRows(isWaitingForInternet: state.isWaitingForInternet, leading: .badge)
        }
        ForEach(state.days, id: \.dayStartMillis) { day in
            Section(ExpenseFormat.day(day.dayStartMillis, relativeDay: day.relativeDay)) {
                ForEach(day.entries, id: \.id) { entry in
                    // A tap opens the line; its "⋯" and a long press offer its options without opening it
                    HStack(spacing: 4) {
                        Button {
                            model.open(entry)
                        } label: {
                            if let payment = entry as? PaymentRow {
                                PhonePaymentRow(row: payment)
                            } else if let expense = entry as? ExpenseRow {
                                PhoneExpenseRow(row: expense)
                            }
                        }
                        .buttonStyle(.plain)
                        DayEntryMenuButton(entry: entry, model: model)
                    }
                    .contextMenu {
                        DayEntryMenuItems(entry: entry, model: model)
                    }
                }
            }
        }
    }
}

private struct PhoneExpenseRow: View {
    let row: ExpenseRow

    var body: some View {
        DayEntryLine(systemImage: "receipt", category: row.category, title: row.description_, meta: expenseMeta(row), isPending: row.isPending) {
            Text(verbatim: row.currency.format(row.totalMinor))
                .monospacedDigit()
            MyBalanceText(minor: row.myBalanceMinor?.int64Value, currency: row.currency)
                .font(.footnote.weight(.semibold))
        }
    }
}

/// "Ana paid you · 12:10 PM · Payment · Pix · $50.00 · payment"; a tap opens its details.
private struct PhonePaymentRow: View {
    let row: PaymentRow

    var body: some View {
        DayEntryLine(
            systemImage: "banknote",
            title: PaymentCopy.title(row),
            meta: PaymentCopy.meta(row),
            isPending: row.isPending,
        ) {
            Text(verbatim: row.currency.format(row.amountMinor))
                .monospacedDigit()
            PaymentTag()
        }
    }
}
#endif

#if os(macOS)
/// A table row holds one type: a bill or a payment of the day.
private struct DayEntryItem: Identifiable {
    let entry: any DayEntry

    var id: String {
        entry.id
    }

    var expense: ExpenseRow? {
        entry as? ExpenseRow
    }

    var payment: PaymentRow? {
        entry as? PaymentRow
    }

    var time: Int64 {
        expense?.spentAt ?? payment?.paidAt ?? 0
    }

    var isPending: Bool {
        expense?.isPending ?? payment?.isPending ?? false
    }
}

/// The Mac window: the group's strip and its balance card, drawn once, over the tab picked.
private struct MacGroupScreen: View {
    let model: GroupDetailModel
    let members: MembersModel?
    let host: AppRoute

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            MacGroupTop(
                header: model.state.header,
                isMyBalanceLoading: model.state.isMyBalanceLoading,
                tab: model.tab,
                actions: model.actions,
            )
            switch model.tab {
            case .expenses: MacGroupExpenses(model: model)
            case .balances:
                // Drawn in the window's middle, as Expenses' empty state, not as a row of the list
                if model.state.isBalancesEmpty {
                    NoBalancesYet()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List {
                        BalancesSections(model: model)
                    }
                    .scrollContentBackground(.hidden)
                }
            case .members:
                List {
                    if let members { MembersSections(model: members, host: host) }
                }
                .scrollContentBackground(.hidden)
                .frame(maxWidth: ScreenLayout.twoColumnsMaxWidth)
                .frame(maxWidth: .infinity)
            }
        }
        .background(Palette.groupedBackground)
    }
}

/// Expenses in a Mac window: a table of the bills and payments by day.
private struct MacGroupExpenses: View {
    let model: GroupDetailModel

    @State private var selection = Set<String>()
    @Environment(\.isNarrowWindow) private var isNarrowWindow

    private var state: GroupDetailUiState {
        model.state
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if let joinedAs = state.joinedAs {
                VStack(alignment: .leading, spacing: 10) {
                    JoinedNotice(name: joinedAs, showsProtectAccess: state.showsProtectAccess) {
                        model.send(GroupDetailUiEventOnProtectAccessClicked.shared)
                    }
                }
                .padding(14)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Palette.card, in: .rect(cornerRadius: 12))
                .padding(.horizontal, 20)
                .padding(.bottom, 12)
            }
            if state.isEmpty {
                EmptyExpenses { model.send($0) }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if state.isLoadingBills {
                // Bills may still be coming with the account's first full sync: never "No expenses yet" before them
                List {
                    GroupLoadingRows(isWaitingForInternet: state.isWaitingForInternet, leading: .badge)
                }
                .scrollContentBackground(.hidden)
            } else {
                table
            }
        }
    }

    private var currency: GroupCurrency {
        state.header.currency
    }

    private var table: some View {
        Table(of: DayEntryItem.self, selection: $selection) {
            TableColumn("Expense") { item in
                HStack(spacing: 8) {
                    if let expense = item.expense {
                        CategoryBadge(category: expense.category, size: 24)
                    } else {
                        ExpenseIcon(size: 24, systemName: "banknote")
                    }
                    Text(verbatim: DayEntryCopy.title(item.entry))
                        .lineLimit(1)
                    if item.payment != nil {
                        PaymentTag()
                    }
                    if item.isPending {
                        HStack(spacing: 4) {
                            PendingDot(size: 6)
                            Text("queued")
                        }
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    }
                }
            }
            TableColumn("Time") { item in
                Text(verbatim: ExpenseFormat.time(item.time))
                    .monospacedDigit()
                    .foregroundStyle(.secondary)
            }
            .width(min: 50, ideal: 60, max: 80)
            // A narrow window keeps Who paid out, as the board's narrow window draws it
            if !isNarrowWindow {
                TableColumn("Who paid") { item in
                    Text(verbatim: item.expense.flatMap(payerText) ?? item.payment.map(PaymentCopy.kindAndMethod) ?? "")
                        .foregroundStyle(.secondary)
                }
            }
            TableColumn("Total") { item in
                Text(verbatim: (item.expense?.currency ?? item.payment?.currency ?? currency)
                    .format(item.expense?.totalMinor ?? item.payment?.amountMinor ?? 0))
                    .monospacedDigit()
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
            .width(min: 90, ideal: 110)
            TableColumn("Your share") { item in
                MyBalanceText(minor: item.expense?.myBalanceMinor?.int64Value, currency: item.expense?.currency ?? currency)
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
            .width(min: 100, ideal: 130)
            // The line's "⋯", which a right click on it opens too
            TableColumn(Text(verbatim: "")) { item in
                DayEntryMenuButton(entry: item.entry, model: model)
            }
            .width(32)
        } rows: {
            ForEach(state.days, id: \.dayStartMillis) { day in
                Section(ExpenseFormat.day(day.dayStartMillis, relativeDay: day.relativeDay)) {
                    ForEach(day.entries.map(DayEntryItem.init)) { item in
                        TableRow(item)
                    }
                }
            }
        }
        .contextMenu(forSelectionType: String.self) { ids in
            // One line's options; several picked at once have none in common
            if ids.count == 1, let entry = entry(ids.first) {
                DayEntryMenuItems(entry: entry, model: model)
            }
        } primaryAction: { ids in
            if let entry = entry(ids.first) { model.open(entry) }
        }
        // Delete (⌫) asks what the line's "Delete…" asks, for one line picked
        .onDeleteCommand {
            if selection.count == 1, let entry = entry(selection.first) { model.delete(entry) }
        }
    }

    /// The bill or payment of a line, nil once it left the list.
    private func entry(_ id: String?) -> (any DayEntry)? {
        guard let id else { return nil }
        for day in state.days {
            if let entry = day.entries.first(where: { $0.id == id }) { return entry }
        }
        return nil
    }
}
#endif
