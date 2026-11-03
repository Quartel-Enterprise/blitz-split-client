package com.quare.blitzsplit.feature.creategroup.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.groups.domain.model.GroupNameRules
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.model.MemberColors
import com.quare.blitzsplit.core.groups.domain.model.MemberNameCheck
import com.quare.blitzsplit.core.groups.domain.model.NewGroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.CheckMemberName
import com.quare.blitzsplit.core.groups.domain.usecase.CreateGroup
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.LoadCropPhoto
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.profile.domain.usecase.PrepareProfilePhoto
import com.quare.blitzsplit.core.profile.photo.ProfilePhotoPicker
import com.quare.blitzsplit.core.utils.DeviceRegion
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupMember
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupUiEvent
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

class CreateGroupViewModel(
    private val navigator: Navigator,
    private val createGroup: CreateGroup,
    private val checkMemberName: CheckMemberName,
    observeProfile: ObserveProfile,
    deviceRegion: DeviceRegion,
    loadCropPhoto: LoadCropPhoto,
    prepareProfilePhoto: PrepareProfilePhoto,
) : ViewModel() {
    val uiState: StateFlow<CreateGroupUiState>
        field = MutableStateFlow(
            CreateGroupUiState(
                name = "",
                initials = "",
                color = AvatarColor.default,
                shape = GroupShape.default,
                photo = null,
                isPreparingPhoto = false,
                showsPhotoError = false,
                photoCrop = null,
                type = GroupType.default,
                currency = GroupCurrency.ofRegion(deviceRegion()),
                members = emptyList(),
                draft = "",
                takenName = null,
                nicknameSuggestions = emptyList(),
                isCreating = false,
                showsCreateError = false,
            ),
        )

    val stateWatcher: FlowWatcher<CreateGroupUiState> = FlowWatcher(uiState)

    // Why: the group's photo is the same 512x512 JPEG as a profile's, framed the same way.
    private val photoPicker = ProfilePhotoPicker(
        loadCropPhoto = loadCropPhoto,
        prepareProfilePhoto = prepareProfilePhoto,
        scope = viewModelScope,
        showStatus = ::showPhotoStatus,
        showEditor = { editor -> uiState.update { it.copy(photoCrop = editor) } },
        onPrepared = ::showPhoto,
    )

    // Why: the nickname's initial ("Ana P."), the creator's.
    private var creatorName = ""

    /*
     * Why: the creator is the group's first member, with the name and color of their profile, and the group starts
     * in the first color that isn't theirs. Read once, so a sync landing while someone types changes nothing here.
     */
    init {
        viewModelScope.launch {
            val profile = observeProfile().filterNotNull().first()
            creatorName = profile.displayName.trim()
            val me = CreateGroupMember(
                id = Uuid.random().toString(),
                name = creatorName,
                initials = Initials.firstOf(creatorName),
                avatarColor = profile.avatarColor,
                isMe = true,
            )
            uiState.update { state ->
                state.copy(
                    color = AvatarColor.entries.first { it != profile.avatarColor },
                    members = listOf(me) + state.members,
                )
            }
            checkDraft()
        }
    }

    fun onEvent(event: CreateGroupUiEvent) {
        when (event) {
            // Why: leaving clears the ViewModel, which would cancel a save in flight.
            CreateGroupUiEvent.OnCloseClicked -> if (!uiState.value.isCreating) leave()

            is CreateGroupUiEvent.OnNameChanged -> changeName(event.name)

            CreateGroupUiEvent.OnColorClicked -> changeColor { it.copy(color = it.color.toNextColor()) }

            CreateGroupUiEvent.OnShapeAndColorClicked -> changeColor {
                it.copy(
                    color = it.color.toNextColor(),
                    shape = it.shape.toNext(),
                )
            }

            is CreateGroupUiEvent.OnPhotoPicked -> preparePhoto(event.source)

            is CreateGroupUiEvent.OnPhotoCrop -> cropPhoto(event.event)

            CreateGroupUiEvent.OnPhotoUnreadable -> photoPicker.fail()

            CreateGroupUiEvent.OnPhotoRemoved -> removePhoto()

            is CreateGroupUiEvent.OnTypeSelected -> uiState.update { it.copy(type = event.type) }

            is CreateGroupUiEvent.OnCurrencySelected -> uiState.update { it.copy(currency = event.currency) }

            is CreateGroupUiEvent.OnDraftChanged -> changeDraft(event.draft)

            CreateGroupUiEvent.OnAddMemberClicked -> addDraft()

            is CreateGroupUiEvent.OnSuggestionClicked -> addSuggestion(event.name)

            is CreateGroupUiEvent.OnRemoveMemberClicked -> removeMember(event.memberId)

            CreateGroupUiEvent.OnCreateClicked -> create()
        }
    }

    private fun changeColor(transform: (CreateGroupUiState) -> CreateGroupUiState) {
        uiState.update { if (it.canChangeColor) transform(it) else it }
    }

    // Why: Criar sends the photo shown, so a pick or "Usar foto" while creating is ignored.
    private fun preparePhoto(source: ByteArray) {
        if (uiState.value.isCreating) return
        photoPicker.prepare(source)
    }

    private fun cropPhoto(event: PhotoCropEvent) {
        if (event == PhotoCropEvent.OnConfirmClicked && uiState.value.isCreating) return
        photoPicker.onEvent(event)
    }

    private fun removePhoto() {
        if (uiState.value.isCreating) return
        photoPicker.cancel()
        uiState.update {
            it.copy(
                photo = null,
                isPreparingPhoto = false,
                showsPhotoError = false,
            )
        }
    }

    private fun showPhotoStatus(
        isPreparing: Boolean,
        hasFailed: Boolean,
    ) {
        uiState.update {
            it.copy(
                isPreparingPhoto = isPreparing,
                showsPhotoError = hasFailed,
            )
        }
    }

    // Why: the picker only prepares pending photos.
    private fun showPhoto(photo: ProfilePhoto) {
        uiState.update {
            it.copy(
                photo = photo as? ProfilePhoto.Pending,
                isPreparingPhoto = false,
                showsPhotoError = false,
            )
        }
    }

    private fun changeName(name: String) {
        val capped = GroupNameRules.toCapped(name)
        uiState.update {
            it.copy(
                name = capped,
                initials = Initials.of(capped),
            )
        }
    }

    private fun changeDraft(draft: String) {
        uiState.update { it.copy(draft = GroupNameRules.toCapped(draft)) }
        checkDraft()
    }

    private fun checkDraft() {
        val check = checkOf(uiState.value.draft)
        uiState.update {
            it.copy(
                takenName = (check as? MemberNameCheck.Taken)?.existingName,
                nicknameSuggestions = (check as? MemberNameCheck.Taken)?.suggestions.orEmpty(),
            )
        }
    }

    private fun checkOf(name: String): MemberNameCheck = checkMemberName(
        name = name,
        takenNames = uiState.value.members.map(CreateGroupMember::name),
        adderName = creatorName,
    )

    private fun addDraft() {
        val check = checkOf(uiState.value.draft) as? MemberNameCheck.Available ?: return
        add(check.name)
    }

    // Why: a suggestion is free when offered, but the members may have changed since.
    private fun addSuggestion(name: String) {
        if (name !in uiState.value.nicknameSuggestions) return
        val check = checkOf(name) as? MemberNameCheck.Available ?: return
        add(check.name)
    }

    private fun add(name: String) {
        uiState.update { state ->
            state.copy(
                members = state.members + CreateGroupMember(
                    id = Uuid.random().toString(),
                    name = name,
                    initials = Initials.of(name),
                    avatarColor = findLeastUsedColor(state.members),
                    isMe = false,
                ),
                draft = "",
            )
        }
        checkDraft()
    }

    private fun findLeastUsedColor(members: List<CreateGroupMember>): AvatarColor =
        MemberColors.findLeastUsed(members.map(CreateGroupMember::avatarColor))

    private fun removeMember(memberId: String) {
        uiState.update { state -> state.copy(members = state.members.filterNot { it.id == memberId && !it.isMe }) }
        checkDraft()
    }

    private fun create() {
        val state = uiState.value
        if (!state.canCreate) return
        val draft = checkOf(state.draft) as? MemberNameCheck.Available
        uiState.update {
            it.copy(
                isCreating = true,
                showsCreateError = false,
            )
        }
        val members = state.members.map {
            NewGroupMember(
                name = it.name,
                avatarColor = it.avatarColor,
                isMe = it.isMe,
            )
        } + listOfNotNull(
            draft?.let {
                NewGroupMember(
                    name = it.name,
                    avatarColor = findLeastUsedColor(state.members),
                    isMe = false,
                )
            },
        )
        viewModelScope.launch {
            createGroup(
                name = state.name,
                type = state.type,
                color = state.color,
                shape = state.shape,
                photo = state.photo,
                currency = state.currency,
                members = members,
            ).onSuccess(::openInvite)
                .onFailure {
                    uiState.update {
                        it.copy(
                            isCreating = false,
                            showsCreateError = true,
                        )
                    }
                }
        }
    }

    private fun AvatarColor.toNextColor(): AvatarColor = AvatarColor.entries[(ordinal + 1) % AvatarColor.entries.size]

    // Why: "Convidar" takes the form's place, so back goes to the home; only while the form is still on top, as leave.
    private fun openInvite(groupId: String) {
        if (isOnTop()) navigator.replaceTop(Route.Invite(groupId, isNewGroup = true))
    }

    // Why: only while it is still on top: a native back (a swipe, the browser's, a sheet dismissed) may have closed it.
    private fun leave() {
        if (isOnTop()) navigator.navigateBack()
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == Route.CreateGroup
}
