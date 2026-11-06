package com.quare.blitzsplit.android.feature.groups

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.core.model.GroupDeleteFailure
import com.quare.blitzsplit.feature.groups.presentation.model.DeletedGroupNotice

/*
 * Why: "Carla apagou o grupo X." on Grupos and Início, or why this device's own deletion didn't go through and the group
 * is back. State, not an action, so it waits until the screen is drawn; the next deleted group follows once this one
 * goes.
 */
@Composable
internal fun DeletedGroupNoticeEffect(
    notice: DeletedGroupNotice?,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit,
) {
    val message = notice?.let {
        val deletedByName = it.deletedByName
        when {
            it.failure == GroupDeleteFailure.HAS_BALANCE -> stringResource(
                R.string.groups_delete_has_balance,
                it.groupName,
            )

            it.failure == GroupDeleteFailure.FAILED -> stringResource(R.string.groups_delete_failed, it.groupName)

            deletedByName != null -> stringResource(R.string.groups_deleted_by, deletedByName, it.groupName)

            else -> stringResource(R.string.groups_deleted, it.groupName)
        }
    }
    LaunchedEffect(notice) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onShown()
        }
    }
}
