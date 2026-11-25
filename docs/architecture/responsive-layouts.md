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
- **Count the navigation bar once above the keyboard.** On Android the keyboard's inset (`imePadding()`) holds the
  navigation bar too, so content already padded by a `Scaffold` consumes that padding first
  (`.padding(padding).consumeWindowInsets(padding).imePadding()`, as `ColumnsFormLayout` and `FormLayout` do);
  otherwise a blank band the height of the navigation bar sits over the keyboard and cuts the form.
- The product decisions under `docs/product/` say how each flow is drawn at each size; where a board and a bullet disagree, the
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
| `isOnCoverScreen` | A Flip's cover screen: the whole display (the largest window it allows) is under 480 dp on its long side, so a phone's split screen half never counts. `formFactor` stays `Phone` there; Início (`HomeLayout.COVER`) and Grupos draw their cover, every other screen draws as on a phone |

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
| `rememberUsesFormDialog()` | The keys, Excluir conta: a dialog on a tablet, full screen on any foldable. Flow 6 has its own rules in `:android:feature:edit_group`: Editar grupo is a dialog on anything wider than a phone (`rememberUsesEditGroupDialog()`, off the hinge as every dialog), Divisão padrão on a tablet, a book and a tabletop, full screen on a foldable upright (`rememberUsesDefaultSplitDialog()`) |
| `rememberUsesGroupDialog()` | Atividade's filter: a dialog on a tablet and a foldable upright, a sheet on a phone, a book and a tabletop |
| `rememberFormPresentation()` | Registrar pagamento, Convidar of Contatos, Edit profile and a payment method: a sheet on a phone, a dialog wider, a book's in the right half, full screen on a tabletop (Registrar pagamento and Convidar split on the hinge, `TabletopGap` apart; Edit profile and a payment method in the screen they always drew there); each entry keeps the one it opened with, as its scene does |
| `ColumnsFormLayout(tabletop = TabletopForm.…)` | Rachar conta, Dividir and Itens full screen on a tabletop: the first column above the hinge, the second below it, the confirm button in the bar (`ConfirmInBar`) or under the second half (`ConfirmAtBottom`). Rachar conta and Conta otherwise open over the group as a dialog on anything wider than a phone (`AppNavDisplay`), a book's right half and, for Editar conta and Conta, a tabletop's half above its hinge |
| `rememberUsesFormColumns(isDialog)` / `rememberUsesTabletopHalves(isDialog)` | Whether a form draws two columns (a dialog only at a tablet lying down's width, never in a book's half) or a tabletop's halves |
| `rememberListDetailLayout()` | The home's list and detail (Grupos, Saldos): a tablet lying down (520 dp list) and a book (the half before the hinge), `paneSpacing` apart (the band the hinge reports on a book, at least 24 dp) |
| `TrailingOrBelowLayout(main, trailing, minMainWidth = 160.dp)` | A row's text and what trails it (an amount, a button): side by side while the text keeps `minMainWidth` (or all it needs, if less), the trailing part under the text otherwise. Members' rows and Balances' pairs; the Apple app has a `Layout` of the same name, the web a `flex-wrap` that moves the trailing part under once the whole text and it don't fit |
| `@DevicePreviews` | Phone, phone lying down, tablet both ways, foldable upright (673×841) and book (841×673, split in the middle by its size). A tabletop preview provides `LocalFoldLayout provides tabletopFoldLayoutOf(841.dp, 673.dp)`, a cover screen one `coverFoldLayoutOf(360.dp, 374.dp)` |
| `HomeNavigationRail` / `WithGroupNavigationRail` | The home's rail (no labels on a phone lying down nor a tabletop), and the same rail beside a group's screen drawn alone wider than a phone upright, never on a tabletop nor in the pane beside the list, with the tab the group was opened from selected (`homeTab`) and tappable again to go back to it (`reselects`); both drawn once around `NavDisplay` by `HomeBarsLayout`, each screen leaving their place free, insets included, through `HomeBarsScaffold` (a preview draws them inline) |

A confirmation that keeps off the hinge is `ConfirmDialog(placement = rememberDialogPlacement(...))`, and any other
alert `FoldAlertDialog(..., placement = rememberDialogPlacement(...))` (the icon, title, text and buttons of Material's
`AlertDialog`, which `ConfirmDialog` is built on): centered it is Material's `AlertDialog`, in a half a `FoldDialog`
drawn like it (Convidar's "Trocar o link?" in a book's right half and a tabletop's top half, the profile's sign out
dialogs in a book's right half).

Checked on the Pixel Fold emulator: Registrar pagamento in a book's right half (`FoldDialog`) and full screen on a
tabletop (`TopBottomHalvesLayout`), and the group's Saldos drawn alone on a tabletop; the Galaxy Z Fold and the phone
lying down are still preview only for them.

Not checked on a device yet: `isOnCoverScreen` (the Galaxy Z Flip's and the Razr's cover screens), `TwoHalvesLayout` (All expenses, Boas-vindas, Seu perfil, Recuperar, Convidar, Convite and Quem é você,
Perfil, Editar perfil, the keys and Estatísticas on a book), `FoldDialog`'s halves (the recovery code, Novo grupo, "Trocar o link?",
"Não estou na lista", the profile's dialogs, Excluir conta, Cobrar, Editar grupo, Divisão padrão and its exception, Membros' dialogs),
`TopBottomHalvesLayout` (Convidar, Convite and Estatísticas on a tabletop, `TabletopForm`), `tabletopFoldLayoutOf` and `FoldHinge.isSeparating`:
their layout math (right to left, a hinge band partly off the layout) has only run in previews. Each flow's
responsive slice adopts them (see below) and checks them on the Pixel Fold and Galaxy Z Fold emulators then.
Estatísticas in the group's pane on a tablet lying down has only run in builds too.
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
stack that already ends in a key or a bill never shows pushed and then jumps into a sheet. The idiom checks left
are `DeviceName`, the word the copy says ("iPhone", "iPad"), since an iPhone Duo is an iPhone whatever its size, and
the bars, which iOS draws by idiom: minimizing them on scroll is the iPhone's (`PhoneBarMinimization`), and the
sidebar by default where iPadOS docks it and the crowded top bar's rule are the iPad's (`docksTabSidebar`,
`isTabBarCrowded`), since an iPhone Duo open keeps its tabs in the system's vertical bar.

The pieces in `iosApp/Sources/Shared/DesignSystem`:

| Piece | Use it for |
| --- | --- |
| `TwoColumnsLayout(firstWidth:secondWidth:spacing:maxWidth:alignment:first:second:)` | Two columns of one screen in a container of at most 920 pt (or `maxWidth`), the first fixed (or half), or the second fixed (Convidar's QR card); on an iPhone Duo whose fold runs down, each column on its side of the fold |
| `formSheetSizing()` | A form sheet over the screen behind it (the recovery code, Novo grupo, Editar perfil, Editar grupo, Divisão padrão's exception and Registrar pagamento use it; Cobrar too, a large detent there so it stays in the middle): about 540 pt on iPad |
| `billSheetSizing(in: windowLayout)` | A bill's sheet with an iPad's room (Rachar conta, Conta): 820×640 lying down, 620×860 upright, never more than the window (`ScreenLayout.billSheetSize`); a phone keeps its own sheet. The root's sheet asks for the same size (`AppRoute.usesBillSheet`, `foldSheetPlacement(isBill:)`), since its sizing is the one iOS 27 keeps, and grows to it while Divisão padrão is pushed inside Editar grupo's (`AppRoute.growsSheetToBill`) |
| `FoldDivision` / `.readingFoldDivision($division)` | The iPhone Duo's fold over a view, in that view's coordinates, for a layout `TwoColumnsLayout` and `FoldHalves` don't cover (a list ending at the fold, a custom overlay); `verticalFrame` and `horizontalFrame` say which way it runs |
| `ListDetailSeparator(fold:)` | Between a list and the column beside it: the fold's band kept empty (`verticalFrame`), else a divider |
| `FoldHalves(centersTop:scrollsHalves:splits:top:bottom:single:)` | The Duo open upright, as a notebook, where the boards draw their own version (2.2, 3.1, 6.2, 6.5, 6.6, 7.1, 7.2): `top` above the fold, `bottom` below it, each scrolling on its own (`scrollsHalves: false` for a Form or a List in each half); `single`, the screen's one column, everywhere else and while `splits` is false. Its first frame takes the window's fold (`windowFold`), so a screen opening on the fold never rebuilds what it holds |
| `foldSheetPlacement(isForm:isBill:)` | On a sheet's content (`formSheetSizing()` already has it with `isForm`, the root's sheet from `AppRoute.usesFormSheet` and `usesBillSheet`, a bill's while a route that `growsSheetToBill` is pushed inside it, with `resizingSheet`, `rootSheetSizing(for:pushed:)`): with the fold running down the window (`windowFold`, read by the root over the whole window) the sheet opens in the leading half, no wider than it (`presentationPlacement(.leading)`, iOS 27.0), with what it opened over in the other half; everywhere else the system places it at its own size, the form's or the system's. A routed screen that adopts another sizing needs its route's sizing at the root too, as `billSheetSizing` has with `isBill` |
| `ToolbarContent.prefersVerticalBar()` / `.readingVerticalBar($isVertical)` | A toolbar item the boards put in the closed Duo's vertical bar (`axisBehavior(.verticalPreferred)`: the avatar, "Rachar conta"), and whether the bar is vertical now (`toolbarVerticalEdge`), so a button drops its title (`SplitBillButton(showsTitle:)`) or leaves the bottom bar for it |

`formSheetSizing()` sizes the recovery code's, Novo grupo's, Editar perfil's and Editar grupo's sheets;
`billSheetSizing` Rachar conta's and Conta's. Pushing Divisão padrão (`AppRoute.growsSheetToBill`) grows Editar
grupo's form sheet to a bill's (the boards' 820×640, in two columns, lying down; 620×860 upright) and popping it
shrinks it back, while anything else pushed (Mapa de categorias over a bill) keeps the sheet's size. SwiftUI sizes a
sheet only as it opens, so the root's sheet resizes through its `preferredContentSize` (`resizingSheet`), and gives
it back to the system on the pop, which sizes it for the window it is in then; Divisão padrão lays out for the grown
size (`grownSheetSize`) from its first frame, not for the form's it measures while the sheet grows. Not checked on a
device yet: Estatísticas in an iPad's group pane.

A screen that draws both in the whole tab and in the column beside a list measures itself, not the window, and
reads `showsGroupBesideList` for what its board draws in that column. The group's screen (`GroupDetailView`) puts
its header and balance card in a side column wherever it is lying down with the tab to itself (an iPhone, an iPad,
an iPhone Duo open, through `TwoColumnsLayout`) and keeps one column beside the groups, while Statistics is two
columns there too. Until its first measure the group's screen goes by `windowLayout`, so it never opens in one
column and then splits. Not checked on a device yet: the group's two columns on an iPad lying down (issue 388; the
iPhone Duo simulator open, and closed lying down, were).

The iPhone Duo (iOS 27.1 SDK, decision 7): the app builds with the iOS 27.1 SDK (Xcode 27.1, see
[getting-started.md](../getting-started.md)) and keeps iOS 26 as its minimum, so every Duo API sits behind
`#available(iOS 27.1, *)` and below it the size rules decide alone. `FoldDivision` reads
`GeometryProxy.reservedRegions(kind: .division, options: .includeInactive)`: the fold counts open flat too, as the
boards split there; `isActive` says the device is partially open, `isVertical` whether it folds like a book or like a
notebook. Apple steers layout to these regions, not to the hinge's angle, so `onHingeChange` (live status and angle,
for interactions) is not read. Before building anything by hand for the Duo:

- System sheets, alerts, menus, split views and tab bars already keep off the fold and the camera, but a form sheet
  keeps its width over the fold: `foldSheetPlacement()` moves it into the leading half.
- Built with the 27.1 SDK, the closed Duo's vertical bars (back, glass buttons, a sheet's ✕ and ✓, tabs on the
  camera's edge) are the system's vertical toolbars, and the open Duo lying down keeps the tabs and the bar there too;
  a screen only tunes them with `prefersVerticalBar()`, and a button drops its title wherever the bar is vertical. A `NavigationStack` nested beside another (Grupos' and Contatos' list beside the
  detail) keeps its own horizontal toolbar and leaves about 84 pt on its trailing side for a vertical bar, so its List
  ends short of the fold; `toolbarVerticalBehavior(.disabled)` there would take the tabs' vertical bar away too, so
  it stays.
- `ArrangementView` (`.arrangementViewStyle(.split)`) places two related views on each side of the fold, an option
  for a screen whose two halves are one unit. It is iOS 27.1 only, so it needs an iOS 26 fallback beside it.
- The Mac builds with the macOS 27 SDK, which has none of these: they stay behind `#if os(iOS)`.

The Mac's profile is the app's `Settings` scene (Ajustes, ⌘,), which shares the app's `AppModel` with the main
window: `AppModel` pops a `Route.Profile` the shared code pushes and asks the root to `openSettings()` instead, and
what the profile opens (`AppRoute.opensInSettings`: a key, deleting the account) is `AppModel.settingsSheet`, a sheet
on the Settings window that the main window never draws (see
[navigation.md](navigation.md#the-macs-ajustes-two-windows-one-back-stack)).

The boards' Duo measures are provisional (taken from Apple's diagrams): the iOS 27.2 simulator's Duo is about 466×678
pt closed and 951×669 pt open, with a 40 pt fold (its 20 pt margins included) at x = 455.5 lying down. Compare with
Apple's template before matching a number exactly.

### Web

The web follows the CSS width only: Tailwind's `md` (768 px) and `lg` (1024 px) in the markup, and `useMediaQuery`
where a component changes (a dialog under 768 px becomes a drawer, Saldos' detail shows from 1024 px). Grupos and
Saldos sit in `HomeShell`: the sidebar on the desktop, the icon rail on the tablet, the bottom bar on the phone.

### Home's "Balances with people"

One shared rule, `HomePeopleLayout.of(peopleCount, blockWidth, isTouch)` in `:feature:groups`, picks how the block
draws its people on the three UIs. It reads three things only, never the device: the width the block itself gets (dp
on Android, pt on Apple, CSS px on the web; Android and Apple take the column's width less the gutters, the web the
card's content box), the input, and the count.

| Block | Input | 1–3 people | 4+ people |
| --- | --- | --- | --- |
| < 560 | touch | list | carousel of everyone, about 2.5 cards in sight |
| < 560 | pointer | list | the first 3 rows, then a "+N" row |
| ≥ 560 | either | fixed-width cards, aligned to the start | touch: carousel; pointer: cards on 2 lines at most, the last slot a "+N" tile |

- A list row is the face, the name, the amount with its sign and its word on one line; it is never wider than
  **560**, so a name never sits far from its amount: the list only comes below the threshold, **560**
  (`WIDE_BLOCK_MIN_WIDTH`, the design system's `column-android-portrait`), which caps it.
- A card stacks the face, the name, the amount and the word, **200** wide (`CARD_WIDTH`) with **8** between cards
  (`CARD_GAP`). Cards never stretch: the space after them stays empty on purpose. A carousel's cards narrow so about
  2.5 show (never under 112, never over 200), the cut one saying the row scrolls.
- The input: Android, iOS and iPadOS are always touch (an iPad with a trackpad and a Chromebook with a mouse keep the
  carousel); the Mac is always a pointer; the web is touch on `(pointer: coarse), (hover: none)`.
- So a phone, a foldable, the Duo, a phone lying down and the narrow first column of a two-column tablet (~440) get the
  list or the carousel; a tablet or an iPad upright sits at the threshold; the web's desktop and the Mac's default
  window get cards; the Mac's narrow window and the web's tablet with a mouse get the list (the web's phone, touch,
  the carousel from 4).
- The people left out show as the hidden ones' first three faces and "+N", in the list's last row or the cards' last
  slot (so one person fewer shows), and open Balances; a screen reader reads "See balances (+N)", a string of its own.
  Only the Mac and the web reach it: Android and iOS are touch, where nobody is left out. The header's "See
  balances" stays the only link in words: a "See balances (+N)" button under the cards repeated it (Pierre's call,
  2026-11-24).
- The order is the ViewModel's: whom you owe first, then who owes you, the largest first. The title counts everyone
  (" · 9") once the carousel scrolls or someone is left out. While the people are read, the faces' shimmer holds the
  place.

### A flow's responsive slice

1. List the flow's boards per size (the base files' `size` Tweak) and the bullet of its `docs/product/<area>.md` that says how it opens.
2. Android: pick the presentation per `formFactor` in `AppNavDisplay`, and the content per size inside: `DeviceLayout`
   off a foldable, `TwoHalvesLayout` on a book, `TopBottomHalvesLayout` where a tabletop board exists, `FoldDialog`
   for a dialog or sheet the book draws in a half. Previews with `@DevicePreviews` cover the book.
3. Apple: `ScreenLayout` rules and `TwoColumnsLayout`; the sheet sizing helper the board's size calls for; nothing
   by idiom.
4. Update the flow's bullet in its `docs/product/<area>.md` with what changed, and check the screen at each size on an emulator or simulator
   (the Pixel Fold and Galaxy Z Fold emulators, the iPhone Duo simulator).

### Right to left

Arabic reads right to left (Hebrew, Urdu and Persian join it), and every UI mirrors in it: what is at the start of a row in English is
at its right edge. The rules, the same on the three platforms:

- **Start and end, never left and right.** Compose's `start`/`end` paddings, alignments and `Arrangement`s;
  SwiftUI's `leading`/`trailing`; Tailwind's logical classes (`ms-`/`me-`, `ps-`/`pe-`, `inset-s-`/`inset-e-`,
  `text-start`/`text-end`, `border-s`/`border-e`, `rounded-s`/`rounded-e`). A physical side is kept only where the
  drawing is about the picture, not the text: a centered `left-1/2`, Adjust photo's crop, the color picker's square
  and hue bar, Simplify debts' example drawing. Compose's `offset(x)` and SwiftUI's `.offset(x:)` already flip in right
  to left, so a corner badge's offset stays as it is.
- **The page's direction comes from the language.** Android's `supportsRtl` and SwiftUI flip by themselves (Apple's
  `appLocale` also sets the windows' `layoutDirection` when Profile › Language picks a language while the app runs).
  The web sets `<html dir>` and `lang` from the locale (`applyDocumentLocale`, `isRightToLeft`) and wraps the app in
  Radix's `Direction.Provider`, so menus, tabs and sliders follow it.
- **Directional icons mirror; the others don't.** Back, chevrons, forward and back arrows, a pager's previous and
  next, log out: Android's `Icons.AutoMirrored`, SF Symbols' `chevron.forward`/`arrow.backward` (never `…right`), and
  the web's `index.css` rule on lucide's directional classes. A checkmark, the camera, the clock, a search glass, a
  chart and a logo stay as they are. A "→" written in text becomes "←" (Android's `ActivityRow`/`TermSheet` do it;
  each translation writes its own), since arrows don't mirror; a menu path's "›" stays "›" in every translation, since
  it is a mirrored character that right to left text already draws as "‹" ("Profile › Language").
- **Gestures and keys follow the reading order.** A pager's swipe and the arrow keys of Bill attachments' viewer
  swap their meaning; a sheet slides in from the inline end.
- **A chart over time doesn't mirror.** Statistics' time chart keeps time running left to right (Compose's
  `LocalLayoutDirection` set to `Ltr` on the chart, SwiftUI's `layoutDirection` on the `Chart`, the web's `dir="ltr"`
  on its SVG), while its title, legend and callout follow the page. A ring needs nothing.
- **Amounts keep their order.** A formatted amount inside a right to left language is wrapped in the Unicode isolates
  U+2068 … U+2069 (`MoneyFormatter`, `ExpenseFormat`, the web's `money.ts`), so "−R$ 12,50" never comes apart in a
  sentence, and so is its currency symbol ("US$", never "$US"); left to right languages get the same text as before.
  A "+" or "−" the app adds goes right before the digits, after the mark the language's own negative number puts
  before its minus, so it sits where Arabic and Hebrew write it (on the left of the digits). Arabic, Bengali, Urdu, Persian, Tamil, Telugu, Marathi, Gujarati, Kannada, Burmese and Khmer write numbers, amounts and dates with
  Latin digits (`nu-latn`: Android's `LegacyAppLocale.wrap` for the whole context, Apple's `AppNumbers`, the web's
  `formattingLocale`), so a field shows what it parses.
- **An amount field types left to right.** Its digits run left to right ("12." never shows as ".12") while the field,
  its symbol and its label sit on the right to left side (Compose's `withLeftToRightDigits`, the web's `dir="ltr"` on
  number inputs).
- **Look at it.** A change to a layout is checked once in Arabic too: Profile › Language, Home, a group, Split a bill,
  Statistics and Activity.
