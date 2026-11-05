package com.quare.blitzsplit.feature.globalbalances

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.expenses.domain.model.RecordedPayment
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteSettlementUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsMoneyUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObservePeopleBalancesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettledUpUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveHasGroups
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveHasGroupsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.feature.globalbalances.presentation.model.BalancesEmptyState
import com.quare.blitzsplit.feature.globalbalances.presentation.model.CurrencyAmount
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiAction
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiEvent
import com.quare.blitzsplit.feature.globalbalances.presentation.viewmodel.GlobalBalancesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import software.quare.connectivity.NetworkConnectivityObserver
import software.quare.loadable.isLoading
import software.quare.loadable.valueOrNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GlobalBalancesViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var recordedPayments: InMemoryRecordedPaymentRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: GlobalBalancesViewModel
    private lateinit var firstDownload: MutableStateFlow<FirstDownloadProgress?>
    private val actions = mutableListOf<GlobalBalancesUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN people across groups WHEN Saldos opens THEN shows whom you owe then who owes you with the totals`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertEquals(listOf(true, false), state.sections.map { it.isYouOwe })
            assertEquals(listOf("Bruno"), state.sections[0].rows.map { it.name })
            assertEquals(listOf("Ana", "Diego", "Carla"), state.sections[1].rows.map { it.name })
            assertEquals(listOf(CurrencyAmount(GroupCurrency.BRL, 12_600L)), state.sections[0].totals)
            assertEquals(listOf(CurrencyAmount(GroupCurrency.BRL, 24_020L)), state.sections[1].totals)
            assertEquals(11_420L, state.primaryTotal.valueOrNull()?.netMinor)
            assertEquals(3, state.primaryTotal.valueOrNull()?.toReceiveCount)
            assertNull(state.emptyState)
        }

    @Test
    fun `GIVEN Carla across three groups WHEN Saldos opens THEN shows her once with each group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val carla = viewModel.uiState.value.people
                .valueOrNull()
                .orEmpty()
                .single { it.name == "Carla" }

            // Then
            assertEquals(3_290L, carla.netMinor)
            assertEquals(listOf(9_840L, -9_800L, 3_250L), carla.groups.map { it.amountMinor })
            assertTrue(carla.isNetOfSeveralGroups)
            assertTrue(carla.owesYou)
            assertFalse(carla.isExpanded)
        }

    @Test
    fun `GIVEN a guest WHEN Saldos opens THEN marks them as such`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val diego = viewModel.uiState.value.people
            .valueOrNull()
            .orEmpty()
            .single { it.name == "Diego" }

        // Then
        assertTrue(diego.isGuest)
        assertEquals(1, diego.groupCount)
    }

    @Test
    fun `GIVEN Carla owes you in two currencies WHEN Saldos opens THEN counts her once among who owes you`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = defaultGroups + group(
                    "usa",
                    "Viagem EUA",
                    me("me-usa"),
                    joined("carla-usa", "Carla", "carla"),
                    currency = GroupCurrency.USD,
                ),
                bills = defaultBills + owes("usa", "carla-usa", "me-usa", 5_000L, id = "e7"),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                4,
                state.totals
                    .valueOrNull()
                    .orEmpty()
                    .sumOf { it.toReceiveCount },
            )
            assertEquals(3, state.toReceivePeopleCount)
            assertEquals(1, state.toPayPeopleCount)
        }

    @Test
    fun `GIVEN A pagar opened Saldos WHEN it opens THEN shows only whom you owe with the lone row open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(filter = BalancesFilter.YOU_OWE)

            // When
            val sections = viewModel.uiState.value.sections

            // Then
            val row = sections.single().rows.single()
            assertEquals("Bruno", row.name)
            assertTrue(row.isExpanded)
        }

    @Test
    fun `GIVEN the lone row open WHEN tapping Por grupo THEN closes it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(filter = BalancesFilter.YOU_OWE)
        val bruno = viewModel.uiState.value.people
            .valueOrNull()
            .orEmpty()
            .single { it.name == "Bruno" }

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnPersonExpandToggled(bruno.id))

        // Then
        assertFalse(
            viewModel.uiState.value.people
                .valueOrNull()
                .orEmpty()
                .single { it.id == bruno.id }
                .isExpanded,
        )
    }

    @Test
    fun `GIVEN everyone WHEN tapping Por grupo on a row THEN opens only that row`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val carla = viewModel.uiState.value.people
            .valueOrNull()
            .orEmpty()
            .single { it.name == "Carla" }

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnPersonExpandToggled(carla.id))

        // Then
        assertEquals(
            listOf("Carla"),
            viewModel.uiState.value.people
                .valueOrNull()
                .orEmpty()
                .filter { it.isExpanded }
                .map { it.name },
        )
    }

    @Test
    fun `GIVEN nobody picked WHEN Saldos opens THEN the pane shows the first person listed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val selected = viewModel.uiState.value.selectedPerson

            // Then
            assertEquals("Bruno", selected?.name)
        }

    @Test
    fun `GIVEN everyone WHEN picking Carla THEN the pane shows her`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val carla = viewModel.uiState.value.people
            .valueOrNull()
            .orEmpty()
            .single { it.name == "Carla" }

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnPersonSelected(carla.id))

        // Then
        assertEquals(
            carla.id,
            viewModel.uiState.value.selectedPerson
                ?.id,
        )
    }

    @Test
    fun `GIVEN Carla picked WHEN filtering Voce deve THEN the pane shows whom you owe`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val carla = viewModel.uiState.value.people
            .valueOrNull()
            .orEmpty()
            .single { it.name == "Carla" }
        viewModel.onEvent(GlobalBalancesUiEvent.OnPersonSelected(carla.id))

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnFilterChanged(BalancesFilter.YOU_OWE))

        // Then
        assertEquals(
            "Bruno",
            viewModel.uiState.value.selectedPerson
                ?.name,
        )
    }

    @Test
    fun `GIVEN Carla picked WHEN going back to Tudo THEN the pane shows her again`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val carla = viewModel.uiState.value.people
            .valueOrNull()
            .orEmpty()
            .single { it.name == "Carla" }
        viewModel.onEvent(GlobalBalancesUiEvent.OnPersonSelected(carla.id))
        viewModel.onEvent(GlobalBalancesUiEvent.OnFilterChanged(BalancesFilter.YOU_OWE))

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnFilterChanged(BalancesFilter.ALL))

        // Then
        assertEquals(
            carla.id,
            viewModel.uiState.value.selectedPerson
                ?.id,
        )
    }

    @Test
    fun `GIVEN someone not listed WHEN a stale pick arrives THEN the pane keeps the first person`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnPersonSelected("user:nobody|BRL"))

            // Then
            assertNull(viewModel.uiState.value.pickedPersonId)
            assertEquals(
                "Bruno",
                viewModel.uiState.value.selectedPerson
                    ?.name,
            )
        }

    @Test
    fun `GIVEN Voce deve WHEN a stale pick of someone who owes you arrives THEN Tudo keeps the first person`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(filter = BalancesFilter.YOU_OWE)
            val carla = viewModel.uiState.value.people
                .valueOrNull()
                .orEmpty()
                .single { it.name == "Carla" }
            viewModel.onEvent(GlobalBalancesUiEvent.OnPersonSelected(carla.id))

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnFilterChanged(BalancesFilter.ALL))

            // Then
            assertNull(viewModel.uiState.value.pickedPersonId)
            assertEquals(
                "Bruno",
                viewModel.uiState.value.selectedPerson
                    ?.name,
            )
        }

    @Test
    fun `GIVEN everything settled WHEN Saldos opens THEN the pane shows nobody`() = runTest(testDispatcher) {
        // Given
        prepareScenario(bills = emptyList())

        // When
        val selected = viewModel.uiState.value.selectedPerson

        // Then
        assertNull(selected)
    }

    @Test
    fun `GIVEN a row opened on Te devem WHEN a sync leaves that person alone THEN the row stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(filter = BalancesFilter.OWES_YOU)
            val carla = viewModel.uiState.value.people
                .valueOrNull()
                .orEmpty()
                .single { it.name == "Carla" }
            viewModel.onEvent(GlobalBalancesUiEvent.OnPersonExpandToggled(carla.id))

            // When
            expenses.expenses.update { bills -> bills.filterNot { it.id in setOf("e2", "e6") } }

            // Then
            val rows = viewModel.uiState.value.sections
                .single()
                .rows
            assertEquals(listOf("Carla"), rows.map { it.name })
            assertTrue(rows.single().isExpanded)
        }

    @Test
    fun `GIVEN the lone row closed WHEN a sync brings someone else into the filter THEN the row stays closed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(filter = BalancesFilter.YOU_OWE)
            val bruno = viewModel.uiState.value.people
                .valueOrNull()
                .orEmpty()
                .single { it.name == "Bruno" }
            viewModel.onEvent(GlobalBalancesUiEvent.OnPersonExpandToggled(bruno.id))

            // When
            expenses.expenses.update { bills -> bills + owes(FLORIPA, ME_FLORIPA, ANA_FLORIPA, 20_000L, id = "e7") }

            // Then
            val rows = viewModel.uiState.value.sections
                .single()
                .rows
            assertEquals(listOf("Ana", "Bruno"), rows.mapNotNull { it.name }.sorted())
            assertTrue(rows.none { it.isExpanded })
        }

    @Test
    fun `GIVEN a row opened WHEN changing the filter THEN rows open as the new filter opens them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val carla = viewModel.uiState.value.people
                .valueOrNull()
                .orEmpty()
                .single { it.name == "Carla" }
            viewModel.onEvent(GlobalBalancesUiEvent.OnPersonExpandToggled(carla.id))

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnFilterChanged(BalancesFilter.OWES_YOU))

            // Then
            val state = viewModel.uiState.value
            assertEquals(BalancesFilter.OWES_YOU, state.filter)
            assertEquals(listOf(false), state.sections.map { it.isYouOwe })
            assertTrue(
                state.people
                    .valueOrNull()
                    .orEmpty()
                    .none { it.isExpanded },
            )
        }

    @Test
    fun `GIVEN nothing to pay WHEN filtering Voce deve THEN says you owe nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(bills = listOf(owes(FLORIPA, ANA_FLORIPA, ME_FLORIPA, 16_230L)))

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnFilterChanged(BalancesFilter.YOU_OWE))

        // Then
        assertEquals(BalancesEmptyState.NOTHING_TO_PAY, viewModel.uiState.value.emptyState)
        assertTrue(
            viewModel.uiState.value.sections
                .isEmpty(),
        )
    }

    @Test
    fun `GIVEN only another currency owes you WHEN filtering Voce deve THEN the empty state counts that currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = defaultGroups.map { group ->
                    if (group.id == CHURRASCO) group.copy(currency = GroupCurrency.USD) else group
                },
                bills = listOf(owes(CHURRASCO, CARLA_CHURRASCO, ME_CHURRASCO, 3_250L)),
            )

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnFilterChanged(BalancesFilter.YOU_OWE))

            // Then
            val state = viewModel.uiState.value
            assertEquals(BalancesEmptyState.NOTHING_TO_PAY, state.emptyState)
            assertEquals(GroupCurrency.BRL, state.primaryTotal.valueOrNull()?.currency)
            assertEquals(GroupCurrency.USD, state.emptyStateTotal?.currency)
            assertEquals(3_250L, state.emptyStateTotal?.toReceiveMinor)
            assertEquals(1, state.emptyStateTotal?.toReceiveCount)
        }

    @Test
    fun `GIVEN you owe nothing WHEN tapping Ver quem te deve THEN shows Te devem`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            filter = BalancesFilter.YOU_OWE,
            bills = listOf(owes(FLORIPA, ANA_FLORIPA, ME_FLORIPA, 16_230L)),
        )

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnEmptyActionClicked)

        // Then
        assertEquals(BalancesFilter.OWES_YOU, viewModel.uiState.value.filter)
        assertNull(viewModel.uiState.value.emptyState)
    }

    @Test
    fun `GIVEN nobody owes you WHEN tapping the empty button on Te devem THEN shows Voce deve`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                filter = BalancesFilter.OWES_YOU,
                bills = listOf(owes(FLORIPA, ME_FLORIPA, ANA_FLORIPA, 1_000L)),
            )
            val emptyState = viewModel.uiState.value.emptyState

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnEmptyActionClicked)

            // Then
            assertEquals(BalancesEmptyState.NOTHING_TO_RECEIVE, emptyState)
            assertEquals(BalancesFilter.YOU_OWE, viewModel.uiState.value.filter)
        }

    @Test
    fun `GIVEN everything settled WHEN tapping Ver grupos THEN opens the Grupos tab`() = runTest(testDispatcher) {
        // Given
        prepareScenario(bills = emptyList())
        val emptyState = viewModel.uiState.value.emptyState

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnEmptyActionClicked)

        // Then
        assertEquals(BalancesEmptyState.SETTLED, emptyState)
        assertEquals(
            0L,
            viewModel.uiState.value.primaryTotal
                .valueOrNull()
                ?.netMinor,
        )
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN no group at all WHEN Saldos opens THEN still has a zero total`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = emptyList(),
            bills = emptyList(),
        )

        // When
        val totals = viewModel.uiState.value.totals
            .valueOrNull()
            .orEmpty()

        // Then
        assertEquals(listOf(0L), totals.map { it.netMinor })
    }

    @Test
    fun `GIVEN the account copy not whole yet WHEN Saldos opens THEN shimmers instead of saying all settled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                bills = emptyList(),
                downloading = FirstDownloadProgress(
                    arrivedGroupCount = 1,
                    groupCount = 3,
                    isWaitingForInternet = false,
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertTrue(state.people.isLoading())
            assertTrue(state.primaryTotal.isLoading())
            assertNull(state.emptyState)
            assertNull(state.emptyStateTotal)
            assertTrue(state.sections.isEmpty())
        }

    @Test
    fun `GIVEN the account copy waiting offline WHEN Saldos opens THEN says it waits for the internet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                downloading = FirstDownloadProgress(
                    arrivedGroupCount = 0,
                    groupCount = null,
                    isWaitingForInternet = true,
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isWaitingForInternet)
            assertTrue(state.totals.isLoading())
        }

    @Test
    fun `GIVEN the account copy not whole yet WHEN its full sync ends THEN shows the balances`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                downloading = FirstDownloadProgress(
                    arrivedGroupCount = 0,
                    groupCount = null,
                    isWaitingForInternet = false,
                ),
            )

            // When
            firstDownload.value = null

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isLoaded)
            assertFalse(state.isWaitingForInternet)
            assertEquals(11_420L, state.primaryTotal.valueOrNull()?.netMinor)
        }

    @Test
    fun `GIVEN someone you owe WHEN tapping Acertar THEN opens settling up with them`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val bruno = viewModel.uiState.value.people
            .valueOrNull()
            .orEmpty()
            .single { it.name == "Bruno" }

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnSettleClicked(bruno.id))

        // Then
        assertEquals(Route.SettleUp("user:bruno", GroupCurrency.BRL), navigator.backStack.value.last())
    }

    @Test
    fun `GIVEN someone who owes you WHEN tapping Recebi THEN opens settling up with them`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val carla = viewModel.uiState.value.people
            .valueOrNull()
            .orEmpty()
            .single { it.name == "Carla" }

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnReceivedClicked(carla.id))

        // Then
        assertEquals(Route.SettleUp("user:carla", GroupCurrency.BRL), navigator.backStack.value.last())
    }

    @Test
    fun `GIVEN someone you owe WHEN Recebi arrives for them THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val bruno = viewModel.uiState.value.people
            .valueOrNull()
            .orEmpty()
            .single { it.name == "Bruno" }

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnReceivedClicked(bruno.id))

        // Then
        assertEquals(route, navigator.backStack.value.last())
    }

    @Test
    fun `GIVEN Carla WHEN tapping her face THEN opens her profile over Saldos`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val carla = viewModel.uiState.value.people
            .valueOrNull()
            .orEmpty()
            .single { it.name == "Carla" }

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnPersonClicked(carla.id))

        // Then
        assertEquals(
            listOf(Route.Home, route, Route.ContactProfile("user:carla")),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a guest WHEN tapping their name THEN opens their profile by their name`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val diego = viewModel.uiState.value.people
            .valueOrNull()
            .orEmpty()
            .single { it.name == "Diego" }

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnPersonClicked(diego.id))

        // Then
        assertTrue(diego.opensProfile)
        assertEquals(Route.ContactProfile("name:diego"), navigator.backStack.value.last())
    }

    @Test
    fun `GIVEN Carla owes you in two currencies WHEN tapping her dollar row THEN opens the same profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = defaultGroups + group(
                    "usa",
                    "Viagem EUA",
                    me("me-usa"),
                    joined("carla-usa", "Carla", "carla"),
                    currency = GroupCurrency.USD,
                ),
                bills = defaultBills + owes("usa", "carla-usa", "me-usa", 5_000L, id = "e7"),
            )
            val carlaInDollars = viewModel.uiState.value.people
                .valueOrNull()
                .orEmpty()
                .single { it.name == "Carla" && it.currency == GroupCurrency.USD }

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnPersonClicked(carlaInDollars.id))

            // Then
            assertEquals(Route.ContactProfile("user:carla"), navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN someone no longer in the group WHEN Saldos opens THEN their row opens no profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(bills = defaultBills + exMemberBill)

            // When
            val exMember = viewModel.uiState.value.people
                .valueOrNull()
                .orEmpty()
                .single { it.name == null }

            // Then
            assertFalse(exMember.opensProfile)
        }

    @Test
    fun `GIVEN someone no longer in the group WHEN tapping their face THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario(bills = defaultBills + exMemberBill)
        val exMember = viewModel.uiState.value.people
            .valueOrNull()
            .orEmpty()
            .single { it.name == null }

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnPersonClicked(exMember.id))

        // Then
        assertEquals(listOf(Route.Home, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a sync took someone away WHEN a late tap on their face arrives THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val carla = viewModel.uiState.value.people
                .valueOrNull()
                .orEmpty()
                .single { it.name == "Carla" }
            expenses.expenses.update { bills -> bills.filterNot { it.id in setOf("e3", "e4", "e5") } }

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnPersonClicked(carla.id))

            // Then
            assertEquals(listOf(Route.Home, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN something opened over Saldos WHEN a late tap on a face arrives THEN nothing stacks over it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val carla = viewModel.uiState.value.people
                .valueOrNull()
                .orEmpty()
                .single { it.name == "Carla" }
            viewModel.onEvent(GlobalBalancesUiEvent.OnPersonClicked(carla.id))

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnPersonClicked(carla.id))

            // Then
            assertEquals(
                listOf(Route.Home, route, Route.ContactProfile("user:carla")),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Carla across three groups WHEN tapping a group in Por grupo THEN opens its Despesas over Saldos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val apeOfCarla = viewModel.uiState.value.people
                .valueOrNull()
                .orEmpty()
                .single { it.name == "Carla" }
                .groups
                .single { it.groupName == "Apê 302" }

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnGroupClicked(apeOfCarla.groupId))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    route,
                    Route.GroupDetail(
                        APE,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a group no Por grupo lists WHEN a late tap on it arrives THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnGroupClicked("gone"))

            // Then
            assertEquals(listOf(Route.Home, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN something opened over Saldos WHEN a late tap on a group arrives THEN nothing stacks over it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.SettleUp("user:carla", GroupCurrency.BRL))

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnGroupClicked(FLORIPA))

            // Then
            assertEquals(
                listOf(Route.Home, route, Route.SettleUp("user:carla", GroupCurrency.BRL)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN someone who owes you WHEN tapping Cobrar THEN opens their message over Saldos in the currency shown`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val carlaId = idOfPerson("Carla")

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnChargeClicked(carlaId))

            // Then
            assertEquals(
                listOf(Route.Home, route, Route.Charge("user:carla", GroupCurrency.BRL)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN someone you owe WHEN Cobrar arrives for them THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val brunoId = idOfPerson("Bruno")

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargeClicked(brunoId))

        // Then
        assertEquals(listOf(Route.Home, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Cobrar already open WHEN a late Cobrar arrives THEN stacks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val carlaId = idOfPerson("Carla")
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargeClicked(carlaId))

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargeClicked(idOfPerson("Ana")))

        // Then
        assertEquals(
            listOf(Route.Home, route, Route.Charge("user:carla", GroupCurrency.BRL)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Carla owes you in two currencies WHEN tapping Cobrar on her dollar row THEN opens it in dollars`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = defaultGroups + group(
                    "usa",
                    "Viagem EUA",
                    me("me-usa"),
                    joined("carla-usa", "Carla", "carla"),
                    currency = GroupCurrency.USD,
                ),
                bills = defaultBills + owes("usa", "carla-usa", "me-usa", 5_000L, id = "e7"),
            )
            val carlaInDollars = viewModel.uiState.value.people
                .valueOrNull()
                .orEmpty()
                .single { it.name == "Carla" && it.currency == GroupCurrency.USD }

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnChargeClicked(carlaInDollars.id))

            // Then
            assertEquals(
                listOf(Route.Home, route, Route.Charge("user:carla", GroupCurrency.USD)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a guest who owes you WHEN tapping Cobrar THEN opens their message by their name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val diegoId = idOfPerson("Diego")

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnChargeClicked(diegoId))

            // Then
            assertEquals(
                listOf(Route.Home, route, Route.Charge("name:diego", GroupCurrency.BRL)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN someone not listed WHEN a stale Cobrar arrives THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val goneId = "gone"

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargeClicked(goneId))

        // Then
        assertEquals(listOf(Route.Home, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a sync settled Carla WHEN a late Cobrar for her arrives THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val carlaId = idOfPerson("Carla")
        expenses.expenses.update { bills -> bills.filterNot { it.id in setOf("e3", "e4", "e5") } }

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargeClicked(carlaId))

        // Then
        assertEquals(listOf(Route.Home, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN settling up open over Saldos WHEN a late Cobrar arrives THEN nothing stacks over it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val carlaId = idOfPerson("Carla")
            navigator.navigate(Route.SettleUp("user:carla", GroupCurrency.BRL))

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnChargeClicked(carlaId))

            // Then
            assertEquals(
                listOf(Route.Home, route, Route.SettleUp("user:carla", GroupCurrency.BRL)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the same debts not simplified WHEN looking at the people THEN Diego owes nobody here`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(simplifiedTrip(simplifies = false)),
                bills = simplifiedTripBills,
            )

            // When
            val people = viewModel.uiState.value.people
                .valueOrNull()
                .orEmpty()

            // Then
            assertEquals(listOf("Bruno"), people.map { it.name })
            assertEquals(listOf(false), people.single().groups.map { it.isSimplified })
        }

    @Test
    fun `GIVEN Saldos WHEN tapping the Grupos tab THEN Grupos takes its place`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnGroupsTabClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Saldos WHEN tapping the Contatos tab THEN opens Contatos in its place`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnContactsTabClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Saldos WHEN tapping the profile THEN opens it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnProfileClicked)

        // Then
        assertEquals(Route.Profile, navigator.backStack.value.last())
        assertEquals("P", viewModel.uiState.value.avatarInitial)
        assertEquals(AccountProvider.GOOGLE, viewModel.uiState.value.accountProvider)
    }

    @Test
    fun `GIVEN groups on the device WHEN Saldos opens THEN offers the search`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val canSearch = viewModel.uiState.value.canSearch

        // Then
        assertTrue(canSearch)
    }

    @Test
    fun `GIVEN every group settled WHEN Saldos opens THEN Tudo acertado still offers the search`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(bills = emptyList())

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(BalancesEmptyState.SETTLED, state.emptyState)
            assertTrue(state.canSearch)
        }

    @Test
    fun `GIVEN no group at all WHEN Saldos opens THEN offers no search`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = emptyList(),
            bills = emptyList(),
        )

        // When
        val canSearch = viewModel.uiState.value.canSearch

        // Then
        assertFalse(canSearch)
    }

    @Test
    fun `GIVEN the groups not read yet WHEN Saldos opens THEN offers no search`() = runTest(testDispatcher) {
        // Given
        prepareScenario(groupsRead = false)

        // When
        val canSearch = viewModel.uiState.value.canSearch

        // Then
        assertFalse(canSearch)
    }

    @Test
    fun `GIVEN no group at all WHEN a sync brings the first group THEN offers the search`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = emptyList(),
            bills = emptyList(),
        )
        val firstGroup = defaultGroups.first()

        // When
        groups.groups.value = listOf(firstGroup)

        // Then
        assertTrue(viewModel.uiState.value.canSearch)
    }

    @Test
    fun `GIVEN a single group WHEN a sync takes it away THEN offers no search`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = defaultGroups.take(1),
            bills = emptyList(),
        )
        val noGroups = emptyList<Group>()

        // When
        groups.groups.value = noGroups

        // Then
        assertFalse(viewModel.uiState.value.canSearch)
    }

    @Test
    fun `GIVEN Saldos on top WHEN tapping the magnifier THEN opens the search over Saldos`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnSearchClicked)

        // Then
        assertEquals(listOf(Route.Home, route, Route.Search), navigator.backStack.value)
    }

    @Test
    fun `GIVEN every group settled WHEN tapping the magnifier THEN opens the search over Saldos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(bills = emptyList())

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnSearchClicked)

            // Then
            assertEquals(listOf(Route.Home, route, Route.Search), navigator.backStack.value)
        }

    @Test
    fun `GIVEN no group at all WHEN the search is asked for THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = emptyList(),
            bills = emptyList(),
        )

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnSearchClicked)

        // Then
        assertEquals(listOf(Route.Home, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the groups not read yet WHEN the search is asked for THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario(groupsRead = false)

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnSearchClicked)

        // Then
        assertEquals(listOf(Route.Home, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN settling up open over Saldos WHEN a late search arrives THEN nothing stacks over it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val bruno = viewModel.uiState.value.people
                .valueOrNull()
                .orEmpty()
                .single { it.name == "Bruno" }
            viewModel.onEvent(GlobalBalancesUiEvent.OnSettleClicked(bruno.id))

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnSearchClicked)

            // Then
            assertEquals(
                listOf(Route.Home, route, Route.SettleUp("user:bruno", GroupCurrency.BRL)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Cobrar open over Saldos WHEN the search is asked for THEN nothing stacks over it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val carla = viewModel.uiState.value.people
                .valueOrNull()
                .orEmpty()
                .single { it.name == "Carla" }
            viewModel.onEvent(GlobalBalancesUiEvent.OnChargeClicked(carla.id))

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnSearchClicked)

            // Then
            assertEquals(
                listOf(Route.Home, route, Route.Charge("user:carla", GroupCurrency.BRL)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the search already open WHEN a second search arrives THEN opens no second one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GlobalBalancesUiEvent.OnSearchClicked)

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnSearchClicked)

            // Then
            assertEquals(listOf(Route.Home, route, Route.Search), navigator.backStack.value)
        }

    @Test
    fun `GIVEN no internet WHEN Saldos opens THEN works on the device balances and says so`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsOfflineNotice)
            assertEquals(2, state.sections.size)
        }

    @Test
    fun `GIVEN a settling up just recorded WHEN Saldos is on top again THEN says so once`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.SettleUp("user:carla", GroupCurrency.BRL))
        recordedPayments.post(
            RecordedPayment.SettledUp(
                listOf("p1", "p2"),
                personName = "Carla",
                isPaidByMe = false,
            ),
        )

        // When
        navigator.navigateBack()

        // Then
        assertEquals(
            PaymentNotice(
                settlementIds = listOf("p1", "p2"),
                fromName = "Carla",
                fromIsMe = false,
                toName = null,
            ),
            viewModel.uiState.value.recordedPayment,
        )
    }

    @Test
    fun `GIVEN the notice of a settling up WHEN tapping Desfazer THEN undoes every payment of it at once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedSettlements = listOf(payment("p1", FLORIPA), payment("p2", APE)),
            )
            recordedPayments.post(
                RecordedPayment.SettledUp(
                    listOf("p1", "p2"),
                    personName = "Bruno",
                    isPaidByMe = true,
                ),
            )

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnUndoPaymentClicked)

            // Then
            assertEquals(listOf(listOf("p1", "p2")), settlements.deleteCalls)
            assertEquals(listOf("p1", "p2"), settlements.deletedIds)
            assertNull(viewModel.uiState.value.recordedPayment)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN undoing fails WHEN tapping Desfazer THEN says so`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        recordedPayments.post(
            RecordedPayment.SettledUp(
                listOf("p1"),
                personName = "Bruno",
                isPaidByMe = true,
            ),
        )
        settlements.deleteError = IllegalStateException("disk full")

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnUndoPaymentClicked)

        // Then
        assertEquals(listOf<GlobalBalancesUiAction>(GlobalBalancesUiAction.ShowChangeError), actions)
    }

    @Test
    fun `GIVEN the notice shown WHEN it goes away THEN it is not shown again`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        recordedPayments.post(
            RecordedPayment.SettledUp(
                listOf("p1"),
                personName = "Bruno",
                isPaidByMe = true,
            ),
        )

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnRecordedPaymentNoticeShown)

        // Then
        assertNull(viewModel.uiState.value.recordedPayment)
    }

    @Test
    fun `GIVEN everyone WHEN filtering Voce deve THEN sends the filter picked on Saldos`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnFilterChanged(BalancesFilter.YOU_OWE))

        // Then
        assertEquals(
            listOf(
                "balances_filter_selected" to mapOf<String, Any>(
                    "filter" to BalancesFilter.YOU_OWE,
                    "source" to "global_balances",
                ),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN Te devem WHEN filtering Tudo THEN sends Tudo as the filter picked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(filter = BalancesFilter.OWES_YOU)

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnFilterChanged(BalancesFilter.ALL))

        // Then
        assertEquals(
            listOf(
                "balances_filter_selected" to mapOf<String, Any>(
                    "filter" to BalancesFilter.ALL,
                    "source" to "global_balances",
                ),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN Te devem WHEN picking Te devem again THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(filter = BalancesFilter.OWES_YOU)

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnFilterChanged(BalancesFilter.OWES_YOU))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN everyone WHEN opening Por grupo on a row THEN sends that it ended up open`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val carlaId = idOfPerson("Carla")

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnPersonExpandToggled(carlaId))

        // Then
        assertEquals(
            listOf("balances_person_expand_toggled" to mapOf<String, Any>("is_expanded" to true)),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN the lone row of Voce deve open WHEN tapping Por grupo THEN sends that it ended up closed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(filter = BalancesFilter.YOU_OWE)
            val brunoId = idOfPerson("Bruno")

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnPersonExpandToggled(brunoId))

            // Then
            assertEquals(
                listOf("balances_person_expand_toggled" to mapOf<String, Any>("is_expanded" to false)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a row opened by a tap WHEN tapping Por grupo again THEN sends that it ended up closed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val carlaId = idOfPerson("Carla")
            viewModel.onEvent(GlobalBalancesUiEvent.OnPersonExpandToggled(carlaId))
            trackedEvents.clear()

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnPersonExpandToggled(carlaId))

            // Then
            assertEquals(
                listOf("balances_person_expand_toggled" to mapOf<String, Any>("is_expanded" to false)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN someone not listed WHEN a stale Por grupo arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val staleId = "gone"

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnPersonExpandToggled(staleId))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN everyone WHEN picking Carla beside the list THEN sends the pick with no params`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val carlaId = idOfPerson("Carla")

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnPersonSelected(carlaId))

            // Then
            assertEquals(listOf("balances_person_selected" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN someone not listed WHEN a stale pick arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnPersonSelected("user:nobody|BRL"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Carla WHEN tapping her face THEN sends the person clicked from Saldos`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val carlaId = idOfPerson("Carla")

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnPersonClicked(carlaId))

        // Then
        assertEquals(listOf("person_clicked" to mapOf<String, Any>("source" to "global_balances")), trackedEvents)
    }

    @Test
    fun `GIVEN Carla across three groups WHEN tapping a group in Por grupo THEN sends the group clicked from Saldos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val apeId = viewModel.uiState.value.people
                .valueOrNull()
                .orEmpty()
                .single { it.name == "Carla" }
                .groups
                .single { it.groupName == "Apê 302" }
                .groupId

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnGroupClicked(apeId))

            // Then
            assertEquals(listOf("group_clicked" to mapOf<String, Any>("source" to "global_balances")), trackedEvents)
        }

    @Test
    fun `GIVEN someone you owe WHEN tapping Acertar THEN sends settle clicked from Saldos`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val brunoId = idOfPerson("Bruno")

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnSettleClicked(brunoId))

        // Then
        assertEquals(listOf("settle_clicked" to mapOf<String, Any>("source" to "global_balances")), trackedEvents)
    }

    @Test
    fun `GIVEN someone who owes you WHEN tapping Recebi THEN sends received clicked from Saldos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val carlaId = idOfPerson("Carla")

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnReceivedClicked(carlaId))

            // Then
            assertEquals(
                listOf("received_clicked" to mapOf<String, Any>("source" to "global_balances")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN someone who owes you WHEN tapping Cobrar THEN sends charge clicked from Saldos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val carlaId = idOfPerson("Carla")

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnChargeClicked(carlaId))

            // Then
            assertEquals(
                listOf("charge_clicked" to mapOf<String, Any>("source" to "global_balances")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN you owe nothing WHEN tapping Ver quem te deve THEN sends the empty action clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                filter = BalancesFilter.YOU_OWE,
                bills = listOf(owes(FLORIPA, ANA_FLORIPA, ME_FLORIPA, 16_230L)),
            )

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnEmptyActionClicked)

            // Then
            assertEquals(listOf("balances_empty_action_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN Balances WHEN tapping the logo THEN sends brand_clicked from Balances`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnBrandClicked)

        // Then
        assertEquals(listOf("brand_clicked" to mapOf<String, Any>("source" to "global_balances")), trackedEvents)
    }

    @Test
    fun `GIVEN Balances WHEN tapping the logo THEN goes back to Home`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnBrandClicked)

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Saldos WHEN tapping the profile THEN sends the avatar clicked from Saldos`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnProfileClicked)

        // Then
        assertEquals(
            listOf("profile_avatar_clicked" to mapOf<String, Any>("source" to "global_balances")),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN the notice of a settling up WHEN tapping Desfazer THEN sends the undo clicked from Saldos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedSettlements = listOf(payment("p1", FLORIPA), payment("p2", APE)),
            )
            recordedPayments.post(
                RecordedPayment.SettledUp(
                    listOf("p1", "p2"),
                    personName = "Bruno",
                    isPaidByMe = true,
                ),
            )

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnUndoPaymentClicked)

            // Then
            assertEquals(
                listOf("payment_undo_clicked" to mapOf<String, Any>("source" to "global_balances")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the notice shown WHEN it goes away THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        recordedPayments.post(
            RecordedPayment.SettledUp(
                listOf("p1"),
                personName = "Bruno",
                isPaidByMe = true,
            ),
        )

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnRecordedPaymentNoticeShown)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Saldos on top WHEN tapping the magnifier THEN sends the search opened from Saldos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnSearchClicked)

            // Then
            assertEquals(
                listOf("search_opened" to mapOf<String, Any>("source" to "global_balances")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN no group at all WHEN the search is asked for THEN sends no search opened`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = emptyList(),
            bills = emptyList(),
        )

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnSearchClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the groups not read yet WHEN the search is asked for THEN sends no search opened`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(groupsRead = false)

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnSearchClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN settling up open over Saldos WHEN a late search arrives THEN sends no search opened`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.SettleUp("user:bruno", GroupCurrency.BRL))

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnSearchClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the search already open WHEN a second search arrives THEN sends no second search opened`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GlobalBalancesUiEvent.OnSearchClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnSearchClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Saldos WHEN tapping the Inicio tab THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnHomeTabClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Saldos WHEN tapping the Grupos tab THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnGroupsTabClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Saldos WHEN tapping the Contatos tab THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnContactsTabClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    private fun idOfPerson(name: String): String = viewModel.uiState.value.people
        .valueOrNull()
        .orEmpty()
        .single { it.name == name }
        .id

    // Why: you owe someone the group no longer has, whom Saldos calls "Ex-membro".
    private val exMemberBill = owes(FLORIPA, ME_FLORIPA, "gone-floripa", 2_000L, id = "e8")

    private fun payment(
        id: String,
        groupId: String,
    ): Settlement = Settlement(
        id = id,
        groupId = groupId,
        fromMemberId = if (groupId == FLORIPA) ME_FLORIPA else ME_APE,
        toMemberId = if (groupId == FLORIPA) BRUNO_FLORIPA else CARLA_APE,
        amountMinor = 100L,
        method = SettlementMethod.PIX,
        paidAt = 0L,
        isPending = true,
        updatedAt = 0L,
    )

    private val route = Route.GlobalBalances(BalancesFilter.ALL)

    private fun TestScope.prepareScenario(
        filter: BalancesFilter = BalancesFilter.ALL,
        storedGroups: List<Group> = defaultGroups,
        bills: List<Expense> = defaultBills,
        storedSettlements: List<Settlement> = emptyList(),
        online: Boolean = true,
        groupsRead: Boolean = true,
        downloading: FirstDownloadProgress? = null,
    ) {
        groups = FakeGroupRepository(initialGroups = storedGroups)
        expenses = FakeExpenseRepository(initialExpenses = bills)
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        recordedPayments = InMemoryRecordedPaymentRepository()
        isOnline = MutableStateFlow(online)
        firstDownload = MutableStateFlow(downloading)
        val opened = Route.GlobalBalances(filter)
        navigator = BackStackNavigator(startRoute = Route.Home).also { it.navigate(opened) }
        val money = ObserveGroupsMoneyUseCase(expenses, settlements)
        viewModel = GlobalBalancesViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            route = opened,
            navigator = navigator,
            consumeRecordedPayment = ConsumeRecordedPaymentUseCase(recordedPayments),
            deleteSettlement = DeleteSettlementUseCase(settlements, recordedPayments) { 0L },
            observePeopleBalances = ObservePeopleBalancesUseCase(
                observeGroups = ObserveGroupsUseCase(groups),
                observeGroupsMoney = money,
            ),
            observeSettledUp = ObserveSettledUpUseCase(recordedPayments),
            observeProfile = ObserveProfileUseCase(
                FakeProfileRepository(UserProfile("Pierre", AvatarColor.BLUE, null)),
            ),
            observeAccount = ObserveAccountUseCase(
                FakeAccountRepository(AccountStatus.Connected("pierre", AccountProvider.GOOGLE)),
            ),
            observeFirstDownload = ObserveFirstDownload { firstDownload },
            connectivityObserver = NetworkConnectivityObserver { isOnline },
            // Why: Room not read yet is a flow that hasn't emitted.
            observeHasGroups = if (groupsRead) {
                ObserveHasGroupsUseCase(groups)
            } else {
                ObserveHasGroups { flow { awaitCancellation() } }
            },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
