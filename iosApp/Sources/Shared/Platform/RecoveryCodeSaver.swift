import AuthenticationServices
import Foundation
import Security

/// Copies the recovery code and saves it to the Passwords app, which keeps it safer than a
/// screenshot and brings it to the person's other Apple devices.
enum RecoveryCodeSaver {
    private static let domain = "blitzsplit.com"
    /// The entry's name in the Passwords app, in the app's language like the app name
    /// ("Bora Rachar · código de recuperação" in Portuguese).
    private static var account: String {
        String(localized: "Blitz Split · recovery code")
    }
    private static var title: String {
        String(localized: "Blitz Split")
    }

    static func copy(_ code: String) {
        Pasteboard.copy(code)
    }

    /*
     * iOS saves a password for blitzsplit.com through the system, which the webcredentials associated
     * domain allows: ASCredentialDataManager from iOS 26.2, SecAddSharedWebCredential (deprecated in
     * 26.2 in its favor) on 26.0 and 26.1. macOS can't share that path: Apple marks
     * ASCredentialDataManager.save (and SwiftUI's credentialDataManager.save) unavailable on macOS, and
     * SecAddSharedWebCredential is deprecated there too, pointing at it. So macOS saves the code as an
     * iCloud Keychain internet password for the same domain, which the Passwords app shows too.
     */
    static func saveToPasswords(_ code: String) async -> Bool {
        #if os(iOS)
        if #available(iOS 26.2, *) {
            return await saveWithCredentialDataManager(code)
        }
        return await withCheckedContinuation { continuation in
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
        let attributes: [String: Any] = [
            kSecValueData as String: Data(code.utf8),
            kSecAttrLabel as String: title,
        ]
        // Updates the saved code in place, so a failed save never loses the one already there
        let status = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)
        guard status == errSecItemNotFound else { return status == errSecSuccess }
        let item = query.merging(attributes) { _, new in new }
        return SecItemAdd(item as CFDictionary, nil) == errSecSuccess
        #endif
    }

    #if os(iOS)
    @available(iOS 26.2, *)
    private static func saveWithCredentialDataManager(_ code: String) async -> Bool {
        guard let anchor = PresentationAnchor.current else { return false }
        do {
            try await ASCredentialDataManager().save(
                password: ASPasswordCredential(user: account, password: code),
                for: ASAutoFillURLScope(host: domain),
                title: title,
                anchor: anchor
            )
            return true
        } catch {
            return false
        }
    }
    #endif
}
