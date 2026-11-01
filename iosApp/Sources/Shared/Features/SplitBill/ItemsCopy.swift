import Shared
import SwiftUI

/// The words of "Bill items" (7.5) and "Item" (7.6): the shared code hands over the items, their statuses and the
/// amounts, and this file writes the sentences.
enum ItemsCopy {
    /// "Delete" in "Delete this item?", which Portuguese words as "Apagar" rather than the app's other "Excluir".
    static let deleteConfirm = LocalizedStringResource(
        "Delete (the item)",
        defaultValue: "Delete",
        comment: "Deletes the item, in Delete this item?",
    )

    static func modeLabel(_ mode: ItemSplitMode) -> Text {
        switch mode {
        case .units: Text("Units")
        case .shares: Text("Shares")
        case .amount: Text("Amount")
        case .percent: Text(verbatim: "%")
        default: Text("Equal")
        }
    }

    /// "2 items with nobody", "R$ 15,00 left" or "Adds up: R$ 642,00".
    static func statusTitle(_ status: ItemsStatus, currency: GroupCurrency) -> String {
        switch status {
        case let status as ItemsStatusLoose:
            return looseCount(status.items.count)
        case let status as ItemsStatusAmountLeft:
            return String(localized: "\(currency.format(status.amountMinor)) left")
        case let status as ItemsStatusAmountOver:
            return String(localized: "\(currency.format(status.amountMinor)) over")
        case let status as ItemsStatusAddsUp:
            return String(localized: "Adds up: \(currency.format(status.totalMinor))")
        default:
            return ""
        }
    }

    static func looseCount(_ count: Int) -> String {
        count == 1 ? String(localized: "1 item with nobody") : String(localized: "\(count) items with nobody")
    }

    static func statusDetail(_ status: ItemsStatus, currency: GroupCurrency) -> String {
        switch status {
        case let status as ItemsStatusLoose:
            if status.isEveryItem {
                return String(localized: "Tap each item and check who had it. Done unlocks once every item has someone.")
            }
            let names = ExpenseFormat.names(status.items.map(\.name))
            let left = currency.format(status.looseMinor)
            return status.items.count == 1
                ? String(localized: "\(names) has nobody yet: \(left) out of the split.")
                : String(localized: "\(names) have nobody yet: \(left) out of the split.")
        case let status as ItemsStatusAmountLeft:
            return sum(status.sumMinor, total: status.totalMinor, isFromReceipt: status.isFromReceipt, currency: currency)
        case let status as ItemsStatusAmountOver:
            return sum(status.sumMinor, total: status.totalMinor, isFromReceipt: status.isFromReceipt, currency: currency)
        case let status as ItemsStatusAddsUp:
            let items = currency.format(status.itemsMinor)
            let service = currency.format(status.serviceMinor)
            let couvert = currency.format(status.couvertMinor)
            guard status.extraMinor != 0 else {
                return String(localized: "\(items) in items, \(service) of service and \(couvert) of cover.")
            }
            let extra = currency.formatSigned(status.extraMinor)
            return String(localized: "\(items) in items, \(service) of service, \(couvert) of cover and \(extra) of difference.")
        default:
            return ""
        }
    }

    private static func sum(_ sum: Int64, total: Int64, isFromReceipt: Bool, currency: GroupCurrency) -> String {
        let sum = currency.format(sum)
        let total = currency.format(total)
        return isFromReceipt
            ? String(localized: "Items, service and cover add up to \(sum), but the receipt says \(total).")
            : String(localized: "Items, service and cover add up to \(sum), but the total is \(total).")
    }

    /// "6 × R$ 14,00 · By unit", or "Nobody yet" for an item nobody is marked in.
    static func rowSubtitle(_ row: ItemsItemRow, currency: GroupCurrency) -> String {
        "\(row.item.quantity) × \(currency.format(row.item.unitPriceMinor)) · \(rowHow(row))"
    }

    private static func rowHow(_ row: ItemsItemRow) -> String {
        if row.isLoose { return String(localized: "Nobody yet") }
        switch row.item.mode {
        case .units: return String(localized: "By unit")
        case .shares: return String(localized: "By shares")
        case .amount: return String(localized: "By amount")
        case .percent: return String(localized: "By percentage")
        default: return String(localized: "Equal among \(row.consumers.count)")
        }
    }

    /// "R$ 120,00 in items + R$ 14,50 of service and cover", or only the cover before anything is marked for them.
    static func personSubtitle(_ row: ItemsPersonRow, currency: GroupCurrency) -> String {
        if row.itemsMinor > 0 {
            let items = currency.format(row.itemsMinor)
            return String(localized: "\(items) in items + \(currency.format(row.feesMinor)) of service and cover")
        }
        return row.feesMinor > 0 ? String(localized: "Only the cover, for now") : String(localized: "Nothing yet")
    }

    static func serviceTitle(_ items: ItemsEditor) -> String {
        String(localized: "Service (\(SplitBillCopy.percent(items.split.servicePercent)))")
    }

    static func couvertSubtitle(_ items: ItemsEditor) -> String {
        String(localized: "\(items.currency.format(items.split.couvertMinor)) per person · the same for everyone")
    }

    /// The item's status: the main line and the one under it.
    static func itemStatus(_ editor: ItemEditor) -> (title: String, detail: String) {
        let currency = editor.currency
        switch editor.status {
        case let status as ItemStatusUnitsAddUp:
            let quantity = Int(status.quantity)
            return (
                quantity == 1 ? String(localized: "1 of 1 unit") : String(localized: "\(quantity) of \(quantity) units"),
                String(localized: "Each unit costs \(currency.format(status.unitPriceMinor))."),
            )
        case let status as ItemStatusUnitsLeft:
            let missing = Int(status.missing)
            let marked = Int(status.marked)
            let quantity = Int(status.quantity)
            return missing == 1
                ? (String(localized: "1 unit left"), String(localized: "\(marked) of \(quantity) checked. Say who had the other one."))
                : (String(localized: "\(missing) units left"), String(localized: "\(marked) of \(quantity) checked. Say who had the others."))
        case let status as ItemStatusUnitsOver:
            let over = Int(status.over)
            let checked = String(localized: "\(Int(status.marked)) of \(Int(status.quantity)) checked.")
            return (over == 1 ? String(localized: "1 unit over") : String(localized: "\(over) units over"), checked)
        case let status as ItemStatusEach:
            return (
                String(localized: "\(currency.format(status.eachMinor)) each"),
                among(status.totalMinor, count: Int(status.count), currency: currency),
            )
        case let status as ItemStatusParts:
            let several = status.severalParts.map { parts in
                parts.person.isMe
                    ? String(localized: "You had \(Int(parts.parts)) shares.")
                    : String(localized: "\(parts.person.name) had \(Int(parts.parts)) shares.")
            }
            return (
                String(localized: "\(Int(status.parts)) shares of \(currency.format(status.partMinor))"),
                several.isEmpty ? among(status.totalMinor, count: Int(status.count), currency: currency) : several.joined(separator: " "),
            )
        case let status as ItemStatusAmountAddsUp:
            return (String(localized: "Adds up: \(currency.format(status.totalMinor))"), String(localized: "The parts add up to the item."))
        case let status as ItemStatusAmountLeft:
            return (String(localized: "\(currency.format(status.amountMinor)) left"), soFar(status.assignedMinor, status.totalMinor, currency))
        case let status as ItemStatusAmountOver:
            return (String(localized: "\(currency.format(status.amountMinor)) over"), soFar(status.assignedMinor, status.totalMinor, currency))
        case let status as ItemStatusPercentAddsUp:
            return (
                String(localized: "Adds up: \(SplitBillCopy.percent(100))"),
                among(status.totalMinor, count: Int(status.count), currency: currency),
            )
        case let status as ItemStatusPercentLeft:
            return (String(localized: "\(SplitBillCopy.percent(status.percent)) left"), percentSoFar(status.sum))
        case let status as ItemStatusPercentOver:
            return (String(localized: "\(SplitBillCopy.percent(status.percent)) over"), percentSoFar(status.sum))
        default:
            return (
                String(localized: "Nobody checked"),
                String(localized: "Check who had this item. Done unlocks once it has someone."),
            )
        }
    }

    private static func among(_ total: Int64, count: Int, currency: GroupCurrency) -> String {
        String(localized: "\(currency.format(total)) among \(count) people.")
    }

    private static func soFar(_ assigned: Int64, _ total: Int64, _ currency: GroupCurrency) -> String {
        String(localized: "\(currency.format(assigned)) of \(currency.format(total)) so far.")
    }

    private static func percentSoFar(_ sum: Int64) -> String {
        String(localized: "\(SplitBillCopy.percent(sum)) of \(SplitBillCopy.percent(100)) so far.")
    }

    static func listLabel(_ mode: ItemSplitMode) -> LocalizedStringKey {
        switch mode {
        case .units: "How many each one had"
        case .shares: "Each one's shares"
        case .amount: "How much each one had"
        case .percent: "Each one's percentage"
        default: "Who shared it"
        }
    }

    static func hint(_ mode: ItemSplitMode, currency: GroupCurrency) -> String {
        switch mode {
        case .units: String(localized: "Units only shows when the item has more than one.")
        case .shares: String(localized: "Use shares when someone eats more of the same dish.")
        case .amount: String(localized: "Whoever is left at \(currency.format(0)) is out of the item.")
        case .percent: String(localized: "Whole numbers only.")
        default: String(localized: "The cents left over go to whoever paid.")
        }
    }

    /// "“Long neck beer” (R$ 84,00) leaves the bill and the service is redone. The items no longer add up…"
    static func deletion(_ deletion: ItemDeletion, currency: GroupCurrency) -> String {
        let amount = currency.format(deletion.totalMinor)
        let first = deletion.redoesService
            ? String(localized: "“\(deletion.name)” (\(amount)) leaves the bill and the service is redone.")
            : String(localized: "“\(deletion.name)” (\(amount)) leaves the bill.")
        guard let left = deletion.leftMinor else { return first }
        return "\(first) \(String(localized: "The items no longer add up to the total: \(currency.format(left.int64Value)) is left to split."))"
    }

    static func feeTitle(_ fee: ItemsFee) -> LocalizedStringKey {
        fee == .service ? "Service" : "Cover"
    }

    static func feeField(_ fee: ItemsFee) -> String {
        fee == .service ? String(localized: "Service percentage") : String(localized: "Cover per person")
    }

    static func feeHelp(_ fee: ItemsFee) -> String {
        fee == .service
            ? String(localized: "0 for a bill without service.")
            : String(localized: "The same for everyone at the table. 0 for none.")
    }
}
