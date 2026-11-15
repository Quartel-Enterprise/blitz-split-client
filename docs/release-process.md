# Release process

How a release of Bora Rachar goes out. A **beta** reaches a closed group of testers without the stores or the app's
domain: one command tags it, publishes each platform to its own channel and records it as a GitHub pre-release
([`release-beta.yml`](../.github/workflows/release-beta.yml)). A **stable version** goes to Google Play and the App
Store, after an approval ([`release-production.yml`](../.github/workflows/release-production.yml),
[Releasing a stable version](#releasing-a-stable-version)).

## Channels

A stable version goes to Google Play and the App Store ([What the stores get](#what-the-stores-get)). A beta's
channels:

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
run per concurrency group, so a third dispatch would cancel the one waiting. `release-beta.sh` refuses to dispatch
while a run is already waiting. The two workflows share the group because both take the next `versionCode` from the
same place.

## Releasing a stable version

**Actions → release-production → Run workflow**, on `main`. Or from a terminal, every input written out:

```bash
gh workflow run release-production.yml --ref main -f track=production -f rollout=100 -f submit_for_review=true
```

| Input | Default | What it does |
| --- | --- | --- |
| `version` | blank | `X.Y.Z`. Blank takes the one `version.properties` and the release notes give ([Versions](#versions)) |
| `android`, `ios`, `macos` | on | Turn a platform off for a run |
| `track` | `production` | The Google Play track: `production`, `beta` (open testing), `alpha` (closed testing) or `internal` |
| `rollout` | `100` | On the production track, the percentage of the users who get the release: under 100 is a staged rollout |
| `submit_for_review` | on | Sends the iOS and macOS builds to App Store review. Off, they stop at TestFlight |
| `phased_release` | off | Releases an approved update over seven days, to the people with automatic updates. Off, everyone gets it at once |
| `screenshots` | on | Makes the store listings' screenshots again and uploads them, with a release |

**A run with nothing changed publishes to everyone** (Pierre's call): the production track at 100%, and a submission
for review that goes out by itself once Apple approves it. [The approval](#the-approval) is the one gate. For a run
that reaches no user, choose the `internal` track and turn `submit_for_review` off: the bundle goes to Play's internal
testers and the Apple builds to TestFlight, with no tag and no GitHub release.

1. `plan` resolves the version and the next `versionCode`, checks that the approval can't be skipped, and picks the
   notes the version tells, once, for every job after it: the stores' "What's new" by locale and the GitHub release's
   ([release-notes.md](release-notes.md)). The run summary shows the version, where each platform goes and the notes.
2. `approve` waits for [the approval](#the-approval). Nothing is built before it.
3. Each platform builds and publishes, in jobs of its own, so **Re-run failed jobs** sends a failed upload again
   without building a second time. The bundle, the IPA and the package are kept as build artifacts for a week.
   - `android-build` signs an AAB (`:androidApp:bundleRelease`) and prints its certificate, and `android-upload`
     sends it to the track with the notes ([What the stores get](#what-the-stores-get)).
   - `ios-build` and `ios-upload`, `macos-build` and `macos-upload` are the betas' jobs: the build lands on
     TestFlight. `ios-submit` and `macos-submit` then send it for review, with `submit_for_review`.
   - The screenshots' jobs run next to them ([Screenshots with a release](#screenshots-with-a-release)).
4. `finalize` runs when a store took a build, and a platform that failed does not hold it back. When the run
   **released**, that is, a bundle reached the production track or a build was sent for review, it tags the commit
   the run built and publishes the GitHub release `X.Y.Z`: the build number, "What's new", what each store got and
   the pull requests since the stable version before it. It then squash-merges a pull request into `main` with the
   `versionCode`, the `versionName` and the removal of the release notes' fragments the version told
   ([`merge-back.sh`](../scripts/release/merge-back.sh)). A run that released nothing leaves no tag and removes no
   fragment: it merges back its `versionCode`, and its version when that is higher than `main`'s, as a beta does.
5. `site-release-notes` sends the notes of a released version to blitz-split-site, with the job the betas use
   ([`site-release-notes.yml`](../.github/workflows/site-release-notes.yml), which both workflows call). It doesn't
   wait for the stores' reviews (Pierre's call): the notes are out when the build is submitted. A version the site
   already has, as one written there by hand, is left as it is.

### The approval

The `approve` job names the GitHub Environment `Production`, and waits until one of its **required reviewers**
approves the run on its page. Reject a run to change anything, and dispatch another. Create the environment once, in
**Settings → Environments → New environment**: name it `Production`, tick **Required reviewers** and add who may
release. GitHub makes an environment a job names and that doesn't exist, with no protection, and the run would then
publish with no one asked: so `plan` fails when the environment is missing, has no required reviewer, or can't be
read.

A run waiting for its approval holds the releases' concurrency group: a beta dispatched meanwhile waits behind it, and
a second one would cancel that beta. Approve or reject a run before starting another release.

### What the stores get

- **Google Play.** The `play_upload` lane of [`fastlane/Fastfile`](../fastlane/Fastfile) sends the bundle, the track
  and the notes in one edit (`supply`), so a failed attempt leaves nothing behind. On the production track a
  `rollout` under 100 starts a staged rollout: raise it, halt it or finish it in the Play Console (Test and release ›
  Production). The other tracks get the release whole. The listing's text is not touched.
- **App Store.** The `app_store_submit` lane waits for App Store Connect to process the build (up to 45 minutes),
  makes `X.Y.Z` the editable version, writes its "What's new", selects the build and submits it (`deliver`). An
  approved version is released by itself, to everyone, or over seven days with `phased_release`. The Mac app is a
  version of its own, submitted by its own job. The lane refuses to submit a version with a locale left without
  screenshots, which Apple would reject.
- **The notes** are written once per store locale by `release-notes.mjs collect --out`
  ([release-notes.md](release-notes.md#where-the-notes-go)). Each lane asks its store which languages the listing
  has and sends those: a locale the listing lacks is left out (the App Store would open an empty listing for it),
  and a listing locale the notes don't name (`en-GB`, `fr-CA`) gets the notes of its language, or English's. An
  app's first version on a platform has no "What's new" and can't be phased, which the lane leaves out.

### Screenshots with a release

With `screenshots` on, a run that releases makes the listings' screenshots again
([store-listing-screenshots.md](store-listing-screenshots.md)), as Bible Planner's releases do:

- **Google Play**, after the bundle reached the production track: `android-screenshots` draws them and uploads the
  ones that changed. Play shows a listing's screenshots to everyone at once, so a testing track brings none.
- **App Store**, before the submission, since a version in review can't take them, and only in a run that submits:
  `app-store-versions` creates the version, `apple-screenshots-capture` runs the UI tests on one simulator per slot
  and, for the Mac, on the runner's own screen, `apple-screenshots-frame` frames the captures, and `ios-screenshots`
  and `macos-screenshots` upload them.

None of the App Store's jobs holds the release back: one that fails is listed as failed, the run is not, and the
submission goes on with the screenshots the version already has. Two captures are expected to fail on some runners:
the iPhone Duo's, where Xcode has no runtime for it, and the Mac's, which had never run in CI when this was written.
The runner has no signing account, so the Mac app is built signed to run locally and without its entitlements, and
the job refuses captures from a display that isn't Retina, which would reach the listing blurred. The Mac's listing
then keeps its screenshots: `fastlane screenshots platform:ios` on a Mac replaces them, on a version that is still
editable.

### Re-runs and what a run leaves

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
- **The web** is not part of this workflow.

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
resolved). A stable run that released nothing publishes no GitHub release, so only its merge-back records the number
it used: re-run its `finalize` if that failed.
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
| `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID` | A token with **Account › Cloudflare Pages › Edit**, and the account's id |
| `MERGE_WHEN_GREEN_TOKEN` | Already there, for the pull request merges: `finalize` merges the merge-back with it |
| `SITE_RELEASE_NOTES_TOKEN` | A fine-grained personal access token restricted to `Quartel-Enterprise/blitz-split-site`, with Contents and Pull requests set to Read and write: `site-release-notes` pushes the notes and merges their pull request with it. The organization caps a token at 366 days, so it has to be made again each year (the one made on 2026-11-14 expires on 2027-11-15); with it expired or missing the job fails naming the secret, the release itself is untouched, and a re-run of the job sends the notes |
| `GRADLE_ENCRYPTION_KEY` | Optional, and not set today: the builds pass it to the Gradle action, which then keeps the configuration cache between runs ([ci.md](ci.md)) |

| Variable (public values) | What it is |
| --- | --- |
| `SUPABASE_GOOGLE_WEB_CLIENT_ID` | See [Google sign in](#google-sign-in) |
| `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_APP_ID`, `VITE_FIREBASE_PROJECT_ID`, `VITE_FIREBASE_MEASUREMENT_ID` | The web app's Firebase config, `firebase apps:sdkconfig WEB <appId>` |
| `VITE_VAPID_PUBLIC_KEY` | The public half of the server's VAPID pair, see [Push](#push) |

Also needed once: **Settings → Actions → General → Allow GitHub Actions to create and approve pull requests**, for the
merge-back, and for the stable versions the `Production` environment with its required reviewers
([The approval](#the-approval)). Every secret above is a repository secret, which any job of the repository's
workflows can read: the environment is a gate on the run, not on the secrets.

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
switches the app target's Release configuration to manual signing (the committed `project.yml` keeps automatic
signing for local builds), sets `aps-environment` to `production`, and writes the export options. The macOS package
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
fastlane match appstore --platform ios --app_identifier com.quare.blitzsplit --team_id TTV2A365LG --git_url git@github.com:Quartel-Enterprise/bible-planner-certs.git
fastlane match appstore --platform macos --additional_cert_types mac_installer_distribution --app_identifier com.quare.blitzsplit --team_id TTV2A365LG --git_url git@github.com:Quartel-Enterprise/bible-planner-certs.git
fastlane match adhoc --platform ios --app_identifier com.quare.blitzsplit --team_id TTV2A365LG --git_url git@github.com:Quartel-Enterprise/bible-planner-certs.git
```

A capability added to the App ID later (Sign in with Apple, push, associated domains) needs the profiles made again:
the same commands with `--force`.

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
   fastlane match adhoc --platform ios --force_for_new_devices --app_identifier com.quare.blitzsplit --team_id TTV2A365LG --git_url git@github.com:Quartel-Enterprise/bible-planner-certs.git
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

- **Web sign in.** Supabase's redirect URLs and the Google and Apple providers know the production domain only. For the
  beta site, add `https://blitz-split-beta.pages.dev` to the redirect URLs of the Supabase project, to the Google OAuth
  client's authorized JavaScript origins and redirect URIs, and (for Sign in with Apple on the web) to the Services ID's
  domains.
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
