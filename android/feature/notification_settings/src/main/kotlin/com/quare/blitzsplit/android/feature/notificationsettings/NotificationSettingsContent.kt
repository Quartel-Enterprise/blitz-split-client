package com.quare.blitzsplit.android.feature.notificationsettings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationPermission
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiEvent
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiState
import com.quare.blitzsplit.android.designsystem.R as DesignR

private val singleColumnMaxWidth = 592.dp
private const val DISABLED_ALPHA = 0.38f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsContent(
    state: NotificationSettingsUiState,
    onEvent: (NotificationSettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notification_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(NotificationSettingsUiEvent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(DesignR.string.designsystem_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = singleColumnMaxWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
            ) {
                if (state.showsPermissionDenied) {
                    PermissionDeniedBanner(onEvent = onEvent)
                }
                Text(
                    text = stringResource(R.string.notification_settings_section),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = 4.dp,
                        top = 20.dp,
                        bottom = 8.dp,
                    ),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    state.types.forEachIndexed { index, type ->
                        NotificationRow(
                            type = type,
                            isChecked = state.isEnabled(type),
                            isEnabled = state.areTogglesEnabled,
                            shape = createRowShape(
                                index = index,
                                count = state.types.size,
                            ),
                            onEvent = onEvent,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.notification_settings_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = 4.dp,
                        end = 4.dp,
                        top = 12.dp,
                        bottom = 16.dp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun PermissionDeniedBanner(onEvent: (NotificationSettingsUiEvent) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsOff,
                    contentDescription = null,
                )
                val title = stringResource(R.string.notification_settings_denied_title)
                val body = stringResource(R.string.notification_settings_denied_body)
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(title) }
                        append(" ")
                        append(body)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = { onEvent(NotificationSettingsUiEvent.OnOpenSettingsClicked) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) { Text(stringResource(R.string.notification_settings_open_settings)) }
        }
    }
}

@Composable
private fun NotificationRow(
    type: NotificationType,
    isChecked: Boolean,
    isEnabled: Boolean,
    shape: RoundedCornerShape,
    onEvent: (NotificationSettingsUiEvent) -> Unit,
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isEnabled) 1f else DISABLED_ALPHA),
    ) {
        Row(
            modifier = Modifier
                .toggleable(
                    value = isChecked,
                    enabled = isEnabled,
                    role = Role.Switch,
                    onValueChange = { onEvent(NotificationSettingsUiEvent.OnToggled(type, it)) },
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
                    text = stringResource(type.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(type.detailRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Why: the whole row toggles, so the switch only shows the state.
            Switch(
                checked = isChecked,
                onCheckedChange = null,
                enabled = isEnabled,
            )
        }
    }
}

// Why: the grouped rows of the canvas, round at the ends of the group and tight between rows.
private fun createRowShape(
    index: Int,
    count: Int,
): RoundedCornerShape {
    val top = if (index == 0) 20.dp else 6.dp
    val bottom = if (index == count - 1) 20.dp else 6.dp
    return RoundedCornerShape(
        topStart = top,
        topEnd = top,
        bottomStart = bottom,
        bottomEnd = bottom,
    )
}

@get:StringRes
private val NotificationType.titleRes: Int
    get() = when (this) {
        NotificationType.NEW_EXPENSE -> R.string.notification_settings_new_expense
        NotificationType.PAYMENT_MARKED -> R.string.notification_settings_payment_marked
        NotificationType.SETTLE_REMINDERS -> R.string.notification_settings_settle_reminders
        NotificationType.INVITES -> R.string.notification_settings_invites
    }

@get:StringRes
private val NotificationType.detailRes: Int
    get() = when (this) {
        NotificationType.NEW_EXPENSE -> R.string.notification_settings_new_expense_detail
        NotificationType.PAYMENT_MARKED -> R.string.notification_settings_payment_marked_detail
        NotificationType.SETTLE_REMINDERS -> R.string.notification_settings_settle_reminders_detail
        NotificationType.INVITES -> R.string.notification_settings_invites_detail
    }

@Preview
@Composable
private fun NotificationSettingsPreview() {
    BlitzSplitTheme {
        NotificationSettingsContent(
            state = NotificationSettingsUiState(
                enabledTypes = NotificationPreferences.DEFAULT.enabledTypes,
                permission = NotificationPermission.GRANTED,
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Denied in the system")
@Composable
private fun NotificationSettingsDeniedPreview() {
    BlitzSplitTheme {
        NotificationSettingsContent(
            state = NotificationSettingsUiState(
                enabledTypes = NotificationPreferences.DEFAULT.enabledTypes,
                permission = NotificationPermission.DENIED,
            ),
            onEvent = {},
        )
    }
}
