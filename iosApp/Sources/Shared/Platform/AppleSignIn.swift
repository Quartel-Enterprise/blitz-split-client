import AuthenticationServices
import CryptoKit
import Foundation

/// The result of the native Sign in with Apple sheet, mapped to the shared ViewModels' events.
enum AppleSignInResult {
    /// `fullName` comes only on the first sign in of this Apple ID in the app, never in the token.
    /// `authorizationCode` is what the server trades for the refresh token it revokes when the account
    /// is deleted.
    case success(idToken: String, rawNonce: String, fullName: String?, authorizationCode: String?)
    case cancelled
    case failed
}

/// Runs Sign in with Apple and hands the ID token to the shared ViewModel. Apple signs the SHA-256
/// of the nonce into the token and Supabase checks it against the raw nonce, so a stolen token
/// can't be replayed.
final class AppleSignIn: NSObject {
    private var continuation: CheckedContinuation<AppleSignInResult, Never>?
    private var rawNonce = ""

    func signIn() async -> AppleSignInResult {
        rawNonce = Self.createRawNonce()
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = Self.sha256Hex(of: rawNonce)
        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        return await withCheckedContinuation { continuation in
            self.continuation = continuation
            controller.performRequests()
        }
    }

    private func finish(with result: AppleSignInResult) {
        continuation?.resume(returning: result)
        continuation = nil
    }

    private static func createRawNonce() -> String {
        UUID().uuidString + UUID().uuidString
    }

    private static func sha256Hex(of value: String) -> String {
        SHA256.hash(data: Data(value.utf8)).map { String(format: "%02x", $0) }.joined()
    }
}

extension AppleSignIn: ASAuthorizationControllerDelegate {
    nonisolated func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization,
    ) {
        let credential = authorization.credential as? ASAuthorizationAppleIDCredential
        let idToken = credential?.identityToken.flatMap { String(data: $0, encoding: .utf8) }
        let fullName = credential?.fullName
            .map { PersonNameComponentsFormatter.localizedString(from: $0, style: .default) }
            .flatMap { $0.isEmpty ? nil : $0 }
        let authorizationCode = credential?.authorizationCode.flatMap { String(data: $0, encoding: .utf8) }
        MainActor.assumeIsolated {
            guard let idToken else {
                finish(with: .failed)
                return
            }
            finish(with: .success(
                idToken: idToken,
                rawNonce: rawNonce,
                fullName: fullName,
                authorizationCode: authorizationCode,
            ))
        }
    }

    nonisolated func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        let isCancelled = (error as? ASAuthorizationError)?.code == .canceled
        MainActor.assumeIsolated {
            finish(with: isCancelled ? .cancelled : .failed)
        }
    }
}

extension AppleSignIn: ASAuthorizationControllerPresentationContextProviding {
    nonisolated func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        MainActor.assumeIsolated {
            #if os(iOS)
            let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
            return scenes.compactMap(\.keyWindow).first ?? UIWindow(windowScene: scenes[0])
            #else
            NSApplication.shared.keyWindow ?? ASPresentationAnchor()
            #endif
        }
    }
}
