package com.quare.blitzsplit.android.feature.billdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.DialogCard
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiState

private val singleColumnMaxWidth = 592.dp
private val sectionSpacing = 16.dp

/*
 * Why: the canvas opens Conta full screen on a phone, in two columns lying down; on a tablet and the upright
 * foldable it is a dialog over the group, two columns wide on a tablet lying down, like Rachar conta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailContent(
    state: BillDetailUiState,
    isDialog: Boolean,
    snackbarHostState: SnackbarHostState,
    onEvent: (BillDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layout = rememberDeviceLayout()
    val usesTwoColumns = if (isDialog) layout.isTablet && layout.isLandscape else layout.isLandscape
    if (isDialog) {
        DialogCard(
            isWide = usesTwoColumns,
            spacing = sectionSpacing,
            snackbarHostState = snackbarHostState,
            modifier = modifier,
        ) {
            DialogHeader(
                state = state,
                onEvent = onEvent,
            )
            BillColumnsLayout(
                state = state,
                usesTwoColumns = usesTwoColumns,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.weight(
                    weight = 1f,
                    fill = false,
                ),
            )
        }
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = state.groupName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { onEvent(BillDetailUiEvent.OnBackClicked) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.bill_detail_back),
                            )
                        }
                    },
                    actions = {
                        BillTopBarAction(
                            state = state,
                            onEvent = onEvent,
                        )
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter,
            ) {
                BillColumnsLayout(
                    state = state,
                    usesTwoColumns = usesTwoColumns,
                    contentPadding = if (usesTwoColumns) {
                        PaddingValues(
                            start = 56.dp,
                            end = 24.dp,
                            top = 4.dp,
                            bottom = 16.dp,
                        )
                    } else {
                        PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 24.dp,
                        )
                    },
                    modifier = if (usesTwoColumns) Modifier else Modifier.widthIn(max = singleColumnMaxWidth),
                )
            }
        }
    }
}

@Composable
private fun BillTopBarAction(
    state: BillDetailUiState,
    onEvent: (BillDetailUiEvent) -> Unit,
) {
    IconButton(
        onClick = { onEvent(BillDetailUiEvent.OnEditClicked) },
        enabled = state.areActionsEnabled,
    ) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = stringResource(R.string.bill_detail_edit),
        )
    }
    IconButton(
        onClick = { onEvent(BillDetailUiEvent.OnDeleteClicked) },
        enabled = state.areActionsEnabled,
    ) {
        Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = stringResource(R.string.bill_detail_delete),
        )
    }
}

@Composable
private fun DialogHeader(
    state: BillDetailUiState,
    onEvent: (BillDetailUiEvent) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = state.groupName,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BillTopBarAction(
            state = state,
            onEvent = onEvent,
        )
        IconButton(onClick = { onEvent(BillDetailUiEvent.OnBackClicked) }) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.bill_detail_back),
            )
        }
    }
}

// Why: the bill and your part first; who is in the split next to them when lying down, under them otherwise.
@Composable
private fun BillColumnsLayout(
    state: BillDetailUiState,
    usesTwoColumns: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    if (!state.isLoaded) return
    val money = rememberMoneyFormatter(state.currency)
    val dates = rememberDateTimeTextFormatter()
    val first: @Composable ColumnScope.() -> Unit = {
        BillSummaryColumn(
            state = state,
            money = money,
            dates = dates,
        )
        if (state.showsMyPart) {
            MyPartCard(
                state = state,
                money = money,
            )
        }
    }
    val second: @Composable ColumnScope.() -> Unit = {
        BillSplitSection(
            state = state,
            money = money,
        )
    }
    if (usesTwoColumns) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                content = first,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                content = second,
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing),
        ) {
            first()
            second()
        }
    }
}

@DevicePreviews
@Composable
private fun BillDetailContentPreview() {
    PreviewBillDetailContent(createPreviewBillDetailState())
}

@DevicePreviews
@Composable
private fun BillDetailContentPayerPreview() {
    PreviewBillDetailContent(createPreviewBillDetailState(isMePayer = true))
}

@DevicePreviews
@Composable
private fun BillDetailContentPendingPreview() {
    PreviewBillDetailContent(createPreviewBillDetailState(isPending = true))
}

@Composable
private fun PreviewBillDetailContent(state: BillDetailUiState) {
    BlitzSplitTheme {
        BillDetailContent(
            state = state,
            isDialog = false,
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}
