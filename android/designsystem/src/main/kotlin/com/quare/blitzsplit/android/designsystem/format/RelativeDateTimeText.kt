package com.quare.blitzsplit.android.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay

/*
 * Why: "Hoje, 28 de out. · 21:30", "Ontem, 27 de out. · 21:30" or "Sex., 24 de out. · 21:30", Rachar conta's and
 * Registrar pagamento's "Quando".
 */
@Composable
fun relativeDateTimeTextOf(
    epochMillis: Long,
    day: RelativeDay,
    dates: DateTimeTextFormatter,
): String {
    val time = dates.toTimeText(epochMillis)
    return when (day) {
        RelativeDay.TODAY -> stringResource(R.string.designsystem_when_today, dates.toDateText(epochMillis), time)

        RelativeDay.YESTERDAY -> stringResource(
            R.string.designsystem_when_yesterday,
            dates.toDateText(epochMillis),
            time,
        )

        RelativeDay.OTHER -> stringResource(
            R.string.designsystem_when_other,
            dates.toWeekdayDateText(epochMillis),
            time,
        )
    }
}
