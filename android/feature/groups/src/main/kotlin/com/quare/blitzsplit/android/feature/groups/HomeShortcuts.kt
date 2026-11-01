package com.quare.blitzsplit.android.feature.groups

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiState
import com.quare.blitzsplit.android.designsystem.R as DesignR

private const val DISABLED_ALPHA = 0.6f
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
 * "Ler cupom fiscal" needs the internet and says so offline.
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
        modifier = modifier.fillMaxWidth(),
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
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// Why: lying down there is no height for a column, so the icon goes beside the label.
@Composable
private fun ShortcutTile(
    shortcut: Shortcut,
    shape: RoundedCornerShape,
    isCompact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon: @Composable () -> Unit = {
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
    }
    val label: @Composable () -> Unit = {
        Column(horizontalAlignment = if (isCompact) Alignment.Start else Alignment.CenterHorizontally) {
            Text(
                text = stringResource(shortcut.label),
                style = MaterialTheme.typography.labelLarge,
                textAlign = if (isCompact) TextAlign.Start else TextAlign.Center,
            )
            if (!shortcut.isEnabled) {
                Text(
                    text = stringResource(DesignR.string.designsystem_group_exit_needs_internet),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = if (isCompact) TextAlign.Start else TextAlign.Center,
                )
            }
        }
    }
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
        val contentModifier = Modifier
            .alpha(if (shortcut.isEnabled) 1f else DISABLED_ALPHA)
            .padding(
                horizontal = 6.dp,
                vertical = if (isCompact) 8.dp else 12.dp,
            )
        if (isCompact) {
            Row(
                modifier = contentModifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon()
                label()
            }
        } else {
            Column(
                modifier = contentModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            ) {
                icon()
                label()
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
                    imageVector = Icons.Outlined.Key,
                    contentDescription = null,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.groups_home_recovery_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.groups_home_recovery_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                IconButton(onClick = { onEvent(HomeUiEvent.OnRecoveryCodeCardDismissed) }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.groups_home_recovery_dismiss),
                    )
                }
            }
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(
                    onClick = { onEvent(HomeUiEvent.OnConnectAccountClicked) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                ) { Text(stringResource(R.string.groups_home_recovery_connect)) }
                Button(
                    onClick = { onEvent(HomeUiEvent.OnShowRecoveryCodeClicked) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) { Text(stringResource(R.string.groups_home_recovery_show)) }
            }
        }
    }
}
