package com.quare.blitzsplit.android.feature.categorymap

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.quare.blitzsplit.android.designsystem.component.CategoryDropdownField
import com.quare.blitzsplit.android.designsystem.component.CategoryFormActions
import com.quare.blitzsplit.android.designsystem.component.CategoryFormContent
import com.quare.blitzsplit.android.designsystem.component.DeleteCategoryDialog
import com.quare.blitzsplit.android.designsystem.layout.FoldDialog
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiEvent
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiState

/*
 * Why: "Nova categoria", "Nova subcategoria" and "Editar categoria" from the map (9.4), the form a bill's Categoria
 * opens: full screen on a phone, in two columns lying down, and a dialog anywhere wider (a book's right half), with
 * "Grupo" above the name while a new one may go in more than one group. "Apagar “X”?" opens over it.
 */
@Composable
internal fun CategoryFormDialog(
    state: CategoryMapUiState,
    // Why: the map's, so "Não deu para salvar a categoria" shows over the form that stays open.
    snackbarHostState: SnackbarHostState,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    val editor = state.categoryEditor ?: return
    val formFactor = rememberFoldLayout().formFactor
    val isDialog = formFactor != FormFactor.Phone && formFactor != FormFactor.PhoneLandscape
    val actions = remember(onEvent) {
        CategoryFormActions(
            onNameChanged = { onEvent(CategoryMapUiEvent.OnCategoryNameChanged(it)) },
            onParentPicked = { onEvent(CategoryMapUiEvent.OnCategoryParentPicked(it)) },
            onIconPicked = { onEvent(CategoryMapUiEvent.OnCategoryIconPicked(it)) },
            onSave = { onEvent(CategoryMapUiEvent.OnCategoryFormSaveClicked) },
            onDismiss = { onEvent(CategoryMapUiEvent.OnCategoryFormDismissed) },
            onDelete = { onEvent(CategoryMapUiEvent.OnCategoryDeleteClicked) },
        )
    }
    val groupField: (@Composable () -> Unit)? = if (state.canPickCategoryGroup) {
        {
            CategoryDropdownField(
                label = stringResource(R.string.category_map_category_group),
                value = editor.groupName,
                help = null,
                isEnabled = !editor.form.isSaving,
                options = state.groups.map { it.id to it.name },
                onPicked = { id -> id?.let { onEvent(CategoryMapUiEvent.OnCategoryGroupPicked(it)) } },
            )
        }
    } else {
        null
    }
    val content: @Composable () -> Unit = {
        CategoryFormContent(
            editor = editor,
            isDialog = isDialog,
            snackbarHostState = snackbarHostState,
            actions = actions,
            groupField = groupField,
        )
    }
    if (isDialog) {
        // Why: a tap beside the dialog never throws away what was typed, as Rachar conta's half pages.
        FoldDialog(
            onDismissRequest = actions.onDismiss,
            properties = DialogProperties(
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
        ) { content() }
    } else {
        Dialog(
            onDismissRequest = actions.onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
            ) { content() }
        }
    }
    editor.deletion?.let { deletion ->
        DeleteCategoryDialog(
            deletion = deletion,
            groupName = editor.groupName,
            onConfirm = { onEvent(CategoryMapUiEvent.OnCategoryDeleteConfirmed) },
            onDismiss = { onEvent(CategoryMapUiEvent.OnCategoryDeleteDismissed) },
        )
    }
}
