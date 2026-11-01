package com.quare.blitzsplit.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupTabsTest {
    private lateinit var navigator: BackStackNavigator

    @Test
    fun `GIVEN a tab over the home WHEN opening the other tab THEN takes its place`() {
        // Given
        prepareScenario(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null))

        // When
        navigator.openGroupTab(
            from = Route.GroupDetail(GROUP_ID, joinedAs = null),
            tab = Route.Members(GROUP_ID),
        )

        // Then
        assertEquals(listOf(Route.Groups, Route.Members(GROUP_ID)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Convidar over Despesas WHEN opening Membros THEN puts Membros in place of the group`() {
        // Given
        prepareScenario(
            Route.Groups,
            Route.GroupDetail(GROUP_ID, joinedAs = null),
            Route.Invite(GROUP_ID, isNewGroup = false),
        )

        // When
        navigator.openGroupTab(
            from = Route.Invite(GROUP_ID, isNewGroup = false),
            tab = Route.Members(GROUP_ID),
        )

        // Then
        assertEquals(listOf(Route.Groups, Route.Members(GROUP_ID)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Convidar over Membros WHEN opening Despesas THEN puts Despesas in place of the group`() {
        // Given
        prepareScenario(
            Route.Groups,
            Route.Members(GROUP_ID),
            Route.Invite(GROUP_ID, isNewGroup = false),
        )

        // When
        navigator.openGroupTab(
            from = Route.Invite(GROUP_ID, isNewGroup = false),
            tab = Route.GroupDetail(GROUP_ID, joinedAs = null),
        )

        // Then
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Convidar over Saldos WHEN opening Membros THEN puts Membros in place of the group`() {
        // Given
        prepareScenario(Route.Groups, Route.Balances(GROUP_ID), Route.Invite(GROUP_ID, isNewGroup = false))

        // When
        navigator.openGroupTab(
            from = Route.Invite(GROUP_ID, isNewGroup = false),
            tab = Route.Members(GROUP_ID),
        )

        // Then
        assertEquals(listOf(Route.Groups, Route.Members(GROUP_ID)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Convidar over Despesas just joined WHEN opening Despesas THEN goes back to it`() {
        // Given
        prepareScenario(
            Route.Groups,
            Route.GroupDetail(GROUP_ID, joinedAs = "Bruno"),
            Route.Invite(GROUP_ID, isNewGroup = false),
        )

        // When
        navigator.openGroupTab(
            from = Route.Invite(GROUP_ID, isNewGroup = false),
            tab = Route.GroupDetail(GROUP_ID, joinedAs = null),
        )

        // Then
        assertEquals(
            listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = "Bruno")),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Membros over Despesas WHEN opening Despesas THEN goes back to it`() {
        // Given
        prepareScenario(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null), Route.Members(GROUP_ID))

        // When
        navigator.openGroupTab(
            from = Route.Members(GROUP_ID),
            tab = Route.GroupDetail(GROUP_ID, joinedAs = null),
        )

        // Then
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Convidar over a tab of another group WHEN opening Despesas THEN takes the place of Convidar`() {
        // Given
        prepareScenario(
            Route.Groups,
            Route.Members(OTHER_GROUP_ID),
            Route.Invite(GROUP_ID, isNewGroup = false),
        )

        // When
        navigator.openGroupTab(
            from = Route.Invite(GROUP_ID, isNewGroup = false),
            tab = Route.GroupDetail(GROUP_ID, joinedAs = null),
        )

        // Then
        assertEquals(
            listOf(Route.Groups, Route.Members(OTHER_GROUP_ID), Route.GroupDetail(GROUP_ID, joinedAs = null)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the screen no longer on top WHEN opening a tab THEN keeps the back stack`() {
        // Given
        prepareScenario(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null))

        // When
        navigator.openGroupTab(
            from = Route.Invite(GROUP_ID, isNewGroup = false),
            tab = Route.Members(GROUP_ID),
        )

        // Then
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Despesas WHEN moving to Saldos of the same group THEN is a tab switch`() {
        // Given
        val from = Route.GroupDetail(GROUP_ID, joinedAs = null)
        val to = Route.Balances(GROUP_ID)

        // When
        val isSwitch = isGroupTabSwitch(
            from = from,
            to = to,
        )

        // Then
        assertTrue(isSwitch)
    }

    @Test
    fun `GIVEN Saldos WHEN moving to Membros of the same group THEN is a tab switch`() {
        // Given
        val from = Route.Balances(GROUP_ID)
        val to = Route.Members(GROUP_ID)

        // When
        val isSwitch = isGroupTabSwitch(
            from = from,
            to = to,
        )

        // Then
        assertTrue(isSwitch)
    }

    @Test
    fun `GIVEN Membros WHEN moving to Despesas of the same group THEN is a tab switch`() {
        // Given
        val from = Route.Members(GROUP_ID)
        val to = Route.GroupDetail(GROUP_ID, joinedAs = null)

        // When
        val isSwitch = isGroupTabSwitch(
            from = from,
            to = to,
        )

        // Then
        assertTrue(isSwitch)
    }

    @Test
    fun `GIVEN Saldos WHEN moving to Despesas of the same group THEN is a tab switch`() {
        // Given
        val from = Route.Balances(GROUP_ID)
        val to = Route.GroupDetail(GROUP_ID, joinedAs = null)

        // When
        val isSwitch = isGroupTabSwitch(
            from = from,
            to = to,
        )

        // Then
        assertTrue(isSwitch)
    }

    @Test
    fun `GIVEN Despesas just joined WHEN moving to Despesas of the same group THEN is a tab switch`() {
        // Given
        val from = Route.GroupDetail(GROUP_ID, joinedAs = "Bruno")
        val to = Route.GroupDetail(GROUP_ID, joinedAs = null)

        // When
        val isSwitch = isGroupTabSwitch(
            from = from,
            to = to,
        )

        // Then
        assertTrue(isSwitch)
    }

    @Test
    fun `GIVEN Despesas WHEN moving to Despesas of another group THEN is not a tab switch`() {
        // Given
        val from = Route.GroupDetail(GROUP_ID, joinedAs = null)
        val to = Route.GroupDetail(OTHER_GROUP_ID, joinedAs = null)

        // When
        val isSwitch = isGroupTabSwitch(
            from = from,
            to = to,
        )

        // Then
        assertFalse(isSwitch)
    }

    @Test
    fun `GIVEN Saldos WHEN moving to Membros of another group THEN is not a tab switch`() {
        // Given
        val from = Route.Balances(GROUP_ID)
        val to = Route.Members(OTHER_GROUP_ID)

        // When
        val isSwitch = isGroupTabSwitch(
            from = from,
            to = to,
        )

        // Then
        assertFalse(isSwitch)
    }

    @Test
    fun `GIVEN Despesas WHEN opening Convidar of the same group THEN is not a tab switch`() {
        // Given
        val from = Route.GroupDetail(GROUP_ID, joinedAs = null)
        val to = Route.Invite(GROUP_ID, isNewGroup = false)

        // When
        val isSwitch = isGroupTabSwitch(
            from = from,
            to = to,
        )

        // Then
        assertFalse(isSwitch)
    }

    @Test
    fun `GIVEN Convidar WHEN moving to Membros of the same group THEN is not a tab switch`() {
        // Given
        val from = Route.Invite(GROUP_ID, isNewGroup = false)
        val to = Route.Members(GROUP_ID)

        // When
        val isSwitch = isGroupTabSwitch(
            from = from,
            to = to,
        )

        // Then
        assertFalse(isSwitch)
    }

    @Test
    fun `GIVEN Grupos WHEN opening Despesas THEN is not a tab switch`() {
        // Given
        val from = Route.Groups
        val to = Route.GroupDetail(GROUP_ID, joinedAs = null)

        // When
        val isSwitch = isGroupTabSwitch(
            from = from,
            to = to,
        )

        // Then
        assertFalse(isSwitch)
    }

    @Test
    fun `GIVEN Saldos WHEN going back to Grupos THEN is not a tab switch`() {
        // Given
        val from = Route.Balances(GROUP_ID)
        val to = Route.Groups

        // When
        val isSwitch = isGroupTabSwitch(
            from = from,
            to = to,
        )

        // Then
        assertFalse(isSwitch)
    }

    @Test
    fun `GIVEN Despesas WHEN opening a bill of the same group THEN is not a tab switch`() {
        // Given
        val from = Route.GroupDetail(GROUP_ID, joinedAs = null)
        val to = Route.BillDetail(GROUP_ID, expenseId = "bill-1")

        // When
        val isSwitch = isGroupTabSwitch(
            from = from,
            to = to,
        )

        // Then
        assertFalse(isSwitch)
    }

    @Test
    fun `GIVEN a bill WHEN going back to Despesas of the same group THEN is not a tab switch`() {
        // Given
        val from = Route.BillDetail(GROUP_ID, expenseId = "bill-1")
        val to = Route.GroupDetail(GROUP_ID, joinedAs = null)

        // When
        val isSwitch = isGroupTabSwitch(
            from = from,
            to = to,
        )

        // Then
        assertFalse(isSwitch)
    }

    @Test
    fun `GIVEN Membros WHEN the top stays Membros of the same group THEN is not a tab switch`() {
        // Given
        val from = Route.Members(GROUP_ID)
        val to = Route.Members(GROUP_ID)

        // When
        val isSwitch = isGroupTabSwitch(
            from = from,
            to = to,
        )

        // Then
        assertFalse(isSwitch)
    }

    @Test
    fun `GIVEN Despesas WHEN the top stays the same Despesas THEN is not a tab switch`() {
        // Given
        val from = Route.GroupDetail(GROUP_ID, joinedAs = null)
        val to = Route.GroupDetail(GROUP_ID, joinedAs = null)

        // When
        val isSwitch = isGroupTabSwitch(
            from = from,
            to = to,
        )

        // Then
        assertFalse(isSwitch)
    }

    @Test
    fun `GIVEN two screens that are not tabs WHEN moving between them THEN is not a tab switch`() {
        // Given
        val from = Route.Groups
        val to = Route.Home

        // When
        val isSwitch = isGroupTabSwitch(
            from = from,
            to = to,
        )

        // Then
        assertFalse(isSwitch)
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
