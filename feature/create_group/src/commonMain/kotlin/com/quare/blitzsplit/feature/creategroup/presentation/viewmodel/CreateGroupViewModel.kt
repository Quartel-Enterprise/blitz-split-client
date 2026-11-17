package com.quare.blitzsplit.feature.creategroup.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.contacts.domain.usecase.ObserveContactBook
import com.quare.blitzsplit.core.groups.domain.model.GroupNameRules
import com.quare.blitzsplit.core.groups.domain.model.GroupReminder
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.model.MemberColors
import com.quare.blitzsplit.core.groups.domain.model.MemberNameCheck
import com.quare.blitzsplit.core.groups.domain.model.NewGroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.BuildReminderCalendarEvent
import com.quare.blitzsplit.core.groups.domain.usecase.CheckMemberName
import com.quare.blitzsplit.core.groups.domain.usecase.CreateGroup
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.guardUnsavedInput
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.LoadCropPhoto
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.profile.domain.usecase.PrepareProfilePhoto
import com.quare.blitzsplit.core.profile.photo.ProfilePhotoPicker
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.utils.DeviceRegion
import com.quare.blitzsplit.feature.creategroup.presentation.model.ContactSuggestions
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupMember
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupUiAction
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupUiEvent
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

class CreateGroupViewModel(
    private val navigator: Navigator,
    private val createGroup: CreateGroup,
    private val buildReminderCalendarEvent: BuildReminderCalendarEvent,
    private val checkMemberName: CheckMemberName,
    observeProfile: ObserveProfile,
    observeContactBook: ObserveContactBook,
    deviceRegion: DeviceRegion,
    loadCropPhoto: LoadCropPhoto,
    prepareProfilePhoto: PrepareProfilePhoto,
    trackEvent: TrackEvent,
) : TrackedViewModel<CreateGroupUiEvent>(trackEvent) {
    val uiState: StateFlow<CreateGroupUiState>
        field = MutableStateFlow(
            CreateGroupUiState(
                name = "",
                initials = "",
                color = AvatarColor.default,
                photo = null,
                isPreparingPhoto = false,
                showsPhotoError = false,
                photoCrop = null,
                type = GroupType.default,
                currency = GroupCurrency.ofRegion(deviceRegion()),
                members = emptyList(),
                draft = "",
                suggestions = emptyList(),
                isCreating = false,
                showsCreateError = false,
                openedColor = AvatarColor.default,
                openedCurrency = GroupCurrency.ofRegion(deviceRegion()),
                reminder = GroupReminder.Default,
                addsReminderToCalendar = false,
            ),
        )

    val uiAction: SharedFlow<CreateGroupUiAction>
        field = MutableSharedFlow<CreateGroupUiAction>()

    val stateWatcher: FlowWatcher<CreateGroupUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<CreateGroupUiAction> = FlowWatcher(uiAction)

    // Why: the group's photo is the same 512x512 JPEG as a profile's, framed the same way.
    private val photoPicker = ProfilePhotoPicker(
        loadCropPhoto = loadCropPhoto,
        prepareProfilePhoto = prepareProfilePhoto,
        scope = viewModelScope,
        showStatus = ::showPhotoStatus,
        showEditor = { editor -> uiState.update { it.copy(photoCrop = editor) } },
        onPrepared = ::showPhoto,
        // Why: Criar sends the photo shown.
        isBusy = { uiState.value.isCreating },
    )

    // Why: the device's Contacts, offline too, kept current so a sync landing while someone types suggests it too.
    private var contacts: List<Contact> = emptyList()

    /*
     * Why: the creator is the group's first member, with the name and color of their profile, and the group starts
     * in the first color that isn't theirs. Read once, so a sync landing while someone types changes nothing here.
     */
    init {
        viewModelScope.launch {
            val profile = observeProfile().filterNotNull().first()
            val creatorName = profile.displayName.trim()
            val me = CreateGroupMember(
                id = Uuid.random().toString(),
                name = creatorName,
                initials = Initials.firstOf(creatorName),
                avatarColor = profile.avatarColor,
                photoUrl = profile.photo?.toImageUrl(),
                isMe = true,
                userId = null,
            )
            uiState.update { state ->
                val color = AvatarColor.entries.first { it != profile.avatarColor }
                withSuggestions(
                    state.copy(
                        color = color,
                        openedColor = color,
                        members = listOf(me) + state.members,
                    ),
                )
            }
        }
    }

    init {
        viewModelScope.launch {
            observeContactBook().collect { snapshot ->
                contacts = snapshot.contacts
                uiState.update(::withSuggestions)
            }
        }
    }

    init {
        navigator.guardUnsavedInput(viewModelScope, Route.CreateGroup, uiState.map { it.hasUnsavedInput })
    }

    override fun handleEvent(event: CreateGroupUiEvent) {
        when (event) {
            // Why: leaving clears the ViewModel, which would cancel a save in flight.
            CreateGroupUiEvent.OnCloseClicked -> if (!uiState.value.isCreating) leave()

            is CreateGroupUiEvent.OnNameChanged -> changeName(event.name)

            CreateGroupUiEvent.OnColorClicked -> changeColor { it.copy(color = it.color.toNextColor()) }

            is CreateGroupUiEvent.OnPhotoPicked -> photoPicker.prepare(event.source)

            is CreateGroupUiEvent.OnPhotoCrop -> photoPicker.onEvent(event.event)

            CreateGroupUiEvent.OnPhotoUnreadable -> photoPicker.fail()

            CreateGroupUiEvent.OnPhotoRemoved -> removePhoto()

            is CreateGroupUiEvent.OnTypeSelected -> uiState.update { it.copy(type = event.type) }

            is CreateGroupUiEvent.OnCurrencySelected -> uiState.update { it.copy(currency = event.currency) }

            is CreateGroupUiEvent.OnReminderEnabledChanged -> uiState.update {
                it.copy(
                    reminder = it.reminder.copy(isEnabled = event.isEnabled),
                    // Why: with the reminder off there is nothing to add to a calendar.
                    addsReminderToCalendar = it.addsReminderToCalendar && event.isEnabled,
                )
            }

            is CreateGroupUiEvent.OnReminderKindSelected -> uiState.update {
                it.copy(reminder = it.reminder.withKind(event.kind))
            }

            is CreateGroupUiEvent.OnReminderDaySelected -> uiState.update {
                it.copy(reminder = it.reminder.withDay(event.day))
            }

            is CreateGroupUiEvent.OnReminderTimeSelected -> uiState.update {
                it.copy(reminder = it.reminder.withMinute(event.minute))
            }

            is CreateGroupUiEvent.OnAddToCalendarChanged -> uiState.update {
                it.copy(addsReminderToCalendar = event.isEnabled && it.canAddReminderToCalendar)
            }

            is CreateGroupUiEvent.OnDraftChanged -> changeDraft(event.draft)

            CreateGroupUiEvent.OnAddMemberClicked -> addDraft()

            is CreateGroupUiEvent.OnSuggestionClicked -> addContact(event.key)

            is CreateGroupUiEvent.OnRemoveMemberClicked -> removeMember(event.memberId)

            CreateGroupUiEvent.OnCreateClicked -> create()
        }
    }

    private fun changeColor(transform: (CreateGroupUiState) -> CreateGroupUiState) {
        uiState.update { if (it.canChangeColor) transform(it) else it }
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
        uiState.update { withSuggestions(it.copy(draft = GroupNameRules.toCapped(draft))) }
    }

    // Why: a name may repeat in the group (beta feedback 3, item 29); people are told apart by their member ids.
    private fun addDraft() {
        val check = checkMemberName(uiState.value.draft) as? MemberNameCheck.Available ?: return
        add(check.name)
    }

    private fun add(name: String) {
        uiState.update { state ->
            state.withMember(
                CreateGroupMember(
                    id = Uuid.random().toString(),
                    name = name,
                    initials = Initials.of(name),
                    avatarColor = findLeastUsedColor(state.members),
                    photoUrl = null,
                    isMe = false,
                    userId = null,
                ),
            )
        }
    }

    /*
     * Why: the same person Contacts lists, as its "Add to a group" adds them: someone who uses the app as their account,
     * a guest as their name, in their own color unless someone added already has it.
     */
    private fun addContact(key: String) {
        val state = uiState.value
        if (state.isCreating || state.members.none { it.isMe }) return
        if (state.members.any { it.personKey == key }) return
        val contact = contacts.firstOrNull { it.key == key } ?: return
        val name = (checkMemberName(contact.name) as? MemberNameCheck.Available)?.name ?: return
        uiState.update { current ->
            current.withMember(
                CreateGroupMember(
                    id = Uuid.random().toString(),
                    name = name,
                    initials = Initials.of(name),
                    avatarColor = MemberColors.findFor(
                        contact.color,
                        current.members.map(CreateGroupMember::avatarColor),
                    ),
                    photoUrl = contact.photoUrl,
                    isMe = false,
                    userId = contact.userId,
                ),
            )
        }
    }

    private fun CreateGroupUiState.withMember(member: CreateGroupMember): CreateGroupUiState = withSuggestions(
        copy(
            members = members + member,
            draft = "",
        ),
    )

    // Why: none until this device's user is in the list, since a pick needs them there first (addContact).
    private fun withSuggestions(state: CreateGroupUiState): CreateGroupUiState = state.copy(
        suggestions = if (state.members.none { it.isMe }) {
            emptyList()
        } else {
            ContactSuggestions.of(
                contacts = contacts,
                draft = state.draft,
                addedKeys = state.members.mapNotNullTo(mutableSetOf(), CreateGroupMember::personKey),
            )
        },
    )

    private fun findLeastUsedColor(members: List<CreateGroupMember>): AvatarColor =
        MemberColors.findLeastUsed(members.map(CreateGroupMember::avatarColor))

    private fun removeMember(memberId: String) {
        uiState.update { state ->
            withSuggestions(state.copy(members = state.members.filterNot { it.id == memberId && !it.isMe }))
        }
    }

    private fun create() {
        trackEvent("create_group_create_clicked", emptyMap())
        val state = uiState.value
        if (!state.canCreate) return
        val draft = checkMemberName(state.draft) as? MemberNameCheck.Available
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
                userId = it.userId,
            )
        } + listOfNotNull(
            draft?.let {
                NewGroupMember(
                    name = it.name,
                    avatarColor = findLeastUsedColor(state.members),
                    isMe = false,
                    userId = null,
                )
            },
        )
        viewModelScope.launch {
            createGroup(
                name = state.name,
                type = state.type,
                color = state.color,
                // Why: every group is the same rounded square; the shape stays in the data for the groups made before.
                shape = GroupShape.fallback,
                photo = state.photo,
                currency = state.currency,
                members = members,
                reminder = state.reminder,
            ).onSuccess { groupId ->
                trackEvent(
                    "group_created",
                    mapOf(
                        "type" to state.type,
                        "currency" to state.currency,
                        "members" to members.size,
                        "has_photo" to (state.photo != null),
                        "reminder" to reminderKindOf(state),
                    ),
                )
                if (state.addsReminderToCalendar) {
                    buildReminderCalendarEvent(state.name, state.reminder)?.let {
                        uiAction.emit(CreateGroupUiAction.AddReminderToCalendar(it))
                    }
                }
                openInvite(groupId)
            }.onFailure {
                uiState.update {
                    it.copy(
                        isCreating = false,
                        showsCreateError = true,
                    )
                }
            }
        }
    }

    // Why: "off", "weekly" or "monthly", for the group's analytics.
    private fun reminderKindOf(state: CreateGroupUiState): String =
        if (state.reminder.isEnabled) state.reminder.kind.code else "off"

    private fun AvatarColor.toNextColor(): AvatarColor = AvatarColor.entries[(ordinal + 1) % AvatarColor.entries.size]

    // Why: "Convidar" takes the form's place, so back goes to the home; only while the form is still on top, as leave.
    private fun openInvite(groupId: String) {
        // Why: the group is saved, so the form's input is no longer unsaved and closing it asks nothing.
        navigator.markUnsaved(Route.CreateGroup, false)
        if (isOnTop()) navigator.replaceTop(Route.Invite(groupId, isNewGroup = true))
    }

    // Why: only while it is still on top: a native back (a swipe, the browser's, a sheet dismissed) may have closed it.
    private fun leave() {
        if (isOnTop()) navigator.navigateBack()
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == Route.CreateGroup
}
