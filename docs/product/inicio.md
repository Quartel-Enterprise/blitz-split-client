# Início

The home tab, its shortcuts and pinned groups.

  - "Início" (flow 10, `HomeViewModel` in `:feature:groups`), top to bottom: "Saldos com pessoas", a row of faces
    with each person's net across groups as Saldos nets it (`PeopleBalances`), whom you owe first, then who owes you,
    the largest first in each, hidden when nobody owes anybody; a face opens settling up for the net
    (`Route.SettleUp`, Pierre's call: it stays so after beta feedback 18), whose two faces open the profiles, and a
    guest (a name nobody joined as) gets a badge and, on a long press or hover, "Diego é convidado e não tem o app…".
    "Saldo geral" as Grupos' (big in the first currency with a net, a line for each other one with a net,
    `HomeSummary.main` and `others`), with "Te devem" and "Você deve", each in the first currency with something that
    way (`HomeSummary.toReceive`, `toPay`), which open Saldos filtered (Você deve straight to settling up while only
    one person is in it), disabled at zero. Three shortcuts: "Ler cupom fiscal", which opens Rachar conta as
    "Rachar conta" does (asking the group first) and its camera at once (7.1e, `Route.SplitBill.scansReceipt`;
    offline disabled with "Precisa de internet"),
    "Registrar pagamento", which asks "Quem pagou a quem?" first (`Route.PickPayment`: every open pair of every
    group, the most recent group first, pairs with you first; the pair opens Registrar pagamento in its place), and
    "Novo grupo". "Seus grupos": up to three open groups, the pinned first, then the latest activity, as Grupos draws
    them; "Ver todos" opens Grupos. "Últimas despesas": the five latest bills you paid or take part in, by day, with
    your share and its category's badge exactly as the group's Despesas rows draw it (Pierre's call, the category as
    it reads now, `HomeBill.category`); a bill opens its Conta over its group, and "Ver tudo" opens Atividade.
    "Rachar conta" is Grupos' (asks the group first, 7.1i; ⌘N on the Mac). "Buscar" is the home's (5.2), over
    Início. States: a shimmer until Room is read; no group yet shows only the first use's card (1.3), and only
    once the account's copy is whole; a new device's first download (an account restored on a device with no profile,
    or a sign in that switched to another user, until a sync settled, also across relaunches, `FirstDownload`,
    `AccountSnapshot.awaitsFullSync`)
    shows "Sincronizando… 3 de 7 grupos" with its bar filling (the server says first how many are coming,
    `first_download_groups`, then each group comes down whole, the most recent activity first; until it answered,
    and after a relaunch midway, "Sincronizando…" and a bar that runs), what already came with skeletons under it and
    "até agora · chegando o resto"; offline the skeletons stay with "Sem internet. Seus grupos aparecem quando
    reconectar." (Mac: the window's subtitle); the sync chip as Grupos'; an account with
    neither Google nor Apple shows "Guarde seu código de recuperação" once the code was snoozed ("Lembrar depois"), ✕
    hiding it until the next launch. A Flip's cover screen shows only Saldo geral, the latest bill and "Abra o celular para ver tudo".
  - "Fixar na Início" (the group's ⋮/⋯ menu on Despesas and Saldos, "Desafixar da Início" once pinned) is each
    user's own, synced with their account and never shown to other members (`group_pins`, `GroupPinRepository`), works
    offline, and only orders Início's "Seus grupos" (Grupos keeps its order). A settled group stays out of Início
    even when pinned.
