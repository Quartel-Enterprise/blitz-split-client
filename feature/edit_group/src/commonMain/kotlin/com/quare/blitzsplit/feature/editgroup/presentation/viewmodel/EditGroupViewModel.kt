package com.quare.blitzsplit.feature.editgroup.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.DebtSimplificationSummary
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SimplifyDebtsConflicts
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveSettlements
import com.quare.blitzsplit.core.expenses.domain.usecase.SetSimplifiesDebts
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupNameRules
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.usecase.ConsumeSimplifyDebtsConflict
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.UpdateGroup
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.openActivity
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.LoadCropPhoto
import com.quare.blitzsplit.core.profile.domain.usecase.PrepareProfilePhoto
import com.quare.blitzsplit.core.profile.photo.ProfilePhotoPicker
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiState
import com.quare.blitzsplit.feature.editgroup.presentation.model.GroupIconEditor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class EditGroupViewModel(
    private val route: Route.EditGroup,
    private val navigator: Navigator,
    private val updateGroup: UpdateGroup,
    private val setSimplifiesDebts: SetSimplifiesDebts,
    private val consumeSimplifyDebtsConflict: ConsumeSimplifyDebtsConflict,
    observeGroup: ObserveGroup,
    observeExpenses: ObserveExpenses,
    observeSettlements: ObserveSettlements,
    connectivityObserver: NetworkConnectivityObserver,
    loadCropPhoto: LoadCropPhoto,
    prepareProfilePhoto: PrepareProfilePhoto,
    trackEvent: TrackEvent,
) : TrackedViewModel<EditGroupUiEvent>(trackEvent) {
    val uiState: StateFlow<EditGroupUiState>
        field = MutableStateFlow(
            EditGroupUiState(
                name = "",
                initials = "",
                color = AvatarColor.default,
                photo = null,
                isPreparingPhoto = false,
                showsPhotoError = false,
                iconEditor = null,
                photoCrop = null,
                type = GroupType.default,
                currency = GroupCurrency.fallback,
                defaultSplitMode = DefaultSplitMode.EQUAL,
                isOnline = true,
                isLoaded = false,
                isSaving = false,
                showsSaveError = false,
                hasChanges = false,
                simplifiesDebts = false,
                isSimplifyDebtsPending = false,
                simplification = null,
                memberCount = 0,
                simplifyConfirmation = null,
                isChangingSimplifyDebts = false,
                simplifyConflict = null,
            ),
        )

    val stateWatcher: FlowWatcher<EditGroupUiState> = FlowWatcher(uiState)

    // Why: the group went while another screen covered this one, which then leaves as soon as it is on top again.
    private var isGroupGone = false

    // Why: what the form opened with, so saving tells what this person changed from what others changed since.
    private var original: Group? = null

    // Why: the group as Room has it now, a sync from another member included.
    private var latest: Group? = null

    /*
     * Why: the bills and payments on the device, which "Simplificar dívidas" reads. Its line and confirmation show the
     * group's own currency, the one the form's amounts are written in; the switch itself covers every currency, each on
     * its own (SetSimplifiesDebtsUseCase).
     */
    private var expenses: List<Expense>? = null
    private var settlements: List<Settlement> = emptyList()

    // Why: the conflict shows once, not again from an emission that came before its consume landed.
    private var isConsumingConflict = false

    // Why: the group's photo is the same 512x512 JPEG as a profile's.
    private val photoPicker = ProfilePhotoPicker(
        loadCropPhoto = loadCropPhoto,
        prepareProfilePhoto = prepareProfilePhoto,
        scope = viewModelScope,
        showStatus = ::showPhotoStatus,
        showEditor = { editor -> uiState.update { it.copy(photoCrop = editor) } },
        onPrepared = ::showPhoto,
        // Why: Salvar sends the photo shown.
        isBusy = { uiState.value.isSaving },
    )

    /*
     * Why: the form is filled from the group once, so a sync landing while someone types changes nothing here. The
     * currency is only the default of new bills, so it is always editable and no bill or payment changes with it.
     */
    init {
        observeGroup(route.groupId)
            .onEach { group ->
                isGroupGone = group == null
                if (group == null) leave() else show(group)
            }.launchIn(viewModelScope)
        navigator.backStack
            .onEach {
                if (isGroupGone) leave() else latest?.let(::showConflict)
            }.launchIn(viewModelScope)
        /*
         * Why: the switch's line and its open confirmation follow the pairs as bills, payments and syncs change them,
         * so what it says is always what confirming applies.
         */
        combine(
            observeExpenses(route.groupId),
            observeSettlements(route.groupId),
            ::Pair,
        ).onEach { (expenses, settlements) ->
            this.expenses = expenses
            this.settlements = settlements
            latest?.let(::showSimplification)
        }.launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: EditGroupUiEvent) {
        when (event) {
            // Why: leaving clears the ViewModel, which would cancel a save in flight.
            EditGroupUiEvent.OnCloseClicked -> if (!uiState.value.isSaving) leave()

            is EditGroupUiEvent.OnNameChanged -> changeName(event.name)

            EditGroupUiEvent.OnIconClicked -> openIconEditor()

            EditGroupUiEvent.OnIconDismissed -> uiState.update { it.copy(iconEditor = null) }

            is EditGroupUiEvent.OnIconColorPicked -> if (uiState.value.iconEditor != null) {
                trackEvent("group_icon_color_picked", mapOf("color" to event.color))
                uiState.update { it.copy(iconEditor = it.iconEditor?.copy(color = event.color)) }
            }

            EditGroupUiEvent.OnIconConfirmed -> confirmIcon()

            is EditGroupUiEvent.OnColorSelected -> useIcon(event.color)

            is EditGroupUiEvent.OnPhotoPicked -> preparePhoto(event.source)

            is EditGroupUiEvent.OnPhotoCrop -> photoPicker.onEvent(event.event)

            EditGroupUiEvent.OnPhotoUnreadable -> {
                uiState.update { it.copy(iconEditor = null) }
                photoPicker.fail()
            }

            is EditGroupUiEvent.OnTypeSelected -> updateForm { it.copy(type = event.type) }

            is EditGroupUiEvent.OnCurrencySelected -> selectCurrency(event.currency)

            EditGroupUiEvent.OnDefaultSplitClicked -> openDefaultSplit()

            EditGroupUiEvent.OnSaveClicked -> save()

            EditGroupUiEvent.OnSimplifyDebtsClicked -> openSimplifyConfirmation()

            EditGroupUiEvent.OnSimplifyDebtsConfirmed -> confirmSimplifyDebts()

            EditGroupUiEvent.OnSimplifyDebtsDismissed -> dismissSimplifyDebts()

            EditGroupUiEvent.OnSimplifyDebtsInfoClicked -> openSimplifyDebtsInfo()

            EditGroupUiEvent.OnSimplifyConflictDismissed -> uiState.update { it.copy(simplifyConflict = null) }

            EditGroupUiEvent.OnSimplifyConflictActivityClicked -> openActivity()
        }
    }

    private fun show(group: Group) {
        latest = group
        if (original == null) original = group
        updateForm { state ->
            val filled = if (state.isLoaded) {
                state
            } else {
                state.copy(
                    name = group.name,
                    initials = Initials.of(group.name),
                    color = group.color,
                    photo = group.photo,
                    type = group.type,
                    currency = group.currency,
                    isLoaded = true,
                )
            }
            filled.copy(
                defaultSplitMode = group.defaultSplit?.mode ?: DefaultSplitMode.EQUAL,
            )
        }
        showSimplification(group)
        showConflict(group)
    }

    // Why: the switch is the group's, as Room has it, a sync's change included; it is no change of the form's.
    private fun showSimplification(group: Group) {
        val raw = rawOf(group)
        val shown = raw?.takeIf { group.simplifiesDebts }?.toSimplified(group.members.map { it.id })
        uiState.update { state ->
            state.copy(
                simplifiesDebts = group.simplifiesDebts,
                isSimplifyDebtsPending = group.isSimplifyDebtsPending,
                simplification = shown?.let(DebtSimplificationSummary::ofOrNull),
                memberCount = group.members.size,
                // Why: a sync turning it the other way meanwhile closes the question, which no longer asks the same.
                simplifyConfirmation = state.simplifyConfirmation
                    ?.takeIf { it.isTurningOn != group.simplifiesDebts && raw != null }
                    ?.let { simplifyConfirmationOf(group, checkNotNull(raw), settlements) },
            )
        }
    }

    // Why: 13.1e shows on whichever screen of the group is on top first, once.
    private fun showConflict(group: Group) {
        if (navigator.backStack.value.lastOrNull() != route) return
        if (isConsumingConflict) return
        val notice = SimplifyDebtsConflicts.noticeOrNull(group) ?: return
        isConsumingConflict = true
        uiState.update { it.copy(simplifyConflict = notice) }
        viewModelScope.launch {
            consumeSimplifyDebtsConflict(route.groupId)
            isConsumingConflict = false
        }
    }

    // Why: the pairs as each bill makes them in the group's currency, never with another's added.
    private fun rawOf(group: Group): GroupBalances? =
        expenses?.let { GroupBalances.createUnsimplified(it, settlements, group.currency) }

    private fun openSimplifyConfirmation() {
        val state = uiState.value
        val group = latest ?: return
        val raw = rawOf(group) ?: return
        if (!state.canChangeSimplifyDebts) return
        uiState.update { it.copy(simplifyConfirmation = simplifyConfirmationOf(group, raw, settlements)) }
    }

    private fun selectCurrency(currency: GroupCurrency) {
        trackEvent("group_currency_selected", mapOf("currency" to currency, "source" to "edit_group"))
        updateForm { it.copy(currency = currency) }
    }

    // Why: counted only while the question still asks, but it closes in any case, as it always did.
    private fun dismissSimplifyDebts() {
        val state = uiState.value
        if (state.simplifyConfirmation != null && !state.isChangingSimplifyDebts) {
            trackEvent("simplify_debts_cancelled", emptyMap())
        }
        uiState.update { it.copy(simplifyConfirmation = null) }
    }

    private fun confirmSimplifyDebts() {
        val confirmation = uiState.value.simplifyConfirmation ?: return
        if (uiState.value.isChangingSimplifyDebts) return
        trackEvent("simplify_debts_toggled", mapOf("is_on" to confirmation.isTurningOn))
        uiState.update {
            it.copy(
                isChangingSimplifyDebts = true,
                showsSaveError = false,
            )
        }
        viewModelScope.launch {
            val result = setSimplifiesDebts(
                groupId = route.groupId,
                simplifiesDebts = confirmation.isTurningOn,
            )
            uiState.update {
                it.copy(
                    isChangingSimplifyDebts = false,
                    simplifyConfirmation = null,
                    showsSaveError = result.isFailure,
                )
            }
        }
    }

    // Why: only from the form on top, as "Divisão padrão".
    private fun openSimplifyDebtsInfo() {
        val state = uiState.value
        if (!state.isLoaded || navigator.backStack.value.lastOrNull() != route) return
        navigator.navigate(Route.SimplifyDebtsInfo(state.currency))
    }

    /*
     * Why: Atividade lists both changes, on top of its Hoje; the form, its group and whatever else is above Início
     * close first, so no platform draws Atividade inside a sheet.
     */
    private fun openActivity() {
        uiState.update { it.copy(simplifyConflict = null) }
        if (navigator.backStack.value.lastOrNull() != route) return
        navigator.openActivity()
    }

    private fun openIconEditor() {
        val state = uiState.value
        if (!state.isLoaded || state.isSaving) return
        uiState.update {
            it.copy(
                iconEditor = GroupIconEditor(color = it.color),
            )
        }
    }

    private fun confirmIcon() {
        val editor = uiState.value.iconEditor ?: return
        useIcon(editor.color)
    }

    // Why: the icon stands in place of the photo, so picking one takes the photo off, and a pick still prepared too.
    private fun useIcon(color: AvatarColor) {
        if (uiState.value.isSaving) return
        photoPicker.cancel()
        updateForm {
            it.copy(
                color = color,
                photo = null,
                isPreparingPhoto = false,
                showsPhotoError = false,
                iconEditor = null,
            )
        }
    }

    private fun preparePhoto(source: ByteArray) {
        uiState.update { it.copy(iconEditor = null) }
        photoPicker.prepare(source)
    }

    private fun showPhotoStatus(
        isPreparing: Boolean,
        hasFailed: Boolean,
    ) {
        updateForm {
            it.copy(
                isPreparingPhoto = isPreparing,
                showsPhotoError = hasFailed,
            )
        }
    }

    private fun showPhoto(photo: ProfilePhoto) {
        updateForm {
            it.copy(
                photo = photo,
                isPreparingPhoto = false,
                showsPhotoError = false,
            )
        }
    }

    // Why: only from the form on top, so a late second tap never stacks the split twice.
    private fun openDefaultSplit() {
        val state = uiState.value
        if (!state.isLoaded || state.isSaving || navigator.backStack.value.lastOrNull() != route) return
        navigator.navigate(Route.DefaultSplit(route.groupId))
    }

    private fun changeName(name: String) {
        val capped = GroupNameRules.toCapped(name)
        updateForm {
            it.copy(
                name = capped,
                initials = Initials.of(capped),
            )
        }
    }

    private fun save() {
        val state = uiState.value
        val original = original ?: return
        val latest = latest ?: return
        if (!state.canSave) return
        uiState.update {
            it.copy(
                isSaving = true,
                showsSaveError = false,
            )
        }
        // Why: a field this person left as it opened keeps what another member changed it to meanwhile.
        val name = if (state.name.trim() != original.name) state.name else latest.name
        val type = if (state.type != original.type) state.type else latest.type
        val color = if (state.color != original.color) state.color else latest.color
        val photo = if (state.photo != original.photo) state.photo else latest.photo
        val currency = if (state.currency != original.currency) {
            state.currency
        } else {
            latest.currency
        }
        viewModelScope.launch {
            updateGroup(
                groupId = route.groupId,
                name = name,
                type = type,
                color = color,
                // Why: every group draws the same rounded square now, so the shape a group has stays as Room has it.
                shape = null,
                photo = photo,
                currency = currency,
            ).onSuccess {
                // Why: the form may not be on top to leave, so it stops saving and starts from what it just saved.
                this@EditGroupViewModel.original = latest.copy(
                    name = name.trim(),
                    type = type,
                    color = color,
                    photo = photo,
                    currency = currency,
                )
                updateForm { it.copy(isSaving = false) }
                leave()
            }.onFailure {
                uiState.update {
                    it.copy(
                        isSaving = false,
                        showsSaveError = true,
                    )
                }
            }
        }
    }

    // Why: every change to the form says whether it still differs from what it opened with, which enables Salvar.
    private fun updateForm(transform: (EditGroupUiState) -> EditGroupUiState) {
        uiState.update { state ->
            val next = transform(state)
            next.copy(hasChanges = next.isChangedFrom(original))
        }
    }

    private fun EditGroupUiState.isChangedFrom(group: Group?): Boolean = group != null &&
        (
            name.trim() != group.name ||
                type != group.type ||
                color != group.color ||
                photo != group.photo ||
                currency != group.currency
        )

    // Why: only while it is still on top: a native back (a swipe, the browser's, a sheet dismissed) may have closed it.
    private fun leave() {
        if (navigator.backStack.value.lastOrNull() == route) navigator.navigateBack()
    }
}
