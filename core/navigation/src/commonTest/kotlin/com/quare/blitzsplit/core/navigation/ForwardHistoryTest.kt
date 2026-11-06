package com.quare.blitzsplit.core.navigation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ForwardHistoryTest {
    private lateinit var navigator: BackStackNavigator
    private lateinit var history: ForwardHistory

    // Why: the history watches every move of the back stack as it happens, as the app's main thread delivers it.
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @AfterTest
    fun cancelScope() {
        scope.cancel()
    }

    @Test
    fun `GIVEN a screen over the home WHEN going back THEN closes the screen on top`() {
        // Given
        prepareScenario(Route.Groups, Route.CreateGroup)

        // When
        history.goBack()

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN nothing went back yet WHEN checking forward THEN forward is off`() = runTest {
        // Given
        prepareScenario(Route.Groups, Route.CreateGroup)

        // When
        val canGoForward = history.canGoForward.first()

        // Then
        assertFalse(canGoForward)
    }

    @Test
    fun `GIVEN a screen back closed WHEN checking forward THEN forward is on`() = runTest {
        // Given
        prepareScenario(Route.Groups, Route.CreateGroup)
        history.goBack()

        // When
        val canGoForward = history.canGoForward.first()

        // Then
        assertTrue(canGoForward)
    }

    @Test
    fun `GIVEN a screen back closed WHEN going forward THEN opens it again`() {
        // Given
        prepareScenario(Route.Groups, Route.CreateGroup)
        history.goBack()

        // When
        history.goForward()

        // Then
        assertEquals(listOf(Route.Home, Route.Groups, Route.CreateGroup), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the only screen back closed opened again WHEN checking forward THEN forward is off`() = runTest {
        // Given
        prepareScenario(Route.Groups, Route.CreateGroup)
        history.goBack()
        history.goForward()

        // When
        val canGoForward = history.canGoForward.first()

        // Then
        assertFalse(canGoForward)
    }

    @Test
    fun `GIVEN two screens back closed WHEN going forward twice THEN opens them again in reverse order of closing`() {
        // Given
        prepareScenario(Route.Groups, Route.CreateGroup)
        history.goBack()
        history.goBack()
        history.goForward()

        // When
        history.goForward()

        // Then
        assertEquals(listOf(Route.Home, Route.Groups, Route.CreateGroup), navigator.backStack.value)
    }

    @Test
    fun `GIVEN two screens back closed WHEN going forward once THEN opens the last one closed`() = runTest {
        // Given
        prepareScenario(Route.Groups, Route.CreateGroup)
        history.goBack()
        history.goBack()

        // When
        history.goForward()

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
        assertTrue(history.canGoForward.first())
    }

    @Test
    fun `GIVEN nothing back closed WHEN going forward THEN the back stack stays`() {
        // Given
        prepareScenario(Route.Groups)

        // When
        history.goForward()

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN only the root WHEN going back THEN the back stack stays and forward is off`() = runTest {
        // Given
        prepareScenario()

        // When
        history.goBack()

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
        assertFalse(history.canGoForward.first())
    }

    @Test
    fun `GIVEN back reached the root WHEN going back again THEN forward still opens the closed screen`() = runTest {
        // Given
        prepareScenario(Route.Groups)
        history.goBack()
        history.goBack()

        // When
        history.goForward()

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
        assertFalse(history.canGoForward.first())
    }

    @Test
    fun `GIVEN back reached the root WHEN going back again THEN forward stays on`() = runTest {
        // Given
        prepareScenario(Route.Groups)
        history.goBack()

        // When
        history.goBack()

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
        assertTrue(history.canGoForward.first())
    }

    @Test
    fun `GIVEN a screen back closed WHEN another screen opens THEN forward is off`() = runTest {
        // Given
        prepareScenario(Route.Groups, Route.CreateGroup)
        history.goBack()

        // When
        navigator.navigate(Route.Profile)

        // Then
        assertFalse(history.canGoForward.first())
    }

    @Test
    fun `GIVEN another screen opened after a back WHEN going forward THEN the back stack stays`() {
        // Given
        prepareScenario(Route.Groups, Route.CreateGroup)
        history.goBack()
        navigator.navigate(Route.Profile)

        // When
        history.goForward()

        // Then
        assertEquals(listOf(Route.Home, Route.Groups, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a screen back closed WHEN a tab replaces what is above the home THEN forward is off`() = runTest {
        // Given
        prepareScenario(Route.Groups, Route.CreateGroup)
        history.goBack()

        // When
        navigator.openHomeTab(Route.Contacts)

        // Then
        assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
        assertFalse(history.canGoForward.first())
    }

    @Test
    fun `GIVEN a tab replaced what is above the home after a back WHEN going forward THEN the back stack stays`() {
        // Given
        prepareScenario(Route.Groups, Route.CreateGroup)
        history.goBack()
        navigator.openHomeTab(Route.Contacts)

        // When
        history.goForward()

        // Then
        assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a screen back closed WHEN a pop not done through back closes another THEN forward is off`() = runTest {
        // Given
        prepareScenario(Route.Groups, Route.CreateGroup)
        history.goBack()

        // When
        navigator.navigateBack()

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
        assertFalse(history.canGoForward.first())
    }

    @Test
    fun `GIVEN a pop not done through back after a back WHEN going forward THEN the back stack stays`() {
        // Given
        prepareScenario(Route.Groups, Route.CreateGroup)
        history.goBack()
        navigator.popTo(1)

        // When
        history.goForward()

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a screen opened and swiped closed after a back WHEN checking forward THEN forward is off`() = runTest {
        // Given
        prepareScenario(Route.Groups, Route.CreateGroup)
        history.goBack()
        navigator.navigate(Route.Profile)
        navigator.popTo(2)

        // When
        val canGoForward = history.canGoForward.first()

        // Then
        assertFalse(canGoForward)
    }

    @Test
    fun `GIVEN a screen opened and swiped closed after a back WHEN going forward THEN the back stack stays`() {
        // Given
        prepareScenario(Route.Groups, Route.CreateGroup)
        history.goBack()
        navigator.navigate(Route.Profile)
        navigator.popTo(2)

        // When
        history.goForward()

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another screen opened after a back WHEN back then forward twice THEN only the new screen opens`() {
        // Given
        prepareScenario(Route.Groups, Route.CreateGroup)
        history.goBack()
        navigator.navigate(Route.Profile)
        history.goBack()
        history.goForward()

        // When
        history.goForward()

        // Then
        assertEquals(listOf(Route.Home, Route.Groups, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN back back forward and back again WHEN going forward THEN opens the screen the last back closed`() {
        // Given
        prepareScenario(Route.Groups, Route.CreateGroup, Route.Profile)
        history.goBack()
        history.goBack()
        history.goForward()
        history.goBack()

        // When
        history.goForward()

        // Then
        assertEquals(listOf(Route.Home, Route.Groups, Route.CreateGroup), navigator.backStack.value)
    }

    @Test
    fun `GIVEN back back forward and back again WHEN going forward twice THEN opens the remaining screen after it`() =
        runTest {
            // Given
            prepareScenario(Route.Groups, Route.CreateGroup, Route.Profile)
            history.goBack()
            history.goBack()
            history.goForward()
            history.goBack()
            history.goForward()

            // When
            history.goForward()

            // Then
            assertEquals(
                listOf(Route.Home, Route.Groups, Route.CreateGroup, Route.Profile),
                navigator.backStack.value,
            )
            assertFalse(history.canGoForward.first())
        }

    @Test
    fun `GIVEN every closed screen opened again WHEN going forward THEN the back stack stays`() {
        // Given
        prepareScenario(Route.Groups, Route.CreateGroup)
        history.goBack()
        history.goForward()

        // When
        history.goForward()

        // Then
        assertEquals(listOf(Route.Home, Route.Groups, Route.CreateGroup), navigator.backStack.value)
    }

    private fun prepareScenario(vararg above: Route) {
        navigator = BackStackNavigator(startRoute = Route.Home).also { navigator ->
            above.forEach(navigator::navigate)
        }
        history = ForwardHistory(navigator, scope)
    }
}
