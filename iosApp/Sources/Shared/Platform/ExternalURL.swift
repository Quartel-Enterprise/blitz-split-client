#if os(iOS)
import UIKit
#else
import AppKit
#endif

/// Opens a link outside the app, in the browser or the app that handles it, on each platform.
enum ExternalURL {
    static func open(_ text: String) {
        guard let url = URL(string: text) else { return }
        #if os(iOS)
        UIApplication.shared.open(url)
        #else
        NSWorkspace.shared.open(url)
        #endif
    }
}
