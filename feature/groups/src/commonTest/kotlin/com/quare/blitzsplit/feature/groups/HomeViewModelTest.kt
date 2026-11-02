package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.UpdateRecoveryCodePromptUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.domain.usecase.ObserveActivityUnreadCount
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.expenses.domain.model.RecordedPayment
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteSettlementUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsMoneyUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObservePeopleBalancesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettledUpUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.data.repository.InMemoryGroupDeletionRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDeletion
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeGroupDeletionUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupDeletionsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObservePinnedGroupIdsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupPinRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.sync.FirstDownload
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import com.quare.blitzsplit.core.sync.domain.usecase.impl.ObserveFirstDownloadUseCase
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.feature.groups.presentation.model.DeletedGroupNotice
import com.quare.blitzsplit.feature.groups.presentation.model.GroupBalanceKind
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.SyncBadge
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.HomeViewModel
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.RecoveryCodeCardDismissal
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class HomeViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var account: FakeAccountRepository
    private lateinit var groups: FakeGroupRepository
    private lateinit var pins: FakeGroupPinRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var recordedPayments: InMemoryRecordedPaymentRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var syncState: MutableStateFlow<SyncState>
    private lateinit var minutes: MutableStateFlow<Int>
    private lateinit var navigator: BackStackNavigator
    private lateinit var deletions: InMemoryGroupDeletionRepository
    private lateinit var dismissal: RecoveryCodeCardDismissal
    private lateinit var firstDownload: FirstDownload
    private lateinit var activityUnread: MutableStateFlow<Int>
    private lateinit var viewModel: HomeViewModel
    private val actions = mutableListOf<HomeUiAction>()
    private var retryCount = 0
    private var syncNowCount = 0
    private var syncGate: CompletableDeferred<Unit>? = null

    // Why: the device's clock, which a test moves on and then ticks a minute for.
    private var clockMillis = 0L

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN Inicio WHEN tapping the profile THEN opens the profile over it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(HomeUiEvent.OnProfileClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the recovery code card WHEN tapping Conectar conta THEN opens the profile over Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.SNOOZED,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnConnectAccountClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Inicio WHEN tapping the Saldos tab THEN opens Saldos with everyone above Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            viewModel.onEvent(HomeUiEvent.OnBalancesTabClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL, charge = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Inicio WHEN tapping Ver saldos THEN opens Saldos with everyone above Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            viewModel.onEvent(HomeUiEvent.OnSeeBalancesClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL, charge = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Inicio WHEN tapping the Grupos tab THEN opens the groups above Inicio`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        viewModel.onEvent(HomeUiEvent.OnGroupsTabClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Inicio WHEN tapping Ver todos THEN opens the groups above Inicio`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        viewModel.onEvent(HomeUiEvent.OnSeeAllGroupsClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the profile over Inicio WHEN a late tap on Ver todos arrives THEN the profile stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(HomeUiEvent.OnSeeAllGroupsClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the profile over Inicio WHEN a late tap on Ver saldos arrives THEN the profile stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(HomeUiEvent.OnSeeBalancesClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the profile over Inicio WHEN late taps on its cards and shortcuts arrive THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                        amountMinor = 9000L,
                    ),
                ),
            )
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(HomeUiEvent.OnProfileClicked)
            viewModel.onEvent(HomeUiEvent.OnRecordPaymentClicked)
            viewModel.onEvent(HomeUiEvent.OnCreateGroupClicked)
            viewModel.onEvent(HomeUiEvent.OnJoinWithLinkClicked)
            viewModel.onEvent(HomeUiEvent.OnGroupClicked("trio"))
            viewModel.onEvent(HomeUiEvent.OnBillClicked("trio", "dinner"))
            viewModel.onEvent(HomeUiEvent.OnPersonClicked("name:ana", GroupCurrency.BRL))
            viewModel.onEvent(HomeUiEvent.OnToPayClicked)
            viewModel.onEvent(HomeUiEvent.OnSplitBillClicked)
            viewModel.onEvent(HomeUiEvent.OnSeeBalancesClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the profile over Inicio WHEN tapping the Saldos tab THEN closes it and opens Saldos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(HomeUiEvent.OnBalancesTabClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL, charge = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the profile over Inicio WHEN tapping the Grupos tab THEN closes it and opens Grupos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(HomeUiEvent.OnGroupsTabClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Inicio WHEN tapping Registrar pagamento THEN asks who paid whom over it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        viewModel.onEvent(HomeUiEvent.OnRecordPaymentClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.PickPayment), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Inicio WHEN tapping Novo grupo THEN opens it over Inicio`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        viewModel.onEvent(HomeUiEvent.OnCreateGroupClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.CreateGroup), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the first use WHEN tapping Entrar com link ou QR THEN opens the reader over Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(HomeUiEvent.OnJoinWithLinkClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.JoinWithLink(
                        skippedCode = null,
                        skippedMemberCode = null,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN internet WHEN tapping Ler cupom fiscal THEN says it is coming soon`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)

        // Then
        assertEquals(listOf<HomeUiAction>(HomeUiAction.ShowComingSoon), actions)
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN no internet WHEN tapping Ler cupom fiscal THEN does nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            online = false,
            storedGroups = listOf(trio()),
        )

        // When
        viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)

        // Then
        assertTrue(actions.isEmpty())
        assertFalse(viewModel.uiState.value.canScanReceipt)
    }

    @Test
    fun `GIVEN Inicio WHEN tapping Ver tudo THEN opens Atividade above Inicio`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        viewModel.onEvent(HomeUiEvent.OnSeeAllBillsClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Activity(personKey = null)), navigator.backStack.value)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN a group opened from Inicio WHEN tapping Atividade THEN Atividade takes its place above Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(HomeUiEvent.OnGroupClicked("trio"))

            // When
            viewModel.onEvent(HomeUiEvent.OnActivityTabClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Activity(personKey = null)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN unread events WHEN Inicio shows THEN its Atividade tab counts them`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        activityUnread.value = 4

        // Then
        assertEquals(4, viewModel.uiState.value.activityUnreadCount)
    }

    @Test
    fun `GIVEN a group opened from Inicio WHEN tapping Contatos THEN Contatos takes its place above Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(HomeUiEvent.OnGroupClicked("trio"))

            // When
            viewModel.onEvent(HomeUiEvent.OnContactsTabClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
        }

    @Test
    fun `GIVEN no group WHEN tapping Rachar conta THEN opens Novo grupo`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(HomeUiEvent.OnSplitBillClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.CreateGroup), navigator.backStack.value)
    }

    @Test
    fun `GIVEN an older group with a newer bill WHEN tapping Rachar conta THEN asks which group with it marked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    trio(
                        id = "old",
                        updatedAt = daysAgo(3),
                    ),
                    trio(
                        id = "new",
                        updatedAt = daysAgo(1),
                    ),
                ),
                storedExpenses = listOf(
                    bill(
                        "fresh",
                        groupId = "old",
                        spentAt = hoursAgo(1),
                    ),
                ),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnSplitBillClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.SplitBill(
                        groupId = "old",
                        expenseId = null,
                        picksGroupFirst = true,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the groups not read yet WHEN tapping Rachar conta THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(groupsRead = false)

        // When
        viewModel.onEvent(HomeUiEvent.OnSplitBillClicked)

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN people you owe and who owe you WHEN Inicio opens THEN whom you owe come first the largest first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "a",
                        members = listOf(me(), member("Ana"), member("Bruno")),
                    ),
                    group(
                        id = "b",
                        members = listOf(me(), member("Carla"), member("Diego")),
                    ),
                ),
                storedExpenses = listOf(
                    bill(
                        "ana",
                        groupId = "a",
                        amountMinor = 9000L,
                        splitOver = listOf("Pierre", "Ana"),
                    ),
                    bill(
                        "bruno",
                        groupId = "a",
                        amountMinor = 1000L,
                        splitOver = listOf("Pierre", "Bruno"),
                    ),
                    bill(
                        "carla",
                        groupId = "b",
                        paidBy = "Carla",
                        amountMinor = 600L,
                        splitOver = listOf("Pierre", "Carla"),
                    ),
                    bill(
                        "diego",
                        groupId = "b",
                        paidBy = "Diego",
                        amountMinor = 3000L,
                        splitOver = listOf("Pierre", "Diego"),
                    ),
                ),
            )

            // When
            val people = viewModel.uiState.value.people

            // Then
            assertEquals(
                listOf("Diego" to -1500L, "Carla" to -300L, "Ana" to 4500L, "Bruno" to 500L),
                people.map { it.name to it.netMinor },
            )
            assertTrue(people.all { it.isGuest })
        }

    @Test
    fun `GIVEN you owe Ana WHEN tapping her face THEN opens settling up with her over Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                        amountMinor = 9000L,
                    ),
                ),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnPersonClicked("name:ana", GroupCurrency.BRL))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.SettleUp(
                        personKey = "name:ana",
                        currency = GroupCurrency.BRL,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN you owe Ana in reais and dollars WHEN tapping her dollar face THEN settles up in dollars`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    trio(),
                    trio(
                        id = "trip",
                        currency = GroupCurrency.USD,
                    ),
                ),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                        amountMinor = 9000L,
                    ),
                    bill(
                        "taxi",
                        groupId = "trip",
                        paidBy = "Ana",
                        amountMinor = 600L,
                    ),
                ),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnPersonClicked("name:ana", GroupCurrency.USD))

            // Then
            assertEquals(
                listOf(GroupCurrency.BRL to -3000L, GroupCurrency.USD to -200L),
                viewModel.uiState.value.people
                    .filter { it.key == "name:ana" }
                    .map { it.currency to it.netMinor },
            )
            assertEquals(
                listOf(
                    Route.Home,
                    Route.SettleUp(
                        personKey = "name:ana",
                        currency = GroupCurrency.USD,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Ana owed only in reais WHEN tapping her face in dollars THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = listOf(
                bill(
                    "dinner",
                    paidBy = "Ana",
                    amountMinor = 9000L,
                ),
            ),
        )

        // When
        viewModel.onEvent(HomeUiEvent.OnPersonClicked("name:ana", GroupCurrency.USD))

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a sync settled Ana after her face showed WHEN a late tap on it arrives THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                        amountMinor = 9000L,
                    ),
                ),
            )
            settlements.settlements.update {
                it +
                    payment(
                        "paid",
                        from = "Pierre",
                        to = "Ana",
                        amountMinor = 3000L,
                    )
            }

            // When
            viewModel.onEvent(HomeUiEvent.OnPersonClicked("name:ana", GroupCurrency.BRL))

            // Then
            assertTrue(
                viewModel.uiState.value.people
                    .isEmpty(),
            )
            assertEquals(listOf(Route.Home), navigator.backStack.value)
        }

    @Test
    fun `GIVEN groups in two currencies WHEN Inicio opens THEN Saldo geral sums the people per currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    trio(),
                    trio(
                        id = "trip",
                        currency = GroupCurrency.USD,
                    ),
                ),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                        amountMinor = 9000L,
                    ),
                    bill(
                        "taxi",
                        groupId = "trip",
                        amountMinor = 600L,
                    ),
                ),
            )

            // When
            val summary = viewModel.uiState.value.summary

            // Then
            assertEquals(
                listOf(
                    PeopleTotal(
                        currency = GroupCurrency.BRL,
                        netMinor = -3000L,
                        toReceiveMinor = 0L,
                        toPayMinor = 3000L,
                        toReceiveCount = 0,
                        toPayCount = 1,
                    ),
                    PeopleTotal(
                        currency = GroupCurrency.USD,
                        netMinor = 400L,
                        toReceiveMinor = 400L,
                        toPayMinor = 0L,
                        toReceiveCount = 2,
                        toPayCount = 0,
                    ),
                ),
                summary.totals,
            )
            assertEquals(GroupBalanceKind.YOU_OWE, summary.kind)
        }

    @Test
    fun `GIVEN something to receive WHEN tapping Te devem THEN opens Saldos on Te devem above Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnToReceiveClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.OWES_YOU, charge = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN nothing to receive WHEN tapping Te devem THEN does nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = listOf(
                bill(
                    "dinner",
                    paidBy = "Ana",
                ),
            ),
        )

        // When
        viewModel.onEvent(HomeUiEvent.OnToReceiveClicked)

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN one person to pay WHEN tapping Voce deve THEN opens settling up with them over Voce deve`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                    ),
                ),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnToPayClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.GlobalBalances(BalancesFilter.YOU_OWE, charge = null),
                    Route.SettleUp(
                        personKey = "name:ana",
                        currency = GroupCurrency.BRL,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN two people to pay WHEN tapping Voce deve THEN opens Saldos on Voce deve above Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                    ),
                    bill(
                        "lunch",
                        paidBy = "Bruno",
                        amountMinor = 6000L,
                    ),
                ),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnToPayClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.YOU_OWE, charge = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN one person to pay in two currencies WHEN tapping Voce deve THEN opens Saldos on Voce deve`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    trio(),
                    trio(
                        id = "trip",
                        currency = GroupCurrency.USD,
                    ),
                ),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                    ),
                    bill(
                        "taxi",
                        groupId = "trip",
                        paidBy = "Ana",
                        amountMinor = 600L,
                    ),
                ),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnToPayClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.YOU_OWE, charge = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN nothing to pay WHEN tapping Voce deve THEN does nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = listOf(bill("dinner")),
        )

        // When
        viewModel.onEvent(HomeUiEvent.OnToPayClicked)

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN five open groups and one pinned far down WHEN Inicio opens THEN shows it and the two latest`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = (1..5).map {
                    trio(
                        id = "g$it",
                        updatedAt = daysAgo(it),
                    )
                },
                pinnedGroupIds = setOf("g5"),
            )

            // When
            val cards = viewModel.uiState.value.groups

            // Then
            assertEquals(listOf("g5", "g1", "g2"), cards.map { it.row.id })
            assertEquals(listOf(true, false, false), cards.map { it.isPinned })
        }

    @Test
    fun `GIVEN Inicio open WHEN a group gets pinned THEN its card moves first`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = (1..4).map {
                trio(
                    id = "g$it",
                    updatedAt = daysAgo(it),
                )
            },
        )

        // When
        pins.pinnedGroupIds.value = setOf("g4")

        // Then
        assertEquals(
            listOf("g4", "g1", "g2"),
            viewModel.uiState.value.groups
                .map { it.row.id },
        )
    }

    @Test
    fun `GIVEN a pinned group where everyone settled WHEN Inicio opens THEN leaves its card out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    trio(
                        id = "settled",
                        updatedAt = hoursAgo(1),
                    ),
                    trio(
                        id = "open",
                        updatedAt = daysAgo(2),
                    ),
                ),
                storedExpenses = listOf(
                    bill(
                        "gas",
                        groupId = "settled",
                        paidBy = "Ana",
                        amountMinor = 6000L,
                        splitOver = listOf("Ana", "Pierre"),
                        spentAt = daysAgo(3),
                    ),
                ),
                storedSettlements = listOf(
                    payment(
                        "paid",
                        groupId = "settled",
                        from = "Pierre",
                        to = "Ana",
                        amountMinor = 3000L,
                    ),
                ),
                pinnedGroupIds = setOf("settled"),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf("open"), state.groups.map { it.row.id })
            assertEquals(2, state.groupCount)
        }

    @Test
    fun `GIVEN only a group where everyone settled WHEN Inicio opens THEN shows the home without a card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "gas",
                        paidBy = "Ana",
                        amountMinor = 6000L,
                        splitOver = listOf("Ana", "Pierre"),
                    ),
                ),
                storedSettlements = listOf(
                    payment(
                        "paid",
                        from = "Pierre",
                        to = "Ana",
                        amountMinor = 3000L,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.groups.isEmpty())
            assertTrue(state.showsHome)
            assertFalse(state.showsFirstUse)
        }

    @Test
    fun `GIVEN a group card WHEN tapping it THEN opens the group over Inicio`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        viewModel.onEvent(HomeUiEvent.OnGroupClicked("trio"))

        // Then
        assertEquals(
            listOf(
                Route.Home,
                Route.GroupDetail(
                    "trio",
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN seven bills of yours and a newer one of others WHEN Inicio opens THEN lists your five latest by day`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    trio(),
                    trio(id = "trip"),
                ),
                storedExpenses = listOf(
                    bill(
                        "others",
                        paidBy = "Ana",
                        splitOver = listOf("Ana", "Bruno"),
                        spentAt = hoursAgo(1),
                    ),
                    bill("h1", spentAt = hoursAgo(2)),
                    bill(
                        "t1",
                        groupId = "trip",
                        spentAt = hoursAgo(3),
                    ),
                    bill("h2", spentAt = hoursAgo(20)),
                    bill(
                        "t2",
                        groupId = "trip",
                        spentAt = hoursAgo(22),
                    ),
                    bill("h3", spentAt = daysAgo(3)),
                    bill(
                        "t3",
                        groupId = "trip",
                        spentAt = daysAgo(4),
                    ),
                    bill("h4", spentAt = daysAgo(5)),
                ),
            )

            // When
            val days = viewModel.uiState.value.billDays

            // Then
            assertEquals(
                listOf(
                    RelativeDay.TODAY to listOf("h1", "t1"),
                    RelativeDay.YESTERDAY to listOf("h2", "t2"),
                    RelativeDay.OTHER to listOf("h3"),
                ),
                days.map { day -> day.relativeDay to day.bills.map { it.id } },
            )
        }

    @Test
    fun `GIVEN a bill on Inicio WHEN tapping it THEN opens its Conta over its group`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = listOf(bill("dinner")),
        )

        // When
        viewModel.onEvent(HomeUiEvent.OnBillClicked("trio", "dinner"))

        // Then
        assertEquals(
            listOf(
                Route.Home,
                Route.GroupDetail(
                    "trio",
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
                Route.BillDetail("trio", "dinner"),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a bill from this afternoon on an open Inicio WHEN midnight passes THEN its day says yesterday`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner", spentAt = hoursAgo(2))),
            )

            // When
            clockMillis = Instant.parse("2026-11-01T00:01:00Z").toEpochMilliseconds()
            minutes.value += 1

            // Then
            assertEquals(
                RelativeDay.YESTERDAY,
                viewModel.uiState.value.billDays
                    .single()
                    .relativeDay,
            )
        }

    @Test
    fun `GIVEN the groups not read yet WHEN Inicio opens THEN shows neither the home nor the first use`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(groupsRead = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertFalse(state.showsHome)
            assertFalse(state.showsFirstUse)
        }

    @Test
    fun `GIVEN no group on a local only install WHEN Inicio opens THEN shows the first use`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsFirstUse)
            assertFalse(state.showsHome)
            assertFalse(state.isFirstDownload)
        }

    @Test
    fun `GIVEN an account relaunched with no group WHEN its sync runs THEN shows the first use`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                syncState = SyncState.SYNCING,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isFirstDownload)
            assertTrue(state.showsFirstUse)
        }

    @Test
    fun `GIVEN a first download with no group yet WHEN Inicio opens THEN shows the first download`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                syncState = SyncState.SYNCING,
                isFirstDownloadPending = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isFirstDownload)
            assertTrue(state.showsHome)
            assertFalse(state.showsFirstUse)
        }

    @Test
    fun `GIVEN a restore whose sync has not started WHEN Inicio opens THEN shows the first download`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("pierre", AccountProvider.GOOGLE),
                syncState = SyncState.SYNCED,
                isFirstDownloadPending = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isFirstDownload)
            assertFalse(state.showsFirstUse)
        }

    @Test
    fun `GIVEN a first download with groups already down WHEN Inicio opens THEN is no first download`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                syncState = SyncState.SYNCING,
                storedGroups = listOf(trio()),
                isFirstDownloadPending = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isFirstDownload)
            assertTrue(state.showsHome)
        }

    @Test
    fun `GIVEN a profile just made here WHEN its first sync registers it THEN shows the first use`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(syncState = SyncState.SYNCING)

            // When
            account.status = AccountStatus.Anonymous("anonymous")

            // Then
            assertFalse(viewModel.uiState.value.isFirstDownload)
            assertTrue(viewModel.uiState.value.showsFirstUse)
        }

    @Test
    fun `GIVEN a Google account downloading WHEN the first download ends with no group THEN shows the first use`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("pierre", AccountProvider.GOOGLE),
                syncState = SyncState.SYNCING,
                isFirstDownloadPending = true,
            )

            // When
            firstDownload.end()

            // Then
            assertFalse(viewModel.uiState.value.isFirstDownload)
            assertTrue(viewModel.uiState.value.showsFirstUse)
        }

    @Test
    fun `GIVEN a Google account downloading WHEN its first group arrives THEN shows the home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("pierre", AccountProvider.GOOGLE),
                syncState = SyncState.SYNCING,
                isFirstDownloadPending = true,
            )

            // When
            groups.groups.value = listOf(trio())

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsHome)
            assertFalse(state.showsFirstUse)
            assertFalse(state.isFirstDownload)
            assertEquals(1, state.groupCount)
        }

    @Test
    fun `GIVEN a profile and a Google account WHEN Inicio opens THEN shows the trimmed name and the account`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("pierre", AccountProvider.GOOGLE),
                profile = UserProfile("  pierre ", AvatarColor.GREEN, null),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("pierre", state.displayName)
            assertEquals("P", state.avatarInitial)
            assertEquals(AvatarColor.GREEN, state.avatarColor)
            assertEquals(AccountProvider.GOOGLE, state.accountProvider)
        }

    @Test
    fun `GIVEN everything synced WHEN Inicio opens THEN shows no sync badge`() = runTest(testDispatcher) {
        // Given
        prepareScenario(syncState = SyncState.SYNCED)

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.syncBadge)
    }

    @Test
    fun `GIVEN no internet and changes in the queue WHEN Inicio opens THEN shows the offline badge counting them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                syncState = SyncState.PENDING,
                storedGroups = listOf(trio().copy(isPending = true)),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        isPending = true,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SyncBadge.OFFLINE, state.syncBadge)
            assertEquals(2, state.pendingChangeCount)
        }

    @Test
    fun `GIVEN internet and no answer from the server WHEN Inicio opens THEN shows the failed badge to retry`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(syncState = SyncState.FAILED)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SyncBadge.FAILED, state.syncBadge)
            assertTrue(state.canRetrySync)
        }

    @Test
    fun `GIVEN internet and a change in the queue WHEN Inicio opens THEN shows the syncing badge`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(syncState = SyncState.PENDING)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SyncBadge.SYNCING, state.syncBadge)
            assertFalse(state.canRetrySync)
        }

    @Test
    fun `GIVEN the failed badge WHEN tapping it THEN asks for one sync`() = runTest(testDispatcher) {
        // Given
        prepareScenario(syncState = SyncState.FAILED)

        // When
        viewModel.onEvent(HomeUiEvent.OnRetrySyncClicked)

        // Then
        assertEquals(1, retryCount)
    }

    @Test
    fun `GIVEN a sync running WHEN a late tap on retry arrives THEN asks for no sync`() = runTest(testDispatcher) {
        // Given
        prepareScenario(syncState = SyncState.SYNCING)

        // When
        viewModel.onEvent(HomeUiEvent.OnRetrySyncClicked)

        // Then
        assertEquals(0, retryCount)
    }

    @Test
    fun `GIVEN online WHEN pulling to refresh THEN syncs once and waits for it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))
        syncGate = CompletableDeferred()

        // When
        viewModel.onEvent(HomeUiEvent.OnRefresh)
        viewModel.onEvent(HomeUiEvent.OnRefresh)

        // Then
        assertTrue(viewModel.uiState.value.isRefreshing)
        assertEquals(1, syncNowCount)
        syncGate?.complete(Unit)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `GIVEN offline WHEN pulling to refresh THEN ends at once without a sync`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            online = false,
            storedGroups = listOf(trio()),
        )

        // When
        viewModel.onEvent(HomeUiEvent.OnRefresh)

        // Then
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals(0, syncNowCount)
    }

    @Test
    fun `GIVEN an anonymous account that snoozed its code WHEN Inicio opens THEN shows the card without the sheet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.SNOOZED,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsRecoveryCodeCard)
            assertFalse(state.showsRecoveryCodePrompt)
        }

    @Test
    fun `GIVEN a Google account with a snoozed code WHEN Inicio opens THEN shows no card`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = AccountStatus.Connected("pierre", AccountProvider.GOOGLE),
            prompt = RecoveryCodePrompt.SNOOZED,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.showsRecoveryCodeCard)
    }

    @Test
    fun `GIVEN a local only install with a snoozed code WHEN Inicio opens THEN shows no card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(prompt = RecoveryCodePrompt.SNOOZED)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.showsRecoveryCodeCard)
        }

    @Test
    fun `GIVEN an anonymous account with a due code WHEN Inicio opens THEN shows the sheet without the card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.DUE,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsRecoveryCodePrompt)
            assertFalse(state.showsRecoveryCodeCard)
        }

    @Test
    fun `GIVEN a saved code opened for review WHEN Inicio shows THEN shows the sheet`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = AccountStatus.Anonymous("anonymous"),
            prompt = RecoveryCodePrompt.REVIEW,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.showsRecoveryCodePrompt)
    }

    @Test
    fun `GIVEN the recovery code card WHEN closing it THEN hides it and keeps the code snoozed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.SNOOZED,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnRecoveryCodeCardDismissed)

            // Then
            assertFalse(viewModel.uiState.value.showsRecoveryCodeCard)
            assertEquals(RecoveryCodePrompt.SNOOZED, account.recoveryCodePrompt)
        }

    @Test
    fun `GIVEN the recovery code card closed earlier in this launch WHEN Inicio opens again THEN it stays closed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.SNOOZED,
                isCardDismissedThisLaunch = true,
            )

            // When
            val showsCard = viewModel.uiState.value.showsRecoveryCodeCard

            // Then
            assertFalse(showsCard)
        }

    @Test
    fun `GIVEN two groups someone else deleted WHEN Inicio is on top THEN says so one at a time`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            deletions.post(
                GroupDeletion(
                    groupId = "a",
                    groupName = "Apê 302",
                    deletedByName = "Carla",
                ),
            )
            deletions.post(
                GroupDeletion(
                    groupId = "b",
                    groupName = "Churrasco",
                    deletedByName = null,
                ),
            )

            // Then
            assertEquals(
                DeletedGroupNotice(
                    groupId = "a",
                    groupName = "Apê 302",
                    deletedByName = "Carla",
                ),
                viewModel.uiState.value.deletedGroupNotice,
            )
            viewModel.onEvent(HomeUiEvent.OnDeletedGroupNoticeShown)
            assertEquals(
                DeletedGroupNotice(
                    groupId = "b",
                    groupName = "Churrasco",
                    deletedByName = null,
                ),
                viewModel.uiState.value.deletedGroupNotice,
            )
            viewModel.onEvent(HomeUiEvent.OnDeletedGroupNoticeShown)
            assertNull(viewModel.uiState.value.deletedGroupNotice)
            assertTrue(deletions.observeDeletions().first().isEmpty())
        }

    @Test
    fun `GIVEN the profile over Inicio WHEN a group is deleted THEN says so once Inicio is on top again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(Route.Profile)
            deletions.post(
                GroupDeletion(
                    groupId = "a",
                    groupName = "Apê 302",
                    deletedByName = "Carla",
                ),
            )
            assertNull(viewModel.uiState.value.deletedGroupNotice)

            // When
            navigator.navigateBack()

            // Then
            assertEquals(
                "Apê 302",
                viewModel.uiState.value.deletedGroupNotice
                    ?.groupName,
            )
        }

    @Test
    fun `GIVEN the recovery code card WHEN tapping Ver codigo THEN makes the code due and shows the sheet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.SNOOZED,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnShowRecoveryCodeClicked)

            // Then
            assertEquals(RecoveryCodePrompt.DUE, account.recoveryCodePrompt)
            assertTrue(viewModel.uiState.value.showsRecoveryCodePrompt)
            assertFalse(viewModel.uiState.value.showsRecoveryCodeCard)
        }

    @Test
    fun `GIVEN no recovery code card WHEN a late tap on Ver codigo arrives THEN leaves the code as it is`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.SAVED,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnShowRecoveryCodeClicked)

            // Then
            assertEquals(RecoveryCodePrompt.SAVED, account.recoveryCodePrompt)
        }

    @Test
    fun `GIVEN Registrar pagamento opened from the shortcut WHEN it closes after recording THEN Inicio says so`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(Route.PickPayment)
            navigator.replaceTop(registerPayment)
            val paid = payment(
                "paid",
                from = "Ana",
                to = "Pierre",
                amountMinor = 3000L,
            )
            settlements.settlements.update { it + paid }
            recordedPayments.post(RecordedPayment.InGroup(paid))

            // When
            navigator.navigateBack()

            // Then
            assertEquals(
                PaymentNotice(
                    settlementIds = listOf("paid"),
                    fromName = "Ana",
                    fromIsMe = false,
                    toName = "Pierre",
                ),
                viewModel.uiState.value.recordedPayment,
            )
        }

    @Test
    fun `GIVEN Registrar pagamento opened from a group WHEN back on Inicio THEN Inicio shows no notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(
                Route.GroupDetail(
                    "trio",
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
            )
            navigator.navigate(registerPayment)
            recordedPayments.post(
                RecordedPayment.InGroup(
                    payment(
                        "paid",
                        from = "Ana",
                        to = "Pierre",
                        amountMinor = 3000L,
                    ),
                ),
            )

            // When
            navigator.navigateBack()
            navigator.navigateBack()

            // Then
            assertNull(viewModel.uiState.value.recordedPayment)
        }

    @Test
    fun `GIVEN a settling up opened from a face WHEN it closes after recording THEN Inicio says so`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(Route.SettleUp("name:ana", GroupCurrency.BRL))
            recordedPayments.post(
                RecordedPayment.SettledUp(
                    listOf("p1", "p2"),
                    personName = "Ana",
                    isPaidByMe = true,
                ),
            )

            // When
            navigator.navigateBack()

            // Then
            assertEquals(
                PaymentNotice(
                    settlementIds = listOf("p1", "p2"),
                    fromName = null,
                    fromIsMe = true,
                    toName = "Ana",
                ),
                viewModel.uiState.value.recordedPayment,
            )
        }

    @Test
    fun `GIVEN the notice of a settling up WHEN tapping Desfazer THEN undoes all its payments and hides it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedSettlements = listOf(
                    payment(
                        "p1",
                        from = "Pierre",
                        to = "Ana",
                        amountMinor = 100L,
                    ),
                    payment(
                        "p2",
                        from = "Pierre",
                        to = "Ana",
                        amountMinor = 200L,
                    ),
                ),
            )
            recordedPayments.post(
                RecordedPayment.SettledUp(
                    listOf("p1", "p2"),
                    personName = "Ana",
                    isPaidByMe = true,
                ),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnUndoPaymentClicked)

            // Then
            assertEquals(listOf(listOf("p1", "p2")), settlements.deleteCalls)
            assertNull(viewModel.uiState.value.recordedPayment)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN undoing fails WHEN tapping Desfazer THEN says the change failed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))
        recordedPayments.post(
            RecordedPayment.SettledUp(
                listOf("p1"),
                personName = "Ana",
                isPaidByMe = true,
            ),
        )
        settlements.deleteError = IllegalStateException("disk full")

        // When
        viewModel.onEvent(HomeUiEvent.OnUndoPaymentClicked)

        // Then
        assertEquals(listOf<HomeUiAction>(HomeUiAction.ShowChangeError), actions)
    }

    @Test
    fun `GIVEN no notice WHEN a late tap on Desfazer arrives THEN deletes nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        viewModel.onEvent(HomeUiEvent.OnUndoPaymentClicked)

        // Then
        assertTrue(settlements.deleteCalls.isEmpty())
    }

    @Test
    fun `GIVEN the notice WHEN it was shown THEN Inicio clears it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))
        recordedPayments.post(
            RecordedPayment.SettledUp(
                listOf("p1"),
                personName = "Ana",
                isPaidByMe = true,
            ),
        )

        // When
        viewModel.onEvent(HomeUiEvent.OnRecordedPaymentNoticeShown)

        // Then
        assertNull(viewModel.uiState.value.recordedPayment)
    }

    private val everyone = listOf("Pierre", "Ana", "Bruno")

    // Why: Saturday, 31 October 2026, 15:00 in UTC, the time zone the tests count days in.
    private val now = Instant.parse("2026-10-31T15:00:00Z").toEpochMilliseconds()

    private val pierre = UserProfile("Pierre", AvatarColor.GREEN, null)

    private val registerPayment = Route.RegisterPayment(
        groupId = "trio",
        fromMemberId = "member-Ana",
        toMemberId = "member-Pierre",
    )

    private fun hoursAgo(hours: Int): Long = now - hours.hours.inWholeMilliseconds

    private fun daysAgo(days: Int): Long = now - days.days.inWholeMilliseconds

    private fun group(
        id: String,
        name: String = "Grupo",
        members: List<GroupMember>,
        currency: GroupCurrency = GroupCurrency.BRL,
        updatedAt: Long = daysAgo(10),
    ): Group = Group(
        id = id,
        name = name,
        type = GroupType.OTHER,
        color = AvatarColor.BLUE,
        currency = currency,
        members = members,
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = updatedAt,
    )

    // Why: Pierre is this device's user, in a group with Ana and Bruno.
    private fun trio(
        id: String = "trio",
        currency: GroupCurrency = GroupCurrency.BRL,
        updatedAt: Long = daysAgo(10),
    ): Group = group(
        id = id,
        name = "Apê 302",
        members = listOf(me(), member("Ana"), member("Bruno")),
        currency = currency,
        updatedAt = updatedAt,
    )

    private fun me(): GroupMember = member("Pierre").copy(isMe = true)

    private fun member(name: String): GroupMember = GroupMember(
        id = "member-$name",
        name = name,
        avatarColor = AvatarColor.BLUE,
        isMe = false,
        hasJoined = false,
        userId = null,
        inviteCode = null,
    )

    // Why: one payer for the whole bill, split over the members given.
    private fun bill(
        id: String,
        groupId: String = "trio",
        paidBy: String = "Pierre",
        amountMinor: Long = 9000L,
        splitOver: List<String> = everyone,
        spentAt: Long = daysAgo(5),
        isPending: Boolean = false,
    ): Expense = Expense(
        id = id,
        groupId = groupId,
        description = "Mercado",
        amountMinor = amountMinor,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer("member-$paidBy", amountMinor)),
        shares = splitOver.map { ExpenseShare("member-$it", amountMinor / splitOver.size, 1L) },
        spentAt = spentAt,
        isPending = isPending,
        updatedAt = spentAt,
        updatedByMemberId = null,
        items = null,
    )

    private fun payment(
        id: String,
        groupId: String = "trio",
        from: String,
        to: String,
        amountMinor: Long,
    ): Settlement = Settlement(
        id = id,
        groupId = groupId,
        fromMemberId = "member-$from",
        toMemberId = "member-$to",
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = daysAgo(1),
        isPending = false,
        updatedAt = daysAgo(1),
    )

    private fun TestScope.prepareScenario(
        online: Boolean = true,
        status: AccountStatus = AccountStatus.LocalOnly,
        prompt: RecoveryCodePrompt = RecoveryCodePrompt.NOT_DUE,
        syncState: SyncState = SyncState.SYNCED,
        profile: UserProfile? = pierre,
        storedGroups: List<Group> = emptyList(),
        groupsRead: Boolean = true,
        storedExpenses: List<Expense> = emptyList(),
        storedSettlements: List<Settlement> = emptyList(),
        pinnedGroupIds: Set<String> = emptySet(),
        isCardDismissedThisLaunch: Boolean = false,
        isFirstDownloadPending: Boolean = false,
    ) {
        account = FakeAccountRepository(initialStatus = status).also { it.recoveryCodePrompt = prompt }
        groups = FakeGroupRepository(initialGroups = storedGroups)
        pins = FakeGroupPinRepository(initialPinnedGroupIds = pinnedGroupIds)
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        recordedPayments = InMemoryRecordedPaymentRepository()
        isOnline = MutableStateFlow(online)
        this@HomeViewModelTest.syncState = MutableStateFlow(syncState)
        minutes = MutableStateFlow(0)
        clockMillis = now
        retryCount = 0
        syncNowCount = 0
        syncGate = null
        navigator = BackStackNavigator(startRoute = Route.Home)
        deletions = InMemoryGroupDeletionRepository()
        dismissal = RecoveryCodeCardDismissal().also { it.isDismissed.value = isCardDismissedThisLaunch }
        firstDownload = FirstDownload().also { if (isFirstDownloadPending) it.start() }
        activityUnread = MutableStateFlow(0)
        val money = ObserveGroupsMoneyUseCase(expenses, settlements)
        viewModel = HomeViewModel(
            navigator = navigator,
            retrySync = RetrySync { retryCount++ },
            syncNow = SyncNow {
                syncNowCount++
                syncGate?.await()
                Result.success(Unit)
            },
            updateRecoveryCodePrompt = UpdateRecoveryCodePromptUseCase(account),
            consumeGroupDeletion = ConsumeGroupDeletionUseCase(deletions),
            recoveryCodeCardDismissal = dismissal,
            observeRecordedPayment = ObserveRecordedPaymentUseCase(recordedPayments),
            observeSettledUp = ObserveSettledUpUseCase(recordedPayments),
            consumeRecordedPayment = ConsumeRecordedPaymentUseCase(recordedPayments),
            deleteSettlement = DeleteSettlementUseCase(settlements, recordedPayments) { clockMillis },
            currentTimeMillis = CurrentTimeMillis { clockMillis },
            currentTimeZone = CurrentTimeZone { TimeZone.UTC },
            minuteTicks = MinuteTicks { minutes.map { } },
            observeProfile = ObserveProfileUseCase(FakeProfileRepository(profile)),
            observeAccount = ObserveAccountUseCase(account),
            observeSyncState = ObserveSyncState { this@HomeViewModelTest.syncState },
            observeFirstDownload = ObserveFirstDownloadUseCase(firstDownload),
            // Why: a StateFlow is read at once, so the device not answering yet is a flow that has not emitted.
            observeGroups = if (groupsRead) ObserveGroupsUseCase(groups) else ObserveGroups { MutableSharedFlow() },
            observeGroupsMoney = money,
            observePeopleBalances = ObservePeopleBalancesUseCase(
                observeGroups = ObserveGroupsUseCase(groups),
                observeGroupsMoney = money,
            ),
            observePinnedGroupIds = ObservePinnedGroupIdsUseCase(pins),
            observeGroupDeletions = ObserveGroupDeletionsUseCase(deletions),
            connectivityObserver = NetworkConnectivityObserver { isOnline },
            observeActivityUnreadCount = ObserveActivityUnreadCount { activityUnread },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
