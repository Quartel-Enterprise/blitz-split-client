import Foundation
import Shared
import SwiftUI
import Synchronization

/// Profile › Language on the Apple apps. The shared code keeps the pick in the app's own AppleLanguages, which the
/// system only reads at the next launch, so the app also draws in it at once: SwiftUI's literals through the windows'
/// `locale`, and every `String(localized:)` through `Bundle.appLanguage`.
enum AppLocale {
    /// The `.lproj` of the language picked; nil before the first `apply`. Written on the main actor and read by any
    /// `String(localized:)`, on whichever thread it runs.
    fileprivate nonisolated static let strings = Mutex<Bundle?>(nil)

    /// Nil for the language the app launched in, whose lookup the system does itself. The bare identifier otherwise,
    /// never with the device's region: SwiftUI reads the `.lproj` of the locale's exact identifier, so "es_US" would
    /// draw every literal in English. Dates and amounts take the device's region at the next launch.
    static func of(_ language: AppLanguage) -> Locale? {
        let identifier = identifier(of: language)
        if identifier == Bundle.main.preferredLocalizations.first { return nil }
        return Locale(identifier: identifier)
    }

    /// Called before the windows draw in `language`, at launch and on each pick.
    static func apply(_ language: AppLanguage) {
        // Spain's es-ES.lproj is whole, the build filling it with es (merge-regional-strings.py)
        let bundle = Bundle.main
            .url(forResource: identifier(of: language), withExtension: "lproj")
            .flatMap(Bundle.init(url:))
        strings.withLock { $0 = bundle }
    }

    /// Spain's Spanish (es-ES) on a device in Spain, as the shared code writes it to AppleLanguages.
    private static func identifier(of language: AppLanguage) -> String {
        language.toTag(region: Locale.current.region?.identifier) ?? systemLanguage
    }

    /// The app's language for the device's own languages, English when it has none of them, read from the device's
    /// settings, never `Locale.preferredLanguages`, which starts with the language picked before System while the app's
    /// AppleLanguages still holds it, until the next launch.
    private static var systemLanguage: String {
        let key = "AppleLanguages"
        let languages = UserDefaults.standard.persistentDomain(forName: UserDefaults.globalDomain)?[key] as? [String]
            ?? CFPreferencesCopyValue(
                key as CFString,
                kCFPreferencesAnyApplication,
                kCFPreferencesCurrentUser,
                kCFPreferencesAnyHost,
            ) as? [String]
            ?? Locale.preferredLanguages
        let localizations = Bundle.main.localizations.filter { $0 != "Base" }
        return Bundle.preferredLocalizations(from: localizations, forPreferences: languages).first ?? "en"
    }
}

extension View {
    /// The windows' `locale` for Profile › Language, left as the system's for the language the app launched in.
    @ViewBuilder
    func appLocale(_ language: AppLanguage) -> some View {
        if let locale = AppLocale.of(language) {
            environment(\.locale, locale)
        } else {
            self
        }
    }
}

extension Bundle {
    /// Where every `String(localized:)` looks its string up (`bundle: .appLanguage`), so it follows Profile ›
    /// Language at once rather than at the next launch; `check-localized-bundle.py` fails the build on a call
    /// without it.
    nonisolated static var appLanguage: Bundle {
        AppLocale.strings.withLock { $0 } ?? .main
    }
}
