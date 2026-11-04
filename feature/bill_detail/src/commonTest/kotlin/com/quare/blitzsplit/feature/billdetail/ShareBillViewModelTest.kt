package com.quare.blitzsplit.feature.billdetail

import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.expenses.domain.model.BillShareLink
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DisableBillShareLinkUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.LoadBillShareLinkUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SetBillShareLinkUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeBillShareLinkRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.billdetail.presentation.model.ShareBillError
import com.quare.blitzsplit.feature.billdetail.presentation.model.ShareBillUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.ShareBillUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.ShareBillViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
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

class ShareBillViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var links: FakeBillShareLinkRepository
    private lateinit var navigator: BackStackNavigator
    private val isOnline = MutableStateFlow(true)
    private lateinit var viewModel: ShareBillViewModel
    private val actions = mutableListOf<ShareBillUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN a bill on the device WHEN the sheet opens THEN shows its description and total`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("Jantar no Ostradamus", state.description)
            assertEquals(64_200, state.totalMinor)
            assertEquals(GroupCurrency.USD, state.currency)
        }

    @Test
    fun `GIVEN Bruno never joined WHEN the sheet opens THEN names whoever joined but the device user`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf("Ana", "Carla"), state.memberNames)
        }

    @Test
    fun `GIVEN a link anyone opens WHEN the sheet opens online THEN shows that link as it is`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedLinks = mapOf(EXPENSE_ID to BillShareLink("Ab12Cd34", BillLinkVisibility.ANYONE)))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("https://blitzsplit.com/c/Ab12Cd34", state.link)
            assertEquals("blitzsplit.com/c/Ab12Cd34", state.displayLink)
            assertEquals(BillLinkVisibility.ANYONE, state.visibility)
            assertEquals(listOf(EXPENSE_ID), links.loadedLinkIds)
            assertTrue(links.updatedLinks.isEmpty())
            assertFalse(state.isLoading)
            assertFalse(state.hasFailed)
        }

    @Test
    fun `GIVEN no link yet WHEN the sheet opens online THEN makes one only whoever is in the bill opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(EXPENSE_ID to BillLinkVisibility.MEMBERS), links.updatedLinks)
            assertEquals("https://blitzsplit.com/c/${FakeBillShareLinkRepository.NEW_CODE}", state.link)
            assertEquals(BillLinkVisibility.MEMBERS, state.visibility)
            assertTrue(state.canSendLink)
            assertTrue(state.canChangeLink)
        }

    @Test
    fun `GIVEN the server not answering yet WHEN the sheet opens THEN it is loading and nothing can be sent`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(loadGate = CompletableDeferred())

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoading)
            assertNull(state.link)
            assertFalse(state.canSendLink)
            assertFalse(state.canChangeLink)
        }

    @Test
    fun `GIVEN the server cannot read the link WHEN the sheet opens THEN fails without making a link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(getLinkError = IllegalStateException("offline"))

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.hasFailed)
            assertFalse(state.isLoading)
            assertNull(state.link)
            assertTrue(links.updatedLinks.isEmpty())
        }

    @Test
    fun `GIVEN the server cannot make the link WHEN the sheet opens THEN says it failed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(updateError = IllegalStateException("server"))

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.hasFailed)
        assertFalse(state.isLoading)
        assertNull(state.link)
    }

    @Test
    fun `GIVEN the bill is not on the server yet WHEN the sheet opens THEN says it failed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isGoneFromServer = true)

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.hasFailed)
        assertNull(state.link)
    }

    @Test
    fun `GIVEN the link failed to load WHEN tapping Tentar de novo THEN loads it again and shows it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(getLinkError = IllegalStateException("server"))
            links.getLinkError = null

            // When
            viewModel.onEvent(ShareBillUiEvent.OnRetryClicked)

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf(EXPENSE_ID, EXPENSE_ID), links.loadedLinkIds)
            assertFalse(state.hasFailed)
            assertEquals("https://blitzsplit.com/c/${FakeBillShareLinkRepository.NEW_CODE}", state.link)
        }

    @Test
    fun `GIVEN a failed load and no internet WHEN tapping Tentar de novo THEN asks the server nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(getLinkError = IllegalStateException("server"))
            isOnline.value = false

            // When
            viewModel.onEvent(ShareBillUiEvent.OnRetryClicked)

            // Then
            assertEquals(listOf(EXPENSE_ID), links.loadedLinkIds)
            assertTrue(viewModel.uiState.value.hasFailed)
        }

    @Test
    fun `GIVEN no internet WHEN the sheet opens THEN says it is offline and asks the server nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsOfflineNotice)
            assertFalse(state.isLoading)
            assertFalse(state.canSendLink)
            assertFalse(state.canChangeLink)
            assertTrue(links.loadedLinkIds.isEmpty())
            assertTrue(links.updatedLinks.isEmpty())
        }

    @Test
    fun `GIVEN the connectivity not known yet WHEN the sheet opens THEN it is loading rather than without a link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsConnectivity = true)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoading)
            assertFalse(state.hasFailed)
            assertNull(state.link)
            assertTrue(links.loadedLinkIds.isEmpty())
        }

    @Test
    fun `GIVEN the link being read WHEN the internet goes THEN keeps loading until the read ends`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(loadGate = gate)

            // When
            isOnline.value = false
            val whileReading = viewModel.uiState.value
            gate.complete(Unit)

            // Then
            assertTrue(whileReading.isLoading)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `GIVEN the sheet open offline WHEN the internet comes back THEN loads the link`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        isOnline.value = true

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showsOfflineNotice)
        assertEquals(listOf(EXPENSE_ID), links.loadedLinkIds)
        assertEquals("https://blitzsplit.com/c/${FakeBillShareLinkRepository.NEW_CODE}", state.link)
    }

    @Test
    fun `GIVEN the link shown WHEN the internet goes and comes back THEN reads it only once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            isOnline.value = false

            // When
            isOnline.value = true

            // Then
            assertEquals(listOf(EXPENSE_ID), links.loadedLinkIds)
            assertEquals(1, links.updatedLinks.size)
        }

    @Test
    fun `GIVEN the link shown WHEN the internet goes THEN it can still be sent but not changed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            isOnline.value = false

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsOfflineNotice)
            assertTrue(state.canSendLink)
            assertFalse(state.canChangeLink)
        }

    @Test
    fun `GIVEN the link shown offline WHEN tapping Copiar THEN copies it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        isOnline.value = false

        // When
        viewModel.onEvent(ShareBillUiEvent.OnCopyClicked)

        // Then
        assertEquals(
            listOf<ShareBillUiAction>(
                ShareBillUiAction.CopyLink("https://blitzsplit.com/c/${FakeBillShareLinkRepository.NEW_CODE}"),
            ),
            actions,
        )
    }

    @Test
    fun `GIVEN a link for the bill members WHEN picking anyone with the link THEN the same link opens to anyone`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ShareBillUiEvent.OnVisibilityPicked(BillLinkVisibility.ANYONE))

            // Then
            val state = viewModel.uiState.value
            assertEquals(BillLinkVisibility.ANYONE, state.visibility)
            assertEquals(EXPENSE_ID to BillLinkVisibility.ANYONE, links.updatedLinks.last())
            assertEquals("https://blitzsplit.com/c/${FakeBillShareLinkRepository.NEW_CODE}", state.link)
            assertFalse(state.isUpdating)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the server not answering yet WHEN picking anyone with the link THEN shows it and blocks changes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            links.updateGate = CompletableDeferred()

            // When
            viewModel.onEvent(ShareBillUiEvent.OnVisibilityPicked(BillLinkVisibility.ANYONE))

            // Then
            val state = viewModel.uiState.value
            assertEquals(BillLinkVisibility.ANYONE, state.visibility)
            assertTrue(state.isUpdating)
            assertFalse(state.canChangeLink)
        }

    @Test
    fun `GIVEN a change on its way WHEN picking again THEN asks the server nothing more`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        links.updateGate = CompletableDeferred()
        viewModel.onEvent(ShareBillUiEvent.OnVisibilityPicked(BillLinkVisibility.ANYONE))

        // When
        viewModel.onEvent(ShareBillUiEvent.OnVisibilityPicked(BillLinkVisibility.MEMBERS))

        // Then
        assertEquals(2, links.updatedLinks.size)
    }

    @Test
    fun `GIVEN the server cannot change the link WHEN picking anyone THEN goes back and says it failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            links.updateError = IllegalStateException("server")

            // When
            viewModel.onEvent(ShareBillUiEvent.OnVisibilityPicked(BillLinkVisibility.ANYONE))

            // Then
            val state = viewModel.uiState.value
            assertEquals(BillLinkVisibility.MEMBERS, state.visibility)
            assertFalse(state.isUpdating)
            assertEquals(listOf<ShareBillUiAction>(ShareBillUiAction.ShowError(ShareBillError.UPDATE_FAILED)), actions)
        }

    @Test
    fun `GIVEN the bill left the server WHEN picking anyone THEN goes back and says it failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            links.goneExpenseIds += EXPENSE_ID

            // When
            viewModel.onEvent(ShareBillUiEvent.OnVisibilityPicked(BillLinkVisibility.ANYONE))

            // Then
            assertEquals(BillLinkVisibility.MEMBERS, viewModel.uiState.value.visibility)
            assertEquals(listOf<ShareBillUiAction>(ShareBillUiAction.ShowError(ShareBillError.UPDATE_FAILED)), actions)
        }

    @Test
    fun `GIVEN a link for the bill members WHEN picking that again THEN asks the server nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ShareBillUiEvent.OnVisibilityPicked(BillLinkVisibility.MEMBERS))

            // Then
            assertEquals(1, links.updatedLinks.size)
            assertFalse(viewModel.uiState.value.isUpdating)
        }

    @Test
    fun `GIVEN the link shown offline WHEN picking anyone with the link THEN nothing changes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            isOnline.value = false

            // When
            viewModel.onEvent(ShareBillUiEvent.OnVisibilityPicked(BillLinkVisibility.ANYONE))

            // Then
            assertEquals(BillLinkVisibility.MEMBERS, viewModel.uiState.value.visibility)
            assertEquals(1, links.updatedLinks.size)
        }

    @Test
    fun `GIVEN the link shown WHEN tapping Copiar THEN copies it and shows copied`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ShareBillUiEvent.OnCopyClicked)

        // Then
        assertEquals(
            listOf<ShareBillUiAction>(
                ShareBillUiAction.CopyLink("https://blitzsplit.com/c/${FakeBillShareLinkRepository.NEW_CODE}"),
            ),
            actions,
        )
        assertTrue(viewModel.uiState.value.isLinkCopied)
    }

    @Test
    fun `GIVEN the link just copied WHEN the feedback time passes THEN no longer shows copied`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ShareBillUiEvent.OnCopyClicked)
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
    fun `GIVEN the link not known yet WHEN tapping Copiar THEN nothing is copied`() = runTest(testDispatcher) {
        // Given
        prepareScenario(loadGate = CompletableDeferred())

        // When
        viewModel.onEvent(ShareBillUiEvent.OnCopyClicked)

        // Then
        assertTrue(actions.isEmpty())
        assertFalse(viewModel.uiState.value.isLinkCopied)
    }

    @Test
    fun `GIVEN the link not known yet WHEN tapping Enviar link THEN nothing is sent`() = runTest(testDispatcher) {
        // Given
        prepareScenario(loadGate = CompletableDeferred())

        // When
        viewModel.onEvent(ShareBillUiEvent.OnSendClicked)

        // Then
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN the link shown WHEN tapping Enviar link THEN sends it with the bill description`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ShareBillUiEvent.OnSendClicked)

            // Then
            assertEquals(
                listOf<ShareBillUiAction>(
                    ShareBillUiAction.SendLink(
                        link = "https://blitzsplit.com/c/${FakeBillShareLinkRepository.NEW_CODE}",
                        description = "Jantar no Ostradamus",
                    ),
                ),
                actions,
            )
        }

    @Test
    fun `GIVEN the link shown WHEN tapping Desativar link THEN turns it off and closes the sheet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ShareBillUiEvent.OnDisableClicked)

            // Then
            assertEquals(listOf(EXPENSE_ID), links.disabledExpenseIds)
            assertEquals(listOf(Route.Groups, groupRoute, billRoute), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the server cannot turn the link off WHEN tapping Desativar link THEN stays saying it failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            links.disableError = IllegalStateException("server")

            // When
            viewModel.onEvent(ShareBillUiEvent.OnDisableClicked)

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf(Route.Groups, groupRoute, billRoute, route), navigator.backStack.value)
            assertEquals(listOf<ShareBillUiAction>(ShareBillUiAction.ShowError(ShareBillError.DISABLE_FAILED)), actions)
            assertFalse(state.isUpdating)
            assertTrue(state.canChangeLink)
            assertEquals("https://blitzsplit.com/c/${FakeBillShareLinkRepository.NEW_CODE}", state.link)
        }

    @Test
    fun `GIVEN the link shown offline WHEN tapping Desativar link THEN asks the server nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            isOnline.value = false

            // When
            viewModel.onEvent(ShareBillUiEvent.OnDisableClicked)

            // Then
            assertTrue(links.disabledExpenseIds.isEmpty())
            assertEquals(listOf(Route.Groups, groupRoute, billRoute, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the sheet open WHEN closing it THEN returns to the bill`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ShareBillUiEvent.OnCloseClicked)

        // Then
        assertEquals(listOf(Route.Groups, groupRoute, billRoute), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a native back already closed the sheet WHEN closing it THEN the bill stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigateBack()

            // When
            viewModel.onEvent(ShareBillUiEvent.OnCloseClicked)

            // Then
            assertEquals(listOf(Route.Groups, groupRoute, billRoute), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the sheet open WHEN the bill leaves the device THEN closes the sheet`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        expenses.expenses.value = emptyList()

        // Then
        assertEquals(listOf(Route.Groups, groupRoute, billRoute), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the sheet open WHEN the group leaves the device THEN closes the sheet`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        groups.groups.value = emptyList()

        // Then
        assertEquals(listOf(Route.Groups, groupRoute, billRoute), navigator.backStack.value)
    }

    private val route = Route.ShareBill(
        groupId = GROUP_ID,
        expenseId = EXPENSE_ID,
    )
    private val billRoute = Route.BillDetail(
        groupId = GROUP_ID,
        expenseId = EXPENSE_ID,
        focusCommentId = null,
    )
    private val groupRoute = Route.GroupDetail(
        GROUP_ID,
        joinedAs = null,
        tab = GroupTab.EXPENSES,
    )

    private val trip = Group(
        id = GROUP_ID,
        name = "Viagem de Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.LILAC,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.USD,
        members = listOf(
            member(ANA, "Ana", hasJoined = true),
            member(
                PIERRE,
                "Pierre",
                isMe = true,
                hasJoined = true,
            ),
            member(BRUNO, "Bruno"),
            member(CARLA, "Carla", hasJoined = true),
        ),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    private fun member(
        id: String,
        name: String,
        isMe: Boolean = false,
        hasJoined: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.PEACH,
        isMe = isMe,
        hasJoined = hasJoined,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    private val expense = Expense(
        id = EXPENSE_ID,
        groupId = GROUP_ID,
        description = "Jantar no Ostradamus",
        amountMinor = 64_200,
        splitMode = SplitMode.EQUAL,
        payers = listOf(
            ExpensePayer(
                memberId = ANA,
                amountMinor = 64_200,
            ),
        ),
        shares = listOf(
            ExpenseShare(
                memberId = ANA,
                amountMinor = 32_100,
                weight = 1,
            ),
            ExpenseShare(
                memberId = PIERRE,
                amountMinor = 32_100,
                weight = 1,
            ),
        ),
        items = null,
        categoryId = null,
        spentAt = 0L,
        isPending = false,
        updatedAt = 0L,
        updatedByMemberId = null,
    )

    private fun TestScope.prepareScenario(
        online: Boolean = true,
        storedLinks: Map<String, BillShareLink> = emptyMap(),
        getLinkError: Throwable? = null,
        updateError: Throwable? = null,
        isGoneFromServer: Boolean = false,
        loadGate: CompletableDeferred<Unit>? = null,
        holdsConnectivity: Boolean = false,
    ) {
        isOnline.value = online
        groups = FakeGroupRepository(initialGroups = listOf(trip))
        expenses = FakeExpenseRepository(initialExpenses = listOf(expense))
        links = FakeBillShareLinkRepository(initialLinks = storedLinks).also {
            it.getLinkError = getLinkError
            it.updateError = updateError
            it.loadGate = loadGate
            if (isGoneFromServer) it.goneExpenseIds += EXPENSE_ID
        }
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(groupRoute)
            it.navigate(billRoute)
            it.navigate(route)
        }
        viewModel = ShareBillViewModel(
            route = route,
            navigator = navigator,
            loadBillShareLink = LoadBillShareLinkUseCase(links),
            setBillShareLink = SetBillShareLinkUseCase(links),
            disableBillShareLink = DisableBillShareLinkUseCase(links),
            observeGroup = ObserveGroupUseCase(groups),
            observeExpense = ObserveExpenseUseCase(expenses),
            connectivityObserver = NetworkConnectivityObserver { if (holdsConnectivity) emptyFlow() else isOnline },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val EXPENSE_ID = "expense-1"
        const val ANA = "member-1"
        const val PIERRE = "member-2"
        const val BRUNO = "member-3"
        const val CARLA = "member-4"
    }
}
