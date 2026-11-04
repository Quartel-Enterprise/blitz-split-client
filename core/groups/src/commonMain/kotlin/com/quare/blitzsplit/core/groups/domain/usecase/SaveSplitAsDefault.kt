package com.quare.blitzsplit.core.groups.domain.usecase

import com.quare.blitzsplit.core.model.DefaultSplitMode

/*
 * Why: Rachar conta's "Usar como padrão do grupo" (beta feedback 11): the split a new bill is saved with becomes the
 * group's "Divisão padrão", or the exception of the category it opened with, saved on this device and synced later.
 */
fun interface SaveSplitAsDefault {
    // Why: categoryId names the exception to replace, null for the group's own default; weights by member id.
    suspend operator fun invoke(
        groupId: String,
        categoryId: String?,
        mode: DefaultSplitMode,
        weights: Map<String, Long>,
    ): Result<Unit>
}
