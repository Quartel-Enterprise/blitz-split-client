import Foundation

/// What the iPhone, the Mac and the Apple Watch apps hand the shared code with their APNs token: the token as hex, and
/// whether it is the sandbox's.
enum ApnsToken {
    static func hex(_ deviceToken: Data) -> String {
        deviceToken.map { String(format: "%02x", $0) }.joined()
    }

    /// The APNs environment the build was signed for, as its provisioning profile says, since a Release build signed
    /// for development gets a sandbox token. A build with no profile (from the App Store, or the simulator) goes by
    /// its configuration: production for a release, the sandbox for a debug build.
    static var isSandbox: Bool {
        #if os(macOS)
        let profileURL: URL? = Bundle.main.bundleURL.appendingPathComponent("Contents/embedded.provisionprofile")
        let key = "com.apple.developer.aps-environment"
        #else
        let profileURL = Bundle.main.url(forResource: "embedded", withExtension: "mobileprovision")
        let key = "aps-environment"
        #endif
        if let environment = profileURL.flatMap({ apsEnvironment(inProfileAt: $0, key: key) }) {
            return environment == "development"
        }
        #if DEBUG
        return true
        #else
        return false
        #endif
    }

    /// The profile is a signed envelope around a plist, read here without checking the signature.
    private static func apsEnvironment(inProfileAt url: URL, key: String) -> String? {
        guard let data = try? Data(contentsOf: url),
              let start = data.range(of: Data("<?xml".utf8)),
              let end = data.range(of: Data("</plist>".utf8), in: start.lowerBound..<data.endIndex),
              let plist = try? PropertyListSerialization.propertyList(
                  from: data.subdata(in: start.lowerBound..<end.upperBound),
                  format: nil
              ) as? [String: Any],
              let entitlements = plist["Entitlements"] as? [String: Any] else { return nil }
        return entitlements[key] as? String
    }
}
