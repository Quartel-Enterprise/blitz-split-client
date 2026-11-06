import SwiftUI

/// The round glass button on an avatar's corner (a pencil on a profile or a group, a camera on a new group): 32 pt
/// to look at, 44 pt to tap. Wrap it in a Button or a Menu, and place it with `.overlay(alignment: .bottomTrailing)`.
struct CornerGlassIcon: View {
    let systemName: String
    var iconSize: CGFloat = 14

    var body: some View {
        Image(systemName: systemName)
            .font(.system(size: iconSize, weight: .semibold))
            .frame(width: 32, height: 32)
            .glassEffect(.regular.interactive(), in: .circle)
            // Why: the look stays 32 pt, the target reaches the 44 pt minimum around it.
            .contentShape(Circle().inset(by: -6))
    }
}
