package com.quare.blitzsplit.core.provider.analytics.screen

import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.navigation.Route
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteScreenViewTest {
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
    fun `GIVEN a group just joined on its Saldos WHEN naming its screen THEN sends no params`() {
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
                params = emptyMap(),
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
                params = mapOf("is_edit" to true),
            ),
            screenView,
        )
    }

    @Test
    fun `GIVEN a new bill from the receipt shortcut WHEN naming its screen THEN says only that it is new`() {
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
                params = mapOf("is_edit" to false),
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
        val routes = everyRouteKind

        // When
        val params = routes.map { it.toScreenView().params }

        // Then
        val leaks = params.flatMap { it.entries }.filter { (key, value) -> "secret" in key || "secret" in "$value" }
        assertTrue(leaks.isEmpty(), "Params leaking a route's data: $leaks")
    }

    @Test
    fun `GIVEN every kind of route WHEN naming their screens THEN each kind has a name of its own`() {
        // Given
        val routes = everyRouteKind

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
