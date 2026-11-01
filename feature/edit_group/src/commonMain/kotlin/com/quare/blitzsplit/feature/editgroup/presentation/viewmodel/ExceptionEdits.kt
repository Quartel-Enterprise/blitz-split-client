package com.quare.blitzsplit.feature.editgroup.presentation.viewmodel

import com.quare.blitzsplit.core.groups.domain.model.CategorySplit
import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.groups.domain.model.IncomePercents
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitFigures
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitPerson
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitUiState
import com.quare.blitzsplit.feature.editgroup.presentation.model.ExceptionEditor

// Why: "Adicionar exceção" starts with no category, equally among everyone; an exception opens as it was saved.
internal fun DefaultSplitUiState.withExceptionOpened(categoryId: String?): DefaultSplitUiState {
    val saved = categoryId?.let { id -> exceptions.firstOrNull { it.categoryId == id } }
    val mode = saved?.mode ?: DefaultSplitMode.EQUAL
    val people = people.map { it.inException(mode, saved?.weights?.let { weights -> weights[it.id] ?: newcomerOf(mode) }) }
    val main = saved?.let { categories.mainOf(categories.resolve(it.categoryId)) }
    return copy(
        exceptionEditor = ExceptionEditor(
            editedCategoryId = saved?.categoryId,
            categoryId = saved?.categoryId,
            openMainId = main?.id,
            mode = mode,
            people = people,
            figures = figuresOf(mode, people),
        ),
    )
}

// Why: a main category shows its subcategories and is picked itself until one of them is.
internal fun DefaultSplitUiState.withExceptionCategory(categoryId: String): DefaultSplitUiState {
    val editor = exceptionEditor ?: return this
    val category = categories.findOrNull(categoryId) ?: return this
    return copy(
        exceptionEditor = editor.copy(
            categoryId = category.id,
            openMainId = if (category.isMain) category.id else editor.openMainId,
        ),
    )
}

// Why: who is in stays in: "Igual" checks them, "Partes" gives them one each, "Porcentagem" shares 100 among them.
internal fun DefaultSplitUiState.withExceptionMode(mode: DefaultSplitMode): DefaultSplitUiState {
    val editor = exceptionEditor?.takeIf { mode in CategorySplit.modes && mode != it.mode } ?: return this
    val inSplit = editor.figures.weights.filterValues { it > 0 }.keys.ifEmpty { editor.people.map { it.id }.toSet() }
    val percents = IncomePercents.of(editor.people.map { it.id to if (it.id in inSplit) 1L else 0L })
    val people = editor.people.map { person ->
        val isIn = person.id in inSplit
        val weight = when (mode) {
            DefaultSplitMode.PERCENT -> percents[person.id] ?: 0L
            else -> if (isIn) 1L else 0L
        }
        person.inException(mode, weight)
    }
    return copy(exceptionEditor = editor.copy(mode = mode, people = people, figures = figuresOf(mode, people)))
}

internal fun DefaultSplitUiState.withExceptionPerson(
    memberId: String,
    change: (DefaultSplitPerson) -> DefaultSplitPerson,
): DefaultSplitUiState {
    val editor = exceptionEditor ?: return this
    val people = editor.people.map { if (it.id == memberId) change(it) else it }
    return copy(exceptionEditor = editor.copy(people = people, figures = figuresOf(editor.mode, people)))
}

// Why: "Salvar exceção" changes the list in place, or adds it at the end.
internal fun DefaultSplitUiState.withExceptionSaved(): DefaultSplitUiState {
    val editor = exceptionEditor ?: return this
    val categoryId = editor.categoryId ?: return this
    if (!canSaveException) return this
    val exception = CategorySplit(
        categoryId = categoryId,
        mode = editor.mode,
        weights = editor.figures.weights,
    )
    val edited = editor.editedCategoryId
    val list = if (edited != null && exceptions.any { it.categoryId == edited }) {
        exceptions.map { if (it.categoryId == edited) exception else it }
    } else {
        exceptions + exception
    }
    return copy(
        exceptions = list,
        exceptionEditor = null,
    )
}

internal fun DefaultSplitUiState.withExceptionRemoved(): DefaultSplitUiState {
    val edited = exceptionEditor?.editedCategoryId ?: return copy(exceptionEditor = null)
    return copy(
        exceptions = exceptions.filterNot { it.categoryId == edited },
        exceptionEditor = null,
    )
}

private fun newcomerOf(mode: DefaultSplitMode): Long = GroupDefaultSplit(mode, emptyMap(), emptyList()).weightOf("")

private fun DefaultSplitPerson.inException(
    mode: DefaultSplitMode,
    weight: Long?,
): DefaultSplitPerson {
    val value = weight ?: 1L
    return copy(
        isNew = false,
        isIncluded = mode != DefaultSplitMode.EQUAL || value > 0,
        parts = if (mode == DefaultSplitMode.SHARES) value else 1L,
        percentText = if (mode == DefaultSplitMode.PERCENT) value.toString() else "",
        incomeText = "",
    )
}

private fun DefaultSplitUiState.figuresOf(
    mode: DefaultSplitMode,
    people: List<DefaultSplitPerson>,
): DefaultSplitFigures = DefaultSplitFigures.of(
    mode = mode,
    people = people,
    incomeFallback = emptyMap(),
    currency = currency,
)
