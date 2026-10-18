#if os(iOS)
import UIKit
#endif

/// The copy names the device: Android says "aparelho", Apple says "iPhone", "iPad" or "Mac".
enum DeviceName {
    static var current: String {
        #if os(iOS)
        UIDevice.current.userInterfaceIdiom == .pad ? "iPad" : "iPhone"
        #else
        "Mac"
        #endif
    }

    /// Where the person connects an account later ("Perfil" on iOS, "Ajustes › Conta" on the Mac).
    static var accountPlace: String {
        #if os(iOS)
        "Perfil"
        #else
        "Ajustes › Conta"
        #endif
    }

    /// Where the recovery code stays available after the sheet.
    static var recoveryPlace: String {
        #if os(iOS)
        "Perfil › Recuperação"
        #else
        "Ajustes › Recuperação"
        #endif
    }
}
