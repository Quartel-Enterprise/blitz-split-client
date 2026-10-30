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
    // Why: the GroupType's code ("trip"); types lists them in the order the UIs draw them.
    val type: String,
    val types: Array<String>,
    // Why: the ISO 4217 code ("BRL"); each UI names it in its own language.
    val currency: String,
    val currencies: Array<String>,
    val members: Array<JsCreateGroupMember>,
    val draft: String,
    val takenName: String?,
    val nicknameSuggestions: Array<String>,
    val isCreating: Boolean,
    val showsCreateError: Boolean,
    val canAddMember: Boolean,
    val canCreate: Boolean,
    val showsNoAppNotice: Boolean,
)

class CreateGroupPresenter {
    private val holder = ViewModelHolder(CreateGroupViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsCreateGroupState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsCreateGroupState(
                name = state.name,
                initials = state.initials,
                color = state.color.code,
                type = state.type.code,
                types = state.types.toCodes(),
                currency = state.currency.code,
                currencies = state.currencies.toCurrencyCodes(),
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
                takenName = state.takenName,
                nicknameSuggestions = state.nicknameSuggestions.toTypedArray(),
                isCreating = state.isCreating,
                showsCreateError = state.showsCreateError,
                canAddMember = state.canAddMember,
                canCreate = state.canCreate,
                showsNoAppNotice = state.showsNoAppNotice,
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

    fun useSuggestion(name: String) {
        viewModel.onEvent(CreateGroupUiEvent.OnSuggestionClicked(name))
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
