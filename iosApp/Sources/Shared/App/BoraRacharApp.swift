import Shared
import SwiftUI

@main
struct BoraRacharApp: App {
    #if os(macOS)
    private static let willResignActive = NSApplication.willResignActiveNotification
    private static let didBecomeActive = NSApplication.didBecomeActiveNotification
    #else
    private static let willResignActive = UIApplication.willResignActiveNotification
    private static let didBecomeActive = UIApplication.didBecomeActiveNotification
    #endif

    init() {
        BlitzSplitApple.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                // borarachar://auth-callback: the browser sign in (Google) coming back
                .onOpenURL { url in
                    BlitzSplitApple.shared.handleDeepLink(url: url)
                }
                // The browser sign in opens over the app
                .onReceive(NotificationCenter.default.publisher(for: Self.willResignActive)) { _ in
                    BlitzSplitApple.shared.handleAppPaused()
                }
                // After onOpenURL: a browser sign in left without finishing never comes back through it
                .onReceive(NotificationCenter.default.publisher(for: Self.didBecomeActive)) { _ in
                    BlitzSplitApple.shared.handleAppResumed()
                }
        }
        #if os(macOS)
        .defaultSize(width: 960, height: 680)
        .windowResizability(.contentMinSize)
        #endif
    }
}
