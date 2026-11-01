import Shared
import SwiftUI

/// A section's title with its link on the right: "Your groups" with "See all".
struct HomeSectionHeader: View {
    let title: String
    let action: String
    let onAction: () -> Void

    var body: some View {
        HStack {
            Text(verbatim: title)
                .font(.title3.bold())
                .accessibilityAddTraits(.isHeader)
            Spacer(minLength: 8)
            Button(action, action: onAction)
                .font(.subheadline)
                .buttonStyle(.borderless)
        }
        .frame(minHeight: 44)
    }
}

/// "Balances with people": a row of faces, whom you owe first, then who owes you. A face settles up with them, or
/// records what they paid you; a guest (no app) wears a crossed-out phone and explains itself on a long press, a hover
/// on the Mac. While the first download fills the device, grey faces stand in.
struct HomePeopleSection: View {
    let people: [HomePerson]
    let isLoading: Bool
    let faceSize: CGFloat
    let send: (any HomeUiEvent) -> Void

    /// The faces run past the row, so the title counts them.
    @State private var isCounted = false

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HomeSectionHeader(
                title: HomeCopy.peopleTitle(count: people.count, isCounted: isCounted),
                action: String(localized: "See balances"),
            ) {
                send(HomeUiEventOnSeeBalancesClicked.shared)
            }
            ScrollView(.horizontal) {
                HStack(alignment: .top, spacing: 8) {
                    if people.isEmpty, isLoading {
                        ForEach(0..<5, id: \.self) { _ in
                            SkeletonFace(size: faceSize)
                        }
                    }
                    ForEach(people, id: \.self) { person in
                        PersonFace(person: person, size: faceSize) {
                            send(HomeUiEventOnPersonClicked(key: person.key, currency: person.currency))
                        }
                    }
                }
            }
            .scrollIndicators(.hidden)
            .onScrollGeometryChange(for: Bool.self) { geometry in
                geometry.contentSize.width > geometry.containerSize.width + 1
            } action: { _, overflows in
                isCounted = overflows
            }
        }
    }
}

private struct PersonFace: View {
    let person: HomePerson
    let size: CGFloat
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 2) {
                avatar
                    .overlay(alignment: .bottomTrailing) {
                        if person.isGuest {
                            Image(systemName: "iphone.slash")
                                .font(.system(size: 10, weight: .bold))
                                .foregroundStyle(.secondary)
                                .frame(width: 20, height: 20)
                                .background(Palette.card, in: .circle)
                                .offset(x: 2, y: 2)
                        }
                    }
                    .padding(.bottom, 2)
                Text(verbatim: HomeCopy.name(person))
                    .font(.footnote)
                    .lineLimit(1)
                Text(verbatim: HomeCopy.amount(person))
                    .font(.caption.weight(.semibold))
                    .monospacedDigit()
                    .foregroundStyle(Palette.balance(person.netMinor))
                    .lineLimit(1)
                Text(verbatim: HomeCopy.word(person))
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            .frame(width: 76)
            .padding(.vertical, 6)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(verbatim: HomeCopy.accessibilityLabel(person)))
        .accessibilityHint(Text(verbatim: HomeCopy.accessibilityHint(person)))
        .accessibilityAddTraits(.isButton)
        #if os(iOS)
        .contextMenu {
            if person.isGuest {
                Text(verbatim: HomeCopy.guestHint(person))
                Button(HomeCopy.accessibilityHint(person), systemImage: "banknote", action: action)
            }
        }
        #else
        .help(person.isGuest ? HomeCopy.guestHint(person) : "")
        #endif
    }

    @ViewBuilder
    private var avatar: some View {
        if let color = person.color {
            AvatarPhoto(
                initial: person.initials,
                color: color,
                photoUrl: nil,
                size: size,
                font: .system(size: size * 0.34, weight: .semibold),
            )
        } else {
            // Someone no longer in the group, a blank face as Balances draws them
            Circle()
                .fill(.fill.tertiary)
                .frame(width: size, height: size)
        }
    }
}

private struct SkeletonFace: View {
    let size: CGFloat

    var body: some View {
        VStack(spacing: 6) {
            Circle()
                .fill(.fill.tertiary)
                .frame(width: size, height: size)
            Capsule()
                .fill(.fill.tertiary)
                .frame(width: 44, height: 10)
            Capsule()
                .fill(.fill.quaternary)
                .frame(width: 52, height: 10)
        }
        .frame(width: 76)
        .padding(.vertical, 6)
        .accessibilityHidden(true)
    }
}

/// "Overall balance": the first currency's net, big, a line per other currency, and what it means; "Owe you" and
/// "You owe" beside it (under it where the column is narrow) open Balances already filtered, disabled at zero.
struct HomeSummaryCard: View {
    let summary: HomeSummary
    let isFirstDownload: Bool
    /// The buttons go under the balance, side by side: an iPhone lying down, an iPhone Duo upright.
    let stacksButtons: Bool
    let send: (any HomeUiEvent) -> Void

    var body: some View {
        let layout = stacksButtons
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: 12))
            : AnyLayout(HStackLayout(alignment: .center, spacing: 12))
        layout {
            balance
            if stacksButtons {
                HStack(spacing: 6) { buttons }
            } else {
                Spacer(minLength: 0)
                VStack(spacing: 6) { buttons }
                    .frame(width: 146)
            }
        }
        .padding(.leading, 18)
        .padding([.trailing, .vertical], 12)
        .padding(.top, 4)
        .background(Palette.card, in: .rect(cornerRadius: 26))
    }

    private var balance: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("Overall balance")
                .font(.footnote.weight(.medium))
                .foregroundStyle(.secondary)
            if let hero = HomeCopy.hero(summary) {
                Text(verbatim: hero)
                    .font(.system(size: 30, weight: .bold))
                    .monospacedDigit()
                    .foregroundStyle(Palette.balance(summary.main?.netMinor ?? 0))
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
            }
            ForEach(HomeCopy.otherCurrencies(summary), id: \.self) { amount in
                Text(verbatim: amount)
                    .font(.title3.bold())
                    .monospacedDigit()
            }
            Text(verbatim: HomeCopy.summaryHint(summary, isFirstDownload: isFirstDownload))
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder
    private var buttons: some View {
        let toReceive = HomeCopy.toReceive(summary)
        let toPay = HomeCopy.toPay(summary)
        SummaryButton(
            label: toReceive.label,
            amounts: toReceive.amount,
            color: Palette.accent,
            isEnabled: summary.canOpenToReceive,
            hint: "See who owes you",
        ) {
            send(HomeUiEventOnToReceiveClicked.shared)
        }
        SummaryButton(
            label: toPay.label,
            amounts: toPay.amount,
            color: Palette.negative,
            isEnabled: summary.canOpenToPay,
            hint: "See whom you owe",
        ) {
            send(HomeUiEventOnToPayClicked.shared)
        }
    }
}

/// "Scan a receipt" (it needs the internet, and answers "Coming soon" for now), "Record payment" and "New group".
/// The Mac has them in its toolbar instead.
struct HomeShortcuts: View {
    let canScanReceipt: Bool
    let send: (any HomeUiEvent) -> Void

    var body: some View {
        HStack(spacing: 10) {
            ShortcutTile(title: "Scan a receipt", systemImage: "doc.text.viewfinder", isEnabled: canScanReceipt) {
                send(HomeUiEventOnScanReceiptClicked.shared)
            }
            ShortcutTile(title: "Record payment", systemImage: "banknote", isEnabled: true) {
                send(HomeUiEventOnRecordPaymentClicked.shared)
            }
            ShortcutTile(title: "New group", systemImage: "person.badge.plus", isEnabled: true) {
                send(HomeUiEventOnCreateGroupClicked.shared)
            }
        }
    }
}

private struct ShortcutTile: View {
    let title: LocalizedStringKey
    let systemImage: String
    let isEnabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(systemName: systemImage)
                    .font(.title3)
                    .foregroundStyle(isEnabled ? Palette.accent : .secondary)
                    .frame(width: 38, height: 38)
                    .background(isEnabled ? AnyShapeStyle(Palette.accent.opacity(0.12)) : AnyShapeStyle(.fill.tertiary), in: .circle)
                Text(title)
                    .font(.footnote.weight(.medium))
                    .multilineTextAlignment(.center)
                if !isEnabled {
                    Text("Needs internet")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
            .frame(maxWidth: .infinity, minHeight: 92)
            .padding(.horizontal, 6)
            .padding(.vertical, 12)
            .background(Palette.card, in: .rect(cornerRadius: 22))
            .opacity(isEnabled ? 1 : 0.7)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .disabled(!isEnabled)
    }
}

/// "Save your recovery code", after the first sync of an account without Google or Apple: "Connect account" opens
/// the profile, "See code" the code's sheet, ✕ hides it until the next launch.
struct HomeRecoveryCard: View {
    let send: (any HomeUiEvent) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: "key.horizontal")
                    .foregroundStyle(Palette.accent)
                    .frame(width: 36, height: 36)
                    .background(Palette.accent.opacity(0.12), in: .rect(cornerRadius: 10))
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Save your recovery code")
                        .font(.headline)
                    Text(verbatim: HomeCopy.recoveryMessage)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .accessibilityElement(children: .combine)
                Spacer(minLength: 0)
                Button("Dismiss", systemImage: "xmark") {
                    send(HomeUiEventOnRecoveryCodeCardDismissed.shared)
                }
                .labelStyle(.iconOnly)
                .buttonStyle(.bordered)
                .buttonBorderShape(.circle)
                .controlSize(.small)
            }
            HStack(spacing: 8) {
                Button {
                    send(HomeUiEventOnConnectAccountClicked.shared)
                } label: {
                    Text("Connect account").frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                Button {
                    send(HomeUiEventOnShowRecoveryCodeClicked.shared)
                } label: {
                    Text("See code").frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
            }
            .font(.subheadline.weight(.medium))
            .lineLimit(1)
            .buttonBorderShape(.capsule)
            .controlSize(.large)
        }
        .padding(.leading, 16)
        .padding([.trailing, .bottom], 12)
        .padding(.top, 14)
        .background(Palette.card, in: .rect(cornerRadius: 26))
    }
}

/// How "Your groups" lays its groups out: a row of cards that scrolls sideways (iPhone, iPad), a grid of three cards
/// (a narrow Mac window) or a list of rows beside the bills (a wide Mac window).
enum HomeGroupsStyle {
    case cards(width: CGFloat)
    case grid
    case rows
}

/// "Your groups": up to three, the pinned first, each as Groups' row says it, and "See all" opens Groups.
struct HomeGroupsSection: View {
    let groups: [HomeGroup]
    let style: HomeGroupsStyle
    let send: (any HomeUiEvent) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HomeSectionHeader(title: String(localized: "Your groups"), action: HomeCopy.seeAllGroups) {
                send(HomeUiEventOnSeeAllGroupsClicked.shared)
            }
            switch style {
            case let .cards(width):
                ScrollView(.horizontal) {
                    HStack(spacing: 10) {
                        ForEach(groups, id: \.row.id) { group in
                            card(group).frame(width: width)
                        }
                    }
                }
                .scrollIndicators(.hidden)
            case .grid:
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 3), spacing: 6) {
                    ForEach(groups, id: \.row.id) { group in
                        card(group)
                    }
                }
            case .rows:
                VStack(spacing: 0) {
                    ForEach(groups, id: \.row.id) { group in
                        open(group) { HomeGroupRow(group: group) }
                    }
                }
                .background(Palette.card, in: .rect(cornerRadius: 18))
            }
        }
    }

    private func card(_ group: HomeGroup) -> some View {
        open(group) { HomeGroupCard(group: group) }
    }

    private func open(_ group: HomeGroup, @ViewBuilder label: () -> some View) -> some View {
        Button {
            send(HomeUiEventOnGroupClicked(groupId: group.row.id))
        } label: {
            label()
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(verbatim: HomeCopy.accessibilityLabel(group)))
        .accessibilityAddTraits(.isButton)
    }
}

/// The group's shape and pin, its name, the last activity ("queued · Groceries" while a change waits), and the balance
/// with its word.
private struct HomeGroupCard: View {
    let group: HomeGroup

    var body: some View {
        let row = group.row
        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .top) {
                GroupAvatar(initials: row.initials, color: row.color, size: 38)
                Spacer(minLength: 4)
                if group.isPinned {
                    Image(systemName: "pin.fill")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer(minLength: 0)
            VStack(alignment: .leading, spacing: 1) {
                Text(verbatim: row.name)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                GroupActivityLine(row: row)
            }
            GroupBalanceLines(row: row)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, minHeight: 144, alignment: .topLeading)
        .background(Palette.card, in: .rect(cornerRadius: 22))
        .contentShape(.rect)
    }
}

/// A wide Mac window's row of "Your groups", beside the latest bills.
private struct HomeGroupRow: View {
    let group: HomeGroup

    var body: some View {
        let row = group.row
        HStack(spacing: 10) {
            GroupAvatar(initials: row.initials, color: row.color, size: 32)
            VStack(alignment: .leading, spacing: 1) {
                HStack(spacing: 4) {
                    Text(verbatim: row.name)
                        .fontWeight(.medium)
                        .lineLimit(1)
                    if group.isPinned {
                        Image(systemName: "pin.fill")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
                GroupActivityLine(row: row)
            }
            Spacer(minLength: 8)
            GroupBalanceLines(row: row)
                .multilineTextAlignment(.trailing)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .contentShape(.rect)
    }
}

/// "today", or "queued · Groceries" with the orange dot, as Groups' row says it.
private struct GroupActivityLine: View {
    let row: GroupRow

    var body: some View {
        HStack(spacing: 5) {
            if row.isPending {
                PendingIndicator()
            }
            Text(verbatim: GroupsCopy.detail(row))
                .lineLimit(1)
        }
        .font(.caption)
        .foregroundStyle(.secondary)
    }
}

/// "+$214.90" over "to get back".
private struct GroupBalanceLines: View {
    let row: GroupRow

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(verbatim: row.currency.formatSigned(row.netMinor))
                .font(.headline)
                .monospacedDigit()
                .foregroundStyle(Palette.balance(row.netMinor))
            Text(verbatim: GroupsCopy.balanceLabel(row.balanceKind))
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }
}

/// "Latest expenses": the five latest bills you paid or take part in, by day, the most recent first. A bill opens its
/// Bill over its group; "See everything" is Activity, not built yet. While the first download fills the device, grey
/// rows stand in.
struct HomeBillsSection: View {
    let days: [HomeBillDay]
    let isLoading: Bool
    let send: (any HomeUiEvent) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HomeSectionHeader(title: String(localized: "Latest expenses"), action: String(localized: "See everything")) {
                send(HomeUiEventOnSeeAllBillsClicked.shared)
            }
            ForEach(days, id: \.dayStartMillis) { day in
                Text(verbatim: ExpenseFormat.day(day.dayStartMillis, relativeDay: day.relativeDay))
                    .font(.footnote)
                    .textCase(.uppercase)
                    .foregroundStyle(.secondary)
                    .padding(.horizontal, 20)
                    .padding(.top, 10)
                    .padding(.bottom, 6)
                    .accessibilityAddTraits(.isHeader)
                rows(day.bills.map { bill in AnyView(billRow(bill)) })
            }
            if days.isEmpty, isLoading {
                rows((0..<3).map { _ in AnyView(SkeletonBillRow()) })
                    .padding(.top, 8)
            }
        }
    }

    /// The rows of one day in a card, a hairline between them.
    private func rows(_ rows: [AnyView]) -> some View {
        VStack(spacing: 0) {
            ForEach(rows.indices, id: \.self) { index in
                rows[index]
                if index < rows.count - 1 {
                    Divider()
                        .padding(.leading, 64)
                }
            }
        }
        .background(Palette.card, in: .rect(cornerRadius: 26))
    }

    private func billRow(_ bill: HomeBill) -> some View {
        Button {
            send(HomeUiEventOnBillClicked(groupId: bill.groupId, expenseId: bill.id))
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "receipt")
                    .foregroundStyle(Palette.accent)
                    .frame(width: 36, height: 36)
                    .background(Palette.accent.opacity(0.12), in: .rect(cornerRadius: 10))
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 1) {
                    Text(verbatim: bill.description_)
                        .lineLimit(1)
                    HStack(spacing: 5) {
                        if bill.isPending {
                            PendingIndicator()
                        }
                        Text(verbatim: HomeCopy.meta(bill))
                            .lineLimit(1)
                    }
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                }
                Spacer(minLength: 8)
                VStack(alignment: .trailing, spacing: 1) {
                    Text(verbatim: bill.currency.format(bill.totalMinor))
                        .font(.subheadline.weight(.semibold))
                        .monospacedDigit()
                    Text(verbatim: HomeCopy.share(bill))
                        .font(.caption.weight(.medium))
                        .monospacedDigit()
                        .foregroundStyle(Palette.balance(bill.myBalanceMinor))
                }
                // The amounts stay whole; the description and the meta give way
                .fixedSize()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
    }
}

private struct SkeletonBillRow: View {
    var body: some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 10)
                .fill(.fill.tertiary)
                .frame(width: 36, height: 36)
            VStack(alignment: .leading, spacing: 6) {
                Capsule().fill(.fill.tertiary).frame(width: 120, height: 12)
                Capsule().fill(.fill.quaternary).frame(width: 180, height: 10)
            }
            Spacer()
            Capsule().fill(.fill.tertiary).frame(width: 60, height: 12)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .accessibilityHidden(true)
    }
}

/// No group and nobody yet: the card of 1.3, "Create group" and "Join with a link or QR code".
struct HomeFirstUseCard: View {
    let send: (any HomeUiEvent) -> Void

    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: "person.3")
                .font(.system(size: 34))
                .foregroundStyle(Palette.accent)
                .frame(width: 80, height: 80)
                .background(Palette.accent.opacity(0.12), in: .rect(cornerRadius: 22))
                .accessibilityHidden(true)
            Text("No groups yet")
                .font(.title2.bold())
                .padding(.top, 8)
                .accessibilityAddTraits(.isHeader)
            Text(verbatim: HomeCopy.firstUseMessage)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
            VStack(spacing: 8) {
                PrimaryButton(title: "Create group") {
                    send(HomeUiEventOnCreateGroupClicked.shared)
                }
                TintedButton(title: "Join with a link or QR code") {
                    send(HomeUiEventOnJoinWithLinkClicked.shared)
                }
            }
            .padding(.top, 10)
        }
        .padding(.horizontal, 20)
        .padding(.top, 32)
        .padding(.bottom, 20)
        .frame(maxWidth: 480)
        .background(Palette.card, in: .rect(cornerRadius: 26))
    }
}
