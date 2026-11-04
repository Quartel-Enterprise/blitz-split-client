package com.quare.blitzsplit.core.provider.analytics.screen

import com.quare.blitzsplit.core.navigation.Route
import kotlin.test.Test
import kotlin.test.assertEquals

class EveryRouteKindTest {
    @Test
    fun `GIVEN every kind of Route WHEN listing the samples RouteScreenViewTest checks THEN none is missing`() {
        // Given
        val kinds = Route::class.sealedSubclasses.toSet()

        // When
        val sampled = everyRouteKind.map { it::class }.toSet()

        // Then
        assertEquals(emptySet(), kinds - sampled, "Route kinds missing from everyRouteKind")
    }
}
