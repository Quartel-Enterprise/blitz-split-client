package com.quare.blitzsplit.feature.contacts

import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.contacts.domain.model.GuestMerge
import com.quare.blitzsplit.core.contacts.domain.model.HiddenContact
import com.quare.blitzsplit.core.contacts.domain.usecase.AddContactToGroup
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
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.BalancesCharge
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.contacts.presentation.model.AddGroupOption
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiAction
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContactProfileViewModelTest {
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
    fun `GIVEN the history WHEN Ver tudo is tapped THEN says it is coming soon`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = CARLA_KEY)

        // When
        viewModel.onEvent(ContactProfileUiEvent.OnSeeAllHistoryClicked)

        // Then
        assertEquals(listOf<ContactProfileUiAction>(ContactProfileUiAction.ShowComingSoon), actions)
    }

    @Test
    fun `GIVEN Carla owes you WHEN Cobrar is tapped THEN opens the message of Saldos for her`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnChargeClicked)

            // Then
            assertEquals(
                Route.GlobalBalances(BalancesFilter.OWES_YOU, charge = BalancesCharge(CARLA_KEY, GroupCurrency.BRL)),
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
                    Route.Groups,
                    Route.Contacts,
                    Route.GlobalBalances(
                        BalancesFilter.OWES_YOU,
                        charge = BalancesCharge(CARLA_KEY, GroupCurrency.BRL),
                    ),
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
    fun `GIVEN her profile WHEN Ver em Saldos is tapped THEN opens the Saldos tab over the groups`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = CARLA_KEY)

            // When
            viewModel.onEvent(ContactProfileUiEvent.OnViewBalancesClicked)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.GlobalBalances(BalancesFilter.ALL, charge = null)),
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
        assertEquals(listOf(Route.Groups, Route.Contacts), navigator.backStack.value)
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
            assertEquals(listOf(Route.Groups, Route.Contacts), navigator.backStack.value)
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
            assertEquals(listOf(Route.Groups, Route.Contacts), navigator.backStack.value)
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
            assertEquals(listOf(Route.Groups, Route.Contacts), navigator.backStack.value)
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
            assertEquals(listOf(Route.Groups, Route.Contacts), navigator.backStack.value)
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
            assertEquals("merge-1", state.pendingMerge?.id)
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
        assertEquals(waitingMerge, state.pendingMerge)
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
            assertNull(state.pendingMerge)
            assertTrue(state.showsGuestCard)
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
        assertNull(viewModel.uiState.value.pendingMerge)
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
            assertEquals(waitingMerge, viewModel.uiState.value.pendingMerge)
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

    private val waitingMerge = GuestMerge(
        id = "merge-9",
        memberIds = listOf(memberId("diego", FLORIPA)),
        link = "https://blitzsplit.com/g/Floripa1/diego1",
        requestedAt = 5L,
    )

    private fun TestScope.prepareScenario(
        personKey: String,
        inPane: Boolean = false,
        storedGroups: List<Group> = defaultGroups(),
        bills: List<Expense> = defaultBills(),
        storedHidden: List<HiddenContact> = emptyList(),
        storedMerge: GuestMerge? = null,
        online: Boolean = true,
        addResult: Result<Unit> = Result.success(Unit),
    ) {
        groups = FakeGroupRepository(initialGroups = storedGroups)
        expenses = FakeExpenseRepository(initialExpenses = bills)
        hidden = FakeHiddenContactRepository(
            now = { 20L },
            initialHidden = storedHidden,
        )
        merges = FakeGuestMergeRepository(pending = storedMerge)
        isOnline = MutableStateFlow(online)
        val route = Route.ContactProfile(personKey)
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.Contacts)
            if (!inPane) it.navigate(route)
        }
        viewModel = ContactProfileViewModel(
            route = route,
            navigator = navigator,
            setContactHidden = SetContactHiddenUseCase(hidden),
            addContactToGroup = AddContactToGroup { contact: Contact, groupId: String ->
                addedToGroups += contact.key to groupId
                addResult
            },
            requestGuestMerge = RequestGuestMergeUseCase(merges),
            findPendingGuestMerge = FindPendingGuestMergeUseCase(merges),
            cancelGuestMerge = CancelGuestMergeUseCase(merges),
            observeContactBook = observeContactBookOf(groups, expenses, FakeSettlementRepository(), hidden),
            connectivityObserver = NetworkConnectivityObserver { isOnline },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
