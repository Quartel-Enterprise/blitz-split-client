import Shared
import SwiftUI

/// The words of "Simplify debts" (page 13), composed from the state: the shared code hands over counts, names and
/// amounts, and these write "On · 5 payments instead of 7", what changes for each person and the notices.
enum SimplifyDebtsCopy {
    /// The symbol of the feature, three points joined into one, as the boards draw it.
    static let icon = "point.3.connected.trianglepath.dotted"

    /// "1 payment", "5 payments".
    static func payments(_ count: Int32) -> String {
        String(
            localized: "\(Int(count)) payments",
            bundle: .appLanguage,
            comment: "How many payments settle a group's debts",
        )
    }

    /// The word beside a big count, "payment" or "payments", in the confirmation's "7 payments → 5 payments".
    static func paymentsWord(_ count: Int32) -> String {
        count == 1
            ? String(
                localized: "simplify.payments.one",
                defaultValue: "payment",
                bundle: .appLanguage,
                comment: "After a big 1: one payment",
            )
            : String(
                localized: "simplify.payments.other",
                defaultValue: "payments",
                bundle: .appLanguage,
                comment: "After a big count: payments",
            )
    }

    /// "you" inside a sentence, a name, or "Former member" for someone no longer in the group.
    static func name(_ name: String?, isMe: Bool) -> String {
        isMe ? String(localized: "you", bundle: .appLanguage) : PaymentCopy.name(name)
    }

    /// Who changed it, "Someone" when nobody is known.
    static func changedBy(_ name: String?) -> String {
        name ?? String(localized: "Someone", bundle: .appLanguage, comment: "Who did something in Activity, unnamed")
    }

    // MARK: - Edit group (13.1)

    /// The line under "Simplify debts".
    static func status(_ state: EditGroupUiState) -> String {
        switch state.simplifyStatus {
        case .on:
            if let simplification = state.simplification, !simplification.isAlreadyMinimal {
                let count = payments(simplification.paymentCount)
                return String(
                    localized: "On · \(count) instead of \(Int(simplification.unsimplifiedPaymentCount))",
                    bundle: .appLanguage,
                    comment: "Edit group: Simplify debts is on; %1$@ is how many payments now",
                )
            }
            return String(localized: "On · already the fewest payments", bundle: .appLanguage)
        case .pending:
            return state.simplifiesDebts
                ? String(localized: "On on this \(DeviceName.current) · waiting to upload", bundle: .appLanguage)
                : String(localized: "Off on this \(DeviceName.current) · waiting to upload", bundle: .appLanguage)
        case .needsThirdPerson:
            return String(
                localized: "With 2 people it's already a single payment. Available once a third person joins.",
                bundle: .appLanguage,
            )
        default:
            return String(
                localized: "Combines the group's debts into fewer payments. Nobody pays or receives more.",
                bundle: .appLanguage,
            )
        }
    }

    /// Under the switch: who may change it, or what offline means for it.
    static func note(_ state: EditGroupUiState) -> String {
        if state.showsSimplifyOfflineNotice {
            return String(
                localized: "No internet: the suggested payments already changed on this \(DeviceName.current). The group sees it once it reconnects.",
                bundle: .appLanguage,
            )
        }
        return String(
            localized: "Anyone in the group can change the name, icon, type and Simplify debts. Activity shows who changed it.",
            bundle: .appLanguage,
        )
    }

    // MARK: - The confirmation (13.3)

    static func title(_ confirmation: SimplifyDebtsConfirmation) -> LocalizedStringKey {
        confirmation.isTurningOn ? "Simplify debts?" : "Turn off Simplify debts?"
    }

    /// "7 payments become 5 payments", what the counts block reads as.
    static func countsLabel(_ confirmation: SimplifyDebtsConfirmation) -> String {
        let before = payments(confirmation.paymentsBefore)
        let after = payments(confirmation.paymentsAfter)
        if confirmation.isAlreadyMinimal {
            return String(
                localized: "\(before) stay \(after)",
                bundle: .appLanguage,
                comment: "Simplify debts: the count doesn't drop",
            )
        }
        return String(
            localized: "\(before) become \(after)",
            bundle: .appLanguage,
            comment: "Simplify debts: how many payments before and after",
        )
    }

    static func body(_ confirmation: SimplifyDebtsConfirmation) -> String {
        if !confirmation.isTurningOn {
            return String(
                localized: "Payments go back to being between whoever split each bill. Totals don't change.",
                bundle: .appLanguage,
            )
        }
        if confirmation.isAlreadyMinimal {
            return String(
                localized: "\(confirmation.groupName) already has the fewest payments. Turn it on anyway and the next bills come in simplified.",
                bundle: .appLanguage,
            )
        }
        return String(
            localized: "Everyone keeps paying or receiving the same total. Only who pays whom changes.",
            bundle: .appLanguage,
        )
    }

    /// "You", "Ana", "Diego · guest" or "Former member", bold at the start of its row.
    static func personName(_ change: SimplifyPersonChange) -> String {
        let name = change.person.isMe ? String(
            localized: "You",
            bundle: .appLanguage,
        ) : PaymentCopy.name(change.person.name)
        return change.isGuest ? String(localized: "\(name) · guest", bundle: .appLanguage) : name
    }

    /// What changes for one person, in the shared code's order: "No longer pays $118 to Bruno. Receives from Carla,
    /// Diego and Elisa." This device's user reads it without the third person in English.
    static func personText(_ change: SimplifyPersonChange, currency: GroupCurrency) -> String {
        let isMe = change.person.isMe
        var sentences: [String] = []
        if !change.stopsPaying.isEmpty {
            let list = amounts(change.stopsPaying, currency: currency)
            sentences.append(isMe ? String(localized: "No longer pay \(list).", bundle: .appLanguage) : String(
                localized: "No longer pays \(list).",
                bundle: .appLanguage,
            ))
        }
        if !change.pays.isEmpty {
            let list = amounts(change.pays, currency: currency)
            sentences.append(isMe ? String(localized: "Pay \(list).", bundle: .appLanguage) : String(
                localized: "Pays \(list).",
                bundle: .appLanguage,
            ))
        }
        if !change.stopsReceivingFrom.isEmpty {
            let list = names(change.stopsReceivingFrom)
            sentences.append(
                isMe ? String(localized: "No longer receive from \(list).", bundle: .appLanguage) : String(
                    localized: "No longer receives from \(list).",
                    bundle: .appLanguage,
                ),
            )
        }
        if !change.receivesFrom.isEmpty {
            let list = names(change.receivesFrom)
            sentences.append(isMe ? String(localized: "Receive from \(list).", bundle: .appLanguage) : String(
                localized: "Receives from \(list).",
                bundle: .appLanguage,
            ))
        }
        if change.isGuest {
            sentences.append(String(
                localized: "Doesn't have the app: let them know with Request.",
                bundle: .appLanguage,
            ))
        }
        return sentences.joined(separator: " ")
    }

    /// "Elisa paid you $50": a payment waiting to go up (13.3d).
    static func queued(_ payments: [SimplifyQueuedPayment], currency: GroupCurrency) -> String? {
        guard let first = payments.first else { return nil }
        guard payments.count == 1 else {
            return String(
                localized: "You have \(payments.count) payments in the queue. The simplification counts them; if they change, the suggested payments adjust by themselves.",
                bundle: .appLanguage,
            )
        }
        let amount = currency.format(first.amountMinor)
        let from = name(first.from.name, isMe: first.from.isMe)
        let to = name(first.to.name, isMe: first.to.isMe)
        let payment = if first.to.isMe {
            String(
                localized: "\(from) paid you \(amount)",
                bundle: .appLanguage,
                comment: "A queued payment someone made to this device's user",
            )
        } else if first.from.isMe {
            String(
                localized: "you paid \(amount) to \(to)",
                bundle: .appLanguage,
                comment: "A queued payment this device's user made",
            )
        } else {
            String(
                localized: "\(from) paid \(amount) to \(to)",
                bundle: .appLanguage,
                comment: "A queued payment between two other members",
            )
        }
        return String(
            localized: "You have 1 payment in the queue: \(payment). The simplification counts it; if it changes, the suggested payments adjust by themselves.",
            bundle: .appLanguage,
        )
    }

    static func foot(_ confirmation: SimplifyDebtsConfirmation) -> String {
        if !confirmation.isTurningOn {
            let others = Int(confirmation.othersChangingCount)
            guard others > 0 else { return String(localized: "Everyone sees it in Activity.", bundle: .appLanguage) }
            let more = String(
                localized: "\(others) more people change.",
                bundle: .appLanguage,
                comment: "Turning Simplify debts off",
            )
            return String(
                localized: "\(more) Everyone sees it in Activity, and whoever changes gets a notification.",
                bundle: .appLanguage,
            )
        }
        if confirmation.isAlreadyMinimal {
            return String(localized: "Everyone sees it in Activity. You can turn it off anytime.", bundle: .appLanguage)
        }
        return String(
            localized: "Everyone sees it in Activity. Whoever now pays or receives from someone else gets a notification. You can turn it off anytime.",
            bundle: .appLanguage,
        )
    }

    static func confirm(_ confirmation: SimplifyDebtsConfirmation) -> LocalizedStringKey {
        if !confirmation.isTurningOn { return "Turn off" }
        return confirmation.isAlreadyMinimal ? "Turn on anyway" : "Simplify"
    }

    static func cancel(_ confirmation: SimplifyDebtsConfirmation) -> LocalizedStringKey {
        confirmation.isTurningOn ? "Not now" : "Cancel"
    }

    // MARK: - The conflict (13.1e)

    static func conflict(_ notice: SimplifyDebtsConflictNotice) -> String {
        let name = changedBy(notice.changedByName)
        return notice.simplifiesDebts
            ? String(
                localized: "\(name) turned it on after you. The last change counts: Simplify debts is on.",
                bundle: .appLanguage,
            )
            : String(
                localized: "\(name) turned it off after you. The last change counts: Simplify debts is off.",
                bundle: .appLanguage,
            )
    }

    // MARK: - The group's Balances (13.4)

    static func card(_ simplification: DebtSimplificationSummary) -> String {
        if simplification.isAlreadyMinimal {
            return String(
                localized: "Already the fewest payments. The next bills come in simplified.",
                bundle: .appLanguage,
            )
        }
        let count = payments(simplification.paymentCount)
        return String(
            localized: "\(count) instead of \(Int(simplification.unsimplifiedPaymentCount)). You may pay or receive from someone you never split a bill with.",
            bundle: .appLanguage,
            comment: "The group's Balances while Simplify debts is on; %1$@ is how many payments now",
        )
    }

    // MARK: - Record payment (13.5)

    /// "Simplified debt: Diego pays you what they owed Bruno." or "Simplified debt: includes what Ana owed you."
    static func pairNote(_ note: SimplifiedPairNote, from: PaymentPerson?, to: PaymentPerson?) -> String {
        guard !note.insteadOf.isEmpty else { return String(localized: "Simplified debt.", bundle: .appLanguage) }
        let insteadOf = ExpenseFormat.names(note.insteadOf.map { name($0.name, isMe: $0.isMe) })
        let payerIsMe = from?.isMe == true
        let payer = name(from?.name, isMe: payerIsMe)
        let receiver = name(to?.name, isMe: to?.isMe == true)
        let reason = if note.isNewPair {
            payerIsMe
                ? String(localized: "You pay \(receiver) what you owed \(insteadOf).", bundle: .appLanguage)
                : String(localized: "\(payer) pays \(receiver) what they owed \(insteadOf).", bundle: .appLanguage)
        } else {
            String(localized: "includes what \(payer) owed \(insteadOf).", bundle: .appLanguage)
        }
        return String(localized: "Simplified debt: \(reason)", bundle: .appLanguage)
    }

    /// 13.5c's banner: "Ana simplified the debts just now. You no longer owe Bruno in Floripa trip."
    static func changed(_ notice: SimplifyChangedNotice, to: PaymentPerson?) -> String {
        let name = changedBy(notice.changedByName)
        let receiver = PaymentCopy.name(to?.name)
        return notice.simplifiesDebts
            ? String(
                localized: "\(name) simplified the debts just now. You no longer owe \(receiver) in \(notice.groupName).",
                bundle: .appLanguage,
            )
            : String(
                localized: "\(name) turned off Simplify debts just now. You no longer owe \(receiver) in \(notice.groupName).",
                bundle: .appLanguage,
            )
    }

    // MARK: - Helpers

    /// "$29.50 to you and $19.70 to Bruno".
    private static func amounts(_ amounts: [SimplifyAmount], currency: GroupCurrency) -> String {
        ExpenseFormat.names(amounts.map { amount in
            let value = currency.format(amount.amountMinor)
            let to = name(amount.to.name, isMe: amount.to.isMe)
            return String(
                localized: "\(value) to \(to)",
                bundle: .appLanguage,
                comment: "Simplify debts: an amount and whom it is paid to",
            )
        })
    }

    private static func names(_ people: [SimplifyName]) -> String {
        ExpenseFormat.names(people.map { name($0.name, isMe: $0.isMe) })
    }
}
