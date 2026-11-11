package com.quare.blitzsplit.core.navigation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnsavedInputTest {
    private lateinit var navigator: BackStackNavigator

    private val form = Route.EditProfile
    private val bill = Route.SplitBill.createNew("group-1")
    private val group = Route.GroupDetail.expensesOf("group-1")

    @Test
    fun `GIVEN a form with unsaved input WHEN going back THEN holds the change and asks`() {
        // Given
        prepareScenario(Route.Profile, form)

        // When
        navigator.navigateBack()

        // Then
        assertEquals(listOf(Route.Home, Route.Profile, form), navigator.backStack.value)
        assertTrue(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN a held back WHEN confirming the discard THEN applies it and stops asking`() {
        // Given
        prepareScenario(Route.Profile, form)
        navigator.navigateBack()

        // When
        navigator.confirmDiscard()

        // Then
        assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
        assertFalse(navigator.hasUnsavedInput.value)
    }

    @Test
    fun `GIVEN a held back WHEN the form becomes clean THEN the prompt is dropped`() {
        // Given
        prepareScenario(Route.Profile, form)
        navigator.navigateBack()

        // When
        navigator.markUnsaved(form, false)

        // Then
        assertFalse(navigator.pendingDiscard.value)
        assertEquals(listOf(Route.Home, Route.Profile, form), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a held back WHEN cancelling the discard THEN keeps the stack and the form unsaved`() {
        // Given
        prepareScenario(Route.Profile, form)
        navigator.navigateBack()

        // When
        navigator.cancelDiscard()

        // Then
        assertEquals(listOf(Route.Home, Route.Profile, form), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
        assertTrue(navigator.hasUnsavedInput.value)
    }

    @Test
    fun `GIVEN a form with unsaved input WHEN replacing the top THEN holds the change`() {
        // Given
        prepareScenario(form)

        // When
        navigator.replaceTop(Route.Profile)

        // Then
        assertEquals(listOf(Route.Home, form), navigator.backStack.value)
        assertTrue(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN a held replaceTop WHEN confirming THEN applies exactly the held stack`() {
        // Given
        prepareScenario(form)
        navigator.replaceTop(Route.Profile)

        // When
        navigator.confirmDiscard()

        // Then
        assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a form with unsaved input WHEN the shell replaces above a home tab THEN holds the change`() {
        // Given
        prepareScenario(form)

        // When
        navigator.replaceAbove(1, Route.Groups)

        // Then
        assertEquals(listOf(Route.Home, form), navigator.backStack.value)
        assertTrue(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN a held replaceAbove WHEN confirming THEN applies exactly the held stack`() {
        // Given
        prepareScenario(form)
        navigator.replaceAbove(1, listOf(Route.Groups, Route.Profile))

        // When
        navigator.confirmDiscard()

        // Then
        assertEquals(listOf(Route.Home, Route.Groups, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a form with unsaved input WHEN popping to below it THEN holds the change`() {
        // Given
        prepareScenario(form)

        // When
        navigator.popTo(1)

        // Then
        assertEquals(listOf(Route.Home, form), navigator.backStack.value)
        assertTrue(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN a held popTo WHEN confirming THEN applies it`() {
        // Given
        prepareScenario(form)
        navigator.popTo(1)

        // When
        navigator.confirmDiscard()

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a form with unsaved input WHEN navigating to a route below it THEN holds the change`() {
        // Given
        prepareScenario(Route.Profile, form)

        // When
        navigator.navigate(Route.Profile)

        // Then
        assertEquals(listOf(Route.Home, Route.Profile, form), navigator.backStack.value)
        assertTrue(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN a held navigate below the form WHEN confirming THEN goes back to that route`() {
        // Given
        prepareScenario(Route.Profile, form)
        navigator.navigate(Route.Profile)

        // When
        navigator.confirmDiscard()

        // Then
        assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a form with unsaved input WHEN pushing a screen over it THEN applies at once`() {
        // Given
        prepareScenario(form)

        // When
        navigator.navigate(Route.CategoryMap)

        // Then
        assertEquals(listOf(Route.Home, form, Route.CategoryMap), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
        assertTrue(navigator.hasUnsavedInput.value)
    }

    @Test
    fun `GIVEN a form with unsaved input WHEN navigating to the form itself THEN applies at once`() {
        // Given
        prepareScenario(form)

        // When
        navigator.navigate(form)

        // Then
        assertEquals(listOf(Route.Home, form), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN a screen over an unsaved form WHEN going back from it THEN pops it without asking`() {
        // Given
        prepareScenario(form, Route.CategoryMap)

        // When
        navigator.navigateBack()

        // Then
        assertEquals(listOf(Route.Home, form), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN a group over the bill form WHEN popping only the group THEN pops without asking`() {
        // Given
        prepareScenario(bill, group)

        // When
        navigator.popTo(2)

        // Then
        assertEquals(listOf(Route.Home, bill), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN a group over the bill form WHEN popping to below the form THEN holds the change`() {
        // Given
        prepareScenario(bill, group)

        // When
        navigator.popTo(1)

        // Then
        assertEquals(listOf(Route.Home, bill, group), navigator.backStack.value)
        assertTrue(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN a held change WHEN another change is requested THEN the last request wins`() {
        // Given
        prepareScenario(Route.Profile, form)
        navigator.navigateBack()
        navigator.popTo(1)

        // When
        navigator.confirmDiscard()

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN a held change WHEN a change that keeps the form is requested THEN applies it and drops the held one`() {
        // Given
        prepareScenario(form)
        navigator.navigateBack()

        // When
        navigator.navigate(Route.CategoryMap)

        // Then
        assertEquals(listOf(Route.Home, form, Route.CategoryMap), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
        navigator.confirmDiscard()
        assertEquals(listOf(Route.Home, form, Route.CategoryMap), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a form marked saved WHEN going back THEN lets the exit through`() {
        // Given
        prepareScenario(form)
        navigator.markUnsaved(form, hasUnsavedInput = false)

        // When
        navigator.navigateBack()

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
        assertFalse(navigator.hasUnsavedInput.value)
    }

    @Test
    fun `GIVEN an unsaved form closed by a confirmed discard WHEN opening it again and going back THEN does not ask`() {
        // Given
        prepareScenario(form)
        navigator.navigateBack()
        navigator.confirmDiscard()
        navigator.navigate(form)

        // When
        navigator.navigateBack()

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN a route that is not on the stack WHEN marking it unsaved THEN nothing is marked`() {
        // Given
        prepareScenario()

        // When
        navigator.markUnsaved(form, hasUnsavedInput = true)

        // Then
        assertFalse(navigator.hasUnsavedInput.value)
        navigator.navigate(form)
        navigator.navigateBack()
        assertEquals(listOf(Route.Home), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN two unsaved forms WHEN one is marked saved THEN input stays unsaved while the other is`() {
        // Given
        prepareScenario(form, Route.CreateGroup)
        navigator.markUnsaved(Route.CreateGroup, hasUnsavedInput = true)

        // When
        navigator.markUnsaved(form, hasUnsavedInput = false)

        // Then
        assertTrue(navigator.hasUnsavedInput.value)
        navigator.markUnsaved(Route.CreateGroup, hasUnsavedInput = false)
        assertFalse(navigator.hasUnsavedInput.value)
    }

    @Test
    fun `GIVEN no unsaved form WHEN checking hasUnsavedInput THEN is false`() {
        // Given
        prepareScenario(form)

        // When
        navigator.markUnsaved(form, hasUnsavedInput = false)

        // Then
        assertFalse(navigator.hasUnsavedInput.value)
    }

    @Test
    fun `GIVEN an unsaved form WHEN checking hasUnsavedInput THEN is true`() {
        // Given
        prepareScenario(form)

        // When
        navigator.markUnsaved(form, hasUnsavedInput = true)

        // Then
        assertTrue(navigator.hasUnsavedInput.value)
    }

    @Test
    fun `GIVEN a held change WHEN replacing all THEN starts over without asking and clears every mark`() {
        // Given
        prepareScenario(Route.Profile, form)
        navigator.navigateBack()

        // When
        navigator.replaceAll(Route.Groups)

        // Then
        assertEquals(listOf(Route.Groups), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
        assertFalse(navigator.hasUnsavedInput.value)
        navigator.confirmDiscard()
        assertEquals(listOf(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN an unsaved form WHEN replacing all THEN a form pushed later does not inherit the mark`() {
        // Given
        prepareScenario(form)
        navigator.replaceAll(Route.Groups)
        navigator.navigate(form)

        // When
        navigator.navigateBack()

        // Then
        assertEquals(listOf(Route.Groups), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN nothing held WHEN confirming THEN nothing changes`() {
        // Given
        prepareScenario(Route.Profile)

        // When
        navigator.confirmDiscard()

        // Then
        assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN nothing held WHEN cancelling THEN nothing changes`() {
        // Given
        prepareScenario(Route.Profile)

        // When
        navigator.cancelDiscard()

        // Then
        assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN a form guarded by a flow WHEN the input appears THEN back is held`() =
        runTest(UnconfinedTestDispatcher()) {
            // Given
            prepareScenario(form)
            navigator.markUnsaved(form, hasUnsavedInput = false)
            val input = MutableStateFlow(false)
            navigator.guardUnsavedInput(backgroundScope, form, input)

            // When
            input.value = true
            navigator.navigateBack()

            // Then
            assertTrue(navigator.pendingDiscard.value)
            assertEquals(listOf(Route.Home, form), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a form guarded by a flow WHEN the input is cleared THEN back goes through`() =
        runTest(UnconfinedTestDispatcher()) {
            // Given
            prepareScenario(form)
            val input = MutableStateFlow(true)
            navigator.guardUnsavedInput(backgroundScope, form, input)
            input.value = false

            // When
            navigator.navigateBack()

            // Then
            assertFalse(navigator.pendingDiscard.value)
            assertEquals(listOf(Route.Home), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a form guarded by a flow WHEN its scope is cancelled THEN later input is no longer tracked`() =
        runTest(UnconfinedTestDispatcher()) {
            // Given
            prepareScenario(form)
            navigator.markUnsaved(form, hasUnsavedInput = false)
            val input = MutableStateFlow(false)
            val scope = backgroundScope.let {
                kotlinx.coroutines.CoroutineScope(
                    it.coroutineContext + kotlinx.coroutines.Job(),
                )
            }
            navigator.guardUnsavedInput(scope, form, input)
            scope.coroutineContext[kotlinx.coroutines.Job]!!.cancel()

            // When
            input.value = true

            // Then
            assertFalse(navigator.hasUnsavedInput.value)
        }

    // Why: each form is on the stack, marked unsaved as a form with typed input would be.
    private fun prepareScenario(vararg above: Route) {
        navigator = BackStackNavigator(startRoute = Route.Home).also { navigator ->
            above.forEach { route ->
                navigator.navigate(route)
                navigator.markUnsaved(route, hasUnsavedInput = route == form || route == bill)
            }
        }
    }
}
