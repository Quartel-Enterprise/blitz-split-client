package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.utils.TextFolding

/*
 * Why: a group's categories in two levels, the app's fixed ones with the group's among them (7.7): the group's main
 * categories before "Outros", each main category's subcategories the app's first. A deleted group category stays
 * known, so its bills read as its parent, or as "Outros" for a main one (7.8f), on every device.
 */
class CategoryTree private constructor(
    val branches: List<CategoryBranch>,
    private val groupCategories: Map<String, GroupCategory>,
) {
    // Why: every category one can pick, main ones and subcategories, in the order they are listed.
    val all: List<Category> = branches.flatMap { listOf(it.category) + it.subcategories }

    private val live: Map<String, Category> = all.associateBy { it.id }

    val other: Category
        get() = live.getValue(AppCategory.OTHER.key)

    // Why: a live category, null for one deleted or unknown.
    fun findOrNull(id: String?): Category? = id?.let(live::get)

    // Why: what a bill's category reads as: itself while it lives, its parent once deleted, "Outros" when none is left.
    fun resolve(id: String?): Category {
        var current = id
        repeat(MAX_DEPTH) {
            val key = current ?: return other
            live[key]?.let { return it }
            current = groupCategories[key]?.parentId
        }
        return other
    }

    // Why: the main category a category belongs to, itself for a main one.
    fun mainOf(category: Category): Category = category.parentId?.let(::resolve)?.takeIf { it.isMain } ?: category

    fun branchOf(mainId: String): CategoryBranch? = branches.firstOrNull { it.category.id == mainId }

    // Why: the group a category was created in, null for the app's; "só no Viagem Floripa" in the map (9.1).
    fun groupIdOf(categoryId: String): String? = groupCategories[categoryId]?.groupId

    // Why: "Não pode repetir dentro da mesma categoria" (7.8c), ignoring case and accents, among the group's.
    fun hasGroupName(
        parentId: String?,
        name: String,
        exceptId: String?,
    ): Boolean {
        val folded = TextFolding.toFolded(name.trim())
        return all.any { category ->
            category.isGroupCategory && category.id != exceptId && category.parentId == parentId &&
                category.name?.let { TextFolding.toFolded(it) } == folded
        }
    }

    // Why: the subcategories a group category has, which it can't become a subcategory with (two levels only).
    fun hasSubcategories(id: String): Boolean = branchOf(id)?.subcategories?.isNotEmpty() == true

    companion object {
        // Why: two levels, so a deleted subcategory reaches a live category in at most two steps.
        private const val MAX_DEPTH = 3

        fun of(groupCategories: List<GroupCategory>): CategoryTree {
            val liveGroup = groupCategories.filterNot { it.isDeleted }
            val groupMains = liveGroup.filter { it.parentId == null }
            // Why: a group main category's color goes by its place among all of them, so deleting one moves none.
            val colorIndexes = groupCategories
                .filter { it.parentId == null }
                .mapIndexed { index, main -> main.id to index }
                .toMap()
            val appBranches = AppCategory.mains.map { main ->
                val category = appCategoryOf(main)
                CategoryBranch(
                    category = category,
                    subcategories = AppCategory.entries.filter { it.parentKey == main.key }.map(::appCategoryOf) +
                        groupSubcategoriesOf(liveGroup, category),
                )
            }
            val groupBranches = groupMains.map { main ->
                val category = groupCategoryOf(
                    category = main,
                    icon = main.icon ?: CategoryIcon.DOTS,
                    color = AvatarColor.entries[colorIndexes.getValue(main.id) % AvatarColor.entries.size],
                )
                CategoryBranch(
                    category = category,
                    subcategories = groupSubcategoriesOf(liveGroup, category),
                )
            }
            val other = appBranches.last()
            return CategoryTree(
                branches = appBranches.dropLast(1) + groupBranches + other,
                groupCategories = groupCategories.associateBy { it.id },
            )
        }

        // Why: the app's alone, for a bill whose group has no category of its own; built once, as it never changes.
        val appOnly: CategoryTree by lazy { of(emptyList()) }

        private fun groupSubcategoriesOf(
            groupCategories: List<GroupCategory>,
            parent: Category,
        ): List<Category> = groupCategories
            .filter { it.parentId == parent.id }
            .map {
                groupCategoryOf(
                    category = it,
                    icon = parent.icon,
                    color = parent.color,
                )
            }

        private fun appCategoryOf(category: AppCategory): Category = Category(
            id = category.key,
            app = category,
            name = null,
            icon = category.icon,
            color = category.color,
            parentId = category.parentKey,
            createdByMemberId = null,
            isPending = false,
        )

        private fun groupCategoryOf(
            category: GroupCategory,
            icon: CategoryIcon,
            color: AvatarColor,
        ): Category = Category(
            id = category.id,
            app = null,
            name = category.name,
            icon = icon,
            color = color,
            parentId = category.parentId,
            createdByMemberId = category.createdByMemberId,
            isPending = category.isPending,
        )
    }
}
