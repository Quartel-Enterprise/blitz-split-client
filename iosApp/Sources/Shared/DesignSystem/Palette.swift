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
    /// The viewfinder's corners on a QR that isn't an invite: the camera stays dark, so it is the dark error in both.
    static let cameraError = Color(light: 0xFFB599, dark: 0xFFB599)
    /// The tile of a Pix key, and of a Bitcoin key, in the profile's list.
    static let pixTile = Color(light: 0xC9E6FF, dark: 0x004B6F)
    static let onPixTile = Color(light: 0x001E2F, dark: 0xC9E6FF)
    static let bitcoinTile = Color(light: 0xFFDBCE, dark: 0x71361C)
    static let onBitcoinTile = Color(light: 0x370E00, dark: 0xFFDBCE)
    /// The tertiary container of a notice that informs rather than warns (the CPF a payer sees).
    static let infoContainer = Color(light: 0xC3E7FF, dark: 0x004C69)
    static let onInfoContainer = Color(light: 0x001E2C, dark: 0xC3E7FF)
    /// The status card of a split that doesn't add up yet ("$84.00 left").
    static let errorContainer = Color(light: 0xFFDBCE, dark: 0x71361C)
    static let onErrorContainer = Color(light: 0x370E00, dark: 0xFFDBCE)
    /// The selected segment of a custom segmented control, as the system draws its thumb.
    static let segmentThumb = Color(light: 0xFFFFFF, dark: 0x636366)

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
        case .peach: String(localized: "Peach")
        case .green: String(localized: "Green")
        case .lilac: String(localized: "Lilac")
        case .sand: String(localized: "Sand")
        default: String(localized: "Blue")
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
