import UIKit
import XCTest

/// Captures the App Store screenshots (docs/store-listing-screenshots.md) on the simulator it runs on: for every
/// language, it launches the app seeded with the sample trip on each listing screen and writes the whole screen to
/// `$STORE_SCREENSHOTS_OUTPUT/<slot>/<locale>/<file>.png`, which the Robolectric generators then frame. Run through
/// scripts/store-screenshots/capture-ios.sh, which picks the simulator of each slot, passes these variables and runs
/// the test once per language (`STORE_SCREENSHOTS_LOCALES`), so its log says which language is being captured. The
/// Mac's window is captured by capture-mac.sh instead, with no test: a Mac's UI test runner is sandboxed and can't
/// write the captures out.
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

    /// How long a launched screen takes to seed, open and finish its transitions before the capture. "Bill items"
    /// opens by itself in that time (BlitzSplitApple.createSplitBillViewModel), so the test taps nothing.
    private static let settle: TimeInterval = 4

    /// The slot of the iPhone Duo open, which capture-ios.sh opens in Device Hub before the test runs.
    private static let duoOpenSlot = "iphoneduoopen"

    /// The long side of the iPhone Duo's inner display, in pixels.
    private static let innerDisplayLongSide = 2853

    override func setUp() {
        continueAfterFailure = false
    }

    @MainActor
    func testCaptureStoreScreenshots() throws {
        let environment = ProcessInfo.processInfo.environment
        let output = URL(fileURLWithPath: try XCTUnwrap(environment["STORE_SCREENSHOTS_OUTPUT"]))
        let slot = try XCTUnwrap(environment["STORE_SCREENSHOTS_SLOT"])
        // The tables capture-mac.sh reads too, beside the photos: scripts/store-screenshots
        let tables = URL(fileURLWithPath: try XCTUnwrap(environment["STORE_SCREENSHOTS_ASSETS"]))
        let locales = try Self.rows(of: "locales", fields: 4, in: tables).map {
            ListingLocale(tag: $0[0], language: $0[1], appleLanguage: $0[2], appleLocale: $0[3])
        }
        let screens = try Self.rows(of: "screens", fields: 3, in: tables).map {
            ListingScreen(code: $0[0], fileName: $0[1], isDark: $0[2] == "dark")
        }
        let assets = try Self.assetsEnvironment(tables)
        let onlyLocales = Self.subset(environment["STORE_SCREENSHOTS_LOCALES"])
        let onlyScreens = Self.subset(environment["STORE_SCREENSHOTS_SCREENS"])
        XCUIDevice.shared.orientation = .portrait
        // capture-ios.sh runs this test once per language. The setting stays on the simulator, so only the runs
        // before a slot's first captured language switch it: 26 visits to Settings would be 25 more chances to fail.
        let isDeviceSetUp = !(environment["STORE_SCREENSHOTS_DEVICE_IS_SET_UP"] ?? "").isEmpty
        if slot.hasPrefix("ipad"), !isDeviceSetUp { useFullScreenApps() }
        // The app's own language comes back once the captures end, whatever they stopped on.
        defer { restoreAppLanguage() }
        for locale in locales where onlyLocales?.contains(locale.tag) ?? true {
            let directory = output.appendingPathComponent(slot).appendingPathComponent(locale.tag)
            try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
            for screen in screens where onlyScreens?.contains(screen.code) ?? true {
                let png = try capture(screen, in: locale, assets: assets, isDuoOpen: slot == Self.duoOpenSlot)
                try png.write(to: directory.appendingPathComponent("\(screen.fileName).png"))
            }
        }
    }

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

    /// The rows of `<name>.tsv`, each its tab-separated fields, without the `#` comment lines and the empty ones. A
    /// row short of a field (spaces typed for a tab) fails the test here, by name, instead of trapping where it is
    /// read.
    private static func rows(of name: String, fields: Int, in directory: URL) throws -> [[String]] {
        let rows = try String(contentsOf: directory.appendingPathComponent("\(name).tsv"), encoding: .utf8)
            .split(separator: "\n")
            .filter { !$0.hasPrefix("#") }
            .map { $0.split(separator: "\t").map(String.init) }
        for row in rows {
            XCTAssertEqual(row.count, fields, "\(name).tsv: \(row.joined(separator: " ")) has not \(fields) fields")
        }
        return rows
    }

    /// The faces, the groups' photos and the receipt's photo of assets.tsv, base64 in the launch environment, which
    /// the app writes to a folder of its own (StoreScreenshotsRun): the Mac's capture hands them the same way, since
    /// the Mac app's sandbox can't read the repository.
    private static func assetsEnvironment(_ directory: URL) throws -> [String: String] {
        try rows(of: "assets", fields: 2, in: directory).reduce(into: [:]) { environment, asset in
            let photo = try Data(contentsOf: directory.appendingPathComponent(asset[1]))
            environment["STORE_SCREENSHOTS_ASSET_\(asset[0])"] = photo.base64EncodedString()
        }
    }

    @MainActor
    private func capture(
        _ screen: ListingScreen,
        in locale: ListingLocale,
        assets: [String: String],
        isDuoOpen: Bool
    ) throws -> Data {
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
        let png = isDuoOpen ? Self.innerDisplayPNG() : XCUIScreen.main.screenshot().pngRepresentation
        app.terminate()
        return try XCTUnwrap(png, "The iPhone Duo isn't open: no screen of its inner display's size is lying down")
    }

    /// The iPhone Duo's inner display, nil while the Duo isn't open. Open, the Duo draws there, on what XCTest lists
    /// as a second screen, while the main one, the outer display, is off: of the two, the larger is the one in use. A
    /// screen longer than the inner display is none of the Duo's own (the simulator has a resizable scene display
    /// too) and is left out. XCTest hands the display lying down as upright pixels with a rotation tag, which the
    /// frame's decoder doesn't read, so the image is drawn again the way it is seen, pixel for pixel. Closed, the
    /// inner display is off and upright, and nothing is returned rather than a black capture for the frame to stretch.
    @MainActor
    private static func innerDisplayPNG() -> Data? {
        let images = XCUIScreen.screens.map { $0.screenshot().image }.filter { image in
            image.cgImage.map { max($0.width, $0.height) <= innerDisplayLongSide } ?? false
        }
        guard let image = images.max(by: { pixels(of: $0) < pixels(of: $1) }),
              image.size.width > image.size.height
        else { return nil }
        let size = CGSize(
            width: (image.size.width * image.scale).rounded(),
            height: (image.size.height * image.scale).rounded()
        )
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        format.opaque = true
        return UIGraphicsImageRenderer(size: size, format: format).pngData { _ in
            image.draw(in: CGRect(origin: .zero, size: size))
        }
    }

    private static func pixels(of image: UIImage) -> Int {
        image.cgImage.map { $0.width * $0.height } ?? 0
    }
}
