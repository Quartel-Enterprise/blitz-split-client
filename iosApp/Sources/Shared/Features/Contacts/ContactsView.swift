import Observation
import Shared
import SwiftUI

@Observable
final class ContactsModel {
    private(set) var state: ContactsUiState
    private(set) var changeErrorCount = 0

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createContactsViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! ContactsUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            switch action {
            case is ContactsUiActionShowChangeError: self?.changeErrorCount += 1
            default: break
            }
        })
    }

    func send(_ event: any ContactsUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// The Contacts tab (11.1), right above the groups in the shared back stack. On an iPhone upright, an iPad upright and
/// a narrow Mac window, one column: a person's profile and the agenda push over the list. Lying down with room for two
/// columns (an iPhone lying down, an iPad lying down, an iPhone Duo open, split on its fold) and in a Mac window wide
/// enough, the list keeps its column and the profile picked, or the agenda, fills the column beside it, with the
/// canvas' shapes before anything is picked. Anything else Contacts opens (Balances, the profile's screens) covers the
/// whole tab; the invite is a sheet everywhere.
struct ContactsColumns: View {
    /// The shared back stack above Contacts.
    @Binding var path: [AppRoute]

    /// Made once the tab appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: ContactsModel?
    /// The whole tab, the column beside the list included, so showing it never changes what decides it.
    @State private var layout = ScreenLayout()
    @State private var division: FoldDivision?

    /// The narrowest Mac window content that still fits the profile beside the list.
    private static let macMinimumWidth: CGFloat = 700

    /// Room for the column beside the list, while nothing is pushed over Contacts (sheets aside).
    private var showsDetail: Bool {
        #if os(macOS)
        let isWide = layout.size.width >= Self.macMinimumWidth
        #else
        let isWide = layout.usesTwoColumns
        #endif
        return isWide && path.isEmpty
    }

    /// What the ViewModel is told, nil before the first layout, whose zero size would say there is no column.
    private var reportedDetail: Bool? {
        guard model != nil, layout.size != .zero else { return nil }
        return showsDetail
    }

    /// The fold of an iPhone Duo open as a book, which the list ends at and the profile starts after.
    private var verticalFold: CGRect? {
        division?.verticalFrame
    }

    /// The list's column: as wide as the canvas draws it (320 on an iPhone lying down, 400 with an iPad's room, 360
    /// on the Mac), or up to the fold.
    private var listWidth: CGFloat {
        if let verticalFold { return verticalFold.minX }
        #if os(macOS)
        return 360
        #else
        return layout.isLarge ? ScreenLayout.listColumnWidth : 320
        #endif
    }

    var body: some View {
        Group {
            if let model {
                HStack(spacing: 0) {
                    NavigationStack(path: $path) {
                        ContactsView(model: model, showsDetail: showsDetail)
                            .navigationDestination(for: AppRoute.self) { MainTabView.destination($0) }
                    }
                    .frame(width: showsDetail ? listWidth : nil)
                    if showsDetail {
                        ListDetailSeparator(fold: verticalFold)
                        NavigationStack {
                            ContactsPane(model: model)
                        }
                    }
                }
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        .measuringScreenLayout($layout)
        .readingFoldDivision($division)
        // The ViewModel then picks a person beside the list instead of opening their profile over it
        .onChange(of: reportedDetail, initial: true) { _, isShown in
            guard let isShown else { return }
            model?.send(ContactsUiEventOnDetailPaneChanged(isShown: isShown))
        }
        .onAppear {
            if model == nil { model = ContactsModel() }
        }
    }
}

/// The column beside the list: the person picked, the agenda, or the canvas' shapes before anything is picked.
private struct ContactsPane: View {
    let model: ContactsModel

    var body: some View {
        if let key = model.state.selectedKey {
            ContactProfileView(route: RouteContactProfile(personKey: key), isInPane: true)
                .id(key)
        } else if model.state.isAgendaPicked {
            AgendaView(isInPane: true)
        } else {
            GroupPanePlaceholder()
        }
    }
}

/// The people of your groups A to Z with the net with each, then the invites and whoever is hidden. The magnifier
/// opens the home's search over Contacts (`Route.Search`).
struct ContactsView: View {
    let model: ContactsModel
    /// The column beside the list shows, so the row of what it shows is highlighted.
    let showsDetail: Bool

    var body: some View {
        let state = model.state
        Group {
            if state.isEmpty {
                ContactsEmptyState(model: model)
            } else {
                List {
                    if state.showsOfflineNotice {
                        Section {
                            Label("Offline · contacts on this \(DeviceName.current)", systemImage: "wifi.slash")
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                    }
                    if state.isLoading {
                        ContactsSkeleton(isWaitingForInternet: state.isWaitingForInternet)
                    } else {
                        ContactsSections(model: model, showsDetail: showsDetail)
                    }
                }
                #if os(iOS)
                .listStyle(.insetGrouped)
                #endif
                .scrollContentBackground(.hidden)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.groupedBackground)
        .navigationTitle("Contacts")
        #if os(iOS)
        .tabTitleDisplayMode()
        #endif
        .toolbar {
            #if os(iOS)
            TabTitle(title: String(localized: "Contacts", bundle: .appLanguage))
            #endif
            #if os(macOS)
            ToolbarItem(placement: .primaryAction) {
                Button("Invite with a link or QR code", systemImage: "qrcode") {
                    model.send(ContactsUiEventOnInviteWithLinkClicked.shared)
                }
            }
            SearchToolbarItem(canSearch: state.canSearch) { model.send(ContactsUiEventOnSearchClicked.shared) }
            #endif
            ToolbarItem(placement: .primaryAction) {
                ProfileToolbarButton(initial: state.avatarInitial, color: state.avatarColor, photoUrl: state.photoUrl) {
                    model.send(ContactsUiEventOnProfileClicked.shared)
                }
            }
            .prefersVerticalBar()
        }
        .glassToast([
            GlassToastMessage(text: "Couldn't save the change. Try again.", trigger: model.changeErrorCount),
        ])
    }
}

/// The summary, the people, the invites and the hidden ones.
private struct ContactsSections: View {
    let model: ContactsModel
    let showsDetail: Bool

    var body: some View {
        let state = model.state
        if state.showsSummary {
            Section {
                // The totals are read apart from the people: they shimmer rather than say R$ 0,00 before it
                if let total = state.total.loaded(as: PeopleTotal.self) {
                    ContactsSummaryCard(model: model, total: total)
                        .listRowInsets(EdgeInsets())
                        .listRowBackground(Color.clear)
                } else {
                    ShimmerCard()
                        .shimmering()
                }
            }
        }
        Section {
            ForEach(state.contacts, id: \.key) { row in
                ContactPickRow(
                    model: model,
                    row: row,
                    isSelected: showsDetail && row.key == state.selectedKey,
                )
            }
        } header: {
            Text("People in your groups · \(state.contacts.count)")
        }
        Section {
            InviteActions(model: model, isAgendaSelected: showsDetail && state.isAgendaPicked)
        } header: {
            Text("Bring more people")
        }
        if !state.hiddenContacts.isEmpty {
            HiddenContactsSection(model: model, showsDetail: showsDetail)
        }
    }
}

/// "You owe R$ 126,00 · Owe you R$ 312,40 · See balances": the same totals as Balances, which each one opens.
private struct ContactsSummaryCard: View {
    let model: ContactsModel
    let total: PeopleTotal

    var body: some View {
        VStack(spacing: 12) {
            HStack(spacing: 10) {
                tile(
                    "You owe",
                    amountMinor: total.toPayMinor,
                    color: Palette.negative,
                    event: ContactsUiEventOnYouOweClicked.shared,
                )
                tile(
                    "Owe you",
                    amountMinor: total.toReceiveMinor,
                    color: Palette.positive,
                    event: ContactsUiEventOnOwedToYouClicked.shared,
                )
            }
            Button {
                model.send(ContactsUiEventOnBalancesCardClicked.shared)
            } label: {
                HStack {
                    Text("See balances")
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(.tertiary)
                }
                .contentShape(.rect)
            }
            .buttonStyle(.plain)
            .foregroundStyle(Palette.accent)
        }
        .padding(16)
        .background(Palette.card, in: .rect(cornerRadius: 24))
    }

    private func tile(
        _ label: LocalizedStringKey,
        amountMinor: Int64,
        color: Color,
        event: any ContactsUiEvent,
    ) -> some View {
        Button {
            model.send(event)
        } label: {
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                Text(verbatim: total.currency.format(amountMinor))
                    .font(.title3.weight(.bold))
                    .monospacedDigit()
                    .foregroundStyle(amountMinor == 0 ? Color.secondary : color)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(Palette.groupedBackground, in: .rect(cornerRadius: 16))
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .disabled(amountMinor == 0)
    }
}

/// Someone once: the face, the name, their group or how many, and the net with its
/// word.
struct ContactRowLabel: View {
    let row: ContactRow

    var body: some View {
        HStack(spacing: 12) {
            AvatarPhoto(initial: row.initials, color: row.color, photoUrl: row.photoUrl, size: 40)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 1) {
                Text(verbatim: row.name)
                    .lineLimit(1)
                Text(verbatim: ContactsCopy.meta(row))
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 8)
            VStack(alignment: .trailing, spacing: 0) {
                Text(verbatim: ContactsCopy.amount(row.net))
                    .font(.subheadline.weight(.semibold))
                    .monospacedDigit()
                    .foregroundStyle(Palette.balance(row.net.amountMinor))
                Text(ContactsCopy.word(row.net.amountMinor))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
    }
}

/// A row that opens the person: over the list, or beside it, highlighted while it shows there.
private struct ContactPickRow: View {
    let model: ContactsModel
    let row: ContactRow
    let isSelected: Bool

    var body: some View {
        Button {
            model.send(ContactsUiEventOnContactClicked(key: row.key))
        } label: {
            ContactRowLabel(row: row)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .selectedRowBackground(isSelected)
    }
}

/// "Invite from contacts" (not on the web) and "Invite with a link or QR code".
private struct InviteActions: View {
    let model: ContactsModel
    let isAgendaSelected: Bool

    var body: some View {
        Button {
            model.send(ContactsUiEventOnAgendaClicked.shared)
        } label: {
            InviteActionLabel(
                title: "Invite from contacts",
                subtitle: "Invite someone from your contacts",
                systemImage: "person.crop.circle.badge.plus",
            )
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isAgendaSelected ? .isSelected : [])
        .selectedRowBackground(isAgendaSelected)
        Button {
            model.send(ContactsUiEventOnInviteWithLinkClicked.shared)
        } label: {
            InviteActionLabel(
                title: "Invite with a link or QR code",
                subtitle: "The invite is for one of your groups",
                systemImage: "qrcode",
            )
        }
        .buttonStyle(.plain)
    }
}

private struct InviteActionLabel: View {
    let title: LocalizedStringKey
    let subtitle: LocalizedStringKey
    let systemImage: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: systemImage)
                .foregroundStyle(Palette.accent)
                .frame(width: 40, height: 40)
                .background(Palette.accent.opacity(0.14), in: .rect(cornerRadius: 12))
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 1) {
                Text(title)
                Text(subtitle)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 0)
        }
        .contentShape(.rect)
    }
}

/// "Hidden · 1", closed until tapped: each one with Show, and why they stay in the groups.
private struct HiddenContactsSection: View {
    let model: ContactsModel
    let showsDetail: Bool

    var body: some View {
        let state = model.state
        Section {
            if state.isHiddenExpanded {
                ForEach(state.hiddenContacts, id: \.key) { row in
                    HStack(spacing: 8) {
                        Button {
                            model.send(ContactsUiEventOnContactClicked(key: row.key))
                        } label: {
                            ContactRowLabel(row: row)
                        }
                        .buttonStyle(.plain)
                        Button("Show") {
                            model.send(ContactsUiEventOnShowContactClicked(key: row.key))
                        }
                        .buttonStyle(.bordered)
                        .buttonBorderShape(.capsule)
                        .accessibilityLabel(Text("Show \(row.name) in Contacts"))
                    }
                    .selectedRowBackground(showsDetail && row.key == state.selectedKey)
                }
            }
        } header: {
            Button {
                model.send(ContactsUiEventOnHiddenToggled.shared)
            } label: {
                HStack(spacing: 6) {
                    Text("Hidden · \(state.hiddenContacts.count)")
                    Image(systemName: state.isHiddenExpanded ? "chevron.up" : "chevron.down")
                        .font(.caption.weight(.bold))
                }
            }
            .buttonStyle(.plain)
            .accessibilityAddTraits(.isHeader)
        } footer: {
            if state.isHiddenExpanded {
                Text("Whoever is hidden stays in the groups. They come back to the list by themselves if they join a new account with you.")
            }
        }
    }
}

/// Someone of the agenda: their initials, the name (what was found in bold), the number, and Invite.
struct AgendaContactRow: View {
    let match: AgendaMatch
    let onInvite: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Text(verbatim: match.initials)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.secondary)
                .frame(width: 40, height: 40)
                .background(.fill.tertiary, in: .circle)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 1) {
                Text(ContactsCopy.marked(match.contact.name, match.match))
                    .lineLimit(1)
                if let phone = match.contact.phone {
                    Text(verbatim: phone)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
            Spacer(minLength: 8)
            Button(action: onInvite) {
                Text(String(
                    localized: "Invite (a contact)",
                    defaultValue: "Invite",
                    bundle: .appLanguage,
                    comment: "Invites someone of the agenda with a group's link",
                ))
            }
                .buttonStyle(.bordered)
                .buttonBorderShape(.capsule)
                .accessibilityLabel(Text("Invite \(match.contact.name)"))
        }
    }
}

/// Rows shimmering in the people's place until the device's data is read and the account's copy is whole, so the
/// empty state never flashes; offline while that copy waits, the note under them says so.
private struct ContactsSkeleton: View {
    let isWaitingForInternet: Bool

    var body: some View {
        Section {
            ShimmerList(count: 6)
                .shimmering()
        } footer: {
            if isWaitingForInternet {
                WaitingForInternetNote()
            }
        }
    }
}

/// "Nobody here yet": nobody shares a group with you, with the two ways to bring people in.
private struct ContactsEmptyState: View {
    let model: ContactsModel

    var body: some View {
        ContentUnavailableView {
            Label("Nobody here yet", systemImage: "person.2")
        } description: {
            Text("Whoever shares a group with you shows up here, with the balance of each one.")
        } actions: {
            VStack(spacing: 10) {
                PrimaryButton(title: "Invite from contacts", systemImage: "person.crop.circle.badge.plus") {
                    model.send(ContactsUiEventOnAgendaClicked.shared)
                }
                TintedButton(title: "Invite with a link or QR code") {
                    model.send(ContactsUiEventOnInviteWithLinkClicked.shared)
                }
            }
            .frame(maxWidth: 320)
        }
    }
}
