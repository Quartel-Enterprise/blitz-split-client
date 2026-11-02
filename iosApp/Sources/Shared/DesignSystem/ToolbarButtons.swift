import Shared
import SwiftUI

/// The profile's avatar in a home tab's toolbar (Home, Groups): opens the profile.
struct ProfileToolbarButton: View {
    let initial: String
    let color: AvatarColor
    let photoUrl: String?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            AvatarPhoto(
                initial: initial,
                color: color,
                photoUrl: photoUrl,
                size: 32,
                font: .subheadline.weight(.semibold),
            )
        }
        .accessibilityLabel("Profile")
    }
}

/// "Split a bill" spelled out with its plus, a toolbar draws a Label as its icon only: iOS's tinted glass item (iOS
/// has no FAB), the Mac window's prominent button.
struct SplitBillButton: View {
    let action: () -> Void

    var body: some View {
        ProminentBarButton(title: "Split a bill", systemImage: "plus", action: action)
    }
}

/// Members' "Add person", in the place the group's other tabs give "Split a bill".
struct AddPersonButton: View {
    let action: () -> Void

    var body: some View {
        ProminentBarButton(title: "Add person", systemImage: "person.badge.plus", action: action)
    }
}

/// A toolbar's main button spelled out with its icon.
private struct ProminentBarButton: View {
    let title: LocalizedStringKey
    let systemImage: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: systemImage)
                Text(title)
            }
            .fontWeight(.semibold)
        }
        #if os(iOS)
        .buttonStyle(.glassProminent)
        #else
        .buttonStyle(.borderedProminent)
        #endif
    }
}

#if os(iOS)
/// The bar's title on iPhone and iPad, where the selected tab already names the screen: no visible title, but the
/// header VoiceOver reads. The navigation title still names the screen in the back stack.
struct HiddenTabTitle: ToolbarContent {
    let title: String

    var body: some ToolbarContent {
        ToolbarItem(placement: .principal) {
            Color.clear
                .frame(width: 1, height: 1)
                .accessibilityElement()
                .accessibilityLabel(Text(verbatim: title))
                .accessibilityAddTraits(.isHeader)
        }
        .sharedBackgroundVisibility(.hidden)
    }
}
#endif
