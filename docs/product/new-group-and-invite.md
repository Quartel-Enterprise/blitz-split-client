# Novo grupo and Convidar

Creating a group and inviting people.

  - "Novo grupo" opens full screen on an Android phone, in two columns lying down (the group, then "Quem
    participa"), and as a dialog over the home anywhere wider: 560 dp on a tablet and a foldable upright, in a book's
    right half and a tabletop's top half; a sheet on iOS (two columns on an iPhone lying down, a 540 pt form sheet
    with an iPad's room) and macOS, a dialog on the web (a drawer on a phone). Tapping the group's avatar steps
    its color through the palette, and on Android its shape too, both at once ("Trocar forma e cor do grupo": five
    colors over four shapes go through all twenty), starting as a peach Biscoito, and Moeda starts in the currency of the device's region (`GroupCurrency.ofRegion`), USD when the list has none. A name already in
    the group offers two nicknames, "Ana P." (the initial of whoever adds the name, so the creator's in "Novo
    grupo") and "Ana 2". The avatar's corner holds the photo's menu (Tirar foto where there is a camera, Escolher
    foto, the Mac's "Escolher foto…" a file, and Remover foto once there is one), which opens "Ajustar foto"; while a
    photo is on, the tap changes nothing, and the photo goes up with the group's first sync as Editar grupo's does.
  - "Convidar" opens in the new group's place right after it is created (`Route.Invite` with `isNewGroup`), the
    only time it shows the "Grupo criado" badge. The server makes each group's invite code, and only the group's creator may change it ("Trocar link",
    `InviteUiState.showsChangeLink`): only on Convidar opened from the group and online, at the top on Android and
    iOS, "Trocar link…" in the ⋯ menu on macOS, and "Alguém que não devia tem o link? Trocar link" below the link on
    the web. It asks "Trocar o link do convite?" first, then shows the new link and QR code right away; the old link
    and every personal link not used yet stop working (the "invite no longer valid" screen), whoever joined stays,
    and the other members' devices get the new code on their next sync. A failure says so and keeps the link shown
    until a sync brings the server's. Nobody may change it once the creator's account is deleted. Convidar is full
    screen on Android at every size (2.2): one column on a phone; lying down the title, who picks and the buttons on
    the left and the QR and link card on the right (380 dp, 420 dp in a tablet's 920 dp container); upright on a
    tablet and a foldable one 560 dp column with the QR card first; on a book the text in the half before the hinge
    and the QR card in the one after it; on a tabletop the QR and link in the top half, facing whoever scans, and the
    title and buttons below. "Trocar o link?" takes a book's right half and a tabletop's top half. iOS opens it as a
    sheet on an iPhone (two columns lying down) and pushes it full screen with an iPad's room (two columns lying
    down, the QR card first upright); the Mac opens the new group's in a sheet and the group's as a popover on the
    group's "Convidar pessoas" (or Membros' Compartilhar), with "Trocar link…" under its share button.
