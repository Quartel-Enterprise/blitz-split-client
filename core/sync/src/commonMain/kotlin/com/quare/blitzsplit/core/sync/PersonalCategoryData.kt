package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.expenses.domain.repository.CategoryTermRepository
import com.quare.blitzsplit.core.expenses.domain.repository.CustomColorRepository
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/*
 * Why: what the person keeps about categories for themselves, in every group and on every device of the account: their
 * category map (flow 9) and their palette of custom colors. Neither needs a group, so they sync right after the profile,
 * and both go with the account's copy.
 */
class PersonalCategoryData(
    private val categoryTermRepository: CategoryTermRepository,
    private val customColorRepository: CustomColorRepository,
) {
    // Why: the map first; a palette that fails still fails the sync, but never holds the map back.
    suspend fun sync(userId: String): Result<Unit> {
        val terms = suspendRunCatching {
            categoryTermRepository.download(userId).getOrThrow()
            categoryTermRepository.uploadPending(userId).getOrThrow()
        }
        val colors = suspendRunCatching {
            customColorRepository.download(userId).getOrThrow()
            customColorRepository.uploadPending(userId).getOrThrow()
        }
        return if (terms.isFailure) terms else colors
    }

    // Why: each on its own, so one that fails never keeps the other on the device; the first failure still fails the sign out.
    suspend fun clear() {
        val terms = suspendRunCatching { categoryTermRepository.clear() }
        val colors = suspendRunCatching { customColorRepository.clear() }
        terms.getOrThrow()
        colors.getOrThrow()
    }

    // Why: changes the sync keys on, so each new one counts while one going out triggers nothing.
    fun observeChanges(): Flow<List<Any>> = combine(
        categoryTermRepository.observeChanges(),
        customColorRepository.observeVersions(),
    ) { terms, colors -> listOf(terms, colors) }

    fun observeHasPendingChanges(): Flow<Boolean> = combine(
        categoryTermRepository.observeHasPendingChanges(),
        customColorRepository.observeHasPendingChanges(),
    ) { terms, colors -> terms || colors }
}
