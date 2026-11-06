package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.SimplifyDebtsConflictNotice
import com.quare.blitzsplit.core.groups.domain.model.GroupNameRules
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.PhotoCropEditor
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.toImageUrl

data class EditGroupUiState(
    val name: String,
    // Why: "VF" for "Viagem Floripa", what the group's avatar shows; empty while there is no name.
    val initials: String,
    val color: AvatarColor,
    // Why: drawn in place of the color and the initials; picking a color takes it off.
    val photo: ProfilePhoto?,
    val isPreparingPhoto: Boolean,
    // Why: the photo picked couldn't be read; the one shown stays.
    val showsPhotoError: Boolean,
    // Why: "Ícone do grupo", open on Android and iOS; null while closed.
    val iconEditor: GroupIconEditor?,
    // Why: "Ajustar foto" while a picked photo is being framed, drawn over the form in the group's rounded square.
    val photoCrop: PhotoCropEditor?,
    val type: GroupType,
    // Why: only the default of the group's new bills; each bill and payment keeps its own.
    val currency: GroupCurrency,
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
    // Why: "Simplificar dívidas" (13.1) as the group has it now; it applies at once, apart from Salvar.
    val simplifiesDebts: Boolean,
    // Why: changed on this device and not up yet (13.1d).
    val isSimplifyDebtsPending: Boolean,
    // Why: "Ligado · 5 pagamentos em vez de 7", one row per currency the group has bills or payments in; empty while off.
    val simplifications: List<CurrencySimplification>,
    // Why: with 2 people it is already one payment (13.1c).
    val memberCount: Int,
    // Why: the confirmation the switch opens (13.3, 13.3b); null while closed.
    val simplifyConfirmation: SimplifyDebtsConfirmation?,
    val isChangingSimplifyDebts: Boolean,
    // Why: "Ana desligou depois de você" (13.1e), until it is dismissed.
    val simplifyConflict: SimplifyDebtsConflictNotice?,
) {
    // Why: off and unavailable with fewer than 3 people, though a group already on can still turn it off.
    val canChangeSimplifyDebts: Boolean
        get() = isLoaded && !isChangingSimplifyDebts && (simplifiesDebts || memberCount >= MIN_PEOPLE_TO_SIMPLIFY)

    val simplifyStatus: SimplifyDebtsStatus
        get() = when {
            isSimplifyDebtsPending -> SimplifyDebtsStatus.PENDING
            simplifiesDebts -> SimplifyDebtsStatus.ON
            memberCount < MIN_PEOPLE_TO_SIMPLIFY -> SimplifyDebtsStatus.NEEDS_THIRD_PERSON
            else -> SimplifyDebtsStatus.OFF
        }

    /*
     * Why: "Sem internet: os pagamentos sugeridos já mudaram neste aparelho. O grupo vê quando ele reconectar." in
     * place of the switch's note (13.1d).
     */
    val showsSimplifyOfflineNotice: Boolean
        get() = !isOnline && isSimplifyDebtsPending

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

    companion object {
        // Why: two people are already one payment at most.
        const val MIN_PEOPLE_TO_SIMPLIFY = 3
    }
}
