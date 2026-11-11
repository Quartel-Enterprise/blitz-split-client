package com.quare.blitzsplit.feature.creategroup.presentation.model

import com.quare.blitzsplit.core.groups.domain.model.MemberNameRules
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.PhotoCropEditor
import com.quare.blitzsplit.core.model.ProfilePhoto

data class CreateGroupUiState(
    val name: String,
    // Why: "VF" for "Viagem Floripa", what the group's avatar shows; empty while there is no name.
    val initials: String,
    val color: AvatarColor,
    // Why: drawn in place of the color and the initials, framed in "Ajustar foto"; null for none.
    val photo: ProfilePhoto.Pending?,
    val isPreparingPhoto: Boolean,
    // Why: the photo picked couldn't be read; the one shown stays.
    val showsPhotoError: Boolean,
    // Why: "Ajustar foto" while a picked photo is being framed, drawn over the form in the group's rounded square.
    val photoCrop: PhotoCropEditor?,
    val type: GroupType,
    val currency: GroupCurrency,
    // Why: this device's user first, then in the order they were added. Empty until the profile is read.
    val members: List<CreateGroupMember>,
    // Why: what is typed in "Adicionar pelo nome", not a member yet.
    val draft: String,
    val isCreating: Boolean,
    // Why: the device couldn't save the group; cleared by the next try.
    val showsCreateError: Boolean,
    // Why: the color and the currency the form opened with, what "unsaved input" is compared against.
    val openedColor: AvatarColor,
    val openedCurrency: GroupCurrency,
) {
    /*
     * Why: closing the form asks "Discard changes?" only when something differs from how it opened: a name, a photo,
     * a person added, a draft typed, another color, type or currency. The creator alone, as it opened, closes silently.
     */
    val hasUnsavedInput: Boolean
        get() = name.isNotBlank() ||
            photo != null ||
            isPreparingPhoto ||
            photoCrop != null ||
            draft.isNotBlank() ||
            members.any { !it.isMe } ||
            type != GroupType.default ||
            color != openedColor ||
            currency != openedCurrency

    val types: List<GroupType>
        get() = GroupType.entries

    val currencies: List<GroupCurrency>
        get() = GroupCurrency.entries

    // Why: the picked photo's data URI, which ProfilePhoto.Pending encodes once for every state copy.
    val photoUrl: String?
        get() = photo?.dataUri

    val hasPhoto: Boolean
        get() = photo != null

    // Why: the avatar steps its color only while no photo covers it.
    val canChangeColor: Boolean
        get() = photo == null && !isPreparingPhoto

    val canAddMember: Boolean
        get() = MemberNameRules.isLongEnough(draft)

    // Why: a draft typed and not added yet goes in with the group, so a single letter one holds it back.
    val canCreate: Boolean
        get() = name.isNotBlank() &&
            members.isNotEmpty() &&
            (draft.isBlank() || MemberNameRules.isLongEnough(draft)) &&
            !isPreparingPhoto &&
            photoCrop == null &&
            !isCreating
}
