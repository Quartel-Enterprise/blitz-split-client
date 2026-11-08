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
    /// What comes back to the device's user: a positive net, "owes you", "to get back".
    static let positive = Color(light: 0x006C4C, dark: 0x6EDBAA)
    static let negative = Color(light: 0x8E4D31, dark: 0xFFB599)
    /// The label of a button filled with the negative color ("Delete account"): white reads at 1.7:1 on the dark one.
    static let onNegative = Color(light: 0xFFFFFF, dark: 0x552108)
    /// What is saved here and not on the server yet (the queue's dot, "Waiting to send"): an amber that reads as text
    /// (5:1 or more), apart from the negative's orange, which the system's orange matched in dark mode.
    static let pending = Color(light: 0x7C5800, dark: 0xF9BC3E)
    /// The same amber on what stays dark in both appearances (a badge over a photo), as `cameraError` is.
    static let pendingOnDark = Color(light: 0xF9BC3E, dark: 0xF9BC3E)
    static let pendingContainer = Color(light: 0xFFDEA8, dark: 0x5E4200)
    static let onPendingContainer = Color(light: 0x271900, dark: 0xFFDEA8)
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

    /// A balance or a share in minor units: the positive green for what comes back, the negative color for what is
    /// owed, secondary at zero.
    static func balance(_ minor: Int64) -> Color {
        if minor > 0 { return positive }
        if minor < 0 { return negative }
        return .secondary
    }

    #if os(iOS)
    static let groupedBackground = Color(uiColor: .systemGroupedBackground)
    static let card = Color(uiColor: .secondarySystemGroupedBackground)
    #else
    static let groupedBackground = Color(nsColor: .windowBackgroundColor)
    // The grouped cards of a Mac window: a quiet fill that reads on both appearances
    static let card = Color(nsColor: .quaternarySystemFill)
    #endif
}

/// The label of a filled (prominent) button in the color that reads on its tint. The system draws it white, which
/// reads on the light accent (6.4:1) but not on the dark one (#95CDF7, 1.7:1). Resolved where the label is drawn, so
/// a disabled button keeps the system's muted label whatever the order of `.disabled` and this modifier. On the Mac,
/// a window that isn't key draws the button's bezel gray, so its label goes back to the system's.
struct OnTintLabelStyle: ShapeStyle {
    let color: Color

    func resolve(in environment: EnvironmentValues) -> Color.Resolved {
        if !environment.isEnabled { return Color.secondary.resolve(in: environment) }
        #if os(macOS)
        if !environment.appearsActive { return Color.primary.resolve(in: environment) }
        #endif
        return color.resolve(in: environment)
    }
}

extension View {
    /// Goes on every `.borderedProminent` and `.glassProminent` button: `Palette.onNegative` for one tinted with
    /// `Palette.negative`, `Palette.onAccent` otherwise.
    func prominentLabel(_ color: Color = Palette.onAccent) -> some View {
        foregroundStyle(OnTintLabelStyle(color: color))
    }
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

    /// A mark that stands for a person (a bar, a segment of a split) takes this tone, as a category's does.
    var data: Color {
        switch self {
        case .peach: Color(light: 0xB95D35, dark: 0xFFA17B)
        case .green: Color(light: 0x008863, dark: 0x5ACDA1)
        case .lilac: Color(light: 0x8069C0, dark: 0xC3ACFF)
        case .sand: Color(light: 0x907400, dark: 0xD9B53C)
        default: Color(light: 0x017EB7, dark: 0x69C1FE)
        }
    }

    var label: String {
        switch self {
        case .peach: String(localized: "Peach", bundle: .appLanguage)
        case .green: String(localized: "Green", bundle: .appLanguage)
        case .lilac: String(localized: "Lilac", bundle: .appLanguage)
        case .sand: String(localized: "Sand", bundle: .appLanguage)
        default: String(localized: "Blue", bundle: .appLanguage)
        }
    }
}

/// The twelve category colors, with the same values on every platform (see the canvas rules).
extension CategoryColor {
    var container: Color {
        switch self {
        case .orange: Color(light: 0xFFDBCE, dark: 0x71361C)
        case .green: Color(light: 0xBDEFD3, dark: 0x00513A)
        case .purple: Color(light: 0xE8DDFF, dark: 0x4F378B)
        case .yellow: Color(light: 0xFFE08B, dark: 0x574500)
        case .red: Color(light: 0xFFDAD6, dark: 0x93000A)
        case .teal: Color(light: 0xA5F0E6, dark: 0x00504A)
        case .pink: Color(light: 0xFFD8EC, dark: 0x7B2951)
        case .indigo: Color(light: 0xDDE1FF, dark: 0x2F3A8F)
        case .lime: Color(light: 0xDDF0A0, dark: 0x3B4D00)
        case .brown: Color(light: 0xF1DFD1, dark: 0x5B4130)
        case .gray: Color(light: 0xE0E3E8, dark: 0x44474C)
        default: Color(light: 0xC9E6FF, dark: 0x004B6F)
        }
    }

    var content: Color {
        switch self {
        case .orange: Color(light: 0x370E00, dark: 0xFFDBCE)
        case .green: Color(light: 0x00210F, dark: 0xBDEFD3)
        case .purple: Color(light: 0x21005D, dark: 0xE8DDFF)
        case .yellow: Color(light: 0x241A00, dark: 0xFFE08B)
        case .red: Color(light: 0x410002, dark: 0xFFDAD6)
        case .teal: Color(light: 0x00201C, dark: 0xA5F0E6)
        case .pink: Color(light: 0x3E001D, dark: 0xFFD8EC)
        case .indigo: Color(light: 0x00164E, dark: 0xDDE1FF)
        case .lime: Color(light: 0x141E00, dark: 0xDDF0A0)
        case .brown: Color(light: 0x2A1709, dark: 0xF1DFD1)
        case .gray: Color(light: 0x191C20, dark: 0xE0E3E8)
        default: Color(light: 0x001E2F, dark: 0xC9E6FF)
        }
    }

    /// A mark that carries the category (a slice, a legend dot, a bar) takes this tone, not the container's: the
    /// container is a background and reads at under 2:1 on a card, while this one reads at 4:1 light and 7:1 dark
    /// (WCAG's 3:1 for a meaningful mark), and it never equals one of the theme's own container colors.
    var data: Color {
        switch self {
        case .orange: Color(light: 0xB95D35, dark: 0xFFA17B)
        case .green: Color(light: 0x008863, dark: 0x5ACDA1)
        case .purple: Color(light: 0x8069C0, dark: 0xC3ACFF)
        case .yellow: Color(light: 0x907400, dark: 0xD9B53C)
        case .red: Color(light: 0xD93D35, dark: 0xFF9F94)
        case .teal: Color(light: 0x00867C, dark: 0x3ECDC0)
        case .pink: Color(light: 0xB55882, dark: 0xFF9AC4)
        case .indigo: Color(light: 0x6570C7, dark: 0xABB3FF)
        case .lime: Color(light: 0x678020, dark: 0xA8C35C)
        case .brown: Color(light: 0x986E53, dark: 0xE0AF90)
        case .gray: Color(light: 0x74777C, dark: 0xB7B9BE)
        default: Color(light: 0x017EB7, dark: 0x69C1FE)
        }
    }

    var label: String {
        switch self {
        case .orange: String(localized: "Orange", bundle: .appLanguage)
        case .green: String(localized: "Green", bundle: .appLanguage)
        case .purple: String(localized: "Purple", bundle: .appLanguage)
        case .yellow: String(localized: "Yellow", bundle: .appLanguage)
        case .red: String(localized: "Red", bundle: .appLanguage)
        case .teal: String(localized: "Teal", bundle: .appLanguage)
        case .pink: String(localized: "Pink", bundle: .appLanguage)
        case .indigo: String(localized: "Indigo", bundle: .appLanguage)
        case .lime: String(localized: "Lime", bundle: .appLanguage)
        case .brown: String(localized: "Brown", bundle: .appLanguage)
        case .gray: String(localized: "Gray", bundle: .appLanguage)
        default: String(localized: "Blue", bundle: .appLanguage)
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
