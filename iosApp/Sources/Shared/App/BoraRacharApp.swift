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
        #if os(macOS)
        // One window: a tab's "+" (View › Show Tab Bar) would open a second one drawing the same shared back stack,
        // as File › New Window did (its `.newItem` group below is empty)
        NSWindow.allowsAutomaticWindowTabbing = false
        #endif
        FirebaseSetup.start()
        #if DEBUG
        if let run = StoreScreenshotsRun.current {
            StoreScreenshotsRun.saveLanguages()
            BlitzSplitApple.shared.startForStoreScreenshots(
                language: run.language,
                isDark: run.isDark,
                assetsDirectory: run.assetsDirectory,
                screenCode: run.screen,
            )
        } else {
            StoreScreenshotsRun.restoreLanguagesIfAsked()
            BlitzSplitApple.shared.start()
        }
        #else
        BlitzSplitApple.shared.start()
        #endif
        FirebaseSetup.attachAnalytics()
        _app = State(initialValue: AppModel())
    }

    var body: some Scene {
        WindowGroup {
            RootView(app: app)
                #if DEBUG && os(macOS)
                .onAppear { StoreScreenshotsRun.sizeWindow() }
                #endif
                .appLocale(app.mainWindowLanguage)
                // Drawn again in a language picked, as Android opens its activity again: titles and the strings
                // views read once take it too, and the shared back stack puts back where the person was
                .id(app.mainWindowLanguage)
                // borarachar://auth-callback: the browser sign in (Google) coming back
                .onOpenURL { url in
                    BlitzSplitApple.shared.handleDeepLink(url: url)
                }
                #if os(macOS)
                // A URL the app is opened with (the browser sign in coming back, an invite link) goes to the window
                // already open, never a new one on top of it (beta feedback 12); with none open, the group opens one
                .handlesExternalEvents(preferring: ["*"], allowing: ["*"])
                #endif
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
            // One window: a second one would draw the same shared back stack and move with it, so File › New Window
            // goes, and ⌘N is "Split a bill", the main button of Home, Groups and a group's screens, ⇧⌘N Groups'
            // "New group"
            NewItemCommands(language: app.language)
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
