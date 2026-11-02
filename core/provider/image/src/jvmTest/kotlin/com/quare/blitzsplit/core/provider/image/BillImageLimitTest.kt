package com.quare.blitzsplit.core.provider.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BillImageLimitTest {
    @Test
    fun `GIVEN a photo over the limit at every quality WHEN encoding THEN throws ImageTooLargeException`() {
        // Given
        val tried = mutableListOf<Int>()

        // When
        val error = runCatching {
            encodeUnderLimit { quality ->
                tried += quality
                ByteArray(BILL_IMAGE_MAX_BYTES + 1)
            }
        }.exceptionOrNull()

        // Then
        assertEquals(listOf(80, 65, 50), tried)
        assertFailsWith<ImageTooLargeException> { error?.let { throw it } }
    }

    @Test
    fun `GIVEN a photo under the limit at a lower quality WHEN encoding THEN keeps the first one under it`() {
        // Given
        val sizes = mapOf(80 to BILL_IMAGE_MAX_BYTES + 1, 65 to BILL_IMAGE_MAX_BYTES, 50 to 10)

        // When
        val jpeg = encodeUnderLimit { quality -> ByteArray(sizes.getValue(quality)) }

        // Then
        assertEquals(BILL_IMAGE_MAX_BYTES, jpeg.size)
    }
}
