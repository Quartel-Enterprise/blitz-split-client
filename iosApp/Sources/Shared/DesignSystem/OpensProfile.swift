import SwiftUI

/// A person's face, name or row that opens their profile (beta feedback 18): the one Contacts opens, or Profile (the
/// Mac's Settings) for this device's user. While `action` is nil (someone no longer in the group) it stays plain
/// content, never a disabled button.
private struct OpensProfile: ViewModifier {
    let action: (() -> Void)?

    func body(content: Content) -> some View {
        if let action {
            Button(action: action) {
                content
                    .contentShape(.rect)
            }
            .buttonStyle(.plain)
            .accessibilityHint(Text("Opens the profile", comment: "What tapping a person's face or name does"))
        } else {
            content
        }
    }
}

extension View {
    func opensProfile(_ action: (() -> Void)?) -> some View {
        modifier(OpensProfile(action: action))
    }
}
