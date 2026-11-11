#if DEBUG
import Foundation
#if os(macOS)
import AppKit
#endif

/// The App Store screenshots' launch (docs/store-listing-screenshots.md): the UI tests in UITests start a debug build
/// with these arguments, and the shared code seeds the sample trip in that language and theme, then opens the screen.
/// Compiled out of a release build, whose shared framework would ignore the seed anyway.
struct StoreScreenshotsRun {
    let language: String
    let isDark: Bool
    let assetsDirectory: String?
    let screen: String?

    /// The Mac window's size in points: 2880x1800 on a Retina display, the largest the Mac slot takes.
    static let windowSize = CGSize(width: 1440, height: 900)

    /// Set only when the app was launched with `-StoreScreenshotsLanguage <code>`, which a person never does. Read
    /// once, so the assets are written once per launch however often the window appears.
    static let current: StoreScreenshotsRun? = {
        let defaults = UserDefaults.standard
        guard let language = defaults.string(forKey: "StoreScreenshotsLanguage") else { return nil }
        return StoreScreenshotsRun(
            language: language,
            isDark: defaults.string(forKey: "StoreScreenshotsTheme") != "light",
            assetsDirectory: writeAssets(),
            screen: defaults.string(forKey: "StoreScreenshotsScreen"),
        )
    }()

    /// The key that keeps the app's own language while a run sets the listing's, and the argument that puts it back.
    private static let savedLanguagesKey = "StoreScreenshotsSavedLanguages"
    private static let restoreArgument = "StoreScreenshotsRestore"

    /// The app's own AppleLanguages (the Settings app's per-app language, the in-app picker), before the first launch of
    /// a run replaces it with the listing's: on the Mac the captures run the developer's own debug app. An empty list
    /// stands for none. Read from the app's persistent domain, never the launch arguments' -AppleLanguages.
    static func saveLanguages() {
        let defaults = UserDefaults.standard
        guard defaults.object(forKey: savedLanguagesKey) == nil, let domain = Bundle.main.bundleIdentifier else { return }
        let languages = defaults.persistentDomain(forName: domain)?["AppleLanguages"] as? [String] ?? []
        defaults.set(languages, forKey: savedLanguagesKey)
    }

    /// The UI test's last launch, `-StoreScreenshotsRestore YES`, puts the app's own language back once every capture
    /// is made.
    static func restoreLanguagesIfAsked() {
        let defaults = UserDefaults.standard
        guard defaults.bool(forKey: restoreArgument), let saved = defaults.stringArray(forKey: savedLanguagesKey) else {
            return
        }
        if saved.isEmpty {
            defaults.removeObject(forKey: "AppleLanguages")
        } else {
            defaults.set(saved, forKey: "AppleLanguages")
        }
        defaults.removeObject(forKey: savedLanguagesKey)
    }

    /// The faces and the receipt the UI test passed as base64, written where this app may read them: the shared code
    /// reads `faces/person<n>.jpg` and `receipt.jpg` there. Nil when the test passed none.
    private static func writeAssets() -> String? {
        let prefix = "STORE_SCREENSHOTS_ASSET_"
        let assets = ProcessInfo.processInfo.environment.filter { $0.key.hasPrefix(prefix) }
        guard !assets.isEmpty else { return nil }
        let root = FileManager.default.temporaryDirectory.appendingPathComponent("store-screenshots")
        let faces = root.appendingPathComponent("faces")
        try? FileManager.default.createDirectory(at: faces, withIntermediateDirectories: true)
        for (key, base64) in assets {
            guard let data = Data(base64Encoded: base64) else { continue }
            let name = String(key.dropFirst(prefix.count))
            let file = name == "receipt" ? root.appendingPathComponent("receipt.jpg") : faces.appendingPathComponent("\(name).jpg")
            try? data.write(to: file)
        }
        return root.path
    }

    #if os(macOS)
    /// Every capture of the Mac is the same window size, whatever the last run left it at, alone on the desktop: the
    /// other apps' windows are hidden (as ⌥⌘H does, no permission needed), so the sidebar's glass shows nothing of
    /// them. They come back with a click on their Dock icon.
    static func sizeWindow() {
        guard current != nil, let window = NSApplication.shared.windows.first else { return }
        NSApplication.shared.hideOtherApplications(nil)
        window.setContentSize(windowSize)
        window.center()
        window.makeKeyAndOrderFront(nil)
    }
    #endif
}
#endif
