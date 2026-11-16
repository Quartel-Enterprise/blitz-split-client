import Shared
import SwiftUI

/// The profile's avatar in a home tab's toolbar (Home, Groups): opens the profile.
struct ProfileToolbarButton: View {
    /// Each tab's stack has one, so one id names it in every stack.
    private static let faceId = "profile-toolbar"

    let initial: String
    let color: AvatarColor
    let photoUrl: String?
    let action: () -> Void

    /// The tab's stack, whose Profile zooms out of this avatar (beta feedback of 2026-11-15).
    @Environment(\.profileZoomScope) private var profileZoom

    var body: some View {
        // A name always has an initial, so an empty one is a profile not read yet: a shimmer, never an empty badge
        // in the default color
        if initial.isEmpty, photoUrl == nil {
            ShimmerCircle(size: 32).shimmering()
        } else {
            Button {
                profileZoom?.zoom.arm(Self.faceId)
                action()
            } label: {
                AvatarPhoto(
                    initial: initial,
                    color: color,
                    photoUrl: photoUrl,
                    size: 32,
                    font: .subheadline.weight(.semibold),
                )
                .profileZoomSource(faceId: Self.faceId)
            }
            .accessibilityLabel("Profile")
        }
    }
}

/// "Split a bill" spelled out with its receipt and plus (the legacy app's icon, `SplitBill` in the asset catalog), a
/// toolbar draws a Label as its icon only: iOS's tinted glass item (iOS has no FAB), the Mac window's prominent
/// button. On an iPhone and an iPad it shrinks to its icon alone once the list scrolls down from its top
/// (`splitBillExpandsAtTop`), and in an iPhone Duo's vertical bar, which has no room for words, it is always the icon
/// alone (`showsTitle`). Where its bar leaves it little room (`titleRoom`, the words' width at most), it is the icon
/// alone too, rather than iOS moving it into its own "⋯" overflow, out of sight.
struct SplitBillButton: View {
    var showsTitle = true
    var titleRoom: CGFloat = .infinity
    let action: () -> Void

    /// The words' width, measured in the button's own font; until that first measure, as wide as nothing fits, so a
    /// tight bar opens with the icon rather than the words jumping out of it.
    @State private var titleWidth: CGFloat = .infinity

    var body: some View {
        Button(action: action) {
            if showsTitle && titleWidth <= titleRoom {
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
        .background {
            if titleRoom.isFinite {
                Text("Split a bill")
                    .fontWeight(.semibold)
                    .fixedSize()
                    .hidden()
                    .onGeometryChange(for: CGFloat.self) { $0.size.width } action: { titleWidth = $0 }
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
/// A home tab's trailing items (Home, Groups, Balances, Contacts): its buttons grouped in glass, then "Split a bill" as
/// the bar's tinted glass item on its own, never a FAB floating over the tab bar. Both go into an iPhone Duo's
/// vertical bar.
struct TabTrailingItems<Buttons: View>: ToolbarContent {
    @Environment(\.windowLayout) private var window
    @Environment(\.horizontalSizeClass) private var sizeClass
    @Environment(\.isInTabListColumn) private var isInTabListColumn

    let showsSplitBill: Bool
    /// Spelled out while the list is at its top, the icon alone once it scrolls down and in an iPhone Duo's vertical
    /// bar.
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
        // A crowded tab bar leaves room for "Split a bill" alone: the buttons go to the row atop the content
        // (`tabButtonsRow`)
        if !isTabBarCrowded(window, sizeClass, inListColumn: isInTabListColumn) {
            ToolbarItemGroup(placement: .primaryAction) { buttons }
                .prefersVerticalBar()
        }
        if showsSplitBill {
            if !isTabBarCrowded(window, sizeClass, inListColumn: isInTabListColumn) {
                ToolbarSpacer(.fixed, placement: .primaryAction)
            }
            ToolbarItem(placement: .primaryAction) {
                SplitBillButton(showsTitle: showsSplitBillTitle, action: onSplitBill)
            }
            .prefersVerticalBar()
        }
    }
}

extension View {
    /// Keeps `isExpanded` on whether this list is at its top, which spells "Split a bill" out in the top bar: it starts
    /// expanded and shrinks to its icon as the list scrolls down, growing back at the top, as Android's extended FAB
    /// does (beta feedback of 2026-11-15). A list too short to scroll keeps it spelled out. Put on the screen's
    /// vertical list.
    func splitBillExpandsAtTop(_ isExpanded: Binding<Bool>) -> some View {
        onScrollGeometryChange(for: Bool.self) { geometry in
            let insets = geometry.contentInsets
            let scrolls = geometry.contentSize.height > geometry.containerSize.height - insets.top - insets.bottom
            return !scrolls || geometry.contentOffset.y + insets.top <= 1
        } action: { _, isAtTop in
            withAnimation(.snappy) { isExpanded.wrappedValue = isAtTop }
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
    /// standard behavior): Activity. Home, Groups, Balances and Contacts keep theirs, since "Split a bill" lives there,
    /// and so does a group's screen, whose inline title only shows once the header scrolls away.
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

/// An iPad window under 1100 pt wide with a regular width (an iPad upright, the 13" one included, Stage Manager's
/// narrower windows): its tabs
/// sit in the top bar's middle whenever the sidebar is closed (it can't stay beside the content there), which leaves the
/// bar's trailing side room for one item. "Split a bill" keeps it; the tab's other buttons go to a row atop the content
/// (`tabButtonsRow`), so iOS never hides any in its own "⋯" (Pierre's call, beta feedback of 2026-11-15). A compact
/// window has its tab bar at the bottom, as an iPhone. A tab's list column beside a detail (`inListColumn`: Groups'
/// beside a group, Contacts' beside a person) is crowded at any window width: its own bar, under the tabs, has room
/// for "Split a bill" alone too. An 11" iPad lying down (1194 pt) has room for all of them beside its tabs.
///
/// The idiom check is on purpose, as `PhoneBarMinimization`'s: an iPhone Duo open has an iPad's room but its tabs stay
/// in the system's vertical bar, never in the top bar's middle.
func isTabBarCrowded(_ window: ScreenLayout, _ sizeClass: UserInterfaceSizeClass?, inListColumn: Bool = false) -> Bool {
    guard UIDevice.current.userInterfaceIdiom == .pad, sizeClass == .regular else { return false }
    return inListColumn || (window.size != .zero && window.size.width < 1100)
}

extension View {
    /// A group's screens (the group, its Statistics, its expenses by category) keep their own bars without the app's
    /// tabs on an iPhone, and on an iPad whose tabs would crowd the top bar (`isTabBarCrowded`), where they left the
    /// group's buttons no room; not beside Groups' list, whose column keeps the tabs.
    func groupFlowHidesTabBar() -> some View {
        modifier(GroupFlowTabBar())
    }

    /// The tab's buttons (Home's bell and profile, Groups' QR and profile, the profile of Balances and Contacts) in a
    /// row of glass atop the content, at its trailing side, while a crowded tab bar leaves them no room in the top bar
    /// (`isTabBarCrowded`); nothing anywhere else.
    func tabButtonsRow(@ViewBuilder buttons: () -> some View) -> some View {
        modifier(TabButtonsRow(buttons: buttons()))
    }
}

private struct GroupFlowTabBar: ViewModifier {
    @Environment(\.windowLayout) private var window
    @Environment(\.horizontalSizeClass) private var sizeClass
    @Environment(\.showsGroupBesideList) private var showsGroupBesideList

    func body(content: Content) -> some View {
        let hides = sizeClass == .compact || (isTabBarCrowded(window, sizeClass) && !showsGroupBesideList)
        content.toolbar(hides ? .hidden : .automatic, for: .tabBar)
    }
}

private struct TabButtonsRow<Buttons: View>: ViewModifier {
    let buttons: Buttons
    @Environment(\.windowLayout) private var window
    @Environment(\.horizontalSizeClass) private var sizeClass
    @Environment(\.isInTabListColumn) private var isInTabListColumn

    func body(content: Content) -> some View {
        content.safeAreaInset(edge: .top, spacing: 0) {
            if isTabBarCrowded(window, sizeClass, inListColumn: isInTabListColumn) {
                HStack(spacing: 10) {
                    Spacer(minLength: 0)
                    buttons
                        .buttonStyle(.glass)
                        .buttonBorderShape(.circle)
                        .controlSize(.large)
                        .labelStyle(.iconOnly)
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 6)
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
