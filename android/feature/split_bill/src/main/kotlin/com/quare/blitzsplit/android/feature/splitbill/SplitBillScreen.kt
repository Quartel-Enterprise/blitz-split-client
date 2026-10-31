package com.quare.blitzsplit.android.feature.splitbill

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.DeleteBillDialog
import com.quare.blitzsplit.android.designsystem.component.rememberActionSnackbarHostState
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiAction
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val tabletSmallestWidth = 600.dp
private val formDialogLongestSide = 960.dp

@Composable
fun SplitBillScreen(
    route: Route.SplitBill,
    // Why: a tablet and the upright foldable keep the group behind the bill, as a dialog.
    isDialog: Boolean,
    viewModel: SplitBillViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberActionSnackbarHostState(viewModel.uiAction) { action ->
        when (action) {
            SplitBillUiAction.ShowSaveError -> R.string.split_bill_save_error
            SplitBillUiAction.ShowDeleteError -> R.string.split_bill_delete_error
        }
    }
    var isPickingSpentAt by rememberSaveable { mutableStateOf(false) }
    BackHandler {
        viewModel.onEvent(
            if (state.splitEditor == null) SplitBillUiEvent.OnCloseClicked else SplitBillUiEvent.OnSplitDismissed,
        )
    }
    AnimatedContent(
        targetState = state.splitEditor,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        contentKey = { it != null },
        label = "split-editor",
    ) { editor ->
        if (editor == null) {
            SplitBillContent(
                state = state,
                isDialog = isDialog,
                snackbarHostState = snackbarHostState,
                onWhenClick = {
                    viewModel.onEvent(SplitBillUiEvent.OnSpentAtOpened)
                    isPickingSpentAt = true
                },
                onEvent = viewModel::onEvent,
            )
        } else {
            SplitEditorContent(
                state = state,
                editor = editor,
                isDialog = isDialog,
                snackbarHostState = snackbarHostState,
                onEvent = viewModel::onEvent,
            )
        }
    }
    state.payerSelection?.let { selectedId ->
        PayerSheet(
            state = state,
            selectedId = selectedId,
            money = rememberMoneyFormatter(state.currency),
            onEvent = viewModel::onEvent,
        )
    }
    state.deletion?.let { deletion ->
        DeleteBillDialog(
            description = deletion.description,
            myBalanceMinor = deletion.myBalanceMinor,
            isDeleting = state.isDeleting,
            money = rememberMoneyFormatter(state.currency),
            onConfirm = { viewModel.onEvent(SplitBillUiEvent.OnDeleteConfirmed) },
            onDismiss = { viewModel.onEvent(SplitBillUiEvent.OnDeleteDismissed) },
        )
    }
    if (isPickingSpentAt) {
        SpentAtPickerDialog(
            spentAt = state.spentAt,
            latestSpentAt = state.latestSpentAt,
            onPicked = { millis ->
                isPickingSpentAt = false
                viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(millis))
            },
            onDismiss = { isPickingSpentAt = false },
        )
    }
}

/*
 * Why: the canvas draws Rachar conta as a dialog over the group on a tablet and on the upright foldable, and full
 * screen on a phone, lying down and on the tabletop (a foldable lying down, under 960dp wide), unlike the other
 * forms, which open full screen on any foldable.
 */
@Composable
fun rememberSplitBillUsesDialog(): Boolean {
    val size = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    return remember(size, density) {
        with(density) {
            val width = size.width.toDp()
            val height = size.height.toDp()
            val isTablet = min(width, height) >= tabletSmallestWidth
            isTablet && (height > width || max(width, height) >= formDialogLongestSide)
        }
    }
}
