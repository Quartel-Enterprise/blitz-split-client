import Foundation
import Shared
import UIKit
import UserNotifications
import WatchConnectivity

/// The iPhone's side of the Apple Watch's link (docs/product/watch.md), Android's WatchPairingListenerService and
/// WatchSignOutRelay: over WatchConnectivity each message is a dictionary of strings, its "path" and the pairing's
/// "code", its "reply" or the signed out "userId" (`AppleWatchLink`, the shared WatchLink).
///
/// - Nearby pairing (15.10): the paired watch sends its code, which the iPhone approves with its own session and no
///   confirmation (Pierre's call: the channel only exists with a watch paired to this iPhone, which needs it
///   unlocked), answers with one of WatchLink's replies and then shows "Apple Watch connected" (15.15), information
///   only. The watch's own poll brings its session; the reply only tells it to stop waiting, or that this profile is
///   local only (15.11b). The message wakes the app in the background, where the shared code starts as on any launch.
/// - Signed out here (15.13): the watch is told, queued until it can be reached, and signs out too if it holds the
///   same user. A watch out of reach finds out on its next sync, when its session is refused.
final class WatchPairingRelay: NSObject, WCSessionDelegate {
    static let shared = WatchPairingRelay()

    private let watches = WatchBag()
    /// A sign out seen before the session finished activating, sent once it does.
    private var pendingSignOut: String?

    /// From the app's launch, after the shared code started, so a message that woke the app finds its delegate.
    func start() {
        guard WCSession.isSupported() else { return }
        let session = WCSession.default
        session.delegate = self
        session.activate()
        watches.add(BlitzSplitApple.shared.createAccountSignOutsWatcher().observe { [weak self] userId in
            self?.tellWatchSignedOut(userId: userId as String)
        })
    }

    private func tellWatchSignedOut(userId: String) {
        let session = WCSession.default
        guard session.activationState == .activated else {
            pendingSignOut = userId
            return
        }
        pendingSignOut = nil
        guard session.isPaired, session.isWatchAppInstalled else { return }
        let link = AppleWatchLink.shared
        session.transferUserInfo([link.PATH_KEY: link.SIGNED_OUT_PATH, link.USER_ID_KEY: userId])
    }

    nonisolated func session(
        _ session: WCSession,
        didReceiveMessage message: [String: Any],
        replyHandler: @escaping ([String: Any]) -> Void,
    ) {
        let link = AppleWatchLink.shared
        guard message[link.PATH_KEY] as? String == link.PAIR_PATH,
              let code = message[link.CODE_KEY] as? String else {
            replyHandler([link.REPLY_KEY: link.REPLY_FAILED])
            return
        }
        let reply = ReplyHandler(send: replyHandler)
        Task { @MainActor in
            let link = AppleWatchLink.shared
            // The message may have woken the app in the background, which iOS suspends as soon as it can: the time
            // asked here lasts until the server answered and the watch got the reply
            let backgroundTime = BackgroundTime(name: "Watch pairing")
            BlitzSplitApple.shared.approveNearbyWatch(code: code) { answer in
                reply.send([link.REPLY_KEY: answer])
                if answer == link.REPLY_APPROVED { WatchConnectedNotice.show() }
                backgroundTime.end()
            }
        }
    }

    nonisolated func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: (any Error)?,
    ) {
        guard activationState == .activated else { return }
        Task { @MainActor in
            if let userId = self.pendingSignOut { self.tellWatchSignedOut(userId: userId) }
        }
    }

    nonisolated func sessionDidBecomeInactive(_ session: WCSession) {}

    /// The person switched to another watch: the session starts again for it.
    nonisolated func sessionDidDeactivate(_ session: WCSession) {
        session.activate()
    }
}

/// Time iOS gives the app to finish a piece of work in the background, ended once the work is done or the time is up.
private final class BackgroundTime {
    private var identifier = UIBackgroundTaskIdentifier.invalid

    init(name: String) {
        identifier = UIApplication.shared.beginBackgroundTask(withName: name) { [weak self] in
            self?.end()
        }
    }

    func end() {
        guard identifier != .invalid else { return }
        UIApplication.shared.endBackgroundTask(identifier)
        identifier = .invalid
    }
}

/// WatchConnectivity's reply, answered once the server approved the code, on the main thread.
private nonisolated struct ReplyHandler: @unchecked Sendable {
    let send: ([String: Any]) -> Void
}

/// "Apple Watch connected" (15.15), information only, with no buttons; a tap opens the app. Shown only where the
/// person allowed notifications.
enum WatchConnectedNotice {
    private static let identifier = "watch-connected"

    static func show() {
        let content = UNMutableNotificationContent()
        content.title = String(localized: "Apple Watch connected", bundle: .appLanguage)
        content.body = String(
            localized: "It sees your groups and syncs on its own. To disconnect it, sign out on the watch.",
            bundle: .appLanguage,
        )
        let request = UNNotificationRequest(identifier: identifier, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request)
    }
}
