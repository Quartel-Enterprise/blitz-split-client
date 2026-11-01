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
    /// Whether the window has an iPad's room (`ScreenLayout.isLarge`), which decides whether a key or a bill opens as
    /// a sheet. Nil until the root first measures the window, so nothing is pushed or presented by a guess; it only
    /// changes when the window crosses the threshold, never on each step of a live resize.
    private(set) var isLargeWindow: Bool?

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

    /// Whether the root measured the window yet: until then it draws nothing, as while the start route loads.
    var isWindowMeasured: Bool {
        isLargeWindow != nil
    }

    /// The root's measure of the window, kept only when it moves `isLargeWindow`.
    func windowMeasured(_ window: ScreenLayout) {
        guard isLargeWindow != window.isLarge else { return }
        isLargeWindow = window.isLarge
    }

    var root: AppRoute {
        backStack.first ?? .welcome
    }

    /// The entries above the root, but the ones the sheets show. A native back gesture shortens it, and
    /// the shared back stack catches up with how many entries remain.
    var path: [AppRoute] {
        get { Array(backStack.dropFirst().dropLast(modalCount)) }
        set {
            guard newValue.count < path.count else { return }
            holder.viewModel.navigator.popTo(size: Int32(newValue.count + 1))
        }
    }

    /// The entry a sheet shows instead of a pushed screen (a key or a bill with an iPad's room and on the Mac,
    /// deleting the account): the top one, or the one under it while a second sheet stacks over it or a screen is
    /// pushed inside it.
    /// Closing the sheet (a swipe down, Esc) pops it, as the back button pops a pushed screen.
    var sheet: AppRoute? {
        get {
            if isSheetStacked { return backStack[backStack.count - 2] }
            if sheetPushCount > 0 { return backStack[backStack.count - 1 - sheetPushCount] }
            guard backStack.count > 1, let top = backStack.last, presentsAsSheet(top), invitePopover == nil else {
                return nil
            }
            return top
        }
        set {
            guard newValue == nil, sheet != nil else { return }
            let shown = isSheetStacked ? 2 : 1 + sheetPushCount
            holder.viewModel.navigator.popTo(size: Int32(backStack.count - shown))
        }
    }

    /// The entries pushed inside the sheet (the default split inside Edit group's): its own NavigationStack's path.
    /// Its back button shortens it, and the shared back stack catches up.
    var sheetPath: [AppRoute] {
        get { Array(backStack.suffix(sheetPushCount)) }
        set {
            guard newValue.count < sheetPushCount else { return }
            holder.viewModel.navigator.popTo(size: Int32(backStack.count - sheetPushCount + newValue.count))
        }
    }

    /// How many entries on top are pushed inside the sheet right under them.
    private var sheetPushCount: Int {
        let pushed = backStack.reversed().prefix { $0.pushesInSheet }.count
        guard pushed > 0, backStack.count > pushed + 1, presentsAsSheet(backStack[backStack.count - 1 - pushed]) else {
            return 0
        }
        return pushed
    }

    /// The top entry when it opens as a sheet over another sheet: editing a bill open as a sheet (iPad's room, Mac).
    var stackedSheet: AppRoute? {
        get { isSheetStacked ? backStack.last : nil }
        set {
            guard newValue == nil, stackedSheet != nil else { return }
            holder.viewModel.navigator.popTo(size: Int32(backStack.count - 1))
        }
    }

    /// The two top entries both open as sheets, so the second one stacks over the first instead of taking its place.
    private var isSheetStacked: Bool {
        backStack.count > 2 && presentsAsSheet(backStack[backStack.count - 1])
            && presentsAsSheet(backStack[backStack.count - 2])
    }

    /// Before the window is measured no entry opens as a sheet, and the root draws none of the stack either.
    private func presentsAsSheet(_ route: AppRoute) -> Bool {
        guard let isLargeWindow else { return false }
        return route.presentsAsSheet(isLargeWindow: isLargeWindow)
    }

    private var modalCount: Int {
        if isSheetStacked { return 2 }
        if sheetPushCount > 0 { return 1 + sheetPushCount }
        return sheet == nil && cover == nil && invitePopover == nil ? 0 : 1
    }

    /// The Mac's invite over the group's tab that opened it (F2Mac): a popover on that tab's "Invite people" rather
    /// than a sheet. The new group's invite has no group behind it, so it stays a sheet. Closing it pops it.
    var invitePopover: InvitePopover? {
        get {
            #if os(macOS)
            guard backStack.count > 2, case let .invite(route) = backStack[backStack.count - 1] else { return nil }
            let host = backStack[backStack.count - 2]
            guard host.isGroupTab else { return nil }
            return InvitePopover(host: host, route: route)
            #else
            return nil
            #endif
        }
        set {
            guard newValue == nil, invitePopover != nil else { return }
            holder.viewModel.navigator.popTo(size: Int32(backStack.count - 1))
        }
    }

    /// The top entry when it covers the whole screen (the invite reader on iPhone and iPad). Closing it pops it.
    var cover: AppRoute? {
        get {
            guard backStack.count > 1, let top = backStack.last, top.presentsFullScreen else { return nil }
            return top
        }
        set {
            guard newValue == nil, cover != nil else { return }
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
