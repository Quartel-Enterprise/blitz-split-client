package com.quare.blitzsplit.feature.editgroup.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupNameRules
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.UpdateGroup
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.LoadCropPhoto
import com.quare.blitzsplit.core.profile.domain.usecase.PrepareProfilePhoto
import com.quare.blitzsplit.core.profile.photo.ProfilePhotoPicker
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiState
import com.quare.blitzsplit.feature.editgroup.presentation.model.GroupIconEditor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class EditGroupViewModel(
    private val route: Route.EditGroup,
    private val navigator: Navigator,
    private val updateGroup: UpdateGroup,
    observeGroup: ObserveGroup,
    observeExpenses: ObserveExpenses,
    connectivityObserver: NetworkConnectivityObserver,
    loadCropPhoto: LoadCropPhoto,
    prepareProfilePhoto: PrepareProfilePhoto,
) : ViewModel() {
    val uiState: StateFlow<EditGroupUiState>
        field = MutableStateFlow(
            EditGroupUiState(
                name = "",
                initials = "",
                color = AvatarColor.default,
                shape = GroupShape.fallback,
                photo = null,
                isPreparingPhoto = false,
                showsPhotoError = false,
                iconEditor = null,
                photoCrop = null,
                type = GroupType.default,
                currency = GroupCurrency.fallback,
                isCurrencyLocked = false,
                defaultSplitMode = DefaultSplitMode.EQUAL,
                isOnline = true,
                isLoaded = false,
                isSaving = false,
                showsSaveError = false,
                hasChanges = false,
            ),
        )

    val stateWatcher: FlowWatcher<EditGroupUiState> = FlowWatcher(uiState)

    // Why: the group went while another screen covered this one, which then leaves as soon as it is on top again.
    private var isGroupGone = false

    // Why: what the form opened with, so saving tells what this person changed from what others changed since.
    private var original: Group? = null

    // Why: the group as Room has it now, a sync from another member included.
    private var latest: Group? = null

    // Why: the group's photo is the same 512x512 JPEG as a profile's.
    private val photoPicker = ProfilePhotoPicker(
        loadCropPhoto = loadCropPhoto,
        prepareProfilePhoto = prepareProfilePhoto,
        scope = viewModelScope,
        showStatus = ::showPhotoStatus,
        showEditor = { editor -> uiState.update { it.copy(photoCrop = editor) } },
        onPrepared = ::showPhoto,
    )

    /*
     * Why: the form is filled from the group once, so a sync landing while someone types changes nothing here. Its
     * currency still follows the group's once a bill locks it, since the bill is in that currency.
     */
    init {
        combine(
            observeGroup(route.groupId),
            observeExpenses(route.groupId)
                .map { it.isNotEmpty() }
                .distinctUntilChanged(),
            ::Pair,
        ).onEach { (group, hasExpenses) ->
            isGroupGone = group == null
            if (group == null) leave() else show(group, hasExpenses)
        }.launchIn(viewModelScope)
        navigator.backStack
            .onEach { if (isGroupGone) leave() }
            .launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: EditGroupUiEvent) {
        when (event) {
            // Why: leaving clears the ViewModel, which would cancel a save in flight.
            EditGroupUiEvent.OnCloseClicked -> if (!uiState.value.isSaving) leave()

            is EditGroupUiEvent.OnNameChanged -> changeName(event.name)

            EditGroupUiEvent.OnIconClicked -> openIconEditor()

            EditGroupUiEvent.OnIconDismissed -> uiState.update { it.copy(iconEditor = null) }

            is EditGroupUiEvent.OnIconColorPicked -> uiState.update {
                it.copy(iconEditor = it.iconEditor?.copy(color = event.color))
            }

            is EditGroupUiEvent.OnIconShapePicked -> uiState.update {
                it.copy(iconEditor = it.iconEditor?.copy(shape = event.shape))
            }

            EditGroupUiEvent.OnIconConfirmed -> confirmIcon()

            is EditGroupUiEvent.OnColorSelected -> useIcon(
                color = event.color,
                shape = uiState.value.shape,
            )

            is EditGroupUiEvent.OnPhotoPicked -> preparePhoto(event.source)

            is EditGroupUiEvent.OnPhotoCrop -> cropPhoto(event.event)

            EditGroupUiEvent.OnPhotoUnreadable -> {
                uiState.update { it.copy(iconEditor = null) }
                photoPicker.fail()
            }

            is EditGroupUiEvent.OnTypeSelected -> updateForm { it.copy(type = event.type) }

            is EditGroupUiEvent.OnCurrencySelected -> updateForm {
                if (it.isCurrencyLocked) it else it.copy(currency = event.currency)
            }

            EditGroupUiEvent.OnDefaultSplitClicked -> openDefaultSplit()

            EditGroupUiEvent.OnSaveClicked -> save()
        }
    }

    private fun show(
        group: Group,
        hasExpenses: Boolean,
    ) {
        latest = group
        if (original == null) original = group
        // Why: a locked currency follows the group's, which is no change of this person's once the bills go.
        if (hasExpenses) original = original?.copy(currency = group.currency)
        updateForm { state ->
            val filled = if (state.isLoaded) {
                state
            } else {
                state.copy(
                    name = group.name,
                    initials = Initials.of(group.name),
                    color = group.color,
                    shape = group.shape,
                    photo = group.photo,
                    type = group.type,
                    currency = group.currency,
                    isLoaded = true,
                )
            }
            filled.copy(
                isCurrencyLocked = hasExpenses,
                defaultSplitMode = group.defaultSplit?.mode ?: DefaultSplitMode.EQUAL,
                currency = if (hasExpenses) group.currency else filled.currency,
            )
        }
    }

    private fun openIconEditor() {
        val state = uiState.value
        if (!state.isLoaded || state.isSaving) return
        uiState.update {
            it.copy(
                iconEditor = GroupIconEditor(
                    color = it.color,
                    shape = it.shape,
                ),
            )
        }
    }

    private fun confirmIcon() {
        val editor = uiState.value.iconEditor ?: return
        useIcon(
            color = editor.color,
            shape = editor.shape,
        )
    }

    // Why: the icon stands in place of the photo, so picking one takes the photo off, and a pick still prepared too.
    private fun useIcon(
        color: AvatarColor,
        shape: GroupShape,
    ) {
        if (uiState.value.isSaving) return
        photoPicker.cancel()
        updateForm {
            it.copy(
                color = color,
                shape = shape,
                photo = null,
                isPreparingPhoto = false,
                showsPhotoError = false,
                iconEditor = null,
            )
        }
    }

    // Why: Salvar sends the photo shown, so a pick while saving is ignored.
    private fun preparePhoto(source: ByteArray) {
        uiState.update { it.copy(iconEditor = null) }
        if (uiState.value.isSaving) return
        photoPicker.prepare(source)
    }

    // Why: "Usar foto" brings the photo to the form, so it is ignored while saving, as a pick is.
    private fun cropPhoto(event: PhotoCropEvent) {
        if (event == PhotoCropEvent.OnConfirmClicked && uiState.value.isSaving) return
        photoPicker.onEvent(event)
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
        // Why: null keeps what Room has, which the domain may only read as the fallback (none, or a newer app's).
        val shape = state.shape.takeIf { it != original.shape }
        val photo = if (state.photo != original.photo) state.photo else latest.photo
        val currency = if (!state.isCurrencyLocked && state.currency != original.currency) {
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
                shape = shape,
                photo = photo,
                currency = currency,
            ).onSuccess {
                // Why: the form may not be on top to leave, so it stops saving and starts from what it just saved.
                this@EditGroupViewModel.original = latest.copy(
                    name = name.trim(),
                    type = type,
                    color = color,
                    shape = shape ?: latest.shape,
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
                shape != group.shape ||
                photo != group.photo ||
                (!isCurrencyLocked && currency != group.currency)
        )

    // Why: only while it is still on top: a native back (a swipe, the browser's, a sheet dismissed) may have closed it.
    private fun leave() {
        if (navigator.backStack.value.lastOrNull() == route) navigator.navigateBack()
    }
}
