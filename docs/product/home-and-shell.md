# The home and the shells

The home's five tabs and the web's and the Mac's shell.

  - The home shows the five tabs of the design (Home, Groups, Balances, Contacts, Activity), every one with
    Activity's unread badge. Home (`Route.Home`) is the root and where the app opens; every other tab is one route
    right above it, taking each other's place (`Navigator.openHomeTab`). Android's rail drops its labels on a phone
    lying down and a tabletop, and stays, without "Split a bill", beside a group's Expenses, Balances, Members and
    Statistics drawn alone anywhere wider than a phone upright (a phone lying down, a tablet or a foldable upright,
    Statistics over a whole book; never on a tabletop, nor beside the home's list, which has its own), the
    tab the group was opened from selected (`homeTab`, `findHomeTabUnder`); each of its tabs, that one included, goes back
    to that tab or opens it in the place of the others (`Navigator.openHomeTab(HomeTab)`, the group's
    `OnRailTabClicked`).
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
    the toolbar held below it, into the content (Groups' Open / Settled up, which a search's results hide at
    every width, a group's Expenses / Balances / Members), and drops a column: Groups' "Latest activity" and Expenses' "Who paid" (`isNarrowWindow`, measured
    per tab by `measuringNarrowWindow()`).
