package com.quare.blitzsplit.feature.globalbalances

import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountStatus
import com.quare.blitzsplit.core.activity.domain.model.ChargeSent
import com.quare.blitzsplit.core.activity.domain.usecase.impl.RecordChargesUseCase
import com.quare.blitzsplit.core.activity.testing.FakeActivityRepository
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePeopleBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsMoneyUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObservePeopleBalancesUseCase
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
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObservePaymentKeysUseCase
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import com.quare.blitzsplit.feature.globalbalances.presentation.model.ChargeReroute
import com.quare.blitzsplit.feature.globalbalances.presentation.model.ChargeUiAction
import com.quare.blitzsplit.feature.globalbalances.presentation.model.ChargeUiEvent
import com.quare.blitzsplit.feature.globalbalances.presentation.viewmodel.ChargeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChargeViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var keys: FakePaymentKeyRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var activity: FakeActivityRepository
    private lateinit var viewModel: ChargeViewModel
    private val actions = mutableListOf<ChargeUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN someone who owes you WHEN Cobrar opens THEN builds the message from the debt and your Pix key`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val charge = viewModel.uiState.value.charge

            // Then
            assertEquals("Carla", charge?.name)
            assertEquals(3_290L, charge?.amountMinor)
            assertEquals(listOf("Viagem Floripa", "Apê 302", "Churrasco de sábado"), charge?.groupNames)
            assertEquals(true, charge?.includesPixKey)
            assertEquals(true, charge?.canTogglePixKey)
            assertEquals(pixKey, viewModel.uiState.value.pixKey)
        }

    @Test
    fun `GIVEN Carla owes you in two currencies WHEN Cobrar opens in dollars THEN the message is in dollars`() =
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
                currency = GroupCurrency.USD,
            )

            // When
            val charge = viewModel.uiState.value.charge

            // Then
            assertEquals("Carla", charge?.name)
            assertEquals(5_000L, charge?.amountMinor)
            assertEquals(listOf("Viagem EUA"), charge?.groupNames)
        }

    @Test
    fun `GIVEN a group with its debts simplified WHEN Cobrar opens THEN says whom they pay you instead of`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(simplifiedTrip(simplifies = true)),
                bills = simplifiedTripBills,
                personKey = "user:diego",
            )

            // When
            val charge = viewModel.uiState.value.charge

            // Then
            assertEquals(6_400L, charge?.amountMinor)
            assertEquals(listOf(ChargeReroute("Viagem", listOf("Bruno"))), charge?.reroutes)
        }

    @Test
    fun `GIVEN a guest WHEN Cobrar opens THEN the message carries their bills`() = runTest(testDispatcher) {
        // Given
        val diego = defaultGroups.flatMap { it.members }.single { it.name == "Diego" }
        prepareScenario(personKey = PeopleBalances.keyOf(diego))

        // When
        val charge = viewModel.uiState.value.charge

        // Then
        assertEquals(true, charge?.isGuest)
        assertEquals(listOf("Carne"), charge?.guestBills)
    }

    @Test
    fun `GIVEN no Pix key WHEN Cobrar opens THEN leaves the key out`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedKeys = emptyList())

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(false, state.charge?.includesPixKey)
        assertNull(state.pixKey)
    }

    @Test
    fun `GIVEN only a Bitcoin key WHEN Cobrar opens THEN offers no key`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedKeys = listOf(bitcoinKey))

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.pixKey)
        assertEquals(false, state.charge?.includesPixKey)
    }

    @Test
    fun `GIVEN Cobrar opened before the Pix key loaded WHEN the key arrives THEN includes it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedKeys = emptyList())

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
        viewModel.onEvent(ChargeUiEvent.OnPixKeyToggled(isIncluded = false))

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
            viewModel.onEvent(ChargeUiEvent.OnMessageChanged("Carla, me paga!"))

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
    fun `GIVEN Cobrar open WHEN switching the Pix key off THEN leaves it out`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ChargeUiEvent.OnPixKeyToggled(isIncluded = false))

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
        viewModel.onEvent(ChargeUiEvent.OnMessageChanged("Carla, me paga!"))

        // When
        viewModel.onEvent(ChargeUiEvent.OnPixKeyToggled(isIncluded = false))

        // Then
        val charge = viewModel.uiState.value.charge
        assertEquals("Carla, me paga!", charge?.editedMessage)
        assertEquals(true, charge?.includesPixKey)
        assertEquals(false, charge?.canTogglePixKey)
    }

    @Test
    fun `GIVEN the message edited WHEN a sync changes the debt THEN keeps the text as typed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ChargeUiEvent.OnMessageChanged("Carla, me paga!"))

            // When
            expenses.expenses.update { it + owes(FLORIPA, CARLA_FLORIPA, ME_FLORIPA, 1_000L, id = "e9") }

            // Then
            val charge = viewModel.uiState.value.charge
            assertEquals(4_290L, charge?.amountMinor)
            assertEquals("Carla, me paga!", charge?.editedMessage)
        }

    @Test
    fun `GIVEN Cobrar open WHEN tapping Enviar THEN opens the share sheet with the message`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ChargeUiEvent.OnShareClicked("Oi, Carla!"))

            // Then
            assertEquals(listOf<ChargeUiAction>(ChargeUiAction.ShareMessage("Oi, Carla!")), actions)
        }

    @Test
    fun `GIVEN Cobrar open WHEN tapping Copiar mensagem THEN copies the message`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ChargeUiEvent.OnCopyClicked("Oi, Carla!"))

        // Then
        assertEquals(listOf<ChargeUiAction>(ChargeUiAction.CopyMessage("Oi, Carla!")), actions)
    }

    @Test
    fun `GIVEN Cobrar over a profile WHEN closing it THEN goes back to the profile`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ChargeUiEvent.OnDismissed)

        // Then
        assertEquals(
            listOf(Route.Home, Route.Contacts, Route.ContactProfile(CARLA_KEY)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Cobrar for someone you owe WHEN it opens THEN goes back to the screen under it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = "user:bruno")

            // When
            val stack = navigator.backStack.value

            // Then
            assertNull(viewModel.uiState.value.charge)
            assertEquals(Route.ContactProfile("user:bruno"), stack.last())
        }

    @Test
    fun `GIVEN Cobrar open WHEN a sync settles the person THEN goes back to the screen under it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            expenses.expenses.update { list ->
                list.filterNot { it.shares.any { share -> share.memberId.startsWith("carla") } }
            }

            // Then
            assertEquals(Route.ContactProfile(CARLA_KEY), navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN Cobrar closed WHEN a sync settles the person THEN goes back only once`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ChargeUiEvent.OnDismissed)

        // When
        expenses.expenses.update { list ->
            list.filterNot { it.shares.any { share -> share.memberId.startsWith("carla") } }
        }

        // Then
        assertEquals(
            listOf(Route.Home, Route.Contacts, Route.ContactProfile(CARLA_KEY)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Cobrar open WHEN tapping Enviar THEN records a charge in each group where the person owes you`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ChargeUiEvent.OnShareClicked("Oi, Carla!"))

            // Then
            assertEquals(carlaCharges, activity.recordedCharges)
        }

    @Test
    fun `GIVEN Cobrar open WHEN tapping Copiar mensagem THEN records a charge in each group where they owe you`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ChargeUiEvent.OnCopyClicked("Oi, Carla!"))

            // Then
            assertEquals(carlaCharges, activity.recordedCharges)
        }

    @Test
    fun `GIVEN someone who owes you in a single group WHEN tapping Enviar THEN records one charge in that group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = "user:ana")

            // When
            viewModel.onEvent(ChargeUiEvent.OnShareClicked("Oi, Ana!"))

            // Then
            assertEquals(listOf(ChargeSent(FLORIPA, ANA_FLORIPA, "Ana", 16_230L)), activity.recordedCharges)
        }

    @Test
    fun `GIVEN the charge already sent WHEN tapping Copiar mensagem THEN records nothing more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ChargeUiEvent.OnShareClicked("Oi, Carla!"))
            viewModel.onEvent(ChargeUiEvent.OnShareClicked("Oi, Carla!"))

            // When
            viewModel.onEvent(ChargeUiEvent.OnCopyClicked("Oi, Carla!"))

            // Then
            assertEquals(carlaCharges, activity.recordedCharges)
            assertEquals(1, activity.chargeSentAts.size)
        }

    @Test
    fun `GIVEN the charge sent then its message edited WHEN tapping Enviar again THEN records nothing more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ChargeUiEvent.OnShareClicked("Oi, Carla!"))
            viewModel.onEvent(ChargeUiEvent.OnMessageChanged("Carla, me paga!"))

            // When
            viewModel.onEvent(ChargeUiEvent.OnShareClicked("Carla, me paga!"))

            // Then
            assertEquals(carlaCharges, activity.recordedCharges)
        }

    @Test
    fun `GIVEN Cobrar closed unsent WHEN a late Copiar mensagem arrives THEN copies and records nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ChargeUiEvent.OnDismissed)

            // When
            viewModel.onEvent(ChargeUiEvent.OnCopyClicked("Oi, Carla!"))

            // Then
            assertTrue(activity.recordedCharges.isEmpty())
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN Cobrar popped by the platform WHEN a late Copiar arrives THEN copies records and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigateBack()

            // When
            viewModel.onEvent(ChargeUiEvent.OnCopyClicked("Oi, Carla!"))

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(activity.recordedCharges.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Cobrar open WHEN tapping Enviar THEN sends the share without the message`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ChargeUiEvent.OnShareClicked("Oi, Carla!"))

        // Then
        assertEquals(listOf("charge_share_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN Cobrar open WHEN tapping Copiar mensagem THEN sends the copy without the message`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ChargeUiEvent.OnCopyClicked("Oi, Carla!"))

            // Then
            assertEquals(listOf("charge_copy_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN Cobrar closed WHEN a late Enviar Copiar or Pix switch arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ChargeUiEvent.OnDismissed)

            // When
            viewModel.onEvent(ChargeUiEvent.OnShareClicked("Oi, Carla!"))
            viewModel.onEvent(ChargeUiEvent.OnCopyClicked("Oi, Carla!"))
            viewModel.onEvent(ChargeUiEvent.OnPixKeyToggled(isIncluded = false))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the Pix key switched off WHEN switching it on again THEN sends one event per toggle`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ChargeUiEvent.OnPixKeyToggled(isIncluded = false))

            // When
            viewModel.onEvent(ChargeUiEvent.OnPixKeyToggled(isIncluded = true))

            // Then
            assertEquals(
                listOf(
                    "charge_pix_key_toggled" to mapOf<String, Any>("is_included" to false),
                    "charge_pix_key_toggled" to mapOf<String, Any>("is_included" to true),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the message edited WHEN switching the Pix key THEN sends nothing since the switch is locked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ChargeUiEvent.OnMessageChanged("Carla, me paga!"))

            // When
            viewModel.onEvent(ChargeUiEvent.OnPixKeyToggled(isIncluded = false))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Cobrar open WHEN typing the message and closing it THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ChargeUiEvent.OnMessageChanged("Carla, me paga!"))
        viewModel.onEvent(ChargeUiEvent.OnDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN two Pix keys and the second marked default WHEN Cobrar opens THEN offers the default one`() =
        runTest(testDispatcher) {
            // Given
            val phoneKey = PaymentKey(
                "key-3",
                PaymentKeyType.PHONE,
                "+5511999999999",
                isDefault = false,
                addedAt = 2L,
            )
            prepareScenario(storedKeys = listOf(phoneKey, pixKey))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(pixKey, state.pixKey)
        }

    @Test
    fun `GIVEN two Pix keys none marked default WHEN Cobrar opens THEN offers the first one`() =
        runTest(testDispatcher) {
            // Given
            val phoneKey = PaymentKey(
                "key-3",
                PaymentKeyType.PHONE,
                "+5511999999999",
                isDefault = false,
                addedAt = 2L,
            )
            val emailKey = pixKey.copy(isDefault = false)
            prepareScenario(storedKeys = listOf(phoneKey, emailKey))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(phoneKey, state.pixKey)
        }

    @Test
    fun `GIVEN a default Bitcoin key and a Pix key not marked default WHEN Cobrar opens THEN offers the Pix key`() =
        runTest(testDispatcher) {
            // Given
            val emailKey = pixKey.copy(isDefault = false)
            prepareScenario(storedKeys = listOf(bitcoinKey, emailKey))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(emailKey, state.pixKey)
            assertEquals(true, state.charge?.includesPixKey)
        }

    @Test
    fun `GIVEN the Pix key switched off then the message edited WHEN switching it on THEN stays off`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ChargeUiEvent.OnPixKeyToggled(isIncluded = false))
            viewModel.onEvent(ChargeUiEvent.OnMessageChanged("Carla, me paga!"))

            // When
            viewModel.onEvent(ChargeUiEvent.OnPixKeyToggled(isIncluded = true))

            // Then
            val charge = viewModel.uiState.value.charge
            assertEquals(false, charge?.includesPixKey)
            assertEquals(false, charge?.canTogglePixKey)
            assertEquals("Carla, me paga!", charge?.editedMessage)
        }

    @Test
    fun `GIVEN a guest who already paid part of the bill WHEN Cobrar opens THEN the message says what they paid`() =
        runTest(testDispatcher) {
            // Given
            val diego = GroupMember(
                DIEGO_BAR,
                "Diego",
                AvatarColor.GREEN,
                isMe = false,
                hasJoined = false,
                userId = null,
                inviteCode = null,
                photoUrl = null,
            )
            val beer = owes(
                BAR,
                DIEGO_BAR,
                ME_BAR,
                10_000L,
                id = "e10",
                description = "Cerveja",
            ).copy(
                payers = listOf(ExpensePayer(ME_BAR, 7_000L), ExpensePayer(DIEGO_BAR, 3_000L)),
                shares = listOf(ExpenseShare(DIEGO_BAR, 6_000L, 6_000L), ExpenseShare(ME_BAR, 4_000L, 4_000L)),
            )
            prepareScenario(
                storedGroups = listOf(group(BAR, "Bar", me(ME_BAR), diego)),
                bills = listOf(beer),
                personKey = PeopleBalances.keyOf(diego),
            )

            // When
            val charge = viewModel.uiState.value.charge

            // Then
            assertEquals(true, charge?.isGuest)
            assertEquals(3_000L, charge?.amountMinor)
            assertEquals(listOf("Cerveja"), charge?.guestBills)
            assertEquals(3_000L, charge?.guestPaidMinor)
        }

    @Test
    fun `GIVEN a guest WHEN tapping Enviar THEN records the charge by their name in their group`() =
        runTest(testDispatcher) {
            // Given
            val diego = defaultGroups.flatMap { it.members }.single { it.name == "Diego" }
            prepareScenario(personKey = PeopleBalances.keyOf(diego))

            // When
            viewModel.onEvent(ChargeUiEvent.OnShareClicked("Oi, Diego!"))

            // Then
            assertEquals(listOf(ChargeSent(CHURRASCO, DIEGO_CHURRASCO, "Diego", 4_500L)), activity.recordedCharges)
        }

    @Test
    fun `GIVEN Carla owes you in two currencies WHEN tapping Enviar in dollars THEN records only the dollar group`() =
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
                currency = GroupCurrency.USD,
            )

            // When
            viewModel.onEvent(ChargeUiEvent.OnShareClicked("Hi, Carla!"))

            // Then
            assertEquals(listOf(ChargeSent("usa", "carla-usa", "Carla", 5_000L)), activity.recordedCharges)
        }

    @Test
    fun `GIVEN Carla owes you only in reais WHEN Cobrar opens in dollars THEN goes back to the screen under it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(currency = GroupCurrency.USD)

            // When
            val stack = navigator.backStack.value

            // Then
            assertNull(viewModel.uiState.value.charge)
            assertEquals(listOf(Route.Home, Route.Contacts, Route.ContactProfile(CARLA_KEY)), stack)
        }

    @Test
    fun `GIVEN Cobrar open WHEN a sync settles the person THEN keeps the last message drawn while it leaves`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ChargeUiEvent.OnMessageChanged("Carla, me paga!"))

            // When
            expenses.expenses.update { list ->
                list.filterNot { it.shares.any { share -> share.memberId.startsWith("carla") } }
            }

            // Then
            val charge = viewModel.uiState.value.charge
            assertEquals(3_290L, charge?.amountMinor)
            assertEquals("Carla, me paga!", charge?.editedMessage)
        }

    @Test
    fun `GIVEN Cobrar popped by the platform WHEN a sync settles the person THEN the profile under it stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigateBack()

            // When
            expenses.expenses.update { list ->
                list.filterNot { it.shares.any { share -> share.memberId.startsWith("carla") } }
            }

            // Then
            assertEquals(
                listOf(Route.Home, Route.Contacts, Route.ContactProfile(CARLA_KEY)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Cobrar popped by the platform WHEN a late close arrives THEN the profile under it stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigateBack()

            // When
            viewModel.onEvent(ChargeUiEvent.OnDismissed)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Contacts, Route.ContactProfile(CARLA_KEY)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Cobrar closed WHEN a second close arrives THEN goes back only once`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ChargeUiEvent.OnDismissed)

        // When
        viewModel.onEvent(ChargeUiEvent.OnDismissed)

        // Then
        assertEquals(
            listOf(Route.Home, Route.Contacts, Route.ContactProfile(CARLA_KEY)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Cobrar closed unsent WHEN a late Enviar arrives THEN shares and records nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ChargeUiEvent.OnDismissed)

            // When
            viewModel.onEvent(ChargeUiEvent.OnShareClicked("Oi, Carla!"))

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(activity.recordedCharges.isEmpty())
        }

    @Test
    fun `GIVEN Cobrar popped by the platform WHEN a late Enviar arrives THEN shares records and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigateBack()

            // When
            viewModel.onEvent(ChargeUiEvent.OnShareClicked("Oi, Carla!"))

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(activity.recordedCharges.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Cobrar popped by the platform WHEN a late Pix switch arrives THEN the switch and analytics stay`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigateBack()

            // When
            viewModel.onEvent(ChargeUiEvent.OnPixKeyToggled(isIncluded = false))

            // Then
            assertTrue(trackedEvents.isEmpty())
            assertEquals(
                true,
                viewModel.uiState.value.charge
                    ?.includesPixKey,
            )
        }

    @Test
    fun `GIVEN Cobrar over Saldos WHEN closing it THEN goes back to Saldos`() = runTest(testDispatcher) {
        // Given
        val balances = Route.GlobalBalances(BalancesFilter.ALL)
        prepareScenario(openedOver = listOf(balances))

        // When
        viewModel.onEvent(ChargeUiEvent.OnDismissed)

        // Then
        assertEquals(listOf(Route.Home, balances), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Cobrar over the profile beside Contatos in a wide window WHEN closing it THEN goes back to Contatos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(openedOver = listOf(Route.Contacts))

            // When
            viewModel.onEvent(ChargeUiEvent.OnDismissed)

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the balances not read yet WHEN an Enviar arrives THEN shares records and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(balancesRead = false)

            // When
            viewModel.onEvent(ChargeUiEvent.OnShareClicked("Oi, Carla!"))

            // Then
            assertNull(viewModel.uiState.value.charge)
            assertTrue(actions.isEmpty())
            assertTrue(activity.recordedCharges.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the balances not read yet WHEN a Copiar mensagem arrives THEN copies records and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(balancesRead = false)

            // When
            viewModel.onEvent(ChargeUiEvent.OnCopyClicked("Oi, Carla!"))

            // Then
            assertNull(viewModel.uiState.value.charge)
            assertTrue(actions.isEmpty())
            assertTrue(activity.recordedCharges.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    // Why: Cobrar opened from the person's profile over Contatos, as the profile's own button pushes it.
    private fun TestScope.prepareScenario(
        storedGroups: List<Group> = defaultGroups,
        bills: List<Expense> = defaultBills,
        storedKeys: List<PaymentKey> = listOf(bitcoinKey, pixKey),
        personKey: String = CARLA_KEY,
        currency: GroupCurrency = GroupCurrency.BRL,
        openedOver: List<Route> = listOf(Route.Contacts, Route.ContactProfile(personKey)),
        balancesRead: Boolean = true,
    ) {
        val groups = FakeGroupRepository(initialGroups = storedGroups)
        expenses = FakeExpenseRepository(initialExpenses = bills)
        keys = FakePaymentKeyRepository(initialKeys = storedKeys)
        activity = FakeActivityRepository()
        val route = Route.Charge(personKey, currency)
        navigator = BackStackNavigator(startRoute = Route.Home).also { navigator ->
            openedOver.forEach(navigator::navigate)
            navigator.navigate(route)
        }
        val accountStatus = ObserveAccountStatus { flowOf(AccountStatus.Connected("pierre", AccountProvider.GOOGLE)) }
        viewModel = ChargeViewModel(
            route = route,
            navigator = navigator,
            recordCharges = RecordChargesUseCase(activity, accountStatus) { 0L },
            // Why: Room not read yet is a flow that hasn't emitted.
            observePeopleBalances = if (balancesRead) {
                ObservePeopleBalancesUseCase(
                    observeGroups = ObserveGroupsUseCase(groups),
                    observeGroupsMoney = ObserveGroupsMoneyUseCase(expenses, FakeSettlementRepository()),
                )
            } else {
                ObservePeopleBalances { flow { awaitCancellation() } }
            },
            observePaymentKeys = ObservePaymentKeysUseCase(keys),
            trackEvent = { name, params -> trackedEvents += name to params },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val CARLA_KEY = "user:carla"
        const val BAR = "bar"
        const val ME_BAR = "me-bar"
        const val DIEGO_BAR = "diego-bar"
    }
}
