package com.quare.blitzsplit.android.feature.editgroup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarColorPicker
import com.quare.blitzsplit.android.designsystem.component.FlatListItem
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveSheet
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.model.GroupIconEditor

private val previewSize = 64.dp
private val iconDialogWidth = 480.dp

/*
 * Why: "Ícone do grupo" (6.4d): the shape and the color picked show in the shape buttons, and only reach the form
 * on "Usar este ícone"; a photo picked or taken goes straight to the form, closing the sheet. A bottom sheet where
 * Editar grupo is full screen; where it is a dialog, a 480dp dialog over it, in a book's right half on a foldable,
 * never over the hinge.
 */
@Composable
internal fun GroupIconSheet(
    editor: GroupIconEditor,
    initials: String,
    isDialog: Boolean,
    onEvent: (EditGroupUiEvent) -> Unit,
    onTakePhotoClick: (() -> Unit)?,
    onChoosePhotoClick: () -> Unit,
) {
    AdaptiveSheet(
        title = stringResource(R.string.edit_group_icon_title),
        onDismissRequest = { onEvent(EditGroupUiEvent.OnIconDismissed) },
        usesDialog = isDialog,
        skipPartiallyExpanded = false,
        dialogMaxWidth = iconDialogWidth,
        dialogShape = MaterialTheme.shapes.extraLarge,
    ) {
        GroupIconColumn(
            editor = editor,
            initials = initials,
            onEvent = onEvent,
            onTakePhotoClick = onTakePhotoClick,
            onChoosePhotoClick = onChoosePhotoClick,
            /*
             * Why: a phone lying down is shorter than the sheet's content, so it scrolls there too. The
             * dialog's window keeps off the navigation bar by itself, so its padding only lifts the sheet.
             */
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = if (isDialog) 24.dp else 16.dp),
        )
    }
}

@Composable
private fun GroupIconColumn(
    editor: GroupIconEditor,
    initials: String,
    onEvent: (EditGroupUiEvent) -> Unit,
    onTakePhotoClick: (() -> Unit)?,
    onChoosePhotoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.edit_group_icon_title),
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        IconPreviewBadge(
            editor = editor,
            initials = initials,
        )
        IconSheetLabel(stringResource(R.string.edit_group_icon_color))
        AvatarColorPicker(
            selected = editor.color,
            colors = editor.colors,
            onSelected = { onEvent(EditGroupUiEvent.OnIconColorPicked(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )
        HorizontalDivider(
            modifier = Modifier.padding(
                top = 20.dp,
                bottom = 8.dp,
            ),
        )
        PhotoItem(Icons.Outlined.PhotoLibrary, stringResource(R.string.edit_group_choose_photo)) {
            onEvent(EditGroupUiEvent.OnIconDismissed)
            onChoosePhotoClick()
        }
        if (onTakePhotoClick != null) {
            PhotoItem(Icons.Outlined.PhotoCamera, stringResource(R.string.edit_group_take_photo)) {
                onEvent(EditGroupUiEvent.OnIconDismissed)
                onTakePhotoClick()
            }
        }
        Button(
            onClick = { onEvent(EditGroupUiEvent.OnIconConfirmed) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 16.dp,
                ),
        ) {
            Text(stringResource(R.string.edit_group_use_icon))
        }
    }
}

@Composable
private fun IconSheetLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(
            start = 24.dp,
            end = 24.dp,
            top = 20.dp,
            bottom = 12.dp,
        ),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// Why: the group's rounded square with the color picked, the only shape a group has.
@Composable
private fun IconPreviewBadge(
    editor: GroupIconEditor,
    initials: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        GroupBadge(
            initials = initials,
            color = editor.color,
            size = previewSize,
        )
    }
}

@Composable
private fun PhotoItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    FlatListItem(
        headlineContent = { Text(label) },
        modifier = Modifier.clickable(
            role = Role.Button,
            onClick = onClick,
        ),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )
}
