import Foundation

#if os(iOS)
import UIKit
#endif

/// The copy names the device: Android says "device", Apple says "iPhone", "iPad" or "Mac".
enum DeviceName {
    /// A product name, the same in every language.
    static var current: String {
        #if os(iOS)
        UIDevice.current.userInterfaceIdiom == .pad ? "iPad" : "iPhone"
        #else
        "Mac"
        #endif
    }

    /// Where the person connects an account later ("Profile" on iOS, "Settings › Account" on the Mac).
    static var accountPlace: String {
        #if os(iOS)
        String(localized: "Profile")
        #else
        String(localized: "Settings › Account")
        #endif
    }

    /// Where the recovery code stays available after the sheet.
    static var recoveryPlace: String {
        #if os(iOS)
        String(localized: "Profile › Recovery")
        #else
        String(localized: "Settings › Recovery")
        #endif
    }
}
