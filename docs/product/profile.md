# Profile

The profile, its photo, appearance, in-app update and Adjust photo.

  - "Profile" (4.1, `Route.Profile`) opens from the avatar of each home tab (Home, Groups…). It is one column on a
    phone and upright (560 dp on an Android tablet or foldable, 520 pt with an iPad's room); lying down who you are,
    your account and "Privacy and about" go on the left (330 dp / 300 pt on a phone, 380 dp in a tablet's 1016 dp container, 360 pt on an iPad, as the
    glass panel of a Settings split) and the keys and preferences on the right, each column scrolling on its own.
    "Sign out" (or "Erase profile and sign out") comes right after "Account and sync" on every platform and layout, one
    column included (Pierre's call, beta feedback 16), and "Delete account" stays the last thing of its column; on a book (a tabletop lying down alike) a half each, the 72 dp
    gap centered on the hinge, as Edit profile and the keys do, and its dialogs ("Who sees your profile", Sign out,
    Disconnect, "Erase profile and sign out") and Delete account open in the right half. Edit profile is full screen on
    Android at every size; iOS pushes it on an iPhone (two columns lying down: the photo and its color, then the
    name) and opens it as a 540 pt form sheet with an iPad's room, with Cancel and Save (✓). The Mac's profile is
    Settings (⌘,), the app's Settings window, never pushed: whatever opens the profile (any tab's avatar, "Protect your access", a browser sign in started there coming back) opens it instead; its sidebar lists the sections and
    scrolls the form to the one picked, on every click (hidden under 700 pt), a key and Delete account open as sheets
    on it, leaving the main window's own sheet as it was and its window unclickable until they close, and it closes
    once no profile is left. The web's is a page.
  - On the web and the Mac, "App on your phone" (Settings › iPhone on the Mac) sits right under the profile once a
    store lists the app, highlighted while the invite to the phone app stands ([get-the-app.md](get-the-app.md)).
  - The profile's avatar (Profile's on Android and iOS, the inline form's on the web and the Mac, never Edit profile's) opens the photo big, as Bible Planner's viewer (Pierre's call, beta feedback 16, `Route.ProfilePhoto`,
    `ProfilePhotoViewModel`), the initial on its color too when there is no photo: 200 pt/dp/px (260 once both sides
    have 600, so a phone lying down keeps 200, scrolling where it still doesn't fit) over the dimmed, blurred
    profile, with its shortcuts under it, the account's photo (its logo, "Google" or "Apple", while it isn't the one
    shown), "Camera" where there is one, "Gallery" and "Remove" (with a photo). Each saves the profile's photo at
    once, offline too, with no Save, and the viewer stays open showing the new one; a photo picked opens "Adjust photo" over it first, and is still saved when the viewer closes while it is prepared. ✕, a tap outside, back and
    Esc close it. Android draws it as a dialog over the whole window (a book's right half), iOS covers the screen,
    the Mac opens it as a sheet on Settings and the web as a dialog over the profile.
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
    opened it). Profile's "Privacy and about" ends in "Check for updates" with "Version <versionName>" under it in the
    place of "Version" (Pierre's call; the Mac's row keeps "Version <versionName>" with the button), "Checking…" while it
    asks, which offers the update even within the hour, or says "You already have the latest version.", "The update
    is already downloading." (Android) or that it couldn't check. The web always serves the latest, so it never checks and
    keeps "Version".
  - Profile › Preferences › "Notifications" ("5 active", the rows whose Push is on) opens Notifications: a row per
    kind of Activity and the weekly reminder, each with a Push and an In app toggle. Activity's bar opens the same
    screen; see [activity-and-push.md](activity-and-push.md) for the rows, the defaults and how each platform draws
    it (the Mac and the web keep it inside the profile, as before).
  - Android's Profile › Preferences › "Appearance" is a row ("System", "System · dynamic colors") opening a sheet
    as Bible Planner's theme picker (`ProfileDialog.APPEARANCE`): a card per theme with a preview of the app in it
    (System half light, half dark, in the wallpaper's colors while they are on), then "Dynamic colors" with an ⓘ
    explaining it over the sheet (`ProfileDialog.DYNAMIC_COLORS_INFO`, "Got it" back to the sheet); a bottom sheet
    on a phone, a 560 dp dialog anywhere wider, a book's right half. The Apple apps and the web keep their own picker.
  - Profile › Preferences › "Language", as Bible Planner's (Pierre's call): "System" first, the default, which follows
    the device's language (English for one the app doesn't have), then each language in its own name, "English",
    "Português" and "Español", whatever the app's current language, so whoever can't read it still finds theirs;
    "Español" is Spain's Spanish (es-ES) on a device whose region is Spain, the neutral one anywhere else. Kept per device and
    never synced, still there after a sign out, and kept by each platform's own per-app language
    (`AppLanguage`, `AppLanguageStore`), so the system applies it before the first frame: Android 13 and later's
    LocaleManager, which the system's app language settings also change (the languages the build generates its locale config from), the app's
    own copy before 13; the Apple apps' AppleLanguages, which iOS's Settings › Bora Rachar › Language also changes; the
    browser's storage on the web. Android's row ("System", "Português") opens a sheet as Bible Planner's
    (`ProfileDialog.LANGUAGE`): a row per option with its chip ("EN", "PT", "ES", a phone for System, "Device language"
    under it) and a radio, a bottom sheet on a phone, a 560 dp dialog anywhere wider, a book's right half; a pick
    closes it and the app draws again in it (Android 13 and later on their own, the activity opening again before).
    The Apple apps pick it in Preferences' menu, Settings on the Mac, and every window redraws in it at once
    (`AppLocale`, and every `String(localized:)` reading `bundle: .appLanguage`); what the system formats itself
    (dates, amounts) and Spain's own words in SwiftUI's literals follow at the next launch. The web picks it in
    the Appearance card's segments and reloads the page in it.
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
    for a profile and a rounded square for a group (the group's one shape off Android, its badge's 32% corners): drag,
    pinch around the fingers (the trackpad's too; on the web a scroll moves it, Ctrl and the wheel zoom) and a 1× to 3× slider zoom, "Rotate" turns it 90°
    clockwise, "Horizontal" and "Vertical" mirror it; the photo always covers the frame. "Done" makes the 512x512
    JPEG of what the frame holds; ✕, Cancel or back leave the photo shown as it was. The framing is the shared
    `PhotoCrop` (fractions of the frame, so a turned device keeps it), the photo the upright, at most 1600 px JPEG of
    `LoadCropPhoto`, and each platform's canvas draws the result from `CropTransform`. On a fold the photo and the
    controls take a half each, never the hinge: side by side on a book and an iPhone Duo open lying down, the photo
    on top on a tabletop and an iPhone Duo open upright. New group's Create and Your profile's Continue wait while it
    is open.
  - Member names have 2 to 40 letters, like a profile's, since joining without a profile makes one from it.
