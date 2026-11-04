package com.quare.blitzsplit.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    @Test
    fun `GIVEN the device user without a key WHEN opening them THEN opens Perfil over the bill`() {
        // Given
        prepareScenario()

        // When
        navigator.openPersonProfile(
            personKey = null,
            isMe = true,
        )

        // Then
        assertEquals(
            listOf(Route.Home, Route.BillDetail(GROUP_ID, EXPENSE_ID), Route.Profile),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN someone no longer in the group without a key WHEN opening them THEN the back stack stays as it was`() {
        // Given
        prepareScenario()

        // When
        navigator.openPersonProfile(
            personKey = null,
            isMe = false,
        )

        // Then
        assertEquals(
            listOf(Route.Home, Route.BillDetail(GROUP_ID, EXPENSE_ID)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Ana with a key WHEN asking her profile route THEN is her contact profile`() {
        // Given
        val personKey = PERSON_KEY

        // When
        val route = personProfileRouteOf(personKey, isMe = false)

        // Then
        assertEquals(Route.ContactProfile(PERSON_KEY), route)
    }

    @Test
    fun `GIVEN the device user with a key WHEN asking their profile route THEN is Perfil`() {
        // Given
        val personKey = PERSON_KEY

        // When
        val route = personProfileRouteOf(personKey, isMe = true)

        // Then
        assertEquals(Route.Profile, route)
    }

    @Test
    fun `GIVEN the device user without a key WHEN asking their profile route THEN is Perfil`() {
        // Given
        val personKey: String? = null

        // When
        val route = personProfileRouteOf(personKey, isMe = true)

        // Then
        assertEquals(Route.Profile, route)
    }

    @Test
    fun `GIVEN someone else without a key WHEN asking their profile route THEN there is none`() {
        // Given
        val personKey: String? = null

        // When
        val route = personProfileRouteOf(personKey, isMe = false)

        // Then
        assertNull(route)
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
