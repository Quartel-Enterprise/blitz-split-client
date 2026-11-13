---
name: audit-responsiveness
description: "Sweep the app for layout problems at every window size: small phones and large text, phones lying down, tablets, Android foldables (upright, book, tabletop), the iPhone Duo (closed, open, upright), the iPad, the Mac and the web's breakpoints. Reads the code against docs/architecture/responsive-layouts.md, Google's and Apple's guidelines, then runs the app on the task's emulator and simulators at each size, and fixes what it finds. Use whenever someone asks to check, test or fix responsiveness, adaptive layouts, foldables, the iPhone Duo, tablets, font scaling or \"does this screen work on X\" (\"testa a responsividade\", \"vê se quebra no tablet\")."
argument-hint: "[screens or platforms to limit the sweep to]"
---

# Audit responsiveness

Finds what breaks when the window, the fold or the text size changes, on the three UIs, and fixes it. The rules it
checks against are in [docs/architecture/responsive-layouts.md](../../../docs/architecture/responsive-layouts.md)
(how each platform tells the sizes apart and which pieces a screen uses) and in the area docs under `docs/product/`
(how each flow is drawn at each size). Read the first before anything else.

The sweep has two halves, and neither replaces the other: reading the code finds what a device would take hours to
reach (a sheet that misses `foldSheetPlacement()`, a new screen that never asks for the fold), and running the app
finds what no reading predicts (an amount that wraps after its "+", a tile that breaks a word in two).

## 1. Scope

With no argument, sweep everything, weighing what changed since the last sweep: `git log --oneline -- android iosApp
webApp | grep -i -E "responsive|adaptive|layout"` finds it, and `git diff <that commit>..HEAD --stat` lists the
screens added or changed since. With an argument, keep to those screens or platforms.

Start the task with `start-task` if it has not started: it needs an Android emulator and iOS simulators.

## 2. Read the code, one subagent per platform

Launch three read-only subagents in one message (Android, Apple, web), each told to report `path:line`, what breaks,
at which size, whether it is confirmed by arithmetic or only plausible, and the smallest fix with the existing helper
to use. They must not build: the builds of step 3 run in the same checkout. What each one looks for:

**Android (Compose)**: Google's [adaptive quality guidelines](https://developer.android.com/docs/quality-guidelines/adaptive-app-quality) and the doc's Android section.

- A screen or dialog that never reads `rememberFoldLayout()` / `DeviceLayout` where its siblings do, and so crosses the
  hinge on a book: a centered column, a raw Material `AlertDialog` instead of `FoldAlertDialog`, a bottom sheet where
  the boards want a `FoldDialog`.
- The keyboard rule: `.padding(padding).consumeWindowInsets(padding).imePadding()`, never `imePadding()` alone after a
  `Scaffold`'s padding.
- Fixed `height(…)` or `width(…)` around text, which clips from font scale 1.3 up: `heightIn(min = …)` instead.
- A row of text and amount with no `weight`, or a line that may wrap inside a word or after a sign: money is one line,
  `softWrap = false`, shrinking to fit.
- Touch targets under 48 dp, missing `@DevicePreviews` on a new screen.

**Apple (SwiftUI)**: Apple's HIG on [layout](https://developer.apple.com/design/human-interface-guidelines/layout) and the doc's Apple section.

- Any `userInterfaceIdiom` or `UIDevice` check besides `DeviceName`: the window decides (`ScreenLayout`).
- A sheet with detents and no `foldSheetPlacement()`, a centered column that spans the Duo's fold with no
  `windowFold` / `TwoColumnsLayout` / `FoldHalves`, a toolbar item without `prefersVerticalBar()` where the closed
  Duo's vertical bar takes it.
- Fixed `.frame(width:height:)` around text and `.font(.system(size:))` on content, which ignore Dynamic Type
  (`minHeight`, `@ScaledMetric`); an amount with no `lineLimit(1)` + `minimumScaleFactor`.
- Touch targets under 44 pt.

**Web (React, Tailwind)**: WCAG 1.4.10 (reflow at 320 CSS px) and 1.4.4 (text at 200%), and the doc's Web section.

- Fixed pixel widths that overflow under 360 px, flex rows without `min-w-0` / `truncate`, a dialog that should be a
  drawer under 768 px (`ResponsiveDialog`), a sticky pane with no max height, inputs under 16 px on a phone (iOS
  Safari zooms on focus), `100vh` instead of `dvh`, targets under 24 px.

## 3. Run the app at each size

Build once per platform (through `scripts/heavy-check.sh`, in the background), install on the task's devices, and
look at the main screens at each size. Read whole contact sheets, not single screenshots: `scripts/contact-sheet.py`
puts a run side by side.

**Data.** iOS has a seed: a debug build launched with `-StoreScreenshotsLanguage en -StoreScreenshotsTheme
light|dark -StoreScreenshotsScreen <code>` opens the sample trip on that screen
(`scripts/ios-seeded.sh <udid> <screen> <theme> <output.png> [display]`). Android and the web have none: go through
"Use without an account", then create a group with a long name ("Trip to Rio de Janeiro with friends"), a member
with a long name, and a bill with a long description and a large amount (1234567 types $12,345.67). Long names and
large amounts are what break layouts; short sample data hides every truncation bug. Tap "I've saved it" on the
recovery code sheet, or it covers every launch.

**Android**: one emulator covers every size, since the app follows its window:

| Size | Commands (`adb -s <serial> shell …`) |
| --- | --- |
| Phone | `wm size reset`, `wm density reset` |
| Small phone, large text | `wm size 720x1280`, `wm density 320`, `settings put system font_scale 1.5` (360×640 dp) |
| Foldable upright | `wm size 1768x2208`, `wm density 420` (673×841 dp) |
| Book | `wm size 2208x1768`, `wm density 420` (841×673 dp; with no hinge reported it falls back on its size) |
| Tablet lying down | `wm size 2560x1600`, `wm density 320` (1280×800 dp) |
| Tablet upright | `wm size 1600x2560`, `wm density 320` |
| Dark | `cmd uimode night yes` (`no` to go back) |

`scripts/android-tour.py <serial> <output directory> <prefix>` walks Home, the tabs, Activity, Profile, the group and
its tabs, Statistics, a bill, the group's menu, Invite and Search, finding each element by its text, and writes the
contact sheets. Run it once per size. Put `font_scale`, `wm size` and `wm density` back when done. A real hinge
(tabletop, a book's band) needs the Pixel Fold or Galaxy Z Fold emulator; say so when they were not run.

**iOS**: the iPhone Duo closed (`--display=1`), an iPhone and an iPad, in light and dark, on the seeded screens.
The Duo's inner display stays black until the simulator is unfolded by hand in Device Hub, and no tool does it: ask
the user to unfold it while they are there, capture with display 3, and otherwise report the open Duo as read in code
only. Large text: `xcrun simctl ui <udid> content_size extra-extra-extra-large`.

**Web**: the worktree's dev server on a free port, in the browser pane, at 320, 375, 768, 1024 and 1440 px wide
(`resize_window`), light and dark.

What to look for in the sheets: text cut without an ellipsis, a word broken in two, an amount that lost its digits
or kept only its sign, content under a bar or a floating button with no way to scroll it out, a card clipped at the
bottom, a column over the hinge, a dialog taller than the window, one column stretched across a tablet where its
siblings cap their width.

## 4. Fix, then look again

Fix on the platform where it breaks, with the pieces the doc lists; a fix that needs a layout the boards and the
docs don't decide follows what the sibling screens do and is listed in the pull request as a decision taken. Build,
install and run the same size again to see the fix, and one neighbouring size to see nothing else moved. Update the
area's `docs/product/*.md` bullet when how a screen is drawn changed, and the "checked on a device" notes of
`responsive-layouts.md` when something was checked for the first time.

## 5. Report

In the pull request: what was fixed per platform, the sizes it was seen on before and after, what was read in code
only (the open Duo, a real hinge), and what was left as a decision for Pierre. Shut the emulator and the simulators
down as soon as the look is over.
