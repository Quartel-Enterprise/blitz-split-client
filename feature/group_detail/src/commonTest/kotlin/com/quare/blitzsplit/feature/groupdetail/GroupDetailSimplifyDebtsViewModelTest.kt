package com.quare.blitzsplit.feature.groupdetail

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.domain.usecase.ObserveActivityUnreadCount
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryCategoryTeachRepository
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.DebtKind
import com.quare.blitzsplit.core.expenses.domain.model.DebtRow
import com.quare.blitzsplit.core.expenses.domain.model.DebtSimplificationSummary
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupFace
import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SimplifyDebtsConflictNotice
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeCategoryTeachOfferUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeDeletedBillNoticeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteSettlementUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryMapUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryTeachOfferUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryTreeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveDeletedBillNoticeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveMapCategoriesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettlementsUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.RecordSettlementUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SaveCategoryTermUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryTermRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.data.repository.InMemoryGroupDeletionRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeGroupDeletionUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeSimplifyDebtsConflictUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupDeletionsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObservePinnedGroupIdsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.SetGroupPinnedUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupPinRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.usecase.DeleteGroup
import com.quare.blitzsplit.core.sync.domain.usecase.LeaveGroup
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.CategoryTeachUseCases
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupDetailViewModel
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupScreenUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

// Why: "Simplificar dívidas" on the group's Saldos (13.4) and the notice of a change that lost to a later one (13.1e).
class GroupDetailSimplifyDebtsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var recordSettlement: RecordSettlementUseCase
    private lateinit var viewModel: GroupDetailViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN Simplificar dividas off WHEN Saldos opens THEN shows the pairs the bills made unsimplified`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = diegoOwesBrunoWhoOwesMe)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(BRUNO to PIERRE), state.myDebts.map { it.fromMemberId to it.toMemberId })
            assertEquals(listOf(DIEGO to BRUNO), state.otherDebts.map { it.fromMemberId to it.toMemberId })
            assertTrue((state.myDebts + state.otherDebts).none { it.isSimplified })
            assertNull(state.simplification)
            assertFalse(state.showsSimplificationCard)
        }

    @Test
    fun `GIVEN Diego owes Bruno who owes me and the switch on WHEN Saldos opens THEN Diego owes me directly`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(simplifiesDebts = true),
                storedExpenses = diegoOwesBrunoWhoOwesMe,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                listOf(
                    DebtRow(
                        fromMemberId = DIEGO,
                        toMemberId = PIERRE,
                        fromName = "Diego",
                        fromFace = GroupFace("D", AvatarColor.SAND, photoUrl = null),
                        toName = "Pierre",
                        toFace = GroupFace("P", AvatarColor.BLUE, photoUrl = null),
                        amountMinor = 4_000,
                        kind = DebtKind.OWES_YOU,
                        isSimplified = true,
                    ),
                ),
                state.myDebts,
            )
            assertTrue(state.otherDebts.isEmpty())
        }

    @Test
    fun `GIVEN the switch on WHEN Saldos opens THEN counts the payments with and without simplifying`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(simplifiesDebts = true),
                storedExpenses = diegoOwesBrunoWhoOwesMe,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                DebtSimplificationSummary(
                    paymentCount = 1,
                    unsimplifiedPaymentCount = 2,
                ),
                state.simplification,
            )
            assertTrue(state.showsSimplificationCard)
        }

    @Test
    fun `GIVEN the switch on and a pair a bill made WHEN Saldos opens THEN only the new pair is marked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(simplifiesDebts = true),
                storedExpenses = diegoOwesBrunoWhoOwesMe + bill(
                    "snacks",
                    paidBy = ANA,
                    amountMinor = 4_000,
                    sharedWith = CARLA,
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                listOf(Triple(DIEGO, PIERRE, true)),
                state.myDebts.map { Triple(it.fromMemberId, it.toMemberId, it.isSimplified) },
            )
            assertEquals(
                listOf(Triple(CARLA, ANA, false)),
                state.otherDebts.map { Triple(it.fromMemberId, it.toMemberId, it.isSimplified) },
            )
            assertEquals(
                DebtSimplificationSummary(
                    paymentCount = 2,
                    unsimplifiedPaymentCount = 3,
                ),
                state.simplification,
            )
        }

    @Test
    fun `GIVEN the switch on and the fewest pairs WHEN Saldos opens THEN none is marked and the card shows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(simplifiesDebts = true),
                storedExpenses = listOf(lodgeDiegoOwesBruno),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                listOf(Triple(DIEGO, BRUNO, false)),
                state.otherDebts.map {
                    Triple(it.fromMemberId, it.toMemberId, it.isSimplified)
                },
            )
            assertEquals(
                DebtSimplificationSummary(
                    paymentCount = 1,
                    unsimplifiedPaymentCount = 1,
                ),
                state.simplification,
            )
            assertTrue(state.showsSimplificationCard)
        }

    @Test
    fun `GIVEN I owe Ana and Bruno owes me and the switch on WHEN Saldos opens THEN my balance is settled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(simplifiesDebts = true),
                storedExpenses = listOf(
                    bill(
                        "taxi",
                        paidBy = ANA,
                        amountMinor = 8_000,
                        sharedWith = PIERRE,
                    ),
                    bill(
                        "dinner",
                        paidBy = PIERRE,
                        amountMinor = 8_000,
                        sharedWith = BRUNO,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 0,
                ),
                state.header.myBalance,
            )
            assertTrue(state.myDebts.isEmpty())
            assertEquals(listOf(BRUNO to ANA), state.otherDebts.map { it.fromMemberId to it.toMemberId })
        }

    @Test
    fun `GIVEN I owe Bruno who owes Ana and the switch on WHEN tapping Acertar THEN opens the payment to Ana`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(simplifiesDebts = true),
                storedExpenses = listOf(
                    bill(
                        "lodge",
                        paidBy = BRUNO,
                        amountMinor = 10_000,
                        sharedWith = PIERRE,
                    ),
                    bill(
                        "boat",
                        paidBy = ANA,
                        amountMinor = 10_000,
                        sharedWith = BRUNO,
                    ),
                ),
            )
            val event = GroupDetailUiEvent.OnSettleClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.Groups,
                    route,
                    Route.RegisterPayment(GROUP_ID, PIERRE, ANA, offersReceiverKeys = true),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the switch on WHEN tapping the simplified pair THEN opens Registrar pagamento for that pair`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(simplifiesDebts = true),
                storedExpenses = diegoOwesBrunoWhoOwesMe,
            )
            val row = viewModel.uiState.value.myDebts
                .single()
            val event = GroupDetailUiEvent.OnDebtClicked(
                fromMemberId = row.fromMemberId,
                toMemberId = row.toMemberId,
            )

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.Groups,
                    route,
                    Route.RegisterPayment(GROUP_ID, DIEGO, PIERRE, offersReceiverKeys = true),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Saldos open with the switch off WHEN someone turns it on THEN the pairs are simplified`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = diegoOwesBrunoWhoOwesMe)

            // When
            groups.groups.value = listOf(trip.copy(simplifiesDebts = true))

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf(DIEGO to PIERRE), state.myDebts.map { it.fromMemberId to it.toMemberId })
            assertTrue(state.otherDebts.isEmpty())
            assertEquals(
                DebtSimplificationSummary(
                    paymentCount = 1,
                    unsimplifiedPaymentCount = 2,
                ),
                state.simplification,
            )
        }

    @Test
    fun `GIVEN Saldos open with the switch on WHEN someone turns it off THEN the pairs the bills made come back`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(simplifiesDebts = true),
                storedExpenses = diegoOwesBrunoWhoOwesMe,
            )

            // When
            groups.groups.value = listOf(trip.copy(simplifiesDebts = false))

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf(BRUNO to PIERRE), state.myDebts.map { it.fromMemberId to it.toMemberId })
            assertEquals(listOf(DIEGO to BRUNO), state.otherDebts.map { it.fromMemberId to it.toMemberId })
            assertTrue((state.myDebts + state.otherDebts).none { it.isSimplified })
            assertNull(state.simplification)
            assertFalse(state.showsSimplificationCard)
        }

    @Test
    fun `GIVEN the switch on and Diego owing Bruno WHEN a bill makes Bruno owe me THEN Diego owes me directly`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(simplifiesDebts = true),
                storedExpenses = listOf(lodgeDiegoOwesBruno),
            )

            // When
            expenses.expenses.value += dinnerBrunoOwesMe

            // Then
            val state = viewModel.uiState.value
            assertEquals(
                listOf(Triple(DIEGO, PIERRE, true)),
                state.myDebts.map { Triple(it.fromMemberId, it.toMemberId, it.isSimplified) },
            )
            assertTrue(state.otherDebts.isEmpty())
            assertEquals(
                MemberBalance(
                    receivesMinor = 4_000,
                    owesMinor = 0,
                ),
                state.header.myBalance,
            )
        }

    @Test
    fun `GIVEN the switch on WHEN Diego pays me the simplified pair THEN no pair is left and the card hides`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(simplifiesDebts = true),
                storedExpenses = diegoOwesBrunoWhoOwesMe,
            )

            // When
            recordSettlement(
                groupId = GROUP_ID,
                fromMemberId = DIEGO,
                toMemberId = PIERRE,
                amountMinor = 4_000,
                method = SettlementMethod.PIX,
                paidAt = paidAt,
            ).getOrThrow()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.myDebts.isEmpty())
            assertTrue(state.otherDebts.isEmpty())
            assertFalse(state.showsSimplificationCard)
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 0,
                ),
                state.header.myBalance,
            )
        }

    @Test
    fun `GIVEN a group in US dollars WHEN tapping Como funciona THEN opens it in dollars over the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(simplifiesDebts = true),
                storedExpenses = diegoOwesBrunoWhoOwesMe,
            )
            val event = GroupDetailUiEvent.OnSimplifyDebtsInfoClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Groups, route, Route.SimplifyDebtsInfo(GroupCurrency.USD)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN no later change WHEN the group opens THEN no conflict shows and nothing is consumed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.simplifyConflict)
            assertTrue(groups.consumedSimplifyConflictGroupIds.isEmpty())
        }

    @Test
    fun `GIVEN Ana turned it off after this device WHEN the group opens THEN says so and consumes it once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(simplifyDebtsConflictBy = ANA_USER_ID))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                SimplifyDebtsConflictNotice(
                    changedByName = "Ana",
                    simplifiesDebts = false,
                ),
                state.simplifyConflict,
            )
            assertEquals(listOf(GROUP_ID), groups.consumedSimplifyConflictGroupIds)
        }

    @Test
    fun `GIVEN an ex-member turned it on later WHEN the group opens THEN the notice has no name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(
                    simplifiesDebts = true,
                    simplifyDebtsConflictBy = "user-gone",
                ),
            )

            // When
            val notice = viewModel.uiState.value.simplifyConflict

            // Then
            assertEquals(
                SimplifyDebtsConflictNotice(
                    changedByName = null,
                    simplifiesDebts = true,
                ),
                notice,
            )
        }

    @Test
    fun `GIVEN the group open WHEN a sync brings a later change THEN the notice shows`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        groups.groups.value = listOf(trip.copy(simplifyDebtsConflictBy = ANA_USER_ID))

        // Then
        assertEquals(
            SimplifyDebtsConflictNotice(
                changedByName = "Ana",
                simplifiesDebts = false,
            ),
            viewModel.uiState.value.simplifyConflict,
        )
        assertEquals(listOf(GROUP_ID), groups.consumedSimplifyConflictGroupIds)
    }

    @Test
    fun `GIVEN Editar grupo over the group WHEN a later change comes THEN nothing shows nor is consumed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(simplifyDebtsConflictBy = ANA_USER_ID),
                routesAbove = listOf(Route.EditGroup(GROUP_ID)),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.simplifyConflict)
            assertTrue(groups.consumedSimplifyConflictGroupIds.isEmpty())
        }

    @Test
    fun `GIVEN a later change while Editar grupo covered the group WHEN it closes THEN the notice shows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(simplifyDebtsConflictBy = ANA_USER_ID),
                routesAbove = listOf(Route.EditGroup(GROUP_ID)),
            )

            // When
            navigator.navigateBack()

            // Then
            assertEquals(
                SimplifyDebtsConflictNotice(
                    changedByName = "Ana",
                    simplifiesDebts = false,
                ),
                viewModel.uiState.value.simplifyConflict,
            )
            assertEquals(listOf(GROUP_ID), groups.consumedSimplifyConflictGroupIds)
        }

    @Test
    fun `GIVEN the notice shown and dismissed WHEN another screen opens and closes THEN it never shows again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(simplifyDebtsConflictBy = ANA_USER_ID))
            viewModel.onEvent(GroupDetailUiEvent.OnSimplifyConflictDismissed)
            navigator.navigate(Route.GroupStats(GROUP_ID))

            // When
            navigator.navigateBack()

            // Then
            assertNull(viewModel.uiState.value.simplifyConflict)
            assertEquals(listOf(GROUP_ID), groups.consumedSimplifyConflictGroupIds)
        }

    @Test
    fun `GIVEN the notice shown WHEN dismissing it THEN it goes away`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = trip.copy(simplifyDebtsConflictBy = ANA_USER_ID))
        val event = GroupDetailUiEvent.OnSimplifyConflictDismissed

        // When
        viewModel.onEvent(event)

        // Then
        assertNull(viewModel.uiState.value.simplifyConflict)
        assertEquals(listOf(Route.Home, Route.Groups, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the notice shown WHEN tapping Ver na Atividade THEN it goes away and the Atividade tab opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(simplifyDebtsConflictBy = ANA_USER_ID))
            val event = GroupDetailUiEvent.OnSimplifyConflictActivityClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertNull(viewModel.uiState.value.simplifyConflict)
            assertEquals(listOf(Route.Home, Route.Activity(personKey = null)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the notice and another screen on top WHEN tapping Ver na Atividade THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(simplifyDebtsConflictBy = ANA_USER_ID))
            navigator.navigate(Route.GroupStats(GROUP_ID))
            val event = GroupDetailUiEvent.OnSimplifyConflictActivityClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Groups, route, Route.GroupStats(GROUP_ID)),
                navigator.backStack.value,
            )
        }

    private val route = Route.GroupDetail(GROUP_ID, joinedAs = null, GroupTab.BALANCES)

    private val nowMillis = Instant.parse("2026-10-30T15:00:00Z").toEpochMilliseconds()
    private val paidAt = Instant.parse("2026-10-30T13:00:00Z").toEpochMilliseconds()

    private val ana = member(
        ANA,
        "Ana",
        AvatarColor.PEACH,
        hasJoined = true,
        userId = ANA_USER_ID,
    )
    private val pierre = member(
        PIERRE,
        "Pierre",
        AvatarColor.BLUE,
        isMe = true,
        hasJoined = true,
    )
    private val bruno = member(BRUNO, "Bruno", AvatarColor.GREEN)
    private val carla = member(CARLA, "Carla", AvatarColor.LILAC)

    // Why: a guest, a name nobody joined as.
    private val diego = member(DIEGO, "Diego", AvatarColor.SAND)
    private val elisa = member(ELISA, "Elisa", AvatarColor.PEACH)

    private val trip = Group(
        id = GROUP_ID,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.LILAC,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.USD,
        members = listOf(ana, pierre, bruno, carla, diego, elisa),
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

    // Why: Diego owes Bruno 40 and Bruno owes this device's user 40, which simplified is Diego owing them 40.
    private val lodgeDiegoOwesBruno = bill(
        "lodge",
        paidBy = BRUNO,
        amountMinor = 8_000,
        sharedWith = DIEGO,
    )
    private val dinnerBrunoOwesMe = bill(
        "dinner",
        paidBy = PIERRE,
        amountMinor = 8_000,
        sharedWith = BRUNO,
    )
    private val diegoOwesBrunoWhoOwesMe = listOf(lodgeDiegoOwesBruno, dinnerBrunoOwesMe)

    private fun member(
        id: String,
        name: String,
        color: AvatarColor,
        isMe: Boolean = false,
        hasJoined: Boolean = false,
        userId: String? = null,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = color,
        isMe = isMe,
        hasJoined = hasJoined,
        userId = userId,
        inviteCode = null,
        photoUrl = null,
    )

    // Why: one payer, split in equal halves with the other member, so the other owes the payer half the bill.
    private fun bill(
        id: String,
        paidBy: String,
        amountMinor: Long,
        sharedWith: String,
    ): Expense = Expense(
        id = id,
        groupId = GROUP_ID,
        description = "Conta $id",
        amountMinor = amountMinor,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer(paidBy, amountMinor)),
        shares = listOf(
            ExpenseShare(
                memberId = paidBy,
                amountMinor = amountMinor / 2,
                weight = 1,
            ),
            ExpenseShare(
                memberId = sharedWith,
                amountMinor = amountMinor / 2,
                weight = 1,
            ),
        ),
        items = null,
        categoryId = null,
        spentAt = paidAt,
        isPending = false,
        updatedAt = paidAt,
        updatedByMemberId = null,
    )

    private fun prepareScenario(
        storedGroup: Group = trip,
        storedExpenses: List<Expense> = emptyList(),
        routesAbove: List<Route> = emptyList(),
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup))
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        val settlements = FakeSettlementRepository(initialSettlements = emptyList())
        val pins = FakeGroupPinRepository(initialPinnedGroupIds = emptySet())
        val recordedPayments = InMemoryRecordedPaymentRepository()
        val deletions = InMemoryGroupDeletionRepository()
        val deletedBills = InMemoryDeletedBillRepository()
        val teach = InMemoryCategoryTeachRepository()
        val terms = FakeCategoryTermRepository()
        val categories = FakeCategoryRepository()
        navigator = BackStackNavigator(startRoute = Route.Home).also {
            it.navigate(Route.Groups)
            it.navigate(route)
            routesAbove.forEach(it::navigate)
        }
        recordSettlement = RecordSettlementUseCase(settlements, recordedPayments) { nowMillis }
        viewModel = GroupDetailViewModel(
            route = route,
            currentTimeMillis = { nowMillis },
            currentTimeZone = { TimeZone.UTC },
            consumeDeletedBillNotice = ConsumeDeletedBillNoticeUseCase(deletedBills),
            consumeSimplifyDebtsConflict = ConsumeSimplifyDebtsConflictUseCase(groups),
            observeDeletedBillNotice = ObserveDeletedBillNoticeUseCase(deletedBills),
            observeCategoryTree = ObserveCategoryTreeUseCase(categories),
            teachUseCases = CategoryTeachUseCases(
                observeOffer = ObserveCategoryTeachOfferUseCase(teach),
                consumeOffer = ConsumeCategoryTeachOfferUseCase(teach),
                observeMap = ObserveCategoryMapUseCase(terms),
                observeMapCategories = ObserveMapCategoriesUseCase(categories),
                saveTerm = SaveCategoryTermUseCase(terms) { nowMillis },
            ),
            observeAccount = ObserveAccountUseCase(FakeAccountRepository(initialStatus = AccountStatus.LocalOnly)),
            observeFirstDownload = ObserveFirstDownload { MutableStateFlow(null) },
            useCases = GroupScreenUseCases(
                navigator = navigator,
                observeGroup = ObserveGroupUseCase(groups),
                observeExpenses = ObserveExpensesUseCase(expenses),
                observeSettlements = ObserveSettlementsUseCase(settlements),
                observeRecordedPayment = ObserveRecordedPaymentUseCase(recordedPayments),
                consumeRecordedPayment = ConsumeRecordedPaymentUseCase(recordedPayments),
                deleteSettlement = DeleteSettlementUseCase(settlements, recordedPayments) { nowMillis },
                observeGroupDeletions = ObserveGroupDeletionsUseCase(deletions),
                consumeGroupDeletion = ConsumeGroupDeletionUseCase(deletions),
                connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
                leaveGroup = LeaveGroup { groups.requestLeaveGroup(it) },
                deleteGroup = DeleteGroup { groups.deleteGroup(it) },
                observePinnedGroupIds = ObservePinnedGroupIdsUseCase(pins),
                setGroupPinned = SetGroupPinnedUseCase(pins) { nowMillis },
                observeActivityUnreadCount = ObserveActivityUnreadCount { MutableStateFlow(0) },
            ),
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val ANA = "member-1"
        const val PIERRE = "member-2"
        const val BRUNO = "member-3"
        const val CARLA = "member-4"
        const val DIEGO = "member-5"
        const val ELISA = "member-6"
        const val ANA_USER_ID = "user-ana"
    }
}
