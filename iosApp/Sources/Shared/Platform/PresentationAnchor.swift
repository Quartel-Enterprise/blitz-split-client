import AuthenticationServices

/// The window the system sheets of AuthenticationServices (Sign in with Apple, saving a password)
/// present from: the key window, or a new window of the first scene while none is key.
enum PresentationAnchor {
    static var current: ASPresentationAnchor {
        #if os(iOS)
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        return scenes.compactMap(\.keyWindow).first ?? UIWindow(windowScene: scenes[0])
        #else
        NSApplication.shared.keyWindow ?? ASPresentationAnchor()
        #endif
    }
}
