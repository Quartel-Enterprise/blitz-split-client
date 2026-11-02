package com.quare.blitzsplit.feature.billdetail

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
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
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.NoticeBillGoneUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettlementsUseCase
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
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillPayer
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillShareRow
import com.quare.blitzsplit.feature.billdetail.presentation.model.MyPartNote
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.BillDetailViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class BillDetailViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var deletedBills: InMemoryDeletedBillRepository
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

    private val route = Route.BillDetail(
        groupId = GROUP_ID,
        expenseId = EXPENSE_ID,
    )
    private val groupRoute = Route.GroupDetail(
        GROUP_ID,
        joinedAs = null,
        tab = GroupTab.EXPENSES,
    )

    private val nowMillis = Instant.parse("2026-10-30T15:00:00Z").toEpochMilliseconds()
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
        spentAt = spentAt,
        isPending = isPending,
        updatedAt = spentAt,
        updatedByMemberId = null,
    )

    private fun TestScope.prepareScenario(
        storedExpense: Expense? = expense(),
        isRoomRead: Boolean = true,
        timeZone: TimeZone = TimeZone.UTC,
        deleteError: Throwable? = null,
        deleteGate: CompletableDeferred<Unit>? = null,
        storedSettlements: List<Settlement> = emptyList(),
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(trip))
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        expenses = FakeExpenseRepository(initialExpenses = listOfNotNull(storedExpense)).also {
            it.deleteError = deleteError
            it.deleteGate = deleteGate
        }
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(groupRoute)
            it.navigate(route)
        }
        deletedBills = InMemoryDeletedBillRepository()
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
