package com.quare.blitzsplit.core.navigation

import com.quare.blitzsplit.core.model.GroupTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GroupTabsTest {
    private lateinit var navigator: BackStackNavigator

    @Test
    fun `GIVEN Convidar over the home WHEN opening the group on Membros THEN the group takes its place`() {
        // Given
        prepareScenario(Route.Groups, Route.Invite(GROUP_ID, isNewGroup = true))

        // When
        navigator.openGroup(
            from = Route.Invite(GROUP_ID, isNewGroup = true),
            groupId = GROUP_ID,
            tab = GroupTab.MEMBERS,
        )

        // Then
        assertEquals(
            listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null, GroupTab.MEMBERS)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Convidar over Despesas WHEN opening Despesas THEN goes back to it`() {
        // Given
        prepareScenario(
            Route.Groups,
            Route.GroupDetail(
                GROUP_ID,
                joinedAs = null,
                tab = GroupTab.EXPENSES,
            ),
            Route.Invite(GROUP_ID, isNewGroup = false),
        )

        // When
        navigator.openGroup(
            from = Route.Invite(GROUP_ID, isNewGroup = false),
            groupId = GROUP_ID,
            tab = GroupTab.EXPENSES,
        )

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Convidar over Despesas just joined WHEN opening Despesas THEN goes back to it`() {
        // Given
        prepareScenario(
            Route.Groups,
            Route.GroupDetail(
                GROUP_ID,
                joinedAs = "Bruno",
                tab = GroupTab.EXPENSES,
            ),
            Route.Invite(GROUP_ID, isNewGroup = false),
        )

        // When
        navigator.openGroup(
            from = Route.Invite(GROUP_ID, isNewGroup = false),
            groupId = GROUP_ID,
            tab = GroupTab.EXPENSES,
        )

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = "Bruno",
                    tab = GroupTab.EXPENSES,
                ),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Convidar over Despesas WHEN opening Membros THEN puts the group on Membros in its place`() {
        // Given
        prepareScenario(
            Route.Groups,
            Route.GroupDetail(
                GROUP_ID,
                joinedAs = null,
                tab = GroupTab.EXPENSES,
            ),
            Route.Invite(GROUP_ID, isNewGroup = false),
        )

        // When
        navigator.openGroup(
            from = Route.Invite(GROUP_ID, isNewGroup = false),
            groupId = GROUP_ID,
            tab = GroupTab.MEMBERS,
        )

        // Then
        assertEquals(
            listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null, GroupTab.MEMBERS)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Convidar over the group opened on Membros WHEN opening Membros THEN goes back to it`() {
        // Given
        prepareScenario(
            Route.Groups,
            Route.GroupDetail(GROUP_ID, joinedAs = null, GroupTab.MEMBERS),
            Route.Invite(GROUP_ID, isNewGroup = false),
        )

        // When
        navigator.openGroup(
            from = Route.Invite(GROUP_ID, isNewGroup = false),
            groupId = GROUP_ID,
            tab = GroupTab.MEMBERS,
        )

        // Then
        assertEquals(
            listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null, GroupTab.MEMBERS)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Convidar over another group WHEN opening the group THEN takes the place of Convidar`() {
        // Given
        prepareScenario(
            Route.Groups,
            Route.GroupDetail(
                OTHER_GROUP_ID,
                joinedAs = null,
                tab = GroupTab.EXPENSES,
            ),
            Route.Invite(GROUP_ID, isNewGroup = false),
        )

        // When
        navigator.openGroup(
            from = Route.Invite(GROUP_ID, isNewGroup = false),
            groupId = GROUP_ID,
            tab = GroupTab.EXPENSES,
        )

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    OTHER_GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the screen no longer on top WHEN opening the group THEN keeps the back stack`() {
        // Given
        prepareScenario(
            Route.Groups,
            Route.GroupDetail(
                GROUP_ID,
                joinedAs = null,
                tab = GroupTab.EXPENSES,
            ),
        )

        // When
        navigator.openGroup(
            from = Route.Invite(GROUP_ID, isNewGroup = false),
            groupId = GROUP_ID,
            tab = GroupTab.MEMBERS,
        )

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the group screen WHEN reading its group THEN is the group`() {
        // Given
        val route = Route.GroupDetail(GROUP_ID, joinedAs = null, GroupTab.BALANCES)

        // When
        val groupId = groupScreenIdOrNull(route)

        // Then
        assertEquals(GROUP_ID, groupId)
    }

    @Test
    fun `GIVEN another screen of the group WHEN reading its group THEN is null`() {
        // Given
        val route = Route.Invite(GROUP_ID, isNewGroup = false)

        // When
        val groupId = groupScreenIdOrNull(route)

        // Then
        assertNull(groupId)
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
