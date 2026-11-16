package com.quare.blitzsplit.feature.creategroup.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

// Why: someone from Contacts whose name matches what is typed in "Add by name", drawn under the field.
data class CreateGroupSuggestion(
    // Why: Contacts' person key (PeopleBalances.keyOf), what OnSuggestionClicked hands back.
    val key: String,
    val name: String,
    val initials: String,
    val avatarColor: AvatarColor,
    // Why: the profile photo over the initials, null for someone with none or a guest.
    val photoUrl: String?,
)
