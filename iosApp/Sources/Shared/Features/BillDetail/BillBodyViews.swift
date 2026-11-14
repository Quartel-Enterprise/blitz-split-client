import Shared
import SwiftUI

/// What Bill and a bill's link page both draw of a bill: what it was, when, how much, who paid and each one's part.
/// Both states hold them under the same names, so the pieces below take either.
protocol BillFacts {
    var currency: GroupCurrency { get }
    var description_: String { get }
    var totalMinor: Int64 { get }
    var spentAt: Int64 { get }
    var spentDay: RelativeDay { get }
    var category: Shared.Category { get }
    var categoryParent: Shared.Category? { get }
    var payers: [BillPayer] { get }
    var splitMode: SplitMode { get }
    var people: [BillShareRow] { get }
    var participantCount: Int32 { get }
}

extension BillDetailUiState: BillFacts {}

extension SharedBillUiState: BillFacts {}

/// The bill not read yet: its title, amount and split shimmer in place.
struct BillLoadingShimmer: View {
    var body: some View {
        ShimmerScreen {
            HStack(spacing: 12) {
                ShimmerBlock(width: 40, height: 40, cornerRadius: 12)
                VStack(alignment: .leading, spacing: 8) {
                    ShimmerLine(width: 120, height: 10)
                    ShimmerLine(width: 200, height: 20)
                }
            }
            ShimmerLine(width: 140, height: 28)
            ShimmerList(count: 4)
        }
        .background(Palette.groupedBackground.ignoresSafeArea())
    }
}

/// "Bruno paid" next to Bruno's face, or "A former member paid". A lone payer opens their profile from here; several
/// each have their row in the split, which opens them.
struct BillPayerLine: View {
    let payers: [BillPayer]
    let lonePayer: BillPayer?
    let size: CGFloat
    /// Nil where nobody opens a profile (a bill's link page).
    let onPerson: ((String) -> Void)?

    var body: some View {
        HStack(spacing: 8) {
            // Whoever left the group has no face
            let faces = payers.filter { $0.name != nil }.prefix(3)
            if !faces.isEmpty {
                HStack(spacing: -size * 0.3) {
                    ForEach(Array(faces.enumerated()), id: \.offset) { _, payer in
                        AvatarPhoto(initial: payer.initials, color: payer.avatarColor, photoUrl: payer.photoUrl, size: size)
                    }
                }
                .accessibilityHidden(true)
            }
            Text(verbatim: BillCopy.paid(payers))
        }
        .opensProfile(onPerson.flatMap { open in lonePayer.map { payer in { open(payer.memberId) } } })
    }
}

/// One person of the split: their face, "Paid R$ 642,00" or "Hasn't joined yet", and their part. The row opens their
/// profile, which someone no longer in the group doesn't have, nor anyone on a bill's link page.
struct BillShareRowView: View {
    let row: BillShareRow
    let currency: GroupCurrency
    let avatarSize: CGFloat
    /// Nil where nobody opens a profile (a bill's link page).
    let onPerson: ((String) -> Void)?

    var body: some View {
        HStack(spacing: 12) {
            ShareAvatar(row: row, size: avatarSize)
            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: BillCopy.name(row))
                    .lineLimit(1)
                if let subtitle = BillCopy.subtitle(row, currency: currency) {
                    Text(verbatim: subtitle)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer(minLength: 8)
            Text(verbatim: BillCopy.share(row, currency: currency))
                .monospacedDigit()
                .foregroundStyle(row.shareMinor == nil ? .secondary : .primary)
        }
        .accessibilityElement(children: .combine)
        .opensProfile(row.opensProfile ? onPerson.map { open in { open(row.memberId) } } : nil)
    }
}

/// Someone who left the group has no name nor color left: a plain person.
private struct ShareAvatar: View {
    let row: BillShareRow
    let size: CGFloat

    var body: some View {
        Group {
            if row.name == nil, !row.isMe {
                Image(systemName: "person.fill")
                    .font(.system(size: size * 0.45))
                    .foregroundStyle(.secondary)
                    .frame(width: size, height: size)
                    .background(.fill.tertiary, in: .circle)
            } else {
                AvatarPhoto(initial: row.initials, color: row.avatarColor, photoUrl: row.photoUrl, size: size)
            }
        }
        .accessibilityHidden(true)
    }
}

#if os(iOS)
/// A bill's page on an iPhone and an iPad. Upright and on a narrow iPad sheet: one column, `first` then `second`, with
/// the composer floating in glass at the bottom, above the keyboard. Lying down and on a wide iPad sheet: `first` (the
/// bill and its Attachments, which wrap there) on the left; `second` (how it splits and what was said) on the right
/// with the composer under it.
struct BillPhoneColumns<First: View, Second: View, Composer: View>: View {
    private let usesTwoColumns: Bool
    private let showsComposer: Bool
    private let first: (Bool) -> First
    private let second: Second
    private let composer: (Bool) -> Composer

    init(
        usesTwoColumns: Bool,
        showsComposer: Bool = true,
        @ViewBuilder first: @escaping (_ wrapsAttachments: Bool) -> First,
        @ViewBuilder second: () -> Second,
        @ViewBuilder composer: @escaping (_ isFloating: Bool) -> Composer,
    ) {
        self.usesTwoColumns = usesTwoColumns
        self.showsComposer = showsComposer
        self.first = first
        self.second = second()
        self.composer = composer
    }

    var body: some View {
        if usesTwoColumns {
            TwoColumnsLayout(spacing: 0) {
                list { first(true) }
            } second: {
                VStack(spacing: 0) {
                    list { second }
                    if showsComposer {
                        composer(false)
                            .padding(.horizontal, 20)
                            .padding(.top, 8)
                            .padding(.bottom, 12)
                    }
                }
            }
        } else {
            list {
                first(false)
                second
            }
            .frame(maxWidth: 640)
            .frame(maxWidth: .infinity)
            .safeAreaInset(edge: .bottom, spacing: 0) {
                if showsComposer {
                    composer(true)
                        .frame(maxWidth: 640)
                        .padding(.horizontal, 16)
                        .padding(.top, 8)
                        .padding(.bottom, 8)
                }
            }
        }
    }

    private func list<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        List(content: content)
            .listStyle(.insetGrouped)
            .listSectionSpacing(.compact)
            .scrollContentBackground(.hidden)
            // Scrolling back up to the bill puts the keyboard away
            .scrollDismissesKeyboard(.interactively)
    }
}

/// The head of a bill's list: its category over its title, "Food › Groceries" (7.3), when it was, its total and who
/// paid, with `notes` under them (Bill's "Queued" and its unknown currency).
struct BillHeadSection<Notes: View>: View {
    let bill: any BillFacts
    let lonePayer: BillPayer?
    /// Nil where nobody opens a profile (a bill's link page).
    let onPerson: ((String) -> Void)?
    @ViewBuilder let notes: Notes

    /// The total's 40 pt, growing with the text size.
    @ScaledMetric(relativeTo: .largeTitle) private var totalSize: CGFloat = 40

    var body: some View {
        Section {
            VStack(alignment: .leading, spacing: 18) {
                HStack(spacing: 12) {
                    CategoryBadge(category: bill.category, size: 52)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(verbatim: CategoryCopy.path(bill.category, parent: bill.categoryParent))
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        Text(verbatim: bill.description_)
                            .font(.title.bold())
                            .lineLimit(3)
                            .accessibilityAddTraits(.isHeader)
                        Text(verbatim: ExpenseFormat.dayTime(bill.spentAt, relativeDay: bill.spentDay))
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text(verbatim: bill.currency.format(bill.totalMinor))
                        .font(.system(size: totalSize, weight: .bold))
                        .monospacedDigit()
                        .lineLimit(1)
                        .minimumScaleFactor(0.5)
                    BillPayerLine(payers: bill.payers, lonePayer: lonePayer, size: 24, onPerson: onPerson)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    notes
                }
                // The row clips to its rounded corner, which would cut the payer's face at its edge
                .padding(.horizontal, 4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .listRowBackground(Color.clear)
        .listRowInsets(EdgeInsets(top: 8, leading: 0, bottom: 4, trailing: 0))
    }
}

/// "Equally among 5": everyone of the split, each with their part.
struct BillSplitSection: View {
    let bill: any BillFacts
    /// Nil where nobody opens a profile (a bill's link page).
    let onPerson: ((String) -> Void)?

    var body: some View {
        Section {
            ForEach(bill.people, id: \.memberId) { row in
                BillShareRowView(row: row, currency: bill.currency, avatarSize: 32, onPerson: onPerson)
            }
        } header: {
            Text(verbatim: BillCopy.splitHeader(bill.splitMode, count: bill.participantCount))
        }
    }
}
#endif

#if os(macOS)
/// The bill's category, its name and when it was, after the group's name where the page doesn't say it elsewhere.
struct MacBillTitles: View {
    let bill: any BillFacts
    let groupName: String?
    let isNarrow: Bool

    var body: some View {
        let day = ExpenseFormat.dayTime(bill.spentAt, relativeDay: bill.spentDay)
        VStack(alignment: .leading, spacing: 2) {
            Text(verbatim: CategoryCopy.path(bill.category, parent: bill.categoryParent))
                .font(.callout)
                .foregroundStyle(.secondary)
                .lineLimit(1)
            Text(verbatim: bill.description_)
                .font(isNarrow ? .title3.bold() : .title2.bold())
                .lineLimit(2)
                .accessibilityAddTraits(.isHeader)
            Text(verbatim: groupName.map { "\($0) · \(day)" } ?? day)
                .foregroundStyle(.secondary)
                .lineLimit(1)
        }
    }
}

/// The bill's total over who paid, with `notes` under them (Bill's "Queued" and its unknown currency).
struct MacBillTotal<Notes: View>: View {
    let bill: any BillFacts
    let lonePayer: BillPayer?
    let isNarrow: Bool
    /// Nil where nobody opens a profile (a bill's link page).
    let onPerson: ((String) -> Void)?
    @ViewBuilder let notes: Notes

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(verbatim: bill.currency.format(bill.totalMinor))
                .font(.system(size: isNarrow ? 28 : 34, weight: .bold))
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.6)
            BillPayerLine(payers: bill.payers, lonePayer: lonePayer, size: 20, onPerson: onPerson)
                .foregroundStyle(.secondary)
            notes
        }
    }
}

/// "Equally among 5" over a card with everyone of the split, each with their part.
struct MacBillSplit: View {
    let bill: any BillFacts
    /// Nil where nobody opens a profile (a bill's link page).
    let onPerson: ((String) -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(verbatim: BillCopy.splitHeader(bill.splitMode, count: bill.participantCount))
                .font(.headline)
            VStack(spacing: 0) {
                ForEach(Array(bill.people.enumerated()), id: \.element.memberId) { index, row in
                    if index > 0 { Divider() }
                    BillShareRowView(row: row, currency: bill.currency, avatarSize: 24, onPerson: onPerson)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                }
            }
            .background(Palette.card, in: .rect(cornerRadius: 10))
        }
    }
}
#endif
