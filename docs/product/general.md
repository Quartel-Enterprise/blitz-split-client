# General rules

Cross-cutting product decisions: language, amounts, colors, currencies, platform minimums and loading states. Follow them on every platform.

  - The recovery code's "Connect Google or Apple instead" expands in place into one button per provider
    (`RecoveryCodeUiState.isConnectExpanded`).
  - Apple's sign in button comes before Google's on iOS, macOS and in the browser of an Apple device (Mac, iPhone,
    iPad); Google comes first on Android and in any other browser.
  - Every UI is in English, Portuguese, Spanish, French, German, Italian and Polish: the device's language, English
    for any other, unless Profile › Preferences › "Language" picked one ([profile.md](profile.md)) (Pierre's call: two Spanish variants, a neutral
    Latin-American one for every `es` tag, and Spain's `es-ES` overriding only the words Spain says differently, the
    rest coming from `es` on every platform; Spanish calls the app Blitz Split, as English does). French is one
    variant for every `fr` tag (fr-FR, fr-CA, fr-BE, fr-CH…), talks to the user as "tu" (Pierre's call: friends splitting
    a bill, as Spanish's "tú") and calls the app Blitz Split. German, Italian and Polish (Pierre's calls) are likewise one
    variant each for every tag of theirs (de-AT and de-CH are `de`, it-CH is `it`), talk to the user informally ("du",
    "tu", "ty") and call the app Blitz Split. Polish's past tense says the speaker's gender, which the app never knows,
    so Polish prefers wording without a gendered verb (impersonal "Dodano", nouns, the present, imperatives on buttons)
    and, where a past about a person can't be avoided, writes both forms the usual way: "dodał(a)", "dodałeś(-aś)".
    Polish also has two plurals (few: "2 grupy", many: "5 grup") where the other languages have one, in every plural
    of the three platforms. The brand badge shows the currency symbol of the device's region, `$` when there is none.
  - Every amount field (Split a bill's total, "Several people paid", Amounts, an item's price and Amount, the
    cover, Record payment, By income's incomes) types as a cash machine does (Pierre's call): the number
    keyboard, digits coming in from the right with the bill's currency's decimals fixed (`GroupCurrency.fractionDigits`:
    "0,01", "0,12", "1,23"; yen "1", "12", "1.234"), the caret at the end, a comma or a dot typed changing nothing,
    and the device language's separators whatever the currency ("1.234,56" in Portuguese, "1,234.56" in English). A
    paste reads as an amount ("12,5", "R$ 1.234,56"), and a bill moved to a group of another currency keeps its number
    with the decimals cut (`TypedText.toAmountText`, `toCurrency`). Percentages, parts and quantities stay whole
    numbers.
  - A name, a description or a comment starts with a capital letter (Pierre's call, beta feedback 3 item 27): a
    group's name, a member's (Add by name, Members' Name, joining as a new member), your own (the first profile, Edit
    profile), a bill's description, an item's name, a comment, a category's name and Request's message. A phone's
    or a tablet's keyboard capitalizes each sentence (Android's `freeTextKeyboardOptions`, Apple's
    `.capitalizesFirstLetter(isPersonName:)`, the web's `autocapitalize` from `freeTextProps`); a person's name
    capitalizes each word instead ("Maria Silva", `personNameKeyboardOptions`, `isPersonName`, `'words'`: chosen
    without Pierre in item 27, keeping what Android already did, for him to confirm). The Mac and a computer's
    browser, whose keyboard doesn't, uppercase the first letter as it is typed (Apple's `firstLetterCapitalized`
    binding, the web's `freeTextProps`): a lone lowercase letter typed into an empty field, after a dead key's
    accent or over a selected text, the caret staying after it. Not the capital lowercased, nor a lowercase letter
    the field had before (deleting down to it), so someone who wants it lowercase keeps it. Never an email, a payment key, an invite or recovery code,
    a link, an amount, a search or a category map's word.
  - An amount shown, not typed, drops its decimals when they are all zero (Pierre's call, beta feedback 9): "R$ 500",
    "+R$ 125 to get back", "you get 120 and owe 218", and keeps every one otherwise ("R$ 12,50", never "R$ 12,5"),
    decided amount by amount, so one list may mix "R$ 125" and "R$ 12,50" (`GroupCurrency.getDisplayFractionDigits`,
    which Android's `MoneyFormatter`, Apple's `GroupCurrency.format` and the web's `formatMoney` apply), Request's
    message included. Amount fields and their placeholders keep the currency's decimals, and what a machine reads
    (the Pix BR Code) keeps its exact format.
  - Money coming to you is green, what you owe the brand's orange (Pierre's call, as the legacy app): a positive net
    or share, "to get back", "owes you", Owed to you's and To get back's totals, in `#006C4C` light and `#6EDBAA` dark (a
    teal-leaning green that colorblind eyes still tell apart from the orange), never the accent blue of links and
    buttons (Android's Saldo geral alone keeps its filled card's colors), and never swapped for the wallpaper's by
    Dynamic colors. Zero stays muted, debts between two others keep their color, and a status that adds up
    ("Adds up") stays the accent. No preference inverts them: the sign and the words already say which way the money
    goes. With Android's Dynamic colors on, what you owe takes the wallpaper scheme's red for now
    ([#179](https://github.com/Quartel-Enterprise/blitz-split-client/issues/179) decides it).
  - What is saved here and not on the server yet (the queue's dot, "queued", "Waiting to send", a profile change
    not up yet) is amber (chosen in the contrast audit, for Pierre to confirm), `#7C5800` light and `#F9BC3E` dark,
    with `#FFDEA8` / `#5E4200` behind a badge or a notice, and `#F9BC3E` in both themes on what stays dark (the
    attachment viewer, Apple's badge over a photo; Android's badge over a photo sits on the inverse surface, so it
    takes the other theme's amber, and the web's is a badge on the amber container): never the orange of what you
    owe (the system's orange matched it in dark mode), and outside the scheme like the green (Android's
    `pendingColor()`, Apple's `Palette.pending`, the web's `--pending`).
  - A category's or a person's color has three tones on every platform (Android's `toContainerColor`,
    `toContentColor`, `toDataColor`; Apple's `.container`, `.content`, `.data`; the web's `--avatar-bg`,
    `--avatar-fg` and `--avatar-data`, which `avatarStyle()` and `categoryStyle()` set, the `avatar` class painting
    the first two and `avatar-mark`, `avatar-stroke` and `avatar-edge` the data tone): a badge or a face is the
    container with the content on it, and a mark that carries the color by itself (a slice, a legend dot, a bar, a segment of the default split, a group's dot in Activity) takes the data
    tone, Material tone 50 light and 75 dark of the color's hue (4:1 and 7:1 on a card), since the container reads at
    under 2:1 there. A bar measures in the color of what it measures, never the accent, and a picked icon in the
    category form sits on the category's color, not the accent's.
  - No category or avatar color copies a color of the theme (Pierre's call): each one's hue stays apart from the
    accent's blues, the orange of what you owe, the green of what comes back, the pending amber and Android Dynamic's
    red, so a slice, a badge or a face never reads as a status or a link. Blue is a royal blue (HCT hue 264), Orange a
    true orange (62), Green a true green (146), Yellow (Sand) a lemon yellow (103), Red a crimson (16), with Indigo
    (286) and Purple (Lilac, 310) moved to make room. Each colored container (Gray is the neutral) sits at a CIEDE2000
    distance of 5 or more from every role container of the brand's scheme, in light and dark; Red's light one alone
    sits 1.7 from the Dynamic red's, as no red at tone 90 can sit further from it. The avatars share the values of the
    category color of the same hue (Blue, Peach = Orange, Green, Lilac = Purple, Sand = Yellow), and what shows a theme
    color (Activity's badge of a bill on the web) takes the theme's token, never an avatar or category color.
  - Every text reads at 4.5:1 or more on what is behind it, and a dot, an icon or a field's edge that carries meaning
    at 3:1 (WCAG AA), in light and dark. A filled button's label takes the color made for its fill: on the dark accent
    `#95CDF7` it is `#00344E` (the system's white reads at 1.7:1, so every prominent button on Apple sets
    `.prominentLabel()`, which a build phase checks), and on the dark negative `#FFB599` it is `#552108`. Android's
    scheme sets every Material role, the fixed ones included, so none falls back to Material's baseline purple.
  - The currency belongs to each bill and each payment, not to the group (Pierre's call, as Splitwise does it, made
    before the app is in production): `Expense.currency`, `Settlement.currency` and `SettlementLeg.currency`, stored
    on the `expense` and `settlement` rows (Room 40, which gave every row its group's) and synced as the API's
    `currency`. A group's currency (`Group.currency`, New group's and Edit group's "Default currency", always editable) stays as the
    default of its new bills. Balances are never added across currencies: a group with bills in two currencies shows one
    balance per currency, "you owe R$ 50 and US$ 20". Shipped: every bill and payment is saved in its own currency;
    the group's own screens (header, Balances, Members, settling up, Statistics, the group exit card) show one balance
    per currency with a currency chip row (the code, "BRL", "USD") that appears only while the group has money in more
    than one (`GroupBalances.getByCurrency`, the group's default currency first; every figure under a chip is that
    currency's only), and every amount of a bill row, a payment row, a debt row and a detail is in the bill's or
    payment's own currency, never the group's. The home's overall balance is the global Balances' computation
    (`PeopleBalances.totals`). The bill form picks the bill's currency beside the total (see
    [Split a bill](split-bill.md)), and Activity shows each amount in its bill's or payment's own currency.
  - A bill or payment in a currency code this version does not know (only a newer app or server writes one) is never
    skipped and never turned into another currency (Pierre's call): it shows with its raw code and two decimals, "XYZ
    12,50", in the bill list, the detail, the balances, settle up and the activity (Android's `MoneyFormatter`, Apple's
    `GroupCurrency.format`, the web's `formatMoney` write the code where the symbol would go). `GroupCurrency` is a
    class, not an enum: `GroupCurrency.fromCode` gives the listed currency or a read-only one that carries the code
    (`isKnown` false, equal only to the same code, never in `entries`, so never offered by a picker). It is its own
    currency in every balance, with its own chip. It is read-only: Edit and Undo are off on the bill, with "This bill
    uses a currency this app version doesn't know. Update the app to edit it.", no payment can be registered in it (a
    tap on its debt row does nothing, the group's bill menu does not open the form for it, leaving the group offers
    "See balances" rather than "Settle up" when it is all you owe, and a settle up opened in it leaves at once), and no
    amount in it is valid (`ExpenseRules.isAmountValid`, and the expense and settlement repositories refuse to store
    one); the stored code goes back to the server as it came. A group whose own default currency is unknown reads as
    the fallback, USD (`GroupMapperImpl`), which only sets the default of a new bill.
  - The zloty (PLN), the Turkish lira (TRY) and the rupiah (IDR) are listed after the Swiss franc, and start a new
    group in Poland, Turkey and Indonesia. The rupiah has no decimals (Pierre's call), as the yen, though ISO 4217
    gives it 2: no price or receipt there shows sen. Its bills and payments cap at 100 billion, not 100 million
    (Pierre's call: 100 million rupiah is only about US$ 6,000). An app version from before it reads IDR as an
    unknown code with two decimals and shows its amounts 100 times smaller until it updates (Pierre's call: an
    accepted beta trade-off, since no group split in rupiah before).
  - Bitcoin and Ether (Pierre's calls, beta feedback 7): "Default currency" (New group, Edit group) lists BTC and ETH last,
    under their own "Cryptocurrencies" header, as "Bitcoin (₿)" and "Ethereum (Ξ)", and no region starts a group in them.
    Both have 8 decimals (a satoshi; ether's 18 wouldn't fit a Long of the minor unit past about 9 ether), and cap a
    bill or a payment by each coin's supply: 21 million bitcoin and 90 million ether (the most the web's numbers
    count to the unit), every other currency keeping 100 million but the rupiah (above)
    (`ExpenseRules.maxAmountMinorFor`, the API's `max_amount_minor`). Shown, they take ₿ and Ξ where the language puts a symbol and only the decimals the amount
    needs ("₿ 0,00125", "Ξ 0,5", "₿ 1"); fields type all 8 ("0,00000001"). Profile › Preferences' "Bitcoin in
    satoshis" (always shown, Settings on the Mac), kept per device and never synced (`showsBitcoinInSats` in
    `device_settings`), shows every bitcoin amount as whole sats ("125,000 sats", the field's symbol "sats") and
    types them so (`GroupCurrency.getTypedFractionDigits`); a form open when it changes (the Mac's Settings is a window
    of its own) turns what was typed into the new unit, keeping its value (`TypedText.toUnit`). "Scan receipt"
    isn't offered in a group in either (no receipt is printed in them), and Home's shortcut opens such a bill
    without the camera; the reader refuses them too.
  - Minimum iOS 26 and macOS 26, so Liquid Glass is native with no fallbacks. The Apple app builds with the iOS 27.1
    SDK (Xcode 27.1), its iPhone Duo APIs behind `#available(iOS 27.1, *)`.
  - The Apple app has no floating action button (Pierre's call): a screen's main button ("Split a bill", on every
    tab of a group) is a toolbar item, the bar's tinted glass item on iPhone and iPad (the bottom toolbar inside a group
    on an iPhone upright, except on an iPhone Duo, whose vertical bar takes it as its icon alone) and the window
    toolbar's primary button on the Mac. Android keeps its FABs.
  - A Mac sheet ends in a trailing row of regular-size buttons, as a Mac dialog's (Record payment, Split a bill and
    its editors, Request, the new group's Invite, a personal invite): Cancel or Close on Esc, then the main one,
    prominent, on Return (⌘Return where Return types a new line, as in Request's message). The large buttons are the iPhone's and iPad's, for a thumb; a footer shared with them
    (Share bill, a contact's invite, "Update available") is large there and regular on the Mac (`sheetButtonSize()`).
  - The Android UI uses the AndroidX Compose artifacts (BOM), not the JetBrains Compose coordinates; only the shared
    ViewModels take JetBrains' multiplatform lifecycle.
  - The Android launcher icon comes from the legacy app until Bora Rachar has its own.
  - Nothing is guessed while loading (Pierre's call): a section with no concrete answer yet (Room not read, the
    server not answered, or data only the account's first full sync brings while `AccountSnapshot.awaitsFullSync`)
    shows a shimmer in the shape of what comes, never an empty state, a zero, a default or a disabled guess. A read
    of the device only shows it after 150 ms, so a fast one never flashes. Offline while the account's copy is still
    partial, Home and Groups say "No internet. Your groups show up once you're back online." and the other screens
    "No internet. Your data shows up once you're back online." under it. The shared state models such a section as a
    `Loadable<T>` (`software.quare:loadable`); each platform draws it with its one shimmer component. A sync that
    fails online ends the wait until a later one gets through or another account is restored (`FirstDownload`): the
    screens show what the device holds under the failed sync's badge, since a change the server keeps refusing fails
    every try. Offline, Bill shows the device's comments and photos and takes new ones. Record
    payment never waits offline for the receiver's keys: the form opens, so a payment is recorded offline too.
