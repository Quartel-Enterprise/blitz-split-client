package com.quare.blitzsplit.wear.ui.balances

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SuccessConfirmationDialog
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.confirmationDialogCurvedText
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchDebt
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchGroupUiAction
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchGroupUiEvent
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchGroupUiState
import com.quare.blitzsplit.feature.watchbalances.presentation.viewmodel.WatchGroupViewModel
import com.quare.blitzsplit.wear.R
import com.quare.blitzsplit.wear.ui.component.ListScrollIndicator
import com.quare.blitzsplit.wear.ui.theme.WearTheme
import com.quare.blitzsplit.wear.ui.theme.moneyColorOf
import kotlin.math.abs

@Composable
fun WatchGroupScreen(viewModel: WatchGroupViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showsRecorded by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                WatchGroupUiAction.ShowPaymentRecorded -> showsRecorded = true
            }
        }
    }
    WatchGroupContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
    // Why: 15.6, Wear's own confirmation, which goes back to the balances once it ends.
    val recorded = stringResource(R.string.group_payment_recorded)
    val style = ConfirmationDialogDefaults.curvedTextStyle
    SuccessConfirmationDialog(
        visible = showsRecorded,
        onDismissRequest = {
            showsRecorded = false
            viewModel.onEvent(WatchGroupUiEvent.OnRecordedShown)
        },
        curvedText = { confirmationDialogCurvedText(recorded, style) },
    )
}

// Why: "I paid" as the EdgeButton while you owe one person; with no button the list keeps its whole height.
@Composable
private fun GroupScaffold(
    listState: TransformingLazyColumnState,
    payDebt: WatchDebt?,
    onEvent: (WatchGroupUiEvent) -> Unit,
    content: @Composable BoxScope.(PaddingValues) -> Unit,
) {
    if (payDebt == null) {
        ScreenScaffold(
            scrollState = listState,
            scrollIndicator = { ListScrollIndicator(listState) },
            content = content,
        )
        return
    }
    ScreenScaffold(
        scrollState = listState,
        scrollIndicator = { ListScrollIndicator(listState) },
        edgeButton = {
            EdgeButton(
                onClick = { onEvent(WatchGroupUiEvent.OnIPaidClicked(payDebt.memberId)) },
                modifier = Modifier.scrollable(
                    listState,
                    orientation = Orientation.Vertical,
                    reverseDirection = true,
                    overscrollEffect = rememberOverscrollEffect(),
                ),
            ) { Text(stringResource(R.string.group_i_paid)) }
        },
        content = content,
    )
}

// Why: a group on the watch (15.4): your net in it, each pair with you, and "I paid" while you owe one person.
@Composable
fun WatchGroupContent(
    state: WatchGroupUiState,
    onEvent: (WatchGroupUiEvent) -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val money = rememberMoneyFormatter(state.currency)
    GroupScaffold(
        listState = listState,
        payDebt = state.singleDebtToPay?.takeIf { state.showsIPaid },
        onEvent = onEvent,
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
                ) {
                    Text(
                        state.groupName.orEmpty(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            item {
                Text(
                    text = money.toSignedText(state.netMinor),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    color = moneyColorOf(state.netMinor),
                    textAlign = TextAlign.Center,
                )
            }
            if (state.isQueued) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
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
            items(state.debts, key = { it.memberId }) { debt ->
                DebtRow(
                    debt = debt,
                    money = money,
                    isClickable = state.areDebtsClickable && debt.isOwedByMe,
                    onClick = { onEvent(WatchGroupUiEvent.OnIPaidClicked(debt.memberId)) },
                    modifier = Modifier
                        .transformedHeight(this, spec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(spec),
                )
            }
        }
    }
    ConfirmPaymentDialog(
        state = state,
        money = money,
        onEvent = onEvent,
    )
}

@Composable
private fun DebtRow(
    debt: WatchDebt,
    money: MoneyFormatter,
    isClickable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    transformation: SurfaceTransformation,
) {
    val name = debt.name ?: stringResource(R.string.group_former_member)
    val sentence = if (debt.isOwedByMe) {
        stringResource(R.string.group_you_owe_person, name)
    } else {
        stringResource(R.string.group_owes_you, name)
    }
    Card(
        onClick = onClick,
        enabled = isClickable,
        modifier = modifier.fillMaxWidth(),
        transformation = transformation,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarBadge(
                initial = debt.initials,
                color = debt.color ?: AvatarColor.BLUE,
                size = 24.dp,
                photoUrl = debt.photoUrl,
            )
            Column {
                Text(
                    text = sentence,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                )
                Text(
                    text = money.toText(abs(debt.amountMinor)),
                    style = MaterialTheme.typography.labelMedium,
                    color = moneyColorOf(debt.amountMinor),
                )
                if (isClickable) {
                    Text(
                        text = stringResource(R.string.group_i_paid),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

// Why: 15.5, "Paid R$ 98,00 to Carla?" with ✕ and ✓, the only question the watch asks.
@Composable
private fun ConfirmPaymentDialog(
    state: WatchGroupUiState,
    money: MoneyFormatter,
    onEvent: (WatchGroupUiEvent) -> Unit,
) {
    val debt = state.confirming
    AlertDialog(
        visible = debt != null,
        onDismissRequest = { onEvent(WatchGroupUiEvent.OnConfirmDismissed) },
        icon = {
            Icon(
                Icons.Outlined.Payments,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        },
        title = {
            if (debt != null) {
                Text(
                    stringResource(
                        R.string.group_confirm_title,
                        money.toText(abs(debt.amountMinor)),
                        debt.name ?: stringResource(R.string.group_former_member),
                    ),
                )
            }
        },
        text = { Text(stringResource(R.string.group_confirm_body, state.groupName.orEmpty())) },
        confirmButton = {
            // Why: the ViewModel ignores a second tap while the payment saves.
            AlertDialogDefaults.ConfirmButton(onClick = { onEvent(WatchGroupUiEvent.OnPaymentConfirmed) })
        },
        dismissButton = {
            AlertDialogDefaults.DismissButton(onClick = { onEvent(WatchGroupUiEvent.OnConfirmDismissed) })
        },
    )
}

@WearPreviewDevices
@Composable
private fun GroupPreview() {
    WearTheme {
        WatchGroupContent(
            state = WatchGroupUiState(
                isLoaded = true,
                groupName = "Apê 302",
                currency = GroupCurrency.BRL,
                netMinor = -9_800,
                debts = listOf(WatchDebt("carla", "Carla", "C", AvatarColor.LILAC, null, -9_800)),
                confirming = null,
                isRecording = false,
                isQueued = false,
            ),
            onEvent = {},
        )
    }
}
