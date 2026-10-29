import Observation
import Shared
import SwiftUI

/// The app-wide ViewModel: whether the start route is known yet and the shared back stack, which
/// the ViewModels move and SwiftUI only draws.
@Observable
final class AppModel {
    private(set) var isReady: Bool
    private(set) var backStack: [AppRoute]
    private(set) var theme: ThemePreference

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createAppViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        isReady = (viewModel.uiState.value as? AppUiState) == .ready
        backStack = Self.routes(of: viewModel.navigator.backStack.value)
        theme = viewModel.theme.value as? ThemePreference ?? .system
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.isReady = state == .ready
        })
        watches.add(viewModel.backStackWatcher.observe { [weak self] routes in
            self?.backStack = Self.routes(of: routes)
        })
        watches.add(viewModel.themeWatcher.observe { [weak self] theme in
            self?.theme = theme
        })
    }

    var root: AppRoute {
        backStack.first ?? .welcome
    }

    /// The entries above the root, but the one a sheet shows. A native back gesture shortens it, and
    /// the shared back stack catches up with how many entries remain.
    var path: [AppRoute] {
        get { Array(backStack.dropFirst().dropLast(sheet == nil ? 0 : 1)) }
        set {
            guard newValue.count < path.count else { return }
            holder.viewModel.navigator.popTo(size: Int32(newValue.count + 1))
        }
    }

    /// The top entry when it opens as a sheet instead of a pushed screen (a key on iPad and Mac,
    /// deleting the account).
    /// Closing the sheet (a swipe down, Esc) pops it, as the back button pops a pushed screen.
    var sheet: AppRoute? {
        get {
            guard backStack.count > 1, let top = backStack.last, top.presentsAsSheet else { return nil }
            return top
        }
        set {
            guard newValue == nil, sheet != nil else { return }
            holder.viewModel.navigator.popTo(size: Int32(backStack.count - 1))
        }
    }

    isolated deinit {
        holder.clear()
    }

    private static func routes(of value: Any?) -> [AppRoute] {
        let routes = (value as? [Any])?.compactMap { $0 as? any Route } ?? []
        return routes.isEmpty ? [.welcome] : routes.map { AppRoute($0) }
    }
}
