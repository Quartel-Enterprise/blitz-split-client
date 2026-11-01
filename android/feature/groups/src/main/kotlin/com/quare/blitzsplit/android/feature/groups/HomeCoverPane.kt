package com.quare.blitzsplit.android.feature.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiState

/*
 * Why: a Flip's cover screen has room for a glance: "Saldo geral", who owes whom in total and the latest bill, read
 * only. Opening the phone draws the whole of Início. The first use has nothing to glance at but the hint.
 */
@Composable
internal fun HomeCoverPane(state: HomeUiState) {
    val main = state.summary.main
    val money = rememberMoneyFormatter(main?.currency ?: rememberRegionCurrency())
    val latestBill = state.billDays
        .firstOrNull()
        ?.bills
        ?.firstOrNull()
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.groups_home_cover_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
            if (state.showsHome) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.groups_summary_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = money.toSignedText(main?.netMinor ?: 0L),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 40.sp,
                            lineHeight = 48.sp,
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        softWrap = false,
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 20.sp,
                            maxFontSize = 40.sp,
                        ),
                    )
                    Text(
                        text = stringResource(
                            R.string.groups_home_cover_totals,
                            money.toText(main?.toReceiveMinor ?: 0L),
                            money.toText(main?.toPayMinor ?: 0L),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                latestBill?.let { bill ->
                    HomeBillRow(
                        bill = bill,
                        shape = RoundedCornerShape(20.dp),
                        isCompact = true,
                        onClick = null,
                    )
                }
            }
            Text(
                text = stringResource(R.string.groups_home_cover_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
