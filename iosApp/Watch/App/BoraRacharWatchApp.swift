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
        BlitzSplitWatch.shared.start()
        WatchWidgetWriter.shared.start()
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

final class WatchAppDelegate: NSObject, WKApplicationDelegate {
    func applicationDidFinishLaunching() {
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
