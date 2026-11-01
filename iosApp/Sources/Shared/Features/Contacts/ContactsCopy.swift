import Shared
import SwiftUI

/// The words of Contatos, the profile, the agenda and the invite, composed from the state: the shared code hands over
/// names, amounts and flags, and each platform writes them.
enum ContactsCopy {
    /// "Floripa trip" for one group, "3 groups" for more, and "· guest" for someone who never joined.
    static func meta(_ row: ContactRow) -> String {
        let groups = row.groupCount > 1 ? String(localized: "\(Int(row.groupCount)) groups") : row.firstGroupName
        return row.isGuest ? String(localized: "\(groups) · guest") : groups
    }

    /// "+R$ 32,90", "−R$ 98,00" or "R$ 0,00", with its word under it.
    static func amount(_ net: ContactNet) -> String {
        net.currency.formatSigned(net.amountMinor)
    }

    /// "owes you", "you owe" or "settled up", whose way a net goes.
    static func word(_ amountMinor: Int64) -> LocalizedStringKey {
        if amountMinor > 0 { return "owes you" }
        if amountMinor < 0 { return "you owe" }
        return "settled up"
    }

    /// The name with what the search found in bold ("Ca" in "Carla").
    static func marked(_ name: String, _ match: NameMatch?) -> AttributedString {
        GroupsSearchCopy.marked(name, from: match.map { Int($0.start) }, to: match.map { Int($0.end) })
    }

    /// "Carla owes you", "You owe Bruno" or "You're settled up", over the profile's net.
    static func balanceTitle(_ contact: Contact) -> String {
        let amount = contact.net.amountMinor
        if amount > 0 { return String(localized: "\(contact.name) owes you") }
        if amount < 0 {
            return String(
                localized: "You owe %@ (a member)",
                defaultValue: "You owe \(contact.name)",
                comment: "A pair in Balances; %@ is whom this device's user owes",
            )
        }
        return String(localized: "You're settled up")
    }

    /// Under the net: "Already offset across 3 groups", the one group's name, or nothing open anywhere.
    static func balanceNote(_ state: ContactProfileUiState, contact: Contact) -> String {
        if state.isNetOfSeveralGroups {
            return String(localized: "Already offset across \(contact.groups.count) groups")
        }
        if contact.net.amountMinor == 0 { return String(localized: "Nothing open in any group") }
        return contact.groups.first?.groupName ?? ""
    }

    /// "Uses Blitz Split · 3 groups in common", or a guest's "Guest · not on the app yet".
    static func profileMeta(_ contact: Contact) -> String {
        guard !contact.isGuest else { return String(localized: "Guest · not on the app yet") }
        return String(localized: "Uses Blitz Split · \(contact.groups.count) groups in common")
    }

    /// "Oct 30" or "30 de out.", the day of something in the history.
    static func shortDate(_ millis: Int64) -> String {
        ExpenseFormat.date(of: millis).formatted(.dateTime.day().month(.abbreviated))
    }

    /// A bill in the history: "Apê 302 · Oct 28 · Carla paid R$ 196,00".
    static func billMeta(_ bill: ContactHistoryItemBill) -> String {
        let total = bill.currency.format(bill.totalMinor)
        let payer = if bill.isPaidByMe {
            String(localized: "you paid \(total)")
        } else if bill.payerCount > 1 {
            String(localized: "\(Int(bill.payerCount)) people paid \(total)")
        } else {
            String(localized: "\(PaymentCopy.name(bill.payerName)) paid \(total)")
        }
        return "\(bill.groupName) · \(shortDate(bill.at)) · \(payer)"
    }

    /// A payment in the history: "Carla paid you" or "You paid Felipe".
    static func paymentTitle(_ payment: ContactHistoryItemPayment, name: String) -> String {
        payment.isFromMe ? String(localized: "You paid \(name)") : String(localized: "\(name) paid you")
    }

    /// "Floripa trip · Oct 30 · Pix".
    static func paymentMeta(_ payment: ContactHistoryItemPayment) -> String {
        "\(payment.groupName) · \(shortDate(payment.at)) · \(PaymentCopy.method(payment.method))"
    }

    /// "Personal invite sent today, 2:20 PM".
    static func pendingMergeSent(_ merge: GuestMerge) -> String {
        let date = ExpenseFormat.date(of: merge.requestedAt)
        let time = ExpenseFormat.time(merge.requestedAt)
        let day = Calendar.current.isDateInToday(date)
            ? String(localized: "today, \(time)")
            : "\(shortDate(merge.requestedAt)), \(time)"
        return String(localized: "Personal invite sent \(day)")
    }

    /// The invite's message around the link: for someone named, or for whoever gets it.
    static func inviteMessage(groupName: String, name: String?) -> Text {
        if let name {
            return Text("Hi \(name)! Let's split the bills of \(groupName) on Blitz Split. Join with this link:")
        }
        return Text("Let's split the bills of \(groupName) on Blitz Split. Join with this link:")
    }

    /// The personal invite of a guest being merged: they join as themselves.
    static func mergeMessage(name: String) -> Text {
        Text("Hi \(name)! This is your personal invite on Blitz Split: open it and you join as yourself.")
    }
}
