package com.quare.blitzsplit.android.feature.editgroup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.CurrencyMenuContent
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.component.ShimmerAvatarSkeleton
import com.quare.blitzsplit.android.designsystem.component.ShimmerBox
import com.quare.blitzsplit.android.designsystem.component.ShimmerFormSkeleton
import com.quare.blitzsplit.android.designsystem.component.freeTextKeyboardOptions
import com.quare.blitzsplit.android.designsystem.format.groupCurrencyLabel
import com.quare.blitzsplit.android.designsystem.format.labelRes
import com.quare.blitzsplit.android.designsystem.layout.FormLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiState

private val listEndRadius = 20.dp
private val listInnerRadius = 6.dp
private val phoneAvatarSize = 112.dp
private val wideAvatarSize = 96.dp
private val iconColumnWidth = 240.dp

/*
 * Why: full screen on a phone, in two columns lying down (the icon on the left, the fields on the right); anything wider
 * draws it as a dialog over the group, with the smaller icon the canvas gives it there.
 */
@Composable
fun EditGroupContent(
    state: EditGroupUiState,
    // Why: anything wider than a phone draws the form as a dialog over the group, in a book's half on a foldable.
    isDialog: Boolean,
    onEvent: (EditGroupUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    // Why: "Ícone do grupo"'s "Tirar foto", null where there is no camera, and "Escolher foto".
    onTakePhotoClick: (() -> Unit)? = null,
    onChoosePhotoClick: () -> Unit = {},
) {
    state.iconEditor?.let { editor ->
        GroupIconSheet(
            editor = editor,
            initials = state.initials,
            isDialog = isDialog,
            onEvent = onEvent,
            onTakePhotoClick = onTakePhotoClick,
            onChoosePhotoClick = onChoosePhotoClick,
        )
    }
    val usesTwoColumns = !isDialog && rememberDeviceLayout().usesTwoColumns
    FormLayout(
        isDialog = isDialog,
        topBar = { isInDialog ->
            EditGroupTopBar(
                state = state,
                onEvent = onEvent,
                isDialog = isInDialog,
            )
        },
        modifier = modifier,
        secondColumn = if (usesTwoColumns) {
            {
                if (state.isLoaded) {
                    EditGroupFieldsColumn(
                        state = state,
                        onEvent = onEvent,
                    )
                } else {
                    ShimmerFormSkeleton()
                }
            }
        } else {
            null
        },
        firstColumnWidth = iconColumnWidth.takeIf { usesTwoColumns },
    ) {
        if (state.isLoaded) {
            if (usesTwoColumns) {
                IconSection(
                    state = state,
                    onEvent = onEvent,
                    avatarSize = wideAvatarSize,
                )
            } else {
                EditGroupFormColumn(
                    state = state,
                    onEvent = onEvent,
                    avatarSize = if (isDialog) wideAvatarSize else phoneAvatarSize,
                )
            }
        } else {
            // Why: the group's icon and fields shimmer until Room is read, never an empty name or a default color.
            ShimmerBox(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                ShimmerAvatarSkeleton(
                    size = if (usesTwoColumns || isDialog) wideAvatarSize else phoneAvatarSize,
                    shape = RoundedCornerShape(28.dp),
                )
            }
            if (!usesTwoColumns) ShimmerFormSkeleton()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditGroupTopBar(
    state: EditGroupUiState,
    onEvent: (EditGroupUiEvent) -> Unit,
    isDialog: Boolean,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.edit_group_title)) },
        navigationIcon = {
            IconButton(
                onClick = { onEvent(EditGroupUiEvent.OnCloseClicked) },
                enabled = !state.isSaving,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.edit_group_close),
                )
            }
        },
        actions = {
            Button(
                onClick = { onEvent(EditGroupUiEvent.OnSaveClicked) },
                enabled = state.canSave,
                modifier = Modifier.padding(end = 12.dp),
            ) { Text(stringResource(R.string.edit_group_save)) }
        },
        windowInsets = if (isDialog) WindowInsets(0) else TopAppBarDefaults.windowInsets,
        colors = if (isDialog) {
            TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        } else {
            TopAppBarDefaults.topAppBarColors()
        },
    )
}

@Composable
private fun ColumnScope.EditGroupFormColumn(
    state: EditGroupUiState,
    onEvent: (EditGroupUiEvent) -> Unit,
    avatarSize: Dp,
) {
    SaveErrorText(state)
    IconSection(
        state = state,
        onEvent = onEvent,
        avatarSize = avatarSize,
    )
    EditGroupFieldsSection(
        state = state,
        onEvent = onEvent,
    )
}

// Why: lying down, the fields on the right, the error over them.
@Composable
private fun EditGroupFieldsColumn(
    state: EditGroupUiState,
    onEvent: (EditGroupUiEvent) -> Unit,
) {
    SaveErrorText(state)
    EditGroupFieldsSection(
        state = state,
        onEvent = onEvent,
    )
}

@Composable
private fun SaveErrorText(state: EditGroupUiState) {
    if (!state.showsSaveError) return
    Text(
        text = stringResource(R.string.edit_group_save_error),
        modifier = Modifier
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
            ).semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun EditGroupFieldsSection(
    state: EditGroupUiState,
    onEvent: (EditGroupUiEvent) -> Unit,
) {
    NameField(
        state = state,
        onEvent = onEvent,
    )
    TypeChipRow(
        state = state,
        onEvent = onEvent,
    )
    CurrencyRow(
        state = state,
        onEvent = onEvent,
    )
    DefaultSplitRow(
        state = state,
        onEvent = onEvent,
    )
    SimplifyDebtsSection(
        state = state,
        onEvent = onEvent,
    )
    Text(
        text = stringResource(
            when {
                state.showsSimplifyOfflineNotice -> R.string.simplify_offline_note
                state.showsOfflineNotice -> R.string.edit_group_offline_note
                else -> R.string.edit_group_note
            },
        ),
        modifier = Modifier.padding(
            start = 20.dp,
            end = 20.dp,
            top = 16.dp,
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// Why: tapping the avatar, or the button under it, opens "Ícone do grupo".
@Composable
private fun ColumnScope.IconSection(
    state: EditGroupUiState,
    onEvent: (EditGroupUiEvent) -> Unit,
    avatarSize: Dp,
) {
    val changeIconLabel = stringResource(R.string.edit_group_change_icon)
    Column(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            GroupBadge(
                initials = state.initials,
                color = state.color,
                size = avatarSize,
                photoUrl = state.photoUrl,
                modifier = Modifier
                    .clickable(
                        role = Role.Button,
                        onClick = { onEvent(EditGroupUiEvent.OnIconClicked) },
                    ).semantics { contentDescription = changeIconLabel },
            )
            if (state.isPreparingPhoto) CircularProgressIndicator()
        }
        FilledTonalButton(onClick = { onEvent(EditGroupUiEvent.OnIconClicked) }) {
            Icon(
                imageVector = Icons.Outlined.Palette,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = changeIconLabel,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (state.showsPhotoError) {
            Text(
                text = stringResource(R.string.edit_group_photo_error),
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun NameField(
    state: EditGroupUiState,
    onEvent: (EditGroupUiEvent) -> Unit,
) {
    OutlinedTextField(
        value = state.name,
        onValueChange = { onEvent(EditGroupUiEvent.OnNameChanged(it)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 24.dp,
            ),
        label = { Text(stringResource(R.string.edit_group_name_label)) },
        supportingText = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(
                        if (state.showsNameError) R.string.edit_group_name_error else R.string.edit_group_name_help,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Text(stringResource(R.string.edit_group_name_count, state.nameLength, state.maxNameLength))
            }
        },
        isError = state.showsNameError,
        singleLine = true,
        keyboardOptions = freeTextKeyboardOptions.copy(imeAction = ImeAction.Done),
    )
}

@Composable
private fun TypeChipRow(
    state: EditGroupUiState,
    onEvent: (EditGroupUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier.padding(
            start = 20.dp,
            end = 20.dp,
            top = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.edit_group_type_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.types.forEach { type ->
                val isSelected = type == state.type
                FilterChip(
                    selected = isSelected,
                    onClick = { onEvent(EditGroupUiEvent.OnTypeSelected(type)) },
                    label = { Text(stringResource(type.labelRes)) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

// Why: the currency is the default of new bills, always editable; each bill keeps its own.
@Composable
private fun CurrencyRow(
    state: EditGroupUiState,
    onEvent: (EditGroupUiEvent) -> Unit,
) {
    var isMenuOpen by rememberSaveable { mutableStateOf(false) }
    val locale = LocalLocale.current.platformLocale
    Box(
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 24.dp,
        ),
    ) {
        SettingRow(
            title = stringResource(R.string.edit_group_currency),
            subtitle = stringResource(R.string.edit_group_currency_caption),
            value = groupCurrencyLabel(state.currency, locale),
            icon = Icons.Outlined.KeyboardArrowDown,
            isTop = true,
            isEnabled = true,
            onClick = { isMenuOpen = true },
        )
        DropdownMenu(
            expanded = isMenuOpen,
            onDismissRequest = { isMenuOpen = false },
        ) {
            CurrencyMenuContent(
                currencies = state.currencies,
                selected = state.currency,
                onSelected = { currency ->
                    isMenuOpen = false
                    onEvent(EditGroupUiEvent.OnCurrencySelected(currency))
                },
            )
        }
    }
}

// Why: "Divisão padrão" under Moeda, with the way the group splits now; it opens over the form, which keeps what is typed.
@Composable
private fun DefaultSplitRow(
    state: EditGroupUiState,
    onEvent: (EditGroupUiEvent) -> Unit,
) {
    SettingRow(
        title = stringResource(R.string.edit_group_default_split),
        subtitle = stringResource(R.string.edit_group_default_split_help),
        value = stringResource(state.defaultSplitMode.labelRes),
        icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        isTop = false,
        isEnabled = !state.isSaving,
        onClick = { onEvent(EditGroupUiEvent.OnDefaultSplitClicked) },
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 2.dp,
        ),
    )
}

// Why: Moeda and Divisão padrão, one grouped list: the first with the top corners round, the last the bottom ones.
@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    value: String,
    icon: ImageVector,
    isTop: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val top = if (isTop) listEndRadius else listInnerRadius
    val bottom = if (isTop) listInnerRadius else listEndRadius
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = isEnabled,
        shape = RoundedCornerShape(
            topStart = top,
            topEnd = top,
            bottomStart = bottom,
            bottomEnd = bottom,
        ),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
