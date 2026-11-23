# Activity and push notifications

The feed, unread state, real time and push notifications.

  - "Activity" (page 12, `Route.Activity`, `ActivityViewModel`) is every group's feed and the notification center. It
    is no tab (Pierre's call: the room it left in the tab bar is iOS's search tab): a bell in Home's bar, beside the
    profile, opens it over Home on every platform (`HomeUiEvent.OnActivityClicked`, `activity_bell_clicked`), as does
    a push notification (Home's "See all" over "Latest expenses" opens "All expenses" now, [home-tab.md](home-tab.md);
    `Route.Activity.showsBillsOnly`, which it opened Activity with, has no opener left); back returns to Home. It has a back button and
    neither the tabs, the profile nor the search of its own (on the web, the home's frame around it, the tab it sits
    over selected and its Search going back to that tab, and on the phone its own bar with back). Its bar's
    "Notification settings" (`ActivityUiEvent.OnNotificationSettingsClicked`, `activity_notification_settings_clicked`)
    opens Notifications, below. It lists who split, edited or deleted a bill, recorded or undid a payment, created, edited or
    deleted a group, changed its invite link, added, joined, left, was removed, released or renamed in it, and the
    payment requests sent from Balances, by day (Today, Yesterday, the date), the most recent first. Each row draws
    who did it by their profile photo, their initials on their color without one (`ActivityActor.photoUrl`), with
    what it was as a badge on its corner, and under the sentence the group's name and the time, after a small square
    in the group's shape: the group's photo when it has one (`ActivityItem.groupPhotoUrl`, beta feedback of Nov 15,
    2026), else its color, grey once the group is gone from the device. The server writes the events
    (`activity_events`, by triggers); each keeps the names it had, so it still reads once its bill, group or person is
    gone. Unread is someone else's event never opened and after the last "Mark all as read", of a row of
    Notifications whose In app toggle is on: a blue dot and bold names, counted on the bell's badge in the primary
    color (`activityUnreadCount`, Home's only), whatever the filters. A row whose In app is off still lists its events,
    born read (`ActivityFeed.isUnread`, `ActivityKind.notificationType`), and turning it back on saves them read first
    (`MarkActivityTypeRead`), so the bell doesn't light up with everything that came meanwhile. Your own events are born read, opening Activity marks nothing, tapping an item marks only it. A tap opens what it is about
    (the bill's screen (Bill) over its Expenses, the group, its Members, its Balances for a payment request); a deleted bill or group
    keeps its row with the trash and says who deleted it and when instead. A bill's edit shows its first change,
    "and 2 more changes" and "See changes". "Filter activity": one group or all ("All groups" first, with no avatar
    nor an empty slot for one, beta feedback; each group with its avatar as Groups draws it: its photo, else its
    initials on its color, `ActivityGroupOption.initials`, `photoUrl`), and the types (Bills, Payments and
    settle-ups, Comments, People and groups, Payment requests), as removable chips with "Clear"; a contact's history opens it
    on that person over the profile (`Route.Activity.personKey`), a chip too. Offline it shows the device's last 90 days, the changes
    not up yet on top of Today as "Waiting to send", and says older ones load when the internet is back; a device that
    never synced says only what was done on it shows. A tablet lying down, an unfolded book, an iPad lying down, the
    web's desktop and the Mac's inspector show the item picked beside the list ("What changed", "More in" the bill's
    other events), at 640 pt at most, centred, in a wide Mac window (full screen). The Mac's View › Refresh (⌘R)
    syncs the feed, as pulling it down does on iPhone and iPad; it and Edit › Mark All as Read work only while
    Activity is the screen in front, never from under a screen it opened nor under a sheet. Comments lists the comments on bills ("Carla commented on Dinner", the text under it as edited,
    "Carla deleted a comment on Dinner" once deleted), which open the bill's screen at the comment; a bill's attachments and its share link show only in its screen's History, never in the
    feed nor its badge (`ActivityKind.isInFeed`).
  - Amounts in Activity are in the currency of the bill or payment they belong to, never the group's (Pierre's call:
    balances are one per currency, the group's currency only pre-fills a new bill): "Ana split “Dinner”" with "$ 40.00"
    in a Bora Rachar group whose default is reais. The server writes it in each bill's and payment's event
    (`currency`), and in each pair of "Simplify debts" (a group's simplification is per currency, so "See changes"
    lists each pair in its own, the group's first); a bill edited into another currency writes its "Amount" from the
    old currency to the new one. An event the server wrote before that, and a pair with none, reads in the group's
    currency (`ActivityEvent.currency`, `ActivityDebt.currency`; the push does the same on the server).
  - Portugal's Portuguese (pt-PT) talks to the user as "tu", so the verb of an Activity row and of a bill's History
    line is conjugated to the second person singular preterite when the user is the actor ("Tu dividiste Jantar",
    "Tu saíste do grupo"), on every platform: the first word of the verb phrase ends in "-ou", "-eu" or "-iu" and
    becomes "-aste", "-este" or "-iste" ("dividiu" to "dividiste", "leu" to "leste"), with "fez", "desfez", "saiu" and
    "pôs" as the few irregulars ("fizeste", "desfizeste", "saíste", "puseste"). The deleted-item notice starts with who
    deleted it ("Tu apagaste Jantar…"). Brazil's Portuguese says "Você dividiu" and needs no rule, so it keeps the verb
    as it is; the rule only runs for the pt-PT row, told from pt-BR by its region on Android and by its `.lproj` on Apple.
  - In Spanish the verb of an Activity row and of a bill's History line is conjugated to "tú" when the user is the
    actor ("Tú dividiste Cena", "Tú te uniste a Viaje", "Tú cambiaste la categoría"), on every platform: the first word
    of the verb phrase moves to the second person singular preterite ("dividió" to "dividiste", "se unió a" to "te
    uniste a", "deshizo" to "deshiciste"); other languages and other people's rows keep the verb as it is. French does
    it with its auxiliary: every French verb phrase is a passé composé that starts with "a" ("Ana a divisé Dîner"), and
    the user's rows say "Tu as divisé Dîner" (the leading "a" becomes "as"), so a French verb phrase never starts with
    another word. German and Italian do the same with theirs: every German verb phrase is a perfect starting with "hat"
    or "ist" ("Ana hat Abendessen aufgeteilt", "Du hast…", "ist beigetreten" to "bist beigetreten"), every Italian one a
    passato prossimo starting with "ha", never "è" ("Ana ha diviso Cena", "Tu hai diviso…"). Polish's past says the
    gender, which the app doesn't know, so every Polish verb phrase starts with a regular past written for both,
    "podzielił(a)", which becomes "podzieliłeś(-aś)" for the user ("Ty podzieliłeś(-aś) Kolacja"). Dutch does as German:
    every Dutch verb phrase is a perfect starting with "heeft" or "is" ("Ana heeft Diner verdeeld", "Ana is lid
    geworden van Reis"), which the user's rows turn into "hebt" or "bent" ("Jij hebt Diner verdeeld", "Jij bent lid
    geworden van Reis"). The deleted-item notice starts with who deleted it in these four languages too, as in Spanish
    and French. Japanese doesn't conjugate by person, so its rows need no rule: the sentence follows the name with
    its particle and no space ("Anaが夕食を割り勘しました", "あなたが…"), and the sentences the
    code joins (a deleted item's date, Simplify debts' detail) go together with no space and end with "。". Turkish
    puts the verb last: every Turkish verb phrase ends with its simple past in "-dı/-di/-du/-dü" (or "-tı…"), before
    the colon that leads to the object ("Ana bir harcamayı böldü: Akşam yemeği"), and the user's rows add an "n" to
    that past ("Sen bir harcamayı böldün: …", "Sen gruba katıldın: …"); its deleted-item notice starts with who deleted
    it too ("Sen sildin: …"). A line that names the user without a verb rule (a photo's "added by") is impersonal in
    Turkish ("Sen · bugün, 09:12 eklendi"). Indonesian verbs don't change with the person, so its rows need no rule
    ("Ana membagi Makan malam", "Kamu membagi…"). Neither do Russian's and Ukrainian's: their past says the gender, not
    the person, so a verb phrase written for both genders, as Polish's, serves the user as it is ("Ана разделил(а)…",
    "Ты разделил(а)…"; "Ти розділив(-ла)…"), and the name it follows is never declined. Their deleted-item notice starts
    with who deleted it ("Ты удалил(а)…"), as in Spanish, rather than a "by you" that would decline everyone else's
    name. Korean verbs don't change with the person either, but a particle after a name would follow its sound, so
    someone else is "Ana 님이" (님 always takes 이) and every Korean verb phrase starts with "님이": the user's rows drop
    it after the user's subject word "내가", which holds its own particle ("Ana 님이 ‘저녁’ 지출을 나눴어요", "내가 ‘저녁’
    지출을 나눴어요"; Android's `toSecondPerson`, Apple's `SecondPerson`, the web's `verbForYou`). Its deleted-item
    notice starts with who deleted it too ("내가 …"). Every other "You" (a list, a payment's "나 → Ana") is "나".
    Hindi follows the same rule with its ergative "ने": someone else is "Ana ने", the user "आपने" (one word), so
    every Hindi verb phrase starts with "ने", which the user's rows drop ("Ana ने ‘डिनर’ खर्च बांटा", "आपने ‘डिनर’ खर्च
    बांटा"), a join or a leave being said with a transitive verb too ("ने ‘गोवा ट्रिप’ ग्रुप जॉइन किया"). Its
    deleted-item notice starts with who deleted it ("आपने …"); every other "You" is "आप". Vietnamese verbs don't change with the person
    either ("Ana đã chia…", "Bạn đã chia…"), and its deleted-item notice starts with who deleted it ("Bạn đã xóa…",
    `youSubjectLanguages` on Android). Swedish verbs don't change with the person either ("Ana delade…", "Du
    delade…"), its notice starting with who deleted it too. Czech says the person with an auxiliary the third person
    lacks, so every Czech verb phrase starts with a past in "-l(a)", the app not knowing the gender, and the user's rows
    put "jsi" before it ("Ana rozdělil(a)…", "Ty jsi rozdělil(a)…"). Romanian swaps its perfect's auxiliary as French
    does: every Romanian verb phrase starts with "a", which the user's rows turn into "ai" ("Ana a împărțit…", "Tu ai
    împărțit…"). Both start the deleted-item notice with who deleted it too. Greek ends its aorist in "-ε" for someone
    else and "-ες" for the user, so every Greek verb phrase starts with that aorist, after at most one object pronoun,
    and the user's rows add the "ς" ("Ana μοίρασε…", "Εσύ μοίρασες…", "το επισύναψε", "το επισύναψες"). Bengali needs no
    rule: its verbs are always in the respectful form, the same for a name and for "আপনি" ("Ana ভাগ করেছেন…", "আপনি ভাগ
    করেছেন…"). Both start the deleted-item notice with who deleted it (`youSubjectLanguages`). Malay verbs don't change
    with the person ("Ana membahagikan…", "Anda membahagikan…"), and its notice stays passive ("dipadam oleh anda"). Croatian
    swaps its perfect's auxiliary as Romanian does: every Croatian verb phrase starts with "je" and a past written
    "-o/la", the app not knowing the gender, which the user's rows turn into "si" ("Ana je podijelio/la…", "Ti si
    podijelio/la…"). No reflexive "se" nor object pronoun comes before that "je", since Croatian would put them after "je"
    but before "si": a join is "je ušao/la u grupu", a photo line "je priložio/la danas u 21:10". A name is never declined,
    standing after a noun that takes the case ("je dodao/la člana Ana"), and the deleted-item notice starts with who
    deleted it ("Ti si izbrisao/la…", `youSubjectLanguages`) rather than a "by" that would decline the name ("od Ane").
    Serbian's past is Croatian's perfect, so it follows Croatian's rule: every Serbian verb phrase starts with "je" and
    a past written "-o/la", the app not knowing the gender, which the user's rows turn into "si" ("Ana je podelio/la
    trošak", "Ti si podelio/la trošak"); no reflexive "se" nor object pronoun comes before that "je", a name is never
    declined, and the deleted-item notice starts with who deleted it ("Ti si izbrisao/la…", `youSubjectLanguages`).
    Tamil puts its verb last, as Turkish does, and says the person with it, so every Tamil verb phrase ends, before the
    colon that leads to the object, with a respectful past in "-ார்", which the user's rows turn into "-ீர்கள்" ("Ana
    ஒரு செலவைப் பிரித்தார்: டின்னர்", "நீங்கள் ஒரு செலவைப் பிரித்தீர்கள்: டின்னர்"); the object after the colon also
    keeps every name free of case endings. Its deleted-item notice starts with who deleted it ("நீங்கள்
    நீக்கினீர்கள்: …", `youSubjectLanguages`), and "You" is "நீங்கள்" everywhere.
    Filipino verbs don't change with the person either, so every Filipino verb phrase starts with "ay" and the
    verb, after the actor ("Ana ay nagbahagi ng gastos", "Ikaw ay nagbahagi ng gastos"), and the deleted-item notice
    starts with who deleted it too ("Ikaw ay nagbura ng …", `youSubjectLanguages`); every other "You" is "Ikaw".
    Hungarian conjugates by person, so every Hungarian verb phrase starts with a past right after the actor, the object
    after a colon with no suffix: a definite past in "-ta"/"-te" takes a "d" in the user's rows ("Ana létrehozta a
    csoportot: …", "Te létrehoztad…"), and an indefinite one is one of the few listed in the three helpers, each with its
    own second person ("Ana felosztott egy számlát: …", "Te felosztottál…"; "kilépett", "kiléptél"). A translation
    reaches for a definite past, or for one of those, and never puts another word before the verb. Its deleted-item
    notice starts with who deleted it ("Te törölted ezt: …", `youSubjectLanguages`). Arabic's     verbs say both the person and a gender the app doesn't know, so an Arabic row names the actor, a colon and a
    verbal noun instead of a verb ("Ana: تقسيم «عشاء»", "أنت: تقسيم «عشاء»"): the same words serve the user and everyone
    else, and none of the three helpers has an Arabic rule. Its deleted-item notice is passive ("تم الحذف بواسطة Ana"). Hebrew rows are built the same way ("Ana: פיצול "ארוחת ערב"", "אני: פיצול …"), the user's subject word being "אני", and its deleted-item notice agrees with the item, never with the person. The same rules say a bill's photo line to the user who attached it, in the subject's form ("Tu", not the list's "Toi"): "Tú lo adjuntaste hoy a las 21:10", "Tu as joint la photo…" or
    "Tu l’as joint…", "Ty dodałeś(-aś) je…". A Spanish object pronoun before the verb stays ("lo adjuntó" to "lo
    adjuntaste"), and French's and Italian's elided auxiliaries change as the bare ones do ("l’a" to "l’as", "l’ha" to
    "l’hai"). A line written without a verb after the name stays as it is (German's "Du · heute um 21:10 angehängt",
    Turkish's "Sen · bugün, 21:10 eklendi").
  - Each kind's headline is one whole sentence per language with the actor's name and the subject in it, never a verb
    phrase glued between the names, so each language puts the subject where its word order does: "Ana split Dinner",
    "Ana hat Abendessen aufgeteilt", "Ana heeft Diner verdeeld", "Anaが夕食を割り勘しました" (Android's
    `activity_kind_*`, the Apple app's `SecondPerson` stand-ins, the web's `{who}` / `{subject}` messages, issue #283).
    The second-person rules above change the words right after the actor's name. Both names stay bold wherever the
    sentence puts them. Turkish keeps the colon before the subject on every platform ("Ana bir harcamayı böldü: Akşam
    yemeği"), as its verb comes last.
  - Real time (beta feedback 13, Pierre's call): while the app is in front and online, the other members' changes
    show on the device right away, with no sync to wait for: someone joining, leaving, added, removed or released, a
    group renamed or edited, a bill created, edited or deleted, a payment recorded or undone, and everything else
    Activity records (comments, photos, the invite link changed, a charge, a group deleted with its "Group deleted"),
    Activity's feed and badge with them, plus a member's profile photo and this user's own profile changed on another
    of their devices. Each platform says when it is in front (Android's process, an Apple scene not in the background,
    the page visible); coming back to the front, back online or with the socket back syncs once to catch up. The
    server's Realtime only says what changed and the device pulls it as its sync does, so Room stays the one source
    of truth (`RemoteChange`, `RemotePull`, the inserts of `activity_events` and the updates of `profiles`; see
    [docs/architecture/data-sources-and-sync.md](../architecture/data-sources-and-sync.md#real-time)).
  - Notifications (Pierre's call, beta feedback part 2, items 4 and 10; `Route.NotificationSettings`,
    `NotificationSettingsViewModel`): one screen, opened from Profile › Notifications and from Activity's bar. A row
    per kind of Activity, in its filter's order, then the weekly reminder, each with two toggles under the column
    names "Push" (the system's notification) and "In app" (the bell: Activity's blue dot and the badge):

    | Row | What it covers | Push by default | In app by default |
    |---|---|---|---|
    | Bills | a bill created, edited or deleted (its attachments and link only show in its History) | on | on |
    | Payments and settle-ups | a payment recorded or undone, "Simplify debts" turned on or off | on | on |
    | Comments | a comment on a bill | on | on |
    | People and groups | someone added, joining, leaving, removed, released or renamed; a group created, edited or deleted; its invite link changed | off | on |
    | Payment requests | a request sent from Balances | on | on |
    | Open balance reminder | the Monday reminder | on | none (no Activity event) |

    The choices sync with the account (`notification_preferences`), the OS permission stays per device. Over the
    rows, a "Push notifications" switch ("On this device only"; "In this browser only" on the web) says whether this
    device takes pushes at all (Pierre's call): kept per device and never synced, like the permission, on until turned
    off (`PushDeviceRepository.observeIsEnabled`, the `push_settings` table), and still there after a sign out. It
    shows on only while it is on and the system allows them (granted, or Apple's quiet delivery), so it is off until
    the permission is granted, and while it is off every row's Push shows off and disabled, its choice kept and back
    once it is on. Only the switch asks for the permission: turned on, it asks the system while it still can (never
    asked, or Android refused once) and, refused for good, opens the system settings (the web, which can't, does
    nothing more than its notice says); `notification_push_toggled`, with `is_enabled`. Turned off, the device's token
    leaves the server once online (`registerPushDevice`), so no push reaches it while the other devices of the
    account keep theirs, and Profile's row reads "0 active". With the
    permission denied the notice shows with "Open settings" and every Push toggle shows off and disabled, while the
    In app toggles stay in reach ("The bell keeps working."); an In app toggle never asks for the permission. On Android after a first refusal (`NotificationPermission.REFUSED_ONCE`),
    which Android still lets the app ask once more, the same notice says so and its button is "Turn on", which asks
    again (`NotificationSettingsUiEvent.OnAskAgainClicked`, `notification_ask_again_clicked`); only a
    refusal for good shows "Open settings". While Apple delivers quietly (`NotificationPermission.PROVISIONAL`, below)
    the switch is on and the Push toggles work as allowed, and a notice says "Blitz Split notifications arrive quietly in Notification
    Center, without a sound or banner." with "Turn on", which brings Apple's full prompt
    (`OnQuietTurnOnClicked`, `notification_quiet_turn_on_clicked`), as does a Push turned on meanwhile. On Android a
    row whose channel the person silenced in the system's settings (`PushChannel`, read on appear and on each return,
    `OnSystemBlockedTypesChecked`) shows its Push off and disabled, its choice kept, with "Blocked in Android" in the
    error color under its detail and "Open settings", which opens that channel's settings
    (`OnChannelSettingsClicked`, `notification_channel_settings_clicked`); iOS and the web have no switch per row, so
    none is ever blocked there. The app's notifications all off show the notice instead, never a line per row. The Profile row reads "5 active", the rows whose Push is on. Before this,
    comments rode on "New expense in my groups", payment requests and Simplify debts on "Settle-up reminders", and the
    bell had no control at all; the server copied each new Push toggle from the one it used to ride on. Each platform
    draws it its way: Android a full screen (both entries) with two Material icon toggle buttons per row (a phone,
    a bell; filled when on, crossed out when off); iOS and iPadOS a grouped Form pushed from the profile and over
    Activity, with an icon toggle button each (an iPhone, a bell; a laptop on the Mac); the Mac keeps it inside its
    Settings (⌘,), which Activity's toolbar button opens on Notifications; the web a card inside the profile and, from
    Activity, a dialog (a drawer on a phone) with two switches per row. A row's toggles are read out as "Bills,
    Push".
  - Push notifications (page 12's "only what involves you") go to every device of whom an event involves, never its
    author nor anyone no longer in the group, under the Push toggle of its row: Bills for a bill created, edited or
    deleted while you are its payer or in its split (before the change too); Comments for a comment on a bill you're in
    or a thread you wrote in; Payments and settle-ups for a payment recorded or undone that you paid or received, and
    "Simplify debts" changing whom you pay or who pays you; Payment requests for a request sent to you; People and
    groups for being added to a group and someone joining yours; Open balance reminder for, at each group's reminder
    schedule (Monday 10:00 unless the group changed it in Edit group's "Group reminder", on each device's own clock, which
    every app sends as `time_zone` when it registers the device), "You have an open balance in Apt 302" (or "in 3 groups") only
    while you owe in it; the group's banner for debtors is in [members-and-payments.md](members-and-payments.md#group-reminder). The other kinds of
    People and groups only reach the bell. No amount in
    them, but for "Simplify debts" (in [members-and-payments.md](members-and-payments.md)), whose `debts_simplified_*` and `debts_unsimplified_*` (under
    Payments and settle-ups) name the new pair's amount, which the server writes out (Pierre's call). The server picks who and sends (`push_devices`, the `push` edge function, after each `activity_events`
    insert), and each device words them in its language from a kind and the names (Android's channels are the six rows,
    the first four keeping their ids `new_expense`, `payment_marked`, `invites` and `settle_reminders` so a channel
    already silenced stays so, plus `comments` and `payment_requests`; Apple's `push.<kind>` strings, the web's service worker): the group's name over "Ana split “Dinner”
    with you". A tap opens what the item's tap in Activity opens, over Activity over Home (`PushLink.homeTab` null),
    and marks it read; a reminder opens the group's Balances over Groups, or Balances. The specific screen opens at
    once with what the device already has (beta feedback 3, item 28: waiting for a sync first left Activity on screen
    for seconds, so a tap seemed to always open Activity): the group, then the bill (Bill) when it is on the device, at
    the comment for a comment once its event is. Only what isn't there yet (a group just joined, a new bill or comment)
    waits up to 5 seconds, for a sync or for it to reach the device by the sync its push's arrival started (a comment
    waits for the sync, its comment coming with the event), then opens over what is on top while the person stayed there; nothing opens
    for a group or bill still missing after it. Android, iOS, macOS and the web share this (`AppViewModel.openPushLink`).
    So that wait is rare, Android, iOS and macOS sync as an event's push arrives, before any tap (Pierre's call,
    feedback of Nov 21, 2026: the tap opened the group and the bill only seconds later), with nothing new on screen:
    Android right after it shows the notification, for at most 9 seconds (FCM's data message, which reaches the app
    open or closed); iOS woken in the background by the alert's `content-available` (the `remote-notification`
    background mode, at most 25 of the 30 seconds iOS gives; iOS may skip a wake, and never wakes an app the person
    swiped away); the Mac while the app runs. Each is the usual sync (`SyncNow`), never the bill in the push (Pierre
    turned that down: the payload's 4 KB, and it would repeat the sync). The reminder brings nothing to sync, nor does
    a push whose event is already on the device (an earlier push of a burst synced it, or real time did with the app
    in front), so only the first of a burst holds Android's next notification back (`PushArrivalSync`). The web keeps
    the wait above. Each device
    registers its token for the account signed in, an anonymous one included (`PushDeviceRepository`), and removes
    it while none is or while Notifications' "Push notifications" switch is off, a sign out made offline once back
    online; a failed call is tried again after 30 seconds, 2 and 10 minutes, then waits for the next change. Android and Apple register their token at every launch
    whatever the permission; the web subscribes as soon as the browser allows it, from its prompt, the site settings or
    on load (`watchNotificationPermission`).
  - Asking for the permission (Pierre's call, issue 208; per device, never synced): no platform asks on its own. Home
    shows a card under the shortcuts once there is a group to be notified about (created or joined), never on the first
    use, during a first download nor beside the recovery code's card or sheet (`HomeUiState.notificationPrompt`). Never asked
    (`SOFT_ASK`): "Know when a bill comes in", "Get notified when someone adds a bill with you or marks a payment to
    you.", with "Not now" and "Turn on", which alone brings the system's prompt, so "Not now" never spends it. Any card's
    "Turn on" also turns Notifications' "Push notifications" switch on for this device (Pierre's call), so its
    "Notifications on" holds after the switch was turned off. Android
    refused once (`ASK_AGAIN`, its rationale flag): the same card says Android lets you allow them once more, and "Turn
    on" asks again. Refused for good (`BLOCKED`): "Notifications are off", where "Turn on" opens the app's notification
    settings on Android, iOS and macOS; the web, which can't open its settings, says "Notifications are blocked in this
    browser" and where to allow them, with only "Not now". Either answer hides the card for three weeks on this device
    (`lastNotificationPromptAt` of the device settings; `notification_prompt_turn_on_clicked`,
    `notification_prompt_dismissed`, with `prompt`), and it never comes back once allowed. iOS, iPadOS and macOS
    (Pierre's call) ask for no soft ask: once there is a group they deliver quietly to Notification Center with no
    prompt (Apple's provisional authorization, `HomeUiAction.RequestQuietNotifications`, once per Home, the card hidden
    meanwhile), and the card right away says "Notifications arrive quietly", "They go to Notification Center without a
    sound or banner. Turn them on to see bills as they come in." (`QUIET`), whose "Turn on" brings Apple's full prompt
    for banners and sounds, under the same three weeks. A quiet request that changed nothing falls back to the soft
    ask. Android and the web have no quiet delivery and behave as above. Each platform reads the
    permission when Home appears, on each return to the app and after its prompt; one turning allowed after the card's
    "Turn on" (the prompt, or back from the settings) shows "Notifications on" (`HomeUiAction.ShowNotificationsOn`),
    while one allowed from Notifications confirms there, its Push toggles coming back. Analytics counts the card
    coming up once per showing, whatever brought it, once Home is on top to draw it (`notification_prompt_shown`,
    decided by `HomeViewModel`), and the
    system's answer after the app asked, from Home or from Notifications (`notification_permission_answered`, with
    `is_granted`).
    Each platform stays off until its keys are set: Android's Firebase config in the build, the APNs key and the
    Firebase service account on the server, the web's VAPID keys on both (`docs/push-notifications.md`). Muting a
    group isn't built yet, though the footer of Notifications mentions it.
