# Release process

How a beta of Bora Rachar reaches a closed group of testers without the stores or the app's domain. One command
tags the release, publishes each platform to its own channel and records it as a GitHub pre-release. The workflow is
[`release-beta.yml`](../.github/workflows/release-beta.yml).

## Channels

| Platform | Channel |
| --- | --- |
| Android | Firebase App Distribution, group `beta`, project `blitz-split` |
| Web | Cloudflare Pages, project `blitz-split-beta`: https://blitz-split-beta.pages.dev |
| iOS and macOS | TestFlight, signed by Xcode with the App Store Connect API key |

Each platform is a build job and a publish job of its own, between `plan` and `finalize`. The `android`, `web`, `ios` and
`macos` inputs of the workflow turn a platform off for a run.

The iOS and macOS jobs share two composite actions, [`apple-build`](../.github/actions/apple-build/action.yml) and
[`apple-upload`](../.github/actions/apple-upload/action.yml).

The Apple and web jobs are written but have not run yet: expect the first run to need a fix or two. The likeliest
spot is the upload, which uses `xcrun altool --upload-app`: if the runner's Xcode refuses it, switch
`apple-upload` to `xcrun iTMSTransporter` or export with `destination: upload` in `iosApp/ExportOptions.plist`.

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
   upload again without building a second time. The binaries are kept as build artifacts for one day.
3. `finalize` squash-merges a pull request with the new `versionCode` into `main`, tags the commit the run built and
   publishes the GitHub pre-release, whose notes list what each channel got. It runs when at least one platform
   published, since the Apple stores refuse a build number they already hold, and a platform that failed does not hold
   the release back.

## Versions

| What | Value |
| --- | --- |
| Tag and GitHub release | `3.0.0-beta_7` (no `v`, an underscore before the number) |
| Android `versionName` | `3.0.0-beta_7` |
| Apple version string (iOS and macOS) | `3.0.0`: Apple refuses a suffix, so a beta is told apart by its build number |
| `versionCode`, and the Apple build number | the file's value plus one, on every beta |

[`version.properties`](../version.properties) stays the single source. A beta never changes its `versionName`: that
moves to the next version by hand, when the work for it starts. The beta number is one past the highest existing
`X.Y.Z-beta_N` tag for that `X.Y.Z`, so a new `X.Y.Z` starts again at `beta_1`.

The merge-back commit is why `versionCode` only grows: the next beta, and the store build after the betas, start above
the code this one used. If the pull request cannot merge (the `Main rules` ruleset requires one, with threads
resolved), `finalize` fails before tagging. **Re-run it before starting another beta**: each part of it skips what
the earlier attempt did (the code already on `main`, the pushed branch, the open pull request), so a re-run finishes
the job. Until the code reaches `main`, the next beta plans the same `versionCode` again, and App Store Connect refuses
a build number it already holds. `finalize` never lowers a `versionCode` that someone raised on `main` meanwhile.

## Secrets

Repository secrets and variables (**Settings → Secrets and variables → Actions**).

| Secret | What it is |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | The release keystore, `base64 -i release.jks`. The same key every beta, or testers cannot update, and Google sign in is registered for its SHA-1 |
| `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` | Its store password, alias and key password |
| `GOOGLE_SERVICES_JSON` | `androidApp/google-services.json`, `base64 -i` encoded: the file is not committed |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | A service account key (JSON) with the **Firebase App Distribution Admin** role on `blitz-split` |
| `GOOGLE_SERVICE_INFO_PLIST` | `iosApp/Resources/GoogleService-Info.plist`, `base64 -i` encoded |
| `APP_STORE_CONNECT_API_KEY_ID`, `APP_STORE_CONNECT_API_ISSUER_ID`, `APP_STORE_CONNECT_API_KEY_P8` | An App Store Connect API key with the **Admin** role (Xcode needs it to create the cloud-managed distribution certificate) and the text of its `.p8`. The Issuer ID is on Users and Access › Integrations |
| `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID` | A token with **Account › Cloudflare Pages › Edit**, and the account's id |

| Variable (public values) | What it is |
| --- | --- |
| `SUPABASE_GOOGLE_WEB_CLIENT_ID` | See [Google sign in](#google-sign-in) |
| `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_APP_ID`, `VITE_FIREBASE_PROJECT_ID`, `VITE_FIREBASE_MEASUREMENT_ID` | The web app's Firebase config, `firebase apps:sdkconfig WEB <appId>` |
| `VITE_VAPID_PUBLIC_KEY` | The public half of the server's VAPID pair, see [Push](#push) |

Also needed once: **Settings → Actions → General → Allow GitHub Actions to create and approve pull requests**, for the
merge-back.

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
  [Joining and foldables](product/joining-and-folds.md)): a beta distributed outside the stores does not change that,
  and a link opens the browser on the production domain.
- **TestFlight** needs the app record `com.quare.blitzsplit` in App Store Connect (it exists: this app replaces the
  legacy one) and, for external testers, a beta review of the first build.
