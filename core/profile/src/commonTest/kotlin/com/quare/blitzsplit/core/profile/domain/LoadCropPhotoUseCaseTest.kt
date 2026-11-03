package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.profile.domain.model.PhotoTooLargeException
import com.quare.blitzsplit.core.profile.domain.usecase.impl.LoadCropPhotoUseCase
import com.quare.blitzsplit.core.provider.image.BillImage
import com.quare.blitzsplit.core.provider.image.UnsupportedImageException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LoadCropPhotoUseCaseTest {
    private lateinit var encodedSources: MutableList<ByteArray>
    private lateinit var useCase: LoadCropPhotoUseCase

    @Test
    fun `GIVEN a photo over 30 MB WHEN loading THEN fails as too large without encoding it`() = runTest {
        // Given
        prepareScenario()
        val source = ByteArray(THIRTY_MEGABYTES + 1)

        // When
        val result = useCase(source)

        // Then
        assertIs<PhotoTooLargeException>(result.exceptionOrNull())
        assertTrue(encodedSources.isEmpty())
    }

    @Test
    fun `GIVEN a photo of exactly 30 MB WHEN loading THEN encodes it`() = runTest {
        // Given
        prepareScenario()
        val source = ByteArray(THIRTY_MEGABYTES)

        // When
        val result = useCase(source)

        // Then
        assertTrue(result.isSuccess)
        assertContentEquals(source, encodedSources.single())
    }

    @Test
    fun `GIVEN a file that is not an image WHEN loading THEN fails as unsupported`() = runTest {
        // Given
        prepareScenario(encoderError = UnsupportedImageException())
        val source = "not an image".encodeToByteArray()

        // When
        val result = useCase(source)

        // Then
        assertIs<UnsupportedImageException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN a picked photo WHEN loading THEN returns the upright JPEG with its size for the editor`() = runTest {
        // Given
        prepareScenario()
        val source = byteArrayOf(1, 2, 3)

        // When
        val photo = useCase(source).getOrThrow()

        // Then
        assertContentEquals(encodedJpeg, photo.jpeg)
        assertEquals(1600, photo.width)
        assertEquals(1200, photo.height)
        assertContentEquals(source, encodedSources.single())
    }

    private val encodedJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 9)

    private fun prepareScenario(encoderError: Exception? = null) {
        encodedSources = mutableListOf()
        useCase = LoadCropPhotoUseCase(
            encodeImage = { source ->
                encodedSources += source
                encoderError?.let { throw it }
                BillImage(
                    jpeg = encodedJpeg,
                    width = 1600,
                    height = 1200,
                )
            },
        )
    }

    private companion object {
        const val THIRTY_MEGABYTES = 30 * 1024 * 1024
    }
}
