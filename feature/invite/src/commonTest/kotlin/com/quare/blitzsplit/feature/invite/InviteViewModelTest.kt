package com.quare.blitzsplit.feature.invite

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.EncodeQrCode
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.QrCode
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.feature.invite.presentation.model.InviteMember
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiAction
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiEvent
import com.quare.blitzsplit.feature.invite.presentation.viewmodel.InviteViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class InviteViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var syncState: MutableStateFlow<SyncState>
    private var syncRetries = 0
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: InviteViewModel
    private val encodedTexts = mutableListOf<String>()
    private val actions = mutableListOf<InviteUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN Room not read yet WHEN the screen opens THEN nothing is loaded and the link is not pending`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isRoomRead = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertFalse(state.isLinkPending)
            assertNull(state.link)
            assertTrue(state.members.isEmpty())
        }

    @Test
    fun `GIVEN connectivity not known yet WHEN the screen opens THEN nothing is loaded`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isConnectivityKnown = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoaded)
        assertFalse(state.isLinkPending)
    }

    @Test
    fun `GIVEN a group on the device WHEN the screen opens THEN shows its name and members in order`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertEquals("Viagem Floripa", state.groupName)
            assertEquals(
                listOf(
                    InviteMember(
                        "member-1",
                        "Pierre",
                        "P",
                        AvatarColor.BLUE,
                        isMe = true,
                        hasJoined = true,
                    ),
                    InviteMember(
                        "member-2",
                        "Ana",
                        "AN",
                        AvatarColor.PEACH,
                        isMe = false,
                        hasJoined = false,
                    ),
                    InviteMember(
                        "member-3",
                        "Bruno",
                        "BR",
                        AvatarColor.GREEN,
                        isMe = false,
                        hasJoined = false,
                    ),
                    InviteMember(
                        "member-4",
                        "Carla",
                        "CA",
                        AvatarColor.LILAC,
                        isMe = false,
                        hasJoined = false,
                    ),
                ),
                state.members,
            )
        }

    @Test
    fun `GIVEN members with two words WHEN the screen opens THEN only the creator shows a single initial`() =
        runTest(testDispatcher) {
            // Given
            val members = listOf(
                pierre.copy(name = "Pierre Vieira"),
                ana.copy(name = "Ana Souza"),
            )
            prepareScenario(storedGroup = trip.copy(members = members))

            // When
            val initials = viewModel.uiState.value.members
                .map { it.initials }

            // Then
            assertEquals(listOf("P", "AS"), initials)
        }

    @Test
    fun `GIVEN a group without an invite code WHEN the screen opens THEN the link is pending with no QR code`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(inviteCode = null))

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.link)
            assertNull(state.displayLink)
            assertNull(state.qrCode)
            assertTrue(state.isLinkPending)
            assertTrue(encodedTexts.isEmpty())
        }

    @Test
    fun `GIVEN a group with an invite code WHEN the screen opens THEN shows the link and its QR code`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(inviteCode = "7Hk2pQ"))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("https://borarachar.com/g/7Hk2pQ", state.link)
            assertEquals("borarachar.com/g/7Hk2pQ", state.displayLink)
            assertEquals(qrCodeOf("https://borarachar.com/g/7Hk2pQ"), state.qrCode)
            assertFalse(state.isLinkPending)
            assertEquals(listOf("https://borarachar.com/g/7Hk2pQ"), encodedTexts)
        }

    @Test
    fun `GIVEN a pending link WHEN a sync brings the invite code THEN shows the link and its QR code`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(inviteCode = null))

            // When
            groups.groups.value = listOf(trip.copy(inviteCode = "7Hk2pQ"))

            // Then
            val state = viewModel.uiState.value
            assertEquals("https://borarachar.com/g/7Hk2pQ", state.link)
            assertEquals(qrCodeOf("https://borarachar.com/g/7Hk2pQ"), state.qrCode)
            assertFalse(state.isLinkPending)
        }

    @Test
    fun `GIVEN a link on screen WHEN a member is renamed THEN shows the new name without encoding the QR code again`() =
        runTest(testDispatcher) {
            // Given
            val withCode = trip.copy(inviteCode = "7Hk2pQ")
            prepareScenario(storedGroup = withCode)

            // When
            groups.groups.value =
                listOf(withCode.copy(members = listOf(pierre, ana.copy(name = "Aninha"), bruno, carla)))

            // Then
            val state = viewModel.uiState.value
            assertEquals("Aninha", state.members[1].name)
            assertEquals(qrCodeOf("https://borarachar.com/g/7Hk2pQ"), state.qrCode)
            assertEquals(listOf("https://borarachar.com/g/7Hk2pQ"), encodedTexts)
        }

    @Test
    fun `GIVEN a link on screen WHEN the invite code changes THEN encodes the QR code of the new link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(inviteCode = "7Hk2pQ"))

            // When
            groups.groups.value = listOf(trip.copy(inviteCode = "Zx9Yw8"))

            // Then
            val state = viewModel.uiState.value
            assertEquals("https://borarachar.com/g/Zx9Yw8", state.link)
            assertEquals(qrCodeOf("https://borarachar.com/g/Zx9Yw8"), state.qrCode)
            assertEquals(listOf("https://borarachar.com/g/7Hk2pQ", "https://borarachar.com/g/Zx9Yw8"), encodedTexts)
        }

    @Test
    fun `GIVEN some members did not join WHEN the screen opens THEN names only them in order`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(members = listOf(pierre, ana, bruno.copy(hasJoined = true), carla)))

            // When
            val names = viewModel.uiState.value.notJoinedNames

            // Then
            assertEquals(listOf("Ana", "Carla"), names)
        }

    @Test
    fun `GIVEN everyone joined WHEN the screen opens THEN names nobody`() = runTest(testDispatcher) {
        // Given
        val members = listOf(pierre, ana, bruno, carla).map { it.copy(hasJoined = true) }
        prepareScenario(storedGroup = trip.copy(members = members))

        // When
        val names = viewModel.uiState.value.notJoinedNames

        // Then
        assertTrue(names.isEmpty())
    }

    @Test
    fun `GIVEN no internet WHEN the screen opens THEN it is offline`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isOnline)
    }

    @Test
    fun `GIVEN no internet WHEN the connection comes back THEN it is online`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        isOnline.value = true

        // Then
        assertTrue(viewModel.uiState.value.isOnline)
    }

    @Test
    fun `GIVEN internet WHEN the connection drops THEN it is offline`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = true)

        // When
        isOnline.value = false

        // Then
        assertFalse(viewModel.uiState.value.isOnline)
    }

    @Test
    fun `GIVEN the group is not on the device WHEN the screen opens THEN goes back to the home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = null)

            // When
            val backStack = navigator.backStack.value

            // Then
            assertEquals(listOf(Route.Groups), backStack)
        }

    @Test
    fun `GIVEN the screen on top WHEN the group leaves the device THEN goes back to the home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            groups.clear()

            // Then
            assertEquals(listOf(Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a native back already closed the screen WHEN the group leaves the device THEN the stack stays as is`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.popTo(1)
            navigator.navigate(Route.Profile)

            // When
            groups.clear()

            // Then
            assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the screen on top WHEN close is tapped THEN goes back to the home`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(InviteUiEvent.OnCloseClicked)

        // Then
        assertEquals(listOf(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a native back already closed the screen WHEN close is tapped THEN the stack stays as is`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.popTo(1)
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(InviteUiEvent.OnCloseClicked)

            // Then
            assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a link WHEN copy is tapped THEN copies the whole link and shows it copied`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = trip.copy(inviteCode = "7Hk2pQ"))

        // When
        viewModel.onEvent(InviteUiEvent.OnCopyClicked)

        // Then
        assertEquals(listOf<InviteUiAction>(InviteUiAction.CopyLink("https://borarachar.com/g/7Hk2pQ")), actions)
        assertTrue(viewModel.uiState.value.isLinkCopied)
    }

    @Test
    fun `GIVEN the link just copied WHEN two seconds pass THEN it no longer shows copied`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = trip.copy(inviteCode = "7Hk2pQ"))
        viewModel.onEvent(InviteUiEvent.OnCopyClicked)
        advanceTimeBy(1_999.milliseconds)
        runCurrent()
        val copiedJustBefore = viewModel.uiState.value.isLinkCopied

        // When
        advanceTimeBy(1.milliseconds)
        runCurrent()

        // Then
        assertTrue(copiedJustBefore)
        assertFalse(viewModel.uiState.value.isLinkCopied)
    }

    @Test
    fun `GIVEN the link copied 1500 ms ago WHEN copy is tapped again THEN shows copied 2 s from the second tap`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(inviteCode = "7Hk2pQ"))
            viewModel.onEvent(InviteUiEvent.OnCopyClicked)
            advanceTimeBy(1_500.milliseconds)
            runCurrent()

            // When
            viewModel.onEvent(InviteUiEvent.OnCopyClicked)

            // Then
            advanceTimeBy(1_000.milliseconds)
            runCurrent()
            val copiedAfterTheFirstTimer = viewModel.uiState.value.isLinkCopied
            advanceTimeBy(1_000.milliseconds)
            runCurrent()
            assertTrue(copiedAfterTheFirstTimer)
            assertFalse(viewModel.uiState.value.isLinkCopied)
            assertEquals(2, actions.size)
        }

    @Test
    fun `GIVEN a pending link WHEN copy is tapped THEN copies nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = trip.copy(inviteCode = null))

        // When
        viewModel.onEvent(InviteUiEvent.OnCopyClicked)

        // Then
        assertTrue(actions.isEmpty())
        assertFalse(viewModel.uiState.value.isLinkCopied)
    }

    @Test
    fun `GIVEN a link WHEN share is tapped THEN shares the link with the group name`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = trip.copy(inviteCode = "7Hk2pQ"))

        // When
        viewModel.onEvent(InviteUiEvent.OnShareClicked)

        // Then
        assertEquals(
            listOf<InviteUiAction>(InviteUiAction.ShareLink("https://borarachar.com/g/7Hk2pQ", "Viagem Floripa")),
            actions,
        )
    }

    @Test
    fun `GIVEN a pending link WHEN share is tapped THEN shares nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = trip.copy(inviteCode = null))

        // When
        viewModel.onEvent(InviteUiEvent.OnShareClicked)

        // Then
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN the screen open WHEN see who joined is tapped THEN says it is coming soon`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(InviteUiEvent.OnSeeWhoJoinedClicked)

        // Then
        assertEquals(listOf<InviteUiAction>(InviteUiAction.ShowComingSoon), actions)
    }

    @Test
    fun `GIVEN a pending link WHEN go to the group is tapped THEN says it is coming soon`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = trip.copy(inviteCode = null))

        // When
        viewModel.onEvent(InviteUiEvent.OnGoToGroupClicked)

        // Then
        assertEquals(listOf<InviteUiAction>(InviteUiAction.ShowComingSoon), actions)
        assertEquals(listOf(Route.Groups, Route.Invite("group-1")), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a pending link online and a failed sync WHEN the screen opens THEN the link failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(inviteCode = null),
                online = true,
                sync = SyncState.FAILED,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertTrue(state.isLinkFailed)
        }

    @Test
    fun `GIVEN a pending link offline and a failed sync WHEN the screen opens THEN it is pending not failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(inviteCode = null),
                online = false,
                sync = SyncState.FAILED,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLinkPending)
            assertFalse(state.isOnline)
            assertFalse(state.isLinkFailed)
        }

    @Test
    fun `GIVEN a link online and a failed sync WHEN the screen opens THEN the link is not failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(inviteCode = "7Hk2pQ"),
                online = true,
                sync = SyncState.FAILED,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("https://borarachar.com/g/7Hk2pQ", state.link)
            assertFalse(state.isLinkFailed)
        }

    @Test
    fun `GIVEN a pending link online and a sync waiting to run WHEN the screen opens THEN the link is not failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(inviteCode = null),
                online = true,
                sync = SyncState.PENDING,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLinkPending)
            assertFalse(state.isLinkFailed)
        }

    @Test
    fun `GIVEN a pending link online and a synced device WHEN the screen opens THEN the link is not failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(inviteCode = null),
                online = true,
                sync = SyncState.SYNCED,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLinkPending)
            assertFalse(state.isLinkFailed)
        }

    @Test
    fun `GIVEN Room not read yet and a failed sync online WHEN the screen opens THEN the link is not failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                isRoomRead = false,
                online = true,
                sync = SyncState.FAILED,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertFalse(state.isLinkFailed)
        }

    @Test
    fun `GIVEN connectivity not known yet and a failed sync WHEN the screen opens THEN nothing is loaded nor failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(inviteCode = null),
                isConnectivityKnown = false,
                sync = SyncState.FAILED,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertFalse(state.isLinkPending)
            assertFalse(state.isLinkFailed)
        }

    @Test
    fun `GIVEN a pending link online WHEN the sync fails THEN the link failed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroup = trip.copy(inviteCode = null),
            online = true,
            sync = SyncState.SYNCING,
        )

        // When
        syncState.value = SyncState.FAILED

        // Then
        assertTrue(viewModel.uiState.value.isLinkFailed)
    }

    @Test
    fun `GIVEN the link failed WHEN the sync starts again THEN the link is pending and no longer failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(inviteCode = null),
                online = true,
                sync = SyncState.FAILED,
            )

            // When
            syncState.value = SyncState.SYNCING

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isLinkPending)
            assertFalse(state.isLinkFailed)
        }

    @Test
    fun `GIVEN the link failed WHEN a sync succeeds without the code THEN the link is no longer failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(inviteCode = null),
                online = true,
                sync = SyncState.FAILED,
            )

            // When
            syncState.value = SyncState.SYNCED

            // Then
            assertFalse(viewModel.uiState.value.isLinkFailed)
        }

    @Test
    fun `GIVEN the link failed WHEN the invite code arrives THEN shows the link and it is not failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(inviteCode = null),
                online = true,
                sync = SyncState.FAILED,
            )

            // When
            groups.groups.value = listOf(trip.copy(inviteCode = "7Hk2pQ"))

            // Then
            val state = viewModel.uiState.value
            assertEquals("https://borarachar.com/g/7Hk2pQ", state.link)
            assertFalse(state.isLinkFailed)
        }

    @Test
    fun `GIVEN the link failed WHEN the connection drops THEN the link is pending offline and not failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(inviteCode = null),
                online = true,
                sync = SyncState.FAILED,
            )

            // When
            isOnline.value = false

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isLinkPending)
            assertFalse(state.isOnline)
            assertFalse(state.isLinkFailed)
        }

    @Test
    fun `GIVEN a pending link offline and a failed sync WHEN the connection comes back THEN the link failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(inviteCode = null),
                online = false,
                sync = SyncState.FAILED,
            )

            // When
            isOnline.value = true

            // Then
            assertTrue(viewModel.uiState.value.isLinkFailed)
        }

    @Test
    fun `GIVEN the link failed WHEN retry is tapped THEN syncs again once`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroup = trip.copy(inviteCode = null),
            online = true,
            sync = SyncState.FAILED,
        )

        // When
        viewModel.onEvent(InviteUiEvent.OnRetryClicked)

        // Then
        assertEquals(1, syncRetries)
        assertTrue(actions.isEmpty())
        assertEquals(listOf(Route.Groups, Route.Invite("group-1")), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a pending link offline and a failed sync WHEN retry is tapped THEN does not sync`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(inviteCode = null),
                online = false,
                sync = SyncState.FAILED,
            )

            // When
            viewModel.onEvent(InviteUiEvent.OnRetryClicked)

            // Then
            assertEquals(0, syncRetries)
        }

    @Test
    fun `GIVEN a link online and a failed sync WHEN retry is tapped THEN does not sync`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroup = trip.copy(inviteCode = "7Hk2pQ"),
            online = true,
            sync = SyncState.FAILED,
        )

        // When
        viewModel.onEvent(InviteUiEvent.OnRetryClicked)

        // Then
        assertEquals(0, syncRetries)
    }

    @Test
    fun `GIVEN a pending link online while syncing WHEN retry is tapped THEN does not sync`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(inviteCode = null),
                online = true,
                sync = SyncState.SYNCING,
            )

            // When
            viewModel.onEvent(InviteUiEvent.OnRetryClicked)

            // Then
            assertEquals(0, syncRetries)
        }

    @Test
    fun `GIVEN connectivity not known yet and a failed sync WHEN retry is tapped THEN does not sync`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(inviteCode = null),
                isConnectivityKnown = false,
                sync = SyncState.FAILED,
            )

            // When
            viewModel.onEvent(InviteUiEvent.OnRetryClicked)

            // Then
            assertEquals(0, syncRetries)
        }

    private val pierre = GroupMember(
        "member-1",
        "Pierre",
        AvatarColor.BLUE,
        isMe = true,
        hasJoined = true,
    )
    private val ana = GroupMember(
        "member-2",
        "Ana",
        AvatarColor.PEACH,
        isMe = false,
        hasJoined = false,
    )
    private val bruno = GroupMember(
        "member-3",
        "Bruno",
        AvatarColor.GREEN,
        isMe = false,
        hasJoined = false,
    )
    private val carla = GroupMember(
        "member-4",
        "Carla",
        AvatarColor.LILAC,
        isMe = false,
        hasJoined = false,
    )

    private val trip = Group(
        id = "group-1",
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.BLUE,
        currency = GroupCurrency.BRL,
        members = listOf(pierre, ana, bruno, carla),
        isPending = true,
        inviteCode = null,
    )

    // Why: a QR code that differs per text, so a case can tell which link was encoded.
    private fun qrCodeOf(text: String): QrCode = QrCode(
        size = text.length,
        modules = text.map { it.isUpperCase() },
    )

    private fun TestScope.prepareScenario(
        storedGroup: Group? = trip,
        isRoomRead: Boolean = true,
        online: Boolean = true,
        isConnectivityKnown: Boolean = true,
        sync: SyncState = SyncState.PENDING,
    ) {
        groups = FakeGroupRepository(initialGroups = listOfNotNull(storedGroup))
        isOnline = MutableStateFlow(online)
        syncState = MutableStateFlow(sync)
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.CreateGroup)
            it.replaceTop(Route.Invite("group-1"))
        }
        val roomNeverAnswers = MutableSharedFlow<Group?>()
        val connectivityNeverAnswers = MutableSharedFlow<Boolean>()
        viewModel = InviteViewModel(
            route = Route.Invite("group-1"),
            navigator = navigator,
            encodeQrCode = EncodeQrCode { text ->
                encodedTexts += text
                qrCodeOf(text)
            },
            retrySync = RetrySync { syncRetries++ },
            observeGroup = if (isRoomRead) ObserveGroupUseCase(groups) else ObserveGroup { roomNeverAnswers },
            observeSyncState = ObserveSyncState { syncState },
            connectivityObserver = NetworkConnectivityObserver {
                if (isConnectivityKnown) isOnline else connectivityNeverAnswers
            },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
