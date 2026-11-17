import Foundation
import SwiftUI

/// The right-to-left languages the app may draw in: the sentences mirror, and an amount inside one keeps its own order.
enum RightToLeft {
    /// The language codes written right to left (Arabic, Hebrew).
    static let languageCodes: Set<String> = ["ar", "he"]

    /// Whether the app's language (Profile › Language, or the device's) is written right to left.
    static var isAppLanguage: Bool {
        Bundle.appLanguageCode.map(languageCodes.contains) ?? false
    }

    /// "→", the arrow that points the way a line reads: "←" in a right-to-left language, where arrows don't mirror.
    static var forwardArrow: String {
        isAppLanguage ? "←" : "→"
    }

    /// `text` between a first strong isolate and its pop, so an amount ("R$ 12,50", "−€ 3") keeps its order inside
    /// a right-to-left sentence; `text` as it is in every other language.
    static func isolated(_ text: String) -> String {
        isAppLanguage ? "\u{2068}\(text)\u{2069}" : text
    }
}

/// The locale amounts, numbers and dates are written in: the device's, with Latin digits (0-9) in Arabic, so an amount
/// reads as the amount fields type and parse it.
enum AppNumbers {
    /// The device's locale, on Latin digits when it would write Arabic ones.
    static var locale: Locale { latin(.autoupdatingCurrent) }

    /// `base` on Latin digits in Arabic; `base` itself in every other language.
    static func latin(_ base: Locale) -> Locale {
        guard base.language.languageCode == .arabic, base.numberingSystem.identifier != "latn" else { return base }
        var components = Locale.Components(locale: base)
        components.numberingSystem = Locale.NumberingSystem("latn")
        return Locale(components: components)
    }
}
