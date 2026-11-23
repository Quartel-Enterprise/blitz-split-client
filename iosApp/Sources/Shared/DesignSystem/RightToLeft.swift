import Foundation
import Synchronization
import SwiftUI

/// The right-to-left languages the app may draw in: the sentences mirror, and an amount inside one keeps its own order.
enum RightToLeft {
    /// The language codes written right to left (Arabic, Hebrew, Urdu, Persian).
    nonisolated static let languageCodes: Set<String> = ["ar", "he", "ur", "fa"]

    /// Whether the app's language (Profile › Language, or the device's) is written right to left. Read once per
    /// language (`refresh`), since every amount of a list asks.
    nonisolated static var isAppLanguage: Bool {
        cached.withLock { cached in
            if let cached { return cached }
            let isRightToLeft = Bundle.appLanguageCode.map(languageCodes.contains) ?? false
            cached = isRightToLeft
            return isRightToLeft
        }
    }

    private nonisolated static let cached = Mutex<Bool?>(nil)

    /// Called by `AppLocale.apply` once the app's language changes.
    nonisolated static func refresh() {
        cached.withLock { $0 = nil }
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

/// The locale amounts, numbers and dates are written in: the device's, with Latin digits (0-9) in Arabic, Bengali, Urdu,
/// Persian and Tamil, so an amount reads as the amount fields type and parse it.
enum AppNumbers {
    /// The device's locale, on Latin digits when it would write Arabic, Bengali, Urdu, Persian or Tamil ones.
    static var locale: Locale { latin(.autoupdatingCurrent) }

    private static let latinDigitCodes: Set = ["ar", "bn", "ur", "fa", "ta"]

    /// `base` on Latin digits in Arabic, Bengali, Urdu, Persian and Tamil; `base` itself in every other language.
    static func latin(_ base: Locale) -> Locale {
        guard let code = base.language.languageCode?.identifier, latinDigitCodes.contains(code),
              base.numberingSystem.identifier != "latn" else { return base }
        var components = Locale.Components(locale: base)
        components.numberingSystem = Locale.NumberingSystem("latn")
        return Locale(components: components)
    }
}
