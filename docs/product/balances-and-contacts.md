# Balances, Contacts and people

The global balances, a person's profile and Contacts.

  - "Balances" (flow 8, `Route.GlobalBalances` over the groups) is the home's second tab: each person once, netted
    across every group of one currency (`PeopleBalances`): someone who joined is their account in every group, a name
    nobody joined as is the same person wherever it is written the same, and someone no longer in a group only
    themselves ("Former member"); someone with groups in two currencies shows once per currency. All / You owe / Owed to you filter it, each with its total (the net always matches the home's Overall balance; "To get back" and "To pay"
    there count each group's net, so they can differ), "By group" opens in the row (already open for someone alone
    in You owe or Owed to you), and the empty "You owe nothing" leads to Owed to you, "Nobody owes you" to You owe and "All settled" to Groups. "Settle up" (you owe, as "Pay Ana" while Ana has a key) and "Received" (owed to you)
    open Record payment for the net (`Route.SettleUp`): one payment in each group, the way that group's pair owes, all or none (`SettleUp`), the
    amount locked while more than one group is in it, then "Carla's payment recorded." on Balances, whose Undo
    undoes them all in one go (never on a later Balances, when the one it opened over closed while saving). "Request"
    moves no money: each platform writes the message ("you owe me R$ 32,90 from the groups …", a guest's bills and what they already paid while those numbers add up, the default Pix key while "Include my Pix key" is on, which locks once the message is edited), "Send" opens the system share (a ShareLink on Apple,
    `navigator.share` or a copy on the web) and "Copy message" copies; a bottom sheet on an Android phone and a
    560 dp dialog on its tablets, a book's right half and a tabletop's top half (its right half above the hinge lying
    down) with the people under the scrim, a
    sheet on iOS (the whole screen on an iPhone lying down, whom and the message beside the Pix key and the buttons;
    a 540 pt form sheet in the middle with an iPad's room) and macOS, a 480 px dialog on the web (a drawer on a phone). It all
    works offline on the device's data, and has no "Split a bill". An Android tablet lying down, an unfolded book, an
    iPad lying down, the web's desktop (from 1024 px) and a Mac window wide enough for the inspector list the people
    beside the one picked (`GlobalBalancesUiState.selectedPerson`, the first listed until a row is picked or once the
    filter hides the pick): their groups by name, "Carla owes you R$ 32,90" with the actions, "By group" (a table on
    the web) and "The net already offsets the groups: Request and Received use R$ 32,90. The app records a payment in each group." ("Only one group open with Ana." for one group); the empty states take that pane. Everywhere else one
    column with "By group" in the row, the filter beside the list on a phone lying down (on an iPhone, Overall balance
    and the filter stay still in a 300 pt column on the left while the people scroll on the right).
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
    A profile opened from settling up goes back to that form on its Settle up or Received rather than opening another;
    opened from one group's Record payment, they open settling up for the net over it (Pierre's call).
  - "Contacts" (flow 11, `Route.Contacts`) is the home's third tab: everyone the user shares a group with, guests
    included, once (Balances' person key, `PeopleBalances.keyOf`), A to Z ignoring case and accents, each with their
    net in the first currency they have one ("+R$ 32,90 owes you", "R$ 0 · settled") and "3 groups" or the group's
    name, under the card "You owe / Owed to you / See balances" (Balances' totals). It all works offline on the device's
    data ("Offline · this device's contacts"). Its magnifier opens the home's "Search" (5.2) over Contacts (Pierre's call: the people are in its People), and the agenda is searched only in "Invite from contacts". "Hidden" holds whoever
    was hidden ("Hide from Contacts", only while the balance with them is zero, asked first), synced to the user's
    other devices (`hidden_contact`, the server's `hidden_contacts`), with "Show"; someone hidden comes back by
    themselves once a bill shared with them is saved later or the balance opens again. The profile
    (`Route.ContactProfile`) shows the net ("Already offset across 3 groups"), Request (Balances' charge, opened over
    Balances for that person), Received or Settle up (Record payment for the net, `Route.SettleUp`), "Add to a group" (an account goes straight in, joined, with no part in the old bills; a guest as a name; a group they're
    in shows disabled), "Groups in common" and the latest four of "History with Carla" (bills that move something
    between the two and their payments); "See all" opens Activity on that person. A guest's profile
    offers "Invite Diego" (their personal link) and "Merge with an account" (online only): the server makes a
    request and a single use personal link, and once the guest joins through it everything in that name, in every
    group, passes to their account ("Waiting for Diego to accept", Cancel); "Ask someone who already uses the app" answers
    "Coming soon". "Invite from contacts" (`Route.ContactsAgenda`, Android and Apple; the web says the browser can't read
    the agenda) explains before the system asks, leads to the settings once refused, and lists the agenda (only
    the contacts picked under iOS's limited access); no phone number leaves the device, so nobody is shown as
    already using the app. Every invite is a group's link: "Invite with link or QR" (`Route.ContactInvite`) picks
    the group (chips, the most recent first, and "New group") and shows its QR and link, offline too once the
    group has it. Wide windows (the same as Balances') list the people beside the one picked
    (`ContactsUiState.selectedKey`) or the agenda; the invite is a bottom sheet on an Android phone, a 560 dp dialog
    on its tablets (the book's right half), a sheet on iOS and the Mac (540 pt on the iPad), a 480 px dialog on the
    web (a drawer on a phone).
