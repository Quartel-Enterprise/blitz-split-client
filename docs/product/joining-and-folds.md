# Joining and foldables

Invite links, joining, cameras, foldables and the iPhone Duo.

  - Where a link opens: a device with the app (Android, iPhone, iPad or Mac) opens a group's link (`/g/`) and a comment's (`/b/`) in the app,
    through Android App Links and Apple Universal Links, before any browser asks for them. Anywhere else the browser
    asks blitzsplit.com, which answers `/g/…`, `/b/…` and `/c/…` (a bill's link, which the apps leave to the browser)
    with a 302 to the same path and query of the web app, and the web app opens the invite, the comment or the bill
    (beta feedback 3, item 24). The web app is `WEB_APP_URL` of `blitz-split-site`'s `wrangler.jsonc`: the beta,
    https://blitz-split-beta.pages.dev, until app.blitzsplit.com serves it. `/.well-known/*` is never redirected.
    Android opens the links in the app only for a build whose signing key's SHA-256 is in blitzsplit.com's
    `assetlinks.json`, which lists the Play keys: a debug build, or a beta signed with a key not listed there, opens the
    browser, and so the web app (see [Release process](../release-process.md#outside-the-stores)).
  - A group's invite link is `https://blitzsplit.com/g/<code>` (borarachar.com will redirect to blitzsplit.com).
    Joining through it lands on the group's Expenses (3.3, `Route.GroupDetail` with `joinedAs`) with "You joined as
    <name>" over the bills and, while the account has no Google or Apple, "Protect your access", which opens the
    Profile. "Invite" (3.1) and "Who are you in the group?" (3.2) are one column on a phone and upright on a tablet; lying
    down, on a tablet lying down (a 920 dp / 900 pt container) and on a book (one half each, 24 dp off the hinge's
    band) the title, or the question with "I'm not on the list", on the left and the group with its buttons, or the
    names with "Join as Bruno" always in sight, on the right; an Android tabletop puts the invite's title and
    group on top and "Join group" and "Not now" below ("Who are you" is drawn like the book there). "I'm not on the list" is a bottom sheet on an Android phone, a 560 dp dialog on its tablets and foldables upright and in a
    book's right half, a sheet on iOS, a dialog on the web (a drawer on a phone). The web's "Who are you" heads the
    names with the invite ("Invite from Pierre", "Join Florianopolis Trip"), beside the names from 1024 px, and pins
    "Join as Bruno" at the bottom of a phone. The Mac opens the invite as one sheet over the window, "Who are you"
    pushed inside it. Joining with no profile on the device makes one from the name and
    color joined as. The invite and "Who are you" show the group's photo and the profile photo of each member someone
    joined as, over the color and initials (beta feedback 3, item 23; `get_invite` answers both `photo_url`s). "I'm not
    on the list" with a Google or Apple account signed in joins at once as a new member with the account's name and
    photo, no sheet, its button spinning while it goes (item 22); an anonymous or local only account, or a profile name
    shorter than 2 letters, opens "Join with another name" instead. That sheet takes any name of 2 letters or more,
    one already in the group too ("At least 2 letters.", item 29), and while the join is on its way its button spins,
    the field, Cancel and dismissing it are off, and it stays until the server answers (item 30,
    `JoinPickUiState.isJoiningAsNewMember`). A member someone else already joined as says "If it was you, ask <creator> to release the name in
    Members" ("If it was you, talk to whoever invited you" once the creator's account is gone).
  - The home's "Join with a link or QR code" opens the reader (`Route.JoinWithLink`) on every platform: the camera reads
    an invite's QR and keeps reading after a wrong one ("This QR code isn't a Blitz Split invite" for a few
    seconds), and "Have the link?" under it takes a pasted or typed link. Only links on blitzsplit.com and
    borarachar.com count as invites there, since a QR can hold any site's address. Android and iOS open it full
    screen, macOS as a sheet and the web as a dialog (a drawer on a phone), whose camera reads through the browser's
    BarcodeDetector. An invite no longer valid offers "Scan another QR code", which goes back to the reader; that reader
    ignores the QR of the invite that just failed, which may still be in front of the camera, so only another QR or
    a pasted link opens.
  - The reader never opens the camera on its own (beta feedback 2, item 8): opening it asks for nothing. In the camera's
    place, on its dark background, it shows the QR icon, "Scan the invite's QR code", "The camera only opens when you
    tap the button." and "Open camera"; only that tap asks for the camera (the system's prompt, or the browser's) and
    starts it, and "Have the link?" works the whole time without it. The choice is never remembered: every time the
    reader opens it starts at "Open camera" again, even with the camera already allowed (one tap, and the camera never
    turns on unasked). Refused, the camera's place says "No access to the camera" with "Open settings" ("Open Settings"
    on iOS, "Open System Settings" on the Mac, "Try again" on the web, which has no settings page to open). A device
    with no camera (a Mac mini, a computer without a webcam, as the system or the browser lists them, again when a
    camera is plugged in or out on Apple and the web) never offers "Open camera" and says "This device has no camera.
    Paste the link below." ("This Mac has no camera" on the Mac); a browser that can't read QR codes (Safari, Firefox)
    keeps its own line, with no button either. The prompt takes the camera's place wherever the layout puts it, so
    foldables, the iPhone Duo and the Mac's sheet need nothing of their own. The ViewModel holds the stage
    (`CameraStage`: prompt, no camera, viewfinder, denied, failed), and the platforms only ask when it says so
    (`RequestCameraAccess`).
  - Android's "Take photo" asks for the camera first. Once it is refused for good, a snackbar "Allow the camera in
    Settings" offers "Settings", which opens the app's settings page.
  - The web's "Take photo" (beta feedback 2): wherever the web takes a photo from a file (Your profile, Edit profile,
    New group, Edit group, Bill's Attachments, Scan receipt's choice) it also offers the browser's camera, a desktop's
    webcam too, but only while the browser lists a camera (`useCameras`). One dialog (`CameraCaptureDialog`, a
    drawer on a phone) shows it live, the selfie camera mirrored and the photo never, with the shutter, "Retake"
    and "Use photo", "Switch camera" with more than one (front and back where the cameras say which they are, else
    one camera after the other) and the flashlight where the browser has it; "Use photo" hands the JPEG on as a file
    picked would be, so a profile's or a group's goes through "Adjust photo". A camera refused says so ("No access
    to the camera") with "Try again" and "Choose a file", and closing stops it, as does the photo taken.
  - Foldables and the iPhone Duo follow the fold the device reports, never the window alone (decision 8: Android's
    `FoldingFeature`, read as `FormFactor`; iOS 27.1's division region, `FoldDivision`), so a Galaxy Z Fold held as a
    book is a book: two halves meet on the hinge band (24 dp on Android) and a dialog the book draws opens in its
    right half with the other half under the scrim (`FoldDialog`), as the recovery code does on a book and on a
    tabletop lying down (there above its hinge, which runs across the right half too). First access splits there
    too: Welcome's pitch, Your profile's avatar and Recover's accounts before the fold, what to tap after it, on
    a book, a tabletop lying down and an iPhone Duo open; the Duo closed drops Welcome's benefits. Every Apple
    layout rule is the window's size (`ScreenLayout`), never the idiom, so an iPhone Duo open gets an iPad's list and
    detail and
    opens a key and a bill as sheets; its copy still says "iPhone". How each screen picks its layout:
    [docs/architecture/responsive-layouts.md](../architecture/responsive-layouts.md).
  - The iPhone Duo (iOS 27.1): closed, the system's vertical bar on the camera's edge holds Back, the screen's glass
    buttons, the avatar and "Split a bill" (only its +) over the tabs
    (`prefersVerticalBar()`), and the group's bottom bar moves into it; open lying down the system keeps that bar.
    Open lying down, every two-column screen
    splits on the fold with nothing in its band (Groups' and Contacts' lists end at it, Balances, Statistics with
    the total and categories before it and By person after it), and a form sheet opens in the leading half
    (`foldSheetPlacement()`). Open upright, as a notebook, Invite people (QR and link on top), Invite (the group on top),
    the group (the header and "Your balance" on top, the tabs and what they hold below, on every tab), Record payment (who and how much on top, how and when below), Statistics (the total and Over time on top),
    Split a bill (description and amount on top) and Split (the summary and modes on top, the people below) split on
    the fold (`FoldHalves`); everything else is an iPad's upright. Pix and Bitcoin keys stay a sheet in the leading
    half lying down, not the boards' two columns.
