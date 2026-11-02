package com.quare.blitzsplit.android.feature.categorymap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.CategoryBadge
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.format.categoryName
import com.quare.blitzsplit.core.expenses.domain.model.MapTerm
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiEvent
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiState
import com.quare.blitzsplit.feature.categorymap.presentation.model.MapRow
import com.quare.blitzsplit.feature.categorymap.presentation.model.MapSection

private val pendingDotSize = 8.dp
private val rowLabelWidth = 96.dp
private val pendingColor = Color(0xFFD97706)

// Why: "Transporte · 7 termos", "Nenhum termo · categoria do Viagem Floripa".
@Composable
internal fun sectionCountText(section: MapSection): String {
    val count = section.termCount
    val terms = if (count == 0) {
        stringResource(R.string.category_map_no_terms)
    } else {
        pluralStringResource(R.plurals.category_map_term_count, count, count)
    }
    val group = section.groupName?.let { stringResource(R.string.category_map_group_category, it) }.orEmpty()
    return terms + group
}

// Why: a phone's card per main category (9.1): its terms in lines by subcategory, then "Adicionar termo em X".
@Composable
internal fun SectionCard(
    section: MapSection,
    state: CategoryMapUiState,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(
                start = 12.dp,
                end = 8.dp,
                top = 12.dp,
                bottom = 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionHeader(section = section)
            SectionRowsList(
                section = section,
                state = state,
                onEvent = onEvent,
            )
            AddInSectionButton(
                section = section,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun SectionHeader(section: MapSection) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(category = section.category)
        Column {
            Text(
                text = categoryName(section.category),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = sectionCountText(section),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionRowsList(
    section: MapSection,
    state: CategoryMapUiState,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    if (section.rows.isEmpty()) {
        Text(
            text = stringResource(R.string.category_map_no_terms_yet),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    section.rows.forEach { row ->
        TermsLine(
            row = row,
            state = state,
            onEvent = onEvent,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TermsLine(
    row: MapRow,
    state: CategoryMapUiState,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            modifier = Modifier
                .width(rowLabelWidth)
                .padding(top = 7.dp),
        ) {
            Text(
                text = rowLabelOf(row),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            row.groupName?.let {
                Text(
                    text = stringResource(R.string.category_map_only_in_group, it),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            row.terms.forEach { term ->
                TermChip(
                    term = term,
                    path = pathOf(row.category, state),
                    onClick = { onEvent(CategoryMapUiEvent.OnTermClicked(term.key)) },
                )
            }
        }
    }
}

@Composable
private fun rowLabelOf(row: MapRow): String = if (row.isOnlyMain) {
    stringResource(R.string.category_map_only_main, categoryName(row.category))
} else {
    categoryName(row.category)
}

// Why: a term as a chip, with the orange dot while it waits in the queue (9.1g); a tap opens "Editar termo".
@Composable
internal fun TermChip(
    term: MapTerm,
    path: String,
    onClick: () -> Unit,
) {
    val queued = stringResource(R.string.category_map_queued)
    val label = stringResource(R.string.category_map_term_action, term.term, path) +
        if (term.isPending) ", $queued" else ""
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .heightIn(min = 32.dp)
            .semantics { contentDescription = label }
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 6.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = term.term,
                style = MaterialTheme.typography.labelLarge,
            )
            if (term.isPending) {
                Box(
                    modifier = Modifier
                        .size(pendingDotSize)
                        .background(pendingColor, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun AddInSectionButton(
    section: MapSection,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    TextButton(onClick = { onEvent(CategoryMapUiEvent.OnAddTermClicked(section.category.id, "")) }) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.category_map_add_in, categoryName(section.category)),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

// Why: lying down and on tablets, a main category to pick, its terms showing beside the list.
@Composable
internal fun SectionListRow(
    section: MapSection,
    isSelected: Boolean,
    index: Int,
    count: Int,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    Surface(
        shape = groupedRowShapeOf(index, count),
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        contentColor = if (isSelected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { selected = isSelected }
            .clickable(role = Role.Tab) { onEvent(CategoryMapUiEvent.OnSectionSelected(section.category.id)) },
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 60.dp)
                .padding(
                    horizontal = 12.dp,
                    vertical = 6.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryBadge(category = section.category)
            Column {
                Text(
                    text = categoryName(section.category),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
                Text(
                    text = sectionCountText(section),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

// Why: the terms of the main category picked, beside the list (9.1, two columns).
@Composable
internal fun SectionDetailPane(
    section: MapSection,
    state: CategoryMapUiState,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryBadge(
                category = section.category,
                size = 48.dp,
            )
            Column {
                Text(
                    text = categoryName(section.category),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = sectionCountText(section) + subcategoriesText(section),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionRowsList(
                    section = section,
                    state = state,
                    onEvent = onEvent,
                )
                AddInSectionButton(
                    section = section,
                    onEvent = onEvent,
                )
            }
        }
        Text(
            text = stringResource(R.string.category_map_tip),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun subcategoriesText(section: MapSection): String {
    if (section.subcategoryCount == 0) return ""
    return " · " +
        pluralStringResource(
            R.plurals.category_map_subcategory_count,
            section.subcategoryCount,
            section.subcategoryCount,
        )
}
