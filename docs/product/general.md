# General rules

Cross-cutting product decisions: language, amounts, colors, currencies, platform minimums and loading states. Follow them on every platform.

  - The recovery code's "Connect Google or Apple instead" expands in place into one button per provider
    (`RecoveryCodeUiState.isConnectExpanded`).
  - The recovery code is 12 English words of the BIP39 list, in every language (Pierre's call, November 2026: a code of
    12 letters and digits was easy to copy wrong by hand, and said nothing about which character was wrong). Every
    platform shows them numbered, row by row (three columns where the longest words fit, two on a phone and in Profile's sign out dialog), with Copy, which copies
    them joined by spaces. "Save your recovery code" also offers them as a text file (Download beside Copy on Android,
    iOS and macOS, through the system's save sheet; "Download as a file" on the web), which lists them numbered, one a
    line, under the app's name and where to type them. Restoring takes them in 12 numbered fields, one per
    word: pasting or typing several words into any field fills the fields from it on, a space or the keyboard's next
    after a whole word moves on, up to three words of the list starting with the field's letters show to tap, and a
    field no word can come out of is marked at once. Only the first four letters of a word count. A pasted text with more than the words (the downloaded file, a note) keeps its first run of 12 words of the list, and a scanned QR that isn't a code says "Code not found". The codes from before
    the words stopped working, as Pierre decided the anonymous accounts of the beta would be deleted anyway.
  - Apple's sign in button comes before Google's on iOS, macOS and in the browser of an Apple device (Mac, iPhone,
    iPad); Google comes first on Android and in any other browser.
  - An account without Google or Apple is deleted once none of its devices synced for 30 days (Pierre's call,
    November 2026): every test install made one, and a forgotten one stayed forever. Only a sync counts, so 30 days
    offline in a row count too. The server does it (`expire-anonymous-accounts` in blitz-split-api), as a deletion
    asked in Profile: the person's name stays in their groups with no account, and a group left with no account goes.
    Every platform says so in the same five places:
    - Welcome, in a small centered line right under "Use without an account": "Without an account, your data is
      deleted after 30 days without use. Connect Google or Apple to keep it." It starts with "Without an account" so a
      person who signs in with Google or Apple does not read it as a rule for them (Pierre's call, November 2026).
      Offline, while the Google and Apple buttons are disabled, it ends "Connect Google or Apple later, in Profile, to
      keep it." instead, like the offline notice (`WelcomeUiState.showsOfflineNotice`).
    - Profile's "No account" row, whose line reads "Deleted after 30 days without use" once the account is on the
      server ([profile.md](profile.md)).
    - "Save your recovery code", whose text says the 12 words bring the person's data back and ends "After 30 days
      without use, your data is deleted and the words stop working." (Pierre's call, November 2026: "data", not
      "groups", and the 30 days said again where the code is saved).
    - The sign out of such an account, whose dialog adds that after 30 days without use the account is deleted and
      the code stops working.
    - A device that comes back to a deleted account is erased, profile included, and lands on Welcome with an alert,
      once: "Your data was deleted", "You were using the app without an account and didn't open it for 30 days, so
      your data was deleted. Connect Google or Apple to keep it next time." and "OK"
      (`WelcomeUiState.showsExpiredAccountNotice`). The device asks the server first whether it deleted that
      account: a session lost for any other reason (revoked, the account deleted from another device) keeps the
      profile as before, with no alert.
  - Welcome ends in one small centered line under its buttons, "By continuing, you accept the Terms of Service and
    acknowledge the Privacy Policy.", both names links to the legal texts (the app's own screens; the site's page in
    another tab on the web), with no checkbox nor step of its own (Pierre's call, #184): Analytics rests on legitimate
    interest with Profile's "Share usage data" as its opt out ([profile.md](profile.md),
    [docs/analytics](../analytics/README.md#consent)), and the web shows no cookie banner, in the EU either.
  - Every UI is in English, Portuguese, Spanish, French, German, Italian, Polish, Dutch, Japanese, Turkish, Indonesian,
    Russian, Ukrainian, Korean, Hindi and Arabic: the device's language, English for any other, unless Profile › Preferences › "Language" picked one ([profile.md](profile.md)) (Pierre's call: two Portuguese variants, Brazil's
    `pt-BR` for every `pt` tag but `pt-PT`, and Portugal's `pt-PT`, each its own row of the picker ("Português (Brasil)",
    "Português (Portugal)"), Portugal's overriding only the words Portugal says differently, the rest coming from
    `pt-BR` on every platform (Android's `values-pt-rPT`, Apple's `pt-PT` localization, the web's `ptPT.ts`). Portugal's
    talks to the user as "tu" (friends splitting a bill), says "estar a + infinitive" where Brazil's uses the gerund,
    and calls the app Blitz Split, as every language but Brazil's does; Brazil's keeps Bora Rachar. Two Spanish variants, a neutral
    Latin-American one for every `es` tag but `es-ES`, and Spain's `es-ES` overriding only the words Spain says
    differently, the rest coming from `es` on every platform (Android's `values-es-rES`, Apple's `es-ES` localization,
    the web's `esES.ts`), each its own row of the picker since Pierre's feedback of November 2026 ("Español
    (Latinoamérica)", "Español (España)"); Spanish calls the app Blitz Split, as English does). French is one
    variant for every `fr` tag (fr-FR, fr-CA, fr-BE, fr-CH…), talks to the user as "tu" (Pierre's call: friends splitting
    a bill, as Spanish's "tú") and calls the app Blitz Split. German, Italian and Polish (Pierre's calls) are likewise one
    variant each for every tag of theirs (de-AT and de-CH are `de`, it-CH is `it`), talk to the user informally ("du",
    "tu", "ty") and call the app Blitz Split. Polish's past tense says the speaker's gender, which the app never knows,
    so Polish prefers wording without a gendered verb (impersonal "Dodano", nouns, the present, imperatives on buttons)
    and, where a past about a person can't be avoided, writes both forms the usual way: "dodał(a)", "dodałeś(-aś)".
    Polish also has two plurals (few: "2 grupy", many: "5 grup") where the other languages have one, in every plural
    of the three platforms. Dutch (Pierre's call) is one variant for every `nl` tag (nl-NL and nl-BE alike, no Flemish
    variant), talks to the user as "je/jij", never "u", and calls the app Blitz Split. Japanese (Pierre's call) is one
    variant, in the neutral polite です/ます style standard in apps (no keigo, no plain form), with buttons and titles
    as short nouns (保存, グループを作成), and calls the app Blitz Split; its plurals have only one form. Turkish
    (Pierre's call) is one variant, talks to the user as "sen", never "siz", and calls the app Blitz Split; since a
    Turkish suffix follows the sound of the word before it, no suffix is ever glued to a name or amount the app fills
    in ("Ödeme: Ana", "Ana kişisine"), and its dotted and dotless i ("İptal", "ı") are kept apart. Indonesian
    (Pierre's call) is one variant, talks to the user as "kamu", never "Anda", calls the app Blitz Split, and its
    plurals have only one form. Russian and Ukrainian (Pierre's calls, added together) are one variant each for every
    tag of theirs (ru-KZ is `ru`), talk to the user informally ("ты", "ти"), never "Вы"/"Ви", and call the app Blitz
    Split. Like Polish, their past says the gender, so they prefer impersonal wording and, where a past about a person
    can't be avoided, write both forms ("добавил(а)", "додав(-ла)"); and they never decline a name the app fills in,
    putting a noun before it in the case the sentence needs ("участника Иван"). Their plurals have three forms past
    the singular's one: one (1, 21, 31…: "21 группа"), few ("2 группы") and many ("5 групп"), in every plural of the
    three platforms (the web writes an `…Other` message as "one|few|many"). Korean (Pierre's call) is one variant for
    every `ko` tag, in the friendly-polite 해요체 standard in Korean apps ("만들었어요", never 합니다체 or 반말), with buttons
    and titles as short nouns (저장, 그룹 만들기), calls the app Blitz Split, and its plurals have only one form. Since a
    Korean particle follows the last sound of the word before it (이/가, 을/를), no particle is ever glued to a name, an
    amount or a name of a bill or group the app fills in (Pierre's call, as Turkish): a person's name takes " 님" and
    the particle goes on 님 ("Ana 님이", "Ana 님에게"), a bill or group is quoted before a noun that carries it ("‘저녁’
    지출을"), and anything else is written as a label ("결제: Ana"). The user is "나", or "내가" as the subject of a
    verb ([activity-and-push.md](activity-and-push.md)). Hindi (Pierre's call, issue #448) is one variant for every `hi`
    tag, in Devanagari, and talks to the user as "आप" (the norm in Indian apps, even among friends: करें, never करो),
    in the plain Hindi Indian apps write, English loanwords included (ग्रुप, बैलेंस, अकाउंट); it calls the app Blitz
    Split, ends a sentence with the danda "।" (the code too, where it finishes one: `fullStopUi`, `WordGap.fullStop`,
    `fullStop`), and its plurals have one and other, "one" covering 0 too. Its past puts the ergative "ने" after the
    actor, apart after a name and one word with the user's "आपने", so the activity sentences follow Korean's rule
    with "ने" ([activity-and-push.md](activity-and-push.md)). Arabic (Pierre's call) is one variant, Modern Standard Arabic,
    for every `ar` tag (ar-SA, ar-AE, ar-EG, ar-MA…), talks to the user in the singular "أنت" (Arabic has no polite
    "you" an app would use) and calls the app Blitz Split. It reads right to left, so every UI mirrors in it (the rules
    are in [responsive-layouts.md](../architecture/responsive-layouts.md#right-to-left)), and it writes numbers,
    amounts and dates with Latin digits (0–9), not Arabic-Indic ones, so the amount fields read what the number
    keyboard types (a call taken without Pierre: the Gulf and the Maghreb both read them, and one set of digits keeps
    parsing simple). On the Apple apps a count inside a sentence ("٣ عناصر") follows the device's own digits setting
    (Language & Region › Numbers), since SwiftUI finds its strings by the locale's exact identifier. Since Arabic's verbs, adjectives and "you" say a gender the app never knows (Pierre's call, for
    the user and for everyone else), Arabic prefers wording without one: verbal nouns on buttons and titles ("إضافة",
    "حفظ"), labels ("الدافع: Ana"), the passive ("تم الحفظ") and "على Ana لك" for a debt; only where none fits does it
    use the masculine as the generic, never "(ة)" or a slash. Its activity rows are "Ana: تقسيم «عشاء»", the actor and
    a verbal noun, so the user's own rows need no other verb ([activity-and-push.md](activity-and-push.md)). Its plurals
    have six forms (zero, one, two, few: 3–10, many: 11–99, other), in every plural of the three platforms (the web
    writes an `…Other` message as "zero|one|two|few|many|other"). The brand
    badge shows the currency symbol of the device's region, `$` when there is none.
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
    a link, an amount, a search or a category map's word. A group's or a member's initials are capitalized as the
    app's language writes them ("ilker" is "İ" in Turkish, `toAppLanguageUppercase`).
  - An amount shown, not typed, drops its decimals when they are all zero (Pierre's call, beta feedback 9): "R$ 500",
    "+R$ 125 to get back", "you get 120 and owe 218", and keeps every one otherwise ("R$ 12,50", never "R$ 12,5"),
    decided amount by amount, so one list may mix "R$ 125" and "R$ 12,50" (`GroupCurrency.getDisplayFractionDigits`,
    which Android's `MoneyFormatter`, Apple's `GroupCurrency.format` and the web's `formatMoney` apply), Request's
    message included. Amount fields and their placeholders keep the currency's decimals, and what a machine reads
    (the Pix BR Code) keeps its exact format.
  - Money coming to you is green, what you owe the brand's orange (Pierre's call, as the legacy app): a positive net
    or share, "to get back", "owes you", Owed to you's and To get back's totals, in `#006C4C` light and `#6EDBAA` dark (a
    teal-leaning green that colorblind eyes still tell apart from the orange), never the accent blue of links and
    buttons (Android's Saldo geral alone keeps its filled card's colors), and never swapped for the Android system's by
    Dynamic colors. Zero stays muted, debts between two others keep their color, and a status that adds up
    ("Adds up") stays the accent. No preference inverts them: the sign and the words already say which way the money
    goes. What you owe keeps the brand's orange with Android's Dynamic colors on too (Pierre's call,
    [#179](https://github.com/Quartel-Enterprise/blitz-split-client/issues/179)), `#8E4D31` light and `#FFB599` dark,
    fixed outside the scheme like the green (Android's `negativeAmountColor()` beside `positiveAmountColor()`): the
    dynamic schemes' red has the green's luminance (1.01:1), which red-green colorblind eyes can't tell apart. Real
    errors (a failed action, a field to fix, Delete) keep the scheme's error color, the Dynamic red included.
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
    under 2:1 there. A bar measures in the color of what it measures, never the accent (the default split's bar and Statistics' "By person" ring alone take a color per person by place, the same on both, [members-and-payments.md](members-and-payments.md)), and a picked icon in the
    category form sits on the category's color, not the accent's.
  - A group is always its rounded square, a person always a circle: the group's photo, or its initials on its color,
    with corners at 8% of the side at every size (the group photo viewer's 16 on its 200, beta feedback of 2026-11-12,
    Pierre's call), in every list and grid, Home's cards, the group's header, Balances, a contact's groups in common,
    the pickers, Activity's group dot, "Adjust photo"'s group frame and the photo opened big. One token per platform
    draws it: Android's `GroupBadge` (`GroupAvatarShape`, `GROUP_CORNER_RATIO`), Apple's
    `GroupAvatar` (`GroupAvatar.shape(size:)`, continuous corners) and the web's `GroupAvatar` (`GROUP_SHAPE`);
    a group's avatar never takes another shape at its call site.
  - No category or avatar color copies a color of the theme (Pierre's call): each one's hue stays apart from the
    accent's blues, the orange of what you owe, the green of what comes back, the pending amber and Android Dynamic's
    red, so a slice, a badge or a face never reads as a status or a link. Blue is a royal blue (HCT hue 264), Orange a
    true orange (62), Green a true green (146), Yellow (Sand) a lemon yellow (103), Red a crimson (16), with Indigo
    (286) and Purple (Lilac, 310) moved to make room. Each colored container (Gray is the neutral) sits at a CIEDE2000
    distance of 5 or more from every role container of the brand's scheme, in light and dark; Red's light one alone
    sits 1.7 from the Dynamic red's, as no red at tone 90 can sit further from it. The avatars share the values of the
    category color of the same hue (Blue, Peach = Orange, Green, Lilac = Purple, Sand = Yellow), and what shows a theme
    color (Activity's badge of a bill on the web) takes the theme's token, never an avatar or category color.
    The person's own custom colors (see split-bill.md) are the one exception: they pick any hue, a theme-like one
    included, on purpose. Their tones aren't written per platform but derived from the base in the shared code
    (`CustomColor.toTones`), each lightness searched for its contrast: the badge's content at 7:1 on it, the data tone
    at 4.5:1 on a light card and 7:1 on a dark one.
  - Every text reads at 4.5:1 or more on what is behind it, and a dot, an icon or a field's edge that carries meaning
    at 3:1 (WCAG AA), in light and dark. A filled button's label takes the color made for its fill: on the dark accent
    `#95CDF7` it is `#00344E` (the system's white reads at 1.7:1, so every prominent button on Apple sets
    `.prominentLabel()`, which a build phase checks), and on the dark negative `#FFB599` it is `#552108`. Android's
    scheme sets every Material role, the fixed ones included, so none falls back to Material's baseline purple.
    Dimming never takes a text that still informs below that (the contrast sweep of 2026-11-12, chosen without
    Pierre, for him to confirm): a disabled control may fade, but the line that says why ("Needs internet"), a status
    drawn as done ("Full debt") or a setting it still states keeps its full muted color. The web's focus ring is the
    solid accent (`--ring`, `#266489` light and `#95CDF7` dark, 3 px), never a translucent one, and its unchecked
    switch is the field's edge color in the light theme. The photo opened big sits on a scrim dark enough for its
    captions over a white page: 80% on the web, 75% black on Apple. A selected chip's label takes the color made
    for the accent (`Palette.onAccent` on Apple), and on the Mac a money color in a selected row gives way to the
    row's own label color.
  - Every stack of faces shows as many bubbles as its place has room for, the same on every platform (Pierre's
    call, 2026-11-15, after the beta feedback that capped every stack at two). In a list's row or card and in a form's
    field, where the stack shares its line with a name or an amount (a group's members on its row or card, who paid a
    bill in a list, who is in a split and who shares an item in Split a bill), it is compact, at most two bubbles: one
    or two people show their faces; three or more show the first face and a second bubble "+N" for the rest (three
    people: a face and "+2"). On a screen's own header or body, alone on its line or beside a sentence free to wrap (a
    group's members under its name, who already joined on the invite, who paid on the Bill), it is roomy, at most
    five: up to five people all show their faces; six or more show the first four and "+N" (six people: four faces and
    "+2"). "+N" never stands for one person alone. On the invite, a sentence too long for one line beside the
    faces ("Pierre, Ana, Bruno, Carla and Diego are already in the group") goes under them with the whole width
    rather than wrap in a narrow column, and the group header's loading skeleton shimmers three faces, between the
    smallest group's two and the header's five (both Pierre's call, 2026-11-15). A new stack takes the size of the kind of place it is in, and this
    list names it. The rule is
    `FaceStack` in `:core:model` (`COMPACT` and `ROOMY`), which the ViewModels cut their faces by (the group's header, Groups' rows, Home's bills) and each UI's one stack
    component applies to the stacks it is handed whole (the invite, the Bill, Split a bill), with the size this list
    gives the place
    (Android's `AvatarStackRow`, Apple's `AvatarStack`, the web's `AvatarStack` through `getStackedFaceCount`). Each
    bubble is a true circle ringed in the surface behind it, covers the one before it ("+N" sits over the last face),
    and keeps its size whatever room the row leaves it (a squeezed one read flattened); screen readers skip the stack,
    the row around it says who.
  - A money amount is never broken in two lines nor cut to its sign (the same sweep): it stays on one line and
    shrinks to fit its card (Android's auto size, Apple's `minimumScaleFactor`, the web's `truncate`), and the words
    beside it ("to get back") drop under it when the row is too narrow.
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
  - The ruble (RUB) and the hryvnia (UAH) follow the rupiah (Pierre's call, with Russian and Ukrainian), and start a
    new group in Russia and Ukraine. Both have 2 decimals and the usual 100 million cap, so an app version from before
    them shows their amounts by the raw code, with the right decimals.
  - The South Korean won (KRW) follows the hryvnia (Pierre's call, with Korean), and starts a new group in South
    Korea. It has no decimals, as the yen (no price there shows jeon), and the usual 100 million cap (about US$ 70,000).
    An app version from before it reads KRW as an unknown code with two decimals and shows its amounts 100 times
    smaller until it updates, as the rupiah did.
  - The Indian rupee (INR) follows the won (Pierre's call, with Hindi), and starts a new group in India. It has 2
    decimals and the usual 100 million cap (about US$ 1.2 million), so an app version from before it shows its
    amounts by the raw code, with the right decimals.
  - The Saudi riyal (SAR), the UAE dirham (AED), the Egyptian pound (EGP) and the Moroccan dirham (MAD) follow the
    won (Pierre's call, with Arabic), and start a new group in Saudi Arabia, the United Arab Emirates, Egypt and
    Morocco. All four have 2 decimals and the usual 100 million cap, so an app version from before them shows their
    amounts by the raw code, with the right decimals.
  - Bitcoin and Ether (Pierre's calls, beta feedback 7): "Default currency" (New group, Edit group) lists BTC and ETH last,
    under their own "Cryptocurrencies" header, as "Bitcoin (₿)" and "Ethereum (Ξ)", and no region starts a group in them.
    Both have 8 decimals (a satoshi; ether's 18 wouldn't fit a Long of the minor unit past about 9 ether), and cap a
    bill or a payment by each coin's supply: 21 million bitcoin and 90 million ether (the most the web's numbers
    count to the unit), every other currency keeping 100 million but the rupiah (above)
    (`ExpenseRules.maxAmountMinorFor`, the API's `max_amount_minor`). Shown, they take ₿ and Ξ where the language puts
    a symbol and only the decimals the amount needs ("₿ 0,00125", "Ξ 0,5", "₿ 1"); fields type all 8
    ("0,00000001"). Profile › Preferences' "Bitcoin in satoshis" (always shown, Settings on the Mac), kept per
    device and never synced (`showsBitcoinInSats` in `device_settings`), shows every bitcoin amount as whole sats
    ("125,000 sats", the field's symbol "sats") and types them so (`GroupCurrency.getTypedFractionDigits`); a form
    open when it changes (the Mac's Settings is a window of its own) turns what was typed into the new unit, keeping
    its value (`TypedText.toUnit`). "Scan receipt" isn't offered in a group in either (no receipt is printed in
    them), and Home's shortcut opens such a bill without the camera; the reader refuses them too.
  - Minimum iOS 26 and macOS 26, so Liquid Glass is native with no fallbacks. The Apple app builds with the iOS 27.1
    SDK (Xcode 27.1), its iPhone Duo APIs behind `#available(iOS 27.1, *)`.
  - The Apple app has no floating action button (Pierre's call): a screen's main button ("Split a bill", on every
    tab of a group) is a toolbar item, the bar's tinted glass item on iPhone and iPad (the bottom toolbar inside a group
    on an iPhone upright, except on an iPhone Duo, whose vertical bar takes it as its icon alone) and the window
    toolbar's primary button on the Mac. Android keeps its FABs on a phone upright; anywhere wider a group's "Split a
    bill" sits in its top bar ([group-and-bills.md](group-and-bills.md)).
  - A Mac sheet ends in a trailing row of regular-size buttons, as a Mac dialog's (Record payment, Split a bill and
    its editors, Request, the new group's Invite, a personal invite): Cancel or Close on Esc, then the main one,
    prominent, on Return (⌘Return where Return types a new line, as in Request's message). The large buttons are the iPhone's and iPad's, for a thumb; a footer shared with them
    (Share bill, a contact's invite, "Update available") is large there and regular on the Mac (`sheetButtonSize()`).
  - The Android UI uses the AndroidX Compose artifacts (BOM), not the JetBrains Compose coordinates; only the shared
    ViewModels take JetBrains' multiplatform lifecycle.
  - On Android a shadow means "tap me" (Pierre's call, beta feedback, after Material 3's card guidelines): a card that
    stands on its own and opens something as one touch target is `:android:designsystem`'s `TappableCard`, Material's
    elevated card, with its ripple, TalkBack's "button" and its hovered elevation (1 dp at rest, 3 dp under a pointer;
    pressed keeps the resting elevation, as Material's tokens say), and in a dark theme, where a shadow barely shows,
    Material's tonal tint of that level (the surface tint over the container at 5 %): a group's card on Home and in
    Groups' grid (the cards of one collection share their resting elevation; the grid's long press, its actions, is on
    the same touch target), the balances summary on Contacts, the identity card on Profile, Statistics' "Biggest
    expense", Split a bill's "Category map", Groups' search "Did you mean …?", the joined banner's "Protect your
    access" and Restore's "Scan the QR code from your old device". A card may still hold buttons of its own beside its
    one action (the balances summary's two totals, the identity card's photo), as Material's cards do. The card keeps
    its screen's container color (Material's elevated card is `surfaceContainerLow`, beside rows of
    `surfaceContainerLowest`). A card that only informs (balances, notices, banners, empty states) stays flat, a
    filled card, its buttons the only thing to tap. Rows that form one rounded group (Split a bill's Who paid / When /
    Split, settings, Balances, a day's bills, Home's shortcuts) and a single picker row of a form (Record payment's
    When, New group's currency) are a list, not cards, and stay flat even when tappable: a shadow per segment would
    draw a seam in the 2 dp gaps and break the group into separate cards. iOS, the Mac and the web keep their own
    platform's look.
  - The Android launcher icon comes from the legacy app until Bora Rachar has its own.
  - The app is free, with nothing paid in it for now, and shows no ads (Pierre's call). The stores' listings say so
    ([store-listing.md](../store-listing.md)): something paid changes them in the same pull request.
  - Nothing comes over from the legacy app (Pierre's call): 3.0.0 is a clean break. The legacy Blitz Split (2.4.0)
    kept its groups and bills on its own server, which this app never reads, so someone who updates starts on
    Welcome like anyone new. Of its own data, the legacy app only left a DataStore file on the device
    (`blitz_split_data.preferences_pb`), which this app neither reads nor removes; it wrote nothing of its own to the
    user defaults the two apps share on Apple, having the same bundle id. What the Firebase SDKs keep on the device
    is theirs: both apps use the same Firebase project.
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
  - Syncs run silently (Pierre's call, beta feedback: "Syncing" showed often enough to annoy, in an offline first app).
    Home's and Groups' sync chip says "Syncing…" only during the account's first full sync, from a restore (a sign
    in, a recovery code, a new device) until a sync gets the whole copy through, retries and relaunches included
    (`AccountSnapshot.awaitsFullSync`), and for the sync after "No response from the server", the retry, until it
    settles, so a tap on "Try again" shows it working. Every other sync, a queued change going up or the catch up on
    coming back to the front, shows nothing. And never while the screen already shows the account's data (Pierre's
    call, beta feedback: the chip came back again and again over a full Home): once Home or Groups shows a group, a
    restore's first full sync and a retry run silently too, and "Syncing…" goes as the first group arrives; the first
    download's own "Syncing… 3 of 7 groups" still counts them. A sync that runs while the session waits for its token's
    refresh, as on coming back to the front after a while, asked the server nothing: it is put off, not failed, so it
    shows neither "No response from the server" nor, once the token comes back and it runs again, "Syncing…". Only a
    session still refreshing 30 seconds later (Supabase tries every 10) counts as failed: the server doesn't answer. "Offline · 1 change queued" and "No response from the server" still
    show as they did, and win over "Syncing…" (`SyncBadge.ofOrNull`, so no UI special-cases it). Pulling down
    (Android, iOS) keeps its own spinner until that sync ends, as Activity's refresh does on the Mac (View › Refresh,
    ⌘R) and the web; the Mac's and the web's Home and Groups have no refresh of their own. Profile's "Account and sync" row still says
    "Syncing…" while one runs, a status read there on purpose, and Activity's "Syncing…" only shows while a new device
    waits for the feed's first page.
  - **Discard changes?** Every form with input to lose asks before closing: Split a bill (a new bill, "Edit bill" and
    "Duplicate"), Record payment, New group, Edit group, Default split and Edit profile (the sheet or dialog; the web's and
    the Mac's inline editor under the name keeps its explicit Cancel, which puts the saved values back). Pierre's
    call. Each form's state says whether it holds unsaved input (`hasUnsavedInput`) by comparing with how it opened,
    so a form changed back to how it opened, or never touched, closes silently, and so does a form just saved, deleted
    or whose group is gone. What counts: Split a bill's total, description, currency, group, payers, "When", split,
    category, a receipt, "Use as the group's default"; Record payment's amount other than the whole debt, a method
    other than Pix and "When"; New group's name, photo, people, draft, color, type and currency; Edit group's name,
    photo, color, type and currency (Simplify debts applies at once, so it is no input); Default split's weights,
    incomes and exceptions (not a member who joined meanwhile); Edit profile's name, color and photo. The answer is
    one shared prompt, because the shared back stack holds *any* change that would close such a form until it is
    answered (`Navigator.markUnsaved`, `pendingDiscard`, `confirmDiscard`, `cancelDiscard`): the close button, system
    back and predictive back on Android (the form's own back handler, so the gesture never completes before the
    answer, and a bottom sheet's drag or scrim asks instead of closing), the swipe down of an iOS sheet (disabled
    while there is unsaved input, as the bar's Cancel asks), the Mac's Esc and Cancel, the web's ✕, Esc and browser
    back (which goes forward again until it is answered), and the shell's navigation (Android's rail and bottom bar,
    the web's and the Mac's sidebar) while the form is open or under another screen (Split a bill → its group). The
    answer is "Discard changes?" with "Discard" (destructive) and "Keep editing": a Material dialog on Android, an
    action sheet on iPhone, an alert on iPad and a sheet on the Mac window, the web's AlertDialog, in every language
    the app ships. The web also warns before the tab closes or reloads (`beforeunload`) while any form has unsaved
    input. Restoring or deleting the account starts the app over and never asks.
    ⌘R) and the web; the Mac's and the web's Home and Groups have no refresh of their own. The first download's chip
    stays: it is the only place that says "Syncing… 3 of 7 groups", the bar under it has no text. Profile's "Account
    and sync" row follows the same rule (`ProfileSyncStatus`): "Syncing…" only during the first full sync and the
    retry, the row at rest otherwise, "Offline" and "No response from the server" as they were.
    Activity's "Syncing…" only shows while a new device waits for the feed's first page.
  - Pix is only in the way of people likely to use it (Pierre's call). One boolean, `PixSignals.showsPix`
    (`ObserveShowsPix`, live), decided from signals about the person and never from where the phone is (no
    location, no IP), so Brazilians abroad keep it. Pix is offered when any of these holds: the app's effective
    language is Portuguese of Brazil (Profile's "Language", or the system's language, `pt` or `pt-BR`, while it is on
    "System"; pt-PT is not); the person has a saved Pix key; the person is in a group in reais or in a group where
    any member has a Pix key (so a foreigner in a Brazilian group can still pay by Pix; the member keys are those the
    last sync cached); the device's region is Brazil (one extra signal, never the only path). When none holds,
    Profile's Pix keys collapse behind an "Add a Pix key" row next to "Add a Bitcoin key" (nobody is locked out;
    Bitcoin stays as it is) and Record payment's "How it was paid" has no Pix chip and opens on Transfer. What already
    exists keeps showing: a payment recorded by Pix still reads Pix, a debtor's Pix keys show while they apply (a
    saved or a member's Pix key turns the rule on), and Settle up's "Pix copy and paste" and Charge's "Include my Pix
    key" only exist with a BRL group or a Pix key, which turn it on too. Adding a key or joining a group in reais
    re-evaluates it at once (the device's own language and region are read each time something else changes, not watched). The website and the store screenshots follow their own rule.
    A person holds one Pix key of each type at most, random keys aside ([profile.md](profile.md)'s keys).
