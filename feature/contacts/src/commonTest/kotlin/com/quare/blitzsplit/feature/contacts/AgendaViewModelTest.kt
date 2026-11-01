package com.quare.blitzsplit.feature.contacts

import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.contacts.presentation.model.AgendaAccess
import com.quare.blitzsplit.feature.contacts.presentation.model.AgendaContact
import com.quare.blitzsplit.feature.contacts.presentation.model.AgendaUiAction
import com.quare.blitzsplit.feature.contacts.presentation.model.AgendaUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.NameMatch
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.AgendaViewModel
import kotlinx.coroutines.Dispatchers
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgendaViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: AgendaViewModel
    private val actions = mutableListOf<AgendaUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN the access not read yet WHEN the agenda opens THEN shows neither the ask nor the list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.showsAskAccess)
            assertFalse(state.showsDenied)
            assertFalse(state.showsContacts)
        }

    @Test
    fun `GIVEN the agenda never asked WHEN the access is read THEN explains before asking`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(AgendaUiEvent.OnAccessChanged(AgendaAccess.NOT_ASKED))

        // Then
        assertTrue(viewModel.uiState.value.showsAskAccess)
        assertFalse(viewModel.uiState.value.showsContacts)
    }

    @Test
    fun `GIVEN the agenda refused WHEN the access is read THEN leads to the settings`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(AgendaUiEvent.OnAccessChanged(AgendaAccess.DENIED))

        // Then
        assertTrue(viewModel.uiState.value.showsDenied)
        assertFalse(viewModel.uiState.value.showsAskAccess)
    }

    @Test
    fun `GIVEN the access granted WHEN the contacts are read THEN lists all of them`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(AgendaUiEvent.OnAccessChanged(AgendaAccess.GRANTED))

        // When
        viewModel.onEvent(AgendaUiEvent.OnContactsLoaded(agenda))

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.showsContacts)
        assertEquals(agenda, state.results.map { it.contact })
        assertEquals(listOf("CM", "HS", "JP"), state.results.map { it.initials })
    }

    @Test
    fun `GIVEN a limited access WHEN the contacts picked are read THEN lists them`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(AgendaUiEvent.OnAccessChanged(AgendaAccess.LIMITED))

        // When
        viewModel.onEvent(AgendaUiEvent.OnContactsLoaded(agenda.take(1)))

        // Then
        assertTrue(viewModel.uiState.value.showsContacts)
        assertEquals(
            listOf("Carlos Mendes"),
            viewModel.uiState.value.results
                .map { it.contact.name },
        )
    }

    @Test
    fun `GIVEN the agenda never asked WHEN contacts arrive THEN ignores them`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(AgendaUiEvent.OnAccessChanged(AgendaAccess.NOT_ASKED))

        // When
        viewModel.onEvent(AgendaUiEvent.OnContactsLoaded(agenda))

        // Then
        assertTrue(
            viewModel.uiState.value.contacts
                .isEmpty(),
        )
    }

    @Test
    fun `GIVEN the contacts listed WHEN the access is refused in the settings THEN drops them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(AgendaUiEvent.OnAccessChanged(AgendaAccess.GRANTED))
            viewModel.onEvent(AgendaUiEvent.OnContactsLoaded(agenda))

            // When
            viewModel.onEvent(AgendaUiEvent.OnAccessChanged(AgendaAccess.DENIED))

            // Then
            assertTrue(
                viewModel.uiState.value.contacts
                    .isEmpty(),
            )
            assertTrue(viewModel.uiState.value.showsDenied)
        }

    @Test
    fun `GIVEN the contacts listed WHEN a limited access replaces the full one THEN keeps them until read again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(AgendaUiEvent.OnAccessChanged(AgendaAccess.GRANTED))
            viewModel.onEvent(AgendaUiEvent.OnContactsLoaded(agenda))

            // When
            viewModel.onEvent(AgendaUiEvent.OnAccessChanged(AgendaAccess.LIMITED))

            // Then
            assertEquals(agenda, viewModel.uiState.value.contacts)
        }

    @Test
    fun `GIVEN the contacts listed WHEN typing a name without its accent THEN finds it and marks where`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(AgendaUiEvent.OnAccessChanged(AgendaAccess.GRANTED))
            viewModel.onEvent(AgendaUiEvent.OnContactsLoaded(agenda))

            // When
            viewModel.onEvent(AgendaUiEvent.OnQueryChanged("PRADO"))

            // Then
            val result = viewModel.uiState.value.results
                .single()
            assertEquals("Júlia Prado", result.contact.name)
            assertEquals(NameMatch(6, 11), result.match)
            assertFalse(viewModel.uiState.value.showsNoResults)
        }

    @Test
    fun `GIVEN the contacts listed WHEN typing a name nobody has THEN says nothing was found`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(AgendaUiEvent.OnAccessChanged(AgendaAccess.GRANTED))
            viewModel.onEvent(AgendaUiEvent.OnContactsLoaded(agenda))

            // When
            viewModel.onEvent(AgendaUiEvent.OnQueryChanged("Rodrigo"))

            // Then
            assertTrue(
                viewModel.uiState.value.results
                    .isEmpty(),
            )
            assertTrue(viewModel.uiState.value.showsNoResults)
        }

    @Test
    fun `GIVEN the agenda never asked WHEN Permitir acesso is tapped THEN asks the system`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(AgendaUiEvent.OnAccessChanged(AgendaAccess.NOT_ASKED))

        // When
        viewModel.onEvent(AgendaUiEvent.OnAllowAccessClicked)

        // Then
        assertEquals(listOf<AgendaUiAction>(AgendaUiAction.RequestAccess), actions)
    }

    @Test
    fun `GIVEN the agenda refused WHEN Permitir acesso is tapped THEN asks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(AgendaUiEvent.OnAccessChanged(AgendaAccess.DENIED))

        // When
        viewModel.onEvent(AgendaUiEvent.OnAllowAccessClicked)

        // Then
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN the agenda refused WHEN Abrir configuracoes is tapped THEN opens the settings`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(AgendaUiEvent.OnAccessChanged(AgendaAccess.DENIED))

            // When
            viewModel.onEvent(AgendaUiEvent.OnOpenSettingsClicked)

            // Then
            assertEquals(listOf<AgendaUiAction>(AgendaUiAction.OpenSettings), actions)
        }

    @Test
    fun `GIVEN the access granted WHEN Abrir configuracoes is tapped THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(AgendaUiEvent.OnAccessChanged(AgendaAccess.GRANTED))

        // When
        viewModel.onEvent(AgendaUiEvent.OnOpenSettingsClicked)

        // Then
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN the agenda over Contatos WHEN Convidar is tapped on Helena THEN opens the invite with her name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(AgendaUiEvent.OnAccessChanged(AgendaAccess.GRANTED))
            viewModel.onEvent(AgendaUiEvent.OnContactsLoaded(agenda))

            // When
            viewModel.onEvent(AgendaUiEvent.OnInviteClicked("agenda-helena"))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.Contacts,
                    Route.ContactsAgenda,
                    Route.ContactInvite(
                        personKey = null,
                        name = "Helena Souza",
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the agenda in the pane of Contatos WHEN Convidar is tapped on Helena THEN opens the invite`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(inPane = true)
            viewModel.onEvent(AgendaUiEvent.OnAccessChanged(AgendaAccess.GRANTED))
            viewModel.onEvent(AgendaUiEvent.OnContactsLoaded(agenda))

            // When
            viewModel.onEvent(AgendaUiEvent.OnInviteClicked("agenda-helena"))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.Contacts,
                    Route.ContactInvite(
                        personKey = null,
                        name = "Helena Souza",
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the invite already open WHEN a late Convidar arrives THEN stacks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(AgendaUiEvent.OnAccessChanged(AgendaAccess.GRANTED))
        viewModel.onEvent(AgendaUiEvent.OnContactsLoaded(agenda))
        viewModel.onEvent(AgendaUiEvent.OnInviteClicked("agenda-helena"))

        // When
        viewModel.onEvent(AgendaUiEvent.OnInviteClicked("agenda-carlos"))

        // Then
        assertEquals(
            Route.ContactInvite(
                personKey = null,
                name = "Helena Souza",
            ),
            navigator.backStack.value.last(),
        )
        assertEquals(4, navigator.backStack.value.size)
    }

    @Test
    fun `GIVEN a contact no longer listed WHEN Convidar is tapped on it THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(AgendaUiEvent.OnInviteClicked("agenda-helena"))

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts, Route.ContactsAgenda), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the agenda WHEN Convidar com link is tapped THEN opens the invite for nobody in particular`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(AgendaUiEvent.OnInviteWithLinkClicked)

            // Then
            assertEquals(
                Route.ContactInvite(
                    personKey = null,
                    name = null,
                ),
                navigator.backStack.value.last(),
            )
        }

    @Test
    fun `GIVEN the agenda on top WHEN back is tapped THEN goes back to Contatos`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(AgendaUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the agenda in the pane of Contatos WHEN back is tapped THEN leaves Contatos open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(inPane = true)

            // When
            viewModel.onEvent(AgendaUiEvent.OnBackClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
        }

    private val agenda = listOf(
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
        AgendaContact(
            id = "agenda-julia",
            name = "Júlia Prado",
            phone = "(11) 98765-4321",
        ),
    )

    private fun TestScope.prepareScenario(inPane: Boolean = false) {
        navigator = BackStackNavigator(startRoute = Route.Home).also {
            it.navigate(Route.Contacts)
            if (!inPane) it.navigate(Route.ContactsAgenda)
        }
        viewModel = AgendaViewModel(navigator = navigator)
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
