# The group screen and its bills

Despesas do grupo, Conta, comments, attachments, sharing, Estatísticas, editing and deleting a bill.

  - A group tapped on the home opens "Despesas do grupo" (`Route.GroupDetail`): the bills by day, the most recent on
    top, with "Rachar conta" (Android's extended FAB, iOS's glass toolbar item, a primary button on macOS and the
    web). Its tabs Despesas / Saldos / Membros are one screen (`Route.GroupDetail.tab` only says which it opens on):
    the header, "Seu saldo neste grupo" and the tabs stay, only what is under them changes, with no transition nor
    check on the tab picked (`GroupDetailUiState.tab`), the back stack never moves, and back from any tab leaves the
    group. Convidar's "Ir para o grupo" and "Ver quem já entrou" open it in Convidar's place (`openGroup`), so the
    group never stacks over itself.
  - A bill tapped there opens "Conta" (`Route.BillDetail`): the bill, "Sua parte" and the split over every member,
    with its category over the title and Compartilhar, Editar and Apagar in the top bar. A phone opens it full screen; Android tablets open it as a dialog (a book's right half, a tabletop's
    half above its hinge), the iPad as a sheet (820×640 lying down, 620×860 upright, `billSheetSizing`), the Mac as the
    main window's inspector (720 pt, 330 pt in a window under 1180 pt, the bill beside its split from 600 pt, ✕ and Esc
    closing it, another bill or whatever the group opens taking its place), and the web as a side sheet (a drawer on a
    phone), all over the group.
  - Conta's "Comentários" (7.3c to 7.3f): anyone in the group comments, offline too (the comment waits in the queue,
    "Na fila · sai ao reconectar", like a bill, `bill_comments`), and "Responder" answers a comment or a reply, one
    level deep: a reply to a reply joins its thread and names whom it answers. "Enviar comentário" stays disabled
    until something is typed (1 to 1000 letters); the composer is pinned at the bottom on a phone and under the
    comments in two columns. Offline, a banner says "Sem internet. Comentários e fotos ficam na fila e saem ao
    reconectar.". Each comment (beta feedback 14, Pierre's call) has a heart and a menu (⋮/⋯, the row's long press
    and right click): the heart likes it or undoes the like, one per person, offline too through the queue
    (`bill_comment_likes`, the newer toggle wins), with the count beside it, which opens "Curtidas" (who liked it, in
    the order they did); "Compartilhar" sends "Ana comentou em “Jantar”: “…”" with the comment's
    link through the system share, offline too; "Editar" and "Apagar" only on the user's own. Editar puts the text in
    the composer ("Editando comentário", ✕ gives back what was typed, "Salvar"), and an edited comment says "editado"
    after its time, with no history; Apagar asks "Apagar comentário?" first. Both work offline through the queue like
    a new comment: the newer edit wins on the server (`edited_at`), a deletion is a tombstone that wins over any edit,
    and only its author may do either (the server's RLS). A deleted comment with replies stays as "Comentário apagado"
    over them; one without replies, and a deleted reply, go. Edits, deletions and likes record no Atividade event and
    push nothing: Comentários' row shows the text as edited, and "Ana apagou um comentário em Jantar" once deleted. A
    comment's link, `https://blitzsplit.com/b/<bill>?comentario=<comment>` (`BillCommentLink`), which the apps claim
    (App Links, Universal Links) and the web opens, opens the bill's Conta over its group's Despesas, over Grupos,
    scrolled to the comment and highlighted, after a sync of a few seconds as a push notification's tap
    (`Route.BillDetail.focusCommentId`); a deleted comment opens Conta with "Esse comentário foi apagado", a deleted
    bill its group's Despesas with "Esta conta foi apagada", and someone outside the group (or without a profile)
    gets "Você não está neste grupo" with "Ir para o início" (offline: "Sem internet", `BillLinkNotice`). A comment's
    tap in Atividade scrolls Conta to it too. "Histórico" lists what was done to the bill, by day,
    the most recent first, never the comments: created ("Igual entre 5 · R$ 312", "criou a conta sem internet"
    while it waits to go up), each edit's first change with the others under it ("trocou a categoria", "mudou a
    divisão" with each part that moved), photos added in a row as one line, a photo removed and the share link
    changed. It reads the device's 90 days of Atividade and, online, the bill's whole history from the server
    (`LoadBillHistory`). "Desfazer" shows only on the latest line when it is an edit, the bill is still as that edit
    left it, isn't waiting to go up nor split by items, and saves the edit's "before" as a new edit
    (`BillHistoryEntry.canUndo`).
  - Conta's "Anexos" (7.3g): any member adds up to 5 photos to a bill (the camera, the gallery, the Mac's files,
    the web's files or its camera), offline too ("Na fila" until the photo is up, drawn from the device meanwhile), and removes any of
    them from the viewer ("Remover foto", no question, as the boards draw none). "Anexos · 2 de 5 fotos" lists the
    receipt the bill was read from first (badge "Cupom", never removed from Conta; reading it is 7.1e), then the photos
    as "Foto 2" with who added it and when, since the server keeps no caption, then "Adicionar foto" while there is
    room. The viewer pages through them full screen ("Foto 1 de 2", "Ana anexou ontem às 22:40") with Baixar (the
    gallery on Android, the share sheet on iOS, a file on the Mac and the web). Each photo is an upright JPEG of at most
    1600 px under 1.5 MB (`BillImageEncoder`), in the private `bill-attachments` bucket, read through signed URLs, so a
    photo added on another device shows only online. Histórico says who added or removed them; the feed doesn't.
  - Conta's "Compartilhar conta" (7.3h, `Route.ShareBill`, `ShareBillViewModel`): the bill's link,
    `https://blitzsplit.com/c/<code>`, one per bill, whose code the server makes (`set_bill_link`). Opening it makes
    the link as "Só quem está na conta" when there is none (the safer of the two, which Histórico records as the link
    turned on); "Quem pode abrir" switches it to "Qualquer pessoa com o link" and back, keeping the code, and
    "Desativar link" turns it off with no question and closes. Online only: offline it says "Compartilhar precisa de
    internet." while Copiar and "Enviar link" (the system share) still work once the link is known; a bill not up yet
    fails with "Tentar de novo". Laid out as Contatos' "Convidar com link ou QR": a bottom sheet on an Android phone,
    a 560 dp dialog on its tablets (a book's right half), a sheet on iOS (a 540 pt form sheet with an iPad's room) and
    the Mac, a 480 px dialog on the web (a drawer on a phone). The link opens only on the web (`Route.SharedBill`,
    page 7.3h), since the apps claim only `/g/` and `/b/` links: the bill, who paid, each one's part, the attachments and the
    comments, never the balances nor the group's other bills, under "Esta conta é do grupo X" (the server doesn't
    say who shared it, and a group's own category shows as Outros). "Só quem está na conta" opens it to the group's
    members signed in; "Qualquer pessoa" to anyone, and any device with an account (an anonymous one too) comments
    there, in the group or not, online only, and likes, shares (the page's link at the comment, `/c/<code>?comentario=`,
    which scrolls to it) and edits and deletes their own comments there, online only. "Entrar" goes through the app's own sign in (Boas-vindas without a
    profile, else Perfil) and the link opens again when the browser's Google or Apple sign in comes back within 15
    minutes, or once Boas-vindas makes the profile on the page ("Usar sem conta"). A link only the group opens
    offers "Entrar" until the device has a Google or Apple account; a member with the bill on the device gets "Abrir
    no app".
  - "Estatísticas" (6.6, `Route.GroupStats`) opens from the chart button on the group's bar (Despesas and Saldos;
    "Estatísticas" with text on the web from the tablet up), with Back and no "Rachar conta". It counts only the bills
    on the device, those in the queue too ("7 despesas, 1 na fila", the orange dot), never the payments of Saldos
    (but in "Acertos"), so it works offline. Períodos (only this screen keeps one): a trip lists "Toda a viagem" (the default, from its first
    to its last day with a bill), "Este mês", "Mês passado" and "Escolher datas…"; any other group "Este ano", "Este
    mês", "Mês passado", "Todo o período" (the default, as the notes say) and "Escolher datas…" (each platform's date
    range picker, never after today). Total gasto, how many bills, "Média por pessoa" (the total over every member,
    who hasn't joined included), "Sua parte" and "Você pagou"; "Por pessoa" shows Consumo (each part as each bill
    split it) or Pagou, the largest first, someone who left the group as "Ex-membro". "Acertos" (Pierre's call, beta
    feedback 12, `GroupStatsUiState.settling`, `SettlingCalculator`) names under it who "Acerta mais rápido" and who
    "Demora mais para acertar" ("em média 2 dias", "no mesmo dia", "R$ 80 em aberto"): each member's bills and
    payments of the group's whole history, the queue's too, go in date order, what a bill puts on them opening a
    debt on its "Quando" and what they pay (a payment, or a later bill they paid more of than their part) covering
    the oldest first, a debt still open counting up to today; only debts born of the period's bills count, the amount
    weighting the average, whoever is still in the group (guests too, never an ex-member), and only once two people
    had one and the two named don't tie (the same days and the same open amount hide the card); whoever covered all of
    the period's debts ranks before whoever still has some open, so a part put on someone today never reads as fast. "Ao longo do tempo" is Barras or
    Linha by Dia, Semana (Monday to Sunday), Mês or Ano, kept per device for every group and never synced
    (`StatsChartPreferences` in `device_settings`): Dia only up to 3 months, the others once the period touches two of
    them, Ano once the group's bills touch two years, and picking it switches to the whole period; a grain kept that
    doesn't fit opens on Dia (up to 3 months) or Mês, the disabled ones say why under the chart (`StatsGrainNote`).
    Then the most expensive one, and "Maior despesa", which opens its Conta. A group without bills says "Nada para
    mostrar ainda" with "Rachar conta"; a period without bills offers "Ver toda a viagem". "Por categoria" draws the
    period's spending as a ring in each main category's fixed color, the largest in its center, then each main
    category with its amount, its bar and its subcategories' amounts (`StatsCategoryRow`, whole percentages adding up
    to 100), a bill without a category counted in Outros and one of a deleted category in its parent or Outros.
    While a new device's first download still brings bills down it counts what came, as "Total gasto até agora"
    (5.1d, `GroupStatsUiState.isFirstDownload`). A phone, a tablet and a foldable upright
    scroll one column; lying down (a phone, a tablet, in the group's pane beside the groups too) the total and "Por
    categoria" take the first column and the rest the second: on Android each scrolling on its own, "Ao longo do
    tempo" on top of the second, then "Por pessoa", "Acertos" and "Maior despesa"; on iOS both in one scroll, "Por
    pessoa" and "Acertos" first, as each board draws it. A book puts the same two columns one per half of the hinge; a tabletop (Android)
    a small bar with the period, the total and "Ao longo do tempo" side by side in the top half, against the hinge,
    and "Por categoria" beside "Por pessoa" and "Acertos" in the bottom half, scrolling, without the grains' note nor
    "Maior despesa". The web from the tablet up and the Mac put "Ao longo do tempo" and "Maior despesa" in a second
    column, with four summary cards, "Acertos" under "Por pessoa".
  - Any member may edit or delete a bill. "Editar conta" is "Rachar conta" filled in (`Route.SplitBill` with the
    bill's id), keeping who created it and when, with "Qualquer pessoa do grupo pode editar ou apagar." and "Apagar
    conta"; saving goes back to Conta. Members who left the group are dropped from the edited split, but a payer who
    left stays its payer ("Ex-membro") until someone else is picked, and someone who joined while it was being
    edited starts out of an Igual split. Last-Write-Wins:
    when someone else's save syncs while editing, a banner says who changed it and when
    (`SplitBillUiState.conflict`, the server's `expenses.updated_by`), what was typed stays, and saving wins.
  - Apagar asks first: "“X” sai do grupo e a parte de cada um nesta conta deixa de contar.", plus "Você deixa de
    dever/receber R$ Y" while the bill moves the device user's balance (`BillDeletion`). It deletes offline as a
    tombstone and goes back to the group, past Conta. A bill someone else deleted while open closes, and Despesas
    do grupo says "Esta conta foi apagada" once it is on top (`GroupDetailUiState.showsBillDeletedNotice`); never
    for a bill this device deleted (`NoticeBillGone`).
  - Editing or deleting a bill after a payment (7.4b) is allowed: the payment still counts and the balance is redone.
    While the change typed in "Editar conta" moves the balance of a pair with a payment dated at or after the
    "Quando" being typed (the saved one for "Apagar"), the form says so before saving, live as it is typed, so moving
    Quando past a payment or before it changes what it says: "Elisa já acertou com Bruno depois desta conta. O acerto
    continua valendo e o saldo é refeito: com a mudança, Elisa volta a dever R$ 20 a Bruno." ("passa a dever" the
    other way round or for more of what was still owed, "fica devendo R$ 30 a Bruno" for less of it, the same way
    round, "ficam quitados" when they end up even; "você fica devendo R$ X a Bruno" and "Bruno fica te devendo R$ X"
    with this device's user), naming only the latest such payment (`SettledPairChange`, the group's `GroupBalances`
    with the saved bill and with the change). It takes the place of "Qualquer pessoa do grupo pode editar ou
    apagar.", as a card above "Apagar conta" on Android and a section above it on iOS, at the top of the form on the
    Mac and the web; a screen reader says it when it shows and when the pair or how it ends up changes, never on each
    digit of the amount (`SettledPairChange.announcementKey`). Salvar saves at once, with no extra question. "Apagar
    esta conta?" ends with the same sentence, from Conta too (`BillDeletion.settledPair`). A payment dated before the
    bill, or one whose pair the change doesn't move, says nothing.
