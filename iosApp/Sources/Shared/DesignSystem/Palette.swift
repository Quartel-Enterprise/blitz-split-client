import Shared
import SwiftUI

#if os(iOS)
import UIKit
#else
import AppKit
#endif

/// The canvas tokens for iOS and macOS (Cupertino + Liquid Glass), light and dark.
enum Palette {
    static let accent = Color("AccentColor")
    static let onAccent = Color(light: 0xFFFFFF, dark: 0x00344E)
    static let negative = Color(light: 0x8E4D31, dark: 0xFFB599)

    #if os(iOS)
    static let groupedBackground = Color(uiColor: .systemGroupedBackground)
    static let card = Color(uiColor: .secondarySystemGroupedBackground)
    #else
    static let groupedBackground = Color(nsColor: .windowBackgroundColor)
    // The grouped cards of a Mac window: a quiet fill that reads on both appearances
    static let card = Color(nsColor: .quaternarySystemFill)
    #endif
}

/// The five avatar colors, with the same values on every platform (see the canvas rules).
extension AvatarColor {
    var container: Color {
        switch self {
        case .peach: Color(light: 0xFFDBCE, dark: 0x71361C)
        case .green: Color(light: 0xBDEFD3, dark: 0x00513A)
        case .lilac: Color(light: 0xE8DDFF, dark: 0x4F378B)
        case .sand: Color(light: 0xFFE08B, dark: 0x574500)
        default: Color(light: 0xC9E6FF, dark: 0x004B6F)
        }
    }

    var content: Color {
        switch self {
        case .peach: Color(light: 0x370E00, dark: 0xFFDBCE)
        case .green: Color(light: 0x00210F, dark: 0xBDEFD3)
        case .lilac: Color(light: 0x21005D, dark: 0xE8DDFF)
        case .sand: Color(light: 0x241A00, dark: 0xFFE08B)
        default: Color(light: 0x001E2F, dark: 0xC9E6FF)
        }
    }

    var label: String {
        switch self {
        case .peach: "Pêssego"
        case .green: "Verde"
        case .lilac: "Lilás"
        case .sand: "Areia"
        default: "Azul"
        }
    }
}

extension Color {
    /// A color that follows the light and dark appearance. Nonisolated: SwiftUI resolves colors off the
    /// main thread while it renders.
    nonisolated init(light: UInt32, dark: UInt32) {
        #if os(iOS)
        self.init(uiColor: UIColor { @Sendable traits in
            UIColor(rgb: traits.userInterfaceStyle == .dark ? dark : light)
        })
        #else
        self.init(nsColor: NSColor(name: nil) { @Sendable appearance in
            let isDark = appearance.bestMatch(from: [.darkAqua, .aqua]) == .darkAqua
            return NSColor(rgb: isDark ? dark : light)
        })
        #endif
    }
}

#if os(iOS)
private extension UIColor {
    nonisolated convenience init(rgb: UInt32) {
        self.init(
            red: CGFloat((rgb >> 16) & 0xFF) / 255,
            green: CGFloat((rgb >> 8) & 0xFF) / 255,
            blue: CGFloat(rgb & 0xFF) / 255,
            alpha: 1,
        )
    }
}
#else
private extension NSColor {
    nonisolated convenience init(rgb: UInt32) {
        self.init(
            srgbRed: CGFloat((rgb >> 16) & 0xFF) / 255,
            green: CGFloat((rgb >> 8) & 0xFF) / 255,
            blue: CGFloat(rgb & 0xFF) / 255,
            alpha: 1,
        )
    }
}
#endif
