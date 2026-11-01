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

    /// The shared back stack's view, made once the shared code started: every window draws it, and on the Mac the
    /// Settings window too.
    @State private var app: AppModel

    init() {
        BlitzSplitApple.shared.start()
        _app = State(initialValue: AppModel())
    }

    var body: some Scene {
        WindowGroup {
            RootView(app: app)
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
        .commands {
            GroupsCommands()
        }
        #endif

        #if os(macOS)
        // Ajustes (⌘,): the profile, as the canvas draws it on the Mac
        Settings {
            SettingsView(app: app)
        }
        #endif
    }
}
