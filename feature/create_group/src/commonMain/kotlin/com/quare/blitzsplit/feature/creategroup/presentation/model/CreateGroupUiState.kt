package com.quare.blitzsplit.feature.creategroup.presentation.model

import com.quare.blitzsplit.core.groups.domain.model.MemberNameRules
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.PhotoCropEditor
import com.quare.blitzsplit.core.model.ProfilePhoto

data class CreateGroupUiState(
    val name: String,
    // Why: "VF" for "Viagem Floripa", what the group's avatar shows; empty while there is no name.
    val initials: String,
    val color: AvatarColor,
    // Why: only Android draws it, so only its avatar changes it.
    val shape: GroupShape,
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
) {
    val types: List<GroupType>
        get() = GroupType.entries

    val currencies: List<GroupCurrency>
        get() = GroupCurrency.entries

    // Why: the picked photo's data URI, which ProfilePhoto.Pending encodes once for every state copy.
    val photoUrl: String?
        get() = photo?.dataUri

    val hasPhoto: Boolean
        get() = photo != null

    // Why: the avatar steps its color (and Android's shape too) only while no photo covers them.
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
