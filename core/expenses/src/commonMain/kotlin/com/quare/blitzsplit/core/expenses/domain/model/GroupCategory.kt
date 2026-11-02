package com.quare.blitzsplit.core.expenses.domain.model

// Why: a category someone created in a group (7.8), as Room holds it, a deleted one too.
data class GroupCategory(
    val id: String,
    val groupId: String,
    // Why: null for a main category; the main category it belongs to otherwise, the app's key or a group category's id.
    val parentId: String?,
    val name: String,
    // Why: a main category's icon; null for a subcategory, which takes its parent's.
    val icon: CategoryIcon?,
    // Why: "Criada por Ana".
    val createdByMemberId: String?,
    val isDeleted: Boolean,
    // Why: created or changed on this device and not up on the server yet.
    val isPending: Boolean,
)
