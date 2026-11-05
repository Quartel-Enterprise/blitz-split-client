package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.quare.blitzsplit.android.designsystem.R

private const val MAX_BADGE_COUNT = 99

/*
 * Why: Início's bell, which opens Atividade over it, now that Atividade left the tabs (Pierre's call). Its unread count
 * shows in the primary color since it asks for a look, never an error; TalkBack reads it with the name ("Atividade,
 * 3 não lidas").
 */
@Composable
fun ActivityBellButton(
    unreadCount: Int,
    onClick: () -> Unit,
) {
    val label = stringResource(R.string.designsystem_activity)
    IconButton(onClick = onClick) {
        if (unreadCount <= 0) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = label,
            )
            return@IconButton
        }
        val description = pluralStringResource(R.plurals.designsystem_activity_unread, unreadCount, unreadCount)
        BadgedBox(
            badge = {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text(if (unreadCount > MAX_BADGE_COUNT) "$MAX_BADGE_COUNT+" else unreadCount.toString())
                }
            },
            modifier = Modifier.semantics { stateDescription = description },
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = label,
            )
        }
    }
}
