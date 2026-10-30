import Observation
import Shared
import SwiftUI

@Observable
final class GroupDetailModel {
    private(set) var state: GroupDetailUiState
    private(set) var comingSoonCount = 0
    private(set) var billDeletedCount = 0

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
            case is GroupDetailUiActionShowComingSoon: self?.comingSoonCount += 1
            default: break
            }
        })
    }

    private func showBillDeleted() {
        billDeletedCount += 1
        send(GroupDetailUiEventOnBillDeletedNoticeShown.shared)
    }

    func send(_ event: any GroupDetailUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    /// Expenses is this screen: Balances answers "Coming soon" and Members takes its place.
    func select(_ tab: GroupTab) {
        switch tab {
        case .balances: send(GroupDetailUiEventOnBalancesTabClicked.shared)
        case .members: send(GroupDetailUiEventOnMembersClicked.shared)
        case .expenses: break
        }
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Expenses" of a group, pushed over the groups: the group's header, its tabs and the bills by day. On iPhone
/// "Split a bill" is the bottom toolbar's prominent glass item (iOS has no FAB), with the glass buttons at the top
/// on iPad and lying down; on the Mac it is the window toolbar's primary button, with the tabs in the toolbar and
/// the bills in a table.
struct GroupDetailView: View {
    let route: RouteGroupDetail

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: GroupDetailModel?
    @State private var showsInlineTitle = false

    #if os(iOS)
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.verticalSizeClass) private var verticalSizeClass
    #endif

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                #if os(macOS)
                MacGroupExpenses(model: model)
                #else
                PhoneGroupExpenses(
                    model: model,
                    usesTwoColumns: verticalSizeClass == .compact,
                    showsInlineTitle: $showsInlineTitle,
                )
                #endif
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        .navigationTitle(Text(verbatim: model?.state.groupName ?? ""))
        #if os(iOS)
        .toolbarTitleDisplayMode(.inline)
        // On iPhone the group's own bar replaces the app's tabs, as the canvas draws it
        .toolbar(horizontalSizeClass == .compact ? .hidden : .automatic, for: .tabBar)
        #else
        .navigationSubtitle(model.map { groupMeta($0.state) } ?? "")
        #endif
        .toolbar { toolbar }
        .glassToast("Coming soon", trigger: model?.comingSoonCount ?? 0)
        .glassToast("This bill was deleted", trigger: model?.billDeletedCount ?? 0)
        .onAppear {
            if model == nil { model = GroupDetailModel(route: route) }
        }
    }

    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        #if os(iOS)
        // The large title shows the name; the bar only names the group once it scrolls away
        ToolbarItem(placement: .principal) {
            Text(verbatim: model?.state.groupName ?? "")
                .font(.headline)
                .lineLimit(1)
                .opacity(showsInlineTitle ? 1 : 0)
                .accessibilityAddTraits(.isHeader)
        }
        #else
        ToolbarItem(placement: .principal) {
            GroupTabsPicker(selected: .expenses) { model?.select($0) }
                .disabled(model?.state.isLoaded != true)
        }
        #endif
        ToolbarItem(placement: .primaryAction) {
            Button("Invite people", systemImage: "person.badge.plus") {
                model?.send(GroupDetailUiEventOnInviteClicked.shared)
            }
            .disabled(model?.state.isLoaded != true)
        }
        ToolbarItem(placement: .primaryAction) { groupMenu }
        if model?.state.showsSplitBillButton == true {
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

    /// "⋯": only what changes the group itself (6.1i); Leave and Delete wait for the balances. The Mac's items
    /// that open a dialog end in "…".
    private var groupMenu: some View {
        Menu("More group options", systemImage: "ellipsis") {
            Button(editGroupTitle, systemImage: "pencil") {
                model?.send(GroupDetailUiEventOnEditGroupClicked.shared)
            }
            Divider()
            Button("Leave group", systemImage: "rectangle.portrait.and.arrow.right") {
                model?.send(GroupDetailUiEventOnLeaveGroupClicked.shared)
            }
            Button("Delete group", systemImage: "trash", role: .destructive) {
                model?.send(GroupDetailUiEventOnDeleteGroupClicked.shared)
            }
        }
        .disabled(model?.state.isLoaded != true)
    }

    private var editGroupTitle: LocalizedStringKey {
        #if os(macOS)
        "Edit group…"
        #else
        "Edit group"
        #endif
    }

    private var splitBillButton: some View {
        Button {
            model?.send(GroupDetailUiEventOnSplitBillClicked.shared)
        } label: {
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
private func groupMeta(_ state: GroupDetailUiState) -> String {
    let members = String(localized: "\(Int(state.memberCount)) members")
    let spent = state.isEmpty
        ? String(localized: "no expenses yet")
        : String(localized: "\(state.currency.format(state.totalSpentMinor)) in total")
    return "\(members) · \(spent)"
}

/// The faces of the group, "+N" for the rest, and "1 hasn't joined yet": a tap opens Members.
private struct GroupFaces: View {
    let state: GroupDetailUiState
    let size: CGFloat
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                HStack(spacing: -size * 0.25) {
                    ForEach(Array(state.faces.enumerated()), id: \.offset) { _, face in
                        AvatarPhoto(initial: face.initials, color: face.color, photoUrl: nil, size: size)
                            .overlay(Circle().strokeBorder(Palette.groupedBackground, lineWidth: 2))
                    }
                    if state.extraMemberCount > 0 {
                        Text(verbatim: "+\(state.extraMemberCount)")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.secondary)
                            .frame(minWidth: size, minHeight: size)
                            .padding(.horizontal, 4)
                            .background(.fill.tertiary, in: .capsule)
                    }
                }
                if state.notJoinedCount > 0 {
                    Text("\(Int(state.notJoinedCount)) hasn't joined yet")
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
        let members = String(localized: "\(Int(state.memberCount)) members")
        guard state.notJoinedCount > 0 else { return Text(verbatim: members) }
        return Text(verbatim: "\(members), \(String(localized: "\(Int(state.notJoinedCount)) hasn't joined yet"))")
    }
}

/// The bill's line on the list: "21:30 · Bruno paid", "17:40 · queued · You paid", or just the time once whoever
/// paid left the group.
private func expenseMeta(_ row: ExpenseRow) -> String {
    let time = ExpenseFormat.time(row.spentAt)
    let when = row.isPending ? String(localized: "\(time) · queued") : time
    if row.payerIsMe {
        return "\(when) · \(String(localized: "You paid"))"
    } else if let payerName = row.payerName {
        return "\(when) · \(String(localized: "\(payerName) paid"))"
    }
    return when
}

private func payerText(_ row: ExpenseRow) -> String {
    if row.payerIsMe { return String(localized: "You paid") }
    if let payerName = row.payerName { return String(localized: "\(payerName) paid") }
    return ""
}

/// No categories yet: every bill gets the same receipt.
struct ExpenseIcon: View {
    var size: CGFloat = 38

    var body: some View {
        Image(systemName: "receipt")
            .font(.system(size: size * 0.45, weight: .medium))
            .foregroundStyle(Palette.accent)
            .frame(width: size, height: size)
            .background(Palette.accent.opacity(0.12), in: .rect(cornerRadius: size * 0.3))
            .accessibilityHidden(true)
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
/// iPhone upright and iPad: one column (640 wide at most on iPad). iPhone lying down: the header on the left, the
/// tabs and the bills on the right.
private struct PhoneGroupExpenses: View {
    let model: GroupDetailModel
    let usesTwoColumns: Bool
    @Binding var showsInlineTitle: Bool

    private var state: GroupDetailUiState {
        model.state
    }

    var body: some View {
        Group {
            if usesTwoColumns {
                HStack(alignment: .top, spacing: 8) {
                    ScrollView {
                        header
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

    private var header: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 14) {
                GroupAvatar(initials: state.groupInitials, color: state.groupColor, size: 56)
                VStack(alignment: .leading, spacing: 2) {
                    Text(verbatim: state.groupName)
                        .font(.title.bold())
                        .lineLimit(2)
                        .accessibilityAddTraits(.isHeader)
                    Text(verbatim: groupMeta(state))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            GroupFaces(state: state, size: 28) {
                model.send(GroupDetailUiEventOnMembersClicked.shared)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func list(withHeader: Bool) -> some View {
        List {
            if withHeader {
                Section {
                    header
                }
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets(top: 4, leading: 20, bottom: 4, trailing: 20))
            }
            Section {
                GroupTabsPicker(selected: .expenses) { model.select($0) }
            }
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 0, trailing: 20))
            if state.isEmpty {
                Section {
                    EmptyExpenses { model.send($0) }
                }
                .listRowBackground(Color.clear)
            }
            ForEach(state.days, id: \.dayStartMillis) { day in
                Section(ExpenseFormat.day(day.dayStartMillis, relativeDay: day.relativeDay)) {
                    ForEach(day.expenses, id: \.id) { row in
                        Button {
                            model.send(GroupDetailUiEventOnExpenseClicked(expenseId: row.id))
                        } label: {
                            PhoneExpenseRow(row: row, currency: state.currency)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
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

private struct PhoneExpenseRow: View {
    let row: ExpenseRow
    let currency: GroupCurrency

    var body: some View {
        HStack(spacing: 12) {
            ExpenseIcon()
            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: row.description_)
                    .lineLimit(1)
                HStack(spacing: 6) {
                    if row.isPending {
                        Circle()
                            .fill(.orange)
                            .frame(width: 8, height: 8)
                            .accessibilityHidden(true)
                    }
                    Text(verbatim: expenseMeta(row))
                        .lineLimit(1)
                }
                .font(.footnote)
                .foregroundStyle(.secondary)
            }
            Spacer(minLength: 8)
            VStack(alignment: .trailing, spacing: 2) {
                Text(verbatim: currency.format(row.totalMinor))
                    .monospacedDigit()
                MyBalanceText(minor: row.myBalanceMinor?.int64Value, currency: currency)
                    .font(.footnote.weight(.semibold))
            }
        }
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
    }
}
#endif

#if os(macOS)
extension ExpenseRow: @retroactive Identifiable {}

/// The Mac window: the group's strip (icon, faces, who hasn't joined) over a table of the bills by day.
private struct MacGroupExpenses: View {
    let model: GroupDetailModel

    @State private var selection = Set<String>()

    private var state: GroupDetailUiState {
        model.state
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 12) {
                GroupAvatar(initials: state.groupInitials, color: state.groupColor, size: 36)
                    .accessibilityHidden(true)
                GroupFaces(state: state, size: 24) {
                    model.send(GroupDetailUiEventOnMembersClicked.shared)
                }
                Spacer()
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            if state.isEmpty {
                EmptyExpenses { model.send($0) }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                table
            }
        }
        .background(Palette.groupedBackground)
    }

    private var table: some View {
        Table(of: ExpenseRow.self, selection: $selection) {
            TableColumn("Expense") { row in
                HStack(spacing: 8) {
                    ExpenseIcon(size: 24)
                    Text(verbatim: row.description_)
                        .lineLimit(1)
                    if row.isPending {
                        HStack(spacing: 4) {
                            Circle()
                                .fill(.orange)
                                .frame(width: 6, height: 6)
                            Text("queued")
                        }
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    }
                }
            }
            TableColumn("Time") { row in
                Text(verbatim: ExpenseFormat.time(row.spentAt))
                    .monospacedDigit()
                    .foregroundStyle(.secondary)
            }
            .width(min: 50, ideal: 60, max: 80)
            TableColumn("Who paid") { row in
                Text(verbatim: payerText(row))
                    .foregroundStyle(.secondary)
            }
            TableColumn("Total") { row in
                Text(verbatim: state.currency.format(row.totalMinor))
                    .monospacedDigit()
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
            .width(min: 90, ideal: 110)
            TableColumn("Your share") { row in
                MyBalanceText(minor: row.myBalanceMinor?.int64Value, currency: state.currency)
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
            .width(min: 100, ideal: 130)
        } rows: {
            ForEach(state.days, id: \.dayStartMillis) { day in
                Section(ExpenseFormat.day(day.dayStartMillis, relativeDay: day.relativeDay)) {
                    ForEach(day.expenses, id: \.id) { row in
                        TableRow(row)
                    }
                }
            }
        }
        .contextMenu(forSelectionType: String.self) { _ in
            EmptyView()
        } primaryAction: { ids in
            if let id = ids.first {
                model.send(GroupDetailUiEventOnExpenseClicked(expenseId: id))
            }
        }
    }
}
#endif
