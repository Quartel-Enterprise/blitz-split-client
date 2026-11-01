/// The pulls to refresh waiting for the sync they asked for: each returns once the shared state says the sync ended,
/// so the spinner stays exactly as long as the sync.
final class PendingRefreshes {
    private var continuations: [CheckedContinuation<Void, Never>] = []

    /// Waits until `endAll`, or returns at once when the event started no sync (offline).
    func wait(isRefreshing: Bool) async {
        guard isRefreshing else { return }
        await withCheckedContinuation { continuation in
            continuations.append(continuation)
        }
    }

    /// The state says the sync ended, or the screen went away: every pull waiting returns.
    func endAll() {
        let ended = continuations
        continuations = []
        ended.forEach { $0.resume() }
    }
}
