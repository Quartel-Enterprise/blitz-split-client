import Shared
import SwiftUI

/// The invite a group's tab opened on the Mac (F2Mac): `host` is the tab under it, whose "Invite people" anchors the
/// popover, and `route` the invite it shows.
struct InvitePopover: Equatable {
    let host: AppRoute
    let route: RouteInvite
}

extension EnvironmentValues {
    /// The root's invite popover, nil while none is open (always on iPhone and iPad), so the tab that opened it draws
    /// it on its own button.
    @Entry var invitePopover: InvitePopover? = nil
    /// Closes the invite popover (a click outside, Esc), which pops it from the shared back stack.
    @Entry var dismissInvitePopover: () -> Void = {}
}

extension View {
    /// The Mac's invite popover on this button, while the invite on top was opened from `host`. Elsewhere nothing.
    func invitePopover(host: AppRoute) -> some View {
        modifier(InvitePopoverAnchor(host: host))
    }
}

private struct InvitePopoverAnchor: ViewModifier {
    let host: AppRoute

    @Environment(\.invitePopover) private var popover
    @Environment(\.dismissInvitePopover) private var dismiss

    func body(content: Content) -> some View {
        #if os(macOS)
        content.popover(isPresented: isShown, arrowEdge: .bottom) {
            if let popover, popover.host == host {
                InviteView(route: popover.route, isPopover: true)
            }
        }
        #else
        content
        #endif
    }

    private var isShown: Binding<Bool> {
        Binding(
            get: { popover?.host == host },
            set: { shown in
                if !shown { dismiss() }
            },
        )
    }
}
