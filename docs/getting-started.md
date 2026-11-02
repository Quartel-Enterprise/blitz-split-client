# Getting started

## Prerequisites

- JDK 21 or higher
- Android Studio, with the Android SDK, for the Android app
- macOS Tahoe 26.6 or later with [XcodeGen](https://github.com/yonaskolb/XcodeGen) (`brew install xcodegen`) and an
  Xcode with the iOS 27.1 SDK or newer, Xcode 27.1 at the least, for the iOS, iPadOS and macOS app. That SDK has the
  iPhone Duo APIs (`reservedRegions(kind: .division)`), which the app uses behind `#available(iOS 27.1, *)` while
  still running on iOS 26, and its Xcode the iPhone Duo simulator
- Node.js with npm (a current LTS release), for the web app

## Local configuration

`local.properties` stays out of the repository. It only needs the Android SDK path, which Android Studio writes on
the first sync; from the terminal, create it yourself:

```properties
sdk.dir=/Users/<you>/Library/Android/sdk
```

### Supabase

The shared code talks to the hosted Supabase project **Blitz Split** out of the box: its URL and publishable key
are defaults in `core/provider/supabase/build.gradle.kts`. The publishable key is public by design (it ships in
every client) and Row Level Security protects the data.

BuildKonfig compiles three settings into `SupabaseConfig`. Each one can be overridden by a Gradle property (`-P`,
or `~/.gradle/gradle.properties`) or an environment variable of the same name, for instance to point a build at a
local `supabase start`:

| Setting | Default |
| --- | --- |
| `SUPABASE_URL` | the hosted project |
| `SUPABASE_PUBLISHABLE_KEY` | the hosted project's publishable key |
| `SUPABASE_GOOGLE_WEB_CLIENT_ID` | empty |

`SUPABASE_GOOGLE_WEB_CLIENT_ID` is the Google OAuth **web** client id that Credential Manager asks Google to sign the
ID token for. While it is empty the app builds and runs, but the native Google sign in on Android fails; set it to
use that path:

```properties
# ~/.gradle/gradle.properties
SUPABASE_GOOGLE_WEB_CLIENT_ID=<web client id>.apps.googleusercontent.com
```

Some Supabase settings are not in migrations and are set once, by hand, in the project's dashboard: anonymous
sign ins, the Google and Apple providers with their authorized client ids, the redirect URL
`borarachar://auth-callback` and the site URL of the web app. The list is in `docs/auth.md` of the
`blitz-split-api` repository; a sign in that fails right after a fresh setup is usually one of them.

## Build and run the Android app

Run the `androidApp` module from Android Studio, or build it from the terminal:

```shell
./gradlew :androidApp:assembleDebug
```

A debug build is signed with your machine's `~/.android/debug.keystore`, so a group's invite link
(`blitzsplit.com/g/<code>`) only opens the app once that key's SHA-256 is in blitzsplit.com's `assetlinks.json`:
otherwise Android fails the App Links verification and the link opens in the browser.
`adb shell pm get-app-links com.quare.blitzsplit` says `verified` once it works, `legacy_failure` while the key is
missing. Add the fingerprint that `keytool -list -v -keystore ~/.android/debug.keystore -storepass android` prints to
`webApp/public/.well-known/assetlinks.json` and to the same file in `blitz-split-site`, whose pushes to `main` deploy
blitzsplit.com. Android reads the file through Google's Digital Asset Links API, which keeps the old one for up to an
hour (the `maxAge` of
`https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://blitzsplit.com&relation=delegate_permission/common.handle_all_urls`):
until it lists your key, verification stays pending (a number such as `1024`). Then run
`adb shell pm verify-app-links --re-verify com.quare.blitzsplit`.

## Build and run the Apple app

The Xcode project is generated, not versioned. Generate it, then open it:

```shell
./iosApp/generate-project.sh
open iosApp/BoraRachar.xcodeproj
```

Run the `BoraRachar-iOS` scheme on an iPhone, iPad or iPhone Duo simulator or the `BoraRachar-macOS` scheme on the
Mac. A
pre-build phase runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode`, so the first build also compiles the
Kotlin framework and takes a while. Rerun `generate-project.sh` after adding or removing Swift files, or after
changing `iosApp/project.yml` or `version.properties`.

The project signs automatically with the team in `project.yml`; Sign in with Apple and the `webcredentials`
associated domain need a team that holds those capabilities for `com.quare.blitzsplit`.

A group's invite link opens the app through the `applinks` associated domain, once the system has downloaded
blitzsplit.com's `apple-app-site-association` after the install: a link opened in the first seconds still goes to
Safari. Test it with `xcrun simctl openurl <udid> https://blitzsplit.com/g/<code>`, or by tapping the link in Notes
or Messages; a link typed into Safari's address bar always stays in Safari.

## Build and run the web app

The web app consumes the shared Kotlin as the npm package `blitzsplit-shared`, a local dependency on
`shared/build/dist/js/productionLibrary`. Build it once before installing:

```shell
./gradlew :shared:jsBrowserProductionLibraryDistribution
npm --prefix webApp install
```

Then run the dev server, or build `webApp/dist`:

```shell
npm --prefix webApp run dev
npm --prefix webApp run build
```

Both rebuild the shared library first (the `predev` and `prebuild` scripts). Vite serves on
http://localhost:5173. The local database lives in the browser's OPFS storage for that origin, and only one tab can
hold it at a time: a second tab says the app is open in another tab and opens once the first closes, as soon as
it's visible. Clear the database from DevTools → Application → Storage → Clear site data to start over.

## Check your change

```shell
./gradlew jvmTest            # the unit tests
./gradlew :koverVerifyCi     # 80% line coverage of core and feature
./gradlew assertModuleGraph  # the module dependency rules
./scripts/ktlint.sh          # Kotlin style (--format to autocorrect)
./scripts/cpd.sh             # no new copied code in Kotlin, Swift and TypeScript
```

## Next steps

- [Architecture guide](ai_agents.md): the conventions every module follows, and the checklist a new feature goes
  through.
- [Code style](architecture/code-style.md): the ktlint rules, and the Swift and TypeScript conventions.
- [Testing](testing/README.md): what is tested, and how.
