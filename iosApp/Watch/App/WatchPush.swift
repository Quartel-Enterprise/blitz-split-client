import Foundation
import Shared
import UserNotifications
import WatchKit

/// The watch's own pushes of Activity (15.7), APNs to the watch app, never the iPhone's mirrored notifications. The
/// server words each one with a `loc-key` of Localizable.xcstrings, as for the iPhone, and its `link`
/// (blitzsplit.com/n?k=…&e=…&g=…). "Someone marked that they paid you" comes in the `PAYMENT_MARKED` category, whose
/// "Received" marks the activity read and "View group" opens the group on the watch; a tap on any other opens its
/// group.
final class WatchPush: NSObject, UNUserNotificationCenterDelegate {
    static let shared = WatchPush()

    private static let paymentMarked = "PAYMENT_MARKED"
    private static let received = "RECEIVED"
    private static let viewGroup = "VIEW_GROUP"

    /// At launch, so a tap that launched the app reaches it.
    func start() {
        let center = UNUserNotificationCenter.current()
        center.delegate = self
        center.setNotificationCategories([
            UNNotificationCategory(
                identifier: Self.paymentMarked,
                actions: [
                    // Background: the watch marks it read without opening the app
                    UNNotificationAction(
                        identifier: Self.received,
                        title: String(localized: "Received", bundle: .appLanguage),
                        options: [],
                    ),
                    UNNotificationAction(
                        identifier: Self.viewGroup,
                        title: String(localized: "View group", bundle: .appLanguage),
                        options: [.foreground],
                    ),
                ],
                intentIdentifiers: [],
            ),
        ])
        WKApplication.shared().registerForRemoteNotifications()
    }

    private var askedPermission = false

    /// Once connected, once per launch: the system's prompt, so the watch's pushes can show (it answers at once after
    /// the person's first answer).
    func requestPermission() {
        guard !askedPermission else { return }
        askedPermission = true
        Task {
            _ = try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound])
        }
    }

    func register(deviceToken: Data) {
        BlitzSplitWatch.shared.setPushToken(token: ApnsToken.hex(deviceToken), isSandbox: ApnsToken.isSandbox)
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void,
    ) {
        completionHandler([.banner, .list, .sound])
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping @Sendable () -> Void,
    ) {
        let link = response.notification.request.content.userInfo["link"] as? String
        let action = response.actionIdentifier
        DispatchQueue.main.async {
            guard let link else {
                completionHandler()
                return
            }
            if action == Self.received {
                BlitzSplitWatch.shared.handleReceived(link: link) { completionHandler() }
            } else {
                if action != UNNotificationDismissActionIdentifier {
                    BlitzSplitWatch.shared.openPush(link: link)
                }
                completionHandler()
            }
        }
    }
}
