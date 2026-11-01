package com.quare.blitzsplit.feature.contacts

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.domain.usecase.ObserveActivityUnreadCount
import com.quare.blitzsplit.core.contacts.domain.model.ContactBookSnapshot
import com.quare.blitzsplit.core.contacts.domain.model.ContactNet
import com.quare.blitzsplit.core.contacts.domain.model.HiddenContact
import com.quare.blitzsplit.core.contacts.domain.usecase.ObserveContactBook
import com.quare.blitzsplit.core.contacts.domain.usecase.impl.SetContactHiddenUseCase
import com.quare.blitzsplit.core.contacts.testing.FakeHiddenContactRepository
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsMoneyUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObservePeopleBalancesUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.feature.contacts.presentation.model.AgendaContact
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactsUiAction
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactsUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.NameMatch
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContactsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var hidden: FakeHiddenContactRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: ContactsViewModel
    private val activityUnread = MutableStateFlow(0)
    private val actions = mutableListOf<ContactsUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN people in the groups WHEN Contatos opens THEN lists them A to Z with their net`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf("Bruno", "Carla", "Diego", "Júlia"), state.contacts.map { it.name })
            val carla = state.contacts.single { it.key == CARLA_KEY }
            assertEquals(ContactNet(GroupCurrency.BRL, 5_000L), carla.net)
            assertEquals(2, carla.groupCount)
            assertEquals("Viagem Floripa", carla.firstGroupName)
            assertTrue(state.contacts.single { it.key == DIEGO_KEY }.isGuest)
            assertTrue(state.hiddenContacts.isEmpty())
        }

    @Test
    fun `GIVEN Julia hidden WHEN Contatos opens THEN lists her apart under the hidden ones`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedHidden = listOf(
                    HiddenContact(
                        JULIA_KEY,
                        isHidden = true,
                        hiddenAt = 10L,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf("Bruno", "Carla", "Diego"), state.contacts.map { it.name })
            assertEquals(listOf(JULIA_KEY), state.hiddenContacts.map { it.key })
            assertFalse(state.isHiddenExpanded)
            assertFalse(state.isEmpty)
        }

    @Test
    fun `GIVEN people you owe and who owe you WHEN Contatos opens THEN the summary has the totals of Saldos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val total = viewModel.uiState.value.total

            // Then
            assertEquals(GroupCurrency.BRL, total.currency)
            assertEquals(7_500L, total.netMinor)
            assertEquals(9_500L, total.toReceiveMinor)
            assertEquals(2_000L, total.toPayMinor)
            assertTrue(viewModel.uiState.value.showsSummary)
        }

    @Test
    fun `GIVEN no group WHEN Contatos opens THEN shows the empty state without the summary`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = emptyList(),
                bills = emptyList(),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isEmpty)
            assertFalse(state.isLoading)
            assertFalse(state.showsSummary)
            assertEquals(0L, state.total.netMinor)
        }

    @Test
    fun `GIVEN the device not read yet WHEN Contatos opens THEN shows it loading and never empty`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isBookRead = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoading)
            assertFalse(state.isEmpty)
            assertFalse(state.showsSummary)
        }

    @Test
    fun `GIVEN no internet WHEN Contatos opens THEN shows the offline notice with the people`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsOfflineNotice)
            assertEquals(4, state.contacts.size)
        }

    @Test
    fun `GIVEN a profile with a Google account WHEN Contatos opens THEN shows its name avatar and provider`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("Pierre", state.displayName)
            assertEquals("P", state.avatarInitial)
            assertEquals(AvatarColor.PEACH, state.avatarColor)
            assertEquals(AccountProvider.GOOGLE, state.accountProvider)
        }

    @Test
    fun `GIVEN Julia hidden WHEN the hidden section is toggled THEN it opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedHidden = listOf(
                HiddenContact(
                    JULIA_KEY,
                    isHidden = true,
                    hiddenAt = 10L,
                ),
            ),
        )

        // When
        viewModel.onEvent(ContactsUiEvent.OnHiddenToggled)

        // Then
        assertTrue(viewModel.uiState.value.isHiddenExpanded)
    }

    @Test
    fun `GIVEN nobody hidden WHEN the hidden section is toggled THEN it stays closed`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ContactsUiEvent.OnHiddenToggled)

        // Then
        assertFalse(viewModel.uiState.value.isHiddenExpanded)
    }

    @Test
    fun `GIVEN Julia hidden WHEN Mostrar is tapped on her THEN she is back in the list`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedHidden = listOf(
                HiddenContact(
                    JULIA_KEY,
                    isHidden = true,
                    hiddenAt = 10L,
                ),
            ),
        )

        // When
        viewModel.onEvent(ContactsUiEvent.OnShowContactClicked(JULIA_KEY))

        // Then
        assertFalse(
            hidden.hidden.value
                .single { it.personKey == JULIA_KEY }
                .isHidden,
        )
        assertEquals(
            listOf("Bruno", "Carla", "Diego", "Júlia"),
            viewModel.uiState.value.contacts
                .map { it.name },
        )
        assertTrue(
            viewModel.uiState.value.hiddenContacts
                .isEmpty(),
        )
    }

    @Test
    fun `GIVEN the hidden section open with only Julia WHEN she is shown again THEN the section closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedHidden = listOf(
                    HiddenContact(
                        JULIA_KEY,
                        isHidden = true,
                        hiddenAt = 10L,
                    ),
                ),
            )
            viewModel.onEvent(ContactsUiEvent.OnHiddenToggled)

            // When
            viewModel.onEvent(ContactsUiEvent.OnShowContactClicked(JULIA_KEY))

            // Then
            assertFalse(viewModel.uiState.value.isHiddenExpanded)
        }

    @Test
    fun `GIVEN the device fails to save WHEN Mostrar is tapped THEN says the change failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
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
            viewModel.onEvent(ContactsUiEvent.OnShowContactClicked(JULIA_KEY))

            // Then
            assertEquals(listOf<ContactsUiAction>(ContactsUiAction.ShowChangeError), actions)
            assertEquals(
                listOf(JULIA_KEY),
                viewModel.uiState.value.hiddenContacts
                    .map { it.key },
            )
        }

    @Test
    fun `GIVEN Carla not hidden WHEN Mostrar is tapped on her THEN changes nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ContactsUiEvent.OnShowContactClicked(CARLA_KEY))

        // Then
        assertTrue(hidden.hidden.value.isEmpty())
    }

    @Test
    fun `GIVEN the search open WHEN typing a name in another case without its accent THEN finds it and marks where`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnSearchOpened)

            // When
            viewModel.onEvent(ContactsUiEvent.OnQueryChanged("JULIA"))

            // Then
            val results = viewModel.uiState.value.groupResults
            assertEquals(listOf(JULIA_KEY), results.map { it.key })
            assertEquals(NameMatch(0, 5), results.single().match)
            assertTrue(viewModel.uiState.value.isSearching)
            assertFalse(viewModel.uiState.value.showsSummary)
        }

    @Test
    fun `GIVEN the search open WHEN typing the middle of a name THEN marks that part of it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnSearchOpened)

            // When
            viewModel.onEvent(ContactsUiEvent.OnQueryChanged("rla"))

            // Then
            val results = viewModel.uiState.value.groupResults
            assertEquals(listOf(CARLA_KEY), results.map { it.key })
            assertEquals(NameMatch(2, 5), results.single().match)
        }

    @Test
    fun `GIVEN Julia hidden WHEN searching her name THEN the search finds her too`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedHidden = listOf(
                HiddenContact(
                    JULIA_KEY,
                    isHidden = true,
                    hiddenAt = 10L,
                ),
            ),
        )
        viewModel.onEvent(ContactsUiEvent.OnSearchOpened)

        // When
        viewModel.onEvent(ContactsUiEvent.OnQueryChanged("júl"))

        // Then
        assertEquals(
            listOf(JULIA_KEY),
            viewModel.uiState.value.groupResults
                .map { it.key },
        )
    }

    @Test
    fun `GIVEN the agenda read WHEN searching THEN lists the agenda after the groups without the same name twice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnAgendaLoaded(agenda))
            viewModel.onEvent(ContactsUiEvent.OnSearchOpened)

            // When
            viewModel.onEvent(ContactsUiEvent.OnQueryChanged("carl"))

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf(CARLA_KEY), state.groupResults.map { it.key })
            assertEquals(listOf("Carlos Mendes"), state.agendaResults.map { it.contact.name })
            assertEquals(NameMatch(0, 4), state.agendaResults.single().match)
            assertEquals("CM", state.agendaResults.single().initials)
            assertFalse(state.showsNoResults)
        }

    @Test
    fun `GIVEN the search open WHEN typing a name nobody has THEN says nothing was found`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ContactsUiEvent.OnAgendaLoaded(agenda))
        viewModel.onEvent(ContactsUiEvent.OnSearchOpened)

        // When
        viewModel.onEvent(ContactsUiEvent.OnQueryChanged("Rodrigo"))

        // Then
        assertTrue(viewModel.uiState.value.showsNoResults)
    }

    @Test
    fun `GIVEN something typed in the search WHEN it closes THEN forgets what was typed`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ContactsUiEvent.OnSearchOpened)
        viewModel.onEvent(ContactsUiEvent.OnQueryChanged("carla"))

        // When
        viewModel.onEvent(ContactsUiEvent.OnSearchClosed)

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isSearchOpen)
        assertEquals("", state.query)
        assertFalse(state.isSearching)
        assertTrue(state.groupResults.isEmpty())
    }

    @Test
    fun `GIVEN no pane beside the list WHEN Carla is tapped THEN opens her profile over Contatos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ContactsUiEvent.OnContactClicked(CARLA_KEY))

            // Then
            assertEquals(
                listOf(Route.Home, Route.Contacts, Route.ContactProfile(CARLA_KEY)),
                navigator.backStack.value,
            )
            assertNull(viewModel.uiState.value.pickedKey)
        }

    @Test
    fun `GIVEN the pane beside the list WHEN Carla is tapped THEN picks her without navigating`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnDetailPaneChanged(isShown = true))

            // When
            viewModel.onEvent(ContactsUiEvent.OnContactClicked(CARLA_KEY))

            // Then
            assertEquals(CARLA_KEY, viewModel.uiState.value.pickedKey)
            assertEquals(CARLA_KEY, viewModel.uiState.value.selectedKey)
            assertFalse(viewModel.uiState.value.isAgendaPicked)
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the pane showing the agenda WHEN Carla is tapped THEN she takes its place`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ContactsUiEvent.OnDetailPaneChanged(isShown = true))
        viewModel.onEvent(ContactsUiEvent.OnAgendaClicked)

        // When
        viewModel.onEvent(ContactsUiEvent.OnContactClicked(CARLA_KEY))

        // Then
        assertEquals(CARLA_KEY, viewModel.uiState.value.pickedKey)
        assertFalse(viewModel.uiState.value.isAgendaPicked)
    }

    @Test
    fun `GIVEN Julia hidden and the pane beside the list WHEN she is tapped THEN picks her`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedHidden = listOf(
                    HiddenContact(
                        JULIA_KEY,
                        isHidden = true,
                        hiddenAt = 10L,
                    ),
                ),
            )
            viewModel.onEvent(ContactsUiEvent.OnDetailPaneChanged(isShown = true))

            // When
            viewModel.onEvent(ContactsUiEvent.OnContactClicked(JULIA_KEY))

            // Then
            assertEquals(JULIA_KEY, viewModel.uiState.value.selectedKey)
        }

    @Test
    fun `GIVEN Carla picked in the pane WHEN a sync takes her away THEN the pane shows nobody`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(ContactsUiEvent.OnContactClicked(CARLA_KEY))

            // When
            groups.groups.value = listOf(futebol())

            // Then
            assertNull(viewModel.uiState.value.selectedKey)
        }

    @Test
    fun `GIVEN her profile already open WHEN a late tap on Bruno arrives THEN stacks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnContactClicked(CARLA_KEY))

            // When
            viewModel.onEvent(ContactsUiEvent.OnContactClicked(BRUNO_KEY))

            // Then
            assertEquals(
                listOf(Route.Home, Route.Contacts, Route.ContactProfile(CARLA_KEY)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN someone not in Contatos WHEN their key is tapped THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ContactsUiEvent.OnContactClicked("user:nobody"))

        // Then
        assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
    }

    @Test
    fun `GIVEN no pane beside the list WHEN the agenda is tapped THEN opens it over Contatos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ContactsUiEvent.OnAgendaClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts, Route.ContactsAgenda), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Carla in the pane WHEN the agenda is tapped THEN the pane shows the agenda instead`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(ContactsUiEvent.OnContactClicked(CARLA_KEY))

            // When
            viewModel.onEvent(ContactsUiEvent.OnAgendaClicked)

            // Then
            assertTrue(viewModel.uiState.value.isAgendaPicked)
            assertNull(viewModel.uiState.value.pickedKey)
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Carla in the pane WHEN back is tapped in the pane THEN the pane shows nobody`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(ContactsUiEvent.OnContactClicked(CARLA_KEY))

            // When
            viewModel.onEvent(ContactsUiEvent.OnPaneBackClicked)

            // Then
            assertNull(viewModel.uiState.value.pickedKey)
            assertFalse(viewModel.uiState.value.isAgendaPicked)
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Contatos WHEN inviting with a link THEN opens the invite for nobody in particular`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ContactsUiEvent.OnInviteWithLinkClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.Contacts,
                    Route.ContactInvite(
                        personKey = null,
                        name = null,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Carlos found in the agenda WHEN Convidar is tapped on him THEN opens the invite with his name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnAgendaLoaded(agenda))

            // When
            viewModel.onEvent(ContactsUiEvent.OnAgendaContactInviteClicked("agenda-carlos"))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.Contacts,
                    Route.ContactInvite(
                        personKey = null,
                        name = "Carlos Mendes",
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN an agenda contact no longer read WHEN Convidar is tapped on it THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ContactsUiEvent.OnAgendaContactInviteClicked("agenda-carlos"))

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the summary WHEN Ver Saldos is tapped THEN Saldos takes the place of Contatos over Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ContactsUiEvent.OnBalancesCardClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL, charge = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN you owe Bruno WHEN Voce deve is tapped THEN opens Saldos on whom you owe`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ContactsUiEvent.OnYouOweClicked)

        // Then
        assertEquals(
            listOf(Route.Home, Route.GlobalBalances(BalancesFilter.YOU_OWE, charge = null)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN you owe nobody WHEN Voce deve is tapped THEN stays on Contatos`() = runTest(testDispatcher) {
        // Given
        prepareScenario(bills = listOf(owes(FLORIPA, memberId("carla", FLORIPA), meId(FLORIPA), 5_000L, "dinner")))

        // When
        viewModel.onEvent(ContactsUiEvent.OnYouOweClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Carla owes you WHEN Te devem is tapped THEN opens Saldos on who owes you`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ContactsUiEvent.OnOwedToYouClicked)

        // Then
        assertEquals(
            listOf(Route.Home, Route.GlobalBalances(BalancesFilter.OWES_YOU, charge = null)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN nobody owes you WHEN Te devem is tapped THEN stays on Contatos`() = runTest(testDispatcher) {
        // Given
        prepareScenario(bills = listOf(owes(FLORIPA, meId(FLORIPA), memberId("bruno", FLORIPA), 2_000L, "taxi")))

        // When
        viewModel.onEvent(ContactsUiEvent.OnOwedToYouClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Contatos WHEN the Inicio tab is tapped THEN goes back to Inicio`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ContactsUiEvent.OnHomeTabClicked)

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Contatos WHEN the Grupos tab is tapped THEN Grupos takes its place`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ContactsUiEvent.OnGroupsTabClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Contatos WHEN the Saldos tab is tapped THEN opens Saldos over Inicio`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ContactsUiEvent.OnBalancesTabClicked)

        // Then
        assertEquals(
            listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL, charge = null)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a profile over Contatos WHEN the Saldos tab is tapped THEN closes it and opens Saldos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnContactClicked(CARLA_KEY))

            // When
            viewModel.onEvent(ContactsUiEvent.OnBalancesTabClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL, charge = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a profile over Contatos WHEN the avatar is tapped THEN closes it and opens the user profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnContactClicked(CARLA_KEY))

            // When
            viewModel.onEvent(ContactsUiEvent.OnProfileClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Saldos in place of Contatos WHEN a late Contatos tab event arrives THEN changes nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnBalancesTabClicked)

            // When
            viewModel.onEvent(ContactsUiEvent.OnGroupsTabClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL, charge = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Contatos WHEN the Atividade tab is tapped THEN opens Atividade in its place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ContactsUiEvent.OnActivityTabClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Activity(personKey = null)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN unread events WHEN Contatos shows THEN its Atividade tab counts them`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        activityUnread.value = 4

        // Then
        assertEquals(4, viewModel.uiState.value.activityUnreadCount)
    }

    @Test
    fun `GIVEN Contatos WHEN the avatar is tapped THEN opens the profile`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ContactsUiEvent.OnProfileClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Contacts, Route.Profile), navigator.backStack.value)
    }

    private val agenda = listOf(
        AgendaContact(
            id = "agenda-carla",
            name = "Carla",
            phone = "(11) 91234-5678",
        ),
        AgendaContact(
            id = "agenda-carlos",
            name = "Carlos Mendes",
            phone = null,
        ),
        AgendaContact(
            id = "agenda-helena",
            name = "Helena Souza",
            phone = "(21) 99876-5432",
        ),
    )

    private fun TestScope.prepareScenario(
        storedGroups: List<Group> = defaultGroups(),
        bills: List<Expense> = defaultBills(),
        storedHidden: List<HiddenContact> = emptyList(),
        online: Boolean = true,
        isBookRead: Boolean = true,
    ) {
        groups = FakeGroupRepository(initialGroups = storedGroups)
        val expenses = FakeExpenseRepository(initialExpenses = bills)
        val settlements = FakeSettlementRepository()
        hidden = FakeHiddenContactRepository(
            now = { 20L },
            initialHidden = storedHidden,
        )
        navigator = BackStackNavigator(startRoute = Route.Home).also { it.navigate(Route.Contacts) }
        val roomNeverAnswers = MutableSharedFlow<ContactBookSnapshot>()
        viewModel = ContactsViewModel(
            navigator = navigator,
            setContactHidden = SetContactHiddenUseCase(hidden),
            observeContactBook = if (isBookRead) {
                observeContactBookOf(groups, expenses, settlements, hidden)
            } else {
                ObserveContactBook { roomNeverAnswers }
            },
            observePeopleBalances = ObservePeopleBalancesUseCase(
                observeGroups = ObserveGroupsUseCase(groups),
                observeGroupsMoney = ObserveGroupsMoneyUseCase(expenses, settlements),
            ),
            observeProfile = ObserveProfileUseCase(
                FakeProfileRepository(UserProfile("Pierre", AvatarColor.PEACH, null)),
            ),
            observeAccount = ObserveAccountUseCase(
                FakeAccountRepository(AccountStatus.Connected("pierre", AccountProvider.GOOGLE)),
            ),
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(online) },
            observeActivityUnreadCount = ObserveActivityUnreadCount { activityUnread },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
