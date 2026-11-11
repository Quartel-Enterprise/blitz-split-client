import Foundation
import Observation
import Shared

/// Preferências' "Bitcoin in satoshis" as `AppModel` last read it: every bitcoin amount drawn follows it, and a view
/// that writes one redraws when it changes, since `GroupCurrency.format` reads it inside the body.
@Observable
final class BitcoinUnit {
    static let shared = BitcoinUnit()

    var showsSats = false

    private init() {}
}

/// Amounts arrive in the group currency's minor unit (cents, or whole yen) and dates as epoch millis: each is
/// written here the way the device's language writes it.
extension GroupCurrency {
    /// "R$ 312,50" in Portuguese, "R$312.50" in English: 31250 cents in the group's currency. Decimals that are all
    /// zero don't show ("R$ 312"), as `getDisplayFractionDigits` decides for each amount. Bitcoin and ether take
    /// "₿" and "Ξ" where the language puts a currency's symbol, with only the decimals the amount needs ("₿ 0,00125"),
    /// and bitcoin in satoshis is whole sats ("125.000 sats").
    func format(_ minor: Int64) -> String {
        if showsSats { return String(localized: "\(formatPlain(minor)) sats", bundle: .appLanguage) }
        if let cryptoSymbol {
            return Self.cryptoFormatter(symbol: cryptoSymbol, digits: shownDigits(of: minor))
                .string(from: decimal(of: minor) as NSDecimalNumber) ?? formatPlain(minor)
        }
        return decimal(of: minor).formatted(.currency(code: code).precision(.fractionLength(shownDigits(of: minor))))
    }

    /// "+R$ 600" or "−R$ 118,50", with a true minus sign: what a bill does to a balance. Zero takes no sign.
    func formatSigned(_ minor: Int64) -> String {
        let amount = format(minor == .min ? .max : abs(minor))
        if minor == 0 { return amount }
        return minor < 0 ? "−\(amount)" : "+\(amount)"
    }

    /// "120,50" or "120.50", with no symbol: where the currency is already on the same line ("you get 120 and owe
    /// 218.50").
    func formatPlain(_ minor: Int64) -> String {
        decimal(of: minor).formatted(.number.precision(.fractionLength(shownDigits(of: minor))))
    }

    /// "0,00" or "0.00": the placeholder of an amount field, with the digits it types ("0" for bitcoin in sats).
    var zeroPlaceholder: String {
        Decimal(0).formatted(.number.precision(.fractionLength(typedDigits)))
    }

    /// The symbol in front of an amount field ("R$", "US$" or "$", as the device's language names it; "₿", "Ξ", or
    /// "sats" for bitcoin in satoshis).
    var symbol: String {
        if showsSats { return String(localized: "sats", bundle: .appLanguage) }
        if let cryptoSymbol { return cryptoSymbol }
        if let symbol = Self.symbols[code] { return symbol }
        let formatter = NumberFormatter()
        formatter.locale = .current
        formatter.numberStyle = .currency
        formatter.currencyCode = code
        let symbol = formatter.currencySymbol ?? code
        Self.symbols[code] = symbol
        return symbol
    }

    /// "₿" and "Ξ", which no locale knows as a currency's symbol, or the raw code of a currency this version doesn't
    /// know; nil for every other currency.
    private var cryptoSymbol: String? {
        if self == GroupCurrency.companion.BTC { return "₿" }
        if self == GroupCurrency.companion.ETH { return "Ξ" }
        // A code this version doesn't know has no symbol: the raw code stands in its place ("XYZ 12,50").
        return isKnown ? nil : code
    }

    private var showsSats: Bool {
        self == GroupCurrency.companion.BTC && BitcoinUnit.shared.showsSats
    }

    private var typedDigits: Int {
        Int(getTypedFractionDigits(showsSats: BitcoinUnit.shared.showsSats))
    }

    private func shownDigits(of minor: Int64) -> Int {
        Int(getDisplayFractionDigits(amountMinor: minor, showsSats: BitcoinUnit.shared.showsSats))
    }

    /// The number shown: the major unit, or the minor one itself for bitcoin in sats.
    private func decimal(of minor: Int64) -> Decimal {
        Decimal(minor) / pow(Decimal(10), typedDigits)
    }

    private static func cryptoFormatter(symbol: String, digits: Int) -> NumberFormatter {
        let key = "\(symbol):\(digits)"
        if let formatter = cryptoFormatters[key] { return formatter }
        let formatter = NumberFormatter()
        formatter.locale = .current
        formatter.numberStyle = .currency
        formatter.currencySymbol = symbol
        formatter.minimumFractionDigits = digits
        formatter.maximumFractionDigits = digits
        cryptoFormatters[key] = formatter
        return formatter
    }

    private static var symbols: [String: String] = [:]
    private static var cryptoFormatters: [String: NumberFormatter] = [:]
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
        case .today: String(localized: "Today", bundle: .appLanguage)
        case .yesterday: String(localized: "Yesterday", bundle: .appLanguage)
        default: weekdayDate(millis)
        }
    }

    /// "When" on a bill: "Today, Oct 28 · 9:30 PM", "Yesterday, Oct 27 · 9:30 PM" or "Fri, Oct 24 · 9:30 PM".
    static func when(_ millis: Int64, relativeDay: RelativeDay) -> String {
        let day = switch relativeDay {
        case .today: String(localized: "Today, \(shortDate(millis))", bundle: .appLanguage)
        case .yesterday: String(localized: "Yesterday, \(shortDate(millis))", bundle: .appLanguage)
        default: weekdayDate(millis)
        }
        return "\(day) · \(time(millis))"
    }

    /// Under a bill's description: "Today, 2:30 PM", "Yesterday, 9:10 PM" or "Oct 27, 9:10 PM" ("Hoje, 14:30",
    /// "27 de out., 21:10").
    static func dayTime(_ millis: Int64, relativeDay: RelativeDay) -> String {
        switch relativeDay {
        case .today: String(localized: "Today, \(time(millis))", bundle: .appLanguage)
        case .yesterday: String(localized: "Yesterday, \(time(millis))", bundle: .appLanguage)
        default: "\(shortDate(millis)), \(time(millis))"
        }
    }

    /// "Oct 28" or "28 de out.".
    static func shortDate(_ millis: Int64) -> String {
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
        names.count <= 2 ? self.names(names) : String(localized: "\(names.count) people", bundle: .appLanguage)
    }

    /// "Bruno paid", "You paid", "You and Ana paid" or "3 people paid", from whoever paid in the shared code's order
    /// (a nil name is someone who left the group). Conta and Despesas differ only in what a lone payer who left gets:
    /// `loneFormer`, also for a bill with no payer.
    static func paid(_ payers: [(name: String?, isMe: Bool)], loneFormer: String?) -> String? {
        if payers.count > 1 {
            let formerMember = String(localized: "Former member", bundle: .appLanguage)
            return paidBySeveral(payers.map { $0.isMe ? String(
                localized: "You",
                bundle: .appLanguage,
            ) : $0.name ?? formerMember })
        }
        guard let payer = payers.first else { return loneFormer }
        if payer.isMe { return String(localized: "You paid", bundle: .appLanguage) }
        guard let name = payer.name else { return loneFormer }
        return String(localized: "\(name) paid", bundle: .appLanguage)
    }

    /// "You and Ana paid", "3 people paid", or "Ana paid" once the other payers of a new bill left the group and
    /// only one is left paying part of it.
    static func paidBySeveral(_ names: [String]) -> String {
        if names.count == 1, let name = names.first { return String(localized: "\(name) paid", bundle: .appLanguage) }
        return String(
            localized: "Several people paid a bill",
            defaultValue: "\(payerNames(names)) paid",
            bundle: .appLanguage,
            comment: "Who paid a bill several people paid; %@ is their names, or how many they are",
        )
    }
}
