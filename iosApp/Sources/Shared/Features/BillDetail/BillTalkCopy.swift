import Foundation
import Shared
import SwiftUI

/// One "before → after" under a line of History: "Bar → Restaurant", "Carla R$ 112.00 → R$ 98.00", or only the field
/// ("Split") over the parts that moved.
struct BillHistoryDiff: Hashable {
    let field: String?
    let before: String?
    let after: String?
}

/// The words of Bill's Comments and History (7.3): the shared code hands over who, when and each change's before and
/// after, and each platform writes its own sentences.
extension BillCopy {
    // MARK: - Comments

    /// "Comments · 3", or just "Comments" while there is none.
    static func commentsHeader(_ count: Int32) -> String {
        let title = String(localized: "Comments", comment: "The bill's comments section")
        return count > 0 ? "\(title) · \(Int(count))" : title
    }

    /// "You", the author's name, or "Someone" when the server kept no name.
    static func author(_ comment: BillCommentRow) -> String {
        if comment.isMe { return String(localized: "You") }
        return comment.authorName ?? String(localized: "Someone", comment: "Who did something in Activity, unnamed")
    }

    /// "Today, 10:40 PM", "Yesterday, 10:40 PM" or "Oct 27, 10:40 PM".
    static func posted(_ comment: BillCommentRow) -> String {
        ExpenseFormat.dayTime(comment.postedAt, relativeDay: comment.postedDay)
    }

    /// "@Diego Pay me tomorrow": a reply to a reply names whom it answers, in the accent.
    static func body(_ comment: BillCommentRow) -> Text {
        guard let name = comment.replyToName else { return Text(verbatim: comment.body) }
        // Why: what people wrote is never looked up as a localized key
        var mention = AttributedString("@\(name) ")
        mention.foregroundColor = Palette.accent
        mention.inlinePresentationIntent = .stronglyEmphasized
        return Text(mention + AttributedString(comment.body))
    }

    /// "Replying to Diego" or "Replying to you", over the composer.
    static func replying(_ target: BillReplyTarget) -> String {
        if target.isMe { return String(localized: "Replying to you") }
        let name = target.name ?? String(localized: "Someone", comment: "Who did something in Activity, unnamed")
        return String(localized: "Replying to \(name)")
    }

    // MARK: - History

    /// "Today", "Yesterday" or "Fri, Oct 24", over the day's lines.
    static func historyDay(_ day: BillHistoryDay) -> String {
        ExpenseFormat.day(day.dayStartMillis, relativeDay: day.relativeDay)
    }

    /// "**Ana** changed the category": what someone did, their name bolder.
    static func headline(_ entry: BillHistoryEntry) -> Text {
        let name = entry.actorIsMe
            ? String(localized: "You")
            : entry.actorName ?? String(localized: "Someone", comment: "Who did something in Activity, unnamed")
        let who = Text(verbatim: name).fontWeight(.semibold)
        switch entry.kind {
        case .created:
            return entry.isPending
                ? Text("\(who) created the bill offline", comment: "Bill history: created and not up yet")
                : Text("\(who) created the bill", comment: "Bill history")
        case .attachmentsAdded:
            if entry.isReceipt { return Text("\(who) attached the receipt", comment: "Bill history") }
            let count = Int(entry.attachmentCount)
            return count > 1
                ? Text("\(who) attached \(count) photos", comment: "Bill history: photos added in a row")
                : Text("\(who) attached a photo", comment: "Bill history")
        case .attachmentRemoved:
            return entry.isReceipt
                ? Text("\(who) removed the receipt", comment: "Bill history")
                : Text("\(who) removed a photo", comment: "Bill history")
        case .linkChanged:
            if entry.linkVisibility == .anyone {
                return Text("\(who) turned on the link for anyone", comment: "Bill history: the share link")
            }
            if entry.linkVisibility == .members {
                return Text("\(who) made the link only for people in the bill", comment: "Bill history: the share link")
            }
            return Text("\(who) turned off the link", comment: "Bill history: the share link")
        default:
            return edited(entry.changes.first, who: who, isPending: entry.isPending)
        }
    }

    /// An edit is named by its first change: "changed the category", "changed the split"…
    private static func edited(_ change: (any BillHistoryChange)?, who: Text, isPending: Bool) -> Text {
        switch change {
        case is BillHistoryChangeAmount: Text("\(who) changed the amount", comment: "Bill history")
        case is BillHistoryChangeDescription: Text("\(who) changed the description", comment: "Bill history")
        case is BillHistoryChangeSpentAt: Text("\(who) changed the date", comment: "Bill history")
        case is BillHistoryChangeCategory: Text("\(who) changed the category", comment: "Bill history")
        case is BillHistoryChangePayers: Text("\(who) changed who paid", comment: "Bill history")
        case is BillHistoryChangeSplit: Text("\(who) changed the split", comment: "Bill history")
        default:
            isPending
                ? Text("\(who) edited the bill offline", comment: "Bill history: edited and not up yet")
                : Text("\(who) edited the bill", comment: "Bill history")
        }
    }

    /// The SF Symbol on the face's corner: what kind of thing was done.
    static func historyIcon(_ entry: BillHistoryEntry) -> String {
        switch entry.kind {
        case .created: "plus"
        case .attachmentsAdded: entry.isReceipt ? "receipt" : "photo"
        case .attachmentRemoved: "trash"
        case .linkChanged: "link"
        default: entry.changes.first is BillHistoryChangeCategory ? "tag" : "pencil"
        }
    }

    /// Created's note: "Split equally among 5 · R$ 312.00", leaving out what the server didn't keep.
    static func createdNote(_ entry: BillHistoryEntry, currency: GroupCurrency) -> String? {
        guard entry.kind == .created else { return nil }
        let split: String? = if let mode = entry.splitMode {
            entry.sharesCount.map { splitHeader(mode, count: $0.int32Value) } ?? modeName(mode)
        } else {
            nil
        }
        let amount = entry.amountMinor.map { currency.format($0.int64Value) }
        let parts = [split, amount].compactMap { $0 }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }

    /// What an edit changed, the first change without its field (the headline names it) and the others with it.
    static func diffs(_ entry: BillHistoryEntry, currency: GroupCurrency, categories: CategoryTree) -> [BillHistoryDiff] {
        guard entry.kind == .edited else { return [] }
        return entry.changes.enumerated().flatMap { index, change in
            diffs(change, isFirst: index == 0, currency: currency, categories: categories)
        }
    }

    private static func diffs(
        _ change: any BillHistoryChange,
        isFirst: Bool,
        currency: GroupCurrency,
        categories: CategoryTree,
    ) -> [BillHistoryDiff] {
        let none = "—"
        func line(_ field: String, _ before: String?, _ after: String?) -> [BillHistoryDiff] {
            [BillHistoryDiff(field: isFirst ? nil : field, before: before ?? none, after: after ?? none)]
        }
        switch change {
        case let amount as BillHistoryChangeAmount:
            return line(
                String(localized: "Amount"),
                amount.beforeMinor.map { currency.format($0.int64Value) },
                amount.afterMinor.map { currency.format($0.int64Value) },
            )
        case let description as BillHistoryChangeDescription:
            return line(
                String(localized: "Description"),
                description.before.map { "“\($0)”" },
                description.after.map { "“\($0)”" },
            )
        case let spentAt as BillHistoryChangeSpentAt:
            return line(
                String(localized: "When"),
                spentAt.beforeMillis.map { ExpenseFormat.dayTime($0.int64Value, relativeDay: .other) },
                spentAt.afterMillis.map { ExpenseFormat.dayTime($0.int64Value, relativeDay: .other) },
            )
        case let category as BillHistoryChangeCategory:
            // A null category is "Other", as the bill reads it
            return line(
                String(localized: "Category"),
                CategoryCopy.name(categories.resolve(id: category.beforeId)),
                CategoryCopy.name(categories.resolve(id: category.afterId)),
            )
        case let payers as BillHistoryChangePayers:
            // The same payers paying other amounts say each amount that moved, as the split's parts do
            guard payers.parts.isEmpty else { return partDiffs(payers.parts, currency: currency) }
            return line(String(localized: "Who paid"), payerNames(payers.before), payerNames(payers.after))
        case let split as BillHistoryChangeSplit:
            return splitDiffs(split, isFirst: isFirst, currency: currency)
        default:
            return []
        }
    }

    /// "Equal → Shares" when the mode changed (under "Split" when it isn't the first change), then each part that
    /// moved: "Carla R$ 112.00 → R$ 98.00", "out" on the side someone wasn't in.
    private static func splitDiffs(
        _ split: BillHistoryChangeSplit,
        isFirst: Bool,
        currency: GroupCurrency,
    ) -> [BillHistoryDiff] {
        var lines: [BillHistoryDiff] = []
        let field = String(localized: "Split")
        if let before = split.beforeMode, let after = split.afterMode, before != after {
            lines.append(BillHistoryDiff(field: isFirst ? nil : field, before: modeName(before), after: modeName(after)))
        } else if !isFirst {
            lines.append(BillHistoryDiff(field: field, before: nil, after: nil))
        }
        return lines + partDiffs(split.parts, currency: currency)
    }

    /// "Carla R$ 112.00 → R$ 98.00" for each part that moved, "out" on the side someone wasn't in.
    private static func partDiffs(_ parts: [BillHistoryPart], currency: GroupCurrency) -> [BillHistoryDiff] {
        let out = String(localized: "out", comment: "Bill history: someone not in the split on that side")
        return parts.map { part in
            BillHistoryDiff(
                field: part.isMe ? String(localized: "You") : part.name ?? formerMember,
                before: part.beforeMinor.map { currency.format($0.int64Value) } ?? out,
                after: part.afterMinor.map { currency.format($0.int64Value) } ?? out,
            )
        }
    }

    /// "Ana and Bruno"; Kotlin hands a list of nullable names over as NSNull for each null.
    private static func payerNames(_ names: [Any]) -> String? {
        guard !names.isEmpty else { return nil }
        return ExpenseFormat.names(names.map { ($0 as? String) ?? formerMember })
    }

    /// "Equal", "Amounts", "Percentage", "Shares" or "Items".
    static func modeName(_ mode: SplitMode) -> String {
        switch mode {
        case .amount: String(localized: "Amounts")
        case .percent: String(localized: "Percentage")
        case .shares: String(localized: "Shares")
        case .items: String(localized: "Items")
        default: String(localized: "Equal")
        }
    }
}
