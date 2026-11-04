#if os(iOS)
import UIKit
#else
import AppKit
#endif

/// The system share a `ShareLink` opens, for a text the shared code hands over as an action (a comment's "Share"),
/// which no button holds beforehand: the share sheet over the front window on an iPhone and an iPad, the sharing
/// picker where the pointer clicked on the Mac.
@MainActor
enum SystemShare {
    static func share(_ text: String) {
        #if os(iOS)
        guard let window = PresentationAnchor.current, var top = window.rootViewController else { return }
        // Over the sheet the bill may be in
        while let presented = top.presentedViewController, !presented.isBeingDismissed {
            top = presented
        }
        let controller = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        // An iPad shows it as a popover, which needs somewhere to point: the middle of the window, with no arrow
        if let popover = controller.popoverPresentationController {
            popover.sourceView = top.view
            popover.sourceRect = CGRect(x: top.view.bounds.midX, y: top.view.bounds.midY, width: 0, height: 0)
            popover.permittedArrowDirections = []
        }
        top.present(controller, animated: true)
        #else
        guard let window = NSApplication.shared.keyWindow, let view = window.contentView else { return }
        // The menu item was just clicked where the comment is, so the picker opens beside it
        let point = view.convert(window.mouseLocationOutsideOfEventStream, from: nil)
        let picker = NSSharingServicePicker(items: [text])
        picker.show(relativeTo: NSRect(origin: point, size: .zero), of: view, preferredEdge: .minY)
        #endif
    }
}
