package com.quare.blitzsplit.android.storescreenshots

/**
 * The listing's screenshots, in shelf order: both stores show them by file name, so the prefix of [fileName] is the
 * order, and only the first two or three are seen without swiping. Play holds 8 per device type and the App Store
 * 10 per display size, hence [isAppStoreOnly]. Every shot is in the dark theme except [LIGHT_THEME].
 */
internal enum class StoreScreen(
    val fileName: String,
    val isAppStoreOnly: Boolean,
    val isDark: Boolean,
) {
    GROUP(
        fileName = "01_group",
        isAppStoreOnly = false,
        isDark = true,
    ),
    ITEMS(
        fileName = "02_split_by_items",
        isAppStoreOnly = false,
        isDark = true,
    ),
    RECEIPT(
        fileName = "03_receipt",
        isAppStoreOnly = false,
        isDark = true,
    ),
    BALANCES(
        fileName = "04_balances",
        isAppStoreOnly = false,
        isDark = true,
    ),
    INVITE(
        fileName = "05_invite",
        isAppStoreOnly = false,
        isDark = true,
    ),
    STATISTICS(
        fileName = "06_statistics",
        isAppStoreOnly = false,
        isDark = true,
    ),
    HOME(
        fileName = "07_home",
        isAppStoreOnly = false,
        isDark = true,
    ),
    PAYERS_AND_CURRENCIES(
        fileName = "08_payers_and_currencies",
        isAppStoreOnly = false,
        isDark = true,
    ),
    ACTIVITY(
        fileName = "09_activity",
        isAppStoreOnly = true,
        isDark = true,
    ),
    LIGHT_THEME(
        fileName = "10_light_theme",
        isAppStoreOnly = true,
        isDark = false,
    ),
}
