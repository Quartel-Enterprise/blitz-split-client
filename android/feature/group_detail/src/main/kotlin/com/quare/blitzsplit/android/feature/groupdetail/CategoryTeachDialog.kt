package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.categoryName
import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.feature.groupdetail.presentation.model.CategoryTeach
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.TeachWord

@Composable
private fun pathOf(
    category: Category,
    parent: Category?,
): String = parent?.let { stringResource(R.string.group_detail_category_path, categoryName(it), categoryName(category)) }
    ?: categoryName(category)

/*
 * Why: "Conta salva em Lazer › Esportes, e não em Casa › Aluguel, que o mapa sugeriu. Ensinar ao mapa?" (9.3e): the
 * snackbar's action teaches, dismissing it is "Agora não".
 */
@Composable
internal fun CategoryTeachNoticeEffect(
    teach: CategoryTeach?,
    snackbarHostState: SnackbarHostState,
    onEvent: (GroupDetailUiEvent) -> Unit,
) {
    val message = teach?.let {
        val saved = pathOf(it.category, it.categoryParent)
        val mapCategory = it.mapCategory
        if (mapCategory == null) {
            stringResource(R.string.group_detail_teach_offer_no_map, saved)
        } else {
            stringResource(R.string.group_detail_teach_offer, saved, pathOf(mapCategory, it.mapCategoryParent))
        }
    }
    val action = stringResource(R.string.group_detail_teach)
    LaunchedEffect(teach?.expenseId, teach?.isPickingWord) {
        if (message == null || teach.isPickingWord) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = action,
            withDismissAction = true,
            duration = SnackbarDuration.Indefinite,
        )
        onEvent(
            if (result == SnackbarResult.ActionPerformed) {
                GroupDetailUiEvent.OnTeachClicked
            } else {
                GroupDetailUiEvent.OnTeachDismissed
            },
        )
    }
}

// Why: "Ensinar ao mapa" (9.3f): which word of the title picks the category on the next bills.
@Composable
internal fun CategoryTeachDialog(
    teach: CategoryTeach,
    onEvent: (GroupDetailUiEvent) -> Unit,
) {
    val target = pathOf(teach.category, teach.categoryParent)
    AlertDialog(
        onDismissRequest = { onEvent(GroupDetailUiEvent.OnTeachDismissed) },
        title = { Text(stringResource(R.string.group_detail_teach_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.group_detail_teach_question, teach.title, target))
                Column(modifier = Modifier.selectableGroup()) {
                    teach.words.forEach { word ->
                        TeachWordRow(
                            word = word,
                            target = target,
                            isSelected = word.key == teach.selectedKey,
                            onClick = { onEvent(GroupDetailUiEvent.OnTeachWordPicked(word.key)) },
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.group_detail_teach_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(GroupDetailUiEvent.OnTeachConfirmed) },
                enabled = teach.selectedWord != null && !teach.isSaving,
            ) {
                Text(stringResource(R.string.group_detail_teach))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(GroupDetailUiEvent.OnTeachDismissed) }) {
                Text(stringResource(R.string.group_detail_teach_cancel))
            }
        },
    )
}

@Composable
private fun TeachWordRow(
    word: TeachWord,
    target: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val current = word.currentCategory
    val detail = if (current == null) {
        stringResource(R.string.group_detail_teach_word_new, word.term, target)
    } else {
        stringResource(R.string.group_detail_teach_word_moves, categoryName(current), word.term, target)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Column {
            Text(text = word.term, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
