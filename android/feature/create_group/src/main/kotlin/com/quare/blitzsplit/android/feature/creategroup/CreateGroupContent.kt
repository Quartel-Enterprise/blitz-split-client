package com.quare.blitzsplit.android.feature.creategroup

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupMember
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupUiEvent
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupUiState
import java.util.Currency
import java.util.Locale

private val singleColumnMaxWidth = 592.dp
private val dialogWidth = 560.dp
internal val listEndRadius = 20.dp
internal val listInnerRadius = 6.dp

@Composable
fun CreateGroupContent(
    state: CreateGroupUiState,
    // Why: a tablet draws the new group as a 560dp dialog over the home.
    isDialog: Boolean,
    onEvent: (CreateGroupUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isDialog) {
        Surface(
            modifier = modifier
                .widthIn(max = dialogWidth)
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column {
                CreateGroupTopBar(
                    state = state,
                    onEvent = onEvent,
                    isDialog = true,
                )
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(bottom = 24.dp),
                ) {
                    CreateGroupFormColumn(
                        state = state,
                        onEvent = onEvent,
                    )
                }
            }
        }
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                CreateGroupTopBar(
                    state = state,
                    onEvent = onEvent,
                    isDialog = false,
                )
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = singleColumnMaxWidth)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp),
                ) {
                    CreateGroupFormColumn(
                        state = state,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }
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
            IconButton(onClick = { onEvent(CreateGroupUiEvent.OnCloseClicked) }) {
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
private fun ColumnScope.CreateGroupFormColumn(
    state: CreateGroupUiState,
    onEvent: (CreateGroupUiEvent) -> Unit,
) {
    IdentitySection(
        state = state,
        onEvent = onEvent,
    )
    TypeChipRow(
        state = state,
        onEvent = onEvent,
    )
    MembersSection(
        state = state,
        onEvent = onEvent,
    )
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
    CurrencyRow(
        state = state,
        onEvent = onEvent,
    )
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

private val GroupType.labelRes: Int
    @StringRes get() = when (this) {
        GroupType.TRIP -> R.string.create_group_type_trip
        GroupType.HOME -> R.string.create_group_type_home
        GroupType.EVENT -> R.string.create_group_type_event
        GroupType.OTHER -> R.string.create_group_type_other
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
                    text = currencyLabel(state.currency, locale),
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
                    text = { Text(currencyLabel(currency, locale)) },
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

// Why: "Real brasileiro (R$)", named by the platform in the device's language.
@Composable
private fun currencyLabel(
    currency: GroupCurrency,
    locale: Locale,
): String {
    val label = remember(currency, locale) {
        val platform = Currency.getInstance(currency.code)
        platform.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) } to platform.getSymbol(locale)
    }
    return stringResource(R.string.create_group_currency_value, label.first, label.second)
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
)
