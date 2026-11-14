import FirebaseAnalytics
import FirebaseCore
import Shared

/// Firebase's Crashlytics and Analytics, only in a build with the Firebase project's GoogleService-Info.plist
/// (git-ignored, in Resources): without it the app never starts Firebase. A debug build reports and tracks nothing.
enum FirebaseSetup {
    #if DEBUG
    private static let isDebug = true
    #else
    private static let isDebug = false
    #endif

    /// Firebase Analytics collects in release builds, on by default under legitimate interest, until Profile's "Share
    /// usage data" turns it off on the device (docs/analytics/README.md#consent). false turns it off for everyone.
    private static let collectsAnalytics = true

    private static var isConfigured: Bool {
        Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil
    }

    /// Before the shared code starts, so a crash while it starts is reported.
    static func start() {
        guard isConfigured else { return }
        FirebaseApp.configure()
        // Info.plist keeps it off until the shared code reads "Share usage data", which Analytics then keeps on the
        // device: a debug build turns it off again on every launch, in case a release build ran there before
        if isDebug || !collectsAnalytics { Analytics.setAnalyticsCollectionEnabled(false) }
        BlitzSplitApple.shared.startCrashReporting(isDebug: isDebug)
    }

    /// Once the shared code started: the events it tracks reach Firebase from then on.
    static func attachAnalytics() {
        guard isConfigured, collectsAnalytics, !isDebug else { return }
        BlitzSplitApple.shared.attachAnalytics(service: FirebaseAnalyticsService())
    }
}

/// The shared code's events, as Firebase Analytics takes them: Kotlin's Long and Double arrive as NSNumber.
nonisolated private final class FirebaseAnalyticsService: AnalyticsService {
    func logEvent(name: String, params: [String: Any]) {
        Analytics.logEvent(name, parameters: params)
    }

    func setCollectionEnabled(isEnabled: Bool) {
        Analytics.setAnalyticsCollectionEnabled(isEnabled)
    }

    func setUserId(userId: String?) {
        Analytics.setUserID(userId)
    }
}
