import Shared
import SwiftUI

/// The words of "Split a bill" and "Split", composed from the state: the shared code hands over people, modes and
/// amounts, and each platform writes its own sentences.
enum SplitBillCopy {
    static let modes: [SplitMode] = [.equal, .amount, .percent, .shares]

    /// "Split" names both the bill's row ("Divisão") and the screen that edits it ("Dividir"): the screen's title
    /// has a key of its own.
    static let editorTitle = LocalizedStringResource(
        "Split (title of the screen that edits it)",
        defaultValue: "Split",
        comment: "Title of the screen that edits how a bill splits",
    )

    /// "You" for this device's user, the name for anyone else.
    static func name(_ person: BillPerson) -> String {
        person.isMe ? String(localized: "You") : person.name
    }

    /// The mode selector: Equal, Amounts, % and Shares.
    static func modeLabel(_ mode: SplitMode) -> Text {
        switch mode {
        case .amount: Text("Amounts")
        case .percent: Text(verbatim: "%")
        case .shares: Text("Shares")
        default: Text("Equal")
        }
    }

    /// The split row of the bill: Equally, By amount, By percentage or By shares.
    static func modeSummary(_ mode: SplitMode) -> String {
        switch mode {
        case .amount: String(localized: "By amount")
        case .percent: String(localized: "By percentage")
        case .shares: String(localized: "By shares")
        default: String(localized: "Equally")
        }
    }

    /// Above the people: "Who's in", "How much each one had", "Each one's percentage" or "Each one's shares".
    static func listLabel(_ mode: SplitMode) -> LocalizedStringKey {
        switch mode {
        case .amount: "How much each one had"
        case .percent: "Each one's percentage"
        case .shares: "Each one's shares"
        default: "Who's in"
        }
    }

    static func hint(_ editor: SplitEditor) -> String {
        switch editor.mode {
        case .amount:
            let zero = editor.currency.format(0)
            let out = String(localized: "Whoever is left at \(zero) is out of the bill.")
            return "\(out) \(String(localized: "Changing the total later rescales the amounts."))"
        case .percent: return String(localized: "Whole numbers only. The leftover cents go to whoever paid.")
        case .shares: return String(localized: "Use shares when someone has more, like who brought a plus-one.")
        default: return String(localized: "When it doesn't split evenly, the leftover cents go to whoever paid.")
        }
    }

    /// "Diego is out", "Diego and Elisa are out" or "You're out".
    static func out(_ people: [BillPerson]) -> String {
        if people.count == 1, let person = people.first {
            return person.isMe ? String(localized: "You're out") : String(localized: "\(person.name) is out")
        }
        return String(localized: "\(ExpenseFormat.names(people.map(name))) are out")
    }

    /// "Elisa has no amount", "Elisa and Diego have no amount" or "You have no amount".
    static func noAmount(_ people: [BillPerson]) -> String {
        if people.count == 1, let person = people.first {
            return person.isMe
                ? String(localized: "You have no amount")
                : String(localized: "\(person.name) has no amount")
        }
        return String(localized: "\(ExpenseFormat.names(people.map(name))) have no amount")
    }

    /// "Groceries · you paid", "Dinner · Bruno paid", "Dinner · You and Ana paid", or "You paid" while the bill has
    /// no description.
    static func paidBy(_ lead: String, state: SplitBillUiState) -> String {
        let lead = lead.trimmingCharacters(in: .whitespacesAndNewlines)
        if let several = state.severalPayers {
            let paid = ExpenseFormat.paidBySeveral(payerNames(several))
            return lead.isEmpty ? paid : "\(lead) · \(paid)"
        }
        guard let payer = state.payer else { return lead }
        if lead.isEmpty {
            return payer.isMe ? String(localized: "You paid") : String(localized: "\(payer.name) paid")
        }
        return payer.isMe ? String(localized: "\(lead) · you paid") : String(localized: "\(lead) · \(payer.name) paid")
    }

    /// The split row's second line: "Among 5 · $62.40 each · Diego is out", "Among 6 · $84.00 left" or "Among 5"
    /// once nothing more needs saying. The shared code picks the facts; the row's title only names the mode.
    static func splitSubtitle(_ state: SplitBillUiState) -> String {
        let split = state.split
        let count = Int(split.includedCount)
        guard count > 0 else { return String(localized: "Nobody is in yet") }
        let facts = split.summary.map { text($0, currency: split.currency) }
        return ([String(localized: "Among \(count)")] + facts).joined(separator: " · ")
    }

    /// Whoever paid part of the bill: "You", a name, or "Former member" for someone who left the group.
    static func payerNames(_ amounts: PayerAmounts) -> [String] {
        amounts.paying.map(payerName)
    }

    static func payerName(_ row: PayerAmountRow) -> String {
        row.person.map(name) ?? String(localized: "Former member")
    }

    /// "R$ 112,00 left" or "R$ 12,00 over": how far what several people paid is from the total.
    static func payersMismatch(_ amounts: PayerAmounts) -> String {
        let amount = amounts.currency.format(abs(amounts.remainingMinor))
        return amounts.remainingMinor > 0 ? String(localized: "\(amount) left") : String(localized: "\(amount) over")
    }

    /// Under "Several people paid": "R$ 312,00 of R$ 312,00. Adds up.", or what is missing or over.
    static func payersStatus(_ amounts: PayerAmounts) -> String? {
        if amounts.addsUp {
            let paid = amounts.currency.format(amounts.paidMinor)
            return String(localized: "\(paid) of \(amounts.currency.format(amounts.totalMinor)). Adds up.")
        }
        return amounts.showsMismatch ? payersMismatch(amounts) : nil
    }

    /// "Say how much each one paid of R$ 312,00", the total only while there is one.
    static func payersLead(_ amounts: PayerAmounts) -> String {
        amounts.totalMinor > 0
            ? String(localized: "Say how much each one paid of \(amounts.currency.format(amounts.totalMinor))")
            : String(localized: "Say how much each one paid")
    }

    /// "Who paid"'s second line: "R$ 312,00 · Mercado", each part only once it is filled in.
    static func payerSubtitle(_ state: SplitBillUiState) -> String {
        var parts: [String] = []
        if state.isAmountValid, let amount = state.amountMinor {
            parts.append(state.currency.format(amount.int64Value))
        }
        let description = state.description_.trimmingCharacters(in: .whitespacesAndNewlines)
        if !description.isEmpty { parts.append(description) }
        return parts.joined(separator: " · ")
    }

    /// "Split"'s status: the main line, the details under it, whether it still doesn't add up and, by amount, how
    /// much of the total the parts cover.
    struct Status {
        var title: String
        var detail: String?
        var isError: Bool
        var progress: Double?
    }

    static func status(_ editor: SplitEditor) -> Status {
        let currency = editor.currency
        let title = if let each = editor.statusTitle as? SplitFactEach {
            String(localized: "\(currency.format(each.amountMinor)) each")
        } else {
            text(editor.statusTitle, currency: currency)
        }
        let sentences = editor.statusDetails.map { sentence(text($0, currency: currency)) }
        let progress: Double? = editor.mode == .amount
            ? (editor.totalMinor > 0
                ? min(max(Double(editor.assignedMinor) / Double(editor.totalMinor), 0), 1)
                : 0)
            : nil
        return Status(
            title: title,
            detail: sentences.isEmpty ? nil : sentences.joined(separator: " "),
            isError: !editor.addsUp,
            progress: progress,
        )
    }

    /// The words of one thing a split says about itself, as the status and the split row write it. Equal's "each"
    /// reads "$62.40 each" in both, but Portuguese says "R$ 62,40 cada" here, for the row, and "R$ 62,40 para cada
    /// um" in the status's title (see `status(_:)`).
    static func text(_ fact: any SplitFact, currency: GroupCurrency) -> String {
        switch fact {
        case let fact as SplitFactEach:
            return summaryEach(fact.amountMinor, currency: currency)
        case is SplitFactNobody:
            return String(localized: "Nobody is in yet")
        case let fact as SplitFactNobodyChecked:
            return String(localized: "Check at least one person to split \(currency.format(fact.totalMinor)).")
        case let fact as SplitFactNobodyHasParts:
            return String(localized: "Give at least one person a share to split \(currency.format(fact.totalMinor)).")
        case let fact as SplitFactAmong:
            return String(localized: "\(currency.format(fact.totalMinor)) among \(Int(fact.count)) people.")
        case let fact as SplitFactOut:
            return out(fact.people)
        case let fact as SplitFactAmountLeft:
            return String(localized: "\(currency.format(fact.amountMinor)) left")
        case let fact as SplitFactAmountOver:
            return String(localized: "\(currency.format(fact.amountMinor)) over")
        case let fact as SplitFactAmountAddsUp:
            return String(localized: "Adds up: \(currency.format(fact.totalMinor))")
        case is SplitFactPartsMatchTotal:
            return String(localized: "The parts add up to the total.")
        case let fact as SplitFactAmountSoFar:
            let assigned = currency.format(fact.assignedMinor)
            return String(localized: "\(assigned) of \(currency.format(fact.totalMinor)) so far.")
        case let fact as SplitFactWithoutValue:
            return noAmount(fact.people)
        case let fact as SplitFactPercentLeft:
            return String(localized: "\(percent(fact.percent)) left")
        case let fact as SplitFactPercentOver:
            return String(localized: "\(percent(fact.percent)) over")
        case is SplitFactPercentAddsUp:
            return String(localized: "Adds up: \(percent(100))")
        case let fact as SplitFactParts:
            return String(localized: "\(Int(fact.count)) shares of \(currency.format(fact.partMinor))")
        case let fact as SplitFactPersonParts:
            let parts = Int(fact.parts)
            return fact.person.isMe
                ? String(localized: "You have \(parts) shares")
                : String(localized: "\(fact.person.name) has \(parts) shares")
        default:
            return ""
        }
    }

    /// "$62.40 each" in the split row, "R$ 62,40 cada" in Portuguese (shorter than the status's "para cada um").
    private static func summaryEach(_ minor: Int64, currency: GroupCurrency) -> String {
        String(
            localized: "Each one's part in the split row",
            defaultValue: "\(currency.format(minor)) each",
            comment: "Equal split, in the bill's split row: each one's part",
        )
    }

    /// A fact as a sentence of the status's details: "Diego is out." takes the period the row leaves out.
    private static func sentence(_ words: String) -> String {
        words.isEmpty || words.hasSuffix(".") ? words : "\(words)."
    }

    /// "15%" as the device's language writes a percentage.
    static func percent(_ value: Int64) -> String {
        (Double(value) / 100).formatted(.percent)
    }

    /// A person's second line in "Split", by mode.
    static func rowSubtitle(_ row: SplitRow, editor: SplitEditor, isOut: Bool) -> (text: String, isError: Bool) {
        let person = row.person
        let isPayer = editor.payerIds.contains(person.id)
        let share = row.shareMinor.map { editor.currency.format($0.int64Value) }
        switch editor.mode {
        case .amount:
            if isPayer { return (String(localized: "Paid the bill"), false) }
            if !editor.addsUp, editor.withoutValue.contains(where: { $0.id == person.id }) {
                return (String(localized: "No amount"), true)
            }
            if !person.hasJoined { return (String(localized: "Hasn't joined yet"), false) }
            return (String(localized: "Their part"), false)
        case .percent, .shares:
            if isOut { return (String(localized: "Out"), false) }
            return (share ?? "—", false)
        default:
            if isPayer { return (String(localized: "Paid the bill"), false) }
            return (row.isIncluded ? String(localized: "In") : String(localized: "Out"), false)
        }
    }
}
