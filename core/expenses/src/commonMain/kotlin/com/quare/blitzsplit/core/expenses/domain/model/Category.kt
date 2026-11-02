package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.AvatarColor

/*
 * Why: a category as a bill, the picker and Estatísticas show it, the app's or one of the group's. UIs name an app
 * category from `app` and a group's from `name`.
 */
data class Category(
    // Why: what a bill stores: the app category's key or the group category's id.
    val id: String,
    // Why: one of the app's; null for one created in the group.
    val app: AppCategory?,
    // Why: the name someone gave a group category; null for the app's, which each UI names.
    val name: String?,
    val icon: CategoryIcon,
    // Why: its main category's color, the same in the pie and in its row.
    val color: AvatarColor,
    // Why: the main category of a subcategory; null for a main one.
    val parentId: String?,
    // Why: "Criada por Ana · do grupo"; null for the app's.
    val createdByMemberId: String?,
    // Why: created or changed on this device and not up on the server yet.
    val isPending: Boolean,
) {
    val isMain: Boolean
        get() = parentId == null

    // Why: only the group's categories can be edited or deleted.
    val isGroupCategory: Boolean
        get() = app == null

    val isOther: Boolean
        get() = app == AppCategory.OTHER
}
