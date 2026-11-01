package com.quare.blitzsplit.feature.contacts.presentation.model

// Why: the device user's groups, each one the person is already in shown disabled ("Já está no grupo").
data class AddToGroupDraft(
    val options: List<AddGroupOption>,
    val selectedGroupId: String?,
) {
    val canAdd: Boolean
        get() = options.any { it.groupId == selectedGroupId && !it.isAlreadyIn }
}
