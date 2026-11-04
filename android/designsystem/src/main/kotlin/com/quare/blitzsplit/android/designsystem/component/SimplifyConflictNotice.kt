package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.core.expenses.domain.model.SimplifyDebtsConflictNotice

/*
 * Why: "Ana desligou depois de você. Vale a última mudança: Simplificar dívidas está desligado." (13.1e), with "Ver na
 * Atividade", on Editar grupo or the group's screen, whichever is on top as this device's change loses. The notice
 * stays in the state while the snackbar shows; it goes once the snackbar does.
 */
@Composable
fun SimplifyConflictNoticeEffect(
    notice: SimplifyDebtsConflictNotice?,
    snackbarHostState: SnackbarHostState,
    onActivityClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val message = notice?.let {
        val name = it.changedByName ?: stringResource(R.string.designsystem_simplify_someone)
        stringResource(
            if (it.simplifiesDebts) {
                R.string.designsystem_simplify_conflict_on
            } else {
                R.string.designsystem_simplify_conflict_off
            },
            name,
        )
    }
    val action = stringResource(R.string.designsystem_simplify_see_activity)
    val currentOnActivityClick by rememberUpdatedState(onActivityClick)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(notice) {
        if (message == null) return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = action,
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )
        when (result) {
            SnackbarResult.ActionPerformed -> currentOnActivityClick()
            SnackbarResult.Dismissed -> currentOnDismiss()
        }
    }
}
