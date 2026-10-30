package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.groups.domain.model.GroupNameRules
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType

data class EditGroupUiState(
    val name: String,
    // Why: "VF" for "Viagem Floripa", what the group's avatar shows; empty while there is no name.
    val initials: String,
    val color: AvatarColor,
    val type: GroupType,
    val currency: GroupCurrency,
    // Why: "Não muda depois da primeira despesa", once the group has a bill.
    val isCurrencyLocked: Boolean,
    val isOnline: Boolean,
    // Why: the form is filled from the group once Room is read; nothing is drawn before.
    val isLoaded: Boolean,
    val isSaving: Boolean,
    // Why: the device couldn't save the change; cleared by the next try.
    val showsSaveError: Boolean,
    // Why: Salvar stays off until something differs from what the form opened with, so a save always changes the group.
    val hasChanges: Boolean,
) {
    val types: List<GroupType>
        get() = GroupType.entries

    val currencies: List<GroupCurrency>
        get() = GroupCurrency.entries

    // Why: "14/40", in letters as the server counts them.
    val nameLength: Int
        get() = GroupNameRules.lengthOf(name)

    val maxNameLength: Int
        get() = GroupNameRules.MAX_LENGTH

    // Why: "Dê um nome ao grupo, com até 40 caracteres." in place of the field's help (6.4b).
    val showsNameError: Boolean
        get() = isLoaded && name.isBlank()

    val canSave: Boolean
        get() = isLoaded && hasChanges && name.isNotBlank() && !isSaving

    // Why: the change holds on this device now and reaches the others once it reconnects (6.4c).
    val showsOfflineNotice: Boolean
        get() = !isOnline
}
