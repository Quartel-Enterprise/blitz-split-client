package com.quare.blitzsplit.feature.watchbalances

import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchBalancesUiEvent
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchGroupRow
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchTotal
import com.quare.blitzsplit.feature.watchbalances.presentation.viewmodel.WatchBalancesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
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

class WatchBalancesViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var peopleBalances: MutableStateFlow<PeopleBalances?>
    private lateinit var groupsMoney: MutableStateFlow<Map<String, GroupMoney>>
    private lateinit var firstDownload: MutableStateFlow<FirstDownloadProgress?>
    private lateinit var lastSyncedAt: MutableStateFlow<Long?>
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: WatchBalancesViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN reais and dollars open and euros settled WHEN the balances open THEN shows a total per open currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                balances = balances(
                    person("Carla", pair(FLORIPA, "carla", 5_000L)),
                    totals = listOf(
                        total(
                            GroupCurrency.BRL,
                            toReceiveMinor = 5_000L,
                            toPayMinor = 2_000L,
                        ),
                        total(
                            GroupCurrency.EUR,
                            toReceiveMinor = 0L,
                            toPayMinor = 0L,
                        ),
                        total(
                            GroupCurrency.USD,
                            toReceiveMinor = 0L,
                            toPayMinor = 700L,
                        ),
                    ),
                ),
            )

            // When
            val totals = viewModel.uiState.value.totals

            // Then
            assertEquals(
                listOf(
                    WatchTotal(
                        GroupCurrency.BRL,
                        netMinor = 3_000L,
                        toReceiveMinor = 5_000L,
                        toPayMinor = 2_000L,
                    ),
                    WatchTotal(
                        GroupCurrency.USD,
                        netMinor = -700L,
                        toReceiveMinor = 0L,
                        toPayMinor = 700L,
                    ),
                ),
                totals,
            )
        }

    @Test
    fun `GIVEN two people in one group WHEN the balances open THEN the group shows once with the sum of its pairs`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                balances = balances(
                    person("Carla", pair(FLORIPA, "carla", 5_000L)),
                    person("Ana", pair(FLORIPA, "ana", -2_000L)),
                ),
            )

            // When
            val groups = viewModel.uiState.value.groups

            // Then
            assertEquals(
                listOf(
                    WatchGroupRow(
                        groupId = FLORIPA,
                        name = "Floripa",
                        initials = "FL",
                        color = AvatarColor.BLUE,
                        photoUrl = "https://photos/floripa.jpg",
                        currency = GroupCurrency.BRL,
                        netMinor = 3_000L,
                        isQueued = false,
                    ),
                ),
                groups,
            )
        }

    @Test
    fun `GIVEN a group with balances in two currencies WHEN the balances open THEN shows a row per currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                balances = balances(
                    person("Carla", pair(FLORIPA, "carla", 5_000L)),
                    person("Carla", pair(FLORIPA, "carla", -800L), currency = GroupCurrency.USD),
                ),
            )

            // When
            val groups = viewModel.uiState.value.groups

            // Then
            assertEquals(
                listOf(FLORIPA to GroupCurrency.BRL, FLORIPA to GroupCurrency.USD),
                groups.map { it.groupId to it.currency },
            )
            assertEquals(listOf(5_000L, -800L), groups.map { it.netMinor })
        }

    @Test
    fun `GIVEN a group whose pairs offset each other WHEN the balances open THEN the group shows with a net of zero`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                balances = balances(
                    person("Carla", pair(FLORIPA, "carla", 2_000L), pair(APE, "carla-ape", 1_000L)),
                    person("Ana", pair(FLORIPA, "ana", -2_000L)),
                ),
            )

            // When
            val groups = viewModel.uiState.value.groups

            // Then
            assertEquals(listOf(APE to 1_000L, FLORIPA to 0L), groups.map { it.groupId to it.netMinor })
        }

    @Test
    fun `GIVEN someone even overall WHEN the balances open THEN each of their groups shows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                balances = balances(
                    evenPeople = listOf(
                        person("Carla", pair(FLORIPA, "carla", 5_000L), pair(APE, "carla-ape", -5_000L)),
                    ),
                ),
            )

            // When
            val groups = viewModel.uiState.value.groups

            // Then
            assertEquals(setOf(FLORIPA to 5_000L, APE to -5_000L), groups.map { it.groupId to it.netMinor }.toSet())
        }

    @Test
    fun `GIVEN groups you owe and groups that owe you WHEN the balances open THEN the largest balance comes first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                balances = balances(
                    person("Carla", pair(FLORIPA, "carla", 1_000L)),
                    person("Ana", pair(APE, "ana", -9_000L)),
                    person("Bia", pair(CHURRASCO, "bia", 4_000L)),
                ),
            )

            // When
            val groups = viewModel.uiState.value.groups

            // Then
            assertEquals(listOf(APE, CHURRASCO, FLORIPA), groups.map { it.groupId })
        }

    @Test
    fun `GIVEN a payment waiting to go up in one group WHEN the balances open THEN only that group shows as queued`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                balances = balances(
                    person("Carla", pair(FLORIPA, "carla", -5_000L)),
                    person("Ana", pair(APE, "ana", 3_000L)),
                ),
                money = mapOf(
                    FLORIPA to moneyWith(settlement(FLORIPA, isPending = true)),
                    APE to moneyWith(settlement(APE, isPending = false)),
                ),
            )

            // When
            val groups = viewModel.uiState.value.groups

            // Then
            assertEquals(listOf(FLORIPA to true, APE to false), groups.map { it.groupId to it.isQueued })
        }

    @Test
    fun `GIVEN a payment queued in another currency of the group WHEN the balances open THEN the row is not queued`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                balances = balances(person("Carla", pair(FLORIPA, "carla", -5_000L))),
                money = mapOf(
                    FLORIPA to moneyWith(
                        settlement(
                            FLORIPA,
                            isPending = true,
                            currency = GroupCurrency.USD,
                        ),
                    ),
                ),
            )

            // When
            val row = viewModel.uiState.value.groups
                .single()

            // Then
            assertFalse(row.isQueued)
        }

    @Test
    fun `GIVEN payments waiting to go up in two groups WHEN the balances open THEN counts every queued change`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                balances = balances(person("Carla", pair(FLORIPA, "carla", -5_000L))),
                money = mapOf(
                    FLORIPA to moneyWith(settlement(FLORIPA, isPending = true), settlement(FLORIPA, isPending = false)),
                    APE to moneyWith(
                        settlement(
                            APE,
                            isPending = true,
                            currency = GroupCurrency.USD,
                        ),
                    ),
                ),
            )

            // When
            val count = viewModel.uiState.value.queuedChangeCount

            // Then
            assertEquals(2, count)
        }

    @Test
    fun `GIVEN the watch never synced WHEN a sync goes through THEN shows when it was updated`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(syncedAt = null)
            val before = viewModel.uiState.value.lastSyncedAt

            // When
            lastSyncedAt.value = 1_700L

            // Then
            assertNull(before)
            assertEquals(1_700L, viewModel.uiState.value.lastSyncedAt)
        }

    @Test
    fun `GIVEN no internet WHEN the balances open THEN shows the offline notice`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isOnline)
        assertTrue(state.showsOfflineNotice)
    }

    @Test
    fun `GIVEN no internet WHEN the internet comes back THEN the offline notice goes away`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        isOnline.value = true

        // Then
        assertTrue(viewModel.uiState.value.isOnline)
        assertFalse(viewModel.uiState.value.showsOfflineNotice)
    }

    @Test
    fun `GIVEN the balances did not come yet WHEN the screen opens THEN it is not loaded nor all settled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(balances = null)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertFalse(state.isAllSettled)
        }

    @Test
    fun `GIVEN a first download filling the watch WHEN the balances come THEN it is not loaded nor all settled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                balances = balances(),
                download = FirstDownloadProgress(
                    arrivedGroupCount = 1,
                    groupCount = 3,
                    isWaitingForInternet = false,
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertFalse(state.isAllSettled)
        }

    @Test
    fun `GIVEN a first download filling the watch WHEN it finishes THEN the balances are loaded`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                balances = balances(person("Carla", pair(FLORIPA, "carla", 5_000L))),
                download = FirstDownloadProgress(
                    arrivedGroupCount = 1,
                    groupCount = 3,
                    isWaitingForInternet = false,
                ),
            )

            // When
            firstDownload.value = null

            // Then
            assertTrue(viewModel.uiState.value.isLoaded)
            assertFalse(viewModel.uiState.value.isAllSettled)
        }

    @Test
    fun `GIVEN nobody owes you and you owe nobody WHEN the balances open THEN shows nothing open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(balances = balances())

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertTrue(state.isAllSettled)
            assertTrue(state.groups.isEmpty())
        }

    @Test
    fun `GIVEN an open group WHEN a sync settles it THEN the balances show nothing open`() = runTest(testDispatcher) {
        // Given
        prepareScenario(balances = balances(person("Carla", pair(FLORIPA, "carla", 5_000L))))

        // When
        peopleBalances.value = balances()

        // Then
        assertTrue(viewModel.uiState.value.isAllSettled)
    }

    @Test
    fun `GIVEN an open group WHEN tapping its row THEN opens the group in that currency`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            balances = balances(person("Carla", pair(FLORIPA, "carla", -800L), currency = GroupCurrency.USD)),
        )
        val event = WatchBalancesUiEvent.OnGroupClicked(
            groupId = FLORIPA,
            currency = GroupCurrency.USD,
        )

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(
            listOf(Route.WatchBalances, Route.WatchGroup(FLORIPA, GroupCurrency.USD)),
            navigator.backStack.value,
        )
        assertEquals(listOf("watch_group_opened" to emptyMap<String, Any>()), trackedEvents)
    }

    private fun prepareScenario(
        balances: PeopleBalances? = balances(),
        money: Map<String, GroupMoney> = emptyMap(),
        download: FirstDownloadProgress? = null,
        syncedAt: Long? = 1_000L,
        online: Boolean = true,
    ) {
        peopleBalances = MutableStateFlow(balances)
        groupsMoney = MutableStateFlow(money)
        firstDownload = MutableStateFlow(download)
        lastSyncedAt = MutableStateFlow(syncedAt)
        isOnline = MutableStateFlow(online)
        navigator = BackStackNavigator(startRoute = Route.WatchBalances)
        viewModel = WatchBalancesViewModel(
            navigator = navigator,
            observePeopleBalances = { peopleBalances.filterNotNull() },
            observeGroupsMoney = { groupsMoney },
            observeFirstDownload = { firstDownload },
            observeLastSyncedAt = { lastSyncedAt },
            connectivityObserver = NetworkConnectivityObserver { isOnline },
            trackEvent = { name, params -> trackedEvents += name to params },
        )
    }
}
