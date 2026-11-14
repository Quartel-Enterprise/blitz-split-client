# Profile

The profile, its photo, appearance, in-app update and Adjust photo.

  - "Profile" (4.1, `Route.Profile`) opens from the avatar of each home tab (Home, Groups…). It is one column on a
    phone and upright (560 dp on an Android tablet or foldable, 520 pt with an iPad's room); lying down who you are,
    your account and "Privacy and about" go on the left (330 dp / 300 pt on a phone, 380 dp in a tablet's 1016 dp container, 360 pt on an iPad, as the
    glass panel of a Settings split) and the keys and preferences on the right, each column scrolling on its own.
    "Sign out" (or "Erase profile and sign out") comes right after "Account and sync" on every platform and layout, one
    column included (Pierre's call, beta feedback 16), and "Delete account" stays the last thing of its column, only
    the legal footer under it; on a book (a tabletop lying down alike) a half each, the 72 dp
    gap centered on the hinge, as the keys do, and its dialogs (Sign out,
    "Erase profile and sign out") and Delete account open in the right half. Edit profile is never a screen of its
    own, since a photo, a name and a notice are too little for one (Pierre's call, beta feedback): Android opens it as
    Registrar pagamento (`FormPresentation`), a bottom sheet on a phone with "Edit profile" and Save on top (the photo
    left and the name right lying down), a 560 dp dialog with Cancel and Save anywhere wider (a book's right half),
    and full screen only on a tabletop, as the screen always drew it there (the photo and the name side by side
    lying down); iOS opens it as a sheet with
    Cancel (✕) and Save (✓), at half height on an iPhone, growing to the whole screen (two columns lying down: the
    photo and its color, then the name), and a 540 pt form sheet with an iPad's room. The keyboard lifts the sheet with
    the name field, and the photo menu and "Adjust photo" open over it. Dismissing it (back, a drag down, ✕, Cancel)
    leaves without saving, as leaving the screen did; a drag down, ✕ and Cancel wait while it saves. The Mac and the web never open it: they
    edit the name and photo inline, in Settings and in the profile page's "Your profile". The Mac's profile is
    Settings (⌘,), the app's Settings window, never pushed: whatever opens the profile (any tab's avatar, "Protect your access", a browser sign in started there coming back) opens it instead; its sidebar lists the sections and
    scrolls the form to the one picked, on every click (hidden under 700 pt; its button at the leading edge of the
    detail's toolbar, right of the divider, as in the main window, Pierre's beta feedback; the form 720 pt at most,
    centred, in a wider window), a key (also from its row's right-click, "Edit…") and Delete account open as sheets
    on it, leaving the main window's own sheet as it was and its window unclickable until they close, and it closes
    once no profile is left. The web's is a page.
  - "Share profile" (Pierre's call, beta feedback: it took the place of the top bar's "i", and "Who sees your profile",
    its row and its dialog, are gone): the top bar's share button shares the account's profile link,
    `https://blitzsplit.com/u/<code>`, with "Here's my profile on Blitz Split: <link>" ("Esse é meu perfil no Bora
    Rachar" in Portuguese). The link shared or copied ends in `?lang=<the app's language>` (`LinkLanguage`): a profile
    has no currency to pick its link's preview language by, so blitzsplit.com writes the preview in that one (any of
    the app's eleven languages), and the site's own pages read the same parameter (the web app, once it serves
    `/u/<code>` itself, doesn't read it yet). The language is the one the app is speaking (`ObserveSpokenLanguage`):
    the pick, or the first of the device's languages the app has, English otherwise. Android and iOS open the system
    share, whose own Copy covers copying; the Mac's toolbar and
    the web's header (the page's heading from 768 px) open a menu with "Share profile" (the Mac's sharing picker,
    the browser's share where there is one, else a copy) and "Copy link" ("Link copied"). The link is made on the
    device the moment the account exists (its first sync, `ProfileUiState.shareLink`), so it is there offline right
    after a sign in on a new device once the account's profile came down, and it never changes; a profile no account
    owns yet (local only, before the first connection) has no link, and the button hides. Opening someone's link:
    [balances-and-contacts.md](balances-and-contacts.md) (the profile link's page) and
    [joining-and-folds.md](joining-and-folds.md) (where it opens).
  - "Sign out" and "Delete account" are two buttons for every account the server keeps, an anonymous one too, as for
    Google or Apple (Pierre's call, beta feedback 17): signing out an anonymous account asks "Sign out?" and for the
    recovery code to be kept, since it is the way back, and Delete account deletes it on the server (the
    `delete-account` function takes any signed in user). Only a profile nothing brings back (no Google or Apple and no
    recovery code yet, `ProfileUiState.erasesProfileOnSignOut`) reads "Erase profile and sign out", whose dialog says
    it is gone for good; one the server never saw (`isLocalOnly`) has no Delete account either, while an anonymous
    account whose code isn't made yet keeps it.
  - "Account and sync"'s Google or Apple row only says which account it is: the provider's own logo (Google's
    four-color "G", Apple's in the text's color) and the account's whole email, unmasked so the person really sees
    which account it is, with no "Connected" and no "Google"/"Apple" title (Pierre's call, testers' feedback,
    November 2026). An ⓘ button next to the email explains that only the person sees it, in each platform's way
    (Android's dialog, a popover on Apple platforms and the web, `ProfileDialog.EMAIL_INFO`); without an email the
    row falls back to the provider's name and has no ⓘ. Without Google or Apple the row keeps "No account" and its
    line, under an anonymous-person icon (a person with a slash). On Android its "Connect" button drops under the
    texts where the row is narrow for its font (under 300 dp times the font scale), so the line beside it never breaks inside a word. The row opens nothing and nothing disconnects the
    Google or Apple account from the user (Pierre's call, beta feedback). Who wants it gone signs out or deletes the
    account. The server has no `unlink-identity` function any more.
  - Delete account lists, truthfully to the server's `delete-account` function and its migrations, "What gets
    deleted" (the profile with its photo, the Pix and Bitcoin keys, the link to Google or Apple, an anonymous
    account's recovery code, the copy on this device) and then "What stays in your groups" (testers' feedback,
    November 2026): the groups with their bills and payments, for the other members (the server keeps them, even one
    nobody else is in); the person's name in each group, as a member no longer linked to any account
    (`group_members.user_id` is set to null); and their comments and the photos they added, under the name they had
    (`author_user_id` and `added_by` set to null). The server also drops, without the screen naming them, the
    notification settings, pins, hidden contacts, likes on comments and push devices (`on delete cascade`).
  - The profile ends in a discreet, centered legal footer, as Bible Planner's (Pierre's call, beta feedback 18):
    "Privacy Policy • Terms of Service", two links to the legal texts (below), and "Version X" under them
    in small, secondary text. It closes the column that ends with Delete account: Android's single column and the
    right column lying down or on a book, iOS's single column and its left column lying down, the Mac's Settings form,
    and the web's page under both columns. The version is the whole versionName, a beta's suffix too
    ("Version 3.0.0-beta_7", beta feedback 19): Android and the web read it as built, and the Apple apps from the
    Info.plist's `BlitzSplitVersionName`, since Apple refuses a suffix in the version string
    ([release-process.md](../release-process.md#versions)); the Apple betas only carry the suffix once the release
    workflow hands it to their builds (#239, which Pierre merges, since a workflow change needs the `workflow` scope).
  - "Privacy Policy" and "Terms of Service" (`Route.LegalDocument`, `:core:legal`, `:feature:legal`) are drawn by
    the app itself on Android, iOS and the Mac, with each platform's own components, and never copied into the apps:
    the text is written once, as Markdown, in `blitz-split-site` (`public/legal/<page>.<language>.md`, its
    `docs/legal.md`), which the site's own pages are generated from and the apps download
    (`https://blitzsplit.com/legal/…`). The web keeps opening https://blitzsplit.com/privacy and
    https://blitzsplit.com/terms in another tab (`LegalLinks`), a browser's own way (Pierre's call).
    - The language is Profile › Language's, or the device's own while it follows the system, so a device in Turkish
      reads them in Turkish though the app is in English; the site writes them in all eleven of the app's languages
      (blitz-split-site#29), and a language it doesn't write them in reads the English text.
    - The device keeps the last text it downloaded (`legal_document`, per page and language) and shows it at once,
      offline too, while a newer one downloads and takes its place. A device that never downloaded it draws the
      shimmer, then "Couldn't load this page" with "Try again" when the download fails (Pierre's call: no copy ships
      inside the app).
    - Both texts download at launch, once the first screen is drawn, and again when Profile › Language changes, so
      the footer opens them offline too (not on the web, which opens the site's pages). The site is asked at most once
      a day per text, at launch or on the screen, with the saved text's ETag: an unchanged one answers 304 with no
      body, so a text only downloads again when the site changed it.
    - Android and iOS push it over Profile, the page's name in the bar; the Mac opens it as a sheet on Settings with
      Done. The text is one column, 640 dp / pt at most, centred in a wider window: "Updated November 5, 2026" in the
      device's date format, the summary in a tinted card, each section's number beside its title, and a table as
      cards, one per row, its first cell the title and the others under their column's name, as the site's phone
      layout.
    - A link to the other text, or to a section of it, opens that text over this one, scrolled to the section; a
      link to a text already open under this one goes back to it (in its place, scrolled to the section, when the link
      names another one), so the two never pile up, and a link to the section this very screen opened at scrolls
      back to it. A legal page's full address (`https://blitzsplit.com/privacy`) opens in the app like its short one.
      Any other web address, or a mail one, opens outside the app.
  - On the web and the Mac, "App on your phone" (Settings › iPhone on the Mac) sits right under the profile once a
    store lists the app, highlighted while the invite to the phone app stands ([get-the-app.md](get-the-app.md)).
  - The profile's avatar (Profile's on Android and iOS, the inline form's on the web and the Mac, never Edit profile's) opens the photo big, as Bible Planner's viewer (Pierre's call, beta feedback 16, `Route.ProfilePhoto`,
    `ProfilePhotoViewModel`), the initial on its color too when there is no photo: 200 pt/dp/px (260 once both sides
    have 600, so a phone lying down keeps 200, scrolling where it still doesn't fit) over the dimmed, blurred
    profile, with its shortcuts under it, the account's photo (its logo, "Google" or "Apple", while it isn't the one
    shown), "Camera" where there is one, "Gallery" and "Remove" (with a photo). Each saves the profile's photo at
    once, offline too, with no Save, and the viewer stays open showing the new one; a photo picked opens "Adjust photo" over it first, and is still saved when the viewer closes while it is prepared. ✕, a tap outside, back and
    Esc close it. Android draws it as a dialog over the whole window (a book's right half), iOS covers the screen,
    the Mac opens it as a sheet on Settings and the web as a dialog over the profile. The group's own photo opens the same
    way from its header ([The group screen](group-and-bills.md)).
  - In-app update, as Bible Planner's (`:core:in_app_update`, `:feature:in_app_update`): each time the app comes to
    the front (`AppViewModel`, once the first screen is in place) a newer version is offered at most once an hour,
    kept per device (`lastUpdatePromptAt` in `device_settings`), and the offer waits until a home tab or a group's
    screen is on top, never over a form, a sheet or a cover. Android hands it to Google Play's flexible update,
    whose download shows "Downloading update… 42%" at the bottom of every screen (taps still reach what is under
    it) and installs (the app restarts) as soon as it ends, one already downloaded installing again after 15
    minutes; iOS and macOS read the App Store's version by bundle id (the legacy Blitz Split's 2.4.0 until this app is published; the iPhone
    and Mac apps are one purchase with one listing, whose version both read) and open "Update available"
    ("Version 3.1.0", "Open in the App Store", "Not now", only its buttons and Esc closing it) over the app, a sheet as
    tall as its content on an iPhone, a form sheet with an iPad's room, a sheet on the Mac (on Settings when its check
    opened it). Profile's "Privacy and about" ("Export my data") ends in "Check for updates" with
    "Version <versionName>" under it (Pierre's call; the Mac's row keeps "Version <versionName>" with the button), as
    Bible Planner's keeps it next to its footer's version, "Checking…" while it
    asks, which offers the update even within the hour, or says "You already have the latest version.", "The update
    is already downloading." (Android) or that it couldn't check. The web always serves the latest, so it never checks: its
    version is only the footer's.
  - Profile's "Privacy and about" holds "Share usage data" ("Screens and taps, to improve the app. Never names or
    amounts."), a switch right under "Export my data" (Pierre's call, #184): on by default, per device like the theme,
    it turns Firebase Analytics off on this device, the user id included. Nothing asks about it first: Analytics rests
    on legitimate interest, and Welcome's line tells about the Privacy Policy ([general.md](general.md),
    [docs/analytics](../analytics/README.md#consent)).
  - Profile › Preferences › "Notifications" ("5 active", the rows whose Push is on; "0 active" while its "Push notifications" switch is off on this device) opens Notifications: a row per
    kind of Activity and the weekly reminder, each with a Push and an In app toggle. Activity's bar opens the same
    screen; see [activity-and-push.md](activity-and-push.md) for the rows, the defaults and how each platform draws
    it (the Mac and the web keep it inside the profile, as before).
  - Android's Profile › Preferences › "Appearance" is a row ("System", "System · dynamic colors") opening a sheet
    as Bible Planner's theme picker (`ProfileDialog.APPEARANCE`): a card per theme with a preview of the app in it
    (System half light, half dark, in the wallpaper's colors while they are on), then "Dynamic colors" with an ⓘ
    explaining it over the sheet (`ProfileDialog.DYNAMIC_COLORS_INFO`, "Got it" back to the sheet); a bottom sheet
    on a phone, a 560 dp dialog anywhere wider, a book's right half. The Apple apps and the web keep their own picker.
  - Profile › Preferences › "Language", as Bible Planner's (Pierre's call): "System" first, the default, which follows
    the device's language (English for one the app doesn't have), then each language in its own name, the way its
    speakers look for it, with its name in the app's language as a smaller line under it (Pierre's call, beta feedback,
    back from only translated names): "English", "Português (Brasil)", "Español", "Français", "Deutsch", "Italiano",
    "Polski", "Nederlands", "日本語", "Türkçe" and "Bahasa Indonesia", the same in every language, under them "Portuguese
    (Brazil)", "Spanish"… in English and "Inglês", "Espanhol"… in Portuguese, the smaller line left out where both read
    the same (Italian's "Italiano" in Portuguese, Portuguese's own row). A region is spelled out, never a code:
    "Português (Brasil)", "Portuguese (Brazil)". "Spanish" is Spain's Spanish (es-ES) on a device whose region is Spain,
    the neutral one anywhere else, so it carries no region. Each own name is marked with its language, so a screen
    reader reads "日本語" as Japanese and the font draws it as Japan's. Kept per device and
    never synced, still there after a sign out, and kept by each platform's own per-app language
    (`AppLanguage`, `AppLanguageLocalDataSource`), so the system applies it before the first frame: Android 13 and later's
    LocaleManager, which the system's app language settings also change (the languages the build generates its locale config from), the app's
    own copy before 13; the Apple apps' AppleLanguages, which iOS's Settings › Bora Rachar › Language also changes; the
    browser's storage on the web. Android's row ("System", "Português (Brasil)") opens the picker as Bible Planner's
    (`ProfileDialog.LANGUAGE`): a row per option with its chip ("EN", "PT", "ES", a phone for System, "Device language"
    under it) and a radio. With eleven languages a bottom sheet filled a phone's screen (Pierre's feedback, November
    2026), so a phone, upright or lying down, opens it as a screen of its own, "Language" in its bar with back, and
    anywhere wider it stays a 560 dp dialog, a book's right half. Both start with "Search languages", which keeps the
    rows any of whose names (its own, the app language's, System's "Device language") holds what is typed, or whose
    chip's code it is, ignoring case and accents ("alemao", "brasil", "pt"; `ProfileUiState.findLanguageOptions`),
    "No language matches “…”" when none does; each opening starts from the whole list. A pick
    closes it and the app draws again in it (Android 13 and later on their own, the activity opening again before,
    where the notifications and their channels' names follow from the app's next start). The region the app reads
    (the brand badge's currency, a new group's) stays the device's, whatever the language. The Apple apps pick it in
    Preferences' menu, Settings on the Mac, and every window redraws in it at once, the Mac's menu bar too
    (`AppLocale`, and every `String(localized:)` reading `bundle: .appLanguage`), a window with a sheet open once it
    closes, so a form in it keeps what was typed; what the system formats itself (dates, amounts) follows at the next
    launch. The web picks it in the Appearance card's segments and
    reloads the page in it, back on the profile. Neither has the search: the Apple menu is the system's, scrolling
    where it doesn't fit, and the web's segments sit in the page, never over it.
  - "Dynamic colors" (Material You) shows in Appearance only where the platform draws a scheme from the wallpaper,
    Android 12 and later (`DynamicColorSupport`, `ProfileUiState.showsDynamicColors`): off by default, it swaps the
    brand's Material scheme for the wallpaper's in light and dark alike, while the groups', avatars' and categories'
    own colors stay the brand's. Kept per device and never synced (`usesDynamicColors` in `device_settings`, so it
    outlives a sign out); a choice restored onto a device that can't draw it reads as off. The Apple apps and the web
    have no such scheme and show nothing.
  - "Edit profile": the avatar's pencil opens the photo menu (take a photo where there is a camera, choose from the
    gallery, use the Google or Apple account's photo while it isn't the one shown, remove the photo), and the avatar
    color picker only shows while there is no photo (`EditProfileUiState.showsAvatarColors`).
  - "Adjust photo" (Bible Planner's editor, Pierre's call): every photo taken or picked for a profile (Your profile,
    Edit profile, the Mac's Settings) or a group (New group, Edit group) opens it before it becomes the avatar,
    never the Google or Apple account's photo. Dark, full screen on Android, iOS and a phone's browser, a 560×720
    sheet on the Mac and a 560 px dialog on the web from the tablet up. The photo moves under a fixed frame, a circle
    for a profile and a rounded square for a group (the group's one shape, its corners 8% of the side, see [General rules](general.md)): drag,
    pinch around the fingers (the trackpad's too; on the web a scroll moves it, Ctrl and the wheel zoom) and a 1× to 3× slider zoom, "Rotate" turns it 90°
    clockwise, "Horizontal" and "Vertical" mirror it; the photo always covers the frame. "Done" makes the 512x512
    JPEG of what the frame holds; ✕, Cancel or back leave the photo shown as it was. The framing is the shared
    `PhotoCrop` (fractions of the frame, so a turned device keeps it), the photo the upright, at most 1600 px JPEG of
    `LoadCropPhoto`, and each platform's canvas draws the result from `CropTransform`. On a fold the photo and the
    controls take a half each, never the hinge: side by side on a book and an iPhone Duo open lying down, the photo
    on top on a tabletop and an iPhone Duo open upright. New group's Create and Your profile's Continue wait while it
    is open.
  - Member names have 2 to 40 letters, like a profile's, since joining without a profile makes one from it.
  - Closing Edit profile with unsaved input asks "Discard changes?" first, on every way out ([general.md](general.md)).
  - "Keys to receive payments" follows [the Pix rule](general.md): with no Pix signal it shows the Bitcoin keys and two
    rows, "Add a Bitcoin key" and "Add a Pix key", in place of the "Add a Pix or Bitcoin key" menu, so nobody is locked
    out of Pix (`ProfileUiState.showsPix`). Each key's row starts with its method's tile on every platform: Pix's own
    mark (Material's Pix glyph) on the blue tile, Lightning's bolt or on-chain's link on Bitcoin's orange one.
  - One Pix key per type, random keys aside (Pierre's call, feedback of November 14, 2026): a person holds at most one
    CPF/CNPJ, one phone and one email key (`PaymentKeyType.isOnePerPerson`), while random keys and Bitcoin keys may
    repeat. In the key form a type's tab that already holds a saved key shows that key, "Edit Pix key" with "Delete
    key", and "You can only have one Pix key of this type, so this is the one you saved. Change it here if you want."
    above the field (`PaymentKeyUiState.isShowingHeldKey`), so saving changes it rather than adding a second one;
    going back to the tab of the key opened (or of no saved key) returns to it. A new key opens on the first type not
    held yet, and a key typed or pasted of a held type goes to that saved key the same way. The shared
    `PaymentKeyViewModel` decides it, so the three UIs follow, and `SavePaymentKey` saves a new key of a held type
    over the held one, whoever calls it. Each key keeps its "Default Pix key" switch as it was left while the tabs
    move between keys. Duplicates saved before the rule stay as they are: each
    still opens and saves on its own. The server has no constraint on it (`payment_keys` only checks each value's
    shape).
  - A key's "Default Pix key" (or "Default Bitcoin key") switch is locked on for the method's default key and for its
    first key, since a default only gives way to another key made default (`PaymentKeyUiState.isDefaultLocked`).
    Locked, it still reads as on: Android keeps the switch's "on" colors, faded, instead of Material's disabled gray
    one, which read as off in the dark theme (testers' feedback, November 2026); iOS and the web dim the "on" switch.
