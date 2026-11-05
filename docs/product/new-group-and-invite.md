# New group and Invite

Creating a group and inviting people.

  - "New group" opens full screen on an Android phone, in two columns lying down (the group, then "Who's
    in"), and as a dialog over the home anywhere wider: 560 dp on a tablet and a foldable upright, in a book's
    right half and a tabletop's top half; a sheet on iOS (two columns on an iPhone lying down, a 540 pt form sheet
    with an iPad's room) and macOS, a dialog on the web (a drawer on a phone). Tapping the group's avatar steps
    its color through the palette, and on Android its shape too, both at once ("Change the group's shape and color": five
    colors over four shapes go through all twenty), starting as a peach Cookie, and Currency starts in the currency of the device's region (`GroupCurrency.ofRegion`), USD when the list has none. A name already in
    the group offers two nicknames, "Ana P." (the initial of whoever adds the name, so the creator's in "New
    group") and "Ana 2". The avatar's corner holds the photo's menu (Take photo where there is a camera, Choose
    photo, the Mac's "Choose photo…" a file, and Remove photo once there is one), which opens "Adjust photo"; while a
    photo is on, the tap changes nothing, and the photo goes up with the group's first sync as Edit group's does.
  - "Invite" opens in the new group's place right after it is created (`Route.Invite` with `isNewGroup`), the
    only time it shows the "Group created" badge. The server makes each group's invite code, and only the group's creator may change it ("Change link",
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
    group's "Invite people" (or Members' Share), with "Change link…" under its share button. Every Invite, the new
    group's too, ends with "Add someone without the app": someone who won't install it goes in by name, as in "New
    group", so it opens the group's Members with "Add person" already open (`RequestAddPerson`, which the group's
    Members on top takes once).
