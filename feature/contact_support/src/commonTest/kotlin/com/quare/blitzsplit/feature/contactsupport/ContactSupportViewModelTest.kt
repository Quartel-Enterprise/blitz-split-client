package com.quare.blitzsplit.feature.contactsupport

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.support.domain.model.SupportAccount
import com.quare.blitzsplit.core.support.domain.model.SupportDiagnostics
import com.quare.blitzsplit.core.support.domain.model.SupportPlatform
import com.quare.blitzsplit.feature.contactsupport.presentation.model.ContactSupportNotice
import com.quare.blitzsplit.feature.contactsupport.presentation.model.ContactSupportUiAction
import com.quare.blitzsplit.feature.contactsupport.presentation.model.ContactSupportUiEvent
import com.quare.blitzsplit.feature.contactsupport.presentation.viewmodel.ContactSupportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import software.quare.loadable.Loadable
import software.quare.loadable.valueOrNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContactSupportViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var diagnostics: MutableSharedFlow<SupportDiagnostics>
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: ContactSupportViewModel
    private val actions = mutableListOf<ContactSupportUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN the diagnostics were not read yet WHEN the sheet opens THEN shows the support address`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = null)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SUPPORT_EMAIL, state.email)
        }

    @Test
    fun `GIVEN the diagnostics were not read yet WHEN the sheet opens THEN they load and the email waits`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = null)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(Loadable.Loading, state.diagnostics)
            assertFalse(state.canOpenEmail)
            assertNull(state.notice)
        }

    @Test
    fun `GIVEN the diagnostics were read WHEN the sheet opens THEN shows them and the email can open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = androidDiagnostics)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(androidDiagnostics, state.diagnostics.valueOrNull())
            assertTrue(state.canOpenEmail)
            assertEquals(SUPPORT_EMAIL, state.email)
            assertNull(state.notice)
        }

    @Test
    fun `GIVEN the sheet is loading WHEN the diagnostics are read THEN shows them and the email can open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = null)

            // When
            diagnostics.emit(androidDiagnostics)

            // Then
            assertEquals(
                androidDiagnostics,
                viewModel.uiState.value.diagnostics
                    .valueOrNull(),
            )
            assertTrue(viewModel.uiState.value.canOpenEmail)
        }

    @Test
    fun `GIVEN the sheet shows the diagnostics WHEN the language and the account change THEN shows the new ones`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = androidDiagnostics)

            // When
            diagnostics.emit(connectedDiagnostics)

            // Then
            assertEquals(
                connectedDiagnostics,
                viewModel.uiState.value.diagnostics
                    .valueOrNull(),
            )
            assertTrue(viewModel.uiState.value.canOpenEmail)
        }

    @Test
    fun `GIVEN the diagnostics were not read yet WHEN tapping Open email THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = null)

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailClicked)

            // Then
            assertTrue(actions.isEmpty())
            assertFalse(viewModel.uiState.value.canOpenEmail)
            assertNull(viewModel.uiState.value.notice)
        }

    @Test
    fun `GIVEN the diagnostics were not read yet WHEN tapping Open email THEN no event is tracked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = null)

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the address was copied before the diagnostics WHEN tapping Open email THEN the notice stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = null)
            viewModel.onEvent(ContactSupportUiEvent.OnCopyEmailClicked)

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailClicked)

            // Then
            assertEquals(ContactSupportNotice.EMAIL_COPIED, viewModel.uiState.value.notice)
            assertEquals(listOf<ContactSupportUiAction>(copyAddress), actions)
        }

    @Test
    fun `GIVEN the diagnostics were read WHEN tapping Open email THEN opens the link built for them once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = androidDiagnostics)

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailClicked)

            // Then
            assertEquals(
                listOf<ContactSupportUiAction>(ContactSupportUiAction.OpenEmail(linkOf(androidDiagnostics))),
                actions,
            )
        }

    @Test
    fun `GIVEN the diagnostics were read WHEN tapping Open email THEN tracks the email opened without params`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = androidDiagnostics)

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailClicked)

            // Then
            assertEquals(listOf(emailOpened), trackedEvents)
        }

    @Test
    fun `GIVEN the diagnostics were read WHEN tapping Open email THEN shows no notice`() = runTest(testDispatcher) {
        // Given
        prepareScenario(readDiagnostics = androidDiagnostics)

        // When
        viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailClicked)

        // Then
        assertNull(viewModel.uiState.value.notice)
    }

    @Test
    fun `GIVEN the diagnostics arrived after the sheet opened WHEN tapping Open email THEN opens their link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = null)
            diagnostics.emit(androidDiagnostics)

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailClicked)

            // Then
            assertEquals(
                listOf<ContactSupportUiAction>(ContactSupportUiAction.OpenEmail(linkOf(androidDiagnostics))),
                actions,
            )
            assertEquals(listOf(emailOpened), trackedEvents)
        }

    @Test
    fun `GIVEN the account changed while the sheet was open WHEN tapping Open email THEN the link is the new one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = androidDiagnostics)
            diagnostics.emit(connectedDiagnostics)

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailClicked)

            // Then
            assertEquals(
                listOf<ContactSupportUiAction>(ContactSupportUiAction.OpenEmail(linkOf(connectedDiagnostics))),
                actions,
            )
        }

    @Test
    fun `GIVEN the email was opened once WHEN tapping Open email again THEN opens it again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = androidDiagnostics)
            viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailClicked)

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailClicked)

            // Then
            val openEmail = ContactSupportUiAction.OpenEmail(linkOf(androidDiagnostics))
            assertEquals(listOf<ContactSupportUiAction>(openEmail, openEmail), actions)
            assertEquals(listOf(emailOpened, emailOpened), trackedEvents)
        }

    @Test
    fun `GIVEN the diagnostics were read WHEN tapping Copy email address THEN copies the support address`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = androidDiagnostics)

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnCopyEmailClicked)

            // Then
            assertEquals(listOf<ContactSupportUiAction>(copyAddress), actions)
        }

    @Test
    fun `GIVEN the diagnostics were read WHEN tapping Copy email address THEN says the address was copied`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = androidDiagnostics)

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnCopyEmailClicked)

            // Then
            assertEquals(ContactSupportNotice.EMAIL_COPIED, viewModel.uiState.value.notice)
        }

    @Test
    fun `GIVEN the diagnostics were read WHEN tapping Copy email address THEN tracks the copy without params`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = androidDiagnostics)

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnCopyEmailClicked)

            // Then
            assertEquals(listOf(emailCopied), trackedEvents)
        }

    @Test
    fun `GIVEN the diagnostics were not read yet WHEN tapping Copy email address THEN copies it all the same`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = null)

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnCopyEmailClicked)

            // Then
            assertEquals(listOf<ContactSupportUiAction>(copyAddress), actions)
            assertEquals(ContactSupportNotice.EMAIL_COPIED, viewModel.uiState.value.notice)
            assertEquals(listOf(emailCopied), trackedEvents)
            assertEquals(Loadable.Loading, viewModel.uiState.value.diagnostics)
        }

    @Test
    fun `GIVEN Open email was tapped WHEN no app takes the link THEN copies the support address instead`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = androidDiagnostics)
            viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailClicked)

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailFailed)

            // Then
            assertEquals(
                listOf(ContactSupportUiAction.OpenEmail(linkOf(androidDiagnostics)), copyAddress),
                actions,
            )
        }

    @Test
    fun `GIVEN Open email was tapped WHEN no app takes the link THEN says there is no email app`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = androidDiagnostics)
            viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailClicked)

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailFailed)

            // Then
            assertEquals(ContactSupportNotice.NO_EMAIL_APP, viewModel.uiState.value.notice)
        }

    @Test
    fun `GIVEN Open email was tapped WHEN no app takes the link THEN sends that there is no email app`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = androidDiagnostics)
            viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailClicked)

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailFailed)

            // Then
            assertEquals(listOf(emailOpened, noEmailApp), trackedEvents)
        }

    @Test
    fun `GIVEN the no email app notice shows WHEN tapping Open email again THEN the notice is gone`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = androidDiagnostics)
            viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailClicked)
            viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailFailed)

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailClicked)

            // Then
            assertNull(viewModel.uiState.value.notice)
            assertEquals(ContactSupportUiAction.OpenEmail(linkOf(androidDiagnostics)), actions.last())
        }

    @Test
    fun `GIVEN the address copied notice shows WHEN tapping Open email THEN the notice is gone`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = androidDiagnostics)
            viewModel.onEvent(ContactSupportUiEvent.OnCopyEmailClicked)

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailClicked)

            // Then
            assertNull(viewModel.uiState.value.notice)
        }

    @Test
    fun `GIVEN the no email app notice shows WHEN tapping Copy email address THEN says the address was copied`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = androidDiagnostics)
            viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailClicked)
            viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailFailed)

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnCopyEmailClicked)

            // Then
            assertEquals(ContactSupportNotice.EMAIL_COPIED, viewModel.uiState.value.notice)
        }

    @Test
    fun `GIVEN the sheet is over Profile WHEN closing it THEN returns to Profile`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ContactSupportUiEvent.OnDismissed)

        // Then
        assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the sheet is over Profile WHEN closing it THEN tracks the dismissal without params`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnDismissed)

            // Then
            assertEquals(listOf(dismissed), trackedEvents)
        }

    @Test
    fun `GIVEN the diagnostics were not read yet WHEN closing the sheet THEN returns to Profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = null)

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnDismissed)

            // Then
            assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the sheet was closed WHEN a second dismissal arrives THEN Profile stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ContactSupportUiEvent.OnDismissed)

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnDismissed)

            // Then
            assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
            assertEquals(listOf(dismissed), trackedEvents)
        }

    @Test
    fun `GIVEN another screen is on top of the sheet WHEN a dismissal arrives THEN nothing closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.DeleteAccount)

            // When
            viewModel.onEvent(ContactSupportUiEvent.OnDismissed)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.Profile, Route.ContactSupport, Route.DeleteAccount),
                navigator.backStack.value,
            )
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the sheet is open WHEN closing it THEN nothing is opened nor copied`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ContactSupportUiEvent.OnDismissed)

        // Then
        assertTrue(actions.isEmpty())
        assertNull(viewModel.uiState.value.notice)
    }

    @Test
    fun `GIVEN a connected account with a profile link WHEN every event is sent THEN no event carries a param`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = connectedDiagnostics)

            // When
            everyEvent.forEach(viewModel::onEvent)

            // Then
            assertEquals(listOf(emailOpened, noEmailApp, emailCopied, dismissed), trackedEvents)
            assertTrue(trackedEvents.all { (_, params) -> params.isEmpty() })
        }

    @Test
    fun `GIVEN a connected account with a profile link WHEN every event is sent THEN no name holds an address`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(readDiagnostics = connectedDiagnostics)

            // When
            everyEvent.forEach(viewModel::onEvent)

            // Then
            assertFalse(trackedEvents.any { (name, _) -> name.contains(SUPPORT_EMAIL) || name.contains(PROFILE_LINK) })
        }

    private val androidDiagnostics = SupportDiagnostics(
        appVersion = "1.4.0",
        platform = SupportPlatform.ANDROID,
        language = AppLanguage.SYSTEM,
        spokenLanguage = "en",
        account = SupportAccount.LOCAL_ONLY,
        profileLink = null,
    )
    private val connectedDiagnostics = SupportDiagnostics(
        appVersion = "1.4.0",
        platform = SupportPlatform.ANDROID,
        language = AppLanguage.PORTUGUESE,
        spokenLanguage = "pt",
        account = SupportAccount.GOOGLE,
        profileLink = PROFILE_LINK,
    )
    private val copyAddress = ContactSupportUiAction.CopyToClipboard(SUPPORT_EMAIL)
    private val emailOpened = "contact_support_open_email_clicked" to emptyMap<String, Any>()
    private val emailCopied = "contact_support_copy_email_clicked" to emptyMap<String, Any>()
    private val noEmailApp = "contact_support_no_email_app" to emptyMap<String, Any>()
    private val dismissed = "contact_support_dismissed" to emptyMap<String, Any>()
    private val everyEvent = listOf(
        ContactSupportUiEvent.OnOpenEmailClicked,
        ContactSupportUiEvent.OnOpenEmailFailed,
        ContactSupportUiEvent.OnCopyEmailClicked,
        ContactSupportUiEvent.OnDismissed,
    )

    // Why: a link that names the diagnostics it was built for, so a case tells which ones the email carries.
    private fun linkOf(diagnostics: SupportDiagnostics): String = "mailto:link-for-$diagnostics"

    private suspend fun TestScope.prepareScenario(readDiagnostics: SupportDiagnostics? = androidDiagnostics) {
        diagnostics = MutableSharedFlow()
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.Profile)
            it.navigate(Route.ContactSupport)
        }
        viewModel = ContactSupportViewModel(
            navigator = navigator,
            buildSupportEmailLink = ::linkOf,
            observeSupportDiagnostics = { diagnostics },
            trackEvent = { name, params -> trackedEvents += name to params },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
        readDiagnostics?.let { diagnostics.emit(it) }
    }

    private companion object {
        const val SUPPORT_EMAIL = "appborarachar@gmail.com"
        const val PROFILE_LINK = "https://blitzsplit.com/u/7Hk2pQ8xZa"
    }
}
