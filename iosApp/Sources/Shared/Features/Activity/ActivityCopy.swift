import Foundation
import Shared
import SwiftUI

/// The words of Activity, the same on iPhone, iPad and Mac: the shared code hands over each item's kind, people, group,
/// amount and changes, and these write "Bruno split Dinner at Ostradamus" and the line under it.
enum ActivityCopy {
    /// The types Filter lists, in the shared code's order.
    static let types: [ActivityType] = [.bills, .payments, .comments, .peopleAndGroups, .charges]

    // MARK: - An item

    /// "Open Ana's profile", or "Open your profile": what a tap on the face does.
    static func openActorLabel(_ item: ActivityItem) -> String {
        if item.actor.isMe { return String(localized: "Open your profile") }
        return String(localized: "Open \(actor(item))'s profile")
    }

    /// Who did it: "You", their name, or "Someone" when the server kept no name.
    static func actor(_ item: ActivityItem) -> String {
        if item.actor.isMe { return String(localized: "You") }
        return item.actor.name ?? String(localized: "Someone", comment: "Who did something in Activity, unnamed")
    }

    /// What it is about: the bill, the group or the member, as the headline names it.
    static func object(_ item: ActivityItem) -> String {
        let group = item.groupName ?? item.subject
        let name: String? = switch item.kind {
        case .groupCreated, .groupEdited, .groupDeleted: item.subject ?? item.groupName
        case .inviteLinkChanged, .memberJoined, .memberLeft, .simplifyDebtsChanged: group
        default: item.subject
        }
        return name ?? "—"
    }

    /// "**Bruno** split **Dinner at Ostradamus**": the names bolder while the item is unread. In Spanish, when the user
    /// did it, the verb is said to "Tú" ("Tú dividiste").
    static func headline(_ item: ActivityItem) -> Text {
        let weight: Font.Weight = item.isUnread ? .bold : .semibold
        let who = Text(verbatim: actor(item)).fontWeight(weight)
        let what = Text(verbatim: object(item)).fontWeight(weight)
        return SecondPerson.text(sentence(item), actor: who, object: what, actorIsMe: item.actor.isMe)
    }

    /// The headline's words with `SecondPerson.actor` and `SecondPerson.object` where the names go.
    private static func sentence(_ item: ActivityItem) -> String {
        let who = SecondPerson.actor
        let what = SecondPerson.object
        switch item.kind {
        case .billCreated: return String(localized: "\(who) split \(what)", comment: "Activity: someone split a bill")
        case .billEdited: return String(localized: "\(who) edited \(what)", comment: "Activity: someone edited a bill")
        case .billDeleted: return String(localized: "\(who) deleted \(what)", comment: "Activity: someone deleted a bill")
        case .paymentRecorded:
            return String(localized: "\(who) recorded a payment", comment: "Activity: someone recorded a payment")
        case .paymentUndone:
            return String(localized: "\(who) undid a payment", comment: "Activity: someone undid a payment")
        case .groupCreated:
            return String(localized: "\(who) created the group \(what)", comment: "Activity: someone created a group")
        case .groupEdited:
            return String(localized: "\(who) edited the group \(what)", comment: "Activity: someone edited a group")
        case .groupDeleted:
            return String(localized: "\(who) deleted the group \(what)", comment: "Activity: someone deleted a group")
        case .inviteLinkChanged:
            return String(
                localized: "\(who) changed the invite link of \(what)",
                comment: "Activity: someone changed a group's link",
            )
        case .memberAdded: return String(localized: "\(who) added \(what)", comment: "Activity: someone added a member")
        case .memberJoined: return String(localized: "\(who) joined \(what)", comment: "Activity: someone joined a group")
        case .memberLeft: return String(localized: "\(who) left \(what)", comment: "Activity: someone left a group")
        case .memberRemoved:
            return String(localized: "\(who) removed \(what)", comment: "Activity: someone removed a member")
        case .memberReleased:
            return String(
                localized: "\(who) released the name \(what)",
                comment: "Activity: the creator released a member's name",
            )
        case .memberRenamed:
            return String(localized: "\(who) renamed \(what)", comment: "Activity: someone renamed a member")
        case .simplifyDebtsChanged:
            return item.isSimplifyTurnedOn
                ? String(
                    localized: "\(who) turned on Simplify debts in \(what)",
                    comment: "Activity: someone turned on Simplify debts",
                )
                : String(
                    localized: "\(who) turned off Simplify debts in \(what)",
                    comment: "Activity: someone turned off Simplify debts",
                )
        case .commentAdded:
            // A comment deleted since keeps its row, with no text
            if item.isCommentDeleted {
                return String(
                    localized: "\(who) deleted a comment on \(what)",
                    comment: "Activity: someone deleted their comment on a bill",
                )
            }
            return String(localized: "\(who) commented on \(what)", comment: "Activity: someone commented on a bill")
        default:
            return String(
                localized: "\(who) sent a request to \(what)",
                comment: "Activity: someone asked a member to pay",
            )
        }
    }

    /// The line under the headline: "R$ 642,00 · Equal", "Ana → You · R$ 50,00 · Pix", an edit's first change
    /// "Amount R$ 612,00 → R$ 642,00 · 2 more changes", or a comment's text in quotes. Nil when there is nothing more
    /// to say.
    static func detail(_ item: ActivityItem) -> String? {
        if let first = item.changes.first {
            let change = changeLine(first)
            guard item.moreChangesCount > 0 else { return change }
            return "\(change) · \(String(localized: "\(Int(item.moreChangesCount)) more changes"))"
        }
        let amountText = amount(item)
        switch item.kind {
        case .billCreated, .billEdited:
            return joined([amountText, item.code.map { splitMode($0) }])
        case .billDeleted:
            return joined([amountText, String(localized: "the bill no longer exists")])
        case .paymentRecorded, .paymentUndone:
            let from = item.fromIsMe ? String(localized: "You") : PaymentCopy.name(item.fromName)
            let to = item.toIsMe ? String(localized: "You") : PaymentCopy.name(item.toName)
            let method = item.code.map { PaymentCopy.method(SettlementMethod.companion.fromCode(code: $0)) }
            return joined(["\(from) → \(to)", amountText, method])
        case .groupCreated, .groupEdited:
            return groupType(item)
        case .groupDeleted:
            return String(localized: "The group no longer exists")
        case .inviteLinkChanged:
            return String(localized: "The old link no longer works")
        case .memberAdded:
            return String(localized: "Hasn't joined yet")
        case .memberJoined:
            if let name = item.subject, name != item.actor.name {
                return String(localized: "Joined as \(name)")
            }
            return String(localized: "Through the invite link")
        case .memberLeft:
            return String(localized: "Stays as a name in the bills and payments")
        case .memberRemoved:
            return String(localized: "Hadn't joined the group")
        case .memberReleased:
            return String(localized: "Whoever joined as \(object(item)) left the group")
        case .memberRenamed:
            return nil
        case .simplifyDebtsChanged:
            // A change still waiting to go up has no counts yet: its meta line already says "Waiting to send"
            return item.simplification.map { simplification($0, currency: item.currency) }
        case .commentAdded:
            return item.isCommentDeleted ? nil : item.body.map { "“\($0)”" }
        default:
            return joined([amountText, String(localized: "message sent")])
        }
    }

    /// "Viagem Floripa · Today, 8:15 PM", or "· waiting to send" for a change not up yet: the detail beside the list.
    static func meta(_ item: ActivityItem, days: [ActivityDay]) -> String {
        let group = item.groupName ?? ""
        if item.isPending { return "\(group) · \(String(localized: "waiting to send"))" }
        let relativeDay = days.first { $0.items.contains { $0.id == item.id } }?.relativeDay ?? .other
        return "\(group) · \(ExpenseFormat.dayTime(item.occurredAt, relativeDay: relativeDay))"
    }

    /// Under the row's lines: the time, the day being its section's header, or "Waiting to send".
    static func time(_ item: ActivityItem) -> String {
        item.isPending ? String(localized: "Waiting to send") : ExpenseFormat.time(item.occurredAt)
    }

    /// "Open bill", "See payment", "Open group" or "See balances": what the item opens.
    static func openLabel(_ item: ActivityItem) -> LocalizedStringKey {
        switch item.target {
        case .bill: return "Open bill"
        case .balances: return "See balances"
        default:
            let isPayment = item.kind == .paymentRecorded || item.kind == .paymentUndone
            return isPayment ? "See payment" : "Open group"
        }
    }

    /// The day's header: "Today", "Yesterday" or "Friday, October 30".
    static func day(_ day: ActivityDay) -> String {
        switch day.relativeDay {
        case .today: String(localized: "Today")
        case .yesterday: String(localized: "Yesterday")
        default: ExpenseFormat.date(of: day.dayStartMillis).formatted(.dateTime.weekday(.wide).day().month(.wide))
        }
    }

    /// The icon on the face's corner: what kind of thing happened, a trash can once its bill or group is gone.
    static func icon(_ item: ActivityItem) -> String {
        if item.isTargetDeleted || item.kind == .billDeleted || item.kind == .groupDeleted { return "trash" }
        switch item.kind {
        case .billCreated, .billEdited: return "banknote"
        case .commentAdded: return "text.bubble"
        case .paymentRecorded: return "checkmark"
        case .paymentUndone: return "arrow.uturn.backward"
        case .groupCreated: return "plus"
        case .groupEdited, .memberRenamed: return "pencil"
        case .inviteLinkChanged: return "link"
        case .simplifyDebtsChanged: return SimplifyDebtsCopy.icon
        case .memberAdded, .memberJoined: return "person.badge.plus"
        case .memberLeft, .memberRemoved, .memberReleased: return "person.badge.minus"
        default: return "paperplane"
        }
    }

    /// The tile behind that icon, and the icon's color: bills and requests blue, payments teal, people and groups
    /// plain, anything deleted peach.
    static func iconColors(_ item: ActivityItem) -> (fill: Color, content: Color) {
        if item.isTargetDeleted || item.kind == .billDeleted || item.kind == .groupDeleted {
            return (Palette.errorContainer, Palette.onErrorContainer)
        }
        // Simplify debts in the secondary container, as its board draws it
        if item.kind == .simplifyDebtsChanged { return (Palette.infoContainer, Palette.onInfoContainer) }
        switch item.kind.type {
        case .bills, .comments, .charges: return (Palette.pixTile, Palette.onPixTile)
        case .payments: return (Palette.infoContainer, Palette.onInfoContainer)
        default: return (Palette.segmentThumb, .primary)
        }
    }

    // MARK: - A change

    /// "Amount", "Who paid", "Default split"…
    static func field(_ field: ActivityField) -> String {
        switch field {
        case .amount: String(localized: "Amount")
        case .description_: String(localized: "Description")
        case .spentAt: String(localized: "When")
        case .payers: String(localized: "Who paid")
        case .split: String(localized: "Split")
        case .name: String(localized: "Name")
        case .type: String(localized: "Type")
        case .color: String(localized: "Color", comment: "A group's avatar color, in what an edit changed")
        case .shape: String(localized: "Shape", comment: "A group icon's shape, in what an edit changed")
        case .photo: String(localized: "Photo", comment: "A group's photo, in what an edit changed")
        case .currency: String(localized: "Currency")
        case .category: String(localized: "Category")
        case .visibility: String(localized: "Link", comment: "Who may open a bill's share link, in what an edit changed")
        default: String(localized: "Default split")
        }
    }

    /// One side of a change, as the field writes it: an amount, a date, the names, a mode or a type.
    static func value(_ value: ActivityChangeValue, field: ActivityField) -> String {
        let written: String? = switch field {
        case .amount: value.minor.map { minor in
                value.currency?.format(minor.int64Value) ?? minor.int64Value.formatted()
            }
        case .spentAt: value.millis.map { ExpenseFormat.dayTime($0.int64Value, relativeDay: .other) }
        case .payers: names(value)
        case .split: value.code.map { splitMode($0) }
        case .type: value.code.flatMap { GroupType.companion.fromCodeOrNull(code: $0) }?.typeLabel
        case .color: value.code.flatMap { AvatarColor.companion.fromCodeOrNull(code: $0) }?.label
        case .shape: value.code.flatMap { GroupShape.companion.fromCodeOrNull(code: $0) }.map(shapeLabel)
        // The shared code says previous and new for a photo, never its URL, and nothing for none
        case .photo: switch value.code {
            case ActivityField.companion.PHOTO_PREVIOUS: String(localized: "Previous photo")
            case ActivityField.companion.PHOTO_NEW: String(localized: "New photo")
            default: String(localized: "No photo")
            }
        case .currency: value.code
        // Resolved in every group's categories, so a group's own is named too; none is "Other"
        case .category: value.category.map { CategoryCopy.name($0) }
        case .visibility: value.code.map { code in
                code == BillLinkVisibility.anyone.code
                    ? String(localized: "Anyone with the link")
                    : String(localized: "Only people in the bill")
            } ?? String(localized: "Off", comment: "A bill's share link, turned off")
        case .defaultSplit: value.code.map { code in
                DefaultSplitMode.companion.fromCodeOrNull(code: code).map(DefaultSplitCopy.modeLabel) ?? code
            }
        default: value.text
        }
        return written.flatMap { $0.isEmpty ? nil : $0 } ?? "—"
    }

    /// "Cookie", "Petal"…: only Android draws the shape, but every feed names it.
    private static func shapeLabel(_ shape: GroupShape) -> String {
        switch shape {
        case .cookie: String(localized: "Cookie", comment: "A group icon's shape")
        case .petal: String(localized: "Petal", comment: "A group icon's shape")
        case .circle: String(localized: "Circle", comment: "A group icon's shape")
        default: String(localized: "Square", comment: "A group icon's shape")
        }
    }

    /// "Amount R$ 612,00 → R$ 642,00".
    static func changeLine(_ change: ActivityChangeRow) -> String {
        let before = value(change.before, field: change.field)
        let after = value(change.after, field: change.field)
        return "\(field(change.field)) \(before) → \(after)"
    }

    // MARK: - Simplify debts (13.7)

    /// "7 payments became 5. Now Diego pays you $64, and Ana no longer owes you."
    static func simplification(_ simplification: ActivitySimplification, currency: GroupCurrency?) -> String {
        let counts = String(
            localized: "\(Int(simplification.paymentsBefore)) payments became \(Int(simplification.paymentsAfter)).",
            comment: "Activity: how many payments before Simplify debts changed, and after; the verb agrees with the first",
        )
        var after = ""
        if let pair = simplification.newPair {
            let name = PaymentCopy.name(pair.name)
            let amount = debtAmount(abs(pair.afterMinor), currency: currency)
            after = pair.afterMinor > 0
                ? String(localized: "Now \(name) pays you \(amount)", comment: "Activity: a pair Simplify debts made")
                : String(localized: "Now you pay \(name) \(amount)", comment: "Activity: a pair Simplify debts made")
        }
        if let pair = simplification.gonePair {
            let name = PaymentCopy.name(pair.name)
            let gone = pair.beforeMinor > 0
                ? String(localized: "\(name) no longer owes you", comment: "Activity: a pair Simplify debts took away")
                : String(localized: "you no longer owe \(name)", comment: "Activity: a pair Simplify debts took away")
            after = after.isEmpty
                ? String(localized: "Now \(gone)", comment: "Activity: only a pair that went, after the counts")
                : String(localized: "\(after), and \(gone)", comment: "Activity: a new pair and one that went")
        }
        return after.isEmpty ? counts : "\(counts) \(after)."
    }

    /// One side of "See changes" of Simplify debts: "owed you $20", "you owe $64", "didn't owe you".
    static func debtSide(_ minor: Int64, isBefore: Bool, currency: GroupCurrency?) -> String {
        let amount = debtAmount(abs(minor), currency: currency)
        if minor > 0 {
            return isBefore
                ? String(localized: "owed you \(amount)", comment: "Activity: what someone owed this device's user before")
                : String(localized: "owes you \(amount)", comment: "Activity: what someone owes this device's user now")
        }
        if minor < 0 {
            return isBefore
                ? String(localized: "you owed \(amount)", comment: "Activity: what this device's user owed someone before")
                : String(localized: "you owe \(amount)", comment: "Activity: what this device's user owes someone now")
        }
        return isBefore
            ? String(localized: "didn't owe you", comment: "Activity: nobody owed anything before")
            : String(localized: "owes nothing now", comment: "Activity: nobody owes anything now")
    }

    private static func debtAmount(_ minor: Int64, currency: GroupCurrency?) -> String {
        currency?.format(minor) ?? minor.formatted()
    }

    // MARK: - A deleted bill or group

    /// "Bill deleted" or "Group deleted": the alert's title and what the detail's button would have opened.
    static func deletedTitle(_ deletion: ActivityDeletion) -> LocalizedStringKey {
        deletion.isGroup ? "Group deleted" : "Bill deleted"
    }

    /// "Bruno deleted Pastel on the beach on October 30. The entry stays in Activity, but the bill no longer opens."
    static func deleted(_ deletion: ActivityDeletion) -> String {
        let subject = deletion.subject ?? "—"
        let date = deletion.atMillis.map {
            ExpenseFormat.date(of: $0.int64Value).formatted(.dateTime.day().month(.wide))
        }
        let who: String? = deletion.byMe ? String(localized: "You") : deletion.byName
        let first: String = if let who, let date {
            SecondPerson.sentence(
                String(localized: "\(SecondPerson.actor) deleted \(subject) on \(date)."),
                actor: who,
                actorIsMe: deletion.byMe,
            )
        } else if deletion.isGroup {
            String(localized: "The group “\(subject)” was deleted.")
        } else {
            String(localized: "The bill “\(subject)” was deleted.")
        }
        let second = deletion.isGroup
            ? String(localized: "The entry stays in Activity, but the group no longer opens.")
            : String(localized: "The entry stays in Activity, but the bill no longer opens.")
        return "\(first) \(second)"
    }

    // MARK: - The screen

    /// "Offline · 2 changes queued", "No response from the server" or "Syncing…", as the home words them.
    static func status(_ status: ActivityStatus, queuedCount: Int32) -> String {
        switch status {
        case .offline:
            queuedCount > 0
                ? String(localized: "Offline · \(Int(queuedCount)) changes queued")
                : String(localized: "Offline · everything is saved on your \(DeviceName.current)")
        case .serverError: String(localized: "No response from the server")
        default: String(localized: "Syncing…")
        }
    }

    static func statusIcon(_ status: ActivityStatus) -> String {
        switch status {
        case .offline: "icloud.slash"
        case .serverError: "exclamationmark.icloud"
        default: "arrow.triangle.2.circlepath.icloud"
        }
    }

    /// "3 new".
    static func unread(_ count: Int32) -> String {
        String(localized: "\(Int(count)) new", comment: "How many Activity items are unread")
    }

    /// "Contas", "Pagamentos e acertos"…
    static func type(_ type: ActivityType) -> String {
        switch type {
        case .bills: String(localized: "Bills")
        case .payments: String(localized: "Payments and settlements")
        case .comments: String(localized: "Comments")
        case .peopleAndGroups: String(localized: "People and groups")
        default: String(localized: "Requests", comment: "The Activity filter's type of requests to pay")
        }
    }

    static func typeIcon(_ type: ActivityType) -> String {
        switch type {
        case .bills: "banknote"
        case .payments: "checkmark.circle"
        case .comments: "text.bubble"
        case .peopleAndGroups: "person.badge.plus"
        default: "paperplane"
        }
    }

    /// The Type chip: "Type" with none picked, the type with one, "2 types" with more.
    static func typeChip(_ types: [ActivityType]) -> String {
        switch types.count {
        case 0: String(localized: "Type")
        case 1: type(types[0])
        default: String(localized: "\(types.count) types")
        }
    }

    /// "4 results".
    static func results(_ count: Int32) -> String {
        String(localized: "\(Int(count)) results", comment: "How many Activity items the filter being picked shows")
    }

    // MARK: - Helpers

    private static func amount(_ item: ActivityItem) -> String? {
        guard let minor = item.amountMinor?.int64Value else { return nil }
        return item.currency?.format(minor) ?? minor.formatted()
    }

    private static func groupType(_ item: ActivityItem) -> String? {
        (item.code.flatMap { GroupType.companion.fromCodeOrNull(code: $0) } ?? item.groupType)?.typeLabel
    }

    private static func splitMode(_ code: String) -> String {
        guard let mode = SplitMode.companion.fromCodeOrNull(code: code) else { return code }
        return switch mode {
        case .amount: String(localized: "Amounts")
        case .percent: String(localized: "Percentage")
        case .shares: String(localized: "Shares")
        default: String(localized: "Equal")
        }
    }

    private static func names(_ value: ActivityChangeValue) -> String? {
        var names = value.names
        if value.hasFormerMember { names.append(String(localized: "Former member")) }
        return names.isEmpty ? nil : ExpenseFormat.names(names)
    }

    private static func joined(_ parts: [String?]) -> String? {
        let parts = parts.compactMap { $0 }.filter { !$0.isEmpty }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }
}

private extension GroupType {
    /// "Trip", "Home", "Event" or "Other", as Novo grupo names them.
    var typeLabel: String {
        switch self {
        case .home: String(localized: "Home")
        case .event: String(localized: "Event")
        case .other: String(localized: "Other")
        default: String(localized: "Trip")
        }
    }
}
