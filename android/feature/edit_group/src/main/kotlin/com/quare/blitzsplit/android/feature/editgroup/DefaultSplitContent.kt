package com.quare.blitzsplit.android.feature.editgroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.labelRes
import com.quare.blitzsplit.android.designsystem.format.nameListOf
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.ColumnsFormLayout
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.FormChrome
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitFigures
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitPerson
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitUiState

private val outer = 20.dp
private val inner = 6.dp

/*
 * Why: "Divisão padrão" (6.7), over Editar grupo: the ways to split on one side, the people with the example bill on
 * the other when the window lies down; a dialog where Editar grupo is one.
 */
@Composable
fun DefaultSplitContent(
    state: DefaultSplitUiState,
    isDialog: Boolean,
    onEvent: (DefaultSplitUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val money = rememberMoneyFormatter(state.currency)
    ColumnsFormLayout(
        chrome = FormChrome(
            title = stringResource(R.string.default_split_title),
            subtitle = state.groupName
                .takeIf { it.isNotEmpty() }
                ?.let { stringResource(R.string.default_split_subtitle, it) },
            navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
            navigationLabel = stringResource(R.string.default_split_back),
            onNavigate = { onEvent(DefaultSplitUiEvent.OnBackClicked) },
            confirmLabel = stringResource(R.string.edit_group_save),
            isConfirmEnabled = state.canSave,
            onConfirm = { onEvent(DefaultSplitUiEvent.OnSaveClicked) },
            cancelLabel = stringResource(R.string.default_split_cancel),
            isNavigationAtEndInDialog = false,
            iconAction = null,
        ),
        isDialog = isDialog,
        snackbarHostState = remember { SnackbarHostState() },
        modifier = modifier,
        first = {
            if (state.isLoaded) {
                NoticesSection(state)
                ModesSection(
                    state = state,
                    onEvent = onEvent,
                )
            }
        },
        second = {
            if (state.isLoaded) {
                PeopleSection(
                    state = state,
                    money = money,
                    onEvent = onEvent,
                )
                ExampleCard(
                    state = state,
                    money = money,
                )
                Text(
                    text = stringResource(findNoteRes(state.showsOfflineNotice)),
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun NoticesSection(state: DefaultSplitUiState) {
    if (state.showsSaveError) {
        Text(
            text = stringResource(R.string.default_split_save_error),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
    val joinedPeople = state.joinedPeople
    if (joinedPeople.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = newcomerTextOf(state.mode, joinedPeople.map { it.name }),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// Why: "Carla entrou no grupo. Ela começa com 1 parte; ajuste se precisar.", for one or more people.
@Composable
private fun newcomerTextOf(
    mode: DefaultSplitMode,
    names: List<String>,
): String {
    val joined = pluralStringResource(R.plurals.default_split_joined, names.size, nameListOf(names))
    val start = when (mode) {
        DefaultSplitMode.EQUAL -> pluralStringResource(R.plurals.default_split_joined_equal, names.size)

        DefaultSplitMode.SHARES -> pluralStringResource(R.plurals.default_split_joined_shares, names.size)

        DefaultSplitMode.PERCENT, DefaultSplitMode.INCOME ->
            pluralStringResource(R.plurals.default_split_joined_percent, names.size)
    }
    return stringResource(R.string.default_split_joined_sentence, joined, start)
}

@Composable
private fun ModesSection(
    state: DefaultSplitUiState,
    onEvent: (DefaultSplitUiEvent) -> Unit,
) {
    SectionLabel(stringResource(R.string.default_split_modes))
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        state.modes.forEachIndexed { index, mode ->
            val isSelected = mode == state.mode
            Surface(
                shape = createGroupedShape(index, state.modes.size),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLowest
                },
                modifier = Modifier.selectable(
                    selected = isSelected,
                    role = Role.RadioButton,
                    onClick = { onEvent(DefaultSplitUiEvent.OnModeSelected(mode)) },
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .padding(
                            horizontal = 16.dp,
                            vertical = 10.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(mode.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = stringResource(mode.hintRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

internal fun createGroupedShape(
    index: Int,
    count: Int,
): Shape = RoundedCornerShape(
    topStart = if (index == 0) outer else inner,
    topEnd = if (index == 0) outer else inner,
    bottomStart = if (index == count - 1) outer else inner,
    bottomEnd = if (index == count - 1) outer else inner,
)

private val DefaultSplitMode.hintRes: Int
    get() = when (this) {
        DefaultSplitMode.EQUAL -> R.string.default_split_hint_equal
        DefaultSplitMode.SHARES -> R.string.default_split_hint_shares
        DefaultSplitMode.PERCENT -> R.string.default_split_hint_percent
        DefaultSplitMode.INCOME -> R.string.default_split_hint_income
    }

@DevicePreviews
@Composable
private fun DefaultSplitSharesPreview() {
    BlitzSplitTheme {
        DefaultSplitContent(
            state = createPreviewState(
                mode = DefaultSplitMode.SHARES,
                people = listOf(
                    createPreviewPerson(
                        "1",
                        "Pierre",
                        isMe = true,
                        parts = 3,
                    ),
                    createPreviewPerson("2", "Ana", parts = 1),
                ),
            ),
            isDialog = false,
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun DefaultSplitPercentErrorPreview() {
    BlitzSplitTheme {
        DefaultSplitContent(
            state = createPreviewState(
                mode = DefaultSplitMode.PERCENT,
                people = listOf(
                    createPreviewPerson(
                        "1",
                        "Pierre",
                        isMe = true,
                        percent = "75",
                    ),
                    createPreviewPerson("2", "Ana", percent = "15"),
                ),
            ),
            isDialog = false,
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun DefaultSplitNewMemberPreview() {
    BlitzSplitTheme {
        DefaultSplitContent(
            state = createPreviewState(
                mode = DefaultSplitMode.SHARES,
                people = listOf(
                    createPreviewPerson(
                        "1",
                        "Pierre",
                        isMe = true,
                        parts = 3,
                    ),
                    createPreviewPerson("2", "Ana", parts = 1),
                    createPreviewPerson("3", "Carla", parts = 1).copy(isNew = true),
                ),
            ),
            isDialog = false,
            onEvent = {},
        )
    }
}

private fun createPreviewState(
    mode: DefaultSplitMode,
    people: List<DefaultSplitPerson>,
): DefaultSplitUiState = DefaultSplitUiState(
    groupName = "Apê 302",
    currency = GroupCurrency.BRL,
    mode = mode,
    people = people,
    incomeFallback = emptyMap(),
    isOnline = true,
    isLoaded = true,
    isSaving = false,
    showsSaveError = false,
    hasChanges = true,
    figures = DefaultSplitFigures.of(
        mode = mode,
        people = people,
        incomeFallback = emptyMap(),
        currency = GroupCurrency.BRL,
    ),
)

private fun createPreviewPerson(
    id: String,
    name: String,
    isMe: Boolean = false,
    parts: Long = 1L,
    percent: String = "",
): DefaultSplitPerson = DefaultSplitPerson(
    id = id,
    name = name,
    initials = name.take(2).uppercase(),
    avatarColor = if (isMe) AvatarColor.BLUE else AvatarColor.GREEN,
    isMe = isMe,
    isNew = false,
    isIncluded = true,
    parts = parts,
    percentText = percent,
    incomeText = "",
)

private fun findNoteRes(isOffline: Boolean): Int =
    if (isOffline) R.string.default_split_offline_note else R.string.default_split_note
