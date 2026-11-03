package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: what a receipt read may choose the category from besides the app's (7.1e): the group's own categories and the
 * person's map terms that count in the group, so the group's "Pet" or "ração" among the items can choose it.
 */
data class ReceiptCategoryHints(
    // Why: the group's live categories, main ones and subcategories; the app's the reader already knows.
    val groupCategories: List<Category>,
    // Why: each pointing to a live category of the group or of the app, never to "Outros".
    val terms: List<CategoryTerm>,
) {
    companion object {
        // Why: read-receipt's limits; past them it drops the rest, so the request never carries what goes unread.
        const val MAX_CATEGORIES = 200
        const val MAX_TERMS = 500

        /*
         * Why: a term of another group's category doesn't count in this bill, and one of a deleted category reads as
         * where that category's bills go (7.8f). Past the limit the map's last terms stay, the person's newest after
         * the app's 60.
         */
        fun of(
            tree: CategoryTree,
            terms: List<CategoryTerm>,
        ): ReceiptCategoryHints = ReceiptCategoryHints(
            groupCategories = tree.all.filter { it.isGroupCategory }.take(MAX_CATEGORIES),
            terms = terms
                .mapNotNull { term ->
                    tree
                        .readAsOrNull(term.categoryId)
                        ?.takeIf { !it.isOther }
                        ?.let { term.copy(categoryId = it.id) }
                }.takeLast(MAX_TERMS),
        )
    }
}
