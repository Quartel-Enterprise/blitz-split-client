package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.model.CropPhoto
import com.quare.blitzsplit.core.model.PhotoCrop
import com.quare.blitzsplit.core.profile.domain.usecase.impl.PrepareProfilePhotoUseCase
import com.quare.blitzsplit.core.provider.image.CropTransform
import com.quare.blitzsplit.core.provider.image.UnsupportedImageException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PrepareProfilePhotoUseCaseTest {
    private lateinit var encoded: MutableList<Pair<ByteArray, CropTransform>>
    private lateinit var useCase: PrepareProfilePhotoUseCase

    @Test
    fun `GIVEN a framed photo WHEN preparing THEN encodes its JPEG where the crop put it on the 512 avatar`() =
        runTest {
            // Given
            prepareScenario()
            val photo = CropPhoto(
                jpeg = byteArrayOf(1, 2, 3),
                width = 1200,
                height = 600,
            )

            // When
            val result = useCase(
                photo,
                PhotoCrop.of(
                    photoWidth = 1200,
                    photoHeight = 600,
                ),
            )

            // Then
            assertContentEquals(encodedJpeg, result.getOrThrow().jpeg)
            val (source, transform) = encoded.single()
            assertContentEquals(photo.jpeg, source)
            // The centered square: 600 px fill the 512, the 300 px on each side left out
            assertEquals(512.0 / 600, transform.scaleX, TOLERANCE)
            assertEquals(-300 * 512.0 / 600, transform.translateX, TOLERANCE)
            assertEquals(0.0, transform.translateY, TOLERANCE)
        }

    @Test
    fun `GIVEN the encoder fails WHEN preparing THEN fails with its error`() = runTest {
        // Given
        prepareScenario(encoderError = UnsupportedImageException())
        val photo = CropPhoto(
            jpeg = byteArrayOf(1),
            width = 10,
            height = 10,
        )

        // When
        val result = useCase(
            photo,
            PhotoCrop.of(
                photoWidth = 10,
                photoHeight = 10,
            ),
        )

        // Then
        assertIs<UnsupportedImageException>(result.exceptionOrNull())
    }

    private val encodedJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 9)

    private fun prepareScenario(encoderError: Exception? = null) {
        encoded = mutableListOf()
        useCase = PrepareProfilePhotoUseCase(
            encodeAvatar = { source, transform ->
                encoded += source to transform
                encoderError?.let { throw it }
                encodedJpeg
            },
        )
    }

    private companion object {
        const val TOLERANCE = 1e-9
    }
}
