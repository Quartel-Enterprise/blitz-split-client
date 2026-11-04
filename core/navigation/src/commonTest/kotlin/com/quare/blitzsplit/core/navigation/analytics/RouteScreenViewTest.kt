package com.quare.blitzsplit.core.navigation.analytics

import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.navigation.BalancesCharge
import com.quare.blitzsplit.core.navigation.Route
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteScreenViewTest {
    // Why: one route of each kind, every id, code, name and key of it marked "secret", so a leak shows by its text.
    private val everyRoute: List<Route> = listOf(
        Route.Welcome,
        Route.ProfileSetup,
        Route.Restore,
        Route.Home,
        Route.Groups,
        Route.Search,
        Route.Profile,
        Route.CreateGroup,
        Route.EditProfile,
        Route.ProfilePhoto,
        Route.DeleteAccount,
        Route.NotificationSettings,
        Route.CategoryMap,
        Route.Invite(
            groupId = "secret-group",
            isNewGroup = true,
        ),
        Route.GroupDetail(
            groupId = "secret-group",
            joinedAs = "secret-name",
            tab = GroupTab.MEMBERS,
        ),
        Route.GroupStats(groupId = "secret-group"),
        Route.EditGroup(groupId = "secret-group"),
        Route.DefaultSplit(groupId = "secret-group"),
        Route.SplitBill(
            groupId = "secret-group",
            expenseId = "secret-expense",
            picksGroupFirst = true,
            scansReceipt = true,
        ),
        Route.BillDetail(
            groupId = "secret-group",
            expenseId = "secret-expense",
            focusCommentId = "secret-comment",
        ),
        Route.ShareBill(
            groupId = "secret-group",
            expenseId = "secret-expense",
        ),
        Route.SharedBill(
            code = "secret-code",
            commentId = "secret-comment",
        ),
        Route.RegisterPayment(
            groupId = "secret-group",
            fromMemberId = "secret-from",
            toMemberId = "secret-to",
            offersReceiverKeys = true,
        ),
        Route.PickPayment,
        Route.GlobalBalances(
            filter = BalancesFilter.OWES_YOU,
            charge = BalancesCharge(
                personKey = "secret-person",
                currency = GroupCurrency.BRL,
            ),
        ),
        Route.Contacts,
        Route.ContactProfile(personKey = "secret-person"),
        Route.ContactsAgenda,
        Route.ContactInvite(
            personKey = "secret-person",
            name = "secret-name",
        ),
        Route.Activity(personKey = "secret-person"),
        Route.SettleUp(
            personKey = "secret-person",
            currency = GroupCurrency.BRL,
        ),
        Route.JoinInvite(
            code = "secret-code",
            memberCode = "secret-member",
            inviterCode = "secret-inviter",
        ),
        Route.JoinWithLink(
            skippedCode = "secret-code",
            skippedMemberCode = "secret-member",
        ),
        Route.JoinPick(code = "secret-code"),
        Route.PaymentKey(
            method = PaymentMethod.PIX,
            keyId = "secret-key",
        ),
    )

    @Test
    fun `GIVEN Inicio WHEN naming its screen THEN is home with no params`() {
        // Given
        val route = Route.Home

        // When
        val screenView = route.toScreenView()

        // Then
        assertEquals(
            ScreenView(
                name = "home",
                params = emptyMap(),
            ),
            screenView,
        )
    }

    @Test
    fun `GIVEN a group just joined WHEN naming its screen THEN sends the tab and that it was a join`() {
        // Given
        val route = Route.GroupDetail(
            groupId = "group-1",
            joinedAs = "Bruno",
            tab = GroupTab.BALANCES,
        )

        // When
        val screenView = route.toScreenView()

        // Then
        assertEquals(
            ScreenView(
                name = "group_detail",
                params = mapOf("tab" to GroupTab.BALANCES, "is_join" to true),
            ),
            screenView,
        )
    }

    @Test
    fun `GIVEN a group opened from the home WHEN naming its screen THEN says it was no join`() {
        // Given
        val route = Route.GroupDetail(
            groupId = "group-1",
            joinedAs = null,
            tab = GroupTab.EXPENSES,
        )

        // When
        val screenView = route.toScreenView()

        // Then
        assertEquals(
            ScreenView(
                name = "group_detail",
                params = mapOf("tab" to GroupTab.EXPENSES, "is_join" to false),
            ),
            screenView,
        )
    }

    @Test
    fun `GIVEN a bill being edited WHEN naming its screen THEN says it is an edit`() {
        // Given
        val route = Route.SplitBill(
            groupId = "group-1",
            expenseId = "expense-1",
            picksGroupFirst = false,
            scansReceipt = false,
        )

        // When
        val screenView = route.toScreenView()

        // Then
        assertEquals(
            ScreenView(
                name = "split_bill",
                params = mapOf("is_edit" to true, "picks_group_first" to false, "scans_receipt" to false),
            ),
            screenView,
        )
    }

    @Test
    fun `GIVEN a new bill from the receipt shortcut WHEN naming its screen THEN says it picks the group and scans`() {
        // Given
        val route = Route.SplitBill(
            groupId = "group-1",
            expenseId = null,
            picksGroupFirst = true,
            scansReceipt = true,
        )

        // When
        val screenView = route.toScreenView()

        // Then
        assertEquals(
            ScreenView(
                name = "split_bill",
                params = mapOf("is_edit" to false, "picks_group_first" to true, "scans_receipt" to true),
            ),
            screenView,
        )
    }

    @Test
    fun `GIVEN a new Pix key WHEN naming its screen THEN sends the method and that it is new`() {
        // Given
        val route = Route.PaymentKey(
            method = PaymentMethod.PIX,
            keyId = null,
        )

        // When
        val screenView = route.toScreenView()

        // Then
        assertEquals(
            ScreenView(
                name = "payment_key",
                params = mapOf("method" to PaymentMethod.PIX, "is_new" to true),
            ),
            screenView,
        )
    }

    @Test
    fun `GIVEN a saved Bitcoin key WHEN naming its screen THEN says it is not new`() {
        // Given
        val route = Route.PaymentKey(
            method = PaymentMethod.BITCOIN,
            keyId = "key-1",
        )

        // When
        val screenView = route.toScreenView()

        // Then
        assertEquals(
            ScreenView(
                name = "payment_key",
                params = mapOf("method" to PaymentMethod.BITCOIN, "is_new" to false),
            ),
            screenView,
        )
    }

    @Test
    fun `GIVEN a personal invite link WHEN naming its screen THEN says it is personal`() {
        // Given
        val route = Route.JoinInvite(
            code = "ABC123",
            memberCode = "MEM456",
            inviterCode = "INV789",
        )

        // When
        val screenView = route.toScreenView()

        // Then
        assertEquals(
            ScreenView(
                name = "join_invite",
                params = mapOf("is_personal" to true),
            ),
            screenView,
        )
    }

    @Test
    fun `GIVEN a group invite link WHEN naming its screen THEN says it is not personal`() {
        // Given
        val route = Route.JoinInvite(
            code = "ABC123",
            memberCode = null,
            inviterCode = null,
        )

        // When
        val screenView = route.toScreenView()

        // Then
        assertEquals(
            ScreenView(
                name = "join_invite",
                params = mapOf("is_personal" to false),
            ),
            screenView,
        )
    }

    @Test
    fun `GIVEN every kind of route WHEN naming their screens THEN no param carries an id code name or key`() {
        // Given
        val routes = everyRoute

        // When
        val params = routes.map { it.toScreenView().params }

        // Then
        val leaks = params.flatMap { it.entries }.filter { (key, value) -> "secret" in key || "secret" in "$value" }
        assertTrue(leaks.isEmpty(), "Params leaking a route's data: $leaks")
    }

    @Test
    fun `GIVEN every kind of route WHEN naming their screens THEN each kind has a name of its own`() {
        // Given
        val routes = everyRoute

        // When
        val names = routes.map { it.toScreenView().name }

        // Then
        assertEquals(
            names.size,
            names.toSet().size,
            "Screen names repeated: ${names.groupBy { it }.filter { it.value.size > 1 }.keys}",
        )
    }
}
