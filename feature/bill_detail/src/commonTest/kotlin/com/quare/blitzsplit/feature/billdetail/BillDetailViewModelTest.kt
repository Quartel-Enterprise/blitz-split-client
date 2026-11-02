package com.quare.blitzsplit.feature.billdetail

import com.quare.blitzsplit.core.activity.domain.model.ActivityChange
import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import com.quare.blitzsplit.core.activity.domain.model.ActivityField
import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.activity.domain.model.ActivityMemberValue
import com.quare.blitzsplit.core.activity.domain.model.ActivityValue
import com.quare.blitzsplit.core.activity.domain.usecase.impl.LoadBillHistoryUseCase
import com.quare.blitzsplit.core.activity.domain.usecase.impl.ObserveBillHistoryUseCase
import com.quare.blitzsplit.core.activity.testing.FakeActivityRepository
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairOutcome
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.AddBillCommentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.NoticeBillGoneUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveBillCommentsUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryTreeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettlementsUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.UpdateExpenseUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeBillCommentRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryKind
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillPayer
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillReplyTarget
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillShareRow
import com.quare.blitzsplit.feature.billdetail.presentation.model.MyPartNote
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.BillDetailViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class BillDetailViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var deletedBills: InMemoryDeletedBillRepository
    private lateinit var comments: FakeBillCommentRepository
    private lateinit var activity: FakeActivityRepository
    private val isOnline = MutableStateFlow(true)
    private val minutes = MutableSharedFlow<Unit>()
    private lateinit var viewModel: BillDetailViewModel
    private val actions = mutableListOf<BillDetailUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN Room not read yet WHEN the bill opens THEN nothing is loaded and the actions are disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isRoomRead = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertFalse(state.areActionsEnabled)
            assertFalse(state.showsMyPart)
        }

    @Test
    fun `GIVEN a bill in Mercado WHEN it opens THEN shows Alimentacao in Mercado and Outros without one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = expense().copy(categoryId = AppCategory.FOOD_MARKET.key))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(AppCategory.FOOD_MARKET.key, state.category.id)
            assertEquals(AppCategory.FOOD.key, state.categoryParent?.id)
        }

    @Test
    fun `GIVEN a bill on the device WHEN it opens THEN shows the group and the bill`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isLoaded)
        assertTrue(state.areActionsEnabled)
        assertEquals("Viagem de Floripa", state.groupName)
        assertEquals(GroupCurrency.USD, state.currency)
        assertEquals("Mercado", state.description)
        assertEquals(48_000, state.totalMinor)
        assertEquals(earlierToday, state.spentAt)
        assertEquals(SplitMode.EQUAL, state.splitMode)
        assertFalse(state.isPending)
    }

    @Test
    fun `GIVEN a bill spent earlier today WHEN it opens THEN its day is today`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpense = expense(spentAt = earlierToday))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(RelativeDay.TODAY, state.spentDay)
    }

    @Test
    fun `GIVEN a bill spent yesterday WHEN it opens THEN its day is yesterday`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpense = expense(spentAt = yesterdayEvening))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(RelativeDay.YESTERDAY, state.spentDay)
    }

    @Test
    fun `GIVEN a bill spent last week WHEN it opens THEN its day is another day`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpense = expense(spentAt = lastWeek))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(RelativeDay.OTHER, state.spentDay)
    }

    @Test
    fun `GIVEN a bill of yesterday in the device time zone WHEN it opens THEN its day is yesterday`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(spentAt = Instant.parse("2026-10-30T01:00:00Z").toEpochMilliseconds()),
                timeZone = UtcOffset(hours = -3).asTimeZone(),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(RelativeDay.YESTERDAY, state.spentDay)
        }

    @Test
    fun `GIVEN a bill waiting for the sync WHEN it opens THEN shows it is pending`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpense = expense(isPending = true))

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isPending)
    }

    @Test
    fun `GIVEN Ana paid the bill WHEN it opens THEN shows Ana as the payer`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpense = expense(payerId = ANA))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(
            listOf(
                BillPayer(
                    name = "Ana",
                    initials = "AN",
                    avatarColor = AvatarColor.PEACH,
                    isMe = false,
                ),
            ),
            state.payers,
        )
    }

    @Test
    fun `GIVEN the device user paid the bill WHEN it opens THEN the payer is them`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpense = expense(payerId = PIERRE))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(
            listOf(
                BillPayer(
                    name = "Pierre",
                    initials = "P",
                    avatarColor = AvatarColor.BLUE,
                    isMe = true,
                ),
            ),
            state.payers,
        )
    }

    @Test
    fun `GIVEN whoever paid left the group WHEN it opens THEN the payer has no name`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpense = expense(payerId = FORMER))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(listOf(null), state.payers.map { it.name })
    }

    @Test
    fun `GIVEN Bruno and Ana paid in that order WHEN it opens THEN lists them in the order they joined the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = expense(payers = listOf(paid(BRUNO, 18_000), paid(ANA, 30_000))))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                listOf(
                    BillPayer(
                        name = "Ana",
                        initials = "AN",
                        avatarColor = AvatarColor.PEACH,
                        isMe = false,
                    ),
                    BillPayer(
                        name = "Bruno",
                        initials = "BR",
                        avatarColor = AvatarColor.GREEN,
                        isMe = false,
                    ),
                ),
                state.payers,
            )
        }

    @Test
    fun `GIVEN the device user paid along with Ana and Bruno WHEN it opens THEN lists the device user first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payers = listOf(paid(ANA, 16_000), paid(BRUNO, 16_000), paid(PIERRE, 16_000)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf("Pierre", "Ana", "Bruno"), state.payers.map { it.name })
            assertEquals(listOf(true, false, false), state.payers.map { it.isMe })
        }

    @Test
    fun `GIVEN a former member paid along with Ana and the device user WHEN it opens THEN lists them last`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payers = listOf(paid(FORMER, 18_000), paid(ANA, 20_000), paid(PIERRE, 10_000)),
                    shares = listOf(share(ANA, 16_000), share(PIERRE, 16_000), share(FORMER, 16_000)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                listOf(
                    BillPayer(
                        name = "Pierre",
                        initials = "P",
                        avatarColor = AvatarColor.BLUE,
                        isMe = true,
                    ),
                    BillPayer(
                        name = "Ana",
                        initials = "AN",
                        avatarColor = AvatarColor.PEACH,
                        isMe = false,
                    ),
                    BillPayer(
                        name = null,
                        initials = "",
                        avatarColor = AvatarColor.entries.first(),
                        isMe = false,
                    ),
                ),
                state.payers,
            )
        }

    @Test
    fun `GIVEN two people who left the group paid WHEN it opens THEN lists one payer without a name for each`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payers = listOf(paid(FORMER, 24_000), paid(OTHER_FORMER, 24_000)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(null, null), state.payers.map { it.name })
        }

    @Test
    fun `GIVEN Carla paid without being in the split WHEN it opens THEN Carla is the payer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = expense(payerId = CARLA))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf("Carla"), state.payers.map { it.name })
        }

    @Test
    fun `GIVEN someone else paid and the device user is in it WHEN it opens THEN they owe the payer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = expense(payerId = ANA))

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsMyPart)
            assertEquals(-16_000, state.myBalanceMinor)
            assertEquals(MyPartNote.OwesPayers(names = listOf("Ana")), state.myPartNote)
        }

    @Test
    fun `GIVEN the device user paid and the others owe the same WHEN it opens THEN they get back what each owes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = expense(payerId = PIERRE))

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsMyPart)
            assertEquals(32_000, state.myBalanceMinor)
            assertEquals(
                MyPartNote.OwedBy(
                    names = listOf("Ana", "Bruno"),
                    eachMinor = 16_000,
                    totalMinor = 32_000,
                ),
                state.myPartNote,
            )
        }

    @Test
    fun `GIVEN the device user paid and the others owe unlike amounts WHEN it opens THEN gives the total`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payerId = PIERRE,
                    splitMode = SplitMode.AMOUNT,
                    shares = listOf(share(ANA, 10_000), share(PIERRE, 18_000), share(BRUNO, 20_000)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(30_000, state.myBalanceMinor)
            assertEquals(
                MyPartNote.OwedBy(
                    names = listOf("Ana", "Bruno"),
                    eachMinor = null,
                    totalMinor = 30_000,
                ),
                state.myPartNote,
            )
        }

    @Test
    fun `GIVEN the device user paid and someone who left the group owes WHEN it opens THEN that debtor has no name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payerId = PIERRE,
                    shares = listOf(share(PIERRE, 16_000), share(ANA, 16_000), share(FORMER, 16_000)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                MyPartNote.OwedBy(
                    names = listOf("Ana", null),
                    eachMinor = 16_000,
                    totalMinor = 32_000,
                ),
                state.myPartNote,
            )
        }

    @Test
    fun `GIVEN the device user neither paid nor is in the split WHEN it opens THEN hides their part`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payerId = ANA,
                    shares = listOf(share(ANA, 24_000), share(BRUNO, 24_000)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.showsMyPart)
            assertNull(state.myBalanceMinor)
            assertNull(state.myPartNote)
        }

    @Test
    fun `GIVEN the device user paid only their own part WHEN it opens THEN their part is zero with no note`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payerId = PIERRE,
                    amountMinor = 5_000,
                    shares = listOf(share(PIERRE, 5_000)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsMyPart)
            assertEquals(0, state.myBalanceMinor)
            assertNull(state.myPartNote)
        }

    @Test
    fun `GIVEN someone else paid a cent and the device user got a zero part WHEN it opens THEN there is no note`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payerId = ANA,
                    amountMinor = 1,
                    shares = listOf(share(ANA, 1), share(PIERRE, 0)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(0, state.myBalanceMinor)
            assertNull(state.myPartNote)
        }

    @Test
    fun `GIVEN whoever paid left the group and the device user owes WHEN it opens THEN there is no note`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = expense(payerId = FORMER))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(-16_000, state.myBalanceMinor)
            assertNull(state.myPartNote)
        }

    @Test
    fun `GIVEN Ana and Bruno paid more than their parts WHEN it opens THEN the device user owes both`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = expense(payers = listOf(paid(ANA, 30_000), paid(BRUNO, 18_000))))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(-16_000, state.myBalanceMinor)
            assertEquals(MyPartNote.OwesPayers(names = listOf("Ana", "Bruno")), state.myPartNote)
        }

    @Test
    fun `GIVEN Bruno comes before Ana in the split WHEN it opens THEN names the payers in the order of the split`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payers = listOf(paid(ANA, 30_000), paid(BRUNO, 18_000)),
                    shares = listOf(share(BRUNO, 16_000), share(PIERRE, 16_000), share(ANA, 16_000)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(MyPartNote.OwesPayers(names = listOf("Bruno", "Ana")), state.myPartNote)
        }

    @Test
    fun `GIVEN Bruno paid only his own part along with Ana WHEN it opens THEN the device user owes only Ana`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = expense(payers = listOf(paid(ANA, 32_000), paid(BRUNO, 16_000))))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(MyPartNote.OwesPayers(names = listOf("Ana")), state.myPartNote)
        }

    @Test
    fun `GIVEN Bruno owes earlier in the split and covers Ana WHEN it opens THEN the device user owes only Carla`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payers = listOf(paid(ANA, 24_000), paid(CARLA, 24_000)),
                    shares = listOf(
                        share(ANA, 12_000),
                        share(BRUNO, 12_000),
                        share(PIERRE, 12_000),
                        share(CARLA, 12_000),
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(-12_000, state.myBalanceMinor)
            assertEquals(MyPartNote.OwesPayers(names = listOf("Carla")), state.myPartNote)
        }

    @Test
    fun `GIVEN Ana and a former member paid WHEN it opens THEN the device user owes Ana and a nameless payer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payers = listOf(paid(ANA, 24_000), paid(FORMER, 24_000)),
                    shares = listOf(share(ANA, 16_000), share(PIERRE, 16_000), share(FORMER, 16_000)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(MyPartNote.OwesPayers(names = listOf("Ana", null)), state.myPartNote)
        }

    @Test
    fun `GIVEN every payer the device user owes left the group WHEN it opens THEN there is no note`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payers = listOf(paid(FORMER, 24_000), paid(OTHER_FORMER, 24_000)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(-16_000, state.myBalanceMinor)
            assertNull(state.myPartNote)
        }

    @Test
    fun `GIVEN Carla paid without being in the split WHEN it opens THEN the device user owes Carla`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = expense(payerId = CARLA))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(-16_000, state.myBalanceMinor)
            assertEquals(MyPartNote.OwesPayers(names = listOf("Carla")), state.myPartNote)
        }

    @Test
    fun `GIVEN the device user paid most and Ana paid less than her part WHEN it opens THEN Ana and Bruno owe them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = expense(payers = listOf(paid(PIERRE, 40_000), paid(ANA, 8_000))))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(24_000, state.myBalanceMinor)
            assertEquals(
                MyPartNote.OwedBy(
                    names = listOf("Ana", "Bruno"),
                    eachMinor = null,
                    totalMinor = 24_000,
                ),
                state.myPartNote,
            )
        }

    @Test
    fun `GIVEN Ana paid over her part along with the device user WHEN it opens THEN Bruno owes them part of his`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = expense(payers = listOf(paid(ANA, 18_000), paid(PIERRE, 30_000))))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(14_000, state.myBalanceMinor)
            assertEquals(
                MyPartNote.OwedBy(
                    names = listOf("Bruno"),
                    eachMinor = 14_000,
                    totalMinor = 14_000,
                ),
                state.myPartNote,
            )
        }

    @Test
    fun `GIVEN Ana paid exactly her part along with the device user WHEN it opens THEN Bruno and Carla owe the same`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payers = listOf(paid(PIERRE, 36_000), paid(ANA, 12_000)),
                    shares = listOf(
                        share(ANA, 12_000),
                        share(PIERRE, 12_000),
                        share(BRUNO, 12_000),
                        share(CARLA, 12_000),
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                MyPartNote.OwedBy(
                    names = listOf("Bruno", "Carla"),
                    eachMinor = 12_000,
                    totalMinor = 24_000,
                ),
                state.myPartNote,
            )
        }

    @Test
    fun `GIVEN the device user and Ana paid and a former member owes WHEN it opens THEN that debtor has no name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payers = listOf(paid(PIERRE, 32_000), paid(ANA, 16_000)),
                    shares = listOf(share(ANA, 16_000), share(PIERRE, 16_000), share(FORMER, 16_000)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                MyPartNote.OwedBy(
                    names = listOf(null),
                    eachMinor = 16_000,
                    totalMinor = 16_000,
                ),
                state.myPartNote,
            )
        }

    @Test
    fun `GIVEN the device user paid without being in the split WHEN it opens THEN Bruno owes them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payers = listOf(paid(PIERRE, 24_000), paid(ANA, 24_000)),
                    shares = listOf(share(ANA, 24_000), share(BRUNO, 24_000)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsMyPart)
            assertEquals(24_000, state.myBalanceMinor)
            assertEquals(
                MyPartNote.OwedBy(
                    names = listOf("Bruno"),
                    eachMinor = 24_000,
                    totalMinor = 24_000,
                ),
                state.myPartNote,
            )
        }

    @Test
    fun `GIVEN the device user paid exactly their part along with Ana WHEN it opens THEN shows zero with no note`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = expense(payers = listOf(paid(PIERRE, 16_000), paid(ANA, 32_000))))

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsMyPart)
            assertEquals(0, state.myBalanceMinor)
            assertNull(state.myPartNote)
        }

    @Test
    fun `GIVEN Ana and Bruno paid and split it between them WHEN it opens THEN hides the device user part`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payers = listOf(paid(ANA, 30_000), paid(BRUNO, 18_000)),
                    shares = listOf(share(ANA, 24_000), share(BRUNO, 24_000)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.showsMyPart)
            assertNull(state.myBalanceMinor)
            assertNull(state.myPartNote)
        }

    @Test
    fun `GIVEN a bill split among three of four members WHEN it opens THEN lists every member with their part`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                listOf(
                    BillShareRow(
                        memberId = ANA,
                        name = "Ana",
                        initials = "AN",
                        avatarColor = AvatarColor.PEACH,
                        isMe = false,
                        hasJoined = true,
                        paidMinor = 48_000,
                        shareMinor = 16_000,
                    ),
                    BillShareRow(
                        memberId = PIERRE,
                        name = "Pierre",
                        initials = "P",
                        avatarColor = AvatarColor.BLUE,
                        isMe = true,
                        hasJoined = true,
                        paidMinor = null,
                        shareMinor = 16_000,
                    ),
                    BillShareRow(
                        memberId = BRUNO,
                        name = "Bruno",
                        initials = "BR",
                        avatarColor = AvatarColor.GREEN,
                        isMe = false,
                        hasJoined = false,
                        paidMinor = null,
                        shareMinor = 16_000,
                    ),
                    BillShareRow(
                        memberId = CARLA,
                        name = "Carla",
                        initials = "CA",
                        avatarColor = AvatarColor.LILAC,
                        isMe = false,
                        hasJoined = false,
                        paidMinor = null,
                        shareMinor = null,
                    ),
                ),
                state.people,
            )
        }

    @Test
    fun `GIVEN a bill split among three of four members WHEN it opens THEN counts three in the split`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(3, state.participantCount)
        }

    @Test
    fun `GIVEN someone who left the group paid the bill WHEN it opens THEN lists them last without a name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payerId = FORMER,
                    shares = listOf(share(ANA, 16_000), share(PIERRE, 16_000), share(FORMER, 16_000)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(ANA, PIERRE, BRUNO, CARLA, FORMER), state.people.map { it.memberId })
            val former = state.people.last()
            assertNull(former.name)
            assertFalse(former.isMe)
            assertTrue(former.hasJoined)
            assertEquals(48_000, former.paidMinor)
            assertEquals(16_000, former.shareMinor)
            assertEquals(3, state.participantCount)
        }

    @Test
    fun `GIVEN Ana Carla and someone who left paid WHEN it opens THEN each row shows what that person paid`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payers = listOf(paid(ANA, 20_000), paid(CARLA, 10_000), paid(FORMER, 18_000)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(ANA, PIERRE, BRUNO, CARLA, FORMER), state.people.map { it.memberId })
            assertEquals(listOf(20_000L, null, null, 10_000L, 18_000L), state.people.map { it.paidMinor })
            assertEquals(listOf(16_000L, 16_000L, 16_000L, null, null), state.people.map { it.shareMinor })
            assertNull(state.people.last().name)
            assertEquals(3, state.participantCount)
        }

    @Test
    fun `GIVEN the bill is open WHEN a sync brings someone else edit THEN shows the bill as edited`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val edited = expense(
                description = "Mercado e padaria",
                amountMinor = 60_000,
            ).copy(
                shares = listOf(share(ANA, 20_000), share(PIERRE, 20_000), share(BRUNO, 20_000)),
            )

            // When
            expenses.expenses.value = listOf(edited)

            // Then
            val state = viewModel.uiState.value
            assertEquals("Mercado e padaria", state.description)
            assertEquals(60_000, state.totalMinor)
            assertEquals(-20_000, state.myBalanceMinor)
        }

    @Test
    fun `GIVEN the bill is open WHEN going back THEN returns to the group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(BillDetailUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
        assertNull(deletedBills.observeNoticeGroupId().first())
    }

    @Test
    fun `GIVEN a native back already closed the bill WHEN going back THEN the group stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigateBack()

            // When
            viewModel.onEvent(BillDetailUiEvent.OnBackClicked)

            // Then
            assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the bill is open WHEN tapping Editar THEN opens Editar conta for this bill`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(BillDetailUiEvent.OnEditClicked)

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                groupRoute,
                route,
                Route.SplitBill(
                    groupId = GROUP_ID,
                    expenseId = EXPENSE_ID,
                    picksGroupFirst = false,
                ),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Room not read yet WHEN tapping Editar THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomRead = false)

        // When
        viewModel.onEvent(BillDetailUiEvent.OnEditClicked)

        // Then
        assertEquals(listOf(Route.Groups, groupRoute, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a native back already closed the bill WHEN tapping Editar THEN nothing opens over the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigateBack()

            // When
            viewModel.onEvent(BillDetailUiEvent.OnEditClicked)

            // Then
            assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the bill is being deleted WHEN tapping Editar THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario(deleteGate = CompletableDeferred())
        viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)
        viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed)

        // When
        viewModel.onEvent(BillDetailUiEvent.OnEditClicked)

        // Then
        assertEquals(listOf(Route.Groups, groupRoute, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the device user owes WHEN tapping Apagar THEN asks with the description and their balance`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)

            // Then
            assertEquals(
                BillDeletion(
                    description = "Mercado",
                    myBalanceMinor = -16_000,
                    settledPair = null,
                ),
                viewModel.uiState.value.deletion,
            )
            assertTrue(expenses.deletedIds.isEmpty())
        }

    @Test
    fun `GIVEN Bruno paid Ana after the bill WHEN tapping Apagar THEN says Ana comes to owe him the payment`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedSettlements = listOf(settlement(BRUNO, ANA, 16_000, laterToday)))

            // When
            viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)

            // Then
            assertEquals(
                SettledPairChange(
                    payerName = "Bruno",
                    payerIsMe = false,
                    receiverName = "Ana",
                    receiverIsMe = false,
                    outcome = SettledPairOutcome.OWES,
                    debtorIsPayer = false,
                    amountMinor = 16_000,
                ),
                viewModel.uiState.value.deletion
                    ?.settledPair,
            )
        }

    @Test
    fun `GIVEN a payment dated before the bill WHEN tapping Apagar THEN says nothing about it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedSettlements = listOf(settlement(BRUNO, ANA, 16_000, lastWeek)))

            // When
            viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)

            // Then
            assertNull(
                viewModel.uiState.value.deletion
                    ?.settledPair,
            )
        }

    @Test
    fun `GIVEN Apagar open WHEN a payment after the bill syncs THEN the question says it at once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)

            // When
            settlements.settlements.value = listOf(settlement(BRUNO, ANA, 16_000, laterToday))

            // Then
            assertEquals(
                "Bruno",
                viewModel.uiState.value.deletion
                    ?.settledPair
                    ?.payerName,
            )
        }

    @Test
    fun `GIVEN the device user is not in the bill WHEN tapping Apagar THEN asks with no balance of theirs`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payerId = ANA,
                    shares = listOf(share(ANA, 24_000), share(BRUNO, 24_000)),
                ),
            )

            // When
            viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)

            // Then
            assertEquals(
                BillDeletion(
                    description = "Mercado",
                    myBalanceMinor = null,
                    settledPair = null,
                ),
                viewModel.uiState.value.deletion,
            )
        }

    @Test
    fun `GIVEN Room not read yet WHEN tapping Apagar THEN asks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomRead = false)

        // When
        viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)

        // Then
        assertNull(viewModel.uiState.value.deletion)
    }

    @Test
    fun `GIVEN Apagar asks WHEN dismissing THEN closes the question and keeps the bill`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)

        // When
        viewModel.onEvent(BillDetailUiEvent.OnDeleteDismissed)

        // Then
        assertNull(viewModel.uiState.value.deletion)
        assertTrue(expenses.deletedIds.isEmpty())
        assertEquals(listOf(Route.Groups, groupRoute, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Apagar asks WHEN confirming THEN deletes the bill and returns to the group without the notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)

            // When
            viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed)

            // Then
            assertEquals(listOf(EXPENSE_ID), expenses.deletedIds)
            assertEquals(listOf(nowMillis), expenses.deletedAts)
            assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
            assertNull(deletedBills.observeNoticeGroupId().first())
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN Apagar asks WHEN confirming while the delete is on its way THEN the actions are disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(deleteGate = CompletableDeferred())
            viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)

            // When
            viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isDeleting)
            assertFalse(state.areActionsEnabled)
            assertEquals(listOf(Route.Groups, groupRoute, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Apagar confirmed twice in a row WHEN the delete finishes THEN deletes the bill once`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(deleteGate = gate)
            viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)
            viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed)
            viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed)

            // When
            gate.complete(Unit)

            // Then
            assertEquals(listOf(EXPENSE_ID), expenses.deletedIds)
            assertTrue(actions.isEmpty())
            assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Apagar never asked WHEN a confirmation arrives THEN nothing is deleted`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed)

        // Then
        assertTrue(expenses.deletedIds.isEmpty())
        assertEquals(listOf(Route.Groups, groupRoute, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the device cannot delete the bill WHEN confirming THEN stays on the bill and says it failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(deleteError = IllegalStateException("disk full"))
            viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)

            // When
            viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.deletion)
            assertFalse(state.isDeleting)
            assertTrue(state.areActionsEnabled)
            assertEquals(listOf<BillDetailUiAction>(BillDetailUiAction.ShowDeleteError), actions)
            assertEquals(listOf(Route.Groups, groupRoute, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Apagar asks WHEN a sync changes the bill THEN the question tells what deleting it changes now`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)
            val edited = expense(
                description = "Feira",
                amountMinor = 30_000,
            ).copy(
                shares = listOf(share(ANA, 10_000), share(PIERRE, 10_000), share(BRUNO, 10_000)),
            )

            // When
            expenses.expenses.value = listOf(edited)

            // Then
            assertEquals(
                BillDeletion(
                    description = "Feira",
                    myBalanceMinor = -10_000,
                    settledPair = null,
                ),
                viewModel.uiState.value.deletion,
            )
        }

    @Test
    fun `GIVEN the bill is open WHEN someone else deletes it THEN returns to the group saying it was deleted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            expenses.expenses.value = emptyList()

            // Then
            assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
            assertEquals(GROUP_ID, deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN a bill that is not on the device WHEN it opens THEN returns to the group without the notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = null)

            // When
            val stack = navigator.backStack.value

            // Then
            assertEquals(listOf(Route.Groups, groupRoute), stack)
            assertNull(deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN the bill is open WHEN its group leaves the device THEN closes without the notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            groups.groups.value = emptyList()

            // Then
            assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
            assertNull(deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN Editar conta is open over the bill WHEN someone else deletes it THEN Editar conta stays on top`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(BillDetailUiEvent.OnEditClicked)

            // When
            expenses.expenses.value = emptyList()

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    groupRoute,
                    route,
                    Route.SplitBill(
                        groupId = GROUP_ID,
                        expenseId = EXPENSE_ID,
                        picksGroupFirst = false,
                    ),
                ),
                navigator.backStack.value,
            )
            assertNull(deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN someone deleted the bill under Editar conta WHEN it closes THEN returns saying it was deleted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(BillDetailUiEvent.OnEditClicked)
            expenses.expenses.value = emptyList()

            // When
            navigator.navigateBack()

            // Then
            assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
            assertEquals(GROUP_ID, deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN the group left the device under Editar conta WHEN it closes THEN returns without the notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(BillDetailUiEvent.OnEditClicked)
            groups.groups.value = emptyList()

            // When
            navigator.navigateBack()

            // Then
            assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
            assertNull(deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN Editar conta over the bill WHEN it deletes the bill and returns to the group THEN no notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(BillDetailUiEvent.OnEditClicked)

            // When
            expenses.expenses.value = emptyList()
            navigator.popTo(2)

            // Then
            assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
            assertNull(deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN Editar conta deleting the bill WHEN a back gesture shows the bill first THEN returns with no notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(BillDetailUiEvent.OnEditClicked)
            navigator.navigateBack()

            // When
            DeleteExpenseUseCase(expenses, deletedBills) { nowMillis }(EXPENSE_ID)

            // Then
            assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
            assertNull(deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN the bill is open WHEN typing a comment THEN keeps the draft and it can be sent`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val typed = "Quem pagou o estacionamento?"

            // When
            viewModel.onEvent(BillDetailUiEvent.OnCommentChanged(typed))

            // Then
            assertEquals(typed, viewModel.uiState.value.commentDraft)
            assertTrue(viewModel.uiState.value.canSendComment)
        }

    @Test
    fun `GIVEN the bill is open WHEN typing only spaces THEN the comment cannot be sent`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(BillDetailUiEvent.OnCommentChanged("   "))

        // Then
        assertFalse(viewModel.uiState.value.canSendComment)
    }

    @Test
    fun `GIVEN the bill is open WHEN typing over 1000 letters THEN the comment cannot be sent`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(BillDetailUiEvent.OnCommentChanged("a".repeat(1_001)))

            // Then
            assertFalse(viewModel.uiState.value.canSendComment)
        }

    @Test
    fun `GIVEN the bill is open WHEN typing 1000 letters between spaces THEN the comment can be sent`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(BillDetailUiEvent.OnCommentChanged("  " + "a".repeat(1_000) + "  "))

            // Then
            assertTrue(viewModel.uiState.value.canSendComment)
        }

    @Test
    fun `GIVEN Room not read yet WHEN typing a comment THEN it cannot be sent and no empty state shows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isRoomRead = false)

            // When
            viewModel.onEvent(BillDetailUiEvent.OnCommentChanged("Eu levo o carvão"))

            // Then
            assertFalse(viewModel.uiState.value.canSendComment)
            assertFalse(viewModel.uiState.value.showsNoComments)
        }

    @Test
    fun `GIVEN the bill is being deleted WHEN typing a comment THEN it cannot be sent`() = runTest(testDispatcher) {
        // Given
        prepareScenario(deleteGate = CompletableDeferred())
        viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)
        viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed)

        // When
        viewModel.onEvent(BillDetailUiEvent.OnCommentChanged("Eu levo o carvão"))

        // Then
        assertFalse(viewModel.uiState.value.canSendComment)
    }

    @Test
    fun `GIVEN no internet and a comment typed WHEN sending it THEN keeps it on the device in the queue as the user`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)
            viewModel.onEvent(BillDetailUiEvent.OnCommentChanged("  Paguei no Pix  "))

            // When
            viewModel.onEvent(BillDetailUiEvent.OnSendCommentClicked)

            // Then
            val saved = comments.comments.value.single()
            assertEquals("Paguei no Pix", saved.body)
            assertEquals(EXPENSE_ID, saved.expenseId)
            assertEquals(GROUP_ID, saved.groupId)
            assertEquals(PIERRE, saved.authorMemberId)
            assertEquals("Pierre", saved.authorName)
            assertNull(saved.threadId)
            assertNull(saved.replyToName)
            assertEquals(nowMillis, saved.postedAt)
            assertTrue(saved.isPending)
            val state = viewModel.uiState.value
            assertEquals("", state.commentDraft)
            assertNull(state.replyTarget)
            val row = state.threads.single().comment
            assertTrue(row.isPending)
            assertTrue(row.isMe)
            assertEquals("Paguei no Pix", row.body)
            assertEquals(1, state.commentCount)
            assertFalse(state.showsNoComments)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN only spaces typed WHEN sending the comment THEN nothing is saved`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(BillDetailUiEvent.OnCommentChanged("   "))

        // When
        viewModel.onEvent(BillDetailUiEvent.OnSendCommentClicked)

        // Then
        assertTrue(comments.comments.value.isEmpty())
        assertEquals("   ", viewModel.uiState.value.commentDraft)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN a comment the device fails to keep WHEN another was typed meanwhile THEN keeps the new text`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(
                commentError = IllegalStateException("bill gone"),
                commentGate = gate,
            )
            viewModel.onEvent(BillDetailUiEvent.OnCommentChanged("Paguei no Pix"))
            viewModel.onEvent(BillDetailUiEvent.OnSendCommentClicked)
            viewModel.onEvent(BillDetailUiEvent.OnCommentChanged("Eu levo"))

            // When
            gate.complete(Unit)

            // Then
            assertEquals("Eu levo", viewModel.uiState.value.commentDraft)
            assertEquals(listOf<BillDetailUiAction>(BillDetailUiAction.ShowCommentError), actions)
        }

    @Test
    fun `GIVEN a comment of today WHEN midnight passes with the bill open THEN it reads as yesterday`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedComments = listOf(comment(id = "c1")))
            assertEquals(
                RelativeDay.TODAY,
                viewModel.uiState.value.threads
                    .single()
                    .comment.postedDay,
            )
            nowMillis += 1.days.inWholeMilliseconds

            // When
            minutes.emit(Unit)

            // Then
            assertEquals(
                RelativeDay.YESTERDAY,
                viewModel.uiState.value.threads
                    .single()
                    .comment.postedDay,
            )
        }

    @Test
    fun `GIVEN the device cannot keep the comment WHEN sending a reply THEN brings it back and says so`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedComments = listOf(comment(id = "c1")),
                commentError = IllegalStateException("bill gone"),
            )
            viewModel.onEvent(BillDetailUiEvent.OnReplyClicked("c1"))
            viewModel.onEvent(BillDetailUiEvent.OnCommentChanged("Eu levo"))

            // When
            viewModel.onEvent(BillDetailUiEvent.OnSendCommentClicked)

            // Then
            val state = viewModel.uiState.value
            assertEquals("Eu levo", state.commentDraft)
            assertEquals(
                BillReplyTarget(
                    threadId = "c1",
                    commentId = "c1",
                    name = "Bruno",
                    isMe = false,
                ),
                state.replyTarget,
            )
            assertEquals(listOf<BillDetailUiAction>(BillDetailUiAction.ShowCommentError), actions)
            assertEquals(listOf("c1"), comments.comments.value.map { it.id })
        }

    @Test
    fun `GIVEN the bill is open WHEN a sync brings a comment by Bruno THEN shows it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        comments.comments.value = listOf(
            comment(
                id = "c1",
                body = "Quem trouxe o carvão?",
            ),
        )

        // Then
        val row = viewModel.uiState.value.threads
            .single()
            .comment
        assertEquals("Bruno", row.authorName)
        assertEquals("Quem trouxe o carvão?", row.body)
        assertFalse(row.isMe)
        assertFalse(row.isPending)
    }

    @Test
    fun `GIVEN two comments and a reply WHEN the bill opens THEN counts the reply too and shows the thread`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedComments = listOf(
                    comment(
                        id = "c1",
                        postedAt = earlierToday,
                    ),
                    comment(
                        id = "c2",
                        postedAt = earlierToday + 1,
                    ),
                    comment(
                        id = "r1",
                        threadId = "c1",
                        authorMemberId = ANA,
                        authorName = "Ana",
                        postedAt = laterToday,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(3, state.commentCount)
            assertFalse(state.showsNoComments)
            assertEquals(listOf("c1", "c2"), state.threads.map { it.comment.id })
            assertEquals(
                listOf("r1"),
                state.threads
                    .first()
                    .replies
                    .map { it.id },
            )
        }

    @Test
    fun `GIVEN a bill without comments WHEN it opens THEN says there are none yet`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.showsNoComments)
        assertEquals(0, state.commentCount)
    }

    @Test
    fun `GIVEN a comment by Ana WHEN a sync renames her THEN the comment shows her new name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedComments = listOf(
                    comment(
                        authorMemberId = ANA,
                        authorName = "Ana",
                    ),
                ),
            )
            val renamed = trip.copy(members = listOf(ana.copy(name = "Ana Paula"), pierre, bruno, carla))

            // When
            groups.groups.value = listOf(renamed)

            // Then
            assertEquals(
                "Ana Paula",
                viewModel.uiState.value.threads
                    .single()
                    .comment.authorName,
            )
        }

    @Test
    fun `GIVEN a comment by Bruno WHEN replying to it THEN says it answers Bruno in his thread`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedComments = listOf(comment(id = "c1")))

            // When
            viewModel.onEvent(BillDetailUiEvent.OnReplyClicked("c1"))

            // Then
            assertEquals(
                BillReplyTarget(
                    threadId = "c1",
                    commentId = "c1",
                    name = "Bruno",
                    isMe = false,
                ),
                viewModel.uiState.value.replyTarget,
            )
        }

    @Test
    fun `GIVEN a comment by the device user WHEN replying to it THEN the reply answers them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedComments = listOf(
                    comment(
                        id = "c1",
                        authorMemberId = PIERRE,
                        authorName = "Pierre",
                    ),
                ),
            )

            // When
            viewModel.onEvent(BillDetailUiEvent.OnReplyClicked("c1"))

            // Then
            assertEquals(
                BillReplyTarget(
                    threadId = "c1",
                    commentId = "c1",
                    name = "Pierre",
                    isMe = true,
                ),
                viewModel.uiState.value.replyTarget,
            )
        }

    @Test
    fun `GIVEN Ana replied to Bruno WHEN replying to Ana THEN the reply joins the thread of Bruno and answers Ana`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedComments = threadWithReply)

            // When
            viewModel.onEvent(BillDetailUiEvent.OnReplyClicked("r1"))

            // Then
            assertEquals(
                BillReplyTarget(
                    threadId = "c1",
                    commentId = "r1",
                    name = "Ana",
                    isMe = false,
                ),
                viewModel.uiState.value.replyTarget,
            )
        }

    @Test
    fun `GIVEN replying to the reply of Ana WHEN sending THEN the comment joins the thread and names Ana`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedComments = threadWithReply)
            viewModel.onEvent(BillDetailUiEvent.OnReplyClicked("r1"))
            viewModel.onEvent(BillDetailUiEvent.OnCommentChanged("Combinado"))

            // When
            viewModel.onEvent(BillDetailUiEvent.OnSendCommentClicked)

            // Then
            val sent = comments.comments.value.single { it.id !in setOf("c1", "r1") }
            assertEquals("c1", sent.threadId)
            assertEquals("Ana", sent.replyToName)
            val state = viewModel.uiState.value
            assertNull(state.replyTarget)
            assertEquals("", state.commentDraft)
            assertEquals(
                listOf("r1", sent.id),
                state.threads
                    .single()
                    .replies
                    .map { it.id },
            )
            assertEquals(3, state.commentCount)
        }

    @Test
    fun `GIVEN replying to the comment of Bruno WHEN sending THEN the comment joins his thread naming nobody`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedComments = listOf(comment(id = "c1")))
            viewModel.onEvent(BillDetailUiEvent.OnReplyClicked("c1"))
            viewModel.onEvent(BillDetailUiEvent.OnCommentChanged("Eu levo"))

            // When
            viewModel.onEvent(BillDetailUiEvent.OnSendCommentClicked)

            // Then
            val sent = comments.comments.value.single { it.id != "c1" }
            assertEquals("c1", sent.threadId)
            assertNull(sent.replyToName)
            assertNull(viewModel.uiState.value.replyTarget)
        }

    @Test
    fun `GIVEN replying to Bruno WHEN cancelling the reply THEN the composer answers nobody`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedComments = listOf(comment(id = "c1")))
            viewModel.onEvent(BillDetailUiEvent.OnReplyClicked("c1"))

            // When
            viewModel.onEvent(BillDetailUiEvent.OnReplyCancelled)

            // Then
            assertNull(viewModel.uiState.value.replyTarget)
        }

    @Test
    fun `GIVEN replying to Bruno WHEN replying to a comment that is not there THEN still answers Bruno`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedComments = listOf(comment(id = "c1")))
            viewModel.onEvent(BillDetailUiEvent.OnReplyClicked("c1"))

            // When
            viewModel.onEvent(BillDetailUiEvent.OnReplyClicked("unknown"))

            // Then
            assertEquals(
                "c1",
                viewModel.uiState.value.replyTarget
                    ?.commentId,
            )
        }

    @Test
    fun `GIVEN Ana created the bill WHEN it opens THEN the history says she created it today`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(created))

            // When
            val history = viewModel.uiState.value.history

            // Then
            val day = history.single()
            assertEquals(RelativeDay.TODAY, day.relativeDay)
            val entry = day.entries.single()
            assertEquals(BillHistoryKind.CREATED, entry.kind)
            assertEquals("Ana", entry.actorName)
            assertEquals(48_000, entry.amountMinor)
        }

    @Test
    fun `GIVEN Ana edited the bill last WHEN it opens THEN her edit can be undone`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedEvents = listOf(totalEdit, created))

        // When
        val entries = viewModel.uiState.value.history
            .flatMap { it.entries }

        // Then
        assertEquals(listOf(true, false), entries.map { it.canUndo })
        assertFalse(viewModel.uiState.value.isUndoing)
    }

    @Test
    fun `GIVEN Ana edited the bill last WHEN tapping Desfazer THEN saves the bill as it was before her edit`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(totalEdit, created))

            // When
            viewModel.onEvent(BillDetailUiEvent.OnUndoClicked)

            // Then
            val saved = expenses.updatedExpenses.single()
            assertEquals("Feira", saved.description)
            assertEquals(30_000, saved.amountMinor)
            assertEquals(listOf(paid(ANA, 30_000)), saved.payers)
            assertEquals(SplitMode.EQUAL, saved.splitMode)
            assertEquals(listOf(share(ANA, 10_000), share(PIERRE, 10_000), share(BRUNO, 10_000)), saved.shares)
            assertNull(saved.items)
            assertNull(saved.categoryId)
            assertEquals(earlierToday, saved.spentAt)
            assertEquals(GROUP_ID, saved.groupId)
            assertFalse(viewModel.uiState.value.isUndoing)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN Ana gave a bill without category Mercado WHEN tapping Desfazer THEN the bill has no category again`() =
        runTest(testDispatcher) {
            // Given
            val categoryEdit = historyEvent(
                id = "edit",
                kind = ActivityKind.BILL_EDITED,
                occurredAt = laterToday,
                changes = listOf(
                    ActivityChange(
                        field = ActivityField.CATEGORY,
                        before = ActivityValue.none,
                        after = ActivityValue.none.copy(code = AppCategory.FOOD_MARKET.key),
                    ),
                ),
            )
            prepareScenario(
                storedExpense = expense().copy(categoryId = AppCategory.FOOD_MARKET.key),
                storedEvents = listOf(categoryEdit, created),
            )

            // When
            viewModel.onEvent(BillDetailUiEvent.OnUndoClicked)

            // Then
            assertNull(expenses.updatedExpenses.single().categoryId)
        }

    @Test
    fun `GIVEN Desfazer tapped WHEN the save is on its way THEN shows it is undoing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedEvents = listOf(totalEdit, created),
            updateGate = CompletableDeferred(),
        )

        // When
        viewModel.onEvent(BillDetailUiEvent.OnUndoClicked)

        // Then
        assertTrue(viewModel.uiState.value.isUndoing)
        assertTrue(expenses.updatedExpenses.isEmpty())
    }

    @Test
    fun `GIVEN Desfazer tapped twice in a row WHEN the save finishes THEN saves the bill once`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(
                storedEvents = listOf(totalEdit, created),
                updateGate = gate,
            )
            viewModel.onEvent(BillDetailUiEvent.OnUndoClicked)
            viewModel.onEvent(BillDetailUiEvent.OnUndoClicked)

            // When
            gate.complete(Unit)

            // Then
            assertEquals(1, expenses.updatedExpenses.size)
            assertFalse(viewModel.uiState.value.isUndoing)
        }

    @Test
    fun `GIVEN the device cannot save the bill WHEN tapping Desfazer THEN keeps the bill and says it failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedEvents = listOf(totalEdit, created),
                updateError = IllegalStateException("disk full"),
            )

            // When
            viewModel.onEvent(BillDetailUiEvent.OnUndoClicked)

            // Then
            assertEquals(listOf<BillDetailUiAction>(BillDetailUiAction.ShowUndoError), actions)
            assertFalse(viewModel.uiState.value.isUndoing)
            assertEquals(
                "Mercado",
                expenses.expenses.value
                    .single()
                    .description,
            )
        }

    @Test
    fun `GIVEN the bill changed since the latest edit WHEN tapping Desfazer THEN nothing is saved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(description = "Mercado do Zé"),
                storedEvents = listOf(descriptionEdit("Feira", "Mercado"), created),
            )

            // When
            viewModel.onEvent(BillDetailUiEvent.OnUndoClicked)

            // Then
            assertTrue(expenses.updatedExpenses.isEmpty())
            assertFalse(viewModel.uiState.value.isUndoing)
        }

    @Test
    fun `GIVEN the bill is being deleted WHEN tapping Desfazer THEN nothing is saved`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedEvents = listOf(totalEdit, created),
            deleteGate = CompletableDeferred(),
        )
        viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)
        viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed)

        // When
        viewModel.onEvent(BillDetailUiEvent.OnUndoClicked)

        // Then
        assertTrue(expenses.updatedExpenses.isEmpty())
        assertFalse(viewModel.uiState.value.isUndoing)
    }

    @Test
    fun `GIVEN two edits in a row WHEN the bill opens THEN only the latest can be undone`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedEvents = twoEdits)

        // When
        val entries = viewModel.uiState.value.history
            .flatMap { it.entries }

        // Then
        assertEquals(listOf("edit-2", "edit-1", "created"), entries.map { it.id })
        assertEquals(listOf(true, false, false), entries.map { it.canUndo })
    }

    @Test
    fun `GIVEN two edits in a row WHEN undoing the latest THEN the bill waits to go up with nothing to undo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = twoEdits)

            // When
            viewModel.onEvent(BillDetailUiEvent.OnUndoClicked)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isPending)
            assertEquals("Feira", state.description)
            val entries = state.history.flatMap { it.entries }
            assertTrue(entries.none { it.canUndo })
            assertEquals(BillHistoryKind.EDITED, entries.first().kind)
            assertTrue(entries.first().isPending)
        }

    @Test
    fun `GIVEN the latest edit undone WHEN tapping Desfazer again THEN nothing more is saved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = twoEdits)
            viewModel.onEvent(BillDetailUiEvent.OnUndoClicked)

            // When
            viewModel.onEvent(BillDetailUiEvent.OnUndoClicked)

            // Then
            assertEquals(listOf("Feira"), expenses.updatedExpenses.map { it.description })
        }

    @Test
    fun `GIVEN internet WHEN the bill opens THEN asks the server for its whole history`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = true)

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isOnline)
        assertEquals(listOf(EXPENSE_ID), activity.loadedHistoryIds)
    }

    @Test
    fun `GIVEN no internet WHEN the bill opens THEN says it is offline and asks the server nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isOnline)
            assertTrue(activity.loadedHistoryIds.isEmpty())
        }

    @Test
    fun `GIVEN the bill open offline WHEN the internet comes back THEN asks the server for its history`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)

            // When
            isOnline.value = true

            // Then
            assertTrue(viewModel.uiState.value.isOnline)
            assertEquals(listOf(EXPENSE_ID), activity.loadedHistoryIds)
        }

    @Test
    fun `GIVEN the bill open online WHEN the internet goes THEN says it is offline`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = true)

        // When
        isOnline.value = false

        // Then
        assertFalse(viewModel.uiState.value.isOnline)
        assertEquals(listOf(EXPENSE_ID), activity.loadedHistoryIds)
    }

    @Test
    fun `GIVEN the internet went and came back WHEN it comes back again THEN asks the server each time`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = true)
            isOnline.value = false

            // When
            isOnline.value = true

            // Then
            assertEquals(listOf(EXPENSE_ID, EXPENSE_ID), activity.loadedHistoryIds)
        }

    @Test
    fun `GIVEN the server keeps older events WHEN the bill opens online THEN the history shows them too`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedEvents = listOf(descriptionEdit("Feira", "Mercado")),
                serverHistory = listOf(created.copy(occurredAt = lastWeek)),
            )

            // When
            val history = viewModel.uiState.value.history

            // Then
            assertEquals(listOf(RelativeDay.TODAY, RelativeDay.OTHER), history.map { it.relativeDay })
            assertEquals(
                listOf(BillHistoryKind.EDITED, BillHistoryKind.CREATED),
                history.flatMap { day -> day.entries.map { it.kind } },
            )
        }

    private val route = Route.BillDetail(
        groupId = GROUP_ID,
        expenseId = EXPENSE_ID,
    )
    private val groupRoute = Route.GroupDetail(
        GROUP_ID,
        joinedAs = null,
        tab = GroupTab.EXPENSES,
    )

    private var nowMillis = Instant.parse("2026-10-30T15:00:00Z").toEpochMilliseconds()
    private val earlierToday = Instant.parse("2026-10-30T12:00:00Z").toEpochMilliseconds()
    private val laterToday = Instant.parse("2026-10-30T13:00:00Z").toEpochMilliseconds()
    private val yesterdayEvening = Instant.parse("2026-10-29T20:00:00Z").toEpochMilliseconds()
    private val lastWeek = Instant.parse("2026-10-20T12:00:00Z").toEpochMilliseconds()

    private val ana = member(ANA, "Ana", AvatarColor.PEACH, hasJoined = true)
    private val pierre = member(
        PIERRE,
        "Pierre",
        AvatarColor.BLUE,
        isMe = true,
        hasJoined = true,
    )
    private val bruno = member(BRUNO, "Bruno", AvatarColor.GREEN)
    private val carla = member(CARLA, "Carla", AvatarColor.LILAC)

    private val trip = Group(
        id = GROUP_ID,
        name = "Viagem de Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.LILAC,
        currency = GroupCurrency.USD,
        members = listOf(ana, pierre, bruno, carla),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
    )

    private fun member(
        id: String,
        name: String,
        color: AvatarColor,
        isMe: Boolean = false,
        hasJoined: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = color,
        isMe = isMe,
        hasJoined = hasJoined,
        userId = null,
        inviteCode = null,
    )

    private fun settlement(
        fromMemberId: String,
        toMemberId: String,
        amountMinor: Long,
        paidAt: Long,
    ): Settlement = Settlement(
        id = "settlement-1",
        groupId = GROUP_ID,
        fromMemberId = fromMemberId,
        toMemberId = toMemberId,
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = paidAt,
        isPending = false,
        updatedAt = paidAt,
    )

    private fun share(
        memberId: String,
        amountMinor: Long,
    ): ExpenseShare = ExpenseShare(
        memberId = memberId,
        amountMinor = amountMinor,
        weight = 1,
    )

    private fun paid(
        memberId: String,
        amountMinor: Long,
    ): ExpensePayer = ExpensePayer(
        memberId = memberId,
        amountMinor = amountMinor,
    )

    // Why: by default Ana paid R$ 480 split equally among Ana, Pierre (this device's user) and Bruno; Carla is out.
    private fun expense(
        description: String = "Mercado",
        amountMinor: Long = 48_000,
        splitMode: SplitMode = SplitMode.EQUAL,
        payerId: String = ANA,
        payers: List<ExpensePayer> = listOf(paid(payerId, amountMinor)),
        shares: List<ExpenseShare> = listOf(share(ANA, 16_000), share(PIERRE, 16_000), share(BRUNO, 16_000)),
        spentAt: Long = earlierToday,
        isPending: Boolean = false,
    ): Expense = Expense(
        id = EXPENSE_ID,
        groupId = GROUP_ID,
        description = description,
        amountMinor = amountMinor,
        splitMode = splitMode,
        payers = payers,
        shares = shares,
        items = null,
        categoryId = null,
        spentAt = spentAt,
        isPending = isPending,
        updatedAt = spentAt,
        updatedByMemberId = null,
    )

    private fun comment(
        id: String = "comment-1",
        authorMemberId: String? = BRUNO,
        authorName: String? = "Bruno",
        threadId: String? = null,
        body: String = "Quem trouxe o carvão?",
        postedAt: Long = earlierToday,
    ): BillComment = BillComment(
        id = id,
        expenseId = EXPENSE_ID,
        groupId = GROUP_ID,
        authorUserId = null,
        authorMemberId = authorMemberId,
        authorName = authorName,
        threadId = threadId,
        replyToName = null,
        body = body,
        viaLink = false,
        postedAt = postedAt,
        isPending = false,
    )

    // Why: Bruno asked something and Ana answered him.
    private val threadWithReply = listOf(
        comment(
            id = "c1",
            postedAt = earlierToday,
        ),
        comment(
            id = "r1",
            authorMemberId = ANA,
            authorName = "Ana",
            threadId = "c1",
            postedAt = laterToday,
        ),
    )

    private fun historyEvent(
        id: String,
        kind: ActivityKind,
        occurredAt: Long,
        changes: List<ActivityChange> = emptyList(),
    ): ActivityEvent = ActivityEvent(
        id = id,
        seq = 1L,
        groupId = GROUP_ID,
        kind = kind,
        actorUserId = null,
        actorMemberId = ANA,
        actorName = "Ana",
        groupName = "Viagem de Floripa",
        targetId = EXPENSE_ID,
        occurredAt = occurredAt,
        subject = "Mercado",
        amountMinor = 48_000,
        code = SplitMode.EQUAL.code.takeIf { kind == ActivityKind.BILL_CREATED },
        fromMemberId = null,
        toMemberId = null,
        changes = changes,
        sharesCount = 3.takeIf { kind == ActivityKind.BILL_CREATED },
        body = null,
        isPending = false,
    )

    private fun descriptionEdit(
        before: String,
        after: String,
        id: String = "edit",
        occurredAt: Long = laterToday,
    ): ActivityEvent = historyEvent(
        id = id,
        kind = ActivityKind.BILL_EDITED,
        occurredAt = occurredAt,
        changes = listOf(
            ActivityChange(
                field = ActivityField.DESCRIPTION,
                before = ActivityValue.none.copy(text = before),
                after = ActivityValue.none.copy(text = after),
            ),
        ),
    )

    private val created = historyEvent(
        id = "created",
        kind = ActivityKind.BILL_CREATED,
        occurredAt = earlierToday,
    )

    // Why: Ana turned "Feira", R$ 300 she paid, into the default bill, "Mercado", R$ 480 she paid.
    private val totalEdit = historyEvent(
        id = "edit",
        kind = ActivityKind.BILL_EDITED,
        occurredAt = laterToday,
        changes = listOf(
            ActivityChange(
                field = ActivityField.AMOUNT,
                before = ActivityValue.none.copy(minor = 30_000),
                after = ActivityValue.none.copy(minor = 48_000),
            ),
            ActivityChange(
                field = ActivityField.PAYERS,
                before = ActivityValue.none.copy(
                    memberIds = listOf(ANA),
                    memberValues = listOf(ActivityMemberValue(ANA, 30_000)),
                ),
                after = ActivityValue.none.copy(
                    memberIds = listOf(ANA),
                    memberValues = listOf(ActivityMemberValue(ANA, 48_000)),
                ),
            ),
            ActivityChange(
                field = ActivityField.DESCRIPTION,
                before = ActivityValue.none.copy(text = "Feira"),
                after = ActivityValue.none.copy(text = "Mercado"),
            ),
        ),
    )

    // Why: "Padaria" became "Feira", then "Mercado", the most recent first.
    private val twoEdits = listOf(
        descriptionEdit(
            "Feira",
            "Mercado",
            id = "edit-2",
            occurredAt = laterToday,
        ),
        descriptionEdit(
            "Padaria",
            "Feira",
            id = "edit-1",
            occurredAt = earlierToday + 30 * 60_000L,
        ),
        created,
    )

    private fun TestScope.prepareScenario(
        storedExpense: Expense? = expense(),
        isRoomRead: Boolean = true,
        timeZone: TimeZone = TimeZone.UTC,
        deleteError: Throwable? = null,
        deleteGate: CompletableDeferred<Unit>? = null,
        storedSettlements: List<Settlement> = emptyList(),
        storedComments: List<BillComment> = emptyList(),
        storedEvents: List<ActivityEvent> = emptyList(),
        serverHistory: List<ActivityEvent> = emptyList(),
        online: Boolean = true,
        commentError: Throwable? = null,
        commentGate: CompletableDeferred<Unit>? = null,
        updateError: Throwable? = null,
        updateGate: CompletableDeferred<Unit>? = null,
    ) {
        isOnline.value = online
        groups = FakeGroupRepository(initialGroups = listOf(trip))
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        expenses = FakeExpenseRepository(initialExpenses = listOfNotNull(storedExpense)).also {
            it.deleteError = deleteError
            it.deleteGate = deleteGate
            it.updateError = updateError
            it.updateGate = updateGate
        }
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(groupRoute)
            it.navigate(route)
        }
        deletedBills = InMemoryDeletedBillRepository()
        comments = FakeBillCommentRepository(initialComments = storedComments).also {
            it.addError = commentError
            it.addGate = commentGate
        }
        activity = FakeActivityRepository(initialEvents = storedEvents).also { it.historyPage = serverHistory }
        val groupNeverAnswers = MutableSharedFlow<Group?>()
        val expenseNeverAnswers = MutableSharedFlow<Expense?>()
        viewModel = BillDetailViewModel(
            route = route,
            navigator = navigator,
            deleteExpense = DeleteExpenseUseCase(expenses, deletedBills, currentTimeMillis = { nowMillis }),
            noticeBillGone = NoticeBillGoneUseCase(deletedBills),
            currentTimeMillis = { nowMillis },
            currentTimeZone = { timeZone },
            observeGroup = if (isRoomRead) ObserveGroupUseCase(groups) else ObserveGroup { groupNeverAnswers },
            observeExpense = if (isRoomRead) {
                ObserveExpenseUseCase(
                    expenses,
                )
            } else {
                ObserveExpense { expenseNeverAnswers }
            },
            observeExpenses = ObserveExpensesUseCase(expenses),
            observeSettlements = ObserveSettlementsUseCase(settlements),
            observeCategoryTree = ObserveCategoryTreeUseCase(FakeCategoryRepository()),
            addBillComment = AddBillCommentUseCase(comments, currentTimeMillis = { nowMillis }),
            updateExpense = UpdateExpenseUseCase(expenses, currentTimeMillis = { nowMillis }),
            loadBillHistory = LoadBillHistoryUseCase(activity),
            observeBillComments = ObserveBillCommentsUseCase(comments),
            observeBillHistory = ObserveBillHistoryUseCase(activity),
            connectivityObserver = NetworkConnectivityObserver { isOnline },
            minuteTicks = MinuteTicks { minutes },
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
        const val FORMER = "member-9"
        const val OTHER_FORMER = "member-10"
    }
}
