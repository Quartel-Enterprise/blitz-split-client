import Foundation
import Observation
import Shared
import WidgetKit

/// Writes the balance widget's file (`WidgetBalancesStore`) from the shared code's snapshot, whenever a group, a bill
/// or a payment changes here or through a sync, when Profile's language, Time format or bitcoin unit change, and when
/// the app goes to the background, then reloads the widget. Only what the widget would draw differently is written
/// and reloaded, so going to the background spends none of WidgetKit's daily reloads unless something changed, and a
/// burst of changes (a first sync) is written once. The widget's process has no Room: it only reads this file.
@MainActor
final class BalanceWidgetWriter {
    static let shared = BalanceWidgetWriter()

    /// How long a burst of snapshots waits before its last one is written.
    private static let settleDelay: Duration = .milliseconds(400)

    private let watches = WatchBag()
    private var snapshot: BalanceSnapshot?
    private var written: WidgetBalances?
    private var pendingWrite: Task<Void, Never>?

    private init() {}

    /// Called once the shared code started, from the app's launch.
    func start() {
        watches.add(BlitzSplitApple.shared.createBalanceSnapshotWatcher().observe { [weak self] snapshot in
            self?.snapshot = snapshot
            self?.scheduleWrite()
        })
        observeFormats()
    }

    /// The app going to the background, or Profile's language changed: written again only if the widget would draw
    /// it differently.
    func refresh() {
        pendingWrite?.cancel()
        write()
    }

    private func scheduleWrite() {
        pendingWrite?.cancel()
        pendingWrite = Task { [weak self] in
            try? await Task.sleep(for: Self.settleDelay)
            guard !Task.isCancelled else { return }
            self?.write()
        }
    }

    /// The bitcoin unit and the Time format arrive from the shared code after launch: a snapshot written before them
    /// is written again in them.
    private func observeFormats() {
        withObservationTracking {
            _ = BitcoinUnit.shared.showsSats
            _ = ClockFormat.shared.locale
        } onChange: {
            Task { @MainActor [weak self] in
                self?.scheduleWrite()
                self?.observeFormats()
            }
        }
    }

    private func write() {
        guard let snapshot else { return }
        let balances = WidgetBalances(snapshot: snapshot, language: Bundle.appLanguageIdentifier)
        if let written, written.drawsLike(balances) { return }
        guard WidgetBalancesStore.write(balances) else { return }
        written = balances
        WidgetCenter.shared.reloadTimelines(ofKind: WidgetBalancesStore.kind)
    }
}
