package com.quare.blitzsplit.feature.invite

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.EncodeQrCode
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.QrCode
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.usecase.ChangeInviteLink
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiAction
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiEvent
import com.quare.blitzsplit.feature.invite.presentation.viewmodel.InviteViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Why: "Trocar link" and its confirmation; the rest of Convidar is in InviteViewModelTest.
class InviteChangeLinkViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
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
    fun `GIVEN the creator online with a link WHEN the screen opens from the group THEN offers to change the link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isCreatedByMe)
            assertFalse(state.isNewGroup)
            assertTrue(state.showsChangeLink)
            assertFalse(state.isConfirmingLinkChange)
            assertFalse(state.isChangingLink)
        }

    @Test
    fun `GIVEN the creator online with a link WHEN the screen opens after creating the group THEN does not offer it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isNewGroup = true)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isNewGroup)
            assertTrue(state.isCreatedByMe)
            assertFalse(state.showsChangeLink)
        }

    @Test
    fun `GIVEN a group someone else created WHEN the screen opens from the group THEN hides change link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = creatorsTrip.copy(isCreatedByMe = false))

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isCreatedByMe)
            assertFalse(state.showsChangeLink)
        }

    @Test
    fun `GIVEN a group created offline with no link yet WHEN the screen opens THEN does not offer to change it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = creatorsTrip.copy(inviteCode = null))

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLinkPending)
            assertFalse(state.showsChangeLink)
        }

    @Test
    fun `GIVEN no internet WHEN the screen opens from the group THEN does not offer to change the link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.showsChangeLink)
        }

    @Test
    fun `GIVEN change link offered WHEN the connection drops THEN no longer offers it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        isOnline.value = false

        // Then
        assertFalse(viewModel.uiState.value.showsChangeLink)
    }

    @Test
    fun `GIVEN no internet WHEN the connection comes back THEN offers to change the link`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        isOnline.value = true

        // Then
        assertTrue(viewModel.uiState.value.showsChangeLink)
    }

    @Test
    fun `GIVEN change link offered WHEN the creator deleted their account THEN nobody is offered it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            groups.groups.value = listOf(creatorsTrip.copy(isCreatedByMe = false))

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isCreatedByMe)
            assertFalse(state.showsChangeLink)
        }

    @Test
    fun `GIVEN change link offered WHEN change link is tapped THEN asks to confirm without changing it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(InviteUiEvent.OnChangeLinkClicked)

            // Then
            assertTrue(viewModel.uiState.value.isConfirmingLinkChange)
            assertTrue(groups.rotatedInviteCodeGroupIds.isEmpty())
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN a group someone else created WHEN change link is tapped THEN does not ask to confirm`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = creatorsTrip.copy(isCreatedByMe = false))

            // When
            viewModel.onEvent(InviteUiEvent.OnChangeLinkClicked)

            // Then
            assertFalse(viewModel.uiState.value.isConfirmingLinkChange)
        }

    @Test
    fun `GIVEN no internet WHEN change link is tapped THEN does not ask to confirm`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        viewModel.onEvent(InviteUiEvent.OnChangeLinkClicked)

        // Then
        assertFalse(viewModel.uiState.value.isConfirmingLinkChange)
    }

    @Test
    fun `GIVEN the link being changed WHEN change link is tapped again THEN does not ask to confirm`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(rotateGate = CompletableDeferred())
            viewModel.onEvent(InviteUiEvent.OnChangeLinkClicked)
            viewModel.onEvent(InviteUiEvent.OnChangeLinkConfirmed)

            // When
            viewModel.onEvent(InviteUiEvent.OnChangeLinkClicked)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isChangingLink)
            assertFalse(state.isConfirmingLinkChange)
        }

    @Test
    fun `GIVEN the confirmation open WHEN it is dismissed THEN closes it and keeps the link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(InviteUiEvent.OnChangeLinkClicked)

            // When
            viewModel.onEvent(InviteUiEvent.OnChangeLinkDismissed)

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isConfirmingLinkChange)
            assertFalse(state.isChangingLink)
            assertEquals(OLD_LINK, state.link)
            assertTrue(groups.rotatedInviteCodeGroupIds.isEmpty())
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the confirmation open WHEN it is confirmed THEN closes it and changes the link of the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(rotateGate = CompletableDeferred())
            viewModel.onEvent(InviteUiEvent.OnChangeLinkClicked)

            // When
            viewModel.onEvent(InviteUiEvent.OnChangeLinkConfirmed)

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isConfirmingLinkChange)
            assertTrue(state.isChangingLink)
            assertEquals(OLD_LINK, state.link)
            assertEquals(qrCodeOf(OLD_LINK), state.qrCode)
        }

    @Test
    fun `GIVEN the link being changed WHEN the server makes the new link THEN shows it and its new QR code`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(rotateGate = gate)
            viewModel.onEvent(InviteUiEvent.OnChangeLinkClicked)
            viewModel.onEvent(InviteUiEvent.OnChangeLinkConfirmed)

            // When
            gate.complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf("group-1"), groups.rotatedInviteCodeGroupIds)
            assertEquals(NEW_LINK, state.link)
            assertEquals(qrCodeOf(NEW_LINK), state.qrCode)
            assertEquals(listOf(OLD_LINK, NEW_LINK), encodedTexts)
            assertFalse(state.isChangingLink)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the link being changed WHEN the server fails THEN keeps the old link and says it failed`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(
                rotateError = IllegalStateException("server down"),
                rotateGate = gate,
            )
            viewModel.onEvent(InviteUiEvent.OnChangeLinkClicked)
            viewModel.onEvent(InviteUiEvent.OnChangeLinkConfirmed)

            // When
            gate.complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertEquals(OLD_LINK, state.link)
            assertEquals(qrCodeOf(OLD_LINK), state.qrCode)
            assertEquals(listOf(OLD_LINK), encodedTexts)
            assertFalse(state.isChangingLink)
            assertFalse(state.isConfirmingLinkChange)
            assertEquals(listOf<InviteUiAction>(InviteUiAction.ShowChangeLinkFailed), actions)
        }

    @Test
    fun `GIVEN no confirmation open WHEN a confirmation arrives THEN does not change the link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(InviteUiEvent.OnChangeLinkConfirmed)

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isChangingLink)
            assertEquals(OLD_LINK, state.link)
            assertTrue(groups.rotatedInviteCodeGroupIds.isEmpty())
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the link being changed WHEN the confirmation arrives again THEN changes the link only once`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(rotateGate = gate)
            viewModel.onEvent(InviteUiEvent.OnChangeLinkClicked)
            viewModel.onEvent(InviteUiEvent.OnChangeLinkConfirmed)
            viewModel.onEvent(InviteUiEvent.OnChangeLinkConfirmed)

            // When
            gate.complete(Unit)

            // Then
            assertEquals(listOf("group-1"), groups.rotatedInviteCodeGroupIds)
        }

    @Test
    fun `GIVEN the confirmation open WHEN the connection drops THEN closes it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(InviteUiEvent.OnChangeLinkClicked)

        // When
        isOnline.value = false

        // Then
        assertFalse(viewModel.uiState.value.isConfirmingLinkChange)
    }

    @Test
    fun `GIVEN the confirmation closed by going offline WHEN the connection comes back THEN it stays closed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(InviteUiEvent.OnChangeLinkClicked)
            isOnline.value = false

            // When
            isOnline.value = true

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsChangeLink)
            assertFalse(state.isConfirmingLinkChange)
        }

    @Test
    fun `GIVEN the confirmation open WHEN the group is no longer created by this user THEN closes it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(InviteUiEvent.OnChangeLinkClicked)

            // When
            groups.groups.value = listOf(creatorsTrip.copy(isCreatedByMe = false))

            // Then
            assertFalse(viewModel.uiState.value.isConfirmingLinkChange)
        }

    @Test
    fun `GIVEN the confirmation open WHEN a member is renamed THEN it stays open`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(InviteUiEvent.OnChangeLinkClicked)

        // When
        groups.groups.value = listOf(creatorsTrip.copy(members = listOf(pierre, ana.copy(name = "Aninha"))))

        // Then
        assertTrue(viewModel.uiState.value.isConfirmingLinkChange)
    }

    @Test
    fun `GIVEN the old link just copied WHEN the change brings the new link THEN it no longer shows copied`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(InviteUiEvent.OnCopyClicked)
            viewModel.onEvent(InviteUiEvent.OnChangeLinkClicked)

            // When
            viewModel.onEvent(InviteUiEvent.OnChangeLinkConfirmed)

            // Then
            val state = viewModel.uiState.value
            assertEquals(NEW_LINK, state.link)
            assertFalse(state.isLinkCopied)
        }

    @Test
    fun `GIVEN the link just copied WHEN the group changes without a new link THEN it still shows copied`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(InviteUiEvent.OnCopyClicked)

            // When
            groups.groups.value = listOf(creatorsTrip.copy(members = listOf(pierre, ana.copy(name = "Aninha"))))

            // Then
            val state = viewModel.uiState.value
            assertEquals(OLD_LINK, state.link)
            assertTrue(state.isLinkCopied)
        }

    @Test
    fun `GIVEN the link just copied WHEN changing it fails THEN it still shows copied`() = runTest(testDispatcher) {
        // Given
        prepareScenario(rotateError = IllegalStateException("server down"))
        viewModel.onEvent(InviteUiEvent.OnCopyClicked)
        viewModel.onEvent(InviteUiEvent.OnChangeLinkClicked)

        // When
        viewModel.onEvent(InviteUiEvent.OnChangeLinkConfirmed)

        // Then
        val state = viewModel.uiState.value
        assertEquals(OLD_LINK, state.link)
        assertTrue(state.isLinkCopied)
    }

    private val pierre = GroupMember(
        "member-1",
        "Pierre",
        AvatarColor.BLUE,
        isMe = true,
        hasJoined = true,
        userId = null,
        inviteCode = null,
    )
    private val ana = GroupMember(
        "member-2",
        "Ana",
        AvatarColor.PEACH,
        isMe = false,
        hasJoined = true,
        userId = null,
        inviteCode = null,
    )

    // Why: a group this device's user created and that already synced, so it has a link to change.
    private val creatorsTrip = Group(
        id = "group-1",
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.BLUE,
        currency = GroupCurrency.BRL,
        members = listOf(pierre, ana),
        isPending = false,
        inviteCode = OLD_CODE,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
    )

    // Why: a QR code that differs per text, so a case can tell which link was encoded.
    private fun qrCodeOf(text: String): QrCode = QrCode(
        size = text.length,
        modules = text.map { it.isUpperCase() },
    )

    private fun TestScope.prepareScenario(
        storedGroup: Group = creatorsTrip,
        online: Boolean = true,
        isNewGroup: Boolean = false,
        rotateError: Throwable? = null,
        rotateGate: CompletableDeferred<Unit>? = null,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup)).also {
            it.nextInviteCode = NEW_CODE
            it.rotateInviteCodeError = rotateError
            it.rotateInviteCodeGate = rotateGate
        }
        isOnline = MutableStateFlow(online)
        val route = Route.Invite("group-1", isNewGroup = isNewGroup)
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            if (isNewGroup) {
                it.navigate(Route.CreateGroup)
                it.replaceTop(route)
            } else {
                it.navigate(
                    Route.GroupDetail(
                        "group-1",
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                    ),
                )
                it.navigate(route)
            }
        }
        viewModel = InviteViewModel(
            route = route,
            navigator = navigator,
            encodeQrCode = EncodeQrCode { text ->
                encodedTexts += text
                qrCodeOf(text)
            },
            retrySync = RetrySync { },
            changeInviteLink = ChangeInviteLink { groups.rotateInviteCode(it) },
            observeGroup = ObserveGroupUseCase(groups),
            observeSyncState = ObserveSyncState { MutableStateFlow(SyncState.SYNCED) },
            connectivityObserver = NetworkConnectivityObserver { isOnline },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val OLD_CODE = "7Hk2pQ8x"
        const val NEW_CODE = "Zx9Yw8Vu"
        const val OLD_LINK = "https://blitzsplit.com/g/$OLD_CODE"
        const val NEW_LINK = "https://blitzsplit.com/g/$NEW_CODE"
    }
}
