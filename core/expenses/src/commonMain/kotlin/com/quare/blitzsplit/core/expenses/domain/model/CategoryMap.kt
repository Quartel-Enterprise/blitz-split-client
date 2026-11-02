package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: the person's map (flow 9): the app's 60 terms with what they changed on top: a term added or moved to another
 * category, a term removed, and what "Restaurar padrão" gave back. pendingCount counts the changes waiting to go up,
 * removed terms included, for "Offline · 2 alterações na fila".
 */
data class CategoryMap(
    val terms: List<MapTerm>,
    val pendingCount: Int,
) {
    val isEmpty: Boolean
        get() = terms.isEmpty()

    fun findOrNull(key: String): MapTerm? = terms.firstOrNull { it.key == key }

    companion object {
        fun of(
            changes: List<CategoryTermChange>,
            defaults: List<CategoryTerm> = DefaultCategoryTerms.terms,
        ): CategoryMap {
            val appKeys = defaults.map { CategoryMatcher.keyOf(it.term) }.toSet()
            val byKey = LinkedHashMap<String, MapTerm>()
            defaults.forEach { term ->
                val key = CategoryMatcher.keyOf(term.term)
                byKey[key] = MapTerm(
                    term = term.term,
                    key = key,
                    categoryId = term.categoryId,
                    isAppTerm = true,
                    isPending = false,
                )
            }
            changes.forEach { change ->
                val categoryId = change.categoryId
                when {
                    change.isRestored -> byKey[change.key]?.let {
                        byKey[change.key] =
                            it.copy(isPending = change.isPending)
                    }

                    categoryId == null -> byKey.remove(change.key)

                    else -> byKey[change.key] = MapTerm(
                        term = change.term,
                        key = change.key,
                        categoryId = categoryId,
                        isAppTerm = change.key in appKeys,
                        isPending = change.isPending,
                    )
                }
            }
            return CategoryMap(
                terms = byKey.values.toList(),
                pendingCount = changes.count { it.isPending },
            )
        }
    }
}
