package com.quare.blitzsplit.feature.globalbalances

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.expenses.domain.model.RecordedPayment
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteSettlementUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsMoneyUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObservePeopleBalancesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettledUpUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.BalancesCharge
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObservePaymentKeysUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.feature.globalbalances.presentation.model.BalancesEmptyState
import com.quare.blitzsplit.feature.globalbalances.presentation.model.CurrencyAmount
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiAction
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiEvent
import com.quare.blitzsplit.feature.globalbalances.presentation.viewmodel.GlobalBalancesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GlobalBalancesViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var recordedPayments: InMemoryRecordedPaymentRepository
    private lateinit var keys: FakePaymentKeyRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: GlobalBalancesViewModel
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
            assertEquals(11_420L, state.primaryTotal.netMinor)
            assertEquals(3, state.primaryTotal.toReceiveCount)
            assertNull(state.emptyState)
        }

    @Test
    fun `GIVEN Carla across three groups WHEN Saldos opens THEN shows her once with each group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val carla = viewModel.uiState.value.people
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
            assertEquals(4, state.totals.sumOf { it.toReceiveCount })
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
            .single { it.name == "Bruno" }

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnPersonExpandToggled(bruno.id))

        // Then
        assertFalse(
            viewModel.uiState.value.people
                .single { it.id == bruno.id }
                .isExpanded,
        )
    }

    @Test
    fun `GIVEN everyone WHEN tapping Por grupo on a row THEN opens only that row`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val carla = viewModel.uiState.value.people
            .single { it.name == "Carla" }

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnPersonExpandToggled(carla.id))

        // Then
        assertEquals(
            listOf("Carla"),
            viewModel.uiState.value.people
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
                .single { it.name == "Carla" }
            viewModel.onEvent(GlobalBalancesUiEvent.OnPersonExpandToggled(carla.id))

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnFilterChanged(BalancesFilter.OWES_YOU))

            // Then
            val state = viewModel.uiState.value
            assertEquals(BalancesFilter.OWES_YOU, state.filter)
            assertEquals(listOf(false), state.sections.map { it.isYouOwe })
            assertTrue(state.people.none { it.isExpanded })
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
            assertEquals(GroupCurrency.BRL, state.primaryTotal.currency)
            assertEquals(GroupCurrency.USD, state.emptyStateTotal.currency)
            assertEquals(3_250L, state.emptyStateTotal.toReceiveMinor)
            assertEquals(1, state.emptyStateTotal.toReceiveCount)
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
    fun `GIVEN everything settled WHEN tapping Ver grupos THEN goes back to the groups`() = runTest(testDispatcher) {
        // Given
        prepareScenario(bills = emptyList())
        val emptyState = viewModel.uiState.value.emptyState

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnEmptyActionClicked)

        // Then
        assertEquals(BalancesEmptyState.SETTLED, emptyState)
        assertEquals(0L, viewModel.uiState.value.primaryTotal.netMinor)
        assertEquals(listOf(Route.Groups), navigator.backStack.value)
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

        // Then
        assertEquals(listOf(0L), totals.map { it.netMinor })
    }

    @Test
    fun `GIVEN someone you owe WHEN tapping Acertar THEN opens settling up with them`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val bruno = viewModel.uiState.value.people
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
            .single { it.name == "Bruno" }

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnReceivedClicked(bruno.id))

        // Then
        assertEquals(route, navigator.backStack.value.last())
    }

    @Test
    fun `GIVEN someone who owes you WHEN tapping Cobrar THEN builds the message from the debt and your Pix key`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val carla = viewModel.uiState.value.people
                .single { it.name == "Carla" }

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnChargeClicked(carla.id))

            // Then
            val charge = viewModel.uiState.value.charge
            assertEquals("Carla", charge?.name)
            assertEquals(3_290L, charge?.amountMinor)
            assertEquals(listOf("Viagem Floripa", "Apê 302", "Churrasco de sábado"), charge?.groupNames)
            assertEquals(true, charge?.includesPixKey)
            assertEquals(true, charge?.canTogglePixKey)
            assertEquals(pixKey, viewModel.uiState.value.pixKey)
        }

    @Test
    fun `GIVEN a guest WHEN tapping Cobrar THEN the message carries their bills`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val diego = viewModel.uiState.value.people
            .single { it.name == "Diego" }

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargeClicked(diego.id))

        // Then
        val charge = viewModel.uiState.value.charge
        assertEquals(true, charge?.isGuest)
        assertEquals(listOf("Carne"), charge?.guestBills)
    }

    @Test
    fun `GIVEN no Pix key WHEN tapping Cobrar THEN leaves the key out`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedKeys = emptyList())
        val carla = viewModel.uiState.value.people
            .single { it.name == "Carla" }

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargeClicked(carla.id))

        // Then
        assertEquals(
            false,
            viewModel.uiState.value.charge
                ?.includesPixKey,
        )
        assertNull(viewModel.uiState.value.pixKey)
    }

    @Test
    fun `GIVEN Cobrar opened before the Pix key loaded WHEN the key arrives THEN includes it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedKeys = emptyList())
            openChargeForCarla()

            // When
            keys.keys.value = listOf(pixKey)

            // Then
            assertEquals(pixKey, viewModel.uiState.value.pixKey)
            assertEquals(
                true,
                viewModel.uiState.value.charge
                    ?.includesPixKey,
            )
        }

    @Test
    fun `GIVEN the Pix key switched off WHEN the profile keys change THEN stays off`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openChargeForCarla()
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargePixKeyToggled(isIncluded = false))

        // When
        keys.keys.value = listOf(pixKey.copy(value = "pierre@novo.com"))

        // Then
        assertEquals(
            false,
            viewModel.uiState.value.charge
                ?.includesPixKey,
        )
    }

    @Test
    fun `GIVEN the message edited without a Pix key WHEN the key arrives THEN the locked switch stays off`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedKeys = emptyList())
            openChargeForCarla()
            viewModel.onEvent(GlobalBalancesUiEvent.OnChargeMessageChanged("Carla, me paga!"))

            // When
            keys.keys.value = listOf(pixKey)

            // Then
            assertEquals(
                false,
                viewModel.uiState.value.charge
                    ?.includesPixKey,
            )
        }

    @Test
    fun `GIVEN someone you owe WHEN Cobrar arrives for them THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val bruno = viewModel.uiState.value.people
            .single { it.name == "Bruno" }

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargeClicked(bruno.id))

        // Then
        assertNull(viewModel.uiState.value.charge)
    }

    @Test
    fun `GIVEN Cobrar open WHEN switching the Pix key off THEN leaves it out`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openChargeForCarla()

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargePixKeyToggled(isIncluded = false))

        // Then
        assertEquals(
            false,
            viewModel.uiState.value.charge
                ?.includesPixKey,
        )
    }

    @Test
    fun `GIVEN the message edited WHEN switching the Pix key THEN keeps the text as typed`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openChargeForCarla()
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargeMessageChanged("Carla, me paga!"))

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargePixKeyToggled(isIncluded = false))

        // Then
        val charge = viewModel.uiState.value.charge
        assertEquals("Carla, me paga!", charge?.editedMessage)
        assertEquals(true, charge?.includesPixKey)
        assertEquals(false, charge?.canTogglePixKey)
    }

    @Test
    fun `GIVEN Cobrar open WHEN tapping Enviar THEN opens the share sheet with the message`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openChargeForCarla()

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnChargeShareClicked("Oi, Carla!"))

            // Then
            assertEquals(listOf<GlobalBalancesUiAction>(GlobalBalancesUiAction.ShareMessage("Oi, Carla!")), actions)
        }

    @Test
    fun `GIVEN Cobrar open WHEN tapping Copiar mensagem THEN copies the message`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openChargeForCarla()

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargeCopyClicked("Oi, Carla!"))

        // Then
        assertEquals(listOf<GlobalBalancesUiAction>(GlobalBalancesUiAction.CopyMessage("Oi, Carla!")), actions)
    }

    @Test
    fun `GIVEN Cobrar closed WHEN a late Enviar arrives THEN shares nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargeShareClicked("Oi, Carla!"))

        // Then
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN Cobrar from the profile of Carla WHEN Saldos opens THEN her message opens in the currency shown`() =
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
                filter = BalancesFilter.OWES_YOU,
                charge = BalancesCharge("user:carla", GroupCurrency.USD),
            )

            // When
            val charge = viewModel.uiState.value.charge

            // Then
            assertEquals("Carla", charge?.name)
            assertEquals(5_000L, charge?.amountMinor)
            assertEquals(listOf("Viagem EUA"), charge?.groupNames)
        }

    @Test
    fun `GIVEN Cobrar from a profile WHEN the person owes you nothing there THEN goes back to the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                filter = BalancesFilter.OWES_YOU,
                charge = BalancesCharge("user:bruno", GroupCurrency.BRL),
            )

            // When
            val stack = navigator.backStack.value

            // Then
            assertNull(viewModel.uiState.value.charge)
            assertEquals(Route.ContactProfile("user:bruno"), stack.last())
        }

    @Test
    fun `GIVEN the message Cobrar opened from a profile WHEN closing it THEN goes back to the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                filter = BalancesFilter.OWES_YOU,
                charge = BalancesCharge("user:carla", GroupCurrency.BRL),
            )

            // When
            viewModel.onEvent(GlobalBalancesUiEvent.OnChargeDismissed)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.Contacts, Route.ContactProfile("user:carla")),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Cobrar open WHEN closing it THEN it goes`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openChargeForCarla()

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargeDismissed)

        // Then
        assertNull(viewModel.uiState.value.charge)
    }

    @Test
    fun `GIVEN Cobrar open WHEN a sync settles the person THEN it closes`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openChargeForCarla()

        // When
        expenses.expenses.update { list ->
            list.filterNot { it.shares.any { share -> share.memberId.startsWith("carla") } }
        }

        // Then
        assertNull(viewModel.uiState.value.charge)
    }

    @Test
    fun `GIVEN Saldos WHEN tapping the Grupos tab THEN goes back to the groups`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnGroupsTabClicked)

        // Then
        assertEquals(listOf(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Saldos WHEN tapping a tab not built yet THEN says it is coming soon`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnActivityTabClicked)

        // Then
        assertEquals(listOf<GlobalBalancesUiAction>(GlobalBalancesUiAction.ShowComingSoon), actions)
    }

    @Test
    fun `GIVEN Saldos WHEN tapping the Contatos tab THEN opens Contatos in its place`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GlobalBalancesUiEvent.OnContactsTabClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.Contacts), navigator.backStack.value)
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

    private fun openChargeForCarla() {
        val carla = viewModel.uiState.value.people
            .single { it.name == "Carla" }
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargeClicked(carla.id))
    }

    private val pixKey = PaymentKey(
        "key-1",
        PaymentKeyType.EMAIL,
        "pierre@email.com",
        isDefault = true,
        addedAt = 0L,
    )

    private val bitcoinKey =
        PaymentKey(
            "key-2",
            PaymentKeyType.LIGHTNING,
            "pierre@ln.com",
            isDefault = true,
            addedAt = 1L,
        )

    // Why: the canvas's people, a little simpler: Bruno you owe, Ana, Diego (a guest) and Carla (across three groups).
    private val defaultBills = listOf(
        owes(FLORIPA, ME_FLORIPA, BRUNO_FLORIPA, 12_600L, id = "e1"),
        owes(FLORIPA, ANA_FLORIPA, ME_FLORIPA, 16_230L, id = "e2"),
        owes(FLORIPA, CARLA_FLORIPA, ME_FLORIPA, 9_840L, id = "e3"),
        owes(APE, ME_APE, CARLA_APE, 9_800L, id = "e4"),
        owes(CHURRASCO, CARLA_CHURRASCO, ME_CHURRASCO, 3_250L, id = "e5"),
        owes(
            CHURRASCO,
            DIEGO_CHURRASCO,
            ME_CHURRASCO,
            4_500L,
            id = "e6",
            description = "Carne",
        ),
    )

    private val defaultGroups = listOf(
        group(
            FLORIPA,
            "Viagem Floripa",
            me(ME_FLORIPA),
            joined(ANA_FLORIPA, "Ana", "ana"),
            joined(BRUNO_FLORIPA, "Bruno", "bruno"),
            joined(CARLA_FLORIPA, "Carla", "carla"),
        ),
        group(APE, "Apê 302", me(ME_APE), joined(CARLA_APE, "Carla", "carla")),
        group(
            CHURRASCO,
            "Churrasco de sábado",
            me(ME_CHURRASCO),
            joined(CARLA_CHURRASCO, "Carla", "carla"),
            GroupMember(
                DIEGO_CHURRASCO,
                "Diego",
                AvatarColor.GREEN,
                isMe = false,
                hasJoined = false,
                userId = null,
                inviteCode = null,
            ),
        ),
    )

    private fun me(id: String): GroupMember = GroupMember(
        id,
        "Pierre",
        AvatarColor.BLUE,
        isMe = true,
        hasJoined = true,
        userId = "pierre",
        inviteCode = null,
    )

    private fun joined(
        id: String,
        name: String,
        userId: String,
    ): GroupMember = GroupMember(
        id,
        name,
        AvatarColor.LILAC,
        isMe = false,
        hasJoined = true,
        userId = userId,
        inviteCode = null,
    )

    private fun group(
        id: String,
        name: String,
        vararg members: GroupMember,
        currency: GroupCurrency = GroupCurrency.BRL,
    ): Group = Group(
        id = id,
        name = name,
        type = GroupType.OTHER,
        color = AvatarColor.PEACH,
        currency = currency,
        members = members.toList(),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
    )

    // Why: a bill only one member paid, all of it on another, so the second owes the first that much.
    private fun owes(
        groupId: String,
        fromMemberId: String,
        toMemberId: String,
        amountMinor: Long,
        id: String = "bill-$groupId",
        description: String = "Bill",
    ): Expense = Expense(
        id = id,
        groupId = groupId,
        description = description,
        amountMinor = amountMinor,
        splitMode = SplitMode.AMOUNT,
        payers = listOf(ExpensePayer(toMemberId, amountMinor)),
        shares = listOf(ExpenseShare(fromMemberId, amountMinor, amountMinor)),
        items = null,
        spentAt = 0L,
        isPending = false,
        updatedAt = 0L,
        updatedByMemberId = null,
    )

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

    private val route = Route.GlobalBalances(BalancesFilter.ALL, charge = null)

    private fun TestScope.prepareScenario(
        filter: BalancesFilter = BalancesFilter.ALL,
        storedGroups: List<Group> = defaultGroups,
        bills: List<Expense> = defaultBills,
        storedSettlements: List<Settlement> = emptyList(),
        storedKeys: List<PaymentKey> = listOf(bitcoinKey, pixKey),
        online: Boolean = true,
        charge: BalancesCharge? = null,
    ) {
        val groups = FakeGroupRepository(initialGroups = storedGroups)
        expenses = FakeExpenseRepository(initialExpenses = bills)
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        recordedPayments = InMemoryRecordedPaymentRepository()
        keys = FakePaymentKeyRepository(initialKeys = storedKeys)
        isOnline = MutableStateFlow(online)
        val opened = Route.GlobalBalances(filter, charge = charge)
        navigator = BackStackNavigator(startRoute = Route.Groups).also { navigator ->
            // Why: Contatos' "Cobrar" opens Saldos over the person's profile.
            if (charge != null) {
                navigator.navigate(Route.Contacts)
                navigator.navigate(Route.ContactProfile(charge.personKey))
            }
            navigator.navigate(opened)
        }
        val money = ObserveGroupsMoneyUseCase(expenses, settlements)
        viewModel = GlobalBalancesViewModel(
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
            observePaymentKeys = ObservePaymentKeysUseCase(keys),
            connectivityObserver = NetworkConnectivityObserver { isOnline },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val FLORIPA = "floripa"
        const val APE = "ape"
        const val CHURRASCO = "churrasco"
        const val ME_FLORIPA = "me-floripa"
        const val ME_APE = "me-ape"
        const val ME_CHURRASCO = "me-churrasco"
        const val ANA_FLORIPA = "ana-floripa"
        const val BRUNO_FLORIPA = "bruno-floripa"
        const val CARLA_FLORIPA = "carla-floripa"
        const val CARLA_APE = "carla-ape"
        const val CARLA_CHURRASCO = "carla-churrasco"
        const val DIEGO_CHURRASCO = "diego-churrasco"
    }
}
