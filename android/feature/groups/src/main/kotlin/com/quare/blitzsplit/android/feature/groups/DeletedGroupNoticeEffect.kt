package com.quare.blitzsplit.android.feature.groups

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.feature.groups.presentation.model.DeletedGroupNotice

/*
 * Why: "Carla apagou o grupo X." on Grupos and Início. State, not an action, so it waits until the screen is drawn;
 * the next deleted group follows once this one goes.
 */
@Composable
internal fun DeletedGroupNoticeEffect(
    notice: DeletedGroupNotice?,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit,
) {
    val message = notice?.let {
        it.deletedByName?.let { name -> stringResource(R.string.groups_deleted_by, name, it.groupName) }
            ?: stringResource(R.string.groups_deleted, it.groupName)
    }
    LaunchedEffect(notice) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onShown()
        }
    }
}
