# Activity and push notifications

The feed, unread state, real time and push notifications.

  - "Activity" (page 12, `Route.Activity`, `ActivityViewModel`) is every group's feed and the notification center. It
    is no tab (Pierre's call: the room it left in the tab bar is iOS's search tab): a bell in Home's bar, beside the
    profile, opens it over Home on every platform (`HomeUiEvent.OnActivityClicked`, `activity_bell_clicked`), as do
    Home's "See all" over "Latest expenses" and a push notification; back returns to Home. It has a back button and
    neither the tabs, the profile nor the search of its own (on the web, the home's frame around it, the tab it sits
    over selected and its Search going back to that tab, and on the phone its own bar with back). Its bar's
    "Notification settings" (`ActivityUiEvent.OnNotificationSettingsClicked`, `activity_notification_settings_clicked`)
    opens Notifications, below. It lists who split, edited or deleted a bill, recorded or undid a payment, created, edited or
    deleted a group, changed its invite link, added, joined, left, was removed, released or renamed in it, and the
    payment requests sent from Balances, by day (Today, Yesterday, the date), the most recent first. The server writes the events
    (`activity_events`, by triggers); each keeps the names it had, so it still reads once its bill, group or person is
    gone. Unread is someone else's event never opened and after the last "Mark all as read", of a row of
    Notifications whose In app toggle is on: a blue dot and bold names, counted on the bell's badge in the primary
    color (`activityUnreadCount`, Home's only), whatever the filters. A row whose In app is off still lists its events,
    born read (`ActivityFeed.isUnread`, `ActivityKind.notificationType`), and turning it back on saves them read first
    (`MarkActivityTypeRead`), so the bell doesn't light up with everything that came meanwhile. Your own events are born read, opening Activity marks nothing, tapping an item marks only it. A tap opens what it is about
    (the bill's screen (Bill) over its Expenses, the group, its Members, its Balances for a payment request); a deleted bill or group
    keeps its row with the trash and says who deleted it and when instead. A bill's edit shows its first change,
    "and 2 more changes" and "See changes". "Filter activity": one group or all, and the types (Bills, Payments and
    settle-ups, Comments, People and groups, Payment requests), as removable chips with "Clear"; a contact's history opens it
    on that person over the profile (`Route.Activity.personKey`), a chip too. Offline it shows the device's last 90 days, the changes
    not up yet on top of Today as "Waiting to send", and says older ones load when the internet is back; a device that
    never synced says only what was done on it shows. A tablet lying down, an unfolded book, an iPad lying down, the
    web's desktop and the Mac's inspector show the item picked beside the list ("What changed", "More in" the bill's
    other events). Comments lists the comments on bills ("Carla commented on Dinner", the text under it as edited,
    "Carla deleted a comment on Dinner" once deleted), which open the bill's screen at the comment; a bill's attachments and its share link show only in its screen's History, never in the
    feed nor its badge (`ActivityKind.isInFeed`).
  - In Spanish the verb of an Activity row and of a bill's History line is conjugated to "tú" when the user is the
    actor ("Tú dividiste Cena", "Tú te uniste a Viaje", "Tú cambiaste la categoría"), on every platform: the first word
    of the verb phrase moves to the second person singular preterite ("dividió" to "dividiste", "se unió a" to "te
    uniste a", "deshizo" to "deshiciste"); other languages and other people's rows keep the verb as it is.
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

    The choices sync with the account (`notification_preferences`), the OS permission stays per device. With the
    permission denied the notice shows with "Open settings" and every Push toggle shows off and disabled, while the
    In app toggles stay in reach ("The bell keeps working."); a Push turned on while the permission was never asked
    asks for it, an In app one never does. On Android after a first refusal (`NotificationPermission.REFUSED_ONCE`),
    which Android still lets the app ask once more, the same notice says so and its button is "Turn on", which asks
    again (`NotificationSettingsUiEvent.OnAskAgainClicked`, `notification_ask_again_clicked`); only a
    refusal for good shows "Open settings". While Apple delivers quietly (`NotificationPermission.PROVISIONAL`, below)
    the Push toggles work as allowed, and a notice says "Blitz Split notifications arrive quietly in Notification
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
    groups for being added to a group and someone joining yours; Open balance reminder for, every Monday at 10:00 in
    Brasília, "You have an open balance in Apt 302" (or "in 3 groups") while you owe or are owed. The other kinds of
    People and groups only reach the bell. No amount in
    them, but for "Simplify debts" (in [members-and-payments.md](members-and-payments.md)), whose `debts_simplified_*` and `debts_unsimplified_*` (under
    Payments and settle-ups) name the new pair's amount, which the server writes out (Pierre's call). The server picks who and sends (`push_devices`, the `push` edge function, after each `activity_events`
    insert), and each device words them in its language from a kind and the names (Android's channels are the six rows,
    the first four keeping their ids `new_expense`, `payment_marked`, `invites` and `settle_reminders` so a channel
    already silenced stays so, plus `comments` and `payment_requests`; Apple's `push.<kind>` strings, the web's service worker): the group's name over "Ana split “Dinner”
    with you". A tap opens what the item's tap in Activity opens, over Activity over Home (`PushLink.homeTab` null, after a sync when its group isn't
    on the device yet) and marks it read; a reminder opens the group's Balances over Groups, or Balances. Each device
    registers its token for the account signed in, an anonymous one included (`PushDeviceRepository`), and removes
    it while none is, a sign out made offline once back online. Android and Apple register their token at every launch
    whatever the permission; the web subscribes as soon as the browser allows it, from its prompt, the site settings or
    on load (`watchNotificationPermission`).
  - Asking for the permission (Pierre's call, issue 208; per device, never synced): no platform asks on its own. Home
    shows a card under the shortcuts once there is a group to be notified about (created or joined), never on the first
    use, during a first download nor beside the recovery code's card or sheet (`HomeUiState.notificationPrompt`). Never asked
    (`SOFT_ASK`): "Know when a bill comes in", "Get notified when someone adds a bill with you or marks a payment to
    you.", with "Not now" and "Turn on", which alone brings the system's prompt, so "Not now" never spends it. Android
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
