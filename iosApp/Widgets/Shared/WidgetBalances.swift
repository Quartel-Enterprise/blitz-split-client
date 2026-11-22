import Foundation

/// What the balance widget draws, as the app last wrote it to the App Group (`BalanceWidgetWriter`): the widget's
/// process has no Room and no `Shared` framework, so every amount arrives already written by the app's own formatter,
/// in the app's language (Profile › Language), and the widget only picks the words. On the Apple Watch, the watch app
/// writes its own (`WatchWidgetWriter`), which its complications and Smart Stack card read.
struct WidgetBalances: Codable, Equatable, Sendable {
    /// Which way the money goes: the green "to get back", the orange "you owe", or nothing open.
    enum Direction: String, Codable, Sendable {
        case toReceive
        case youOwe
        case settled

        init(minor: Int64) {
            self = minor > 0 ? .toReceive : minor < 0 ? .youOwe : .settled
        }
    }

    /// The overall balance, Home's big amount: "+R$ 186,40" and, for the Lock Screen's circle, "+186".
    struct Overall: Codable, Equatable, Sendable {
        var signed: String
        var compact: String
        var direction: Direction
        /// "You're owed R$ 312,40" and "You owe R$ 126,00", in the same currency; nil when there is none.
        var owed: String?
        var owing: String?
    }

    /// A group with an open balance: "Apê 302", "−R$ 98".
    struct Group: Codable, Equatable, Sendable, Identifiable {
        var id: String
        var name: String
        var signed: String
        var direction: Direction
    }

    /// The `.lproj` the app draws in ("pt-BR", "es-ES"…), which the widget draws its words in too; nil for the
    /// device's language.
    var language: String?
    /// "All settled": nobody owes this person and they owe nobody, or there is no group yet.
    var isSettled: Bool
    var overall: Overall?
    /// The most recently active first.
    var groups: [Group]
    var updatedAt: Date
    /// "14:32" or "2:32 PM", as Profile's Time format writes it.
    var updatedTime: String

    /// Whether the app wrote anything yet: with no file (an update not opened yet, a build without the App Group) the
    /// widget can't tell "All settled" from a debt, so it draws a dash.
    var isKnown: Bool { overall != nil || isSettled }

    /// What a widget shows before the app ever wrote the file.
    static let unknown = WidgetBalances(
        language: nil,
        isSettled: false,
        overall: nil,
        groups: [],
        updatedAt: .distantPast,
        updatedTime: "",
    )

    /// The widget gallery's preview, the canvas's example (board 15.8).
    static let preview = WidgetBalances(
        language: nil,
        isSettled: false,
        overall: Overall(
            signed: "+$186.40",
            compact: "+186",
            direction: .toReceive,
            owed: "$312.40",
            owing: "$126",
        ),
        groups: [
            Group(id: "floripa", name: "Floripa trip", signed: "+$214.90", direction: .toReceive),
            Group(id: "ape", name: "Apt 302", signed: "−$98", direction: .youOwe),
            Group(id: "bbq", name: "Saturday barbecue", signed: "+$69.50", direction: .toReceive),
        ],
        updatedAt: .now,
        updatedTime: "2:32 PM",
    )
}

/// The file in the App Group's container that the app writes and the widget reads.
enum WidgetBalancesStore {
    /// The App Group of the app and its widget (project.yml's entitlements).
    static let appGroup = "group.com.quare.blitzsplit"
    /// What WidgetKit names the widget, for `reloadTimelines(ofKind:)`.
    static let kind = "BalanceWidget"
    /// What a tap on the widget opens: Balances (`WidgetLink` in the shared code).
    static let balancesURL = URL(string: "borarachar://balances")!

    private static var fileURL: URL? {
        FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: appGroup)?
            .appendingPathComponent("widget-balances.json")
    }

    static func read() -> WidgetBalances? {
        guard let url = fileURL, let data = try? Data(contentsOf: url) else { return nil }
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return try? decoder.decode(WidgetBalances.self, from: data)
    }

    /// Whether the file now holds `balances`; false when the App Group can't be reached (a build signed without it).
    @discardableResult
    static func write(_ balances: WidgetBalances) -> Bool {
        guard let url = fileURL else { return false }
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        guard let data = try? encoder.encode(balances) else { return false }
        return (try? data.write(to: url, options: .atomic)) != nil
    }
}
