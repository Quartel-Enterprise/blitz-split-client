package com.quare.blitzsplit.feature.contacts

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.contacts.domain.model.ContactBookSnapshot
import com.quare.blitzsplit.core.contacts.domain.model.ContactNet
import com.quare.blitzsplit.core.contacts.domain.model.HiddenContact
import com.quare.blitzsplit.core.contacts.domain.usecase.ObserveContactBook
import com.quare.blitzsplit.core.contacts.domain.usecase.impl.SetContactHiddenUseCase
import com.quare.blitzsplit.core.contacts.testing.FakeHiddenContactRepository
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveRecentGroupId
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsByActivityUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsMoneyUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObservePeopleBalancesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveRecentGroupIdUseCase
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
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactsUiAction
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactsUiEvent
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
import software.quare.loadable.isLoaded
import software.quare.loadable.isLoading
import software.quare.loadable.valueOrNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContactsViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var hidden: FakeHiddenContactRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: ContactsViewModel
    private lateinit var firstDownload: MutableStateFlow<FirstDownloadProgress?>
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
            val total = assertNotNull(
                viewModel.uiState.value.total
                    .valueOrNull(),
            )

            // Then
            assertEquals(GroupCurrency.BRL, total.currency)
            assertEquals(7_500L, total.netMinor)
            assertEquals(9_500L, total.toReceiveMinor)
            assertEquals(2_000L, total.toPayMinor)
            assertTrue(viewModel.uiState.value.showsSummary)
        }

    @Test
    fun `GIVEN the copy not whole yet WHEN Contatos opens with no group THEN a skeleton not the empty state`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = emptyList(),
                bills = emptyList(),
                downloading = FirstDownloadProgress(
                    arrivedGroupCount = 0,
                    groupCount = null,
                    isWaitingForInternet = true,
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isEmpty)
            assertTrue(state.isLoading)
            assertTrue(state.isWaitingForInternet)
            assertTrue(state.total.isLoading())
            assertFalse(state.showsSummary)
        }

    @Test
    fun `GIVEN someone hidden on another device WHEN the account copy becomes whole THEN only then lists the people`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                downloading = FirstDownloadProgress(
                    arrivedGroupCount = 1,
                    groupCount = 2,
                    isWaitingForInternet = false,
                ),
            )
            val downloading = viewModel.uiState.value

            // When
            firstDownload.value = null

            // Then
            assertTrue(downloading.contacts.isEmpty())
            assertTrue(downloading.isLoading)
            assertFalse(viewModel.uiState.value.isLoading)
            assertTrue(
                viewModel.uiState.value.contacts
                    .isNotEmpty(),
            )
            assertTrue(
                viewModel.uiState.value.total
                    .isLoaded(),
            )
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
            assertEquals(0L, state.total.valueOrNull()?.netMinor)
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
    fun `GIVEN the summary WHEN Ver Saldos is tapped THEN Saldos takes the place of Contatos over Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ContactsUiEvent.OnBalancesCardClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL)),
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
            listOf(Route.Home, Route.GlobalBalances(BalancesFilter.YOU_OWE)),
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
            listOf(Route.Home, Route.GlobalBalances(BalancesFilter.OWES_YOU)),
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
    fun `GIVEN Contatos WHEN the web logo is tapped THEN goes back to Inicio and sends brand_clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ContactsUiEvent.OnBrandClicked)

            // Then
            assertEquals(listOf(Route.Home), navigator.backStack.value)
            assertEquals(listOf("brand_clicked" to mapOf<String, Any>("source" to "contacts")), trackedEvents)
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
            listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL)),
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
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL)),
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
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL)),
                navigator.backStack.value,
            )
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

    @Test
    fun `GIVEN groups on the device WHEN Contatos opens THEN offers the search`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.canSearch)
    }

    @Test
    fun `GIVEN no group WHEN Contatos opens THEN offers no search`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = emptyList(),
            bills = emptyList(),
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.canSearch)
    }

    @Test
    fun `GIVEN the groups not read yet WHEN Contatos opens THEN offers no search`() = runTest(testDispatcher) {
        // Given
        prepareScenario(areGroupsRead = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.canSearch)
    }

    @Test
    fun `GIVEN only a settled group WHEN Contatos opens THEN offers the search`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(futebol()),
            bills = listOf(
                owes(FUTEBOL, memberId("julia", FUTEBOL), meId(FUTEBOL), 3_000L, id = "ball"),
                owes(FUTEBOL, meId(FUTEBOL), memberId("julia", FUTEBOL), 3_000L, id = "drinks"),
            ),
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.canSearch)
    }

    @Test
    fun `GIVEN no group WHEN a sync brings the first group THEN offers the search`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = emptyList(),
            bills = emptyList(),
        )

        // When
        groups.groups.value = listOf(ape())

        // Then
        assertTrue(viewModel.uiState.value.canSearch)
    }

    @Test
    fun `GIVEN a single group WHEN a sync takes it away THEN offers no search`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(ape()),
            bills = emptyList(),
        )

        // When
        groups.groups.value = emptyList()

        // Then
        assertFalse(viewModel.uiState.value.canSearch)
    }

    @Test
    fun `GIVEN Contatos on top WHEN the magnifier is tapped THEN opens the search over Contatos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ContactsUiEvent.OnSearchClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts, Route.Search), navigator.backStack.value)
        }

    @Test
    fun `GIVEN no group WHEN the magnifier is tapped THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = emptyList(),
            bills = emptyList(),
        )

        // When
        viewModel.onEvent(ContactsUiEvent.OnSearchClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the groups not read yet WHEN the magnifier is tapped THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(areGroupsRead = false)

        // When
        viewModel.onEvent(ContactsUiEvent.OnSearchClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the search already open WHEN a late tap on the magnifier arrives THEN stacks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnSearchClicked)

            // When
            viewModel.onEvent(ContactsUiEvent.OnSearchClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts, Route.Search), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Carla profile over Contatos WHEN the magnifier is tapped THEN stacks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnContactClicked(CARLA_KEY))

            // When
            viewModel.onEvent(ContactsUiEvent.OnSearchClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Contacts, Route.ContactProfile(CARLA_KEY)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the agenda over Contatos WHEN the magnifier is tapped THEN stacks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ContactsUiEvent.OnAgendaClicked)

        // When
        viewModel.onEvent(ContactsUiEvent.OnSearchClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Contacts, Route.ContactsAgenda), navigator.backStack.value)
    }

    @Test
    fun `GIVEN groups on the device WHEN Contatos opens THEN offers Rachar conta`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.showsSplitBillButton)
    }

    @Test
    fun `GIVEN no group WHEN Contatos opens THEN offers no Rachar conta`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = emptyList(),
            bills = emptyList(),
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.showsSplitBillButton)
    }

    @Test
    fun `GIVEN the groups not read yet WHEN Contatos opens THEN offers no Rachar conta`() = runTest(testDispatcher) {
        // Given
        prepareScenario(areGroupsRead = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.showsSplitBillButton)
    }

    @Test
    fun `GIVEN no group WHEN a sync brings the first group THEN offers Rachar conta`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = emptyList(),
            bills = emptyList(),
        )

        // When
        groups.groups.value = listOf(ape())

        // Then
        assertTrue(viewModel.uiState.value.showsSplitBillButton)
    }

    @Test
    fun `GIVEN a single group WHEN a sync takes it away THEN offers no Rachar conta`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(ape()),
            bills = emptyList(),
        )

        // When
        groups.groups.value = emptyList()

        // Then
        assertFalse(viewModel.uiState.value.showsSplitBillButton)
    }

    @Test
    fun `GIVEN Contatos on top WHEN Rachar conta is tapped THEN asks the group first with Viagem Floripa marked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ContactsUiEvent.OnSplitBillClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Contacts, splitBillIn(FLORIPA)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a bill just saved in Futebol WHEN Rachar conta is tapped THEN marks Futebol`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                bills = defaultBills() +
                    owes(
                        FUTEBOL,
                        memberId("julia", FUTEBOL),
                        meId(FUTEBOL),
                        3_000L,
                        id = "ball",
                        updatedAt = 9_000L,
                    ),
            )

            // When
            viewModel.onEvent(ContactsUiEvent.OnSplitBillClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Contacts, splitBillIn(FUTEBOL)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a sync made Futebol the most recently used WHEN Rachar conta is tapped THEN marks Futebol`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            groups.groups.value = listOf(floripa(), ape(), futebol().copy(updatedAt = 9_000L))

            // When
            viewModel.onEvent(ContactsUiEvent.OnSplitBillClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Contacts, splitBillIn(FUTEBOL)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN no group WHEN a sync brings the first group and Rachar conta is tapped THEN marks that group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = emptyList(),
                bills = emptyList(),
            )
            groups.groups.value = listOf(ape())

            // When
            viewModel.onEvent(ContactsUiEvent.OnSplitBillClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Contacts, splitBillIn(APE)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Carla profile over Contatos WHEN Rachar conta is tapped THEN opens it over her profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnContactClicked(CARLA_KEY))

            // When
            viewModel.onEvent(ContactsUiEvent.OnSplitBillClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Contacts, Route.ContactProfile(CARLA_KEY), splitBillIn(FLORIPA)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Carla in the pane beside the list WHEN Rachar conta is tapped THEN opens it over Contatos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(ContactsUiEvent.OnContactClicked(CARLA_KEY))

            // When
            viewModel.onEvent(ContactsUiEvent.OnSplitBillClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Contacts, splitBillIn(FLORIPA)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN no group WHEN Rachar conta is tapped THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = emptyList(),
            bills = emptyList(),
        )

        // When
        viewModel.onEvent(ContactsUiEvent.OnSplitBillClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the groups not read yet WHEN Rachar conta is tapped THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(areGroupsRead = false)

        // When
        viewModel.onEvent(ContactsUiEvent.OnSplitBillClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a sync took the only group away WHEN Rachar conta is tapped THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(ape()),
                bills = emptyList(),
            )
            groups.groups.value = emptyList()

            // When
            viewModel.onEvent(ContactsUiEvent.OnSplitBillClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Saldos in place of Contatos WHEN a late Rachar conta tap arrives THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnBalancesTabClicked)

            // When
            viewModel.onEvent(ContactsUiEvent.OnSplitBillClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Grupos in place of Contatos WHEN a late Rachar conta tap arrives THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnGroupsTabClicked)

            // When
            viewModel.onEvent(ContactsUiEvent.OnSplitBillClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
        }

    // region Analytics

    @Test
    fun `GIVEN the events nobody taps to act WHEN reading their analytics THEN none is tracked`() {
        // Given
        val events = listOf(
            ContactsUiEvent.OnDetailPaneChanged(isShown = true),
            ContactsUiEvent.OnPaneBackClicked,
            ContactsUiEvent.OnHomeTabClicked,
            ContactsUiEvent.OnGroupsTabClicked,
            ContactsUiEvent.OnBalancesTabClicked,
        )

        // When
        val analytics = events.map { it.analytics }

        // Then
        assertEquals(List(events.size) { EventAnalytics.NotTracked }, analytics)
    }

    @Test
    fun `GIVEN the taps Contatos may refuse WHEN reading their analytics THEN the ViewModel sends each event`() {
        // Given
        val expected = mapOf(
            ContactsUiEvent.OnContactClicked(CARLA_KEY) to "contact_clicked",
            ContactsUiEvent.OnSearchClicked to "search_opened",
            ContactsUiEvent.OnHiddenToggled to "contacts_hidden_toggled",
            ContactsUiEvent.OnShowContactClicked(JULIA_KEY) to "contact_show_clicked",
            ContactsUiEvent.OnBalancesCardClicked to "contacts_balances_clicked",
            ContactsUiEvent.OnYouOweClicked to "contacts_balances_clicked",
            ContactsUiEvent.OnOwedToYouClicked to "contacts_balances_clicked",
            ContactsUiEvent.OnAgendaClicked to "contacts_agenda_clicked",
            ContactsUiEvent.OnInviteWithLinkClicked to "contacts_invite_with_link_clicked",
            ContactsUiEvent.OnProfileClicked to "profile_avatar_clicked",
        )

        // When
        val analytics = expected.keys.associateWith { it.analytics }

        // Then
        assertEquals(expected.mapValues { (_, name) -> EventAnalytics.Manual(name) }, analytics)
    }

    @Test
    fun `GIVEN Carla listed WHEN she is tapped THEN sends the contact clicked as not hidden`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ContactsUiEvent.OnContactClicked(CARLA_KEY))

            // Then
            assertEquals(listOf("contact_clicked" to mapOf<String, Any>("is_hidden" to false)), trackedEvents)
        }

    @Test
    fun `GIVEN Carla listed and the pane beside the list WHEN she is tapped THEN sends the contact clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnDetailPaneChanged(isShown = true))

            // When
            viewModel.onEvent(ContactsUiEvent.OnContactClicked(CARLA_KEY))

            // Then
            assertEquals(listOf("contact_clicked" to mapOf<String, Any>("is_hidden" to false)), trackedEvents)
        }

    @Test
    fun `GIVEN Julia under the hidden ones WHEN she is tapped THEN sends the contact clicked as hidden`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedHidden = juliaHidden)

            // When
            viewModel.onEvent(ContactsUiEvent.OnContactClicked(JULIA_KEY))

            // Then
            assertEquals(listOf("contact_clicked" to mapOf<String, Any>("is_hidden" to true)), trackedEvents)
        }

    @Test
    fun `GIVEN someone not in Contatos WHEN their key is tapped THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ContactsUiEvent.OnContactClicked("user:nobody"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN her profile already open over Contatos WHEN a late tap on Bruno arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnContactClicked(CARLA_KEY))
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactsUiEvent.OnContactClicked(BRUNO_KEY))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN groups and Contatos on top WHEN the magnifier is tapped THEN sends the search opened from contacts`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ContactsUiEvent.OnSearchClicked)

            // Then
            assertEquals(listOf("search_opened" to contactsSource), trackedEvents)
        }

    @Test
    fun `GIVEN no group WHEN the magnifier is tapped THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = emptyList(),
            bills = emptyList(),
        )

        // When
        viewModel.onEvent(ContactsUiEvent.OnSearchClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Carla profile over Contatos WHEN the magnifier is tapped THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnContactClicked(CARLA_KEY))
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactsUiEvent.OnSearchClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Julia hidden WHEN the hidden section is opened THEN sends it as expanded`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedHidden = juliaHidden)

        // When
        viewModel.onEvent(ContactsUiEvent.OnHiddenToggled)

        // Then
        assertEquals(listOf("contacts_hidden_toggled" to mapOf<String, Any>("is_expanded" to true)), trackedEvents)
    }

    @Test
    fun `GIVEN the hidden section open WHEN it is closed THEN sends it as not expanded`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedHidden = juliaHidden)
        viewModel.onEvent(ContactsUiEvent.OnHiddenToggled)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ContactsUiEvent.OnHiddenToggled)

        // Then
        assertEquals(listOf("contacts_hidden_toggled" to mapOf<String, Any>("is_expanded" to false)), trackedEvents)
    }

    @Test
    fun `GIVEN nobody hidden WHEN the hidden section is toggled THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ContactsUiEvent.OnHiddenToggled)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Julia hidden WHEN Mostrar is tapped on her THEN sends the show clicked from contacts`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedHidden = juliaHidden)

            // When
            viewModel.onEvent(ContactsUiEvent.OnShowContactClicked(JULIA_KEY))

            // Then
            assertEquals(listOf("contact_show_clicked" to contactsSource), trackedEvents)
        }

    @Test
    fun `GIVEN Carla not hidden WHEN Mostrar is tapped on her THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ContactsUiEvent.OnShowContactClicked(CARLA_KEY))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the summary WHEN Ver Saldos is tapped THEN sends the balances clicked on all`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ContactsUiEvent.OnBalancesCardClicked)

            // Then
            assertEquals(
                listOf("contacts_balances_clicked" to mapOf<String, Any>("filter" to BalancesFilter.ALL)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN you owe Bruno WHEN Voce deve is tapped THEN sends the balances clicked on whom you owe`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ContactsUiEvent.OnYouOweClicked)

            // Then
            assertEquals(
                listOf("contacts_balances_clicked" to mapOf<String, Any>("filter" to BalancesFilter.YOU_OWE)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Carla owes you WHEN Te devem is tapped THEN sends the balances clicked on who owes you`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ContactsUiEvent.OnOwedToYouClicked)

            // Then
            assertEquals(
                listOf("contacts_balances_clicked" to mapOf<String, Any>("filter" to BalancesFilter.OWES_YOU)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN you owe nobody WHEN Voce deve is tapped THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(bills = listOf(owes(FLORIPA, memberId("carla", FLORIPA), meId(FLORIPA), 5_000L, "dinner")))

        // When
        viewModel.onEvent(ContactsUiEvent.OnYouOweClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN nobody owes you WHEN Te devem is tapped THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(bills = listOf(owes(FLORIPA, meId(FLORIPA), memberId("bruno", FLORIPA), 2_000L, "taxi")))

        // When
        viewModel.onEvent(ContactsUiEvent.OnOwedToYouClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Carla profile over Contatos WHEN Ver Saldos is tapped THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ContactsUiEvent.OnContactClicked(CARLA_KEY))
        trackedEvents.clear()

        // When
        viewModel.onEvent(ContactsUiEvent.OnBalancesCardClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Contatos on top WHEN Convidar da agenda is tapped THEN sends the agenda clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ContactsUiEvent.OnAgendaClicked)

            // Then
            assertEquals(listOf("contacts_agenda_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN the pane beside the list WHEN Convidar da agenda is tapped THEN sends the agenda clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnDetailPaneChanged(isShown = true))

            // When
            viewModel.onEvent(ContactsUiEvent.OnAgendaClicked)

            // Then
            assertEquals(listOf("contacts_agenda_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN the agenda already over Contatos WHEN a late agenda tap arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnAgendaClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactsUiEvent.OnAgendaClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Contatos on top WHEN Convidar com link is tapped THEN sends the invite with link from contacts`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ContactsUiEvent.OnInviteWithLinkClicked)

            // Then
            assertEquals(listOf("contacts_invite_with_link_clicked" to contactsSource), trackedEvents)
        }

    @Test
    fun `GIVEN Carla profile over Contatos WHEN Convidar com link is tapped THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnContactClicked(CARLA_KEY))
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactsUiEvent.OnInviteWithLinkClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Contatos WHEN the avatar is tapped THEN sends the profile avatar clicked from contacts`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ContactsUiEvent.OnProfileClicked)

            // Then
            assertEquals(listOf("profile_avatar_clicked" to contactsSource), trackedEvents)
        }

    @Test
    fun `GIVEN a profile over the Contatos tab WHEN the avatar is tapped THEN still sends the avatar clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnContactClicked(CARLA_KEY))
            trackedEvents.clear()

            // When
            viewModel.onEvent(ContactsUiEvent.OnProfileClicked)

            // Then
            assertEquals(listOf("profile_avatar_clicked" to contactsSource), trackedEvents)
        }

    @Test
    fun `GIVEN Saldos in place of Contatos WHEN a late avatar tap arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnBalancesTabClicked)

            // When
            viewModel.onEvent(ContactsUiEvent.OnProfileClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Contatos WHEN the pane shows and closes and a tab is tapped THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val events = listOf(
                ContactsUiEvent.OnDetailPaneChanged(isShown = true),
                ContactsUiEvent.OnPaneBackClicked,
                ContactsUiEvent.OnBalancesTabClicked,
            )

            // When
            events.forEach(viewModel::onEvent)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Rachar conta WHEN reading its analytics THEN only the ViewModel sends it`() {
        // Given
        val event = ContactsUiEvent.OnSplitBillClicked

        // When
        val analytics = event.analytics

        // Then
        assertEquals(EventAnalytics.Manual("split_bill_clicked"), analytics)
    }

    @Test
    fun `GIVEN groups and Contatos on top WHEN Rachar conta is tapped THEN sends split_bill_clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ContactsUiEvent.OnSplitBillClicked)

            // Then
            assertEquals(listOf("split_bill_clicked" to contactsSource), trackedEvents)
        }

    @Test
    fun `GIVEN Rachar conta already open WHEN a second tap arrives THEN opens and sends nothing more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactsUiEvent.OnSplitBillClicked)

            // When
            viewModel.onEvent(ContactsUiEvent.OnSplitBillClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts, splitBillIn(FLORIPA)), navigator.backStack.value)
            assertEquals(listOf("split_bill_clicked" to contactsSource), trackedEvents)
        }

    @Test
    fun `GIVEN no group WHEN Rachar conta is tapped THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = emptyList())

        // When
        viewModel.onEvent(ContactsUiEvent.OnSplitBillClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    // endregion

    private val contactsSource: Map<String, Any> = mapOf("source" to "contacts")

    private val juliaHidden = listOf(
        HiddenContact(
            JULIA_KEY,
            isHidden = true,
            hiddenAt = 10L,
        ),
    )

    private fun splitBillIn(groupId: String): Route.SplitBill = Route.SplitBill(
        groupId = groupId,
        expenseId = null,
        picksGroupFirst = true,
        scansReceipt = false,
    )

    private fun TestScope.prepareScenario(
        storedGroups: List<Group> = defaultGroups(),
        bills: List<Expense> = defaultBills(),
        storedHidden: List<HiddenContact> = emptyList(),
        online: Boolean = true,
        isBookRead: Boolean = true,
        areGroupsRead: Boolean = true,
        downloading: FirstDownloadProgress? = null,
    ) {
        groups = FakeGroupRepository(initialGroups = storedGroups)
        val expenses = FakeExpenseRepository(initialExpenses = bills)
        val settlements = FakeSettlementRepository()
        hidden = FakeHiddenContactRepository(
            now = { 20L },
            initialHidden = storedHidden,
        )
        navigator = BackStackNavigator(startRoute = Route.Home).also { it.navigate(Route.Contacts) }
        firstDownload = MutableStateFlow(downloading)
        val roomNeverAnswers = MutableSharedFlow<ContactBookSnapshot>()
        viewModel = ContactsViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
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
            observeFirstDownload = ObserveFirstDownload { firstDownload },
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(online) },
            observeRecentGroupId = if (areGroupsRead) {
                ObserveRecentGroupIdUseCase(
                    ObserveGroupsByActivityUseCase(
                        ObserveGroupsUseCase(groups),
                        ObserveGroupsMoneyUseCase(expenses, settlements),
                    ),
                )
            } else {
                ObserveRecentGroupId { MutableSharedFlow() }
            },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
