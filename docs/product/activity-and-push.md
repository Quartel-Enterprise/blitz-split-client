# Atividade and push notifications

The feed, unread state, real time and push notifications.

  - "Atividade" (page 12, `Route.Activity` right above Início like Saldos, `ActivityViewModel`) is every group's feed and
    the notification center: who split, edited or deleted a bill, recorded or undid a payment, created, edited or
    deleted a group, changed its invite link, added, joined, left, was removed, released or renamed in it, and the
    charges sent from Saldos, by day (Hoje, Ontem, the date), the most recent first. The server writes the events
    (`activity_events`, by triggers); each keeps the names it had, so it still reads once its bill, group or person is
    gone. Unread is someone else's event never opened and after the last "Marcar tudo como lido": a blue dot and bold
    names, counted on the tab's badge in the primary color (`activityUnreadCount`), whatever the filters. Your own
    events are born read, opening the tab marks nothing, tapping an item marks only it. A tap opens what it is about
    (the bill's Conta over its Despesas, the group, its Membros, its Saldos for a charge); a deleted bill or group
    keeps its row with the trash and says who deleted it and when instead. A bill's edit shows its first change,
    "mais 2 mudanças" and "Ver mudanças". "Filtrar atividade": one group or all, and the types (Contas, Pagamentos e
    acertos, Comentários, Pessoas e grupos, Cobranças), as removable chips with "Limpar"; a contact's history opens it
    on that person (`Route.Activity.personKey`), a chip too. Offline it shows the device's last 90 days, the changes
    not up yet on top of Hoje as "Aguardando envio", and says older ones load when the internet is back; a device that
    never synced says only what was done on it shows. A tablet lying down, an unfolded book, an iPad lying down, the
    web's desktop and the Mac's inspector show the item picked beside the list ("O que mudou", "Mais em" the bill's
    other events). Comentários lists the comments on bills ("Carla comentou em Jantar", the text under it as edited,
    "Carla apagou um comentário em Jantar" once deleted), which open the bill's Conta at the comment; a bill's attachments and its share link show only in its Conta's Histórico, never in the
    feed nor its badge (`ActivityKind.isInFeed`).
  - Real time (beta feedback 13, Pierre's call): while the app is in front and online, the other members' changes
    show on the device right away, with no sync to wait for: someone joining, leaving, added, removed or released, a
    group renamed or edited, a bill created, edited or deleted, a payment recorded or undone, and everything else
    Atividade records (comments, photos, the invite link changed, a charge, a group deleted with its "Grupo apagado"),
    Atividade's feed and badge with them, plus a member's profile photo and this user's own profile changed on another
    of their devices. Each platform says when it is in front (Android's process, an Apple scene not in the background,
    the page visible); coming back to the front, back online or with the socket back syncs once to catch up. The
    server's Realtime only says what changed and the device pulls it as its sync does, so Room stays the one source
    of truth (`RemoteChange`, `RemotePull`, the inserts of `activity_events` and the updates of `profiles`; see
    [docs/architecture/data-sources-and-sync.md](../architecture/data-sources-and-sync.md#real-time)).
  - Push notifications (page 12's "só o que te envolve", Perfil › Notificações 4.8) go to every device of whom an
    event involves, never its author nor anyone no longer in the group, under the toggles: "Nova despesa nos meus
    grupos" for a bill created, edited or deleted while you are its payer or in its split (before the change too) and
    a comment on a bill you're in or a thread you wrote in; "Alguém marcou um pagamento para mim" for a payment
    recorded or undone that you paid or received; "Lembretes de acerto" for a charge sent to you and, every Monday at
    10:00 in Brasília, "Você tem saldo em aberto em Apê 302" (or "em 3 grupos") while you owe or are owed;
    "Convites e novos membros" (off by default) for being added to a group and someone joining yours. No amount in
    them, but for "Simplificar dívidas" (below), whose `debts_simplified_*` and `debts_unsimplified_*` (under
    "Lembretes de acerto") name the new pair's amount, which the server writes out (Pierre's call). The server picks who and sends (`push_devices`, the `push` edge function, after each `activity_events`
    insert), and each device words them in its language from a kind and the names (Android's channels are the four
    toggles, Apple's `push.<kind>` strings, the web's service worker): the group's name over "Ana lançou “Jantar”
    com você". A tap opens what the item's tap in Atividade opens, over Atividade (after a sync when its group isn't
    on the device yet) and marks it read; a reminder opens the group's Saldos over Grupos, or Saldos. Each device
    registers its token for the account signed in, an anonymous one included (`PushDeviceRepository`), and removes
    it while none is, a sign out made offline once back online. Android and Apple ask for the permission the first time Início shows, the web on the toggles.
    Each platform stays off until its keys are set: Android's Firebase config in the build, the APNs key and the
    Firebase service account on the server, the web's VAPID keys on both (`docs/push-notifications.md`). Muting a
    group isn't built yet, though the footer of Notificações mentions it.
