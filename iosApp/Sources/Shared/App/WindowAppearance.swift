import Shared
import SwiftUI
#if os(iOS)
import UIKit
#else
import AppKit
#endif

/// The profile's theme, applied to the windows themselves: once Light or Dark was forced,
/// `preferredColorScheme(nil)` doesn't reliably bring the windows and their sheets back to the system's.
enum WindowAppearance {
    @MainActor
    static func apply(_ theme: ThemePreference) {
        #if os(iOS)
        let style: UIUserInterfaceStyle = switch theme {
        case .light: .light
        case .dark: .dark
        default: .unspecified
        }
        for scene in UIApplication.shared.connectedScenes {
            (scene as? UIWindowScene)?.windows.forEach { $0.overrideUserInterfaceStyle = style }
        }
        #else
        NSApp.appearance = switch theme {
        case .light: NSAppearance(named: .aqua)
        case .dark: NSAppearance(named: .darkAqua)
        default: nil
        }
        #endif
    }
}
