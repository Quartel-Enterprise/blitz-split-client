package com.quare.blitzsplit.wear.ui.balances

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchBalanceDirection
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchBalancesUiEvent
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchBalancesUiState
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchGroupRow
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchTotal
import com.quare.blitzsplit.feature.watchbalances.presentation.viewmodel.WatchBalancesViewModel
import com.quare.blitzsplit.wear.R
import com.quare.blitzsplit.wear.ui.component.ListScrollIndicator
import com.quare.blitzsplit.wear.ui.theme.LocalMoneyColors
import com.quare.blitzsplit.wear.ui.theme.WearTheme
import com.quare.blitzsplit.wear.ui.theme.moneyColorOf
import org.koin.compose.viewmodel.koinViewModel

private const val PERMISSION_PREFERENCES = "wear_permissions"
private const val KEY_NOTIFICATIONS_ASKED = "notifications_asked"

@Composable
fun WatchBalancesScreen(viewModel: WatchBalancesViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NotificationPermissionEffect()
    WatchBalancesContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
    SignOutDialog(
        isVisible = state.isConfirmingSignOut,
        onEvent = viewModel::onEvent,
    )
}

/*
 * Why: the watch's own pushes need the permission from Wear OS 4 (API 33) on; asked once the watch has the account,
 * on its first Balances, as there is nothing to notify about before. Only once: a "Don't allow" isn't asked again
 * each time Balances shows.
 */
@Composable
private fun NotificationPermissionEffect() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val preferences = context.getSharedPreferences(PERMISSION_PREFERENCES, Context.MODE_PRIVATE)
        if (preferences.getBoolean(KEY_NOTIFICATIONS_ASKED, false)) return@LaunchedEffect
        val isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (isGranted) return@LaunchedEffect
        preferences.edit { putBoolean(KEY_NOTIFICATIONS_ASKED, true) }
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

// Why: "Sign out of this watch?", the one way to disconnect only the watch; the phone stays signed in.
@Composable
private fun SignOutDialog(
    isVisible: Boolean,
    onEvent: (WatchBalancesUiEvent) -> Unit,
) {
    AlertDialog(
        visible = isVisible,
        onDismissRequest = { onEvent(WatchBalancesUiEvent.OnSignOutDismissed) },
        icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
        title = { Text(stringResource(R.string.balances_sign_out_title)) },
        text = { Text(stringResource(R.string.balances_sign_out_body)) },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(onClick = { onEvent(WatchBalancesUiEvent.OnSignOutConfirmed) })
        },
        dismissButton = {
            AlertDialogDefaults.DismissButton(onClick = { onEvent(WatchBalancesUiEvent.OnSignOutDismissed) })
        },
    )
}

// Why: "Balances" (15.3): the overall balance, each open group and the last sync, in one list that curves at the edges.
@Composable
fun WatchBalancesContent(
    state: WatchBalancesUiState,
    onEvent: (WatchBalancesUiEvent) -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(
        scrollState = listState,
        scrollIndicator = { ListScrollIndicator(listState) },
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec)
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(spec),
                ) { Text(stringResource(R.string.balances_title)) }
            }
            if (state.showsOfflineNotice) {
                item { OfflineLine(queuedCount = state.queuedChangeCount) }
            }
            when {
                !state.isLoaded -> item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }

                state.isAllSettled -> item { SettledMessage() }

                else -> {
                    items(state.totals, key = { it.currency.code }) { total ->
                        TotalCard(
                            total = total,
                            modifier = Modifier
                                .transformedHeight(this, spec)
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                            transformation = SurfaceTransformation(spec),
                        )
                    }
                    items(state.groups, key = { it.groupId + it.currency.code }) { group ->
                        GroupButton(
                            group = group,
                            onClick = { onEvent(WatchBalancesUiEvent.OnGroupClicked(group.groupId, group.currency)) },
                            modifier = Modifier
                                .transformedHeight(this, spec)
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                            transformation = SurfaceTransformation(spec),
                        )
                    }
                }
            }
            state.lastSyncedAt?.let { syncedAt ->
                item { UpdatedAtLine(syncedAt) }
            }
            if (state.isSignOutScheduled) {
                item { SignOutScheduledSection() }
            }
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    FilledTonalButton(
                        onClick = {
                            onEvent(
                                if (state.isSignOutScheduled) {
                                    WatchBalancesUiEvent.OnCancelScheduledSignOutClicked
                                } else {
                                    WatchBalancesUiEvent.OnSignOutClicked
                                },
                            )
                        },
                        label = {
                            Text(
                                stringResource(
                                    if (state.isSignOutScheduled) R.string.pair_cancel else R.string.balances_sign_out,
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OfflineLine(queuedCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (queuedCount > 0) PendingDotIcon()
        Text(
            text = if (queuedCount > 0) {
                pluralStringResource(R.plurals.balances_offline_queued, queuedCount, queuedCount)
            } else {
                stringResource(R.string.balances_offline)
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun PendingDotIcon() {
    Box(
        Modifier
            .size(6.dp)
            .background(LocalMoneyColors.current.pending, CircleShape),
    )
}

@Composable
private fun TotalCard(
    total: WatchTotal,
    modifier: Modifier,
    transformation: SurfaceTransformation,
) {
    val money = rememberMoneyFormatter(total.currency)
    val color = moneyColorOf(total.direction)
    Card(
        onClick = {},
        enabled = false,
        modifier = modifier.fillMaxWidth(),
        transformation = transformation,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.balances_overall),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = money.toSignedText(total.netMinor),
                style = MaterialTheme.typography.titleLarge,
                color = color,
                maxLines = 1,
            )
            Text(
                text = stringResource(
                    when (total.direction) {
                        WatchBalanceDirection.TO_RECEIVE -> R.string.balances_to_receive
                        WatchBalanceDirection.TO_PAY -> R.string.balances_you_owe
                        WatchBalanceDirection.EVEN -> R.string.balances_group_even
                    },
                ),
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
    }
}

@Composable
private fun GroupButton(
    group: WatchGroupRow,
    onClick: () -> Unit,
    modifier: Modifier,
    transformation: SurfaceTransformation,
) {
    val money = rememberMoneyFormatter(group.currency)
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        transformation = transformation,
        colors = ButtonDefaults.filledTonalButtonColors(),
        icon = {
            GroupBadge(
                initials = group.initials,
                color = group.color,
                size = 28.dp,
                photoUrl = group.photoUrl,
            )
        },
        label = {
            Text(
                text = group.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        secondaryLabel = {
            Column {
                val amount = money.toSignedText(group.netMinor)
                Text(
                    text = when (group.direction) {
                        WatchBalanceDirection.TO_PAY -> stringResource(R.string.balances_group_you_owe, amount)

                        WatchBalanceDirection.TO_RECEIVE -> stringResource(R.string.balances_group_to_receive, amount)

                        // Why: open pairs that offset each other: nothing to settle overall, but each has its "I paid".
                        WatchBalanceDirection.EVEN -> stringResource(R.string.balances_group_even)
                    },
                    color = moneyColorOf(group.direction),
                    maxLines = 1,
                )
                if (group.isQueued) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PendingDotIcon()
                        Text(
                            text = stringResource(R.string.balances_waiting),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    )
}

// Why: 15.3b, nothing open: you owe nothing and nobody owes you.
@Composable
private fun SettledMessage() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Icons.Outlined.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.balances_settled_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.balances_settled_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// Why: as the phone's Profile says it, while what the watch queued waits to go up; Cancel keeps it signed in.
@Composable
private fun SignOutScheduledSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.balances_sign_out_scheduled),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.balances_sign_out_scheduled_detail),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun UpdatedAtLine(syncedAt: Long) {
    val time = rememberDateTimeTextFormatter().toTimeText(syncedAt)
    Text(
        text = stringResource(R.string.balances_updated_at, time),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

private fun createPreviewState(isSettled: Boolean): WatchBalancesUiState = WatchBalancesUiState(
    isLoaded = true,
    totals = if (isSettled) {
        emptyList()
    } else {
        listOf(
            WatchTotal(
                GroupCurrency.BRL,
                netMinor = 18_640,
                toReceiveMinor = 31_240,
                toPayMinor = 12_600,
            ),
        )
    },
    groups = if (isSettled) {
        emptyList()
    } else {
        listOf(
            WatchGroupRow("1", "Viagem Floripa", "VF", AvatarColor.BLUE, null, GroupCurrency.BRL, 21_490, false),
            WatchGroupRow("2", "Apê 302", "A3", AvatarColor.LILAC, null, GroupCurrency.BRL, -9_800, true),
            WatchGroupRow("3", "Churrasco de sábado", "CS", AvatarColor.GREEN, null, GroupCurrency.BRL, 6_950, false),
        )
    },
    isOnline = isSettled,
    queuedChangeCount = if (isSettled) 0 else 1,
    lastSyncedAt = 1_795_000_000_000,
    isConfirmingSignOut = false,
    isSignOutScheduled = false,
)

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun BalancesPreview() {
    WearTheme {
        WatchBalancesContent(
            state = createPreviewState(isSettled = false),
            onEvent = {},
        )
    }
}

@WearPreviewDevices
@Composable
private fun SettledPreview() {
    WearTheme {
        WatchBalancesContent(
            state = createPreviewState(isSettled = true),
            onEvent = {},
        )
    }
}
