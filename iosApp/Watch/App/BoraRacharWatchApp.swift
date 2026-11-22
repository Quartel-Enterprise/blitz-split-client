import Shared
import SwiftUI
import WatchKit

@main
struct BoraRacharWatchApp: App {
    @WKApplicationDelegateAdaptor private var appDelegate: WatchAppDelegate
    /// Made once the shared code started, as the iPhone's `AppModel`: the body runs again on each scene phase, and a
    /// model made in `WatchRootView`'s own `@State` would start a `WatchRootViewModel` each time.
    @State private var root: WatchRootModel
    @Environment(\.scenePhase) private var scenePhase

    init() {
        #if DEBUG
        // The README's image (scripts/readme-hero): the sample trip instead of an account, as the iPhone's captures
        if let language = WatchStoreScreenshots.language {
            BlitzSplitWatch.shared.startForStoreScreenshots(language: language, assetsDirectory: WatchStoreScreenshots.assetsDirectory)
        } else {
            BlitzSplitWatch.shared.start()
        }
        #else
        BlitzSplitWatch.shared.start()
        #endif
        // The sample trip must not reach the App Group's snapshot, which the complications read
        if !WatchStoreScreenshots.isRun { WatchWidgetWriter.shared.start() }
        _root = State(initialValue: WatchRootModel())
    }

    var body: some Scene {
        WindowGroup {
            WatchRootView(model: root)
        }
        .onChange(of: scenePhase, initial: true) { _, phase in
            // Real time listens only while the app is on screen; the background refresh catches up after
            BlitzSplitApple.shared.setAppForeground(isForeground: phase == .active)
            if phase == .background { WatchBackgroundRefresh.schedule() }
        }
        .backgroundTask(.appRefresh(WatchBackgroundRefresh.identifier)) {
            await WatchBackgroundRefresh.run()
        }
    }
}

/// The README's capture (scripts/readme-hero/capture-watch.sh) launches the debug build with `-StoreScreenshotsLanguage`.
/// The type exists in every build, since the screens ask it whether a capture is running; what reads the launch
/// arguments is debug only.
enum WatchStoreScreenshots {
    #if DEBUG
    static var language: String? { UserDefaults.standard.string(forKey: "StoreScreenshotsLanguage") }
    /// The store listing's screens (scripts/store-screenshots/watch): `-StoreScreenshotsScreen` names the one the run
    /// walks to from Balances, by acting as a person would: balances, group, confirm or recorded.
    static var screen: String? { UserDefaults.standard.string(forKey: "StoreScreenshotsScreen") }
    /// The folder of the image bank's photos (faces/, groups/) the store capture hands over, a path of this Mac, which
    /// the simulator's app reads as any other.
    static var assetsDirectory: String? { UserDefaults.standard.string(forKey: "StoreScreenshotsAssets") }
    /// Each step of the walk happens once: the screens come back (Balances after the payment) and must not repeat it.
    @MainActor static var opened: Set<String> = []
    #endif
}

extension WatchStoreScreenshots {
    /// The screen the store capture walks to; nil outside it.
    static var targetScreen: String? {
        #if DEBUG
        screen
        #else
        nil
        #endif
    }

    /// Runs `step` once per run, when a store capture is walking to one of `screens`.
    @MainActor static func walk(_ step: String, for screens: Set<String>, _ action: () -> Void) {
        #if DEBUG
        guard let screen = targetScreen, screens.contains(screen), !opened.contains(step) else { return }
        opened.insert(step)
        action()
        #endif
    }

    /// A seeded capture run stays on its own: no iPhone link, push or background refresh may change what it shows.
    static var isRun: Bool {
        #if DEBUG
        language != nil
        #else
        false
        #endif
    }
}

final class WatchAppDelegate: NSObject, WKApplicationDelegate {
    func applicationDidFinishLaunching() {
        if WatchStoreScreenshots.isRun { return }
        PhoneLink.shared.start()
        WatchPush.shared.start()
        WatchBackgroundRefresh.schedule()
    }

    func didRegisterForRemoteNotifications(withDeviceToken deviceToken: Data) {
        WatchPush.shared.register(deviceToken: deviceToken)
    }
}

/// watchOS's background refresh (docs/product/watch.md: battery): about every half hour while the app has the budget,
/// a sync, so the complications and the Smart Stack card follow the other devices' changes. The app opens with what
/// it has and catches up after.
enum WatchBackgroundRefresh {
    static let identifier = "sync"
    private static let interval: TimeInterval = 30 * 60

    static func schedule() {
        WKApplication.shared().scheduleBackgroundRefresh(
            withPreferredDate: Date(timeIntervalSinceNow: interval),
            userInfo: identifier as NSString,
        ) { @Sendable _ in }
    }

    @MainActor
    static func run() async {
        await withCheckedContinuation { continuation in
            BlitzSplitWatch.shared.syncInBackground { continuation.resume() }
        }
        // The faces' file before the task ends, or they would only follow the sync at the app's next launch
        await WatchWidgetWriter.shared.writeAfterSync()
        schedule()
    }
}
