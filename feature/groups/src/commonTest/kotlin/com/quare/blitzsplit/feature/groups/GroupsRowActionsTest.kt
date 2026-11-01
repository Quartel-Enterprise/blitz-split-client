package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.domain.usecase.ObserveActivityUnreadCount
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.DebtKind
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitDialog
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteSettlementUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsMoneyUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.RecordSettlementUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.data.repository.InMemoryGroupDeletionRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupExitOutcome
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeGroupDeletionUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupDeletionsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.GroupsLayout
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveGroupsLayout
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateGroupsLayout
import com.quare.blitzsplit.core.sync.domain.usecase.DeleteGroup
import com.quare.blitzsplit.core.sync.domain.usecase.LeaveGroup
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.GroupsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Why: flows 5.3 and 5.4, "Convidar pessoas", "Sair do grupo" and "Apagar grupo" from a row of the home.
class GroupsRowActionsTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: GroupsViewModel
    private lateinit var recordedPayments: InMemoryRecordedPaymentRepository
    private val leftGroupIds = mutableListOf<String>()
    private val deletedGroupIds = mutableListOf<String>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN a group WHEN tapping Convidar pessoas on its row THEN opens its invite as an old group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(GroupsUiEvent.OnInviteClicked("trio"))

            // Then
            assertEquals(listOf(Route.Groups, Route.Invite("trio", isNewGroup = false)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN another screen over the home WHEN tapping Convidar pessoas THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(GroupsUiEvent.OnInviteClicked("trio"))

            // Then
            assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN you owe in a group WHEN reading its row THEN hints what to settle first and that others owe`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(anaPaidForEveryone))

            // When
            val row = viewModel.uiState.value.groups
                .single()

            // Then
            assertEquals(10000L, row.settleBeforeLeavingMinor)
            assertTrue(row.hasOpenDebts)
        }

    @Test
    fun `GIVEN a group without bills WHEN reading its row THEN nothing blocks leaving nor deleting`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val row = viewModel.uiState.value.groups
                .single()

            // Then
            assertEquals(0L, row.settleBeforeLeavingMinor)
            assertFalse(row.hasOpenDebts)
        }

    @Test
    fun `GIVEN you owe in a group WHEN tapping Sair do grupo on its row THEN says to settle first with Acertar`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(anaPaidForEveryone))

            // When
            viewModel.onEvent(GroupsUiEvent.OnLeaveGroupClicked("trio"))

            // Then
            val state = viewModel.uiState.value
            val dialog = assertIs<GroupExitDialog.LeaveBlocked>(state.exit.dialog)
            assertEquals("Apê 302", dialog.groupName)
            assertTrue(dialog.canSettle)
            assertEquals(listOf(DebtKind.YOU_OWE), dialog.debts.map { it.kind })
            assertEquals("trio", state.exitGroup?.id)
        }

    @Test
    fun `GIVEN only others owe WHEN tapping Sair do grupo on a row THEN asks to leave naming the other members`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(anaPaidForEveryone),
                storedSettlements = listOf(pierrePaidAna),
            )

            // When
            viewModel.onEvent(GroupsUiEvent.OnLeaveGroupClicked("trio"))

            // Then
            assertEquals(
                GroupExitDialog.ConfirmLeave(
                    groupName = "Apê 302",
                    otherMemberCount = 2,
                    deletesGroup = true,
                ),
                viewModel.uiState.value.exit.dialog,
            )
        }

    @Test
    fun `GIVEN a settled balance WHEN confirming Sair THEN leaves through the server and stays on the home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnLeaveGroupClicked("trio"))

            // When
            viewModel.onEvent(GroupsUiEvent.OnGroupExitConfirmed)

            // Then
            assertEquals(listOf("trio"), leftGroupIds)
            assertNull(viewModel.uiState.value.exit.dialog)
            assertFalse(viewModel.uiState.value.exit.isRunning)
            assertEquals(listOf(Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN somebody owes WHEN tapping Apagar grupo on a row THEN lists every pair still open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(anaPaidForEveryone))

            // When
            viewModel.onEvent(GroupsUiEvent.OnDeleteGroupClicked("trio"))

            // Then
            val dialog = assertIs<GroupExitDialog.DeleteBlocked>(viewModel.uiState.value.exit.dialog)
            assertEquals(
                setOf(DebtKind.YOU_OWE, DebtKind.BETWEEN_OTHERS),
                dialog.debts.map { it.kind }.toSet(),
            )
        }

    @Test
    fun `GIVEN everyone settled WHEN confirming Apagar grupo THEN deletes it on the server and stays home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupsUiEvent.OnDeleteGroupClicked("trio"))

            // When
            viewModel.onEvent(GroupsUiEvent.OnGroupExitConfirmed)

            // Then
            assertEquals(listOf("trio"), deletedGroupIds)
            assertNull(viewModel.uiState.value.exit.dialog)
            assertEquals(listOf(Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN offline WHEN tapping Sair or Apagar on a row THEN opens no dialog`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        viewModel.onEvent(GroupsUiEvent.OnLeaveGroupClicked("trio"))
        viewModel.onEvent(GroupsUiEvent.OnDeleteGroupClicked("trio"))

        // Then
        assertFalse(viewModel.uiState.value.exit.canLeaveOrDelete)
        assertNull(viewModel.uiState.value.exit.dialog)
    }

    @Test
    fun `GIVEN Acerte antes de sair WHEN tapping Acertar THEN opens Registrar pagamento over the home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(anaPaidForEveryone))
            viewModel.onEvent(GroupsUiEvent.OnLeaveGroupClicked("trio"))

            // When
            viewModel.onEvent(GroupsUiEvent.OnGroupExitConfirmed)

            // Then
            assertNull(viewModel.uiState.value.exit.dialog)
            assertEquals(
                listOf(Route.Groups, Route.RegisterPayment("trio", "member-Pierre", "member-Ana")),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Ainda ha saldo em aberto WHEN tapping Ver saldos THEN opens the group Saldos over the home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(anaPaidForEveryone))
            viewModel.onEvent(GroupsUiEvent.OnDeleteGroupClicked("trio"))

            // When
            viewModel.onEvent(GroupsUiEvent.OnGroupExitConfirmed)

            // Then
            assertEquals(listOf(Route.Groups, Route.Balances("trio")), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the server cannot be reached WHEN confirming Apagar grupo THEN closes the dialog and says so`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(deleteResult = Result.failure(IllegalStateException("offline")))
            viewModel.onEvent(GroupsUiEvent.OnDeleteGroupClicked("trio"))
            var action: GroupsUiAction? = null
            backgroundScope.launch { action = viewModel.uiAction.first() }

            // When
            viewModel.onEvent(GroupsUiEvent.OnGroupExitConfirmed)

            // Then
            assertEquals(GroupsUiAction.ShowDeleteError, action)
            assertNull(viewModel.uiState.value.exit.dialog)
        }

    @Test
    fun `GIVEN Acerte antes de sair open WHEN a sync brings the payment THEN lists nothing left`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(anaPaidForEveryone))
            viewModel.onEvent(GroupsUiEvent.OnLeaveGroupClicked("trio"))

            // When
            settlements.settlements.value = listOf(pierrePaidAna)

            // Then
            val dialog = assertIs<GroupExitDialog.LeaveBlocked>(viewModel.uiState.value.exit.dialog)
            assertTrue(dialog.debts.isEmpty())
        }

    @Test
    fun `GIVEN a dialog open WHEN its group leaves the device THEN closes it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpenses = listOf(anaPaidForEveryone))
        viewModel.onEvent(GroupsUiEvent.OnDeleteGroupClicked("trio"))

        // When
        groups.groups.value = emptyList()

        // Then
        assertNull(viewModel.uiState.value.exit.dialog)
    }

    @Test
    fun `GIVEN a dialog open WHEN dismissing it THEN closes it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupsUiEvent.OnLeaveGroupClicked("trio"))

        // When
        viewModel.onEvent(GroupsUiEvent.OnGroupExitDismissed)

        // Then
        assertNull(viewModel.uiState.value.exit.dialog)
    }

    @Test
    fun `GIVEN Acertar from a row WHEN Registrar pagamento closes after recording THEN the home shows the notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(anaPaidForEveryone))
            settleFromTheHome()
            val settlementId = recordPierrePaidAna()

            // When
            navigator.navigateBack()

            // Then
            assertEquals(
                PaymentNotice(
                    settlementIds = listOf(settlementId),
                    fromName = "Pierre",
                    fromIsMe = true,
                    toName = "Ana",
                ),
                viewModel.uiState.value.recordedPayment,
            )
            // Why: consumed, so the group's screens never show it later.
            assertNull(recordedPayments.observeRecorded().first())
        }

    @Test
    fun `GIVEN Registrar pagamento still over the home WHEN a payment is recorded THEN the notice waits`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(anaPaidForEveryone))
            settleFromTheHome()

            // When
            val settlementId = recordPierrePaidAna()

            // Then
            assertNull(viewModel.uiState.value.recordedPayment)
            assertEquals(
                settlementId,
                recordedPayments
                    .observeRecorded()
                    .first()
                    ?.settlementIds
                    ?.single(),
            )
        }

    @Test
    fun `GIVEN the payment notice on the home WHEN tapping Desfazer THEN deletes that payment`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(anaPaidForEveryone))
            settleFromTheHome()
            val settlementId = recordPierrePaidAna()
            navigator.navigateBack()

            // When
            viewModel.onEvent(GroupsUiEvent.OnUndoPaymentClicked)

            // Then
            assertEquals(listOf(settlementId), settlements.deletedIds)
            assertNull(viewModel.uiState.value.recordedPayment)
        }

    @Test
    fun `GIVEN the device fails to delete WHEN tapping Desfazer on the home THEN says so`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpenses = listOf(anaPaidForEveryone))
        settleFromTheHome()
        recordPierrePaidAna()
        navigator.navigateBack()
        settlements.deleteError = IllegalStateException("disk full")
        var action: GroupsUiAction? = null
        backgroundScope.launch { action = viewModel.uiAction.first() }

        // When
        viewModel.onEvent(GroupsUiEvent.OnUndoPaymentClicked)

        // Then
        assertEquals(GroupsUiAction.ShowChangeError, action)
    }

    @Test
    fun `GIVEN the payment notice on the home WHEN it goes away THEN clears it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpenses = listOf(anaPaidForEveryone))
        settleFromTheHome()
        recordPierrePaidAna()
        navigator.navigateBack()

        // When
        viewModel.onEvent(GroupsUiEvent.OnRecordedPaymentNoticeShown)

        // Then
        assertNull(viewModel.uiState.value.recordedPayment)
    }

    // Why: "Sair do grupo" says to settle first, and its "Acertar" opens Registrar pagamento over the home.
    private fun settleFromTheHome() {
        viewModel.onEvent(GroupsUiEvent.OnLeaveGroupClicked("trio"))
        viewModel.onEvent(GroupsUiEvent.OnGroupExitConfirmed)
    }

    private suspend fun recordPierrePaidAna(): String =
        RecordSettlementUseCase(settlements, recordedPayments) { 1_000L }
            .invoke(
                groupId = "trio",
                fromMemberId = "member-Pierre",
                toMemberId = "member-Ana",
                amountMinor = 10000L,
                method = SettlementMethod.PIX,
                paidAt = 1_000L,
            ).getOrThrow()

    private val trio = Group(
        id = "trio",
        name = "Apê 302",
        type = GroupType.HOME,
        color = AvatarColor.BLUE,
        currency = GroupCurrency.BRL,
        members = listOf(
            member(
                name = "Pierre",
                isMe = true,
            ),
            member(name = "Ana"),
            member(name = "Bruno"),
        ),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
    )

    // Why: Pierre and Bruno each owe Ana R$ 100,00.
    private val anaPaidForEveryone = Expense(
        id = "market",
        groupId = "trio",
        description = "Mercado",
        amountMinor = 30000L,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer("member-Ana", 30000L)),
        shares = listOf("Pierre", "Ana", "Bruno").map { ExpenseShare("member-$it", 10000L, 1L) },
        spentAt = 0L,
        isPending = false,
        updatedAt = 0L,
        updatedByMemberId = null,
    )

    private val pierrePaidAna = Settlement(
        id = "pierre-ana",
        groupId = "trio",
        fromMemberId = "member-Pierre",
        toMemberId = "member-Ana",
        amountMinor = 10000L,
        method = SettlementMethod.PIX,
        paidAt = 0L,
        isPending = false,
        updatedAt = 0L,
    )

    private fun member(
        name: String,
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = "member-$name",
        name = name,
        avatarColor = AvatarColor.BLUE,
        isMe = isMe,
        hasJoined = false,
        userId = null,
        inviteCode = null,
    )

    private fun prepareScenario(
        online: Boolean = true,
        storedExpenses: List<Expense> = emptyList(),
        storedSettlements: List<Settlement> = emptyList(),
        deleteResult: Result<GroupExitOutcome> = Result.success(GroupExitOutcome.DONE),
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(trio))
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        isOnline = MutableStateFlow(online)
        navigator = BackStackNavigator(startRoute = Route.Groups)
        leftGroupIds.clear()
        deletedGroupIds.clear()
        val deletions = InMemoryGroupDeletionRepository()
        recordedPayments = InMemoryRecordedPaymentRepository()
        viewModel = GroupsViewModel(
            observeProfile = ObserveProfileUseCase(
                FakeProfileRepository(UserProfile("Pierre", AvatarColor.GREEN, null)),
            ),
            observeAccount = ObserveAccountUseCase(FakeAccountRepository()),
            observeSyncState = ObserveSyncState { MutableStateFlow(SyncState.SYNCED) },
            observeGroups = ObserveGroupsUseCase(groups),
            connectivityObserver = NetworkConnectivityObserver { isOnline },
            retrySync = RetrySync {},
            syncNow = SyncNow { Result.success(Unit) },
            observeGroupsMoney = ObserveGroupsMoneyUseCase(expenses, settlements),
            currentTimeMillis = CurrentTimeMillis { 0L },
            currentTimeZone = CurrentTimeZone { TimeZone.UTC },
            minuteTicks = MinuteTicks { flowOf(Unit) },
            navigator = navigator,
            observeGroupDeletions = ObserveGroupDeletionsUseCase(deletions),
            consumeGroupDeletion = ConsumeGroupDeletionUseCase(deletions),
            leaveGroup = LeaveGroup { groupId ->
                leftGroupIds += groupId
                Result.success(GroupExitOutcome.DONE)
            },
            deleteGroup = DeleteGroup { groupId ->
                deletedGroupIds += groupId
                deleteResult
            },
            observeGroupsLayout = ObserveGroupsLayout { flowOf(GroupsLayout.initial) },
            updateGroupsLayout = UpdateGroupsLayout {},
            observeRecordedPayment = ObserveRecordedPaymentUseCase(recordedPayments),
            consumeRecordedPayment = ConsumeRecordedPaymentUseCase(recordedPayments),
            deleteSettlement = DeleteSettlementUseCase(settlements, recordedPayments) { 1_000L },
            observePeopleBalances = { emptyFlow() },
            observeActivityUnreadCount = ObserveActivityUnreadCount { flowOf(0) },
        )
    }
}
