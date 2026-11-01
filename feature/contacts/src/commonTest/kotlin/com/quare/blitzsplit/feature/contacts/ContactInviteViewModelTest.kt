package com.quare.blitzsplit.feature.contacts

import com.quare.blitzsplit.core.contacts.testing.FakeHiddenContactRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.usecase.EncodeQrCode
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.QrCode
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactInviteUiAction
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactInviteUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactInviteViewModel
import kotlinx.coroutines.Dispatchers
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

class ContactInviteViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: ContactInviteViewModel
    private val actions = mutableListOf<ContactInviteUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN nobody in mind WHEN the invite opens THEN lists every group most recent first with the first marked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(FLORIPA, APE, FUTEBOL), state.groups.map { it.groupId })
            assertEquals(FLORIPA, state.selectedGroupId)
            assertEquals("Viagem Floripa", state.selectedGroup?.name)
            assertEquals("https://blitzsplit.com/g/Floripa1", state.link)
            assertEquals(qrCodeOf("https://blitzsplit.com/g/Floripa1"), state.qrCode)
            assertNull(state.name)
            assertFalse(state.isPersonal)
            assertFalse(state.isLinkPending)
        }

    @Test
    fun `GIVEN Helena from the agenda WHEN the invite opens THEN names her with the group link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(name = "Helena Souza")

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("Helena Souza", state.name)
            assertFalse(state.isPersonal)
            assertEquals("https://blitzsplit.com/g/Floripa1", state.link)
        }

    @Test
    fun `GIVEN the invite WHEN a group without its code yet is picked THEN the link is pending`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ContactInviteUiEvent.OnGroupPicked(FUTEBOL))

            // Then
            val state = viewModel.uiState.value
            assertEquals(FUTEBOL, state.selectedGroupId)
            assertNull(state.link)
            assertNull(state.qrCode)
            assertTrue(state.isLinkPending)
        }

    @Test
    fun `GIVEN a group with its link pending WHEN a sync brings its code THEN shows the link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactInviteUiEvent.OnGroupPicked(FUTEBOL))

            // When
            groups.groups.value = listOf(floripa(), ape(), futebol(inviteCode = "Futebol3"))

            // Then
            assertEquals(FUTEBOL, viewModel.uiState.value.selectedGroupId)
            assertEquals("https://blitzsplit.com/g/Futebol3", viewModel.uiState.value.link)
            assertFalse(viewModel.uiState.value.isLinkPending)
        }

    @Test
    fun `GIVEN the link copied WHEN another group is picked THEN switches link and QR and is no longer copied`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactInviteUiEvent.OnCopyClicked)

            // When
            viewModel.onEvent(ContactInviteUiEvent.OnGroupPicked(APE))

            // Then
            val state = viewModel.uiState.value
            assertEquals(APE, state.selectedGroupId)
            assertEquals("https://blitzsplit.com/g/ApeCode2", state.link)
            assertEquals(qrCodeOf("https://blitzsplit.com/g/ApeCode2"), state.qrCode)
            assertFalse(state.isLinkCopied)
        }

    @Test
    fun `GIVEN the invite WHEN a group not listed is picked THEN keeps the one marked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ContactInviteUiEvent.OnGroupPicked("gone"))

        // Then
        assertEquals(FLORIPA, viewModel.uiState.value.selectedGroupId)
    }

    @Test
    fun `GIVEN Ape 302 marked WHEN a sync deletes it THEN marks the first group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ContactInviteUiEvent.OnGroupPicked(APE))

        // When
        groups.groups.value = listOf(floripa(), futebol())

        // Then
        assertEquals(FLORIPA, viewModel.uiState.value.selectedGroupId)
        assertEquals("https://blitzsplit.com/g/Floripa1", viewModel.uiState.value.link)
    }

    @Test
    fun `GIVEN Diego a guest WHEN his invite opens THEN lists only his groups with his personal link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = DIEGO_KEY,
                name = "Diego",
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isPersonal)
            assertEquals(listOf(FLORIPA), state.groups.map { it.groupId })
            assertEquals("https://blitzsplit.com/g/Floripa1/diego1?from=pierr1", state.link)
            assertEquals(qrCodeOf("https://blitzsplit.com/g/Floripa1/diego1?from=pierr1"), state.qrCode)
            assertEquals("blitzsplit.com/g/Floripa1/diego1", state.displayLink)
        }

    @Test
    fun `GIVEN Diego in a group without its code yet WHEN his invite opens THEN his link is pending`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = DIEGO_KEY,
                name = "Diego",
                storedGroups = listOf(floripa(inviteCode = null)),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLinkPending)
            assertNull(state.qrCode)
        }

    @Test
    fun `GIVEN his invite open WHEN a sync takes Diego away THEN the invite closes`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            personKey = DIEGO_KEY,
            name = "Diego",
        )

        // When
        groups.groups.value = listOf(floripa(withDiego = false), ape(), futebol())

        // Then
        assertEquals(
            listOf(Route.Home, Route.Contacts, Route.ContactProfile(DIEGO_KEY)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the invite WHEN Novo grupo is tapped THEN Novo grupo takes its place`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ContactInviteUiEvent.OnNewGroupClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Contacts, Route.CreateGroup), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the invite already closed WHEN a late Novo grupo arrives THEN changes nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigateBack()

            // When
            viewModel.onEvent(ContactInviteUiEvent.OnNewGroupClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a link WHEN Copiar is tapped THEN copies it and shows copied`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ContactInviteUiEvent.OnCopyClicked)

        // Then
        assertEquals(
            listOf<ContactInviteUiAction>(ContactInviteUiAction.CopyLink("https://blitzsplit.com/g/Floripa1")),
            actions,
        )
        assertTrue(viewModel.uiState.value.isLinkCopied)
    }

    @Test
    fun `GIVEN the link just copied WHEN the feedback time passes THEN no longer shows copied`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactInviteUiEvent.OnCopyClicked)
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
    fun `GIVEN the link copied 1500 ms ago WHEN Copiar is tapped again THEN shows copied 2 s from the second tap`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactInviteUiEvent.OnCopyClicked)
            advanceTimeBy(1_500.milliseconds)
            runCurrent()

            // When
            viewModel.onEvent(ContactInviteUiEvent.OnCopyClicked)

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
    fun `GIVEN the link pending WHEN Copiar is tapped THEN copies nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ContactInviteUiEvent.OnGroupPicked(FUTEBOL))

        // When
        viewModel.onEvent(ContactInviteUiEvent.OnCopyClicked)

        // Then
        assertTrue(actions.isEmpty())
        assertFalse(viewModel.uiState.value.isLinkCopied)
    }

    @Test
    fun `GIVEN the link pending WHEN Compartilhar is tapped THEN shares nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ContactInviteUiEvent.OnGroupPicked(FUTEBOL))

        // When
        viewModel.onEvent(ContactInviteUiEvent.OnShareClicked)

        // Then
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN Helena from the agenda WHEN Compartilhar is tapped THEN shares the link with the group and her name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(name = "Helena Souza")

            // When
            viewModel.onEvent(ContactInviteUiEvent.OnShareClicked)

            // Then
            assertEquals(
                listOf<ContactInviteUiAction>(
                    ContactInviteUiAction.ShareLink(
                        link = "https://blitzsplit.com/g/Floripa1",
                        groupName = "Viagem Floripa",
                        name = "Helena Souza",
                    ),
                ),
                actions,
            )
        }

    @Test
    fun `GIVEN a link and no internet WHEN the invite opens THEN says the link works offline`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsOfflineNotice)
        }

    @Test
    fun `GIVEN a pending link and no internet WHEN the invite opens THEN shows no offline notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                storedGroups = listOf(futebol()),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLinkPending)
            assertFalse(state.showsOfflineNotice)
        }

    @Test
    fun `GIVEN no group WHEN the invite opens THEN offers only Novo grupo`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = emptyList())

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.hasNoGroups)
        assertNull(state.selectedGroupId)
        assertFalse(state.isLinkPending)
    }

    @Test
    fun `GIVEN the invite on top WHEN it is closed THEN goes back`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ContactInviteUiEvent.OnCloseClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the invite already closed by a native back WHEN close arrives THEN changes nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigateBack()

            // When
            viewModel.onEvent(ContactInviteUiEvent.OnCloseClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
        }

    // Why: a QR code that differs per text, so a case can tell which link was encoded.
    private fun qrCodeOf(text: String): QrCode = QrCode(
        size = text.length,
        modules = text.map { it.isUpperCase() },
    )

    private fun TestScope.prepareScenario(
        personKey: String? = null,
        name: String? = null,
        storedGroups: List<Group> = defaultGroups(),
        online: Boolean = true,
    ) {
        groups = FakeGroupRepository(initialGroups = storedGroups)
        val route = Route.ContactInvite(
            personKey = personKey,
            name = name,
        )
        navigator = BackStackNavigator(startRoute = Route.Home).also {
            it.navigate(Route.Contacts)
            if (personKey != null) it.navigate(Route.ContactProfile(personKey))
            it.navigate(route)
        }
        viewModel = ContactInviteViewModel(
            route = route,
            navigator = navigator,
            encodeQrCode = EncodeQrCode(::qrCodeOf),
            observeContactBook = observeContactBookOf(
                groups,
                FakeExpenseRepository(initialExpenses = defaultBills()),
                FakeSettlementRepository(),
                FakeHiddenContactRepository(),
            ),
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(online) },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
