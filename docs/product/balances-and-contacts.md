# Saldos, Contatos and people

The global balances, a person's profile and Contatos.

  - "Saldos" (flow 8, `Route.GlobalBalances` over the groups) is the home's second tab: each person once, netted
    across every group of one currency (`PeopleBalances`): someone who joined is their account in every group, a name
    nobody joined as is the same person wherever it is written the same, and someone no longer in a group only
    themselves ("Ex-membro"); someone with groups in two currencies shows once per currency. Tudo / Você deve / Te
    devem filter it, each with its total (the net always matches the home's Saldo geral; "A receber" and "A pagar"
    there count each group's net, so they can differ), "Por grupo" opens in the row (already open for someone alone
    in Você deve or Te devem), and the empty "Você não deve nada" leads to Te devem, "Ninguém te deve" to Você deve and
    "Tudo acertado" to Grupos. "Acertar" (you owe, as "Pagar Ana" while Ana has a key) and "Recebi" (owed to you)
    open Registrar pagamento for the net (`Route.SettleUp`): one payment in each group, the way that group's pair owes, all or none (`SettleUp`), the
    amount locked while more than one group is in it, then "Pagamento de Carla registrado." on Saldos, whose Desfazer
    undoes them all in one go (never on a later Saldos, when the one it opened over closed while saving). "Cobrar"
    moves no money: each platform writes the message ("ficou R$ 32,90 dos grupos …", a guest's bills and what they already paid while those numbers add up, the default Pix key while "Incluir minha
    chave Pix" is on, which locks once the message is edited), "Enviar" opens the system share (a ShareLink on Apple,
    `navigator.share` or a copy on the web) and "Copiar mensagem" copies; a bottom sheet on an Android phone and a
    560 dp dialog on its tablets, a book's right half and a tabletop's top half (its right half above the hinge lying
    down) with the people under the scrim, a
    sheet on iOS (the whole screen on an iPhone lying down, whom and the message beside the Pix key and the buttons;
    a 540 pt form sheet in the middle with an iPad's room) and macOS, a 480 px dialog on the web (a drawer on a phone). It all
    works offline on the device's data, and has no "Rachar conta". An Android tablet lying down, an unfolded book, an
    iPad lying down, the web's desktop (from 1024 px) and a Mac window wide enough for the inspector list the people
    beside the one picked (`GlobalBalancesUiState.selectedPerson`, the first listed until a row is picked or once the
    filter hides the pick): their groups by name, "Carla te deve R$ 32,90" with the actions, "Por grupo" (a table on
    the web) and "O líquido já compensa os grupos: Cobrar e Recebi usam R$ 32,90. O app anota um pagamento em cada
    grupo." ("Só um grupo em aberto com Ana." for one group); the empty states take that pane. Everywhere else one
    column with "Por grupo" in the row, the filter beside the list on a phone lying down (on an iPhone, Saldo geral
    and the filter stay still in a 300 pt column on the left while the people scroll on the right).
  - A person tapped wherever a bill, a list or the feed shows them opens their profile, as Contatos opens it (Pierre's
    call, beta feedback 18, `Route.ContactProfile` by Saldos' person key, `openPersonProfile`): Conta's split rows,
    its lone payer ("Ana pagou"; several payers each have their row), a comment's author and a Histórico line's
    face, Membros' rows, Saldos' people, Atividade's actor face (the rest of the row still opens the item; VoiceOver
    has it as the row's action), Estatísticas' "Por pessoa", and Registrar pagamento's two faces (whoever paid →
    whoever received, and "Pagar Ana"'s, `RegisterPaymentUiEvent.OnPersonClicked`), over the form, which back finds
    again. This device's own user opens Perfil (the Mac's Ajustes); someone no longer in the group opens nothing.
    Início's faces, the group's Saldos pairs and Despesas' rows stay as they were (whatever opened `Route.SettleUp`
    still does, Pierre's call). What Apple draws outside the window's stack (Registrar pagamento's sheet, Conta with an
    iPad's room or in the Mac's inspector) closes while the profile shows and opens again on back, Registrar pagamento starting over
    there (the whole debt, Pix, now, "Pagar Ana" again); Android keeps it as it was, and the web keeps Conta and
    Registrar pagamento mounted under the profile, whose back says "Voltar" (the tab's name only right above a tab).
    A profile opened from settling up goes back to that form on its Acertar or Recebi rather than opening another;
    opened from one group's Registrar pagamento, they open settling up for the net over it (Pierre's call).
  - "Contatos" (flow 11, `Route.Contacts`) is the home's third tab: everyone the user shares a group with, guests
    included, once (Saldos' person key, `PeopleBalances.keyOf`), A to Z ignoring case and accents, each with their
    net in the first currency they have one ("+R$ 32,90 te deve", "R$ 0 · quitado") and "3 grupos" or the group's
    name, under the card "Você deve / Te devem / Ver Saldos" (Saldos' totals). It all works offline on the device's
    data ("Offline · contatos deste aparelho"). Its magnifier opens the home's "Buscar" (5.2) over Contatos (Pierre's
    call: the people are in its Pessoas), and the agenda is searched only in "Convidar da agenda". "Ocultos" holds whoever
    was hidden ("Ocultar de Contatos", only while the balance with them is zero, asked first), synced to the user's
    other devices (`hidden_contact`, the server's `hidden_contacts`), with "Mostrar"; someone hidden comes back by
    themselves once a bill shared with them is saved later or the balance opens again. The profile
    (`Route.ContactProfile`) shows the net ("Já compensado entre 3 grupos"), Cobrar (Saldos' charge, opened over
    Saldos for that person), Recebi or Acertar (Registrar pagamento for the net, `Route.SettleUp`), "Adicionar a um
    grupo" (an account goes straight in, joined, with no part in the old bills; a guest as a name; a group they're
    in shows disabled), "Grupos em comum" and the latest four of "Histórico com Carla" (bills that move something
    between the two and their payments); "Ver tudo" opens Atividade on that person. A guest's profile
    offers "Convidar Diego" (their personal link) and "Juntar com uma conta" (online only): the server makes a
    request and a single use personal link, and once the guest joins through it everything in that name, in every
    group, passes to their account ("Aguardando Diego aceitar", Cancelar); "Pedir para quem já usa o app" answers
    "Em breve". "Convidar da agenda" (`Route.ContactsAgenda`, Android and Apple; the web says the browser can't read
    the agenda) explains before the system asks, leads to the settings once refused, and lists the agenda (only
    the contacts picked under iOS's limited access); no phone number leaves the device, so nobody is shown as
    already using the app. Every invite is a group's link: "Convidar com link ou QR" (`Route.ContactInvite`) picks
    the group (chips, the most recent first, and "Novo grupo") and shows its QR and link, offline too once the
    group has it. Wide windows (the same as Saldos') list the people beside the one picked
    (`ContactsUiState.selectedKey`) or the agenda; the invite is a bottom sheet on an Android phone, a 560 dp dialog
    on its tablets (the book's right half), a sheet on iOS and the Mac (540 pt on the iPad), a 480 px dialog on the
    web (a drawer on a phone).
