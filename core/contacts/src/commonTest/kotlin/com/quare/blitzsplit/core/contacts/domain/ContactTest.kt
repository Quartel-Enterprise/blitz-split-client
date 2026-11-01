package com.quare.blitzsplit.core.contacts.domain

import com.quare.blitzsplit.core.contacts.contact
import com.quare.blitzsplit.core.contacts.domain.model.ContactNet
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContactTest {
    @Test
    fun `GIVEN only another currency open WHEN reading the net THEN shows the open one`() {
        // Given
        val carla = contact(
            key = "user:carla",
            name = "Carla",
            userId = "carla",
            nets = listOf(ContactNet(GroupCurrency.BRL, 0L), ContactNet(GroupCurrency.EUR, 700L)),
        )

        // When
        val net = carla.net

        // Then
        assertEquals(ContactNet(GroupCurrency.EUR, 700L), net)
        assertFalse(carla.isSettled)
    }

    @Test
    fun `GIVEN nothing open in any currency WHEN reading the net THEN shows the main currency at zero`() {
        // Given
        val carla = contact(
            key = "user:carla",
            name = "Carla",
            userId = "carla",
            nets = listOf(ContactNet(GroupCurrency.BRL, 0L), ContactNet(GroupCurrency.EUR, 0L)),
        )

        // When
        val net = carla.net

        // Then
        assertEquals(ContactNet(GroupCurrency.BRL, 0L), net)
        assertTrue(carla.isSettled)
    }

    @Test
    fun `GIVEN something open in both currencies WHEN reading the net THEN shows the main currency`() {
        // Given
        val carla = contact(
            key = "user:carla",
            name = "Carla",
            userId = "carla",
            nets = listOf(ContactNet(GroupCurrency.BRL, -300L), ContactNet(GroupCurrency.EUR, 700L)),
        )

        // When
        val net = carla.net

        // Then
        assertEquals(ContactNet(GroupCurrency.BRL, -300L), net)
    }

    @Test
    fun `GIVEN a name nobody joined as WHEN reading whether it is a guest THEN it is`() {
        // Given
        val diego = contact(
            key = "name:diego",
            name = "Diego",
        )

        // When
        val isGuest = diego.isGuest

        // Then
        assertTrue(isGuest)
    }
}
