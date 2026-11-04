# Analytics

Firebase Analytics (GA4), on Android, iOS, macOS and the web, one Firebase project for all of them, the one
Crashlytics and Android's push notifications use ([crash-reporting.md](../crash-reporting.md)). Each platform sends
through its own official SDK; the shared code decides what is tracked, so the same action sends the same event on
every UI.

## How it fits

| Piece | Where | Does |
| --- | --- | --- |
| `TrackEvent` | `:core:provider:analytics`'s `domain/usecase` | How the shared code tracks an event: a name and its params. A Boolean goes as `"true"` or `"false"` (GA4 has no boolean), a whole number as a Long, any other as a Double, an enum as its name in lowercase; any other value is left out, never sent as its text |
| `AnalyticsHub` | `:core:provider:analytics` | Where the events go: once an app attaches its platform's `AnalyticsService`. The first 50 before that wait for it, so the opening screen counts on the web too, which attaches only once the page drew; Firebase dates them when they are sent, a moment late, since its SDKs take no timestamp |
| `FirebaseAnalyticsService` | `:core:provider:analytics`'s `androidMain` | Android's, attached by `BlitzSplitApplication` |
| `FirebaseSetup.swift` | `iosApp/Sources/Shared/App` | Apple's, attached through `BlitzSplitApple.attachAnalytics` once the shared code started |
| `firebaseAnalytics.ts` | `webApp/src/analytics` | The web's, through `firebase/analytics`, attached through the exported `attachAnalytics` |

Screen views are tracked ([below](#screen-views)); each user action's event comes with its feature, through
`TrackEvent`.

## Screen views

`StartScreenViews` (`:core:navigation`) sends GA4's `screen_view` each time another route comes on top of the back
stack and stays there for half a second, going back to one included, once per route: another group's Despesas is
another view, the same route drawn again is not. One gone at once never counts, nor does the screen under it coming
back: the Mac pops Perfil and Mapa de categorias right away to open them in Ajustes, so Ajustes is not a screen view
there (Notificações, kept inside the profile on the Mac and the web, isn't either). It starts once `AppViewModel` picked the first screen, so the welcome the stack holds before that never counts.
`screen_name` and `screen_class` (the web's adapter renames them `firebase_screen` and `firebase_screen_class`, which
its SDK reads) are the route's name (`RouteScreenView.kt`, a `when` with no `else`, so a new route
can't ship without one); a few routes add what kind of visit it was, never whose:

| `screen_name` | Params |
| --- | --- |
| `welcome`, `profile_setup`, `restore`, `home`, `groups`, `search`, `profile`, `create_group`, `edit_profile`, `profile_photo`, `delete_account`, `notification_settings`, `category_map`, `group_stats`, `edit_group`, `default_split`, `share_bill`, `pick_payment`, `contacts`, `contact_profile`, `contacts_agenda`, `settle_up`, `join_pick` | none |
| `invite` | `is_new_group` |
| `group_detail` | `tab` (`expenses`, `balances`, `members`: the tab it opens on; a tab picked inside it is a click, not a view), `is_join` |
| `split_bill` | `is_edit`, `picks_group_first`, `scans_receipt` |
| `bill_detail`, `shared_bill` | `is_comment` (opened at a comment) |
| `register_payment` | `offers_keys` ("Pagar Ana") |
| `global_balances` | `filter` (`all`, `you_owe`, `owes_you`), `is_charge` |
| `contact_invite` | `is_guest` |
| `activity` | `is_person` |
| `join_invite` | `is_personal` |
| `join_with_link` | `is_retry` |
| `payment_key` | `method` (`pix`, `bitcoin`), `is_new` |

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
