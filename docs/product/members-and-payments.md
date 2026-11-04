# Membros, group balances and payments

Membros, the group's Saldos, Simplificar dívidas, Registrar pagamento, group editing, Divisão padrão, leaving and deleting a group.

  - "Membros" (`GroupTab.MEMBERS`, `MembersViewModel`) is the group's tab, and opens from Convidar's "Ver quem já
    entrou". A member's row opens their profile and the ⋮ (⋯ on iOS, macOS and the web, also a long press or right
    click on Apple) holds their menu. Any member may rename and add members ("Adicionar pessoa", where the other tabs
    have "Rachar conta"), but only a member who hasn't joined can be removed ("Remover do grupo" is disabled for someone
    who joined),
    after confirming "Remover Carla do grupo?" (`MembersUiState.memberToRemove`). "Convite pessoal para Carla" (the
    menu of someone nobody joined as, disabled until the sync brings their code, then offline too) shows their
    single use personal link with Copiar and "Compartilhar com Carla", and says what whoever joins through it already
    finds: "e já vê a parte de R$ 300 na despesa lançada" ("nas 3 despesas lançadas", their parts summed;
    nothing while no bill gives them a part, `PersonalInvite.billCount`). Each member shows their balance ("+R$
    223,50 a receber"), and someone with an open balance can't be removed either ("Carla tem saldo em aberto",
    `MembersMember.hasOpenBalance`). Only the group's creator sees
    "Liberar nome", on someone else who joined (`MembersMember.canBeReleased`), bills and open balances included:
    after "Liberar o nome Carla?" (`MembersUiState.memberToRelease`), online only, whoever joined as Carla leaves
    the group and Carla goes back to "Ainda não entrou", keeping the name, color and bills, with a new personal
    link. The released person gets no notice: the group just leaves their devices on the next sync. On Android its
    dialogs (rename, add, remove, release, and "Convite pessoal", a bottom sheet elsewhere) open in a book's right half;
    a phone lying down, Android and iPhone, draws it in the group's two columns, the header and balance on the left.
  - "Saldos" (`GroupTab.BALANCES`) is the group's tab: each pair once, already netted (simplified across the group only
    while its "Simplificar dívidas" is on, below), "Com você" then "Entre os outros", from the bills' payers and shares (`GroupBalances`, several payers
    included) minus the payments recorded. Rows end in "Acertar" (you owe), "Recebi" (owed to you) or "Já pagou"
    (between others), which all open "Registrar pagamento". Once there were bills and nobody owes anything, "Todo
    mundo acertou" offers "Apagar grupo". An Android tabletop (6.2) draws Saldos alone, full screen, even where its
    other tabs stay beside Grupos' list lying down: a one-line bar, the tabs and "Seu saldo neste grupo" above the
    hinge, and the pairs under it, "Com você" beside "Entre os outros" while both have one, with no "Rachar conta".
  - "Simplificar dívidas" (page 13, `GroupBalances.toSimplified`, `SetSimplifiesDebts`): a group's switch, off in every
    group, in Editar grupo's "Pagamentos" under Moeda and Divisão padrão, free. While on, the group's pairs are the
    fewest payments that keep each one's net: the greatest debtor pays the greatest creditor until even, at most
    members − 1, ties by the order they joined, so every device gets the same pairs offline; payments recorded only
    move the nets, and a group already at its fewest payments keeps its pairs (13.3c). Any member turns it on or off,
    offline too, at once and apart from Salvar ("aguardando envio" with the orange dot meanwhile); with 2 people it is
    off and disabled with the reason (13.1c), and a group already on can still turn it off. The switch opens its
    confirmation first: "7 pagamentos → 5 pagamentos" and what changes for each person (you first, a guest with
    "avise por Cobrar", the payments still in the queue, 13.3d), "Ligar mesmo assim" when it saves none; turning off
    names only what changes for you and counts the others (13.3b); "Como funciona" (13.2, `Route.SimplifyDebtsInfo`)
    explains it. It syncs with its own Last-Write-Wins stamp (`groups.simplify_debts_at`), apart from the row's
    `updatedAt`, so an edit of the name never undoes it nor it the name; when someone's later change beats this
    device's, the group's screen or Editar grupo, whichever is on top first, says "Ana desligou depois de você" once,
    with "Ver na Atividade" (the Atividade tab, 13.1e). Everything that reads the group's pairs follows it: Saldos
    (the card "Dívidas simplificadas" with Como funciona over the pairs, the icon on a pair no bill made, 13.4),
    "Seu saldo neste grupo", Acertar, Recebi, Já pagou, Membros' balances, Grupos' rows, Início's faces and "Quem
    pagou a quem?", the global Saldos and Contatos ("Dívidas simplificadas" on the group's line of "Por grupo", and
    Cobrar saying "em vez de pagar Bruno você paga direto para mim; o seu total não mudou", 13.6). Registrar pagamento
    says why a pair it made or grew is there ("Dívida simplificada: Diego paga a você o que devia a Bruno.", 13.5),
    and a pair that owes nothing only because the switch flipped while it was open stays with "Já paguei mesmo
    assim", which records what was owed (13.5c, a pair of one group). Atividade's `simplify_debts_changed` (Pessoas e
    grupos) says "7 pagamentos viraram 5. Agora Diego te paga R$ 64, e Ana não te deve mais." with "Ver mudanças" of
    what changed for you, from the pairs the device that changed it sent; whoever now pays or receives from someone
    else gets a push (13.7), never who changed it. "Editar conta"'s warning after a payment (7.4b) keeps the pairs as
    the bills make them.
  - "Seu saldo neste grupo" sits above the tabs of Despesas, Saldos and Membros while the group has a bill: "R$ 0 ·
    você está quitado" when settled, and its "Acertar" opens Registrar pagamento with whoever you owe the most, hidden
    when you owe nobody.
  - "Registrar pagamento" (`Route.RegisterPayment`) records a payment made outside the app, which any member may
    record between any two members, with no confirmation by whoever received it: up to the pair's debt, never over
    ("Ana te deve R$ 162,30. Use até esse valor."), partial payments allowed, Pix / Dinheiro / Transferência / Outro
    and "Quando" (date and time, never after now). "Acertar" opens it too, as "Pagar Ana" while Ana has a key (4.5,
    below). It is a bottom sheet on an Android phone
    (640 dp lying down, in two columns: who paid whom and the amount, then how, when and the buttons), a 560 dp
    dialog on its tablets and foldables upright and in a book's right half, and full screen on a tabletop (✕ and
    "Registrar pagamento" on top, who and how much above the hinge, how, when and the buttons under it, while Início's
    "Quem pagou a quem?" stays a dialog there, being short); a sheet on
    iOS (in two columns on an iPhone lying down, its confirm the bar's ✓; the 540×620 form sheet with an iPad's room,
    Início's "Quem pagou a quem?" too) and macOS, a dialog on the web (a drawer on a phone). It works
    offline. A debt with someone no longer in the group stays until it is paid: the form shows them as "Ex-membro"
    and closes only once the debt or the group is gone. A payment is never edited: "Pagamento de Ana registrado."
    offers "Desfazer", and the payment shows in Despesas as an "acerto", where tapping it asks "Apagar este acerto?" (a
    tombstone, offline too). Whoever paid or received it, if not who recorded it, gets a push notification (below).
  - "Pagar Ana" (4.5) is what Registrar pagamento shows, in the same sheet or dialog, when this device's user pays
    someone with an account who has a Pix or Bitcoin key: every "Acertar" (the group's Saldos, "Seu saldo neste
    grupo", the global Saldos' `SettleUp`, Início's faces, Contatos' profile), never "Recebi", "Já pagou" nor Início's
    "Quem pagou a quem?" (`Route.RegisterPayment.offersReceiverKeys`), so a guest, an ex-member or someone with no
    key gets the form as before. The keys of everyone the user shares a group with sync down with the groups
    (`member_payment_key`, read-only, replaced whole on each sync, cleared on sign out) under the server's
    co-member rule, so it works offline on what the last sync brought. It shows "Pagar Ana", where the debt comes
    from ("Apê 302 · você deve", "3 grupos · você deve") and the amount; Pix / Bitcoin only while Ana has both;
    "Chave padrão de Ana" whole with its type and "Copiar chave"; for Pix in BRL "Pix copia e cola", the default
    key's BR Code with the whole debt in it (`PixBrCode`, EMV with CRC16, city "BRASIL", the name folded to 25
    ASCII letters) and its QR code, with "Copiar código"; "Outras chaves de Ana" with a copy button each; "Pague no
    app do seu banco e volte para marcar como pago. Confira se o banco mostra o nome de Ana." (the wallet for
    Bitcoin), and "Depois" (closes) and "Marcar como pago", which records the whole debt now as Pix, or Outro for
    Bitcoin (one payment per group across groups), offline too; "Paguei outro valor ou de outro jeito" opens the
    form in its place for a part of the debt, another method or a past day (`RegisterPaymentUiState.prefersForm`),
    and a form that opened with no key stays one when a sync brings the first. No bank app opens by itself: Pix has
    no standard link for it. Android 13 and later show the system's copy confirmation (a "Copiado" snackbar before), Apple and the web a
    "Copiado" toast; Android keeps the copied key out of the clipboard's preview. A phone's sheet
    is one column, two lying down (who, how and the default key, then the code, the other keys and the buttons);
    the tablet's dialog (a book's right half) and the iPad's form sheet one column; an Android tabletop full screen
    as the form's, who, how and the default key above the hinge and the rest with the buttons under it; the Mac's a 440 pt sheet "Pagar Ana · R$ 98"
    whose keys scroll between the title and the buttons; the web's a 480 px dialog (a drawer on a phone).
  - The group's ⋮ menu (⋯ on iOS, macOS and the web) holds Editar grupo, Sair do grupo and Apagar grupo. "Editar
    grupo" (`Route.EditGroup`) has Novo grupo's fields, the icon being a shape and a color or a photo (6.4d): on
    Android and iOS the avatar or "Trocar ícone ou foto" opens "Ícone do grupo" (Android's bottom sheet: Forma, Cor,
    "Escolher foto", "Tirar foto" and "Usar este ícone", a 480 dp dialog where Editar grupo is one, in a book's right
    half; iOS's medium sheet: the colors as tiles, the photo's two
    sources and ✓), the shape and color picked there reaching the form only on "Usar este ícone"; the web ("Cor do
    ícone", "Usar uma foto" and, with a camera, "Tirar foto") and the Mac (the colors and "Escolher foto…", a file) pick the color in the form.
    Only Android draws and picks the shape (Biscoito, Pétala, Quadrado, Círculo, as the boards draw them, a group
    made before shapes as the old Quadrado); the Apple apps and the web keep every group in their rounded square.
    The photo is the profile's 512x512 JPEG framed in "Ajustar foto" (`PrepareProfilePhoto`), drawn in place of the color and initials in the
    group's shape, and picking a color or a shape takes it off. A new photo waits on the device and goes up with
    the group's next sync, offline too ("Uma foto nova sobe junto."), to the public `group-photos` bucket
    (`{groupId}/photo.jpg?v=<updated_at>`, which only members write); the shape and the photo are fields of the
    group row like the color, saved only when this person changed them, and Atividade says "Forma" and "Foto"
    ("Foto anterior", "Nova foto", "Sem foto"). The photo and the shape show wherever the group's own badge does:
    Grupos' rows and cards, Início, the search, Despesas' header, Rachar conta and "Rachar em qual grupo?", and the
    web's and the Mac's "Em aberto"; Convite (the invite, read before joining), Atividade's dot, "Quem pagou a
    quem?" and the groups listed under a person (Saldos, Contatos) keep the color. The name has 1 to 40 letters and
    the type stays editable. Moeda locks once the group has a bill on the device ("Não muda
    depois da primeira despesa"), with no server check. Any member may edit, offline too: the change holds on the
    device and syncs later, and only the fields this person changed are saved, so another member's newer edit to the
    rest stays. Salvar stays disabled until something changed. It is a sheet on iOS and macOS (a 540 pt form sheet
    with an iPad's room, the avatar beside "Trocar ícone ou foto" on an iPhone lying down) and a dialog on the web; on Android
    full screen on a phone (lying down a 240 dp column with the avatar, the fields beside it) and a 560 dp dialog
    anywhere wider, off the hinge as every dialog (`rememberDialogPlacement()`: a book's right half). Its last row,
    under Moeda, is "Divisão padrão" with the group's way ("Igual", "Partes"…).
  - "Divisão padrão" (6.7, `Route.DefaultSplit`, `DefaultSplitViewModel` in `:feature:edit_group`) opens over
    Editar grupo, which keeps what was typed: pushed inside its sheet on iOS and macOS (`AppModel.sheetPath`), which
    grows from the form sheet to a bill's with an iPad's room (820×640 lying down, 620×860 upright) and shrinks back
    once it is popped, in its dialog's place on the web (a drawer on a phone), and on Android full screen on a phone
    and a foldable upright, a dialog on a tablet (920 dp in two columns lying down, 560 dp upright) and in a book's right half in one column (off
    the hinge as every dialog). It sets
    how the group's new bills split: Igual (who is in it, everyone by default), Partes, Porcentagem
    (whole numbers adding up to 100, else "A soma dá 90%. Faltam 10%." and Salvar disabled) or Pela renda, whose
    incomes stay on the device that typed them (`split_group.splitIncomes`, never synced): the group keeps only the
    percentages they give (`IncomePercents`). A new bill's Dividir may also save its split as the default or its
    category's exception ("Usar como padrão do grupo", below in Rachar conta). "Numa conta de R$ 100: Pierre R$ 75 ·
    Ana R$ 25" with the bar in each one's color is always in sight, the cents as Rachar conta shares them (Igual's
    example is the first round bill everyone gets the same of, R$ 120 among six). It is one field of the group row (`groups.default_split`,
    `GroupDefaultSplit`), saved alone by its own Salvar (back goes to Editar grupo unsaved) and synced with the
    group's Last-Write-Wins, by any member, offline too, keeping the keys a newer app adds to it (this version
    writes only `mode`, `weights` and `exceptions` over the stored object); Igual with everyone in and no exception
    saves no default. Someone who joined after it was saved starts in, with 1 part or at 0% ("Carla entrou no grupo. Começa com 1 parte; ajuste se
    precisar.", until someone saves). Two columns lying down (the ways to split, the people with the example), in
    the Mac's sheet, an iPad lying down and the web from 1024 px. "Exceções por categoria" (6.7f) lists the categories
    that split their own way ("Nenhuma. Todas as categorias seguem a divisão acima."): "Adicionar exceção" opens
    "Exceção por categoria" (a bottom sheet on Android where Divisão padrão is full screen, a 480 dp dialog where it
    is a dialog, in a book's right half; a sheet on iOS and macOS, 540 pt with an iPad's room; in the dialog's place
    on the web),
    one main category or subcategory (each at most once) split by Igual (who is in it), Partes or Porcentagem (whole
    numbers adding up to 100), with its own example, saved with the rest of the default by Salvar
    (`GroupDefaultSplit.exceptions`, `ExceptionEditor`). A new bill of that category, or of a subcategory of it
    without its own, starts from the exception instead of the default (7.1k, `GroupDefaultSplit.exceptionFor`):
    "Divisão" says "Exceção do grupo para Mercado" and Dividir's notice "Exceção do grupo para Mercado: Igual" (7.2g),
    and changing the category moves the bill to the other way while Dividir was never changed by hand.
  - "Sair do grupo" and "Apagar grupo" (5.3c to 5.3f, 6.1i to 6.1k) need the internet: offline the ⋮ menu shows
    them disabled with "Precisa de internet", since only the server knows every device's bills and payments. The
    device syncs first, then the server checks the balances again (`leave_group`, `delete_group`). Leaving needs your
    own balance settled ("Acerte antes de sair" lists what is still open, with "Acertar" while you owe, "Ver saldos"
    otherwise); you go back to Grupos, and your member stays as a name in the bills and payments, not joined, so the
    invite link can bring you back. A group nobody else with an account is in goes, which "Sair" says
    (`ConfirmLeave.deletesGroup`). Any member may delete the group
    once nobody owes anything ("Ainda há saldo em aberto" lists the pairs, with "Ver saldos"), for everyone, with its
    bills and payments, after "Apagar “X”?" ("Apagar para todos", no undo). On the other devices a sync takes it off,
    even with a change not pushed yet (the server's `removed_groups`): the group open on it, whatever the tab, says
    "Grupo apagado" with who deleted it and "Voltar para Grupos" (`GroupExitState.deletedNotice`), once on top again
    if a bill covered it; any other screen of the group (Convidar) just closes, and Grupos or Início, whichever is
    on top first, says "Carla apagou o grupo X." (`GroupsUiState.deletedGroupNotice`, `HomeUiState.deletedGroupNotice`,
    one shared queue); never on the device that deleted it.
