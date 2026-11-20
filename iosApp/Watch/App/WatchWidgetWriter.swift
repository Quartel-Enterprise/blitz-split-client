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
    /// The snapshot `pendingWrite` waits to write.
    private var pendingSnapshot: BalanceSnapshot?

    private init() {}

    /// Called once the shared code started, from the app's launch.
    func start() {
        watches.add(BlitzSplitApple.shared.createBalanceSnapshotWatcher().observe { [weak self] snapshot in
            self?.scheduleWrite(snapshot)
        })
    }

    /// A background refresh ends right after its sync, and watchOS suspends the app then, before a write still
    /// settling would run: this waits for the sync's changes to reach the snapshot and writes them before the task
    /// ends.
    func writeAfterSync() async {
        try? await Task.sleep(for: Self.settleDelay)
        pendingWrite?.cancel()
        pendingWrite = nil
        guard let snapshot = pendingSnapshot else { return }
        pendingSnapshot = nil
        write(snapshot)
    }

    private func scheduleWrite(_ snapshot: BalanceSnapshot) {
        pendingSnapshot = snapshot
        pendingWrite?.cancel()
        pendingWrite = Task { [weak self] in
            try? await Task.sleep(for: Self.settleDelay)
            guard !Task.isCancelled, let self else { return }
            self.pendingSnapshot = nil
            self.write(snapshot)
        }
    }

    private func write(_ snapshot: BalanceSnapshot) {
        // The device's language: the watch has no Profile › Language of its own
        let balances = WidgetBalances(snapshot: snapshot, language: nil)
        if let written, written.drawsLike(balances) { return }
        guard WidgetBalancesStore.write(balances) else { return }
        written = balances
        WidgetCenter.shared.reloadTimelines(ofKind: WidgetBalancesStore.kind)
    }
}
