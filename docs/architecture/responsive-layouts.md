## Responsive layouts

The boards draw each screen once per size: a phone upright and lying down, a tablet upright and lying down, a
foldable upright, as a book and as a tabletop, an iPhone Duo closed, open and upright, the web's desktop, tablet and
phone widths, and a Mac window, wide and narrow. A screen picks its layout and how it opens (full screen, dialog,
sheet, half of a fold) from the size it is drawn at, in its own UI: the shared code never knows the window. This page
says how each platform tells the sizes apart and which pieces a screen uses, so every flow does it the same way.

The rules:

- **The window decides, never the device's model or idiom.** An iPad's layouts go to any window with an iPad's room,
  an iPhone Duo open included; a phone layout goes to a narrow Split View on an iPad.
- **A foldable follows the hinge it reports**, not its window alone (Android's `FoldingFeature`, Apple's division
  reserved region). The same 673×841 window is a book on a Galaxy Z Fold (hinge down the middle) and upright on a
  Pixel Fold (hinge across). A window that reports no fold falls back on its size, so emulators and previews still
  get the book.
- **Nothing crosses the hinge band** (24 dp on Android, the Duo's fold region on iOS): two halves meet on it, and a
  dialog takes one half with the other under the scrim.
- **Measure the window, not what the keyboard leaves**, so opening the keyboard never swaps the layout and rebuilds
  the focused field.
- The CLAUDE.md product decisions say how each flow is drawn at each size; where a board and a bullet disagree, the
  board wins and the bullet changes in the same pull request.

### Android

`MainActivity` wraps the app in `FoldLayoutRoot`, which reads the window (its bounds on screen) and the first fold
the device reports in it (`collectFoldingFeaturesAsState`, androidx.window's `FoldingFeature`: orientation, flat or
half opened, bounds, separating) once, and hands it down as `LocalFoldLayout`. Read it with `rememberFoldLayout()`
(in a preview, which has no root, it reads the preview's window). It holds:

| Value | What it is |
| --- | --- |
| `formFactor` | The board's size: `Phone`, `PhoneLandscape`, `TabletPortrait`, `TabletLandscape`, `FoldableUpright`, `Book`, `Tabletop` |
| `bookSplit` | Where the halves side by side meet, on screen: the vertical hinge (or the middle without one) on a book, the middle on a tabletop lying down, null elsewhere |
| `tabletopSplit` | Where the top and bottom meet on a tabletop (half opened, hinge across), null elsewhere |
| `hinge` | The raw fold (`FoldHinge`), for anything the above doesn't answer |

How `formFactor` is decided: under 600 dp on the short side is a phone, whatever it reports (a Flip open is a phone);
a vertical hinge is a book in any posture; a half opened horizontal one a tabletop; with no fold, 960 dp or more on
the long side is a tablet; anything else (a flat horizontal fold, a foldable-sized window) is a book lying down and
`FoldableUpright` upright.

The pieces built on it, in `:android:designsystem`'s `layout` package:

| Piece | Use it for |
| --- | --- |
| `DeviceLayout` / `rememberDeviceLayout()` | The activity window's size only (`FoldLayout.windowBounds`, so a screen in a dialog sizes itself by what it opened over): two columns lying down, the rail on anything wider than a phone upright. What every screen used before foldables; it stays the rule off a foldable |
| `TwoHalvesLayout(start, end, gap)` | A screen the boards split on a book's hinge: each half on its side, the gap centered on the hinge and never narrower than the band (`gap = 72.dp` for flows 3 and 4). It finds itself on screen, so it works after the rail or inside a dialog |
| `TopBottomHalvesLayout(top, bottom, gap)` | A screen with a tabletop board of its own: what is read or scanned on top, the controls at the bottom. Needs a bounded height; each half scrolls on its own |
| `FoldDialog(onDismissRequest, properties, placement, content)` | A dialog off the hinge: `DialogPlacement.Centered`, `EndHalf` (a book's right half, the other half under the scrim) or `TopHalf` (a tabletop's). `rememberDialogPlacement()` is the canvas' default per posture (a tabletop lying down draws its dialogs like the book's, in the right half above its hinge); `rememberDialogPlacement(isTopHalfOnTabletop = true)` where the boards put them on a tabletop's top half even lying down (flow 2). The content brings its own surface, as in a centered dialog; one that replaces Material's `BasicAlertDialog` puts `Modifier.alertDialogPane(title)` on it, for the 280 to 560 dp width and the pane title TalkBack says |
| `FoldDialogSceneStrategy.dialogMetadataOf(properties, isTopHalfOnTabletop)` | A navigation entry in a `FoldDialog` over the entry under it (`AppNavDisplay` uses it for every dialog route); `isTopHalfOnTabletop` for a flow whose boards put it in a tabletop's top half even lying down (flow 2's Novo grupo) |
| `rememberUsesFormDialog()` | Editar grupo, the keys, Excluir conta: a dialog on a tablet, full screen on any foldable |
| `rememberUsesGroupDialog()` | Rachar conta, Conta, Registrar pagamento, Cobrar: a dialog on a tablet and a foldable upright, full screen or a sheet on a phone, a book and a tabletop |
| `rememberListDetailLayout()` | The home's list and detail (Grupos, Saldos): a tablet lying down (520 dp list) and a book (the half before the hinge), `paneSpacing` apart (the band the hinge reports on a book, at least 24 dp) |
| `@DevicePreviews` | Phone, phone lying down, tablet both ways, foldable upright (673×841) and book (841×673, split in the middle by its size). A tabletop preview provides `LocalFoldLayout provides tabletopFoldLayoutOf(841.dp, 673.dp)` |

A confirmation that keeps off the hinge is `ConfirmDialog(placement = rememberDialogPlacement(...))`, and any other
alert `FoldAlertDialog(..., placement = rememberDialogPlacement(...))` (the icon, title, text and buttons of Material's
`AlertDialog`, which `ConfirmDialog` is built on): centered it is Material's `AlertDialog`, in a half a `FoldDialog`
drawn like it (Convidar's "Trocar o link?" in a book's right half and a tabletop's top half, the profile's sign out
dialogs in a book's right half).

Not checked on a device yet: `TwoHalvesLayout` (Boas-vindas, Seu perfil, Recuperar, Convidar, Convite and Quem é você,
Perfil, Editar perfil and the keys on a book), `FoldDialog`'s halves (the recovery code, Novo grupo, "Trocar o link?",
"Não estou na lista", the profile's dialogs and Excluir conta),
`TopBottomHalvesLayout` (Convidar and Convite on a tabletop), `tabletopFoldLayoutOf` and `FoldHinge.isSeparating`:
their layout math (right to left, a hinge band partly off the layout) has only run in previews. Each flow's
responsive slice adopts them (see below) and checks them on the Pixel Fold and Galaxy Z Fold emulators then.
`FoldDialog` and `FoldDialogSceneStrategy` already draw every dialog route.

The two `rememberUses…Dialog` rules are the presentations of today. A flow whose boards draw something else on a
foldable (Registrar pagamento in the right half instead of a sheet) switches on `formFactor` for that route in
`AppNavDisplay` and tells its screen how it is drawn, as Novo grupo does (a 560 dp dialog upright, in the right half on
a book, in the top half on a tabletop):

```kotlin
val formFactor = rememberFoldLayout().formFactor
val usesCreateGroupDialog = formFactor != FormFactor.Phone && formFactor != FormFactor.PhoneLandscape
val createGroupMetadata = if (usesCreateGroupDialog) {
    FoldDialogSceneStrategy.dialogMetadataOf(properties = dialogProperties, isTopHalfOnTabletop = true)
} else {
    emptyMap()
}
entry<Route.CreateGroup>(metadata = createGroupMetadata) {
    // In a book's half the dialog is the half's width minus 16 dp each side: the form lays out for that width
    CreateGroupScreen(isDialog = usesCreateGroupDialog)
}
```

`FoldDialog` places the dialog in the end half by itself on a book, so the route only says it is a dialog. A sheet
the boards move into a half (Registrar pagamento, Cobrar on a book) becomes a `FoldDialog` there, not a bottom sheet
that would cross the hinge. Inside, a screen lays out for the space it gets: one column in a half, `TwoHalvesLayout`
or `TopBottomHalvesLayout` full screen.

### Apple

Every layout rule is `ScreenLayout`, the size of what a view measures (`.measuringScreenLayout($layout)`), never
`userInterfaceIdiom`:

| Rule | Means |
| --- | --- |
| `usesTwoColumns` | Lying down and at least 640 pt wide: an iPhone lying down, an iPad lying down, an iPhone Duo open, a wide Mac window |
| `isLarge` | An iPad's room, 600 pt or more on the short side: an iPad, an iPhone Duo open (920×648) and upright (648×920), a wide Split View |
| `showsListDetail` | `isLarge` and lying down (iOS only): the home's list and detail on an iPad lying down and an iPhone Duo open |
| `isShortColumn` | Upright, wider than any iPhone (over 440 pt), short of an iPad's room and under 700 pt tall: the iPhone Duo closed (452×648), where the boards drop what doesn't fit (Boas-vindas' benefits). Never on the Mac |

The root measures the whole window (`.measuringWindow`) into the environment's `windowLayout`, so a screen inside a
sheet still knows what it opened over, and into `AppModel.isLargeWindow`, which only changes when the window crosses
an iPad's room, so a live resize never redraws the root: `AppRoute.presentsAsSheet(isLargeWindow:)` opens a key, a
bill and Editar perfil as a sheet with an iPad's room, pushed on an iPhone, and Convidar the other way round (a sheet on an iPhone,
pushed full screen with an iPad's room). Until that first measure the root draws nothing, so a back
stack that already ends in a key or a bill never shows pushed and then jumps into a sheet. The only idiom check left
is `DeviceName`, the word the copy says ("iPhone", "iPad"), since an iPhone Duo is an iPhone whatever its size.

The pieces in `iosApp/Sources/Shared/DesignSystem`:

| Piece | Use it for |
| --- | --- |
| `TwoColumnsLayout(firstWidth:secondWidth:spacing:maxWidth:alignment:first:second:)` | Two columns of one screen in a container of at most 920 pt (or `maxWidth`), the first fixed (or half), or the second fixed (Convidar's QR card); on an iPhone Duo whose fold runs down, each column on its side of the fold |
| `formSheetSizing()` | A form sheet over the screen behind it (the recovery code, Novo grupo and Editar perfil use it; Editar grupo, Registrar pagamento, Cobrar to come): about 540 pt on iPad |
| `billSheetSizing(in: windowLayout)` | A bill's sheet: 820×640 lying down, 620×860 upright, never more than the window. Check on a device that the sheet's two columns still fit when adopting it |
| `FoldDivision` / `.readingFoldDivision($division)` | The iPhone Duo's fold over a view, in that view's coordinates, for a layout `TwoColumnsLayout` doesn't cover (a top and bottom split upright, a custom overlay) |

Not checked on a device yet: `TwoColumnsLayout` (Boas-vindas, Seu perfil, Recuperar, Novo grupo's sheet lying down,
Perfil and Editar perfil lying down), `FoldDivision` / `.readingFoldDivision` (also the empty home and Perfil on an
iPhone Duo open) and `billSheetSizing`, which no screen uses yet; the per-flow slices check them on the iPhone Duo
simulator and an iPad. `formSheetSizing()` sizes the recovery code's, Novo grupo's and Editar perfil's sheets.

The iPhone Duo (iOS 27.1 SDK, decision 7): the app builds with the iOS 27.1 SDK (Xcode 27.1, see
[getting-started.md](../getting-started.md)) and keeps iOS 26 as its minimum, so every Duo API sits behind
`#available(iOS 27.1, *)` and below it the size rules decide alone. `FoldDivision` reads
`GeometryProxy.reservedRegions(kind: .division, options: .includeInactive)`: the fold counts open flat too, as the
boards split there; `isActive` says the device is partially open, `isVertical` whether it folds like a book or like a
notebook. Apple steers layout to these regions, not to the hinge's angle, so `onHingeChange` (live status and angle,
for interactions) is not read. Before building anything by hand for the Duo:

- System sheets, alerts, menus, split views and tab bars already keep off the fold and the camera.
- Built with the 27.1 SDK, the closed Duo's vertical bars (back, glass buttons, tabs on the camera's edge) are the
  system's vertical toolbars (`toolbarVerticalBehavior`, `axisBehavior`, the `toolbarVerticalEdge` environment
  value); a screen only tunes them.
- `ArrangementView` (`.arrangementViewStyle(.split)`) places two related views on each side of the fold, an option
  for a screen whose two halves are one unit. It is iOS 27.1 only, so it needs an iOS 26 fallback beside it.
- The Mac builds with the macOS 27 SDK, which has none of these: they stay behind `#if os(iOS)`.

The Mac's profile is the app's `Settings` scene (Ajustes, ⌘,), which shares the app's `AppModel` with the main
window: `AppModel` pops a `Route.Profile` the shared code pushes and asks the root to `openSettings()` instead, and
what the profile opens (`AppRoute.opensInSettings`: a key, deleting the account) is `AppModel.settingsSheet`, a sheet
on the Settings window that the main window never draws.

The boards' Duo measures are provisional (taken from Apple's diagrams); compare with Apple's template before matching
a number exactly.

### Web

The web follows the CSS width only: Tailwind's `md` (768 px) and `lg` (1024 px) in the markup, and `useMediaQuery`
where a component changes (a dialog under 768 px becomes a drawer, Saldos' detail shows from 1024 px). Grupos and
Saldos sit in `HomeShell`: the sidebar on the desktop, the icon rail on the tablet, the bottom bar on the phone.

### A flow's responsive slice

1. List the flow's boards per size (the base files' `size` Tweak) and the CLAUDE.md bullet that says how it opens.
2. Android: pick the presentation per `formFactor` in `AppNavDisplay`, and the content per size inside: `DeviceLayout`
   off a foldable, `TwoHalvesLayout` on a book, `TopBottomHalvesLayout` where a tabletop board exists, `FoldDialog`
   for a dialog or sheet the book draws in a half. Previews with `@DevicePreviews` cover the book.
3. Apple: `ScreenLayout` rules and `TwoColumnsLayout`; the sheet sizing helper the board's size calls for; nothing
   by idiom.
4. Update the flow's CLAUDE.md bullet with what changed, and check the screen at each size on an emulator or simulator
   (the Pixel Fold and Galaxy Z Fold emulators, the iPhone Duo simulator).
