import Observation
import Shared
import SwiftUI

@Observable
final class ContactProfileModel {
    private(set) var state: ContactProfileUiState
    private(set) var comingSoonCount = 0
    private(set) var changeErrorCount = 0
    private(set) var cancelMergeErrorCount = 0
    private(set) var copiedCount = 0
    private(set) var addedCount = 0
    /// "Carla joined Apê 302.", kept while the toast shows.
    private(set) var addedText: LocalizedStringKey = ""

    @ObservationIgnored private let holder: ViewModelHolder<ContactProfileViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteContactProfile) {
        holder = BlitzSplitApple.shared.createContactProfileViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! ContactProfileUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            switch action {
            case is ContactProfileUiActionShowComingSoon: self?.comingSoonCount += 1
            case is ContactProfileUiActionShowChangeError: self?.changeErrorCount += 1
            case is ContactProfileUiActionShowCancelMergeError: self?.cancelMergeErrorCount += 1
            case let copy as ContactProfileUiActionCopyLink:
                Pasteboard.copy(copy.link)
                self?.copiedCount += 1
            case let added as ContactProfileUiActionShowAddedToGroup:
                self?.addedText = "\(added.name) joined \(added.groupName)."
                self?.addedCount += 1
            // Share is a ShareLink: a share sheet can't be opened from code
            default: break
            }
        })
    }

    /// The state as the ViewModel holds it right now, so a dismissal after a button's own event sends nothing more.
    var current: ContactProfileUiState {
        (holder.viewModel.uiState.value as? ContactProfileUiState) ?? state
    }

    func send(_ event: any ContactProfileUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    /// "Hide Felipe?" is open; Esc or a tap outside closes it in the ViewModel too.
    var isHideConfirmShown: Binding<Bool> {
        Binding(
            get: { self.state.isHideConfirmOpen },
            set: { isShown in
                if !isShown, self.current.isHideConfirmOpen { self.send(ContactProfileUiEventOnHideDismissed.shared) }
            },
        )
    }

    var isAddingToGroup: Binding<Bool> {
        Binding(
            get: { self.state.addToGroup != nil },
            set: { isShown in
                if !isShown, self.current.addToGroup != nil { self.send(ContactProfileUiEventOnAddToGroupDismissed.shared) }
            },
        )
    }

    var isMerging: Binding<Bool> {
        Binding(
            get: { self.state.merge != nil },
            set: { isShown in
                if !isShown, self.current.merge != nil { self.send(ContactProfileUiEventOnMergeDismissed.shared) }
            },
        )
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Contact profile" (11.2): the net with the person across the groups and its actions, the groups in common with
/// what each adds, and the latest bills and payments with them. Pushed over Contacts on an iPhone, an iPad upright and
/// a narrow Mac window; in the column beside the list lying down (`isInPane`), where it has no back button and its
/// header is a row. The ⋯ menu holds Merge (a guest), See in Balances and Hide or Show.
struct ContactProfileView: View {
    let route: RouteContactProfile
    var isInPane = false

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: ContactProfileModel?

    var body: some View {
        Group {
            if let model, model.state.isLoaded, let contact = model.state.contact {
                ContactProfileContent(model: model, contact: contact, isInPane: isInPane)
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        .navigationTitle(Text(verbatim: model?.state.contact?.name ?? ""))
        #if os(iOS)
        .toolbarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            if let model, let contact = model.state.contact {
                ToolbarItem(placement: .primaryAction) {
                    ContactProfileMenu(model: model, contact: contact)
                }
            }
        }
        .alert(
            Text("Hide \(model?.state.contact?.name ?? "")?"),
            isPresented: model?.isHideConfirmShown ?? .constant(false),
        ) {
            Button("Cancel", role: .cancel) {
                model?.send(ContactProfileUiEventOnHideDismissed.shared)
            }
            Button("Hide") {
                model?.send(ContactProfileUiEventOnHideConfirmed.shared)
            }
        } message: {
            if let contact = model?.state.contact {
                let groups = ExpenseFormat.names(contact.groups.map(\.groupName))
                Text("\(contact.name) leaves your Contacts. They stay in \(groups) and come back by themselves if they join a new account with you. You can show them again in Hidden.")
            }
        }
        .sheet(isPresented: model?.isAddingToGroup ?? .constant(false)) {
            if let model, let draft = model.state.addToGroup, let contact = model.state.contact {
                AddToGroupSheet(model: model, draft: draft, contact: contact)
            }
        }
        .sheet(isPresented: model?.isMerging ?? .constant(false)) {
            if let model, let merge = model.state.merge, let contact = model.state.contact {
                MergeGuestSheet(model: model, sheet: merge, contact: contact)
            }
        }
        // The counts only grow on an action: `?? 0` keeps the model's arrival (nil to 0) from showing a toast.
        .glassToast([
            GlassToastMessage(text: "Coming soon", trigger: model?.comingSoonCount ?? 0),
            GlassToastMessage(text: model?.addedText ?? "", trigger: model?.addedCount ?? 0),
            GlassToastMessage(text: "Couldn't cancel the invite. Try again.", trigger: model?.cancelMergeErrorCount ?? 0),
            GlassToastMessage(text: "Couldn't save the change. Try again.", trigger: model?.changeErrorCount ?? 0),
        ])
        .onAppear {
            if model == nil { model = ContactProfileModel(route: route) }
        }
    }
}

/// The ⋯ menu: Merge with their account for a guest (online only), See in Balances, and Hide (only at zero) or Show.
private struct ContactProfileMenu: View {
    let model: ContactProfileModel
    let contact: Contact

    private var state: ContactProfileUiState {
        model.state
    }

    var body: some View {
        Menu("More options", systemImage: "ellipsis") {
            if contact.isGuest {
                Button {
                    model.send(ContactProfileUiEventOnMergeClicked.shared)
                } label: {
                    Label {
                        Text(mergeTitle)
                        if !state.isOnline {
                            Text("Needs internet")
                        }
                    } icon: {
                        Image(systemName: "person.2")
                    }
                }
                .disabled(!state.canMerge)
            }
            Button("See in Balances", systemImage: "creditcard") {
                model.send(ContactProfileUiEventOnViewBalancesClicked.shared)
            }
            if state.canShow {
                Button("Show in Contacts", systemImage: "eye") {
                    model.send(ContactProfileUiEventOnShowClicked.shared)
                }
            } else {
                Button {
                    model.send(ContactProfileUiEventOnHideClicked.shared)
                } label: {
                    Label {
                        Text(hideTitle)
                        if !state.canHide {
                            Text("Only with a zero balance. Settle up first.")
                        }
                    } icon: {
                        Image(systemName: "eye.slash")
                    }
                }
                .disabled(!state.canHide)
            }
        }
    }

    /// The Mac's menu items that open a dialog end in "…".
    private var mergeTitle: LocalizedStringKey {
        #if os(macOS)
        "Merge with their account…"
        #else
        "Merge with their account"
        #endif
    }

    private var hideTitle: LocalizedStringKey {
        #if os(macOS)
        "Hide from Contacts…"
        #else
        "Hide from Contacts"
        #endif
    }
}

private struct ContactProfileContent: View {
    let model: ContactProfileModel
    let contact: Contact
    let isInPane: Bool

    private var state: ContactProfileUiState {
        model.state
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                ContactProfileHeader(state: state, contact: contact, isRow: isInPane)
                if state.showsOfflineNotice {
                    Label("Offline · what you do goes into the queue", systemImage: "wifi.slash")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                if state.showsGuestCard {
                    GuestCard(model: model, contact: contact)
                }
                if let pending = state.pendingMerge, state.showsPendingMerge {
                    PendingMergeCard(model: model, contact: contact, pending: pending)
                }
                NetCard(model: model, contact: contact)
                GroupsInCommon(contact: contact)
                if !state.history.isEmpty {
                    ContactHistoryList(model: model, contact: contact)
                }
            }
            .padding(20)
            .frame(maxWidth: isInPane ? nil : ScreenLayout.singleColumnMaxWidth)
            .frame(maxWidth: .infinity)
        }
        .scrollContentBackground(.hidden)
        .background(Palette.groupedBackground)
    }
}

/// The face, the name and whether they use the app: centered alone, a row beside the list.
private struct ContactProfileHeader: View {
    let state: ContactProfileUiState
    let contact: Contact
    let isRow: Bool

    var body: some View {
        let layout = isRow
            ? AnyLayout(HStackLayout(spacing: 14))
            : AnyLayout(VStackLayout(spacing: 8))
        layout {
            AvatarPhoto(initial: state.initials, color: contact.color, photoUrl: nil, size: isRow ? 56 : 88)
                .accessibilityHidden(true)
            VStack(alignment: isRow ? .leading : .center, spacing: 4) {
                Text(verbatim: contact.name)
                    .font(.title2.weight(.bold))
                    .lineLimit(1)
                    .accessibilityAddTraits(.isHeader)
                Text(verbatim: ContactsCopy.profileMeta(contact))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .padding(.horizontal, contact.isGuest ? 12 : 0)
                    .padding(.vertical, contact.isGuest ? 3 : 0)
                    .background(contact.isGuest ? AnyShapeStyle(.fill.tertiary) : AnyShapeStyle(Color.clear), in: .capsule)
            }
        }
        .frame(maxWidth: .infinity, alignment: isRow ? .leading : .center)
    }
}

/// "Diego doesn't use Blitz Split yet": a guest is only a name in the groups, so the profile offers to merge them with
/// their account (online only) or to send them a personal invite.
private struct GuestCard: View {
    let model: ContactProfileModel
    let contact: Contact

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text("\(contact.name) doesn't use Blitz Split yet")
                    .font(.headline)
                Text("They only exist in your groups, as a name. Send them a personal invite, or merge them with their account if they already use the app.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 10) { buttons }
                VStack(spacing: 10) { buttons }
            }
            .controlSize(.large)
            .buttonBorderShape(.capsule)
        }
        .padding(16)
        .background(Palette.card, in: .rect(cornerRadius: 24))
    }

    @ViewBuilder
    private var buttons: some View {
        Button {
            model.send(ContactProfileUiEventOnMergeClicked.shared)
        } label: {
            Text("Merge with an account")
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(.bordered)
        .disabled(!model.state.canMerge)
        Button {
            model.send(ContactProfileUiEventOnInviteGuestClicked.shared)
        } label: {
            Text("Invite \(contact.name)")
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
    }
}

/// "Waiting for Diego to accept", while the personal invite of a merge is out, with Cancel.
private struct PendingMergeCard: View {
    let model: ContactProfileModel
    let contact: Contact
    let pending: GuestMerge

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "hourglass")
                .foregroundStyle(.orange)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text("Waiting for \(contact.name) to accept")
                    .font(.subheadline.weight(.semibold))
                Text(verbatim: ContactsCopy.pendingMergeSent(pending))
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 8)
            if model.state.isCancellingMerge {
                ProgressView()
            } else {
                Button("Cancel") {
                    model.send(ContactProfileUiEventOnCancelMergeClicked.shared)
                }
                .buttonStyle(.bordered)
                .buttonBorderShape(.capsule)
            }
        }
        .padding(16)
        .background(Palette.card, in: .rect(cornerRadius: 24))
        .accessibilityElement(children: .contain)
    }
}

/// The net with the person and what it allows: Request and Got paid while they owe you, Settle up while you owe them,
/// and Add to a group always.
private struct NetCard: View {
    let model: ContactProfileModel
    let contact: Contact

    private var state: ContactProfileUiState {
        model.state
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: ContactsCopy.balanceTitle(contact))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Text(verbatim: ContactsCopy.amount(contact.net))
                    .font(.system(size: 40, weight: .bold))
                    .monospacedDigit()
                    .foregroundStyle(Palette.balance(contact.net.amountMinor))
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
                Text(verbatim: ContactsCopy.balanceNote(state, contact: contact))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .accessibilityElement(children: .combine)
            if state.canCharge || state.canReceive {
                HStack(spacing: 10) {
                    if state.canCharge {
                        action("Request", systemImage: "text.bubble", isProminent: true) {
                            model.send(ContactProfileUiEventOnChargeClicked.shared)
                        }
                    }
                    if state.canReceive {
                        action("Got paid", systemImage: "arrow.down.to.line", isProminent: false) {
                            model.send(ContactProfileUiEventOnReceivedClicked.shared)
                        }
                    }
                }
            }
            if state.canSettle {
                action("Settle up", systemImage: "checkmark", isProminent: true) {
                    model.send(ContactProfileUiEventOnSettleClicked.shared)
                }
            }
            action("Add to a group", systemImage: "person.badge.plus", isProminent: false) {
                model.send(ContactProfileUiEventOnAddToGroupClicked.shared)
            }
        }
        .padding(16)
        .background(Palette.card, in: .rect(cornerRadius: 24))
        .accessibilityElement(children: .contain)
        .accessibilityLabel(Text("Balance with \(contact.name)"))
    }

    @ViewBuilder
    private func action(
        _ title: LocalizedStringKey,
        systemImage: String,
        isProminent: Bool,
        perform: @escaping () -> Void,
    ) -> some View {
        let button = Button(action: perform) {
            Label(title, systemImage: systemImage)
                .font(.headline)
                .frame(maxWidth: .infinity, minHeight: 32)
        }
        .controlSize(.large)
        .buttonBorderShape(.capsule)
        if isProminent {
            button.buttonStyle(.borderedProminent)
        } else {
            button.buttonStyle(.bordered)
        }
    }
}

/// "Groups in common · 3": each group with what it adds to the net, not tappable.
private struct GroupsInCommon: View {
    let contact: Contact

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Groups in common · \(contact.groups.count)")
                .font(.headline)
                .accessibilityAddTraits(.isHeader)
            VStack(spacing: 0) {
                ForEach(Array(contact.groups.enumerated()), id: \.element.groupId) { index, group in
                    HStack(spacing: 12) {
                        GroupAvatar(initials: group.groupInitials, color: group.groupColor, size: 32)
                            .accessibilityHidden(true)
                        VStack(alignment: .leading, spacing: 0) {
                            Text(verbatim: group.groupName)
                                .lineLimit(1)
                            Text(ContactsCopy.word(group.amountMinor))
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                        Spacer(minLength: 8)
                        Text(verbatim: group.currency.formatSigned(group.amountMinor))
                            .font(.subheadline.weight(.semibold))
                            .monospacedDigit()
                            .foregroundStyle(Palette.balance(group.amountMinor))
                    }
                    .padding(.vertical, 10)
                    .accessibilityElement(children: .combine)
                    if index < contact.groups.count - 1 {
                        Divider()
                    }
                }
            }
            .padding(.horizontal, 16)
            .background(Palette.card, in: .rect(cornerRadius: 20))
        }
    }
}

/// "History with Carla": the latest bills and payments between the two, adding up to the net, and See all.
private struct ContactHistoryList: View {
    let model: ContactProfileModel
    let contact: Contact

    var body: some View {
        let history = model.state.history
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("History with \(contact.name)")
                    .font(.headline)
                    .accessibilityAddTraits(.isHeader)
                Spacer()
                if model.state.hasMoreHistory {
                    Button("See all") {
                        model.send(ContactProfileUiEventOnSeeAllHistoryClicked.shared)
                    }
                    .buttonStyle(.borderless)
                }
            }
            VStack(spacing: 0) {
                ForEach(Array(history.enumerated()), id: \.offset) { index, item in
                    HistoryRow(item: item, name: contact.name)
                        .padding(.vertical, 10)
                    if index < history.count - 1 {
                        Divider()
                    }
                }
            }
            .padding(.horizontal, 16)
            .background(Palette.card, in: .rect(cornerRadius: 20))
        }
    }
}

/// A bill ("Mercado da semana", what it put on whom) or a payment ("Carla paid you", received or paid).
private struct HistoryRow: View {
    let item: any ContactHistoryItem
    let name: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: isPayment ? "arrow.right" : "receipt")
                .font(.subheadline)
                .foregroundStyle(isPayment ? Palette.accent : Color.secondary)
                .frame(width: 32, height: 32)
                .background(isPayment ? AnyShapeStyle(Palette.accent.opacity(0.14)) : AnyShapeStyle(.fill.tertiary), in: .rect(cornerRadius: 9))
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 1) {
                Text(verbatim: title)
                    .lineLimit(1)
                Text(verbatim: meta)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
            Spacer(minLength: 8)
            VStack(alignment: .trailing, spacing: 0) {
                Text(verbatim: amount)
                    .font(.subheadline.weight(.semibold))
                    .monospacedDigit()
                    .foregroundStyle(amountColor)
                Text(word)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .accessibilityElement(children: .combine)
    }

    private var isPayment: Bool {
        item is ContactHistoryItemPayment
    }

    private var title: String {
        if let bill = item as? ContactHistoryItemBill { return bill.description_ }
        if let payment = item as? ContactHistoryItemPayment { return ContactsCopy.paymentTitle(payment, name: name) }
        return ""
    }

    private var meta: String {
        if let bill = item as? ContactHistoryItemBill { return ContactsCopy.billMeta(bill) }
        if let payment = item as? ContactHistoryItemPayment { return ContactsCopy.paymentMeta(payment) }
        return ""
    }

    private var amount: String {
        if let bill = item as? ContactHistoryItemBill { return bill.currency.formatSigned(bill.effectMinor) }
        if let payment = item as? ContactHistoryItemPayment { return payment.currency.format(payment.amountMinor) }
        return ""
    }

    private var amountColor: Color {
        if let bill = item as? ContactHistoryItemBill { return Palette.balance(bill.effectMinor) }
        return .secondary
    }

    private var word: LocalizedStringKey {
        if let bill = item as? ContactHistoryItemBill { return ContactsCopy.word(bill.effectMinor) }
        if let payment = item as? ContactHistoryItemPayment { return payment.isFromMe ? "paid" : "received" }
        return ""
    }
}

/// "Add Carla to a group" (11.5): your groups, the ones they are already in disabled, one marked; they join with no
/// part in the old bills. A sheet over the profile.
private struct AddToGroupSheet: View {
    let model: ContactProfileModel
    let draft: AddToGroupDraft
    let contact: Contact

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(draft.options, id: \.groupId) { option in
                        Button {
                            model.send(ContactProfileUiEventOnAddGroupPicked(groupId: option.groupId))
                        } label: {
                            HStack(spacing: 12) {
                                GroupAvatar(initials: option.initials, color: option.color, size: 32)
                                    .accessibilityHidden(true)
                                VStack(alignment: .leading, spacing: 1) {
                                    Text(verbatim: option.name)
                                    Group {
                                        if option.isAlreadyIn {
                                            Text("Already in the group")
                                        } else {
                                            Text("\(Int(option.memberCount)) members")
                                        }
                                    }
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                                }
                                Spacer(minLength: 8)
                                if option.groupId == draft.selectedGroupId {
                                    Image(systemName: "checkmark")
                                        .fontWeight(.semibold)
                                        .foregroundStyle(Palette.accent)
                                }
                            }
                            .contentShape(.rect)
                        }
                        .buttonStyle(.plain)
                        .disabled(option.isAlreadyIn)
                        .accessibilityAddTraits(option.groupId == draft.selectedGroupId ? .isSelected : [])
                    }
                } footer: {
                    Text(verbatim: footer)
                }
            }
            .navigationTitle(Text("Add \(contact.name) to a group"))
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", role: .cancel) {
                        model.send(ContactProfileUiEventOnAddToGroupDismissed.shared)
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        model.send(ContactProfileUiEventOnAddToGroupConfirmed.shared)
                    }
                    .disabled(!draft.canAdd)
                }
            }
        }
        #if os(iOS)
        .presentationDetents([.medium, .large])
        #else
        .frame(width: 440, height: 440)
        #endif
    }

    private var footer: String {
        contact.isGuest
            ? String(localized: "\(contact.name) joins the group as a name, with no part in the old bills.")
            : String(localized: "\(contact.name) joins right away, with no part in the old bills, and sees the group in their list.")
    }
}

/// "Merge Diego with their account" (11.2h): everything in the guest's name moves to their account once they accept,
/// through a single-use personal invite the server makes as the sheet opens (online only), or a request to someone
/// who already uses the app (not built yet).
private struct MergeGuestSheet: View {
    let model: ContactProfileModel
    let sheet: GuestMergeSheet
    let contact: Contact

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text("Everything in \(contact.name)'s name (bills, payments and the balance of \(ContactsCopy.amount(contact.net))) moves to their account, in every group, and the guest goes away. It only happens once they accept.")
                        .fixedSize(horizontal: false, vertical: true)
                }
                Section {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Send a personal invite")
                            .font(.headline)
                        Text("Single-use link. \(contact.name) joins and becomes themselves right away.")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    personalLink
                }
                Section {
                    Button {
                        model.send(ContactProfileUiEventOnAskAppUserClicked.shared)
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Ask someone who uses the app")
                                .font(.headline)
                            Text("Pick the account; they get the request and accept.")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .contentShape(.rect)
                    }
                    .buttonStyle(.plain)
                }
            }
            .formStyle(.grouped)
            .navigationTitle(Text("Merge \(contact.name) with their account"))
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", systemImage: "xmark", role: .cancel) {
                        model.send(ContactProfileUiEventOnMergeDismissed.shared)
                    }
                }
            }
        }
        .glassToast([
            GlassToastMessage(text: "Coming soon", trigger: model.comingSoonCount),
            GlassToastMessage(text: "Copied", trigger: model.copiedCount),
        ])
        #if os(iOS)
        .presentationDetents([.large])
        #else
        .frame(width: 480, height: 520)
        #endif
    }

    /// The link once the server made it, a spinner while it does, a retry when it failed, or why it waits offline.
    @ViewBuilder
    private var personalLink: some View {
        if let merge = sheet.merge, let displayLink = sheet.displayLink {
            Text(verbatim: displayLink)
                .font(.body.monospaced())
                .lineLimit(1)
                .truncationMode(.middle)
                .textSelection(.enabled)
            HStack(spacing: 10) {
                Button {
                    model.send(ContactProfileUiEventOnMergeCopyClicked.shared)
                } label: {
                    Label("Copy link", systemImage: "doc.on.doc")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                ShareLink(item: merge.link, message: ContactsCopy.mergeMessage(name: contact.name)) {
                    Label("Share", systemImage: "square.and.arrow.up")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
            }
            .buttonBorderShape(.capsule)
        } else if sheet.isRequesting {
            HStack(spacing: 10) {
                ProgressView()
                Text("Creating the personal invite…")
                    .foregroundStyle(.secondary)
            }
        } else if sheet.hasFailed {
            HStack {
                Text("Couldn't create the personal invite.")
                    .foregroundStyle(.secondary)
                Spacer()
                Button("Try again") {
                    model.send(ContactProfileUiEventOnMergeRetryClicked.shared)
                }
                .buttonStyle(.bordered)
                .buttonBorderShape(.capsule)
            }
        } else if !model.state.isOnline {
            Label("Creating the personal invite needs internet.", systemImage: "wifi.slash")
                .foregroundStyle(.secondary)
        }
    }
}
