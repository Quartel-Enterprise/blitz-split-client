# The group screen and its bills

Group expenses, Bill, comments, attachments, sharing, Statistics, editing and deleting a bill.

  - A group tapped on the home opens "Group expenses" (`Route.GroupDetail`): the bills by day, the most recent on
    top, with "Split a bill" (Android's extended FAB, iOS's glass toolbar item, a primary button on macOS, ⌘N, and the
    web), the group's main button on every tab, Members included, in the same place. Its tabs Expenses / Balances / Members are one screen (`Route.GroupDetail.tab` only says which it opens on):
    the header, "Your balance in this group" and the tabs stay, only what is under them changes, with no transition nor
    check on the tab picked (`GroupDetailUiState.tab`), the back stack never moves, and back from any tab leaves the
    group. Invite's "Go to the group" and "See who joined" open it in Invite's place (`openGroup`), so the
    group never stacks over itself; the one exception is "Split a bill"'s group, which opens the group over the form
    as an entry of its own (`Route.GroupDetail.isOverBill`, see [Split a bill](split-bill.md)), and whose "Split a
    bill" goes back to that new bill's form rather than opening a second one. On Android the top bar's subtitle ("2
    members · R$ 3,418.31 in total") ellipsizes 16 dp short of the bar's actions, so the Statistics button keeps room
    around it (beta bugs of 2026-11-10). On an Android phone upright that bar folds to its one line on a scroll down
    and opens again on a scroll up, from anywhere in the list; wider, it opens only back at the top (see
    [the bars on scroll](home-and-shell.md)).
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
    its tablets and foldables' halves, a sheet on iOS and the Mac, a dialog on the web, with "Delete" in it (the web's
    also "Close"), which closes the details and asks the same "Delete this payment?" (`OnPaymentDeleteClicked`).
    The Mac's items end in "…", and the Mac's table shows the menu only for one line picked, whose Delete
    key (⌫) asks the same "Delete this bill?" or "Delete this payment?". Home's "Latest expenses" and
    Activity have no ⋮: their lines open the bill, whose options are a tap away (Pierre's beta feedback asked for
    the group's list, where the bills are managed).
  - A bill tapped there opens "Bill" (`Route.BillDetail`): the bill, "Your share" and the split over every member,
    with its category over the title and Share, Edit and Delete in the top bar. A phone opens it full screen; Android tablets open it as a dialog (a book's right half, a tabletop's
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
    comment's link, `https://blitzsplit.com/b/<bill>?comentario=<comment>` (`BillCommentLink`), which the apps claim
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
  - Bill's "Share bill" (7.3h, `Route.ShareBill`, `ShareBillViewModel`): the bill's link,
    `https://blitzsplit.com/c/<code>`, one per bill, whose code the server makes (`set_bill_link`). Opening it makes
    the link as "Only people in the bill" when there is none (the safer of the two, which History records as the link
    turned on); "Who can open it" switches it to "Anyone with the link" and back, keeping the code, and
    "Turn off link" turns it off with no question and closes. Online only: offline it says "Sharing needs the internet." while Copy and "Send link" (the system share) still work once the link is known; a bill not up yet
    fails with "Try again". Laid out as Contacts' "Invite with link or QR": a bottom sheet on an Android phone,
    a 560 dp dialog on its tablets (a book's right half), a sheet on iOS (a 540 pt form sheet with an iPad's room) and
    the Mac, a 480 px dialog on the web (a drawer on a phone). The link opens only on the web (`Route.SharedBill`,
    page 7.3h), since the apps claim only `/g/`, `/b/` and `/u/` links: the bill, who paid, each one's part, the attachments and the
    comments, never the balances nor the group's other bills, under "This bill belongs to the group X" (the server doesn't
    say who shared it, and a group's own category shows as Other). "Only people in the bill" opens it to the group's
    members signed in; "Anyone" to anyone, and any device with an account (an anonymous one too) comments
    there, in the group or not, online only, and likes, shares (the page's link at the comment, `/c/<code>?comentario=`,
    which scrolls to it) and edits and deletes their own comments there, online only. "Sign in" goes through the app's own sign in (Welcome without a
    profile, else Profile) and the link opens again when the browser's Google or Apple sign in comes back within 15
    minutes, or once Welcome makes the profile on the page ("Use without an account"). A link only the group opens
    offers "Sign in" until the device has a Google or Apple account; a member with the bill on the device gets "Open in the app".
  - Currencies (Pierre's call: the currency belongs to each bill and payment, see [General rules](general.md)): while the
    group has money in more than one currency, a chip row with each currency's code sits in the header and over
    Balances (`GroupHeader.currencies`, `OnCurrencySelected`), the group's default currency first and then the one with
    the most bills; the header's total spent, "Your balance in this group" and Balances' pairs are the chip's currency
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
    range picker, never after today). Total spent, how many bills, "Average per person" (the total over every member,
    who hasn't joined included), "Your share" and "You paid", on a neutral card under a neutral period chip on every
    platform (a pale blue fill read as one more slice of the ring);
    "By person" shows Consumed (each part as each bill
    split it) or Paid (a segmented choice with no check on the one picked, beta feedback: Android's connected
    buttons, Default split's included, say it by the filled pill alone, as the group's tabs, iOS and the web do), the largest first, someone who left the group as "Former member". "Settling up" (Pierre's call, beta
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
    grain, the period or the chart type changes.
    Then the most expensive one, and "Biggest expense", which opens its Bill. A group without bills says "Nothing to show yet" with "Split a bill"; a period without bills offers "See the whole trip". "By category" draws the
    period's spending as a ring in each main category's color (its own, or the one the person gave it), the largest in its center, then each main
    category with its amount and its bar (the ring, the legend's dots, the bars and Expenses by category's chips in the
    color's data tone, a person's bar in their face's, never the accent), and no list of subcategories under it, which would only crowd the row
    (`StatsCategoryRow`, whole percentages adding up to 100), a bill without a category counted in Other and one of
    a deleted category in its parent or Other.
    The slice the mouse is over, or its legend line hovered (focused, on the web), pops out while the others fade,
    and the ring's middle names it with its share instead of the largest category. A tap or a click on a slice, a
    legend line (which makes even a 2% slice easy to reach) or a category's row, which ends in a chevron, opens that
    category's bills, "Expenses by category" (Pierre's beta feedback, `Route.CategoryExpenses`, below); a tap in the
    hole does nothing. The ring sweeps in when the period changes.
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
  - "Expenses by category" (`Route.CategoryExpenses`, `CategoryExpensesViewModel`), from a category of "By category":
    a screen of its own over Statistics, back returning to them, rather than the group's Expenses filtered (whose
    header, tabs and payments aren't about a category, and whose back would leave the statistics). Its subtitle is
    the group and the period Statistics showed ("Viagem Floripa · October 2026"); "The whole trip" or "All time"
    follows new bills, any other period stays those days. On Android its bar is Statistics's: small, with the title
    and subtitle in it, leaving with the content on scroll. Chips list the period's main categories, the largest first
    as "By category" does, the one tapped picked: a tap picks or lets go of more (several at once), "Clear" shows while
    one is picked, and with none picked the list shows every bill of the period; a category picked with no bill left
    stays as a picked chip, over "No expenses in these categories"; with none picked and no bill left in the period it
    says "No expenses in this period", and while a new device's first download may still bring the bills, skeletons
    take the place of either. A main category takes its subcategories' bills
    (Food takes Restaurant and Market), and there are no subcategory chips: each line names its own subcategory in its
    meta ("21:30 · Bruno paid · Restaurant"). Under the chips "R$ 610 · 4 expenses" adds up the bills shown, then the
    bills by day as the group's Expenses draws them, never the payments, with no ⋮ (as the home's "Latest
    expenses": a tap opens Bill, as from Expenses, where its options are). The chips are only this screen's: opening it
    again starts from the category tapped. It lays out as Statistics: in the group's pane beside the groups on an
    Android tablet and an iPad lying down, one column of up to 624 dp elsewhere on Android (a book's chips before the
    hinge and the bills after it), a List on iOS and the Mac, a page in the home's frame on the web, under the
    breadcrumb "Groups › Viagem Floripa › Statistics › Expenses by category" from the tablet up (the group's crumb
    going back to the group past Statistics), with the group's Expenses table. Its subtitle writes the period as
    Statistics did: "This month" and "Last month" as the month, picked dates as their days.
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
    for a bill this device deleted (`NoticeBillGone`).
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
