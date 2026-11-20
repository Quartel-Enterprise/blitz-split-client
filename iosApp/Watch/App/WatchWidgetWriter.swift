import Foundation
import Shared
import WidgetKit

/// Writes the watch's complications and Smart Stack card file (`WidgetBalancesStore`, in the watch's own App Group
/// container) from the shared code's snapshot, as the iPhone app writes its widget's: whenever the watch's Room changes
/// through a payment here or a sync, then reloads them. Only what they would draw differently is written, and a burst
/// of changes (a first sync) is written once, so WidgetKit's reload budget goes to real changes.
@MainActor
final class WatchWidgetWriter {
    static let shared = WatchWidgetWriter()

    private static let settleDelay: Duration = .milliseconds(400)

    private let watches = WatchBag()
    private var written: WidgetBalances?
    private var pendingWrite: Task<Void, Never>?

    private init() {}

    /// Called once the shared code started, from the app's launch.
    func start() {
        watches.add(BlitzSplitApple.shared.createBalanceSnapshotWatcher().observe { [weak self] snapshot in
            self?.scheduleWrite(snapshot)
        })
    }

    private func scheduleWrite(_ snapshot: BalanceSnapshot) {
        pendingWrite?.cancel()
        pendingWrite = Task { [weak self] in
            try? await Task.sleep(for: Self.settleDelay)
            guard !Task.isCancelled else { return }
            self?.write(snapshot)
        }
    }

    private func write(_ snapshot: BalanceSnapshot) {
        let balances = Self.balances(of: snapshot)
        if let written, written.drawsLike(balances) { return }
        guard WidgetBalancesStore.write(balances) else { return }
        written = balances
        WidgetCenter.shared.reloadTimelines(ofKind: WidgetBalancesStore.kind)
    }

    /// The overall balance in the currency with the most groups (the Tile's and complication's rule), as the iPhone's
    /// widget reads it.
    private static func balances(of snapshot: BalanceSnapshot) -> WidgetBalances {
        let overall = snapshot.overall.map { total in
            WidgetBalances.Overall(
                signed: total.currency.formatSigned(total.netMinor),
                compact: total.currency.formatSignedCompact(total.netMinor),
                direction: .init(minor: total.netMinor),
                owed: snapshot.toReceive.map { $0.currency.format($0.toReceiveMinor) },
                owing: snapshot.toPay.map { $0.currency.format($0.toPayMinor) },
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
            language: nil,
            isSettled: snapshot.isSettled,
            overall: overall,
            groups: groups,
            updatedAt: .now,
            updatedTime: ExpenseFormat.time(Int64(Date.now.timeIntervalSince1970 * 1000)),
        )
    }
}

private extension WidgetBalances {
    func drawsLike(_ other: WidgetBalances) -> Bool {
        var other = other
        other.updatedAt = updatedAt
        other.updatedTime = updatedTime
        return self == other
    }
}
