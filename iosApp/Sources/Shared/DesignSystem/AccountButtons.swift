import AuthenticationServices
import SwiftUI

/// The native Sign in with Apple button (ASAuthorizationAppleIDButton). It only reports the tap:
/// the shared ViewModel decides, and asks for the native sheet through its UiAction.
struct AppleIDButton: View {
    enum Label {
        case continueWith
        case short
    }

    let label: Label
    let isEnabled: Bool
    let action: () -> Void

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        AppleIDButtonRepresentable(
            type: label == .continueWith ? .continue : .signIn,
            style: colorScheme == .dark ? .white : .black,
            action: action,
        )
        .id(colorScheme)
        .frame(height: 50)
        .clipShape(.capsule)
        .opacity(isEnabled ? 1 : 0.4)
        .allowsHitTesting(isEnabled)
        .accessibilityAddTraits(.isButton)
    }
}

/// What the Apple button calls on a tap: UIKit and AppKit both send an @objc selector to a target.
private final class AppleIDButtonTarget: NSObject {
    var action: () -> Void

    init(action: @escaping () -> Void) {
        self.action = action
    }

    @objc func tapped() {
        action()
    }
}

private extension ASAuthorizationAppleIDButton {
    static func rounded(type: ButtonType, style: Style) -> ASAuthorizationAppleIDButton {
        let button = ASAuthorizationAppleIDButton(authorizationButtonType: type, authorizationButtonStyle: style)
        button.cornerRadius = 25
        return button
    }
}

#if os(iOS)
private struct AppleIDButtonRepresentable: UIViewRepresentable {
    let type: ASAuthorizationAppleIDButton.ButtonType
    let style: ASAuthorizationAppleIDButton.Style
    let action: () -> Void

    func makeUIView(context: Context) -> ASAuthorizationAppleIDButton {
        let button = ASAuthorizationAppleIDButton.rounded(type: type, style: style)
        button.addTarget(context.coordinator, action: #selector(AppleIDButtonTarget.tapped), for: .touchUpInside)
        return button
    }

    func updateUIView(_ button: ASAuthorizationAppleIDButton, context: Context) {
        context.coordinator.action = action
    }

    // Fills the frame SwiftUI gives it instead of the button's own intrinsic size
    func sizeThatFits(_ proposal: ProposedViewSize, uiView: ASAuthorizationAppleIDButton, context: Context) -> CGSize? {
        proposal.replacingUnspecifiedDimensions(by: CGSize(width: 280, height: 50))
    }

    func makeCoordinator() -> AppleIDButtonTarget {
        AppleIDButtonTarget(action: action)
    }
}
#else
private struct AppleIDButtonRepresentable: NSViewRepresentable {
    let type: ASAuthorizationAppleIDButton.ButtonType
    let style: ASAuthorizationAppleIDButton.Style
    let action: () -> Void

    func makeNSView(context: Context) -> ASAuthorizationAppleIDButton {
        let button = ASAuthorizationAppleIDButton.rounded(type: type, style: style)
        button.target = context.coordinator
        button.action = #selector(AppleIDButtonTarget.tapped)
        return button
    }

    func updateNSView(_ button: ASAuthorizationAppleIDButton, context: Context) {
        context.coordinator.action = action
    }

    // Fills the frame SwiftUI gives it instead of the button's own intrinsic size
    func sizeThatFits(_ proposal: ProposedViewSize, nsView: ASAuthorizationAppleIDButton, context: Context) -> CGSize? {
        proposal.replacingUnspecifiedDimensions(by: CGSize(width: 280, height: 50))
    }

    func makeCoordinator() -> AppleIDButtonTarget {
        AppleIDButtonTarget(action: action)
    }
}
#endif

/// Google signs in through the browser on Apple platforms, so its button is an ordinary one.
struct GoogleButton: View {
    let title: LocalizedStringKey
    let isEnabled: Bool
    let isLoading: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if isLoading {
                    ProgressView().controlSize(.small)
                } else {
                    Text(verbatim: "G").fontWeight(.heavy)
                }
                Text(title)
            }
            .font(.headline)
            .foregroundStyle(.primary)
            .frame(maxWidth: .infinity, minHeight: 50)
            .background(Palette.card, in: .capsule)
            .overlay(Capsule().strokeBorder(.separator, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .disabled(!isEnabled)
        .opacity(isEnabled ? 1 : 0.4)
    }
}
