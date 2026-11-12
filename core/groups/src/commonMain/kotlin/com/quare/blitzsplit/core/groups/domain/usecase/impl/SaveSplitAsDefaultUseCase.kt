package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.model.CategorySplit
import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.SaveSplitAsDefault
import com.quare.blitzsplit.core.groups.domain.usecase.UpdateDefaultSplit
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.first

/*
 * Why: only the part the bill sets changes. The group's own default keeps every exception, and a split by income saves
 * its incomes for the group; an exception replaces only itself, in its place, and leaves the default as it is. An
 * exception has no "By income", so a bill split by income makes it its percentages. Saved through "Divisão padrão"'s
 * own rules, so "Igual" with everyone in and no exception is still saved as none.
 */
class SaveSplitAsDefaultUseCase(
    private val observeGroup: ObserveGroup,
    private val updateDefaultSplit: UpdateDefaultSplit,
) : SaveSplitAsDefault {
    override suspend fun invoke(
        groupId: String,
        categoryId: String?,
        mode: DefaultSplitMode,
        weights: Map<String, Long>,
        incomes: Map<String, Long>,
    ): Result<Unit> = suspendRunCatching {
        val current = observeGroup(groupId).first()?.defaultSplit
        if (categoryId == null) {
            val default = GroupDefaultSplit(
                mode = mode,
                weights = weights,
                exceptions = current?.exceptions.orEmpty(),
            )
            return@suspendRunCatching updateDefaultSplit(groupId, default, incomes).getOrThrow()
        }
        // Why: a group without a default splits equally among everyone, whoever joins included.
        val base = current ?: GroupDefaultSplit(DefaultSplitMode.EQUAL, emptyMap(), emptyList())
        val exception = CategorySplit(
            categoryId = categoryId,
            mode = if (mode == DefaultSplitMode.INCOME) DefaultSplitMode.PERCENT else mode,
            weights = weights,
        )
        val exceptions = if (base.exceptions.any { it.categoryId == categoryId }) {
            base.exceptions.map { if (it.categoryId == categoryId) exception else it }
        } else {
            base.exceptions + exception
        }
        updateDefaultSplit(groupId, base.copy(exceptions = exceptions), emptyMap()).getOrThrow()
    }
}
