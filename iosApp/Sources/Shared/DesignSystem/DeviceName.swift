import Foundation

#if os(iOS)
import UIKit
#endif

/// The copy names the device: Android says "device", Apple says "iPhone", "iPad" or "Mac". The one idiom check the
/// layouts keep, since it names the product, not its room: an iPhone Duo open says "iPhone", as the notes ask.
enum DeviceName {
    /// A product name, the same in every language.
    static var current: String {
        #if os(iOS)
        UIDevice.current.userInterfaceIdiom == .pad ? "iPad" : "iPhone"
        #else
        "Mac"
        #endif
    }

    /// Where the person connects an account later: the profile, pushed over the groups everywhere.
    static var accountPlace: String {
        String(localized: "Profile")
    }

    /// Where the recovery code stays available after the sheet.
    static var recoveryPlace: String {
        String(localized: "Profile › Recovery")
    }
}
