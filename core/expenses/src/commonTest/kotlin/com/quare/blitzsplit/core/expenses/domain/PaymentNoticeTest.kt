package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.expenses.domain.model.RecordedPayment
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaymentNoticeTest {
    @Test
    fun `GIVEN Ana paid Bruno WHEN Pierre reads the notice THEN names who paid and received and Pierre did not pay`() {
        // Given
        val settlement = anaPaysBruno

        // When
        val notice = PaymentNotice.of(
            settlement,
            meId = PIERRE,
            nameOf = names::get,
        )

        // Then
        assertEquals(
            PaymentNotice(
                settlementIds = listOf(SETTLEMENT_ID),
                fromName = "Ana",
                fromIsMe = false,
                toName = "Bruno",
            ),
            notice,
        )
    }

    @Test
    fun `GIVEN Ana paid Bruno WHEN Ana reads the notice THEN she is the one who paid`() {
        // Given
        val settlement = anaPaysBruno

        // When
        val notice = PaymentNotice.of(
            settlement,
            meId = ANA,
            nameOf = names::get,
        )

        // Then
        assertTrue(notice.fromIsMe)
    }

    @Test
    fun `GIVEN Bruno received the payment WHEN Bruno reads the notice THEN he is not the one who paid`() {
        // Given
        val settlement = anaPaysBruno

        // When
        val notice = PaymentNotice.of(
            settlement,
            meId = BRUNO,
            nameOf = names::get,
        )

        // Then
        assertFalse(notice.fromIsMe)
    }

    @Test
    fun `GIVEN a device with no member in the group WHEN reading the notice THEN nobody is me`() {
        // Given
        val settlement = anaPaysBruno

        // When
        val notice = PaymentNotice.of(
            settlement,
            meId = null,
            nameOf = names::get,
        )

        // Then
        assertFalse(notice.fromIsMe)
    }

    @Test
    fun `GIVEN who paid is no longer in the group WHEN reading the notice THEN their name is missing`() {
        // Given
        val settlement = anaPaysBruno
        val namesWithoutAna = names - ANA

        // When
        val notice = PaymentNotice.of(
            settlement,
            meId = PIERRE,
            nameOf = namesWithoutAna::get,
        )

        // Then
        assertNull(notice.fromName)
        assertEquals("Bruno", notice.toName)
    }

    @Test
    fun `GIVEN who received is no longer in the group WHEN reading the notice THEN their name is missing`() {
        // Given
        val settlement = anaPaysBruno
        val namesWithoutBruno = names - BRUNO

        // When
        val notice = PaymentNotice.of(
            settlement,
            meId = PIERRE,
            nameOf = namesWithoutBruno::get,
        )

        // Then
        assertEquals("Ana", notice.fromName)
        assertNull(notice.toName)
    }

    @Test
    fun `GIVEN Carla paid this device user across groups WHEN reading the notice THEN names her as who paid`() {
        // Given
        val settledUp = RecordedPayment.SettledUp(
            settlementIds = listOf("p1", "p2"),
            personName = "Carla",
            isPaidByMe = false,
        )

        // When
        val notice = PaymentNotice.of(settledUp)

        // Then
        assertEquals(
            PaymentNotice(
                settlementIds = listOf("p1", "p2"),
                fromName = "Carla",
                fromIsMe = false,
                toName = null,
            ),
            notice,
        )
        assertEquals("p1", notice.settlementId)
    }

    @Test
    fun `GIVEN this device user paid Bruno across groups WHEN reading the notice THEN names him as who received`() {
        // Given
        val settledUp = RecordedPayment.SettledUp(
            settlementIds = listOf("p1"),
            personName = "Bruno",
            isPaidByMe = true,
        )

        // When
        val notice = PaymentNotice.of(settledUp)

        // Then
        assertTrue(notice.fromIsMe)
        assertNull(notice.fromName)
        assertEquals("Bruno", notice.toName)
    }

    private val names = mapOf(
        PIERRE to "Pierre",
        ANA to "Ana",
        BRUNO to "Bruno",
    )

    private val anaPaysBruno = Settlement(
        id = SETTLEMENT_ID,
        groupId = "group-1",
        fromMemberId = ANA,
        toMemberId = BRUNO,
        amountMinor = 5_000L,
        method = SettlementMethod.PIX,
        paidAt = 1_000L,
        isPending = true,
        updatedAt = 0L,
    )

    private companion object {
        const val SETTLEMENT_ID = "payment-1"
        const val PIERRE = "pierre"
        const val ANA = "ana"
        const val BRUNO = "bruno"
    }
}
