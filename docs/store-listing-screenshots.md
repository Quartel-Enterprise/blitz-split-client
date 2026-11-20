# Store listing screenshots

The Google Play and App Store screenshots are **generated, not taken by hand**. Each one is a real screen of the app,
showing a sample trip among five friends, inside a device frame with a title and description, in all 30 languages
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
fastlane upload_screenshots platform:android locales:es-419,es-US   # some of Play's locales, through one Play edit
fastlane upload_screenshots platform:mac             # upload the Mac's alone
```

A stable release dispatched with `screenshots` on runs the same steps in CI, in jobs of `release-production`: the
capture on one runner per slot, the frames, and the upload to each store, one job per locale on the App Store and a
few locales per job on Google Play ([release-process.md](release-process.md#screenshots-with-a-release)). The input is off unless asked for, so a
release after a new language or a changed screen has to pass `screenshots=true`. That captures the iPhone's and the
iPad's slots (`iphone63`, `iphone67`, `ipad13`). The iPhone Duo's two (`iphoneduo`, `iphoneduoopen`) and the Mac's
are captured only with `screenshots_iphone_duo=true` and `screenshots_mac=true`, off unless asked for, since all
three failed on the runners in the first release that ran them, and only in a run that releases their platform
([release-process.md](release-process.md#the-devices-the-app-stores-captures-run-on)). Run the lane by hand between
releases, when a release's screenshot jobs failed, and for the devices a release leaves out.

**For the App Store, the runners' captures can't be trusted today**, so its screenshots are made on a Mac, looked
at, and sent through GitHub: [Sending a set made on a Mac](#sending-a-set-made-on-a-mac). **Google Play's go alone**
through the `play-store-screenshots` workflow, which draws and sends them without a release and leaves the App
Store's as they are: [Sending Google Play's alone](#sending-google-plays-alone).

The lane runs, each step under `scripts/heavy-check.sh`:

1. `scripts/store-screenshots/capture-ios.sh`: builds the `BoraRachar-iOS-StoreScreenshots` UI tests once and runs
   them on one simulator per App Store slot (the iPhone Duo's two slots, closed and open, share theirs), one
   language at a time, saying which one it is on and how long each took
   ([One language at a time](#one-language-at-a-time)). They write
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
- **Play** sends only the images whose SHA-256 changed (`sync_image_upload`), and leaves a locale out altogether
  when Google Play already holds all of its staged images: with no locale left it opens no edit and saves nothing,
  since every save counts against [Google Play's daily save quota](#google-plays-daily-save-quota). The locales
  named with `locales:` go through one edit, as everything staged does when none is named. It tries a Play 5xx
  three times, 30 seconds apart. `VALIDATE_ONLY=true`, or the lane's `validate_only:true`, checks the upload
  against the API without publishing. The App Store has no such check, so that run leaves its screenshots out, and
  stops when Google Play isn't among the platforms asked for.
- **The Mac app is a version of its own** on App Store Connect: its screenshots (`fastlane/screenshots-mac/`) go up
  through a second `deliver` with `platform: osx`, and only when they were staged.
- **App Store** screenshots only change on an editable version. "Screenshot Set Already Exists" is tried three
  times, and a shelf App Store Connect won't reorder doesn't fail an upload that landed. After an upload of every
  locale, `tidy_app_store_screenshots` reads each shelf in full, sorts it by file name, and then **checks that
  each shelf holds every screen once**: a shelf short of a file, or listing one twice, fails the lane, which names
  the locale, the shelf and the file ([Repairing shelves](#repairing-shelves)). It removes no screenshot: one of a
  doubled entry's two ids takes the other with it. How it sorts and what it reports is decided by
  `scripts/store-screenshots/shelves.mjs`, which has the tests, so the lane needs Node.
  `fastlane finish_app_store_screenshots` runs that tidy-up on its own.
- Both lanes refuse to upload from an empty folder, which would wipe the listing.

The App Store Connect key lives in the repository's secrets alone, so `fastlane upload_screenshots` reaches the
App Store only from a workflow: a release's own jobs, for the images that run framed, and `app-store-screenshots`,
for a set made on a Mac.

## Sending a set made on a Mac

**Until a release's captures can be trusted, this is how screenshots reach the App Store.** The first release that
made them on GitHub's runners (3.0.0, run 44222898644, 18 November 2026) had nothing to upload that anyone would
want on the listing: iPhone and iPad screens photographed before their content loaded, grey placeholders and black
screens, the Mac refused for a display that isn't Retina, and both of the iPhone Duo's captures failing. A release
doesn't look at its captures before they go up. So the set is captured and framed on a Mac, where a person looks at
it, and travels to App Store Connect through GitHub, where the key is:

```bash
fastlane screenshots platform:ios skip_upload:true            # 1. capture and frame, then look at store_listings/
node scripts/store-screenshots/local-set.mjs check            # 2. is the set whole?
node scripts/store-screenshots/local-set.mjs send --name 3.0.1   # 3. pack it and send it to GitHub
gh workflow run app-store-screenshots.yml -f set=3.0.1        # 4. upload it to App Store Connect
```

1. **Capture and frame** as [above](#one-command), with `skip_upload:true`: the Mac's capture takes the screen for
   half an hour, and the open iPhone Duo needs someone to press Open in Device Hub. The framed images are in
   `fastlane/screenshots/` (iPhone, iPad and iPhone Duo) and `fastlane/screenshots-mac/`, and in `store_listings/`
   to look at. **Look at them**: a screen still loading is framed like any other
   ([Things that bite](#things-that-bite)).
2. **Check the set.** `local-set.mjs check` reads the two folders of the checkout, or of a folder passed to it,
   where they may also be named `fastlane-screenshots` and `fastlane-screenshots-mac` (a set kept apart, as
   3.0.0's was: `node scripts/store-screenshots/local-set.mjs check ~/StudioProjects/blitz-split/store-screenshots-3.0.0`).
   It refuses, saying each thing that is wrong and in which locales:

   | It refuses | Why |
   | --- | --- |
   | A locale the App Store listing has and the set lacks, or a folder that is no locale of it | deliver uploads the locales it is handed, and App Store Connect attaches screenshots to a listing's locale |
   | A device without one of the ten screens in a locale, or the same screen twice on the iPhone Duo's shelf | A shelf shows the ten screens of `screens.tsv`, once each; the Duo's takes each from one display, open or closed |
   | An image whose size isn't its device's (the table of [What the images show](#what-the-images-show)) | deliver files an image on a shelf by its size, and the iPad's are framed lying down: an upright one is an old one |
   | A file that isn't a PNG, by its name or by its bytes, or that isn't `<screen>_<device>.png` | Nothing else belongs in a staged folder. Hidden files, the Finder's `.DS_Store`, are left out and not refused |
   | **A device the whole set leaves out** | deliver replaces every screenshot of a locale with what it is handed, so the shelf of a device left out is emptied. `--allow-missing-devices` says it is meant, as for a set without the iPhone Duo |

   `--platform ios` or `--platform mac` checks, and sends, one version's folder alone.
3. **Send it.** `local-set.mjs send` checks the set again, packs each version's folder into an archive, and uploads
   them with their checksums as the assets of a **draft GitHub release**, `store-screenshots-<name>`. `--name` is
   the set's name, the date and hour when left out; `--replace` sends over a set of the same name; **`--dry-run`
   checks and packs and sends nothing**. It ends by printing the command of the next step, with the inputs the set
   needs. It needs the GitHub CLI, signed in as someone who can push to the repository.
4. **Dispatch `app-store-screenshots`**, an administrator, on `main`, once no release is running:

   | Input | Default | |
   | --- | --- | --- |
   | `set` | | The name `send` printed. Blank with `tidy_only` |
   | `ios`, `macos` | on | The iPhone's and iPad's version, the Mac's version: each is a version of its own on App Store Connect |
   | `app_version` | blank | The version to write on, X.Y.Z, created when it is missing. Blank writes on the one open for editing, whatever its number: a release renames it later and keeps its screenshots |
   | `allow_missing_devices` | off | The set leaves a device out on purpose |
   | `locales` | blank | Send these locales of the set alone, App Store codes with a space between (`en-US de-DE`): [Repairing shelves](#repairing-shelves) |
   | `tidy_only` | off | Send nothing and fetch no set: only the tidy-up and its check ([Repairing shelves](#repairing-shelves)) |

   The run fetches the set and **checks it again**, which is what decides: a set that doesn't pass is refused before
   anything is sent, and the run's summary says why. It then uploads it with the jobs a release runs for its own
   screenshots ([`app-store-screenshots-upload.yml`](../.github/workflows/app-store-screenshots-upload.yml)), the
   matrix read off the set as a release's is read off what it framed: "iPhone and iPad / send \<locale\>" and
   "Mac / send \<locale\>", **one locale per job, one after the other** on each version and the two versions side
   by side, then "tidy up", which sorts every shelf and checks that each holds every screen once, removing nothing. The first set (3.0.0's, run 44324532909) took 50 minutes for the iPhone's 26 locales
   and 41 for the Mac's. The last job, "Report and remove the set", writes in the run's summary which locales
   landed, what each took and whether the shelves were left as staged, and **deletes the draft release once every
   locale landed and every shelf is as staged**. A set that holds both versions' screenshots and goes up one
   version per run (`-f ios=false`, then `-f macos=false`) loses, each time, the archive that landed, and is
   deleted with the last one.
5. **A locale that failed** fails the run, and the set is kept, which the summary says. Run that locale's job again
   with the job's own re-run button: it finds the set in the run's artifacts (kept seven days), and the tidy-up and
   the report run again after it. **The other locales are not sent again**: GitHub carries their jobs over as they
   ended (tried in November 2026 on a matrix inside a called workflow, which is what these jobs are). deliver
   deletes a locale's screenshots before it uploads the new ones, so a locale whose job failed halfway may be left
   with none until then. A job run again starts without the run's first job, so each upload job looks for a
   release running by itself, and stops while one is.
6. **Then the release.** The workflow writes screenshots on the editable version and submits nothing. A stable
   release dispatched afterwards **with `screenshots` off** renames that version to its own number, keeping the
   screenshots, and submits it ([release-process.md](release-process.md#screenshots-made-on-a-mac)). Not before the
   run is over: a version waiting for review takes no screenshot, so a release that submits refuses to start while
   the upload is going, and the workflow refuses to start while a release is. One set at a time, too: a second
   run dispatched while one is going is refused, not queued, since GitHub would cancel a queued one without a word
   when a third arrived.

### Repairing shelves

Every locale's job can pass and a shelf still be wrong: an upload can leave a shelf **listing one screenshot
twice**, or short of a file ([Things that bite](#things-that-bite)). The "tidy up" job is what looks. After the
last locale it reads every shelf of the version in full, sorts each by file name, reads the shelves again and
**fails when one isn't as staged**, which is each of the ten screens of `screens.tsv` once, in file-name order, on
every shelf the other locales have. **It removes nothing.** Its job's summary names each shelf and what is wrong
with it, as `<locale> / <shelf>: <what>`, the shelf by App Store Connect's name for the device size
(`APP_IPHONE_67`, `APP_DESKTOP`); the run's last job says whether the shelves are as staged, and keeps the set
while they aren't. Two inputs repair what the tidy-up names without sending the whole set again, both by an
administrator, on `main`, while no release and no other run of the workflow is going:

| The summary says | Repair |
| --- | --- |
| `<file> is listed 2 times`, `<file> is missing`, `<file> never finished uploading`, `holds no screenshot`, `is not there`, `has no shelf`, `no locale of the version holds a screenshot` | **Send that locale again**: `gh workflow run app-store-screenshots.yml -f set=<name> -f locales="en-US de-DE"`, with `-f ios=false` or `-f macos=false` for one version. deliver deletes every shelf of a locale it is handed and uploads it afresh, and the tidy-up then checks every locale again |
| `is not in file-name order`, `could not be read whole`, `<file> is still being processed` | **Tidy up again**: `gh workflow run app-store-screenshots.yml -f tidy_only=true`, which fetches no set and sends nothing. The "tidy up" job's own re-run button does the same within a run that is still on the page. Also the way to check the shelves after a repair made by hand |

- **Never delete one of a file's two entries**, by hand or in a script. A file listed twice was, each time it was
  looked at, **one screenshot under two ids**: the same file name and checksum, and `DELETE appScreenshots/<id>` on
  either id removed both, leaving the shelf with 9. The App Store Connect page shows such a shelf as "10 of 10",
  with ten thumbnails, so nothing there says it. To repair one by hand: delete the pair, upload the file again,
  reorder. Sending the locale again does the same for the whole locale.
- **`locales` needs the set**, which a run keeps while a shelf isn't as staged, for this. When it is gone (the
  draft was dropped, or the run is from before the set was kept), send it again from the Mac first
  (`local-set.mjs send --name <name>`, half a minute for 600 MB), then dispatch with `locales`. A locale the set
  lacks stops the run before anything is sent. The run checks the whole set as any other, uploads the named
  locales' jobs alone, and **keeps the set**: it can't tell whether the other locales are up, so its summary ends
  with the command that drops the draft release. The summary of a run that kept its set for a shelf prints both
  commands with that run's own `ios`, `macos` and `app_version`.
- **`tidy_only` takes no other input** than `ios` and `macos`: with `set`, `locales` or `app_version` it is
  refused, since it works on the version that is open for editing and sends nothing.
- **A locale sent again can come back with another file listed twice**: three of the first set's 156 shelves did.
  The tidy-up of that run says so, and the locale goes again.

How it reads a shelf, since this is what went wrong the first time: App Store Connect hands over **at most 10** of
a shelf's screenshots with the shelf (the relationship included in the answer, which is what fastlane's
`app_screenshots` holds), so the lane asks the shelf's own two lists instead, paged, 200 at a time:
`appScreenshotSets/<id>/relationships/appScreenshots` for the order, which is also what a reorder replaces, and
`appScreenshotSets/<id>/appScreenshots` for each screenshot's file name, state and checksum. The two must name the
same screenshots, as many as App Store Connect counts, and start with the 10 that came with the shelf, in their
order, or the shelf is left untouched and reported. The order it then sends names **every id the shelf lists**, a
doubled entry's two included, side by side: App Store Connect refuses an order that leaves one out, and took one
with all 11.

**Why a draft release.** The set has to get from the Mac to a runner, and it is some 600 MB of images that are not
versioned. A branch would leave them in the repository's history for good, and an artifact can only be made by a
run. A release's asset takes a file under 2 GiB, with no limit on the release's size and at no cost, and
`gh` uploads it. A **draft** shows only to people who can push to the repository (the API and the download address
answer 404 to anyone else, checked in November 2026), has **no tag** in the repository until it is published, which
nothing does, and notifies nobody. A workflow's own token reads it only with `contents: write`, which the job
that fetches the set and the one that removes it have, and no other. Only a draft is taken for a set: the workflow
and `--replace` leave a published release alone, whatever its tag. From the fetch on, the set is two artifacts of
the run, under the names a release gives its own, and those, as every artifact of a public repository, can be
downloaded by anyone signed in to GitHub for the seven days they last.

**What the first set showed** (3.0.0's, run 44324532909, 18 November 2026): all 52 locale jobs passed, deliver
sending each locale's 50 iPhone, iPad and iPhone Duo images and the Mac's 10 without a refusal, the iPad's lying
down and the iPhone Duo's two sizes included. Both tidy-ups then failed, twice, on shelves that listed 11
screenshots, and took a file off a third ([Things that bite](#things-that-bite)): three shelves of 156, put right
by hand in App Store Connect. **What no run has shown yet:** the tidy-up as it reads and sorts shelves now, and
the `locales` and `tidy_only` inputs, which came after that run. The lane was run against a stand-in for App Store
Connect that behaves as those shelves did (one screenshot under two ids, both gone when either is deleted, an
order refused unless it names all 11), with a shelf short of a file, a locale with no shelf, one of 215 screenshots
over two pages and one whose lists disagree; it has not talked to App Store Connect itself.

## Sending Google Play's alone

Robolectric draws Play's images, so nothing is captured and any machine makes the same ones: a runner's are as good
as a Mac's, and they need no set sent from one. The [`play-store-screenshots`](../.github/workflows/play-store-screenshots.yml)
workflow draws them and sends them, with no release and without touching the App Store's:

```bash
gh workflow run play-store-screenshots.yml -f validate_only=true   # draw them and ask Google Play: nothing changes
gh workflow run play-store-screenshots.yml                         # draw them and send them
```

**Google Play shows a listing's screenshots to everyone the moment they change**, whatever release is out, so the
second command is the whole publication. Dispatch it, an administrator, on `main`, once no release and no listing
is going:

1. **"Draw the screenshots"** checks who dispatched the run and from where, then runs
   `./gradlew stagePlayStoreScreenshots` on the runner, as a release does (the
   [`play-screenshots-draw`](../.github/actions/play-screenshots-draw/action.yml) action). The images of every
   locale are the run's artifact `play-store-screenshots`, kept seven days, and the run's summary lists the locales.
2. **"Google Play / send 1 of 5: \<locales\>"** uploads them, **a few locales per job, one job after the other**:
   the jobs a release runs for the same images
   ([`play-store-screenshots-upload.yml`](../.github/workflows/play-store-screenshots-upload.yml)), after "group
   the locales" split them, six to a job unless the run's `group_size` says otherwise. A job's locales go through
   one Play edit, so a whole set is six saves of
   [Google Play's daily save quota](#google-plays-daily-save-quota). Only the images whose content changed go up,
   the phone's too when they did, and a locale Google Play already holds as drawn is left out of its job's edit:
   each job's summary says which went and which were already there. One job per locale took about two minutes a
   locale in the release that timed them (3.0.0); the groups are not timed yet.

**To look before anything is public**, dispatch with `validate_only=true` first: the run draws the same images,
hands them over as the artifact and only asks Google Play whether it would take the images of the locales that
differ from what it holds, which changes nothing (a locale it already holds as drawn is left out, as when sending,
and a group with none left asks nothing). Download the artifact (`gh run download <run> -n play-store-screenshots`) and look, or draw them on any
machine with `./gradlew collectPlayStoreScreenshots` and look at `store_listings/android/`. The run that sends
draws them again, from `main` as it is when that run starts: a screen changed by a pull request merged in between
comes out changed. A run with `validate_only` holds releases and listings off as any run of the workflow does, so
wait for it to end before dispatching either.

**A group that failed** fails the run, and the others are up. Run that group's job again with its own re-run
button: it finds the images in the run's artifacts and draws nothing again, and sends only the locales of the group
Google Play doesn't hold yet. **A group Google Play refused for its daily save quota** did not fail: the run ends
well with a warning, and [what didn't go is sent later](#google-plays-daily-save-quota).

**Not while something else writes on Google Play.** A release's bundle, a listing's text and each group's
screenshots go through a Play edit of their own, and the newer edit drops the one still open. So the run refuses
to start while a stable release, a run of `store-listing` or another run of itself is going, each of its upload
jobs checks again (a job run again later starts without the first one), `store-listing` refuses to start while it
is going, and a stable release refuses too, whatever its platforms: its `plan` fails, naming the run, and
`release-production.sh` says so before dispatching ([`no-run-going`](../.github/actions/no-run-going/action.yml)).
The upload jobs can't tell a release without Android apart, so one let through would fail every group still to
go. A beta reaches neither Google Play's listing nor its tracks, so it goes on. As with the listing text, an edit sent
through the API takes every change waiting in the Play Console's Publishing overview to review with it
([store-listing.md](store-listing.md)).

**What the runs have shown** (November 2026): the workflow's first run, 44335758033 on 2026-11-18, still one
locale per job, sent 18 locales before Google Play refused the 19th for
[its daily save quota](#google-plays-daily-save-quota). The jobs as they are now, a few locales each, have not run
against Google Play yet ([what no run has shown](#google-plays-daily-save-quota)).

## Google Play's daily save quota

**What it is.** Everything sent to Google Play through its API goes through an edit, and the commit of an edit is a
*save*. Google Play counts an app's saves, and once it has had too many in a day it refuses the next one with
"Google Api Error: Invalid request - Daily save quota exceeded." (`publishingDailySaveQuotaExceeded`). The count is
the app's, whatever does the saving: a release's bundle, a run of `store-listing`, each edit of screenshots. Google
documents neither how many saves a day allows nor when the count starts over, and fastlane's maintainers call it
outside their control ([fastlane#8390](https://github.com/fastlane/fastlane/issues/8390)).

**What was seen here.** On 2026-11-18, run 44335758033 of `play-store-screenshots` was refused at its 19th locale,
after roughly 50 saves in about 15 hours: three runs of `store-listing`, a release to a testing track, the 3.0.0
release with its 27 locales of screenshots, then 18 locales of that run. Each locale was a job and a save of its own then, and
the 9 jobs after the refusal ran only to be refused the same way. Other projects report the same error after about
25 runs in 16 hours and after about 120 writes in a day, and assume the count starts over at midnight Pacific time.
**Nobody here has measured when it does**: once refused, try again the next day.

**What the upload does about it** ([`play-store-screenshots-upload.yml`](../.github/workflows/play-store-screenshots-upload.yml)
and `upload_play_screenshots` of the [Fastfile](../fastlane/Fastfile), with the decisions in
[`play-screenshot-groups.mjs`](../scripts/release/play-screenshot-groups.mjs), which has tests):

- **A few locales per save.** The locales go up in groups, `group_size` to a job (6 unless the run says), each
  group through one edit: six saves for Play's 32 locales, where one job per locale was 32.
- **No save for what didn't change.** Before it opens an edit to write, the lane reads what Google Play holds
  (through an edit it deletes, which is no save) and leaves out every locale whose staged images are the ones
  already there, shelf by shelf and in order. A group with no locale left opens no edit. supply's
  `sync_image_upload` alone uploads nothing for such a locale and still commits its edit. So a set sent a second
  time costs nothing for what already landed, and a set where one screen of one language changed costs one save.
- **A refusal for the quota is not a failure.** The job Google Play refused ends well, with a warning on the run's
  page, and its summary names the locales that didn't go and the groups left untried. Every group after it stops
  before it reaches Google Play, each with a warning of its own, since it would only be refused too. Any other
  error from Google Play fails the job as it always did. The lane ends well only for a caller that passes
  `save_quota_is_no_failure:true` and reads its outputs, as these jobs do: anywhere else (`fastlane
  upload_screenshots` on a Mac, another workflow) it stops with an error that says the quota ran out.
- **In a release, the bundle goes first**: `android-screenshots` only starts once `android-upload` sent the
  bundle, so the bundle is never the save its own run's screenshots left without quota.

**Sending what didn't go**, once Google Play takes saves again. Either way costs a save only for the groups that
still have something to send:

- **Run each job again with its own re-run button**, the one Google Play refused first, then each group that
  wasn't tried, one at a time (GitHub runs one job again per attempt; from a terminal,
  `gh run rerun <run> --job <job id>`, the ids from `gh run view <run> --json jobs`). A job sends its group alone
  with the images the run drew, while GitHub keeps the run's artifact (seven days), and draws nothing again. **Re-run failed jobs** does
  not pick these jobs up, since none of them failed. After a release, it is the way that sends the release's own
  images.
- **Or dispatch `play-store-screenshots` again.** It draws the images again from `main` as it is then, and every
  locale that landed costs no save.

**On a day with many sends**, the order matters, since the screenshots are what can wait:

1. **The release first, the screenshots after it.** A bundle Google Play refuses to save is a release that didn't
   go out; a set of screenshots it refuses goes the next day.
2. **Not everything on one day.** A release to a testing track to try the run, a listing, the release and a set of
   screenshots are each at least one save, and a set whose every locale changed is five. Spread them when the day
   already had many. A run with `validate_only` commits nothing, so as far as the error's name says it is no save.
3. **A second set on the same day is cheap only when little changed**: what Google Play already holds costs
   nothing, and a frame or a screen that changed in every language is six saves again.

**What no run has shown yet** (November 2026): the groups, the check of what Google Play holds and the quota's path
have not run against Google Play. The lane's tests run it against a stand-in for supply's client (a group held
whole, one locale changed, the save refused for the quota and for another reason:
[`play-screenshots-lane.test.mjs`](../scripts/release/play-screenshots-lane.test.mjs), which needs fastlane and so
runs on a Mac, not on the runner), and the script's tests cover the rest.
Whether an edit that is opened to read and then deleted counts as a save is assumed, from the error's name, not
measured. Should Google Play refuse to open that edit for the quota, the group waits the same way, with a warning.

## The App Store's creative assets

Besides the screenshots, an App Store product page takes two images of its own, under **Product Page Information ›
Header and Search Results** in App Store Connect. They appear on iOS and iPadOS 27 and later, and are generated like
the screenshots, in all 30 languages:

```bash
./scripts/store-screenshots/creative-assets/generate.sh                               # capture, then draw
CREATIVE_ASSETS_CAPTURE=none ./scripts/store-screenshots/creative-assets/generate.sh  # draw from the captures there
CREATIVE_ASSETS_LOCALES=en-US,ja ./scripts/store-screenshots/creative-assets/generate.sh   # a few languages
```

| Image | Size | Art safe area | Shows |
| --- | --- | --- | --- |
| `header.png` | 3840x1646 | 1646x661 at (1097, 493) | One phrase and a bill split into five tickets, with phones around them |
| `search-results.png` | 3840x2560 | 2168x1030 at (836, 765) | One phrase, three tickets and the group and Bill items screens |

They land in `store_listings/creative-assets/<locale>/`. **Run the script again when one of the four screens they
show changes** (the group, Bill items, Statistics and Home, the `iphone67` captures) **or the sample trip does.**

- **The words** are in [`copy.tsv`](../scripts/store-screenshots/creative-assets/copy.tsv), one row per language of
  `locales.tsv`. A phrase too long for its place is drawn smaller, and the run stops if anything leaves the art safe
  area.
- **The tickets** come from the sample trip: `CreativeAssetFacts` in `:android:store_screenshots` writes each
  person's name and equal part of the dinner in the app's own format, so they can't drift from the screens.
- **The layout** is `header.html` and `search-results.html` beside the script. The sizes and safe areas are Apple's
  [creative assets specifications](https://developer.apple.com/help/app-store-connect/reference/app-information/creative-assets-specifications),
  and the images carry no alpha channel, which App Store Connect refuses.

**Nothing uploads them yet**: `fastlane` and the workflows above send screenshots alone. Each language's two images go
up by hand in App Store Connect, on the version open for editing, with that language picked at the top of Product Page
Information. Latin America's Spanish (`es`) goes on Spanish (Mexico). Check the page's **Preview** before submitting.

## The listing art (new design, not uploaded yet)

A second set of store images, drawn in the ticket design approved in November 2026, sits beside the framed one above
and replaces nothing until someone uploads it: ten images a device on the App Store, eight on Google Play, the words
in [`listing-art/copy.tsv`](../scripts/store-screenshots/listing-art/copy.tsv), and the Brazilian listing with Pix
second ("Pagar Carla", from a made-up key the sample trip gives the person you owe in reais).

```bash
./scripts/store-screenshots/listing-art/capture.sh            # iPhone 6.7" and 6.3", iPad 13", closed iPhone Duo
python3 scripts/store-screenshots/listing-art/capture-duo-open.py <udid>   # the Duo open, once something opened it
./gradlew :android:store_screenshots:testDebugUnitTest -PplayArt --rerun   # Google Play's bare Compose screens
./gradlew :android:store_screenshots:testDebugUnitTest -PcreativeAssets --rerun   # the tickets' names and amounts
node scripts/store-screenshots/listing-art/compose.mjs        # every device and language, into store_listings/listing-art/
```

`ART_DEVICES` and `ART_LOCALES` narrow the last step. A callout (the part of a screen drawn larger) is placed by its
coordinates in `compose.mjs`, and for the cards whose height moves with a language's words, found in the capture by
color. Right-to-left languages get the callout from the mirrored side. Google Play's 7" tablet has no callouts: its
screens lay out differently enough that fixed coordinates miss. The Mac's art (`ART_DEVICES=mac`) draws its window as captured, with no device around it, in the phones' ten images (`STORE_SCREENSHOTS_TABLE=art-screens ./scripts/store-screenshots/capture-mac.sh` captures "Dividir" and Brazil's "Pagar Carla"), with no callouts in Arabic and Hebrew, since the window mirrors only part of its layout. `listing-art/stage-app-store.py` stages the App Store's art where `local-set.mjs` checks and sends it, each image under the screens.tsv name of its place on the shelf.

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

**The sample trip** (`shared/src/commonMain/.../storescreenshots/SampleTrips.kt`, its words in `SampleTripCopies.kt`) is one story told in each
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
| Tablet | `sevenInchScreenshots` and `tenInchScreenshots`, both lying down at 2560x1440: a 960x600 dp tablet on the 7" shelf, a 1280x800 dp one on the 10" ([why lying down](#the-play-tablets-are-drawn-lying-down)) | 13", lying down at 2732x2048, from an iPad Air 13", and 11" framing the same capture on that slot's own canvas, 2388x1668 ([why lying down](#the-ipad-is-captured-lying-down)) |
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
  Accessibility and to drive System Events (the image's `configure-tccdb-macos-27.sh`). **It didn't work there
  either**: in the first release that ran the job (3.0.0, run 44222898644, 18 November 2026) the Duo's simulator
  booted and the script answered "Couldn't press Open in Device Hub (is this terminal allowed in Accessibility?)",
  then gave up with nothing captured. The log doesn't say which step of the press failed: Device Hub's window, the
  simulator's row or the Open button.
- **The closed Duo failed in that release too**, for a reason nobody knows yet: its UI test stopped about nine
  minutes in, after nineteen launches of the app, and `xcodebuild` runs quiet and keeps no result bundle, so the log
  names neither the screen nor the failure.

So a release captures neither Duo slot unless it passes `screenshots_iphone_duo=true`
([release-process.md](release-process.md#the-devices-the-app-stores-captures-run-on)): nothing but a release runs
those jobs, so the marker is also how a fix is tried.

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
group. The list is `duoScreensStagedOpen` in `build-logic/.../StoreScreenshots.kt`, one line to change, then the table
above. Which screens go up open is Pierre's call (November 2026: he kept these five). A display that wasn't
captured gives its screens to the other one, so a release whose open capture failed uploads the ten closed, as
before. `collectAppStoreScreenshots` still frames both whole, into `store_listings/ios/<locale>/iphone_duo/` and
`iphone_duo_open/`.

Not checked yet, since no Duo screenshot has gone to App Store Connect: that it files both sizes on that one shelf,
upright and lying down side by side. Apple's page lists the four sizes under the one device and fastlane's change
says its upload was tried, but the API's own `ScreenshotDisplayType` didn't list `APP_IPHONE_DUO` in version 4.6 of
its specification (its newer asset library does, as the display class `IPHONE_DUO`). Should App Store Connect refuse
a size, deliver fails after the other devices' screenshots are up; the shelves that landed are still sorted (by the
lane when it uploads every locale, by the release's tidy-up job after its one upload job per locale), the upload is
listed as failed and the release goes on
([release-process.md](release-process.md#screenshots-with-a-release)).

**The iPad runs in full-screen apps.** iPadOS 26 opens apps in windows by default, which would leave the app short
of the screen. On an iPad slot, the UI test first switches Settings › Multitasking & Gestures to full-screen apps.

store-screenshots has no frame for the Duo or the Mac, and its iPad and tablet frames only stand upright. The Duo
and the Mac get a plain rounded bezel and a window shadow, the iPad lying down a bezel drawn from the library's own
measures of an iPad Air 13", and the Play tablets the library's own tablet mockup, which does turn
(`CustomBezels.kt`), with the same banner as the other shots.

### The Play tablets are drawn lying down

A tablet lying down is where the app has the most to show: every screen in two columns, beside the navigation rail
where the screen has one ([responsive-layouts.md](architecture/responsive-layouts.md)). Upright, a tablet draws a
screen as one centered column. So both shelves are drawn lying down, as the iPad's is (Pierre's call, November
2026). The phone's shelf stays upright.

- **The frame.** `PlayTabletLandscapeSurface` draws the library's `DeviceMockup` with
  `MockupOrientation.Landscape`: the same neutral Android tablet its upright frame drew, turned, with the status bar
  along the top. The mockup measures the screen at the tablet's own size lying down and tells it so (the window's
  size and the configuration's orientation), which is what `rememberFoldLayout()` reads: 960x600 dp on the 7"
  shelf and 1280x800 dp on the 10" are both a tablet lying down (`FormFactor.TabletLandscape`, 600 dp or more on
  the short side and 960 or more on the long one). Nothing in a `…ScreenshotContent` changed: each screen picked its
  layout by itself.
- **The sizes** (Google's [Add preview assets to showcase your app](https://support.google.com/googleplay/android-developer/answer/9866151),
  read on 18 November 2026). Every screenshot: JPEG or 24-bit PNG without alpha, each side from 320 to 3840 px, the
  long side at most twice the short one. For tablets and Chromebooks it asks for 1080 to 7680 px and **16:9 lying
  down** (9:16 upright), and its recommendation formats take only screenshots of 16:9 at 1920x1080 or more. So both
  canvases are **2560x1440**: 16:9, within every limit, and the library flattens the PNG to 24 bits. The page gives
  the 7" and the 10" shelves no sizes of their own, and the upright images before these were 10:16 (1200x1920 and
  1600x2560), which Google Play took.
- **One canvas, two tablets.** At the library's 2 px per dp the canvas is 1280x720 dp. The banner stays on top, at
  40 sp and 20 sp so its description fits in one line in every language, and the tablet takes the height left:
  about seven tenths of the image's, and a little under two thirds of its width. The 7" tablet is drawn larger than life to fill
  it and the 10" one smaller, so the 7" shelf shows each screen bigger and the 10" one shows more of it.
- **Changing the size** is the canvas of `PlayTabletStoreScreenshots` in `StoreScreenshots.kt`, in pixels a whole
  number of dp at that density. A canvas taller for its width (16:10) gives the tablet more of the image, and leaves the 16:9 Google
  asks for.

### The iPad is captured lying down

An iPad opens with its tabs in the sidebar ([home-and-shell.md](product/home-and-shell.md)). Upright, iPadOS has no
room to dock that sidebar, so it opens over the content: the release of November 2026 captured every iPad screen with
the sidebar covering its left third and the rest dimmed. Lying down the sidebar sits beside the content and nothing
is covered, so the iPad's slot is captured that way (Pierre's call, November 2026), and the listing shows the iPad
at its widest: the sidebar beside a screen in two columns.

- **The capture.** The UI test turns the simulator before it launches anything (`Hold` in
  `StoreScreenshotsUITests.swift`), so the app opens lying down and never has to turn. It then waits for the app's
  window to be wider than tall, and fails the language, which is tried again, on a window or a capture still
  upright. The PNG is 2732x2048. Every other slot stays upright.
- **Which side.** `landscapeLeft`: the top of the device to the left, which puts the long edge with an iPad Air's
  front camera on top, as on a keyboard. The screen reads the same on either side; the side only decides where the
  frame draws the camera and the buttons (the camera above the screen, the volume buttons on the top edge by the left
  corner, the top button on the left edge).
- **The sizes.** App Store Connect takes a 13" screenshot lying down at 2752x2064 or 2732x2048, and an 11" one at
  2420x1668, 2388x1668, 2360x1640, 2316x1512 or 2266x1488
  ([Screenshot specifications](https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications/),
  read in November 2026). The 13" frame is 2732x2048, the capture's own size, and the 11" one 2388x1668, the size its
  upright frame had, turned. deliver files them by those sizes, as it did the upright ones.
- **The frame.** The banner stays on top, as on the Mac's wide canvas, and the iPad takes the height left, about four
  fifths of the image's. A banner beside the device would make the iPad narrower and wrap its text. The title and
  the description are larger than under the library's other custom frames (`BannerFontSizes`), and the description
  fits in one line.
- **The 11" slot frames the 13" capture**, as it did upright: the same iPad Air 13" on the 11" slot's canvas, which
  is wider for its height (1.43 against 1.33), so the device is a little smaller in it. No 11" simulator is
  captured.
- **An upright capture doesn't fit the frame.** One left in `build/outputs/apple-screenshot-captures/ipad13/` from
  before this (a release's `apple-captures-ipad13` artifact, a folder kept from an earlier run) would be drawn
  stretched into the lying-down bezel, so the frame stops at it, by name (`AppleScreenshotSlot.isWide`): capture the
  slot again, which a whole run of the script starts by deleting.

## Where to edit

```
shared/src/commonMain/.../storescreenshots/   # the sample trip, its seed and the screen each capture opens
shared/src/appleMain/.../storescreenshots/    # the Apple captures' Room file and photos folder
android/store_screenshots/src/test/.../        # the Play screens' UiStates, banner copy, the frames
iosApp/UITests/StoreScreenshotsUITests.swift   # the iOS capture
scripts/store-screenshots/capture-ios.sh       # its simulators, the run per language and its progress
shared/src/jsMain/.../StoreScreenshotsWeb.kt   # the web's seed, for the README's image
scripts/readme-hero/                           # the README's image
scripts/store-screenshots/capture-mac.sh       # the Mac capture
scripts/store-screenshots/*.tsv                # the languages, screens and photos both captures read
scripts/store-screenshots/tables.sh            # how both capture scripts read and narrow those tables
build-logic/.../StoreScreenshots.kt            # the collect and stage tasks, the stores' locale codes
fastlane/Fastfile                              # the screenshots and upload lanes
scripts/store-screenshots/local-set.mjs        # the check of a set made on a Mac, its devices' sizes, and its way to GitHub
.github/workflows/app-store-screenshots*.yml   # the upload of such a set, and the jobs a release shares with it
.github/workflows/play-store-screenshots*.yml  # Google Play's alone, and the jobs a release shares with it
.github/actions/play-screenshots-draw/         # the drawing of Google Play's on a runner, a release's and its own
```

- **Banner copy:** `BannerCopies.kt`, per screen and language. The frame wraps the description to two lines, so keep it
  short.
- **A new device size:** its frame, its entry in `appleDevices` (`StoreScreenshots.kt`), and its size and shelf in
  `PLATFORMS` of `local-set.mjs`, which a test holds to that list: a set with a device the check doesn't know is
  refused.
- **A new screen:** an entry in `StoreScreen` with its file name, a `@Test` in `StoreScreenshots` and its
  `…ScreenshotContent` (the Play screen), an entry in `StoreScreenshotsScreen` (its routes on Apple's) and a row
  of `scripts/store-screenshots/screens.tsv`. The file names must match on both sides. It goes on the iPhone Duo's
  shelf closed unless `duoScreensStagedOpen` (`StoreScreenshots.kt`) names it; a test of `store-listing.test.mjs`
  holds that list, `StoreScreen` and `screens.tsv` to the same file names.
- **A new language:** `storeLocales` (its `drawnLocale` is its tag, unless the tag carries a script or needs a region
  to write amounts and dates in: [the rules](#the-locale-every-image-is-drawn-in)), `bannerCopies`, `SampleTripCopies` (its words), `SampleTrips` (its currency), a row of
  `scripts/store-screenshots/locales.tsv`, and `listingLocales` and the two store maps in `StoreScreenshots.kt`. Its `language` is the app language's code,
  which keys the banner copy and the trip. Portugal's Portuguese goes by its whole tag, `pt-PT`, since `pt` is
  Brazil's: its own banners and trip (in euros, "tu", Blitz Split), over the app's `pt-PT` strings. Spain's Spanish
  goes by `es-ES` the same way, since `es` is Latin America's: its own banners and trip ("ha pagado", "coche",
  "ticket"), over the app's `es-ES` strings.

## The locale every image is drawn in

Robolectric draws every image in its set's `StoreLocale.drawnLocale` (`StoreLocale.kt`), the Play screens and the
banner over an Apple capture alike: the language the resources are read in and the region the amounts, dates and
hours are written in. It is the set's own tag for most sets. Chinese (`zh-CN`), Traditional Chinese (`zh-TW`, since
its tag, `zh-Hant`, carries a script), Hebrew (`iw-IL`), Thai (`th-TH`), Vietnamese (`vi-VN`), Swedish (`sv-SE`),
Czech (`cs-CZ`), Bengali (`bn-BD`), Malay (`ms-MY`) and Hungarian (`hu-HU`) are drawn in the locale `StoreLocale.kt` gives each, and so is Latin America's Spanish, whose
tag, `es`, has no region: a bare `es` writes the symbol after the amount, as Spain does
("84,08 €", "120 US$"), while the same listing's App Store set, captured in Equatorial Guinea's region, writes
"€84,08" and "US$120". So that set is drawn in **`es-GQ`** (`LATIN_AMERICAN_DRAWN_LOCALE`), **the region of the `es`
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
- **A `drawnLocale` is a language with at most a region** (`zh-TW`, never `zh-Hant`). Robolectric draws every image
  in it, the banner over an Apple capture too, and gets it as a resource qualifier: a script or a third part makes
  one it refuses, and every frame of that device fails (the first release's frames did, on `zh-Hant`, while the
  Apple slots were still drawn in the listing's tag). A test of `store-listing.test.mjs` holds both.
- **No `-u-` extension in a `drawnLocale`.** The library turns the tag into a resource qualifier (`es-rGQ`), which
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
`LATIN_AMERICAN_DRAWN_LOCALE` for Play's and the frames' banners, which nothing changes by itself
([the locale every image is drawn in](#the-locale-every-image-is-drawn-in)).

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

### One language at a time

`capture-ios.sh` builds the UI tests once (`xcodebuild build-for-testing`) and then, on each slot's simulator, runs
them once per language (`xcodebuild test-without-building`, narrowed to that language by the filter a quick look
uses), so a run of an hour says where it is:

```
Capturing iphone63 on 287748C8-4A8F-4886-9749-1AF3B8784AF9: 3 language(s), 10 screen(s) each
Capturing iphone63: en-US (1 of 3)
Captured iphone63: en-US in 2m 16s (1 of 3 done, about 4 min left)
Capturing iphone63: pt-BR (2 of 3)
Captured iphone63: pt-BR in 1m 55s (2 of 3 done, about 2 min left)
```

xcodebuild's own output for a language, with each launch of the app and the test's words for a failure, goes to
`build/apple-screenshots/logs/<slot>/<locale>.log` (`<locale>.2.log` for a second try). On GitHub Actions it is in
the job's log too, folded under the language's "Capturing" line, and the job's summary on the run's page gets a
table of the languages, each with its result and its time, a row as each one ends (GitHub shows a summary once the
job is over).

**A language that fails is tried once more**, since a launch that hangs or a test runner that dies is rarely the
language's fault. When it fails again:

- What it was to capture is deleted: its folder, so the frame sees a slot short of a language. In a run narrowed
  to some screens, only those screens are, since the others are an earlier run's: the frame then stops at the
  missing file, by name, rather than draw the earlier run's capture of it.
- The script goes on with the other languages and the other slots, so one bad language doesn't hide whether the
  rest work. A slot's third failed language ends the slot instead: by then it is the slot that fails, and a failure
  that hangs for minutes, twice per language, would use up a release job's time.
- The script fails at the end, naming every slot and language that failed (an error on the run's page on GitHub
  Actions). The log has the screen each try stopped at and the test's message, and the summary's table the same
  screen:

  ```
  Failed iphone63: ja after 0m 38s, at 03_receipt (try 2 of 2)
  The App Store captures failed for iphone63 (ja): the frame takes a slot only with every language
  ```

What the frame does with such a slot: `stageAppStoreScreenshots` frames a slot only with every language. Without
one of the iPhone's or the iPad's it stops, naming the slot and the language ("No iOS captures of iphone67 (ar)"),
so nothing is staged and no listing gets a shelf short of a screen. A Duo's or the Mac's slot that misses a language
isn't framed, with a warning: the Mac's listing then gets no screenshots, and the Duo's shelf takes that display's
screens from the other one. The languages that did work are still in `build/outputs/`, so on a Mac only the failed
one is taken again, then everything is framed:

```bash
STORE_SCREENSHOTS_SLOTS=iphone67 STORE_SCREENSHOTS_LOCALES=ar ./scripts/store-screenshots/capture-ios.sh
fastlane screenshots platform:ios skip_capture:true skip_upload:true
```

After a release whose capture job failed on a language, that job still uploads the languages it captured, as the
artifact `apple-captures-<slot>`: download each slot's into `build/outputs/apple-screenshot-captures/` first.

**What it costs.** Launching xcodebuild once per language takes three seconds more per language than one run for
them all, measured on an iPhone 17 Pro's simulator in November 2026 with three languages of ten screens: 327 s in
one run, 333 s in three. The runs read the built tests' `.xctestrun` file; through the project and its scheme, each
one loads the project and resolves the Swift packages again, and the same three took 362 s. What the test does once
per simulator, the iPad's switch to full-screen apps (eleven seconds in Settings), only the runs before a slot's
first captured language do (`STORE_SCREENSHOTS_DEVICE_IS_SET_UP`), and the launch that gives the app its own
language back is the script's, once per slot. A run that hangs fails after ten minutes
(`-default-test-execution-time-allowance`) and counts as a failed try. A language takes a little under two minutes on this project's Mac and about
three on a release's runner, where a slot's 26 took 88 and 95 minutes in the release of November 2026, build
included: the run per language adds about two minutes to that, far from the job's limit of 150.

**Two runs can't share a simulator.** A slot's simulator is "Store screenshots <slot>", made once. A run beside
another run of the script (a task of its own, in a worktree) passes `STORE_SCREENSHOTS_SIMULATOR=<name>` to use, or
make, a simulator of that name instead, for the slots of one device: the script refuses the name with slots of
several.

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

**GitHub's runners have no Retina display.** In the first release that ran the Mac's capture there (3.0.0, run
44222898644, 18 November 2026), the app was built signed to run locally, launched and had its window photographed,
and the script stopped at that first capture: "The Mac capture is 1440 px wide and the frame takes 2880: its display
isn't Retina". So a release captures the Mac only when it passes `screenshots_mac=true`
([release-process.md](release-process.md#the-devices-the-app-stores-captures-run-on)), and until that capture works
on a runner the Mac's listing gets its screenshots from a Mac with a Retina display: `fastlane screenshots
platform:ios skip_upload:true`, then [the set is sent through GitHub](#sending-a-set-made-on-a-mac), with
`--platform mac` and `-f ios=false` to send the Mac's alone and leave the iPhone's and the iPad's listing as it is.

## The README's image

The README opens with the same sample trip: its group, Expenses, on Android, the iPhone, the Mac and the web, side by
side, dark or light as the reader's GitHub is (`docs/images/readme-hero-{dark,light}.webp`, versioned, unlike the store
images). One command captures the four screens and draws it:

```bash
./scripts/readme-hero/generate.sh                                  # all four, then the image
READMEHERO_CAPTURES=web ./scripts/readme-hero/generate.sh          # capture again only the web's
READMEHERO_CAPTURES=none ./scripts/readme-hero/generate.sh         # the image again, from the captures in build/
```

Run it after a change to the group's screen on any platform, and commit the two images: the
`update-readme-screenshots` skill decides when a change shows, runs it and checks the result, and `create-pr` runs
that skill on every branch that changes what the image shows. The pieces of a `whatsapp-feedback` run are the
exception: they leave it to one refresh, in one PR, once they have all merged. Each platform comes from the store
screenshots' own tools:

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

- **Google Play counts every save against a daily quota it documents nowhere**, and refuses the next one with
  "Daily save quota exceeded" once it runs out, the release's bundle included. Roughly 50 saves in about 15 hours
  ran it out on 2026-11-18, and nobody has measured when the count starts over. Send the release first and the
  screenshots after it, and not a release to a testing track, a listing, the release and a set of screenshots on
  one day
  ([Google Play's daily save quota](#google-plays-daily-save-quota)).
- **The generators run only when asked for.** They are the unit tests of `:android:store_screenshots`, but a
  `./gradlew build` or `test` skips them. Only a `*StoreScreenshots` task, or
  `:android:store_screenshots:testDebugUnitTest` asked for by name, draws the images.
- **Robolectric has no SDK 37**: the library pins SDK 35 for the generators.
- **The Play tablets** render the screens' own layouts for a tablet lying down, at the tablet's size and not the
  canvas's, and the iPad capture is the iPad's real layout. Home on the 7" tablet (960 dp wide) is as tight as the
  app is there: its side column cuts a bill's description short after a few letters and its second pinned group
  runs off the edge, where it scrolls.
- **Robolectric lays every locale out left to right**, Arabic and Hebrew too: the library hands it the language,
  never a direction. A Play screen is therefore drawn in its set's own direction (`StoreLocale.layoutDirection`,
  provided in `StoreScreenshots.shot`), or the Arabic listing shows Arabic words in a left-to-right layout, the back
  arrow on the left, as every Play image was drawn until November 2026. Only the screen is turned: the device around it and the frame's status bar, its clock on the left,
  stay as the library draws them, since a device isn't mirrored and the library's phone frame can't be told. An
  Apple capture was taken in its language's direction on the simulator.
- **Faces and group photos on Play** come through Coil's preview handler (`SamplePhotosComponent`), which answers at
  once. A real load would still be running when Robolectric captures.
- **`ItemsEditorContent` is public** so the generator can draw "Bill items" without the ViewModel.
- **An iOS capture can be a screen still loading.** The UI test photographs each screen four seconds after its
  launch, whatever it shows by then, and on a simulator just booted "Bill items" has come out as its grey
  placeholders (the open iPhone Duo's first language, November 2026). Look at `store_listings/` after a capture, and
  take the screen again with `STORE_SCREENSHOTS_SLOTS`, `STORE_SCREENSHOTS_LOCALES` and `STORE_SCREENSHOTS_SCREENS`.
  A release's captures are not looked at before they go up.
- **App Store Connect hands over 10 of a shelf's screenshots, however many it lists.** A shelf read with its
  screenshots included (fastlane's `set.app_screenshots`, which deliver's own sort and the tidy-up both used) is
  the first 10. In the first set's run (44324532909, November 2026) the Mac's `de-DE` shelf and the iPad 11"'s
  `en-US` one listed 11: the tidy-up saw 10 distinct files and sent an order of 10 that App Store Connect refused
  every time, as it refuses any order that leaves an entry of the shelf out ("Can't Add/Remove Relationship when
  reorder Set"). Running it again changed nothing. The same order with all 11 ids was taken. The tidy-up now reads
  a shelf through its own lists, in full, and orders every id it lists
  ([Repairing shelves](#repairing-shelves)); deliver's sort still reads 10, which is why the upload carries on past
  that refusal and leaves the sorting to the tidy-up.
- **A shelf can list one screenshot twice, and deleting either entry deletes both.** Seen in App Store Connect
  while those shelves were repaired by hand: the 11 entries of such a shelf had two with the same file name and
  the same `sourceFileChecksum` under two ids, one a plain UUID and the other in the newer form every other
  screenshot had. `DELETE appScreenshots/<id>` on one of the two answered 204 and both were gone, the shelf going
  from 11 to 9. That is how the iPhone 6.7"'s `en-US` shelf lost `08_payers_and_currencies_iphone_6_7.png` in that
  run: the tidy-up removed "the later copy" of a file name, and the file went with it. The App Store Connect page
  shows such a shelf as "10 of 10". **So the tidy-up deletes nothing**: it sorts, names the file, and fails, and
  the locale is sent again. Whether a doubled entry would be harmless at submission, or show twice on the store
  page, nobody knows. Where the second id comes from is not known either: deliver logs each file as uploaded once,
  and in that run each shelf that ended wrong had an upload that took 9 to 23 seconds where the others took 2,
  which fits a request Spaceship sent again after a gateway error, and proves nothing.
