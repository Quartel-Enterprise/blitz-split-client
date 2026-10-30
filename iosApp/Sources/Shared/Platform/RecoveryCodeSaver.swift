import Foundation
import Security

#if os(iOS)
import UIKit
#else
import AppKit
#endif

/// Copies the recovery code and saves it to the Passwords app, which keeps it safer than a
/// screenshot and brings it to the person's other Apple devices.
enum RecoveryCodeSaver {
    private static let domain = "blitzsplit.com"
    /// The entry's name in the Passwords app, in the app's language like the app name
    /// ("Bora Rachar · código de recuperação" in Portuguese).
    private static var account: String {
        String(localized: "Blitz Split · recovery code")
    }

    static func copy(_ code: String) {
        Pasteboard.copy(code)
    }

    /*
     * iOS: a shared web credential lands in the Passwords app, tied to blitzsplit.com by the
     * webcredentials associated domain. macOS has no shared web credentials, so the code becomes an
     * iCloud Keychain internet password for the same domain, which the Passwords app shows too.
     */
    static func saveToPasswords(_ code: String) async -> Bool {
        #if os(iOS)
        await withCheckedContinuation { continuation in
            SecAddSharedWebCredential(domain as CFString, account as CFString, code as CFString) { error in
                continuation.resume(returning: error == nil)
            }
        }
        #else
        let query: [String: Any] = [
            kSecClass as String: kSecClassInternetPassword,
            kSecAttrServer as String: domain,
            kSecAttrAccount as String: account,
            kSecAttrSynchronizable as String: true,
        ]
        SecItemDelete(query as CFDictionary)
        var item = query
        item[kSecValueData as String] = Data(code.utf8)
        item[kSecAttrLabel as String] = String(localized: "Blitz Split")
        return SecItemAdd(item as CFDictionary, nil) == errSecSuccess
        #endif
    }
}
