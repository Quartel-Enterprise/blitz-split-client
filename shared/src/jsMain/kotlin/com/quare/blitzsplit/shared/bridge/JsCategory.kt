@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree

/*
 * Why: a category as the page draws it. appKey is an app category's key ("food.market"), which the page names in its
 * language, null for one of the group's, named by name; icon is the CategoryIcon's code ("cart"), color the main
 * category's avatar color code.
 */
class JsCategory(
    val id: String,
    val appKey: String?,
    val name: String?,
    val icon: String,
    val color: String,
    val parentId: String?,
    val isGroupCategory: Boolean,
    val isOther: Boolean,
    val createdByMemberId: String?,
    val isPending: Boolean,
)

// Why: a main category with its subcategories, as "Todas as categorias" lists them.
class JsCategoryBranch(
    val category: JsCategory,
    val subcategories: Array<JsCategory>,
)

internal fun toJsCategory(category: Category): JsCategory = JsCategory(
    id = category.id,
    appKey = category.app?.key,
    name = category.name,
    icon = category.icon.code,
    color = category.color.code,
    parentId = category.parentId,
    isGroupCategory = category.isGroupCategory,
    isOther = category.isOther,
    createdByMemberId = category.createdByMemberId,
    isPending = category.isPending,
)

internal fun toJsBranches(tree: CategoryTree): Array<JsCategoryBranch> = tree.branches
    .map { branch ->
        JsCategoryBranch(
            category = toJsCategory(branch.category),
            subcategories = branch.subcategories.map(::toJsCategory).toTypedArray(),
        )
    }.toTypedArray()

// Why: the main category above a subcategory, for "Alimentação › Mercado"; null for a main one.
internal fun parentOf(
    tree: CategoryTree,
    category: Category,
): JsCategory? = category.parentId?.let { tree.findOrNull(it) }?.let(::toJsCategory)
