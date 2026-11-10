# Store listing screenshots

The Google Play and App Store screenshots are **generated, not taken by hand**. Each one is a real screen of the app,
showing a sample trip among five friends, inside a device frame with a title and description, in all 11 languages
of the listings. The frames come from the [store-screenshots](https://github.com/lucianosantosdev/store-screenshots)
library, as in Bible Planner. None of the images is versioned: every run rebuilds them from source.

The two stores draw the screen differently, because the app has a native UI on each:

- **Google Play.** Robolectric renders the real Jetpack Compose screen inside the frame, from `UiState`s built from
  the sample trip. Any machine can do it.
- **App Store.** The UI is SwiftUI, which Robolectric can't draw, so the screens are **captured on the iOS
  simulator** by an XCUITest target. It launches a debug build seeded with the same sample trip and writes each whole
  screen to a PNG. Robolectric then puts that PNG into the iPhone or iPad frame, with the same banner. This half
  **needs a Mac with Xcode**.

## One command

```bash
bundle exec fastlane screenshots                     # or plain `fastlane screenshots`: everything, then upload
fastlane screenshots skip_upload:true                # generate only, nothing reaches a store
fastlane screenshots platform:android skip_upload:true
fastlane screenshots platform:ios skip_capture:true  # frame the iOS captures already in build/ again
fastlane upload_screenshots locale:es-419            # upload what is staged, one locale
```

The lane runs, each step under `scripts/heavy-check.sh`:

1. `scripts/store-screenshots/capture-ios.sh`: builds the `BoraRachar-iOS-StoreScreenshots` UI tests and runs them
   on one simulator per App Store slot. They write `build/outputs/apple-screenshot-captures/<slot>/<locale>/`.
   Then `scripts/store-screenshots/capture-mac.sh` does the same with the Mac app's window
   (`BoraRachar-macOS-StoreScreenshots`). **It drives this Mac's own screen for about half an hour**, so leave
   the keyboard and mouse alone, or pass `skip_mac:true` to leave the Mac listing as it is.
2. `./gradlew stagePlayStoreScreenshots stageAppStoreScreenshots collect…`: the generators (the unit tests of
   `:android:store_screenshots`) draw every image. The tasks then copy the images to `store_listings/` to look at, and
   to the folders fastlane uploads from. Spanish goes out to each store's regional codes.
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
without swiping. Play holds 8 per device type and the App Store 10 per display size, so 09 and 10 are App Store only
(`StoreScreen.isAppStoreOnly`). Every shot is in the dark theme except 10, on the logo's blue dimmed to `#0E2433`.

**The sample trip** (`shared/src/commonMain/.../storescreenshots/SampleTrips.kt`) is one story told in each
language: a trip to Lisbon among five friends, its names and descriptions in that language and its amounts in that
language's currency, at round rates and rounded the way a menu there prices them. The faces are AI-generated portraits of people who don't exist (`scripts/store-screenshots/faces/`), the same
five in every language; the receipt's photo is `scripts/store-screenshots/receipt.jpg`.

| Device | Play | App Store (Apple's screenshot specifications, November 2026) |
| --- | --- | --- |
| Phone | `phoneScreenshots`, 1242x2484 | **Dynamic Island (medium), 1206x2622, the one App Store Connect requires**, from an iPhone 17 Pro. Dynamic Island (large), 1290x2796, from an iPhone 16 Plus. Face ID (large) is only a fallback for the large size, so it isn't made |
| Foldable | | iPhone Duo closed, its outer display at 1398x2034, from the Duo simulator (on the runtime that has it), which boots closed. Optional until April 2027, when apps built with the iOS 27.1 SDK must have it |
| Tablet | `sevenInchScreenshots`, `tenInchScreenshots` | 13" (2048x2732) from an iPad Air 13", and 11" framing the same capture on a taller canvas |
| Mac | | The Mac app's window at 1440x900 points, framed on 2880x1800 (16:10) |

**The Duo's inner display isn't captured.** The simulator boots closed, and neither `simctl` nor XCUITest can unfold
it. deliver files either Duo size under the one Duo display type. If the inner one (2007x2853) is wanted, unfold the
simulator by hand (Device menu), capture with `xcrun simctl io <udid> screenshot --display=3`, and add a slot.

**The iPad runs in full-screen apps.** iPadOS 26 opens apps in windows by default, which would leave the app short
of the screen. On an iPad slot, the UI test first switches Settings › Multitasking & Gestures to full-screen apps.

store-screenshots has no frame for the Duo or the Mac. They get a plain rounded bezel and a window shadow
(`CustomBezels.kt`), with the same banner as the other shots.

## Where to edit

```
shared/src/commonMain/.../storescreenshots/   # the sample trip, both stores
shared/src/appleMain/.../storescreenshots/    # the iOS seed and the screen each capture opens
android/store_screenshots/src/test/.../        # the Play screens' UiStates, banner copy, the frames
iosApp/UITests/StoreScreenshotsUITests.swift   # the iOS capture: languages, screens, the one tap Items needs
build-logic/.../StoreScreenshots.kt            # the collect and stage tasks, the stores' locale codes
fastlane/Fastfile                              # the screenshots and upload lanes
```

- **Banner copy:** `BannerCopy.kt`, per screen and language. The frame wraps the description to two lines, so keep it
  short.
- **A new screen:** an entry in `StoreScreen` with its file name, a `@Test` in `StoreScreenshots` and its
  `…ScreenshotContent` (the Play screen), an entry in `StoreScreenshotsScreen` (its routes on iOS) and in the UI
  test's `screens`. The file names must match on both sides.
- **A new language:** `storeLocales`, `bannerCopies`, `SampleTrips` (copy and currency), the UI test's `locales` and
  the two store maps in `StoreScreenshots.kt`.

## How the iOS capture works

The UI test launches the app once per screen and language with `-StoreScreenshotsLanguage`, `-StoreScreenshotsTheme`
and `-StoreScreenshotsScreen`, plus `-AppleLanguages` and `-AppleLocale`. The faces and the receipt's photo go in
the launch environment as base64, since the Mac app's sandbox can't read the repository. The app writes them to a
temporary folder of its own. In a **debug**
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
4. Once the app has put Home at the root, pushes the screen's routes onto the shared back stack. The test only
   taps once, on "Split" (`splitBill.split`) for Items, so the run never depends on the language's labels.

Each simulator's status bar is set to 9:41 with a full battery, and the frame draws no status bar over the capture.
On the Mac, `StoreScreenshotsRun.sizeWindow` gives the window the same size on every launch and hides every other
app's windows first (as ⌥⌘H does), so the sidebar's glass shows the wallpaper and nothing else. The test captures
the window alone. The hidden apps come back with a click on their Dock icon.
The capture's points must match the frame's screen (`AppleScreenshotSlot`), or the PNG is stretched. Check them
after a library upgrade.

## Things that bite

- **The generators run only when asked for.** They are the unit tests of `:android:store_screenshots`, but a
  `./gradlew build` or `test` skips them. Only a `*StoreScreenshots` task, or
  `:android:store_screenshots:testDebugUnitTest` asked for by name, draws the images.
- **Robolectric has no SDK 37**: the library pins SDK 35 for the generators.
- **The Play tablets** render the screens' own tablet layouts at the frame's size, and the iPad capture is the
  iPad's real layout.
- **Faces on Play** come through Coil's preview handler (`FaceImages`), which answers at once. A real load would
  still be running when Robolectric captures.
- **`ItemsEditorContent` is public** so the generator can draw "Bill items" without the ViewModel.
