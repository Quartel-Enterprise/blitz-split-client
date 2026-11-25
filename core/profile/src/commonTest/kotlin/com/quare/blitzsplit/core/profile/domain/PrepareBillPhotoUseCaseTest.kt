package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.model.CropPhoto
import com.quare.blitzsplit.core.model.PhotoCrop
import com.quare.blitzsplit.core.model.PhotoFrame
import com.quare.blitzsplit.core.profile.domain.usecase.impl.PrepareBillPhotoUseCase
import com.quare.blitzsplit.core.provider.image.BillImage
import com.quare.blitzsplit.core.provider.image.CropTransform
import com.quare.blitzsplit.core.provider.image.ImageTooLargeException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class PrepareBillPhotoUseCaseTest {
    private lateinit var encoded: MutableList<Encoding>
    private lateinit var useCase: PrepareBillPhotoUseCase

    private val photo = CropPhoto(
        jpeg = byteArrayOf(1, 2, 3),
        width = 1200,
        height = 600,
    )

    private val encodedImage = BillImage(
        jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 9),
        width = 600,
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
                keepsWholePhoto = false,
            )

            // Then
            assertSame(encodedImage, result.getOrThrow().image)
            val encoding = encoded.single()
            assertContentEquals(photo.jpeg, encoding.source)
            assertEquals(600, encoding.width)
            assertEquals(600, encoding.height)
            // The centered square: 600 px kept as they are, the 300 px on each side left out
            assertEquals(1.0, encoding.transform.scaleX, TOLERANCE)
            assertEquals(-300.0, encoding.transform.translateX, TOLERANCE)
            assertEquals(0.0, encoding.transform.translateY, TOLERANCE)
        }

    @Test
    fun `GIVEN only the framed square is kept WHEN preparing THEN has no frame`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(
            photo,
            PhotoCrop.of(
                photoWidth = 1200,
                photoHeight = 600,
            ),
            keepsWholePhoto = false,
        )

        // Then
        assertNull(result.getOrThrow().frame)
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
            keepsWholePhoto = false,
        )

        // Then
        val encoding = encoded.single()
        assertEquals(300, encoding.width)
        assertEquals(300, encoding.height)
        assertEquals(1.0, encoding.transform.scaleX, TOLERANCE)
    }

    @Test
    fun `GIVEN a receipt zoomed out to fit and cropped WHEN preparing THEN keeps the whole photo at its longer side`() =
        runTest {
            // Given
            prepareScenario()

            // When
            useCase(
                photo,
                PhotoCrop
                    .of(
                        photoWidth = 1200,
                        photoHeight = 600,
                        canFit = true,
                    ).toZoomed(0.5f),
                keepsWholePhoto = false,
            )

            // Then
            val encoding = encoded.single()
            assertEquals(1200, encoding.width)
            assertEquals(1200, encoding.height)
            // The whole photo at its own pixels, centered with 300 white px above and below
            assertEquals(1.0, encoding.transform.scaleX, TOLERANCE)
            assertEquals(0.0, encoding.transform.translateX, TOLERANCE)
            assertEquals(300.0, encoding.transform.translateY, TOLERANCE)
        }

    @Test
    fun `GIVEN a framed square larger than the limit WHEN preparing THEN encodes it at the limit`() = runTest {
        // Given
        prepareScenario()

        // When
        useCase(
            CropPhoto(
                jpeg = byteArrayOf(1),
                width = 4000,
                height = 3000,
            ),
            PhotoCrop.of(
                photoWidth = 4000,
                photoHeight = 3000,
            ),
            keepsWholePhoto = false,
        )

        // Then
        val encoding = encoded.single()
        assertEquals(1600, encoding.width)
        assertEquals(1600, encoding.height)
    }

    @Test
    fun `GIVEN the whole photo is kept WHEN preparing THEN encodes it at its own pixels`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(
            photo,
            PhotoCrop.of(
                photoWidth = 1200,
                photoHeight = 600,
            ),
            keepsWholePhoto = true,
        )

        // Then
        assertSame(encodedImage, result.getOrThrow().image)
        val encoding = encoded.single()
        assertContentEquals(photo.jpeg, encoding.source)
        assertEquals(1200, encoding.width)
        assertEquals(600, encoding.height)
        assertMaps(
            encoding.transform,
            x = 0.0,
            y = 0.0,
            toX = 0.0,
            toY = 0.0,
        )
        assertMaps(
            encoding.transform,
            x = 1200.0,
            y = 600.0,
            toX = 1200.0,
            toY = 600.0,
        )
    }

    @Test
    fun `GIVEN the whole photo is kept zoomed in and moved WHEN preparing THEN still encodes all of it`() = runTest {
        // Given
        prepareScenario()
        val crop = PhotoCrop
            .of(
                photoWidth = 1200,
                photoHeight = 600,
            ).toZoomed(2f)
            .toDragged(
                dx = 0.5f,
                dy = 0.25f,
            )

        // When
        useCase(
            photo,
            crop,
            keepsWholePhoto = true,
        )

        // Then
        val encoding = encoded.single()
        assertEquals(1200, encoding.width)
        assertEquals(600, encoding.height)
        assertMaps(
            encoding.transform,
            x = 0.0,
            y = 0.0,
            toX = 0.0,
            toY = 0.0,
        )
        assertMaps(
            encoding.transform,
            x = 1200.0,
            y = 600.0,
            toX = 1200.0,
            toY = 600.0,
        )
    }

    @Test
    fun `GIVEN the whole photo is kept turned a quarter WHEN preparing THEN encodes it upright as turned`() = runTest {
        // Given
        prepareScenario()
        val crop = PhotoCrop
            .of(
                photoWidth = 1200,
                photoHeight = 600,
            ).toTurned()

        // When
        useCase(
            photo,
            crop,
            keepsWholePhoto = true,
        )

        // Then
        val encoding = encoded.single()
        assertEquals(600, encoding.width)
        assertEquals(1200, encoding.height)
        // Turned clockwise, the photo's top left corner goes to the top right
        assertMaps(
            encoding.transform,
            x = 0.0,
            y = 0.0,
            toX = 600.0,
            toY = 0.0,
        )
        assertMaps(
            encoding.transform,
            x = 1200.0,
            y = 600.0,
            toX = 0.0,
            toY = 1200.0,
        )
    }

    @Test
    fun `GIVEN the whole photo is kept mirrored WHEN preparing THEN encodes its left on the right`() = runTest {
        // Given
        prepareScenario()
        val crop = PhotoCrop
            .of(
                photoWidth = 1200,
                photoHeight = 600,
            ).copy(isMirrored = true)

        // When
        useCase(
            photo,
            crop,
            keepsWholePhoto = true,
        )

        // Then
        val encoding = encoded.single()
        assertEquals(1200, encoding.width)
        assertEquals(600, encoding.height)
        assertMaps(
            encoding.transform,
            x = 0.0,
            y = 0.0,
            toX = 1200.0,
            toY = 0.0,
        )
    }

    @Test
    fun `GIVEN the whole photo is kept WHEN preparing THEN has the framed square as fractions of the image`() =
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
                keepsWholePhoto = true,
            )

            // Then
            // The centered 600 px square of a 1200 by 600 image
            assertFrame(
                PhotoFrame(
                    left = 0.25f,
                    top = 0f,
                    width = 0.5f,
                    height = 1f,
                ),
                assertNotNull(result.getOrThrow().frame),
            )
        }

    @Test
    fun `GIVEN the whole photo is kept turned a quarter WHEN preparing THEN the frame is in the turned image`() =
        runTest {
            // Given
            prepareScenario()
            val crop = PhotoCrop
                .of(
                    photoWidth = 1200,
                    photoHeight = 600,
                ).toTurned()

            // When
            val result = useCase(
                photo,
                crop,
                keepsWholePhoto = true,
            )

            // Then
            // The centered 600 px square of the 600 by 1200 image it turns into
            assertFrame(
                PhotoFrame(
                    left = 0f,
                    top = 0.25f,
                    width = 1f,
                    height = 0.5f,
                ),
                assertNotNull(result.getOrThrow().frame),
            )
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
            keepsWholePhoto = false,
        )

        // Then
        assertIs<ImageTooLargeException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN the encoder fails on the whole photo WHEN preparing THEN fails with its error`() = runTest {
        // Given
        prepareScenario(encoderError = ImageTooLargeException())

        // When
        val result = useCase(
            photo,
            PhotoCrop.of(
                photoWidth = 1200,
                photoHeight = 600,
            ),
            keepsWholePhoto = true,
        )

        // Then
        assertIs<ImageTooLargeException>(result.exceptionOrNull())
    }

    private fun assertMaps(
        transform: CropTransform,
        x: Double,
        y: Double,
        toX: Double,
        toY: Double,
    ) {
        val mappedX = transform.scaleX * x + transform.skewX * y + transform.translateX
        val mappedY = transform.skewY * x + transform.scaleY * y + transform.translateY
        assertEquals(toX, mappedX, TOLERANCE, "x of ($x, $y)")
        assertEquals(toY, mappedY, TOLERANCE, "y of ($x, $y)")
    }

    private fun assertFrame(
        expected: PhotoFrame,
        actual: PhotoFrame,
    ) {
        assertEquals(expected.left, actual.left, FRAME_TOLERANCE, "left")
        assertEquals(expected.top, actual.top, FRAME_TOLERANCE, "top")
        assertEquals(expected.width, actual.width, FRAME_TOLERANCE, "width")
        assertEquals(expected.height, actual.height, FRAME_TOLERANCE, "height")
    }

    private fun prepareScenario(encoderError: Exception? = null) {
        encoded = mutableListOf()
        useCase = PrepareBillPhotoUseCase(
            encodeCrop = { source, transform, width, height ->
                encoded += Encoding(source, transform, width, height)
                encoderError?.let { throw it }
                encodedImage
            },
        )
    }

    private companion object {
        const val TOLERANCE = 1e-9
        const val FRAME_TOLERANCE = 1e-6f
    }
}

private class Encoding(
    val source: ByteArray,
    val transform: CropTransform,
    val width: Int,
    val height: Int,
)
