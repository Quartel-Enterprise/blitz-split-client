import Shared
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

    func register(deviceToken: Data) {
        let token = deviceToken.map { String(format: "%02x", $0) }.joined()
        #if DEBUG
        let isSandbox = true
        #else
        let isSandbox = false
        #endif
        #if os(iOS)
        let isMac = false
        #else
        let isMac = true
        #endif
        BlitzSplitApple.shared.setPushToken(token: token, isMac: isMac, isSandbox: isSandbox)
    }

    /// Início asks once, the first time it shows: the system asks only while the person never answered.
    static func askPermission() async {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        guard settings.authorizationStatus == .notDetermined else { return }
        _ = try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound])
    }

    /// Shown with the app open too, as Atividade's badge doesn't say what happened.
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .list, .sound]
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        guard let link = response.notification.request.content.userInfo["link"] as? String,
              let url = URL(string: link) else { return }
        await MainActor.run { BlitzSplitApple.shared.handleDeepLink(url: url) }
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
}
#else
final class AppDelegate: NSObject, NSApplicationDelegate {
    func applicationWillFinishLaunching(_ notification: Notification) {
        PushNotifications.shared.start()
    }

    func application(_ application: NSApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        PushNotifications.shared.register(deviceToken: deviceToken)
    }
}
#endif
