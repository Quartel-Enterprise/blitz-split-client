package com.quare.blitzsplit.feature.watchbalances

import com.quare.blitzsplit.core.expenses.domain.model.GroupFace
import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchDebt
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchGroupUiAction
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchGroupUiEvent
import com.quare.blitzsplit.feature.watchbalances.presentation.viewmodel.WatchGroupViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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

class WatchGroupViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val route = Route.WatchGroup(FLORIPA, GroupCurrency.BRL)
    private val recordedCalls = mutableListOf<RecordedCall>()
    private val actions = mutableListOf<WatchGroupUiAction>()
    private lateinit var peopleBalances: MutableStateFlow<PeopleBalances>
    private lateinit var groupsMoney: MutableStateFlow<Map<String, GroupMoney>>
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: WatchGroupViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val carlaFace = GroupFace(
        initials = "C",
        color = AvatarColor.PEACH,
        photoUrl = "https://photos/carla.jpg",
    )

    // Why: in Floripa, in reais, you owe Carla R$ 98,00 and Ana owes you R$ 30,00.
    private val youOweCarla = balances(
        person("Carla", pair(FLORIPA, "carla", -9_800L), pair(APE, "carla-ape", 50_000L), face = carlaFace),
        person("Ana", pair(FLORIPA, "ana", 3_000L)),
        person(
            "Carla",
            pair(FLORIPA, "carla", -100_000L),
            currency = GroupCurrency.USD,
            face = carlaFace,
        ),
    )

    // Why: in Floripa, in reais, you owe Carla R$ 98,00 and Bia R$ 20,00.
    private val youOweTwo = balances(
        person("Carla", pair(FLORIPA, "carla", -9_800L), face = carlaFace),
        person("Bia", pair(FLORIPA, "bia", -2_000L)),
    )

    private val onlyOwedToYou = balances(person("Ana", pair(FLORIPA, "ana", 3_000L)))

    private val settled = balances(person("Carla", pair(APE, "carla-ape", 50_000L)))

    private val carlaDebt = WatchDebt(
        memberId = "carla",
        name = "Carla",
        initials = "C",
        color = AvatarColor.PEACH,
        photoUrl = "https://photos/carla.jpg",
        amountMinor = -9_800L,
    )

    @Test
    fun `GIVEN you owe Carla and Ana owes you WHEN the group opens THEN shows its pairs largest first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertEquals("Floripa", state.groupName)
            assertEquals(GroupCurrency.BRL, state.currency)
            assertEquals(
                listOf(
                    carlaDebt,
                    WatchDebt(
                        memberId = "ana",
                        name = "Ana",
                        initials = "A",
                        color = AvatarColor.PEACH,
                        photoUrl = null,
                        amountMinor = 3_000L,
                    ),
                ),
                state.debts,
            )
            assertEquals(-6_800L, state.netMinor)
            assertFalse(state.isQueued)
        }

    @Test
    fun `GIVEN someone no longer in the group WHEN it opens THEN their pair shows without a name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(balances = balances(person(null, pair(FLORIPA, "gone", -4_000L))))

            // When
            val debt = viewModel.uiState.value.debts
                .single()

            // Then
            assertEquals("gone", debt.memberId)
            assertNull(debt.name)
            assertEquals(-4_000L, debt.amountMinor)
        }

    @Test
    fun `GIVEN a payment of the group waiting to go up WHEN it opens THEN the group shows as queued`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(money = mapOf(FLORIPA to moneyWith(settlement(FLORIPA, isPending = true))))

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isQueued)
        }

    @Test
    fun `GIVEN a payment queued in another currency WHEN the group opens THEN it is not queued`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                money = mapOf(
                    FLORIPA to moneyWith(
                        settlement(
                            FLORIPA,
                            isPending = true,
                            currency = GroupCurrency.USD,
                        ),
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isQueued)
        }

    @Test
    fun `GIVEN you owe only Carla WHEN it opens THEN shows I paid for her and the rows are not clickable`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(carlaDebt, state.singleDebtToPay)
            assertTrue(state.showsIPaid)
            assertFalse(state.areDebtsClickable)
        }

    @Test
    fun `GIVEN you owe two people WHEN it opens THEN each row is its own I paid`() = runTest(testDispatcher) {
        // Given
        prepareScenario(balances = youOweTwo)

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.singleDebtToPay)
        assertFalse(state.showsIPaid)
        assertTrue(state.areDebtsClickable)
    }

    @Test
    fun `GIVEN only people who owe you WHEN it opens THEN there is no I paid`() = runTest(testDispatcher) {
        // Given
        prepareScenario(balances = onlyOwedToYou)

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.singleDebtToPay)
        assertFalse(state.showsIPaid)
        assertFalse(state.areDebtsClickable)
    }

    @Test
    fun `GIVEN you owe two people WHEN tapping I paid on Bia THEN asks whether you paid Bia`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(balances = youOweTwo)
            val event = WatchGroupUiEvent.OnIPaidClicked("bia")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                "bia",
                viewModel.uiState.value.confirming
                    ?.memberId,
            )
            assertEquals(
                -2_000L,
                viewModel.uiState.value.confirming
                    ?.amountMinor,
            )
            assertEquals(listOf("watch_i_paid_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN Ana owes you WHEN tapping I paid on Ana THEN asks nothing and tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val event = WatchGroupUiEvent.OnIPaidClicked("ana")

            // When
            viewModel.onEvent(event)

            // Then
            assertNull(viewModel.uiState.value.confirming)
            assertEquals(emptyList(), trackedEvents)
        }

    @Test
    fun `GIVEN someone not in the pairs WHEN tapping I paid on them THEN asks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = WatchGroupUiEvent.OnIPaidClicked("nobody")

        // When
        viewModel.onEvent(event)

        // Then
        assertNull(viewModel.uiState.value.confirming)
    }

    @Test
    fun `GIVEN the question to Carla WHEN dismissing it THEN the question goes away and nothing is recorded`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(WatchGroupUiEvent.OnIPaidClicked("carla"))

            // When
            viewModel.onEvent(WatchGroupUiEvent.OnConfirmDismissed)

            // Then
            assertNull(viewModel.uiState.value.confirming)
            assertTrue(recordedCalls.isEmpty())
        }

    @Test
    fun `GIVEN nothing asked WHEN confirming THEN nothing is recorded`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = WatchGroupUiEvent.OnPaymentConfirmed

        // When
        viewModel.onEvent(event)

        // Then
        assertTrue(recordedCalls.isEmpty())
        assertFalse(viewModel.uiState.value.isRecording)
    }

    @Test
    fun `GIVEN the Pix rule on and a debt in reais WHEN confirming THEN records it as paid by Pix`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(showsPix = true)
            viewModel.onEvent(WatchGroupUiEvent.OnIPaidClicked("carla"))

            // When
            viewModel.onEvent(WatchGroupUiEvent.OnPaymentConfirmed)

            // Then
            assertEquals(listOf(SettlementMethod.PIX), recordedCalls.map { it.method })
        }

    @Test
    fun `GIVEN the question to Carla WHEN confirming THEN records what you owe her from you to her in the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(WatchGroupUiEvent.OnIPaidClicked("carla"))

            // When
            viewModel.onEvent(WatchGroupUiEvent.OnPaymentConfirmed)

            // Then
            assertEquals(
                listOf(
                    RecordedCall(
                        groupId = FLORIPA,
                        fromMemberId = meIn(FLORIPA),
                        toMemberId = "carla",
                        amountMinor = 9_800L,
                        currency = GroupCurrency.BRL,
                        method = SettlementMethod.OTHER,
                        paidAt = NOW,
                    ),
                ),
                recordedCalls,
            )
        }

    @Test
    fun `GIVEN the question to Carla online WHEN confirming THEN shows it recorded and tracks it online`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(WatchGroupUiEvent.OnIPaidClicked("carla"))
            trackedEvents.clear()

            // When
            viewModel.onEvent(WatchGroupUiEvent.OnPaymentConfirmed)

            // Then
            assertNull(viewModel.uiState.value.confirming)
            assertFalse(viewModel.uiState.value.isRecording)
            assertEquals(listOf<WatchGroupUiAction>(WatchGroupUiAction.ShowPaymentRecorded), actions)
            assertEquals(listOf("watch_payment_recorded" to mapOf<String, Any>("is_online" to true)), trackedEvents)
        }

    @Test
    fun `GIVEN the question to Carla offline WHEN confirming THEN records it on the watch and tracks it offline`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)
            viewModel.onEvent(WatchGroupUiEvent.OnIPaidClicked("carla"))
            trackedEvents.clear()

            // When
            viewModel.onEvent(WatchGroupUiEvent.OnPaymentConfirmed)

            // Then
            assertEquals(1, recordedCalls.size)
            assertEquals(listOf<WatchGroupUiAction>(WatchGroupUiAction.ShowPaymentRecorded), actions)
            assertEquals(listOf("watch_payment_recorded" to mapOf<String, Any>("is_online" to false)), trackedEvents)
        }

    @Test
    fun `GIVEN you owe only Carla WHEN the payment is being saved THEN it shows recording and hides I paid`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(recordGate = CompletableDeferred())
            viewModel.onEvent(WatchGroupUiEvent.OnIPaidClicked("carla"))

            // When
            viewModel.onEvent(WatchGroupUiEvent.OnPaymentConfirmed)

            // Then
            assertTrue(viewModel.uiState.value.isRecording)
            assertFalse(viewModel.uiState.value.showsIPaid)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN you owe two people WHEN the payment is being saved THEN the rows are not clickable`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                balances = youOweTwo,
                recordGate = CompletableDeferred(),
            )
            viewModel.onEvent(WatchGroupUiEvent.OnIPaidClicked("carla"))

            // When
            viewModel.onEvent(WatchGroupUiEvent.OnPaymentConfirmed)

            // Then
            assertFalse(viewModel.uiState.value.areDebtsClickable)
        }

    @Test
    fun `GIVEN the payment to Carla being saved WHEN tapping I paid on Bia THEN keeps asking about Carla`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                balances = youOweTwo,
                recordGate = CompletableDeferred(),
            )
            viewModel.onEvent(WatchGroupUiEvent.OnIPaidClicked("carla"))
            viewModel.onEvent(WatchGroupUiEvent.OnPaymentConfirmed)

            // When
            viewModel.onEvent(WatchGroupUiEvent.OnIPaidClicked("bia"))

            // Then
            assertEquals(
                "carla",
                viewModel.uiState.value.confirming
                    ?.memberId,
            )
        }

    @Test
    fun `GIVEN the save fails WHEN confirming THEN keeps the question and stays on the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(recordResult = Result.failure(IllegalStateException("disk full")))
            viewModel.onEvent(WatchGroupUiEvent.OnIPaidClicked("carla"))
            trackedEvents.clear()

            // When
            viewModel.onEvent(WatchGroupUiEvent.OnPaymentConfirmed)

            // Then
            assertFalse(viewModel.uiState.value.isRecording)
            assertEquals(carlaDebt, viewModel.uiState.value.confirming)
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
            assertEquals(route, navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN the payment recorded WHEN tapping I paid again THEN it is ignored and not tracked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(WatchGroupUiEvent.OnIPaidClicked("carla"))
            viewModel.onEvent(WatchGroupUiEvent.OnPaymentConfirmed)
            trackedEvents.clear()

            // When
            viewModel.onEvent(WatchGroupUiEvent.OnIPaidClicked("carla"))

            // Then
            assertNull(viewModel.uiState.value.confirming)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the payment recorded WHEN payment recorded finishes showing THEN goes back to the balances`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(WatchGroupUiEvent.OnIPaidClicked("carla"))
            viewModel.onEvent(WatchGroupUiEvent.OnPaymentConfirmed)

            // When
            viewModel.onEvent(WatchGroupUiEvent.OnRecordedShown)

            // Then
            assertEquals(listOf(Route.Home, Route.WatchBalances), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the payment recorded and the group settled WHEN the confirmation ends THEN goes back only once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(WatchGroupUiEvent.OnIPaidClicked("carla"))
            viewModel.onEvent(WatchGroupUiEvent.OnPaymentConfirmed)
            peopleBalances.value = onlyOwedToYou
            peopleBalances.value = settled

            // When
            viewModel.onEvent(WatchGroupUiEvent.OnRecordedShown)

            // Then
            assertEquals(listOf(Route.Home, Route.WatchBalances), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the payment recorded and already back WHEN the confirmation ends again THEN stays on the balances`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(WatchGroupUiEvent.OnIPaidClicked("carla"))
            viewModel.onEvent(WatchGroupUiEvent.OnPaymentConfirmed)
            viewModel.onEvent(WatchGroupUiEvent.OnRecordedShown)

            // When
            viewModel.onEvent(WatchGroupUiEvent.OnRecordedShown)

            // Then
            assertEquals(listOf(Route.Home, Route.WatchBalances), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the group on top WHEN a sync settles it THEN goes back to the balances`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        peopleBalances.value = settled

        // Then
        assertEquals(listOf(Route.Home, Route.WatchBalances), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a sync settled the group WHEN another sync comes THEN does not go back again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            peopleBalances.value = settled

            // When
            groupsMoney.value = mapOf(APE to moneyWith(settlement(APE, isPending = false)))

            // Then
            assertEquals(listOf(Route.Home, Route.WatchBalances), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the group on top WHEN it is deleted THEN goes back to the balances`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        peopleBalances.value = balances(person("Ana", pair(APE, "ana-ape", 3_000L)))

        // Then
        assertEquals(listOf(Route.Home, Route.WatchBalances), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another screen over the group WHEN a sync settles the group THEN stays where it is`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.Profile)

            // When
            peopleBalances.value = settled

            // Then
            assertEquals(listOf(Route.Home, Route.WatchBalances, route, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the question to Carla WHEN a sync changes what you owe her THEN asks with the new amount`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(WatchGroupUiEvent.OnIPaidClicked("carla"))

            // When
            peopleBalances.value = balances(
                person("Carla", pair(FLORIPA, "carla", -5_000L), face = carlaFace),
                person("Ana", pair(FLORIPA, "ana", 3_000L)),
            )

            // Then
            assertEquals(
                -5_000L,
                viewModel.uiState.value.confirming
                    ?.amountMinor,
            )
        }

    @Test
    fun `GIVEN the question to Carla WHEN a sync settles her pair but not the group THEN the question goes away`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(WatchGroupUiEvent.OnIPaidClicked("carla"))

            // When
            peopleBalances.value = onlyOwedToYou

            // Then
            assertNull(viewModel.uiState.value.confirming)
            assertEquals(route, navigator.backStack.value.last())
        }

    private fun TestScope.prepareScenario(
        balances: PeopleBalances = youOweCarla,
        money: Map<String, GroupMoney> = emptyMap(),
        online: Boolean = true,
        recordResult: Result<String> = Result.success("settlement-1"),
        recordGate: CompletableDeferred<Unit>? = null,
        showsPix: Boolean = false,
    ) {
        peopleBalances = MutableStateFlow(balances)
        groupsMoney = MutableStateFlow(money)
        isOnline = MutableStateFlow(online)
        navigator = BackStackNavigator(startRoute = Route.Home).also {
            it.navigate(Route.WatchBalances)
            it.navigate(route)
        }
        viewModel = WatchGroupViewModel(
            route = route,
            navigator = navigator,
            recordSettlement = { groupId, fromMemberId, toMemberId, amountMinor, currency, method, paidAt ->
                recordedCalls += RecordedCall(groupId, fromMemberId, toMemberId, amountMinor, currency, method, paidAt)
                recordGate?.await()
                recordResult
            },
            currentTimeMillis = { NOW },
            connectivityObserver = NetworkConnectivityObserver { isOnline },
            observeShowsPix = { flowOf(showsPix) },
            observePeopleBalances = { peopleBalances },
            observeGroupsMoney = { groupsMoney },
            trackEvent = { name, params -> trackedEvents += name to params },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val NOW = 1_234L
    }
}
