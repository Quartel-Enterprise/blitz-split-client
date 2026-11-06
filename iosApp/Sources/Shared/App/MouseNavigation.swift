#if os(macOS)
import AppKit
import SwiftUI

extension View {
    /// The mouse's back and forward buttons (buttons 4 and 5, `buttonNumber` 3 and 4) go back and forward in this
    /// window's navigation, as in the Finder and Safari (Pierre's beta feedback). Only clicks in this view's own window
    /// count, so Ajustes' window and a sheet's never move the main window's back stack.
    func mouseNavigationButtons(back: @escaping () -> Void, forward: @escaping () -> Void) -> some View {
        background(MouseButtonsMonitor(back: back, forward: forward))
    }
}

private struct MouseButtonsMonitor: NSViewRepresentable {
    let back: () -> Void
    let forward: () -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeNSView(context: Context) -> NSView {
        let view = NSView()
        context.coordinator.install(in: view)
        return view
    }

    func updateNSView(_: NSView, context: Context) {
        context.coordinator.back = back
        context.coordinator.forward = forward
    }

    static func dismantleNSView(_: NSView, coordinator: Coordinator) {
        coordinator.remove()
    }

    @MainActor
    final class Coordinator {
        var back: () -> Void = {}
        var forward: () -> Void = {}
        private var monitor: Any?

        func install(in view: NSView) {
            monitor = NSEvent.addLocalMonitorForEvents(matching: .otherMouseDown) { [weak self, weak view] event in
                guard let self, let window = view?.window, event.window === window else { return event }
                switch event.buttonNumber {
                case 3:
                    back()
                    return nil
                case 4:
                    forward()
                    return nil
                default:
                    return event
                }
            }
        }

        func remove() {
            if let monitor { NSEvent.removeMonitor(monitor) }
            monitor = nil
        }
    }
}

extension FocusedValues {
    /// The main window's back and forward, which the Go menu's ⌘[ and ⌘] reach.
    @Entry var windowNavigation: AppModel?
}

/// Go › Back (⌘[) and Forward (⌘]), as in the Finder: the mouse's back and forward buttons from the keyboard, and
/// for a mouse whose driver sends those keys instead of its buttons.
struct NavigationCommands: Commands {
    @FocusedValue(\.windowNavigation) private var app

    var body: some Commands {
        CommandMenu("Go") {
            Button("Back") { app?.goBack() }
                .keyboardShortcut("[", modifiers: .command)
                .disabled(app?.canGoBack != true)
            Button("Forward") { app?.goForward() }
                .keyboardShortcut("]", modifiers: .command)
                .disabled(app?.canGoForward != true)
        }
    }
}
#endif
