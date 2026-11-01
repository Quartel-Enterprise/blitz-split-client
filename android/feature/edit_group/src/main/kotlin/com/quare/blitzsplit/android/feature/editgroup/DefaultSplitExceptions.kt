package com.quare.blitzsplit.android.feature.editgroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.CategoryBadge
import com.quare.blitzsplit.android.designsystem.component.ChoiceChipSurface
import com.quare.blitzsplit.android.designsystem.component.ConnectedButtonRow
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.categoryName
import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitUiState
import com.quare.blitzsplit.feature.editgroup.presentation.model.ExceptionEditor
import com.quare.blitzsplit.feature.editgroup.presentation.model.ExceptionRow

/*
 * Why: "Exceções por categoria" (6.7f) under the example: each category that splits its own way, or "Nenhuma", and
 * "Adicionar exceção".
 */
@Composable
internal fun ExceptionsSection(
    state: DefaultSplitUiState,
    onEvent: (DefaultSplitUiEvent) -> Unit,
) {
    SectionLabel(stringResource(R.string.default_split_exceptions))
    val rows = state.exceptionRows
    if (rows.isEmpty()) {
        Text(
            text = stringResource(R.string.default_split_exceptions_none),
            modifier = Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            rows.forEachIndexed { index, row ->
                Surface(
                    onClick = { onEvent(DefaultSplitUiEvent.OnExceptionClicked(row.exception.categoryId)) },
                    shape = createGroupedShape(index, rows.size),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .padding(
                                horizontal = 16.dp,
                                vertical = 8.dp,
                            ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryBadge(category = row.category)
                        Column {
                            Text(
                                text = categoryName(row.category),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = exceptionSummaryOf(state, row),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
    TextButton(onClick = { onEvent(DefaultSplitUiEvent.OnAddExceptionClicked) }) {
        Icon(
            Icons.Outlined.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.default_split_exception_add),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

// Why: "Igual entre todos", "Igual entre 2", "Partes, 2 para 1", "Porcentagem, 60% e 40%".
@Composable
private fun exceptionSummaryOf(
    state: DefaultSplitUiState,
    row: ExceptionRow,
): String {
    val values = row.valuesOf(state.people.map { it.id })
    return when (row.exception.mode) {
        DefaultSplitMode.SHARES -> stringResource(R.string.default_split_exception_shares, sharesTextOf(values))

        DefaultSplitMode.PERCENT, DefaultSplitMode.INCOME -> stringResource(
            R.string.default_split_exception_percent,
            values.map { stringResource(R.string.default_split_exception_percent_value, it) }.joinToString(" · "),
        )

        DefaultSplitMode.EQUAL -> if (row.includedCount >= row.totalCount) {
            stringResource(R.string.default_split_exception_equal_all)
        } else {
            pluralStringResource(R.plurals.default_split_exception_equal_among, row.includedCount, row.includedCount)
        }
    }
}

@Composable
private fun sharesTextOf(values: List<Long>): String = if (values.size == 2) {
    stringResource(R.string.default_split_exception_shares_two, values[0], values[1])
} else {
    values.joinToString(" · ")
}

// Why: "Exceção por categoria": one category, its own way and who is in it, saved only on "Salvar exceção".
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExceptionSheet(
    state: DefaultSplitUiState,
    editor: ExceptionEditor,
    money: MoneyFormatter,
    onEvent: (DefaultSplitUiEvent) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { onEvent(DefaultSplitUiEvent.OnExceptionDismissed) },
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.default_split_exception_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.default_split_exception_lead),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SectionLabel(stringResource(R.string.default_split_exception_category))
            CategoryChipRow(
                categories = state.exceptionMains,
                selectedId = editor.categoryId,
                openId = editor.openMainId,
                onEvent = onEvent,
            )
            val subcategories = state.exceptionSubcategories
            if (subcategories.isNotEmpty()) {
                CategoryChipRow(
                    categories = subcategories,
                    selectedId = editor.categoryId,
                    openId = null,
                    onEvent = onEvent,
                )
            }
            SectionLabel(stringResource(R.string.default_split_exception_how))
            ConnectedButtonRow(
                labels = state.exceptionModes.map { stringResource(it.exceptionLabelRes) },
                selectedIndex = state.exceptionModes.indexOf(editor.mode),
                groupLabel = stringResource(R.string.default_split_exception_how),
                onClick = { index ->
                    onEvent(DefaultSplitUiEvent.OnExceptionModeSelected(state.exceptionModes[index]))
                },
                itemRole = Role.RadioButton,
                height = 44.dp,
            )
            ExceptionPeopleList(
                editor = editor,
                onEvent = onEvent,
            )
            Text(
                text = exceptionStatusOf(state, editor, money),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodySmall,
                color = if (editor.showsPercentError || state.isExceptionCategoryTaken) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                if (editor.editedCategoryId != null) {
                    TextButton(onClick = { onEvent(DefaultSplitUiEvent.OnExceptionRemoveClicked) }) {
                        Text(
                            text = stringResource(R.string.default_split_exception_remove),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Button(
                    onClick = { onEvent(DefaultSplitUiEvent.OnExceptionSaveClicked) },
                    enabled = state.canSaveException,
                ) { Text(stringResource(R.string.default_split_exception_save)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChipRow(
    categories: List<Category>,
    selectedId: String?,
    openId: String?,
    onEvent: (DefaultSplitUiEvent) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            val isSelected = category.id == selectedId || category.id == openId
            ChoiceChipSurface(
                isSelected = isSelected,
                modifier = Modifier.selectable(
                    selected = category.id == selectedId,
                    role = Role.RadioButton,
                    onClick = { onEvent(DefaultSplitUiEvent.OnExceptionCategoryPicked(category.id)) },
                ),
            ) {
                Row(
                    modifier = Modifier
                        .heightIn(min = 32.dp)
                        .padding(
                            start = if (category.id == selectedId) 8.dp else 12.dp,
                            end = 12.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (category.id == selectedId) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = categoryName(category),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExceptionPeopleList(
    editor: ExceptionEditor,
    onEvent: (DefaultSplitUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        editor.people.forEachIndexed { index, person ->
            val name = personNameOf(person)
            val toggle = if (editor.mode == DefaultSplitMode.EQUAL) {
                Modifier.toggleable(
                    value = person.isIncluded,
                    role = Role.Checkbox,
                    onValueChange = { onEvent(DefaultSplitUiEvent.OnExceptionPersonToggled(person.id)) },
                )
            } else {
                Modifier
            }
            Surface(
                shape = createGroupedShape(index, editor.people.size),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = toggle,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(
                            horizontal = 12.dp,
                            vertical = 6.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (editor.mode ==
                        DefaultSplitMode.EQUAL
                    ) {
                        Checkbox(
                            checked = person.isIncluded,
                            onCheckedChange = null,
                        )
                    }
                    AvatarBadge(
                        initial = person.initials,
                        color = person.avatarColor,
                        size = 32.dp,
                    )
                    Text(
                        text = name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    when (editor.mode) {
                        DefaultSplitMode.SHARES -> PartsStepperRow(
                            parts = person.parts,
                            label = stringResource(R.string.default_split_parts_of, name),
                            onDecrease = { onEvent(DefaultSplitUiEvent.OnExceptionPartsDecreased(person.id)) },
                            onIncrease = { onEvent(DefaultSplitUiEvent.OnExceptionPartsIncreased(person.id)) },
                        )

                        DefaultSplitMode.PERCENT -> NumberField(
                            value = person.percentText,
                            description = stringResource(R.string.default_split_percent_of, name),
                            isError = editor.showsPercentError,
                            prefix = null,
                            suffix = "%",
                            isWide = false,
                            onChange = { onEvent(DefaultSplitUiEvent.OnExceptionPercentChanged(person.id, it)) },
                        )

                        else -> Unit
                    }
                }
            }
        }
    }
}

// Why: what is missing, else "Numa conta de R$ 100,00 no Mercado: Pierre R$ 50,00 · Ana R$ 50,00".
@Composable
private fun exceptionStatusOf(
    state: DefaultSplitUiState,
    editor: ExceptionEditor,
    money: MoneyFormatter,
): String {
    val category = state.exceptionCategory
    return when {
        category == null -> stringResource(R.string.default_split_exception_pick)

        state.isExceptionCategoryTaken -> stringResource(R.string.default_split_exception_taken, categoryName(category))

        editor.showsPercentError && editor.remainingPercent > 0 -> stringResource(
            R.string.default_split_exception_percent_error,
            editor.percentSum.toInt(),
            editor.remainingPercent.toInt(),
        )

        editor.showsPercentError -> stringResource(
            R.string.default_split_exception_percent_over,
            editor.percentSum.toInt(),
            -editor.remainingPercent.toInt(),
        )

        else -> stringResource(
            R.string.default_split_exception_example,
            money.toText(editor.figures.exampleTotalMinor),
            categoryName(category),
            editor.figures.rows
                .filter { (it.exampleMinor ?: 0L) > 0 }
                .map { "${personNameOf(it.person)} ${money.toText(it.exampleMinor ?: 0L)}" }
                .joinToString(" · "),
        )
    }
}

private val DefaultSplitMode.exceptionLabelRes: Int
    get() = when (this) {
        DefaultSplitMode.EQUAL -> R.string.default_split_exception_mode_equal
        DefaultSplitMode.SHARES -> R.string.default_split_exception_mode_shares
        else -> R.string.default_split_exception_mode_percent
    }
