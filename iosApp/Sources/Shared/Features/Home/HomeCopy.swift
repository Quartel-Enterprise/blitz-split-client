import Foundation
import Shared
import SwiftUI

/// The words of Home, the same on iPhone, iPad and Mac: the shared code hands names, amounts and enums, and these
/// pick the text.
enum HomeCopy {
    /// "Home", the tab and the screen. Its own key: "Home" alone is the group type, "Casa" in Portuguese.
    static var tabTitle: String {
        String(localized: "Home tab", defaultValue: "Home", comment: "The home's first tab and its title (Início)")
    }

    /// "See all" beside "Your groups", its own key: Contacts' "See all" opens a history, said differently in Portuguese.
    static var seeAllGroups: String {
        String(localized: "See all groups", defaultValue: "See all", comment: "Home: opens the Groups tab")
    }

    /// "Balances with people", with how many once the row of faces runs past the screen: "Balances with people · 9".
    static func peopleTitle(count: Int, isCounted: Bool) -> String {
        isCounted ? String(localized: "Balances with people · \(count)") : String(localized: "Balances with people")
    }

    /// Someone no longer in the group is "Former member".
    static func name(_ person: HomePerson) -> String {
        PaymentCopy.name(person.name)
    }

    /// "−$126.00" or "+$162.30".
    static func amount(_ person: HomePerson) -> String {
        person.currency.formatSigned(person.netMinor)
    }

    /// Under the amount: "you owe" or "owes you".
    static func word(_ person: HomePerson) -> String {
        person.netMinor < 0 ? String(localized: "you owe") : String(localized: "owes you")
    }

    /// "You owe Bruno $126.00" or "Diego owes you $45.00, guest without the app", as VoiceOver reads the face.
    static func accessibilityLabel(_ person: HomePerson) -> String {
        let amount = person.currency.format(abs(person.netMinor))
        let label = person.netMinor < 0
            ? String(localized: "You owe \(name(person)) \(amount)")
            : String(localized: "\(name(person)) owes you \(amount)")
        return person.isGuest ? String(localized: "\(label), guest without the app") : label
    }

    /// What tapping a face opens: settling up with whom you owe, recording what someone paid you.
    static func accessibilityHint(_ person: HomePerson) -> String {
        person.netMinor < 0 ? String(localized: "Settle up") : String(localized: "Record payment")
    }

    /// A guest's long press (a hover on the Mac): they have no app, so they never hear of the payment.
    static func guestHint(_ person: HomePerson) -> String {
        #if os(macOS)
        String(localized: "\(name(person)) is a guest without the app. Click to note what they paid you; they get no notice.")
        #else
        String(localized: "\(name(person)) is a guest without the app. Tap to note what they paid you; they get no notice.")
        #endif
    }

    /// Under "Overall balance": "to get back, already offset", "to pay, already offset", "all settled up", or "so far
    /// · the rest is on its way" while the first download fills the device.
    static func summaryHint(_ summary: HomeSummary, isFirstDownload: Bool) -> String {
        if isFirstDownload { return String(localized: "so far · the rest is on its way") }
        switch summary.kind {
        case .toReceive: return String(localized: "to get back, already offset")
        case .youOwe: return String(localized: "to pay, already offset")
        default: return String(localized: "all settled up")
        }
    }

    /// The overall balance, big: the shared code's main currency ("+$186.40"), its zero when everything is settled.
    static func hero(_ summary: HomeSummary) -> String? {
        summary.main.map { $0.currency.formatSigned($0.netMinor) }
    }

    /// One smaller line per other currency with a net, as Groups' overall balance has them.
    static func otherCurrencies(_ summary: HomeSummary) -> [String] {
        summary.others.map { $0.currency.formatSigned($0.netMinor) }
    }

    /// "Owe you $312.40" and "You owe $126.00", each in the first currency with something that way; "Nothing to get
    /// back" and "Nothing to pay" at zero.
    static func toReceive(_ summary: HomeSummary) -> (label: LocalizedStringKey, amount: [String]) {
        guard let total = summary.toReceive else { return ("Nothing to get back", []) }
        return ("Owe you", [total.currency.format(total.toReceiveMinor)])
    }

    static func toPay(_ summary: HomeSummary) -> (label: LocalizedStringKey, amount: [String]) {
        guard let total = summary.toPay else { return ("Nothing to pay", []) }
        return ("You owe", [total.currency.format(total.toPayMinor)])
    }

    /// "Save your recovery code"'s message, with the device the person would switch.
    static var recoveryMessage: String {
        String(localized: "You use the app without an account. If you switch \(DeviceName.current)s, this code is what brings your groups back.")
    }

    /// The first use's message, with the device everything stays on.
    static var firstUseMessage: String {
        String(localized: "Create a group to split the first bill, or join one through the link or QR code someone sent you. Everything is saved on this \(DeviceName.current), even without internet.")
    }

    /// A pinned group's card: "Floripa trip, pinned: +$214.90 to get back".
    static func accessibilityLabel(_ group: HomeGroup) -> String {
        let row = group.row
        let balance = "\(row.currency.formatSigned(row.netMinor)) \(GroupsCopy.balanceLabel(row.balanceKind))"
        return group.isPinned
            ? String(localized: "\(row.name), pinned: \(balance)")
            : "\(row.name): \(balance)"
    }

    /// Under a bill: "Floripa trip · You paid", "Ana and Bruno paid", or "queued · Floripa trip · Ana paid" before it
    /// reaches the server.
    static func meta(_ bill: HomeBill) -> String {
        let payers = bill.payers.map { (name: $0.name, isMe: $0.isMe) }
        let meta = ExpenseFormat.paid(payers, loneFormer: nil).map { "\(bill.groupName) · \($0)" } ?? bill.groupName
        return bill.isPending ? String(localized: "queued · \(meta)") : meta
    }

    /// What the bill does to this person's balance: "+$249.60 to get back", "−$29.20 you owe", or a plain zero.
    static func share(_ bill: HomeBill) -> String {
        let amount = bill.currency.formatSigned(bill.myBalanceMinor)
        if bill.myBalanceMinor > 0 { return "\(amount) \(GroupsCopy.balanceLabel(.toReceive))" }
        if bill.myBalanceMinor < 0 { return "\(amount) \(GroupsCopy.balanceLabel(.youOwe))" }
        return amount
    }
}
