import Observation
import Shared
import SwiftUI

/// Swift's view of the watch's back stack entries: Hashable, so a NavigationStack path can hold them.
enum WatchRoute: Hashable {
    /// 15.9 to 15.13, until the watch has the account, and again after a sign out.
    case connect
    /// "Balances" (15.3), the root once connected.
    case balances
    /// A group in one currency (15.4); holds the Kotlin route itself, which its ViewModel reads the group from.
    case group(RouteWatchGroup)

    init?(_ route: Route) {
        switch route {
        case is RouteWatchConnect: self = .connect
        case is RouteWatchBalances: self = .balances
        case let group as RouteWatchGroup: self = .group(group)
        default: return nil
        }
    }
}

/// The watch app's root (`WatchRootViewModel`): it starts the watch's sync and picks the first screen, and the shared
/// back stack moves the screens from then on, as the phone's does.
@Observable
final class WatchRootModel {
    private(set) var isReady: Bool
    private(set) var root: WatchRoute = .connect
    /// What the NavigationStack pushes over the root.
    var path: [WatchRoute] = [] {
        didSet {
            // The system's back (the chevron, the swipe) popped the stack: the shared one follows
            if path.count < oldValue.count, path != pushed {
                BlitzSplitWatch.shared.popTo(size: Int32(path.count + 1))
            }
        }
    }

    @ObservationIgnored private var pushed: [WatchRoute] = []
    @ObservationIgnored private let holder = BlitzSplitWatch.shared.createWatchRootViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        isReady = holder.viewModel.isReady.value as? Bool ?? false
        watches.add(holder.viewModel.readyWatcher.observe { [weak self] ready in
            self?.isReady = ready.boolValue
        })
        backStackChanged(BlitzSplitWatch.shared.backStack)
        watches.add(BlitzSplitWatch.shared.createBackStackWatcher().observe { [weak self] routes in
            self?.backStackChanged(routes as? [Route] ?? [])
        })
    }

    private func backStackChanged(_ routes: [Route]) {
        let mapped = routes.compactMap(WatchRoute.init)
        guard let first = mapped.first else { return }
        // Connected: the system's prompt for the watch's own pushes, once
        if first == .balances, !WatchStoreScreenshots.isRun { WatchPush.shared.requestPermission() }
        root = first
        pushed = Array(mapped.dropFirst())
        if path != pushed { path = pushed }
    }

    isolated deinit {
        holder.clear()
    }
}

/// The watch's one window: the connect screens or the balances, and the group pushed over them.
struct WatchRootView: View {
    @Bindable var model: WatchRootModel

    var body: some View {
        Group {
            if model.isReady {
                NavigationStack(path: $model.path) {
                    screen(model.root)
                        .navigationDestination(for: WatchRoute.self) { route in
                            screen(route)
                        }
                }
            } else {
                // The first screen is chosen in a moment: a paired watch never flashes the connect screen
                Color.black.ignoresSafeArea()
            }
        }
        .preferredColorScheme(.dark)
        .tint(WatchPalette.accent)
    }

    @ViewBuilder
    private func screen(_ route: WatchRoute) -> some View {
        switch route {
        case .connect: WatchPairingView()
        case .balances: WatchBalancesView()
        case let .group(group): WatchGroupView(route: group).id(group)
        }
    }
}
