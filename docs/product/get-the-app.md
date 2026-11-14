# Get the app

The web and the Mac app invite people to the phone app (canvas page 14, "Baixe o app").

  - Only the web on a computer or a tablet and the Mac app invite. Android and iOS never show any of it; they only
    record, on each sync, that the account signed in there (`record_platform_seen`, `platforms_seen` on the server:
    the platform and the dates, never a device id). The web and the Mac record themselves too.
  - Only once a store lists the app (`stores.properties`, which Gradle's `StoreConfig` and the web's Vite read). The web
    names only the stores that have it ("For now the app is only on Google Play, for Android."); with none, nothing
    shows. The Mac comes from the Mac App Store, so it only ever names the iPhone and the App Store (guideline 2.3.10):
    its QR code opens the App Store's listing straight, and without an App Store id the Mac shows nothing.
  - The web's QR code and link are `blitzsplit.com/app-download?lang=<the app's language>` (the link's preview and
    the page it opens speak it), which the site sends to Google Play on Android, to the
    App Store on an iPhone or iPad and, on anything else, to a page with the stores that list the app
    (`blitz-split-site`). The QR codes are encoded in Kotlin (`EncodeQrCode`), so they show offline too.
  - Home's card, "Take Blitz Split on your phone" (14.1, `HomeUiState.showsAppInviteCard`): after the first bill (some
    group holds one), never on the first use, and one card at a time: the recovery code's card or sheet and the
    notification card come first, and the invite waits for them. It has the QR code (a click opens the code big), what
    only the phone gives ("Add bills right away, wherever you are", "Payment notices"; reading a receipt is left out,
    since the web and the Mac read them too), the link, "I downloaded it", ✕ ("Don't show again") in its corner, and
    without Google or Apple "Take your groups to your phone", which says the phone app starts empty, with "Connect
    account" (Profile). No "Not now". The web draws it from 768 px up; the Mac in both window widths.
  - The code, "Blitz Split on your phone" (14.2, `Route.AppDownload`, `AppDownloadViewModel`): a dialog on the web (a
    drawer under 768 px), a sheet on the Mac (on Settings' window when its iPhone row opened it). The QR code big, the
    link with its copy button ("Link copied"), what only the phone gives, the account's note ("Sign in to the app with
    the same Google account…", or that without an account the phone app starts empty), Close ("OK" on the Mac, as the
    canvas draws its sheet), and "I downloaded it"
    while the invite stands. Once the account uses a phone app: "You've used the app on your phone since Oct 12. This
    code always stays here in Profile, for when you change phones." (14.2b), with Close only.
  - The invite ends for good, with no time to come back, on "I downloaded it" (the card or the code) or ✕ (the card or
    Profile's row), and once a sync finds the account signed in on Android or iPhone. Seen with the card on screen, the
    card goes and "We saw you on your phone." shows once (14.1c, `HomeUiAction.ShowUsedOnPhone`).
  - Where it is kept: the dismissal is the account's (`app_invites` on the server, final once there), so it holds in
    every browser and Mac; without Google or Apple it stays in this browser or this Mac (the anonymous user's), and
    connecting an account keeps it, since the user stays the same. Home and Profile read the same state.
  - Profile's "App on your phone" (14.4, the Mac's Settings › iPhone, ⌘,): always right under the profile, the way to
    download later. While the invite stands it is highlighted, with the app's icon, "See code" (the Mac's "Show
    code…") as the main button and ✕; after it, plain for good ("Code to download the app on your phone.", 14.4b); once
    the account uses a phone app, "You already use the app on your phone." (the Mac: "on iPhone" when that is where,
    14.4c). Its ✕ takes Home's card away too, and the card's ✕ the highlight.
  - A phone's browser (14.3): no card nor QR code. Home gets our banner, "Blitz Split in the app", whose "Open in the
    app" goes to `blitzsplit.com/app-download`, and whose ✕ hides it for good on that device (a device setting, never the account's,
    since the banner belongs to the phone's browser). Safari on an iPhone gets the App Store's own banner instead, from the
    `apple-itunes-app` tag the web's build adds once `stores.properties` has an App Store id, so ours stays hidden
    there. Profile's row shows no code: "Download the app" goes to the same link, "Open in the app" once the account
    uses it.
  - Offline, the card and the code still work; only the store, on the phone, needs the internet.
