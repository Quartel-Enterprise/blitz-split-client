# Crash reporting

Firebase Crashlytics on Android, iOS and macOS, and Sentry on the web, since Firebase has no Crashlytics for the
browser.

## How it fits

| Piece | Where | Does |
| --- | --- | --- |
| `CrashReporter` | `:core:provider:crashlytics` | Records a non-fatal: a failure the app handles but nobody expects |
| `startCrashReporting(isDebug)` | `:core:provider:crashlytics` | Turns Crashlytics on, off for a debug build, and on Apple hooks the Kotlin exceptions nobody catches |
| CrashKiOS | the module's `commonMain` | Calls Android's SDK and Apple's, so Apple's report holds the Kotlin stack, not only the abort |
| `BlitzSplitApplication` | `androidApp` | Starts it before Koin, in a build with the Firebase config |
| `FirebaseSetup.swift` | `iosApp/Sources/Shared/App` | `FirebaseApp.configure()`, then `BlitzSplitApple.startCrashReporting`, in a build with the Firebase config |
| `sentry.ts` | `webApp/src/crash` | Starts Sentry in a production build with the DSN, redacts every report, and hands `attachCrashReporter` (`shared/src/jsMain`) where the shared code's non-fatals go |

Until `startCrashReporting` runs, or the web attaches its reporter, `CrashReporter` records nothing, so a build
without Firebase or without Sentry's DSN and the tests need nothing of their own. Crashes are caught by each SDK by
itself: the JVM's uncaught exceptions on Android, on Apple the Swift and Objective-C ones, plus the Kotlin ones through
CrashKiOS' hook, and on the web the page's uncaught errors and rejected promises, a React render that threw included.

What is recorded as a non-fatal, and why:

| Where | What | Why |
| --- | --- | --- |
| `SyncCoordinator` | A sync that failed while online, once until a sync gets through again, as `SyncFailure`: each cause's class with the server's status and PostgREST code | Offline, or with a request that got no answer (`HttpRequestException`), a failure is expected; online it is a change the server keeps refusing or a step that broke, which fails every try and nobody else sees |

Never put a name, an amount or a group's id in what is recorded: the report goes to Google, or to Sentry. So `CrashReporter` sends
only the names of a failure's classes, cause by cause, with its stack (Kotlin/Native keeps the stack of where the
report is made), never their messages: supabase-kt's hold the request's URL and PostgREST's details, a failing row's
values included. A failure written to be reported, with no cause, implements `ReportableFailure` and goes as it is.
A debug build records nothing, and every launch sets that again, since Crashlytics keeps the setting on the device.

The web applies the rule to everything it sends, in `sentry.ts`' `beforeSend`, since an uncaught Kotlin exception
reaches the browser with its message:

- An uncaught error goes with its message blank, cause by cause: its class and stack only. A non-fatal of
  `CrashReporter` keeps the message it arrives with, already its class names or written to be reported.
- Of the stack, only the frames at a line of the app's own scripts go. Sentry reads a stack line by line, and an
  uncaught Kotlin failure's holds its message and its causes', which a URL in them would turn into a frame. An error
  left with no such frame isn't sent at all: it came from an extension or an injected script, or it is a rejected
  promise with nothing to read.
- No page address (a group's invite code is in it), no breadcrumbs (requests, taps, the console), no session, no user
  and nothing a rejected promise carried: only the integrations `sentry.ts` lists run, so one a newer Sentry turns on
  by default stays out.
- Every `dataCollection` option is off, `userInfo` included, which keeps Sentry from taking the request's IP address;
  the Sentry project has **Prevent Storing of IP Addresses** on as well.

Like Crashlytics, Sentry doesn't follow Profile's "Share usage data" ([Consent](analytics/README.md#consent)): a report
carries no usage. A dev server (`npm run dev`) reports nothing.

## What stays off until it is set up

Every build runs without it, CI's included: each app starts Firebase only when its config file is there, git-ignored.
The same Firebase project serves Atividade's push notifications on Android ([push-notifications.md](push-notifications.md)).

### Android

The Android app `com.quare.blitzsplit` of the Firebase project: download its `google-services.json` (Project settings ›
Your apps) to `androidApp/google-services.json`. The build then applies the google-services plugin, which makes the
resources Firebase starts from, and a release build uploads R8's mapping to Crashlytics, so the stacks read with the
real names. Without the file the build skips both (`HAS_FIREBASE` is false in `BuildConfig`).

### iOS and macOS

An Apple app `com.quare.blitzsplit` in the same Firebase project, for both platforms: download its
`GoogleService-Info.plist` to `iosApp/Resources/GoogleService-Info.plist`, then generate the project again
(`./iosApp/generate-project.sh`), which only takes the file in when it is there. A release build uploads its dSYM, the
last build phase, which holds the Shared framework's symbols too, since it is linked into the app. The first build
after this resolves the Firebase Swift package.

### The web

The Sentry project `blitz-split-web`, of the Browser JavaScript platform with only Error monitoring, in the
`quare-software` organization (US data region, the Developer plan's 5,000 errors a month, shared with its other
projects). Turn on **Prevent Storing of IP Addresses** (Settings › Security & Privacy) and leave tracing, replays and profiling off: the app only
sends errors. Then, in GitHub ([release-process.md](release-process.md#secrets)):

| Name | Kind | What it is |
| --- | --- | --- |
| `VITE_SENTRY_DSN` | Variable | The project's DSN (Settings › Client Keys). Public: it ships in the page |
| `SENTRY_ORG`, `SENTRY_PROJECT` | Variable | The organization's and the project's slugs |
| `SENTRY_AUTH_TOKEN` | Secret | An organization token (Settings › Auth Tokens), which only uploads source maps |

Without the DSN the build has no Sentry at all, and the page never downloads it. With it, a non-fatal the shared code
records while Sentry's script still loads waits for it; an uncaught error that early isn't reported. With the token, the `web-deploy`
action builds with source maps, uploads them under the release's version and deletes them before the deploy, so
Sentry shows the TypeScript's own lines and, for the shared code, the JavaScript Kotlin compiled it to, with the
Kotlin names (Vite doesn't follow the library's own maps back to the `.kt` files); without it the reports arrive with the minified stack. To
try it from this machine, put the DSN in `webApp/.env.local` (`VITE_SENTRY_DSN=<dsn>`) and serve a production build
(`npm --prefix webApp run build`, then `preview`).

## Check it

A release build with the config reports; a debug one doesn't. To see a report, build release, make the app crash once,
and open it again: Crashlytics sends a crash on the next launch.

On the web, a production build with the DSN reports as soon as the error happens, and it shows in Sentry's Issues by
its class, with no message. An error thrown from the browser's console isn't sent, since none of its frames is the
app's: to see a report, throw from the app's code in a local production build (`webApp/.env.local` above).
