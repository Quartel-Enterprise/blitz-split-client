# Store listing screenshots

The Google Play and App Store screenshots are **generated, not taken by hand**. Each one is a real screen of the app,
showing a sample trip among five friends, inside a device frame with a title and description, in all 13 languages
of the listings. The frames come from the [store-screenshots](https://github.com/lucianosantosdev/store-screenshots)
library, as in Bible Planner. None of the images is versioned: every run rebuilds them from source.

The two stores draw the screen differently, because the app has a native UI on each:

- **Google Play.** Robolectric renders the real Jetpack Compose screen inside the frame, from `UiState`s built from
  the sample trip. Any machine can do it.
- **App Store.** The UI is SwiftUI, which Robolectric can't draw, so the screens are **captured on the iOS
  simulator** by an XCUITest target. It launches a debug build seeded with the same sample trip and writes each whole
  screen to a PNG. The Mac app's window is photographed on the Mac itself, by a script. Robolectric then puts each
  PNG into its device's frame, with the same banner. This half **needs a Mac with Xcode**.

## One command

```bash
fastlane screenshots                                 # everything, then upload
fastlane screenshots skip_upload:true                # generate only, nothing reaches a store
fastlane screenshots platform:android skip_upload:true
fastlane screenshots platform:ios skip_capture:true  # frame the iOS captures already in build/ again
fastlane upload_screenshots locale:es-419            # upload what is staged, one locale
fastlane upload_screenshots platform:mac             # upload the Mac's alone
```

A stable release runs the same steps in CI, in jobs of `release-production`: the capture on one runner per slot, the
frames, and the upload to each store ([release-process.md](release-process.md#screenshots-with-a-release)). Run the
lane by hand between releases, or when a release's screenshot jobs failed.

The lane runs, each step under `scripts/heavy-check.sh`:

1. `scripts/store-screenshots/capture-ios.sh`: builds the `BoraRachar-iOS-StoreScreenshots` UI tests and runs them
   on one simulator per App Store slot (the iPhone Duo's two slots, closed and open, share theirs). They write
   `build/outputs/apple-screenshot-captures/<slot>/<locale>/`.
   Then `scripts/store-screenshots/capture-mac.sh` builds the Mac app and photographs its window itself, with no
   UI test ([why](#how-the-mac-capture-works)). **It takes this Mac's own screen for about half an hour**, so leave
   the keyboard and mouse alone, or pass `skip_mac:true` to leave the Mac listing as it is. Whatever runs it needs
   the Screen Recording permission (System Settings › Privacy & Security).
   `STORE_SCREENSHOTS_BUILD_SETTINGS` adds build settings to the Mac build, which the release's runner uses to sign
   the app to run locally, having no signing account.
2. `./gradlew stagePlayStoreScreenshots stageAppStoreScreenshots collect…`: the generators (the unit tests of
   `:android:store_screenshots`) draw every image. The tasks then copy the images to `store_listings/` to look at, and
   to the folders fastlane uploads from. Latin America's Spanish goes out to each store's regional codes (`es-419` and
   `es-US` on Play, `es-MX` on the App Store), and Spain's to `es-ES`, as the listing text does
   ([store-listing.md](store-listing.md)).
3. Unless `skip_upload:true` is passed, the upload: `supply` for Play and `deliver` for the App Store, listing
   images only.

Without fastlane, the Gradle tasks alone:

```bash
./gradlew collectPlayStoreScreenshots    # Play, any machine: store_listings/android/<locale>/<device>/
./scripts/store-screenshots/capture-ios.sh && ./gradlew collectAppStoreScreenshots   # App Store, needs a Mac
./gradlew collectStoreScreenshots        # both (the iOS captures must be there)
```

A quick look at a few iOS shots:
`STORE_SCREENSHOTS_SLOTS=iphone67 STORE_SCREENSHOTS_LOCALES=en-US,ja STORE_SCREENSHOTS_SCREENS=group,items ./scripts/store-screenshots/capture-ios.sh`.
`store_listings/`, `fastlane/screenshots/` and `fastlane/metadata/android/*/images/` are git-ignored.

### Uploading

**Nothing goes to a store without Pierre's OK.** `skip_upload:true` is the safe default to try anything. The upload
needs:

| Store | Environment |
| --- | --- |
| Google Play | `PLAY_STORE_JSON_KEY_PATH`: a service account key with **Manage store presence** |
| App Store | `APP_STORE_CONNECT_API_KEY_ID`, `APP_STORE_CONNECT_API_ISSUER_ID`, and `KEY_PATH` (the `.p8`) or `APP_STORE_CONNECT_API_KEY_P8` (base64). `APP_VERSION` optionally names the version to put them on |

The rules the upload follows:

- **Each locale needs its listing in the store first.** Play and App Store Connect attach screenshots to a language
  the listing already has: add its title and description there before uploading its images.
- **Play** sends only the images whose SHA-256 changed (`sync_image_upload`). It tries a Play 5xx three times, 30
  seconds apart. `VALIDATE_ONLY=true` checks the upload against the API without publishing.
- **The Mac app is a version of its own** on App Store Connect: its screenshots (`fastlane/screenshots-mac/`) go up
  through a second `deliver` with `platform: osx`, and only when they were staged.
- **App Store** screenshots only change on an editable version. "Screenshot Set Already Exists" is tried three
  times, and a shelf App Store Connect won't reorder doesn't fail an upload that landed. After an upload of every
  locale, `tidy_app_store_screenshots` removes the duplicates deliver can leave and sorts each shelf by file name.
  `fastlane finish_app_store_screenshots` runs that tidy-up on its own.
- Both lanes refuse to upload from an empty folder, which would wipe the listing.

## What the images show

| # | Screen | Play | App Store | Shows |
| --- | --- | --- | --- | --- |
| 01 | The group, Expenses | ✓ | ✓ | Who paid what, your balance on top |
| 02 | Split by items | ✓ | ✓ | "Bill items" of the trattoria dinner, everyone marked |
| 03 | Receipt read | ✓ | ✓ | A new bill filled in from the receipt's photo, "6 items" |
| 04 | Balances | ✓ | ✓ | The group's Balances tab, who owes whom |
| 05 | Invite | ✓ | ✓ | The link and its QR code |
| 06 | Statistics | ✓ | ✓ | Total, the days, by category |
| 07 | Home | ✓ | ✓ | Balances with people, the three pinned groups |
| 08 | Several payers, another currency | ✓ | ✓ | The boat tour: two people paid it, in euros (dollars for a euro group) |
| 09 | Activity | | ✓ | The feed: who added what, who paid you |
| 10 | Light theme | | ✓ | The group again, light: "light or dark, it follows you" |

The file prefix is the **shelf order**: both stores sort by file name, and only the first two or three show
without swiping. Play holds 8 per device type and the App Store 10 per device size, so 09 and 10 are App Store only
(`StoreScreen.isAppStoreOnly`), and the iPhone Duo's two displays share their ten
([below](#what-the-images-show)). Every shot is in the dark theme except 10, on the logo's blue dimmed to `#0E2433`.

**The sample trip** (`shared/src/commonMain/.../storescreenshots/SampleTrips.kt`) is one story told in each
language: a trip to Lisbon among five friends, its names and descriptions in that language and its amounts in that
language's currency, at round rates and rounded the way a menu there prices them. The faces are AI-generated
portraits of people who don't exist (`scripts/store-screenshots/faces/`), the same five in every language; the
receipt's photo is `scripts/store-screenshots/receipt.jpg`. The three groups have a photo too, AI-generated as well:
`SampleTrips` names each one by its file in the image bank, `scripts/store-screenshots/bank/groups/`, so no group is
drawn as initials on a color. [The bank](../scripts/store-screenshots/bank/README.md) holds more faces and group
photos than the screenshots use, to pick from.

| Device | Play | App Store (Apple's screenshot specifications, November 2026) |
| --- | --- | --- |
| Phone | `phoneScreenshots`, 1242x2484 | **Dynamic Island (medium), 1206x2622, the one App Store Connect requires**, from an iPhone 17 Pro. Dynamic Island (large), 1290x2796, from an iPhone 16 Plus. Face ID (large) is only a fallback for the large size, so it isn't made |
| Foldable | | iPhone Duo, from the Duo simulator (on the runtime that has it): closed, as it boots, its outer display at 1398x2034, and open, its inner display lying down at 2853x2007 (XCTest hands the capture as 2852x2006, which the frame draws on the display's own canvas). Optional until April 2027, when apps built with the iOS 27.1 SDK must have it |
| Tablet | `sevenInchScreenshots`, `tenInchScreenshots` | 13" (2048x2732) from an iPad Air 13", and 11" framing the same capture on a taller canvas |
| Mac | | The Mac app's window at 1440x900 points, framed on 2880x1800 (16:10) |

**The Duo open needs Device Hub.** The simulator boots closed, and its hinge moves only from Device Hub: its Open
button, in the bar under the device, sends the simulator a HID event that neither `simctl`, `devicectl` nor XCUITest
sends. So for the `iphoneduoopen` slot `capture-ios.sh` opens the Device Hub of the Xcode in use and tries to select
the simulator and press Open through accessibility, which the terminal running it must be allowed to (System Settings
› Privacy & Security › Accessibility). Where it fails, the script says so and waits two minutes for a person to press
Open (`STORE_SCREENSHOTS_OPEN_TIMEOUT`, in seconds), then skips the slot and goes on with the others. It knows the
Duo is open by its inner display (`xcrun simctl io <udid> screenshot --display=3`), which then draws lying down. Open,
the outer display is off and still XCTest's main screen, so the UI test photographs the larger of the two screens.

What was checked of that, in November 2026:

- **The script's own press has not opened a Duo anywhere yet.** On this project's Mac, the shell it was run from had
  no Accessibility permission ("osascript is not allowed assistive access"), so the script waited and Open was
  pressed in Device Hub for it. The 13 languages were captured that way, and everything after the press ran as
  written: the script saw the inner display lying down and the UI test photographed it.
- **In a release**, the slot has a runner of its own, with nobody to press Open, so once its own attempt is over it
  waits 30 seconds more and gives up. The `xcode-27` image has what the script needs on paper: Xcode 27.2 with Device Hub inside it,
  the iPhone Duo's runtime (iOS 27.1 and 27.2), a logged-in desktop, and `osascript` and `bash` allowed in
  Accessibility and to drive System Events (the image's `configure-tccdb-macos-27.sh`). Whether Device Hub shows its
  window there and the press lands is only known after the first release with `screenshots` on: nothing but a
  release runs that job.

**The Duo's two displays share one shelf.** App Store Connect has one iPhone Duo device size for both displays, outer
(1398x2034) and inner (2853x2007), and takes ten screenshots per device size
([Screenshot specifications](https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications/)).
deliver (fastlane 2.241.2) files both sizes under `APP_IPHONE_DUO` and skips whatever comes after the tenth by file
name, so twenty images would put the first five screens up twice and leave the other five out. Each screen
therefore goes up once, closed or open:

| Open (the inner display) | Closed (the outer display) |
| --- | --- |
| 01 The group, 04 Balances, 06 Statistics, 07 Home, 09 Activity | 02 Split by items, 03 Receipt read, 05 Invite, 08 Several payers, 10 Light theme |

Open are the screens that use the inner display's width: the group and Home in two columns, Statistics, Activity's
feed beside the item picked. Closed are the bill's sheets, which open are a sheet over the screen behind, Invite, and
the light theme, so the group shows both ways. The shelf keeps the file names' order, so it starts open, on the
group. The list is `duoScreensStagedOpen` in `build-logic/.../StoreScreenshots.kt`: a default nobody chose for the
product yet, so it is Pierre's to change, in that one line. A display that wasn't captured gives its screens to the
other one, so a release whose open capture failed uploads the ten closed, as before. `collectAppStoreScreenshots`
still frames both whole, into `store_listings/ios/<locale>/iphone_duo/` and `iphone_duo_open/`.

Not checked yet, since no Duo screenshot has gone to App Store Connect: that it files both sizes on that one shelf,
upright and lying down side by side. Apple's page lists the four sizes under the one device and fastlane's change
says its upload was tried, but the API's own `ScreenshotDisplayType` didn't list `APP_IPHONE_DUO` in version 4.6 of
its specification (its newer asset library does, as the display class `IPHONE_DUO`). Should App Store Connect refuse
a size, deliver fails after the other devices' screenshots are up; the lane still sorts the shelves that landed, then
fails, the job is listed as failed and the release goes on
([release-process.md](release-process.md#screenshots-with-a-release)).

**The iPad runs in full-screen apps.** iPadOS 26 opens apps in windows by default, which would leave the app short
of the screen. On an iPad slot, the UI test first switches Settings › Multitasking & Gestures to full-screen apps.

store-screenshots has no frame for the Duo or the Mac. They get a plain rounded bezel and a window shadow
(`CustomBezels.kt`), with the same banner as the other shots.

## Where to edit

```
shared/src/commonMain/.../storescreenshots/   # the sample trip, its seed and the screen each capture opens
shared/src/appleMain/.../storescreenshots/    # the Apple captures' Room file and photos folder
android/store_screenshots/src/test/.../        # the Play screens' UiStates, banner copy, the frames
iosApp/UITests/StoreScreenshotsUITests.swift   # the iOS capture
shared/src/jsMain/.../StoreScreenshotsWeb.kt   # the web's seed, for the README's image
scripts/readme-hero/                           # the README's image
scripts/store-screenshots/capture-mac.sh       # the Mac capture
scripts/store-screenshots/*.tsv                # the languages, screens and photos both captures read
build-logic/.../StoreScreenshots.kt            # the collect and stage tasks, the stores' locale codes
fastlane/Fastfile                              # the screenshots and upload lanes
```

- **Banner copy:** `BannerCopies.kt`, per screen and language. The frame wraps the description to two lines, so keep it
  short.
- **A new screen:** an entry in `StoreScreen` with its file name, a `@Test` in `StoreScreenshots` and its
  `…ScreenshotContent` (the Play screen), an entry in `StoreScreenshotsScreen` (its routes on Apple's) and a row
  of `scripts/store-screenshots/screens.tsv`. The file names must match on both sides. It goes on the iPhone Duo's
  shelf closed unless `duoScreensStagedOpen` (`StoreScreenshots.kt`) names it; a test of `store-listing.test.mjs`
  holds that list, `StoreScreen` and `screens.tsv` to the same file names.
- **A new language:** `storeLocales` (its `playLocale` is its tag), `bannerCopies`, `SampleTrips` (copy and currency), a row of
  `scripts/store-screenshots/locales.tsv`, and `listingLocales` and the two store maps in `StoreScreenshots.kt`. Its `language` is the app language's code,
  which keys the banner copy and the trip. Portugal's Portuguese goes by its whole tag, `pt-PT`, since `pt` is
  Brazil's: its own banners and trip (in euros, "tu", Blitz Split), over the app's `pt-PT` strings. Spain's Spanish
  goes by `es-ES` the same way, since `es` is Latin America's: its own banners and trip ("ha pagado", "coche",
  "ticket"), over the app's `es-ES` strings.

## The region Play's screens are drawn in

Robolectric draws each Play set in its `StoreLocale.playLocale` (`StoreLocale.kt`): the language the resources are
read in and the region the amounts, dates and hours are written in. It is the set's own tag for every set but Latin
America's Spanish, whose tag, `es`, has no region: a bare `es` writes the symbol after the amount, as Spain does
("84,08 €", "120 US$"), while the same listing's App Store set, captured in Equatorial Guinea's region, writes
"€84,08" and "US$120". So that set is drawn in **`es-GQ`** (`LATIN_AMERICAN_PLAY_LOCALE`), **the region of the `es`
line of `scripts/store-screenshots/locales.tsv`**, which is Pierre's choice. Nothing reads that line for Play: if it
moves to another region, move this constant by hand, generate both sets and compare them, since Android's data and
Apple's don't always agree on a region (in the United States', Apple writes the euro as "€" and Android as "EUR").

- **The region alone does it.** Unicode's data, which Android and the generators' JVM format with, puts the symbol
  in front in `es_GQ` by itself, with a 24-hour clock and the day before the month: "€84,08", "US$120", "21:00",
  "15 nov", and "19–23 oct" for Statistics' range, as iOS writes them. Nothing is taught to the JVM.
- **The strings stay Latin America's.** `es-GQ` reads `values-es` ("Agregar"), never `values-es-rES` ("Añadir").
- **The folder stays `es`.** The library names an image's folder after the locale it drew it in, so the generator
  moves `es-GQ/` to `es/` (`moveToListingFolder`), where the collect and stage tasks look.
- **One value still differs: the thousands separator.** The App Store's set writes the trip's total as "€1145,80",
  and Play's as "€1.145,80". Spanish only groups from five digits on ("12.345,60"), a rule iOS follows and
  `java.text.NumberFormat`, which the Android app writes amounts with, doesn't: not the JVM's, which draws the
  generators, and not ICU's, which a phone has (ICU4J 79.1 writes "€1.145,80" through that API too, and "€1145,80"
  only through its newer `NumberFormatter`; not checked on a device). So it is not the region's doing, and no locale
  changes it: the bare `es` wrote "1.145,80 €", and Spain's set has the same difference ("1.145,80 €" on Play,
  "1145,80 €" on the App Store), as have Polish and Portugal's Portuguese. Whether the Android app should follow the
  rule is a choice about the app, not about its screenshots.
- **No `-u-` extension in a `playLocale`.** The library turns the tag into a resource qualifier (`es-rGQ`), which
  has no room for one.

## How the iOS capture works

The UI test launches the app once per screen and language with `-StoreScreenshotsLanguage`, `-StoreScreenshotsTheme`
and `-StoreScreenshotsScreen`, plus `-AppleLanguages` and `-AppleLocale`. The languages, the screens and the photos
are the tables of `scripts/store-screenshots/` (`locales.tsv`, `screens.tsv`, `assets.tsv`), which the Mac's capture
reads too.

**The language picks the strings, the region how amounts, dates and hours are written.** Launched in `es-419`, the
app reads its `es` strings in every region, Spain's included. Latin America's set runs in `es_GQ`, Equatorial Guinea's
region (Pierre's call): the only Spanish region that writes the trip's euros with the symbol in front ("€1145,80")
and also the hours on a 24-hour clock ("21:00") and the day before the month ("15 nov", "11–14 nov"). Every Spanish
region was measured with the app's format styles on iOS 27.2 and macOS 27.2, which write them alike:

- **Latin America's**, all of them, write the euro as its code, "EUR 1,145.80" or, in most of South America,
  "EUR 1.145,80", and nearly all on a 12-hour clock. Under Mexico's the wider amount also cut the group's rows short.
- **The United States'** writes "€1,145.80", with "9:00 p.m.", "nov 15" and "nov 11–14".
- **The Canary Islands'** and **Ceuta and Melilla's** write Spain's "1145,80 €", and so does **the Philippines'**,
  on a 12-hour clock.

The trade-off: Equatorial Guinea's separators are Spain's, a decimal comma and no thousands separator under 10 000,
the boat tour's dollars are "US$120" where the United States' wrote "$120", and a compact amount from 1000 up puts
the symbol after ("1 mil €"). The trip's Statistics stays under it: its most expensive day is €678, so the chart's
value lines stop at "€750". Changing the region is that one line of `locales.tsv` for the App Store's set, and
`LATIN_AMERICAN_PLAY_LOCALE` for Play's, which nothing changes by itself
([the region Play's screens are drawn in](#the-region-plays-screens-are-drawn-in)).

The faces, the groups' photos and the receipt's photo go in the launch environment as base64, since the Mac app's
sandbox can't read the repository. The app writes them to a temporary folder of its own. In a **debug**
build, `BoraRacharApp` then calls `BlitzSplitApple.startForStoreScreenshots` in place of `start`
(`StoreScreenshotsRun` is compiled out of a release build, and a release framework ignores the call and starts as
usual). That entry point:

1. Deletes the Room database, so every launch starts from a fresh install.
2. Starts Koin with `storeScreenshotsModuleOf`. The sync never starts and the device reads as online. Supabase points
   at a port nothing listens on, so no anonymous account is made and no sample bill reaches the real server.
   "Scan receipt" answers with the trattoria's items.
3. Seeds the trip through the app's own use cases (profile, three groups, bills, the payment, pins, language, theme).
   It then does with SQL what a sync would: clears the pending flags, sets everyone as joined with a face, the
   invite code, and Activity's feed.
4. Once the app has put Home at the root, pushes the screen's routes onto the shared back stack. Nothing is
   tapped: for Items, the bridge opens "Bill items" on the dinner's bill once it is loaded
   (`BlitzSplitApple.createSplitBillViewModel`), as its "Split" row does. A tap would depend on where that row is, which
   changes with the screen's height (below the screen on the iPhone Duo closed), and the Mac's capture has
   nothing that taps.

Each simulator's status bar is set to 9:41 with a full battery, and the frame draws no status bar over the capture.
The capture's points must match the frame's screen (`AppleScreenshotSlot`), or the PNG is stretched. Check them
after a library upgrade.

## How the Mac capture works

The Mac has no simulator and no UI test: `capture-mac.sh` launches the debug app itself, with the same arguments and
photos as the iOS test, and photographs its window with `screencapture`. A Mac's UI test runner is sandboxed by
Xcode (it reads the disk and writes only its own container), so a test can't write the captures out, and on this
project's Mac its own screenshot of the window failed as well ("Image creation failed").

- **The window opens by itself.** The script, or a runner with no session in front, may launch the app without it
  coming forward, and a Mac app launched behind the others gets no window until it is reopened or activated. A
  screenshot launch asks itself to reopen, as a click on its Dock icon does
  (`StoreScreenshotsRun.openWindowIfLaunchedBehind`).
- **The window alone, always the same size.** `StoreScreenshotsRun.sizeWindow` gives the window the same size on
  every launch and hides every other app's windows first (as ⌥⌘H does), so the sidebar's glass shows the wallpaper
  and nothing else. It then prints the window's number, which the script reads from the app's output and hands to
  `screencapture -l`. The hidden apps come back with a click on their Dock icon.
- **On a Retina display.** The frame draws the window at twice its points (2880x1800). On a display that isn't
  Retina the capture is 1440x900 and would go to the listing blurred, so the script stops at the first one, which
  is still there to look at.

## The README's image

The README opens with the same sample trip: its group, Expenses, on Android, the iPhone, the Mac and the web, side by
side, dark or light as the reader's GitHub is (`docs/images/readme-hero-{dark,light}.webp`, versioned, unlike the store
images). One command captures the four screens and draws it:

```bash
./scripts/readme-hero/generate.sh                                  # all four, then the image
READMEHERO_CAPTURES=web ./scripts/readme-hero/generate.sh          # capture again only the web's
READMEHERO_CAPTURES=none ./scripts/readme-hero/generate.sh         # the image again, from the captures in build/
```

Run it after a change to the group's screen on any platform, and commit the two images. Each platform comes from the
store screenshots' own tools:

- **Android:** `ReadmeHeroScreens` in `:android:store_screenshots` draws the Compose screen under Robolectric, bare
  (no frame, no banner), on a Pixel 9's screen. Only `-PreadmeHero` runs it, and then nothing else.
- **iPhone and Mac:** the App Store captures of `group` and `light` in `en-US` (`capture-ios.sh` on the iPhone 17 Pro,
  `capture-mac.sh`), written apart from the store's, in `build/outputs/readme-hero-apple/`. The Mac's takes the
  screen for a moment, as the store run does.
- **Web:** the web has no store, so it gets the same seed its own way. `capture-web.mjs` opens the dev server in a
  headless Chrome with an empty profile of its own and sets `window.__storeScreenshots` before the page runs; the
  dev build then starts the shared code with `startBlitzSplitForStoreScreenshots` (`StoreScreenshotsWeb.kt`), which
  seeds the trip as the Apple apps' `startForStoreScreenshots` does, and opens the group. A production build never
  looks at it (`webApp/src/bridge/storeScreenshots.ts`). The photos go in as base64, from `assets.tsv`.

`compose.mjs` then lays the four out in `hero.html` (the phones' bezels, Chrome's bar, the captions) and has the same
headless Chrome photograph it. Nothing to install: it drives the Chrome already on the Mac over the DevTools Protocol
(`chrome.mjs`; `CHROME` points at another binary).

## Things that bite

- **The generators run only when asked for.** They are the unit tests of `:android:store_screenshots`, but a
  `./gradlew build` or `test` skips them. Only a `*StoreScreenshots` task, or
  `:android:store_screenshots:testDebugUnitTest` asked for by name, draws the images.
- **Robolectric has no SDK 37**: the library pins SDK 35 for the generators.
- **The Play tablets** render the screens' own tablet layouts at the frame's size, and the iPad capture is the
  iPad's real layout.
- **Faces and group photos on Play** come through Coil's preview handler (`SamplePhotosComponent`), which answers at
  once. A real load would still be running when Robolectric captures.
- **`ItemsEditorContent` is public** so the generator can draw "Bill items" without the ViewModel.
- **An iOS capture can be a screen still loading.** The UI test photographs each screen four seconds after its
  launch, whatever it shows by then, and on a simulator just booted "Bill items" has come out as its grey
  placeholders (the open iPhone Duo's first language, November 2026). Look at `store_listings/` after a capture, and
  take the screen again with `STORE_SCREENSHOTS_SLOTS`, `STORE_SCREENSHOTS_LOCALES` and `STORE_SCREENSHOTS_SCREENS`.
  A release's captures are not looked at before they go up.
