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
        navigator.openPersonProfile(
            personKey = PERSON_KEY,
            isMe = false,
        )

        // Then
        assertEquals(
            listOf(Route.Home, Route.BillDetail(GROUP_ID, EXPENSE_ID), Route.ContactProfile(PERSON_KEY)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the device user in a bill WHEN opening them THEN opens Perfil over the bill`() {
        // Given
        prepareScenario()

        // When
        navigator.openPersonProfile(
            personKey = PERSON_KEY,
            isMe = true,
        )

        // Then
        assertEquals(
            listOf(Route.Home, Route.BillDetail(GROUP_ID, EXPENSE_ID), Route.Profile),
            navigator.backStack.value,
        )
    }

    private fun prepareScenario() {
        navigator =
            BackStackNavigator(startRoute = Route.Home).also { it.navigate(Route.BillDetail(GROUP_ID, EXPENSE_ID)) }
    }

    private companion object {
        const val PERSON_KEY = "name:ana"
        const val GROUP_ID = "group-1"
        const val EXPENSE_ID = "expense-1"
    }
}
