import FirebaseCore
import Shared

/// Crashlytics, only in a build with the Firebase project's GoogleService-Info.plist (git-ignored, in Resources):
/// without it the app never starts Firebase. A debug build reports nothing.
enum CrashReporting {
    static func start() {
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else { return }
        FirebaseApp.configure()
        #if DEBUG
        BlitzSplitApple.shared.startCrashReporting(isDebug: true)
        #else
        BlitzSplitApple.shared.startCrashReporting(isDebug: false)
        #endif
    }
}
