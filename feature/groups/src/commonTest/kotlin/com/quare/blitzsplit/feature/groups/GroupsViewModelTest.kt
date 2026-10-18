package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountStatusUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveRecoveryCodePromptUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.SyncBadge
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.GroupsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var account: FakeAccountRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var viewModel: GroupsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN no internet WHEN the groups open THEN shows the offline badge`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(SyncBadge.OFFLINE, state.syncBadge)
    }

    @Test
    fun `GIVEN the server knows the user WHEN online THEN shows the synced badge`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.Anonymous("user"))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(SyncBadge.SYNCED, state.syncBadge)
    }

    @Test
    fun `GIVEN a profile WHEN the groups open THEN draws its initial and color`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals("P", state.avatarInitial)
        assertEquals(AvatarColor.GREEN, state.avatarColor)
    }

    @Test
    fun `GIVEN a due recovery code WHEN the groups open THEN asks for the sheet`() = runTest(testDispatcher) {
        // Given
        prepareScenario(prompt = RecoveryCodePrompt.DUE)

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.showsRecoveryCodePrompt)
    }

    @Test
    fun `GIVEN the empty groups WHEN tapping a flow not built yet THEN says it is coming soon`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val actions = mutableListOf<GroupsUiAction>()
            backgroundScope.launch { viewModel.uiAction.collect(actions::add) }

            // When
            viewModel.onEvent(GroupsUiEvent.OnCreateGroupClicked)
            viewModel.onEvent(GroupsUiEvent.OnJoinWithLinkClicked)
            viewModel.onEvent(GroupsUiEvent.OnProfileClicked)
            viewModel.onEvent(GroupsUiEvent.OnBalancesTabClicked)
            viewModel.onEvent(GroupsUiEvent.OnContactsTabClicked)
            viewModel.onEvent(GroupsUiEvent.OnActivityTabClicked)

            // Then
            assertEquals(List<GroupsUiAction>(6) { GroupsUiAction.ShowComingSoon }, actions)
        }

    private fun prepareScenario(
        online: Boolean = true,
        status: AccountStatus = AccountStatus.LocalOnly,
        prompt: RecoveryCodePrompt = RecoveryCodePrompt.NOT_DUE,
    ) {
        account = FakeAccountRepository(initialStatus = status).also { it.recoveryCodePrompt.value = prompt }
        isOnline = MutableStateFlow(online)
        viewModel = GroupsViewModel(
            observeProfile = ObserveProfileUseCase(
                FakeProfileRepository(UserProfile("pierre", AvatarColor.GREEN, null)),
            ),
            observeAccountStatus = ObserveAccountStatusUseCase(account),
            observeRecoveryCodePrompt = ObserveRecoveryCodePromptUseCase(account),
            connectivityObserver = NetworkConnectivityObserver { isOnline },
        )
    }
}
