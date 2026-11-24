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
/// Persian, Tamil, Telugu, Marathi, Gujarati and Malayalam, so an amount reads as the amount fields type and parse it, and the calendar dates are written in.
enum AppNumbers {
    private static let gregorianCodes: Set = ["fa", "th"]

    /// The device's calendar, but the Gregorian one in Persian and Thai, whose devices default to the Solar Hijri
    /// calendar (1405 for 2026) and the Buddhist era (2569): a group is shared across platforms, so an iPhone shows the
    /// year Android and the web show (Pierre's call). Persian or Thai is the app's language (Profile › Language, which
    /// may not have relaunched the app yet) or the device's.
    static var calendar: Calendar { calendar(locale: locale) }

    /// `calendar` with its locale already built, as a date style that also needs the locale builds it once.
    static func calendar(locale: Locale) -> Calendar {
        let codes = [Bundle.appLanguageCode, Locale.autoupdatingCurrent.language.languageCode?.identifier]
        guard codes.contains(where: { $0.map(gregorianCodes.contains) ?? false }) else { return .autoupdatingCurrent }
        var gregorian = Calendar(identifier: .gregorian)
        gregorian.locale = locale
        gregorian.timeZone = .autoupdatingCurrent
        gregorian.firstWeekday = Calendar.autoupdatingCurrent.firstWeekday
        return gregorian
    }

    /// The device's locale, on Latin digits when it would write Arabic, Bengali, Urdu, Persian, Tamil, Telugu, Marathi, Gujarati or Malayalam ones.
    static var locale: Locale { latin(.autoupdatingCurrent) }

    private static let latinDigitCodes: Set = ["ar", "bn", "ur", "fa", "ta", "te", "mr", "gu", "ml"]

    /// `base` on Latin digits in Arabic, Bengali, Urdu, Persian, Tamil, Telugu, Marathi, Gujarati and Malayalam; `base` itself in every other language.
    static func latin(_ base: Locale) -> Locale {
        guard let code = base.language.languageCode?.identifier, latinDigitCodes.contains(code),
              base.numberingSystem.identifier != "latn" else { return base }
        var components = Locale.Components(locale: base)
        components.numberingSystem = Locale.NumberingSystem("latn")
        return Locale(components: components)
    }
}

extension Date.FormatStyle {
    /// In the app's locale (`AppNumbers.locale`) and calendar (`AppNumbers.calendar`): 0-9, and the Gregorian year in
    /// Persian and Thai.
    func appDateFormat() -> Date.FormatStyle {
        let appLocale = AppNumbers.locale
        var style = locale(appLocale)
        style.calendar = AppNumbers.calendar(locale: appLocale)
        return style
    }
}

extension Date.IntervalFormatStyle {
    /// In the app's locale and calendar, as `Date.FormatStyle.appDateFormat()`.
    func appDateFormat() -> Date.IntervalFormatStyle {
        let appLocale = AppNumbers.locale
        var style = locale(appLocale)
        style.calendar = AppNumbers.calendar(locale: appLocale)
        return style
    }
}

extension View {
    /// A date picker in the app's calendar: the Gregorian one in Persian and Thai, as every date the app writes.
    func appCalendar() -> some View {
        environment(\.calendar, AppNumbers.calendar)
    }
}
