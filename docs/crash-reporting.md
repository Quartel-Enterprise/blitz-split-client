# Crash reporting

Firebase Crashlytics, on Android, iOS and macOS. The web has none: Firebase has no Crashlytics for the browser.

## How it fits

| Piece | Where | Does |
| --- | --- | --- |
| `CrashReporter` | `:core:provider:crashlytics` | Records a non-fatal: a failure the app handles but nobody expects |
| `startCrashReporting(isDebug)` | `:core:provider:crashlytics` | Turns Crashlytics on, off for a debug build, and on Apple hooks the Kotlin exceptions nobody catches |
| CrashKiOS | the module's `commonMain` | Calls Android's SDK and Apple's, so Apple's report holds the Kotlin stack, not only the abort |
| `BlitzSplitApplication` | `androidApp` | Starts it before Koin, in a build with the Firebase config |
| `CrashReporting.swift` | `iosApp/Sources/Shared/App` | `FirebaseApp.configure()`, then `BlitzSplitApple.startCrashReporting`, in a build with the Firebase config |

Until `startCrashReporting` runs, `CrashReporter` records nothing, so a build without Firebase, the web and the tests
need nothing of their own. Crashes are caught by each SDK by itself: the JVM's uncaught exceptions on Android, and on
Apple the Swift and Objective-C ones, plus the Kotlin ones through CrashKiOS' hook.

What is recorded as a non-fatal, and why:

| Where | What | Why |
| --- | --- | --- |
| `SyncCoordinator` | A sync that failed while online, once until a sync gets through again, as `SyncFailure`: each cause's class with the server's status and PostgREST code | Offline, or with a request that got no answer (`HttpRequestException`), a failure is expected; online it is a change the server keeps refusing or a step that broke, which fails every try and nobody else sees |

Never put a name, an amount or a group's id in what is recorded: the report goes to Google. So `CrashReporter` sends
only the names of a failure's classes, cause by cause, with its stack (Kotlin/Native keeps the stack of where the
report is made), never their messages: supabase-kt's hold the request's URL and PostgREST's details, a failing row's
values included. A failure written to be reported, with no cause, implements `ReportableFailure` and goes as it is.
A debug build records nothing, and every launch sets that again, since Crashlytics keeps the setting on the device.

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

## Check it

A release build with the config reports; a debug one doesn't. To see a report, build release, make the app crash once,
and open it again: Crashlytics sends a crash on the next launch.
