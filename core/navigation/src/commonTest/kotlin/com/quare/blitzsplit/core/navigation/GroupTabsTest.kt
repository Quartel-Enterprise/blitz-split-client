package com.quare.blitzsplit.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class GroupTabsTest {
    private lateinit var navigator: BackStackNavigator

    @Test
    fun `GIVEN a tab over the home WHEN opening the other tab THEN takes its place`() {
        // Given
        prepareScenario(Route.Groups, Route.GroupDetail(GROUP_ID))

        // When
        navigator.openGroupTab(
            from = Route.GroupDetail(GROUP_ID),
            tab = Route.Members(GROUP_ID, joinedAs = null),
        )

        // Then
        assertEquals(listOf(Route.Groups, Route.Members(GROUP_ID, joinedAs = null)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Convidar over Despesas WHEN opening Membros THEN puts Membros in place of the group`() {
        // Given
        prepareScenario(Route.Groups, Route.GroupDetail(GROUP_ID), Route.Invite(GROUP_ID, isNewGroup = false))

        // When
        navigator.openGroupTab(
            from = Route.Invite(GROUP_ID, isNewGroup = false),
            tab = Route.Members(GROUP_ID, joinedAs = null),
        )

        // Then
        assertEquals(listOf(Route.Groups, Route.Members(GROUP_ID, joinedAs = null)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Convidar over Membros WHEN opening Despesas THEN puts Despesas in place of the group`() {
        // Given
        prepareScenario(
            Route.Groups,
            Route.Members(GROUP_ID, joinedAs = null),
            Route.Invite(GROUP_ID, isNewGroup = false),
        )

        // When
        navigator.openGroupTab(
            from = Route.Invite(GROUP_ID, isNewGroup = false),
            tab = Route.GroupDetail(GROUP_ID),
        )

        // Then
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Convidar over Saldos WHEN opening Membros THEN puts Membros in place of the group`() {
        // Given
        prepareScenario(Route.Groups, Route.Balances(GROUP_ID), Route.Invite(GROUP_ID, isNewGroup = false))

        // When
        navigator.openGroupTab(
            from = Route.Invite(GROUP_ID, isNewGroup = false),
            tab = Route.Members(GROUP_ID, joinedAs = null),
        )

        // Then
        assertEquals(listOf(Route.Groups, Route.Members(GROUP_ID, joinedAs = null)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Convidar over Membros just joined WHEN opening Membros THEN goes back to it`() {
        // Given
        prepareScenario(
            Route.Groups,
            Route.Members(GROUP_ID, joinedAs = "Bruno"),
            Route.Invite(GROUP_ID, isNewGroup = false),
        )

        // When
        navigator.openGroupTab(
            from = Route.Invite(GROUP_ID, isNewGroup = false),
            tab = Route.Members(GROUP_ID, joinedAs = null),
        )

        // Then
        assertEquals(listOf(Route.Groups, Route.Members(GROUP_ID, joinedAs = "Bruno")), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Membros over Despesas WHEN opening Despesas THEN goes back to it`() {
        // Given
        prepareScenario(Route.Groups, Route.GroupDetail(GROUP_ID), Route.Members(GROUP_ID, joinedAs = null))

        // When
        navigator.openGroupTab(
            from = Route.Members(GROUP_ID, joinedAs = null),
            tab = Route.GroupDetail(GROUP_ID),
        )

        // Then
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Convidar over a tab of another group WHEN opening Despesas THEN takes the place of Convidar`() {
        // Given
        prepareScenario(
            Route.Groups,
            Route.Members(OTHER_GROUP_ID, joinedAs = null),
            Route.Invite(GROUP_ID, isNewGroup = false),
        )

        // When
        navigator.openGroupTab(
            from = Route.Invite(GROUP_ID, isNewGroup = false),
            tab = Route.GroupDetail(GROUP_ID),
        )

        // Then
        assertEquals(
            listOf(Route.Groups, Route.Members(OTHER_GROUP_ID, joinedAs = null), Route.GroupDetail(GROUP_ID)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the screen no longer on top WHEN opening a tab THEN keeps the back stack`() {
        // Given
        prepareScenario(Route.Groups, Route.GroupDetail(GROUP_ID))

        // When
        navigator.openGroupTab(
            from = Route.Invite(GROUP_ID, isNewGroup = false),
            tab = Route.Members(GROUP_ID, joinedAs = null),
        )

        // Then
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
    }

    private fun prepareScenario(vararg routes: Route) {
        navigator = BackStackNavigator(startRoute = routes.first())
        routes.drop(1).forEach(navigator::navigate)
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val OTHER_GROUP_ID = "group-2"
    }
}
