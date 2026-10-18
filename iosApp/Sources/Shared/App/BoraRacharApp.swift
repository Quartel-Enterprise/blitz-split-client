import Shared
import SwiftUI

@main
struct BoraRacharApp: App {
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
        }
        #if os(macOS)
        .defaultSize(width: 960, height: 680)
        .windowResizability(.contentMinSize)
        #endif
    }
}
