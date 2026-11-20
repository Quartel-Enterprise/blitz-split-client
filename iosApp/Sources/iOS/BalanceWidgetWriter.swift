import Foundation
import Shared
import WidgetKit

/// Writes the balance widget's file (`WidgetBalancesStore`) from the shared code's snapshot, whenever a group, a bill
/// or a payment changes here or through a sync, and again when the app goes to the background, so "Updated at" says
/// when the app last saw them, in the language and Time format picked by then. The widget's process has no Room: it
/// only reads this file.
@MainActor
final class BalanceWidgetWriter {
    static let shared = BalanceWidgetWriter()

    private let watches = WatchBag()
    private var snapshot: BalanceSnapshot?

    private init() {}

    /// Called once the shared code started, from the app's launch.
    func start() {
        watches.add(BlitzSplitApple.shared.createBalanceSnapshotWatcher().observe { [weak self] snapshot in
            self?.snapshot = snapshot
            self?.write()
        })
    }

    /// The app going to the background: the same balances, written again in the language and formats picked since.
    func refresh() {
        write()
    }

    private func write() {
        guard let snapshot else { return }
        guard WidgetBalancesStore.write(Self.balances(of: snapshot)) else { return }
        WidgetCenter.shared.reloadTimelines(ofKind: WidgetBalancesStore.kind)
    }

    private static func balances(of snapshot: BalanceSnapshot) -> WidgetBalances {
        let overall = snapshot.overall.map { total in
            WidgetBalances.Overall(
                signed: total.currency.formatSigned(total.netMinor),
                compact: total.currency.formatSignedCompact(total.netMinor),
                direction: .init(minor: total.netMinor),
                owed: total.toReceiveMinor > 0 ? total.currency.format(total.toReceiveMinor) : nil,
                owing: total.toPayMinor > 0 ? total.currency.format(total.toPayMinor) : nil,
            )
        }
        let groups = snapshot.openGroups.map { group in
            WidgetBalances.Group(
                id: "\(group.groupId):\(group.currency.code)",
                name: group.name,
                signed: group.currency.formatSigned(group.netMinor),
                direction: .init(minor: group.netMinor),
            )
        }
        return WidgetBalances(
            language: Bundle.appLanguageIdentifier,
            isSettled: snapshot.isSettled,
            overall: overall,
            groups: groups,
            updatedAt: .now,
            // "Updated at 14:32": when the app last wrote the file, on Profile's Time format
            updatedTime: ExpenseFormat.time(Int64(Date.now.timeIntervalSince1970 * 1000)),
        )
    }
}
