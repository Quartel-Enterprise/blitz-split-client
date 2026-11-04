# General rules

Cross-cutting product decisions: language, amounts, colors, currencies, platform minimums and loading states. Follow them on every platform.

  - The recovery code's "Conectar Google ou Apple em vez disso" expands in place into one button per provider
    (`RecoveryCodeUiState.isConnectExpanded`).
  - Apple's sign in button comes before Google's on iOS, macOS and in the browser of an Apple device (Mac, iPhone,
    iPad); Google comes first on Android and in any other browser.
  - Every UI is in English and Portuguese, English for any other language. The brand badge shows the currency
    symbol of the device's region, `$` when there is none.
  - Every amount field (Rachar conta's total, "Várias pessoas pagaram", Valores, an item's price and Valor, the
    couvert, Registrar pagamento, Pela renda's incomes) types as a cash machine does (Pierre's call): the number
    keyboard, digits coming in from the right with the group currency's decimals fixed (`GroupCurrency.fractionDigits`:
    "0,01", "0,12", "1,23"; yen "1", "12", "1.234"), the caret at the end, a comma or a dot typed changing nothing,
    and the device language's separators whatever the currency ("1.234,56" in Portuguese, "1,234.56" in English). A
    paste reads as an amount ("12,5", "R$ 1.234,56"), and a bill moved to a group of another currency keeps its number
    with the decimals cut (`TypedText.toAmountText`, `toCurrency`). Percentages, parts and quantities stay whole
    numbers.
  - An amount shown, not typed, drops its decimals when they are all zero (Pierre's call, beta feedback 9): "R$ 500",
    "+R$ 125 a receber", "você recebe 120 e deve 218", and keeps every one otherwise ("R$ 12,50", never "R$ 12,5"),
    decided amount by amount, so one list may mix "R$ 125" and "R$ 12,50" (`GroupCurrency.getDisplayFractionDigits`,
    which Android's `MoneyFormatter`, Apple's `GroupCurrency.format` and the web's `formatMoney` apply), Cobrar's
    message included. Amount fields and their placeholders keep the currency's decimals, and what a machine reads
    (the Pix BR Code) keeps its exact format.
  - Money coming to you is green, what you owe the brand's orange (Pierre's call, as the legacy app): a positive net
    or share, "a receber", "te deve", Te devem's and A receber's totals, in `#006C4C` light and `#6EDBAA` dark (a
    teal-leaning green that colorblind eyes still tell apart from the orange), never the accent blue of links and
    buttons (Android's Saldo geral alone keeps its filled card's colors), and never swapped for the wallpaper's by
    Cores dinâmicas. Zero stays muted, debts between two others keep their color, and a status that adds up
    ("Fechou") stays the accent. No preference inverts them: the sign and the words already say which way the money
    goes.
  - Bitcoin and Ether (Pierre's calls, beta feedback 7): Moeda (Novo grupo, Editar grupo) lists BTC and ETH last,
    under their own "Criptomoedas" header, as "Bitcoin (₿)" and "Ethereum (Ξ)", and no region starts a group in them.
    Both have 8 decimals (a satoshi; ether's 18 wouldn't fit a Long of the minor unit past about 9 ether), and cap a
    bill or a payment by each coin's supply: 21 million bitcoin and 90 million ether (the most the web's numbers
    count to the unit), every other currency keeping 100 million (`ExpenseRules.maxAmountMinorFor`, the API's
    `max_amount_minor`). Shown, they take ₿ and Ξ where the language puts a symbol and only the decimals the amount
    needs ("₿ 0,00125", "Ξ 0,5", "₿ 1"); fields type all 8 ("0,00000001"). Perfil › Preferências' "Bitcoin em
    satoshis" (always shown, Ajustes on the Mac), kept per device and never synced (`showsBitcoinInSats` in
    `device_settings`), shows every bitcoin amount as whole sats ("125.000 sats", the field's symbol "sats") and
    types them so (`GroupCurrency.getTypedFractionDigits`); a form open when it changes (the Mac's Ajustes is a window
    of its own) turns what was typed into the new unit, keeping its value (`TypedText.toUnit`). "Ler cupom fiscal"
    isn't offered in a group in either (no receipt is printed in them), and Início's shortcut opens such a bill
    without the camera; the reader refuses them too.
  - Minimum iOS 26 and macOS 26, so Liquid Glass is native with no fallbacks. The Apple app builds with the iOS 27.1
    SDK (Xcode 27.1), its iPhone Duo APIs behind `#available(iOS 27.1, *)`.
  - The Android UI uses the AndroidX Compose artifacts (BOM), not the JetBrains Compose coordinates; only the shared
    ViewModels take JetBrains' multiplatform lifecycle.
  - The Android launcher icon comes from the legacy app until Bora Rachar has its own.
  - Nothing is guessed while loading (Pierre's call): a section with no concrete answer yet (Room not read, the
    server not answered, or data only the account's first full sync brings while `AccountSnapshot.awaitsFullSync`)
    shows a shimmer in the shape of what comes, never an empty state, a zero, a default or a disabled guess. A read
    of the device only shows it after 150 ms, so a fast one never flashes. Offline while the account's copy is still
    partial, Início and Grupos say "Sem internet. Seus grupos aparecem quando reconectar." and the other screens
    "Sem internet. Seus dados aparecem quando reconectar." under it. The shared state models such a section as a
    `Loadable<T>` (`software.quare:loadable`); each platform draws it with its one shimmer component. A sync that
    fails online ends the wait until a later one gets through or another account is restored (`FirstDownload`): the
    screens show what the device holds under the failed sync's badge, since a change the server keeps refusing fails
    every try. Offline, Conta shows the device's comments and photos and takes new ones. Registrar
    pagamento never waits offline for the receiver's keys: the form opens, so a payment is recorded offline too.
