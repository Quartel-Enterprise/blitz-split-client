package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import kotlin.test.Test
import kotlin.test.assertEquals

class SettlementMethodTest {
    @Test
    fun `GIVEN the codes the server stores WHEN reading them THEN gets every method`() {
        // Given
        val codes = listOf("pix", "cash", "transfer", "other")

        // When
        val methods = codes.map(SettlementMethod::fromCode)

        // Then
        assertEquals(
            listOf(SettlementMethod.PIX, SettlementMethod.CASH, SettlementMethod.TRANSFER, SettlementMethod.OTHER),
            methods,
        )
    }

    @Test
    fun `GIVEN a method a later version added WHEN reading its code THEN it reads as other`() {
        // Given
        val code = "crypto"

        // When
        val method = SettlementMethod.fromCode(code)

        // Then
        assertEquals(SettlementMethod.OTHER, method)
    }

    @Test
    fun `GIVEN every method WHEN reading back its own code THEN gets the same method`() {
        // Given
        val methods = SettlementMethod.entries

        // When
        val readBack = methods.map { SettlementMethod.fromCode(it.code) }

        // Then
        assertEquals(methods, readBack)
    }
}
