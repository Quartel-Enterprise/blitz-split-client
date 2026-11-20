package com.quare.blitzsplit.android.storescreenshots

import androidx.compose.runtime.Composable
import com.quare.blitzsplit.shared.storescreenshots.SampleTrip

// The screens Google Play's art shows (PlayArtCaptures), by the file name the art reads them under.
internal enum class PlayArtScreen(
    val fileName: String,
    val content: @Composable (SampleTrip) -> Unit,
) {
    GROUP("01_group", { GroupScreenshotContent(it) }),
    ITEMS("02_split_by_items", { ItemsScreenshotContent(it) }),
    RECEIPT("03_receipt", { ReceiptScreenshotContent(it) }),
    BALANCES("04_balances", { BalancesScreenshotContent(it) }),
    INVITE("05_invite", { InviteScreenshotContent(it) }),
    STATISTICS("06_statistics", { StatisticsScreenshotContent(it) }),
    HOME("07_home", { HomeScreenshotContent(it) }),
    PAYERS("08_payers_and_currencies", { PayersScreenshotContent(it) }),
    PAY("11_pay", { PayScreenshotContent(it) }),
}
