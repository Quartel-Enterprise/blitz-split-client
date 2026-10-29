package com.quare.blitzsplit.feature.deleteaccount

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountStatusUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.usecase.DeleteAccount
import com.quare.blitzsplit.core.sync.domain.usecase.ExportUserData
import com.quare.blitzsplit.feature.deleteaccount.presentation.model.DeleteAccountUiAction
import com.quare.blitzsplit.feature.deleteaccount.presentation.model.DeleteAccountUiEvent
import com.quare.blitzsplit.feature.deleteaccount.presentation.viewmodel.DeleteAccountViewModel
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeleteAccountViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var account: FakeAccountRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: DeleteAccountViewModel
    private val actions = mutableListOf<DeleteAccountUiAction>()
    private var deletions = 0
    private var deleteResult: Result<Unit> = Result.success(Unit)
    private var deleteGate: CompletableDeferred<Result<Unit>>? = null

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN a Google account WHEN the screen opens THEN names Google with the box unchecked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = googleAccount)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(AccountProvider.GOOGLE, state.accountProvider)
            assertTrue(state.isOnline)
            assertFalse(state.hasConfirmed)
            assertFalse(state.isDeleting)
            assertFalse(state.hasFailed)
        }

    @Test
    fun `GIVEN an Apple account WHEN the screen opens THEN names Apple`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = appleAccount)

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(AccountProvider.APPLE, state.accountProvider)
    }

    @Test
    fun `GIVEN an anonymous user WHEN the screen opens THEN names no account`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID))

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.accountProvider)
    }

    @Test
    fun `GIVEN only a local profile WHEN the screen opens THEN names no account`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.LocalOnly)

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.accountProvider)
    }

    @Test
    fun `GIVEN no internet WHEN the screen opens THEN shows the offline notice and dims the box`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isOnline)
            assertTrue(state.showsOfflineNotice)
            assertFalse(state.isConfirmEnabled)
            assertFalse(state.canDelete)
        }

    @Test
    fun `GIVEN internet WHEN the screen opens THEN enables the box but not the delete button`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.showsOfflineNotice)
            assertTrue(state.isConfirmEnabled)
            assertFalse(state.canDelete)
        }

    @Test
    fun `GIVEN the screen open WHEN the box is checked THEN enables the delete button`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(DeleteAccountUiEvent.OnConfirmChanged(isChecked = true))

        // Then
        assertTrue(viewModel.uiState.value.hasConfirmed)
        assertTrue(viewModel.uiState.value.canDelete)
    }

    @Test
    fun `GIVEN the box checked WHEN it is unchecked THEN disables the delete button`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(DeleteAccountUiEvent.OnConfirmChanged(isChecked = true))

        // When
        viewModel.onEvent(DeleteAccountUiEvent.OnConfirmChanged(isChecked = false))

        // Then
        assertFalse(viewModel.uiState.value.hasConfirmed)
        assertFalse(viewModel.uiState.value.canDelete)
    }

    @Test
    fun `GIVEN no internet WHEN the box is checked THEN it stays unchecked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        viewModel.onEvent(DeleteAccountUiEvent.OnConfirmChanged(isChecked = true))

        // Then
        assertFalse(viewModel.uiState.value.hasConfirmed)
    }

    @Test
    fun `GIVEN a deletion under way WHEN the box is unchecked THEN it stays checked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(deleteHangs = true)
        viewModel.onEvent(DeleteAccountUiEvent.OnConfirmChanged(isChecked = true))
        viewModel.onEvent(DeleteAccountUiEvent.OnDeleteClicked)

        // When
        viewModel.onEvent(DeleteAccountUiEvent.OnConfirmChanged(isChecked = false))

        // Then
        assertTrue(viewModel.uiState.value.hasConfirmed)
        assertFalse(viewModel.uiState.value.isConfirmEnabled)
    }

    @Test
    fun `GIVEN the box unchecked WHEN delete is tapped THEN deletes nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(DeleteAccountUiEvent.OnDeleteClicked)

        // Then
        assertEquals(0, deletions)
        assertFalse(viewModel.uiState.value.isDeleting)
    }

    @Test
    fun `GIVEN the box checked and then no internet WHEN delete is tapped THEN deletes nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(DeleteAccountUiEvent.OnConfirmChanged(isChecked = true))
            isOnline.value = false

            // When
            viewModel.onEvent(DeleteAccountUiEvent.OnDeleteClicked)

            // Then
            assertEquals(0, deletions)
            assertFalse(viewModel.uiState.value.isDeleting)
        }

    @Test
    fun `GIVEN an anonymous user who checked the box WHEN delete is tapped THEN deletes nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID))
            viewModel.onEvent(DeleteAccountUiEvent.OnConfirmChanged(isChecked = true))

            // When
            viewModel.onEvent(DeleteAccountUiEvent.OnDeleteClicked)

            // Then
            assertFalse(viewModel.uiState.value.canDelete)
            assertEquals(0, deletions)
        }

    @Test
    fun `GIVEN the box checked WHEN delete is tapped THEN deletes the account and shows it deleting`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(deleteHangs = true)
            viewModel.onEvent(DeleteAccountUiEvent.OnConfirmChanged(isChecked = true))

            // When
            viewModel.onEvent(DeleteAccountUiEvent.OnDeleteClicked)

            // Then
            assertEquals(1, deletions)
            assertTrue(viewModel.uiState.value.isDeleting)
            assertFalse(viewModel.uiState.value.hasFailed)
            assertFalse(viewModel.uiState.value.canDelete)
        }

    @Test
    fun `GIVEN the box checked WHEN the deletion succeeds THEN stays on the screen deleting`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(deleteResult = Result.success(Unit))
            viewModel.onEvent(DeleteAccountUiEvent.OnConfirmChanged(isChecked = true))

            // When
            viewModel.onEvent(DeleteAccountUiEvent.OnDeleteClicked)

            // Then
            assertTrue(viewModel.uiState.value.isDeleting)
            assertFalse(viewModel.uiState.value.hasFailed)
            assertEquals(listOf(Route.Groups, Route.Profile, Route.DeleteAccount), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a successful deletion WHEN the account is gone THEN names no account and keeps deleting`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(deleteResult = Result.success(Unit))
            viewModel.onEvent(DeleteAccountUiEvent.OnConfirmChanged(isChecked = true))
            viewModel.onEvent(DeleteAccountUiEvent.OnDeleteClicked)

            // When
            account.status = AccountStatus.LocalOnly

            // Then
            assertNull(viewModel.uiState.value.accountProvider)
            assertTrue(viewModel.uiState.value.isDeleting)
        }

    @Test
    fun `GIVEN the box checked WHEN the deletion fails THEN shows the failure and allows another try`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(deleteResult = Result.failure(IllegalStateException("server error")))
            viewModel.onEvent(DeleteAccountUiEvent.OnConfirmChanged(isChecked = true))

            // When
            viewModel.onEvent(DeleteAccountUiEvent.OnDeleteClicked)

            // Then
            assertFalse(viewModel.uiState.value.isDeleting)
            assertTrue(viewModel.uiState.value.hasFailed)
            assertTrue(viewModel.uiState.value.canDelete)
            assertEquals(listOf(Route.Groups, Route.Profile, Route.DeleteAccount), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a failed deletion WHEN delete is tapped again THEN clears the failure and deletes again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(deleteResult = Result.failure(IllegalStateException("server error")))
            viewModel.onEvent(DeleteAccountUiEvent.OnConfirmChanged(isChecked = true))
            viewModel.onEvent(DeleteAccountUiEvent.OnDeleteClicked)
            deleteGate = CompletableDeferred()

            // When
            viewModel.onEvent(DeleteAccountUiEvent.OnDeleteClicked)

            // Then
            assertEquals(2, deletions)
            assertTrue(viewModel.uiState.value.isDeleting)
            assertFalse(viewModel.uiState.value.hasFailed)
        }

    @Test
    fun `GIVEN a deletion under way WHEN delete is tapped again THEN deletes only once`() = runTest(testDispatcher) {
        // Given
        prepareScenario(deleteHangs = true)
        viewModel.onEvent(DeleteAccountUiEvent.OnConfirmChanged(isChecked = true))
        viewModel.onEvent(DeleteAccountUiEvent.OnDeleteClicked)

        // When
        viewModel.onEvent(DeleteAccountUiEvent.OnDeleteClicked)

        // Then
        assertEquals(1, deletions)
    }

    @Test
    fun `GIVEN the screen open WHEN back is tapped THEN returns to the profile`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(DeleteAccountUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a native back already left the screen WHEN back is reported THEN keeps the profile open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigateBack()

            // When
            viewModel.onEvent(DeleteAccountUiEvent.OnBackClicked)

            // Then
            assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a deletion under way WHEN back is tapped THEN stays on the screen`() = runTest(testDispatcher) {
        // Given
        prepareScenario(deleteHangs = true)
        viewModel.onEvent(DeleteAccountUiEvent.OnConfirmChanged(isChecked = true))
        viewModel.onEvent(DeleteAccountUiEvent.OnDeleteClicked)

        // When
        viewModel.onEvent(DeleteAccountUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.Profile, Route.DeleteAccount), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a failed deletion WHEN back is tapped THEN returns to the profile`() = runTest(testDispatcher) {
        // Given
        prepareScenario(deleteResult = Result.failure(IllegalStateException("server error")))
        viewModel.onEvent(DeleteAccountUiEvent.OnConfirmChanged(isChecked = true))
        viewModel.onEvent(DeleteAccountUiEvent.OnDeleteClicked)

        // When
        viewModel.onEvent(DeleteAccountUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN internet WHEN export is tapped THEN saves the exported data`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(DeleteAccountUiEvent.OnExportClicked)

        // Then
        assertEquals(listOf<DeleteAccountUiAction>(DeleteAccountUiAction.SaveExport(exportedJson)), actions)
    }

    @Test
    fun `GIVEN no internet WHEN export is tapped THEN still saves the exported data`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        viewModel.onEvent(DeleteAccountUiEvent.OnExportClicked)

        // Then
        assertEquals(listOf<DeleteAccountUiAction>(DeleteAccountUiAction.SaveExport(exportedJson)), actions)
    }

    @Test
    fun `GIVEN the box checked WHEN the internet drops THEN disables delete and keeps the box checked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(DeleteAccountUiEvent.OnConfirmChanged(isChecked = true))

            // When
            isOnline.value = false

            // Then
            assertTrue(viewModel.uiState.value.showsOfflineNotice)
            assertTrue(viewModel.uiState.value.hasConfirmed)
            assertFalse(viewModel.uiState.value.isConfirmEnabled)
            assertFalse(viewModel.uiState.value.canDelete)
        }

    @Test
    fun `GIVEN the box checked and then no internet WHEN the internet returns THEN enables delete again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(DeleteAccountUiEvent.OnConfirmChanged(isChecked = true))
            isOnline.value = false

            // When
            isOnline.value = true

            // Then
            assertFalse(viewModel.uiState.value.showsOfflineNotice)
            assertTrue(viewModel.uiState.value.hasConfirmed)
            assertTrue(viewModel.uiState.value.canDelete)
        }

    private val googleAccount = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE)
    private val appleAccount = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.APPLE)
    private val exportedJson = """{"profile":{"displayName":"Pierre"}}"""

    private fun TestScope.prepareScenario(
        status: AccountStatus = googleAccount,
        online: Boolean = true,
        deleteResult: Result<Unit> = Result.success(Unit),
        deleteHangs: Boolean = false,
    ) {
        account = FakeAccountRepository(initialStatus = status)
        isOnline = MutableStateFlow(online)
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.Profile)
            it.navigate(Route.DeleteAccount)
        }
        this@DeleteAccountViewModelTest.deleteResult = deleteResult
        deleteGate = if (deleteHangs) CompletableDeferred() else null
        viewModel = DeleteAccountViewModel(
            navigator = navigator,
            deleteAccount = DeleteAccount {
                deletions++
                deleteGate?.await() ?: this@DeleteAccountViewModelTest.deleteResult
            },
            exportUserData = ExportUserData { exportedJson },
            observeAccountStatus = ObserveAccountStatusUseCase(account),
            connectivityObserver = NetworkConnectivityObserver { isOnline },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
