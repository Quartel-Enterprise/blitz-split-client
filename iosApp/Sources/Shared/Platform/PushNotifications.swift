import Shared
import SwiftUI
import UserNotifications
#if os(iOS)
import UIKit
#else
import AppKit
#endif

/// Atividade's push notifications (APNs). The server sends each one with a `loc-key` of Localizable.xcstrings
/// (`push.<kind>`) and the words that change in it, so the system words it in the device's language, and its `link`,
/// which a tap opens as an invite's link is opened. The token goes to the shared code on every launch: the server
/// sends nothing until its APNs key is set, so a build here needs nothing else.
final class PushNotifications: NSObject, UNUserNotificationCenterDelegate {
    static let shared = PushNotifications()

    /// Before the launch ends, so a tap that launched the app reaches it.
    func start() {
        UNUserNotificationCenter.current().delegate = self
        #if os(iOS)
        UIApplication.shared.registerForRemoteNotifications()
        #else
        NSApplication.shared.registerForRemoteNotifications()
        #endif
    }

    /// An event's push carries `content-available`: the sync starts as it arrives, so a tap finds the bill on the device.
    /// Nothing syncs for the reminder's nor for an event already on the device.
    func syncForPush(userInfo: [AnyHashable: Any], onDone: @escaping (PushArrivalOutcome) -> Void = { _ in }) {
        BlitzSplitApple.shared.syncForPush(link: userInfo["link"] as? String, onDone: onDone)
    }

    func register(deviceToken: Data) {
        #if os(iOS)
        let isMac = false
        #else
        let isMac = true
        #endif
        BlitzSplitApple.shared.setPushToken(
            token: ApnsToken.hex(deviceToken),
            isMac: isMac,
            isSandbox: ApnsToken.isSandbox,
        )
    }

    /// The permission as the shared code takes it: provisional delivers quietly, where the full prompt can still ask;
    /// ephemeral (an App Clip's) delivers, so it counts as allowed.
    static func readPermission() async -> NotificationPermission {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        return switch settings.authorizationStatus {
        case .notDetermined: .notDetermined
        case .denied: .denied
        case .provisional: .provisional
        default: .granted
        }
    }

    /// Only from a tap (Home's card, Notifications' "Push notifications" switch or its notice, a Push toggle while Apple
    /// delivers quietly): the system shows its prompt while the person never answered it, provisional included.
    static func requestPermission() async {
        _ = try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound])
    }

    /// Once the shared code says so (there is a group): quiet delivery to Notification Center, with no prompt.
    static func requestQuietPermission() async {
        _ = try? await UNUserNotificationCenter.current()
            .requestAuthorization(options: [.alert, .badge, .sound, .provisional])
    }

    /// The app's own notification settings, where only the person can allow them once refused.
    static func openSettings() {
        #if os(iOS)
        guard let url = URL(string: UIApplication.openNotificationSettingsURLString) else { return }
        UIApplication.shared.open(url)
        #else
        let bundleId = Bundle.main.bundleIdentifier ?? ""
        let address = "x-apple.systempreferences:com.apple.Notifications-Settings.extension?id=\(bundleId)"
        guard let url = URL(string: address) else { return }
        NSWorkspace.shared.open(url)
        #endif
    }

    /// Shown with the app open too, as Atividade's badge doesn't say what happened. Answered at once, on the thread
    /// the system called, for the same reason as the tap below.
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound])
    }

    /// The completion handler, not the async form: Swift calls an async delegate's handler from the executor the method
    /// ended on, off the main thread, and UIKit throws "Call must be made on main thread" when the tap brings the app
    /// back from the background. Both the link and the handler go to the main queue instead.
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping @Sendable () -> Void
    ) {
        let link = response.notification.request.content.userInfo["link"] as? String
        DispatchQueue.main.async {
            if let link, let url = URL(string: link) {
                BlitzSplitApple.shared.handleDeepLink(url: url)
            }
            completionHandler()
        }
    }
}

#if os(iOS)
final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        PushNotifications.shared.start()
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        PushNotifications.shared.register(deviceToken: deviceToken)
    }

    /// Woken in the background by `content-available` (the `remote-notification` background mode), or called in front.
    /// iOS gives the app about 30 seconds and wants the handler called by then; the shared sync stops at 25.
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        PushNotifications.shared.syncForPush(userInfo: userInfo) { outcome in
            let result: UIBackgroundFetchResult = if outcome == .synced {
                .newData
            } else if outcome == .failed {
                .failed
            } else {
                .noData
            }
            completionHandler(result)
        }
    }
}
#else
final class AppDelegate: NSObject, NSApplicationDelegate {
    func applicationWillFinishLaunching(_ notification: Notification) {
        PushNotifications.shared.start()
    }

    #if DEBUG
    func applicationDidFinishLaunching(_ notification: Notification) {
        StoreScreenshotsRun.openWindowIfLaunchedBehind()
    }
    #endif

    func application(_ application: NSApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        PushNotifications.shared.register(deviceToken: deviceToken)
    }

    /// The Mac gets it only while the app runs, with no background limit: real time listens only in front, so a push
    /// that arrives with the window behind another app's syncs here.
    func application(_ application: NSApplication, didReceiveRemoteNotification userInfo: [String: Any]) {
        PushNotifications.shared.syncForPush(userInfo: userInfo)
    }
}
#endif

/// Reads the permission when the view appears and on every return to the app: the Mac's scene stays active while the
/// person is in System Settings, so it listens to the app becoming active.
private struct NotificationPermissionChecks: ViewModifier {
    let check: () -> Void

    func body(content: Content) -> some View {
        content
            .onAppear(perform: check)
            .onReceive(NotificationCenter.default.publisher(for: Self.didBecomeActive)) { _ in check() }
    }

    #if os(macOS)
    private static let didBecomeActive = NSApplication.didBecomeActiveNotification
    #else
    private static let didBecomeActive = UIApplication.didBecomeActiveNotification
    #endif
}

extension View {
    func checkingNotificationPermission(_ check: @escaping () -> Void) -> some View {
        modifier(NotificationPermissionChecks(check: check))
    }
}
