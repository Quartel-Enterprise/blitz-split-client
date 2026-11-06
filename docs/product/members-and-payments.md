# Members, group balances and payments

Members, the group's Balances, Simplify debts, Record payment, group editing, Default split, leaving and deleting a group.

  - "Members" (`GroupTab.MEMBERS`, `MembersViewModel`) is the group's tab, and opens from Invite's "See who joined". A member's row opens their profile and the ⋮ (⋯ on iOS, macOS and the web, also a long press or right
    click on Apple) holds their menu. Any member may rename and add members ("Add person", in the "Invite link" card
    beside "Share", so both ways in sit together: under the link on Android and on a web phone, a row of its own under
    it on iOS, beside Share on the Mac and the web. The group's main button stays "Split a bill" on Members, as on
    Expenses and Balances, so it never changes under the person. "Has the app? Invite with the link" in Add person
    closes it and opens Invite, for someone who would rather join and pick their own name; a name may repeat another
    member's, as everywhere, beta feedback 3 item 29), but only a member who hasn't joined can be removed ("Remove from group" is disabled for someone
    who joined),
    after confirming "Remove Carla from the group?" (`MembersUiState.memberToRemove`). "Personal invite for Carla" (the
    menu of someone nobody joined as, disabled until the sync brings their code, then offline too) shows their
    single use personal link with Copy and "Share with Carla", and says what whoever joins through it already
    finds: "and already sees the R$ 300 share in the expense added" ("in the 3 expenses added", their parts summed;
    nothing while no bill gives them a part, `PersonalInvite.billCount`). Each member shows their balance ("+R$
    223,50 to get back"), and someone with an open balance can't be removed either ("Carla has an open balance",
    `MembersMember.hasOpenBalance`). Only the group's creator sees
    "Release name", on someone else who joined (`MembersMember.canBeReleased`), bills and open balances included:
    after "Release Carla's name?" (`MembersUiState.memberToRelease`), online only, whoever joined as Carla leaves
    the group and Carla goes back to "Hasn't joined yet", keeping the name, color and bills, with a new personal
    link. The released person gets no notice: the group just leaves their devices on the next sync. On Android its
    dialogs (rename, add, remove, release, and "Personal invite", a bottom sheet elsewhere) open in a book's right half;
    a phone lying down, Android and iPhone, draws it in the group's two columns, the header and balance on the left.
  - "Balances" (`GroupTab.BALANCES`) is the group's tab: each pair once, already netted (simplified across the group only
    while its "Simplify debts" is on, below), "With you" then "Between the others", from the bills' payers and shares (`GroupBalances`, several payers
    included) minus the payments recorded. Rows end in "Settle up" (you owe), "Got paid" (owed to you) or "Already paid"
    (between others), which all open "Record payment". Once there were bills and nobody owes anything, "Everyone's settled up" offers "Delete group". An Android tabletop (6.2) draws Balances alone, full screen, even where its
    other tabs stay beside Groups' list lying down: a one-line bar, the tabs and "Your balance in this group" above the
    hinge, and the pairs under it, "With you" beside "Between the others" while both have one, with no "Split a bill".
  - "Simplify debts" (page 13, `GroupBalances.toSimplified`, `SetSimplifiesDebts`): a group's switch, off in every
    group, in Edit group's "Payments" under Currency and Default split, free. While on, the group's pairs are the
    fewest payments that keep each one's net: the greatest debtor pays the greatest creditor until even, at most
    members − 1, ties by the order they joined, so every device gets the same pairs offline; payments recorded only
    move the nets, and a group already at its fewest payments keeps its pairs (13.3c). Any member turns it on or off,
    offline too, at once and apart from Save ("Waiting to upload" with the amber dot meanwhile); with 2 people it is
    off and disabled with the reason (13.1c), and a group already on can still turn it off. The switch opens its
    confirmation first: "7 payments → 5 payments" and what changes for each person (you first, a guest with
    "let them know with Request", the payments still in the queue, 13.3d), "Turn on anyway" when it saves none; turning off
    names only what changes for you and counts the others (13.3b); "How it works" (13.2, `Route.SimplifyDebtsInfo`)
    explains it. It syncs with its own Last-Write-Wins stamp (`groups.simplify_debts_at`), apart from the row's
    `updatedAt`, so an edit of the name never undoes it nor it the name; when someone's later change beats this
    device's, the group's screen or Edit group, whichever is on top first, says "Ana turned it off after you. The last change counts: Simplify debts is off." once,
    with "See in Activity" (Activity over Home, closing the group, 13.1e). Everything that reads the group's pairs follows it: Balances
    (the card "Simplified debts" with How it works over the pairs, the icon on a pair no bill made, 13.4),
    "Your balance in this group", Settle up, Got paid, Already paid, Members' balances, Groups' rows, Home's faces and "Who
    paid whom?", the global Balances and Contacts ("Simplified debts" on the group's line of "By group", and
    Request saying "instead of paying Bruno you pay me directly; your total didn't change", 13.6). Record payment
    says why a pair it made or grew is there ("Simplified debt: Diego pays you what they owed Bruno.", 13.5),
    and a pair that owes nothing only because the switch flipped while it was open stays with "I paid anyway", which records what was owed (13.5c, a pair of one group). Activity's `simplify_debts_changed` (People and
    groups) says "7 payments became 5. Now Diego pays you R$ 64, and Ana no longer owes you." with "See changes" of
    what changed for you, from the pairs the device that changed it sent; whoever now pays or receives from someone
    else gets a push (13.7), never who changed it. "Edit bill"'s warning after a payment (7.4b) keeps the pairs as
    the bills make them.
  - "Your balance in this group" sits above the tabs of Expenses, Balances and Members while the group has a bill: "R$ 0 ·
    you're settled up" when settled, and its "Settle up" opens Record payment with whoever you owe the most, hidden
    when you owe nobody.
  - "Record payment" (`Route.RegisterPayment`) records a payment made outside the app, which any member may
    record between any two members, with no confirmation by whoever received it: up to the pair's debt, never over
    ("Ana owes you R$ 162,30. Use up to that amount."), partial payments allowed, Pix / Cash / Transfer / Other
    and "When" (date and time, never after now). With Pix picked, when this device's user is the one who pays (beta
    feedback 3 item 26, `RegisterPaymentUiState.formPixKeys`), the form lists "Ana's Pix keys" under "How it was paid",
    her default first, each with its type and a copy button (the "Copied" confirmation as in "Pay Ana", below), or
    says "Ana has no Pix key in the app." (`showsNoPixKeyNotice`), a guest with no account included, in a group in
    reais only (the keys show in any currency, but the form opens on Pix in every one, and a euro or dollar group
    would read the notice on every payment); nothing while
    the keys still load, before the account's first download is whole, offline or back online with it still coming
    down (none cached says nothing then), for
    a former member (whose keys the device can't know), and for "Got paid" or "Already
    paid", where the keys would be of no use. The keys are the ones "Pay Ana" reads (`member_payment_key`, below),
    so they need no sync of their own. "Settle up" opens it too, as "Pay Ana" while Ana has a key (4.5,
    below). It is a bottom sheet on an Android phone
    (640 dp lying down, in two columns: who paid whom and the amount, then how, when and the buttons), a 560 dp
    dialog on its tablets and foldables upright and in a book's right half, and full screen on a tabletop (✕ and
    "Record payment" on top, who and how much above the hinge, how, when and the buttons under it, while Home's
    "Who paid whom?" stays a dialog there, being short); a sheet on
    iOS (in two columns on an iPhone lying down, its confirm the bar's ✓; the 540×620 form sheet with an iPad's room,
    Home's "Who paid whom?" too) and macOS, a dialog on the web (a drawer on a phone). It works
    offline. A debt with someone no longer in the group stays until it is paid: the form shows them as "Former member"
    and closes only once the debt or the group is gone. A payment is never edited: "Ana's payment recorded."
    offers "Undo", and the payment shows in Expenses as a "payment", where tapping it opens its details, whose "Delete" asks
    "Delete this payment?" (a tombstone, offline too). Whoever paid or received it, if not who recorded it, gets a push notification (see [activity-and-push.md](activity-and-push.md)).
  - "Pay Ana" (4.5) is what Record payment shows, in the same sheet or dialog, when this device's user pays
    someone with an account who has a Pix or Bitcoin key: every "Settle up" (the group's Balances, "Your balance in this
    group", the global Balances' `SettleUp`, Home's faces, Contacts' profile), never "Got paid", "Already paid" nor Home's
    "Who paid whom?" (`Route.RegisterPayment.offersReceiverKeys`), so a guest, an ex-member or someone with no
    key gets the form as before. The keys of everyone the user shares a group with sync down with the groups
    (`member_payment_key`, read-only, replaced whole on each sync, cleared on sign out) under the server's
    co-member rule, so it works offline on what the last sync brought. It shows "Pay Ana", where the debt comes
    from ("Apt 302 · you owe", "3 groups · you owe") and the amount; Pix / Bitcoin only while Ana has both;
    "Ana's default key" whole with its type and "Copy key"; for Pix in BRL "Pix copy and paste", the default
    key's BR Code with the whole debt in it (`PixBrCode`, EMV with CRC16, city "BRASIL", the name folded to 25
    ASCII letters) and its QR code, with "Copy code"; "Ana's other keys" with a copy button each; "Pay in your bank's app and come back to mark it as paid. Check that the bank shows Ana's name." (the wallet for
    Bitcoin: "Pay in your wallet and come back to mark it as paid."), and "Later" (closes) and "Mark as paid", which records the whole debt now as Pix, or Other for
    Bitcoin (one payment per group across groups), offline too; "I paid another amount or another way" opens the
    form in its place for a part of the debt, another method or a past day (`RegisterPaymentUiState.prefersForm`),
    and a form that opened with no key stays one when a sync brings the first. No bank app opens by itself: Pix has
    no standard link for it. Android 13 and later show the system's copy confirmation (a "Copied" snackbar before), Apple and the web a
    "Copied" toast; Android keeps the copied key out of the clipboard's preview. A phone's sheet
    is one column, two lying down (who, how and the default key, then the code, the other keys and the buttons);
    the tablet's dialog (a book's right half) and the iPad's form sheet one column; an Android tabletop full screen
    as the form's, who, how and the default key above the hinge and the rest with the buttons under it; the Mac's a 440 pt sheet "Pay Ana · R$ 98"
    whose keys scroll between the title and the buttons; the web's a 480 px dialog (a drawer on a phone).
  - The group's ⋮ menu (⋯ on iOS, macOS and the web) holds Edit group, Leave group and Delete group. "Edit
    group" (`Route.EditGroup`) has New group's fields, the icon being a color or a photo (6.4d): on
    Android and iOS the avatar or "Change icon or photo" opens "Group icon" (Android's bottom sheet: the group's
    square in the color picked, Color, "Choose photo", "Take photo" and "Use this icon", a 480 dp dialog where Edit group is one, in a book's right
    half; iOS's medium sheet: the colors as tiles, the photo's two
    sources and ✓), the color picked there reaching the form only on "Use this icon"; the web ("Icon color", "Use a photo" and, with a camera, "Take photo") and the Mac (the colors and "Choose photo…", a file) pick the color in the form.
    Every group is its rounded square on every platform: Android no longer offers or draws the shapes it had (Cookie,
    Petal, Square, Circle; the beta's feedback), and a group an older Android build gave another shape draws the
    square too. The shape stays a field of the group row, which Room, the API and Activity keep and Edit group never
    changes, so an older build's choice survives untouched.
    The photo is the profile's 512x512 JPEG framed in "Adjust photo" (`PrepareProfilePhoto`), drawn in place of the color and initials in the
    group's square, and picking a color takes it off. A new photo waits on the device and goes up with
    the group's next sync, offline too ("A new photo goes up with it."), to the public `group-photos` bucket
    (`{groupId}/photo.jpg?v=<updated_at>`, which only members write); the photo is a field of the group row like the
    color, saved only when this person changed it, and Activity says "Photo" ("Previous photo", "New photo", "No
    photo"), and "Shape" for a change an older Android build made. The photo shows wherever the group's own badge does:
    Groups' rows and cards, Home, the search, Expenses' header, Split a bill and "Split in which group?", and the
    web's and the Mac's "Open"; Invite (the invite, read before joining), Activity's dot, "Who paid
    whom?" and the groups listed under a person (Balances, Contacts) keep the color. The name has 1 to 40 letters and
    the type stays editable. Currency locks once the group has a bill on the device ("Doesn't change after the first expense"), with no server check. Any member may edit, offline too: the change holds on the
    device and syncs later, and only the fields this person changed are saved, so another member's newer edit to the
    rest stays. Save stays disabled until something changed. It is a sheet on iOS and macOS (a 540 pt form sheet
    with an iPad's room, the avatar beside "Change icon or photo" on an iPhone lying down) and a dialog on the web; on Android
    full screen on a phone (lying down a 240 dp column with the avatar, the fields beside it) and a 560 dp dialog
    anywhere wider, off the hinge as every dialog (`rememberDialogPlacement()`: a book's right half). Its last row,
    under Currency, is "Default split" with the group's way ("Equal", "Shares"…).
  - "Default split" (6.7, `Route.DefaultSplit`, `DefaultSplitViewModel` in `:feature:edit_group`) opens over
    Edit group, which keeps what was typed: pushed inside its sheet on iOS and macOS (`AppModel.sheetPath`), which
    grows from the form sheet to a bill's with an iPad's room (820×640 lying down, 620×860 upright) and shrinks back
    once it is popped, in its dialog's place on the web (a drawer on a phone), and on Android full screen on a phone
    and a foldable upright, a dialog on a tablet (920 dp in two columns lying down, 560 dp upright) and in a book's right half in one column (off
    the hinge as every dialog). It sets
    how the group's new bills split: Equal (who is in it, everyone by default), Shares, Percentage
    (whole numbers adding up to 100, else "It adds up to 90%. 10% missing." and Save disabled) or By income, whose
    incomes stay on the device that typed them (`split_group.splitIncomes`, never synced): the group keeps only the
    percentages they give (`IncomePercents`). A new bill's Split may also save its split as the default or its
    category's exception ("Use as the group default", in [split-bill.md](split-bill.md)). "On a R$ 100 bill: Pierre R$ 75 ·
    Ana R$ 25" with the bar in each one's color is always in sight, the cents as Split a bill shares them (Equal's
    example is the first round bill everyone gets the same of, R$ 120 among six). It is one field of the group row (`groups.default_split`,
    `GroupDefaultSplit`), saved alone by its own Save (back goes to Edit group unsaved) and synced with the
    group's Last-Write-Wins, by any member, offline too, keeping the keys a newer app adds to it (this version
    writes only `mode`, `weights` and `exceptions` over the stored object); Equal with everyone in and no exception
    saves no default. Someone who joined after it was saved starts in, with 1 part or at 0% ("Carla joined the group. Starts with 1 share; adjust if
    needed.", until someone saves). Two columns lying down (the ways to split, the people with the example), in
    the Mac's sheet, an iPad lying down and the web from 1024 px. "Exceptions by category" (6.7f) lists the categories
    that split their own way ("None. Every category follows the split above."): "Add exception" opens
    "Exception by category" (a bottom sheet on Android where Default split is full screen, a 480 dp dialog where it
    is a dialog, in a book's right half; a sheet on iOS and macOS, 540 pt with an iPad's room; in the dialog's place
    on the web),
    one main category or subcategory (each at most once) split by Equal (who is in it), Shares or Percentage (whole
    numbers adding up to 100), with its own example, saved with the rest of the default by Save
    (`GroupDefaultSplit.exceptions`, `ExceptionEditor`). A new bill of that category, or of a subcategory of it
    without its own, starts from the exception instead of the default (7.1k, `GroupDefaultSplit.exceptionFor`):
    "Split" says "Group exception for Groceries" and Split's notice "Group exception for Groceries: Equal" (7.2g),
    and changing the category moves the bill to the other way while Split was never changed by hand.
  - "Leave group" and "Delete group" (5.3c to 5.3f, 6.1i to 6.1k) need the internet: offline the ⋮ menu shows
    them disabled with "Needs internet", since only the server knows every device's bills and payments. The
    device pushes first what it hasn't sent yet (a whole sync, only while something waits to go up), then the server
    checks the balances again (`leave_group`, `delete_group`). Leaving needs your
    own balance settled ("Settle up before leaving" lists what is still open, with "Settle up" while you owe, "See balances"
    otherwise); you go back to Groups, and your member stays as a name in the bills and payments, not joined, so the
    invite link can bring you back. A group nobody else with an account is in goes, which "Leave" says
    (`ConfirmLeave.deletesGroup`). Any member may delete the group
    once nobody owes anything ("There are still open balances" lists the pairs, with "See balances"), for everyone, with its
    bills and payments, after "Delete “X”?" ("Delete for everyone", no undo). Deleting answers at once (beta feedback
    3, item 7: the dialog's spinner used to wait for a whole sync, a round trip per part, before the server was even
    asked): "Delete for everyone" closes the dialog and the group's screens, and the group leaves every list, Home
    and the balances right away, while the device asks the server in the background (`DeleteGroup`,
    `GroupRepository.hideGroup`); it leaves the device once the server deleted it. If the server finds a balance
    this device didn't have yet, or can't be reached, the group comes back and Groups or Home, whichever is on top
    first, says "“X” wasn't deleted: there are still open balances in it." or "Couldn't delete “X”. Check your
    connection and try again." (`GroupDeletion.failure`, the same queue as below). Leaving still waits for the
    server in its dialog. On the other devices a sync takes it off,
    even with a change not pushed yet (the server's `removed_groups`): the group open on it, whatever the tab, says
    "Group deleted" with who deleted it and "Back to Groups" (`GroupExitState.deletedNotice`), once on top again
    if a bill covered it; any other screen of the group (Invite) just closes, and Groups or Home, whichever is
    on top first, says "Carla deleted the group X." (`GroupsUiState.deletedGroupNotice`, `HomeUiState.deletedGroupNotice`,
    one shared queue); never on the device that deleted it.
