#if os(iOS)
import UIKit
#else
import AppKit
#endif

/// The system pasteboard of each platform, for the recovery code and the invite link.
enum Pasteboard {
    static func copy(_ text: String) {
        #if os(iOS)
        UIPasteboard.general.string = text
        #else
        NSPasteboard.general.clearContents()
        NSPasteboard.general.setString(text, forType: .string)
        #endif
    }
}
