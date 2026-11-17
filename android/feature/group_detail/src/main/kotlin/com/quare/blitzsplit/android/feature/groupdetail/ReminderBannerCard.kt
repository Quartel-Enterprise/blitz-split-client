package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.reminderScheduleText
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ReminderBanner

/*
 * Why: "Hora de acertar as contas", only for someone who owes in the group, once the group's reminder arrived or passed:
 * "Acertar" opens Registrar pagamento as the balance card's does, "Dispensar" hides it until the next occurrence.
 */
@Composable
internal fun ReminderBannerCard(
    banner: ReminderBanner,
    onSettleClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsActive,
                    contentDescription = null,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.group_detail_reminder_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = reminderScheduleText(banner.reminder),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSettleClick) { Text(stringResource(R.string.group_detail_reminder_settle)) }
                TextButton(onClick = onDismissClick) { Text(stringResource(R.string.group_detail_reminder_dismiss)) }
            }
        }
    }
}
