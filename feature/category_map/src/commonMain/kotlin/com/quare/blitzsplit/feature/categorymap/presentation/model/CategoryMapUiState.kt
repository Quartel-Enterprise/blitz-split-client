package com.quare.blitzsplit.feature.categorymap.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.CategoryDeletion
import com.quare.blitzsplit.core.expenses.domain.model.CategoryEditor
import com.quare.blitzsplit.core.expenses.domain.model.CategoryForm
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
    /*
     * Why: the terms, every group's categories, the groups' names and the account were each read, and the account's
     * copy is whole (a restored account's own terms only come with its first full sync). Until then the map
     * shimmers and takes no change, never "Nenhum termo" nor "Seu mapa fica só neste aparelho" before its time.
     */
    val isLoaded: Boolean,
    val map: CategoryMap,
    // Why: the app's categories with those of every group on the device among them.
    val categories: CategoryTree,
    val groups: List<MapGroup>,
    val query: String,
    val selectedCategoryId: String?,
    val termForm: TermForm?,
    // Why: "“café” saiu do mapa" with "Desfazer" (9.1d), until another change or the notice goes.
    val removedTerm: MapTerm?,
    val isRestoreConfirmOpen: Boolean,
    // Why: "Nova categoria", "Nova subcategoria" and "Editar categoria" (9.4), a group's, as a bill's Categoria opens it.
    val categoryForm: CategoryForm?,
    val categoryDeletion: CategoryDeletion?,
    // Why: how many of the form's group's bills each category has, by the category they read as.
    val categoryUsage: Map<String, Int>,
    val isOnline: Boolean,
    // Why: with Google or Apple the map reaches the person's other devices; otherwise it stays on this one (9.1h).
    val isConnected: Boolean,
) {
    /*
     * Why: the terms as the map shows them: one whose group category was deleted reads as its parent, or Outros, as
     * that category's bills do, so it stays there to edit or remove; one whose group left the device isn't shown, nor
     * counted as a term, though its change waiting to go up still counts in the queue.
     */
    val terms: List<MapTerm> by lazy {
        map.terms.mapNotNull { term ->
            val category = categories.readAsOrNull(term.categoryId) ?: return@mapNotNull null
            if (category.id == term.categoryId) term else term.copy(categoryId = category.id)
        }
    }

    // Why: once per state, since the list, selectedSection and each platform's layout all read it.
    val sections: List<MapSection> by lazy {
        if (!isLoaded) return@lazy emptyList()
        categories.branches.mapNotNull { branch ->
            val main = branch.category
            // Why: the lines with terms, and a group's subcategory with none yet, to add its first one (9.4).
            val rows = (branch.subcategories.map { it to false } + (main to true))
                .map { (category, isOnlyMain) ->
                    MapRow(
                        category = category,
                        isOnlyMain = isOnlyMain,
                        groupName = groupNameOf(category).takeIf { category.parentId != null && !isOnlyMain },
                        terms = terms.filter { it.categoryId == category.id },
                    )
                }.filter { it.terms.isNotEmpty() || (it.category.isGroupCategory && !it.isOnlyMain) }
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
        if (folded.isEmpty() || !isLoaded) {
            emptyList()
        } else {
            terms.mapNotNull { term ->
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
        get() = isLoaded && isSearching && searchResults.isEmpty()

    val isEmpty: Boolean
        get() = isLoaded && terms.isEmpty()

    val showsOfflineNotice: Boolean
        get() = !isOnline && map.pendingCount > 0

    val showsDeviceOnlyNotice: Boolean
        get() = isLoaded && !isConnected

    // Why: a change before the account's own terms came down could go up over them; Restaurar padrão too.
    val canEditMap: Boolean
        get() = isLoaded

    // Why: "O termo que você criou, “picanha”, continua." in "Restaurar o mapa padrão?" (9.1f).
    val ownTerms: List<MapTerm>
        get() = terms.filterNot { it.isAppTerm }

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
            return findTermOrNull(key)?.takeIf { it.key != form.original?.key && it.categoryId != form.categoryId }
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

    // Why: a category goes in a group, so with none on the device there is nowhere to create one (9.4).
    val canCreateCategory: Boolean
        get() = canEditMap && groups.isNotEmpty()

    // Why: "Nova categoria" (9.4) as a bill's Categoria draws it, checked against the categories of its group alone.
    val categoryEditor: CategoryEditor?
        get() = categoryForm?.let(::categoryEditorOf)

    // Why: also the form a sheet still closing holds, once categoryForm is gone.
    fun categoryEditorOf(form: CategoryForm): CategoryEditor {
        val group = groups.firstOrNull { it.id == form.groupId }
        val tree = categories.ofGroup(form.groupId)
        return CategoryEditor(
            form = form,
            categories = tree,
            groupName = group?.name.orEmpty(),
            creatorName = form.createdByMemberId?.let { group?.memberNames?.get(it) },
            usage = CategoryEditor.usageOf(tree, form.categoryId, categoryUsage),
            deletion = categoryDeletion,
            isOnline = isOnline,
        )
    }

    // Why: "Grupo", only for a new category: one being edited stays in its group.
    val canPickCategoryGroup: Boolean
        get() = categoryForm?.isEditing == false && groups.size > 1

    // Why: every category a term may send titles to, in the picker's order, the groups' with their group's name.
    val categoryOptions: List<Category>
        get() = categories.all

    fun findTermOrNull(key: String): MapTerm? = terms.firstOrNull { it.key == key }

    fun groupNameOf(category: Category): String? =
        categories.groupIdOf(category.id)?.let { id -> groups.firstOrNull { it.id == id }?.name }

    private fun matchStartOrNull(
        key: String,
        query: String,
    ): Int? {
        if (key.startsWith(query)) return 0
        val index = key.indexOf(" $query")
        return if (index >= 0) index + 1 else null
    }
}
