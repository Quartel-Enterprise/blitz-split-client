package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor

private const val SENTENCE_SEPARATOR = " "

// Why: whether the split adds up, said per mode, with the bar of "Valores" filling up to the total.
@Composable
internal fun SplitStatusCard(
    editor: SplitEditor,
    money: MoneyFormatter,
) {
    val isOk = editor.addsUp
    val title = statusTitleTextOf(
        fact = editor.statusTitle,
        money = money,
    )
    val message = editor.statusDetails
        .map { fact ->
            statusDetailTextOf(
                fact = fact,
                money = money,
            )
        }.joinToString(SENTENCE_SEPARATOR)
    StatusCard(
        isOk = isOk,
        title = title,
        message = message,
        progress = if (editor.mode == SplitMode.AMOUNT && editor.totalMinor > 0) {
            (editor.assignedMinor.toFloat() / editor.totalMinor).coerceIn(0f, 1f)
        } else {
            null
        },
    )
}

/*
 * Why: Dividir's, "Itens da conta"'s and "Item"'s status: whether it adds up, the bar of how much is covered where
 * there is one, the sentences under it and, for the items, the ways out of a difference.
 */
@Composable
internal fun StatusCard(
    isOk: Boolean,
    title: String,
    message: String,
    progress: Float?,
    actions: @Composable () -> Unit = {},
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(16.dp),
        color = if (isOk) {
            MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        contentColor = if (isOk) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 14.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isOk) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            progress?.let {
                LinearProgressIndicator(
                    progress = { it },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (isOk) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
            )
            actions()
        }
    }
}
