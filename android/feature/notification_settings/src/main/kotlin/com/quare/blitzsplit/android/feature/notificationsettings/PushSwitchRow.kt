package com.quare.blitzsplit.android.feature.notificationsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.ShimmerAvatarSkeleton
import com.quare.blitzsplit.android.designsystem.component.ShimmerBox
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiEvent
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiState

/*
 * Why: whether this device takes pushes at all, over the rows: off until the system allows them, and turned on it asks
 * the system when it still can. The whole row toggles, as a settings row does.
 */
@Composable
internal fun PushSwitchRow(
    state: NotificationSettingsUiState,
    onEvent: (NotificationSettingsUiEvent) -> Unit,
) {
    val onToggle: (Boolean) -> Unit = { onEvent(NotificationSettingsUiEvent.OnPushToggled(it)) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .then(
                    if (state.showsPushSwitchLoading) {
                        Modifier
                    } else {
                        Modifier.toggleable(
                            value = state.isPushOn,
                            enabled = state.isPushSwitchEnabled,
                            role = Role.Switch,
                            onValueChange = onToggle,
                        )
                    },
                ).heightIn(min = 72.dp)
                .padding(
                    horizontal = 16.dp,
                    vertical = 10.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.notification_settings_push_switch),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.notification_settings_push_switch_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.showsPushSwitchLoading) {
                ShimmerBox {
                    ShimmerAvatarSkeleton(
                        size = 32.dp,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.width(52.dp),
                    )
                }
            } else {
                // Why: the row takes the taps, so the switch only draws.
                Switch(
                    checked = state.isPushOn,
                    onCheckedChange = null,
                    enabled = state.isPushSwitchEnabled,
                )
            }
        }
    }
}
