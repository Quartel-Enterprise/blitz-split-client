# The home and the shells

The home's five tabs and the web's and the Mac's shell.

  - The home shows the five tabs of the design (Início, Grupos, Saldos, Contatos, Atividade), every one with
    Atividade's unread badge. Início (`Route.Home`) is the root and where the app opens; every other tab is one route
    right above it, taking each other's place (`Navigator.openHomeTab`). Android's rail drops its labels on a phone
    lying down and a tabletop, and stays, without "Rachar conta", beside a group's Despesas, Saldos, Membros and
    Estatísticas drawn alone anywhere wider than a phone upright (a phone lying down, a tablet or a foldable upright,
    Estatísticas over a whole book; never on a tabletop, nor beside the home's list, which has its own), the
    tab the group was opened from selected (`homeTab`, `findHomeTabUnder`); each of its tabs, that one included, goes back
    to that tab or opens it in the place of the others (`Navigator.openHomeTab(HomeTab)`, the group's
    `OnRailTabClicked`).
  - The web's and the Mac's shell (slice 13): on the web the group's Despesas, Saldos and Membros, Estatísticas and
    Perfil draw inside the home's frame (`PageShell`, `HomeSidebarViewModel`): the sidebar on the desktop, the icon rail
    on the tablet, the tab the group was opened from selected (Perfil selects the profile at the sidebar's foot
    instead). A phone keeps the group's and Estatísticas' own bars (back, name, Estatísticas, Convidar, ⋯, "Rachar
    conta" at the bottom) and Perfil's "Voltar" bar over the bottom navigation. From the tablet up the group has no
    top bar: "Grupos › Viagem Floripa" in the page (its first crumb names the tab it was opened from and opens it) and
    Estatísticas, Convidar, the main button and ⋯ in the header's row. "Em aberto" (the open groups, as Grupos draws
    them: initials, name and the net without the symbol, "+214,90") shows only where the boards draw it: the web
    desktop's sidebar beside a group's pages and Estatísticas (never on the home's tabs, nor on the tablet's rail),
    and the Mac's sidebar, under its tabs, while Grupos or a group's screen is in front, the group in front marked
    (the tab it sits in stays selected). A group
    picked there opens over Grupos as its row does, in the place of what Grupos opened (the one already open goes
    back to it, keeping its tab, and the one in front stays as it is). A Mac window under 800 pt (the boards' narrow window is 768 pt) moves the segments
    the toolbar held below it, into the content (Grupos' Em aberto / Quitados, which a search's results hide at
    every width, a group's Despesas / Saldos / Membros), and drops a column: Grupos' "Última atividade" and Despesas' "Quem pagou" (`isNarrowWindow`, measured
    per tab by `measuringNarrowWindow()`).
