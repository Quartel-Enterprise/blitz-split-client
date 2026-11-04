@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.CategoryDeletion
import com.quare.blitzsplit.core.expenses.domain.model.CategoryEditor

/*
 * Why: "Nova categoria" or "Editar categoria" (7.8), as a bill's Categoria and the map (9.4) both open it; icon is the
 * CategoryIcon's code, color the avatar color code of its preview.
 */
class JsCategoryForm(
    val categoryId: String?,
    val groupId: String,
    val groupName: String,
    val name: String,
    val parentId: String?,
    val parent: JsCategory?,
    val icon: String,
    val color: String,
    // Why: "Criada por Ana", for one being edited.
    val creatorName: String?,
    val isSaving: Boolean,
    val isEditing: Boolean,
    val isSubcategory: Boolean,
    val isNameTaken: Boolean,
    val isParentLocked: Boolean,
    val parents: Array<JsCategory>,
    // Why: the group's bills with it or one of its subcategories, "usada em 3 contas"; null until they are read.
    val usage: Int?,
    val canSave: Boolean,
    // Why: "Apagar categoria" waits for the bills it would move.
    val canDelete: Boolean,
    val showsOfflineNotice: Boolean,
)

class JsCategoryDeletion(
    val category: JsCategory,
    val billCount: Int,
    val movesTo: JsCategory,
    val isDeleting: Boolean,
)

internal fun toJsCategoryForm(editor: CategoryEditor): JsCategoryForm {
    val form = editor.form
    return JsCategoryForm(
        categoryId = form.categoryId,
        groupId = form.groupId,
        groupName = editor.groupName,
        name = form.name,
        parentId = form.parentId,
        parent = editor.parent?.let(::toJsCategory),
        icon = form.icon.code,
        color = editor.color.code,
        creatorName = editor.creatorName,
        isSaving = form.isSaving,
        isEditing = form.isEditing,
        isSubcategory = form.isSubcategory,
        isNameTaken = editor.isNameTaken,
        isParentLocked = editor.isParentLocked,
        parents = editor.parents.map(::toJsCategory).toTypedArray(),
        usage = editor.usage,
        canSave = editor.canSave,
        canDelete = editor.canDelete,
        showsOfflineNotice = editor.showsOfflineNotice,
    )
}

internal fun toJsCategoryDeletion(deletion: CategoryDeletion): JsCategoryDeletion = JsCategoryDeletion(
    category = toJsCategory(deletion.category),
    billCount = deletion.billCount,
    movesTo = toJsCategory(deletion.movesTo),
    isDeleting = deletion.isDeleting,
)
