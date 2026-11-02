package com.quare.blitzsplit.feature.categorymap.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.CategoryMap
import com.quare.blitzsplit.core.expenses.domain.model.CategoryMatcher
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.MapTerm
import com.quare.blitzsplit.core.utils.TextFolding

/*
 * Why: "Mapa de categorias" (9.1): the person's terms by category, the app's and every group's on the device, the
 * search, "Novo termo" and "Editar termo" (9.2), the removed term's "Desfazer" and "Restaurar padrão".
 */
data class CategoryMapUiState(
    val isLoaded: Boolean,
    val map: CategoryMap,
    // Why: the app's categories with those of every group on the device among them.
    val categories: CategoryTree,
    val groupNames: Map<String, String>,
    val query: String,
    val selectedCategoryId: String?,
    val termForm: TermForm?,
    // Why: "“café” saiu do mapa" with "Desfazer" (9.1d), until another change or the notice goes.
    val removedTerm: MapTerm?,
    val isRestoreConfirmOpen: Boolean,
    val isOnline: Boolean,
    // Why: with Google or Apple the map reaches the person's other devices; otherwise it stays on this one (9.1h).
    val isConnected: Boolean,
) {
    // Why: once per state, since the list, selectedSection and each platform's layout all read it.
    val sections: List<MapSection> by lazy {
        categories.branches.mapNotNull { branch ->
            val main = branch.category
            val rows = (branch.subcategories.map { it to false } + (main to true))
                .map { (category, isOnlyMain) ->
                    MapRow(
                        category = category,
                        isOnlyMain = isOnlyMain,
                        groupName = groupNameOf(category).takeIf { category.parentId != null && !isOnlyMain },
                        terms = map.terms.filter { it.categoryId == category.id },
                    )
                }.filter { it.terms.isNotEmpty() }
            MapSection(
                category = main,
                groupName = groupNameOf(main),
                subcategoryCount = branch.subcategories.size,
                rows = rows,
            ).takeUnless { main.isOther && rows.isEmpty() }
        }
    }

    // Why: lying down and on tablets, the category picked beside the list, the first until one is.
    val selectedSection: MapSection?
        get() = sections.firstOrNull { it.category.id == selectedCategoryId } ?: sections.firstOrNull()

    val isSearching: Boolean
        get() = query.isNotBlank()

    // Why: "2 termos com “uber”": each term with a word starting with what is typed, ignoring case and accents.
    val searchResults: List<MapSearchResult> by lazy {
        val folded = CategoryMatcher.keyOf(query)
        if (folded.isEmpty()) {
            emptyList()
        } else {
            map.terms.mapNotNull { term ->
                val start = matchStartOrNull(term.key, folded) ?: return@mapNotNull null
                val category = categories.findOrNull(term.categoryId) ?: return@mapNotNull null
                val isMarked = TextFolding.toFolded(term.term) == term.key
                MapSearchResult(
                    term = term,
                    category = category,
                    parent = category.parentId?.let { categories.findOrNull(it) },
                    matchStart = if (isMarked) start else 0,
                    matchEnd = if (isMarked) start + folded.length else 0,
                )
            }
        }
    }

    val showsNoResult: Boolean
        get() = isSearching && searchResults.isEmpty()

    val isEmpty: Boolean
        get() = isLoaded && map.isEmpty

    val showsOfflineNotice: Boolean
        get() = !isOnline && map.pendingCount > 0

    val showsDeviceOnlyNotice: Boolean
        get() = isLoaded && !isConnected

    // Why: "O termo que você criou, “picanha”, continua." in "Restaurar o mapa padrão?" (9.1f).
    val ownTerms: List<MapTerm>
        get() = map.terms.filterNot { it.isAppTerm }

    val formCategory: Category?
        get() = termForm?.let { categories.findOrNull(it.categoryId) }

    val formCategoryParent: Category?
        get() = formCategory?.parentId?.let { categories.findOrNull(it) }

    val termError: TermError?
        get() {
            val text = termForm?.text?.trim() ?: return null
            if (text.isEmpty()) return null
            return when {
                CategoryMatcher.isTermValid(text) -> null
                text.any { !it.isLetterOrDigit() && it != ' ' } -> TermError.INVALID_CHARACTERS
                text.length > CategoryMatcher.MAX_TERM_LENGTH -> TermError.TOO_LONG
                else -> TermError.TOO_SHORT
            }
        }

    // Why: "“uber” já escolhe Transporte › Corrida por app": the word typed is another term, so Salvar becomes Mover.
    val duplicateTerm: MapTerm?
        get() {
            val form = termForm ?: return null
            val key = CategoryMatcher.keyOf(form.text)
            return map.findOrNull(key)?.takeIf { it.key != form.original?.key && it.categoryId != form.categoryId }
        }

    val duplicateCategory: Category?
        get() = duplicateTerm?.let { categories.findOrNull(it.categoryId) }

    val canSaveTerm: Boolean
        get() {
            val form = termForm ?: return false
            val isChanged =
                form.original?.let { it.term != form.text.trim() || it.categoryId != form.categoryId } ?: true
            return !form.isSaving && termError == null && CategoryMatcher.isTermValid(form.text) && isChanged &&
                formCategory != null
        }

    // Why: every category a term may send titles to, in the picker's order, the groups' with their group's name.
    val categoryOptions: List<Category>
        get() = categories.all

    fun groupNameOf(category: Category): String? = categories.groupIdOf(category.id)?.let(groupNames::get)

    private fun matchStartOrNull(
        key: String,
        query: String,
    ): Int? {
        if (key.startsWith(query)) return 0
        val index = key.indexOf(" $query")
        return if (index >= 0) index + 1 else null
    }
}
