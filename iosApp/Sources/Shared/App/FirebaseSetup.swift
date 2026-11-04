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

    private static var isConfigured: Bool {
        Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil
    }

    /// Before the shared code starts, so a crash while it starts is reported.
    static func start() {
        guard isConfigured else { return }
        FirebaseApp.configure()
        // Analytics keeps the setting on the device, so every launch sets it again
        Analytics.setAnalyticsCollectionEnabled(!isDebug)
        BlitzSplitApple.shared.startCrashReporting(isDebug: isDebug)
    }

    /// Once the shared code started: the events it tracks reach Firebase from then on.
    static func attachAnalytics() {
        guard isConfigured, !isDebug else { return }
        BlitzSplitApple.shared.attachAnalytics(service: FirebaseAnalyticsService())
    }
}

/// The shared code's events, as Firebase Analytics takes them: Kotlin's Long and Double arrive as NSNumber.
nonisolated private final class FirebaseAnalyticsService: AnalyticsService {
    func logEvent(name: String, params: [String: Any]) {
        Analytics.logEvent(name, parameters: params)
    }
}
