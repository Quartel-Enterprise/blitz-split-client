import Shared
import SwiftUI

/// The profile's avatar in a home tab's toolbar (Home, Groups): opens the profile.
struct ProfileToolbarButton: View {
    let initial: String
    let color: AvatarColor
    let photoUrl: String?
    let action: () -> Void

    var body: some View {
        // A name always has an initial, so an empty one is a profile not read yet: a shimmer, never an empty badge
        // in the default color
        if initial.isEmpty, photoUrl == nil {
            ShimmerCircle(size: 32).shimmering()
        } else {
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
        .prominentLabel()
        #else
        .buttonStyle(.borderedProminent)
        .prominentLabel()
        #endif
    }
}

#if os(iOS)
/// A home tab's title (Home, Groups, Balances, Contacts, Activity) shows only in a wide window, an iPad's room, as on
/// every platform (Pierre's call): there a large title under the floating tab bar names it, since iPadOS hides an
/// inline one beside the tabs. On an iPhone, where the selected tab already names the screen, the bar shows no title
/// but keeps the header VoiceOver reads. The navigation title still names the screen in the back stack.
struct TabTitle: ToolbarContent {
    let title: String
    @Environment(\.windowLayout) private var window
    @Environment(\.horizontalSizeClass) private var sizeClass

    var body: some ToolbarContent {
        if !isWideWindow(window, sizeClass) {
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
}

extension View {
    /// A home tab's title mode, with `TabTitle`: large in an iPad's room, inline (and hidden) on an iPhone.
    func tabTitleDisplayMode() -> some View {
        modifier(TabTitleDisplayMode())
    }
}

private struct TabTitleDisplayMode: ViewModifier {
    @Environment(\.windowLayout) private var window
    @Environment(\.horizontalSizeClass) private var sizeClass

    func body(content: Content) -> some View {
        content.toolbarTitleDisplayMode(isWideWindow(window, sizeClass) ? .large : .inline)
    }
}

/// An iPad's room, by the root's measure of the window; until that first measure lands (a size of zero), the size
/// class stands in, so an iPad's tab never draws a first frame with the iPhone's title and then jumps.
private func isWideWindow(_ window: ScreenLayout, _ sizeClass: UserInterfaceSizeClass?) -> Bool {
    window.size == .zero ? sizeClass == .regular : window.isLarge
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

/// Home's bell, which opens Activity over Home now that Activity left the tabs (Pierre's call), the room the search tab
/// takes on an iPhone. Its count is drawn by hand in the accent, as the old tab's badge was tinted: the system's toolbar
/// badge is red, and Activity's unread count is news, not an error. VoiceOver reads the count with the name.
struct ActivityBellButton: View {
    let unreadCount: Int
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "bell")
                .overlay(alignment: .topTrailing) {
                    if unreadCount > 0 {
                        Text(verbatim: unreadCount > 99 ? "99+" : "\(unreadCount)")
                            .font(.caption2.weight(.semibold))
                            .monospacedDigit()
                            .foregroundStyle(Palette.onAccent)
                            .padding(.horizontal, 4)
                            .frame(minWidth: 16, minHeight: 16)
                            .background(Palette.accent, in: .capsule)
                            .fixedSize()
                            .offset(x: 8, y: -6)
                    }
                }
        }
        .accessibilityLabel(Text("Activity"))
        .accessibilityValue(unreadCount > 0 ? Text("\(unreadCount) new") : Text(verbatim: ""))
        .help(Text("Activity"))
    }
}

#if os(macOS)
/// The Mac's magnifier of Home, Balances and Contacts, which opens "Search" (5.2) as a sheet over the tab, once there
/// is a group to look in. An iPhone and an iPad have the search tab instead, which reaches the search from every tab.
struct SearchToolbarItem: ToolbarContent {
    let canSearch: Bool
    let action: () -> Void

    var body: some ToolbarContent {
        if canSearch {
            ToolbarItem(placement: .primaryAction) {
                Button("Search", systemImage: "magnifyingglass", action: action)
                    .help(Text("Search"))
            }
        }
    }
}
#endif
