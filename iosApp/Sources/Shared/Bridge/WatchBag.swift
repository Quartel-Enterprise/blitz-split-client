import Shared

/// Keeps the `Watch`es a screen opened on its ViewModel's flows and cancels them all when the screen
/// goes away, so no callback outlives the view.
final class WatchBag {
    private var watches: [any Watch] = []

    func add(_ watch: any Watch) {
        watches.append(watch)
    }

    isolated deinit {
        watches.forEach { $0.cancel() }
    }
}
