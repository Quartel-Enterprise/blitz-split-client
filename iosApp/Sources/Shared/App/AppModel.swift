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
    /// How many times the shared code opened the profile or the category map on the Mac, which live in Ajustes (⌘,):
    /// each one opens the Settings window (`RootView`) instead of pushing it over the groups.
    private(set) var settingsRequestCount = 0
    #if os(macOS)
    /// The section of Ajustes the last of those asked for (the profile, or Categories for the map), until Ajustes picks
    /// it (`SettingsView`).
    var requestedSettingsPane: ProfileSettingsPane?
    #endif

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
            self?.backStackChanged(routes)
        })
        watches.add(viewModel.themeWatcher.observe { [weak self] theme in
            self?.theme = theme
        })
    }

    /// The Mac never pushes the profile nor the category map: any screen that opens them (the groups' avatar, "Protect
    /// your access", a browser sign in coming back; the profile's "Category map", Split a bill's "Category Map…") opens
    /// Ajustes on their section and leaves the back stack as it was, so nothing is drawn over the groups or the bill. What Ajustes opens (a key, deleting the account) only ever sits on top: a screen the main window opens
    /// over it while the main window can't take clicks (a link opened from outside, a menu command) closes it, since
    /// one back stack has one top. Everywhere else the back stack is drawn as the shared code moves it.
    private func backStackChanged(_ value: Any?) {
        let routes = Self.routes(of: value)
        #if os(macOS)
        if routes.count > 1, let last = routes.last, last == .profile || last == .categoryMap {
            holder.viewModel.navigator.popTo(size: Int32(routes.count - 1))
            requestedSettingsPane = last == .categoryMap ? .categories : .profile
            settingsRequestCount += 1
            return
        }
        if let buried = routes.indices.dropFirst().first(where: { index in
            routes[index].opensInSettings && routes[index...].contains { !$0.opensInSettings }
        }) {
            closeSettingsEntries(from: buried, of: value)
            return
        }
        #endif
        backStack = routes
    }

    #if os(macOS)
    /// Keeps what the main window opened over Ajustes' entries from `index` up, in its order, without them.
    private func closeSettingsEntries(from index: Int, of value: Any?) {
        let above = ((value as? [Any])?.compactMap { $0 as? any Route } ?? [])
            .dropFirst(index)
            .filter { !AppRoute($0).opensInSettings }
        guard let first = above.first else { return }
        let navigator = holder.viewModel.navigator
        navigator.replaceAbove(size: Int32(index), route: first)
        above.dropFirst().forEach { navigator.navigate(route: $0) }
    }
    #endif

    /// A profile exists once Início is the root (or the groups, which still draw the tabs), which is when the Mac's
    /// Ajustes have something to show.
    var hasProfile: Bool {
        isReady && (root == .home || root == .groups)
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

    /// What the main window draws: the back stack without what the Mac's Ajustes opened on top of it (a key, deleting
    /// the account), so a sheet open in either window never changes the other's. The whole back stack off the Mac.
    private var windowStack: [AppRoute] {
        Array(backStack.dropLast(settingsEntryCount))
    }

    /// How many entries on top the Mac's Ajustes opened (`AppRoute.opensInSettings`), never the root.
    private var settingsEntryCount: Int {
        min(backStack.reversed().prefix { $0.opensInSettings }.count, backStack.count - 1)
    }

    /// The entries above the root, but the ones the sheets show. A native back gesture shortens it, and
    /// the shared back stack catches up with how many entries remain.
    var path: [AppRoute] {
        get { Array(windowStack.dropFirst().dropLast(modalCount)) }
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
            if isSheetStacked { return windowStack[sheetBaseIndex - 1] }
            if sheetPushCount > 0 { return windowStack[sheetBaseIndex] }
            guard windowStack.count > 1, let top = windowStack.last, presentsAsSheet(top), invitePopover == nil else {
                return nil
            }
            return top
        }
        set {
            guard newValue == nil, sheet != nil else { return }
            let shown = (isSheetStacked ? 2 : 1) + sheetPushCount
            holder.viewModel.navigator.popTo(size: Int32(windowStack.count - shown))
        }
    }

    /// The entries pushed inside the sheet (the default split inside Edit group's): its own NavigationStack's path.
    /// Its back button shortens it, and the shared back stack catches up.
    var sheetPath: [AppRoute] {
        get { isSheetStacked ? [] : pushedInSheet }
        set { popPushedInSheet(keeping: newValue.count) }
    }

    /// The entries pushed inside a sheet stacked over another (on the Mac, Quem é você inside an invite link's sheet
    /// that opened over Edit group's): the stacked sheet's own path, so the sheet under it stays where it is.
    var stackedSheetPath: [AppRoute] {
        get { isSheetStacked ? pushedInSheet : [] }
        set { popPushedInSheet(keeping: newValue.count) }
    }

    private var pushedInSheet: [AppRoute] {
        Array(windowStack.suffix(sheetPushCount))
    }

    private func popPushedInSheet(keeping count: Int) {
        guard count < sheetPushCount else { return }
        holder.viewModel.navigator.popTo(size: Int32(windowStack.count - sheetPushCount + count))
    }

    /// The sheet the top entries are pushed inside, or the top one when none is.
    private var sheetBaseIndex: Int {
        windowStack.count - 1 - sheetPushCount
    }

    /// How many entries on top are pushed inside the sheet right under them.
    private var sheetPushCount: Int {
        let stack = windowStack
        let pushed = stack.reversed().prefix { $0.pushesInSheet }.count
        guard pushed > 0, stack.count > pushed + 1, presentsAsSheet(stack[stack.count - 1 - pushed]) else { return 0 }
        return pushed
    }

    /// The entry that opens as a sheet over another sheet (editing a bill open as a sheet with an iPad's room and on
    /// the Mac, an invite link's sheet over Edit group's on the Mac), with what is pushed inside it on top.
    var stackedSheet: AppRoute? {
        get { isSheetStacked ? windowStack[sheetBaseIndex] : nil }
        set {
            guard newValue == nil, stackedSheet != nil else { return }
            holder.viewModel.navigator.popTo(size: Int32(windowStack.count - 1 - sheetPushCount))
        }
    }

    /// The two entries under what is pushed inside a sheet both open as sheets, so the second stacks over the first
    /// instead of taking its place.
    private var isSheetStacked: Bool {
        let base = sheetBaseIndex
        return base >= 2 && presentsAsSheet(windowStack[base]) && presentsAsSheet(windowStack[base - 1])
    }

    /// Before the window is measured no entry opens as a sheet, and the root draws none of the stack either. What the
    /// Mac's Ajustes open is their sheet, never the main window's.
    private func presentsAsSheet(_ route: AppRoute) -> Bool {
        guard let isLargeWindow, !route.opensInSettings else { return false }
        return route.presentsAsSheet(isLargeWindow: isLargeWindow)
    }

    private var modalCount: Int {
        if isSheetStacked { return 2 + sheetPushCount }
        if sheetPushCount > 0 { return 1 + sheetPushCount }
        return sheet == nil && cover == nil && invitePopover == nil ? 0 : 1
    }

    /// The top entry when the Mac's Ajustes opened it (a key, deleting the account): a sheet on the Settings window,
    /// which the main window never draws, and which keeps the main window from taking clicks while it is open, so
    /// neither window moves the back stack under the other's top. Closing it pops it, as closing the main window's
    /// sheets does. Nil off the Mac.
    var settingsSheet: AppRoute? {
        get { settingsEntryCount > 0 ? backStack.last : nil }
        set {
            guard newValue == nil, settingsSheet != nil else { return }
            holder.viewModel.navigator.popTo(size: Int32(windowStack.count))
        }
    }

    /// The Mac's invite over the group's tab that opened it (F2Mac): a popover on that tab's "Invite people" rather
    /// than a sheet. The new group's invite has no group behind it, so it stays a sheet. Closing it pops it.
    var invitePopover: InvitePopover? {
        get {
            #if os(macOS)
            let stack = windowStack
            guard stack.count > 2, case let .invite(route) = stack[stack.count - 1] else { return nil }
            let host = stack[stack.count - 2]
            guard host.isGroupTab else { return nil }
            return InvitePopover(host: host, route: route)
            #else
            return nil
            #endif
        }
        set {
            guard newValue == nil, invitePopover != nil else { return }
            holder.viewModel.navigator.popTo(size: Int32(windowStack.count - 1))
        }
    }

    /// The top entry when it covers the whole screen (the invite reader on iPhone and iPad). Closing it pops it.
    var cover: AppRoute? {
        get {
            guard windowStack.count > 1, let top = windowStack.last, top.presentsFullScreen else { return nil }
            return top
        }
        set {
            guard newValue == nil, cover != nil else { return }
            holder.viewModel.navigator.popTo(size: Int32(windowStack.count - 1))
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
