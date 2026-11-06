package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.UpdateRecoveryCodePromptUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.domain.usecase.ObserveActivityUnreadCount
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.expenses.domain.model.RecordedPayment
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.HoldPickedReceipt
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePeopleBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteSettlementUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsMoneyUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveMapCategoriesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObservePeopleBalancesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettledUpUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.expenses.testing.inCurrencyOf
import com.quare.blitzsplit.core.groups.data.repository.InMemoryGroupDeletionRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDeletion
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.EncodeQrCode
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeGroupDeletionUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupDeletionsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObservePinnedGroupIdsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupPinRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AppDownloadSource
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.NotificationPermission
import com.quare.blitzsplit.core.model.QrCode
import com.quare.blitzsplit.core.model.ReceiptPhotoSource
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.StoreListing
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.profile.domain.model.AppInvite
import com.quare.blitzsplit.core.profile.domain.model.ClientPlatform
import com.quare.blitzsplit.core.profile.domain.model.PhoneUse
import com.quare.blitzsplit.core.profile.domain.usecase.DismissAppInvite
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveAppInvite
import com.quare.blitzsplit.core.profile.domain.usecase.UpdatePushEnabled
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.settings.domain.usecase.DismissAppBanner
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveAppBannerDismissed
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveLastNotificationPromptAt
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateLastNotificationPromptAt
import com.quare.blitzsplit.core.sync.FirstDownload
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import com.quare.blitzsplit.core.sync.domain.usecase.impl.ObserveFirstDownloadUseCase
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.feature.groups.presentation.model.DeletedGroupNotice
import com.quare.blitzsplit.feature.groups.presentation.model.DownloadProgress
import com.quare.blitzsplit.feature.groups.presentation.model.GroupBalanceKind
import com.quare.blitzsplit.feature.groups.presentation.model.HomeAppInvite
import com.quare.blitzsplit.feature.groups.presentation.model.HomeSummary
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.NotificationPrompt
import com.quare.blitzsplit.feature.groups.presentation.model.SyncBadge
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.HomeViewModel
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.RecoveryCodeCardDismissal
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import software.quare.connectivity.NetworkConnectivityObserver
import software.quare.loadable.Loadable
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
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class HomeViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val appBannerDismissed = MutableStateFlow(false)
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var account: FakeAccountRepository
    private lateinit var groups: FakeGroupRepository
    private lateinit var pins: FakeGroupPinRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var categories: FakeCategoryRepository
    private lateinit var recordedPayments: InMemoryRecordedPaymentRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var syncState: MutableStateFlow<SyncState>
    private lateinit var minutes: MutableStateFlow<Int>
    private lateinit var navigator: BackStackNavigator
    private lateinit var deletions: InMemoryGroupDeletionRepository
    private lateinit var dismissal: RecoveryCodeCardDismissal
    private lateinit var firstDownload: FirstDownload
    private lateinit var activityUnread: MutableStateFlow<Int>
    private lateinit var peopleGate: MutableStateFlow<Boolean>
    private lateinit var groupsGate: MutableStateFlow<Boolean>

    // Why: "Get the app" as this platform offers it, which a sync or a dismissal changes.
    private lateinit var appInvite: MutableStateFlow<AppInvite?>
    private var appInviteDismissals = 0

    // Why: every link a QR code was encoded for, in order.
    private val encodedLinks = mutableListOf<String>()
    private val heldReceipts = mutableListOf<List<Byte>>()
    private lateinit var viewModel: HomeViewModel
    private val actions = mutableListOf<HomeUiAction>()
    private var retryCount = 0
    private var syncNowCount = 0
    private var syncGate: CompletableDeferred<Unit>? = null
    private var appInviteDismissGate: CompletableDeferred<Unit>? = null

    // Why: the device's clock, which a test moves on and then ticks a minute for.
    private var clockMillis = 0L

    // Why: the device's settings row, when Home's notification card was last answered.
    private val lastNotificationPromptAt = MutableStateFlow<Long?>(null)

    // Why: every time the card's answer was stored, in order.
    private val notificationPromptWrites = mutableListOf<Long>()
    private val pushSwitchWrites = mutableListOf<Boolean>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN Inicio WHEN tapping the profile THEN opens the profile over it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(HomeUiEvent.OnProfileClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the recovery code card WHEN tapping Conectar conta THEN opens the profile over Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.SNOOZED,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnConnectAccountClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Inicio WHEN tapping the Saldos tab THEN opens Saldos with everyone above Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            viewModel.onEvent(HomeUiEvent.OnBalancesTabClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Inicio WHEN tapping Ver saldos THEN opens Saldos with everyone above Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            viewModel.onEvent(HomeUiEvent.OnSeeBalancesClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Inicio WHEN tapping the Grupos tab THEN opens the groups above Inicio`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        viewModel.onEvent(HomeUiEvent.OnGroupsTabClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Inicio WHEN tapping Ver todos THEN opens the groups above Inicio`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        viewModel.onEvent(HomeUiEvent.OnSeeAllGroupsClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the profile over Inicio WHEN a late tap on Ver todos arrives THEN the profile stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(HomeUiEvent.OnSeeAllGroupsClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the profile over Inicio WHEN a late tap on Ver saldos arrives THEN the profile stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(HomeUiEvent.OnSeeBalancesClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the profile over Inicio WHEN late taps on its cards and shortcuts arrive THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                        amountMinor = 9000L,
                    ),
                ),
            )
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(HomeUiEvent.OnProfileClicked)
            viewModel.onEvent(HomeUiEvent.OnRecordPaymentClicked)
            viewModel.onEvent(HomeUiEvent.OnCreateGroupClicked)
            viewModel.onEvent(HomeUiEvent.OnJoinWithLinkClicked)
            viewModel.onEvent(HomeUiEvent.OnGroupClicked("trio"))
            viewModel.onEvent(HomeUiEvent.OnBillClicked("trio", "dinner"))
            viewModel.onEvent(HomeUiEvent.OnPersonClicked("name:ana", GroupCurrency.BRL))
            viewModel.onEvent(HomeUiEvent.OnToPayClicked)
            viewModel.onEvent(HomeUiEvent.OnSplitBillClicked)
            viewModel.onEvent(HomeUiEvent.OnSeeBalancesClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the profile over Inicio WHEN tapping the Saldos tab THEN closes it and opens Saldos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(HomeUiEvent.OnBalancesTabClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the profile over Inicio WHEN tapping the Grupos tab THEN closes it and opens Grupos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(HomeUiEvent.OnGroupsTabClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Inicio WHEN tapping Registrar pagamento THEN asks who paid whom over it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        viewModel.onEvent(HomeUiEvent.OnRecordPaymentClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.PickPayment), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Inicio WHEN tapping Novo grupo THEN opens it over Inicio`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        viewModel.onEvent(HomeUiEvent.OnCreateGroupClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.CreateGroup), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the first use WHEN tapping Entrar com link ou QR THEN opens the reader over Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(HomeUiEvent.OnJoinWithLinkClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.JoinWithLink(
                        skippedCode = null,
                        skippedMemberCode = null,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN internet WHEN tapping Ler cupom fiscal THEN asks where the photo comes from and stays on Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)

            // Then
            assertTrue(viewModel.uiState.value.showsReceiptSourceChoice)
            assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
        }

    @Test
    fun `GIVEN no internet WHEN tapping Ler cupom fiscal THEN does nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            online = false,
            storedGroups = listOf(trio()),
        )

        // When
        viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
        assertFalse(viewModel.uiState.value.canScanReceipt)
    }

    @Test
    fun `GIVEN no internet WHEN tapping Ler cupom fiscal THEN neither asks for the photo nor tracks the tap`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                storedGroups = listOf(trio()),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)

            // Then
            assertFalse(viewModel.uiState.value.showsReceiptSourceChoice)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the groups not read yet WHEN tapping Ler cupom fiscal THEN neither asks for the photo nor tracks`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                groupsRead = false,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)

            // Then
            assertFalse(viewModel.uiState.value.showsReceiptSourceChoice)
            assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the profile over Inicio WHEN a late tap on Ler cupom fiscal arrives THEN neither asks nor tracks`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)

            // Then
            assertFalse(viewModel.uiState.value.showsReceiptSourceChoice)
            assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN no group WHEN tapping Ler cupom fiscal THEN opens Novo grupo instead of asking for the photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.CreateGroup), navigator.backStack.value)
            assertFalse(viewModel.uiState.value.showsReceiptSourceChoice)
        }

    @Test
    fun `GIVEN the photo choice WHEN picking the gallery THEN the choice closes and the gallery opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)

            // When
            viewModel.onEvent(HomeUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.PHOTOS))

            // Then
            assertFalse(viewModel.uiState.value.showsReceiptSourceChoice)
            assertEquals(ReceiptPhotoSource.PHOTOS, viewModel.uiState.value.receiptSource)
            assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the photo choice WHEN picking the camera THEN tracks receipt_source_picked with the camera`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(HomeUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))

            // Then
            assertEquals(
                listOf("receipt_source_picked" to mapOf<String, Any>("source" to ReceiptPhotoSource.CAMERA)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN no photo choice open WHEN a late tap on a source arrives THEN opens no picker and tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            val before = viewModel.uiState.value

            // When
            viewModel.onEvent(HomeUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))

            // Then
            assertEquals(before, viewModel.uiState.value)
            assertNull(viewModel.uiState.value.receiptSource)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the photo choice without a camera WHEN it skips to the files THEN opens them and tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(HomeUiEvent.OnReceiptChoiceSkipped(ReceiptPhotoSource.FILES))

            // Then
            assertFalse(viewModel.uiState.value.showsReceiptSourceChoice)
            assertEquals(ReceiptPhotoSource.FILES, viewModel.uiState.value.receiptSource)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN no photo choice open WHEN a late skip to the gallery arrives THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            val before = viewModel.uiState.value

            // When
            viewModel.onEvent(HomeUiEvent.OnReceiptChoiceSkipped(ReceiptPhotoSource.PHOTOS))

            // Then
            assertEquals(before, viewModel.uiState.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the photo choice WHEN it closes without a pick THEN the choice goes and no picker opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(HomeUiEvent.OnReceiptPickerDismissed)

            // Then
            assertFalse(viewModel.uiState.value.showsReceiptSourceChoice)
            assertNull(viewModel.uiState.value.receiptSource)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the gallery open WHEN it closes without a photo THEN the gallery goes and Inicio stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(HomeUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.PHOTOS))

            // When
            viewModel.onEvent(HomeUiEvent.OnReceiptPickerDismissed)

            // Then
            assertFalse(viewModel.uiState.value.showsReceiptSourceChoice)
            assertNull(viewModel.uiState.value.receiptSource)
            assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
            assertTrue(heldReceipts.isEmpty())
        }

    @Test
    fun `GIVEN an older group with a newer bill WHEN the photo comes THEN opens Rachar conta reading it there`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    trio(
                        id = "old",
                        updatedAt = daysAgo(3),
                    ),
                    trio(
                        id = "new",
                        updatedAt = daysAgo(1),
                    ),
                ),
                storedExpenses = listOf(
                    bill(
                        "fresh",
                        groupId = "old",
                        spentAt = hoursAgo(1),
                    ),
                ),
            )
            viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(HomeUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.PHOTOS))
            val photo = byteArrayOf(1, 2, 3)

            // When
            viewModel.onEvent(HomeUiEvent.OnReceiptPhotoPicked(photo))

            // Then
            assertEquals(listOf(listOf<Byte>(1, 2, 3)), heldReceipts)
            assertEquals(
                listOf(
                    Route.Home,
                    Route.SplitBill(
                        groupId = "old",
                        expenseId = null,
                        picksGroupFirst = true,
                        scansReceipt = true,
                    ),
                ),
                navigator.backStack.value,
            )
            assertFalse(viewModel.uiState.value.showsReceiptSourceChoice)
            assertNull(viewModel.uiState.value.receiptSource)
        }

    @Test
    fun `GIVEN the camera open WHEN a photo comes THEN tracks photo_picked from the home`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))
        viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)
        viewModel.onEvent(HomeUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))
        trackedEvents.clear()

        // When
        viewModel.onEvent(HomeUiEvent.OnReceiptPhotoPicked(byteArrayOf(7)))

        // Then
        assertEquals(listOf("photo_picked" to mapOf<String, Any>("source" to "home")), trackedEvents)
    }

    @Test
    fun `GIVEN the gallery request already ended WHEN its photo comes later THEN still opens Rachar conta`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(HomeUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.PHOTOS))
            viewModel.onEvent(HomeUiEvent.OnReceiptPickerDismissed)

            // When
            viewModel.onEvent(HomeUiEvent.OnReceiptPhotoPicked(byteArrayOf(4, 5)))

            // Then
            assertEquals(listOf(listOf<Byte>(4, 5)), heldReceipts)
            assertEquals(
                listOf(Route.Home, Route.SplitBill.createPickingGroup(trio().id, scansReceipt = true)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the profile opened over Inicio WHEN the photo comes THEN drops it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))
        viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)
        viewModel.onEvent(HomeUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.PHOTOS))
        navigator.navigate(Route.Profile)
        trackedEvents.clear()

        // When
        viewModel.onEvent(HomeUiEvent.OnReceiptPhotoPicked(byteArrayOf(1)))

        // Then
        assertTrue(heldReceipts.isEmpty())
        assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Inicio still being read WHEN the photo comes THEN opens Rachar conta once Inicio is read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                groupsRead = false,
            )
            viewModel.onEvent(HomeUiEvent.OnReceiptPhotoPicked(byteArrayOf(7)))

            // When
            groupsGate.value = true

            // Then
            assertEquals(listOf(listOf<Byte>(7)), heldReceipts)
            assertEquals(
                listOf(Route.Home, Route.SplitBill.createPickingGroup(trio().id, scansReceipt = true)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the camera open WHEN another screen opens over Inicio THEN the camera closes for good`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(HomeUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))
            navigator.navigate(Route.Profile)

            // When
            navigator.navigateBack()

            // Then
            assertNull(viewModel.uiState.value.receiptSource)
            assertFalse(viewModel.uiState.value.showsReceiptSourceChoice)
        }

    @Test
    fun `GIVEN the photo choice WHEN another screen opens over Inicio THEN the choice closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)

            // When
            navigator.navigate(Route.Profile)

            // Then
            assertFalse(viewModel.uiState.value.showsReceiptSourceChoice)
        }

    @Test
    fun `GIVEN the only group gone while the gallery is open WHEN the photo comes THEN drops it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(HomeUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.PHOTOS))
            groups.groups.value = emptyList()
            trackedEvents.clear()

            // When
            viewModel.onEvent(HomeUiEvent.OnReceiptPhotoPicked(byteArrayOf(1)))

            // Then
            assertTrue(heldReceipts.isEmpty())
            assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a file that could not be read WHEN its empty photo comes THEN still opens Rachar conta with it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(HomeUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.FILES))

            // When
            viewModel.onEvent(HomeUiEvent.OnReceiptPhotoPicked(ByteArray(0)))

            // Then
            assertEquals(listOf(emptyList<Byte>()), heldReceipts)
            assertEquals(
                listOf(Route.Home, Route.SplitBill.createPickingGroup(trio().id, scansReceipt = true)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the receipt camera open WHEN turning its flashlight on THEN tracks it on and changes nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(HomeUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))
            trackedEvents.clear()
            val before = viewModel.uiState.value

            // When
            viewModel.onEvent(HomeUiEvent.OnReceiptTorchClicked(isOn = true))

            // Then
            assertEquals(
                listOf("receipt_camera_torch_toggled" to mapOf<String, Any>("is_on" to true)),
                trackedEvents,
            )
            assertEquals(before, viewModel.uiState.value)
            assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the receipt camera refused once WHEN tapping Permitir camera THEN only tracks it from the receipt`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(HomeUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))
            trackedEvents.clear()
            val before = viewModel.uiState.value

            // When
            viewModel.onEvent(HomeUiEvent.OnReceiptCameraAllowClicked)

            // Then
            assertEquals(
                listOf("camera_allow_clicked" to mapOf<String, Any>("source" to "receipt")),
                trackedEvents,
            )
            assertEquals(before, viewModel.uiState.value)
            assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the camera refused for good WHEN tapping Abrir configuracoes THEN only tracks it from the receipt`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(HomeUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))
            trackedEvents.clear()
            val before = viewModel.uiState.value

            // When
            viewModel.onEvent(HomeUiEvent.OnReceiptCameraSettingsClicked)

            // Then
            assertEquals(
                listOf("camera_settings_clicked" to mapOf<String, Any>("source" to "receipt")),
                trackedEvents,
            )
            assertEquals(before, viewModel.uiState.value)
            assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Inicio WHEN tapping Ver tudo THEN opens Atividade above Inicio`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        viewModel.onEvent(HomeUiEvent.OnSeeAllBillsClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Activity(personKey = null)), navigator.backStack.value)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN Inicio on top WHEN tapping the bell THEN opens Atividade above Inicio`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(HomeUiEvent.OnActivityClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Activity(personKey = null)), navigator.backStack.value)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN Inicio WHEN tapping the bell twice quickly THEN opens Atividade once`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(HomeUiEvent.OnActivityClicked)
        viewModel.onEvent(HomeUiEvent.OnActivityClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Activity(personKey = null)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a group opened from Inicio WHEN a late tap on the bell arrives THEN keeps the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(HomeUiEvent.OnGroupClicked("trio"))
            val stack = navigator.backStack.value

            // When
            viewModel.onEvent(HomeUiEvent.OnActivityClicked)

            // Then
            assertEquals(stack, navigator.backStack.value)
        }

    @Test
    fun `GIVEN unread events WHEN Inicio shows THEN its bell counts them`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        activityUnread.value = 4

        // Then
        assertEquals(4, viewModel.uiState.value.activityUnreadCount)
    }

    @Test
    fun `GIVEN a group opened from Inicio WHEN tapping Contatos THEN Contatos takes its place above Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(HomeUiEvent.OnGroupClicked("trio"))

            // When
            viewModel.onEvent(HomeUiEvent.OnContactsTabClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
        }

    @Test
    fun `GIVEN no group WHEN tapping Rachar conta THEN opens Novo grupo`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(HomeUiEvent.OnSplitBillClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.CreateGroup), navigator.backStack.value)
    }

    @Test
    fun `GIVEN an older group with a newer bill WHEN tapping Rachar conta THEN asks which group with it marked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    trio(
                        id = "old",
                        updatedAt = daysAgo(3),
                    ),
                    trio(
                        id = "new",
                        updatedAt = daysAgo(1),
                    ),
                ),
                storedExpenses = listOf(
                    bill(
                        "fresh",
                        groupId = "old",
                        spentAt = hoursAgo(1),
                    ),
                ),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnSplitBillClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.SplitBill(
                        groupId = "old",
                        expenseId = null,
                        picksGroupFirst = true,
                        scansReceipt = false,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the groups not read yet WHEN tapping Rachar conta THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(groupsRead = false)

        // When
        viewModel.onEvent(HomeUiEvent.OnSplitBillClicked)

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN people you owe and who owe you WHEN Inicio opens THEN whom you owe come first the largest first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "a",
                        members = listOf(me(), member("Ana"), member("Bruno")),
                    ),
                    group(
                        id = "b",
                        members = listOf(me(), member("Carla"), member("Diego")),
                    ),
                ),
                storedExpenses = listOf(
                    bill(
                        "ana",
                        groupId = "a",
                        amountMinor = 9000L,
                        splitOver = listOf("Pierre", "Ana"),
                    ),
                    bill(
                        "bruno",
                        groupId = "a",
                        amountMinor = 1000L,
                        splitOver = listOf("Pierre", "Bruno"),
                    ),
                    bill(
                        "carla",
                        groupId = "b",
                        paidBy = "Carla",
                        amountMinor = 600L,
                        splitOver = listOf("Pierre", "Carla"),
                    ),
                    bill(
                        "diego",
                        groupId = "b",
                        paidBy = "Diego",
                        amountMinor = 3000L,
                        splitOver = listOf("Pierre", "Diego"),
                    ),
                ),
            )

            // When
            val people = viewModel.uiState.value.people
                .valueOrNull()
                .orEmpty()

            // Then
            assertEquals(
                listOf("Diego" to -1500L, "Carla" to -300L, "Ana" to 4500L, "Bruno" to 500L),
                people.map { it.name to it.netMinor },
            )
            assertTrue(people.all { it.isGuest })
        }

    @Test
    fun `GIVEN you owe Ana WHEN tapping her face THEN opens settling up with her over Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                        amountMinor = 9000L,
                    ),
                ),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnPersonClicked("name:ana", GroupCurrency.BRL))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.SettleUp(
                        personKey = "name:ana",
                        currency = GroupCurrency.BRL,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN you owe Ana in reais and dollars WHEN tapping her dollar face THEN settles up in dollars`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    trio(),
                    trio(
                        id = "trip",
                        currency = GroupCurrency.USD,
                    ),
                ),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                        amountMinor = 9000L,
                    ),
                    bill(
                        "taxi",
                        groupId = "trip",
                        paidBy = "Ana",
                        amountMinor = 600L,
                    ),
                ),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnPersonClicked("name:ana", GroupCurrency.USD))

            // Then
            assertEquals(
                listOf(GroupCurrency.BRL to -3000L, GroupCurrency.USD to -200L),
                viewModel.uiState.value.people
                    .valueOrNull()
                    .orEmpty()
                    .filter { it.key == "name:ana" }
                    .map { it.currency to it.netMinor },
            )
            assertEquals(
                listOf(
                    Route.Home,
                    Route.SettleUp(
                        personKey = "name:ana",
                        currency = GroupCurrency.USD,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Ana owed only in reais WHEN tapping her face in dollars THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = listOf(
                bill(
                    "dinner",
                    paidBy = "Ana",
                    amountMinor = 9000L,
                ),
            ),
        )

        // When
        viewModel.onEvent(HomeUiEvent.OnPersonClicked("name:ana", GroupCurrency.USD))

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a sync settled Ana after her face showed WHEN a late tap on it arrives THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                        amountMinor = 9000L,
                    ),
                ),
            )
            settlements.settlements.update {
                it +
                    payment(
                        "paid",
                        from = "Pierre",
                        to = "Ana",
                        amountMinor = 3000L,
                    )
            }

            // When
            viewModel.onEvent(HomeUiEvent.OnPersonClicked("name:ana", GroupCurrency.BRL))

            // Then
            assertTrue(
                viewModel.uiState.value.people
                    .valueOrNull()
                    .orEmpty()
                    .isEmpty(),
            )
            assertEquals(listOf(Route.Home), navigator.backStack.value)
        }

    @Test
    fun `GIVEN groups in two currencies WHEN Inicio opens THEN Saldo geral sums the people per currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    trio(),
                    trio(
                        id = "trip",
                        currency = GroupCurrency.USD,
                    ),
                ),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                        amountMinor = 9000L,
                    ),
                    bill(
                        "taxi",
                        groupId = "trip",
                        amountMinor = 600L,
                    ),
                ),
            )

            // When
            val summary = assertNotNull(
                viewModel.uiState.value.summary
                    .valueOrNull(),
            )

            // Then
            assertEquals(
                listOf(
                    PeopleTotal(
                        currency = GroupCurrency.BRL,
                        netMinor = -3000L,
                        toReceiveMinor = 0L,
                        toPayMinor = 3000L,
                        toReceiveCount = 0,
                        toPayCount = 1,
                    ),
                    PeopleTotal(
                        currency = GroupCurrency.USD,
                        netMinor = 400L,
                        toReceiveMinor = 400L,
                        toPayMinor = 0L,
                        toReceiveCount = 2,
                        toPayCount = 0,
                    ),
                ),
                summary.totals,
            )
            assertEquals(GroupBalanceKind.YOU_OWE, summary.kind)
        }

    @Test
    fun `GIVEN something to receive WHEN tapping Te devem THEN opens Saldos on Te devem above Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnToReceiveClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.OWES_YOU)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN nothing to receive WHEN tapping Te devem THEN does nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = listOf(
                bill(
                    "dinner",
                    paidBy = "Ana",
                ),
            ),
        )

        // When
        viewModel.onEvent(HomeUiEvent.OnToReceiveClicked)

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN one person to pay WHEN tapping Voce deve THEN opens settling up with them over Voce deve`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                    ),
                ),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnToPayClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.GlobalBalances(BalancesFilter.YOU_OWE),
                    Route.SettleUp(
                        personKey = "name:ana",
                        currency = GroupCurrency.BRL,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN two people to pay WHEN tapping Voce deve THEN opens Saldos on Voce deve above Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                    ),
                    bill(
                        "lunch",
                        paidBy = "Bruno",
                        amountMinor = 6000L,
                    ),
                ),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnToPayClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.YOU_OWE)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN one person to pay in two currencies WHEN tapping Voce deve THEN opens Saldos on Voce deve`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    trio(),
                    trio(
                        id = "trip",
                        currency = GroupCurrency.USD,
                    ),
                ),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                    ),
                    bill(
                        "taxi",
                        groupId = "trip",
                        paidBy = "Ana",
                        amountMinor = 600L,
                    ),
                ),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnToPayClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.YOU_OWE)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN nothing to pay WHEN tapping Voce deve THEN does nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = listOf(bill("dinner")),
        )

        // When
        viewModel.onEvent(HomeUiEvent.OnToPayClicked)

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the balances not read yet WHEN Inicio opens THEN the faces and Saldo geral are loading`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                    ),
                ),
                peopleRead = false,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(Loadable.Loading, state.people)
            assertEquals(Loadable.Loading, state.summary)
            assertFalse(state.showsPeople)
            assertNull(state.onlyPersonToPay)
        }

    @Test
    fun `GIVEN the balances not read yet WHEN they are read THEN shows the faces and Saldo geral`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                    ),
                ),
                peopleRead = false,
            )

            // When
            peopleGate.value = true

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf("name:ana"), state.people.valueOrNull()?.map { it.key })
            assertEquals(GroupBalanceKind.YOU_OWE, state.summary.valueOrNull()?.kind)
            assertTrue(state.showsPeople)
        }

    @Test
    fun `GIVEN a group where nobody owes anybody WHEN Inicio opens THEN reads everyone settled rather than loading`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(Loadable.Loaded(emptyList()), state.people)
            val summary = assertIs<Loadable.Loaded<HomeSummary>>(state.summary).value
            assertEquals(GroupBalanceKind.YOU_ARE_SETTLED, summary.kind)
            assertFalse(state.showsPeople)
        }

    @Test
    fun `GIVEN no group WHEN Inicio opens THEN reads nobody and no total rather than loading`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(Loadable.Loaded(emptyList()), state.people)
            assertEquals(Loadable.Loaded(HomeSummary(totals = emptyList())), state.summary)
        }

    @Test
    fun `GIVEN something to receive and the balances not read yet WHEN tapping Te devem THEN does nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                peopleRead = false,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnToReceiveClicked)

            // Then
            assertEquals(listOf(Route.Home), navigator.backStack.value)
        }

    @Test
    fun `GIVEN one person to pay and the balances not read yet WHEN tapping Voce deve THEN does nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                    ),
                ),
                peopleRead = false,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnToPayClicked)

            // Then
            assertEquals(listOf(Route.Home), navigator.backStack.value)
        }

    @Test
    fun `GIVEN one person to pay read late WHEN tapping Voce deve THEN opens settling up with them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                    ),
                ),
                peopleRead = false,
            )
            peopleGate.value = true

            // When
            viewModel.onEvent(HomeUiEvent.OnToPayClicked)

            // Then
            assertEquals(
                Route.SettleUp(
                    personKey = "name:ana",
                    currency = GroupCurrency.BRL,
                ),
                navigator.backStack.value
                    .last(),
            )
        }

    @Test
    fun `GIVEN five open groups and one pinned far down WHEN Inicio opens THEN shows it and the two latest`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = (1..5).map {
                    trio(
                        id = "g$it",
                        updatedAt = daysAgo(it),
                    )
                },
                pinnedGroupIds = setOf("g5"),
            )

            // When
            val cards = viewModel.uiState.value.groups

            // Then
            assertEquals(listOf("g5", "g1", "g2"), cards.map { it.row.id })
            assertEquals(listOf(true, false, false), cards.map { it.isPinned })
        }

    @Test
    fun `GIVEN Inicio open WHEN a group gets pinned THEN its card moves first`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = (1..4).map {
                trio(
                    id = "g$it",
                    updatedAt = daysAgo(it),
                )
            },
        )

        // When
        pins.pinnedGroupIds.value = setOf("g4")

        // Then
        assertEquals(
            listOf("g4", "g1", "g2"),
            viewModel.uiState.value.groups
                .map { it.row.id },
        )
    }

    @Test
    fun `GIVEN a pinned group where everyone settled WHEN Inicio opens THEN leaves its card out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    trio(
                        id = "settled",
                        updatedAt = hoursAgo(1),
                    ),
                    trio(
                        id = "open",
                        updatedAt = daysAgo(2),
                    ),
                ),
                storedExpenses = listOf(
                    bill(
                        "gas",
                        groupId = "settled",
                        paidBy = "Ana",
                        amountMinor = 6000L,
                        splitOver = listOf("Ana", "Pierre"),
                        spentAt = daysAgo(3),
                    ),
                ),
                storedSettlements = listOf(
                    payment(
                        "paid",
                        groupId = "settled",
                        from = "Pierre",
                        to = "Ana",
                        amountMinor = 3000L,
                    ),
                ),
                pinnedGroupIds = setOf("settled"),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf("open"), state.groups.map { it.row.id })
            assertEquals(2, state.groupCount)
        }

    @Test
    fun `GIVEN only a group where everyone settled WHEN Inicio opens THEN shows the home without a card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "gas",
                        paidBy = "Ana",
                        amountMinor = 6000L,
                        splitOver = listOf("Ana", "Pierre"),
                    ),
                ),
                storedSettlements = listOf(
                    payment(
                        "paid",
                        from = "Pierre",
                        to = "Ana",
                        amountMinor = 3000L,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.groups.isEmpty())
            assertTrue(state.showsHome)
            assertFalse(state.showsFirstUse)
        }

    @Test
    fun `GIVEN a group WHEN tapping Buscar THEN opens the search over Inicio`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        viewModel.onEvent(HomeUiEvent.OnSearchClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Search), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the recovery code prompt over Inicio WHEN Buscar is asked for THEN nothing stacks over it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.DUE,
                storedGroups = listOf(trio()),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnSearchClicked)

            // Then
            assertTrue(viewModel.uiState.value.showsRecoveryCodePrompt)
            assertEquals(listOf(Route.Home), navigator.backStack.value)
        }

    @Test
    fun `GIVEN no group WHEN tapping Buscar THEN opens the search over Inicio`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(HomeUiEvent.OnSearchClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Search), navigator.backStack.value)
    }

    @Test
    fun `GIVEN only a group where everyone settled WHEN tapping Buscar THEN opens the search over Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "gas",
                        paidBy = "Ana",
                        amountMinor = 6000L,
                        splitOver = listOf("Ana", "Pierre"),
                    ),
                ),
                storedSettlements = listOf(
                    payment(
                        "paid",
                        from = "Pierre",
                        to = "Ana",
                        amountMinor = 3000L,
                    ),
                ),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnSearchClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Search), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the profile over Inicio WHEN a late tap on Buscar arrives THEN the profile stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(HomeUiEvent.OnSearchClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a group opened from Inicio WHEN a late tap on Buscar arrives THEN the group stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(HomeUiEvent.OnGroupClicked("trio"))

            // When
            viewModel.onEvent(HomeUiEvent.OnSearchClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.GroupDetail(
                        "trio",
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the search already open over Inicio WHEN a second tap on Buscar arrives THEN opens no second search`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(HomeUiEvent.OnSearchClicked)

            // When
            viewModel.onEvent(HomeUiEvent.OnSearchClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Search), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a group card WHEN tapping it THEN opens the group over Inicio`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        viewModel.onEvent(HomeUiEvent.OnGroupClicked("trio"))

        // Then
        assertEquals(
            listOf(
                Route.Home,
                Route.GroupDetail(
                    "trio",
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN seven bills of yours and a newer one of others WHEN Inicio opens THEN lists your five latest by day`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    trio(),
                    trio(id = "trip"),
                ),
                storedExpenses = listOf(
                    bill(
                        "others",
                        paidBy = "Ana",
                        splitOver = listOf("Ana", "Bruno"),
                        spentAt = hoursAgo(1),
                    ),
                    bill("h1", spentAt = hoursAgo(2)),
                    bill(
                        "t1",
                        groupId = "trip",
                        spentAt = hoursAgo(3),
                    ),
                    bill("h2", spentAt = hoursAgo(20)),
                    bill(
                        "t2",
                        groupId = "trip",
                        spentAt = hoursAgo(22),
                    ),
                    bill("h3", spentAt = daysAgo(3)),
                    bill(
                        "t3",
                        groupId = "trip",
                        spentAt = daysAgo(4),
                    ),
                    bill("h4", spentAt = daysAgo(5)),
                ),
            )

            // When
            val days = viewModel.uiState.value.billDays

            // Then
            assertEquals(
                listOf(
                    RelativeDay.TODAY to listOf("h1", "t1"),
                    RelativeDay.YESTERDAY to listOf("h2", "t2"),
                    RelativeDay.OTHER to listOf("h3"),
                ),
                days.map { day -> day.relativeDay to day.bills.map { it.id } },
            )
        }

    @Test
    fun `GIVEN a bill of a group category WHEN someone renames it THEN its row on Inicio follows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("meat", categoryId = "barbecue")),
                storedCategories = listOf(barbecue),
            )

            // When
            categories.groupCategories.value = listOf(barbecue.copy(name = "Churras"))

            // Then
            val category = viewModel.uiState.value.billDays
                .single()
                .bills
                .single()
                .category
            assertEquals("Churras", category.name)
            assertEquals(CategoryIcon.GIFT, category.icon)
        }

    @Test
    fun `GIVEN a bill of a group category WHEN someone deletes it THEN its row on Inicio reads Outros`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("meat", categoryId = "barbecue")),
                storedCategories = listOf(barbecue),
            )

            // When
            categories.groupCategories.value = listOf(barbecue.copy(isDeleted = true))

            // Then
            val category = viewModel.uiState.value.billDays
                .single()
                .bills
                .single()
                .category
            assertTrue(category.isOther)
        }

    @Test
    fun `GIVEN a bill on Inicio WHEN tapping it THEN opens its Conta over its group`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = listOf(bill("dinner")),
        )

        // When
        viewModel.onEvent(HomeUiEvent.OnBillClicked("trio", "dinner"))

        // Then
        assertEquals(
            listOf(
                Route.Home,
                Route.GroupDetail(
                    "trio",
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
                Route.BillDetail("trio", "dinner", focusCommentId = null),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a bill from this afternoon on an open Inicio WHEN midnight passes THEN its day says yesterday`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner", spentAt = hoursAgo(2))),
            )

            // When
            clockMillis = Instant.parse("2026-11-01T00:01:00Z").toEpochMilliseconds()
            minutes.value += 1

            // Then
            assertEquals(
                RelativeDay.YESTERDAY,
                viewModel.uiState.value.billDays
                    .single()
                    .relativeDay,
            )
        }

    @Test
    fun `GIVEN the groups not read yet WHEN Inicio opens THEN shows neither the home nor the first use`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(groupsRead = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertFalse(state.showsHome)
            assertFalse(state.showsFirstUse)
        }

    @Test
    fun `GIVEN no group on a local only install WHEN Inicio opens THEN shows the first use`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsFirstUse)
            assertFalse(state.showsHome)
            assertFalse(state.isFirstDownload)
        }

    @Test
    fun `GIVEN an account relaunched with no group WHEN its sync runs THEN shows the first use`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                syncState = SyncState.SYNCING,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isFirstDownload)
            assertTrue(state.showsFirstUse)
        }

    @Test
    fun `GIVEN a first download with no group yet WHEN Inicio opens THEN shows the first download`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                syncState = SyncState.SYNCING,
                isFirstDownloadPending = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isFirstDownload)
            assertTrue(state.showsHome)
            assertFalse(state.showsFirstUse)
        }

    @Test
    fun `GIVEN a restore whose sync has not started WHEN Inicio opens THEN shows the first download`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("pierre", AccountProvider.GOOGLE),
                syncState = SyncState.SYNCED,
                isFirstDownloadPending = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isFirstDownload)
            assertFalse(state.showsFirstUse)
        }

    @Test
    fun `GIVEN a first download with groups already down WHEN Inicio opens THEN shows them as still downloading`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                syncState = SyncState.SYNCING,
                storedGroups = listOf(trio()),
                isFirstDownloadPending = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isFirstDownload)
            assertTrue(state.showsHome)
        }

    @Test
    fun `GIVEN a profile just made here WHEN its first sync registers it THEN shows the first use`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(syncState = SyncState.SYNCING)

            // When
            account.status = AccountStatus.Anonymous("anonymous")

            // Then
            assertFalse(viewModel.uiState.value.isFirstDownload)
            assertTrue(viewModel.uiState.value.showsFirstUse)
        }

    @Test
    fun `GIVEN a Google account downloading WHEN the first download ends with no group THEN shows the first use`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("pierre", AccountProvider.GOOGLE),
                syncState = SyncState.SYNCING,
                isFirstDownloadPending = true,
            )

            // When
            firstDownload.end()

            // Then
            assertFalse(viewModel.uiState.value.isFirstDownload)
            assertTrue(viewModel.uiState.value.showsFirstUse)
        }

    @Test
    fun `GIVEN a Google account downloading WHEN its first group arrives THEN shows the home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("pierre", AccountProvider.GOOGLE),
                syncState = SyncState.SYNCING,
                isFirstDownloadPending = true,
            )

            // When
            groups.groups.value = listOf(trio())

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsHome)
            assertFalse(state.showsFirstUse)
            assertTrue(state.isFirstDownload)
            assertEquals(1, state.groupCount)
        }

    @Test
    fun `GIVEN a first download just started WHEN Inicio opens THEN counts no group out of an unknown number`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("pierre", AccountProvider.GOOGLE),
                syncState = SyncState.SYNCING,
                isFirstDownloadPending = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                DownloadProgress(
                    arrivedGroupCount = 0,
                    groupCount = null,
                    isWaitingForInternet = false,
                ),
                state.firstDownload,
            )
        }

    @Test
    fun `GIVEN 2 of 7 groups down WHEN the third one arrives THEN counts 3 of 7`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = AccountStatus.Connected("pierre", AccountProvider.GOOGLE),
            syncState = SyncState.SYNCING,
            isFirstDownloadPending = true,
        )
        firstDownload.expect(7)
        firstDownload.groupArrived()
        firstDownload.groupArrived()

        // When
        firstDownload.groupArrived()

        // Then
        assertEquals(
            DownloadProgress(
                arrivedGroupCount = 3,
                groupCount = 7,
                isWaitingForInternet = false,
            ),
            viewModel.uiState.value.firstDownload,
        )
        assertTrue(viewModel.uiState.value.isFirstDownload)
    }

    @Test
    fun `GIVEN a first download with groups already down WHEN it ends THEN shows the home without the progress`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("pierre", AccountProvider.GOOGLE),
                syncState = SyncState.SYNCING,
                storedGroups = listOf(trio()),
                isFirstDownloadPending = true,
            )

            // When
            firstDownload.end()

            // Then
            val state = viewModel.uiState.value
            assertNull(state.firstDownload)
            assertFalse(state.isFirstDownload)
            assertTrue(state.showsHome)
            assertFalse(state.showsFirstUse)
        }

    @Test
    fun `GIVEN a restore whose full sync never finished WHEN Inicio opens offline THEN waits for the internet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                status = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE),
                awaitsFullSync = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                DownloadProgress(
                    arrivedGroupCount = 0,
                    groupCount = null,
                    isWaitingForInternet = true,
                ),
                state.firstDownload,
            )
            assertNull(state.firstDownload?.fraction)
            assertTrue(state.isFirstDownload)
            assertFalse(state.showsFirstUse)
        }

    @Test
    fun `GIVEN a full sync awaited offline WHEN the internet comes back THEN the download no longer waits`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                status = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE),
                awaitsFullSync = true,
            )

            // When
            isOnline.value = true

            // Then
            assertEquals(
                DownloadProgress(
                    arrivedGroupCount = 0,
                    groupCount = null,
                    isWaitingForInternet = false,
                ),
                viewModel.uiState.value.firstDownload,
            )
        }

    @Test
    fun `GIVEN an account relaunched with its groups WHEN Inicio opens THEN shows no first download`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("pierre", AccountProvider.GOOGLE),
                syncState = SyncState.SYNCING,
                storedGroups = listOf(trio()),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.firstDownload)
            assertTrue(state.showsHome)
        }

    @Test
    fun `GIVEN a profile and a Google account WHEN Inicio opens THEN shows the trimmed name and the account`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("pierre", AccountProvider.GOOGLE),
                profile = UserProfile("  pierre ", AvatarColor.GREEN, null),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("pierre", state.displayName)
            assertEquals("P", state.avatarInitial)
            assertEquals(AvatarColor.GREEN, state.avatarColor)
            assertEquals(AccountProvider.GOOGLE, state.accountProvider)
        }

    @Test
    fun `GIVEN everything synced WHEN Inicio opens THEN shows no sync badge`() = runTest(testDispatcher) {
        // Given
        prepareScenario(syncState = SyncState.SYNCED)

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.syncBadge)
    }

    @Test
    fun `GIVEN no internet and changes in the queue WHEN Inicio opens THEN shows the offline badge counting them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                syncState = SyncState.PENDING,
                storedGroups = listOf(trio().copy(isPending = true)),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        isPending = true,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SyncBadge.OFFLINE, state.syncBadge)
            assertEquals(2, state.pendingChangeCount)
        }

    @Test
    fun `GIVEN internet and no answer from the server WHEN Inicio opens THEN shows the failed badge to retry`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(syncState = SyncState.FAILED)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SyncBadge.FAILED, state.syncBadge)
            assertTrue(state.canRetrySync)
        }

    @Test
    fun `GIVEN internet and a change in the queue WHEN Inicio opens THEN shows the syncing badge`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(syncState = SyncState.PENDING)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SyncBadge.SYNCING, state.syncBadge)
            assertFalse(state.canRetrySync)
        }

    @Test
    fun `GIVEN the failed badge WHEN tapping it THEN asks for one sync`() = runTest(testDispatcher) {
        // Given
        prepareScenario(syncState = SyncState.FAILED)

        // When
        viewModel.onEvent(HomeUiEvent.OnRetrySyncClicked)

        // Then
        assertEquals(1, retryCount)
    }

    @Test
    fun `GIVEN a sync running WHEN a late tap on retry arrives THEN asks for no sync`() = runTest(testDispatcher) {
        // Given
        prepareScenario(syncState = SyncState.SYNCING)

        // When
        viewModel.onEvent(HomeUiEvent.OnRetrySyncClicked)

        // Then
        assertEquals(0, retryCount)
    }

    @Test
    fun `GIVEN online WHEN pulling to refresh THEN syncs once and waits for it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))
        syncGate = CompletableDeferred()

        // When
        viewModel.onEvent(HomeUiEvent.OnRefresh)
        viewModel.onEvent(HomeUiEvent.OnRefresh)

        // Then
        assertTrue(viewModel.uiState.value.isRefreshing)
        assertEquals(1, syncNowCount)
        syncGate?.complete(Unit)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `GIVEN offline WHEN pulling to refresh THEN ends at once without a sync`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            online = false,
            storedGroups = listOf(trio()),
        )

        // When
        viewModel.onEvent(HomeUiEvent.OnRefresh)

        // Then
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals(0, syncNowCount)
    }

    @Test
    fun `GIVEN an anonymous account that snoozed its code WHEN Inicio opens THEN shows the card without the sheet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.SNOOZED,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsRecoveryCodeCard)
            assertFalse(state.showsRecoveryCodePrompt)
        }

    @Test
    fun `GIVEN a Google account with a snoozed code WHEN Inicio opens THEN shows no card`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = AccountStatus.Connected("pierre", AccountProvider.GOOGLE),
            prompt = RecoveryCodePrompt.SNOOZED,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.showsRecoveryCodeCard)
    }

    @Test
    fun `GIVEN a local only install with a snoozed code WHEN Inicio opens THEN shows no card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(prompt = RecoveryCodePrompt.SNOOZED)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.showsRecoveryCodeCard)
        }

    @Test
    fun `GIVEN an anonymous account with a due code WHEN Inicio opens THEN shows the sheet without the card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.DUE,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsRecoveryCodePrompt)
            assertFalse(state.showsRecoveryCodeCard)
        }

    @Test
    fun `GIVEN a saved code opened for review WHEN Inicio shows THEN shows the sheet`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = AccountStatus.Anonymous("anonymous"),
            prompt = RecoveryCodePrompt.REVIEW,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.showsRecoveryCodePrompt)
    }

    @Test
    fun `GIVEN the recovery code card WHEN closing it THEN hides it and keeps the code snoozed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.SNOOZED,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnRecoveryCodeCardDismissed)

            // Then
            assertFalse(viewModel.uiState.value.showsRecoveryCodeCard)
            assertEquals(RecoveryCodePrompt.SNOOZED, account.recoveryCodePrompt)
        }

    @Test
    fun `GIVEN the recovery code card closed earlier in this launch WHEN Inicio opens again THEN it stays closed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.SNOOZED,
                isCardDismissedThisLaunch = true,
            )

            // When
            val showsCard = viewModel.uiState.value.showsRecoveryCodeCard

            // Then
            assertFalse(showsCard)
        }

    @Test
    fun `GIVEN two groups someone else deleted WHEN Inicio is on top THEN says so one at a time`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            deletions.post(
                GroupDeletion(
                    groupId = "a",
                    groupName = "Apê 302",
                    deletedByName = "Carla",
                    failure = null,
                ),
            )
            deletions.post(
                GroupDeletion(
                    groupId = "b",
                    groupName = "Churrasco",
                    deletedByName = null,
                    failure = null,
                ),
            )

            // Then
            assertEquals(
                DeletedGroupNotice(
                    groupId = "a",
                    groupName = "Apê 302",
                    deletedByName = "Carla",
                    failure = null,
                ),
                viewModel.uiState.value.deletedGroupNotice,
            )
            viewModel.onEvent(HomeUiEvent.OnDeletedGroupNoticeShown)
            assertEquals(
                DeletedGroupNotice(
                    groupId = "b",
                    groupName = "Churrasco",
                    deletedByName = null,
                    failure = null,
                ),
                viewModel.uiState.value.deletedGroupNotice,
            )
            viewModel.onEvent(HomeUiEvent.OnDeletedGroupNoticeShown)
            assertNull(viewModel.uiState.value.deletedGroupNotice)
            assertTrue(deletions.observeDeletions().first().isEmpty())
        }

    @Test
    fun `GIVEN the profile over Inicio WHEN a group is deleted THEN says so once Inicio is on top again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(Route.Profile)
            deletions.post(
                GroupDeletion(
                    groupId = "a",
                    groupName = "Apê 302",
                    deletedByName = "Carla",
                    failure = null,
                ),
            )
            assertNull(viewModel.uiState.value.deletedGroupNotice)

            // When
            navigator.navigateBack()

            // Then
            assertEquals(
                "Apê 302",
                viewModel.uiState.value.deletedGroupNotice
                    ?.groupName,
            )
        }

    @Test
    fun `GIVEN the recovery code card WHEN tapping Ver codigo THEN makes the code due and shows the sheet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.SNOOZED,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnShowRecoveryCodeClicked)

            // Then
            assertEquals(RecoveryCodePrompt.DUE, account.recoveryCodePrompt)
            assertTrue(viewModel.uiState.value.showsRecoveryCodePrompt)
            assertFalse(viewModel.uiState.value.showsRecoveryCodeCard)
        }

    @Test
    fun `GIVEN no recovery code card WHEN a late tap on Ver codigo arrives THEN leaves the code as it is`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.SAVED,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnShowRecoveryCodeClicked)

            // Then
            assertEquals(RecoveryCodePrompt.SAVED, account.recoveryCodePrompt)
        }

    @Test
    fun `GIVEN Registrar pagamento opened from the shortcut WHEN it closes after recording THEN Inicio says so`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(Route.PickPayment)
            navigator.replaceTop(registerPayment)
            val paid = payment(
                "paid",
                from = "Ana",
                to = "Pierre",
                amountMinor = 3000L,
            )
            settlements.settlements.update { it + paid }
            recordedPayments.post(RecordedPayment.InGroup(paid))

            // When
            navigator.navigateBack()

            // Then
            assertEquals(
                PaymentNotice(
                    settlementIds = listOf("paid"),
                    fromName = "Ana",
                    fromIsMe = false,
                    toName = "Pierre",
                ),
                viewModel.uiState.value.recordedPayment,
            )
        }

    @Test
    fun `GIVEN Registrar pagamento opened from a group WHEN back on Inicio THEN Inicio shows no notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(
                Route.GroupDetail(
                    "trio",
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
            )
            navigator.navigate(registerPayment)
            recordedPayments.post(
                RecordedPayment.InGroup(
                    payment(
                        "paid",
                        from = "Ana",
                        to = "Pierre",
                        amountMinor = 3000L,
                    ),
                ),
            )

            // When
            navigator.navigateBack()
            navigator.navigateBack()

            // Then
            assertNull(viewModel.uiState.value.recordedPayment)
        }

    @Test
    fun `GIVEN a settling up opened from a face WHEN it closes after recording THEN Inicio says so`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(Route.SettleUp("name:ana", GroupCurrency.BRL))
            recordedPayments.post(
                RecordedPayment.SettledUp(
                    listOf("p1", "p2"),
                    personName = "Ana",
                    isPaidByMe = true,
                ),
            )

            // When
            navigator.navigateBack()

            // Then
            assertEquals(
                PaymentNotice(
                    settlementIds = listOf("p1", "p2"),
                    fromName = null,
                    fromIsMe = true,
                    toName = "Ana",
                ),
                viewModel.uiState.value.recordedPayment,
            )
        }

    @Test
    fun `GIVEN the notice of a settling up WHEN tapping Desfazer THEN undoes all its payments and hides it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedSettlements = listOf(
                    payment(
                        "p1",
                        from = "Pierre",
                        to = "Ana",
                        amountMinor = 100L,
                    ),
                    payment(
                        "p2",
                        from = "Pierre",
                        to = "Ana",
                        amountMinor = 200L,
                    ),
                ),
            )
            recordedPayments.post(
                RecordedPayment.SettledUp(
                    listOf("p1", "p2"),
                    personName = "Ana",
                    isPaidByMe = true,
                ),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnUndoPaymentClicked)

            // Then
            assertEquals(listOf(listOf("p1", "p2")), settlements.deleteCalls)
            assertNull(viewModel.uiState.value.recordedPayment)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN undoing fails WHEN tapping Desfazer THEN says the change failed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))
        recordedPayments.post(
            RecordedPayment.SettledUp(
                listOf("p1"),
                personName = "Ana",
                isPaidByMe = true,
            ),
        )
        settlements.deleteError = IllegalStateException("disk full")

        // When
        viewModel.onEvent(HomeUiEvent.OnUndoPaymentClicked)

        // Then
        assertEquals(listOf<HomeUiAction>(HomeUiAction.ShowChangeError), actions)
    }

    @Test
    fun `GIVEN no notice WHEN a late tap on Desfazer arrives THEN deletes nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        viewModel.onEvent(HomeUiEvent.OnUndoPaymentClicked)

        // Then
        assertTrue(settlements.deleteCalls.isEmpty())
    }

    @Test
    fun `GIVEN the notice WHEN it was shown THEN Inicio clears it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))
        recordedPayments.post(
            RecordedPayment.SettledUp(
                listOf("p1"),
                personName = "Ana",
                isPaidByMe = true,
            ),
        )

        // When
        viewModel.onEvent(HomeUiEvent.OnRecordedPaymentNoticeShown)

        // Then
        assertNull(viewModel.uiState.value.recordedPayment)
    }

    @Test
    fun `GIVEN Home WHEN tapping the logo THEN tracks brand_clicked from the home`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(HomeUiEvent.OnBrandClicked)

        // Then
        assertEquals(listOf("brand_clicked" to mapOf<String, Any>("source" to "home")), trackedEvents)
    }

    @Test
    fun `GIVEN Inicio WHEN tapping the avatar THEN tracks profile_avatar_clicked from the home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(HomeUiEvent.OnProfileClicked)

            // Then
            assertEquals(listOf("profile_avatar_clicked" to mapOf<String, Any>("source" to "home")), trackedEvents)
        }

    @Test
    fun `GIVEN Inicio WHEN tapping Buscar THEN tracks search_opened from the home`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        viewModel.onEvent(HomeUiEvent.OnSearchClicked)

        // Then
        assertEquals(listOf("search_opened" to mapOf<String, Any>("source" to "home")), trackedEvents)
    }

    @Test
    fun `GIVEN the recovery code prompt over Inicio WHEN Buscar is asked for THEN tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.DUE,
                storedGroups = listOf(trio()),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnSearchClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN no group WHEN tapping Buscar THEN tracks search_opened from the home`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(HomeUiEvent.OnSearchClicked)

        // Then
        assertEquals(listOf("search_opened" to mapOf<String, Any>("source" to "home")), trackedEvents)
    }

    @Test
    fun `GIVEN the profile over Inicio WHEN a late tap on Buscar arrives THEN tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(HomeUiEvent.OnSearchClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the search already open over Inicio WHEN a second tap on Buscar arrives THEN tracks only the first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(HomeUiEvent.OnSearchClicked)

            // When
            viewModel.onEvent(HomeUiEvent.OnSearchClicked)

            // Then
            assertEquals(listOf("search_opened" to mapOf<String, Any>("source" to "home")), trackedEvents)
        }

    @Test
    fun `GIVEN Inicio WHEN tapping tentar de novo THEN tracks sync_retry_clicked from the home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(syncState = SyncState.FAILED)

            // When
            viewModel.onEvent(HomeUiEvent.OnRetrySyncClicked)

            // Then
            assertEquals(listOf("sync_retry_clicked" to mapOf<String, Any>("source" to "home")), trackedEvents)
        }

    @Test
    fun `GIVEN Inicio WHEN pulling down THEN tracks pull_to_refresh from the home`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(HomeUiEvent.OnRefresh)

        // Then
        assertEquals(listOf("pull_to_refresh" to mapOf<String, Any>("source" to "home")), trackedEvents)
    }

    @Test
    fun `GIVEN Inicio WHEN tapping Te devem THEN tracks balances_to_receive_clicked from the home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(HomeUiEvent.OnToReceiveClicked)

            // Then
            assertEquals(listOf("balances_to_receive_clicked" to mapOf<String, Any>("source" to "home")), trackedEvents)
        }

    @Test
    fun `GIVEN Inicio WHEN tapping Voce deve THEN tracks balances_to_pay_clicked from the home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(HomeUiEvent.OnToPayClicked)

            // Then
            assertEquals(listOf("balances_to_pay_clicked" to mapOf<String, Any>("source" to "home")), trackedEvents)
        }

    @Test
    fun `GIVEN Inicio WHEN tapping Rachar conta THEN tracks split_bill_clicked from the home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            viewModel.onEvent(HomeUiEvent.OnSplitBillClicked)

            // Then
            assertEquals(listOf("split_bill_clicked" to mapOf<String, Any>("source" to "home")), trackedEvents)
        }

    @Test
    fun `GIVEN Inicio WHEN tapping Ler cupom fiscal THEN tracks scan_receipt_clicked from the home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)

            // Then
            assertEquals(listOf("scan_receipt_clicked" to mapOf<String, Any>("source" to "home")), trackedEvents)
        }

    @Test
    fun `GIVEN Inicio WHEN tapping Registrar pagamento THEN tracks record_payment_clicked from the home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            viewModel.onEvent(HomeUiEvent.OnRecordPaymentClicked)

            // Then
            assertEquals(listOf("record_payment_clicked" to mapOf<String, Any>("source" to "home")), trackedEvents)
        }

    @Test
    fun `GIVEN Inicio WHEN tapping Novo grupo THEN tracks create_group_clicked from the home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(HomeUiEvent.OnCreateGroupClicked)

            // Then
            assertEquals(listOf("create_group_clicked" to mapOf<String, Any>("source" to "home")), trackedEvents)
        }

    @Test
    fun `GIVEN Inicio WHEN tapping Entrar com link ou QR THEN tracks join_with_link_clicked from the home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(HomeUiEvent.OnJoinWithLinkClicked)

            // Then
            assertEquals(listOf("join_with_link_clicked" to mapOf<String, Any>("source" to "home")), trackedEvents)
        }

    @Test
    fun `GIVEN Inicio WHEN tapping Desfazer THEN tracks payment_undo_clicked from the home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(HomeUiEvent.OnUndoPaymentClicked)

            // Then
            assertEquals(listOf("payment_undo_clicked" to mapOf<String, Any>("source" to "home")), trackedEvents)
        }

    @Test
    fun `GIVEN someone owes you WHEN tapping their face THEN tracks balance_face_clicked with only the source`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill(id = "market")),
            )

            // When
            viewModel.onEvent(
                HomeUiEvent.OnPersonClicked(
                    key = "member-Ana",
                    currency = GroupCurrency.BRL,
                ),
            )

            // Then
            assertEquals(listOf("balance_face_clicked" to mapOf<String, Any>("source" to "home")), trackedEvents)
        }

    @Test
    fun `GIVEN a group in Seus grupos WHEN tapping its card THEN tracks group_clicked with only the source`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            viewModel.onEvent(HomeUiEvent.OnGroupClicked("trio"))

            // Then
            assertEquals(listOf("group_clicked" to mapOf<String, Any>("source" to "home")), trackedEvents)
        }

    @Test
    fun `GIVEN a bill in Ultimas despesas WHEN tapping it THEN tracks bill_clicked with only the source`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill(id = "market")),
            )

            // When
            viewModel.onEvent(
                HomeUiEvent.OnBillClicked(
                    groupId = "trio",
                    expenseId = "market",
                ),
            )

            // Then
            assertEquals(listOf("bill_clicked" to mapOf<String, Any>("source" to "home")), trackedEvents)
        }

    @Test
    fun `GIVEN Inicio WHEN tapping Ver saldos THEN tracks home_see_balances_clicked with no params`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(HomeUiEvent.OnSeeBalancesClicked)

            // Then
            assertEquals(listOf("home_see_balances_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN Inicio WHEN tapping Ver todos THEN tracks home_see_all_groups_clicked with no params`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            viewModel.onEvent(HomeUiEvent.OnSeeAllGroupsClicked)

            // Then
            assertEquals(listOf("home_see_all_groups_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN Inicio WHEN tapping Ver tudo THEN tracks home_see_all_bills_clicked with no params`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            viewModel.onEvent(HomeUiEvent.OnSeeAllBillsClicked)

            // Then
            assertEquals(
                listOf("home_see_all_bills_clicked" to emptyMap<String, Any>()),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Inicio WHEN tapping the bell THEN tracks activity_bell_clicked with no params`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(HomeUiEvent.OnActivityClicked)

            // Then
            assertEquals(listOf("activity_bell_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN Inicio WHEN tapping the bell twice quickly THEN tracks activity_bell_clicked once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(HomeUiEvent.OnActivityClicked)
            viewModel.onEvent(HomeUiEvent.OnActivityClicked)

            // Then
            assertEquals(listOf("activity_bell_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN a group opened from Inicio WHEN a late tap on the bell arrives THEN tracks no bell tap`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(HomeUiEvent.OnGroupClicked("trio"))

            // When
            viewModel.onEvent(HomeUiEvent.OnActivityClicked)

            // Then
            assertEquals(listOf("group_clicked" to mapOf<String, Any>("source" to "home")), trackedEvents)
        }

    @Test
    fun `GIVEN the bell of Inicio WHEN reading its analytics THEN the ViewModel sends activity_bell_clicked`() {
        // Given
        val event = HomeUiEvent.OnActivityClicked

        // When
        val analytics = event.analytics

        // Then
        assertEquals(EventAnalytics.Manual("activity_bell_clicked"), analytics)
    }

    @Test
    fun `GIVEN the recovery code card WHEN tapping Ver codigo THEN tracks the card show tap with no params`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.SNOOZED,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnShowRecoveryCodeClicked)

            // Then
            assertEquals(
                listOf("recovery_code_card_show_clicked" to emptyMap<String, Any>()),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the recovery code card WHEN tapping Conectar conta THEN tracks the card connect tap with no params`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.SNOOZED,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnConnectAccountClicked)

            // Then
            assertEquals(listOf("recovery_code_card_connect_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN the recovery code card WHEN closing it THEN tracks the card dismissed with no params`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.SNOOZED,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnRecoveryCodeCardDismissed)

            // Then
            assertEquals(listOf("recovery_code_card_dismissed" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN Inicio WHEN the payment and deleted group notices go away THEN tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(HomeUiEvent.OnRecordedPaymentNoticeShown)
            viewModel.onEvent(HomeUiEvent.OnDeletedGroupNoticeShown)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Inicio WHEN tapping each other home tab THEN sends nothing since where it went is its screen view`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(HomeUiEvent.OnGroupsTabClicked)
            viewModel.onEvent(HomeUiEvent.OnBalancesTabClicked)
            viewModel.onEvent(HomeUiEvent.OnContactsTabClicked)

            // Then
            assertEquals(emptyList(), trackedEvents)
        }

    @Test
    fun `GIVEN a group and notifications never asked WHEN the platform reports it THEN shows the soft ask card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )

            // Then
            assertEquals(NotificationPrompt.SOFT_ASK, viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN a group and notifications refused once WHEN the platform reports it THEN shows the ask again card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.REFUSED_ONCE, canAskQuietly = false),
            )

            // Then
            assertEquals(NotificationPrompt.ASK_AGAIN, viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN a group and notifications denied WHEN the platform reports it THEN shows the blocked card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.DENIED, canAskQuietly = false),
            )

            // Then
            assertEquals(NotificationPrompt.BLOCKED, viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN a group and notifications allowed WHEN the platform reports it THEN shows no notification card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.GRANTED, canAskQuietly = false),
            )

            // Then
            assertNull(viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN a group WHEN Inicio opens before the platform reports the permission THEN shows no notification card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.notificationPermission)
            assertNull(state.notificationPrompt)
        }

    @Test
    fun `GIVEN the first use with no group WHEN notifications were never asked THEN shows no notification card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )

            // Then
            assertTrue(viewModel.uiState.value.showsFirstUse)
            assertNull(viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN the first use WHEN the first group arrives THEN shows the soft ask card`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(
            HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.NOT_DETERMINED, canAskQuietly = false),
        )

        // When
        groups.groups.value = listOf(trio())

        // Then
        assertEquals(NotificationPrompt.SOFT_ASK, viewModel.uiState.value.notificationPrompt)
    }

    @Test
    fun `GIVEN a first download with a group already down WHEN notifications were never asked THEN shows no card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("pierre", AccountProvider.GOOGLE),
                syncState = SyncState.SYNCING,
                storedGroups = listOf(trio()),
                isFirstDownloadPending = true,
            )

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )

            // Then
            assertTrue(viewModel.uiState.value.isFirstDownload)
            assertNull(viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN a first download with a group WHEN it ends THEN shows the soft ask card`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = AccountStatus.Connected("pierre", AccountProvider.GOOGLE),
            syncState = SyncState.SYNCING,
            storedGroups = listOf(trio()),
            isFirstDownloadPending = true,
        )
        viewModel.onEvent(
            HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.NOT_DETERMINED, canAskQuietly = false),
        )

        // When
        firstDownload.end()

        // Then
        assertEquals(NotificationPrompt.SOFT_ASK, viewModel.uiState.value.notificationPrompt)
    }

    @Test
    fun `GIVEN the recovery code card of an anonymous account WHEN notifications were never asked THEN it wins`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.SNOOZED,
                storedGroups = listOf(trio()),
            )

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )

            // Then
            assertTrue(viewModel.uiState.value.showsRecoveryCodeCard)
            assertNull(viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN the recovery code card over the notification card WHEN closing it THEN shows the soft ask card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.SNOOZED,
                storedGroups = listOf(trio()),
            )
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnRecoveryCodeCardDismissed)

            // Then
            assertEquals(NotificationPrompt.SOFT_ASK, viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN a Google account offline with a group WHEN never asked THEN shows the soft ask card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                status = AccountStatus.Connected("pierre", AccountProvider.GOOGLE),
                storedGroups = listOf(trio()),
            )

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )

            // Then
            assertEquals(NotificationPrompt.SOFT_ASK, viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN the card answered 20 days ago WHEN notifications were never asked THEN shows no notification card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                notificationPromptAnsweredAt = daysAgo(20),
            )

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )

            // Then
            assertTrue(viewModel.uiState.value.isNotificationPromptSnoozed)
            assertNull(viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN the card answered a minute short of 21 days ago WHEN Inicio opens THEN shows no notification card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                notificationPromptAnsweredAt = now - 21.days.inWholeMilliseconds + oneMinute,
            )

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )

            // Then
            assertNull(viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN the card answered exactly 21 days ago WHEN notifications were never asked THEN shows the card again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                notificationPromptAnsweredAt = daysAgo(21),
            )

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )

            // Then
            assertEquals(NotificationPrompt.SOFT_ASK, viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN the card snoozed a minute short of 21 days WHEN that minute passes THEN shows it again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                notificationPromptAnsweredAt = now - 21.days.inWholeMilliseconds + oneMinute,
            )
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )

            // When
            clockMillis = now + oneMinute
            minutes.value += 1

            // Then
            assertEquals(NotificationPrompt.SOFT_ASK, viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN Not now tapped on an open Inicio WHEN three weeks pass THEN shows the card again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptDismissed)

            // When
            clockMillis = now + 21.days.inWholeMilliseconds
            minutes.value += 1

            // Then
            assertEquals(NotificationPrompt.SOFT_ASK, viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN the card answered at a time after the clock now WHEN Inicio opens THEN still shows the card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                notificationPromptAnsweredAt = now + 1.days.inWholeMilliseconds,
            )

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )

            // Then
            assertFalse(viewModel.uiState.value.isNotificationPromptSnoozed)
            assertEquals(NotificationPrompt.SOFT_ASK, viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN the last answer not read yet from the device WHEN notifications were never asked THEN shows no card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                notificationPromptTimeRead = false,
            )

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )

            // Then
            assertTrue(viewModel.uiState.value.isNotificationPromptSnoozed)
            assertNull(viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN the soft ask card WHEN tapping Turn on THEN turns the push switch on`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))
        viewModel.onEvent(
            HomeUiEvent.OnNotificationPermissionChecked(
                NotificationPermission.NOT_DETERMINED,
                canAskQuietly = false,
            ),
        )

        // When
        viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)

        // Then
        assertEquals(listOf(true), pushSwitchWrites)
    }

    @Test
    fun `GIVEN the soft ask card WHEN tapping Not now THEN leaves the push switch as it is`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptDismissed)

            // Then
            assertTrue(pushSwitchWrites.isEmpty())
        }

    @Test
    fun `GIVEN the soft ask card WHEN tapping Turn on THEN asks the system for the permission`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)

            // Then
            assertEquals(listOf<HomeUiAction>(HomeUiAction.RequestNotificationPermission), actions)
        }

    @Test
    fun `GIVEN the ask again card WHEN tapping Turn on THEN asks the system for the permission`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.REFUSED_ONCE, canAskQuietly = false),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)

            // Then
            assertEquals(listOf<HomeUiAction>(HomeUiAction.RequestNotificationPermission), actions)
        }

    @Test
    fun `GIVEN the blocked card WHEN tapping Turn on THEN opens the notification settings`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))
        viewModel.onEvent(
            HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.DENIED, canAskQuietly = false),
        )

        // When
        viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)

        // Then
        assertEquals(listOf<HomeUiAction>(HomeUiAction.OpenNotificationSettings), actions)
    }

    @Test
    fun `GIVEN the soft ask card WHEN tapping Turn on THEN tracks the turn on tap with the soft ask`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )

            // Why: the card's showing has its own tests.
            trackedEvents.clear()

            // When
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)

            // Then
            assertEquals(
                listOf(
                    "notification_prompt_turn_on_clicked" to mapOf<String, Any>(
                        "prompt" to NotificationPrompt.SOFT_ASK,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the blocked card WHEN tapping Turn on THEN tracks the turn on tap with the blocked card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.DENIED, canAskQuietly = false),
            )

            // Why: the card's showing has its own tests.
            trackedEvents.clear()

            // When
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)

            // Then
            assertEquals(
                listOf(
                    "notification_prompt_turn_on_clicked" to mapOf<String, Any>("prompt" to NotificationPrompt.BLOCKED),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the soft ask card WHEN tapping Turn on THEN hides the card and stores now as its last answer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)

            // Then
            assertNull(viewModel.uiState.value.notificationPrompt)
            assertEquals(listOf(now), notificationPromptWrites)
        }

    @Test
    fun `GIVEN the soft ask card WHEN tapping Turn on twice quickly THEN acts tracks and stores only once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )

            // Why: the card's showing has its own tests.
            trackedEvents.clear()

            // When
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)

            // Then
            assertEquals(listOf<HomeUiAction>(HomeUiAction.RequestNotificationPermission), actions)
            assertEquals(1, trackedEvents.size)
            assertEquals(listOf(now), notificationPromptWrites)
        }

    @Test
    fun `GIVEN the ask again card WHEN tapping Not now THEN tracks the dismissal and asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.REFUSED_ONCE, canAskQuietly = false),
            )

            // Why: the card's showing has its own tests.
            trackedEvents.clear()

            // When
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptDismissed)

            // Then
            assertEquals(
                listOf("notification_prompt_dismissed" to mapOf<String, Any>("prompt" to NotificationPrompt.ASK_AGAIN)),
                trackedEvents,
            )
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the blocked card WHEN tapping Not now THEN hides the card and stores now as its last answer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.DENIED, canAskQuietly = false),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptDismissed)

            // Then
            assertNull(viewModel.uiState.value.notificationPrompt)
            assertTrue(viewModel.uiState.value.isNotificationPromptSnoozed)
            assertEquals(listOf(now), notificationPromptWrites)
        }

    @Test
    fun `GIVEN the soft ask card WHEN tapping Not now twice quickly THEN tracks and stores only once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )

            // Why: the card's showing has its own tests.
            trackedEvents.clear()

            // When
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptDismissed)
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptDismissed)

            // Then
            assertEquals(1, trackedEvents.size)
            assertEquals(listOf(now), notificationPromptWrites)
        }

    @Test
    fun `GIVEN Not now tapped WHEN a late tap on Turn on arrives THEN asks tracks and stores nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )
            // Why: the card's showing has its own tests.
            trackedEvents.clear()
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptDismissed)

            // When
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)

            // Then
            assertTrue(actions.isEmpty())
            assertEquals(listOf("notification_prompt_dismissed"), trackedEvents.map { it.first })
            assertEquals(listOf(now), notificationPromptWrites)
        }

    @Test
    fun `GIVEN notifications allowed WHEN a late tap on Turn on arrives THEN asks tracks and stores nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.GRANTED, canAskQuietly = false),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
            assertTrue(notificationPromptWrites.isEmpty())
        }

    @Test
    fun `GIVEN notifications allowed WHEN a late tap on Not now arrives THEN tracks nothing and stores nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.GRANTED, canAskQuietly = false),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptDismissed)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
            assertTrue(notificationPromptWrites.isEmpty())
        }

    @Test
    fun `GIVEN the permission not reported yet WHEN tapping Turn on THEN asks tracks and stores nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
            assertTrue(notificationPromptWrites.isEmpty())
        }

    @Test
    fun `GIVEN the card snoozed WHEN a tap on Turn on arrives THEN asks tracks and stores nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                notificationPromptAnsweredAt = daysAgo(3),
            )
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
            assertTrue(notificationPromptWrites.isEmpty())
        }

    @Test
    fun `GIVEN the first use with no group WHEN a tap on Not now arrives THEN tracks nothing and stores nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptDismissed)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
            assertTrue(notificationPromptWrites.isEmpty())
        }

    @Test
    fun `GIVEN Turn on tapped on the soft ask WHEN the system prompt allows them THEN says notifications are on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.GRANTED, canAskQuietly = false),
            )

            // Then
            assertEquals(
                listOf(HomeUiAction.RequestNotificationPermission, HomeUiAction.ShowNotificationsOn),
                actions,
            )
        }

    @Test
    fun `GIVEN Turn on on the blocked card WHEN back from the settings allowed THEN says notifications are on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.DENIED, canAskQuietly = false),
            )
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.GRANTED, canAskQuietly = false),
            )

            // Then
            assertEquals(
                listOf(HomeUiAction.OpenNotificationSettings, HomeUiAction.ShowNotificationsOn),
                actions,
            )
            assertNull(viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN notifications denied and no Turn on WHEN another screen gets them allowed THEN says nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.DENIED, canAskQuietly = false),
            )

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.GRANTED, canAskQuietly = false),
            )

            // Then
            assertTrue(actions.isEmpty())
            assertNull(viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN Turn on then notifications on WHEN they turn off and on again elsewhere THEN says it only once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.GRANTED, canAskQuietly = false),
            )

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.DENIED, canAskQuietly = false),
            )
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.GRANTED, canAskQuietly = false),
            )

            // Then
            assertEquals(
                listOf(HomeUiAction.RequestNotificationPermission, HomeUiAction.ShowNotificationsOn),
                actions,
            )
        }

    @Test
    fun `GIVEN Turn on tapped after a first refusal WHEN the system allows them THEN says notifications are on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.REFUSED_ONCE, canAskQuietly = false),
            )
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.GRANTED, canAskQuietly = false),
            )

            // Then
            assertEquals(
                listOf(HomeUiAction.RequestNotificationPermission, HomeUiAction.ShowNotificationsOn),
                actions,
            )
        }

    @Test
    fun `GIVEN Inicio just opened WHEN the first report says notifications are allowed THEN says nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            val granted = HomeUiEvent.OnNotificationPermissionChecked(
                NotificationPermission.GRANTED,
                canAskQuietly = false,
            )

            // When
            viewModel.onEvent(granted)

            // Then
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN notifications allowed WHEN a return to the app reports them allowed again THEN says nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.GRANTED, canAskQuietly = false),
            )

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.GRANTED, canAskQuietly = false),
            )

            // Then
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the soft ask card WHEN the system prompt refuses them THEN says nothing and shows the blocked card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(
                    NotificationPermission.NOT_DETERMINED,
                    canAskQuietly = false,
                ),
            )

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.DENIED, canAskQuietly = false),
            )

            // Then
            assertTrue(actions.isEmpty())
            assertEquals(NotificationPrompt.BLOCKED, viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN notifications allowed WHEN the settings turn them off THEN says nothing and shows the blocked card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.GRANTED, canAskQuietly = false),
            )

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.DENIED, canAskQuietly = false),
            )

            // Then
            assertTrue(actions.isEmpty())
            assertEquals(NotificationPrompt.BLOCKED, viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN Inicio WHEN the platform reports the notification permission THEN tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            viewModel.onEvent(
                HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.GRANTED, canAskQuietly = false),
            )

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the notification events WHEN reading their analytics THEN the ViewModel sends the card answers`() {
        // Given
        val events = listOf(
            HomeUiEvent.OnNotificationPromptTurnOnClicked,
            HomeUiEvent.OnNotificationPromptDismissed,
            HomeUiEvent.OnNotificationPermissionChecked(NotificationPermission.GRANTED, canAskQuietly = false),
        )

        // When
        val analytics = events.map { it.analytics }

        // Then
        assertEquals(
            listOf(
                EventAnalytics.Manual("notification_prompt_turn_on_clicked"),
                EventAnalytics.Manual("notification_prompt_dismissed"),
                EventAnalytics.Manual(setOf("notification_permission_answered", "notification_prompt_shown")),
            ),
            analytics,
        )
    }

    @Test
    fun `GIVEN a group on Apple WHEN the platform reports notifications never asked THEN asks to deliver quietly`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))

            // Then
            assertEquals(listOf<HomeUiAction>(HomeUiAction.RequestQuietNotifications), actions)
            assertTrue(viewModel.uiState.value.hasAskedQuietly)
            assertNull(viewModel.uiState.value.notificationPermission)
            assertNull(viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN the first use on Apple WHEN notifications were never asked THEN does not ask quietly`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))

            // Then
            assertTrue(actions.isEmpty())
            assertEquals(NotificationPermission.NOT_DETERMINED, viewModel.uiState.value.notificationPermission)
            assertNull(viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN the first use on Apple never asked WHEN the first group arrives THEN asks quietly once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))

            // When
            groups.groups.value = listOf(trio())

            // Then
            assertEquals(listOf<HomeUiAction>(HomeUiAction.RequestQuietNotifications), actions)
            assertNull(viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN the groups not read yet on Apple WHEN notifications were never asked THEN does not ask quietly`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                groupsRead = false,
            )

            // When
            viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))

            // Then
            assertTrue(actions.isEmpty())
            assertFalse(viewModel.uiState.value.hasAskedQuietly)
        }

    @Test
    fun `GIVEN a group on Android or the web WHEN notifications were never asked THEN shows the soft ask instead`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))

            // Then
            assertTrue(actions.isEmpty())
            assertFalse(viewModel.uiState.value.hasAskedQuietly)
            assertEquals(NotificationPrompt.SOFT_ASK, viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN a group on Apple WHEN the platform reports any permission but never asked THEN does not ask quietly`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            val permissions = listOf(
                NotificationPermission.PROVISIONAL,
                NotificationPermission.GRANTED,
                NotificationPermission.DENIED,
            )

            // When
            permissions.forEach { viewModel.onEvent(appleReports(it)) }

            // Then
            assertTrue(actions.isEmpty())
            assertFalse(viewModel.uiState.value.hasAskedQuietly)
        }

    @Test
    fun `GIVEN the quiet request changed nothing WHEN never asked is read again THEN shows the soft ask`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))

            // When
            viewModel.onEvent(HomeUiEvent.OnQuietNotificationsAnswered(NotificationPermission.NOT_DETERMINED))

            // Then
            assertEquals(listOf<HomeUiAction>(HomeUiAction.RequestQuietNotifications), actions)
            assertEquals(NotificationPrompt.SOFT_ASK, viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN the quiet request out WHEN a read started before it says never asked THEN shows no card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))

            // When
            viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))

            // Then
            assertNull(viewModel.uiState.value.notificationPrompt)
            assertTrue(tracked("notification_prompt_shown").isEmpty())
        }

    @Test
    fun `GIVEN a stale read during the quiet request WHEN quiet delivery answers THEN shows the quiet card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))
            viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))

            // When
            viewModel.onEvent(HomeUiEvent.OnQuietNotificationsAnswered(NotificationPermission.PROVISIONAL))

            // Then
            assertEquals(NotificationPrompt.QUIET, viewModel.uiState.value.notificationPrompt)
            assertEquals(listOf(shown(NotificationPrompt.QUIET)), tracked("notification_prompt_shown"))
        }

    @Test
    fun `GIVEN the soft ask after a quiet request WHEN more checks read never asked THEN never asks quietly again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))
            viewModel.onEvent(HomeUiEvent.OnQuietNotificationsAnswered(NotificationPermission.NOT_DETERMINED))

            // When
            viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))
            groups.groups.value = listOf(trio(), trio(id = "trip"))

            // Then
            assertEquals(listOf<HomeUiAction>(HomeUiAction.RequestQuietNotifications), actions)
        }

    @Test
    fun `GIVEN the recovery code card on Apple WHEN notifications were never asked THEN still asks quietly`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.SNOOZED,
                storedGroups = listOf(trio()),
            )

            // When
            viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))

            // Then
            assertTrue(viewModel.uiState.value.showsRecoveryCodeCard)
            assertEquals(listOf<HomeUiAction>(HomeUiAction.RequestQuietNotifications), actions)
        }

    @Test
    fun `GIVEN the recovery code sheet on Apple WHEN notifications were never asked THEN still asks quietly`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.DUE,
                storedGroups = listOf(trio()),
            )

            // When
            viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))

            // Then
            assertTrue(viewModel.uiState.value.showsRecoveryCodePrompt)
            assertEquals(listOf<HomeUiAction>(HomeUiAction.RequestQuietNotifications), actions)
        }

    @Test
    fun `GIVEN a first download with a group already down on Apple WHEN never asked THEN still asks quietly`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("pierre", AccountProvider.APPLE),
                syncState = SyncState.SYNCING,
                storedGroups = listOf(trio()),
                isFirstDownloadPending = true,
            )

            // When
            viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))

            // Then
            assertTrue(viewModel.uiState.value.isFirstDownload)
            assertEquals(listOf<HomeUiAction>(HomeUiAction.RequestQuietNotifications), actions)
        }

    @Test
    fun `GIVEN the card answered 3 days ago on Apple WHEN notifications were never asked THEN still asks quietly`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                notificationPromptAnsweredAt = daysAgo(3),
            )

            // When
            viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))

            // Then
            assertTrue(viewModel.uiState.value.isNotificationPromptSnoozed)
            assertEquals(listOf<HomeUiAction>(HomeUiAction.RequestQuietNotifications), actions)
        }

    @Test
    fun `GIVEN the quiet request WHEN Apple reports quiet delivery THEN shows the quiet card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))

            // When
            viewModel.onEvent(appleReports(NotificationPermission.PROVISIONAL))

            // Then
            assertEquals(NotificationPrompt.QUIET, viewModel.uiState.value.notificationPrompt)
        }

    @Test
    fun `GIVEN the quiet card WHEN tapping Turn on THEN asks for the full prompt of Apple`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))
        viewModel.onEvent(appleReports(NotificationPermission.PROVISIONAL))

        // When
        viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)

        // Then
        assertEquals(listOf<HomeUiAction>(HomeUiAction.RequestNotificationPermission), actions)
    }

    @Test
    fun `GIVEN the quiet card WHEN tapping Turn on THEN tracks the turn on tap with the quiet card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(appleReports(NotificationPermission.PROVISIONAL))

            // Why: the card's showing has its own tests.
            trackedEvents.clear()

            // When
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)

            // Then
            assertEquals(
                listOf(
                    "notification_prompt_turn_on_clicked" to mapOf<String, Any>("prompt" to NotificationPrompt.QUIET),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the quiet card WHEN tapping Turn on THEN hides the card and stores now as its last answer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(appleReports(NotificationPermission.PROVISIONAL))

            // When
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)

            // Then
            assertNull(viewModel.uiState.value.notificationPrompt)
            assertTrue(viewModel.uiState.value.isNotificationPromptSnoozed)
            assertEquals(listOf(now), notificationPromptWrites)
        }

    @Test
    fun `GIVEN the quiet card WHEN tapping Not now THEN tracks the dismissal with the quiet card and asks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(appleReports(NotificationPermission.PROVISIONAL))

            // Why: the card's showing has its own tests.
            trackedEvents.clear()

            // When
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptDismissed)

            // Then
            assertEquals(
                listOf("notification_prompt_dismissed" to mapOf<String, Any>("prompt" to NotificationPrompt.QUIET)),
                trackedEvents,
            )
            assertTrue(actions.isEmpty())
            assertNull(viewModel.uiState.value.notificationPrompt)
            assertEquals(listOf(now), notificationPromptWrites)
        }

    @Test
    fun `GIVEN Turn on on the quiet card WHEN the full prompt of Apple allows them THEN says notifications are on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(appleReports(NotificationPermission.PROVISIONAL))
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)

            // When
            viewModel.onEvent(appleReports(NotificationPermission.GRANTED))

            // Then
            assertEquals(
                listOf(HomeUiAction.RequestNotificationPermission, HomeUiAction.ShowNotificationsOn),
                actions,
            )
        }

    @Test
    fun `GIVEN Turn on on the quiet card WHEN Apple keeps delivering quietly THEN says nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(appleReports(NotificationPermission.PROVISIONAL))
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)

            // When
            viewModel.onEvent(appleReports(NotificationPermission.PROVISIONAL))

            // Then
            assertEquals(listOf<HomeUiAction>(HomeUiAction.RequestNotificationPermission), actions)
        }

    @Test
    fun `GIVEN a group on Android WHEN notifications were never asked THEN tracks the soft ask shown`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))

            // Then
            assertEquals(listOf("notification_prompt_shown" to shown(NotificationPrompt.SOFT_ASK)), trackedEvents)
        }

    @Test
    fun `GIVEN the soft ask card shown WHEN the platform reports the same permission again THEN tracks nothing more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))

            // When
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))

            // Then
            assertEquals(listOf(shown(NotificationPrompt.SOFT_ASK)), tracked("notification_prompt_shown"))
        }

    @Test
    fun `GIVEN the soft ask card shown WHEN minutes pass THEN tracks no second showing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))
        viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))

        // When
        clockMillis = now + oneMinute
        minutes.value += 1
        clockMillis = now + 2 * oneMinute
        minutes.value += 1

        // Then
        assertEquals(listOf(shown(NotificationPrompt.SOFT_ASK)), tracked("notification_prompt_shown"))
    }

    @Test
    fun `GIVEN the soft ask card shown WHEN other things on Inicio change THEN tracks no second showing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))

            // When
            activityUnread.value = 3
            isOnline.value = false
            groups.groups.value = listOf(trio(), trio(id = "trip"))

            // Then
            assertEquals(listOf(shown(NotificationPrompt.SOFT_ASK)), tracked("notification_prompt_shown"))
        }

    @Test
    fun `GIVEN Not now on the soft ask card WHEN three weeks pass THEN tracks it shown again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptDismissed)

            // When
            clockMillis = now + 21.days.inWholeMilliseconds
            minutes.value += 1

            // Then
            assertEquals(
                listOf(shown(NotificationPrompt.SOFT_ASK), shown(NotificationPrompt.SOFT_ASK)),
                tracked("notification_prompt_shown"),
            )
        }

    @Test
    fun `GIVEN Turn on on the soft ask then refused once WHEN three weeks pass THEN tracks the ask again shown`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)
            viewModel.onEvent(androidReports(NotificationPermission.REFUSED_ONCE))

            // When
            clockMillis = now + 21.days.inWholeMilliseconds
            minutes.value += 1

            // Then
            assertEquals(
                listOf(shown(NotificationPrompt.SOFT_ASK), shown(NotificationPrompt.ASK_AGAIN)),
                tracked("notification_prompt_shown"),
            )
        }

    @Test
    fun `GIVEN the blocked card gone once allowed WHEN notifications get blocked again THEN tracks it again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(androidReports(NotificationPermission.DENIED))
            viewModel.onEvent(androidReports(NotificationPermission.GRANTED))

            // When
            viewModel.onEvent(androidReports(NotificationPermission.DENIED))

            // Then
            assertEquals(
                listOf(shown(NotificationPrompt.BLOCKED), shown(NotificationPrompt.BLOCKED)),
                tracked("notification_prompt_shown"),
            )
        }

    @Test
    fun `GIVEN the soft ask card shown WHEN a refusal makes it the ask again card THEN tracks the ask again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))

            // When
            viewModel.onEvent(androidReports(NotificationPermission.REFUSED_ONCE))

            // Then
            assertEquals(
                listOf(shown(NotificationPrompt.SOFT_ASK), shown(NotificationPrompt.ASK_AGAIN)),
                tracked("notification_prompt_shown"),
            )
        }

    @Test
    fun `GIVEN the ask again card shown WHEN it turns into the blocked card THEN tracks the blocked card shown`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(androidReports(NotificationPermission.REFUSED_ONCE))

            // When
            viewModel.onEvent(androidReports(NotificationPermission.DENIED))

            // Then
            assertEquals(
                listOf(shown(NotificationPrompt.ASK_AGAIN), shown(NotificationPrompt.BLOCKED)),
                tracked("notification_prompt_shown"),
            )
        }

    @Test
    fun `GIVEN the first use never asked WHEN the first group arrives THEN tracks the soft ask shown`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))

            // When
            groups.groups.value = listOf(trio())

            // Then
            assertEquals(listOf("notification_prompt_shown" to shown(NotificationPrompt.SOFT_ASK)), trackedEvents)
        }

    @Test
    fun `GIVEN the recovery code card over the notification card WHEN closing it THEN tracks the soft ask shown`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.SNOOZED,
                storedGroups = listOf(trio()),
            )
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))
            val shownUnderTheRecoveryCard = tracked("notification_prompt_shown")

            // When
            viewModel.onEvent(HomeUiEvent.OnRecoveryCodeCardDismissed)

            // Then
            assertTrue(shownUnderTheRecoveryCard.isEmpty())
            assertEquals(listOf(shown(NotificationPrompt.SOFT_ASK)), tracked("notification_prompt_shown"))
        }

    @Test
    fun `GIVEN a group on Apple WHEN asking quietly THEN tracks no card shown`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the quiet request WHEN Apple reports quiet delivery THEN tracks the quiet card shown`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))

            // When
            viewModel.onEvent(appleReports(NotificationPermission.PROVISIONAL))

            // Then
            assertEquals(listOf("notification_prompt_shown" to shown(NotificationPrompt.QUIET)), trackedEvents)
        }

    @Test
    fun `GIVEN the quiet request changed nothing WHEN Apple reads never asked again THEN tracks the soft ask`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))

            // When
            viewModel.onEvent(HomeUiEvent.OnQuietNotificationsAnswered(NotificationPermission.NOT_DETERMINED))

            // Then
            assertEquals(listOf("notification_prompt_shown" to shown(NotificationPrompt.SOFT_ASK)), trackedEvents)
        }

    @Test
    fun `GIVEN Turn on on the soft ask WHEN the system prompt allows them THEN tracks the answer as granted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(androidReports(NotificationPermission.GRANTED))

            // Then
            assertEquals(
                listOf("notification_permission_answered" to answered(NotificationPrompt.SOFT_ASK, isGranted = true)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Turn on on the soft ask WHEN the system prompt refuses them THEN tracks the answer as not granted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))
            viewModel.onEvent(HomeUiEvent.OnQuietNotificationsAnswered(NotificationPermission.NOT_DETERMINED))
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(appleReports(NotificationPermission.DENIED))

            // Then
            assertEquals(
                listOf("notification_permission_answered" to answered(NotificationPrompt.SOFT_ASK, isGranted = false)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Turn on on the soft ask WHEN the system prompt is dismissed unanswered THEN tracks it as not granted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))

            // Then
            assertEquals(
                listOf("notification_permission_answered" to answered(NotificationPrompt.SOFT_ASK, isGranted = false)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Turn on on the soft ask WHEN Android refuses them once THEN tracks the answer as not granted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(androidReports(NotificationPermission.REFUSED_ONCE))

            // Then
            assertEquals(
                listOf("notification_permission_answered" to answered(NotificationPrompt.SOFT_ASK, isGranted = false)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Turn on on the ask again card WHEN the system allows them THEN tracks the answer as granted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(androidReports(NotificationPermission.REFUSED_ONCE))
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(androidReports(NotificationPermission.GRANTED))

            // Then
            assertEquals(
                listOf("notification_permission_answered" to answered(NotificationPrompt.ASK_AGAIN, isGranted = true)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Turn on on the blocked card WHEN back from the settings still blocked THEN tracks it as not granted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(androidReports(NotificationPermission.DENIED))
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(androidReports(NotificationPermission.DENIED))

            // Then
            assertEquals(
                listOf("notification_permission_answered" to answered(NotificationPrompt.BLOCKED, isGranted = false)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Turn on on the blocked card WHEN back from the settings allowed THEN tracks the answer as granted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(androidReports(NotificationPermission.DENIED))
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(androidReports(NotificationPermission.GRANTED))

            // Then
            assertEquals(
                listOf("notification_permission_answered" to answered(NotificationPrompt.BLOCKED, isGranted = true)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Turn on on the quiet card WHEN Apple keeps delivering quietly THEN tracks the answer as not granted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(appleReports(NotificationPermission.PROVISIONAL))
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(appleReports(NotificationPermission.PROVISIONAL))

            // Then
            assertEquals(
                listOf("notification_permission_answered" to answered(NotificationPrompt.QUIET, isGranted = false)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Turn on on the quiet card WHEN the full prompt allows them THEN tracks the answer as granted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(appleReports(NotificationPermission.PROVISIONAL))
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(appleReports(NotificationPermission.GRANTED))

            // Then
            assertEquals(
                listOf("notification_permission_answered" to answered(NotificationPrompt.QUIET, isGranted = true)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the answer to Turn on tracked WHEN later checks read the permission THEN tracks no more answers`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)
            viewModel.onEvent(androidReports(NotificationPermission.REFUSED_ONCE))
            trackedEvents.clear()

            // When
            viewModel.onEvent(androidReports(NotificationPermission.REFUSED_ONCE))
            viewModel.onEvent(androidReports(NotificationPermission.GRANTED))

            // Then
            assertTrue(tracked("notification_permission_answered").isEmpty())
        }

    @Test
    fun `GIVEN no Turn on WHEN the platform reports a changed permission THEN tracks no answer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(androidReports(NotificationPermission.DENIED))

            // When
            viewModel.onEvent(androidReports(NotificationPermission.GRANTED))

            // Then
            assertTrue(tracked("notification_permission_answered").isEmpty())
        }

    @Test
    fun `GIVEN Not now on the soft ask WHEN the platform reports the permission again THEN tracks no answer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptDismissed)

            // When
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))

            // Then
            assertTrue(tracked("notification_permission_answered").isEmpty())
        }

    @Test
    fun `GIVEN the quiet request WHEN Apple reports quiet delivery THEN tracks no answer`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))
        viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))

        // When
        viewModel.onEvent(appleReports(NotificationPermission.PROVISIONAL))

        // Then
        assertTrue(tracked("notification_permission_answered").isEmpty())
    }

    private val everyone = listOf("Pierre", "Ana", "Bruno")

    private val oneMinute = 60_000L

    // Why: Saturday, 31 October 2026, 15:00 in UTC, the time zone the tests count days in.
    private val now = Instant.parse("2026-10-31T15:00:00Z").toEpochMilliseconds()

    private val pierre = UserProfile("Pierre", AvatarColor.GREEN, null)

    private val barbecue = GroupCategory(
        id = "barbecue",
        groupId = "trio",
        parentId = null,
        name = "Churrasco",
        icon = CategoryIcon.GIFT,
        createdByMemberId = "member-Ana",
        isDeleted = false,
        isPending = false,
    )

    private val registerPayment = Route.RegisterPayment(
        groupId = "trio",
        fromMemberId = "member-Ana",
        toMemberId = "member-Pierre",
        currency = GroupCurrency.BRL,
        offersReceiverKeys = true,
    )

    private val appStoreLink = "https://apps.apple.com/app/id6740000000"

    // Why: the web's invite while only Google Play lists the app.
    private val webInvite = AppInvite(
        link = AppInvite.WEB_LINK,
        stores = StoreListing.GOOGLE_PLAY,
        isDismissed = false,
        phoneUse = null,
    )

    private val onAndroid = PhoneUse(
        platforms = setOf(ClientPlatform.ANDROID),
        sinceMillis = Instant.parse("2026-10-12T12:00:00Z").toEpochMilliseconds(),
    )

    // Why: a code told apart by the link it encodes.
    private fun qrCodeOf(link: String): QrCode = QrCode(
        size = link.length,
        modules = link.map { it.code % 2 == 0 },
    )

    private fun hoursAgo(hours: Int): Long = now - hours.hours.inWholeMilliseconds

    private fun daysAgo(days: Int): Long = now - days.days.inWholeMilliseconds

    // Why: Apple can deliver quietly with no prompt while never asked; Android and the web cannot.
    @Test
    fun `GIVEN Turn on refused WHEN notifications are allowed later elsewhere THEN says nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))
            viewModel.onEvent(HomeUiEvent.OnNotificationPromptTurnOnClicked)
            viewModel.onEvent(androidReports(NotificationPermission.REFUSED_ONCE))

            // When
            viewModel.onEvent(androidReports(NotificationPermission.GRANTED))

            // Then
            assertEquals(listOf<HomeUiAction>(HomeUiAction.RequestNotificationPermission), actions)
        }

    @Test
    fun `GIVEN the quiet answer in WHEN a read started before it says never asked THEN keeps the quiet card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))
            viewModel.onEvent(HomeUiEvent.OnQuietNotificationsAnswered(NotificationPermission.PROVISIONAL))

            // When
            viewModel.onEvent(appleReports(NotificationPermission.NOT_DETERMINED))

            // Then
            assertEquals(NotificationPrompt.QUIET, viewModel.uiState.value.notificationPrompt)
            assertEquals(listOf(shown(NotificationPrompt.QUIET)), tracked("notification_prompt_shown"))
        }

    @Test
    fun `GIVEN another screen over Home WHEN the card comes up THEN counts it only once Home is back on top`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(Route.Profile)
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))
            val shownBehind = tracked("notification_prompt_shown")

            // When
            navigator.navigateBack()

            // Then
            assertTrue(shownBehind.isEmpty())
            assertEquals(listOf(shown(NotificationPrompt.SOFT_ASK)), tracked("notification_prompt_shown"))
        }

    @Test
    fun `GIVEN the card counted as shown WHEN Home comes back on top with it THEN does not count it again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))
            navigator.navigate(Route.Profile)

            // When
            navigator.navigateBack()

            // Then
            assertEquals(listOf(shown(NotificationPrompt.SOFT_ASK)), tracked("notification_prompt_shown"))
        }

    @Test
    fun `GIVEN the web invite WHEN Home opens THEN carries the link the stores and the QR code of the link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite)

            // When
            val invite = viewModel.uiState.value.appInvite

            // Then
            assertEquals(
                HomeAppInvite(
                    link = AppInvite.WEB_LINK,
                    qrCode = qrCodeOf(AppInvite.WEB_LINK),
                    stores = StoreListing.GOOGLE_PLAY,
                    isActive = true,
                ),
                invite,
            )
            assertEquals(listOf(AppInvite.WEB_LINK), encodedLinks)
        }

    @Test
    fun `GIVEN the invite on Home WHEN it comes again with the same link THEN keeps the QR code encoded once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite)

            // When
            appInvite.value = webInvite.copy(stores = StoreListing.BOTH)

            // Then
            assertEquals(
                StoreListing.BOTH,
                viewModel.uiState.value.appInvite
                    ?.stores,
            )
            assertEquals(
                qrCodeOf(AppInvite.WEB_LINK),
                viewModel.uiState.value.appInvite
                    ?.qrCode,
            )
            assertEquals(listOf(AppInvite.WEB_LINK), encodedLinks)
        }

    @Test
    fun `GIVEN the invite on Home WHEN its link changes THEN encodes the QR code of the new link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite)

            // When
            appInvite.value = webInvite.copy(link = appStoreLink)

            // Then
            assertEquals(
                appStoreLink,
                viewModel.uiState.value.appInvite
                    ?.link,
            )
            assertEquals(
                qrCodeOf(appStoreLink),
                viewModel.uiState.value.appInvite
                    ?.qrCode,
            )
            assertEquals(listOf(AppInvite.WEB_LINK, appStoreLink), encodedLinks)
        }

    @Test
    fun `GIVEN no invite on this platform WHEN Home opens with a bill THEN shows no invite card nor encodes a code`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = null,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.appInvite)
            assertFalse(state.showsAppInviteCard)
            assertTrue(encodedLinks.isEmpty())
        }

    @Test
    fun `GIVEN an active invite and a group with a bill WHEN Home opens THEN shows the invite card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.hasBills)
            assertTrue(state.showsAppInviteCard)
        }

    @Test
    fun `GIVEN an active invite and only a bill Pierre takes no part in WHEN Home opens THEN shows the invite card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        id = "taxi",
                        paidBy = "Ana",
                        splitOver = listOf("Ana", "Bruno"),
                    ),
                ),
                invite = webInvite,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.hasBills)
            assertTrue(state.showsAppInviteCard)
        }

    @Test
    fun `GIVEN an active invite and a group with no bill WHEN Home opens THEN shows no invite card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                invite = webInvite,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.hasBills)
            assertFalse(state.showsAppInviteCard)
        }

    @Test
    fun `GIVEN an active invite and a group with no bill WHEN the first bill comes THEN shows the invite card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                invite = webInvite,
            )

            // When
            expenses.expenses.value = listOf(bill("dinner"))

            // Then
            assertTrue(viewModel.uiState.value.showsAppInviteCard)
        }

    @Test
    fun `GIVEN an active invite and a bill WHEN the first download fills the device THEN shows no invite card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected("pierre", AccountProvider.GOOGLE),
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                isFirstDownloadPending = true,
                invite = webInvite,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isFirstDownload)
            assertFalse(state.showsAppInviteCard)
        }

    @Test
    fun `GIVEN the invite card WHEN the stores no longer list the app THEN removes the card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite,
            )

            // When
            appInvite.value = null

            // Then
            assertNull(viewModel.uiState.value.appInvite)
            assertFalse(viewModel.uiState.value.showsAppInviteCard)
        }

    @Test
    fun `GIVEN an active invite and a bill WHEN the notification card comes up THEN the invite card waits`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite,
            )

            // When
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))

            // Then
            assertEquals(NotificationPrompt.SOFT_ASK, viewModel.uiState.value.notificationPrompt)
            assertFalse(viewModel.uiState.value.showsAppInviteCard)
        }

    @Test
    fun `GIVEN the notification card before the invite WHEN notifications are allowed THEN shows the invite card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite,
            )
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))

            // When
            viewModel.onEvent(androidReports(NotificationPermission.GRANTED))

            // Then
            assertTrue(viewModel.uiState.value.showsAppInviteCard)
        }

    @Test
    fun `GIVEN the recovery code card before the invite WHEN dismissing it THEN shows the invite card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.SNOOZED,
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite,
            )
            val shownUnderRecoveryCard = viewModel.uiState.value.showsAppInviteCard

            // When
            viewModel.onEvent(HomeUiEvent.OnRecoveryCodeCardDismissed)

            // Then
            assertFalse(shownUnderRecoveryCard)
            assertTrue(viewModel.uiState.value.showsAppInviteCard)
        }

    @Test
    fun `GIVEN the recovery code sheet WHEN Home opens with an active invite and a bill THEN shows no invite card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous("anonymous"),
                prompt = RecoveryCodePrompt.DUE,
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsRecoveryCodePrompt)
            assertFalse(state.showsAppInviteCard)
        }

    @Test
    fun `GIVEN the invite card WHEN tapping the cross THEN ends the invite once and counts it from Home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnAppInviteDismissed)

            // Then
            assertEquals(1, appInviteDismissals)
            assertEquals(listOf<Map<String, Any>>(mapOf("source" to "home")), tracked("app_invite_dismissed"))
            assertFalse(viewModel.uiState.value.showsAppInviteCard)
        }

    @Test
    fun `GIVEN the invite card WHEN tapping the cross twice before it goes THEN ends and counts it once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite,
            )
            appInviteDismissGate = CompletableDeferred()

            // When
            viewModel.onEvent(HomeUiEvent.OnAppInviteDismissed)
            viewModel.onEvent(HomeUiEvent.OnAppInviteDismissed)
            appInviteDismissGate?.complete(Unit)

            // Then
            assertEquals(1, appInviteDismissals)
            assertEquals(1, tracked("app_invite_dismissed").size)
        }

    @Test
    fun `GIVEN an invite and a group WHEN Home loads THEN shows the phone browser banner`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            invite = webInvite,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.showsAppBanner)
    }

    @Test
    fun `GIVEN the phone browser banner WHEN tapping its cross THEN keeps it closed on this device`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                invite = webInvite,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnAppBannerDismissed)

            // Then
            assertTrue(appBannerDismissed.value)
            assertFalse(viewModel.uiState.value.showsAppBanner)
        }

    @Test
    fun `GIVEN the invite card WHEN tapping the app link by its QR code THEN counts only app_link_clicked from Home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite,
            )
            val trackedBefore = trackedEvents.toList()

            // When
            viewModel.onEvent(HomeUiEvent.OnAppInviteLinkClicked)

            // Then
            assertEquals(
                trackedBefore + ("app_link_clicked" to mapOf<String, Any>("source" to "home")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the invite card WHEN tapping the app link by its QR code THEN keeps the card and opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite,
            )
            val stateBefore = viewModel.uiState.value

            // When
            viewModel.onEvent(HomeUiEvent.OnAppInviteLinkClicked)

            // Then
            assertEquals(stateBefore, viewModel.uiState.value)
            assertTrue(viewModel.uiState.value.showsAppInviteCard)
            assertEquals(0, appInviteDismissals)
            assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the browser banner WHEN tapping Open in the app THEN counts only app_link_clicked from the banner`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                invite = webInvite,
            )
            val trackedBefore = trackedEvents.toList()

            // When
            viewModel.onEvent(HomeUiEvent.OnAppBannerOpenClicked)

            // Then
            assertEquals(
                trackedBefore + ("app_link_clicked" to mapOf<String, Any>("source" to "home_banner")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the phone browser banner WHEN tapping Open in the app THEN keeps the banner and opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                invite = webInvite,
            )
            val stateBefore = viewModel.uiState.value

            // When
            viewModel.onEvent(HomeUiEvent.OnAppBannerOpenClicked)

            // Then
            assertEquals(stateBefore, viewModel.uiState.value)
            assertTrue(viewModel.uiState.value.showsAppBanner)
            assertFalse(appBannerDismissed.value)
            assertEquals(0, appInviteDismissals)
            assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the invite card gone WHEN a late tap on the cross comes THEN neither ends nor counts it again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite,
            )
            viewModel.onEvent(HomeUiEvent.OnAppInviteDismissed)

            // When
            viewModel.onEvent(HomeUiEvent.OnAppInviteDismissed)

            // Then
            assertEquals(1, appInviteDismissals)
            assertEquals(1, tracked("app_invite_dismissed").size)
        }

    @Test
    fun `GIVEN an active invite and no bill yet WHEN tapping the cross THEN neither ends nor counts it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                invite = webInvite,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnAppInviteDismissed)

            // Then
            assertEquals(0, appInviteDismissals)
            assertTrue(tracked("app_invite_dismissed").isEmpty())
        }

    @Test
    fun `GIVEN the invite card WHEN tapping I downloaded it THEN ends the invite once and counts it from Home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnAppInviteDownloadedClicked)

            // Then
            assertEquals(1, appInviteDismissals)
            assertEquals(listOf<Map<String, Any>>(mapOf("source" to "home")), tracked("app_invite_downloaded_clicked"))
            assertFalse(viewModel.uiState.value.showsAppInviteCard)
        }

    @Test
    fun `GIVEN the invite card gone WHEN a late I downloaded it comes THEN neither ends nor counts it again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite,
            )
            viewModel.onEvent(HomeUiEvent.OnAppInviteDownloadedClicked)

            // When
            viewModel.onEvent(HomeUiEvent.OnAppInviteDownloadedClicked)

            // Then
            assertEquals(1, appInviteDismissals)
            assertEquals(1, tracked("app_invite_downloaded_clicked").size)
        }

    @Test
    fun `GIVEN an active invite and no bill yet WHEN tapping I downloaded it THEN neither ends nor counts it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                invite = webInvite,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnAppInviteDownloadedClicked)

            // Then
            assertEquals(0, appInviteDismissals)
            assertTrue(tracked("app_invite_downloaded_clicked").isEmpty())
        }

    @Test
    fun `GIVEN the notification card first WHEN tapping I downloaded it THEN neither ends nor counts the invite`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite,
            )
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))

            // When
            viewModel.onEvent(HomeUiEvent.OnAppInviteDownloadedClicked)

            // Then
            assertEquals(0, appInviteDismissals)
            assertTrue(tracked("app_invite_downloaded_clicked").isEmpty())
        }

    @Test
    fun `GIVEN the invite card WHEN tapping its QR code THEN opens the app code from Home over Home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnAppInviteCodeClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.AppDownload(AppDownloadSource.HOME)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN no invite on this platform WHEN a tap on the invite QR code comes THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = null,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnAppInviteCodeClicked)

            // Then
            assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
        }

    @Test
    fun `GIVEN another screen over Home WHEN a late tap on the invite QR code comes THEN opens nothing over it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite,
            )
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(HomeUiEvent.OnAppInviteCodeClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the invite card WHEN tapping Connect account THEN opens the profile over Home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite,
            )

            // When
            viewModel.onEvent(HomeUiEvent.OnAppInviteConnectClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN another screen over Home WHEN a late invite Connect account comes THEN opens nothing over it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite,
            )
            navigator.navigate(Route.CreateGroup)

            // When
            viewModel.onEvent(HomeUiEvent.OnAppInviteConnectClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.CreateGroup), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the invite card WHEN a sync finds the account on a phone THEN removes it and says it saw the phone`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite,
            )

            // When
            appInvite.value = webInvite.copy(phoneUse = onAndroid)

            // Then
            assertEquals(listOf<HomeUiAction>(HomeUiAction.ShowUsedOnPhone), actions)
            assertEquals(
                false,
                viewModel.uiState.value.appInvite
                    ?.isActive,
            )
            assertFalse(viewModel.uiState.value.showsAppInviteCard)
        }

    @Test
    fun `GIVEN the phone seen with the card WHEN the invite comes again with a phone THEN does not say it again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite,
            )
            appInvite.value = webInvite.copy(phoneUse = onAndroid)

            // When
            appInvite.value = webInvite.copy(
                phoneUse = onAndroid.copy(platforms = setOf(ClientPlatform.ANDROID, ClientPlatform.IOS)),
            )

            // Then
            assertEquals(listOf<HomeUiAction>(HomeUiAction.ShowUsedOnPhone), actions)
        }

    @Test
    fun `GIVEN the invite dismissed WHEN a sync finds the account on a phone THEN says nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite.copy(isDismissed = true),
            )

            // When
            appInvite.value = webInvite.copy(
                isDismissed = true,
                phoneUse = onAndroid,
            )

            // Then
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN an active invite and no bill yet WHEN a sync finds the account on a phone THEN says nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                invite = webInvite,
            )

            // When
            appInvite.value = webInvite.copy(phoneUse = onAndroid)

            // Then
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the notification card before the invite WHEN a sync finds the account on a phone THEN says nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite,
            )
            viewModel.onEvent(androidReports(NotificationPermission.NOT_DETERMINED))

            // When
            appInvite.value = webInvite.copy(phoneUse = onAndroid)

            // Then
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the account already seen on a phone WHEN Home opens with a bill THEN shows no card and says nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                invite = webInvite.copy(phoneUse = onAndroid),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.showsAppInviteCard)
            assertTrue(actions.isEmpty())
        }

    private fun appleReports(permission: NotificationPermission): HomeUiEvent =
        HomeUiEvent.OnNotificationPermissionChecked(permission, canAskQuietly = true)

    private fun androidReports(permission: NotificationPermission): HomeUiEvent =
        HomeUiEvent.OnNotificationPermissionChecked(permission, canAskQuietly = false)

    // Why: the params of each tracked event of that name, in order.
    private fun tracked(name: String): List<Map<String, Any>> =
        trackedEvents.filter { it.first == name }.map { it.second }

    private fun shown(prompt: NotificationPrompt): Map<String, Any> = mapOf("prompt" to prompt)

    private fun answered(
        prompt: NotificationPrompt,
        isGranted: Boolean,
    ): Map<String, Any> = mapOf(
        "source" to "home",
        "prompt" to prompt,
        "is_granted" to isGranted,
    )

    private fun group(
        id: String,
        name: String = "Grupo",
        members: List<GroupMember>,
        currency: GroupCurrency = GroupCurrency.BRL,
        updatedAt: Long = daysAgo(10),
    ): Group = Group(
        id = id,
        name = name,
        type = GroupType.OTHER,
        color = AvatarColor.BLUE,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = currency,
        members = members,
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = updatedAt,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    // Why: Pierre is this device's user, in a group with Ana and Bruno.
    private fun trio(
        id: String = "trio",
        currency: GroupCurrency = GroupCurrency.BRL,
        updatedAt: Long = daysAgo(10),
    ): Group = group(
        id = id,
        name = "Apê 302",
        members = listOf(me(), member("Ana"), member("Bruno")),
        currency = currency,
        updatedAt = updatedAt,
    )

    private fun me(): GroupMember = member("Pierre").copy(isMe = true)

    private fun member(name: String): GroupMember = GroupMember(
        id = "member-$name",
        name = name,
        avatarColor = AvatarColor.BLUE,
        isMe = false,
        hasJoined = false,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    // Why: one payer for the whole bill, split over the members given.
    private fun bill(
        id: String,
        groupId: String = "trio",
        paidBy: String = "Pierre",
        amountMinor: Long = 9000L,
        splitOver: List<String> = everyone,
        spentAt: Long = daysAgo(5),
        isPending: Boolean = false,
        categoryId: String? = null,
    ): Expense = Expense(
        id = id,
        groupId = groupId,
        description = "Mercado",
        currency = GroupCurrency.BRL,
        amountMinor = amountMinor,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer("member-$paidBy", amountMinor)),
        shares = splitOver.map { ExpenseShare("member-$it", amountMinor / splitOver.size, 1L) },
        spentAt = spentAt,
        isPending = isPending,
        updatedAt = spentAt,
        updatedByMemberId = null,
        items = null,
        categoryId = categoryId,
    )

    private fun payment(
        id: String,
        groupId: String = "trio",
        from: String,
        to: String,
        amountMinor: Long,
    ): Settlement = Settlement(
        id = id,
        groupId = groupId,
        fromMemberId = "member-$from",
        toMemberId = "member-$to",
        currency = GroupCurrency.BRL,
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = daysAgo(1),
        isPending = false,
        updatedAt = daysAgo(1),
    )

    private fun TestScope.prepareScenario(
        online: Boolean = true,
        status: AccountStatus = AccountStatus.LocalOnly,
        prompt: RecoveryCodePrompt = RecoveryCodePrompt.NOT_DUE,
        syncState: SyncState = SyncState.SYNCED,
        profile: UserProfile? = pierre,
        storedGroups: List<Group> = emptyList(),
        groupsRead: Boolean = true,
        storedExpenses: List<Expense> = emptyList(),
        storedSettlements: List<Settlement> = emptyList(),
        storedCategories: List<GroupCategory> = emptyList(),
        pinnedGroupIds: Set<String> = emptySet(),
        isCardDismissedThisLaunch: Boolean = false,
        isFirstDownloadPending: Boolean = false,
        peopleRead: Boolean = true,
        awaitsFullSync: Boolean = false,
        notificationPromptAnsweredAt: Long? = null,
        notificationPromptTimeRead: Boolean = true,
        invite: AppInvite? = null,
    ) {
        account = FakeAccountRepository(initialStatus = status).also {
            it.recoveryCodePrompt = prompt
            it.awaitsFullSync = awaitsFullSync
        }
        peopleGate = MutableStateFlow(peopleRead)
        groupsGate = MutableStateFlow(groupsRead)
        groups = FakeGroupRepository(initialGroups = storedGroups)
        pins = FakeGroupPinRepository(initialPinnedGroupIds = pinnedGroupIds)
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses.inCurrencyOf(storedGroups))
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        categories = FakeCategoryRepository(initialGroupCategories = storedCategories)
        recordedPayments = InMemoryRecordedPaymentRepository()
        isOnline = MutableStateFlow(online)
        this@HomeViewModelTest.syncState = MutableStateFlow(syncState)
        minutes = MutableStateFlow(0)
        clockMillis = now
        retryCount = 0
        syncNowCount = 0
        syncGate = null
        navigator = BackStackNavigator(startRoute = Route.Home)
        deletions = InMemoryGroupDeletionRepository()
        dismissal = RecoveryCodeCardDismissal().also { it.isDismissed.value = isCardDismissedThisLaunch }
        firstDownload = FirstDownload().also { if (isFirstDownloadPending) it.start() }
        activityUnread = MutableStateFlow(0)
        lastNotificationPromptAt.value = notificationPromptAnsweredAt
        appInvite = MutableStateFlow(invite)
        appInviteDismissals = 0
        encodedLinks.clear()
        heldReceipts.clear()
        val money = ObserveGroupsMoneyUseCase(expenses, settlements)
        viewModel = HomeViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            navigator = navigator,
            retrySync = RetrySync { retryCount++ },
            syncNow = SyncNow {
                syncNowCount++
                syncGate?.await()
                Result.success(Unit)
            },
            updateRecoveryCodePrompt = UpdateRecoveryCodePromptUseCase(account),
            consumeGroupDeletion = ConsumeGroupDeletionUseCase(deletions),
            recoveryCodeCardDismissal = dismissal,
            // Why: like the repository, a dismissal ends the invite the flow emits.
            dismissAppInvite = DismissAppInvite {
                appInviteDismissals++
                appInviteDismissGate?.await()
                appInvite.update { it?.copy(isDismissed = true) }
                Result.success(Unit)
            },
            encodeQrCode = EncodeQrCode { link ->
                encodedLinks += link
                qrCodeOf(link)
            },
            observeAppInvite = ObserveAppInvite { appInvite },
            holdPickedReceipt = HoldPickedReceipt { heldReceipts += it.toList() },
            updateLastNotificationPromptAt = UpdateLastNotificationPromptAt {
                notificationPromptWrites += it
                lastNotificationPromptAt.value = it
            },
            updatePushEnabled = UpdatePushEnabled { pushSwitchWrites += it },
            // Why: like the groups, the settings row not read yet is a flow that has not emitted.
            dismissAppBanner = DismissAppBanner { appBannerDismissed.value = true },
            observeAppBannerDismissed = ObserveAppBannerDismissed { appBannerDismissed },
            observeLastNotificationPromptAt = ObserveLastNotificationPromptAt {
                if (notificationPromptTimeRead) lastNotificationPromptAt else MutableSharedFlow()
            },
            observeRecordedPayment = ObserveRecordedPaymentUseCase(recordedPayments),
            observeSettledUp = ObserveSettledUpUseCase(recordedPayments),
            consumeRecordedPayment = ConsumeRecordedPaymentUseCase(recordedPayments),
            deleteSettlement = DeleteSettlementUseCase(settlements, recordedPayments) { clockMillis },
            currentTimeMillis = CurrentTimeMillis { clockMillis },
            currentTimeZone = CurrentTimeZone { TimeZone.UTC },
            minuteTicks = MinuteTicks { minutes.map { } },
            observeProfile = ObserveProfileUseCase(FakeProfileRepository(profile)),
            observeAccount = ObserveAccountUseCase(account),
            observeSyncState = ObserveSyncState { this@HomeViewModelTest.syncState },
            observeFirstDownload = ObserveFirstDownloadUseCase(
                firstDownload = firstDownload,
                accountRepository = account,
                connectivityObserver = NetworkConnectivityObserver { isOnline },
            ),
            // Why: a StateFlow is read at once, so the device not answering yet is a flow that has not emitted.
            observeGroups = ObserveGroups { groupsGate.filter { it }.flatMapLatest { ObserveGroupsUseCase(groups)() } },
            observeGroupsMoney = money,
            // Why: the balances are read apart from the groups, so they can still be on their way once the groups are.
            observePeopleBalances = ObservePeopleBalances {
                peopleGate.filter { it }.flatMapLatest {
                    ObservePeopleBalancesUseCase(
                        observeGroups = ObserveGroupsUseCase(groups),
                        observeGroupsMoney = money,
                    )()
                }
            },
            observePinnedGroupIds = ObservePinnedGroupIdsUseCase(pins),
            observeGroupDeletions = ObserveGroupDeletionsUseCase(deletions),
            connectivityObserver = NetworkConnectivityObserver { isOnline },
            observeActivityUnreadCount = ObserveActivityUnreadCount { activityUnread },
            observeMapCategories = ObserveMapCategoriesUseCase(categories),
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
