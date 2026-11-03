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

/// "Split a bill" spelled out with its receipt and plus (the legacy app's icon, `SplitBill` in the asset catalog), a
/// toolbar draws a Label as its icon only: iOS's tinted glass item (iOS has no FAB), the Mac window's prominent
/// button. In an iPhone Duo's vertical bar, the icon alone (`showsTitle`).
struct SplitBillButton: View {
    var showsTitle = true
    let action: () -> Void

    var body: some View {
        ProminentBarButton(
            title: "Split a bill",
            image: Image(decorative: "SplitBill"),
            showsTitle: showsTitle,
            action: action,
        )
    }
}

/// Members' "Add person", in the place the group's other tabs give "Split a bill".
struct AddPersonButton: View {
    var showsTitle = true
    let action: () -> Void

    var body: some View {
        ProminentBarButton(
            title: "Add person",
            image: Image(systemName: "person.badge.plus"),
            showsTitle: showsTitle,
            action: action,
        )
    }
}

/// A toolbar's main button spelled out with its icon, or its icon alone in an iPhone Duo's vertical bar, which has
/// no room for words.
private struct ProminentBarButton: View {
    let title: LocalizedStringKey
    let image: Image
    let showsTitle: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            if showsTitle {
                HStack(spacing: 6) {
                    image
                    Text(title)
                }
                .fontWeight(.semibold)
            } else {
                Label { Text(title) } icon: { image }
                    .labelStyle(.iconOnly)
                    .fontWeight(.semibold)
            }
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

extension ToolbarContent {
    /// Moves the item into the vertical glass bar an iPhone Duo draws on its camera's edge (iOS 27.1), where the
    /// boards put the home tabs' Perfil, + and QR, at the top of the bar, the tabs at its bottom. Anywhere the bar
    /// lies flat, and on the Mac, the item stays where its placement puts it.
    @ToolbarContentBuilder
    func prefersVerticalBar() -> some ToolbarContent {
        #if os(iOS)
        if #available(iOS 27.1, *) {
            axisBehavior(.verticalPreferred)
        } else {
            self
        }
        #else
        self
        #endif
    }
}

extension View {
    /// Keeps `isVertical` on whether this screen's toolbar is the vertical glass bar an iPhone Duo draws on its
    /// camera's edge (iOS 27.1's `toolbarVerticalEdge`), where a screen moves the main button it floats under the
    /// thumb into the bar; false anywhere else.
    func readingVerticalBar(_ isVertical: Binding<Bool>) -> some View {
        modifier(VerticalBarReading(isVertical: isVertical))
    }
}

private struct VerticalBarReading: ViewModifier {
    @Binding var isVertical: Bool

    func body(content: Content) -> some View {
        #if os(iOS)
        if #available(iOS 27.1, *) {
            content.modifier(VerticalEdgeReading(isVertical: $isVertical))
        } else {
            content
        }
        #else
        content
        #endif
    }
}

#if os(iOS)
@available(iOS 27.1, *)
private struct VerticalEdgeReading: ViewModifier {
    @Binding var isVertical: Bool

    @Environment(\.toolbarVerticalEdge) private var edge

    func body(content: Content) -> some View {
        content.onChange(of: edge != nil, initial: true) { _, isVerticalNow in
            isVertical = isVerticalNow
        }
    }
}
#endif

/// The magnifier of Home, Balances, Contacts and Activity, which opens "Search" (5.2) over the tab, once there is a
/// group to look in: pushed on an iPhone, a sheet on the Mac. An iPad's room has the search tab instead, which already
/// reaches the search from every tab.
struct SearchToolbarItem: ToolbarContent {
    let canSearch: Bool
    let action: () -> Void
    #if os(iOS)
    @Environment(\.hasSearchTabRoom) private var hasSearchTabRoom
    #endif

    private var isShown: Bool {
        #if os(iOS)
        canSearch && !hasSearchTabRoom
        #else
        canSearch
        #endif
    }

    var body: some ToolbarContent {
        if isShown {
            ToolbarItem(placement: .primaryAction) {
                Button("Search", systemImage: "magnifyingglass", action: action)
                    .help(Text("Search"))
            }
            .prefersVerticalBar()
        }
    }
}
