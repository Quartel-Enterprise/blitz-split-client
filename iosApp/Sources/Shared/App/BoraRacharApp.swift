import Shared
import SwiftUI

@main
struct BoraRacharApp: App {
    #if os(macOS)
    @NSApplicationDelegateAdaptor private var appDelegate: AppDelegate
    #else
    @UIApplicationDelegateAdaptor private var appDelegate: AppDelegate
    #endif

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
    /// Every scene's phase at once: real time listens to the other devices' changes only while some window isn't in
    /// the background (inactive still shows it: Control Center pulled down, the Mac's window behind another app's).
    @Environment(\.scenePhase) private var scenePhase

    init() {
        FirebaseSetup.start()
        BlitzSplitApple.shared.start()
        FirebaseSetup.attachAnalytics()
        _app = State(initialValue: AppModel())
    }

    var body: some Scene {
        WindowGroup {
            RootView(app: app)
                .appLocale(app.mainWindowLanguage)
                // Drawn again in a language picked, as Android opens its activity again: titles and the strings
                // views read once take it too, and the shared back stack puts back where the person was
                .id(app.mainWindowLanguage)
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
        .onChange(of: scenePhase, initial: true) { _, phase in
            BlitzSplitApple.shared.setAppForeground(isForeground: phase != .background)
        }
        #if os(macOS)
        .defaultSize(width: 960, height: 680)
        .windowResizability(.contentMinSize)
        .commands {
            GroupsCommands(language: app.language)
            ActivityCommands(language: app.language)
            SearchCommands(language: app.language)
            NavigationCommands(language: app.language)
        }
        #endif

        #if os(macOS)
        // Ajustes (⌘,): the profile, as the canvas draws it on the Mac, and Categories, the category map (9.1)
        Settings {
            SettingsView(app: app)
                .appLocale(app.language)
                .id(app.language)
        }
        #endif
    }
}
