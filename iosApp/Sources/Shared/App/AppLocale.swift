import Foundation
import Shared

/// Profile › Language on the Apple apps. The shared code keeps the pick in the app's own AppleLanguages, which the
/// system only reads at the next launch, so the app also draws in it at once: SwiftUI's literals through the windows'
/// `locale`, and every `String(localized:)` through `Bundle.appLanguage`.
enum AppLocale {
    /// The `.lproj` of the language picked; nil before the first `apply`.
    nonisolated(unsafe) fileprivate static var strings: Bundle?

    static func of(_ language: AppLanguage) -> Locale {
        Locale(identifier: identifier(of: language))
    }

    /// Called before the windows draw in `language`, at launch and on each pick.
    static func apply(_ language: AppLanguage) {
        strings = Bundle.main
            .path(forResource: identifier(of: language), ofType: "lproj")
            .flatMap(Bundle.init(path:))
    }

    private static func identifier(of language: AppLanguage) -> String {
        language.tag ?? systemLanguage
    }

    /// The app's language for the device's own languages, English when it has none of them, read from the device's
    /// settings since the app's AppleLanguages may still hold the language picked before System.
    private static var systemLanguage: String {
        let global = UserDefaults.standard.persistentDomain(forName: UserDefaults.globalDomain)
        let languages = global?["AppleLanguages"] as? [String] ?? Locale.preferredLanguages
        let localizations = Bundle.main.localizations.filter { $0 != "Base" }
        return Bundle.preferredLocalizations(from: localizations, forPreferences: languages).first ?? "en"
    }
}

extension Bundle {
    /// Where every `String(localized:)` looks its string up (`bundle: .appLanguage`), so it follows Profile ›
    /// Language at once rather than at the next launch; `check-localized-bundle.py` fails the build on a call
    /// without it.
    nonisolated static var appLanguage: Bundle {
        AppLocale.strings ?? .main
    }
}
