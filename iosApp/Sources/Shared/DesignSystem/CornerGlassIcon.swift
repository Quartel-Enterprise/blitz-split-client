import SwiftUI

/// The round glass button on an avatar's corner (a pencil on a profile or a group, a camera on a new group): 32 pt
/// to look at, 44 pt to tap. Wrap it in a Button or a Menu, and place it with `.overlay(alignment: .bottomTrailing)`
/// and an `.offset` that counts the `CornerGlassIcon.targetPadding` around the glass.
struct CornerGlassIcon: View {
    /// The clear ring around the 32 pt glass that brings the frame, and so the target, to 44 pt.
    static let targetPadding: CGFloat = 6

    let systemName: String

    var body: some View {
        Image(systemName: systemName)
            .font(.system(size: 14, weight: .semibold))
            .frame(width: 32, height: 32)
            .glassEffect(.regular.interactive(), in: .circle)
            // Why: a Menu (UIKit and AppKit backed) taps only inside its frame, so the target is padding, not a
            // content shape wider than the frame.
            .padding(Self.targetPadding)
            .contentShape(.circle)
    }
}
