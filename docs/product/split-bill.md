# Split a bill

Splitting a bill: payers, Split, Items, categories, Category map, receipts and choosing the group.

  - The bill's currency (Pierre's call: the currency belongs to each bill, see [General rules](general.md)): a
    currency control beside the total, the code ("BRL") with a menu that is New group's "Currency" list, the
    cryptocurrencies under their own header (`SplitBillUiEvent.OnCurrencyPicked`; Android a menu on a chip, SwiftUI a
    menu "Currency" row under the total, the web a menu button, labeled "Currency: BRL. Change it" for a screen
    reader). A new bill opens in the group's currency (`Group.currency`, only the default), a bill being edited, and
    a duplicate of a bill (bill's "Duplicate", `Route.SplitBill.duplicatesExpenseId`: a new bill filled in once with
    the original's description, amount, category, payers and split, spent today without a time), in its own (`Expense.currency`); the
    state's `currency` is the one source, so saving an edit never moves the bill to the group's. Nothing remembers the last pick: the next bill opens in the group's again, and a bill moved to
    another group starts in that group's. Changing it re-bases the form as moving the bill to a group of another
    currency does: the typed total, "Amounts" values and what each one paid keep their number with the decimals cut
    (`TypedText.toCurrency`) and follow the new currency's decimals and cap (bitcoin whole sats while "Bitcoin in
    satoshis" is on), and a split by Items starts over, since its prices were in the old minor unit. A payment
    recorded for the bill's balance later is in the same currency. A bill in bitcoin or ether offers no "Scan
    receipt". The control is disabled while a receipt is being read or the bill is saving.
  - "Split a bill" (`Route.SplitBill`): "Who paid" picks one person, who paid the whole bill, or "Several people",
    which opens "Several people paid" in its place: each person's amount, adding up to the total exactly ("R$
    312 of R$ 312. Adds up.", else "R$ X left" / "R$ X over"), and Done waits until it does
    (`SplitBillUiState.severalPayers`, `payerAmounts`). A new total keeps what each one paid, and Save waits until
    someone makes it add up again; one person holding the whole total is one payer again. "When"
    picks the date, and the time is optional (Pierre's call, beta feedback, `SplitBillUiState.hasSpentTime`): a new
    bill (and a duplicate) is today with no time, which is saved as the first instant of its day, 00:00 on the device
    (`SpentDay.hasTime`, so a time picked as 00:00 is no time either), and shows the date alone ("Today, Nov 10").
    Picking a date never chains into the clock and keeps the time the bill has, or its lack of one
    (`OnSpentDayPicked`); the time has its own control (`OnSpentTimeOpened`, `OnSpentTimePicked`,
    `OnSpentTimeCleared`): Android's "Add time" at the end of the row, or the time itself, opening the clock, which
    offers "Remove time" while there is one; iOS's "Time" switch with its picker beside it on its own row, the Mac's
    "Time" checkbox with its field; the web's "Add time" in the popover, then its time input with "Remove time". A bill
    saved with a time keeps showing and editing it. Never after now: a later time today becomes now. Expenses' rows,
    a category's bills and Bill (and the web's bill page) leave the time out of a bill without one ("Bruno paid",
    "Today"). The form asks what it was before how much (Pierre's call, beta feedback
    10): "Description" with its category's icon and the line saying why, then "Amount", in every layout, a new bill
    opening on the description. A bill is 0.01 to 100 million in its own currency (but the bitcoin, ether and rupiah
    caps of `ExpenseRules.maxAmountMinorFor`; a new bill starts in the group's default currency, and "Edit bill" keeps
    the bill's own, whatever the default became since) with a
    description; Save stays disabled until both are valid and the split adds up, and saving goes back to the
    group's screen it was split from, or opens the bill's group's Expenses in its place. "Split" splits by Equal,
    Amounts, % (whole numbers adding up to 100), Shares or Items, and only changes the bill on "Done". A new bill starts
    from the group's "Default split" (By income as its percentages) and follows it, and a sync's change of it,
    until "Done" picks its own (another group picked starts over from that one's); percentages someone who left
    took part of start Equal among everyone. Someone who joins while Split is open and still on the default gets
    the default's part (1 part, in Equal, 0%), so it stays "Default"; once changed there, they start as in any split
    (in Equal, out of the other modes). "Split" then says "Group default", and Split's notice above the
    modes "Apt 302 default: Shares, 3 to 1", or "Different from the group's default" with "Back to default" once
    changed for this bill (`SplitBillUiState.defaultSplit`, `isSplitEditorOffDefault`); "Edit bill" keeps its split
    with no notice. A new bill split by Equal, Shares or % in another way than that default (or, for a group without
    one, anything that doesn't split equally among everyone, Shares at 1 each included) offers "Use as the group
    default" under Split's people (beta feedback 11, Pierre's call, `offersSplitAsDefault`; never Amounts nor Items,
    which are one bill's amounts, nor on "Edit bill"): "Done" with it checked makes "Split" say "Becomes the group
    default", and saving the bill saves its split as the group's "Default split" (`SaveSplitAsDefault`), keeping its
    exceptions, as Default split's Save does (offline too, Last-Write-Wins, a "By income" replaced dropping this
    device's incomes). A bill whose category has an exception, even one that no longer adds up after someone in it
    left (`defaultSplitExceptionId`), offers "Use as the group default for Groceries" and replaces only that exception
    ("Becomes the group exception for Groceries"). Closing the bill unsaved changes nothing; another group or category
    picked (Pierre's call: another category falls under another default) or Items' "Done" drops it, and the
    checkbox unchecks once it hides. The cents a split can't share evenly go to whoever paid, while they are in the split: one each
    to the payers in it, in the group's order, around again while some are left (one payer keeps them all).
    Changing the total redoes an "Amounts" split proportionally. Payers and shares live inside the bill (one
    Last-Write-Wins row). Bill, Expenses and Split name up to two payers ("Ana and Bruno paid", the device's
    user first, `PayerOrder`) and count more ("3 people paid"), and Split marks each of several payers "Paid part
    of the bill"; Bill's "Your share" names whom the bill puts it on, as Balances does
    (`GroupBalances.findDebtsWithin`). On a fold (flow 7) Split a bill, Split, Items and Edit bill open in a
    book's right half with the group under the scrim, "Who paid", "Several people paid", "Split in which group?",
    When's date (in the book only) and every confirmation in that half too; a tabletop splits a new bill in two
    full screen (`TabletopForm`): the description and the amount, Split's modes or the items on top, the rest and Save or Done
    below, Item and Category opening above the hinge, while Edit bill and Bill open as the book's dialog. The
    iPad's Split a bill is a bill sheet (820×640 lying down, 620×860 upright).
  - "Bill items" (7.5, `SplitBillUiState.itemsEditor`) is Split's fifth mode, "Items": it opens in Split's
    place (pushed on iOS, the sheet's next page on the Mac, a 920 px dialog on the web, 640 px on a tablet, a drawer on
    a phone), and a bill already split by its items opens Split straight on it. Each item has a name (1 to 60
    letters), a quantity (1 to 999) and a price of each, and "Item" (7.6, `itemEditor`) splits it among who consumed
    it by Equal, Units (only above one unit), Shares, Amount or %, its own leftover cents to whoever paid while they
    had some (`BillItemsSplit`); its Done waits for a name, a price and someone, and while Units, Amount or % don't
    close it says what is missing next to whoever changed last (`ItemPersonLine.isError`). "Service" is a whole
    percentage of the items (10% to start, 0 for none) shared in proportion to what each one consumed, and the
    "Cover" a value per person (none to start) for everyone in the group when the items were split
    (`BillItems.couvertMemberIds`); tapping either row opens its field. Done waits until every item has someone
    ("2 items with nobody", in red) and the items, service and cover close with the total ("R$ 15 left" or
    "over", "the receipt says" for items a receipt gave), offering "Add an item of R$ 15" (a new item at that price)
    and "Split the difference" (shared in proportion to what each one consumed, a "Difference" row that can be taken
    off). Delete item asks first, saying what is left to split once it goes. The Mac's items are a table 860 pt wide
    (700 in a window under 900 pt), where a double click opens an item ("Double-click each item and check
    who had it."), a right click offers "Edit…" and "Delete Item…", the Delete key (⌫) deletes the item picked, and
    "Add item" opens Item on its name, ready to type; the Mac's hints say "click" where a touch screen says "tap"
    ("Click the icon to change it."). Back keeps the items typed for Items to
    open with again until Split closes or another mode's Done. The bill keeps its items inside its row
    (`expenses.items`, stamped with the bill's `updated_at`) beside shares holding each one's total, stored as
    "amount": an older version reads it by Amounts, and items another version saved the bill without are dropped,
    on the server too. Lying down and on tablets the status and what each one pays sit beside the items; a phone,
    lying down and the Duo show each one's total as a strip of chips, a tablet and the web as a list. A receipt read
    (7.1e) opens Items with its items, nobody marked in any ("14 items with nobody"), its service percentage and
    no cover (a cover printed is one of the items), checked against the total it read ("the receipt says").
  - "Category" (7.7): every bill has one, the icon beside "Description" (a menu with a submenu per main category on
    the Mac). Until it is picked by hand, the description's words choose it (the 60 default terms of flow 9, whole
    words ignoring case and accents, the longest term winning, ties to the first in the text, `CategoryMatcher`), else
    Other, and the line under the description says why ("Food › Groceries by the word “groceries” in your map…", "3
    words of the map matched…", "No word of your map…", "You picked… On saving, we ask whether the map learns."). The
    words are the person's map (flow 9) once it has changes, and a card "Category map" between Suggested and
    All categories names the word that chose and opens the map over the bill (a menu item opening Settings on the
    Mac). The picker (pushed on iOS, in the form's
    place on Android and the web) has "Search categories" (by name, accents ignored, "Create “Pet”" when nothing is
    found), up to four "Suggested" (the description's, the group's most used, the group's own) and every main
    category opening its subcategories ("Only Food" keeps the main one; beside the list lying down and on
    tablets); Other is picked at once. A bill saved as Other without a pick stores no category. The app's
    categories (10 main ones and their subcategories, `AppCategory`) are named by each platform; Expenses' rows and
    Bill show the category's badge in its main category's color, Bill also "Food › Groceries".
    Every main category has a color of its own (Pierre's call, beta feedback 20): one of 12 (`CategoryColor`, the
    same values on every platform), the app's ten each starting with a different one (Food orange, Home purple,
    Transport green, Travel teal, Leisure blue, Subscriptions indigo, Shopping pink, Health red, Education yellow,
    Other gray; Food is not blue so the biggest slice never sits next to the accent's blue), and a subcategory
    shows its main category's. A group's own main category picks its color in its form ("Color", under "Icon";
    a new one starts with the first color no main category uses), stored on the category (`group_categories.color`)
    and synced to the group's members like its icon. The app's own categories are the same for everyone, so their color is changed per person, on this device only (`app_category_color`): Category map's
    palette button on each main category but Other (a menu on Apple and the web, a dialog on Android) with "Restore
    color", which brings the app's default back.
  - "New category" (7.8, `GroupCategory`, the group's `group_categories`): anyone in the group may create, rename,
    move and delete the group's own categories, offline too (the change waits in the queue, Last-Write-Wins per
    row). A name has 1 to 30 letters and can't repeat inside the same main category, the app's names included; a
    main category picks one of 18 icons, a subcategory takes its parent's; one with subcategories stays a main one.
    Editing (a long press or a right click on the group's category, "Edit “X”…" on the Mac) says how many bills
    use it; deleting asks "Delete “X”?" saying where its bills go: they move to its parent, or Other, at read time,
    since a deleted category stays as a tombstone (`CategoryTree.resolve`), its subcategories going with it. A new
    category is picked for the bill at once. Split a bill's Category and Category map open the same form
    (`CategoryForm`, `CategoryEditor` in `:core:expenses`).
  - "Category map" (flow 9, `Route.CategoryMap`, `:feature:category_map`): each person's map of words to
    categories, valid in every group, on the device and offline first (`category_term`, the server's
    `category_terms`), synced to the person's other devices under their account and cleared on sign out; while the
    account has no Google or Apple it says "Your map stays on this device only" with "Open Profile". It starts as the
    app's 60 terms (`DefaultCategoryTerms`) and stores only the person's changes over them (`CategoryMap`): a term
    added or moved (one category per term, so a word another category has turns Save into "Move to X"), a
    term removed (no confirmation, "“coffee” left the map" with Undo), and "Restore default" (⋮/⋯), which brings
    the 60 back as they came and keeps the person's own terms. A term may send titles to a group's category, which
    counts only in that group's bills ("only in Florianopolis Trip"); the map's categories are each group's tree merged
    (`CategoryTree.ofGroups`), so a group's category has the color it has in its group. A term of a deleted group
    category reads as that category's parent, or Other, as its bills do (`CategoryTree.resolve`), and shows, edits
    and removes there; a term of a category of a group no longer on the device isn't shown, nor counted for the empty
    map or "Move to X" (`CategoryMapUiState.terms`), though its change waiting to go up still counts in the queue.
    Terms have 2 to 30 letters, digits and spaces.
    The map also creates, edits and deletes categories (9.4, Pierre's call, beta feedback 19), always a group's
    (`GroupCategory`, "New category"'s rules, offline and synced to the group's members), since there are no personal
    categories: "New category" in the ⋮/⋯ menu (the Mac's "New Category…" beside Restore default) and "New
    subcategory in Transport" in every main category but Other, only once there is a group on the device
    (`CategoryMapUiState.canCreateCategory`). A new subcategory of a group's category goes in that group; anything else
    starts in the group with the latest activity, and "Group" changes it while there are two or more
    (`canPickCategoryGroup`), never while editing; its name and "Inside" are checked against that group alone
    (`CategoryTree.ofGroup`). Saving a new one opens "New term" in it at once (Cancel leaves it without terms), and
    a group's subcategory shows in its card even with no term yet. A group's category is edited from its pencil (the
    Mac's "Categories" menu under the table), with Delete there, which waits until its bills are read; the app's
    categories stay as they are.
    "Search terms" finds words starting with what is typed, accents ignored ("2 terms with “uber”"), else offers
    "Add “pizzeria”"; an empty map offers Add term and Restore default; offline with changes queued it
    says "Offline · 2 changes queued" and each queued term has the amber dot. It opens from Profile ›
    Preferences › Category map (Settings › Categories on the Mac, ⌘,) and from Category (9.0). A phone shows
    one card per main category with its terms by subcategory; lying down, a tablet lying down (in 920 dp), a book
    (split on the hinge), an iPad lying down, an iPhone Duo open and the web from 768 px list the categories beside
    the picked one's terms; a tablet or a foldable upright one 560 dp column. "New term" and "Edit term" (9.2)
    are a bottom sheet on an Android phone (640 dp lying down), a 560 dp dialog on its tablets and the book's right
    half, a sheet on iOS (a form sheet with an iPad's room) and the Mac, a Dialog on the web (a Drawer on a phone).
    On the Mac the map is Categories, the last section of Settings' sidebar (the profile's Settings window, never a
    second one), drawn in the form's place: the categories, a table of terms with + and −, and the search; whatever
    opens the map there (Profile's row, the bill's "Category map…") opens Settings on Categories, and whatever
    opens the profile on the profile.
  - "Teach the map?" (9.3e, 9.3f): once a new bill is saved in a category picked by hand that the map wouldn't
    pick (never Other), the group's Expenses asks "Bill saved in Leisure › Sports, not in Home › Rent, which the
    map suggested. Teach the map?" once (`CategoryTeachOffer`, only while the app runs), only when saving lands on
    that group's Expenses: a bill saved back on Balances, Members or Statistics asks nothing, then or later (the
    group's ViewModel drops the offer while another tab is picked). A snackbar on Android, a glass banner on Apple,
    a toast on the web (swiping it away is Not now), with Teach and Not now. Teach asks which word of the
    title ("New in the map…", "Leaves Home › Rent…"; words under 3 letters only when the map has them) and saves it
    in the map; where a word goes now is the whole map's, every group's categories as Category map reads them,
    so a word of another group's category says "Leaves Beach" and teaching moves it (one category per term). The
    bills already saved don't change.
  - "Scan receipt" (7.1e to 7.1g, `ReadReceipt`, the server's `read-receipt`): a new bill's form offers it under
    "Split" (never on "Edit bill", nor on an Android tabletop nor an iPhone Duo open upright, as the boards
    leave it out there; Home's shortcut still opens it), online only ("Needs internet. Fill it in by hand or scan
    the receipt once you're back online."). It asks first where the photo comes from (Pierre's call, beta feedback
    part 2, item 7, `SplitBillUiState.showsReceiptSourceChoice`, `OnReceiptSourcePicked`, `OnReceiptChoiceSkipped` where there is no camera), as Home's shortcut and
    "Take another photo" do too: Android a bottom sheet "Scan receipt" with "Take photo", "Choose from gallery"
    (the system Photo Picker, no storage permission) and "Choose file" (the documents picker, an image saved as a
    file), iOS and iPadOS an action sheet with "Take photo", "Choose from Photos" (PhotosPicker) and "Choose File…"
    (Files), the Mac one with "Choose File…" and "Choose from Photos", and the web a dialog (a
    drawer on a phone) with "Take photo" and "Choose photo" (the browser's file chooser, the phone's gallery).
    Home's shortcut asks on Home instead and opens the form with the photo picked there (beta feedback part 3, item
    16, `ReceiptRequest.Picked`): it is read once "Split in which group?" is answered and that group is read, in its
    currency, as a photo picked on the form is; a group in bitcoin or ether drops it, and an app restored without the
    photo (the system closed it meanwhile) asks for one again. Where
    there is no camera to take it with (a device without one, a browser that lists none) it goes straight to the
    gallery or the file chooser. "Take photo" opens Android's and iOS's own camera, black, with the receipt's frame,
    the flash, the gallery (still there, to change one's mind) and the shutter, and on the web the browser's camera (a
    webcam too) with "Choose a file" in it. Whichever it came from, the photo is read the same way. The
    photo goes up shrunk as a bill's photo is (`BillImageEncoder`) and the server reads it with Claude, whatever
    the receipt (an NFC-e, a restaurant's bill, a card slip, one from abroad), for each account up to 30 reads a day,
    a failed one included; the photo is never kept there. While it reads, "Receipt photo" says "Reading the receipt…",
    Save waits and every other field of the form shows disabled, so nothing typed is overwritten (Pierre's call,
    beta feedback 17, `SplitBillUiState.isFormLocked`: description, category, amount, group, who paid, when and
    split; a sheet or picker left open closes as the read starts), while × on "Receipt photo" still cancels it. Once read it fills the total, the description (the place)
    and When (the day printed, with its time or the form's, or none, never after now), shows "Read from the receipt" and "14 items" under the description, and its items
    wait for Split's Items, which "14 items" opens straight away (Pierre's call, `OnReceiptItemsClicked`: Split
    under it, so back goes there; the Mac's count is a link); the split stays the group's default until Items or another mode is picked. Its category
    is one of the app's or of the group's own, which go with the photo with the person's map terms counting in that
    group (a term of another group's category or of Other stays out, one of a deleted category as where its bills
    go, `ReceiptCategoryHints`), so "dog food" among the items can choose the group's "Pet", which reads as where its
    bills go once a sync deletes it; it counts only while the
    description's words choose none and none was picked: "Food › Restaurant from the receipt", and "From the
    receipt" in the picker's Suggested. A receipt that can't be read (no receipt in it, no total, a
    photo the camera failed to take) says "Couldn't read the receipt. Try again with more light and the whole receipt in the photo, or fill it in by hand." with "Take another photo"; what isn't the photo's fault says so apart (Pierre's call,
    `SplitBillUiState.receiptError`): the reader that didn't answer or isn't set up "The receipt reader didn't answer.
    It isn't your photo…" and a connection that dropped "No connection. The photo didn't reach the reader…", both with "Try
    again", which sends the same photo again, and the day's 30 reads "You've read 30 receipts in the last 24 hours…" with
    no button. Each leaves the fields as they were and keeps no photo (it may be no receipt at all). × on "Receipt photo" (also "Everyone in the group sees it") drops the photo and the receipt's
    items, keeping what it filled. Saving attaches the photo read, as it was read, as the bill's receipt (Bill's
    "Attachments", badge "Receipt").
  - "Split in which group?" (7.1i): the home's "Split a bill" opens Split a bill for the group with the latest
    activity (the home's first row) and, while there are two groups or more, asks "Split in which group?" over it ("The
    one you used last comes marked", `Route.SplitBill.picksGroupFirst`): every group on the device, settled ones
    included, the most recent activity first as on the home (`ObserveGroupsByActivity`), each with "6 people", one
    marked until "Continue" (`SplitBillUiState.groupSelection`); closing it keeps the marked group's bill. With a
    single group it goes straight to the bill, and a sync bringing a second one never asks later. The bill's group
    opens that group's screen (its Expenses) over the form (beta feedback; the details below were settled while
    Pierre was away, see PR #299: `OnOpenGroupClicked`, `SplitBillUiState.canOpenGroup`): Android's chip, iOS's
    "Group" row, the web's "Group" field and the Mac's "Open group" link beside it, a new bill and "Edit bill" alike,
    never while the bill saves or a receipt is read. Back comes to the form with what was typed: Android keeps its
    entry under the group, iOS and the Mac push the group inside the bill's sheet (as the category map), with what it
    opens but a screen that is only ever a sheet, the web draws it over the form, which stays mounted. The group's
    "Split a bill" there comes back to a new bill's form instead of opening a second one ("Edit bill" opens a new
    bill over the group), and a screen already on the stack (the bill being edited, opened again from its group) goes
    back to it rather than stacking twice. A new bill's group is changed
    apart from it, once there is another group (`SplitBillUiState.canChangeGroup`): "Change" beside Android's chip,
    iOS's row and the web's field reopens "Split in which group?", and the Mac's pop-up button picks in one go (the
    Mac's sheet only asks from the home); "Edit bill" keeps its group. Another group keeps what was typed
    (amount, description, when), but who paid and the split start over with its people: you pay, Equal among everyone.
    A sync deleting the group the bill was moved to sends it back to the group it was opened for, the same way, and
    closes Split a bill only once that one is gone too.
    A bottom sheet on Android, a sheet on iOS and the Mac, a dialog over the bill on the web (in its place on a
    phone's drawer). Saving a bill from the home, or moved to a group other than the one it was split from, opens
    that group's Expenses in Split a bill's place.
