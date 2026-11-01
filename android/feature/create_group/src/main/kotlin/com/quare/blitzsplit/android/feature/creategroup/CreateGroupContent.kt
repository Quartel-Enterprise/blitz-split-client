package com.quare.blitzsplit.android.feature.creategroup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.format.groupCurrencyLabel
import com.quare.blitzsplit.android.designsystem.format.labelRes
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.FormLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupMember
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupUiEvent
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupUiState

internal val listEndRadius = 20.dp
internal val listInnerRadius = 6.dp

/*
 * Why: full screen on a phone, in two columns lying down (who the group is on the left, "Quem participa" on the
 * right); anything wider draws it as a dialog over the home.
 */
@Composable
fun CreateGroupContent(
    state: CreateGroupUiState,
    // Why: anything wider than a phone draws the new group as a dialog over the home.
    isDialog: Boolean,
    onEvent: (CreateGroupUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val usesTwoColumns = !isDialog && rememberDeviceLayout().usesTwoColumns
    FormLayout(
        isDialog = isDialog,
        topBar = { isInDialog ->
            CreateGroupTopBar(
                state = state,
                onEvent = onEvent,
                isDialog = isInDialog,
            )
        },
        modifier = modifier,
        secondColumn = if (usesTwoColumns) {
            {
                MembersColumn(
                    state = state,
                    onEvent = onEvent,
                )
            }
        } else {
            null
        },
    ) {
        if (usesTwoColumns) {
            IdentityColumn(
                state = state,
                onEvent = onEvent,
            )
        } else {
            CreateGroupFormColumn(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}

// Why: lying down, the group itself (name, type, currency) on the left.
@Composable
private fun IdentityColumn(
    state: CreateGroupUiState,
    onEvent: (CreateGroupUiEvent) -> Unit,
) {
    CreateErrorText(state)
    IdentitySection(
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
}

// Why: lying down, who takes part on the right, with what the names say under them.
@Composable
private fun MembersColumn(
    state: CreateGroupUiState,
    onEvent: (CreateGroupUiEvent) -> Unit,
) {
    MembersSection(
        state = state,
        onEvent = onEvent,
    )
    MembersNoticesSection(
        state = state,
        onEvent = onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGroupTopBar(
    state: CreateGroupUiState,
    onEvent: (CreateGroupUiEvent) -> Unit,
    isDialog: Boolean,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.create_group_title)) },
        navigationIcon = {
            IconButton(
                onClick = { onEvent(CreateGroupUiEvent.OnCloseClicked) },
                enabled = !state.isCreating,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.create_group_close),
                )
            }
        },
        actions = {
            Button(
                onClick = { onEvent(CreateGroupUiEvent.OnCreateClicked) },
                enabled = state.canCreate,
                modifier = Modifier.padding(end = 12.dp),
            ) { Text(stringResource(R.string.create_group_create)) }
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
private fun CreateGroupFormColumn(
    state: CreateGroupUiState,
    onEvent: (CreateGroupUiEvent) -> Unit,
) {
    CreateErrorText(state)
    IdentitySection(
        state = state,
        onEvent = onEvent,
    )
    TypeChipRow(
        state = state,
        onEvent = onEvent,
    )
    MembersColumn(
        state = state,
        onEvent = onEvent,
    )
    CurrencyRow(
        state = state,
        onEvent = onEvent,
    )
}

@Composable
private fun CreateErrorText(state: CreateGroupUiState) {
    if (state.showsCreateError) {
        Text(
            text = stringResource(R.string.create_group_save_error),
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
}

@Composable
private fun MembersNoticesSection(
    state: CreateGroupUiState,
    onEvent: (CreateGroupUiEvent) -> Unit,
) {
    if (state.takenName != null) {
        TakenNameMessage(
            takenName = state.takenName.orEmpty(),
            suggestions = state.nicknameSuggestions,
            onEvent = onEvent,
        )
    }
    if (state.showsNoAppNotice) {
        Text(
            text = stringResource(R.string.create_group_no_app_notice),
            modifier = Modifier.padding(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun IdentitySection(
    state: CreateGroupUiState,
    onEvent: (CreateGroupUiEvent) -> Unit,
) {
    Row(
        modifier = Modifier.padding(
            start = 20.dp,
            end = 20.dp,
            top = 16.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val changeColorLabel = stringResource(R.string.create_group_change_color)
        // Why: tapping the avatar cycles its color through the palette.
        GroupBadge(
            initials = state.initials,
            color = state.color,
            size = 72.dp,
            modifier = Modifier
                .clickable(
                    role = Role.Button,
                    onClick = { onEvent(CreateGroupUiEvent.OnColorClicked) },
                ).semantics { contentDescription = changeColorLabel },
        )
        OutlinedTextField(
            value = state.name,
            onValueChange = { onEvent(CreateGroupUiEvent.OnNameChanged(it)) },
            modifier = Modifier.weight(1f),
            label = { Text(stringResource(R.string.create_group_name_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
        )
    }
}

@Composable
private fun TypeChipRow(
    state: CreateGroupUiState,
    onEvent: (CreateGroupUiEvent) -> Unit,
) {
    FlowRow(
        modifier = Modifier.padding(
            start = 20.dp,
            end = 20.dp,
            top = 18.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.types.forEach { type ->
            val isSelected = type == state.type
            FilterChip(
                selected = isSelected,
                onClick = { onEvent(CreateGroupUiEvent.OnTypeSelected(type)) },
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

@Composable
private fun CurrencyRow(
    state: CreateGroupUiState,
    onEvent: (CreateGroupUiEvent) -> Unit,
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
        Surface(
            onClick = { isMenuOpen = true },
            shape = RoundedCornerShape(listEndRadius),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.create_group_currency),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = groupCurrencyLabel(state.currency, locale),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(
            expanded = isMenuOpen,
            onDismissRequest = { isMenuOpen = false },
        ) {
            state.currencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(groupCurrencyLabel(currency, locale)) },
                    onClick = {
                        isMenuOpen = false
                        onEvent(CreateGroupUiEvent.OnCurrencySelected(currency))
                    },
                    trailingIcon = if (currency == state.currency) {
                        {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
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

@DevicePreviews
@Composable
private fun CreateGroupPreview() {
    BlitzSplitTheme {
        CreateGroupContent(
            state = buildPreviewState(
                draft = "",
                takenName = null,
            ),
            isDialog = false,
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun CreateGroupTakenNamePreview() {
    BlitzSplitTheme {
        CreateGroupContent(
            state = buildPreviewState(
                draft = "Ana",
                takenName = "Ana",
            ),
            isDialog = false,
            onEvent = {},
        )
    }
}

private fun buildPreviewState(
    draft: String,
    takenName: String?,
): CreateGroupUiState = CreateGroupUiState(
    name = "Viagem Floripa",
    initials = "VF",
    color = AvatarColor.PEACH,
    type = GroupType.TRIP,
    currency = GroupCurrency.BRL,
    members = listOf(
        CreateGroupMember("1", "Pierre", "P", AvatarColor.BLUE, isMe = true),
        CreateGroupMember("2", "Ana", "AN", AvatarColor.GREEN, isMe = false),
        CreateGroupMember("3", "Bruno", "BR", AvatarColor.PEACH, isMe = false),
        CreateGroupMember("4", "Carla", "CA", AvatarColor.LILAC, isMe = false),
    ),
    draft = draft,
    takenName = takenName,
    nicknameSuggestions = if (takenName == null) emptyList() else listOf("Ana P.", "Ana 2"),
    isCreating = false,
    showsCreateError = false,
)
