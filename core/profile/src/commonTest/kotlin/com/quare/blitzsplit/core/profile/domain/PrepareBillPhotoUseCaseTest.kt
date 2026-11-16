package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.model.CropPhoto
import com.quare.blitzsplit.core.model.PhotoCrop
import com.quare.blitzsplit.core.profile.domain.usecase.impl.PrepareBillPhotoUseCase
import com.quare.blitzsplit.core.provider.image.BillImage
import com.quare.blitzsplit.core.provider.image.CropTransform
import com.quare.blitzsplit.core.provider.image.ImageTooLargeException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class PrepareBillPhotoUseCaseTest {
    private lateinit var encoded: MutableList<Triple<ByteArray, CropTransform, Int>>
    private lateinit var useCase: PrepareBillPhotoUseCase

    private val photo = CropPhoto(
        jpeg = byteArrayOf(1, 2, 3),
        width = 1200,
        height = 600,
    )

    @Test
    fun `GIVEN a framed photo at zoom 1 WHEN preparing THEN encodes the centered square at the shorter side`() =
        runTest {
            // Given
            prepareScenario()

            // When
            val result = useCase(
                photo,
                PhotoCrop.of(
                    photoWidth = 1200,
                    photoHeight = 600,
                ),
            )

            // Then
            assertSame(encodedImage, result.getOrThrow())
            val (source, transform, side) = encoded.single()
            assertContentEquals(photo.jpeg, source)
            assertEquals(600, side)
            // The centered square: 600 px kept as they are, the 300 px on each side left out
            assertEquals(1.0, transform.scaleX, TOLERANCE)
            assertEquals(-300.0, transform.translateX, TOLERANCE)
            assertEquals(0.0, transform.translateY, TOLERANCE)
        }

    @Test
    fun `GIVEN a framed photo zoomed in WHEN preparing THEN keeps only the pixels the frame holds`() = runTest {
        // Given
        prepareScenario()

        // When
        useCase(
            photo,
            PhotoCrop
                .of(
                    photoWidth = 1200,
                    photoHeight = 600,
                ).toZoomed(2f),
        )

        // Then
        val (_, transform, side) = encoded.single()
        assertEquals(300, side)
        assertEquals(1.0, transform.scaleX, TOLERANCE)
    }

    @Test
    fun `GIVEN the encoder fails WHEN preparing THEN fails with its error`() = runTest {
        // Given
        prepareScenario(encoderError = ImageTooLargeException())

        // When
        val result = useCase(
            photo,
            PhotoCrop.of(
                photoWidth = 1200,
                photoHeight = 600,
            ),
        )

        // Then
        assertIs<ImageTooLargeException>(result.exceptionOrNull())
    }

    private val encodedImage = BillImage(
        jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 9),
        width = 600,
        height = 600,
    )

    private fun prepareScenario(encoderError: Exception? = null) {
        encoded = mutableListOf()
        useCase = PrepareBillPhotoUseCase(
            encodeCrop = { source, transform, side ->
                encoded += Triple(source, transform, side)
                encoderError?.let { throw it }
                encodedImage
            },
        )
    }

    private companion object {
        const val TOLERANCE = 1e-9
    }
}
