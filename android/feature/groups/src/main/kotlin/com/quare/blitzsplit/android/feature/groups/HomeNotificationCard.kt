package com.quare.blitzsplit.android.feature.groups

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.NotificationPrompt

/*
 * Why: the soft ask before Android's own dialog, once there is a group, so "Not now" never spends the system's chance;
 * after a refusal it asks once more, and after one for good it sends to the settings.
 */
@Composable
internal fun HomeNotificationCard(
    prompt: NotificationPrompt,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isBlocked = prompt == NotificationPrompt.BLOCKED
    HomeActionCard(
        icon = if (isBlocked) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications,
        title = stringResource(
            if (isBlocked) R.string.groups_home_notifications_off_title else R.string.groups_home_notifications_title,
        ),
        body = stringResource(
            when (prompt) {
                // Why: Android never delivers quietly, so it never shows QUIET.
                NotificationPrompt.SOFT_ASK, NotificationPrompt.QUIET -> R.string.groups_home_notifications_body
                NotificationPrompt.ASK_AGAIN -> R.string.groups_home_notifications_again_body
                NotificationPrompt.BLOCKED -> R.string.groups_home_notifications_off_body
            },
        ),
        secondaryLabel = stringResource(R.string.groups_home_notifications_not_now),
        onSecondaryClick = { onEvent(HomeUiEvent.OnNotificationPromptDismissed) },
        primaryLabel = stringResource(R.string.groups_home_notifications_turn_on),
        onPrimaryClick = { onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked) },
        modifier = modifier,
    )
}
