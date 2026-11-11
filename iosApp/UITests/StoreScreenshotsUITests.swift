import XCTest

/// Captures the App Store screenshots (docs/store-listing-screenshots.md) on the simulator it runs on: for every
/// language, it launches the app seeded with the sample trip on each listing screen and writes the whole screen to
/// `$STORE_SCREENSHOTS_OUTPUT/<slot>/<locale>/<file>.png`, which the Robolectric generators then frame. Run through
/// scripts/store-screenshots/capture-ios.sh, which picks the simulator of each slot and passes these variables.
final class StoreScreenshotsUITests: XCTestCase {
    /// The listing's languages: the generated folder, the app's language code, and what iOS is told to run in.
    private struct ListingLocale {
        let tag: String
        let language: String
        let appleLanguage: String
        let appleLocale: String
    }

    /// One listing screen: the code the app opens it by, the file it is written to (its shelf order) and its theme.
    private struct ListingScreen {
        let code: String
        let fileName: String
        let isDark: Bool
    }

    private static let locales = [
        ListingLocale(tag: "en-US", language: "en", appleLanguage: "en-US", appleLocale: "en_US"),
        ListingLocale(tag: "pt-BR", language: "pt", appleLanguage: "pt-BR", appleLocale: "pt_BR"),
        ListingLocale(tag: "es", language: "es", appleLanguage: "es-419", appleLocale: "es_ES"),
        ListingLocale(tag: "fr", language: "fr", appleLanguage: "fr-FR", appleLocale: "fr_FR"),
        ListingLocale(tag: "de", language: "de", appleLanguage: "de-DE", appleLocale: "de_DE"),
        ListingLocale(tag: "it", language: "it", appleLanguage: "it-IT", appleLocale: "it_IT"),
        ListingLocale(tag: "pl", language: "pl", appleLanguage: "pl-PL", appleLocale: "pl_PL"),
        ListingLocale(tag: "nl", language: "nl", appleLanguage: "nl-NL", appleLocale: "nl_NL"),
        ListingLocale(tag: "tr", language: "tr", appleLanguage: "tr-TR", appleLocale: "tr_TR"),
        ListingLocale(tag: "id", language: "id", appleLanguage: "id-ID", appleLocale: "id_ID"),
        ListingLocale(tag: "ja", language: "ja", appleLanguage: "ja-JP", appleLocale: "ja_JP"),
    ]

    /// The same order and file names as StoreScreen in :android:store_screenshots.
    private static let screens = [
        ListingScreen(code: "group", fileName: "01_group", isDark: true),
        ListingScreen(code: "items", fileName: "02_split_by_items", isDark: true),
        ListingScreen(code: "receipt", fileName: "03_receipt", isDark: true),
        ListingScreen(code: "balances", fileName: "04_balances", isDark: true),
        ListingScreen(code: "invite", fileName: "05_invite", isDark: true),
        ListingScreen(code: "statistics", fileName: "06_statistics", isDark: true),
        ListingScreen(code: "home", fileName: "07_home", isDark: true),
        ListingScreen(code: "payers", fileName: "08_payers_and_currencies", isDark: true),
        ListingScreen(code: "activity", fileName: "09_activity", isDark: true),
        ListingScreen(code: "light", fileName: "10_light_theme", isDark: false),
    ]

    /// How long a launched screen takes to seed, open and finish its transitions before the capture.
    private static let settle: TimeInterval = 4

    override func setUp() {
        continueAfterFailure = false
    }

    @MainActor
    func testCaptureStoreScreenshots() throws {
        let environment = ProcessInfo.processInfo.environment
        let output = URL(fileURLWithPath: try XCTUnwrap(environment["STORE_SCREENSHOTS_OUTPUT"]))
        let slot = try XCTUnwrap(environment["STORE_SCREENSHOTS_SLOT"])
        let assets = try Self.assetsEnvironment(environment["STORE_SCREENSHOTS_ASSETS"])
        let onlyLocales = Self.subset(environment["STORE_SCREENSHOTS_LOCALES"])
        let onlyScreens = Self.subset(environment["STORE_SCREENSHOTS_SCREENS"])
        #if os(iOS)
        XCUIDevice.shared.orientation = .portrait
        if slot.hasPrefix("ipad") { useFullScreenApps() }
        #endif
        // The app's own language comes back once the captures end, whatever they stopped on.
        defer { restoreAppLanguage() }
        for locale in Self.locales where onlyLocales?.contains(locale.tag) ?? true {
            let directory = output.appendingPathComponent(slot).appendingPathComponent(locale.tag)
            try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
            for screen in Self.screens where onlyScreens?.contains(screen.code) ?? true {
                let png = capture(screen, in: locale, assets: assets)
                try png.write(to: directory.appendingPathComponent("\(screen.fileName).png"))
            }
        }
    }

    #if os(iOS)
    /// iPadOS 26 opens apps in windows by default, which leaves the app short of the screen: Settings' Multitasking &
    /// Gestures switches the simulator to full-screen apps first. Settings runs in the simulator's own language,
    /// English, whatever the listing's.
    @MainActor
    private func useFullScreenApps() {
        let settings = XCUIApplication(bundleIdentifier: "com.apple.Preferences")
        settings.launch()
        let multitasking = settings.staticTexts["Multitasking & Gestures"]
        if multitasking.waitForExistence(timeout: Self.settle) {
            multitasking.tap()
            let fullScreen = settings.descendants(matching: .any)
                .matching(NSPredicate(format: "label BEGINSWITH 'Full Screen'"))
                .firstMatch
            if fullScreen.waitForExistence(timeout: Self.settle) { fullScreen.tap() }
        }
        settings.terminate()
    }
    #endif

    /// One last launch, without the seed, that puts back the language the app had before the run (StoreScreenshotsRun).
    @MainActor
    private func restoreAppLanguage() {
        let app = XCUIApplication()
        app.launchArguments = ["-StoreScreenshotsRestore", "YES"]
        app.launch()
        app.terminate()
    }

    /// A comma-separated list narrowing the run, nil (everything) when unset or empty.
    private static func subset(_ list: String?) -> Set<String>? {
        guard let list, !list.isEmpty else { return nil }
        return Set(list.split(separator: ",").map(String.init))
    }

    /// The faces and the receipt's photo, base64 in the launch environment: the Mac app's sandbox can't read the
    /// repository, so the app writes them to a folder of its own (StoreScreenshotsRun).
    private static func assetsEnvironment(_ directory: String?) throws -> [String: String] {
        guard let directory else { return [:] }
        let root = URL(fileURLWithPath: directory)
        var files = ["receipt": root.appendingPathComponent("receipt.jpg")]
        for person in 0..<5 {
            files["person\(person)"] = root.appendingPathComponent("faces/person\(person).jpg")
        }
        return try files.reduce(into: [:]) { environment, file in
            environment["STORE_SCREENSHOTS_ASSET_\(file.key)"] = try Data(contentsOf: file.value).base64EncodedString()
        }
    }

    @MainActor
    private func capture(_ screen: ListingScreen, in locale: ListingLocale, assets: [String: String]) -> Data {
        let app = XCUIApplication()
        app.launchArguments = [
            "-StoreScreenshotsLanguage", locale.language,
            "-StoreScreenshotsTheme", screen.isDark ? "dark" : "light",
            "-StoreScreenshotsScreen", screen.code,
            "-AppleLanguages", "(\(locale.appleLanguage))",
            "-AppleLocale", locale.appleLocale,
        ]
        app.launchEnvironment = assets
        app.launch()
        Thread.sleep(forTimeInterval: Self.settle)
        if screen.code == "items" {
            let split = app.buttons["splitBill.split"]
            XCTAssertTrue(split.waitForExistence(timeout: Self.settle), "No Split row to open Bill items")
            split.tap()
            Thread.sleep(forTimeInterval: Self.settle / 2)
        }
        #if os(macOS)
        // The window alone, at the size StoreScreenshotsRun gives it, which the Mac's frame draws on its own canvas
        let png = app.windows.firstMatch.screenshot().pngRepresentation
        #else
        let png = XCUIScreen.main.screenshot().pngRepresentation
        #endif
        app.terminate()
        return png
    }
}
