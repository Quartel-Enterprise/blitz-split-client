import Observation
import Shared
import SwiftUI

@Observable
final class GlobalBalancesModel {
    private(set) var state: GlobalBalancesUiState
    private(set) var comingSoonCount = 0
    private(set) var changeErrorCount = 0
    private(set) var copiedCount = 0

    @ObservationIgnored private let holder: ViewModelHolder<GlobalBalancesViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteGlobalBalances) {
        holder = BlitzSplitApple.shared.createGlobalBalancesViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! GlobalBalancesUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            switch action {
            case is GlobalBalancesUiActionShowComingSoon: self?.comingSoonCount += 1
            case is GlobalBalancesUiActionShowChangeError: self?.changeErrorCount += 1
            case let copy as GlobalBalancesUiActionCopyMessage:
                Pasteboard.copy(copy.message)
                self?.copiedCount += 1
            // Enviar is a ShareLink: a share sheet can't be opened from code
            default: break
            }
        })
    }

    /// The state as the ViewModel holds it right now: the message field reads what was typed back at once.
    var current: GlobalBalancesUiState {
        (holder.viewModel.uiState.value as? GlobalBalancesUiState) ?? state
    }

    func send(_ event: any GlobalBalancesUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    var filter: Binding<BalancesFilter> {
        Binding(
            get: { self.state.filter },
            set: { self.send(GlobalBalancesUiEventOnFilterChanged(filter: $0)) },
        )
    }

    var isCharging: Binding<Bool> {
        Binding(
            get: { self.state.charge != nil },
            set: { isShown in
                if !isShown, self.current.charge != nil { self.send(GlobalBalancesUiEventOnChargeDismissed.shared) }
            },
        )
    }

    isolated deinit {
        holder.clear()
    }
}

/// The home's Balances tab: each person once, netted across every group. The overall balance and the filter on top,
/// then whom you owe and who owes you, "By group" opening in the row (the canvas' inspector on iPad and Mac waits for
/// the app to have a list and detail). No "Split a bill": the actions are on each person.
struct GlobalBalancesView: View {
    let route: RouteGlobalBalances

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: GlobalBalancesModel?

    var body: some View {
        Group {
            if let model, model.state.isLoaded {
                GlobalBalancesList(model: model)
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        .navigationTitle("Balances")
        #if os(iOS)
        .toolbarTitleDisplayMode(.inline)
        #else
        .navigationSubtitle(model.map { GlobalBalancesCopy.macSubtitle($0.state) } ?? "")
        #endif
        .toolbar {
            #if os(macOS)
            if let model {
                ToolbarItem(placement: .principal) {
                    FilterPicker(model: model)
                }
            }
            #endif
            ToolbarItem(placement: .primaryAction) {
                Button {
                    model?.send(GlobalBalancesUiEventOnProfileClicked.shared)
                } label: {
                    AvatarPhoto(
                        initial: model?.state.avatarInitial ?? "",
                        color: model?.state.avatarColor ?? .blue,
                        photoUrl: model?.state.photoUrl,
                        size: 32,
                        font: .subheadline.weight(.semibold),
                    )
                }
                .accessibilityLabel("Profile")
            }
        }
        .glassToast([
            GlassToastMessage(text: "Coming soon", trigger: model?.comingSoonCount ?? 0),
            GlassToastMessage(text: "Couldn't save the change. Try again.", trigger: model?.changeErrorCount ?? 0),
            GlassToastMessage(text: "Copied", trigger: model?.copiedCount ?? 0),
        ])
        // Record payment, which Settle up or Got paid opened over Balances, just closed
        .recordedPaymentNotice(
            model?.state.recordedPayment,
            onUndo: { model?.send(GlobalBalancesUiEventOnUndoPaymentClicked.shared) },
            onShown: { model?.send(GlobalBalancesUiEventOnRecordedPaymentNoticeShown.shared) },
        )
        .sheet(isPresented: model?.isCharging ?? .constant(false)) {
            if let model, let charge = model.state.charge {
                ChargeSheet(model: model, charge: charge)
            }
        }
        .onAppear {
            if model == nil { model = GlobalBalancesModel(route: route) }
        }
    }
}

/// "All", "You owe" and "Owe you": a segmented control under the overall balance, in the Mac's toolbar.
private struct FilterPicker: View {
    let model: GlobalBalancesModel

    var body: some View {
        Picker("Show", selection: model.filter) {
            Text("All").tag(BalancesFilter.all)
            Text("You owe").tag(BalancesFilter.youOwe)
            Text("Owe you").tag(BalancesFilter.owesYou)
        }
        .pickerStyle(.segmented)
    }
}

private struct GlobalBalancesList: View {
    let model: GlobalBalancesModel

    private var state: GlobalBalancesUiState {
        model.state
    }

    private var heroHint: LocalizedStringKey {
        state.emptyState == nil ? "already offset across groups" : "adding up every group"
    }

    private func sectionTitle(_ section: PeopleSection) -> LocalizedStringKey {
        section.isYouOwe ? "You owe" : "Owe you"
    }

    var body: some View {
        List {
            #if os(iOS)
            Section {
                VStack(alignment: .leading, spacing: 12) {
                    if state.showsOfflineNotice {
                        Label("Offline · this \(DeviceName.current)'s balances", systemImage: "wifi.slash")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Overall balance")
                            .font(.footnote.weight(.medium))
                            .foregroundStyle(.secondary)
                        ForEach(state.totals, id: \.currency) { total in
                            Text(verbatim: total.currency.formatSigned(total.netMinor))
                                .font(.system(size: 36, weight: .bold))
                                .monospacedDigit()
                                .foregroundStyle(total.netMinor == 0 ? Color.secondary : Palette.accent)
                        }
                        Text(heroHint)
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                    FilterPicker(model: model)
                }
                .padding(.vertical, 4)
            }
            #else
            if state.showsOfflineNotice {
                Section {
                    Label("Offline · this \(DeviceName.current)'s balances", systemImage: "wifi.slash")
                        .foregroundStyle(.secondary)
                }
            }
            #endif
            if let emptyState = state.emptyState {
                Section {
                    EmptyBalances(model: model, emptyState: emptyState, total: state.primaryTotal)
                }
                .listRowBackground(Color.clear)
            }
            ForEach(state.sections, id: \.isYouOwe) { section in
                Section {
                    ForEach(section.rows, id: \.id) { row in
                        PersonRowView(model: model, row: row)
                    }
                } header: {
                    HStack {
                        Text(sectionTitle(section))
                        Spacer()
                        Text(verbatim: section.totals.map { $0.currency.format($0.amountMinor) }.joined(separator: " + "))
                            .monospacedDigit()
                            .textCase(nil)
                    }
                }
            }
        }
        .scrollContentBackground(.hidden)
        .background(Palette.groupedBackground)
    }
}

/// Someone once: the net and whose way it goes, "By group" opening in the row, then Got paid and Request, or Settle up.
private struct PersonRowView: View {
    let model: GlobalBalancesModel
    let row: PersonRow

    private var name: String {
        PaymentCopy.name(row.name)
    }

    private var direction: LocalizedStringKey {
        row.owesYou ? "owes you" : "you owe"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 12) {
                if let color = row.color {
                    AvatarPhoto(initial: row.initials, color: color, photoUrl: nil, size: 40)
                        .accessibilityHidden(true)
                } else {
                    Circle()
                        .fill(.fill.tertiary)
                        .frame(width: 40, height: 40)
                        .accessibilityHidden(true)
                }
                VStack(alignment: .leading, spacing: 1) {
                    Text(verbatim: name)
                        .lineLimit(1)
                    Text(verbatim: GlobalBalancesCopy.meta(row))
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
                Spacer(minLength: 8)
                VStack(alignment: .trailing, spacing: 0) {
                    Text(verbatim: row.currency.formatSigned(row.netMinor))
                        .font(.subheadline.weight(.semibold))
                        .monospacedDigit()
                        .foregroundStyle(row.owesYou ? Palette.accent : Palette.negative)
                    Text(direction)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            if row.isExpanded {
                PersonGroupsList(groups: row.groups, currency: row.currency)
                    .padding(.leading, 52)
                    .accessibilityLabel(Text("\(name) by group"))
            }
            HStack(spacing: 8) {
                Button {
                    model.send(GlobalBalancesUiEventOnPersonExpandToggled(personId: row.id))
                } label: {
                    HStack(spacing: 5) {
                        Text("By group")
                        Image(systemName: row.isExpanded ? "chevron.up" : "chevron.down")
                            .font(.caption.weight(.bold))
                    }
                }
                .buttonStyle(.borderless)
                .accessibilityLabel(row.isExpanded ? Text("Hide \(name) by group") : Text("Show \(name) by group"))
                Spacer()
                if row.owesYou {
                    Button("Got paid") {
                        model.send(GlobalBalancesUiEventOnReceivedClicked(personId: row.id))
                    }
                    .buttonStyle(.borderless)
                    .accessibilityLabel(Text("Got paid by \(name)"))
                    Button("Request") {
                        model.send(GlobalBalancesUiEventOnChargeClicked(personId: row.id))
                    }
                    .buttonStyle(.bordered)
                    .buttonBorderShape(.capsule)
                    .accessibilityLabel(Text("Request from \(name)"))
                } else {
                    Button("Settle up") {
                        model.send(GlobalBalancesUiEventOnSettleClicked(personId: row.id))
                    }
                    .buttonStyle(.borderedProminent)
                    .buttonBorderShape(.capsule)
                    .accessibilityLabel(Text("Settle up with \(name)"))
                }
            }
            .padding(.leading, 52)
            .font(.subheadline.weight(.semibold))
        }
        .padding(.vertical, 4)
    }
}

/// "All settled" leads to Groups, "You owe nothing" to Owe you, and nobody owing you to You owe.
private struct EmptyBalances: View {
    let model: GlobalBalancesModel
    let emptyState: BalancesEmptyState
    let total: PeopleTotal

    var body: some View {
        ContentUnavailableView {
            Label(title, systemImage: "checkmark.circle")
        } description: {
            Text(verbatim: description)
        } actions: {
            Button(action) {
                model.send(GlobalBalancesUiEventOnEmptyActionClicked.shared)
            }
            .buttonStyle(.bordered)
            .buttonBorderShape(.capsule)
        }
    }

    private var title: LocalizedStringKey {
        switch emptyState {
        case .nothingToPay: "You owe nothing"
        case .nothingToReceive: "Nobody owes you"
        default: "All settled"
        }
    }

    private var description: String {
        switch emptyState {
        case .nothingToPay:
            let amount = total.currency.format(total.toReceiveMinor)
            return String(localized: "You've settled up with everyone. You still have \(amount) to get back from \(Int(total.toReceiveCount)) people.")
        case .nothingToReceive:
            let amount = total.currency.format(total.toPayMinor)
            return String(localized: "You still owe \(amount) to \(Int(total.toPayCount)) people.")
        default:
            return String(localized: "Nobody owes you and you owe nobody. When someone adds a bill, the balance shows up here.")
        }
    }

    private var action: LocalizedStringKey {
        switch emptyState {
        case .nothingToPay: "See who owes you"
        case .nothingToReceive: "See who you owe"
        default: "See groups"
        }
    }
}

/// The words of Balances and of Request, composed from the state: the shared code hands over names and amounts.
enum GlobalBalancesCopy {
    /// "Floripa trip" for one group, "3 groups" for more, and "· guest" for someone who never joined.
    static func meta(_ row: PersonRow) -> String {
        let groups = groupsText(names: row.groups.map(\.groupName))
        return row.isGuest ? String(localized: "\(groups) · guest") : groups
    }

    static func groupsText(names: [String]) -> String {
        names.count > 1 ? String(localized: "\(names.count) groups") : (names.first ?? "")
    }

    /// "Overall balance +R$ 186,40 · already offset across groups", under the Mac's title.
    static func macSubtitle(_ state: GlobalBalancesUiState) -> String {
        let net = state.primaryTotal.currency.formatSigned(state.primaryTotal.netMinor)
        return state.emptyState == nil
            ? String(localized: "Overall balance \(net) · already offset across groups")
            : String(localized: "Overall balance \(net) · adding up every group")
    }

    /// Request's message in the device's language, from the debt, the groups and, if wanted, the Pix key; a guest's
    /// also says what went into the bill. Once edited, the text typed.
    static func message(_ charge: ChargeDraft, pixKey: PaymentKey?) -> String {
        if let edited = charge.editedMessage { return edited }
        let name = PaymentCopy.name(charge.name)
        let amount = charge.currency.format(charge.amountMinor)
        let names = ExpenseFormat.names(charge.groupNames)
        let groups = charge.groupNames.count > 1
            ? String(localized: "the groups \(names)")
            : String(localized: "the group \(names)")
        let bills = ExpenseFormat.names(charge.guestBills)
        let body: String
        if !charge.isGuest || charge.guestBills.isEmpty {
            body = String(localized: "Hi \(name)! By the Blitz Split math, you owe me \(amount) from \(groups).")
        } else if let paid = charge.guestPaidMinor?.int64Value {
            let paidText = charge.currency.format(paid)
            body = String(localized: "Hi \(name)! You owe me \(amount) from \(groups): your part of \(bills), minus the \(paidText) you already paid.")
        } else {
            body = String(localized: "Hi \(name)! You owe me \(amount) from \(groups): your part of \(bills).")
        }
        guard charge.includesPixKey, let pixKey else { return body }
        return String(localized: "\(body) Pix: \(pixKey.value)")
    }

    /// "Email · pierre@email.com".
    static func pixKey(_ key: PaymentKey) -> String {
        let type = switch key.type {
        case .cpf: String(localized: "CPF")
        case .phone: String(localized: "Phone")
        case .random: String(localized: "Random key")
        default: String(localized: "Email")
        }
        return "\(type) · \(key.value)"
    }
}

/// Request: a message for whoever owes you, which moves no money and sends nothing by itself. Send opens the share
/// sheet (the Share menu on the Mac) and Copy puts the text on the pasteboard. A sheet over Balances.
private struct ChargeSheet: View {
    let model: GlobalBalancesModel
    let charge: ChargeDraft

    private var name: String {
        PaymentCopy.name(charge.name)
    }

    private var message: String {
        GlobalBalancesCopy.message(model.current.charge ?? charge, pixKey: model.state.pixKey)
    }

    private var messageBinding: Binding<String> {
        Binding(
            get: { message },
            set: { model.send(GlobalBalancesUiEventOnChargeMessageChanged(message: $0)) },
        )
    }

    private var pixBinding: Binding<Bool> {
        Binding(
            get: { charge.includesPixKey },
            set: { model.send(GlobalBalancesUiEventOnChargePixKeyToggled(isIncluded: $0)) },
        )
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    HStack(spacing: 12) {
                        AvatarPhoto(initial: charge.initials, color: charge.color ?? .blue, photoUrl: nil, size: 36)
                            .accessibilityHidden(true)
                        Text("\(name) owes you \(Text(verbatim: charge.currency.format(charge.amountMinor)).foregroundStyle(Palette.accent).bold()) · \(GlobalBalancesCopy.groupsText(names: charge.groupNames))")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    if charge.isGuest {
                        Label {
                            Text("\(name) is a guest without the app. They only learn about it from this message, so it already says what went into the bill.")
                                .fixedSize(horizontal: false, vertical: true)
                        } icon: {
                            Image(systemName: "info.circle")
                        }
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    }
                }
                Section {
                    TextField("Message", text: messageBinding, axis: .vertical)
                        .lineLimit(3...8)
                    if let pixKey = model.state.pixKey {
                        Toggle(isOn: pixBinding) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Include my Pix key")
                                Text(verbatim: GlobalBalancesCopy.pixKey(pixKey))
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .disabled(!charge.canTogglePixKey)
                    }
                }
            }
            .formStyle(.grouped)
            .safeAreaInset(edge: .bottom) {
                HStack(spacing: 10) {
                    Button {
                        model.send(GlobalBalancesUiEventOnChargeCopyClicked(message: message))
                    } label: {
                        Text("Copy message")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    ShareLink(item: message) {
                        Label("Send", systemImage: "square.and.arrow.up")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                }
                .controlSize(.large)
                .buttonBorderShape(.capsule)
                .padding(.horizontal, 20)
                .padding(.bottom, 8)
            }
            .navigationTitle(Text("Request from \(name)"))
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", systemImage: "xmark", role: .cancel) {
                        model.send(GlobalBalancesUiEventOnChargeDismissed.shared)
                    }
                }
            }
        }
        #if os(iOS)
        .presentationDetents([.medium, .large])
        #else
        .frame(width: 480, height: 460)
        #endif
    }
}
