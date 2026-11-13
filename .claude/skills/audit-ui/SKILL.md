---
name: audit-ui
description: "Run the whole UI quality sweep in one task and one pull request: responsiveness at every window size (audit-responsiveness), color contrast in light and dark (audit-contrast), and the performance of lists (LazyColumn, LazyRow and grids on Android, List and lazy stacks on SwiftUI, long lists on the web). Use whenever someone asks for a general pass over the app's UI, a sweep for visual or accessibility problems, or names two of the three at once (\"faz uma varredura no app\", \"procura problemas de responsividade e contraste\"); for only one of them, use its own skill."
argument-hint: "[screens or platforms to limit the sweep to]"
---

# Audit the UI

One sweep, three audits, sharing the builds, the devices and the subagents:
[`audit-responsiveness`](../audit-responsiveness/SKILL.md), [`audit-contrast`](../audit-contrast/SKILL.md), and the
list performance audit below. Read the two skills first: this one only says how to run them together.

## 1. Ask everything first

The sweep takes hours, so the user usually leaves. Before starting, ask in one go for what only they can give, then
never ask again:

- Worktree or in-place (`start-task`), and whether the pull request merges when green (`create-pr`) or waits.
- Whether a contrast fix may change a token's value, and whether a layout the docs don't decide follows the sibling
  screens (both recommended).
- Device access, while they are at the keyboard: boot the iPhone Duo, an iPhone and an iPad simulator and take one
  screenshot of each through the simulator tool, so each permission prompt shows now; ask them to unfold the Duo in
  Device Hub if the open Duo matters, since no tool can.

## 2. One read-only subagent per platform

Three subagents in one message (Android, Apple, web), each given the three audits for its platform: the checklists
of the two skills plus the list one below. They report and do not build. Start the builds in the background at the
same time (the APK, the iOS app for one simulator's id, the web's `shared` and `install`), all through
`scripts/heavy-check.sh`.

### List performance

- **Android.** Every `LazyColumn`, `LazyRow`, lazy grid and pager: `items` without a stable, unique `key`;
  no `contentType` where a list mixes headers, rows and skeletons; a whole list inside one `item { Column { … } }`; a
  formatter (`rememberDateTimeTextFormatter()`, `rememberMoneyFormatter()`), a list or a lambda built per row instead
  of once above the list; sorting or filtering in composition instead of in the `UiState`; scroll state read in
  composition instead of through `derivedStateOf` or `snapshotFlow`; a `Column(verticalScroll)` over a list that can
  grow. Shared-module models are unstable to Compose unless the stability configuration lists them.
- **Apple.** A `LazyVStack` whose child is one eager `VStack` holding every row (the last row's `onAppear` then
  fires at once and pages the whole history in); `ForEach(…, id: \.self)` over Kotlin data classes or by offset, where
  a stable id exists; `AnyView` rows; formatters, sorting or grouping in `body`; an eager `HStack` in a horizontal
  `ScrollView` over an unbounded list.
- **Web.** `key` by index on a list that reorders; `Intl` objects built per row instead of cached in
  `lib/money.ts`; an unpaged list with a menu per row and no `content-visibility`; an `IntersectionObserver` with no
  `rootMargin`, which loads the next page late; images without `loading="lazy"`.

## 3. Run, fix, run again

Follow step 3 of `audit-responsiveness` for the sizes, looking at the same contact sheets for contrast too (run each
phone size once in dark). Then hand each platform's subagent its decided list of fixes, from its own report and from
what the devices showed, and let the three fix in parallel: they touch different folders. Only one of them builds
with Gradle at a time, so the Apple one doesn't build; build it yourself once the Android one is done. Look at each
fixed screen again at the size where it broke.

## 4. One pull request

`create-pr`, as an `enhancement`, with the three audits as sections: what was fixed, the ratios and sizes before and
after, what was read in code only, and the decisions left for Pierre. Update `docs/product/general.md` for a color
that changed and the area docs for a layout that changed.
