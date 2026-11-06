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

    /// Nil for the language the app launched in, whose lookup the system does itself, reading a variant (es-ES) over
    /// its language (es): an explicit locale, even that one, makes SwiftUI read its `.lproj` alone, English for every
    /// word a variant doesn't override. A variant picked since draws its language's words until the next launch.
    static func of(_ language: AppLanguage) -> Locale? {
        let identifier = identifier(of: language)
        if identifier == Bundle.main.preferredLocalizations.first { return nil }
        let base = String(identifier.prefix { $0 != "-" })
        let hasBase = base != identifier && Bundle.main.url(forResource: base, withExtension: "lproj") != nil
        // The bare language, never with the device's region: SwiftUI reads the `.lproj` of the locale's exact identifier,
        // so "es_US" draws every literal in English. Dates and amounts take the device's region at the next launch.
        return Locale(identifier: hasBase ? base : identifier)
    }

    /// Called before the windows draw in `language`, at launch and on each pick.
    static func apply(_ language: AppLanguage) {
        let bundle = bundle(for: identifier(of: language))
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

    /// A variant's `.lproj` (es-ES) only holds the words it says differently, which the system reads over its
    /// language's (es); a bundle read on its own has no such fallback, so the two are merged into one.
    private static func bundle(for identifier: String) -> Bundle? {
        guard let variant = Bundle.main.url(forResource: identifier, withExtension: "lproj") else { return nil }
        let language = String(identifier.prefix { $0 != "-" })
        guard language != identifier,
              let base = Bundle.main.url(forResource: language, withExtension: "lproj"),
              let merged = merge(variant, over: base, named: identifier)
        else { return Bundle(url: variant) }
        return Bundle(url: merged)
    }

    /// Once per version of the tables: a launch finds them merged already, and a build that changed a string (a debug
    /// build keeps its number) merges them again.
    private static func merge(_ variant: URL, over base: URL, named identifier: String) -> URL? {
        let files = FileManager.default
        let tables = [base, variant].flatMap { lproj in
            (try? files.contentsOfDirectory(at: lproj, includingPropertiesForKeys: nil)) ?? []
        }
        let version = tables
            .compactMap { try? files.attributesOfItem(atPath: $0.path)[.modificationDate] as? Date }
            .map(\.timeIntervalSince1970)
            .max() ?? 0
        let merged = files.temporaryDirectory
            .appending(path: "AppLocale/\(Int(version))/\(identifier).lproj", directoryHint: .isDirectory)
        let done = merged.appending(path: ".merged")
        if files.fileExists(atPath: done.path) { return merged }
        do {
            try? files.removeItem(at: merged)
            try files.createDirectory(at: merged, withIntermediateDirectories: true)
            for table in try files.contentsOfDirectory(at: base, includingPropertiesForKeys: nil)
            where ["strings", "stringsdict"].contains(table.pathExtension) {
                var entries = dictionary(at: table)
                let overrides = dictionary(at: variant.appending(path: table.lastPathComponent))
                entries.merge(overrides) { _, override in override }
                let data = try PropertyListSerialization.data(fromPropertyList: entries, format: .binary, options: 0)
                try data.write(to: merged.appending(path: table.lastPathComponent))
            }
            try Data().write(to: done)
            return merged
        } catch {
            return nil
        }
    }

    private static func dictionary(at url: URL) -> [String: Any] {
        guard let data = try? Data(contentsOf: url) else { return [:] }
        return (try? PropertyListSerialization.propertyList(from: data, format: nil)) as? [String: Any] ?? [:]
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
