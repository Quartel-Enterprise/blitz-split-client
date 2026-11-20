# Balances, Contacts and people

The global balances, a person's profile and Contacts.

On Apple, Balances and Contacts come back to their tab already read, with the person picked and the rows opened, and
Balances on the filter it was opened with: see the tab switch in [the home and the shells](home-and-shell.md).

  - "Balances" (flow 8, `Route.GlobalBalances` over the groups) is the home's second tab: each person once, netted
    across every group of one currency (`PeopleBalances`): someone who joined is their account in every group, a name
    nobody joined as is the same person wherever it is written the same, and someone no longer in a group only
    themselves ("Former member"); someone with groups in two currencies shows once per currency (the group's own Balances, Members and settling up use the same per-currency computation, `GroupBalances.getByCurrency`, so a group's figures and these never differ). All / You owe / Owed to you filter it, each with its total (the net always matches the home's Overall balance; "To get back" and "To pay"
    there count each group's net, so they can differ), "By group" opens in the row (each group with its photo when it
    has one, its initials otherwise; already open for someone alone
    in You owe or Owed to you), and the empty "You owe nothing" leads to Owed to you, "Nobody owes you" to You owe and "All settled" to Groups. "All settled" stands alone, on every platform (beta feedback of 2026-11-18, Android showing "R$ 0 / € 0" under the filter, one line per currency of the groups; Pierre's call: hide it whenever nothing is open, not only with no group): once nobody has an open balance, whether there is no group yet or every group is settled up, the three totals (All, You owe, Owed to you, the Mac's toolbar filter and subtitle too) and the caption under them leave, since a row of zeros says nothing and invents a currency per group (`GlobalBalancesUiState.showsSummary`). They stay while loading (the shimmer) and whenever someone has an open balance, even when the filter lists no one ("You owe nothing" keeps the filter that leads out of it); a wide layout gives the empty state the whole width, since there is no list or filter beside it (an unfolded book and an open iPhone Duo keep it in the half past the hinge or fold instead, since nothing crosses it). "Settle up" (you owe, as "Pay Ana" while Ana has a key) and "Received" (owed to you)
    open Record payment for the net (`Route.SettleUp`): one payment in each group, the way that group's pair owes, all or none (`SettleUp`), the
    amount locked while more than one group is in it, then "Carla's payment recorded." on Balances, whose Undo
    undoes them all in one go (never on a later Balances, when the one it opened over closed while saving). "Request"
    (`Route.Charge`) opens over the screen it was tapped on, Balances or a contact's profile, and closing it (or a sync
    settling the person) goes back there. It moves no money: each platform writes the message ("you owe me R$ 32,90 from the groups …", a guest's bills and what they already paid while those numbers add up, the default Pix key while "Include my Pix key" is on (it only exists with a saved Pix key, which turns [the Pix rule](general.md) on), which locks once the message is edited), "Send" opens the system share (the share sheet, or the Mac's sharing picker, on Apple,
    `navigator.share` or a copy on the web) and "Copy message" copies; a bottom sheet on an Android phone and a
    560 dp dialog on its tablets, a book's right half and a tabletop's top half (its right half above the hinge lying
    down) with that screen under the scrim, a
    sheet on iOS (the whole screen on an iPhone lying down, whom and the message beside the Pix key and the buttons;
    a 540 pt form sheet in the middle with an iPad's room) and macOS, a 480 px dialog on the web (a drawer on a phone);
    the Mac's sheet draws its title on top and Close, "Copy message" and "Send" in a row at the bottom, as Record
    payment's, Send on ⌘Return (Return adds a line to the message, as in a comment) and Close on Esc, which also
    closes it while the request is still being read. It all works offline on the device's data. Android draws "Split a bill" on Balances as on its other three tabs (the FAB on a
    phone upright, the rail's button anywhere wider, `GlobalBalancesUiEvent.OnSplitBillClicked`; the beta's feedback),
    asking the group first with the most recently used one marked, once there is a group. iPhone and iPad draw it too
    (beta feedback of 2026-11-15), the top bar's tinted glass item after the profile, spelled out while the list is at
    its top and its icon alone once it scrolls down (see [Home](home-tab.md)), which keeps that bar whole on a scroll
    rather than minimizing it; the Mac and the web keep it on Home and Groups (Pierre's call). An Android tablet lying down, an unfolded book, an
    iPad lying down, the web's desktop (from 1024 px) and a Mac window wide enough for the inspector list the people
    beside the one picked (`GlobalBalancesUiState.selectedPerson`, the first listed until a row is picked or once the
    filter hides the pick): their groups by name, "Carla owes you R$ 32,90" with the actions, "By group" (a table on
    the web) and "The net already offsets the groups: Request and Received use R$ 32,90. The app records a payment in each group." ("Only one group open with Ana." for one group); the empty states take that pane. Everywhere else one
    column with "By group" in the row, the filter beside the list on a phone lying down (on an iPhone, Overall balance
    and the filter stay still in a 300 pt column on the left while the people scroll on the right); on Android the top bar, with the profile photo, sits over that filter column, which scrolls under it at a large
    font, and over the list beside a person's pane, so what is beside it starts at the top: see
    [the home and the shells](home-and-shell.md)). On the Mac the
    arrow keys move through the people, as through Activity's items (beside the inspector they pick the one it shows;
    a narrow window only marks the row), a double click or Return opens the person's profile, and a right-click offers
    "Got paid" and "Request" for someone who owes you, else "Settle up", then "Open Ana's profile".
    A person's card keeps "By group" and the actions on one line while they fit, else moves the actions to a line
    of their own at its end (beta feedback of 2026-11-14: on a phone lying down "Request" broke letter by letter);
    no button ever wraps its label.
  - A person tapped wherever a bill, a list or the feed shows them opens their profile, as Contacts opens it (Pierre's
    call, beta feedback 18, `Route.ContactProfile` by Balances' person key, `openPersonProfile`): Bill's split rows,
    its lone payer ("Ana paid"; several payers each have their row), a comment's author and a History line's
    face, Members' rows, Balances' people, Activity's actor face (the rest of the row still opens the item; VoiceOver
    has it as the row's action), Statistics' "By person" and "Settling up", and Record payment's two faces (whoever paid →
    whoever received, and "Pay Ana"'s, `RegisterPaymentUiEvent.OnPersonClicked`), over the form, which back finds
    again. This device's own user opens Profile (the Mac's Settings); someone no longer in the group opens nothing.
    Home's faces, the group's Balances pairs and Expenses' rows stay as they were (whatever opened `Route.SettleUp`
    still does, Pierre's call). What Apple draws outside the window's stack (Record payment's sheet, Bill with an
    iPad's room or in the Mac's inspector) closes while the profile shows and opens again on back, Record payment starting over
    there (the whole debt, Pix, now, "Pay Ana" again); Android keeps it as it was, and the web keeps Bill and
    Record payment mounted under the profile, whose back says "Back" (the tab's name only right above a tab).
    On Android the back gesture over that profile returns to Record payment's sheet like the top bar's arrow does (it
    used to cancel, since the sheet opened a window mid-gesture; beta feedback of 2026-11-16, "Did Larissa pay you?").
  - The face tapped flies into the profile's photo and back into it on back (beta feedback of 2026-11-15, "when
    possible"), Profile's (from a home tab's avatar, "You" in a bill) as a contact's, following the group's
    transition rules: only the face tapped in the last second (a bill shows its payer twice; a search hit, a link or a
    pick beside Contacts' list flies nothing), never beside a list, never on the Mac, and reduced motion stills it.
    Android: shared elements (`ProfileFlight`: `rememberProfileFlightSource` on the face, `rememberProfileFlightTarget`
    on the profile's photo, keyed by the face tapped), from the home tabs' avatar, Bill's split rows, lone payer, comment
    authors and History faces, Members' rows, Contacts' and Balances' people, Activity's actor face and Statistics'
    people; what opens as a dialog or a sheet (Bill on a tablet, Record payment) flies nothing. iOS and iPadOS: the zoom
    navigation transition (`ProfileZoomTransition.swift`, a namespace per tab's stack), from the tabs' avatar, Bill's
    split rows and lone payer, Members' rows, Contacts' and Balances' people and Activity's actor face. The web: the
    View Transitions API (`webApp/src/lib/groupTransition.ts`, `data-profile-link` around the face,
    `data-profile-photo` on the profile's), from the top bar's avatar, the `PersonButton`s given a `flightKey` (Bill's
    split rows, payer and History faces, Members, Balances, Statistics, Activity) and Contacts' rows; Record payment,
    a dialog, flies nothing, as on Android; back flies into the same face once its page drew it
    again, and a face under a closed panel (Bill under the profile) leaves the page to crossfade.
  - With the face, a row that shows the person's name and net (Contacts' rows, Balances' people) flies them too, into
    the contact's profile's name and its balance card's amount and word ("owes you" morphs into "Larissa owes you"),
    and back on back and the back gesture (beta feedback of 2026-11-16, "where applicable"), under the same rules. Only
    where the profile has the piece: Profile has no balance and no tapped row shows the user's own name, so only the
    face flies there; rows whose amount is another one (a group's balance in Members, a share in a bill) fly the face
    alone; a hidden contact's row flies its face and name. Balances lists a person once per currency while the
    profile's card shows one net, so only the row in that currency flies its amount and word; another currency's row
    flies the face and name, its balance crossfading with the page. Android: `rememberProfileFlightSource` hands the row `name`,
    `amount` and `word`, which fly as shared bounds (scaled and cross-faded, since their size and wording change) into
    `rememberProfileFlightTarget`'s, the amount and word keyed by their currency (`balanceKey`). The web: the row
    marks them `data-profile-piece` (`name`, `amount`, `word`, the last two with `data-profile-balance`, their
    currency), in the link or beside it, and the contact's profile marks its own inside `data-profile-page`. iOS and iPadOS: the
    zoom already carries the whole row into the profile, so nothing changes there.
    A profile opened from settling up goes back to that form on its Settle up or Received rather than opening another;
    opened from one group's Record payment, they open settling up for the net over it (Pierre's call).
  - A profile link opened (`Route.ProfileLink`, `https://blitzsplit.com/u/<code>`, Profile's "Share profile") shows
    what this device already has of its owner, so nothing is drawn twice (Pierre's call: it opens a profile page): the
    person's own link opens Profile (the Mac's Settings) at once, offline too; a contact's, someone the person shares
    a group with, opens their contact profile in its place (groups in common, the balance, Request and Settle up),
    whatever the device's copy shows. Anyone else gets a page of their name and photo, "You don't have any groups
    with Ana yet." and "Invite to a group", which opens Contacts' invite with a group's link ("Invite with link or QR"
    for that name); without a profile on the device (signed out, the welcome) it has no invite. Only the person's own
    link opens offline; any other asks the server (`get_profile_by_share_code`), which answers anyone the name, the
    color and the photo, and names the account only to the person and to whoever already shares a group with them,
    so a stranger learns nothing more of who they are. Offline it says "Couldn't open this profile" with "Try again"
    and opens by itself once back online; a code no profile has says "Profile not found". One centered column on
    every platform and size (a pushed screen on Android and Apple, a page of its own on the web).
  - "Contacts" (flow 11, `Route.Contacts`) is the home's third tab: everyone the user shares a group with, guests
    included, once (Balances' person key, `PeopleBalances.keyOf`), A to Z ignoring case and accents, each with their
    net in the first currency they have one ("+R$ 32,90 owes you", "R$ 0 · settled") and "3 groups" or the group's
    name, under the card "You owe / Owed to you / See balances" (Balances' totals). It all works offline on the device's
    data ("Offline · this device's contacts"). Android, iPhone and iPad draw "Split a bill" here too, as on Balances
    (`ContactsUiEvent.OnSplitBillClicked`), beside a profile the pane shows as well. Search, the navigation's item (the sidebar's first on the Mac), opens the home's "Search" (5.2) over Contacts (Pierre's call: the people are in its People, each once opening this profile, then once per group opening its Members), and the agenda is searched only in "Invite from contacts". "Hidden" holds whoever
    was hidden ("Hide from Contacts", only while the balance with them is zero, asked first), synced to the user's
    other devices (`hidden_contact`, the server's `hidden_contacts`), with "Show"; someone hidden comes back by
    themselves once a bill shared with them is saved later or the balance opens again. On the Mac the arrow keys move
    through the people (the hidden ones too, once open), as through Balances': beside the list each opens there as a
    click does, and a narrow window only marks the row, Return or a double click opening it over the list; a right-click
    offers "Open Ana's profile" (and "Show in Contacts" for someone hidden): Contacts' list has no Settle up or
    Request of its own, which stay on the profile. Beside the list a wide window (full screen) keeps the profile at
    640 pt, centred. Pushed in a window lying down (a phone or a tablet lying down, an unfolded book, an iPhone or
    iPad lying down, the web's page from a 56 rem column) the profile takes two columns, who the person is with the
    guest's card and the net on the left, "Groups in common" and the history on the right, each scrolling on its own (the
    web's page scrolls as one); beside Contacts' list it keeps one (beta feedback of 2026-11-14). As the header scrolls up its face shrinks and
    fades while a small face and the name come into the top bar (Android and iOS, as the group's header does; the
    web's header sticks and folds into a compact bar, under the phone's top bar; the Mac, whose toolbar names the
    person, and the pane beside Contacts' list keep their header as it is). The header's face ("View photo") opens
    it big in the viewer of the profile's and the group's photo ([Profile](profile.md)), the initials on their color
    when there is no photo, with no shortcuts under it: no Camera, Gallery or Remove, since the photo is the person's
    to change (Pierre's call, `ContactProfileUiState.isPhotoOpen`); ✕, a tap outside, back and Esc close it, except
    the browser's back, which leaves the profile as it does over the profile's other dialogs (it is the profile's
    state, not a route of its own). "Request" and
    "Received" sit side by side while both labels fit and stack full width otherwise, as the guest card's "Merge"
    and "Invite". "Share profile" beside ⋯ shares the person's profile link
    (`https://blitzsplit.com/u/<code>?lang=<app language>`, as they would share it from their own Profile, the link
    alone), only for someone with an account whose code a sync brought (each member reads the others'
    `profiles.share_code` with their photos, `member_photo`); a guest has no button (Pierre's call). The profile
    (`Route.ContactProfile`) shows the net ("Already offset across 3 groups"), Request (Balances' message, opened over
    the profile), Received or Settle up (Record payment for the net, `Route.SettleUp`), "Add to a group" (an account goes straight in, joined, with no part in the old bills; a guest as a name; either
    keeps their name even when someone in the group has it too, beta feedback 3 item 29; a group they're
    in shows disabled; on the Mac the arrow keys pick the group and Return adds them), "Groups in common" (each group with its photo when it has one, its initials otherwise) and the latest four of "History with Carla" (bills that move something
    between the two and their payments; a bill with its category's badge as the group's Expenses draws it, never a
    generic receipt, beta feedback, `ContactHistoryItem.Bill.category`; a payment with its arrow); "See all" opens
    Activity on that person. A guest's profile
    offers "Invite Diego" (their personal link) and "Merge with an account" (online only): the server makes a
    request and a single use personal link, and once the guest joins through it everything in that name, in every
    group, passes to their account ("Waiting for Diego to accept", Cancel). When the person using the app is that
    guest, the sheet's button above the options, "I am Diego", asks "Merge Diego into your account?" (it can't be
    undone) and moves everything in that name to their own member in every group at once, with nobody to accept
    (`claim_guest_members`, online only; a sync then takes the guest away and closes the profile); "Ask someone who already uses the app" answers
    "Coming soon". "Invite from contacts" (`Route.ContactsAgenda`, Android and Apple; the web says the browser can't read
    the agenda) explains before the system asks, leads to the settings once refused, and lists the agenda (only
    the contacts picked under iOS's limited access); no phone number leaves the device, so nobody is shown as
    already using the app. Every invite is a group's link: "Invite with link or QR" (`Route.ContactInvite`) picks
    the group (chips, the most recent first, and "New group") and shows its QR and link, offline too once the
    group has it. With no group yet the sheet shows only why and a filled, full-width "New group" button: Copy link
    and Share are hidden, never left disabled. Wide windows (the same as Balances') list the people beside the one picked
    (`ContactsUiState.selectedKey`) or the agenda; the invite is a bottom sheet on an Android phone, a 560 dp dialog
    on its tablets (the book's right half), a sheet on iOS and the Mac (540 pt on the iPad), a 480 px dialog on the
    web (a drawer on a phone).

## The balance widget (iPhone)

Board 15.8 of page 15 (the watch, [issue #419](https://github.com/Quartel-Enterprise/blitz-split-client/issues/419)).
Until the watch has its own product doc, its rules live here. The iPhone (and the iPad, which takes the same
extension) gets a WidgetKit widget, "Balances", in four sizes, light and dark from the app's tokens (the money's green
and orange, the accent):

  - Small: "Balances" with the logo, the overall balance with its sign and word ("+R$ 186,40", "to get back"), and under
    it "Owe you R$ 312,40" and "You owe R$ 126", each only when it isn't zero.
  - Medium: the same on the left with "Updated at 14:32" at the bottom, and on the right the three groups most recently
    active that have an open balance, each with its name and "−R$ 98 you owe" or "+R$ 214,90 to get back". A group with
    bills in two currencies is a line per currency with an open balance, as its own Balances; a settled group never
    shows.
  - Lock Screen, circle: the logo over the compact overall balance ("+186", whole units; two significant digits past
    a thousand, "1.2K", and three past a hundred thousand, "456K"), the sign saying which way since the Lock Screen
    drops the color.
  - Lock Screen, rectangle: the app's name ("Bora Rachar" in Brazil's Portuguese, "Blitz Split" elsewhere), "+R$ 186,40
    to get back", and the other way's total ("You owe R$ 126" under what comes back, "Owe you …" under what you owe).

The figures are Home's, from the same rules (`findMain`, `findToReceive`, `findToPay`): the overall balance is the
"Overall balance" of the first currency with a net (the global Balances' totals), "Owe you" and "You owe" each in the
first currency with something that way, and every amount is written by the app's own formatter (decimals dropped when
zero, bitcoin in sats when Profile says so, Latin digits in Arabic and Bengali), in the app's language (Profile ›
Language) whatever the device's, offline too. With nobody owing anyone in any currency and no group open, or no group
yet, every size says "All settled" (the circle a check mark); an overall balance of zero whose two ways offset each
other shows "R$ 0" with no word, "Owe you" and "You owe" under it and the open groups saying what is still open.
Before the app has written anything (an update not opened yet) every size shows a dash, never "All settled". A local
only profile, an anonymous account and a signed in one all get it, since it reads only this device's data. Tapping any
size opens Balances on All (`borarachar://balances`, `WidgetLink`) as its tab does: what was open above Home closes,
as a push notification's tap does; without a profile it opens the app as it is.

The widget's process has no Room and no Kotlin: the app builds the snapshot in the shared code
(`ObserveBalanceSnapshot`, `BalanceSnapshot`), formats it and writes it as JSON to the App Group
`group.com.quare.blitzsplit`, then reloads the widget (`BalanceWidgetWriter`): when a group, a bill or a payment changes
here or through a sync (a burst, such as a first sync, written once), when Profile's language, Time format or bitcoin
unit change, and when the app goes to the background, each time only if the widget would draw something different, so
WidgetKit's daily reloads aren't spent on nothing. "Updated at" is when the figures were last written, on Profile's Time
format. The faces live in `iosApp/Widgets/Shared`, which the watch's complication and Smart Stack card reuse, and draw
with the app's color tokens (`iosApp/Sources/Common`); the Mac, Android and the web have no widget yet.
