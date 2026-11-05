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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.outlined.MobileOff
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.ShimmerAvatarSkeleton
import com.quare.blitzsplit.android.designsystem.component.ShimmerBox
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.NotificationChannel
import com.quare.blitzsplit.core.model.NotificationPermission
import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiEvent
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiState
import software.quare.loadable.Loadable
import software.quare.loadable.toLoaded
import com.quare.blitzsplit.android.designsystem.R as DesignR

private val singleColumnMaxWidth = 592.dp
private val cellWidth = 48.dp
private val cellSpacing = 4.dp

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
        // Why: one short column at every size, so lying down and on a tablet it only centers, as 4.8 draws it.
        AdaptiveScreen(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = singleColumnMaxWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
            ) {
                if (state.showsPermissionDenied) {
                    PermissionDeniedBanner(
                        canAskAgain = state.canAskAgain,
                        onEvent = onEvent,
                    )
                }
                ChannelHeader()
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    state.types.forEachIndexed { index, type ->
                        NotificationRow(
                            type = type,
                            state = state,
                            shape = createRowShape(
                                index = index,
                                count = state.types.size,
                            ),
                            onEvent = onEvent,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.notification_settings_legend),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = 4.dp,
                        end = 4.dp,
                        top = 12.dp,
                    ),
                )
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
private fun PermissionDeniedBanner(
    // Why: refused once, Android still shows its dialog once more, so "Turn on" asks instead of opening the settings.
    canAskAgain: Boolean,
    onEvent: (NotificationSettingsUiEvent) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.extraLarge,
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
                val body = stringResource(
                    if (canAskAgain) {
                        R.string.notification_settings_refused_body
                    } else {
                        R.string.notification_settings_denied_body
                    },
                )
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
                onClick = {
                    onEvent(
                        if (canAskAgain) {
                            NotificationSettingsUiEvent.OnAskAgainClicked
                        } else {
                            NotificationSettingsUiEvent.OnOpenSettingsClicked
                        },
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(
                    stringResource(
                        if (canAskAgain) {
                            R.string.notification_settings_turn_on
                        } else {
                            R.string.notification_settings_open_settings
                        },
                    ),
                )
            }
        }
    }
}

// Why: "Notify me about", and over the toggles the names of their columns, Push and In app.
@Composable
private fun ChannelHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 4.dp,
                end = 16.dp,
                top = 20.dp,
                bottom = 8.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(cellSpacing),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = stringResource(R.string.notification_settings_section),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        NotificationChannel.entries.forEach { channel ->
            Text(
                text = stringResource(channel.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.width(cellWidth),
            )
        }
    }
}

@Composable
private fun NotificationRow(
    type: NotificationType,
    state: NotificationSettingsUiState,
    shape: RoundedCornerShape,
    onEvent: (NotificationSettingsUiEvent) -> Unit,
) {
    val title = stringResource(type.titleRes)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 72.dp)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 10.dp,
                    bottom = 10.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(cellSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(type.detailRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.channels.forEach { channel ->
                Box(
                    modifier = Modifier.width(cellWidth),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        // Why: the weekly reminder never shows in Activity, so it has no In app toggle.
                        !state.hasToggle(type, channel) -> Unit

                        // Why: until the choices and the system's permission are known, it shimmers, neither on nor off.
                        state.showsTogglesLoading -> ShimmerBox {
                            ShimmerAvatarSkeleton(size = 40.dp)
                        }

                        else -> ChannelToggle(
                            type = type,
                            channel = channel,
                            title = title,
                            isChecked = state.isEnabled(type, channel),
                            isEnabled = state.areTogglesEnabled(channel),
                            onEvent = onEvent,
                        )
                    }
                }
            }
        }
    }
}

// Why: a filled icon when on and a crossed-out one when off, so the state never rests on the color alone.
@Composable
private fun ChannelToggle(
    type: NotificationType,
    channel: NotificationChannel,
    title: String,
    isChecked: Boolean,
    isEnabled: Boolean,
    onEvent: (NotificationSettingsUiEvent) -> Unit,
) {
    val description = stringResource(
        R.string.notification_settings_toggle_description,
        title,
        stringResource(channel.labelRes),
    )
    FilledIconToggleButton(
        checked = isChecked,
        onCheckedChange = { onEvent(NotificationSettingsUiEvent.OnToggled(type, channel, it)) },
        enabled = isEnabled,
        modifier = Modifier.semantics {
            contentDescription = description
            role = Role.Switch
        },
    ) {
        Icon(
            imageVector = channel.iconOf(isChecked),
            contentDescription = null,
        )
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
        NotificationType.BILLS -> R.string.notification_settings_bills
        NotificationType.PAYMENTS -> R.string.notification_settings_payments
        NotificationType.COMMENTS -> R.string.notification_settings_comments
        NotificationType.PEOPLE_AND_GROUPS -> R.string.notification_settings_people_and_groups
        NotificationType.PAYMENT_REQUESTS -> R.string.notification_settings_payment_requests
        NotificationType.BALANCE_REMINDER -> R.string.notification_settings_balance_reminder
    }

@get:StringRes
private val NotificationType.detailRes: Int
    get() = when (this) {
        NotificationType.BILLS -> R.string.notification_settings_bills_detail
        NotificationType.PAYMENTS -> R.string.notification_settings_payments_detail
        NotificationType.COMMENTS -> R.string.notification_settings_comments_detail
        NotificationType.PEOPLE_AND_GROUPS -> R.string.notification_settings_people_and_groups_detail
        NotificationType.PAYMENT_REQUESTS -> R.string.notification_settings_payment_requests_detail
        NotificationType.BALANCE_REMINDER -> R.string.notification_settings_balance_reminder_detail
    }

@get:StringRes
private val NotificationChannel.labelRes: Int
    get() = when (this) {
        NotificationChannel.PUSH -> R.string.notification_settings_push
        NotificationChannel.IN_APP -> R.string.notification_settings_in_app
    }

private fun NotificationChannel.iconOf(isChecked: Boolean): ImageVector = when (this) {
    NotificationChannel.PUSH -> if (isChecked) Icons.Filled.PhoneAndroid else Icons.Outlined.MobileOff
    NotificationChannel.IN_APP -> if (isChecked) Icons.Filled.Notifications else Icons.Outlined.NotificationsOff
}

@DevicePreviews
@Composable
private fun NotificationSettingsPreview() {
    BlitzSplitTheme {
        NotificationSettingsContent(
            state = NotificationSettingsUiState(
                preferences = NotificationPreferences.DEFAULT.toLoaded(),
                permission = NotificationPermission.GRANTED,
            ),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun NotificationSettingsLoadingPreview() {
    BlitzSplitTheme {
        NotificationSettingsContent(
            state = NotificationSettingsUiState(
                preferences = Loadable.Loading,
                permission = null,
            ),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun NotificationSettingsDeniedPreview() {
    BlitzSplitTheme {
        NotificationSettingsContent(
            state = NotificationSettingsUiState(
                preferences = NotificationPreferences.DEFAULT.toLoaded(),
                permission = NotificationPermission.DENIED,
            ),
            onEvent = {},
        )
    }
}
