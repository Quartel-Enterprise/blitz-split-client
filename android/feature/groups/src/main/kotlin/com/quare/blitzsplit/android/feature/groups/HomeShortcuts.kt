package com.quare.blitzsplit.android.feature.groups

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiState
import com.quare.blitzsplit.android.designsystem.R as DesignR

private val outerCorner = 24.dp
private val innerCorner = 8.dp

private class Shortcut(
    @StringRes val label: Int,
    val icon: ImageVector,
    val isEnabled: Boolean,
    val event: HomeUiEvent,
)

/*
 * Why: the three shortcuts, joined as one connected group. "Rachar conta" is the FAB, so it is not repeated here;
 * "Ler cupom fiscal" needs the internet and says so offline. Every tile takes the tallest one's height, so a label
 * wrapping to two lines never leaves the others shorter.
 */
@Composable
internal fun HomeShortcutsRow(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    metrics: HomeMetrics,
    modifier: Modifier = Modifier,
) {
    val shortcuts = listOf(
        Shortcut(
            label = R.string.groups_home_scan_receipt,
            icon = Icons.Outlined.DocumentScanner,
            isEnabled = state.canScanReceipt,
            event = HomeUiEvent.OnScanReceiptClicked,
        ),
        Shortcut(
            label = R.string.groups_home_record_payment,
            icon = Icons.Outlined.Payments,
            isEnabled = true,
            event = HomeUiEvent.OnRecordPaymentClicked,
        ),
        Shortcut(
            label = R.string.groups_new_group,
            icon = Icons.Outlined.GroupAdd,
            isEnabled = true,
            event = HomeUiEvent.OnCreateGroupClicked,
        ),
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        shortcuts.forEachIndexed { index, shortcut ->
            ShortcutTile(
                shortcut = shortcut,
                shape = RoundedCornerShape(
                    topStart = if (index == 0) outerCorner else innerCorner,
                    bottomStart = if (index == 0) outerCorner else innerCorner,
                    topEnd = if (index == shortcuts.lastIndex) outerCorner else innerCorner,
                    bottomEnd = if (index == shortcuts.lastIndex) outerCorner else innerCorner,
                ),
                isCompact = metrics.isCompact,
                onClick = { onEvent(shortcut.event) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

/*
 * Why: the icon always sits over the label: beside it, in a compact tile (a book's half, a phone lying down), the label
 * kept a sliver that broke its words in the middle ("Scan a receip/t"). Compact only shrinks the icon and the margins.
 */
@Composable
private fun ShortcutTile(
    shortcut: Shortcut,
    shape: RoundedCornerShape,
    isCompact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = shortcut.isEnabled,
        modifier = modifier.heightIn(min = if (isCompact) 72.dp else 96.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = if (shortcut.isEnabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        // Why: off, the tile still says "needs internet", so it is muted (Surface's content color), never faded.
        Column(
            modifier = Modifier.padding(
                horizontal = 6.dp,
                vertical = if (isCompact) 8.dp else 12.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .size(if (isCompact) 36.dp else 40.dp)
                    .background(
                        if (shortcut.isEnabled) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = shortcut.icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = if (shortcut.isEnabled) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(shortcut.label),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!shortcut.isEnabled) {
                    Text(
                        text = stringResource(DesignR.string.designsystem_group_exit_needs_internet),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/*
 * Why: "Guarde seu código de recuperação", after the first sync of an account without Google or Apple. ✕ hides it
 * until the next launch; "Ver código" opens the code's sheet, "Conectar conta" the profile.
 */
@Composable
internal fun HomeRecoveryCodeCard(
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeActionCard(
        icon = Icons.Outlined.Key,
        title = stringResource(R.string.groups_home_recovery_title),
        body = stringResource(R.string.groups_home_recovery_body),
        secondaryLabel = stringResource(R.string.groups_home_recovery_connect),
        onSecondaryClick = { onEvent(HomeUiEvent.OnConnectAccountClicked) },
        primaryLabel = stringResource(R.string.groups_home_recovery_show),
        onPrimaryClick = { onEvent(HomeUiEvent.OnShowRecoveryCodeClicked) },
        modifier = modifier,
        dismissLabel = stringResource(R.string.groups_home_recovery_dismiss),
        onDismiss = { onEvent(HomeUiEvent.OnRecoveryCodeCardDismissed) },
    )
}

// Why: a card of Início that asks for something, its answers at the bottom end and, when it has one, ✕ at the top.
@Composable
internal fun HomeActionCard(
    icon: ImageVector,
    title: String,
    body: String,
    secondaryLabel: String,
    onSecondaryClick: () -> Unit,
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String? = null,
    onDismiss: () -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 8.dp,
                top = 16.dp,
                bottom = 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = if (dismissLabel == null) 8.dp else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (dismissLabel != null) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = dismissLabel,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(
                    onClick = onSecondaryClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                ) { Text(secondaryLabel) }
                Button(
                    onClick = onPrimaryClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) { Text(primaryLabel) }
            }
        }
    }
}
