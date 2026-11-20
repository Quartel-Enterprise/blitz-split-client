import Foundation
import Shared

/// The widget's file from the shared code's snapshot, as the iPhone app and the Apple Watch app both write it
/// (`BalanceWidgetWriter`, `WatchWidgetWriter`): every amount written by the app's own formatter, so the widget's
/// process, which has no `Shared` framework, only picks the words.
extension WidgetBalances {
    /// `language`: the `.lproj` the faces draw their words in; nil for the device's.
    init(snapshot: BalanceSnapshot, language: String?) {
        let overall = snapshot.overall.map { total in
            Overall(
                signed: total.currency.formatSigned(total.netMinor),
                compact: total.currency.formatSignedCompact(total.netMinor),
                direction: .init(minor: total.netMinor),
                // Home's "Owe you" and "You owe", each in the first currency with something that way
                owed: snapshot.toReceive.map { $0.currency.format($0.toReceiveMinor) },
                owing: snapshot.toPay.map { $0.currency.format($0.toPayMinor) },
            )
        }
        let groups = snapshot.openGroups.map { group in
            Group(
                id: "\(group.groupId):\(group.currency.code)",
                name: group.name,
                signed: group.currency.formatSigned(group.netMinor),
                direction: .init(minor: group.netMinor),
            )
        }
        self.init(
            language: language,
            isSettled: snapshot.isSettled,
            overall: overall,
            groups: groups,
            updatedAt: .now,
            // "Updated at 14:32": when the figures were last written, on Profile's Time format
            updatedTime: ExpenseFormat.time(Int64(Date.now.timeIntervalSince1970 * 1000)),
        )
    }

    /// The same figures, words and language, whenever they were written: nothing to write again nor reload.
    func drawsLike(_ other: WidgetBalances) -> Bool {
        var other = other
        other.updatedAt = updatedAt
        other.updatedTime = updatedTime
        return self == other
    }
}
