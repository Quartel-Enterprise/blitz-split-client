---
name: update-readme-screenshots
description: "Refresh the image that opens README.md (docs/images/readme-hero-{dark,light}.webp): the Lisbon trip's group screen, Expenses tab, captured from the real Android, iPhone, Mac and web apps, dark and light, by scripts/readme-hero/generate.sh. Decides first whether the change shows in that image at all, then recaptures, checks the result by eye and commits the two images. Run by create-pr when a branch visibly changes a screen the README shows, and by whatsapp-feedback once at the end; use it too whenever someone asks to update, redo or refresh the README's images or screenshots (\"atualiza as imagens do README\", \"o print do README está velho\")."
---

# Update the README's screenshots

`README.md` opens with one image, in a dark and a light version that GitHub picks by the reader's theme:
`docs/images/readme-hero-dark.webp` and `docs/images/readme-hero-light.webp`. It shows the same group of the store
screenshots' sample trip, Lisbon trip, on its Expenses tab, on the four native UIs side by side: an Android phone, an
iPhone, the Mac app and the web app in Chrome. Every screen in it is a real capture of the app, never a mockup, so when
a branch changes how that screen looks, the image is out of date until someone captures it again.

[`scripts/readme-hero/generate.sh`](../../../scripts/readme-hero/generate.sh) does the whole capture, and
[The README's image](../../../docs/store-listing-screenshots.md#the-readmes-image) in
`docs/store-listing-screenshots.md` explains how each platform is captured. This skill decides when to run it, runs it,
and checks what it drew. Read that section the first time.

## 1. Decide whether the image needs it

Each run redraws the dates of the sample trip ("Today", "Yesterday" and the days before are counted back from the day
of the capture) and recompresses the images, so a run always changes the two files, even when no pixel of the app
moved. Run it only when the branch changes something the image shows; otherwise every pull request would carry a new
image nobody can tell from the old one.

Look at the image first (convert it to read it: `sips -s format png docs/images/readme-hero-light.webp --out
<scratchpad>/hero.png`, then Read). It shows, in dark and light:

- **The group screen's Expenses tab, on all four:** the top bar and its actions (Statistics, Invite, the menu), the
  group's photo, name, member count and total, the faces, the currency chips (USD, EUR), the Your balance card with
  Settle up, the Expenses / Balances / Members tabs, the expense rows grouped by day (icon, title, time, who paid,
  amount, your share, the payment tag, the row menu) and the Split a bill button.
- **The Mac's and the web's shell around it:** the sidebar (Search, Home, Groups, Balances, Contacts, the Open groups
  with their balances, the profile at the bottom on the web) and the web's breadcrumb.
- **What every screen inherits:** the theme's colors, type and shapes in both themes, the icons, the English copy of
  those screens, how amounts and dates are written, and the sample trip itself.

The code that draws it, to sort a diff by (`git diff --name-only origin/main...HEAD`):

| Platform | Paths |
|---|---|
| Shared | `feature/group_detail` (what the `GroupDetailUiState` holds), the formatting of amounts and dates in `core/*`, the sample trip in `shared/src/commonMain/kotlin/com/quare/blitzsplit/shared/storescreenshots/` |
| Android | `android/feature/group_detail`, `android/designsystem`, `android/store_screenshots/src/test/kotlin/com/quare/blitzsplit/android/storescreenshots/GroupScreenshotContent.kt` and `ReadmeHeroScreens.kt` |
| iPhone and Mac | `iosApp/Sources/Shared/Features/GroupDetail`, `iosApp/Sources/Shared/Features/Home` (the Mac's sidebar), `iosApp/Sources/Shared/DesignSystem`, `iosApp/Sources/Shared/App`, the English strings of `Localizable.xcstrings` |
| Web | `webApp/src/features/group-detail`, `webApp/src/components` (`HomeShell.tsx`, `HomeSidebar.tsx` and the pieces the group uses), `webApp/src/app` (the shadcn/ui components), `webApp/src/index.css`, `webApp/src/i18n/en.ts` |
| The image's layout | `scripts/readme-hero/` (`hero.html`, `compose.mjs`) |

A path in the table is a reason to look, not a verdict: a change to `android/designsystem` that only touches a
component the group screen doesn't use, a Balances tab change, a dialog, a tablet layout, another language, a fix with
no visible effect or a change the sample trip never reaches (an empty group, an error state, offline) leaves the image
as it is. Decide by whether a reader comparing the old and new image would see a difference, and name the platforms
whose screen changes.

If nothing shows, stop here and say so in one line (in `create-pr`, one line of the pull request description is
enough: "The README's image doesn't show this change").

## 2. Capture

What it needs, all on this Mac: Xcode and its iOS runtime (the iPhone capture creates its own simulator, "Store
screenshots iphone63", an iPhone 17 Pro), Google Chrome (`CHROME` points at another binary), Node, and for the Mac
capture an unlocked screen and the Screen Recording permission for whatever runs the script: it brings the app's window
to the front for about half a minute. No emulator, and not the task's own simulator.

The captures land in `build/outputs/readme-hero/` of the checkout, and `generate.sh` draws the image only once all
eight (four platforms, two themes) are there. A fresh worktree has none, so capture all four, even when the change
reaches one platform: the dates then match across the four screens too. In a worktree, run the worktree's script; it
works from its own folder.

```bash
./scripts/readme-hero/generate.sh
```

Run it with the Bash tool's `run_in_background` and let its completion call you back: it builds the Android tests, the
Apple UI tests and the web's shared library, each queued behind other worktrees' builds by
`scripts/heavy-check.sh`, and takes from several minutes to much longer when other sessions build. Don't wrap it in
`heavy-check.sh` yourself and don't poll it.

Once the captures exist, a fix to one platform needs only that one again, and drawing again needs none:

```bash
READMEHERO_CAPTURES=web ./scripts/readme-hero/generate.sh      # android, ios, mac, web, comma separated
READMEHERO_CAPTURES=none ./scripts/readme-hero/generate.sh     # only hero.html and compose.mjs changed
```

If a capture can't run here (the screen is locked, the Screen Recording permission is missing, Xcode or Chrome is
missing), don't ship an image with an old screen beside new ones: leave both images as they are, and say which
capture failed and why in the pull request description and the report, so the person can run it later. Don't touch
system settings to get past it.

## 3. Check what it drew

`generate.sh` writes the two `.webp` files over the old ones. Convert both to PNG and Read them, and the captures in
`build/outputs/readme-hero/` when something looks off. Check:

- All four screens show Lisbon trip's Expenses tab with its photos and faces, not a loading skeleton, an empty group, a
  blank window, an error or a system dialog over the app.
- The dark image is dark on all four, and the light one light.
- The change of the branch is there, on each platform it reaches.
- Nothing else moved by surprise. A difference the branch doesn't explain is either another merged change nobody
  captured yet (fine, it goes in too) or a broken capture: look at that platform's capture before committing.

A broken capture is fixed in the capture, never by editing the image: rerun that platform with
`READMEHERO_CAPTURES=<platform>`. If the README's text below the image (its alt text, the paragraph naming the four
UIs) no longer describes what the image shows, update it in the same commit.

## 4. Commit

Commit the two images with the branch's other changes, or on their own when this skill runs alone:

```bash
git add docs/images/readme-hero-dark.webp docs/images/readme-hero-light.webp
```

The captures in `build/` are git-ignored and stay out. Then say, in the pull request description, that the README's
image was refreshed and which platforms' screens changed in it. When this skill runs on its own, open the pull request
with [`create-pr`](../create-pr/SKILL.md) (a `chore` branch, no release note: the app doesn't change).
