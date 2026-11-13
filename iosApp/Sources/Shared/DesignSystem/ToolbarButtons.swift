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
/// button. In an iPhone Duo's vertical bar, which has no room for words, the icon alone (`showsTitle`).
struct SplitBillButton: View {
    var showsTitle = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            if showsTitle {
                HStack(spacing: 6) {
                    Image(decorative: "SplitBill")
                    Text("Split a bill")
                }
                .fontWeight(.semibold)
            } else {
                Label { Text("Split a bill") } icon: { Image(decorative: "SplitBill") }
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
/// A home tab's trailing items (Home, Groups): its two buttons grouped in glass, then "Split a bill" as the bar's
/// tinted glass item on its own, never a FAB floating over the tab bar. Both go into an iPhone Duo's vertical bar.
struct TabTrailingItems<Buttons: View>: ToolbarContent {
    let showsSplitBill: Bool
    /// Home spells it out where its bar has room (lying down, an iPad); Groups draws its icon alone everywhere.
    let showsSplitBillTitle: Bool
    let onSplitBill: () -> Void
    let buttons: Buttons

    init(
        showsSplitBill: Bool,
        showsSplitBillTitle: Bool,
        onSplitBill: @escaping () -> Void,
        @ViewBuilder buttons: () -> Buttons,
    ) {
        self.showsSplitBill = showsSplitBill
        self.showsSplitBillTitle = showsSplitBillTitle
        self.onSplitBill = onSplitBill
        self.buttons = buttons()
    }

    var body: some ToolbarContent {
        ToolbarItemGroup(placement: .primaryAction) { buttons }
            .prefersVerticalBar()
        if showsSplitBill {
            ToolbarSpacer(.fixed, placement: .primaryAction)
            ToolbarItem(placement: .primaryAction) {
                SplitBillButton(showsTitle: showsSplitBillTitle, action: onSplitBill)
            }
            .prefersVerticalBar()
        }
    }
}

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

extension View {
    /// On an iPhone upright, the tab bar shrinks to a compact floating control on a scroll down, as Apple's guideline
    /// has it. iOS opens it again at the top of the list or on a tap, not on a scroll up (iOS 27.2, although the
    /// documentation says otherwise); Pierre kept Apple's behavior over forcing it open. Never on an iPad nor on a
    /// phone lying down, whose bars stay put.
    func phoneMinimizesTabBar() -> some View {
        modifier(PhoneBarMinimization(bar: .tabBar))
    }

    /// The same for the navigation bar of a long list that holds no main action (iOS 27; before it the bar keeps its
    /// standard behavior): Balances, Contacts and Activity. Home and Groups keep theirs, since "Split a bill" lives
    /// there, and so does a group's screen, whose inline title only shows once the header scrolls away.
    func phoneMinimizesNavigationBar() -> some View {
        modifier(PhoneBarMinimization(bar: .navigationBar))
    }
}

private struct PhoneBarMinimization: ViewModifier {
    enum Bar {
        case tabBar
        case navigationBar
    }

    let bar: Bar
    @Environment(\.verticalSizeClass) private var verticalSizeClass
    @Environment(\.windowLayout) private var window

    /// An iPhone upright in an iPhone's room: the idiom (Apple supports minimizing on iPhone only, so an iPad in a
    /// narrow Split View or Slide Over keeps its bars), short of an iPad's room by the window's size (an iPhone Duo
    /// open keeps its bars, as an iPad does) and a regular height, as an Android phone and the web keep their bars
    /// lying down. Not before the root's first measure.
    private var isPhoneUpright: Bool {
        UIDevice.current.userInterfaceIdiom == .phone
            && window.size != .zero
            && !window.isLarge
            && verticalSizeClass != .compact
    }

    @ViewBuilder
    func body(content: Content) -> some View {
        switch bar {
        case .tabBar:
            content.tabBarMinimizeBehavior(isPhoneUpright ? .onScrollDown : .automatic)
        case .navigationBar:
            if #available(iOS 27, *) {
                content.toolbarMinimizationBehavior(isPhoneUpright ? .onScrollDown : .automatic, for: .navigationBar)
            } else {
                content
            }
        }
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
    /// camera's edge (iOS 27.1's `toolbarVerticalEdge`), which takes a screen's main button as its icon alone, out of
    /// the bottom toolbar inside a group; false anywhere else.
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
