import Observation
import Shared
import SwiftUI

@Observable
final class GroupDetailModel {
    private(set) var state: GroupDetailUiState
    private(set) var leaveErrorCount = 0
    private(set) var deleteErrorCount = 0
    private(set) var changeErrorCount = 0
    private(set) var billDeletedCount = 0
    private(set) var teachErrorCount = 0

    @ObservationIgnored private let holder: ViewModelHolder<GroupDetailViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteGroupDetail) {
        holder = BlitzSplitApple.shared.createGroupDetailViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! GroupDetailUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
            // State, not an action, so it waits for this screen while the bill's screen still covers it
            if state.showsBillDeletedNotice { self?.showBillDeleted() }
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            switch action {
            case is GroupDetailUiActionShowLeaveError: self?.leaveErrorCount += 1
            case is GroupDetailUiActionShowDeleteError: self?.deleteErrorCount += 1
            case is GroupDetailUiActionShowChangeError: self?.changeErrorCount += 1
            case is GroupDetailUiActionShowTeachError: self?.teachErrorCount += 1
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
            confirmExit: { [weak self] in self?.send(GroupDetailUiEventOnGroupExitConfirmed.shared) },
            dismissExit: { [weak self] in self?.send(GroupDetailUiEventOnGroupExitDismissed.shared) },
            backToGroups: { [weak self] in self?.send(GroupDetailUiEventOnBackToGroupsClicked.shared) },
        )
    }

    /// "Delete this payment?" closed without "Delete" (Cancel, Esc, a tap outside on iPad).
    func dismissPaymentDeletion() {
        if current.paymentToDelete != nil { send(GroupDetailUiEventOnPaymentDeleteDismissed.shared) }
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
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        .groupScreenBars(
            tab: model?.tab ?? GroupTab(route.tab),
            host: host,
            header: model?.state.isLoaded == true ? model?.state.header : nil,
            hasExpenses: model?.state.hasBills == true,
            showsSplitBill: model?.state.showsSplitBillButton == true,
            addPerson: addPerson,
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
        .glassToast([
            GlassToastMessage(text: "This bill was deleted", trigger: model?.billDeletedCount ?? 0),
        ] + GroupExitCopy.errorToasts(
            leave: model?.leaveErrorCount ?? 0,
            delete: model?.deleteErrorCount ?? 0,
            change: model?.changeErrorCount ?? 0,
        ))
        .recordedPaymentNotice(
            model?.state.recordedPayment,
            onUndo: { model?.send(GroupDetailUiEventOnUndoPaymentClicked.shared) },
            onShown: { model?.send(GroupDetailUiEventOnRecordedPaymentNoticeShown.shared) },
        )
        .glassToast("Couldn't teach the map. Try again.", trigger: model?.teachErrorCount ?? 0)
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
        .onAppear {
            if model == nil { model = GroupDetailModel(route: route) }
            if members == nil { members = MembersModel(route: route) }
        }
    }

    /// Members' "Add person", once its people are read.
    private var addPerson: (() -> Void)? {
        guard model?.tab == .members, let members, members.state.isLoaded else { return nil }
        return { members.send(MembersUiEventOnAddClicked.shared) }
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
    let time = ExpenseFormat.time(row.spentAt)
    let when = row.isPending ? String(localized: "\(time) · queued") : time
    guard let paid = payerText(row) else { return when }
    return "\(when) · \(paid)"
}

private func payerText(_ row: ExpenseRow) -> String? {
    ExpenseFormat.paid(row.payers.map { (name: $0.name, isMe: $0.isMe) }, loneFormer: nil)
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

/// "+R$ 600,00" in the accent, "−R$ 118,00" in the negative color; nothing when the bill leaves this person out.
private struct MyBalanceText: View {
    let minor: Int64?
    let currency: GroupCurrency

    var body: some View {
        if let minor {
            Text(verbatim: currency.formatSigned(minor))
                .monospacedDigit()
                .foregroundStyle(minor >= 0 ? Palette.accent : Palette.negative)
        }
    }
}

/// The orange dot of what is saved here and not on the server yet.
private struct PendingDot: View {
    var size: CGFloat = 8

    var body: some View {
        Circle()
            .fill(.orange)
            .frame(width: size, height: size)
            .accessibilityHidden(true)
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
        ForEach(state.days, id: \.dayStartMillis) { day in
            Section(ExpenseFormat.day(day.dayStartMillis, relativeDay: day.relativeDay)) {
                ForEach(day.entries, id: \.id) { entry in
                    Button {
                        model.open(entry)
                    } label: {
                        if let payment = entry as? PaymentRow {
                            PhonePaymentRow(row: payment, currency: state.header.currency)
                        } else if let expense = entry as? ExpenseRow {
                            PhoneExpenseRow(row: expense, currency: state.header.currency)
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}

/// An entry of the day: its icon, a title and a meta line (with the orange dot while queued), and its amounts.
private struct PhoneDayRow<Amounts: View>: View {
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

private struct PhoneExpenseRow: View {
    let row: ExpenseRow
    let currency: GroupCurrency

    var body: some View {
        PhoneDayRow(systemImage: "receipt", category: row.category, title: row.description_, meta: expenseMeta(row), isPending: row.isPending) {
            Text(verbatim: currency.format(row.totalMinor))
                .monospacedDigit()
            MyBalanceText(minor: row.myBalanceMinor?.int64Value, currency: currency)
                .font(.footnote.weight(.semibold))
        }
    }
}

/// "Ana paid you · 12:10 PM · Payment · Pix · $50.00 · payment"; a tap asks to delete it.
private struct PhonePaymentRow: View {
    let row: PaymentRow
    let currency: GroupCurrency

    var body: some View {
        PhoneDayRow(
            systemImage: "banknote",
            title: PaymentCopy.title(row),
            meta: PaymentCopy.meta(row),
            isPending: row.isPending,
        ) {
            Text(verbatim: currency.format(row.amountMinor))
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
            MacGroupTop(header: model.state.header, actions: model.actions)
            switch model.tab {
            case .expenses: MacGroupExpenses(model: model)
            case .balances:
                List {
                    BalancesSections(model: model)
                }
                .scrollContentBackground(.hidden)
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
                    Text(verbatim: item.expense?.description_ ?? item.payment.map(PaymentCopy.title) ?? "")
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
            TableColumn("Who paid") { item in
                Text(verbatim: item.expense.flatMap(payerText) ?? item.payment.map(PaymentCopy.kindAndMethod) ?? "")
                    .foregroundStyle(.secondary)
            }
            TableColumn("Total") { item in
                Text(verbatim: currency.format(item.expense?.totalMinor ?? item.payment?.amountMinor ?? 0))
                    .monospacedDigit()
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
            .width(min: 90, ideal: 110)
            TableColumn("Your share") { item in
                MyBalanceText(minor: item.expense?.myBalanceMinor?.int64Value, currency: currency)
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
            .width(min: 100, ideal: 130)
        } rows: {
            ForEach(state.days, id: \.dayStartMillis) { day in
                Section(ExpenseFormat.day(day.dayStartMillis, relativeDay: day.relativeDay)) {
                    ForEach(day.entries.map(DayEntryItem.init)) { item in
                        TableRow(item)
                    }
                }
            }
        }
        .contextMenu(forSelectionType: String.self) { _ in
            EmptyView()
        } primaryAction: { ids in
            let entries = state.days.flatMap(\.entries)
            if let id = ids.first, let entry = entries.first(where: { $0.id == id }) {
                model.open(entry)
            }
        }
    }
}
#endif
