# Home

The home tab, its shortcuts and pinned groups.

  - "Home" (flow 10, `HomeViewModel` in `:feature:groups`), top to bottom: "Balances with people", a row of faces
    with each person's net across groups as Balances nets it (`PeopleBalances`), whom you owe first, then who owes you,
    the largest first in each, hidden when nobody owes anybody; a face opens settling up for the net
    (`Route.SettleUp`, Pierre's call: it stays so after beta feedback 18), whose two faces open the profiles, and a
    guest (a name nobody joined as) gets a badge and, on a long press or hover, "Diego is a guest and doesn't have the app…".
    "Overall balance" as Groups' (both are the global Balances' totals, `PeopleBalances.totals`, so the three never differ: big in the first currency with a net, a line for each other one with a net,
    `HomeSummary.main` and `others`), with "Owed to you" and "You owe", each in the first currency with something that
    way (`HomeSummary.toReceive`, `toPay`), which open Balances filtered (You owe straight to settling up while only
    one person is in it), disabled at zero. Three shortcuts: "Scan receipt", which first asks right on Home where the
    receipt's photo comes from, as the form's "Scan receipt" asks (the same sheet, dialog or drawer: the camera, the
    gallery and a file, as each platform has them; straight to the gallery or the file chooser without a camera),
    then, once a photo is picked, opens Split a bill as "Split a bill" does (asking the group first) with that photo
    already reading (beta feedback part 3, item 16: `HomeUiState.showsReceiptSourceChoice`,
    `HomeUiEvent.OnReceiptPhotoPicked`, the photo handed over by `HoldPickedReceipt` / `TakePickedReceipt`,
    `Route.SplitBill.scansReceipt`; closing the choice or the picker stays on Home, as does another screen opening
    over it; a photo that comes back before Home is read waits for it; offline disabled with "Needs internet"),
    "Record payment", which asks "Who paid whom?" first (`Route.PickPayment`: every open pair of every
    group, the most recent group first, pairs with you first; the pair opens Record payment in its place), and
    "New group". Every platform draws them in the page, each with its name, never as bare icons in a bar (on the Mac
    under Overall balance, beside the faces in a wide window). "Your groups": up to three open groups, the pinned
    first, then the latest activity, as Groups draws them; "See all" opens Groups. "Latest expenses": the five latest
    bills you paid or take part in, by day, with your share and its category's badge exactly as the group's Expenses
    rows draw it (Pierre's call, the category as it reads now, `HomeBill.category`). Under the description, no
    "Floripa trip · You paid" line (beta feedback, it was cut off on a phone): the group's rounded square (its photo,
    or its initials on its color, `HomeBill.groupInitials`, `groupColor`, `groupPhotoUrl`) and then who paid as
    overlapping faces (their photo, or their initial on their color; someone no longer in the group a blank face), up
    to three and "+N" (`HomeBill.payerFaces`, `extraPayerCount`), the amber dot and "queued" after them while the bill
    waits, as the legacy Bora Rachar drew the payer and the group; TalkBack, VoiceOver and the web's screen readers
    still read "Floripa trip · You paid". The web's list keeps the day after the faces, and its desktop table draws the
    group's square in its Group column and only the faces under the description. Inside the group, Expenses keeps its
    rows as they are, the group being implied there. A bill opens its Bill over its group. Each line ends in the
    group's Expenses' ⋮ (⋯ on iOS and macOS), a long press (Android, iPhone, iPad) or a right click (the Mac, the web)
    on the line opening it too, with the same options in the same order: "Edit", "Duplicate", "Share" and "Delete",
    which asks the same "Delete this bill?" and stays on Home, the bill leaving the list (Pierre's call, beta feedback,
    reversing "no ⋮ on Home"; [group-and-bills.md](group-and-bills.md) has what each does). They open over Home, not
    over the group (`BillMenuController` in `:core:expenses`, which the group's Expenses shares). The web's desktop table
    has the ⋮ in a last column. "See all" opens "All expenses" over Home (Pierre's call, beta feedback; it opened
    Activity on its bills before), `Route.AllExpenses`: every bill you paid or take part in, across groups, by day,
    drawn as Home's rows, each with the same ⋮, a tap opening its Bill over its group as from Home. A page on every
    platform and size, back on the left (a long list read a page at a time reads better full height than in a
    dialog): Android a column of up to 640 dp centered, iOS and the Mac pushed over Home in its tab, a column of up to
    520 pt, the web a page in the home's frame with Home's tab selected. It reads the device only, a page of 30 bills
    at first and 30 more each time the list nears its end (`AllExpensesViewModel`: a growing `LIMIT` over Room's
    `observeMyExpenses`, so a bill edited, deleted or synced in changes in place), with skeleton rows while the first
    page is read, under the last row while the next one is (or while a new device's first download may still bring
    bills), and "No expenses yet" / "Bills you paid or take part in, in any group, show up here." when there is none. Over the bills (Pierre's call, beta feedback): "Search expenses", which finds bills by their
    description as the home's search does (case and accents ignored; what holds the text typed, and only when nothing
    does, what comes a typo or two away, `TypoTolerantQuery`), then a row of menus: the order ("Newest first", the
    default, "Oldest first", "Largest first", "Smallest first"; sorted by amount the bills read as one list with no
    day over them, ties the most recent first), "Group" and "Category" (several at once; a main category takes its
    subcategories' bills, as Statistics' chips do; one picked names the menu, several read "Group · 2"), "Your share"
    ("You get back", "You owe") and "Who paid" ("You paid", alone or with others, "Others paid"), a menu filled while
    it narrows the list, and "Clear filters" (which keeps the order) once one does; nothing matching shows "No
    expenses match" / "Try other words or clear the filters." with "Clear filters". Android draws them as a search
    field and a row of filter chips that scrolls sideways, iOS and the Mac the bar's own search field (`.searchable`)
    and a row of menus, the web an input and a row of menu buttons that wraps on a phone. At their default the list
    reads the device a page at a time; any filter, search or other order reads every bill of the device once and
    shows them a page at a time (`AllExpensesFilters`), the page going back to the first on each change.
    "Split a bill" is Groups' (asks the group first, 7.1i; ⌘N on the Mac), on iOS the tinted glass item at the end of
    the top bar, never floating over the tab bar: its icon alone on an iPhone upright, as on Groups, and in an iPhone
    Duo's vertical bar, under the profile; spelled out lying down and on iPad. "Search" is the home's (5.2), over
    Home. States: a shimmer until Room is read; no group yet shows only the first use's card (1.3), and only
    once the account's copy is whole; a new device's first download (an account restored on a device with no profile,
    or a sign in that switched to another user, until a sync settled, also across relaunches, `FirstDownload`,
    `AccountSnapshot.awaitsFullSync`)
    shows "Syncing… 3 of 7 groups" with its bar filling (the server says first how many are coming,
    `first_download_groups`, then each group comes down whole, the most recent activity first; until it answered,
    and after a relaunch midway, "Syncing…" and a bar that runs), what already came with skeletons under it and
    "so far · the rest is on its way"; offline the skeletons stay with "No internet. Your groups show up once you're back
    online." (Mac: the window's subtitle); the sync chip as Groups' ("Syncing…" only during the account's first full
    sync), but in the top bar's empty leading side (Android's
    title slot, beside the title on a tablet; iOS's and iPadOS's leading toolbar item, a button while "No response from
    the server" offers to try again; the web's phone top bar left of the bell, and its title row from the tablet up), so
    it comes and goes without moving the content (the beta's feedback), only the first download's bar staying above the
    sections; the wide Android layouts keep it in their first column's header, beside the title; an account with
    neither Google nor Apple shows "Save your recovery code" once the code was snoozed ("Remind me later"), ✕
    hiding it until the next launch; otherwise, once there is a group, the notification card while the system doesn't
    allow them ("Know when a bill comes in", or on Apple "Notifications arrive quietly" while it delivers quietly, in
    [activity-and-push.md](activity-and-push.md)); on the web and the Mac, after both, the invite to the phone app
    once a group holds a bill ([get-the-app.md](get-the-app.md)). A Flip's cover screen shows only Overall balance, the latest bill and "Open the phone to see everything".
  - "Pin to Home" (the group's ⋮/⋯ menu on Expenses and Balances, "Unpin from Home" once pinned) is each
    user's own, synced with their account and never shown to other members (`group_pins`, `GroupPinRepository`), works
    offline, and only orders Home's "Your groups" (Groups keeps its order). A settled group stays out of Home
    even when pinned.
