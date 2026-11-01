package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.groups.domain.model.CategorySplit
import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency

data class DefaultSplitUiState(
    // Why: "Apê 302 · para as despesas novas".
    val groupName: String,
    val currency: GroupCurrency,
    val mode: DefaultSplitMode,
    // Why: in the order they were added to the group, the creator first.
    val people: List<DefaultSplitPerson>,
    /*
     * Why: the percentages "Pela renda" shows while no income is typed on this device: the ones the group saved, or
     * the split's as it was when someone switched to it. The incomes stay with whoever typed them.
     */
    val incomeFallback: Map<String, Long>,
    val isOnline: Boolean,
    // Why: nothing is drawn until Room is read.
    val isLoaded: Boolean,
    val isSaving: Boolean,
    // Why: the device couldn't save the split; cleared by the next try.
    val showsSaveError: Boolean,
    // Why: Salvar stays off until the split differs from what the screen opened with, or someone new is in it.
    val hasChanges: Boolean,
    /*
     * Why: what the form gives (DefaultSplitFigures.of), worked out once per change of it with every such copy, not
     * on each copy for the connection or a save going on, since every UI reads it on each keystroke.
     */
    val figures: DefaultSplitFigures,
    // Why: "Exceções por categoria" (6.7f) as they will be saved, in the order they were added.
    val exceptions: List<CategorySplit>,
    // Why: the app's categories with the group's, which the exceptions name.
    val categories: CategoryTree,
    // Why: "Exceção por categoria", open while not null.
    val exceptionEditor: ExceptionEditor?,
) {
    // Why: each one's weight as Salvar saves it.
    val weights: Map<String, Long>
        get() = figures.weights

    // Why: "Porcentagem" and "Pela renda" add up to 100; the others don't count percentages.
    val usesPercent: Boolean
        get() = DefaultSplitFigures.isPercentBased(mode)

    val includedCount: Int
        get() = weights.values.count { it > 0 }

    // Why: "A soma dá 90%".
    val percentSum: Long
        get() = if (usesPercent) weights.values.sum() else GroupDefaultSplit.FULL_PERCENT

    // Why: "Faltam 10%" while positive, too much while negative.
    val remainingPercent: Long
        get() = GroupDefaultSplit.FULL_PERCENT - percentSum

    val addsUp: Boolean
        get() = includedCount > 0 && percentSum == GroupDefaultSplit.FULL_PERCENT

    // Why: "A soma dá 90%. Faltam 10%." in place of the example, with Salvar disabled.
    val showsPercentError: Boolean
        get() = isLoaded && usesPercent && percentSum != GroupDefaultSplit.FULL_PERCENT

    /*
     * Why: "Numa conta de R$ 100,00", or for "Igual" the first round amount everyone gets the same of ("Numa conta de
     * R$ 120,00: R$ 20,00 para cada um" among six).
     */
    val exampleTotalMinor: Long
        get() = figures.exampleTotalMinor

    // Why: "R$ 20,00 para cada um", Igual's example.
    val exampleEachMinor: Long
        get() = if (includedCount == 0) 0L else exampleTotalMinor / includedCount

    val rows: List<DefaultSplitRow>
        get() = figures.rows

    // Why: "Carla entrou no grupo. Ela começa com 1 parte; ajuste se precisar.", until someone saves.
    val joinedPeople: List<DefaultSplitPerson>
        get() = people.filter { it.isNew }

    val modes: List<DefaultSplitMode>
        get() = DefaultSplitMode.entries

    val canSave: Boolean
        get() = isLoaded && hasChanges && addsUp && !isSaving

    // Why: "Sem internet: a divisão nova vale neste aparelho agora e chega aos outros quando ele reconectar."
    val showsOfflineNotice: Boolean
        get() = !isOnline

    // Why: what Salvar saves.
    val defaultSplit: GroupDefaultSplit
        get() = GroupDefaultSplit(
            mode = mode,
            weights = weights,
            exceptions = exceptions,
        )

    // Why: "Mercado · Igual entre todos", each exception with the category it reads as now.
    val exceptionRows: List<ExceptionRow>
        get() = exceptions.map { exception ->
            ExceptionRow(
                exception = exception,
                category = categories.resolve(exception.categoryId),
                includedCount = people.count { exception.weights[it.id]?.let { weight -> weight > 0 } ?: true },
                totalCount = people.size,
            )
        }

    // Why: the categories an exception may pick: every main one but "Outros", then the open one's subcategories.
    val exceptionMains: List<Category>
        get() = categories.branches.map { it.category }.filterNot { it.isOther }

    val exceptionSubcategories: List<Category>
        get() = exceptionEditor
            ?.openMainId
            ?.let { categories.branchOf(it) }
            ?.subcategories
            .orEmpty()

    // Why: a category takes one exception; the one being changed may keep its own.
    val isExceptionCategoryTaken: Boolean
        get() {
            val editor = exceptionEditor ?: return false
            return exceptions.any { it.categoryId == editor.categoryId && it.categoryId != editor.editedCategoryId }
        }

    val canSaveException: Boolean
        get() {
            val editor = exceptionEditor ?: return false
            return editor.categoryId != null && editor.addsUp && !isExceptionCategoryTaken
        }

    // Why: "Numa conta de R$ 100,00 no Mercado: Pierre R$ 50,00 · Ana R$ 50,00".
    val exceptionCategory: Category?
        get() = exceptionEditor?.categoryId?.let { categories.resolve(it) }

    val exceptionModes: List<DefaultSplitMode>
        get() = CategorySplit.modes

    // Why: the incomes typed, by member id in minor units, which stay on this device.
    val incomes: Map<String, Long>
        get() = figures.incomes
}
