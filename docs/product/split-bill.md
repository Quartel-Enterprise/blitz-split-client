# Rachar conta

Splitting a bill: payers, Dividir, Itens, categories, Mapa de categorias, receipts and choosing the group.

  - "Rachar conta" (`Route.SplitBill`): "Quem pagou" picks one person, who paid the whole bill, or "Várias pessoas",
    which opens "Várias pessoas pagaram" in its place: each person's amount, adding up to the total exactly ("R$
    312 de R$ 312. Fechou.", else "Faltam R$ X" / "Passou R$ X"), and Pronto waits until it does
    (`SplitBillUiState.severalPayers`, `payerAmounts`). A new total keeps what each one paid, and Salvar waits until
    someone makes it add up again; one person holding the whole total is one payer again. "Quando"
    picks date and time, never after now. The form asks what it was before how much (Pierre's call, beta feedback
    10): "Descrição" with its category's icon and the line saying why, then "Valor", in every layout, a new bill
    opening on the description. A bill is 0.01 to 100 million in the group's currency with a
    description; Salvar stays disabled until both are valid and the split adds up, and saving goes back to the
    group's screen it was split from, or opens the bill's group's Despesas in its place. "Dividir" splits by Igual,
    Valores, % (whole numbers adding up to 100), Partes or Itens, and only changes the bill on "Pronto". A new bill starts
    from the group's "Divisão padrão" (Pela renda as its percentages) and follows it, and a sync's change of it,
    until "Pronto" picks its own (another group picked starts over from that one's); percentages someone who left
    took part of start Igual among everyone. Someone who joins while Dividir is open and still on the default gets
    the default's part (1 part, in Igual, 0%), so it stays "Padrão"; once changed there, they start as in any split
    (in Igual, out of the other modes). "Divisão" then says "Padrão do grupo", and Dividir's notice above the
    modes "Padrão do Apê 302: Partes, 3 para 1", or "Diferente do padrão do grupo" with "Voltar ao padrão" once
    changed for this bill (`SplitBillUiState.defaultSplit`, `isSplitEditorOffDefault`); "Editar conta" keeps its split
    with no notice. A new bill split by Igual, Partes or % in another way than that default (or, for a group without
    one, anything that doesn't split equally among everyone, Partes at 1 each included) offers "Usar como padrão do
    grupo" under Dividir's people (beta feedback 11, Pierre's call, `offersSplitAsDefault`; never Valores nor Itens,
    which are one bill's amounts, nor on "Editar conta"): "Pronto" with it checked makes "Divisão" say "Vira o padrão
    do grupo", and saving the bill saves its split as the group's "Divisão padrão" (`SaveSplitAsDefault`), keeping its
    exceptions, as Divisão padrão's Salvar does (offline too, Last-Write-Wins, a "Pela renda" replaced dropping this
    device's incomes). A bill whose category has an exception, even one that no longer adds up after someone in it
    left (`defaultSplitExceptionId`), offers "Usar como padrão do grupo para Mercado" and replaces only that exception
    ("Vira a exceção do grupo para Mercado"). Closing the bill unsaved changes nothing; another group or category
    picked (Pierre's call: another category falls under another default) or Itens' "Pronto" drops it, and the
    checkbox unchecks once it hides. The cents a split can't share evenly go to whoever paid, while they are in the split: one each
    to the payers in it, in the group's order, around again while some are left (one payer keeps them all).
    Changing the total redoes a "Valores" split proportionally. Payers and shares live inside the bill (one
    Last-Write-Wins row). Conta, Despesas and Dividir name up to two payers ("Ana e Bruno pagaram", the device's
    user first, `PayerOrder`) and count more ("3 pessoas pagaram"), and Dividir marks each of several payers "Pagou
    parte da conta"; Conta's "Sua parte" names whom the bill puts it on, as Saldos does
    (`GroupBalances.findDebtsWithin`). On a fold (flow 7) Rachar conta, Dividir, Itens and Editar conta open in a
    book's right half with the group under the scrim, "Quem pagou", "Várias pessoas pagaram", "Rachar em qual grupo?",
    Quando's date (in the book only) and every confirmation in that half too; a tabletop splits a new bill in two
    full screen (`TabletopForm`): the description and the amount, Dividir's modes or the items on top, the rest and Salvar or Pronto
    below, Item and Categoria opening above the hinge, while Editar conta and Conta open as the book's dialog. The
    iPad's Rachar conta is a bill sheet (820×640 lying down, 620×860 upright).
  - "Itens da conta" (7.5, `SplitBillUiState.itemsEditor`) is Dividir's fifth mode, "Itens": it opens in Dividir's
    place (pushed on iOS, the sheet's next page on the Mac, a 920 px dialog on the web, 640 px on a tablet, a drawer on
    a phone), and a bill already split by its items opens Dividir straight on it. Each item has a name (1 to 60
    letters), a quantity (1 to 999) and a price of each, and "Item" (7.6, `itemEditor`) splits it among who consumed
    it by Igual, Unidades (only above one unit), Partes, Valor or %, its own leftover cents to whoever paid while they
    had some (`BillItemsSplit`); its Pronto waits for a name, a price and someone, and while Unidades, Valor or % don't
    close it says what is missing next to whoever changed last (`ItemPersonLine.isError`). "Serviço" is a whole
    percentage of the items (10% to start, 0 for none) shared in proportion to what each one consumed, and the
    "Couvert" a value per person (none to start) for everyone in the group when the items were split
    (`BillItems.couvertMemberIds`); tapping either row opens its field. Pronto waits until every item has someone
    ("2 itens sem ninguém", in red) and the items, service and couvert close with the total ("Faltam R$ 15" or
    "Passou", "o cupom diz" for items a receipt gave), offering "Adicionar item de R$ 15" (a new item at that price)
    and "Dividir a diferença" (shared in proportion to what each one consumed, a "Diferença" row that can be taken
    off). Apagar item asks first, saying what is left to split once it goes. Back keeps the items typed for Itens to
    open with again until Dividir closes or another mode's Pronto. The bill keeps its items inside its row
    (`expenses.items`, stamped with the bill's `updated_at`) beside shares holding each one's total, stored as
    "amount": an older version reads it by Valores, and items another version saved the bill without are dropped,
    on the server too. Lying down and on tablets the status and what each one pays sit beside the items; a phone,
    lying down and the Duo show each one's total as a strip of chips, a tablet and the web as a list. A receipt read
    (7.1e) opens Itens with its items, nobody marked in any ("14 itens sem ninguém"), its service percentage and
    no couvert (a couvert printed is one of the items), checked against the total it read ("o cupom diz").
  - "Categoria" (7.7): every bill has one, the icon beside "Descrição" (a menu with a submenu per main category on
    the Mac). Until it is picked by hand, the description's words choose it (the 60 default terms of flow 9, whole
    words ignoring case and accents, the longest term winning, ties to the first in the text, `CategoryMatcher`), else
    Outros, and the line under the description says why ("Alimentação › Mercado pela palavra “mercado”…", "3
    palavras do mapa casaram…", "Nenhuma palavra…", "Você escolheu… Ao salvar, perguntamos se o mapa aprende."). The
    words are the person's map (flow 9) once it has changes, and a card "Mapa de categorias" between Sugeridas and
    Todas as categorias names the word that chose and opens the map over the bill (a menu item opening Ajustes on the
    Mac). The picker (pushed on iOS, in the form's
    place on Android and the web) has "Buscar categoria" (by name, accents ignored, "Criar “Pet”" when nothing is
    found), up to four "Sugeridas" (the description's, the group's most used, the group's own) and every main
    category opening its subcategories ("Só Alimentação" keeps the main one; beside the list lying down and on
    tablets); Outros is picked at once. A bill saved as Outros without a pick stores no category. The app's
    categories (10 main ones and their subcategories, `AppCategory`) are named by each platform; Despesas' rows and
    Conta show the category's badge in its main category's color, Conta also "Alimentação › Mercado".
  - "Nova categoria" (7.8, `GroupCategory`, the group's `group_categories`): anyone in the group may create, rename,
    move and delete the group's own categories, offline too (the change waits in the queue, Last-Write-Wins per
    row). A name has 1 to 30 letters and can't repeat inside the same main category, the app's names included; a
    main category picks one of 18 icons, a subcategory takes its parent's; one with subcategories stays a main one.
    Editing (a long press or a right click on the group's category, "Editar “X”…" on the Mac) says how many bills
    use it; deleting asks "Apagar “X”?" saying where its bills go: they move to its parent, or Outros, at read time,
    since a deleted category stays as a tombstone (`CategoryTree.resolve`), its subcategories going with it. A new
    category is picked for the bill at once. Rachar conta's Categoria and Mapa de categorias open the same form
    (`CategoryForm`, `CategoryEditor` in `:core:expenses`).
  - "Mapa de categorias" (flow 9, `Route.CategoryMap`, `:feature:category_map`): each person's map of words to
    categories, valid in every group, on the device and offline first (`category_term`, the server's
    `category_terms`), synced to the person's other devices under their account and cleared on sign out; while the
    account has no Google or Apple it says "Seu mapa fica só neste aparelho" with "Abrir Perfil". It starts as the
    app's 60 terms (`DefaultCategoryTerms`) and stores only the person's changes over them (`CategoryMap`): a term
    added or moved (one category per term, so a word another category has turns Salvar into "Mover para X"), a
    term removed (no confirmation, "“café” saiu do mapa" with Desfazer), and "Restaurar padrão" (⋮/⋯), which brings
    the 60 back as they came and keeps the person's own terms. A term may send titles to a group's category, which
    counts only in that group's bills ("só no Viagem Floripa"); the map's categories are each group's tree merged
    (`CategoryTree.ofGroups`), so a group's category has the color it has in its group. A term of a deleted group
    category reads as that category's parent, or Outros, as its bills do (`CategoryTree.resolve`), and shows, edits
    and removes there; a term of a category of a group no longer on the device isn't shown, nor counted for the empty
    map or "Mover para X" (`CategoryMapUiState.terms`), though its change waiting to go up still counts in the queue.
    Terms have 2 to 30 letters, digits and spaces.
    The map also creates, edits and deletes categories (9.4, Pierre's call, beta feedback 19), always a group's
    (`GroupCategory`, "Nova categoria"'s rules, offline and synced to the group's members), since there are no personal
    categories: "Nova categoria" in the ⋮/⋯ menu (the Mac's "Nova Categoria…" beside Restaurar padrão) and "Nova
    subcategoria em Transporte" in every main category but Outros, only once there is a group on the device
    (`CategoryMapUiState.canCreateCategory`). A new subcategory of a group's category goes in that group; anything else
    starts in the group with the latest activity, and "Grupo" changes it while there are two or more
    (`canPickCategoryGroup`), never while editing; its name and "Dentro de" are checked against that group alone
    (`CategoryTree.ofGroup`). Saving a new one opens "Novo termo" in it at once (Cancelar leaves it without terms), and
    a group's subcategory shows in its card even with no term yet. A group's category is edited from its pencil (the
    Mac's "Categorias" menu under the table), with Apagar there, which waits until its bills are read; the app's
    categories stay as they are.
    "Buscar termo" finds words starting with what is typed, accents ignored ("2 termos com “uber”"), else offers
    "Adicionar “pizzaria”"; an empty map offers Adicionar termo and Restaurar padrão; offline with changes queued it
    says "Offline · 2 alterações na fila" and each queued term has the orange dot. It opens from Perfil ›
    Preferências › Mapa de categorias (Ajustes › Categorias on the Mac, ⌘,) and from Categoria (9.0). A phone shows
    one card per main category with its terms by subcategory; lying down, a tablet lying down (in 920 dp), a book
    (split on the hinge), an iPad lying down, an iPhone Duo open and the web from 768 px list the categories beside
    the picked one's terms; a tablet or a foldable upright one 560 dp column. "Novo termo" and "Editar termo" (9.2)
    are a bottom sheet on an Android phone (640 dp lying down), a 560 dp dialog on its tablets and the book's right
    half, a sheet on iOS (a form sheet with an iPad's room) and the Mac, a Dialog on the web (a Drawer on a phone).
    On the Mac the map is Categorias, the last section of Ajustes' sidebar (the profile's Settings window, never a
    second one), drawn in the form's place: the categories, a table of terms with + and −, and the search; whatever
    opens the map there (Perfil's row, the bill's "Mapa de categorias…") opens Ajustes on Categorias, and whatever
    opens the profile on the profile.
  - "Ensinar ao mapa?" (9.3e, 9.3f): once a new bill is saved in a category picked by hand that the map wouldn't
    pick (never Outros), the group's Despesas asks "Conta salva em Lazer › Esportes, e não em Casa › Aluguel, que o
    mapa sugeriu. Ensinar ao mapa?" once (`CategoryTeachOffer`, only while the app runs), only when saving lands on
    that group's Despesas: a bill saved back on Saldos, Membros or Estatísticas asks nothing, then or later (the
    group's ViewModel drops the offer while another tab is picked). A snackbar on Android, a glass banner on Apple,
    a toast on the web (swiping it away is Agora não), with Ensinar and Agora não. Ensinar asks which word of the
    title ("Nova no mapa…", "Sai de Casa › Aluguel…"; words under 3 letters only when the map has them) and saves it
    in the map; where a word goes now is the whole map's, every group's categories as Mapa de categorias reads them,
    so a word of another group's category says "Sai de Praia" and teaching moves it (one category per term). The
    bills already saved don't change.
  - "Ler cupom fiscal" (7.1e to 7.1g, `ReadReceipt`, the server's `read-receipt`): a new bill's form offers it under
    "Divisão" (never on "Editar conta", nor on an Android tabletop nor an iPhone Duo open upright, as the boards
    leave it out there; Início's shortcut still opens it), online only ("Precisa de internet. Preencha à mão ou leia
    o cupom quando reconectar."). Android and iOS open their own camera, black, with the receipt's frame, the flash,
    the gallery and the shutter; the Mac picks a file, and the web opens the browser's camera (a webcam too) with "Escolher arquivo" in it, a file
    chooser where the browser has no camera. The
    photo goes up shrunk as a bill's photo is (`BillImageEncoder`) and the server reads it with Claude, whatever
    the receipt (an NFC-e, a restaurant's bill, a card slip, one from abroad), for each account up to 30 reads a day,
    a failed one included; the photo is never kept there. While it reads, "Foto do cupom" says "Lendo o cupom…",
    Salvar waits and every other field of the form shows disabled, so nothing typed is overwritten (Pierre's call,
    beta feedback 17, `SplitBillUiState.isFormLocked`: descrição, categoria, valor, grupo, quem pagou, quando and
    divisão; a sheet or picker left open closes as the read starts), while × on "Foto do cupom" still cancels it. Once read it fills the total, the description (the place)
    and Quando (the day printed, with its time or the form's, never after now), shows "Lido do cupom" and "14 itens" under the description, and its items
    wait for Dividir's Itens, which "14 itens" opens straight away (Pierre's call, `OnReceiptItemsClicked`: Dividir
    under it, so back goes there; the Mac's count is a link); the split stays the group's default until Itens or another mode is picked. Its category
    is one of the app's or of the group's own, which go with the photo with the person's map terms counting in that
    group (a term of another group's category or of Outros stays out, one of a deleted category as where its bills
    go, `ReceiptCategoryHints`), so "ração" among the items can choose the group's "Pet", which reads as where its
    bills go once a sync deletes it; it counts only while the
    description's words choose none and none was picked: "Alimentação › Restaurante pelo cupom fiscal", and "Pelo
    cupom" in the picker's Sugeridas. A receipt that can't be read (no receipt in it, no total, a
    photo the camera failed to take) says "Não deu para ler o cupom. Tente de novo com mais luz e o cupom inteiro na
    foto, ou preencha à mão." with "Tirar outra foto"; what isn't the photo's fault says so apart (Pierre's call,
    `SplitBillUiState.receiptError`): the reader that didn't answer or isn't set up "O leitor de cupom não respondeu.
    Não é a sua foto…" and a connection that dropped "Sem conexão. A foto não chegou ao leitor…", both with "Tentar de
    novo", which sends the same photo again, and the day's 30 reads "Você já leu 30 cupons nas últimas 24 horas…" with
    no button. Each leaves the fields as they were and keeps no photo (it may be no receipt at all). × on "Foto do cupom" (also "Todo mundo do grupo vê") drops the photo and the receipt's
    items, keeping what it filled. Saving attaches the photo read, as it was read, as the bill's receipt (Conta's
    "Anexos", badge "Cupom").
  - "Rachar em qual grupo?" (7.1i): the home's "Rachar conta" opens Rachar conta for the group with the latest
    activity (the home's first row) and, while there are two groups or more, asks "Rachar em qual grupo?" over it ("O
    último que você usou vem marcado", `Route.SplitBill.picksGroupFirst`): every group on the device, settled ones
    included, the most recent activity first as on the home (`ObserveGroupsByActivity`), each with "6 pessoas", one
    marked until "Continuar" (`SplitBillUiState.groupSelection`); closing it keeps the marked group's bill. With a
    single group it goes straight to the bill, and a sync bringing a second one never asks later. A new bill's group
    reopens it from the form on every platform, once there is another group (`SplitBillUiState.canChangeGroup`):
    Android's chip, iOS's "Grupo" row, the web's "Grupo" field and a pop-up button on the Mac, which picks in one go
    (the Mac's sheet only asks from the home); "Editar conta" keeps its group. Another group keeps what was typed
    (valor, descrição, quando), but who paid and the split start over with its people: you pay, Igual among everyone.
    A sync deleting the group the bill was moved to sends it back to the group it was opened for, the same way, and
    closes Rachar conta only once that one is gone too.
    A bottom sheet on Android, a sheet on iOS and the Mac, a dialog over the bill on the web (in its place on a
    phone's drawer). Saving a bill from the home, or moved to a group other than the one it was split from, opens
    that group's Despesas in Rachar conta's place.
