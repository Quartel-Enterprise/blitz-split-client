# Analytics

Firebase Analytics (GA4), on Android, iOS, macOS and the web, one Firebase project for all of them, the one
Crashlytics and Android's push notifications use ([crash-reporting.md](../crash-reporting.md)). Each platform sends
through its own official SDK; the shared code decides what is tracked, so the same action sends the same event on
every UI.

## How it fits

| Piece | Where | Does |
| --- | --- | --- |
| `TrackEvent` | `:core:provider:analytics`'s `domain/usecase` | How the shared code tracks an event: a name and its params. A Boolean goes as `"true"` or `"false"` (GA4 has no boolean), a whole number as a Long, any other as a Double, an enum as its name in lowercase; any other value is left out, never sent as its text |
| `AnalyticsHub` | `:core:provider:analytics` | Where the events go, an SDK that throws losing only that event: once an app attaches its platform's `AnalyticsService`. The first 50 before that wait for it, so the opening screen counts on the web too, which attaches only once the page drew; Firebase dates them when they are sent, a moment late, since its SDKs take no timestamp |
| `FirebaseAnalyticsService` | `:core:provider:analytics`'s `androidMain` | Android's, attached by `BlitzSplitApplication` |
| `FirebaseSetup.swift` | `iosApp/Sources/Shared/App` | Apple's, attached through `BlitzSplitApple.attachAnalytics` once the shared code started |
| `firebaseAnalytics.ts` | `webApp/src/analytics` | The web's, through `firebase/analytics`, attached through the exported `attachAnalytics` |

Screen views are tracked ([below](#screen-views)); each user action's event comes with its feature, through
`TrackEvent`.

## Screen views

`StartScreenViews` (`:core:provider:analytics`, which `ScreenViewTracker` behind it follows the back stack for) sends GA4's `screen_view` each time another route comes on top of the back
stack and stays there for half a second, going back to one included, once per route: another group's Despesas is
another view, the same route drawn again is not. One gone at once never counts, nor does the screen under it coming
back: the Mac pops Perfil and Mapa de categorias right away to open them in Ajustes, so Ajustes is not a screen view
there (Notificações, kept inside the profile on the Mac and the web, isn't either). It starts once `AppViewModel` drew the first screen, so the welcome the stack holds before that never counts.
`screen_name` and `screen_class` (the web's adapter renames them `firebase_screen` and `firebase_screen_class`, which
its SDK reads) are the route's name (`RouteScreenView.kt`, a `when` with no `else`, so a new route
can't ship without one); a few routes add what kind of screen it is, which stays true each time it comes back on top, never how it was opened (the tap's own event says that) nor whose:

| `screen_name` | Params |
| --- | --- |
| `invite` | `is_new_group` (right after Novo grupo) |
| `split_bill` | `is_edit` (Editar conta) |
| `register_payment` | `offers_keys` ("Pagar Ana") |
| `contact_invite` | `is_guest` (a guest's personal link) |
| `activity` | `is_person` (one person's history) |
| `join_invite` | `is_personal` (a personal link) |
| `payment_key` | `method` (`pix`, `bitcoin`), `is_new` |
| Every other route (`home`, `groups`, `group_detail`, `bill_detail`…) | none |

## What is never sent

- **A name, an amount or an id** (a group's, a bill's, a person's), in an event's params or anywhere else: the event
  goes to Google. Params say what kind of thing happened (`split_mode`, `source`, `is_new`), never whose or how much.
- **Anything from a debug build**: Android's manifest turns collection off for the debug build type
  (`analyticsCollectionEnabled`), Apple's Info.plist turns it off from the first launch on and `FirebaseSetup` turns it
  on again only for a release build, on every launch, since the setting persists on the device; a debug build never
  attaches either, and the web only starts in a production build.
- **The advertising id**: Android removes the `AD_ID` permission and turns its collection off, Apple links
  `FirebaseAnalytics` without `FirebaseAnalyticsIdentitySupport`, and the web turns Google signals off. Ad
  personalization is off on all of them.
- **A screen view the SDK makes up**: Android's and Apple's automatic screen reporting are off and gtag sends no
  `page_view`, since the activity, the view controllers and the browser's history don't name the app's screens.
  Screen views come from the shared back stack ([Screen views](#screen-views)), the same on every UI.

## Consent

Nothing is collected yet, in any build: whether the app asks first, or offers an opt-out in Perfil, is Pierre's call,
still open. Until then every platform keeps collection off, its events dropped by the SDK, and three switches turn
it on once it is settled, for release builds only: `collectsAnalytics` in `androidApp/build.gradle.kts`, in
`FirebaseSetup.swift` and in `webApp/src/analytics/firebaseAnalytics.ts`. The Play Data safety form and the App Store
privacy labels need updating before a release that collects.

## What stays off until it is set up

Every build runs without it, CI's included.

- **Android, iOS and macOS** read the same config files as Crashlytics: `androidApp/google-services.json` and
  `iosApp/Resources/GoogleService-Info.plist` ([crash-reporting.md](../crash-reporting.md#what-stays-off-until-it-is-set-up)).
  Turn Google Analytics on for the Firebase project first, so the files carry its ids.
- **The web**: a web app in the same Firebase project, whose config the production build reads from the environment
  (`webApp/.env.production.local`, git-ignored, or the host's build settings):

  ```properties
  VITE_FIREBASE_API_KEY=<apiKey>
  VITE_FIREBASE_APP_ID=<appId>
  VITE_FIREBASE_PROJECT_ID=<projectId>
  VITE_FIREBASE_MEASUREMENT_ID=<measurementId>
  ```

  In Google Analytics, turn off the web stream's enhanced measurement of "page changes based on browser history
  events", which would add a `page_view` per route on top of the shared code's screen views.

## Check it

Release builds only. Android: `adb shell setprop debug.firebase.analytics.app com.quare.blitzsplit`, then open the
app and see the events in Firebase's DebugView. Apple: run the release scheme with `-FIRDebugEnabled` among the
launch arguments. Web: the Google Analytics Debugger extension, or DebugView with `?debug_mode=true`.

## Tracking a user action

Each feature's `UiEvent` moves to `TrackedUiEvent` (`:core:presentation`), which declares what it sends in
`analytics`, and its ViewModel extends `TrackedViewModel`, which takes `TrackEvent` and implements `handleEvent`. Every
UI keeps calling `onEvent`, which sends an `Automatic` event before handling it, so the three UIs send the same
events. Once every feature has moved, `TrackedUiEvent` merges into `UiEvent` and the compiler asks for the decision on
every new user action. The `add-analytics-event` skill walks through it.

| `analytics` | When | Sent by |
| --- | --- | --- |
| `EventAnalytics.Automatic(name, params)` | The event and its params are known from the `UiEvent` itself | `TrackedViewModel.onEvent`; the ViewModel never calls `trackEvent` for it. A screen that refuses whole kinds of events for a while (Rachar conta reading a receipt) overrides `isAccepted`, and `onEvent` then neither tracks nor handles those events |
| `EventAnalytics.Manual(name)` | It needs what only handling it tells: the result of a save, the `UiState` (`setOf(…)` for a branch that may send more than one) | A `trackEvent("name", …)` call in the branch, with the same name |
| `EventAnalytics.NotTracked` | Not a user action at all: a scroll, an animation ending, a frame or a window measured, a value typed letter by letter (what was typed shows in the action that saves it), a result the platform hands back (unless it stands for a tap the ViewModel never sees, as a photo picked does, or ends the funnel a tap began, as the native sign in failing or closed does). A back too, since each platform's own back (a gesture, the NavigationStack, a swipe down) reaches the back stack apart, and a tab of the home's bar, rail or sidebar, which each platform routes through whichever screen is in front: where either went is its screen view | Nothing |

Every tap on a button, a row, a tab, a menu item, a chip or a toggle is a user action and gets its event, even when it
opens a screen: the tap and the screen it opens are different signals (how many tapped, how many saw). Picking an
option (a payer, a split mode, a category) is one too; typing isn't.

Naming, as GA4 takes it:

- `<object>_<action>`, the action in the past, snake case, at most 40 letters: `bill_saved`, `group_tab_clicked`,
  `payment_undone`.
- A toggle is one event with an `is_*` param (`is_pinned`), never two. A confirmation dialog's two buttons are
  `*_confirmed` and `*_cancelled`.
- At most 25 params, each value at most 100 letters. An enum goes as its name in lowercase (`split_mode` = `items`).
- Write the name as a literal right in `EventAnalytics.Automatic("…")`, `EventAnalytics.Manual("…")` and
  `trackEvent("…", …)`, with the `EventAnalytics.` prefix: `AnalyticsCatalogTest` (`:core:provider:analytics`'s
  `jvmTest`) reads them there, checks each name, finds its row below, and finds a `trackEvent` call of its module
  sending each `Manual` one.

## Events

Every event a feature declares, one row each, by the feature that sends it. `AnalyticsCatalogTest` fails on a declared
event with no row and on a row no feature declares. Screen views are [above](#screen-views).

| Event | Params | Sent when | Feature |
| --- | --- | --- | --- |
| `sign_in_provider_clicked` | `provider` (`google`, `apple`), `source` (`welcome`, `restore`, `recovery_code`) | Google's or Apple's button, before its sign in starts | `:core:presentation` (`SignInAnalytics`), from `:feature:welcome`, `:feature:restore`, `:feature:recovery_code` |
| `native_sign_in_failed` | `source` | The platform's own sign in sheet failed, which ends the funnel the tap began, so this result counts | `:core:presentation` (`SignInAnalytics`), from the same three |
| `native_sign_in_cancelled` | `source` | The platform's own sign in sheet was closed | `:core:presentation` (`SignInAnalytics`), from the same three |
| `welcome_use_without_account_clicked` | none | Boas-vindas' "Usar sem conta" | `:feature:welcome` |
| `welcome_restore_clicked` | none | Boas-vindas' "Já usei em outro aparelho" | `:feature:welcome` |
| `avatar_color_selected` | `color`, `source` (`profile_setup`) | An avatar color picked | `:feature:profile_setup` |
| `photo_picked` | `source` (`profile_setup`, `create_group`, `edit_group`, `receipt`, `bill_detail`) | A photo handed back by the camera or the picker, which each UI opens straight from its tap, so it stands for the tap | `:feature:profile_setup`, `:feature:create_group`, `:feature:edit_group`, `:feature:split_bill`, `:feature:bill_detail` |
| `photo_crop_turned` | `source` (`profile_setup`, `create_group`, `edit_group`) | "Ajustar foto"'s Girar | `:core:presentation`, from `:feature:profile_setup`, `:feature:create_group` and `:feature:edit_group` |
| `photo_crop_mirrored` | `source` (`profile_setup`, `create_group`, `edit_group`), `axis` (`horizontal`, `vertical`) | "Ajustar foto"'s Horizontal or Vertical | `:core:presentation`, from `:feature:profile_setup`, `:feature:create_group` and `:feature:edit_group` |
| `photo_crop_confirmed` | `source` (`profile_setup`, `create_group`, `edit_group`) | "Ajustar foto"'s Pronto | `:core:presentation`, from `:feature:profile_setup`, `:feature:create_group` and `:feature:edit_group` |
| `photo_crop_dismissed` | `source` (`profile_setup`, `create_group`, `edit_group`) | "Ajustar foto" closed without Pronto | `:core:presentation`, from `:feature:profile_setup`, `:feature:create_group` and `:feature:edit_group` |
| `profile_setup_continue_clicked` | none | Seu perfil's Continuar | `:feature:profile_setup` |
| `retry_clicked` | `source` (`restore`, `invite`, `join_invite`, `receipt`, `share_bill`, `shared_bill`) | "Tentar de novo" after a failure, and Recuperar's button while offline | `:feature:restore`, `:feature:invite`, `:feature:join`, `:feature:split_bill`, `:feature:bill_detail` |
| `restore_code_submitted` | none | Recuperar's button or the keyboard's Done, online; a code read from a QR restores without it, counted by `restore_scan_qr_clicked` | `:feature:restore` |
| `restore_scan_qr_clicked` | none | Recuperar's QR reader opened | `:feature:restore` |
| `restore_start_fresh_clicked` | none | Recuperar's "Começar do zero" | `:feature:restore` |
| `recovery_code_copy_clicked` | none | The recovery code's Copiar | `:feature:recovery_code` |
| `recovery_code_password_manager_clicked` | none | "Salvar no gerenciador de senhas" | `:feature:recovery_code` |
| `recovery_code_connect_instead_clicked` | none | "Conectar Google ou Apple em vez disso" | `:feature:recovery_code` |
| `recovery_code_remind_later_clicked` | none | "Lembrar depois", or the sheet closed any other way while the code is due | `:feature:recovery_code` |
| `recovery_code_saved_clicked` | none | "Já guardei" | `:feature:recovery_code` |
| `delete_account_confirm_toggled` | `is_checked` | Excluir conta's confirmation checkbox, a change the screen takes (not offline nor while deleting) | `:feature:delete_account` |
| `delete_account_clicked` | none | Excluir conta's Excluir | `:feature:delete_account` |
| `delete_account_cancelled` | none | Excluir conta's Cancelar, its back arrow, or a native dismiss | `:feature:delete_account` |
| `data_export_clicked` | `source` (`delete_account`) | "Exportar meus dados" | `:feature:delete_account` |
| `invite_link_copy_clicked` | `source` (`invite`, `personal_invite`) | Copiar on Convidar or on a personal invite | `:feature:invite`, `:feature:members` |
| `invite_link_share_clicked` | `source` (`invite`, `personal_invite`) | Convidar's "Compartilhar link" or "Compartilhar com Carla" | `:feature:invite`, `:feature:members` |
| `invite_see_who_joined_clicked` | none | Convidar's "Ver quem já entrou" | `:feature:invite` |
| `invite_go_to_group_clicked` | none | Convidar's "Ir para o grupo" | `:feature:invite` |
| `invite_link_change_clicked` | none | "Trocar link", which asks first | `:feature:invite` |
| `invite_link_change_confirmed` | none | "Trocar o link do convite?" confirmed, while it was open | `:feature:invite` |
| `invite_link_change_cancelled` | none | "Trocar o link do convite?" closed without confirming, while it was open | `:feature:invite` |
| `members_share_clicked` | none | Membros' Compartilhar, which opens Convidar | `:feature:members` |
| `member_add_clicked` | none | "Adicionar pessoa" | `:feature:members` |
| `person_clicked` | `source` (`members`, `register_payment`, `global_balances`, `group_stats`, `bill_detail`) | A person's row, which opens their profile | `:feature:members`, `:feature:register_payment`, `:feature:global_balances`, `:feature:group_stats`, `:feature:bill_detail` |
| `member_rename_clicked` | none | A member's "Renomear" | `:feature:members` |
| `member_name_suggestion_clicked` | `source` (`members`, `create_group`) | A nickname offered for a name already in the group ("Ana P.", "Ana 2") | `:feature:members`, `:feature:create_group` |
| `member_editor_save_clicked` | none | The name editor's Salvar or the keyboard's Done, adding or renaming; also a Done the screen refuses (a name taken or too short) | `:feature:members` |
| `personal_invite_clicked` | none | "Convite pessoal para Carla" | `:feature:members` |
| `member_remove_clicked` | none | "Remover do grupo", which asks first | `:feature:members` |
| `member_remove_confirmed` | none | "Remover Carla do grupo?" confirmed, while it was open | `:feature:members` |
| `member_remove_cancelled` | none | "Remover Carla do grupo?" closed without confirming, while it was open | `:feature:members` |
| `member_release_clicked` | none | "Liberar nome", which asks first | `:feature:members` |
| `member_release_confirmed` | none | "Liberar o nome Carla?" confirmed, while it was open and not waiting on the server | `:feature:members` |
| `member_release_cancelled` | none | "Liberar o nome Carla?" closed without confirming, while it was open and not waiting on the server | `:feature:members` |
| `join_invite_join_clicked` | none | Convite's "Entrar no grupo" (or "Entrar como Carla" on a personal invite) | `:feature:join` |
| `join_invite_not_me_clicked` | none | A personal invite's "Não sou Carla" | `:feature:join` |
| `join_invite_not_now_clicked` | none | Convite's "Agora não", its ✕, or Esc on the Mac and an iPad's keyboard (the ✕'s shortcut) | `:feature:join` |
| `join_invite_go_home_clicked` | none | "Ir para o início", for an invite that can't open | `:feature:join` |
| `join_invite_scan_another_clicked` | none | "Escanear outro QR", for an invite no longer valid | `:feature:join` |
| `join_pick_member_selected` | none | A name picked in "Quem é você no grupo?" | `:feature:join` |
| `join_pick_join_clicked` | none | "Entrar como Bruno" | `:feature:join` |
| `join_pick_not_in_list_clicked` | none | "Não estou na lista" | `:feature:join` |
| `join_pick_new_member_confirmed` | none | "Entrar no grupo" with a new name | `:feature:join` |
| `camera_settings_clicked` | `source` (`join_with_link`) | "Abrir configurações" with the camera denied | `:feature:join` |
| `join_with_link_torch_toggled` | `is_on` | The reader's flashlight, with where it ended up | `:feature:join` |
| `join_with_link_paste_clicked` | none | The reader's Colar, also with an empty clipboard or one the browser won't read | `:feature:join` |
| `join_with_link_clear_clicked` | none | The link field cleared | `:feature:join` |
| `join_with_link_open_clicked` | none | "Abrir convite" or the keyboard's Go, for a link typed; also a Go the screen refuses (a blank field) | `:feature:join` |
| `group_color_clicked` | `source` (`create_group`) | The group's avatar tapped, stepping its color | `:feature:create_group` |
| `group_shape_and_color_clicked` | `source` (`create_group`) | Android's "Trocar forma e cor do grupo" | `:feature:create_group` |
| `photo_removed` | `source` (`create_group`) | "Remover foto" | `:feature:create_group` |
| `group_type_selected` | `type`, `source` (`create_group`, `edit_group`) | A group type picked | `:feature:create_group`, `:feature:edit_group` |
| `group_currency_selected` | `currency`, `source` (`create_group`, `edit_group`) | A currency picked, one Editar grupo takes (not once the group has a bill) | `:feature:create_group`, `:feature:edit_group` |
| `create_group_add_member_clicked` | none | Novo grupo's "Adicionar" (or the keyboard's Done) | `:feature:create_group` |
| `create_group_remove_member_clicked` | none | A person taken off the new group | `:feature:create_group` |
| `create_group_create_clicked` | none | Novo grupo's Criar, also when the form can't create yet | `:feature:create_group` |
| `group_created` | `type`, `currency`, `members` (how many: you, those added and a valid name still typed in the field), `has_photo` | A new group saved | `:feature:create_group` |
| `update_accepted` | `source` (`startup`, `manual`) | "Abrir na App Store" on "Atualização disponível" (iOS, macOS) | `in_app_update` |
| `update_dismissed` | `source` (`startup`, `manual`) | "Agora não" or Esc on "Atualização disponível" (iOS, macOS) | `in_app_update` |
| `profile_avatar_clicked` | `source` (`home`, `groups`, `sidebar`, `global_balances`) | The avatar that opens Perfil | `:feature:groups`, `:feature:global_balances` |
| `brand_clicked` | `source` (`home`, `groups`, `sidebar`, `global_balances`) | The web's logo, which opens Home (a tap on Home itself counts too); Contacts and Activity send nothing until they move to tracked events | `:feature:groups`, `:feature:global_balances` |
| `search_opened` | `source` (`home`, `groups`, `global_balances`) | Buscar opened: its magnifier, search tab, field or ⌘K (not one refused over a dialog or a pushed screen) | `:feature:groups`, `:feature:global_balances` |
| `search_scope_selected` | `scope`, `source` (`search`, `groups`) | Tudo, Grupos, Pessoas or Despesas | `:feature:groups` |
| `search_result_clicked` | `kind` (`group`, `person`, `bill`), `source` (`search`, `groups`) | A result tapped | `:feature:groups` |
| `sync_retry_clicked` | `source` (`home`, `groups`) | The sync chip's "tentar de novo" | `:feature:groups` |
| `pull_to_refresh` | `source` (`home`, `groups`) | Pulled down on Android and iOS | `:feature:groups` |
| `split_bill_clicked` | `source` (`home`, `groups`, `group_detail`, `group_stats`) | "Rachar conta" | `:feature:groups`, `:feature:group_detail`, `:feature:group_stats` |
| `balances_to_receive_clicked` | `source` (`home`, `groups`) | "A receber" or "Te devem" | `:feature:groups` |
| `balances_to_pay_clicked` | `source` (`home`, `groups`) | "A pagar" or "Você deve" | `:feature:groups` |
| `create_group_clicked` | `source` (`home`, `groups`) | "Novo grupo" | `:feature:groups` |
| `join_with_link_clicked` | `source` (`home`, `groups`) | "Entrar com link ou QR" | `:feature:groups` |
| `group_clicked` | `source` (`home`, `groups`, `sidebar`, `global_balances`) | A group's row, card or sidebar entry, or a group of a person's "Por grupo" on Saldos | `:feature:groups`, `:feature:global_balances` |
| `settled_groups_toggled` | `is_shown` | Quitados expanded or collapsed, or its tab | `:feature:groups` |
| `groups_display_changed` | `display` (`list`, `grid`) | "Lista ou grade", for another way than the one showing | `:feature:groups` |
| `groups_grid_size_changed` | `size` | Another size the grid takes: Compacto, Padrão or Grande, once per size the Mac's slider passes | `:feature:groups` |
| `invite_clicked` | `source` (`groups`, `group_detail`) | "Convidar pessoas" | `:feature:groups`, `:feature:group_detail` |
| `leave_group_clicked` | `source` (`groups`, `group_detail`) | "Sair do grupo" | `:feature:groups`, `:feature:group_detail` |
| `delete_group_clicked` | `source` (`groups`, `group_detail`) | "Apagar grupo" | `:feature:groups`, `:feature:group_detail` |
| `group_exit_confirmed` | `dialog` (`leave_blocked`, `confirm_leave`, `delete_blocked`, `confirm_delete`), `source` (`groups`, `group_detail`) | The dialog's main button (Sair, Apagar para todos, Acertar, Ver saldos), while it could be answered | `:feature:groups`, `:feature:group_detail` |
| `group_exit_cancelled` | `dialog`, `source` (`groups`, `group_detail`) | The dialog closed without its main button, while it could be answered | `:feature:groups`, `:feature:group_detail` |
| `payment_undo_clicked` | `source` (`home`, `groups`, `group_detail`, `global_balances`) | "Desfazer" on "Pagamento de Ana registrado." | `:feature:groups`, `:feature:group_detail`, `:feature:global_balances` |
| `home_see_balances_clicked` | none | "Ver saldos" over Início's faces | `:feature:groups` |
| `balance_face_clicked` | `source` (`home`) | A face of "Saldos com pessoas", which opens settling up with them for the net | `:feature:groups` |
| `scan_receipt_clicked` | `source` (`home`, `split_bill`) | "Ler cupom fiscal" | `:feature:groups`, `:feature:split_bill` |
| `record_payment_clicked` | `source` (`home`) | Início's "Registrar pagamento" | `:feature:groups` |
| `home_see_all_groups_clicked` | none | "Ver todos" over "Seus grupos" | `:feature:groups` |
| `bill_clicked` | `source` (`home`, `group_detail`, `group_stats`) | A bill's row, which opens its Conta | `:feature:groups`, `:feature:group_detail`, `:feature:group_stats` |
| `home_see_all_bills_clicked` | none | "Ver tudo" over "Últimas despesas" | `:feature:groups` |
| `recovery_code_card_show_clicked` | none | "Ver código" on "Guarde seu código de recuperação" | `:feature:groups` |
| `recovery_code_card_connect_clicked` | none | "Conectar conta" on that card | `:feature:groups` |
| `recovery_code_card_dismissed` | none | That card's ✕ | `:feature:groups` |
| `group_stats_clicked` | none | The group's Estatísticas | `:feature:group_detail` |
| `group_tab_clicked` | `tab` (`expenses`, `balances`, `members`) | Despesas, Saldos or Membros | `:feature:group_detail` |
| `edit_group_clicked` | none | "Editar grupo" | `:feature:group_detail` |
| `group_pin_toggled` | `is_pinned` | "Fixar na Início" or "Desafixar da Início" | `:feature:group_detail` |
| `group_deleted_back_clicked` | none | "Voltar para Grupos" on "Grupo apagado", while it shows (Android's back or a tap outside on it too) | `:feature:group_detail` |
| `settle_clicked` | `source` (`group_detail`, `global_balances`) | "Acertar" on "Seu saldo neste grupo" or on a person of Saldos | `:feature:group_detail`, `:feature:global_balances` |
| `debt_clicked` | `source` (`group_detail`) | "Acertar", "Recebi" or "Já pagou" on a pair of Saldos | `:feature:group_detail` |
| `payment_clicked` | `source` (`group_detail`) | A payment of Despesas, which asks "Apagar este acerto?" | `:feature:group_detail` |
| `payment_delete_confirmed` | none | "Apagar este acerto?" confirmed, while it was open | `:feature:group_detail` |
| `payment_delete_cancelled` | none | "Apagar este acerto?" closed without confirming, while it was open | `:feature:group_detail` |
| `category_teach_clicked` | none | "Ensinar" on "Ensinar ao mapa?" | `:feature:group_detail` |
| `category_teach_dismissed` | none | "Agora não" on it, or Cancelar on which word, while it shows and isn't saving | `:feature:group_detail` |
| `category_teach_word_picked` | none | A word of the title picked to teach | `:feature:group_detail` |
| `category_teach_confirmed` | none | The word taught, once per word saved (not a second tap while it saves) | `:feature:group_detail` |
| `protect_access_clicked` | `source` (`group_detail`) | "Proteja seu acesso" | `:feature:group_detail` |
| `simplify_debts_info_clicked` | `source` (`group_detail`, `edit_group`) | "Como funciona" of "Simplificar dívidas" | `:feature:group_detail`, `:feature:edit_group` |
| `simplify_conflict_activity_clicked` | `source` (`group_detail`, `edit_group`) | "Ver na Atividade" on the simplify conflict's notice | `:feature:group_detail`, `:feature:edit_group` |
| `group_icon_clicked` | none | The avatar or "Trocar ícone ou foto", which opens "Ícone do grupo" | `:feature:edit_group` |
| `group_icon_color_picked` | `color` | A color in "Ícone do grupo", while it is open | `:feature:edit_group` |
| `group_icon_shape_picked` | `shape` | A shape in "Ícone do grupo" (Android), while it is open | `:feature:edit_group` |
| `group_icon_confirmed` | none | "Usar este ícone" | `:feature:edit_group` |
| `group_color_selected` | `color`, `source` (`edit_group`) | "Cor do ícone" on the web and the Mac | `:feature:edit_group` |
| `default_split_clicked` | none | Editar grupo's "Divisão padrão" | `:feature:edit_group` |
| `edit_group_save_clicked` | none | Editar grupo's Salvar or the keyboard's Done; also a Done the form refuses (nothing changed) | `:feature:edit_group` |
| `simplify_debts_clicked` | none | The switch of "Simplificar dívidas", which asks first | `:feature:edit_group` |
| `simplify_debts_toggled` | `is_on` | Its confirmation, which turns it on or off | `:feature:edit_group` |
| `simplify_debts_cancelled` | none | Its confirmation closed without answering, while it was open | `:feature:edit_group` |
| `default_split_mode_selected` | `mode` | Igual, Partes, Porcentagem or Pela renda | `:feature:edit_group` |
| `default_split_person_toggled` | `is_included` | A person in or out of Igual, with where they ended up | `:feature:edit_group` |
| `default_split_parts_changed` | `direction` (`up`, `down`) | A part added or taken | `:feature:edit_group` |
| `default_split_save_clicked` | none | Divisão padrão's Salvar | `:feature:edit_group` |
| `default_split_exception_add_clicked` | none | "Adicionar exceção" | `:feature:edit_group` |
| `default_split_exception_clicked` | none | An exception of the list | `:feature:edit_group` |
| `default_split_exception_category_picked` | none | The exception's category picked | `:feature:edit_group` |
| `default_split_exception_mode_selected` | `mode` | The exception's way to split | `:feature:edit_group` |
| `default_split_exception_person_toggled` | `is_included` | A person in or out of the exception's Igual, with where they ended up | `:feature:edit_group` |
| `default_split_exception_parts_changed` | `direction` (`up`, `down`) | A part added or taken in the exception | `:feature:edit_group` |
| `default_split_exception_save_clicked` | none | "Salvar exceção" | `:feature:edit_group` |
| `default_split_exception_remove_clicked` | none | The exception removed | `:feature:edit_group` |
| `pick_payment_pair_selected` | none | A pair of "Quem pagou a quem?", which opens Registrar pagamento for it | `:feature:register_payment` |
| `payment_full_debt_clicked` | none | "Toda a dívida" | `:feature:register_payment` |
| `payment_method_selected` | `method` (`pix`, `cash`, `transfer`, `other`) | "Como foi pago", another than the one picked | `:feature:register_payment` |
| `payment_date_clicked` | none | "Quando", opening its picker | `:feature:register_payment` |
| `payment_confirm_clicked` | none | Registrar pagamento's confirm button, also when the form can't record yet | `:feature:register_payment` |
| `payment_key_method_selected` | `method` (`pix`, `bitcoin`) | "Pagar Ana"'s Pix or Bitcoin, another than the one shown | `:feature:register_payment` |
| `payment_mark_as_paid_clicked` | none | "Marcar como pago" | `:feature:register_payment` |
| `payment_pay_otherwise_clicked` | none | "Paguei outro valor ou de outro jeito" | `:feature:register_payment` |
| `payment_paid_anyway_clicked` | none | "Já paguei mesmo assim" (13.5c) | `:feature:register_payment` |
| `payment_recorded` | `method`, `way` (`form`, `mark_as_paid`, `paid_anyway`), `kind` (`paid`, `received`, `between_others`), `groups` (how many payments, one per group), `is_full_debt` | A payment saved | `:feature:register_payment` |
| `balances_filter_selected` | `filter` (`all`, `you_owe`, `owes_you`), `source` (`global_balances`) | Tudo, Você deve or Te devem, another than the one picked | `:feature:global_balances` |
| `balances_person_expand_toggled` | `is_expanded` | "Por grupo" opened or closed on a row | `:feature:global_balances` |
| `balances_person_selected` | none | A person picked in the list beside the detail, while the filter lists them | `:feature:global_balances` |
| `received_clicked` | `source` (`global_balances`) | "Recebi" | `:feature:global_balances` |
| `charge_clicked` | `source` (`global_balances`) | "Cobrar" | `:feature:global_balances` |
| `charge_pix_key_toggled` | `is_included` | "Incluir minha chave Pix", while the switch takes it (not once the message was edited) | `:feature:global_balances` |
| `charge_share_clicked` | none | Cobrar's "Enviar", while Cobrar is open | `:feature:global_balances` |
| `charge_copy_clicked` | none | Cobrar's "Copiar mensagem", while Cobrar is open | `:feature:global_balances` |
| `balances_empty_action_clicked` | none | The empty state's button ("Ver quem te deve", "Ver quem você deve", "Ver grupos") | `:feature:global_balances` |
| `payment_key_type_selected` | `type` | Another type for the key (CPF, telefone, e-mail, aleatória…) | `:feature:payment_key` |
| `payment_key_paste_clicked` | none | The key's Colar | `:feature:payment_key` |
| `payment_key_default_toggled` | `is_default` | "Chave padrão", while it can change (not the only key of its method) | `:feature:payment_key` |
| `payment_key_save_clicked` | none | The key's Salvar, also when it can't save yet | `:feature:payment_key` |
| `payment_key_saved` | `type`, `is_new`, `is_default` | A key saved | `:feature:payment_key` |
| `payment_key_delete_clicked` | none | "Apagar chave", which asks first, while the key isn't saving | `:feature:payment_key` |
| `payment_key_delete_confirmed` | none | Its question confirmed, while it was open | `:feature:payment_key` |
| `payment_key_delete_cancelled` | none | Its question closed without confirming, while it was open | `:feature:payment_key` |
| `stats_period_selected` | `period` | A period of the menu | `:feature:group_stats` |
| `stats_custom_period_selected` | none | The days picked in "Escolher datas…" | `:feature:group_stats` |
| `stats_people_view_selected` | `view` (`consumption`, `paid`) | "Por pessoa"'s Consumo or Pagou, another than the one showing | `:feature:group_stats` |
| `stats_chart_type_selected` | `chart_type` | Barras or Linha, another than the one showing | `:feature:group_stats` |
| `stats_grain_selected` | `grain` | Dia, Semana, Mês or Ano, another than the one showing or one that switches the period to the whole one | `:feature:group_stats` |
| `stats_whole_period_clicked` | none | "Ver toda a viagem" of a period without bills | `:feature:group_stats` |
| `category_map_profile_clicked` | none | "Abrir Perfil" on "Seu mapa fica só neste aparelho" | `:feature:category_map` |
| `category_map_section_selected` | none | Another category picked beside its terms | `:feature:category_map` |
| `category_term_add_clicked` | `entry` (`map`, `category`, `search`) | "Adicionar termo", "Adicionar termo em Transporte" or "Adicionar “pizzaria”", once the map is read | `:feature:category_map` |
| `category_term_clicked` | none | A term, which opens "Editar termo", once the map is read | `:feature:category_map` |
| `category_term_category_clicked` | none | The term's category, opening its picker (not the tap that closes it) | `:feature:category_map` |
| `category_term_category_picked` | none | A category picked for the term | `:feature:category_map` |
| `category_term_save_clicked` | none | The term's Salvar (or "Mover para X"), also when it can't save yet | `:feature:category_map` |
| `category_term_saved` | `is_new` (from "Novo termo", also when it moves a word another category had) | A term saved | `:feature:category_map` |
| `category_term_remove_clicked` | none | "Remover do mapa" | `:feature:category_map` |
| `category_term_undo_clicked` | none | Desfazer on "“café” saiu do mapa", once the map is read | `:feature:category_map` |
| `category_map_restore_clicked` | none | "Restaurar padrão", which asks first, once the map is read | `:feature:category_map` |
| `category_map_restore_confirmed` | none | Its question confirmed, while it was open | `:feature:category_map` |
| `category_map_restore_cancelled` | none | Its question closed without confirming, while it was open | `:feature:category_map` |
| `category_create_clicked` | `is_subcategory`, `source` (`category_map`, `split_bill`) | "Nova categoria" or "Nova subcategoria em Transporte" in the map, once it is read and there is a group; in Categoria, "Nova", "Nova em Alimentação" or "Criar “Pet”" from a search | `:feature:category_map`, `:feature:split_bill` |
| `category_edit_clicked` | `source` (`category_map`, `split_bill`) | A group's category opened to edit, when its form opens: its pencil in the map, once read; a long press or right click in Categoria | `:feature:category_map`, `:feature:split_bill` |
| `category_group_picked` | `source` (`category_map`) | The new category's Grupo | `:feature:category_map` |
| `category_parent_picked` | `source` (`category_map`, `split_bill`) | The category's "Dentro de" | `:feature:category_map`, `:feature:split_bill` |
| `category_icon_picked` | `icon`, `source` (`category_map`, `split_bill`) | One of the 18 icons | `:feature:category_map`, `:feature:split_bill` |
| `category_save_clicked` | `source` (`category_map`, `split_bill`) | The category's Salvar, also when it can't save yet | `:feature:category_map`, `:feature:split_bill` |
| `category_saved` | `is_new`, `is_subcategory`, `source` (`category_map`, `split_bill`) | A category saved | `:feature:category_map`, `:feature:split_bill` |
| `category_delete_clicked` | `source` (`category_map`, `split_bill`) | The category's Apagar, which asks first | `:feature:category_map`, `:feature:split_bill` |
| `category_delete_confirmed` | `source` (`category_map`, `split_bill`) | "Apagar “X”?" confirmed, while it was open and not deleting yet | `:feature:category_map`, `:feature:split_bill` |
| `category_delete_cancelled` | `source` (`category_map`, `split_bill`) | "Apagar “X”?" closed without confirming, while it was open and not deleting yet | `:feature:category_map`, `:feature:split_bill` |
| `split_bill_group_clicked` | none | The new bill's group, which opens "Rachar em qual grupo?" | `:feature:split_bill` |
| `split_bill_group_picked` | none | A group marked in "Rachar em qual grupo?", while it is open, another than the one marked | `:feature:split_bill` |
| `split_bill_group_confirmed` | `is_another` | "Continuar" on it, while it is open, the group still there and the bill not saving | `:feature:split_bill` |
| `split_bill_payer_clicked` | none | "Quem pagou" | `:feature:split_bill` |
| `split_bill_payer_picked` | none | A person picked in "Quem pagou", while it is open | `:feature:split_bill` |
| `split_bill_payer_confirmed` | none | "Pronto" on "Quem pagou", while it is open | `:feature:split_bill` |
| `split_bill_several_payers_clicked` | none | "Várias pessoas" | `:feature:split_bill` |
| `split_bill_several_payers_confirmed` | `payers` (how many paid) | "Pronto" on "Várias pessoas pagaram", once the amounts add up | `:feature:split_bill` |
| `split_bill_date_clicked` | none | "Quando", opening its picker | `:feature:split_bill` |
| `split_bill_split_clicked` | none | "Divisão", which opens Dividir | `:feature:split_bill` |
| `receipt_items_clicked` | none | "14 itens" beside "Lido do cupom", which opens Itens, while the receipt's items are there in the bill's currency | `:feature:split_bill` |
| `split_bill_split_mode_selected` | `mode` (`equal`, `amount`, `percent`, `shares`, `items`) | Igual, Valores, %, Partes or Itens, another than the one picked | `:feature:split_bill` |
| `split_bill_person_toggled` | `is_included` | A person in or out of Igual, with where they ended up | `:feature:split_bill` |
| `split_bill_everyone_toggled` | `is_included` | "Marcar todos" or "Desmarcar todos" | `:feature:split_bill` |
| `split_bill_parts_changed` | `direction` (`up`, `down`) | A part added or taken | `:feature:split_bill` |
| `split_bill_split_reset_clicked` | none | "Voltar ao padrão" | `:feature:split_bill` |
| `split_bill_split_as_default_toggled` | `is_checked` | "Usar como padrão do grupo", while it shows | `:feature:split_bill` |
| `split_bill_split_confirmed` | `mode`, `is_split_as_default` | Dividir's "Pronto", once the split adds up (Itens has its own) | `:feature:split_bill` |
| `bill_items_confirmed` | `items` (how many) | "Pronto" on "Itens da conta", once the items add up | `:feature:split_bill` |
| `bill_item_add_clicked` | none | "Adicionar item" | `:feature:split_bill` |
| `bill_items_difference_item_clicked` | none | "Adicionar item de R$ 15" | `:feature:split_bill` |
| `bill_items_difference_split_clicked` | none | "Dividir a diferença", while there is one | `:feature:split_bill` |
| `bill_items_difference_removed` | none | The difference shared taken off | `:feature:split_bill` |
| `bill_item_clicked` | none | An item, which opens "Item" | `:feature:split_bill` |
| `bill_item_quantity_changed` | `direction` (`up`, `down`) | "Quantidade"'s stepper | `:feature:split_bill` |
| `bill_item_mode_selected` | `mode` | Igual, Unidades, Partes, Valor or %, another than the one picked | `:feature:split_bill` |
| `bill_item_person_toggled` | `is_included` | A person in or out of the item's Igual, with where they ended up | `:feature:split_bill` |
| `bill_item_person_changed` | `direction` (`up`, `down`) | A unit or a part added or taken for someone | `:feature:split_bill` |
| `bill_item_confirmed` | `mode`, `is_new` | "Pronto" on "Item", once it has a name, a price and someone | `:feature:split_bill` |
| `bill_item_delete_clicked` | none | "Apagar item", when its question opens | `:feature:split_bill` |
| `bill_item_delete_confirmed` | none | Its question confirmed, while it was open | `:feature:split_bill` |
| `bill_item_delete_cancelled` | none | Its question closed without confirming, while it was open | `:feature:split_bill` |
| `bill_items_fee_clicked` | `fee` (`service`, `couvert`) | The row of "Serviço" or "Couvert", when its field opens | `:feature:split_bill` |
| `bill_items_fee_confirmed` | `fee` | Its field's Pronto, with a value it takes | `:feature:split_bill` |
| `bill_category_clicked` | none | The icon beside "Descrição", which opens Categoria once the categories are read | `:feature:split_bill` |
| `bill_category_main_toggled` | `is_expanded` | A main category shown or hidden in "Todas as categorias" | `:feature:split_bill` |
| `bill_category_picked` | none | A category picked for the bill (a suggestion, a subcategory, "Só Alimentação", a result, Outros), one still there | `:feature:split_bill` |
| `category_map_clicked` | `source` (`split_bill`) | The "Mapa de categorias" card | `:feature:split_bill` |
| `receipt_read` | `result` (`read`, `unreadable`, `reader_unavailable`, `limit_reached`, `offline`), `items` (how many read) | How a receipt's read ended, which ends the funnel "Ler cupom fiscal" began | `:feature:split_bill` |
| `receipt_removed` | none | × on "Foto do cupom" | `:feature:split_bill` |
| `split_bill_save_clicked` | none | Rachar conta's Salvar, also when it can't save yet | `:feature:split_bill` |
| `bill_saved` | `is_new`, `split_mode`, `payers` (how many), `is_from_receipt`, `is_category_picked` (a new bill's, by hand), `is_split_as_default` (a new bill asking to save its split as the default) | A bill saved, new or edited | `:feature:split_bill` |
| `bill_delete_clicked` | `source` (`split_bill`, `bill_detail`) | "Apagar conta", when its question opens | `:feature:split_bill`, `:feature:bill_detail` |
| `bill_delete_confirmed` | `source` (`split_bill`, `bill_detail`) | "Apagar esta conta?" confirmed, while it was open and not deleting yet | `:feature:split_bill`, `:feature:bill_detail` |
| `bill_delete_cancelled` | `source` (`split_bill`, `bill_detail`) | "Apagar esta conta?" closed without confirming, while it was open and not deleting yet | `:feature:split_bill`, `:feature:bill_detail` |
| `bill_edit_clicked` | none | Conta's "Editar conta", while it can edit | `:feature:bill_detail` |
| `bill_share_clicked` | none | Conta's Compartilhar, which opens "Compartilhar conta" | `:feature:bill_detail` |
| `bill_undo_clicked` | none | "Desfazer" on Histórico's latest edit, when it undoes | `:feature:bill_detail` |
| `bill_attachment_clicked` | `source` (`bill_detail`, `shared_bill`) | An attachment's tile, which opens the viewer on it | `:feature:bill_detail` |
| `bill_attachment_removed` | none | "Remover foto" in the viewer, on a photo it can remove | `:feature:bill_detail` |
| `comment_send_clicked` | `is_reply` (never with an edit, which saves only its text), `is_edit`, `source` (`bill_detail`, `shared_bill`) | "Enviar comentário", when the composer can send (offline too in Conta, where it waits in the queue) | `:feature:bill_detail` |
| `comment_reply_clicked` | `source` (`bill_detail`, `shared_bill`) | "Responder", on a comment still there | `:feature:bill_detail` |
| `comment_reply_cancelled` | `source` (`bill_detail`, `shared_bill`) | The ✕ of "Respondendo a Diego", while a reply is picked | `:feature:bill_detail` |
| `comment_edit_clicked` | `source` (`bill_detail`, `shared_bill`) | "Editar" on the user's own comment, when the composer takes it | `:feature:bill_detail` |
| `comment_edit_cancelled` | `source` (`bill_detail`, `shared_bill`) | The ✕ of "Editando comentário", while an edit is under way | `:feature:bill_detail` |
| `comment_delete_clicked` | `source` (`bill_detail`, `shared_bill`) | "Apagar" on the user's own comment, when its question opens | `:feature:bill_detail` |
| `comment_delete_confirmed` | `source` (`bill_detail`, `shared_bill`) | Its question confirmed, while it was open | `:feature:bill_detail` |
| `comment_delete_cancelled` | `source` (`bill_detail`, `shared_bill`) | Its question closed without confirming, while it was open | `:feature:bill_detail` |
| `comment_like_toggled` | `is_liked`, `source` (`bill_detail`, `shared_bill`) | The heart, with where it ended up | `:feature:bill_detail` |
| `comment_likes_clicked` | `source` (`bill_detail`, `shared_bill`) | The count beside the heart, on a comment with likes | `:feature:bill_detail` |
| `comment_share_clicked` | `source` (`bill_detail`, `shared_bill`) | "Compartilhar" in a comment's menu | `:feature:bill_detail` |
| `bill_link_visibility_changed` | `visibility` (`members`, `anyone`) | "Quem pode abrir" switched, while the link can change | `:feature:bill_detail` |
| `bill_link_copy_clicked` | none | "Compartilhar conta"'s Copiar, with a link there | `:feature:bill_detail` |
| `bill_link_send_clicked` | none | "Enviar link", with a link there | `:feature:bill_detail` |
| `bill_link_disable_clicked` | none | "Desativar link", while the link can change | `:feature:bill_detail` |
| `shared_bill_sign_in_clicked` | none | The shared page's "Entrar" or "Entrar para comentar" | `:feature:bill_detail` |
| `shared_bill_open_in_app_clicked` | none | The shared page's "Abrir no app" | `:feature:bill_detail` |
