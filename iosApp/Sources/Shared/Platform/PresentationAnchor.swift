import AuthenticationServices

/// The window the system sheets of AuthenticationServices (Sign in with Apple, saving a password)
/// present from: the key window of the scene in the foreground (an iPad may have several), or a new
/// window of a scene while none is key. Nil on iOS while no scene is connected, since a window needs one.
enum PresentationAnchor {
    static var current: ASPresentationAnchor? {
        #if os(iOS)
        let scenes = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .sorted { $0.activationState == .foregroundActive && $1.activationState != .foregroundActive }
        return scenes.lazy.compactMap(\.keyWindow).first ?? scenes.first.map { UIWindow(windowScene: $0) }
        #else
        NSApplication.shared.keyWindow ?? ASPresentationAnchor()
        #endif
    }
}
