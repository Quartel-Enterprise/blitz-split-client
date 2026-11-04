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
| `EventAnalytics.Automatic(name, params)` | The event and its params are known from the `UiEvent` itself | `TrackedViewModel.onEvent`; the ViewModel never calls `trackEvent` for it |
| `EventAnalytics.Manual(name)` | It needs what only handling it tells: the result of a save, the `UiState` (`setOf(…)` for a branch that may send more than one) | A `trackEvent("name", …)` call in the branch, with the same name |
| `EventAnalytics.NotTracked` | Not a user action at all: a scroll, an animation ending, a frame or a window measured, a value typed letter by letter (what was typed shows in the action that saves it), a result the platform hands back | Nothing |

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
