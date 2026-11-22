import SwiftUI

#if os(iOS)
import UIKit
#elseif os(macOS)
import AppKit
#endif

/// The tokens the app (`Palette`) and the balance widget, which can't link the app's code, both draw with, so a token
/// changes in one place (docs/product/general.md: the money's green and orange).
struct ColorToken: Sendable {
    let light: UInt32
    let dark: UInt32

    /// What comes back to the device's user: a positive net, "owes you", "to get back".
    static let positive = ColorToken(light: 0x006C4C, dark: 0x6EDBAA)
    /// What the device's user owes: the brand's orange.
    static let negative = ColorToken(light: 0x8E4D31, dark: 0xFFB599)
    /// The accent, the AccentColor asset's two values, for a target without the app's asset catalog (the widget).
    static let accent = ColorToken(light: 0x266489, dark: 0x95CDF7)
}

extension Color {
    nonisolated init(_ token: ColorToken) {
        self.init(light: token.light, dark: token.dark)
    }

    #if os(iOS)
    /// A grouped card: white in light, the system's dark card in dark (`Palette.card`, the widget's background).
    static let card = Color(uiColor: .secondarySystemGroupedBackground)
    #endif
}

extension Color {
    /// A color that follows the light and dark appearance. Nonisolated: SwiftUI resolves colors off the
    /// main thread while it renders.
    nonisolated init(light: UInt32, dark: UInt32) {
        #if os(iOS)
        self.init(uiColor: UIColor { @Sendable traits in
            UIColor(rgb: traits.userInterfaceStyle == .dark ? dark : light)
        })
        #elseif os(macOS)
        self.init(nsColor: NSColor(name: nil) { @Sendable appearance in
            let isDark = appearance.bestMatch(from: [.darkAqua, .aqua]) == .darkAqua
            return NSColor(rgb: isDark ? dark : light)
        })
        #else
        // The watch is dark only (docs/product/watch.md)
        self.init(
            red: Double((dark >> 16) & 0xFF) / 255,
            green: Double((dark >> 8) & 0xFF) / 255,
            blue: Double(dark & 0xFF) / 255,
        )
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
#elseif os(macOS)
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
