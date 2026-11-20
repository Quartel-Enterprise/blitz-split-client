# The watch

The watch apps (canvas page 15, "Relógio", issue #419): Wear OS first, the Apple Watch in a later phase. Both draw the
shared ViewModels of `:feature:watch_pairing` and `:feature:watch_balances`; Profile › Connect watch is
`:feature:connect_watch` on every platform.

## What the watch does

  - It works on its own once connected: its own Supabase session for the same user, its own Room database and its own
    sync, over Wi-Fi or LTE. No screen needs the phone nearby. Watches are dark only.
  - Balances at a glance (15.3, `WatchBalancesViewModel`): the overall balance on top, one card per currency with
    nothing left out ("+R$ 186,40", "to receive" or "you owe"), each open group under it with your net in that group,
    the largest first (a group whose open pairs offset each other still shows, with its "I paid"), and "Updated at 14:32" at the end, the watch's last sync that went through
    (`device_settings.lastSyncedAt`, recorded by every device's sync). A group in two currencies is two rows. Settled
    groups never show; with nothing open the watch says "Nothing open" (15.3b). The numbers are the global Balances'
    regrouped by group, so the totals always add up with the rows (no new business rule).
  - Nothing to edit: splitting a bill, editing and invites stay on the phone.
  - Pierre's calls (November 2026): one "Overall balance" card per currency, as Balances' totals, and a group line per
    currency; the Tile and complication show the currency with the most groups; in a group where you owe two or more
    people, each of their rows is its own "I paid"; the watch's "Received" (15.7) records no payment (Notifications).
  - One action, "I paid", on a group where you owe (15.4 → 15.5 → 15.6, `WatchGroupViewModel`): the watch asks "Paid
    R$ 98,00 to Carla?" and records the payment with the same use case as Record payment (`RecordSettlement`), for the
    whole debt, "how it was paid" Pix where the phone's Pix rule offers it and the debt is in reais, Other elsewhere (Pierre's call, November 2026). The EdgeButton "I paid" shows while you owe one person in the group; owing two
    or more, each of their rows is its own "I paid". "Payment recorded" shows, then the watch goes back to the balances.
  - Offline (15.6b): the payment saves on the watch at once, the group shows the orange dot and "waiting to send", and
    the top says "Offline · 1 change queued"; it goes up with the next sync. Nothing waits for the internet.
  - A group settled meanwhile (here or by a sync) closes back to the balances.

## Connecting the watch

One flow for every case (`WatchPairingViewModel`): the watch asks the server for a 6-character code that lasts 5
minutes (`watch-pairing-request`) and polls it (`watch-pairing-poll`) until a device approves it
(`watch-pairing-approve`); the approval hands the watch a magic link's token hash, with which it signs in
(`verifyOtp`) as the same user, then pulls the profile as every sign in does. Until the code ends, each approved poll
brings a fresh token, so a sign in that fails polls again for the next (the server's contract is in `blitz-split-api`).

  - First launch (15.9): "Use the same account as your phone here. Then the watch works on its own." and "Connect".
  - Nearby (15.10 → 15.11): the watch sends the code to the paired phone (Wear OS's Data Layer, later
    WatchConnectivity), which approves it with its own session, with no confirmation on the phone (Pierre's call,
    November 2026: the channel only exists with a watch paired to that phone, and pairing needs it unlocked). The phone
    then shows a notice with no buttons, "Pixel Watch connected to your account" with the watch's own name (15.15).
    It goes to a channel of its own, "Watch", outside Notifications' rows, so it can be silenced alone.
    The watch shows "Ready, Pierre" with the photo and "Continue".
  - Works for anonymous, Google and Apple accounts alike. A local only profile can't connect (15.11b): the watch says
    the data is only on the phone, to connect an account in Profile on the phone, and to tap Connect again.
  - Phone out of reach (15.11c): no phone took the code, or none answered within 20 seconds. "Try again", or "Use code"
    (15.12): the watch shows the code ("K7P 4QX"), "On your phone: Profile › Connect watch", "Valid for 5 minutes"; a
    code that expires is replaced by a new one on its own. A code with less than a minute left is never shown or sent: one on screen is replaced then.
  - Profile › Connect watch (15.14, `ConnectWatchViewModel`), on any signed-in device: six boxes for the code (case and
    spaces never matter), "Connect". A wrong or expired code (15.14b) and no internet (15.14c) disable Connect and say
    why; typing again brings it back. Too many wrong codes, and a local only profile, say so too. Once connected the
    screen says so, with Close only. The row shows only with an account (anonymous, Google or Apple).
  - Signed out on the phone (15.13): the phone tells its watch, which signs out and erases its data once what it
    queued went up (`EndWatchSessionFromPhone`, as "Sign out when synced"), then shows "You signed out on your phone" and "Connect again". A watch out of reach
    finds out on its next sync, when its session is refused. Deleting the account drops the watch's session too.
  - To disconnect only the watch, sign out on it: "Sign out" at the end of its Balances asks "Sign out of this watch?" and signs out once what it queued went up; the phone stays signed in. Until then the end of Balances says "Sign out scheduled · Signs out as soon as everything is uploaded" with "Cancel", as the phone's Profile does. A list of connected devices in Profile is for later.
  - The watch asks for the notification permission (Wear OS 4 and later) on its first Balances once connected.
  - The word for the phone is "phone" on Wear OS and "iPhone" on watchOS.

## Glanceable surfaces

  - The Tile (15.1): "Balances", the overall balance in the currency with the most groups ("+R$ 186,40", "to receive
    in total", "Owed to you R$ 312,40", "You owe R$ 126,00") and "Open"; all settled (15.1b): "All settled", "Nothing to
    pay or receive".
  - A watch with no account (never connected, or signed out) never reads "All settled": the Tile shows "Use the same
    account as your phone here…" with "Connect", which opens the connect screens, and the complications show the app's
    icon alone, no number.
  - Complications (15.2): short text "+186" with the app's icon (the word doesn't fit; the color says it); under one
    unit it keeps the cents ("−0,50") rather than a "−0" that hides a debt, and bitcoin follows the sats preference. Long text
    "You owe R$ 126,00". Tapping either opens the app.

## Notifications

  - The watch gets its own pushes of Activity (FCM on Wear OS, APNs on watchOS), worded on the watch as the phone words
    them (`:android:push` holds the kinds, their sentences and the channels, ids and names, both Android apps share), not the
    phone's mirrored notifications. It registers its own token for the account signed in on it.
  - "X marked that they paid you" and "X marked that Y paid you" have "Received" and "View group". The payment is already recorded by whoever paid
    (no one confirms a payment, [members-and-payments.md](members-and-payments.md)), so "Received" records nothing:
    it marks the activity read and clears the notification (Pierre's call, November 2026). "View group" and every
    other notification open the app on its balances for now.

## Shipping

  - Every stable version with Android ships the watch too, at the phone's version: `release-production` signs
    `:wearApp`'s bundle with the phone's key and sends it to the same track of Google Play's Wear OS form factor,
    with `versionCode` = the phone's + 1,000,000 ([release-process.md](../release-process.md#the-watch-on-google-play)).
    It has the phone's applicationId, so it uses the phone's `google-services.json` (the same Firebase Android app).
    Betas don't build it: the watch's testers get it from Play's Wear OS testing tracks.
  - CI builds `:wearApp`'s release (R8) next to the phone's, and `lintDebug` lints it.
