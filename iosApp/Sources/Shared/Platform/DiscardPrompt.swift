#if os(macOS)
import AppKit
#else
import UIKit
#endif

/// "Discard changes?", asked before any way out of a form with unsaved input closes it: a confirmation sheet with a
/// destructive "Discard" (an action sheet on iPhone, an alert on iPad, a sheet on the Mac's window). It presents from
/// the top of what is on screen, so it shows over a sheet or a cover the form is in, which SwiftUI's own
/// `confirmationDialog` of the root couldn't.
@MainActor
enum DiscardPrompt {
    private static var isPresented = false

    /// Asks once at a time; `onDiscard` or `onKeep` runs when it is answered.
    static func present(onDiscard: @escaping () -> Void, onKeep: @escaping () -> Void) {
        guard !isPresented else { return }
        let title = String(localized: "Discard changes?", bundle: .appLanguage)
        let message = String(localized: "Unsaved changes will be lost.", bundle: .appLanguage)
        let discard = String(localized: "Discard", bundle: .appLanguage)
        let keep = String(localized: "Keep editing", bundle: .appLanguage)
        isPresented = true
        let answer: (Bool) -> Void = { discards in
            isPresented = false
            if discards { onDiscard() } else { onKeep() }
        }
        #if os(macOS)
        let alert = NSAlert()
        alert.messageText = title
        alert.informativeText = message
        alert.addButton(withTitle: keep)
        alert.addButton(withTitle: discard)
        alert.buttons[1].hasDestructiveAction = true
        if let window = NSApp.keyWindow ?? NSApp.mainWindow {
            alert.beginSheetModal(for: window) { answer($0 == .alertSecondButtonReturn) }
        } else {
            answer(alert.runModal() == .alertSecondButtonReturn)
        }
        #else
        let style: UIAlertController.Style = UIDevice.current.userInterfaceIdiom == .phone ? .actionSheet : .alert
        let controller = UIAlertController(title: title, message: message, preferredStyle: style)
        controller.addAction(UIAlertAction(title: discard, style: .destructive) { _ in answer(true) })
        controller.addAction(UIAlertAction(title: keep, style: .cancel) { _ in answer(false) })
        guard let presenter = topViewController() else {
            isPresented = false
            onKeep()
            return
        }
        presenter.present(controller, animated: true)
        #endif
    }

    #if !os(macOS)
    private static func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let window = scenes.first { $0.activationState == .foregroundActive }?.windows.first { $0.isKeyWindow }
            ?? scenes.flatMap(\.windows).first { $0.isKeyWindow }
        var top = window?.rootViewController
        while let presented = top?.presentedViewController, !presented.isBeingDismissed { top = presented }
        return top
    }
    #endif
}
