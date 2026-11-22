package com.quare.blitzsplit.feature.watchbalances

import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.watchbalances.presentation.navigation.OpenWatchGroup
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenWatchGroupTest {
    @Test
    fun `GIVEN a group open in two currencies WHEN its push is opened THEN opens it in the larger one`() = runTest {
        // Given
        val navigator = BackStackNavigator(Route.WatchBalances)
        navigator.navigate(Route.WatchGroup(FLORIPA, GroupCurrency.BRL))
        val open = openWatchGroupWith(
            navigator,
            balances(
                person("Carla", pair(APE, "carla", -1_000L)),
                person("Ana", pair(APE, "ana", 5_000L), currency = GroupCurrency.USD),
            ),
        )

        // When
        open(APE)

        // Then
        assertEquals(
            listOf(Route.WatchBalances, Route.WatchGroup(APE, GroupCurrency.USD)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a group with nothing open with you WHEN its push is opened THEN opens only the balances`() = runTest {
        // Given
        val navigator = BackStackNavigator(Route.WatchBalances)
        navigator.navigate(Route.WatchGroup(FLORIPA, GroupCurrency.BRL))
        val open = openWatchGroupWith(navigator, balances(person("Carla", pair(FLORIPA, "carla", 1_000L))))

        // When
        open(CHURRASCO)

        // Then
        assertEquals(listOf(Route.WatchBalances), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a watch not connected yet WHEN a push is opened THEN stays on the connect screens`() = runTest {
        // Given
        val navigator = BackStackNavigator(Route.WatchConnect)
        val open = openWatchGroupWith(navigator, balances(person("Carla", pair(APE, "carla", -1_000L))))

        // When
        open(APE)

        // Then
        assertEquals(listOf(Route.WatchConnect), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a push that launched the app WHEN the root opens the balances after it THEN the group still opens`() =
        runTest(UnconfinedTestDispatcher()) {
            // Given
            val navigator = BackStackNavigator(Route.Welcome)
            val open = openWatchGroupWith(navigator, balances(person("Carla", pair(APE, "carla", -1_000L))))
            launch { open(APE) }

            // When
            navigator.replaceAll(Route.WatchBalances)

            // Then
            assertEquals(
                listOf(Route.WatchBalances, Route.WatchGroup(APE, GroupCurrency.BRL)),
                navigator.backStack.value,
            )
        }

    private fun openWatchGroupWith(
        navigator: BackStackNavigator,
        balances: PeopleBalances,
    ): OpenWatchGroup = OpenWatchGroup(
        navigator = navigator,
        observePeopleBalances = { flowOf(balances) },
    )
}
