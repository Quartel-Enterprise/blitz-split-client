# The home and the shells

The home's four tabs and the web's and the Mac's shell.

  - The home shows four tabs (Home, Groups, Balances, Contacts); Activity left them for a bell in Home's bar with its
    unread count (Pierre's call, see [activity-and-push.md](activity-and-push.md)), which leaves iOS's search tab room
    at the end of the bar on an iPhone too. Search is an item of the navigation on iOS, iPadOS, macOS, Android and the web (where
    each puts it, in [groups-home.md](groups-home.md)). Home (`Route.Home`) is the root and where the app opens; every other tab is one route
    right above it, taking each other's place (`Navigator.openHomeTab`). Android's rail drops its labels on a phone
    lying down and a tabletop, where it also draws "Split a bill" as the small button so the tabs and Search fit its
    height (Android has "Split a bill" on all four tabs: the FAB on a phone upright, the rail's button anywhere wider), and its items scroll should they still not (Search, the last, is never cut off); it stays, without "Split a bill", beside a group's Expenses, Balances, Members and
    Statistics drawn alone anywhere wider than a phone upright (a phone lying down, a tablet or a foldable upright,
    Statistics over a whole book; never on a tabletop, nor beside the home's list, which has its own), the
    tab the group was opened from selected (`homeTab`, `findHomeTabUnder`); each of its tabs, that one included, goes back
    to that tab or opens it in the place of the others (`Navigator.openHomeTab(HomeTab)`, the group's
    `OnRailTabClicked`).
  - Going from a tab to another never redraws the whole screen. Android draws the bottom bar or the rail once, around
    the navigation (`HomeBarsLayout`), and each screen that has them (the tabs, Search, a group's screens and
    Statistics wherever the rail shows beside them) only leaves their place free and says what they show
    (`HomeBarsScaffold`): switching tabs, a rail click from a group and a group opened from a tab change only the content,
    while the selected tab moves within the one bar. Between two tabs (Search included) the tab coming in fades in over
    the one leaving in 180 ms (the web's `page-enter`), the one leaving fading out over it when the switch goes back down
    the stack or follows the back gesture, so the content never shows half transparent (`createHomeTabTransitionSpec`);
    a group and every other screen keep Navigation 3's default fade. A screen without the bars (the
    profile, a group on a phone upright) covers them as it comes in, and they go once no screen with them is left.
    Each layout keeps its bars: the bottom bar on a phone upright, the rail without labels on a phone lying down and a
    tabletop, the rail beside Groups' list and detail on a tablet and an unfolded foldable, and no rail beside a group
    in the pane by the list nor on a tabletop's group. Apple keeps one `TabView` (the iPad's and Mac's sidebar) above
    the tabs, and the web one frame (`HomeFrame`), so their bars never redraw either.
  - The bars make room for the content on a phone upright, on Android and the web (Pierre's call, beta feedback): a
    scroll down slides the top bar up and the bottom bar down, animated, and a scroll up brings both back from
    anywhere in the list, as Material's enter-always top bar does. On Android the top bar follows Material
    (`TopAppBarDefaults.enterAlwaysScrollBehavior`) and the home's bottom bar moves with it, by the same fraction
    (`rememberCollapsingTopBarScrollBehavior`, `HomeBars.collapsedFraction`), on Home, Groups (not while searching),
    Balances and Contacts; the screen's bottom padding shrinks with the bar, so "Split a bill" and a snackbar ride on
    it with no gap and never sit behind it. A group's two-line bar folds to its one line on a scroll down and opens
    again on a scroll up (Material's two-line bars never leave whole, which keeps back and the group's actions in
    reach). The web's phone (under 768 px) does the same on the home tabs with the frame's top bar and bottom
    navigation (`usePhoneBarsHidden`), after 8 px of travel each way, sliding in 200 ms, or at once under reduced
    motion; Activity and Profile keep the navigation under their own bar, as Android's screens over the tabs do. The
    bars only start leaving over content that scrolls, and come back near the top, on every tab switch and every
    screen opened or gone back to, and on the web for a bar that takes the keyboard's focus and for a page that no
    longer scrolls. On Android, once gone, any drag down brings them back, even over content that stopped scrolling: a
    list that fits only because the bars left would otherwise bounce them back in. Anywhere wider (a rail, a phone
    lying down, a tablet, an unfolded foldable or a tabletop, the web from the tablet up) the bars stay put.
    iPhone upright follows Apple's guideline instead of hiding: it minimizes. The main `TabView` has
    `.tabBarMinimizeBehavior(.onScrollDown)`, so the tab bar shrinks to a compact floating control on a scroll down, and
    iOS opens it again only at the top of the list or on a tap, not on a scroll up as Android's bars come back (iOS
    27.2 keeps it minimized on a scroll up even in Apple's plain sample, though its documentation says otherwise;
    Pierre kept Apple's behavior over forcing the bar open, which left it not minimizing again reliably). Balances,
    Contacts and Activity add `.toolbarMinimizationBehavior(.onScrollDown, for: .navigationBar)` (iOS 27; on iOS 26
    the bar keeps its standard behavior), and that bar does come back on a scroll up. Home's and Groups' navigation
    bars stay whole, since "Split a bill", the sync status and the profile live there and iOS has no floating button;
    so does a group's screen (its bottom bar is already replaced, and its name only shows in the bar once the header
    scrolls away), Search (the field lives in the bar), the profile and every form or short screen. The app has no
    bottom accessory to move. Lying down, on an iPad and on the Mac the bars stay put, as everywhere else (Apple
    supports minimizing on iPhone only), and an iPhone Duo's vertical bar is never minimized.
  - What a tab keeps once shown. Apple's tabs keep their content: back on Balances or Contacts, the screen is already
    read, never blank nor shimmering again, and keeps the person picked beside the list and the rows opened
    (`MainTabView`). Android keeps Groups, Balances and Contacts the same way (their list, its scroll, the person picked,
    `rememberAppNavEntries`): Balances opened afresh at every switch used to flash its shimmer under the tab's fade (the
    beta's flicker between Groups and Balances). Balances still comes back on the filter it was opened with, "All" from its tab, on every
    platform, so a shortcut always lands on the filter it names (`GlobalBalancesViewModel`); one opened with another
    filter (Home's totals, Contacts' "You owe" and "Owe you") reads its people afresh, as its ViewModel is the
    route's. The web opens each tab afresh, its picks and opened rows included.
  - The web's and the Mac's shell (slice 13): on the web the group's Expenses, Balances and Members, Statistics and
    Profile draw inside the home's frame (`PageShell`, `HomeSidebarViewModel`): the sidebar on the desktop, the icon rail
    on the tablet, the tab the group was opened from selected (Profile selects the profile at the sidebar's foot
    instead). A phone keeps the group's and Statistics' own bars (back, name, Statistics, Invite, ⋯, "Split a
    bill" at the bottom) and Profile's "Back" bar over the bottom navigation. From the tablet up the group has no
    top bar: "Groups › Florianopolis Trip" in the page (its first crumb names the tab it was opened from and opens it) and
    Statistics, Invite, the main button and ⋯ in the header's row. "Open" (the open groups, as Groups draws
    them: initials, name and the net without the symbol, "+214,90") shows only where the boards draw it: the web
    desktop's sidebar beside a group's pages and Statistics (never on the home's tabs, nor on the tablet's rail),
    and the Mac's sidebar, under its tabs, while Groups or a group's screen is in front, the group in front marked
    (the tab it sits in stays selected). A group
    picked there opens over Groups as its row does, in the place of what Groups opened (the one already open goes
    back to it, keeping its tab, and the one in front stays as it is). A Mac window under 800 pt (the boards' narrow window is 768 pt) moves the segments
    the toolbar held below it, into the content (Groups' Open / Settled up, a group's Expenses / Balances / Members),
    and drops a column: Groups' "Latest activity" and Expenses' "Who paid" (`isNarrowWindow`, measured
    per tab by `measuringNarrowWindow()`).
  - The Mac's window and its navigation (Pierre's beta feedback). The sidebar's button sits at the leading edge of the
    detail's toolbar, right of the sidebar's divider, in the main window and in Settings alike, never in the sidebar's
    header (`SidebarToggleButton`; the system's own is hidden, `hidingSystemSidebarToggle()`); View › Hide Sidebar
    still toggles it. The sidebar lists Search first, then Home, Groups, Balances and Contacts, ⌘K opening Search (or, open, back to
    its results), as the mouse's buttons and the Go menu never while a sheet covers the window, which they would close. The
    mouse's back and forward buttons go back and forward, as in the Finder and Safari: back closes the screen on top
    as its back button does (never under a sheet), forward opens again what back closed, and any other move (a tap, a
    tab, a sheet) forgets what forward had, as a browser's history; Go › Back (⌘[) and Forward (⌘]) do the same from
    the keyboard (`ForwardHistory`). A group's row in Groups' table, a group in the sidebar's "Open"
    and whatever else a tab opens draws over that tab (`SyncedNavigationStack`, which the Mac's `TabView` needs to
    push a tab's path).
  - A home tab (Home, Groups, Balances, Contacts, Activity) shows its title only on a wide screen, on every platform
    (Pierre's call): the web from the tablet up (768 px, atop the content), an Android tablet or unfolded foldable
    (`HomeTabTitle`, 600 dp on the short side; a phone lying down stays untitled), an iPad's room (a large title under the
    floating tab bar, which hides an inline one: `TabTitle`, `tabTitleDisplayMode()`, `ScreenLayout.isLarge`) and the
    Mac's toolbar. On a phone the selected tab already names the screen, which keeps
    its title for screen readers only.
  - On the web every page in the home's frame (the five tabs, a group's pages, Statistics, Profile) centers its
    content at the same width, 1152 px at most (`pageContentClass`; Balances, Contacts and Activity keep their one
    column at 768 px under 1024 px). A new page (a tab, a group, Profile,
    the welcome's steps) comes in with a short fade and rise (180 ms, `page-enter`) under the frame, which stays put;
    a group's own tabs still change with no transition, and reduced motion shows the page at once. The frame (the
    sidebar, the tablet's rail, the phone's top bar and bottom navigation) is mounted once for every page in it
    (`HomeFrame`): going to another tab, a group, Activity or Profile swaps only the content, never redraws the frame,
    and who is signed in and Search never blink while the new page reads its own state (they come from the sidebar's
    `HomeSidebarViewModel` meanwhile).
  - The web's logo, atop the sidebar, the tablet's rail and the phone's top bar, opens Home from every tab and every
    page in the home's frame, as Home's tab does (on Home itself it changes nothing); a tap on it counts as
    `brand_clicked` where the screen's ViewModel tracks its events.
  - The Mac has one window (the Settings window aside): every window would draw the one shared back stack, so the File
    menu has no New Window and windows don't tab (no View › Show Tab Bar, whose "+" opened another; a closed window
    comes back from the Dock). The File menu holds New Window's place with Split a Bill (⌘N) and New Group (⇧⌘N), one
    command each that runs the toolbar's button of the screen in front (`NewItemCommands`): "Split a bill" on Home,
    Groups (asking the group first) and a group's screens (that group, only while its screen is the one in front), "New
    group" on Groups; each is disabled where the screen in front has no such button (Balances, Contacts, Activity,
    Search) and under a sheet. The toolbar's buttons keep their tooltips with the shortcut. View › Refresh (⌘R) syncs
    Activity while it is open, as pulling its feed down does on iPhone and iPad. Settings' sidebar is 250 pt, room for
    "Keys to receive payments" whole, and its form stops at 720 pt, centred, in a wider window (full screen).
  - On the Mac, a link the app is opened with (the browser sign in coming back through `borarachar://auth-callback`, an
    invite link) goes to the main window already open, which updates in place, never to a second window opened on top
    of it (beta feedback 12, `handlesExternalEvents` on the main window's root); with none open, the app opens one.
