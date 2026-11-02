package com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.CategoryMap
import com.quare.blitzsplit.core.expenses.domain.model.CategoryMatcher
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTeachOffer
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.feature.groupdetail.presentation.model.CategoryTeach
import com.quare.blitzsplit.feature.groupdetail.presentation.model.TeachWord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/*
 * Why: "Ensinar ao mapa?" (9.3e) once a bill of this group whose category was picked by hand is saved. The offer is
 * consumed as it arrives, so it shows on this screen once, and teaching saves the word picked in the person's map,
 * moving it from any other category, another group's included (one category per term); the bills already saved
 * don't change.
 */
internal class CategoryTeaching(
    private val groupId: String,
    private val useCases: CategoryTeachUseCases,
    private val scope: CoroutineScope,
    private val read: () -> CategoryTeach?,
    private val write: (CategoryTeach?) -> Unit,
    private val showError: suspend () -> Unit,
) {
    fun watch(trees: Flow<CategoryTree>) {
        useCases
            .observeOffer()
            .filterNotNull()
            .onEach { offer ->
                if (offer.groupId != groupId) return@onEach
                useCases.consumeOffer(offer.expenseId)
                teachOf(
                    offer = offer,
                    tree = trees.first(),
                    map = useCases.observeMap().first(),
                    mapTree = useCases.observeMapCategories().first(),
                )?.let(write)
            }.launchIn(scope)
    }

    fun open() {
        val teach = read() ?: return
        write(teach.copy(isPickingWord = true))
    }

    fun dismiss() {
        if (read()?.isSaving == true) return
        write(null)
    }

    fun pick(key: String) {
        val teach = read() ?: return
        if (teach.words.none { it.key == key }) return
        write(teach.copy(selectedKey = key))
    }

    fun confirm() {
        val teach = read() ?: return
        val word = teach.selectedWord ?: return
        if (teach.isSaving) return
        write(teach.copy(isSaving = true))
        scope.launch {
            useCases
                .saveTerm(
                    term = word.term,
                    categoryId = teach.category.id,
                    replacedTerm = null,
                ).onSuccess { write(null) }
                .onFailure {
                    write(read()?.copy(isSaving = false))
                    showError()
                }
        }
    }

    private fun teachOf(
        offer: CategoryTeachOffer,
        tree: CategoryTree,
        map: CategoryMap,
        mapTree: CategoryTree,
    ): CategoryTeach? {
        val category = tree.findOrNull(offer.categoryId) ?: return null
        val mapCategory = offer.mapCategoryId?.let { tree.findOrNull(it) }
        val words = CategoryMatcher
            .titleWordsOf(offer.title)
            .map { word ->
                val key = CategoryMatcher.keyOf(word)
                TeachWord(
                    term = word.lowercase(),
                    key = key,
                    // Why: the whole map's, as Mapa de categorias shows it, so a word of another group's category moves.
                    currentCategory = map.findOrNull(key)?.let { mapTree.readAsOrNull(it.categoryId) },
                )
            }.filter { word ->
                (word.key.length >= MIN_WORD_LENGTH || word.currentCategory != null) &&
                    word.currentCategory?.id != category.id && CategoryMatcher.isTermValid(word.term)
            }.distinctBy { it.key }
        if (words.isEmpty()) return null
        return CategoryTeach(
            expenseId = offer.expenseId,
            title = offer.title,
            category = category,
            categoryParent = category.parentId?.let { tree.findOrNull(it) },
            mapCategory = mapCategory,
            mapCategoryParent = mapCategory?.parentId?.let { tree.findOrNull(it) },
            words = words,
            selectedKey = (words.firstOrNull { it.currentCategory == null } ?: words.first()).key,
            isPickingWord = false,
            isSaving = false,
        )
    }

    private companion object {
        // Why: "da" or "do" never become a term by themselves, unless the map already has them ("99").
        const val MIN_WORD_LENGTH = 3
    }
}
