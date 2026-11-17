# The group screen and its bills

Group expenses, Bill, comments, attachments, sharing, Statistics, editing and deleting a bill.

  - A group tapped on the home opens "Group expenses" (`Route.GroupDetail`): the bills by day, the most recent on
    top, with "Split a bill" (Android's extended FAB on a phone upright and a filled button at the head of the top
    bar's actions anywhere wider, as the home's tabs leave it to the rail there: on a phone lying down by Pierre's
    call, beta feedback of 2026-11-15, and on a tabletop or a tablet by the same rule, which Pierre confirmed; on
    iPhone and iPad the top bar's tinted glass item after Statistics and ⋯, spelled out at the list's top and its
    icon alone once it scrolls down, never a floating capsule over the list, beta feedback of 2026-11-15, see
    [Home](home-tab.md), with "Add people" moved from that bar to the top of ⋯ to leave it the room, Pierre's call; a
    primary button on macOS, ⌘N, and the
    web), the group's main button on every tab, Members included, in the same place. Its tabs Expenses / Balances / Members are one screen (`Route.GroupDetail.tab` only says which it opens on):
    the header, "Your balance in this group" and the tabs stay, only what is under them changes, with no transition nor
    check on the tab picked (`GroupDetailUiState.tab`), the back stack never moves, and back from any tab leaves the
    group. Invite's "Go to the group" and "See who joined" open it in Invite's place (`openGroup`), so the
    group never stacks over itself; the one exception is "Split a bill"'s group, which opens the group over the form
    as an entry of its own (`Route.GroupDetail.isOverBill`, see [Split a bill](split-bill.md)), and whose "Split a
    bill" goes back to that new bill's form rather than opening a second one. The header has no line under the
    group's name on any platform, open or folded, the Mac's window subtitle included (Pierre's call, beta feedback of
    2026-11-16: it was visual noise): the member faces tell how many people are in the group, and screen readers say
    it on them ("6 members"), while the group's total spent lives in Statistics. On an Android phone upright that bar folds to its one line on a scroll down
    and opens again on a scroll up, from anywhere in the list; wider, it opens only back at the top. On iPhone the
    bars stay put (see [the bars on scroll](home-and-shell.md)).
  - "See all" at the end of Expenses' first day heading ("Today … See all", drawn as Home's "See all" beside "Latest
    expenses"; on the Mac, whose table's day headings take no button, at the right over the table) opens "All
    expenses" with the group already picked in its Group filter (Pierre's call, beta feedback of 2026-11-16;
    `Route.AllExpenses.groupId`, `GroupDetailUiEvent.OnSeeAllBillsClicked`), which the person may clear there for every
    group. It opens over the group, so back comes back to it, and a bill tapped there opens over All expenses as from
    Home. It shows while the list holds a bill you paid or take part in (`GroupDetailUiState.showsSeeAllBills`): All
    expenses lists only those, never payments nor bills you have no part in, so the link never opens onto nothing.
    Apple's words are Home's own for the link ("See everything" in English), as both open the same screen.
  - "Export group data" (the group's ⋮/⋯ menu, after "Pin to Home"; Pierre's call, beta feedback of 2026-11-14) asks
    for a format, CSV, "Excel (XLSX)", JSON or "Text (TXT)" (Android's dialog, a drawer on a phone's web, Apple's
    confirmation dialog: an action sheet on iPhone, a dialog on iPad and the Mac, whose item reads "Export group
    data…"), makes the file from what the device has, offline included, and hands it to the system: Android's save
    sheet (Storage Access Framework, no storage permission), Apple's file exporter (the save panel on the Mac), the
    browser's download. A file that couldn't be made says "Couldn't export the group's data. Try again." The shared
    code builds every format (`ExportGroup`, `:core:expenses`' `export` package, a minimal OOXML writer for Excel, no
    library); the UIs only hand over their words and save the bytes.
    - What it holds: Expenses (date, description, category, amount, currency, then "Paid · Ana" and "Share · Ana" for
      each member, empty when the member neither paid nor is in the split), Payments (date, from, to, amount,
      currency) and Balances (each member's balance per currency, positive when owed, settled members at 0), bills
      and payments oldest first. Members who left read "Former member" ("Former member 2" from the second on, by
      their oldest bill), and a name two members share is numbered the same way ("Ana", "Ana 2"). Headers, section names and category names come in the app's language; JSON keeps English
      keys, with each member's id, the category's id and the payment's method code.
    - "Payment method": the exporter's own "Paid with" of each bill ("Nubank · Credit card", or the type's name for a
      method with no name), in every format (a column after Currency, a `paymentMethod` object with `type` and `name`
      in JSON). Private: only the marks of whoever exports, and only on bills they still pay, so a member's export never
      carries anyone else's; the
      column only shows while the exporter marked a bill of the group, so an export without marks keeps its columns.
    - Formats: dates are ISO (2026-11-14) in CSV, JSON and the text, the device's day; Excel writes real dates (the
      reader's short date) and numbers, a sheet per section, its header row bold and frozen. Amounts carry every
      decimal of their currency with a dot and no grouping (120.50, yen 1500), so they read the same in any language;
      JSON writes them as numbers beside `amountMinor`. CSV is UTF-8 with a BOM (so Excel reads accents), RFC 4180
      (commas, CRLF), the sections one under the other with a blank line between, and a typed text starting with =,
      +, - or @ gets a leading apostrophe so no spreadsheet runs it as a formula. The CSV keeps commas and dots in every language (no `sep=` line, which scripts and Google Sheets would read as
      a row): Excel in a language whose decimals take a comma opens it in one column, and "Excel (XLSX)" is the format
      for it. The text file is the group's name,
      the day, then each section in aligned columns.
    - The file is named after the group and the day, "Trip-to-Rio-2026-11-14.xlsx" (characters a file system refuses
      dropped, spaces as dashes, at most 60 characters of name).
  - The group's avatar is big in the open header and shrinks into the bar as the list scrolls (beta feedback of
    2026-11-12, Pierre's call), following the scroll, never a timed animation. Android: the open bar (160 dp) draws it
    at 64 dp beside the name, shrinking to 40 dp as the bar folds while the one-line bar's title, 32 dp, fades in over
    it, 12 dp over the faces (Material's `TwoRowsTopAppBar`, folding by the rules above); a tabletop's one-line bar keeps 32 dp. The web:
    one sticky header at every width, open 136 px tall on a phone (the 64 px avatar and the 22 px name under the bar
    of back, Statistics, Invite and ⋯) and 96 px from the tablet up (80 px avatar, 24 px name, with Statistics,
    Invite, "Split a bill" and ⋯ in its row, Statistics and Invite as icons alone under 1024 px so the name keeps
    its room), folding over the first 80 px of scroll (40 px from the tablet up) into a
    56 px bar with a 32 px avatar and a 16 px name; the bar's border fades in, and the page under it never jumps. The member faces sit right under the header. iOS and iPadOS, whose bars
    stay put: the header in the list is centred, the group's square at 88 pt over the name (two lines at most) and
    the faces; over the first 88 pt of scroll the square shrinks to half, fading out between 56 and 96
    pt, and between 100 and 132 pt a 30 pt square and the name fade into the bar's title, rising 8 pt (`GroupScreen`).
    Both squares open the photo big. Lying down with the whole tab to itself (any iPhone,
    by its compact height, an iPad, an iPhone Duo open; issue 388, Pierre's call) the screen is two columns: the header (a 72 pt square,
    which never folds), the faces, the currency chips and "Your balance in this group" in a 328 pt side column, the
    Expenses / Balances / Members picker and the list beside it, in 920 pt at most; an iPhone Duo open puts one
    column on each side of its fold, so nothing crosses it. In the column beside the groups (Groups and Search lying
    down with an iPad's room) and upright the group keeps its one column, 640 pt at most; the Mac
    keeps its 36 pt square beside the faces and the name in the window's title.
  - The group's avatar and name fly from the row or card tapped into the group's header, and back on back or the back
    gesture, following the finger (beta feedback of 2026-11-12): from Groups' list and grid and Home's group cards.
    On Android the balance and its word ("+R$ 313,81", "to receive") fly too, into "Your balance in this group"'s
    amount and word (beta feedback of 2026-11-15); once you are settled the group's screen shows the settled line,
    which has no amount, so only the avatar and name fly. The members' stacked faces fly too, from the rows and cards
    that show them (Groups' list and grid, not Home's cards, which have none) into the faces under the group's header
    (beta feedback of 2026-11-16), scaling from the row's small stack to the header's whatever faces each one picks.
    Android: Compose's shared elements (`SharedTransitionLayout` around Navigation 3, `rememberGroupSharedModifiers`),
    keyed by the screen the group opens from (`SharedOrigin`), so a tab switch between Home and Groups flies
    nothing. That screen is kept while the group is on the back stack and read back once it leaves: back pops the group before
    its exit runs, so read live it found nothing and the avatar and name stopped flying home (beta feedback of 2026-11-14).
    The name also flew home but not in (beta feedback of 2026-11-16): Material's top bar reads its title's baseline as
    it measures, which placed the name once at the header's spot before the flight began, so the flight started there;
    every shared text is placed only for real (`skipsBaselinePlacement` inside `SharedScopes.scaled`). From Home it always flies, the group covering the window at any size; from Groups wherever the group opens
    over the list, while beside it (a tablet lying down, a book) the row stays where it is. So the group opens over
    Groups as over Home, a screen of its own, outside Material's list-detail scene, wherever the two don't show side by
    side. The web: the View Transitions API
    (`document.startViewTransition`, `webApp/src/lib/groupTransition.ts`), naming only the row clicked and the group's
    header for the transition, when the group opens within a second of a click on its row and when back (the app's or
    the browser's) leaves it for Home or Groups; it waits up to 250 ms for the other side to draw and otherwise
    crossfades, and a browser without the API, or one that already animated a swipe back, just switches. iOS and
    iPadOS: the zoom navigation transition (`matchedTransitionSource` and `.navigationTransition(.zoom)`,
    `GroupZoomTransition.swift`), from Groups' rows, grid cards and search results and Home's "Your groups", only out
    of a row tapped in the last second, so a group opened from a bill or Activity never zooms out of a row below it;
    never beside the list nor on the Mac. Reduced motion turns it off everywhere: Android's animations off end it at
    once, the web skips it under `prefers-reduced-motion`, and iOS's Reduce Motion turns the zoom into the system's
    crossfade and stills the header's scale and rise, keeping its fades. iOS's zoom already carries the whole card,
    balance included; the web names the avatar, the name and the faces (where the row shows them: Groups' table and
    grid, not the phone's list nor Home's cards), so its balance crossfades with the page. A bill's
    "Latest expenses" row on Home shows the group's avatar but opens the bill, so the group's avatar flies into the
    bill's top bar instead (below).
  - A bill's category badge, title, total and share fly from its row on Home's "Latest expenses", "All expenses" or
    the group's Expenses into the bill's screen (the badge, the title, the big total and "Your share"'s amount), and
    back on back or the back gesture (beta feedback of 2026-11-15, the group's Expenses asked the same day); the row's
    small group square on Home and "All expenses" flies into the group's square of the bill's top bar. The row's payer
    faces on Home and "All expenses" fly into the faces of "Larissa paid", and the time of a row on the group's Expenses
    ("19:22") into the bill's date and time ("Sat, Nov 14, 14:57"); opened from the group's Expenses, the group's square
    and name in the group's top bar fly into the bill's top bar and back (all beta feedback of 2026-11-16, keyed there by
    the group, `rememberGroupBillSharedModifiers`, as only one bill opens over it). Android only,
    where the bill covers the window (a phone; wider it is a dialog over the screen it opened from, and nothing flies,
    even once the window narrows under it): `rememberBillSharedModifiers`, keyed by the bill and the screen it opened
    from (`SharedOrigin`, `GROUP` for the group's Expenses), the screen under the bill kept as the group's is. The row's
    ⋮ stays. A bill opened from Search, Activity or a category's bills just slides in. iOS, the Mac and the web animate
    no bill between screens (Pierre's call: as the group's transition does), so they keep their own push.
  - The group's avatar in the header (its rounded square, the photo or the initials on the color; the web's page header, the top bar on Android, the header on iOS and the Mac) opens the photo big (Pierre's call, `Route.GroupPhoto`,
    `GroupPhotoViewModel`), as the profile's avatar does ([the profile's viewer](profile.md)): the group's square, 200 pt/dp/px (260 once both sides have 600) over the
    dimmed, blurred group, with "Camera" where there is one, "Gallery" and "Remove" (with a photo) under it. Each saves
    the group's photo at once over the group as it is, offline too, with no Save (the photo goes up with the group's next sync, as in
    Edit group), and the viewer stays open showing the new one; a photo picked opens "Adjust photo" over it first, framed in the group's square, and is still
    saved when the viewer closes while it is prepared. Any member can change it, as in Edit group. ✕, a tap outside, back and Esc close it, and so does the
    group going away. Android draws it as a dialog over the whole window, iOS covers the screen, the Mac opens it as a sheet and the web as a dialog over the group.
    To TalkBack, VoiceOver and the web's screen readers the avatar is "View group photo".
  - Each line of Expenses ends in a ⋮ (⋯ on iOS and macOS) with its options, so they don't need the bill opened
    first; a long press (Android, iPhone, iPad) or a right click (the Mac, the web) on the line opens the same menu.
    A bill's are Bill's own top bar, named as short (Pierre's call), plus "Duplicate" (below), which only the list
    has, in this order: "Edit" (Edit bill over the group), "Duplicate", "Share" (Share bill) and "Delete", which asks
    Bill's "Delete this bill?" in the same words (`GroupDetailUiState.billToDelete`) and stays on the group, the bill
    leaving the list; a payment's is "Delete", which asks "Delete this payment?" at
    once. A tap on a payment never asks to delete: it opens the payment's details (`GroupDetailUiState.paymentToView`:
    who paid whom, the amount, when ("Today, Nov 6 · 10:20 AM", "Yesterday, …", or the weekday and date, as a bill's "When"), how, "queued" while it waits), a bottom sheet on an Android phone, a dialog on
    its tablets and foldables' halves, a sheet on iOS and the Mac, a dialog on the web (a drawer under 768 px, as its other dialogs), with "Delete" in it (the web's
    also "Close"), which closes the details and asks the same "Delete this payment?" (`OnPaymentDeleteClicked`).
    The Mac's items end in "…", and the Mac's table shows the menu only for one line picked, whose Delete
    key (⌫) asks the same "Delete this bill?" or "Delete this payment?". Home's "Latest expenses" and its
    "All expenses" have the same ⋮, long press and right click, their options opening over Home (Pierre's call, beta
    feedback, [home-tab.md](home-tab.md)); Activity has none: its lines open the bill, whose options are a tap away.
    One controller does it for all three lists (`BillMenuController`, `:core:expenses`).
  - Bill's "Paid with" (private payment methods, Pierre's call, feedback of November 14, 2026): the method this
    device's user marked the bill with, with "Only you see this.", under the bill and before "Your share"; nothing
    for a bill they never marked, nor while they aren't among its payers (someone else's edit may have taken them
    out; `BillDetailUiState.shownPaidWith`). Marking it on a bill someone else created goes
    through "Edit bill" (any payer may), whose Save then changes only their mark: the bill isn't saved again, so the
    group sees no edit of it. Bill has no picker of its own.
  - A bill's line, in every list of bills (Expenses, Home's "Latest expenses", "All expenses", a category's
    expenses), ends in your share as its signed amount alone ("+R$ 6,79", "−R$ 9") in green or orange, never with
    "to get back" or "you owe" after it, as Groups' list draws a group's net (beta feedback); screen readers still hear
    the word. Bill keeps it under "Your share". Who paid shows as the app's stack of faces, at most two bubbles
    ([General rules](general.md)); the members' faces under the group's name and who paid on the Bill have room for five.
  - A bill tapped there opens "Bill" (`Route.BillDetail`): the bill, "Your share" and the split over every member,
    with its category over the title and Share, Edit and Delete in the top bar. The top bar names the group with its
    rounded square before the name (its photo, or its initials on its color, beta feedback of 2026-11-15,
    `BillDetailUiState.groupInitials`, `groupColor`, `groupPhotoUrl`): Android's top bar (32 dp) and dialog header, the
    iOS and iPadOS bar's title (30 pt, as the group's folded bar), the Mac inspector's line under the bill's name (18
    pt) and the web sheet's header (24 px). It shows once the bill is read, never as an empty square. The square is a button too (Pierre's call, beta
    feedback of 2026-11-16, `BillDetailUiEvent.OnGroupPhotoClicked`): it opens the group's photo big, the viewer of
    the group's header (`Route.GroupPhoto`), over the bill, so back or ✕ comes back to it, with 48 dp/44 pt/44 px to
    tap around the smaller square and "View group photo" to TalkBack, VoiceOver and the web's screen readers. The bill
    is the only other screen with the group's avatar in a bar (Edit group's is the field that picks the icon;
    Statistics and the rest name the group in words). Android's flying avatar is unchanged: the badge keeps its
    shared element inside the button. A phone opens it full screen; Android tablets open it as a dialog (a book's right half, a tabletop's
    half above its hinge), the iPad as a sheet (820×640 lying down, 620×860 upright, `billSheetSizing`), the Mac as the
    main window's inspector (720 pt, 330 pt in a window under 1180 pt, the bill beside its split from 600 pt, ✕ and Esc
    closing it, another bill or whatever the group opens taking its place; the 330 pt one puts Share, Edit, Delete and
    ✕ in a row above the bill's name, which beside them would be cut), and the web as a side sheet (a drawer on a
    phone), all over the group.
  - Bill's "Comments" (7.3c to 7.3f): anyone in the group comments, offline too (the comment waits in the queue,
    "Queued · goes out when you're back online", like a bill, `bill_comments`), and "Reply" answers a comment or a reply, one
    level deep: a reply to a reply joins its thread and names whom it answers. "Send comment" stays disabled
    until something is typed (1 to 1000 letters); the composer is pinned at the bottom on a phone and under the
    comments in two columns. Offline, a banner says "No internet. Comments and photos are queued and go out when you're back online.". Each comment (beta feedback 14, Pierre's call) has a heart and a menu (⋮/⋯, the row's long press
    and right click): the heart likes it or undoes the like, one per person, offline too through the queue
    (`bill_comment_likes`, the newer toggle wins), with the count beside it, which opens "Likes" (who liked it, in
    the order they did); "Share" sends "Ana commented on “Dinner”: “…”" with the comment's
    link through the system share, offline too; "Edit" and "Delete" only on the user's own. Edit puts the text in
    the composer ("Editing comment", ✕ gives back what was typed, "Save"), and an edited comment says "edited"
    after its time, with no history; Delete asks "Delete comment?" first. Both work offline through the queue like
    a new comment: the newer edit wins on the server (`edited_at`), a deletion is a tombstone that wins over any edit,
    and only its author may do either (the server's RLS). A deleted comment with replies stays as "Comment deleted"
    over them; one without replies, and a deleted reply, go. Edits, deletions and likes record no Activity event and
    push nothing: Comments' row shows the text as edited, and "Ana deleted a comment on Dinner" once deleted. A
    comment's link, `https://blitzsplit.com/b/<bill>?comentario=<comment>` (`BillCommentLink`, shared with
    `&lang=<the app's language>` so its preview, which shows nothing of the bill, speaks it), which the apps claim
    (App Links, Universal Links) and the web opens, opens the bill's Bill screen over its group's Expenses, over Groups,
    scrolled to the comment and highlighted, after a sync of a few seconds, since the comment may be newer than the device's copy
    (`Route.BillDetail.focusCommentId`); a deleted comment opens Bill with "That comment was deleted.", a deleted
    bill its group's Expenses with "This bill was deleted", and someone outside the group (or without a profile)
    gets "You're not in this group" with "Go to home" (offline: "No internet", `BillLinkNotice`). Once the person has moved
    on from Groups while the link was being checked, it opens and says nothing, so nothing covers what they opened. A comment's
    tap in Activity scrolls Bill to it too. "History" lists what was done to the bill, by day,
    the most recent first, never the comments: created ("Equally among 5 · R$ 312", "created the bill offline"
    while it waits to go up), each edit's first change with the others under it ("changed the category", "changed the
    split" with each part that moved; an edit of the currency reads "changed the currency" with "BRL → USD", which the
    server records since `bill_edited` carries a `currency` change, an older line without one reads as unchanged),
    photos added in a row as one line, a photo removed and the share link changed. It reads the device's 90 days of Activity and, online, the bill's whole history from the server
    (`LoadBillHistory`). "Undo" shows only on the latest line when it is an edit, the bill is still as that edit
    left it, isn't waiting to go up nor split by items, and saves the edit's "before" as a new edit
    (`BillHistoryEntry.canUndo`), the previous currency together with the previous amount (BRL 100 edited to USD 20
    undoes to BRL 100). Every amount of Bill and of the share sheet is in the bill's own currency, never the group's.
    A history line writes its amounts in the bill's current currency, also those from before a currency edit.
  - Bill's "Attachments" (7.3g): any member adds up to 5 photos to a bill (the camera, the gallery, the Mac's files,
    the web's files or its camera), offline too ("Queued" until the photo is up, drawn from the device meanwhile), and removes any of
    them from the viewer ("Remove photo", no question, as the boards draw none). "Attachments · 2 of 5 photos" lists the
    receipt the bill was read from first (badge "Receipt", never removed from Bill; reading it is 7.1e), then the photos
    as "Photo 2" with who added it and when, since the server keeps no caption, then "Add photo" while there is
    room. A thumbnail shimmers while its photo is on its way, online: its URL being signed
    (`BillAttachmentTile.isImageLoading`, asked again each minute after a failure, as the viewer's spinner) and then the
    image downloading (beta feedback part 3, item 21); offline, or once the image itself failed, it shows the
    receipt's or the photo's icon instead. The viewer pages through them full screen ("Photo 1 of 2", "Ana attached it yesterday at 22:40") with Download (the
    gallery on Android, the share sheet on iOS, a file on the Mac and the web). Each photo is an upright JPEG of at most
    1600 px under 1.5 MB (`BillImageEncoder`), in the private `bill-attachments` bucket, read through signed URLs, so a
    photo added on another device shows only online. History says who added or removed them; the feed doesn't.
    A photo taken or picked through "Add photo" opens "Adjust photo" first ([Profile](profile.md)), framed in the
    tile's rounded square (Pierre's call, beta feedback of 2026-11-16: "square like the preview"), and only "Done"
    attaches it: the square the frame holds, cut from the editor's photo (already upright and at most 1600 px, as
    any bill photo), at as many pixels as that photo has there (its shorter side divided by the zoom, never blown
    up), under the same 1.5 MB (`PrepareBillPhoto`, `BillCropEncoder`); ✕, Cancel or back attach nothing. The tile
    spins while the photo is read and while the square is kept. The receipt Scan receipt attaches is not framed: it
    keeps the whole image it was read from.
  - Bill's "Share bill" (7.3h, `Route.ShareBill`, `ShareBillViewModel`): the bill's link,
    `https://blitzsplit.com/c/<code>`, one per bill, whose code the server makes (`set_bill_link`). Opening it makes
    the link as "Only people in the bill" when there is none (the safer of the two, which History records as the link
    turned on); "Who can open it" switches it to "Anyone with the link" and back, keeping the code, and
    "Turn off link" turns it off with no question and closes. Online only: offline it says "Sharing needs the internet." while Copy and "Send link" (the system share) still work once the link is known; a bill not up yet
    fails with "Try again". Laid out as Contacts' "Invite with link or QR": a bottom sheet on an Android phone,
    a 560 dp dialog on its tablets (a book's right half), a sheet on iOS (a 540 pt form sheet with an iPad's room) and
    the Mac, a 480 px dialog on the web (a drawer on a phone). The link opens the bill's own page (`Route.SharedBill`,
    page 7.3h) in the apps, which claim `/c/` links as they do `/g/`, `/b/` and `/u/`, and on the web, over wherever the
    app is (the welcome without a profile), as an invite does. Every platform draws it with Bill's own pieces, read-only
    (no Edit, Delete, History, nor adding or removing photos): a full screen on Android (one centered column upright
    with the composer pinned at the bottom; two columns lying down and on a book, one on each side of its hinge, with
    the composer under the second), a pushed page on iOS and iPadOS (Bill's
    columns, a centered column on the iPad), a centered column of the main window on the Mac and of the page on the
    web. It shows the bill, who paid, each one's part, the attachments and the
    comments, never the balances nor the group's other bills, under "This bill is from the group X" (the server doesn't
    say who shared it, and a group's own category shows as Other). "Only people in the bill" opens it to the group's
    members signed in; "Anyone" to anyone, and any device with an account (an anonymous one too) comments
    there, in the group or not, online only, and likes, shares (the page's link at the comment, `/c/<code>?comentario=`,
    which scrolls to it) and edits and deletes their own comments there, online only. "Sign in" goes through the app's own sign in (Welcome without a
    profile, else Profile) and the link opens again when the browser's Google or Apple sign in comes back within 15
    minutes, or once Welcome puts Home in place ("Use without an account", or a Google or Apple sign in there), on
    every platform (the page tells `SharedBillLeftForSignIn` it left to sign in, so one left by going back never opens again;
    the web also keeps it across the page load of a browser sign in). A link only the group opens
    offers "Sign in" until the device has a Google or Apple account; a member with the bill on the device gets "Open in the app".
  - Currencies (Pierre's call: the currency belongs to each bill and payment, see [General rules](general.md)): while the
    group has money in more than one currency, a chip row with each currency's code sits in the header and over
    Balances (`GroupHeader.currencies`, `OnCurrencySelected`), the group's default currency first and then the one with
    the most bills; "Your balance in this group" and Balances' pairs are the chip's currency
    only (`GroupHeader.currency`), and "Everyone is settled up" and "Delete group" wait for every currency to be
    settled. Each bill row and payment row writes its amounts in its own currency (`ExpenseRow.currency`,
    `PaymentRow.currency`), as do the bill's details and the "Search" and Home bill rows. Statistics have their own chip
    row and read the bills and payments of that currency only; its category list opens the bills of that currency.
  - "Statistics" (6.6, `Route.GroupStats`) opens from the chart button on the group's bar (Expenses and Balances;
    "Statistics" with text on the web from the tablet up), with Back and no "Split a bill". On Android its bar is
    small, with "Statistics" and the group in it rather than in the body, and leaves with the content on scroll (iOS
    and the web keep their native titles). It counts only the bills on the device, those in the queue too ("7 expenses, 1 queued", the amber dot),
    never the payments of Balances (but in "Settling up"), so it works offline. Periods (only this screen keeps one): a trip lists "The whole trip" (the default, from its first
    to its last day with a bill), "This month", "Last month" and "Pick dates…"; any other group "This year", "This
    month", "Last month", "All time" (the default, as the notes say) and "Pick dates…" (each platform's date
    range picker, never after today; Android's headline keeps each date whole and puts the end date under the start
    one when both don't fit, "25 Oct 2026 –" then "14 Nov 2026", Pierre's feedback of 2026-11-15, where Material's
    wrapped it a letter or two per line; iOS and the web ask "From" and "To" in fields of their own). The period
    pull-down never wraps its name (beta feedback of 2026-11-15: iOS broke "All time" in two beside its dates): on iOS
    the dates move under it when both don't fit on one line, in any language and text size, and only a pull-down
    wider than the screen by itself, at the largest text, cuts its name with an ellipsis. Total spent, how many bills, "Average per person" (the total over every member,
    who hasn't joined included), "Your share" and "You paid", on a neutral card under a neutral period chip on every
    platform (a pale blue fill read as one more slice of the ring);
    "By person" shows Consumed (each part as each bill
    split it) or Paid (a segmented choice with no check on the one picked, beta feedback: Android's connected
    buttons, Default split's included, say it by the filled pill alone, as the group's tabs, iOS and the web do), the largest first, someone who left the group as "Former member".
    It is a donut over the list (Pierre's call, beta feedback of 2026-11-16, in place of a bar per person): "By
    category"'s ring, at 160 dp/pt/px centered on its own card, its slices each person's share
    (`StatsPersonRow.percent`, rounded to add up to 100 as the categories' are) and its middle the largest one, "39%"
    over "Ana", or the one picked (a tap on a slice, a second tap letting go, as does a tap anywhere else on its card
    or in the hole, Pierre's call, beta feedback of 2026-11-16; with a mouse, the slice or the row it is over), the
    others fading. When that one is this device's user (`StatsPersonRow.isMe`) the middle says just "You",
    the row under the ring keeping "Pierre (you)" (beta feedback of 2026-11-16). The middle of both
    rings stays inside the hole with 10 dp/pt/px to spare on each side of the ring's inner edge, so a long name ends
    in an ellipsis instead of touching the ring. Consumed and Paid switch what it shows, and it sweeps in again with them and the period.
    Each person takes Default split's color by their place in the group's list (`StatsPersonRow.color`,
    `SplitBarColors`, [members-and-payments.md](members-and-payments.md)), so they read in one color on both screens,
    a former member the neutral Gray; their row under the ring is its legend: the face, a dot of that color, the name,
    the share and the amount, and still opens their profile. One person with everything fills the whole ring with no
    gap; while nobody has an amount it is a bare neutral ring with nothing in its middle. TalkBack and VoiceOver read
    it as "Split by person: Ana 39%, Bruno 33%, …" and each row as "Ana: 39% of the total, R$ 350,00". "Settling up" (Pierre's call, beta
    feedback 12, `GroupStatsUiState.settling`, `SettlingCalculator`) names under it who "Settles fastest" and who
    "Slowest to settle" ("2 days on average", "same day on average", "R$ 80 still open"): each member's bills and
    payments of the group's whole history, the queue's too, go in date order, what a bill puts on them opening a
    debt on its "When" and what they pay (a payment, or a later bill they paid more of than their part) covering
    the oldest first, a debt still open counting up to today; only debts born of the period's bills count, the amount
    weighting the average, whoever is still in the group (guests too, never an ex-member), and only once two people
    had one and the two named don't tie (the same days and the same open amount hide the card); whoever covered all of
    the period's debts ranks before whoever still has some open, so a part put on someone today never reads as fast. "Over time" is Bars or
    Line by Day, Week (Monday to Sunday), Month or Year, kept per device for every group and never synced
    (`StatsChartPreferences` in `device_settings`): Day only up to 3 months, the others once the period touches two of
    them, Year once the group's bills touch two years, and picking it switches to the whole period; a grain kept that
    doesn't fit opens on Day (up to 3 months) or Month, the disabled ones say why under the chart (`StatsGrainNote`).
    Under the bars a day is its number ("28", "today"), a week its first day ("Oct 19"; its whole range, "Oct 19 –
    25", overlapped the next one's under a phone's chart, beta feedback of 2026-11-14, so it stays in the callout and
    the "Most expensive week" line, whose amount wraps to the next line whole), a month its short name and a year its
    number. The shared code marks which bars carry a label (`StatsBar.showsLabel`); each UI then, working back from the last one, leaves out
    any label that would touch the one after it, so the last one always shows and no two ever overlap, at any
    width, font size and language (`StatsChartLabelsRow`, Swift Charts' greedy collision resolution, `StatsChartLabels`).
    The chart reads against up to three dashed value lines at round steps from 0 ("R$ 500", "R$ 1 mil", "R$ 1,5 mil",
    `GroupStatsUiState.chartTicksMinor`), their short amounts on the end side, the top one being the scale's top, and
    a dashed "Average per day" (week, month, year) line: the mean of every bar, the empty ones too
    (`chartAverageMinor`), named with its amount under the chart, after the most expensive one. The pointer over that
    legend row (the web, the Mac), or tapping or clicking it (a long press too on Android), opens a hint saying
    the darker dashed line across the chart is the average of every period shown, the empty ones included. A click or
    tap keeps the hint open until it is dismissed (a click on the row again, or one elsewhere), and the pointer leaving only closes a hint it opened by hovering;
    the line on the chart stays non-interactive so it never blocks picking a bar. A bar or point picked
    shows a callout over it with its period, amount and bills ("Oct 28", "R$ 1,458", "3 expenses", or "No
    expenses"), the other bars fading and the line drawing a rule and a bigger point: the mouse or trackpad over it
    (the web, the Mac, Android and iPad with a pointer; a click keeps what hovering picked), a tap that keeps it until
    a second tap (or a tap on another one), a sideways drag running along the bars (a mostly vertical one scrolls the
    page and picks nothing), and the arrow keys on the web, the Mac and Android (Home, End and Escape too, but on the
    Mac). Each new bar picked by touch ticks the haptics; VoiceOver plays the series as an Audio Graph, and the web
    reads the picked bar aloud. The bars grow from the base, and the line draws in from the start side, when the
    grain, the period or the chart type changes; with reduced motion on (Reduce Motion on iOS and the Mac, the
    browser's setting on the web, animations off on Android) the chart comes in without animating.
    Then the most expensive one, and "Biggest expense", which opens its Bill and draws its category's badge as the
    group's Expenses draws the bill (its subcategory when it has one, "Other" for none). A group without bills says "Nothing to show yet" with "Split a bill"; a period without bills offers "See the whole trip". "By category" draws the
    period's spending as a ring in each main category's color (its own, or the one the person gave it), the largest in its center, then each main
    category with its amount and its bar (the ring, the legend's dots, the bars and All expenses' category badges in the
    color's data tone, a person's bar in their face's, never the accent), and no list of subcategories under it, which would only crowd the row
    (`StatsCategoryRow`, whole percentages adding up to 100), a bill without a category counted in Other and one of
    a deleted category in its parent or Other.
    The slice the mouse is over, or its legend line hovered (focused, on the web), pops out while the others fade,
    and the ring's middle names it with its share instead of the largest category. A tap or a click on a slice, a
    legend line (which makes even a 2% slice easy to reach) or a category's row, which ends in a chevron, opens "All
    expenses" with the group, that category and the period picked (below); a tap in the hole, or off the ring, lets a
    picked slice go (only a mouse picks one here: a tap on a slice opens it). The ring sweeps in when the period changes; with reduced motion on it comes in without animating,
    and the picked slice pops out at once.
    While a new device's first download still brings bills down it counts what came, as "Total spent so far"
    (5.1d, `GroupStatsUiState.isFirstDownload`). A phone, a tablet and a foldable upright
    scroll one column; lying down (a phone, a tablet, in the group's pane beside the groups too) the total and "By
    category" take the first column and the rest the second: on Android each scrolling on its own, "Over
    time" on top of the second, then "By person", "Settling up" and "Biggest expense"; on iOS both in one scroll, "By
    person" and "Settling up" first, as each board draws it. A book puts the same two columns one per half of the hinge; a tabletop (Android)
    a small bar with the period, the total and "Over time" side by side in the top half, against the hinge,
    and "By category" beside "By person" and "Settling up" in the bottom half, scrolling, without the grains' note nor
    "Biggest expense". The web from the tablet up and the Mac put "Over time" and "Biggest expense" in a second
    column, with four summary cards, "Settling up" under "By person"; the Mac's cards go two by two where four in a
    row would be under 170 pt each, and each keeps two lines for its note ("38% of the total · you paid R$ 1.234,56").
  - A category of "By category" opens "All expenses" ([home-tab.md](home-tab.md), `Route.AllExpenses` with its
    `categoryId` and `period`) over Statistics, back returning to them: the group, that main category and the period
    Statistics showed already picked in its filters, which lead its row of menus in that order (period, category,
    group; Pierre's call, beta feedback of 2026-11-16). It took the place of "Expenses by category", a screen of its
    own that listed the same bills with category chips: All expenses has the search built in and saves a screen. "The
    whole trip" or "All time" goes as the whole history, "This year", "This month" and "Last month" as themselves
    (they follow today there too), picked dates as their days. Statistics' currency chip doesn't go: All expenses
    lists the bills in every currency, each in its own. Opened from Statistics it lists every bill of the group, a
    bill only others shared included (with no share of yours under its amount), as "Expenses by category" did, so the
    list adds up to what the chart counted (Pierre's call, 2026-11-16), while its Group filter holds that group alone;
    any other pick of groups, and Home's or a group's "See all", list only the bills you paid or take part in. All expenses is a page on every size, as from Home or the
    group's "See all": on a tablet lying down it covers the groups' pane rather than drawing in it, and on the web and
    the Mac it keeps its own title and back rather than a breadcrumb.
  - "Duplicate", only in a bill's ⋮ on Expenses (Bill's top bar has no such button), opens "Split a bill" as a new bill
    in the same group (`Route.SplitBill.duplicatesExpenseId`) filled in with the original's description, amount,
    currency, category, payers and split, spent now, to change whatever the person wants before saving; nothing is
    saved until then, and "Back" leaves the original alone. Its attachments, comments and history are not copied, nor
    are people who left the group. A bill in a currency this version doesn't know can't be duplicated, as it can't be
    edited. As in any new bill, the group can still be changed ("Split in which group?"), which starts payers and
    split over among that group's people. Mac: "Duplicate…".
  - Any member may edit or delete a bill. "Edit bill" is "Split a bill" filled in (`Route.SplitBill` with the
    bill's id), keeping who created it and when, with "Anyone in the group can edit or delete it." and "Delete
    bill"; saving goes back to Bill. Members who left the group are dropped from the edited split, but a payer who
    left stays its payer ("Former member") until someone else is picked, and someone who joined while it was being
    edited starts out of an Equal split. Last-Write-Wins:
    when someone else's save syncs while editing, a banner says who changed it and when
    (`SplitBillUiState.conflict`, the server's `expenses.updated_by`), what was typed stays, and saving wins.
  - Delete asks first: "“X” leaves the group and everyone's share of it stops counting.", plus "You no longer owe/get back R$ Y" while the bill moves the device user's balance (`BillDeletion`). It deletes offline as a
    tombstone and goes back to the group, past Bill. A bill someone else deleted while open closes, and Group
    expenses says "This bill was deleted" once it is on top (`GroupDetailUiState.showsBillDeletedNotice`); never
    for a bill this device deleted (`NoticeBillGone`), nor for one Home or All expenses opened, with no group under it
    (`Navigator.isOverGroupScreen`): it closes back to where it was opened, without a word.
  - Editing or deleting a bill after a payment (7.4b) is allowed: the payment still counts and the balance is redone.
    While the change typed in "Edit bill" moves the balance of a pair with a payment dated at or after the
    "When" being typed (the saved one for "Delete"), the form says so before saving, live as it is typed, so moving
    When past a payment or before it changes what it says: "Elisa already settled up with Bruno after this bill. The payment still counts and the balance is worked out again: with the change, Elisa owes Bruno R$ 20 again." ("starts owing" the
    other way round or for more of what was still owed, "is left owing Bruno R$ 30" for less of it, the same way
    round, "are settled up" when they end up even; "you are left owing R$ X to Bruno" and "Bruno is left owing you R$ X"
    with this device's user), naming only the latest such payment (`SettledPairChange`, the group's `GroupBalances`
    with the saved bill and with the change). It takes the place of "Anyone in the group can edit or delete it.", as a card above "Delete bill" on Android and a section above it on iOS, at the top of the form on the
    Mac and the web; a screen reader says it when it shows and when the pair or how it ends up changes, never on each
    digit of the amount (`SettledPairChange.announcementKey`). Save saves at once, with no extra question. "Delete
    this bill?" ends with the same sentence, from Bill too (`BillDeletion.settledPair`). A payment dated before the
    bill, or one whose pair the change doesn't move, says nothing.
