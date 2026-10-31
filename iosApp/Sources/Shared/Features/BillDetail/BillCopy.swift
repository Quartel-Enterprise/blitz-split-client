import Foundation
import Shared
import SwiftUI

/// The words of "Bill", "Edit bill" and "Delete this bill?", composed from the state: the shared code hands over
/// people, amounts and times, and each platform writes its own sentences.
enum BillCopy {
    /// "Delete" names deleting a payment key too, which Portuguese calls "Excluir": a bill's is "Apagar".
    static let delete = LocalizedStringResource(
        "Delete (a bill)",
        defaultValue: "Delete",
        comment: "Deletes a bill: the bill's toolbar button and the confirmation's button",
    )

    /// Someone in a bill who is no longer in the group.
    static var formerMember: String {
        String(localized: "Former member")
    }

    /// "You" for this device's user, the name for anyone else, "Former member" for whoever left the group.
    static func name(_ row: BillShareRow) -> String {
        if row.isMe { return String(localized: "You") }
        return row.name ?? formerMember
    }

    /// "Bruno paid", "You paid", "A former member paid", "You and Ana paid" or "3 people paid".
    static func paid(_ payers: [BillPayer]) -> String {
        let formerPaid = String(localized: "A former member paid")
        return ExpenseFormat.paid(payers.map { (name: $0.name, isMe: $0.isMe) }, loneFormer: formerPaid) ?? formerPaid
    }

    /// Above the people: "Split equally among 5", "By amount among 6", "By % among 4" or "By shares among 3".
    static func splitHeader(_ mode: SplitMode, count: Int32) -> String {
        let count = Int(count)
        return switch mode {
        case .amount: String(localized: "By amount among \(count)")
        case .percent: String(localized: "By % among \(count)")
        case .shares: String(localized: "By shares among \(count)")
        default: String(localized: "Split equally among \(count)")
        }
    }

    /// A person's second line: "Paid R$ 642.00", "Hasn't joined yet", "Not in this bill" or nothing.
    static func subtitle(_ row: BillShareRow, currency: GroupCurrency) -> String? {
        if let paid = row.paidMinor { return String(localized: "Paid \(currency.format(paid.int64Value))") }
        if !row.hasJoined { return String(localized: "Hasn't joined yet") }
        if row.shareMinor == nil { return String(localized: "Not in this bill") }
        return nil
    }

    /// A person's part, or "—" for someone left out of the split.
    static func share(_ row: BillShareRow, currency: GroupCurrency) -> String {
        row.shareMinor.map { currency.format($0.int64Value) } ?? "—"
    }

    /// "Your share": what is owed without its sign, "+R$ 249.60" to get back, or "R$ 0.00".
    static func myPartAmount(_ minor: Int64, currency: GroupCurrency) -> String {
        if minor > 0 { return "+\(currency.format(minor))" }
        return currency.format(minor == .min ? .max : abs(minor))
    }

    /// "you owe", "to get back" or "nothing to settle".
    static func myPartWord(_ minor: Int64) -> String {
        if minor < 0 { return String(localized: "you owe") }
        if minor > 0 { return String(localized: "to get back") }
        return String(localized: "nothing to settle")
    }

    /// Under "Your share": who settles it with whom.
    static func myPartNote(_ note: (any MyPartNote)?, currency: GroupCurrency) -> String? {
        switch note {
        case let owes as MyPartNoteOwesPayers:
            // Kotlin hands a list of nullable names over as NSNull for each null
            let names = ExpenseFormat.names(owes.names.map { ($0 as? String) ?? formerMember })
            return String(localized: "Counts toward what you owe \(names) in Balances.")
        case let owed as MyPartNoteOwedBy:
            // Kotlin hands a list of nullable names over as NSNull for each null
            let names = owed.names.map { ($0 as? String) ?? formerMember }
            let who = ExpenseFormat.names(names)
            if let each = owed.eachMinor {
                let amount = currency.format(each.int64Value)
                return names.count == 1
                    ? String(localized: "\(who) owes you \(amount).")
                    : String(localized: "\(who) owe you \(amount) each.")
            }
            let total = currency.format(owed.totalMinor)
            return names.count == 1
                ? String(localized: "\(who) owes you \(total).")
                : String(localized: "\(who) owe you \(total) in total.")
        default:
            return nil
        }
    }

    /// "“Groceries” leaves the group and everyone's share of it stops counting.", then what it meant for this
    /// device's user: "You no longer owe R$ 118.00." or "You no longer get back R$ 249.60.".
    static func deletionMessage(_ deletion: BillDeletion, currency: GroupCurrency) -> String {
        let leaves = String(localized: "“\(deletion.description_)” leaves the group and everyone's share of it stops counting.")
        guard let mine = deletion.myBalanceMinor?.int64Value, mine != 0 else { return leaves }
        let amount = currency.format(mine == .min ? .max : abs(mine))
        let change = mine < 0
            ? String(localized: "You no longer owe \(amount).")
            : String(localized: "You no longer get back \(amount).")
        return "\(leaves) \(change)"
    }

    /// "Ana changed this bill 2 minutes ago. The last saved edit wins: …", "You changed this bill on another device
    /// just now. …" or "Someone changed this bill …".
    static func conflict(_ conflict: BillConflict, now: Date) -> (lead: String, detail: String) {
        let changedAt = ExpenseFormat.date(of: conflict.changedAt)
        let ago = now.timeIntervalSince(changedAt) < 60
            ? String(localized: "just now")
            : changedAt.formatted(.relative(presentation: .numeric, unitsStyle: .wide))
        let lead = if conflict.changerIsMe {
            String(localized: "You changed this bill on another device \(ago).")
        } else if let name = conflict.changerName {
            String(localized: "\(name) changed this bill \(ago).")
        } else {
            String(localized: "Someone changed this bill \(ago).")
        }
        let detail = String(localized: "The last saved edit wins: if you save now, your edit replaces that change.")
        return (lead, detail)
    }
}

extension View {
    /// "Delete this bill?" while `deletion` is set, from the bill and from editing it. Closing it any other way
    /// than "Delete" (Cancel, Esc, a tap outside on iPad) dismisses it in the ViewModel too.
    func billDeletionAlert(
        _ deletion: BillDeletion?,
        currency: GroupCurrency?,
        isDeleting: Bool,
        onConfirm: @escaping () -> Void,
        onDismiss: @escaping () -> Void,
    ) -> some View {
        alert(
            "Delete this bill?",
            isPresented: Binding(
                // The ViewModel keeps `deletion` while deleting, which must not bring back the alert "Delete" closed
                get: { deletion != nil && !isDeleting },
                set: { shown in
                    if !shown { onDismiss() }
                },
            ),
            presenting: deletion,
        ) { _ in
            Button(role: .destructive, action: onConfirm) {
                Text(BillCopy.delete)
            }
            .disabled(isDeleting)
            Button("Cancel", role: .cancel, action: onDismiss)
        } message: { deletion in
            if let currency {
                Text(verbatim: BillCopy.deletionMessage(deletion, currency: currency))
            }
        }
    }
}
