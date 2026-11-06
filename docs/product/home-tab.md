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
    rows draw it (Pierre's call, the category as it reads now, `HomeBill.category`); a bill opens its Bill over its
    group, and "See all" opens Activity over Home, as the bell in Home's bar does.
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
    online." (Mac: the window's subtitle); the sync chip as Groups', but in the top bar's empty leading side (Android's
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
