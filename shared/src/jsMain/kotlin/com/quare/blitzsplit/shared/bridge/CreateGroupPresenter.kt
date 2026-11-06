@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupUiEvent
import com.quare.blitzsplit.feature.creategroup.presentation.viewmodel.CreateGroupViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

class JsCreateGroupMember(
    val id: String,
    val name: String,
    val initials: String,
    val avatarColor: String,
    val isMe: Boolean,
)

class JsCreateGroupState(
    val name: String,
    val initials: String,
    val color: String,
    // Why: drawn in place of the color and the initials, null for none.
    val photoUrl: String?,
    val hasPhoto: Boolean,
    val isPreparingPhoto: Boolean,
    val showsPhotoError: Boolean,
    // Why: the avatar steps its color only while no photo covers it.
    val canChangeColor: Boolean,
    // Why: "Ajustar foto" while a picked photo is being framed, null while closed.
    val photoCrop: JsPhotoCropState?,
    // Why: the GroupType's code ("trip"); types lists them in the order the UIs draw them.
    val type: String,
    val types: Array<String>,
    // Why: the ISO 4217 code ("BRL"); each UI names it in its own language.
    val currency: String,
    val currencies: Array<String>,
    // Why: the ones among currencies "Moeda" lists under "Criptomoedas".
    val cryptoCurrencies: Array<String>,
    val members: Array<JsCreateGroupMember>,
    val draft: String,
    val isCreating: Boolean,
    val showsCreateError: Boolean,
    val canAddMember: Boolean,
    val canCreate: Boolean,
)

class CreateGroupPresenter {
    private val holder = ViewModelHolder(CreateGroupViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    val crop: PhotoCropPresenter =
        PhotoCropPresenter { event -> viewModel.onEvent(CreateGroupUiEvent.OnPhotoCrop(event)) }

    fun observeState(onChange: (JsCreateGroupState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsCreateGroupState(
                name = state.name,
                initials = state.initials,
                color = state.color.code,
                photoUrl = state.photoUrl,
                hasPhoto = state.hasPhoto,
                isPreparingPhoto = state.isPreparingPhoto,
                showsPhotoError = state.showsPhotoError,
                canChangeColor = state.canChangeColor,
                photoCrop = state.photoCrop?.toJs(),
                type = state.type.code,
                types = state.types.toCodes(),
                currency = state.currency.code,
                currencies = state.currencies.toCurrencyCodes(),
                cryptoCurrencies = state.currencies.toCryptoCurrencyCodes(),
                members = state.members
                    .map { member ->
                        JsCreateGroupMember(
                            id = member.id,
                            name = member.name,
                            initials = member.initials,
                            avatarColor = member.avatarColor.code,
                            isMe = member.isMe,
                        )
                    }.toTypedArray(),
                draft = state.draft,
                isCreating = state.isCreating,
                showsCreateError = state.showsCreateError,
                canAddMember = state.canAddMember,
                canCreate = state.canCreate,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: the ViewModel caps the name and the draft as it handles the event, so the fields read them back at once.
    fun getName(): String = viewModel.uiState.value.name

    fun getDraft(): String = viewModel.uiState.value.draft

    fun close() {
        viewModel.onEvent(CreateGroupUiEvent.OnCloseClicked)
    }

    fun changeName(name: String) {
        viewModel.onEvent(CreateGroupUiEvent.OnNameChanged(name))
    }

    fun changeColor() {
        viewModel.onEvent(CreateGroupUiEvent.OnColorClicked)
    }

    // Why: a Kotlin ByteArray is an Int8Array in JS, so the file's bytes cross without a copy.
    fun photoPicked(source: ByteArray) {
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(source))
    }

    fun photoUnreadable() {
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoUnreadable)
    }

    fun removePhoto() {
        viewModel.onEvent(CreateGroupUiEvent.OnPhotoRemoved)
    }

    fun selectType(code: String) {
        val type = GroupType.fromCodeOrNull(code) ?: return
        viewModel.onEvent(CreateGroupUiEvent.OnTypeSelected(type))
    }

    fun selectCurrency(code: String) {
        val currency = GroupCurrency.fromCodeOrNull(code) ?: return
        viewModel.onEvent(CreateGroupUiEvent.OnCurrencySelected(currency))
    }

    fun changeDraft(draft: String) {
        viewModel.onEvent(CreateGroupUiEvent.OnDraftChanged(draft))
    }

    fun addMember() {
        viewModel.onEvent(CreateGroupUiEvent.OnAddMemberClicked)
    }

    fun removeMember(memberId: String) {
        viewModel.onEvent(CreateGroupUiEvent.OnRemoveMemberClicked(memberId))
    }

    fun create() {
        viewModel.onEvent(CreateGroupUiEvent.OnCreateClicked)
    }

    fun dispose() {
        holder.clear()
    }
}
