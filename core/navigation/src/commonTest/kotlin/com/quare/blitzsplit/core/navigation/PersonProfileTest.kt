package com.quare.blitzsplit.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class PersonProfileTest {
    private lateinit var navigator: BackStackNavigator

    @Test
    fun `GIVEN Ana in a bill WHEN opening her THEN opens her contact profile over the bill`() {
        // Given
        prepareScenario()

        // When
        navigator.openPersonProfile(personKey = PERSON_KEY, isMe = false)

        // Then
        assertEquals(listOf(Route.Home, BILL, Route.ContactProfile(PERSON_KEY)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN this device's user in a bill WHEN opening them THEN opens Perfil over the bill`() {
        // Given
        prepareScenario()

        // When
        navigator.openPersonProfile(personKey = PERSON_KEY, isMe = true)

        // Then
        assertEquals(listOf(Route.Home, BILL, Route.Profile), navigator.backStack.value)
    }

    private fun prepareScenario() {
        navigator = BackStackNavigator(startRoute = Route.Home).also { it.navigate(BILL) }
    }

    private companion object {
        const val PERSON_KEY = "name:ana"
        val BILL = Route.BillDetail("group-1", "expense-1")
    }
}
