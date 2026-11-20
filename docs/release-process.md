# Release process

How a release of Bora Rachar goes out. A **beta** reaches a closed group of testers without the stores or the app's
domain: one command tags it, publishes each platform to its own channel and records it as a GitHub pre-release
([`release-beta.yml`](../.github/workflows/release-beta.yml)). A **stable version** goes to Google Play, the App
Store and the web, as soon as an admin dispatches it ([`release-production.yml`](../.github/workflows/release-production.yml),
[Releasing a stable version](#releasing-a-stable-version)).

## Channels

A stable version's:

| Platform | Channel |
| --- | --- |
| Android | Google Play, the track of the run ([What the stores and the web get](#what-the-stores-and-the-web-get)) |
| iOS and macOS | The App Store, after Apple's review |
| Web | Cloudflare Pages, project `blitz-split`: https://app.blitzsplit.com ([The production web app](#the-production-web-app)) |

A beta's:

| Platform | Channel |
| --- | --- |
| Android | Firebase App Distribution, group `beta`, project `blitz-split` |
| Web | Cloudflare Pages, project `blitz-split-beta`: https://blitz-split-beta.pages.dev |
| iOS and macOS | TestFlight, signed with the team's distribution certificate through fastlane match ([Apple signing](#apple-signing)) |
| iOS, ad hoc | Firebase App Distribution, group `ios-beta`: the same archive exported again for the registered iPhones ([iOS on Firebase App Distribution](#ios-on-firebase-app-distribution)) |

Each platform is a build job and a publish job of its own, between `plan` and `finalize`. The `android`, `web`, `ios` and
`macos` inputs of the workflow turn a platform off for a run.

The iOS and macOS jobs share two composite actions, [`apple-build`](../.github/actions/apple-build/action.yml) and
[`apple-upload`](../.github/actions/apple-upload/action.yml).

The upload uses `xcrun altool --upload-app`: if the runner's Xcode refuses it, switch `apple-upload` to
`xcrun iTMSTransporter`, or have the signing lane write `destination: upload` into the export options.

## Releasing a beta

```bash
./scripts/release/release-beta.sh          # the version the release notes lead to
./scripts/release/release-beta.sh 3.1.0    # another X.Y.Z
```

Or **Actions → release-beta → Run workflow**. The script dispatches the workflow and prints the link to the run.
`gh workflow run` only finds a workflow that is on `main`, so the first beta can only run after this one has merged.

1. `plan` resolves the version and picks the notes this beta tells, the ones in `release-notes/unreleased/` that the
   release before it didn't have ([release-notes.md](release-notes.md)), and shows both in the run summary.
2. Each platform builds and publishes: `android-build` and `android-distribute`, `web`, `ios-build` with
   `ios-upload` and `ios-distribute`, `macos-build` and `macos-upload`. Build and publish are separate jobs, so
   **Re-run failed jobs** sends a failed upload again without building a second time. The APK and the ad hoc IPA are
   kept as build artifacts for one day, since anyone can install them (the ad hoc IPA on the registered iPhones) from
   a public repository, and the App Store IPA and the package for a week, since they install nowhere but TestFlight:
   a re-run of an upload after that needs a new beta.
3. `finalize` tags the commit the run built and publishes the GitHub pre-release, whose notes list what the beta
   changed (as `plan` picked it), what each channel got, the build number and the platforms whose testers got a
   build (`Sent to testers:`, which the next beta reads), then squash-merges a pull request with the new
   `versionCode`, and the version when the release notes raised it, into `main`. It runs when at
   least one platform published, and a platform that failed does not hold the release back. A re-run of `finalize`
   after "Re-run failed jobs" rewrites the table of channels, so the release follows the results. A beta that only
   published the web leaves `main` alone, since it consumed no build number a store cares about.
4. `site-release-notes` sends the notes to blitz-split-site: `send-release-notes-to-site.sh` writes the version's
   files in a checkout of it, runs the site's own check, and opens and merges a pull request there, which deploys the
   site with the notes on its "What's new" page and in the feed the apps read. A beta with no note skips it, and so
   does one whose version the site already has. It runs after `finalize` even when the merge-back failed, but
   only for a version that got its tag: with `finalize` failed before the pre-release it stops, asking for
   `finalize` to be re-run first. **Re-run failed jobs** sends the notes again without touching the release.

One release runs at a time, betas and stable versions together, and one more can wait: GitHub keeps a single pending
run per concurrency group, so a third dispatch would cancel the one waiting. `release-beta.sh` and
`release-production.sh` refuse to dispatch while a run is already waiting
([`dispatch-release.sh`](../scripts/release/dispatch-release.sh)). The two workflows share the group because both
take the next `versionCode` from the same place.

## Releasing a stable version

```bash
./scripts/release/release-production.sh                                                    # every platform, to everyone
./scripts/release/release-production.sh track=internal submit_for_review=false web=false   # reaches no user
./scripts/release/release-production.sh version=3.1.0 rollout=20                           # a staged rollout of 3.1.0
./scripts/release/release-production.sh screenshots=true                                   # and the listings' screenshots made again
./scripts/release/release-production.sh screenshots=true screenshots_iphone_duo=true       # the iPhone Duo's too, closed and open
./scripts/release/release-production.sh screenshots=true screenshots_mac=true              # the Mac's too
```

The script takes the workflow's inputs as `input=value`, dispatches it on `main` and prints the link to the run. It
refuses to dispatch while a release is already waiting behind a running one, as `release-beta.sh` does. **Actions →
release-production → Run workflow** works too, with no such check: look at the Actions tab first, since a release
dispatched behind a waiting one cancels it without a word.

| Input | Default | What it does |
| --- | --- | --- |
| `version` | blank | `X.Y.Z`. Blank takes the one `version.properties` and the release notes give ([Versions](#versions)) |
| `android`, `ios`, `macos`, `web` | on | Turn a platform off for a run |
| `track` | `production` | The Google Play track: `production`, `beta` (open testing), `alpha` (closed testing) or `internal` |
| `rollout` | `100` | On the production track, the percentage of the users who get the release: under 100 is a staged rollout |
| `submit_for_review` | on | Sends the iOS and macOS builds to App Store review. Off, they stop at TestFlight |
| `phased_release` | off | Releases an approved update over seven days, to the people with automatic updates. Off, everyone gets it at once |
| `screenshots` | off | Makes the store listings' screenshots again and uploads them, with a release ([Screenshots with a release](#screenshots-with-a-release)). The App Store's are captured on the iPhone and the iPad |
| `screenshots_iphone_duo` | off | With `screenshots`, captures the iPhone Duo too, closed and open. Its captures fail on the runners today ([The devices the App Store's captures run on](#the-devices-the-app-stores-captures-run-on)) |
| `screenshots_mac` | off | With `screenshots`, captures the Mac too. Its capture fails on the runners today, and the Mac's listing has no screenshots yet, so the Mac can't be submitted until they are uploaded from a Mac |

**A run with nothing changed publishes to everyone, and nothing waits for an approval** (Pierre's calls, as in
Bible Planner): the production track at 100%, a submission for review that goes out by itself once Apple approves
it, and the web app at app.blitzsplit.com. The dispatch is the one gate ([Who releases](#who-releases)), so read the
inputs before it: cancelling a run stops what hasn't started, not what a store or the web already took
([Re-runs and what a run leaves](#re-runs-and-what-a-run-leaves)). For a run that reaches
no user, choose the `internal` track and turn `submit_for_review` and `web` off: the bundle goes to Play's internal
testers and the Apple builds to TestFlight, with no tag and no GitHub release. The web has no testing channel in this
workflow: its trial is a beta. With `web` left on, such a run still deploys the web for everyone and so tags the
version: `plan` warns about it at once, in the run summary, while the web job still builds.

1. `plan` checks who dispatched the run and from where ([Who releases](#who-releases)), resolves the version and the
   next `versionCode`, and picks the notes the version tells, once, for every job after it: the stores' "What's new"
   by language and the GitHub release's ([release-notes.md](release-notes.md)). The run summary shows the version,
   where each platform goes and the notes.
2. Each platform builds and publishes, in jobs of its own, so **Re-run failed jobs** sends a failed upload again
   without building a second time. The bundle, the IPA and the package are kept as build artifacts for a week.
   - `android-build` signs an AAB (`:androidApp:bundleRelease`) and prints its certificate, and `android-upload`
     sends it to the track with the notes ([What the stores and the web get](#what-the-stores-and-the-web-get)).
   - `ios-build` and `ios-upload`, `macos-build` and `macos-upload` are the betas' jobs: the build lands on
     TestFlight. `ios-submit` and `macos-submit` then send it for review, with `submit_for_review`.
   - `web` builds the web app at the stable version and deploys it to the Pages project `blitz-split`, with the
     action the beta's job uses ([`web-deploy`](../.github/actions/web-deploy/action.yml)). It is live at once.
   - The screenshots' jobs run next to them ([Screenshots with a release](#screenshots-with-a-release)).
3. `finalize` runs when a store took a build or the web was deployed, and a platform that failed does not hold it
   back. When the run **released**, that is, a bundle reached the production track, a build was sent for review or
   the web was deployed, it tags the commit the run built and publishes the GitHub release `X.Y.Z`: the build
   number, "What's new", what each channel got and the pull requests since the stable version before it. It then
   squash-merges a pull request into `main` with the `versionCode`, the `versionName` and the removal of the release
   notes' fragments the version told ([`merge-back.sh`](../scripts/release/merge-back.sh)). A run that released
   nothing leaves no tag and removes no fragment: it merges back its `versionCode`, and its version when that is
   higher than `main`'s, as a beta does. A run of the web alone, with every store platform off, leaves the
   `versionCode` of `main` alone, as a web-only beta does, and its GitHub release names no build number, since it
   consumed none a store cares about: the version and the fragments are all it merges back.
4. `site-release-notes` sends the notes of a released version to blitz-split-site, with the job the betas use
   ([`site-release-notes.yml`](../.github/workflows/site-release-notes.yml), which both workflows call). It doesn't
   wait for the stores' reviews (Pierre's call): the notes are out when the build is submitted. A version the site
   already has, as one written there by hand, is left as it is.

### Who releases

Nobody approves a stable release (Pierre's call: he and the other admins are the ones who release, and a release
they dispatch waits for no one, as in Bible Planner). The dispatch is the gate, and `plan` checks it before anything
is built:

- **An administrator of the repository** started the run: every job, from `plan` to `finalize` and the notes sent
  to the site, reads the permission of whoever started that attempt (`collaborators/<login>/permission`, the
  [`release-admin`](../.github/actions/release-admin/action.yml) action) and fails, naming them, when it isn't
  `admin` or can't be read. So **Re-run failed jobs**, which runs jobs again without `plan`, is an admin's too, and
  someone else's re-run stops before it builds anything. The [`store-listing`](../.github/workflows/store-listing.yml)
  workflow, which sends the listing text to the stores, starts each of its jobs with the same check
  ([store-listing.md](store-listing.md)), and so do
  [`app-store-screenshots`](../.github/workflows/app-store-screenshots.yml), which sends screenshots made on a Mac
  ([Screenshots made on a Mac](#screenshots-made-on-a-mac)), and
  [`play-store-screenshots`](../.github/workflows/play-store-screenshots.yml), which sends Google Play's alone
  ([Google Play's screenshots alone](#google-plays-screenshots-alone)).
- **On `main`**: a run dispatched on another branch would take that branch's copy of the workflow, and fails.
- **The `Production` environment only lets `main` deploy.** The jobs that reach a store or app.blitzsplit.com (the
  uploads, the submissions, the creation of the App Store version, the screenshots' uploads, the web) name the
  GitHub Environment `Production`, which has no required reviewer and a deployment branch policy with the branch
  `main` alone. GitHub makes an environment a job names and
  that doesn't exist, with no rule at all, so `plan` fails when it is missing, when it lets any branch or any
  protected branch deploy, when its policy allows anything but `main`, or when it can't be read. It was created
  once, in **Settings → Environments → New environment**: named `Production`, **Deployment branches and tags** set
  to **Selected branches and tags** with `main`, and no reviewer.

These checks are against mistakes: a release from the wrong branch, or by someone who shouldn't. They are no wall
around the secrets, which are the repository's, so a workflow changed on a branch someone can push to could still
read them ([Secrets](#secrets)).

### What the stores and the web get

- **Google Play.** The `play_upload` lane of [`fastlane/Fastfile`](../fastlane/Fastfile) sends the bundle, the track
  and the notes in one edit (`supply`), so a failed attempt leaves nothing behind. On the production track a
  `rollout` under 100 starts a staged rollout: raise it, halt it or finish it in the Play Console (Test and release ›
  Production). The other tracks get the release whole. The listing's text is not touched: it is
  `fastlane upload_listing`'s ([store-listing.md](store-listing.md)).
- **App Store.** The `app_store_submit` lane waits for App Store Connect to process the build (up to 45 minutes),
  makes `X.Y.Z` the editable version, writes its "What's new" and its copyright line with the current year
  ([store-listing.md](store-listing.md#how-it-reaches-the-stores)), selects the build and submits it (`deliver`). An
  approved version is released by itself, to everyone, or over seven days with `phased_release`. The Mac app is a
  version of its own, submitted by its own job. A locale with no screenshots of its own shows the primary
  language's, which the lane only reports; it refuses to submit when the primary language has none.
- **The notes** are written once, by language, by `release-notes.mjs collect --out`
  ([release-notes.md](release-notes.md#where-the-notes-go)). Each lane asks its store which locales the listing has
  and gives each one its language's notes (`pt-BR` reads `pt`, `es-419` and `es-MX` read `es`), or English's for a
  language the notes don't have. A locale the listing lacks gets nothing: the App Store would open an empty listing
  for it. An app's first version on a platform has no "What's new" and can't be phased, which the lane leaves out.
- **The web.** The `web-deploy` action builds `webApp/dist` at the version and deploys it with `wrangler pages
  deploy` to the production branch of the Pages project, which Cloudflare serves at once: there is no review and no
  rollout. Before building, it asks Cloudflare for the project and stops, naming it, when it doesn't exist or when
  its production branch isn't `main`, which would make the deployment a preview no domain serves. It warns when the
  project doesn't serve the domain yet ([The production web app](#the-production-web-app)).

### Screenshots with a release

**For the App Store, leave it off today and send the screenshots from a Mac first**
([Screenshots made on a Mac](#screenshots-made-on-a-mac)): what the runners captured in the one release that tried
was not fit for a listing. `screenshots` makes both stores' in a run that releases to both, so **Google Play's then
go alone**, before or after the release ([Google Play's screenshots alone](#google-plays-screenshots-alone)).

`screenshots` is off unless the release asks for it (Pierre's call): making them again takes a Mac runner per
App Store slot for up to two and a half hours, and most releases change nothing the listings show. Turn it on when
the images would come out different: **a language the listings gained**, a screen of
[the ten](store-listing-screenshots.md#what-the-images-show) that changed, a new device size. Left off, the stores
keep what they have: a new App Store version starts with the previous one's screenshots, and a locale with none of
its own shows the primary language's on the App Store and the default language's on Play. `ios-submit` and
`macos-submit` name those locales in a warning on the run's page, so a language added since the last screenshots
doesn't go unnoticed. Google Play has no such warning: a run of Android alone says nothing.

With `screenshots` on, a run that releases makes the listings' screenshots again
([store-listing-screenshots.md](store-listing-screenshots.md)), as Bible Planner's releases do:

- **Google Play**, after the bundle reached the production track: `android-screenshots-render` draws them next to
  the build (the [`play-screenshots-draw`](../.github/actions/play-screenshots-draw/action.yml) action), and
  `android-screenshots` uploads the ones that changed, a few locales per "send 1 of 5: \<locales\>" job (the jobs
  of [`play-store-screenshots-upload.yml`](../.github/workflows/play-store-screenshots-upload.yml), which also send
  them without a release). Play shows a listing's screenshots to everyone at once, so a testing track brings none.
  **The bundle always goes first**: `android-screenshots` needs `android-upload`, so the bundle is never the save
  the run's own screenshots left without quota ([Google Play's daily save quota](#google-plays-daily-save-quota)).
- **App Store**, before the submission, since a version in review can't take them, and only in a run that submits:
  `app-store-versions` creates the version (as it does in every run that submits, so the previous version's
  screenshots are copied into it long before anything looks at them), `apple-screenshots-capture` runs the UI tests on one simulator per slot
  and, for the Mac, photographs the app's window on the runner's own screen, `apple-screenshots-frame` frames the
  captures, and `ios-screenshots` and `macos-screenshots` upload them, one "send \<locale\>" job each, then
  "tidy up", which sorts every shelf and fails, naming it, on a shelf that doesn't hold every screen once
  ([Repairing shelves](store-listing-screenshots.md#repairing-shelves)), without stopping the release (the jobs of
  [`app-store-screenshots-upload.yml`](../.github/workflows/app-store-screenshots-upload.yml), which a set made on
  a Mac goes up through too). The slots are the
  iPhone's and the iPad's, plus the iPhone Duo's and the Mac's when the run asks for them
  ([The devices the App Store's captures run on](#the-devices-the-app-stores-captures-run-on)).

**The captures say which language they are on.** A slot's job captures its 31 languages one after the other, each
a run of the UI test of its own, about an hour and a half in all. Open the job's "Capture on the simulator" step:
every language has a "Capturing <slot>: <language> (2 of 31)" line there, folded over xcodebuild's output for it,
and a "Captured" one once it is done, with the time it took and about how many minutes the slot still takes. When
the job ends, its summary on the run's page has the table of the languages, each with its result and its time. A
language that fails is tried once more; failing again, the job goes on with the others (a third failed language
ends it) and fails at the end with an error on the run's page that names them, and the screen each stopped at is in
its row of the table ([One language at a time](store-listing-screenshots.md#one-language-at-a-time)). The frame
takes nothing of a slot that misses a language, but the job still uploads the languages it captured, to download.
The Mac's job, whose script launches the app itself, prints a "Captured <language>" line after each one and names
the language and the screen it stops at.

**The uploads are split into jobs**, one after the other, on each store (Bible Planner's releases lost a whole
upload to one image Google Play answered with a server error): what a store refused is all that failed, and the
rest is up. On the App Store a job sends one locale, which adds up over its 31 (not timed yet). On Google Play a
job sends a few, six unless the caller says otherwise, through one Play edit: six jobs for its 33 locales, since
each edit Google Play saves counts against [a daily quota](#google-plays-daily-save-quota) that one job per locale
ran out of (27 saves a set, about two minutes a locale, in the first release that ran them, 3.0.0). The submission
waits for the App Store's, and the next release or beta for them all, since they share the `release` group.

- **On Google Play**, a group's job sends it again with the images the run already drew, at any time: the listing
  takes them whenever, and the locales of the group Google Play already holds are left out. The job stops while a
  listing or a run of `play-store-screenshots` is going, and doesn't look for another release: don't run it again
  during one, nor after a later run sent newer images, which it would cover. Run it with **the job's own re-run
  button**, not **Re-run failed jobs**, which also runs every App Store job that failed without failing the run (a
  capture on a Mac runner, the uploads of a version already in review) and the jobs after them. A group Google Play
  refused for its daily save quota did not fail, and neither did the groups after it, which weren't tried: the run's
  page names their locales in a warning, and the same re-run button sends each later
  ([Google Play's daily save quota](#google-plays-daily-save-quota)).
- **On the App Store**, deliver deletes a locale's screenshots before it uploads the new ones, so a locale whose job
  failed halfway may be left with none: the submission goes on right after the uploads, and its warning names the
  locale. A version waiting for review takes no screenshots, so that job can only be run again, with its own re-run
  button too, once the submission is removed in App Store Connect. The jobs that come after it run again with it:
  the tidy-up, the submit job, `finalize` and `site-release-notes`, the last two finding their work done. Or leave
  the locale for the next release with `screenshots` on.

None of the App Store's jobs holds the release back: one that fails is listed as failed, the run is not, and the
submission goes on with the screenshots the version already has.

#### The devices the App Store's captures run on

With `screenshots` on, the App Store's captures run on the iPhone and the iPad. The iPhone Duo and the Mac are each
behind a marker of their own, off unless the release asks for it (Pierre's call, November 2026: their captures
don't work on the runners yet, so a release leaves them out until they do):

| Marker | Default | Slots it captures |
| --- | --- | --- |
| none: always, with `screenshots` | | `iphone63`, `iphone67`, `ipad13` |
| `screenshots_iphone_duo` | off | `iphoneduo` and `iphoneduoopen`, the Duo closed and open, together |
| `screenshots_mac` | off | `mac` |

`plan` lists the run's slots ([`app-store-capture-slots.sh`](../scripts/release/app-store-capture-slots.sh)) and
says them in the run summary
([`app-store-screenshots-plan.sh`](../scripts/release/app-store-screenshots-plan.sh)), and
`apple-screenshots-capture` is one job per slot of that list: a device left out starts no Mac runner, where a job
that started only to stop would hold one.

- **The iPhone's two sizes and the iPad's have no marker.** The frames of every device, the Mac's too, can't be
  drawn without them (`stageAppStoreScreenshots` fails), so a run without one of them would frame and upload
  nothing.
- **A marker does nothing without `screenshots`**, which stays the switch for all of it: `plan` warns about a run
  that turns one on with `screenshots` off, and so does `release-production.sh`.
- **A device is captured only in a run that can upload its images.** The Duo's go up with the iPhone's and the
  iPad's, so its marker needs `ios` on; the Mac's needs `macos` on; and both need a run that submits, as every
  App Store screenshot does. `plan` warns about a marker that is on and adds no capture. A run of the Mac alone
  still captures the iPhone and the iPad, for the frames, when `screenshots_mac` is on; without it, it captures
  nothing of the App Store's, and the summary says so.
- **Without the iPhone Duo**, the iPhone's and the iPad's shelves are the only ones a locale gets: deliver deletes
  all of a locale's screenshots before it uploads, so a locale that had the Duo's loses them, as it does when both
  of the Duo's captures fail. No release has uploaded them yet, and App Store Connect only requires the Duo's shelf
  from April 2027 ([store-listing-screenshots.md](store-listing-screenshots.md#what-the-images-show)).
- **Without the Mac**, nothing of the Mac's is uploaded or tidied up (`macos-screenshots` skips), so the Mac's
  listing keeps what it has. **That was nothing until a set was sent from a Mac**: the Mac app was never released,
  and `macos-submit` refuses to submit a version whose primary language has no screenshots. Leaving the Mac out
  doesn't make the Mac submittable, it only stops a capture known to fail from taking a runner. Until that capture
  works there, the Mac's images are made on a Mac with a Retina display and sent through GitHub while the Mac's
  version is still editable ([Screenshots made on a Mac](#screenshots-made-on-a-mac)), before the release that
  submits it, or before `macos-submit` is run again.

What is known of the three captures behind the markers, from the first release that ran them on GitHub's runners
(3.0.0, run 44222898644, 18 November 2026), where each of the three failed:

- **The iPhone Duo open (`iphoneduoopen`)**: "Couldn't press Open in Device Hub (is this terminal allowed in
  Accessibility?)". The Duo's simulator runtime is there (the `xcode-27` image has iOS 27.1 and 27.2) and the
  simulator booted, but the script's press on Open, through accessibility, didn't land, and nobody is at a runner to
  press it: the slot gave up half a minute later, having captured nothing
  ([store-listing-screenshots.md](store-listing-screenshots.md#what-the-images-show)).
- **The iPhone Duo closed (`iphoneduo`)**: the UI test failed about nine minutes in
  (`StoreScreenshotsUITests.testCaptureStoreScreenshots()`, "TEST EXECUTE FAILED"), after nineteen launches of the
  app some thirteen seconds apart and then four minutes with none. The cause is not known: `xcodebuild` ran quiet
  then, one test for the 26 languages, so the log names neither the screen, the language nor the failure. Since
  then the capture runs one language at a time and prints xcodebuild's output, so the next run that fails there
  says which language, at which screen and with what message. On this project's Mac the closed Duo's capture
  went through its 26 languages the same day, so the failure is the runner's so far.
- **The Mac (`mac`)**: "The Mac capture is 1440 px wide and the frame takes 2880: its display isn't Retina". The
  runner has no signing account, so the Mac app is built signed to run locally and without its entitlements, and
  that part worked: the app launched and its window was photographed. The runner's display is not Retina, and the
  script refuses a capture that would reach the listing blurred.

With `screenshots_iphone_duo` on, the Duo's shelf on the App Store holds ten screenshots for both displays, so five
screens go up open and five closed; when one of the two captures fails, the other fills the shelf, and when both do
the listing goes without the Duo.

### Screenshots made on a Mac

The App Store's screenshots reach it from a Mac, through the
[`app-store-screenshots`](../.github/workflows/app-store-screenshots.yml) workflow, **before** the release that
submits the version ([Sending a set made on a Mac](store-listing-screenshots.md#sending-a-set-made-on-a-mac) has
every step and what the check refuses). Until a release's own captures can be trusted, that is the only way: in
3.0.0's run the iPhone's and the iPad's were photographed before their content loaded, the Mac's runner has no
Retina display and the iPhone Duo's captures failed, and a release uploads what it captured without anyone looking.

```bash
fastlane screenshots platform:ios skip_upload:true                # capture and frame on the Mac, then look at them
node scripts/store-screenshots/local-set.mjs send --name 3.0.1     # check, pack, send to GitHub as a draft release
gh workflow run app-store-screenshots.yml -f set=3.0.1             # upload to App Store Connect, one locale per job
./scripts/release/release-production.sh                           # once that run is over: screenshots stay off
```

- **The order matters.** The workflow writes on the App Store version that is open for editing, whatever its
  number, and submits nothing. The release then renames that version to its own number (`app-store-versions`),
  which keeps its screenshots, and `ios-submit` and `macos-submit` find them there. A version waiting for review
  takes no screenshot, so a set can't follow its release.
- **Not at the same time.** A release that submitted in the middle of an upload would leave the locales still to
  go without theirs, so each side refuses the other
  ([`no-run-going`](../.github/actions/no-run-going/action.yml)). The workflow refuses to start while a release,
  beta or stable, is in progress or waiting, and each of its upload jobs checks again, a job run again later too.
  A stable release refuses to start while a run of the workflow isn't over, whatever it submits, since the upload
  jobs stop during any release: `plan`
  fails, naming it, however the release was dispatched, and `release-production.sh` says so before dispatching. A
  second set dispatched while one is going up is refused too, not queued.
- **Who and where** are a release's: an administrator of the repository, on `main`, with the jobs that reach App
  Store Connect in the `Production` environment and each one checking who started it
  ([Who releases](#who-releases)). The App Store Connect key never leaves GitHub.
- **One version alone**: `--platform mac` when sending and `-f ios=false` when dispatching, for the Mac's (or
  `--platform ios` and `-f macos=false`). deliver replaces every screenshot of a locale with what it is handed, so
  the iPhone's version always gets the iPhone's, the iPad's and the iPhone Duo's together, and a set that leaves a
  device out is refused unless `allow_missing_devices` says it is meant.
- **A locale that failed** fails the run and keeps the set: run that locale's job again with its own re-run button,
  while the version is still editable. Dispatch the release once the run's summary says every locale landed **and
  the shelves are as staged**.
- **A shelf that isn't as staged** fails the "tidy up" job, though every locale's job passed: an upload can leave a
  shelf listing a screenshot twice, or short of a file. The summary names the locale, the shelf and the file, and
  two inputs repair it without sending the whole set again
  ([Repairing shelves](store-listing-screenshots.md#repairing-shelves)):

  ```bash
  gh workflow run app-store-screenshots.yml -f set=3.0.1 -f locales="en-US de-DE"    # a file listed twice or missing: those locales again
  gh workflow run app-store-screenshots.yml -f tidy_only=true                        # a shelf out of order, or to check after a repair by hand
  ```

  The first needs the set, which the run keeps while a shelf isn't as staged. **Never delete one of a file's two
  entries in App Store Connect**: they are one screenshot under two ids, and both go.

### Google Play's screenshots alone

Google Play's screenshots are drawn by Robolectric, on any machine, and Google Play shows them to everyone the
moment they change, whatever release is out. So they don't need a release, and the
[`play-store-screenshots`](../.github/workflows/play-store-screenshots.yml) workflow sends them without one
([Sending Google Play's alone](store-listing-screenshots.md#sending-google-plays-alone)):

```bash
gh workflow run play-store-screenshots.yml -f validate_only=true   # draw them and ask Google Play: nothing changes
gh workflow run play-store-screenshots.yml                         # draw them and send them, a few locales per job
```

- **When.** Whenever Play's images should change and the App Store's must not be captured on the runners: a
  release with `screenshots` on that submits for review makes both stores', and today the App Store's come from a
  Mac ([above](#screenshots-made-on-a-mac)). Also between releases, for a screen or a frame that changed.
- **Before or after the release, not during.** The bundle and each group's screenshots go through a Play edit of
  their own, and the newer edit drops the one still open, so each side refuses the other
  ([`no-run-going`](../.github/actions/no-run-going/action.yml)): the workflow refuses to start while a stable
  release or a run of `store-listing` is in progress or waiting, each of its upload jobs checks again, and a stable
  release refuses to start while a run of it isn't over, whatever its platforms, `plan` failing however the release
  was dispatched and `release-production.sh` saying so first. Six jobs for Play's 33 locales, not timed yet (one
  job per locale took about an hour).
- **After the release when both happen on one day**: each job that changes something is a save of
  [Google Play's daily save quota](#google-plays-daily-save-quota), and so is the release's bundle, which must not
  be the one refused.
- **Who and where** are a release's: an administrator of the repository, on `main`, with the jobs that reach
  Google Play in the `Production` environment and each one checking who started it
  ([Who releases](#who-releases)).
- **A group that failed** fails the run and the others are up: run that group's job again with its own re-run
  button. A group Google Play refused for its daily save quota did not fail: see below.

### Google Play's daily save quota

Google Play counts every save of an app, the commit of a Play edit, and refuses the next one with "Daily save quota
exceeded" once the day had too many. A release's bundle is a save, a run of `store-listing` that reaches Google
Play is one, a release to a testing track is one, and each job of screenshots that changes something is one. Google
documents neither the number nor when the count starts over: roughly 50 saves in about 15 hours ran it out on
2026-11-18, when the screenshots still went one locale per job, and nobody here has measured when it comes back
([what was seen, and what the upload does about it](store-listing-screenshots.md#google-plays-daily-save-quota)).

- **The release first, the screenshots after.** A bundle Google Play refuses to save is a release that didn't go
  out, and its `android-upload` job fails with that error: run it again with **Re-run failed jobs** once Google Play
  takes saves again (when is not known: try the next day). Screenshots can wait a day. A release with `screenshots`
  on already sends its bundle before any screenshot.
- **Not a release to a testing track, a listing, the release and a set of screenshots on the same day.** Each is
  at least one save, and a set whose every locale changed is five (it was 27). Spread them when the day already had
  many.
- **Screenshots the quota refused are sent later, and nothing failed**: the job Google Play refused ends well with
  a warning, the groups after it stop before they reach Google Play, and the run's summary names the locales that
  didn't go. Send them with each job's own re-run button, the refused group first and one at a time, or dispatch
  `play-store-screenshots` again: either way a locale Google Play already holds costs no save
  ([Sending what didn't go](store-listing-screenshots.md#google-plays-daily-save-quota)).

### Re-runs and what a run leaves

- **A cancelled run** stops the jobs that haven't finished, and nothing undoes the ones that did: a bundle Google
  Play took, a build App Store Connect holds, a web deployment. `finalize` still runs when a store or the web took
  something, to record it: the tag, the GitHub release and the notes sent to the site when the run released, and
  the `versionCode` on `main`. Cancelling is clean only before the first upload job starts, a few minutes after the
  builds do. A job cancelled while it was uploading may have reached its store, and nothing says which:
  - An upload to a store counts for the `versionCode`, which is merged back all the same, since it only has to
    grow, and never for the tag: look at the Play Console or App Store Connect for what is there.
  - The web job is one step from built to live: look at https://app.blitzsplit.com. A deployment that landed in a
    run that tagged nothing is recorded by dispatching the web alone, with the same `version` typed.

  **Re-run failed jobs** on a cancelled run runs every job that was cancelled, the web's too: dispatch a new run for
  what is still wanted.
- **Re-run failed jobs** works for a week, while the build artifacts last. Every job after a build can run twice:
  Play takes or refuses an edit whole, `ios-submit` and `macos-submit` find a version already submitted (waiting
  for review, in review or beyond) and do nothing, and `finalize` skips what an earlier attempt did, as a beta's.
- **App Store Connect refuses a build number it already holds.** An `ios-upload` or `macos-upload` that failed
  although its build shows on TestFlight fails the same way on a re-run, and its submit job never starts: submit
  that build by hand in App Store Connect.
- **A platform left for later** (or rejected by a store and fixed): dispatch again with the same `version` typed and
  the other platforms off. The run builds `main` as it is then, with a new build number, keeps the tag and rewrites
  that platform's row in the GitHub release. Prefer one run for every platform: a later one carries the changes
  merged since, which the version's notes don't tell. While the version still waits for its review with the first
  build, the submit job fails, saying which build is there: remove that submission in App Store Connect first.
- **A store's review rejects the build.** Fix it and release the next version, or the same one again as above. The
  `versionCode` is consumed either way, and it only has to grow.
- **The web** is one job: a deployment that failed leaves the site as it was, and **Re-run failed jobs** builds and
  deploys again. To go back to an earlier deployment, use Cloudflare's dashboard (Workers & Pages › `blitz-split` ›
  Deployments › Rollback), then release a fix.

## Versions

| What | Value |
| --- | --- |
| Tag and GitHub release | `3.0.0-beta_7` (no `v`, an underscore before the number) |
| Android `versionName` | `3.0.0-beta_7` |
| A stable version | `3.0.0` everywhere: the tag, the GitHub release, Android's `versionName` and Apple's version string |
| Apple version string (iOS and macOS) | `3.0.0`: Apple refuses a suffix, so a beta is told apart by its build number. Profile still shows `3.0.0-beta_7`, from the Info.plist's `BlitzSplitVersionName` ([generate-project.sh](../iosApp/generate-project.sh)) |
| `versionCode`, and the Apple build number | the file's value plus one, on every beta |

[`version.properties`](../version.properties) stays the file every platform reads. Until a stable version is tagged,
its `versionName` is the version, moved by hand. From then on the release notes move it: a release is a patch above
the last stable version when the fragments since it only fix, and a minor one when one of them is new or improved
([release-notes.md](release-notes.md#the-version-a-release-gets)). The release takes the higher of that and the
file's, so a major version is still declared by hand in the file, and the merge-back writes the version it used. The
beta number is one past the highest existing `X.Y.Z-beta_N` tag for that `X.Y.Z`, so a new `X.Y.Z` starts again at
`beta_1`.

The `versionCode` is one past the highest of two numbers: the one in `version.properties` and the `Build number: N` line
that every release records in its GitHub release's notes ([`suggest-version.sh`](../scripts/release/suggest-version.sh)
resolves both, with `--stable` for a stable version). The release is published before the merge-back, so a release
never reuses a number even when the merge-back fails (the `Main rules` ruleset requires a pull request, with threads
resolved). A stable run that released nothing publishes no GitHub release, so its number is only in its merge-back:
the script also reads the `versionCode` the `release/merge-back` branch holds, which stays on GitHub until its pull
request merges, so the next release goes past it even then. Re-run its `finalize` if that failed.
Re-running `finalize` finishes the job: it skips the release that exists, leaves a `versionCode` that `main` already
holds (or a higher one someone raised meanwhile) alone, rebuilds the one merge-back branch, `release/merge-back`, from
the current `main` and pushes it over the old one, and opens the pull request only when none is open (an open one is
renamed for this release, so a pull request a failed run left behind never waits next to a newer one). The pull request is created with
`GITHUB_TOKEN`, so no pull request workflow runs for a version bump, and merged with `MERGE_WHEN_GREEN_TOKEN`, so the
push to `main` runs its workflows ([Merge when green](ci.md#merge-when-green)).

## Secrets

Repository secrets and variables (**Settings → Secrets and variables → Actions**).

| Secret | What it is |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | The release keystore, `base64 -i release.jks`. The same key every beta, or testers cannot update, and Google sign in is registered for its SHA-1. Google Play takes a stable version's bundle only when this is the app's **upload key** (Play Console › Test and release › App integrity › App signing): `android-build` prints the certificate's SHA-1 to compare. If it isn't, put the upload key's keystore here or ask Play to reset the upload key to this one |
| `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` | Its store password, alias and key password |
| `GOOGLE_SERVICES_JSON` | `androidApp/google-services.json`, `base64 -i` encoded: the file is not committed |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | A service account key (JSON) with the **Firebase App Distribution Admin** role on `blitz-split` |
| `PLAY_STORE_SERVICE_ACCOUNT_JSON` | For the stable versions: the key (the JSON itself, not base64) of a Google Cloud service account that the Play Console knows ([Google Play access](#google-play-access)) |
| `GOOGLE_SERVICE_INFO_PLIST` | `iosApp/Resources/GoogleService-Info.plist`, `base64 -i` encoded |
| `APP_STORE_CONNECT_API_KEY_ID`, `APP_STORE_CONNECT_API_ISSUER_ID`, `APP_STORE_CONNECT_API_KEY_P8` | An App Store Connect API key and the text of its `.p8`, for match and the upload. The Issuer ID is on Users and Access › Integrations. A stable version also edits the App Store version and submits it with this key, which takes the **App Manager** role (or Admin): a Developer key uploads builds and is refused the rest |
| `MATCH_PASSWORD` | The passphrase of `bible-planner-certs`, the certificates repository Bible Planner's releases use too ([Apple signing](#apple-signing)) |
| `MATCH_SSH_PRIVATE_KEY` | The private half of a read-only deploy key of `bible-planner-certs` |
| `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID` | A token with **Account › Cloudflare Pages › Edit**, and the account's id. One token serves both Pages projects, the betas' `blitz-split-beta` and the stable versions' `blitz-split`, which have to be in that account |
| `SENTRY_AUTH_TOKEN` | Optional: a Sentry organization token, which uploads the web app's source maps ([crash-reporting.md](crash-reporting.md#the-web)). Without it the web deploys the same, and its crash reports show the minified stack |
| `MERGE_WHEN_GREEN_TOKEN` | Already there, for the pull request merges: `finalize` merges the merge-back with it |
| `SITE_RELEASE_NOTES_TOKEN` | A fine-grained personal access token restricted to `Quartel-Enterprise/blitz-split-site`, with Contents and Pull requests set to Read and write: `site-release-notes` pushes the notes and merges their pull request with it. The organization caps a token at 366 days, so it has to be made again each year (the one made on 2026-11-14 expires on 2027-11-15); with it expired or missing the job fails naming the secret, the release itself is untouched, and a re-run of the job sends the notes |
| `GRADLE_ENCRYPTION_KEY` | Not set: the builds pass it to the Gradle action, which kept no configuration cache between runs when it was tried ([ci.md](ci.md#speed-and-timeouts)) |

| Variable (public values) | What it is |
| --- | --- |
| `SUPABASE_GOOGLE_WEB_CLIENT_ID` | See [Google sign in](#google-sign-in) |
| `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_APP_ID`, `VITE_FIREBASE_PROJECT_ID`, `VITE_FIREBASE_MEASUREMENT_ID` | The web app's Firebase config, `firebase apps:sdkconfig WEB <appId>` |
| `VITE_VAPID_PUBLIC_KEY` | The public half of the server's VAPID pair, see [Push](#push) |
| `VITE_SENTRY_DSN`, `SENTRY_ORG`, `SENTRY_PROJECT` | The web's crash reporting: the Sentry project's DSN and the slugs its source maps are uploaded to, see [crash-reporting.md](crash-reporting.md#the-web) |

Also needed once: **Settings → Actions → General → Allow GitHub Actions to create and approve pull requests**, for the
merge-back, and for the stable versions the `Production` environment, with no reviewer and a deployment branch
policy that only allows `main` ([Who releases](#who-releases)). Every secret above is a repository secret, which any
job of the repository's workflows can read: the environment limits which branch deploys through the release's jobs,
not who reads the secrets.

### The production web app

A stable version deploys the web app to the Cloudflare Pages project `blitz-split` (`CLOUDFLARE_PAGES_PROJECT` in
`release-production.yml`), served at https://app.blitzsplit.com. The workflow never creates a project or a domain:
this was set up once, by hand, in the Cloudflare account of `CLOUDFLARE_ACCOUNT_ID`, the one that has
`blitz-split-beta` and the zone `blitzsplit.com`, and the token and the account id of the secrets reach it:

- **The Pages project** `blitz-split`, with `main` as its production branch, the branch the workflow deploys to, at
  `blitz-split.pages.dev`.
- **Its custom domain** `app.blitzsplit.com` (Workers & Pages › `blitz-split` › Custom domains), active, with its
  certificate.
- **The DNS record** in the zone `blitzsplit.com`: a proxied CNAME, `app`, pointing at `blitz-split.pages.dev`.

The domain answers 522 until the project has a deployment: the first run of the `web` job is what puts the app
there.

Should any of it ever have to be made again, the `web` job says what is missing before it builds: it fails naming
the project when there is none, or when its production branch isn't `main`, and warns when the project doesn't
serve the domain. To create the project, make a Direct Upload project of that name from that account's dashboard
(Workers & Pages), or run `npx wrangler pages project create blitz-split --production-branch main`. wrangler
creates it in the account it is logged into, which may be another one: check `npx wrangler whoami` first, or run it
with that account's `CLOUDFLARE_ACCOUNT_ID` and a `CLOUDFLARE_API_TOKEN` exported.

What the production web app needs outside this repository, or its sign in fails:

- `https://app.blitzsplit.com` in the redirect URLs of the Supabase project (`docs/auth.md` of `blitz-split-api`).
- The same origin in the Google OAuth web client's authorized JavaScript origins and redirect URIs.
- `app.blitzsplit.com` in the domains of the Apple Services ID, for Sign in with Apple on the web.

Two changes in `blitz-split-site` wait for the first production deploy, and are not this workflow's:

- The invite links blitzsplit.com redirects to a web app still go to the beta's
  ([Joining and foldables](product/joining-and-folds.md)), until they point at app.blitzsplit.com.
- The site only offers the web app once `webAppLive` is on in its `public/stores.js`, next to the `webAppUrl` that
  already names https://app.blitzsplit.com.

### Google Play access

Once, for `PLAY_STORE_SERVICE_ACCOUNT_JSON`:

1. In the Google Cloud project linked to the Play Console (or any project of the account), enable the **Google Play
   Android Developer API**, create a service account and a JSON key for it.
2. In the Play Console, **Users and permissions → Invite new users**, with the service account's e-mail. Give it, for
   Blitz Split only: **Release to production, exclude devices, and use Play App Signing**, **Release apps to testing
   tracks** and **Manage store presence** (the screenshots).
3. Paste the JSON file's content into the secret. Play can take a few hours to accept a new account's calls.

The same file serves a local `fastlane screenshots`, as `PLAY_STORE_JSON_KEY_PATH`.

## Apple signing

The iOS and macOS betas sign with the team's **Apple Distribution** certificate and an App Store profile for
`com.quare.blitzsplit`, which [fastlane match](https://docs.fastlane.tools/actions/match/) keeps encrypted in the
private repository `Quartel-Enterprise/bible-planner-certs`, shared with Bible Planner (same team, `TTV2A365LG`). The
`apple_signing` lane of [`fastlane/Fastfile`](../fastlane/Fastfile) fetches them read-only into a temporary keychain,
switches the Release configuration of the app target and its extensions (the iPhone's balance widget,
`com.quare.blitzsplit.BalanceWidget`, which has profiles of its own) to manual signing (the committed `project.yml`
keeps automatic signing for local builds), sets `aps-environment` to `production`, and writes the export options. The macOS package
also needs the team's **Mac Installer Distribution** certificate, which the repository holds.

Why not Xcode's cloud-managed certificate, which the betas used first: the certificate's name is "Apple
Distribution: Antônio Vieira", and cloud signing writes the designated requirement with the name decomposed
(`o` + U+0302) while the certificate holds the composed `ô`. The requirement then never matches, `codesign --verify`
says the app "does not satisfy its designated Requirement", and App Store Connect refuses the upload with 90035
("Invalid Signature").

CI never creates a certificate or a profile. Once, from a Mac with fastlane and the passphrase, create the two App
Store profiles of `com.quare.blitzsplit` and the iOS ad hoc one (match reuses the certificates already in the
repository). The iOS job fetches the ad hoc profile too, so without it the iOS beta fails, TestFlight included:

```bash
fastlane match appstore --platform ios --app_identifier com.quare.blitzsplit,com.quare.blitzsplit.BalanceWidget --team_id TTV2A365LG --git_url git@github.com:Quartel-Enterprise/bible-planner-certs.git
fastlane match appstore --platform macos --additional_cert_types mac_installer_distribution --app_identifier com.quare.blitzsplit --team_id TTV2A365LG --git_url git@github.com:Quartel-Enterprise/bible-planner-certs.git
fastlane match adhoc --platform ios --app_identifier com.quare.blitzsplit,com.quare.blitzsplit.BalanceWidget --team_id TTV2A365LG --git_url git@github.com:Quartel-Enterprise/bible-planner-certs.git
```

A capability added to the App ID later (Sign in with Apple, push, associated domains) needs the profiles made again:
the same commands with `--force`.

The balance widget reads the file the iPhone app writes to the App Group `group.com.quare.blitzsplit`, so before the
first iOS beta that has it, once, on developer.apple.com › Certificates, Identifiers & Profiles: create the App Group
`group.com.quare.blitzsplit`, register the App ID `com.quare.blitzsplit.BalanceWidget`, turn App Groups on for it and
for `com.quare.blitzsplit` with that group, then make the iOS profiles again (the two iOS commands above with
`--force`). Until then match finds no profile for the widget and the iOS beta fails; the macOS one, which has no
widget, doesn't change.

## iOS on Firebase App Distribution

Besides TestFlight, the iOS job exports its archive a second time with the ad hoc profile `match AdHoc
com.quare.blitzsplit` and `ios-distribute` sends that IPA to the Firebase App Distribution group `ios-beta`. An ad hoc
build installs only on the iPhones the profile lists, so the group holds only testers whose iPhone is registered on the
team; the Android testers of `beta` stay out of it. The group has to exist before the first beta, or `ios-distribute`
fails: `firebase appdistribution:groups:create "iOS beta" ios-beta --project blitz-split`.

To add a tester's iPhone:

1. Get its UDID: Finder (click the line under the iPhone's name until it shows the UDID), `xcrun devicectl list
   devices` with the iPhone on a cable, or the device registration Firebase App Distribution offers when the tester
   accepts an invite (then Firebase console › App Distribution › Testers & Groups › Export Apple UDIDs).
2. Register it on the team: developer.apple.com › Certificates, Identifiers & Profiles › Devices, or
   `fastlane run register_device name:"<name>" udid:"<udid>" platform:ios team_id:TTV2A365LG api_key_path:<key.json>`.
3. Make the ad hoc profile again with the new device, from a Mac with the passphrase:

   ```bash
   fastlane match adhoc --platform ios --force_for_new_devices --app_identifier com.quare.blitzsplit,com.quare.blitzsplit.BalanceWidget --team_id TTV2A365LG --git_url git@github.com:Quartel-Enterprise/bible-planner-certs.git
   ```

4. Add the tester to the group: `firebase appdistribution:testers:add --group-alias ios-beta --project blitz-split <email>`.
5. Release a new beta: the builds already sent don't install on the new iPhone.

## Before the first beta: analytics consent

Release builds collect Firebase Analytics on every platform, and the betas are release builds: the APK, the TestFlight
builds and the web site all do. Whether the app asks first, or offers an opt-out, is Pierre's call
([#184](https://github.com/Quartel-Enterprise/blitz-split-client/issues/184)), and
[analytics/README.md](analytics/README.md#consent) says it has to be settled, with the Play Data safety form and the
App Store privacy labels, before a build reaches testers. The workflow does not enforce it. Until it is settled, a beta
for people other than the team is a decision to make knowingly.

## Apple's privacy manifest

[`iosApp/Resources/PrivacyInfo.xcprivacy`](../iosApp/Resources/PrivacyInfo.xcprivacy) goes into the iOS and the Mac
app. It says the app doesn't track, and gives Apple's reason for each
[required reason API](https://developer.apple.com/documentation/bundleresources/describing-use-of-required-reason-api)
the app's binary calls, the libraries linked into it included: the Kotlin ones ship no manifest of their own, so the
app's is where their calls are declared.

| API | Who calls it | Reason |
| --- | --- | --- |
| User defaults | The app's language, multiplatform-settings, Firebase | `CA92.1`: values the app itself wrote. `AppLocale.systemLanguage` also reads the device's language list from the global domain, which no reason names: the list `Locale.preferredLanguages` gives every app |
| File timestamps (`stat`, `fstat`, `lstat`) | The linked libraries | `C617.1`: files in the app's own container, where the database is |
| Disk space (`statfs`, `fstatfs`) | The linked libraries | `E174.1`: whether there is room to write |

A library added later may call another one. After adding one, build both apps (the Apple check of the `create-pr`
skill: the iOS simulator and the Mac), since the Mac links its own Kotlin target and Swift sources, then list what
each binary asks the system for and compare with the table. The `nm` line prints the C functions and the Foundation
classes and keys, the `strings` line the ones that are only method names:

```bash
for BIN in \
  iosApp/build/DerivedData/Build/Products/Debug-iphonesimulator/BoraRachar.app/BoraRachar.debug.dylib \
  iosApp/build/DerivedData/Build/Products/Debug/BoraRachar.app/Contents/MacOS/BoraRachar.debug.dylib; do
  echo "== $BIN"
  nm -u "$BIN" | grep -E '^_[fl]?stat(at|fs|vfs)?$|getattrlist|mach_absolute_time|NSUserDefaults|NSURL(Creation|ContentModification)DateKey|NSFile(Creation|Modification)Date|NSFileSystem(Free)?Size|NSURLVolume.*CapacityKey'
  strings - "$BIN" | grep -xE 'systemUptime|activeInputModes|fileCreationDate|fileModificationDate'
done
```

Apple also writes to the account's e-mail after an upload that misses one (`ITMS-91053`), naming the API.

The manifest doesn't list the data the app collects (`NSPrivacyCollectedDataTypes`) yet: it has to say what App
Store Connect's App Privacy says, and those answers, like the ones of Play's Data safety form, wait for Pierre to
confirm them. [analytics/README.md](analytics/README.md#consent) holds the ones Analytics already settled.

## Google sign in

The Android beta signs in with Google through Credential Manager, which needs three things outside the code:

1. **`SUPABASE_GOOGLE_WEB_CLIENT_ID`**, a repository **variable** (it is a public client id, not a secret): the Google
   OAuth web client id, the same one `~/.gradle/gradle.properties` sets for local builds. The workflow fails before
   building when it is empty, since an empty id builds an app whose Google sign in always fails.
2. **The SHA-1 of the signing key on the Firebase Android app.** Credential Manager only answers an app whose package
   and signing certificate match an Android OAuth client of the project, and registering the fingerprint in Firebase
   creates that client. The `android-build` job prints the APK's SHA-1 in its summary; register it with
   `firebase apps:android:sha:create 1:791706056713:android:05eeec36250eb20299a0d8 <sha1> --project blitz-split`.
   An APK from App Distribution is signed with the key in `ANDROID_KEYSTORE_BASE64`, not with Play's app signing key,
   so the Play fingerprints already on the app do not cover it unless the keystore is the same upload key.
3. **The Supabase Google provider** enabled with that web client id (it already is: debug builds sign in), see
   `docs/auth.md` of the `blitz-split-api` repository.

## Testers

The `beta` group exists in the Firebase project. Add people with
`firebase appdistribution:testers:add --group-alias beta --project blitz-split <emails>`, or in the console. A tester
gets an e-mail with the invite and installs from the App Tester app or the web page.

## Push

The betas get push notifications: the `push` edge function of the `blitz-split-api` project sends to FCM, APNs and Web
Push. Its secrets live in the Supabase project, not in this repository: `PUSH_WEBHOOK_SECRET` (with the Vault entries
`push_function_url` and `push_webhook_secret`), `FCM_SERVICE_ACCOUNT` (a service account with only the Firebase Cloud
Messaging Admin role), `APNS_KEY_ID`, `APNS_PRIVATE_KEY` and `APNS_TEAM_ID` (an APNs key for the Sandbox and Production
environments, so a development and a TestFlight build both work) and `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`. How they
are used is in [push-notifications.md](push-notifications.md).

## Outside the stores

- **Web sign in.** Supabase's redirect URLs and the Google and Apple providers only know the domains they were
  given. For the beta site, add `https://blitz-split-beta.pages.dev` to the redirect URLs of the Supabase project, to
  the Google OAuth client's authorized JavaScript origins and redirect URIs, and (for Sign in with Apple on the web)
  to the Services ID's domains. The production web app needs the same for its own domain
  ([The production web app](#the-production-web-app)).
- **Invite links** open the app only through a domain the app owns (see
  [Joining and foldables](product/joining-and-folds.md)), and on Android only when the APK's signing key's SHA-256 is
  in blitzsplit.com's `assetlinks.json`, which lists the Play keys. The `android-build` job prints that SHA-256 beside
  the SHA-1 and warns when `assetlinks.json` doesn't list it; the beta then opens invite links in the browser (Android 8
  to 11 asks which app opens them), which blitzsplit.com redirects to the beta web app. To have the beta open them, add the SHA-256 to
  `public/.well-known/assetlinks.json` of `blitz-split-site`, the copy Android reads, and keep
  `webApp/public/.well-known` the same. Listing it lets any APK signed with the beta keystore open blitzsplit.com's
  links, so it is only as safe as the keystore and its GitHub secrets: never list a debug key (see
  [getting-started](getting-started.md#build-and-run-the-android-app)).
- **TestFlight** needs the app record `com.quare.blitzsplit` in App Store Connect (it exists: this app replaces the
  legacy one) and, for external testers, a beta review of the first build.
