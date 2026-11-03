package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.groups.domain.model.GroupNameRules
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.toImageUrl

data class EditGroupUiState(
    val name: String,
    // Why: "VF" for "Viagem Floripa", what the group's avatar shows; empty while there is no name.
    val initials: String,
    val color: AvatarColor,
    // Why: only Android draws it.
    val shape: GroupShape,
    // Why: drawn in place of the color and the initials; picking a color or a shape takes it off.
    val photo: ProfilePhoto?,
    val isPreparingPhoto: Boolean,
    // Why: the photo picked couldn't be read; the one shown stays.
    val showsPhotoError: Boolean,
    // Why: "Ícone do grupo", open on Android and iOS; null while closed.
    val iconEditor: GroupIconEditor?,
    val type: GroupType,
    val currency: GroupCurrency,
    // Why: "Não muda depois da primeira despesa", once the group has a bill.
    val isCurrencyLocked: Boolean,
    // Why: the "Divisão padrão" row's value ("Igual", "Partes"…), as the group has it now; saved from its own screen.
    val defaultSplitMode: DefaultSplitMode,
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

    // Why: the web's and the Mac's "Cor do ícone", picked in the form itself.
    val colors: List<AvatarColor>
        get() = AvatarColor.entries

    // Why: a picked photo is a data URI, which ProfilePhoto.Pending encodes once for every state copy.
    val photoUrl: String?
        get() = photo?.toImageUrl()

    // Why: the web's and the Mac's colors show none picked while the photo stands for the icon.
    val selectedColor: AvatarColor?
        get() = color.takeIf { photo == null }

    // Why: "14/40", in letters as the server counts them.
    val nameLength: Int
        get() = GroupNameRules.lengthOf(name)

    val maxNameLength: Int
        get() = GroupNameRules.MAX_LENGTH

    // Why: "Dê um nome ao grupo, com até 40 caracteres." in place of the field's help (6.4b).
    val showsNameError: Boolean
        get() = isLoaded && name.isBlank()

    val canSave: Boolean
        get() = isLoaded && hasChanges && name.isNotBlank() && !isSaving && !isPreparingPhoto

    // Why: the change holds on this device now and reaches the others once it reconnects (6.4c).
    val showsOfflineNotice: Boolean
        get() = !isOnline
}
