# New group and Invite

Creating a group and inviting people.

  - The group's type is picked from "Event", "Trip", "Home" and "Other", in that order, and a new group starts as
    "Event": people split more bills at events than on trips (feedback on the beta). Editing a group keeps its type.
  - "New group" opens full screen on an Android phone, in two columns lying down (the group, then "Who's
    in"), and as a dialog over the home anywhere wider: 560 dp on a tablet and a foldable upright, in a book's
    right half and a tabletop's top half; a sheet on iOS (two columns on an iPhone lying down, a 540 pt form sheet
    with an iPad's room) and macOS, whose sheet opens on the name, ready to type, a dialog on the web (a drawer on a
    phone). The group's avatar is its rounded
    square on every platform, Android included (the beta's feedback dropped Android's shapes), and tapping it steps
    its color through the palette ("Change the group's color"), and "Default currency" (the currency new bills start in, with the caption "New bills start in this currency; each bill can use another.") starts in the currency of the device's region (`GroupCurrency.ofRegion`), USD when the list has none. "Add by name"
    takes any name of 2 letters or more, one already in the group too (beta feedback 3, item 29: people are told apart by
    their member and user ids, not their name), here, in Members' "Add person" and "Rename" and in Contacts' "Add to a
    group" alike. A tap or click on the +, as one on the field, focuses the name field and opens the keyboard
    (beta feedback 3, items 6 and 10), anywhere on the row on Android and the web; on iOS and the Mac only the + and
    the field, so no gesture over the field takes the clicks that place its cursor and select; on the Mac the row is the field itself, "Add by name" its
    placeholder, not a label on the left with the field at the far right. While a name is typed, the rows under "Add by name" suggest
    the app's Contacts ([Contacts](balances-and-contacts.md), people already shared a group with, guests included) whose
    name or one of its words starts with it, ignoring case and accents: up to five, a name starting with it first,
    with their photo over the initials, leaving out whoever is hidden from Contacts or already in the list (feedback of
    2026-11-16, Pierre's call: the app's Contacts, never the phone's address book; `CreateGroupUiState.suggestions`).
    A tap adds that same person as Contacts' "Add to a group" does: someone who uses the app goes in as their account,
    joined, a guest as their name, in their own color unless someone in the list has it; it works offline from the
    device's data and goes up with the group's first sync; an account the server no longer takes by then (deleted,
    or no group shared any more) goes up as a guest of that name rather than holding the group, as with Contacts' "Add
    to a group". A name typed and added (Add, Return) is still a new guest,
    even one written as a contact is. "Who's in" shows the profile photos over the initials, yours too (the one the
    home's avatar shows; the initial without one). The avatar's corner holds the photo's menu (Take photo where there is a camera, Choose
    photo, the Mac's "Choose photo…" a file, and Remove photo once there is one), which opens "Adjust photo"; while a
    photo is on, the tap changes nothing, and the photo goes up with the group's first sync as Edit group's does.
  - "Invite" opens in the new group's place right after it is created (`Route.Invite` with `isNewGroup`), the
    only time it shows the "Group created" badge. The device makes the group's invite code when it creates the
    group, so the link and QR code are there at once, offline included (beta bug: the web's "Generating link…" never
    ended): it works for whoever opens it once the group's first sync took it up. In the rare case another group
    already has the code (about one in 10^14), the device makes another before the group goes up, so a link shared
    offline before then changes. A joined group, or one an older version made, gets the server's code with its
    sync. Only the group's creator may change it, online, and the server makes the new code ("Change link",
    `InviteUiState.showsChangeLink`): only on Invite opened from the group and online, at the top on Android and
    iOS, "Change link…" in the ⋯ menu on macOS, and "Someone who shouldn't have the link? Change link" below the link on
    the web. It asks "Change the invite link?" first, then shows the new link and QR code right away; the old link
    and every personal link not used yet stop working (the "invite no longer valid" screen), whoever joined stays,
    and the other members' devices get the new code on their next sync. A failure says so and keeps the link shown
    until a sync brings the server's. Nobody may change it once the creator's account is deleted. Invite is full
    screen on Android at every size (2.2): one column on a phone; lying down the title, who picks and the buttons on
    the left and the QR and link card on the right (380 dp, 420 dp in a tablet's 920 dp container); upright on a
    tablet and a foldable one 560 dp column with the QR card first; on a book the text in the half before the hinge
    and the QR card in the one after it; on a tabletop the QR and link in the top half, facing whoever scans, and the
    title and buttons below. "Change the link?" takes a book's right half and a tabletop's top half. iOS opens it as a
    sheet on an iPhone (two columns lying down) and pushes it full screen with an iPad's room (two columns lying
    down, the QR card first upright); the Mac opens the new group's in a sheet and the group's as a popover on the
    group's "Add people" (or Members' Share), with "Change link…" under its share button. The Mac's popover is
    the exception to the order below: it keeps "Add someone without the app" as a link at its end and has no "See who
    joined", since it opens over the group already. The buttons, on every other Invite (beta bugs of 2026-11-10): the share button first ("Share link" on Android, "Share invite" on iOS, "Share…"
    on the Mac and the web), then "Add someone without the app" as the second, outlined
    (bordered on iOS) button, then "See who joined" as a text link with a people icon ("Go to the group" with an arrow
    while the link waits); the wide boards set the first two side by side. The Mac's sheet ends as the personal invite
    does, "See who joined" as a link and then a trailing row of regular buttons: Close (Esc), "Add someone without
    the app" and "Share…" (Return), or "Go to the group" (Return) in Share's place while the link waits. The web's
    dialog has the link over a footer of Close, "Add someone without the app" and "Share…"; its phone drawer stacks
    "Share…", "Add someone without the app", the link and Close. The web's Invite lists the members with
    their profile photos (yours too) over the initials (beta feedback 3, item 25). Every Invite, the new
    group's too, offers "Add someone without the app": someone who won't install it goes in by name, as in "New
    group", so it opens the group's Members with "Add person" already open (`RequestAddPerson`, which the group's
    Members on top takes once).
  - "Group reminder" (feedback of 2026-11-16, Pierre's call), after "Default currency": the group keeps the
    weekly "Open balance reminder" of Monday at 10:00 unless the creator changes it. A switch turns it off; "Weekly"
    picks the weekday and the time, "Monthly" a day of the month (1 to 31) and the time ("every 25th at 12:00"), each
    read on every member's own clock. Under it, "Add to my calendar" (off, offered while the reminder is on): creating
    the group then hands a repeating event to the device's calendar before Invite opens (see
    [members-and-payments.md](members-and-payments.md#group-reminder)). `CreateGroupUiState.reminder` and
    `addsReminderToCalendar` count as unsaved input once they differ from the default.
  - Closing New group with unsaved input asks "Discard changes?" first, on every way out ([general.md](general.md)).
