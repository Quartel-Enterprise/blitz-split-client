# Home

The home tab, its shortcuts and pinned groups.

  - "Home" (flow 10, `HomeViewModel` in `:feature:groups`), top to bottom: "Balances with people", a row of faces
    with each person's net across groups as Balances nets it (`PeopleBalances`), whom you owe first, then who owes you,
    the largest first in each, hidden when nobody owes anybody; a face opens settling up for the net
    (`Route.SettleUp`, Pierre's call: it stays so after beta feedback 18), whose two faces open the profiles, and a
    guest (a name nobody joined as) gets a badge and, on a long press or hover, "Diego is a guest and doesn't have the app…".
    "Overall balance" as Groups' (big in the first currency with a net, a line for each other one with a net,
    `HomeSummary.main` and `others`), with "Owed to you" and "You owe", each in the first currency with something that
    way (`HomeSummary.toReceive`, `toPay`), which open Balances filtered (You owe straight to settling up while only
    one person is in it), disabled at zero. Three shortcuts: "Scan receipt", which opens Split a bill as
    "Split a bill" does (asking the group first) and its camera at once (7.1e, `Route.SplitBill.scansReceipt`;
    offline disabled with "Needs internet"),
    "Record payment", which asks "Who paid whom?" first (`Route.PickPayment`: every open pair of every
    group, the most recent group first, pairs with you first; the pair opens Record payment in its place), and
    "New group". "Your groups": up to three open groups, the pinned first, then the latest activity, as Groups draws
    them; "See all" opens Groups. "Latest expenses": the five latest bills you paid or take part in, by day, with
    your share and its category's badge exactly as the group's Expenses rows draw it (Pierre's call, the category as
    it reads now, `HomeBill.category`); a bill opens its Bill over its group, and "See all" opens Activity over Home, as the bell in Home's bar does.
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
    online." (Mac: the window's subtitle); the sync chip as Groups'; an account with
    neither Google nor Apple shows "Save your recovery code" once the code was snoozed ("Remind me later"), ✕
    hiding it until the next launch; otherwise, once there is a group, the notification card while the system doesn't
    allow them ("Know when a bill comes in", in [activity-and-push.md](activity-and-push.md)). A Flip's cover screen shows only Overall balance, the latest bill and "Open the phone to see everything".
  - "Pin to Home" (the group's ⋮/⋯ menu on Expenses and Balances, "Unpin from Home" once pinned) is each
    user's own, synced with their account and never shown to other members (`group_pins`, `GroupPinRepository`), works
    offline, and only orders Home's "Your groups" (Groups keeps its order). A settled group stays out of Home
    even when pinned.
