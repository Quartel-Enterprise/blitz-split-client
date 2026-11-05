# Groups

The groups list, its actions, list or grid, search, and list and detail layouts.

  - The home's Groups (5.1): each row shows your net in the group with its word ("+R$ 214,90 to get back", "−R$ 98 you owe", "R$ 0 · you're settled up" while others still owe, "no expenses"), "you get 120 and owe 218"
    when you get and owe at once, "queued · Groceries" while a change waits, and its last activity: the latest save of
    the group itself, of its bills and of its payments, not the "When" they are dated ("today", "yesterday", "3 days ago", "1 week ago" up to four weeks, then the date), counted again each minute so a home left open past midnight says "yesterday"
    (`MinuteTicks`). Open groups go by that activity; a group where nobody owes anybody once there were bills goes to
    "Settled up" (`GroupRow.isSettled`), collapsed under the open ones on Android and iOS, a tab (a segment in the Mac's
    toolbar) on the web and the Mac. With no filter, an empty Open says "No open groups" on every platform
    and an empty Settled up tab "No settled groups" on the web and the Mac (`GroupsUiState.showsNoOpenGroups`,
    `showsNoSettledGroups`); a phone hides an empty Settled up. "Overall balance" sums the nets per currency, never across
    two, the currency with the most groups first (`GroupsSummary`); "To get back" and "To pay" sum the positive and the
    negative nets, are disabled at zero and open Balances (flow 8) on Owed to you and You owe; "To pay" goes straight to
    settling up when only one person is in You owe. "Split a bill"
    (Android's FAB, iOS's +, the main button on the web and the Mac) opens New group while there's no group, and
    otherwise Split a bill asking which group first (7.1i). The sync status shows only when it asks for attention,
    "Offline · 1 change queued" counting the groups, bills and payments waiting, a deleted bill or an undone payment included
    (which also keep their group's "queued"). Pulling down syncs on Android and iOS. A new device's first download
    (5.1d) puts "Bringing your groups" in Overall balance's place, with its bar and "3 of 7 groups. You can use the app meanwhile: what has arrived shows below.", the groups that came over grey rows for up to three still coming,
    and no "Split a bill" (`GroupsUiState.firstDownload`), until the account's first full sync ends, across relaunches
    too; offline it says "No internet. Your groups show up once you're back online." in the count's place. The web draws the
    same, since its board has none, and the Mac says "Syncing… 3 of 7 groups" in the window's subtitle over the
    groups that came. A phone lying down (Android and
    iPhone) splits in two: the sync status and "Overall balance" (its buttons side by side under the amount) stay still on
    the left, 320 dp / 300 pt (Android's QR and profile at its top, iOS's stay in the bar), and the list
    scrolls on the right. On a Flip's cover screen (Android, `FoldLayout.isOnCoverScreen`: the whole display under
    480 dp, the same test as Home's cover) Groups with a group only draws "Overall balance" ("to get back in 4 groups"), "Latest activity" (the bill
    saved last on the device, "Ana added Airport Uber, R$ 87,60, in Florianopolis Trip", `GroupsUiState.latestBill`)
    and "Open on the phone", which says "Open your phone to continue", since no app can open the phone; no list,
    search, form nor the recovery code's prompt.
  - The home's group actions (5.3, 5.4): every row of Groups and Settled up ends in a ⋮ (⋯ on iOS, macOS and the web)
    with "Invite people", "Leave group" and "Delete group", the same as its long press (a bottom sheet with the
    group and your balance on Android, the context menu on iOS) and its right click (the Mac's and the web's table);
    the web's menu starts with "Open group". Invite people opens that group's `Route.Invite`, not as a new group. Leave and Delete open the group's own dialogs, rules and errors (`GroupExitController`), and stay on the home: "Settle up"
    opens Record payment and "See balances" the group's Balances over it. Once that Record payment closes, the
    home shows "Ana's payment recorded." with "Undo", as the group's screens do, and consumes it, so the
    group never shows it later (`GroupsUiState.recordedPayment`). Offline both show disabled with "Needs internet" (Android's sheet adds "Leaving and deleting need the internet…"); online, "Settle the R$ 98 first" (what
    you owe, else what you're owed, `GroupRow.settleBeforeLeavingMinor`) and "Only once everyone has settled up"
    (`GroupRow.hasOpenDebts`) say what blocks them before the tap, which still opens the dialog listing what is open.
    On Android their dialogs (`GroupExitAlertDialog`, the group's too) take a book's right half and a tabletop's top
    half.
  - The home's "List or grid" (5.5, 5.6): the open groups show as a list (the default) or a grid of cards, at
    Compact, Standard or Large, kept per device and never synced (`GroupsLayout` in `device_settings`, so it outlives
    a sign out; the grid keeps its size while the list is picked). Settled up and what a filter or the search finds stay
    a list. Android and iOS pick it in the "Open" header's menu (List, Grid, then "Grid size",
    disabled while the list is picked, `GroupsUiState.canPickGridSize`); the web in a toggle group and a Select next
    to the filter (on a phone the Select only shows with the grid); the Mac in toolbar segments, a Finder-like slider under the grid and View ›
    Grid size. The columns are as many as fit the list's width at each size's minimum card width, never a
    number picked (Android 104/144/220 dp, iOS 104/140/220 pt, the Mac 150/200/280 pt, the web 104/150/300 px on a
    phone and 180/240/340 px from the tablet up), which makes a phone 3, 2 and 1, Android's tablet column 4, 3 and 2,
    and the web's desktop and the Mac 5, 4 and 3. A card holds the row's content stacked (the group's shape, its name
    in two lines, the faces and last activity, in Compact only the dot and "queued", the web's last activity only at
    Large and the Mac's members by name, and the balance with its word) and the same ⋮/⋯, long press and right click
    as the row.
  - The home's "Search" (5.2) looks in the groups, people (the members, never this device's user, once per group they
    are in) and bills on the device, the settled groups included, offline too, ignoring case and accents
    (`GroupNameFilter`, folded in memory over what Room holds, since SQLite's LIKE folds neither), and marks what it
    found. It is the same search on all four tabs (Home, Groups, Balances, Contacts, Pierre's call; Activity, no tab
    since its bell, has none), and only shows once there is a group. It is an item of the navigation (Pierre's call),
    placed where each platform looks for it: iOS's search tab at the end of the tab bar on an iPhone, lying down
    too, and an iPad (in its sidebar), opening over Groups (the room Activity left, as Apple sets the search apart
    at the end of the bar); Android's last item of the bottom bar, after Contacts, under the thumb as on iOS, and
    the last of the rail too, so a phone turning or a foldable opening never moves it (selected while the search is
    in front: its search bar takes the top bar's place on Groups, where Groups leaves it for the groups and back
    closes it); the web phone's last item of the bottom navigation, and from the tablet up the first of the
    sidebar, atop the tabs, the tablet rail's item and the desktop's "Search… ⌘K" field, where a keyboard-first
    window looks for it, both opening a command dialog, as ⌘K or Ctrl+K does; the web's pages outside the tabs (a
    group's, Statistics, Profile, Activity, a contact's profile) keep the same item, so the bars never change, which
    goes back to the tab the page sits in and opens the search over it, as iOS's search tab leaves a pushed screen
    (`HomeSidebarUiEvent.OnSearchClicked`, no ⌘K there, which could close a dialog over the page), and Android's rail
    beside a group's screens drawn alone ends with it too (`Navigator.openSearchFromPage`); over Groups that is
    Groups' own search, which takes the route's place (the first result beside the list), and it closes once Groups
    leaves the back stack, so iOS's search tab never stays in front of Activity opened from a notice; and the Mac's toolbar field on
    Groups, whose results take the groups' place once something is typed, and a magnifier (⌘K's dialog's twin, a
    sheet) on the other tabs, as macOS puts search in the toolbar. Over Home, Balances and Contacts it is a route
    of its own over the tab (`Route.Search`, `SearchViewModel`, `Navigator.openSearchOver`): Android's screen with
    Groups' search bar and the bar or rail around it, Search selected, whose tabs close it (its own tab back to it,
    `SearchUiEvent.OnHomeTabClicked`), a sheet on the Mac and the web's dialog; closing goes back to the tab, and a
    result opens in its place, over the tab. "All", "Groups", "People" and "Expenses" pick the sections, on the web
    and the Mac too. Groups come in the home's order, people by group, bills the most recent first; in "Tudo" a
    settled group's bill comes as its group ("Settled up · expense “Churros”", as the design draws it), and "Expenses"
    lists it too. A group opens the group, a person the Members of the group they were found in, a bill its Bill over the group. Results are always a list, even with the grid; nothing typed says where it
    looks, nothing found says "Nothing for “pizza”". A phone lying down (Android and iPhone) puts the field and the
    scopes on the left (under a "Search" title on the iPhone, whose bar hides) and the results on the right. Closing
    forgets the text; leaving iOS's search tab also goes back
    to the groups (`GroupsUiState.search`). The web's "Filter groups" stays, as the board draws it. Where the home
    draws its list and detail, the first result opens beside the results as it is typed: a group or a bill's group
    as its Expenses, a person as the Members they were found in (a bill's detail waits for a tap), never over a result
    tapped since with the same first result.
  - The home's list and detail (5.1): an Android tablet lying down, an unfolded book (the tabletop alike) and an iPad
    lying down draw the list (with the rail on Android) and, beside it, the group tapped, its Expenses, Balances and
    Members switching in place there, its row or card highlighted (`GroupsUiState.selectedGroupId`). A group
    tapped beside another takes its place; Invite people, Leave, Delete and "See balances" work from the list with the group
    beside it, and a group left or deleted from its row closes the pane. Before a group is picked, or once back
    closed it, the pane holds the canvas' shapes, with no words (no group opens by itself). With no group at all
    (1.3), a tablet lying down and an iPad draw the empty home in one pane, the detail only once the first group
    exists, while a book and an iPhone Duo open keep the half past the fold, saying "No group open"
    (`GroupsUiState.showsEmptyState`). Only the group's tabs go in the pane, and the Statistics they open on a
    tablet lying down and an iPad lying down (decision 2: with its back arrow, the list still tappable beside it; a
    book and an iPhone Duo open draw it across the fold): anything else above the groups (the profile, a joined
    invite, Invite people on an Android tablet and an iPad) covers the whole window,
    and a dialog or sheet (Split a bill, Bill) both panes, so the list never shows while it can't take taps. In the pane the group has no back arrow and, on Android, no "Split a bill": the rail's opens it with that
    group marked. A phone, a phone lying down, anything upright, the Mac and the web keep the group over the list;
    turning the device upright leaves the group open, drawn over it.
