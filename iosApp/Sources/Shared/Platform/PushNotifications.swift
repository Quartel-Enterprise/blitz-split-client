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

    func register(deviceToken: Data) {
        let token = deviceToken.map { String(format: "%02x", $0) }.joined()
        #if os(iOS)
        let isMac = false
        #else
        let isMac = true
        #endif
        BlitzSplitApple.shared.setPushToken(token: token, isMac: isMac, isSandbox: Self.isSandbox)
    }

    /// The APNs environment the build was signed for, as its provisioning profile says, since a Release build signed
    /// for development gets a sandbox token. A build with no profile (from the App Store, or the simulator) goes by
    /// its configuration: production for a release, the sandbox for a debug build.
    private static var isSandbox: Bool {
        #if os(iOS)
        let profileURL = Bundle.main.url(forResource: "embedded", withExtension: "mobileprovision")
        let key = "aps-environment"
        #else
        let profileURL: URL? = Bundle.main.bundleURL.appendingPathComponent("Contents/embedded.provisionprofile")
        let key = "com.apple.developer.aps-environment"
        #endif
        if let environment = profileURL.flatMap({ apsEnvironment(inProfileAt: $0, key: key) }) {
            return environment == "development"
        }
        #if DEBUG
        return true
        #else
        return false
        #endif
    }

    /// The profile is a signed envelope around a plist, read here without checking the signature.
    private static func apsEnvironment(inProfileAt url: URL, key: String) -> String? {
        guard let data = try? Data(contentsOf: url),
              let start = data.range(of: Data("<?xml".utf8)),
              let end = data.range(of: Data("</plist>".utf8), in: start.lowerBound..<data.endIndex),
              let plist = try? PropertyListSerialization.propertyList(
                  from: data.subdata(in: start.lowerBound..<end.upperBound),
                  format: nil
              ) as? [String: Any],
              let entitlements = plist["Entitlements"] as? [String: Any] else { return nil }
        return entitlements[key] as? String
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

    /// Only from a tap (Home's card, a Push toggle, Notifications' notice): the system shows its prompt while the person
    /// never answered it, provisional included.
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
