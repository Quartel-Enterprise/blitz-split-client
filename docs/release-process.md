# Release process

How a beta of Bora Rachar reaches a closed group of testers without the stores or the app's domain. One command
tags the release, publishes each platform to its own channel and records it as a GitHub pre-release. The workflow is
[`release-beta.yml`](../.github/workflows/release-beta.yml).

## Channels

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
./scripts/release/release-beta.sh          # the versionName of version.properties
./scripts/release/release-beta.sh 3.1.0    # another X.Y.Z
```

Or **Actions → release-beta → Run workflow**. The script dispatches the workflow and prints the link to the run.
`gh workflow run` only finds a workflow that is on `main`, so the first beta can only run after this one has merged.

1. `plan` resolves the version and shows it in the run summary.
2. Each platform builds and publishes: `android-build` and `android-distribute`, `web`, `ios-build` and `ios-upload`,
   `macos-build` and `macos-upload`. Build and publish are separate jobs, so **Re-run failed jobs** sends a failed
   upload again without building a second time. The APK is kept as a build artifact for one day, since anyone can
   install it from a public repository, and the IPA and the package for a week, since they install nowhere but
   TestFlight: a re-run of an upload after that needs a new beta.
3. `finalize` tags the commit the run built and publishes the GitHub pre-release, whose notes list what each channel got
   and the build number, then squash-merges a pull request with the new `versionCode` into `main`. It runs when at
   least one platform published, and a platform that failed does not hold the release back. A re-run of `finalize`
   after "Re-run failed jobs" rewrites the table of channels, so the release follows the results. A beta that only
   published the web leaves `main` alone, since it consumed no build number a store cares about.

One beta runs at a time and one more can wait: GitHub keeps a single pending run per concurrency group, so a third
dispatch would cancel the one waiting. `release-beta.sh` refuses to dispatch while a run is already waiting.

## Versions

| What | Value |
| --- | --- |
| Tag and GitHub release | `3.0.0-beta_7` (no `v`, an underscore before the number) |
| Android `versionName` | `3.0.0-beta_7` |
| Apple version string (iOS and macOS) | `3.0.0`: Apple refuses a suffix, so a beta is told apart by its build number. Profile still shows `3.0.0-beta_7`, from the Info.plist's `BlitzSplitVersionName` ([generate-project.sh](../iosApp/generate-project.sh)) |
| `versionCode`, and the Apple build number | the file's value plus one, on every beta |

[`version.properties`](../version.properties) stays the single source. A beta never changes its `versionName`: that
moves to the next version by hand, when the work for it starts. The beta number is one past the highest existing
`X.Y.Z-beta_N` tag for that `X.Y.Z`, so a new `X.Y.Z` starts again at `beta_1`.

The `versionCode` is one past the highest of two numbers: the one in `version.properties` and the `Build number: N` line
that every beta release records in its notes. The release is published before the merge-back, so a beta never reuses a
number even when the merge-back fails (the `Main rules` ruleset requires a pull request, with threads resolved).
Re-running `finalize` finishes the job: it skips the release that exists, leaves a `versionCode` that `main` already
holds (or a higher one someone raised meanwhile) alone, rebuilds the one merge-back branch, `release/merge-back`, from
the current `main` and pushes it over the old one, and opens the pull request only when none is open (an open one is
renamed for this beta, so a pull request a failed run left behind never waits next to a newer one). The pull request is created with
`GITHUB_TOKEN`, so no pull request workflow runs for a version bump, and merged with `MERGE_WHEN_GREEN_TOKEN`, so the
push to `main` runs its workflows ([Merge when green](ci.md#merge-when-green)).

## Secrets

Repository secrets and variables (**Settings → Secrets and variables → Actions**).

| Secret | What it is |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | The release keystore, `base64 -i release.jks`. The same key every beta, or testers cannot update, and Google sign in is registered for its SHA-1 |
| `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` | Its store password, alias and key password |
| `GOOGLE_SERVICES_JSON` | `androidApp/google-services.json`, `base64 -i` encoded: the file is not committed |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | A service account key (JSON) with the **Firebase App Distribution Admin** role on `blitz-split` |
| `GOOGLE_SERVICE_INFO_PLIST` | `iosApp/Resources/GoogleService-Info.plist`, `base64 -i` encoded |
| `APP_STORE_CONNECT_API_KEY_ID`, `APP_STORE_CONNECT_API_ISSUER_ID`, `APP_STORE_CONNECT_API_KEY_P8` | An App Store Connect API key and the text of its `.p8`, for match and the upload. The Issuer ID is on Users and Access › Integrations |
| `MATCH_PASSWORD` | The passphrase of `bible-planner-certs`, the certificates repository Bible Planner's releases use too ([Apple signing](#apple-signing)) |
| `MATCH_SSH_PRIVATE_KEY` | The private half of a read-only deploy key of `bible-planner-certs` |
| `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID` | A token with **Account › Cloudflare Pages › Edit**, and the account's id |
| `MERGE_WHEN_GREEN_TOKEN` | Already there, for the pull request merges: `finalize` merges the merge-back with it |
| `GRADLE_ENCRYPTION_KEY` | Optional, and not set today: the builds pass it to the Gradle action, which then keeps the configuration cache between runs ([ci.md](ci.md)) |

| Variable (public values) | What it is |
| --- | --- |
| `SUPABASE_GOOGLE_WEB_CLIENT_ID` | See [Google sign in](#google-sign-in) |
| `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_APP_ID`, `VITE_FIREBASE_PROJECT_ID`, `VITE_FIREBASE_MEASUREMENT_ID` | The web app's Firebase config, `firebase apps:sdkconfig WEB <appId>` |
| `VITE_VAPID_PUBLIC_KEY` | The public half of the server's VAPID pair, see [Push](#push) |

Also needed once: **Settings → Actions → General → Allow GitHub Actions to create and approve pull requests**, for the
merge-back.

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
Store profiles of `com.quare.blitzsplit` (match reuses the certificates already in the repository):

```bash
fastlane match appstore --platform ios --app_identifier com.quare.blitzsplit --team_id TTV2A365LG --git_url git@github.com:Quartel-Enterprise/bible-planner-certs.git
fastlane match appstore --platform macos --additional_cert_types mac_installer_distribution --app_identifier com.quare.blitzsplit --team_id TTV2A365LG --git_url git@github.com:Quartel-Enterprise/bible-planner-certs.git
```

A capability added to the App ID later (Sign in with Apple, push, associated domains) needs the profiles made again:
the same commands with `--force`.

## iOS on Firebase App Distribution

Besides TestFlight, the iOS job exports its archive a second time with the ad hoc profile `match AdHoc
com.quare.blitzsplit` and `ios-distribute` sends that IPA to the Firebase App Distribution group `ios-beta`. An ad hoc
build installs only on the iPhones the profile lists, so the group holds only testers whose iPhone is registered on the
team; the Android testers of `beta` stay out of it.

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
