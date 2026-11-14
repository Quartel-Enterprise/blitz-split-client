package com.quare.blitzsplit.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TestTimeSource

class SharedBillLeftForSignInTest {
    private val timeSource = TestTimeSource()
    private val left = SharedBillLeftForSignIn(timeSource)
    private val navigator = BackStackNavigator(startRoute = Route.Welcome)
    private val page = Route.SharedBill(
        code = "7Hk2pQ8x",
        commentId = "comment",
    )

    @Test
    fun `GIVEN no page left WHEN taking it THEN there is none`() {
        // Given
        val window = 15.minutes

        // When
        val taken = left.take(window)

        // Then
        assertNull(taken)
    }

    @Test
    fun `GIVEN a page open over the welcome WHEN leaving it to sign in THEN the welcome takes the stack`() {
        // Given
        navigator.navigate(page)

        // When
        left.leaveForWelcome(page, navigator)

        // Then
        assertEquals(listOf<Route>(Route.Welcome), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a page left within the window WHEN taking it THEN gives the page`() {
        // Given
        left.leaveForWelcome(page, navigator)
        timeSource += 15.minutes

        // When
        val taken = left.take(15.minutes)

        // Then
        assertEquals(page, taken)
    }

    @Test
    fun `GIVEN a page left past the window WHEN taking it THEN there is none`() {
        // Given
        left.leaveForWelcome(page, navigator)
        timeSource += 16.minutes

        // When
        val taken = left.take(15.minutes)

        // Then
        assertNull(taken)
    }

    @Test
    fun `GIVEN a page already taken WHEN taking it again THEN there is none`() {
        // Given
        left.leaveForWelcome(page, navigator)
        left.take(15.minutes)

        // When
        val taken = left.take(15.minutes)

        // Then
        assertNull(taken)
    }

    @Test
    fun `GIVEN a page taken past the window WHEN taking it again in time THEN there is still none`() {
        // Given
        left.leaveForWelcome(page, navigator)
        timeSource += 16.minutes
        left.take(15.minutes)

        // When
        val taken = left.take(60.minutes)

        // Then
        assertNull(taken)
    }
}
