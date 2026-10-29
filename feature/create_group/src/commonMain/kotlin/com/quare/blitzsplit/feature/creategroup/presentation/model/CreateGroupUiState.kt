package com.quare.blitzsplit.feature.creategroup.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType

data class CreateGroupUiState(
    val name: String,
    // Why: "VF" for "Viagem Floripa", what the group's avatar shows; empty while there is no name.
    val initials: String,
    val color: AvatarColor,
    val type: GroupType,
    val currency: GroupCurrency,
    // Why: this device's user first, then in the order they were added. Empty until the profile is read.
    val members: List<CreateGroupMember>,
    // Why: what is typed in "Adicionar pelo nome", not a member yet.
    val draft: String,
    // Why: "Já tem uma Ana no grupo", the member the draft repeats as the group spells it, null while it repeats none.
    val takenName: String?,
    // Why: "Usar “Ana P.”" and "Usar “Ana 2”", the free nicknames while the draft repeats a name.
    val nicknameSuggestions: List<String>,
    val isCreating: Boolean,
) {
    val types: List<GroupType>
        get() = GroupType.entries

    val currencies: List<GroupCurrency>
        get() = GroupCurrency.entries

    val canAddMember: Boolean
        get() = draft.isNotBlank() && takenName == null

    // Why: a draft typed and not added yet goes in with the group, so only a repeated one holds it back.
    val canCreate: Boolean
        get() = name.isNotBlank() && members.isNotEmpty() && takenName == null && !isCreating

    // Why: "Ninguém precisa ter o app agora", which the repeated name's warning takes the place of.
    val showsNoAppNotice: Boolean
        get() = takenName == null
}
