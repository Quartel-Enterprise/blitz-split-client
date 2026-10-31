import Foundation
import Shared

/// Amounts arrive in the group currency's minor unit (cents, or whole yen) and dates as epoch millis: each is
/// written here the way the device's language writes it.
extension GroupCurrency {
    /// "R$ 312,00" in Portuguese, "R$312.00" in English: 31200 cents in the group's currency.
    func format(_ minor: Int64) -> String {
        decimal(of: minor).formatted(.currency(code: code).precision(.fractionLength(Int(fractionDigits))))
    }

    /// "+R$ 600,00" or "−R$ 118,00", with a true minus sign: what a bill does to a balance. Zero takes no sign.
    func formatSigned(_ minor: Int64) -> String {
        let amount = format(minor == .min ? .max : abs(minor))
        if minor == 0 { return amount }
        return minor < 0 ? "−\(amount)" : "+\(amount)"
    }

    /// "0,00" or "0.00": the placeholder of an amount field, with the currency's digits.
    var zeroPlaceholder: String {
        Decimal(0).formatted(.number.precision(.fractionLength(Int(fractionDigits))))
    }

    /// The symbol in front of an amount field ("R$", "US$" or "$", as the device's language names it).
    @MainActor var symbol: String {
        if let symbol = Self.symbols[code] { return symbol }
        let formatter = NumberFormatter()
        formatter.locale = .current
        formatter.numberStyle = .currency
        formatter.currencyCode = code
        let symbol = formatter.currencySymbol ?? code
        Self.symbols[code] = symbol
        return symbol
    }

    private func decimal(of minor: Int64) -> Decimal {
        Decimal(minor) / pow(Decimal(10), Int(fractionDigits))
    }

    @MainActor private static var symbols: [String: String] = [:]
}

enum ExpenseFormat {
    static func date(of millis: Int64) -> Date {
        Date(timeIntervalSince1970: Double(millis) / 1000)
    }

    static func millis(of date: Date) -> Int64 {
        Int64((date.timeIntervalSince1970 * 1000).rounded())
    }

    /// "21:30" or "9:30 PM".
    static func time(_ millis: Int64) -> String {
        date(of: millis).formatted(date: .omitted, time: .shortened)
    }

    /// A day of the group's list: "Today", "Yesterday" or "Fri, Oct 24".
    static func day(_ millis: Int64, relativeDay: RelativeDay) -> String {
        switch relativeDay {
        case .today: String(localized: "Today")
        case .yesterday: String(localized: "Yesterday")
        default: weekdayDate(millis)
        }
    }

    /// "When" on a bill: "Today, Oct 28 · 9:30 PM", "Yesterday, Oct 27 · 9:30 PM" or "Fri, Oct 24 · 9:30 PM".
    static func when(_ millis: Int64, relativeDay: RelativeDay) -> String {
        let day = switch relativeDay {
        case .today: String(localized: "Today, \(shortDate(millis))")
        case .yesterday: String(localized: "Yesterday, \(shortDate(millis))")
        default: weekdayDate(millis)
        }
        return "\(day) · \(time(millis))"
    }

    /// Under a bill's description: "Today, 2:30 PM", "Yesterday, 9:10 PM" or "Oct 27, 9:10 PM" ("Hoje, 14:30",
    /// "27 de out., 21:10").
    static func dayTime(_ millis: Int64, relativeDay: RelativeDay) -> String {
        switch relativeDay {
        case .today: String(localized: "Today, \(time(millis))")
        case .yesterday: String(localized: "Yesterday, \(time(millis))")
        default: "\(shortDate(millis)), \(time(millis))"
        }
    }

    /// "Oct 28" or "28 de out.".
    private static func shortDate(_ millis: Int64) -> String {
        date(of: millis).formatted(.dateTime.day().month(.abbreviated))
    }

    /// "Fri, Oct 24" or "sex., 24 de out.".
    private static func weekdayDate(_ millis: Int64) -> String {
        date(of: millis).formatted(.dateTime.weekday(.abbreviated).day().month(.abbreviated))
    }

    /// "Diego", "Diego and Elisa" or "Ana, Diego and Elisa", in the device's language.
    static func names(_ people: [String]) -> String {
        people.formatted(.list(type: .and))
    }

    /// Who paid a bill several people paid: "You and Ana" for two, "3 people" for three or more, in the order the
    /// shared code gives them.
    static func payerNames(_ names: [String]) -> String {
        names.count <= 2 ? self.names(names) : String(localized: "\(names.count) people")
    }

    /// "Bruno paid", "You paid", "You and Ana paid" or "3 people paid", from whoever paid in the shared code's order
    /// (a nil name is someone who left the group). Conta and Despesas differ only in what a lone payer who left gets:
    /// `loneFormer`, also for a bill with no payer.
    static func paid(_ payers: [(name: String?, isMe: Bool)], loneFormer: String?) -> String? {
        if payers.count > 1 {
            let formerMember = String(localized: "Former member")
            return paidBySeveral(payers.map { $0.isMe ? String(localized: "You") : $0.name ?? formerMember })
        }
        guard let payer = payers.first else { return loneFormer }
        if payer.isMe { return String(localized: "You paid") }
        guard let name = payer.name else { return loneFormer }
        return String(localized: "\(name) paid")
    }

    /// "You and Ana paid", "3 people paid", or "Ana paid" once the other payers of a new bill left the group and
    /// only one is left paying part of it.
    static func paidBySeveral(_ names: [String]) -> String {
        if names.count == 1, let name = names.first { return String(localized: "\(name) paid") }
        return String(
            localized: "Several people paid a bill",
            defaultValue: "\(payerNames(names)) paid",
            comment: "Who paid a bill several people paid; %@ is their names, or how many they are",
        )
    }
}
