import Shared
import SwiftUI

/// The words of the group's "Default split" (6.7), which Edit group's row, its own screen and Split's notice share.
enum DefaultSplitCopy {
    /// Equal, Shares, Percentage or By income.
    static func modeLabel(_ mode: DefaultSplitMode) -> String {
        switch mode {
        case .shares: String(localized: "Shares")
        case .percent: String(localized: "Percentage")
        case .income: String(localized: "By income")
        default: String(localized: "Equal")
        }
    }

    /// Under each way to split: "Everyone pays the same part", "E.g. 3 to 1. Good for couples and families"…
    static func modeHint(_ mode: DefaultSplitMode) -> String {
        switch mode {
        case .shares: String(localized: "E.g. 3 to 1. Good for couples and families")
        case .percent: String(localized: "E.g. \(SplitBillCopy.percent(75)) and \(SplitBillCopy.percent(25))")
        case .income: String(localized: "Each one's part follows their income")
        default: String(localized: "Everyone pays the same part")
        }
    }

    /// Above the people: "Who's in the split", "Each one's shares", "Each one's percentage" or "Each one's income".
    static func listLabel(_ mode: DefaultSplitMode) -> String {
        switch mode {
        case .shares: String(localized: "Each one's shares")
        case .percent: String(localized: "Each one's percentage")
        case .income: String(localized: "Each one's income")
        default: String(localized: "Who's in the split")
        }
    }

    /// "Carla joined the group. They start with 1 share; adjust if needed.", for one or more people.
    static func joined(_ names: [String], mode: DefaultSplitMode) -> String {
        let isOne = names.count == 1
        let list = ExpenseFormat.names(names)
        let zero = SplitBillCopy.percent(0)
        let joined = isOne
            ? String(localized: "\(list) joined the group")
            : String(
                localized: "Several people joined the group",
                defaultValue: "\(list) joined the group",
                comment: "Default split: %@ are the names of the people who joined since it was saved",
            )
        let start = switch mode {
        case .shares:
            isOne ? String(localized: "They start with 1 share") : String(localized: "Each one starts with 1 share")
        case .percent, .income:
            isOne ? String(localized: "They start at \(zero)") : String(localized: "Each one starts at \(zero)")
        default:
            isOne
                ? String(localized: "They're already in the split")
                : String(
                    localized: "Several people are already in the split",
                    defaultValue: "They're already in the split",
                    comment: "Default split: the people who joined since it was saved are in it",
                )
        }
        return String(localized: "\(joined). \(start); adjust if needed.")
    }

    /// "Shares, 3 to 1", "Percentage, 75% and 25%", "Equal, without Diego": what a bill opens with.
    static func description(_ split: BillDefaultSplit) -> String {
        let mode = modeLabel(split.mode)
        let values = split.values.map { $0.int64Value }
        let detail: String? = switch split.mode {
        case .shares:
            values.count == 2
                ? String(localized: "\(Int(values[0])) to \(Int(values[1]))")
                : ExpenseFormat.names(values.map { "\($0)" })
        case .percent, .income:
            ExpenseFormat.names(values.map { SplitBillCopy.percent($0) })
        default:
            split.excluded.isEmpty
                ? nil
                : String(localized: "without \(ExpenseFormat.names(split.excluded.map(SplitBillCopy.name)))")
        }
        guard let detail else { return mode }
        return "\(mode), \(detail)"
    }
}
