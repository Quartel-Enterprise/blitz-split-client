# Push notifications

Atividade's push notifications (page 12, "só o que te envolve") and the weekly "Lembretes de acerto" (Perfil ›
Notificações, 4.8). The backend picks who gets what and sends them; each app only registers where it can be reached
and words what arrives. Who gets what, and how the `push` edge function talks to FCM, APNs and Web Push, is in the
`blitz-split-api` repository: `docs/database.md#push-notifications` and `supabase/functions/push/README.md`.

## How it fits

1. An event lands in `activity_events` (a bill saved, a payment recorded, someone joining…). The trigger
   `activity_events_push` asks the `push` edge function, through `pg_net`, to send it; a `pg_cron` job does the same
   for the reminders every Monday at 13:00 UTC (10:00 in Brasília).
2. The function reads `push_messages(event_id)`: one row per device of each person the event involves, their toggle
   on, never its author. It sends each row to FCM (Android), APNs (iOS, macOS) or the browser's push service (web).
3. The message carries a kind (`bill_created`, `payment_to_you`…), the group's name and the words that change
   ("Ana", "Jantar"), never a sentence: each platform words it in the device's language. Its link,
   `https://blitzsplit.com/n?k=<kind>&e=<event>&g=<group>&t=<target>`, is what a tap opens (`PushLink`).

Each app hands its token to the shared code (`SetPushDevice`), and `SyncCoordinator` registers it for the account
signed in, once online (`register_push_device`), and removes it whenever no account is signed in, once online
(`unregister_push_device`, which the server runs for whoever holds the token), so a sign out made offline ends the
notices as soon as the device is back.

| Platform | Token | Wording | Tap |
| --- | --- | --- | --- |
| Android | FCM, `PushSetup` | `BlitzSplitMessagingService`, the app's `push_*` strings, one channel per toggle | The link in `MainActivity`, like an invite's |
| iOS, macOS | APNs, `PushNotifications.swift` | The system, from `push.<kind>` in `Localizable.xcstrings` | `userInfo["link"]` to `handleDeepLink` |
| Web | Web Push, `src/push/webPush.ts` | `public/push-sw.js`, from the words the page leaves it | An open tab gets the link; else a new tab opens it |

## What stays off until it is set up

Nothing breaks without these: each platform simply gets no notifications.

### Server (Supabase, the `blitz-split-api` project)

1. Vault (SQL editor), so the database calls the function:

   ```sql
   select vault.create_secret('https://cfgnuyijsncgpgywoldf.supabase.co/functions/v1/push', 'push_function_url');
   select vault.create_secret('<a long random string>', 'push_webhook_secret');
   ```

2. Edge function secrets (Dashboard › Edge Functions › Secrets, or `supabase secrets set`):

   | Secret | Value |
   | --- | --- |
   | `PUSH_WEBHOOK_SECRET` | The same random string as Vault's `push_webhook_secret` |
   | `FCM_SERVICE_ACCOUNT` | Android: the Firebase project's service account key, the whole JSON (Project settings › Service accounts › Generate new private key) |
   | `APNS_KEY_ID`, `APNS_PRIVATE_KEY` | iOS and macOS: an APNs auth key (Apple Developer › Keys, with Apple Push Notifications service) and its id; the `.p8` file's text |
   | `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY` | Web: a VAPID key pair, `npx web-push generate-vapid-keys` |

   `APNS_TEAM_ID` falls back to `APPLE_TEAM_ID`, `APNS_TOPIC` to `com.quare.blitzsplit` and `VAPID_SUBJECT` to
   `https://blitzsplit.com`.

### Android

A Firebase project with an Android app `com.quare.blitzsplit`, whose `google-services.json` goes in
`androidApp/google-services.json` (git-ignored), for every build that should get notifications (a release build too).
The same file turns on Crashlytics ([crash-reporting.md](crash-reporting.md)).

Until it is there, the app never starts Firebase nor asks for the notification permission.

### iOS and macOS

The entitlements already ask for push (`aps-environment`, `com.apple.developer.aps-environment` in
`iosApp/project.yml`). Xcode's automatic signing adds Push Notifications to the App ID `com.quare.blitzsplit` the
first time it signs a build with them; if it doesn't, turn it on in Apple Developer › Identifiers. A build signed
for development registers its token for APNs' sandbox and any other for production, as its provisioning profile says
(a build without one, from the App Store or the simulator, by its configuration), which the server tells apart.

### Web

The VAPID public key in the web build's environment, the same as the server's `VAPID_PUBLIC_KEY`:

```bash
VITE_VAPID_PUBLIC_KEY=<public key> npm --prefix webApp run build
```

Without it the browser never subscribes. The service worker is `/push-sw.js`, so the site must serve the app's
`public/` folder at its root, and `/n?…` must serve the app, as `/g/…` does.

## Trying it

With the server's secrets set, sign in on a device, allow notifications, and have another account add a bill with
you in a shared group. The function's logs (Dashboard › Edge Functions › push) show how many were sent, skipped
(that platform isn't set up) or failed. `push_devices` lists the registered devices, without exposing them to the
apps.
