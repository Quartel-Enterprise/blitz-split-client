package com.quare.blitzsplit.feature.contacts

import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.contacts.domain.model.GuestMerge
import com.quare.blitzsplit.core.contacts.domain.model.HiddenContact
import com.quare.blitzsplit.core.contacts.domain.usecase.AddContactToGroup
import com.quare.blitzsplit.core.contacts.domain.usecase.CancelGuestMerge
import com.quare.blitzsplit.core.contacts.domain.usecase.FindPendingGuestMerge
import com.quare.blitzsplit.core.contacts.domain.usecase.impl.CancelGuestMergeUseCase
import com.quare.blitzsplit.core.contacts.domain.usecase.impl.FindPendingGuestMergeUseCase
import com.quare.blitzsplit.core.contacts.domain.usecase.impl.RequestGuestMergeUseCase
import com.quare.blitzsplit.core.contacts.domain.usecase.impl.SetContactHiddenUseCase
import com.quare.blitzsplit.core.contacts.testing.FakeGuestMergeRepository
import com.quare.blitzsplit.core.contacts.testing.FakeHiddenContactRepository
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.sync.domain.usecase.ClaimGuestAsMe
import com.quare.blitzsplit.feature.contacts.presentation.model.AddGroupOption
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiAction
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import software.quare.connectivity.NetworkConnectivityObserver
import software.quare.loadable.Loadable
import software.quare.loadable.isLoading
import software.quare.loadable.valueOrNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContactProfileViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var hidden: FakeHiddenContactRepository
    private lateinit var merges: FakeGuestMergeRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: ContactProfileViewModel
    private val actions = mutableListOf<ContactProfileUiAction>()

    // Why: whom "Adicionar" added to which group.
    private val addedToGroups = mutableListOf<Pair<String, String>>()

    // Why: the guests "Eu sou Diego" asked the claim to take, once per call.
    private val claimedContacts = mutableListOf<Contact>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN Carla owes you WHEN her profile opens THEN shows her with the history`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = CARLA_KEY)

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isLoaded)
        assertEquals("Carla", state.contact?.name)
        assertEquals(5_000L, state.contact?.net?.amountMinor)
        assertEquals(listOf("dinner"), state.history.map { it.id })
        assertFalse(state.hasMoreHistory)
        assertTrue(state.isNetOfSeveralGroups)
    }

    @Test
    fun `GIVEN five bills with Carla WHEN her profile opens THEN shows the latest four and that there is more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = CARLA_KEY,
                bills = (1..5).map { index ->
                    owes(
                        FLORIPA,
                        memberId("carla", FLORIPA),
                        meId(FLORIPA),
                        1_000L,
                        id = "bill-$index",
                        spentAt =
                            index * 10L,
                    )
                },
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf("bill-5", "bill-4", "bill-3", "bill-2"), state.history.map { it.id })
            assertTrue(state.hasMoreHistory)
        }

    @Test
    fun `GIVEN the history WHEN Ver tudo is tapped THEN opens Atividade on that person over the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnSeeAllHistoryClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.Contacts,
                    Route.ContactProfile(CARLA_KEY),
                    Route.Activity(personKey = CARLA_KEY),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the profile opened again from her history WHEN Ver tudo is tapped THEN goes back to that history`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnSeeAllHistoryClicked)
            navigator.navigate(Route.ContactProfile(CARLA_KEY))

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnSeeAllHistoryClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.Contacts,
                    Route.ContactProfile(CARLA_KEY),
                    Route.Activity(personKey = CARLA_KEY),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Carla owes you WHEN Cobrar is tapped THEN opens her message over the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnChargeClicked)

            // Then
            assertEquals(
                Route.Charge(CARLA_KEY, GroupCurrency.BRL),
                navigator.backStack.value.last(),
            )
            assertEquals(4, navigator.backStack.value.size)
        }

    @Test
    fun `GIVEN you owe Bruno WHEN Cobrar is tapped THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = BRUNO_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnChargeClicked)

        // Then
        assertEquals(Route.ContactProfile(BRUNO_KEY), navigator.backStack.value.last())
    }

    @Test
    fun `GIVEN Carla owes you WHEN Recebi is tapped THEN opens Registrar pagamento for her net`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnReceivedClicked)

            // Then
            assertEquals(Route.SettleUp(CARLA_KEY, GroupCurrency.BRL), navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN the profile opened from her settling up WHEN Recebi is tapped THEN goes back to that form`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = CARLA_KEY,
                openedFrom = Route.SettleUp(CARLA_KEY, GroupCurrency.BRL),
            )

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnReceivedClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.SettleUp(CARLA_KEY, GroupCurrency.BRL)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the profile opened from settling up in dollars WHEN Recebi is tapped THEN opens the one in reais`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = CARLA_KEY,
                openedFrom = Route.SettleUp(CARLA_KEY, GroupCurrency.USD),
            )

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnReceivedClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.SettleUp(CARLA_KEY, GroupCurrency.USD),
                    Route.ContactProfile(CARLA_KEY),
                    Route.SettleUp(CARLA_KEY, GroupCurrency.BRL),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN you owe Bruno WHEN Acertar is tapped THEN opens Registrar pagamento for his net`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = BRUNO_KEY)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnSettleClicked)

            // Then
            assertEquals(Route.SettleUp(BRUNO_KEY, GroupCurrency.BRL), navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN Carla owes you WHEN Acertar is tapped THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = CARLA_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnSettleClicked)

        // Then
        assertEquals(Route.ContactProfile(CARLA_KEY), navigator.backStack.value.last())
    }

    @Test
    fun `GIVEN you owe Bruno WHEN Recebi is tapped THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = BRUNO_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnReceivedClicked)

        // Then
        assertEquals(Route.ContactProfile(BRUNO_KEY), navigator.backStack.value.last())
    }

    @Test
    fun `GIVEN her own profile WHEN a group in common is tapped THEN opens its Despesas over the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnGroupClicked(FLORIPA))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.Contacts,
                    Route.ContactProfile(CARLA_KEY),
                    floripaExpenses,
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Carla in two groups WHEN the second group in common is tapped THEN opens that one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnGroupClicked(APE))

            // Then
            assertEquals(
                Route.GroupDetail(
                    APE,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
                navigator.backStack.value.last(),
            )
            assertEquals(4, navigator.backStack.value.size)
        }

    @Test
    fun `GIVEN Carla in the pane beside Contatos WHEN a group in common is tapped THEN opens it over Contatos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = CARLA_KEY,
                inPane = true,
            )

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnGroupClicked(FLORIPA))

            // Then
            assertEquals(
                listOf(Route.Home, Route.Contacts, floripaExpenses),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN her profile opened from Saldos WHEN a group in common is tapped THEN opens it over the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = CARLA_KEY,
                openedFrom = Route.GlobalBalances(BalancesFilter.ALL),
            )

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnGroupClicked(FLORIPA))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.GlobalBalances(BalancesFilter.ALL),
                    Route.ContactProfile(CARLA_KEY),
                    floripaExpenses,
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Carla in Saldos WHEN her profile opens over it THEN its home tab is Saldos`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            personKey = CARLA_KEY,
            openedFrom = Route.GlobalBalances(BalancesFilter.OWES_YOU),
        )

        // When
        val homeTab = viewModel.uiState.value.homeTab

        // Then
        assertEquals(HomeTab.BALANCES, homeTab)
    }

    @Test
    fun `GIVEN Carla in Contatos WHEN her profile opens over the list THEN it sits right above the tab`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)

            // When
            val isOverHomeTab = viewModel.uiState.value.isOverHomeTab

            // Then
            assertTrue(isOverHomeTab)
        }

    @Test
    fun `GIVEN Carla in a bill WHEN her profile opens over it THEN it is not right above a tab`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = CARLA_KEY,
                openedFrom = Route.BillDetail(FLORIPA, "dinner", focusCommentId = null),
            )

            // When
            val isOverHomeTab = viewModel.uiState.value.isOverHomeTab

            // Then
            assertFalse(isOverHomeTab)
        }

    @Test
    fun `GIVEN Carla in Contatos WHEN her profile opens beside the list THEN its home tab is Contatos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = CARLA_KEY,
                inPane = true,
            )

            // When
            val homeTab = viewModel.uiState.value.homeTab

            // Then
            assertEquals(HomeTab.CONTACTS, homeTab)
        }

    @Test
    fun `GIVEN Diego a guest WHEN a group in common is tapped THEN opens it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = DIEGO_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnGroupClicked(FLORIPA))

        // Then
        assertEquals(floripaExpenses, navigator.backStack.value.last())
        assertEquals(4, navigator.backStack.value.size)
    }

    @Test
    fun `GIVEN the device offline WHEN a group in common is tapped THEN still opens it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            personKey = CARLA_KEY,
            online = false,
        )

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnGroupClicked(FLORIPA))

        // Then
        assertEquals(floripaExpenses, navigator.backStack.value.last())
    }

    @Test
    fun `GIVEN a group Carla is not in WHEN it is tapped THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = CARLA_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnGroupClicked(FUTEBOL))

        // Then
        assertEquals(
            listOf(Route.Home, Route.Contacts, Route.ContactProfile(CARLA_KEY)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a sync took Ape 302 away WHEN a stale tap on it arrives THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = CARLA_KEY)
        groups.groups.value = listOf(floripa(), futebol())

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnGroupClicked(APE))

        // Then
        assertEquals(
            listOf(Route.Home, Route.Contacts, Route.ContactProfile(CARLA_KEY)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Registrar pagamento over the profile WHEN a late group tap arrives THEN stacks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnReceivedClicked)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnGroupClicked(FLORIPA))

            // Then
            assertEquals(Route.SettleUp(CARLA_KEY, GroupCurrency.BRL), navigator.backStack.value.last())
            assertEquals(4, navigator.backStack.value.size)
        }

    @Test
    fun `GIVEN the group already opened from the profile WHEN a second tap arrives THEN stacks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnGroupClicked(FLORIPA))

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnGroupClicked(APE))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.Contacts,
                    Route.ContactProfile(CARLA_KEY),
                    floripaExpenses,
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Carla in the pane beside Contatos WHEN Cobrar is tapped THEN opens the message over Contatos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = CARLA_KEY,
                inPane = true,
            )

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnChargeClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.Contacts,
                    Route.Charge(CARLA_KEY, GroupCurrency.BRL),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Registrar pagamento already over the profile WHEN a late Cobrar arrives THEN stacks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnReceivedClicked)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnChargeClicked)

            // Then
            assertEquals(Route.SettleUp(CARLA_KEY, GroupCurrency.BRL), navigator.backStack.value.last())
            assertEquals(4, navigator.backStack.value.size)
        }

    @Test
    fun `GIVEN her profile WHEN Ver em Saldos is tapped THEN opens the Saldos tab over Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnViewBalancesClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN her own profile WHEN back is tapped THEN goes back to Contatos`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = CARLA_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Julia settled WHEN Ocultar is tapped THEN asks first`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = JULIA_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnHideClicked)

        // Then
        assertTrue(viewModel.uiState.value.isHideConfirmOpen)
        assertTrue(hidden.hidden.value.isEmpty())
    }

    @Test
    fun `GIVEN Carla owes you WHEN Ocultar is tapped THEN asks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = CARLA_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnHideClicked)

        // Then
        assertFalse(viewModel.uiState.value.isHideConfirmOpen)
    }

    @Test
    fun `GIVEN Julia already hidden WHEN Ocultar is tapped THEN asks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            personKey = JULIA_KEY,
            storedHidden = listOf(
                HiddenContact(
                    JULIA_KEY,
                    isHidden = true,
                    hiddenAt = 10L,
                ),
            ),
        )

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnHideClicked)

        // Then
        assertFalse(viewModel.uiState.value.isHideConfirmOpen)
        assertTrue(viewModel.uiState.value.canShow)
    }

    @Test
    fun `GIVEN the question open on her profile WHEN confirmed THEN hides her and goes back to Contatos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = JULIA_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnHideClicked)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnHideConfirmed)

            // Then
            assertTrue(
                hidden.hidden.value
                    .single { it.personKey == JULIA_KEY }
                    .isHidden,
            )
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
            assertFalse(viewModel.uiState.value.isHideConfirmOpen)
        }

    @Test
    fun `GIVEN the question open in the pane WHEN hiding is confirmed THEN hides her and the pane keeps her`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = JULIA_KEY,
                inPane = true,
            )
            viewModel.onEvent(ContactProfileUiEvent.OnHideClicked)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnHideConfirmed)

            // Then
            assertTrue(
                hidden.hidden.value
                    .single { it.personKey == JULIA_KEY }
                    .isHidden,
            )
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
            assertTrue(viewModel.uiState.value.canShow)
        }

    @Test
    fun `GIVEN the device fails to save WHEN hiding is confirmed THEN says so and stays on the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = JULIA_KEY)
            hidden.setHiddenError = IllegalStateException("disk full")
            viewModel.onEvent(ContactProfileUiEvent.OnHideClicked)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnHideConfirmed)

            // Then
            assertEquals(listOf<ContactProfileUiAction>(ContactProfileUiAction.ShowChangeError), actions)
            assertEquals(Route.ContactProfile(JULIA_KEY), navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN the question never asked WHEN hiding is confirmed THEN hides nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = JULIA_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnHideConfirmed)

        // Then
        assertTrue(hidden.hidden.value.isEmpty())
    }

    @Test
    fun `GIVEN the question open WHEN it is dismissed THEN closes without hiding`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = JULIA_KEY)
        viewModel.onEvent(ContactProfileUiEvent.OnHideClicked)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnHideDismissed)

        // Then
        assertFalse(viewModel.uiState.value.isHideConfirmOpen)
        assertTrue(hidden.hidden.value.isEmpty())
    }

    @Test
    fun `GIVEN the question open WHEN a bill opens her balance again THEN the question closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = JULIA_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnHideClicked)

            // When
            expenses.expenses.value += owes(FUTEBOL, memberId("julia", FUTEBOL), meId(FUTEBOL), 1_000L, id = "ball")

            // Then
            assertFalse(viewModel.uiState.value.isHideConfirmOpen)
        }

    @Test
    fun `GIVEN Julia hidden WHEN Mostrar is tapped THEN shows her in Contatos again`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            personKey = JULIA_KEY,
            storedHidden = listOf(
                HiddenContact(
                    JULIA_KEY,
                    isHidden = true,
                    hiddenAt = 10L,
                ),
            ),
        )

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnShowClicked)

        // Then
        assertFalse(
            hidden.hidden.value
                .single { it.personKey == JULIA_KEY }
                .isHidden,
        )
        assertFalse(viewModel.uiState.value.canShow)
    }

    @Test
    fun `GIVEN the device fails to save WHEN Mostrar is tapped THEN says the change failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = JULIA_KEY,
                storedHidden = listOf(
                    HiddenContact(
                        JULIA_KEY,
                        isHidden = true,
                        hiddenAt = 10L,
                    ),
                ),
            )
            hidden.setHiddenError = IllegalStateException("disk full")

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnShowClicked)

            // Then
            assertEquals(listOf<ContactProfileUiAction>(ContactProfileUiAction.ShowChangeError), actions)
        }

    @Test
    fun `GIVEN Carla in two of three groups WHEN adding her to a group THEN lists every group and marks the other`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupClicked)

            // Then
            val draft = assertNotNull(viewModel.uiState.value.addToGroup)
            assertEquals(
                listOf(FLORIPA to true, APE to true, FUTEBOL to false),
                draft.options.map { it.groupId to it.isAlreadyIn },
            )
            assertEquals(listOf(4, 2, 2), draft.options.map(AddGroupOption::memberCount))
            assertEquals("Futebol de quinta", draft.options.last().name)
            assertEquals(FUTEBOL, draft.selectedGroupId)
            assertTrue(draft.canAdd)
        }

    @Test
    fun `GIVEN Julia in one group WHEN Adicionar a um grupo opens THEN marks the first group she is not in`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = JULIA_KEY)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupClicked)

            // Then
            assertEquals(
                FLORIPA,
                viewModel.uiState.value.addToGroup
                    ?.selectedGroupId,
            )
        }

    @Test
    fun `GIVEN the groups to add Julia to WHEN another group is picked THEN marks it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = JULIA_KEY)
        viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupClicked)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnAddGroupPicked(APE))

        // Then
        assertEquals(
            APE,
            viewModel.uiState.value.addToGroup
                ?.selectedGroupId,
        )
    }

    @Test
    fun `GIVEN the groups to add Julia to WHEN a group she is already in is picked THEN keeps the one marked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = JULIA_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupClicked)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnAddGroupPicked(FUTEBOL))

            // Then
            assertEquals(
                FLORIPA,
                viewModel.uiState.value.addToGroup
                    ?.selectedGroupId,
            )
        }

    @Test
    fun `GIVEN Ape 302 marked for Julia WHEN Adicionar is tapped THEN adds her and says so`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = JULIA_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupClicked)
            viewModel.onEvent(ContactProfileUiEvent.OnAddGroupPicked(APE))

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupConfirmed)

            // Then
            assertEquals(listOf(JULIA_KEY to APE), addedToGroups)
            assertEquals(
                listOf<ContactProfileUiAction>(ContactProfileUiAction.ShowAddedToGroup("Júlia", "Apê 302")),
                actions,
            )
            assertNull(viewModel.uiState.value.addToGroup)
        }

    @Test
    fun `GIVEN the device fails to add WHEN Adicionar is tapped THEN says the change failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = JULIA_KEY,
                addResult = Result.failure(IllegalStateException("disk full")),
            )
            viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupClicked)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupConfirmed)

            // Then
            assertEquals(listOf<ContactProfileUiAction>(ContactProfileUiAction.ShowChangeError), actions)
        }

    @Test
    fun `GIVEN Carla already in every group WHEN Adicionar is tapped THEN adds her nowhere`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = CARLA_KEY,
                storedGroups = listOf(floripa(), ape()),
            )
            viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupClicked)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupConfirmed)

            // Then
            assertNull(
                viewModel.uiState.value.addToGroup
                    ?.selectedGroupId,
            )
            assertTrue(addedToGroups.isEmpty())
        }

    @Test
    fun `GIVEN the groups to add to WHEN they are dismissed THEN they close`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = JULIA_KEY)
        viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupClicked)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupDismissed)

        // Then
        assertNull(viewModel.uiState.value.addToGroup)
        assertTrue(addedToGroups.isEmpty())
    }

    @Test
    fun `GIVEN her own profile on top WHEN a sync takes Carla away THEN the profile closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)

            // When
            groups.groups.value = listOf(futebol())

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Carla in the pane WHEN a sync takes her away THEN Contatos stays and shows nobody`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = CARLA_KEY,
                inPane = true,
            )

            // When
            groups.groups.value = listOf(futebol())

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
            assertNull(viewModel.uiState.value.contact)
        }

    @Test
    fun `GIVEN Registrar pagamento over her profile WHEN a sync takes Carla away THEN closes nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnReceivedClicked)

            // When
            groups.groups.value = listOf(futebol())

            // Then
            assertEquals(Route.SettleUp(CARLA_KEY, GroupCurrency.BRL), navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN Diego a guest WHEN Convidar Diego is tapped THEN opens his personal invite`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = DIEGO_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnInviteGuestClicked)

        // Then
        assertEquals(
            Route.ContactInvite(
                personKey = DIEGO_KEY,
                name = "Diego",
            ),
            navigator.backStack.value.last(),
        )
    }

    @Test
    fun `GIVEN Carla with an account WHEN the guest invite is tapped THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = CARLA_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnInviteGuestClicked)

        // Then
        assertEquals(Route.ContactProfile(CARLA_KEY), navigator.backStack.value.last())
    }

    @Test
    fun `GIVEN Diego and the internet WHEN Juntar is tapped THEN the sheet asks the server for his personal link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = DIEGO_KEY)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)

            // Then
            val sheet = assertNotNull(viewModel.uiState.value.merge)
            assertEquals(listOf(listOf(memberId("diego", FLORIPA))), merges.requestedMemberIds)
            assertEquals("merge-1", sheet.merge?.id)
            assertFalse(sheet.isRequesting)
            assertFalse(sheet.hasFailed)
        }

    @Test
    fun `GIVEN Diego offline WHEN Juntar is tapped THEN the sheet opens and waits for the internet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = DIEGO_KEY,
                online = false,
            )

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)

            // Then
            val sheet = assertNotNull(viewModel.uiState.value.merge)
            assertNull(sheet.merge)
            assertFalse(sheet.isRequesting)
            assertTrue(merges.requestedMemberIds.isEmpty())
        }

    @Test
    fun `GIVEN the sheet open offline WHEN the internet comes back THEN asks the server once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = DIEGO_KEY,
                online = false,
            )
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)

            // When
            isOnline.value = true

            // Then
            assertEquals(1, merges.requestedMemberIds.size)
            assertEquals(
                "merge-1",
                viewModel.uiState.value.merge
                    ?.merge
                    ?.id,
            )
        }

    @Test
    fun `GIVEN the server fails WHEN Juntar is tapped THEN the sheet shows the failure`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = DIEGO_KEY)
        merges.requestError = IllegalStateException("500")

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)

        // Then
        val sheet = assertNotNull(viewModel.uiState.value.merge)
        assertTrue(sheet.hasFailed)
        assertNull(sheet.merge)
        assertFalse(sheet.isRequesting)
    }

    @Test
    fun `GIVEN the request failed WHEN Tentar de novo is tapped THEN asks the server again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = DIEGO_KEY)
            merges.requestError = IllegalStateException("500")
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
            merges.requestError = null

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnMergeRetryClicked)

            // Then
            val sheet = assertNotNull(viewModel.uiState.value.merge)
            assertEquals(2, merges.requestedMemberIds.size)
            assertFalse(sheet.hasFailed)
            assertEquals("merge-2", sheet.merge?.id)
        }

    @Test
    fun `GIVEN the personal link made WHEN Copiar is tapped THEN copies it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = DIEGO_KEY)
        viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
        val link = assertNotNull(
            viewModel.uiState.value.merge
                ?.merge
                ?.link,
        )

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnMergeCopyClicked)

        // Then
        assertEquals(listOf<ContactProfileUiAction>(ContactProfileUiAction.CopyLink(link)), actions)
    }

    @Test
    fun `GIVEN the personal link made WHEN Compartilhar is tapped THEN shares it with his name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = DIEGO_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
            val link = assertNotNull(
                viewModel.uiState.value.merge
                    ?.merge
                    ?.link,
            )

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnMergeShareClicked)

            // Then
            assertEquals(listOf<ContactProfileUiAction>(ContactProfileUiAction.ShareLink(link, "Diego")), actions)
        }

    @Test
    fun `GIVEN the sheet still waiting for the link WHEN Copiar is tapped THEN copies nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = DIEGO_KEY,
                online = false,
            )
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnMergeCopyClicked)

            // Then
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the request made WHEN the sheet closes THEN the profile shows it waiting for Diego`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = DIEGO_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnMergeDismissed)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.merge)
            assertEquals("merge-1", state.pendingMerge.valueOrNull()?.id)
            assertTrue(state.showsPendingMerge)
            assertFalse(state.showsGuestCard)
        }

    @Test
    fun `GIVEN a request waiting for Diego WHEN his profile opens online THEN shows it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            personKey = DIEGO_KEY,
            storedMerge = waitingMerge,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(waitingMerge, state.pendingMerge.valueOrNull())
        assertTrue(state.showsPendingMerge)
        assertTrue(merges.requestedMemberIds.isEmpty())
    }

    @Test
    fun `GIVEN a request waiting for Diego WHEN his profile opens offline THEN does not ask the server`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = DIEGO_KEY,
                storedMerge = waitingMerge,
                online = false,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(merges.pendingLookups.isEmpty())
            assertTrue(state.pendingMerge.isLoading())
            assertTrue(state.isMergeStatusUnavailable)
            assertFalse(state.showsPendingMerge)
            assertFalse(state.canMerge)
        }

    @Test
    fun `GIVEN a request waiting for Diego WHEN Cancelar is tapped THEN cancels it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            personKey = DIEGO_KEY,
            storedMerge = waitingMerge,
        )

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnCancelMergeClicked)

        // Then
        assertEquals(listOf(waitingMerge.id), merges.cancelledIds)
        assertEquals(Loadable.Loaded(null), viewModel.uiState.value.pendingMerge)
        assertFalse(viewModel.uiState.value.isCancellingMerge)
        assertTrue(viewModel.uiState.value.showsGuestCard)
    }

    @Test
    fun `GIVEN the server fails to cancel WHEN Cancelar is tapped THEN says so and keeps the request`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = DIEGO_KEY,
                storedMerge = waitingMerge,
            )
            merges.cancelError = IllegalStateException("500")

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnCancelMergeClicked)

            // Then
            assertEquals(listOf<ContactProfileUiAction>(ContactProfileUiAction.ShowCancelMergeError), actions)
            assertEquals(
                waitingMerge,
                viewModel.uiState.value.pendingMerge
                    .valueOrNull(),
            )
            assertFalse(viewModel.uiState.value.isCancellingMerge)
        }

    @Test
    fun `GIVEN the sheet open WHEN Pedir para quem ja usa o app is tapped THEN says it is coming soon`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = DIEGO_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnAskAppUserClicked)

            // Then
            assertIs<ContactProfileUiAction.ShowComingSoon>(actions.single())
        }

    @Test
    fun `GIVEN Diego a guest WHEN his profile opens before the server answers THEN neither Juntar nor a request`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = DIEGO_KEY,
                storedMerge = waitingMerge,
                pendingAnswered = false,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.pendingMerge.isLoading())
            assertTrue(state.isMergeStatusLoading)
            assertFalse(state.showsPendingMerge)
            assertFalse(state.canMerge)
        }

    @Test
    fun `GIVEN the server failing to say if a request waits WHEN back online THEN asks again and shows it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = DIEGO_KEY,
                storedMerge = waitingMerge,
                findFails = true,
            )
            val failed = viewModel.uiState.value
            merges.findError = null
            isOnline.value = false

            // When
            isOnline.value = true

            // Then
            assertTrue(failed.isMergeStatusUnavailable)
            assertFalse(failed.showsPendingMerge)
            assertEquals(
                waitingMerge,
                viewModel.uiState.value.pendingMerge
                    .valueOrNull(),
            )
            assertTrue(viewModel.uiState.value.showsPendingMerge)
            assertFalse(viewModel.uiState.value.isMergeStatusUnavailable)
        }

    @Test
    fun `GIVEN Carla with an account WHEN Juntar is tapped THEN asks the server for nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)

            // Then
            assertNull(viewModel.uiState.value.merge)
            assertTrue(merges.requestedMemberIds.isEmpty())
            assertTrue(merges.pendingLookups.isEmpty())
        }

    // region Eu sou Diego

    @Test
    fun `GIVEN the merge sheet open WHEN Eu sou Diego is tapped THEN asks for confirmation`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = DIEGO_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeClicked)

            // Then
            assertTrue(assertNotNull(viewModel.uiState.value.merge).isConfirmingClaim)
            assertTrue(merges.claimedMemberIds.isEmpty())
        }

    @Test
    fun `GIVEN no merge sheet WHEN Eu sou Diego is tapped THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = DIEGO_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeClicked)

        // Then
        assertNull(viewModel.uiState.value.merge)
    }

    @Test
    fun `GIVEN the confirmation open WHEN it is dismissed THEN closes it and keeps the sheet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = DIEGO_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
            viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeClicked)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeDismissed)

            // Then
            assertFalse(assertNotNull(viewModel.uiState.value.merge).isConfirmingClaim)
            assertTrue(merges.claimedMemberIds.isEmpty())
        }

    @Test
    fun `GIVEN the confirmation open WHEN Merge is tapped THEN claims Diego and closes the sheet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = DIEGO_KEY,
                storedMerge = waitingMerge,
            )
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
            viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeClicked)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeConfirmed)

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf(DIEGO_KEY), claimedContacts.map { it.key })
            assertNull(state.merge)
            assertEquals(Loadable.Loaded(null), state.pendingMerge)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the server has not answered WHEN Merge is tapped THEN the claim runs and the question closes at once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = DIEGO_KEY,
                claimAnswered = false,
            )
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
            viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeClicked)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeConfirmed)

            // Then
            val sheet = assertNotNull(viewModel.uiState.value.merge)
            assertTrue(sheet.isClaiming)
            assertFalse(sheet.isConfirmingClaim)
        }

    @Test
    fun `GIVEN the server fails WHEN Merge is tapped THEN keeps the sheet open and shows the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = DIEGO_KEY,
                claimResult = Result.failure(IllegalStateException("500")),
            )
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
            viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeClicked)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeConfirmed)

            // Then
            val sheet = assertNotNull(viewModel.uiState.value.merge)
            assertFalse(sheet.isClaiming)
            assertFalse(sheet.isConfirmingClaim)
            assertEquals(listOf<ContactProfileUiAction>(ContactProfileUiAction.ShowClaimError), actions)
        }

    @Test
    fun `GIVEN no member moved WHEN Merge is tapped THEN keeps the sheet open and shows the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = DIEGO_KEY,
                storedMerge = waitingMerge,
                claimResult = Result.success(0),
            )
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
            viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeClicked)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeConfirmed)

            // Then
            val state = viewModel.uiState.value
            val sheet = assertNotNull(state.merge)
            assertFalse(sheet.isClaiming)
            assertEquals(waitingMerge, state.pendingMerge.valueOrNull())
            assertEquals(listOf<ContactProfileUiAction>(ContactProfileUiAction.ShowClaimError), actions)
        }

    @Test
    fun `GIVEN a claim running WHEN the question is dismissed THEN the claim goes on and nothing is sent`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = DIEGO_KEY,
                claimAnswered = false,
            )
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
            viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeClicked)
            viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeConfirmed)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeDismissed)

            // Then
            assertTrue(assertNotNull(viewModel.uiState.value.merge).isClaiming)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the sheet open without the confirmation WHEN it is dismissed THEN nothing changes or is sent`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = DIEGO_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
            val before = viewModel.uiState.value
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeDismissed)

            // Then
            assertEquals(before, viewModel.uiState.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a claim running WHEN Merge is tapped again THEN claims only once`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            personKey = DIEGO_KEY,
            claimAnswered = false,
        )
        viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
        viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeClicked)
        viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeConfirmed)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeConfirmed)

        // Then
        assertEquals(1, claimedContacts.size)
    }

    @Test
    fun `GIVEN the confirmation open and the internet gone WHEN Merge is tapped THEN claims nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = DIEGO_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
            viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeClicked)
            isOnline.value = false

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeConfirmed)

            // Then
            val sheet = assertNotNull(viewModel.uiState.value.merge)
            assertTrue(claimedContacts.isEmpty())
            assertFalse(sheet.isClaiming)
            assertTrue(sheet.isConfirmingClaim)
        }

    @Test
    fun `GIVEN the sheet open without the confirmation WHEN Merge is confirmed THEN claims nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = DIEGO_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeConfirmed)

            // Then
            assertTrue(claimedContacts.isEmpty())
        }

    @Test
    fun `GIVEN no merge sheet WHEN Merge is confirmed THEN claims nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = DIEGO_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeConfirmed)

        // Then
        assertTrue(claimedContacts.isEmpty())
    }

    @Test
    fun `GIVEN the claim events WHEN reading their analytics THEN each is sent by the ViewModel`() {
        // Given
        val expected = mapOf(
            ContactProfileUiEvent.OnClaimAsMeClicked to "contact_claim_clicked",
            ContactProfileUiEvent.OnClaimAsMeConfirmed to "contact_claim_confirmed",
            ContactProfileUiEvent.OnClaimAsMeDismissed to "contact_claim_cancelled",
        )

        // When
        val analytics = expected.keys.associateWith { it.analytics }

        // Then
        assertEquals(expected.mapValues { (_, name) -> EventAnalytics.Manual(name) }, analytics)
    }

    @Test
    fun `GIVEN the merge sheet open WHEN Eu sou Diego is tapped THEN sends the claim clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = DIEGO_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeClicked)

            // Then
            assertEquals(listOf("contact_claim_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN the confirmation open WHEN Merge is tapped THEN sends the claim confirmed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = DIEGO_KEY)
        viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
        viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeConfirmed)

        // Then
        assertEquals(listOf("contact_claim_confirmed" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN the confirmation open WHEN Cancel is tapped THEN sends the claim cancelled`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = DIEGO_KEY)
        viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
        viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnClaimAsMeDismissed)

        // Then
        assertEquals(listOf("contact_claim_cancelled" to emptyMap<String, Any>()), trackedEvents)
    }

    // endregion

    // region Analytics

    @Test
    fun `GIVEN the back and the closes WHEN reading their analytics THEN none is tracked`() {
        // Given
        val events = listOf(
            ContactProfileUiEvent.OnBackClicked,
            ContactProfileUiEvent.OnAddToGroupDismissed,
            ContactProfileUiEvent.OnMergeDismissed,
        )

        // When
        val analytics = events.map { it.analytics }

        // Then
        assertEquals(List(events.size) { EventAnalytics.NotTracked }, analytics)
    }

    @Test
    fun `GIVEN Pedir para quem ja usa o app WHEN reading its analytics THEN it is sent on its own`() {
        // Given
        val event = ContactProfileUiEvent.OnAskAppUserClicked

        // When
        val analytics = event.analytics

        // Then
        assertEquals(EventAnalytics.Automatic("contact_ask_app_user_clicked"), analytics)
    }

    @Test
    fun `GIVEN the taps the profile may refuse WHEN reading their analytics THEN the ViewModel sends each event`() {
        // Given
        val expected = mapOf(
            ContactProfileUiEvent.OnChargeClicked to "charge_clicked",
            ContactProfileUiEvent.OnReceivedClicked to "received_clicked",
            ContactProfileUiEvent.OnSettleClicked to "settle_clicked",
            ContactProfileUiEvent.OnGroupClicked(FLORIPA) to "group_clicked",
            ContactProfileUiEvent.OnAddToGroupClicked to "contact_add_to_group_clicked",
            ContactProfileUiEvent.OnAddGroupPicked(APE) to "contact_add_to_group_picked",
            ContactProfileUiEvent.OnAddToGroupConfirmed to "contact_add_to_group_confirmed",
            ContactProfileUiEvent.OnHideClicked to "contact_hide_clicked",
            ContactProfileUiEvent.OnHideConfirmed to "contact_hide_confirmed",
            ContactProfileUiEvent.OnHideDismissed to "contact_hide_cancelled",
            ContactProfileUiEvent.OnShowClicked to "contact_show_clicked",
            ContactProfileUiEvent.OnViewBalancesClicked to "contact_view_balances_clicked",
            ContactProfileUiEvent.OnSeeAllHistoryClicked to "contact_history_see_all_clicked",
            ContactProfileUiEvent.OnInviteGuestClicked to "contact_invite_guest_clicked",
            ContactProfileUiEvent.OnMergeClicked to "contact_merge_clicked",
            ContactProfileUiEvent.OnMergeRetryClicked to "retry_clicked",
            ContactProfileUiEvent.OnMergeCopyClicked to "invite_link_copy_clicked",
            ContactProfileUiEvent.OnMergeShareClicked to "invite_link_share_clicked",
            ContactProfileUiEvent.OnCancelMergeClicked to "contact_merge_cancel_clicked",
        )

        // When
        val analytics = expected.keys.associateWith { it.analytics }

        // Then
        assertEquals(expected.mapValues { (_, name) -> EventAnalytics.Manual(name) }, analytics)
    }

    @Test
    fun `GIVEN Carla owes you WHEN Cobrar is tapped THEN sends the charge clicked from her profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnChargeClicked)

            // Then
            assertEquals(listOf("charge_clicked" to profileSource), trackedEvents)
        }

    @Test
    fun `GIVEN Carla in the pane beside Contatos WHEN Cobrar is tapped THEN sends the charge clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = CARLA_KEY,
                inPane = true,
            )

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnChargeClicked)

            // Then
            assertEquals(listOf("charge_clicked" to profileSource), trackedEvents)
        }

    @Test
    fun `GIVEN you owe Bruno WHEN Cobrar is tapped THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = BRUNO_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnChargeClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Registrar pagamento already over the profile WHEN a late Cobrar arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnReceivedClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnChargeClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Carla owes you WHEN Recebi is tapped THEN sends the received clicked from her profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnReceivedClicked)

            // Then
            assertEquals(listOf("received_clicked" to profileSource), trackedEvents)
        }

    @Test
    fun `GIVEN the profile opened from her settling up WHEN Recebi goes back to it THEN sends the received clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = CARLA_KEY,
                openedFrom = Route.SettleUp(CARLA_KEY, GroupCurrency.BRL),
            )

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnReceivedClicked)

            // Then
            assertEquals(listOf("received_clicked" to profileSource), trackedEvents)
        }

    @Test
    fun `GIVEN you owe Bruno WHEN Recebi is tapped THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = BRUNO_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnReceivedClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Registrar pagamento already over the profile WHEN a late Recebi arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnReceivedClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnReceivedClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN you owe Bruno WHEN Acertar is tapped THEN sends the settle clicked from his profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = BRUNO_KEY)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnSettleClicked)

            // Then
            assertEquals(listOf("settle_clicked" to profileSource), trackedEvents)
        }

    @Test
    fun `GIVEN the profile opened from his settling up WHEN Acertar goes back to it THEN sends the settle clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = BRUNO_KEY,
                openedFrom = Route.SettleUp(BRUNO_KEY, GroupCurrency.BRL),
            )

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnSettleClicked)

            // Then
            assertEquals(listOf("settle_clicked" to profileSource), trackedEvents)
        }

    @Test
    fun `GIVEN Carla owes you WHEN Acertar is tapped THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = CARLA_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnSettleClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN her own profile WHEN a group in common is tapped THEN sends the group clicked from her profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnGroupClicked(APE))

            // Then
            assertEquals(listOf("group_clicked" to profileSource), trackedEvents)
        }

    @Test
    fun `GIVEN a group Carla is not in WHEN it is tapped THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = CARLA_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnGroupClicked(FUTEBOL))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the group already opened from the profile WHEN a second group tap arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnGroupClicked(FLORIPA))
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnGroupClicked(APE))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN her profile WHEN Adicionar a um grupo is tapped THEN sends the add to group clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = JULIA_KEY)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupClicked)

            // Then
            assertEquals(listOf("contact_add_to_group_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN Registrar pagamento over the profile WHEN a late Adicionar a um grupo arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnReceivedClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the groups to add Julia to WHEN another group is picked THEN sends the add to group picked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = JULIA_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnAddGroupPicked(APE))

            // Then
            assertEquals(listOf("contact_add_to_group_picked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN Viagem Floripa marked for Julia WHEN it is picked again THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = JULIA_KEY)
        viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnAddGroupPicked(FLORIPA))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the groups to add Julia to WHEN a group she is already in is picked THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = JULIA_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnAddGroupPicked(FUTEBOL))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the groups to add to closed WHEN a group is picked THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = JULIA_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnAddGroupPicked(APE))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a group marked for Julia WHEN Adicionar is tapped THEN sends the add confirmed as not a guest`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = JULIA_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupConfirmed)

            // Then
            assertEquals(
                listOf("contact_add_to_group_confirmed" to mapOf<String, Any>("is_guest" to false)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a group marked for Diego a guest WHEN Adicionar is tapped THEN sends the add confirmed as a guest`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = DIEGO_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupConfirmed)

            // Then
            assertEquals(
                listOf("contact_add_to_group_confirmed" to mapOf<String, Any>("is_guest" to true)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Carla already in every group WHEN Adicionar is tapped THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            personKey = CARLA_KEY,
            storedGroups = listOf(floripa(), ape()),
        )
        viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupConfirmed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the groups to add to closed WHEN Adicionar arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = JULIA_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupConfirmed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Julia settled WHEN Ocultar is tapped THEN sends the hide clicked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = JULIA_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnHideClicked)

        // Then
        assertEquals(listOf("contact_hide_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN the question already open WHEN Ocultar is tapped again THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = JULIA_KEY)
        viewModel.onEvent(ContactProfileUiEvent.OnHideClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnHideClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Carla owes you WHEN Ocultar is tapped THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = CARLA_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnHideClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the question open WHEN hiding is confirmed THEN sends the hide confirmed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = JULIA_KEY)
        viewModel.onEvent(ContactProfileUiEvent.OnHideClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnHideConfirmed)

        // Then
        assertEquals(listOf("contact_hide_confirmed" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN the question never asked WHEN hiding is confirmed THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = JULIA_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnHideConfirmed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the question open WHEN it is dismissed THEN sends the hide cancelled`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = JULIA_KEY)
        viewModel.onEvent(ContactProfileUiEvent.OnHideClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnHideDismissed)

        // Then
        assertEquals(listOf("contact_hide_cancelled" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN the question not open WHEN a dismiss arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = JULIA_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnHideDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Julia hidden WHEN Mostrar is tapped THEN sends the show clicked from her profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = JULIA_KEY,
                storedHidden = listOf(
                    HiddenContact(
                        JULIA_KEY,
                        isHidden = true,
                        hiddenAt = 10L,
                    ),
                ),
            )

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnShowClicked)

            // Then
            assertEquals(listOf("contact_show_clicked" to profileSource), trackedEvents)
        }

    @Test
    fun `GIVEN Carla not hidden WHEN Mostrar is tapped THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = CARLA_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnShowClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN her profile WHEN Ver em Saldos is tapped THEN sends the view balances clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnViewBalancesClicked)

            // Then
            assertEquals(listOf("contact_view_balances_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN Registrar pagamento over the profile WHEN a late Ver em Saldos arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnReceivedClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnViewBalancesClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the history WHEN Ver tudo is tapped THEN sends the history see all clicked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = CARLA_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnSeeAllHistoryClicked)

        // Then
        assertEquals(listOf("contact_history_see_all_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN Registrar pagamento over the profile WHEN a late Ver tudo arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnReceivedClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnSeeAllHistoryClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Diego a guest WHEN Convidar Diego is tapped THEN sends the invite guest clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = DIEGO_KEY)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnInviteGuestClicked)

            // Then
            assertEquals(listOf("contact_invite_guest_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN Carla with an account WHEN the guest invite is tapped THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = CARLA_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnInviteGuestClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN his invite already open WHEN a late Convidar Diego arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = DIEGO_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnInviteGuestClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnInviteGuestClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Diego a guest WHEN Juntar is tapped THEN sends the merge clicked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = DIEGO_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)

        // Then
        assertEquals(listOf("contact_merge_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN Carla with an account WHEN Juntar is tapped THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = CARLA_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN his invite over the profile WHEN a late Juntar arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = DIEGO_KEY)
        viewModel.onEvent(ContactProfileUiEvent.OnInviteGuestClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the request failed WHEN Tentar de novo is tapped THEN sends the retry clicked from the merge`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = DIEGO_KEY)
            merges.requestError = IllegalStateException("500")
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
            merges.requestError = null
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnMergeRetryClicked)

            // Then
            assertEquals(listOf("retry_clicked" to mergeSource), trackedEvents)
        }

    @Test
    fun `GIVEN the request failed and then the internet went away WHEN Tentar de novo is tapped THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = DIEGO_KEY)
            merges.requestError = IllegalStateException("500")
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
            merges.requestError = null
            isOnline.value = false
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnMergeRetryClicked)

            // Then
            assertEquals(1, merges.requestedMemberIds.size)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the personal link made WHEN Tentar de novo arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = DIEGO_KEY)
        viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnMergeRetryClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the personal link made WHEN Copiar is tapped THEN sends the link copy clicked from the merge`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = DIEGO_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnMergeCopyClicked)

            // Then
            assertEquals(listOf("invite_link_copy_clicked" to mergeSource), trackedEvents)
        }

    @Test
    fun `GIVEN the sheet still waiting for the link WHEN Copiar is tapped THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = DIEGO_KEY,
                online = false,
            )
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnMergeCopyClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the personal link made WHEN Compartilhar is tapped THEN sends the share clicked from the merge`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = DIEGO_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnMergeShareClicked)

            // Then
            assertEquals(listOf("invite_link_share_clicked" to mergeSource), trackedEvents)
        }

    @Test
    fun `GIVEN the sheet still waiting for the link WHEN Compartilhar is tapped THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = DIEGO_KEY,
                online = false,
            )
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnMergeShareClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a request waiting for Diego WHEN Cancelar is tapped THEN sends the merge cancel clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = DIEGO_KEY,
                storedMerge = waitingMerge,
            )

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnCancelMergeClicked)

            // Then
            assertEquals(listOf("contact_merge_cancel_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN the request already being cancelled WHEN Cancelar is tapped again THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = DIEGO_KEY,
                storedMerge = waitingMerge,
                cancelAnswered = false,
            )
            viewModel.onEvent(ContactProfileUiEvent.OnCancelMergeClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnCancelMergeClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN no request waiting for Diego WHEN Cancelar arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = DIEGO_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnCancelMergeClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the sheet open WHEN Pedir para quem ja usa o app is tapped twice THEN sends it on each tap`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = DIEGO_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
            trackedEvents.clear()
            val taps = List(2) { ContactProfileUiEvent.OnAskAppUserClicked }

            // When
            taps.forEach(viewModel::onEvent)

            // Then
            assertEquals(List(2) { "contact_ask_app_user_clicked" to emptyMap<String, Any>() }, trackedEvents)
        }

    @Test
    fun `GIVEN the merge sheet and the groups to add open WHEN both close and back is tapped THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = DIEGO_KEY)
            viewModel.onEvent(ContactProfileUiEvent.OnMergeClicked)
            viewModel.onEvent(ContactProfileUiEvent.OnAddToGroupClicked)
            trackedEvents.clear()
            val events = listOf(
                ContactProfileUiEvent.OnMergeDismissed,
                ContactProfileUiEvent.OnAddToGroupDismissed,
                ContactProfileUiEvent.OnBackClicked,
            )

            // When
            events.forEach(viewModel::onEvent)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    // endregion

    private val profileSource: Map<String, Any> = mapOf("source" to "contact_profile")

    private val mergeSource: Map<String, Any> = mapOf("source" to "contact_merge")

    private val floripaExpenses = Route.GroupDetail(
        FLORIPA,
        joinedAs = null,
        tab = GroupTab.EXPENSES,
        isOverBill = false,
    )

    private val waitingMerge = GuestMerge(
        id = "merge-9",
        memberIds = listOf(memberId("diego", FLORIPA)),
        link = "https://blitzsplit.com/g/Floripa1/diego1",
        requestedAt = 5L,
    )

    private fun TestScope.prepareScenario(
        personKey: String,
        inPane: Boolean = false,
        openedFrom: Route = Route.Contacts,
        storedGroups: List<Group> = defaultGroups(),
        bills: List<Expense> = defaultBills(),
        storedHidden: List<HiddenContact> = emptyList(),
        storedMerge: GuestMerge? = null,
        online: Boolean = true,
        addResult: Result<Unit> = Result.success(Unit),
        pendingAnswered: Boolean = true,
        findFails: Boolean = false,
        cancelAnswered: Boolean = true,
        claimResult: Result<Int> = Result.success(1),
        claimAnswered: Boolean = true,
    ) {
        claimedContacts.clear()
        groups = FakeGroupRepository(initialGroups = storedGroups)
        expenses = FakeExpenseRepository(initialExpenses = bills)
        hidden = FakeHiddenContactRepository(
            now = { 20L },
            initialHidden = storedHidden,
        )
        merges = FakeGuestMergeRepository(pending = storedMerge).also {
            if (findFails) it.findError = IllegalStateException("500")
        }
        isOnline = MutableStateFlow(online)
        val route = Route.ContactProfile(personKey)
        navigator = BackStackNavigator(startRoute = Route.Home).also {
            it.navigate(openedFrom)
            if (!inPane) it.navigate(route)
        }
        viewModel = ContactProfileViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            route = route,
            navigator = navigator,
            setContactHidden = SetContactHiddenUseCase(hidden),
            addContactToGroup = AddContactToGroup { contact: Contact, groupId: String ->
                addedToGroups += contact.key to groupId
                addResult
            },
            requestGuestMerge = RequestGuestMergeUseCase(merges),
            findPendingGuestMerge = if (pendingAnswered) {
                FindPendingGuestMergeUseCase(merges)
            } else {
                FindPendingGuestMerge { awaitCancellation() }
            },
            cancelGuestMerge = if (cancelAnswered) {
                CancelGuestMergeUseCase(merges)
            } else {
                CancelGuestMerge { awaitCancellation() }
            },
            claimGuestAsMe = ClaimGuestAsMe { contact ->
                claimedContacts += contact
                if (claimAnswered) claimResult else awaitCancellation()
            },
            observeContactBook = observeContactBookOf(groups, expenses, FakeSettlementRepository(), hidden),
            connectivityObserver = NetworkConnectivityObserver { isOnline },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
