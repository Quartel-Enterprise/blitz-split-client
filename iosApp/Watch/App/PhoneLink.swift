import Foundation
import Shared
import WatchConnectivity

/// The watch's side of WatchConnectivity, in the format `AppleWatchLink` (`WatchLink`'s for Swift) names for both
/// apps: the watch sends its pairing code to the iPhone (`PAIR_PATH`, the iPhone approves it with its own session and
/// answers with one of the `REPLY_` values), and the iPhone tells its watch it signed out (`SIGNED_OUT_PATH`, as user
/// info, queued while the watch is away). Nothing else goes over it: once connected, the watch talks to the server
/// on its own.
final class PhoneLink: NSObject, WCSessionDelegate {
    static let shared = PhoneLink()

    /// Read off the main actor too, by the WatchConnectivity callbacks: its values are constants.
    private nonisolated(unsafe) static let link = AppleWatchLink.shared

    /// Called at launch, so the iPhone's queued sign out arrives even before a screen opens.
    func start() {
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = self
        WCSession.default.activate()
    }

    /// The code to the paired iPhone; `reply` comes back on the main actor, `.unreachable` when no iPhone with the app
    /// took it. The pairing waits 20 seconds at most for an answer on its own.
    func sendPairingCode(_ code: String, reply: @escaping @MainActor (PhoneReply) -> Void) {
        let session = WCSession.default
        // Connect tapped right after launch: the session activates in a moment, then the code goes
        if WCSession.isSupported(), session.activationState != .activated {
            pendingSends.append((code, reply))
            return
        }
        guard WCSession.isSupported(), session.isCompanionAppInstalled, session.isReachable else {
            reply(.unreachable)
            return
        }
        let message: [String: Any] = [Self.link.PATH_KEY: Self.link.PAIR_PATH, Self.link.CODE_KEY: code]
        session.sendMessage(message) { answer in
            let value = answer[Self.link.REPLY_KEY] as? String
            Task { @MainActor in reply(Self.phoneReply(of: value)) }
        } errorHandler: { _ in
            Task { @MainActor in reply(.unreachable) }
        }
    }

    private nonisolated static func phoneReply(of value: String?) -> PhoneReply {
        switch value {
        case Self.link.REPLY_APPROVED: .approved
        case Self.link.REPLY_LOCAL_ONLY: .localOnly
        default: .unreachable
        }
    }

    /// Codes sent before the session activated; an activation that fails sends them anyway, as out of reach.
    private var pendingSends: [(code: String, reply: @MainActor (PhoneReply) -> Void)] = []

    nonisolated func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: Error?,
    ) {
        Task { @MainActor in
            let sends = self.pendingSends
            self.pendingSends = []
            for send in sends {
                if activationState == .activated {
                    self.sendPairingCode(send.code, reply: send.reply)
                } else {
                    send.reply(.unreachable)
                }
            }
        }
    }

    /// The iPhone signed out (15.13): queued user info, delivered even while the app wasn't running.
    nonisolated func session(_ session: WCSession, didReceiveUserInfo userInfo: [String: Any] = [:]) {
        handle(userInfo)
    }

    nonisolated func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        handle(message)
    }

    private nonisolated func handle(_ payload: [String: Any]) {
        guard payload[Self.link.PATH_KEY] as? String == Self.link.SIGNED_OUT_PATH,
              let userId = payload[Self.link.USER_ID_KEY] as? String else { return }
        Task { @MainActor in BlitzSplitWatch.shared.endSessionFromPhone(userId: userId) }
    }
}
