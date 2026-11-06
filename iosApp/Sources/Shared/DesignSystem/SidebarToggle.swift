#if os(macOS)
import AppKit
import SwiftUI

/// The sidebar's button at the leading edge of the detail's toolbar, right of the sidebar's divider, in the main
/// window and in Ajustes alike (Pierre's beta feedback), rather than the system's place in the sidebar's own header.
/// Given the split view's visibility it flips it; without it (the main window's `TabView`, which has no binding for its
/// sidebar) it asks the window's split view to toggle, as View › Hide Sidebar does.
struct SidebarToggleButton: View {
    var visibility: Binding<NavigationSplitViewVisibility>?

    var body: some View {
        Button("Toggle Sidebar", systemImage: "sidebar.leading") {
            if let visibility {
                withAnimation {
                    visibility.wrappedValue = visibility.wrappedValue == .detailOnly ? .all : .detailOnly
                }
            } else {
                NSApp.sendAction(#selector(NSSplitViewController.toggleSidebar(_:)), to: nil, from: nil)
            }
        }
        .help(Text("Toggle Sidebar"))
    }
}

extension View {
    /// Hides the system's sidebar button of this view's window, which the Mac's split views keep in the sidebar's header
    /// whatever `.toolbar(removing: .sidebarToggle)` says, so `SidebarToggleButton` is the window's only one. View ›
    /// Hide Sidebar and ⌃⌘S still toggle it. `unifiesToolbar` gives a Settings window the main window's toolbar,
    /// whose items start at the detail's leading edge, rather than a preferences window's centered ones.
    func hidingSystemSidebarToggle(unifiesToolbar: Bool = false) -> some View {
        background(SystemSidebarToggleHider(unifiesToolbar: unifiesToolbar))
    }
}

private struct SystemSidebarToggleHider: NSViewRepresentable {
    let unifiesToolbar: Bool

    func makeNSView(context _: Context) -> HiderView {
        HiderView()
    }

    func updateNSView(_ view: HiderView, context _: Context) {
        view.unifiesToolbar = unifiesToolbar
        view.hideToggle()
    }

    final class HiderView: NSView {
        var unifiesToolbar = false
        private var observer: NSObjectProtocol?

        override func viewDidMoveToWindow() {
            super.viewDidMoveToWindow()
            if let observer { NotificationCenter.default.removeObserver(observer) }
            observer = nil
            guard let window else { return }
            // SwiftUI gives the window its toolbar after this view joins it, adds the items again as the screens
            // change and shows its button again as the sidebar toggles: checked after each event the window handles,
            // a handful of items
            observer = NotificationCenter.default.addObserver(
                forName: NSWindow.didUpdateNotification,
                object: window,
                queue: .main,
            ) { [weak self] _ in
                MainActor.assumeIsolated { self?.hideToggle() }
            }
        }

        func hideToggle() {
            if unifiesToolbar, let window, window.toolbarStyle != .unified { window.toolbarStyle = .unified }
            for item in window?.toolbar?.items ?? [] where !item.isHidden && Self.systemToggles.contains(item.itemIdentifier) {
                item.isHidden = true
            }
        }

        /// SwiftUI's own sidebar button, and AppKit's should a window ever use it.
        private static let systemToggles: Set<NSToolbarItem.Identifier> = [
            NSToolbarItem.Identifier("com.apple.SwiftUI.navigationSplitView.toggleSidebar"),
            .toggleSidebar,
        ]

        isolated deinit {
            if let observer { NotificationCenter.default.removeObserver(observer) }
        }
    }
}
#endif
