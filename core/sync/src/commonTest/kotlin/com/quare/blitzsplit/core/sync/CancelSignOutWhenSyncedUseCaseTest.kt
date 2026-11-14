package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.testing.FakeActivityRepository
import com.quare.blitzsplit.core.contacts.testing.FakeHiddenContactRepository
import com.quare.blitzsplit.core.expenses.testing.FakeBillAttachmentRepository
import com.quare.blitzsplit.core.expenses.testing.FakeBillCommentRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryTermRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCustomColorRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupIncomeRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupPinRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.testing.FakeAppInviteRepository
import com.quare.blitzsplit.core.profile.testing.FakeNotificationPreferencesRepository
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.profile.testing.FakePushDeviceRepository
import com.quare.blitzsplit.core.provider.crashlytics.CrashReporter
import com.quare.blitzsplit.core.sync.domain.usecase.impl.CancelSignOutWhenSyncedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.Test
import kotlin.test.assertFalse

class CancelSignOutWhenSyncedUseCaseTest {
    private val paymentKeys = FakePaymentKeyRepository()

    private lateinit var account: FakeAccountRepository
    private lateinit var useCase: CancelSignOutWhenSyncedUseCase

    @Test
    fun `GIVEN a sign out scheduled WHEN cancelling it THEN the schedule is off`() = runTest {
        // Given
        prepareScenario()

        // When
        useCase()

        // Then
        assertFalse(account.signOutWhenSynced)
    }

    private fun TestScope.prepareScenario() {
        account = FakeAccountRepository(
            initialStatus = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE),
        ).also { it.signOutWhenSynced = true }
        useCase = CancelSignOutWhenSyncedUseCase(
            SyncCoordinator(
                accountRepository = account,
                profileRepository = FakeProfileRepository(
                    initialProfile = UserProfile("Pierre", AvatarColor.BLUE, photo = null),
                ),
                paymentKeyRepository = paymentKeys,
                appInviteRepository = FakeAppInviteRepository(),
                notificationPreferencesRepository = FakeNotificationPreferencesRepository(),
                groupRepository = FakeGroupRepository(),
                groupPinRepository = FakeGroupPinRepository(),
                groupIncomeRepository = FakeGroupIncomeRepository(),
                expenseRepository = FakeExpenseRepository(),
                settlementRepository = FakeSettlementRepository(),
                categoryRepository = FakeCategoryRepository(),
                personalCategoryData = PersonalCategoryData(FakeCategoryTermRepository(), FakeCustomColorRepository()),
                billCommentRepository = FakeBillCommentRepository(),
                billAttachmentRepository = FakeBillAttachmentRepository(),
                activityRepository = FakeActivityRepository(),
                hiddenContactRepository = FakeHiddenContactRepository(),
                pushDeviceRepository = FakePushDeviceRepository(),
                remoteChangeRepository = FakeRemoteChangeRepository(),
                firstDownload = FirstDownload(),
                appForeground = AppForeground(),
                connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
                crashReporter = CrashReporter {},
                scope = backgroundScope,
            ),
        )
    }
}
