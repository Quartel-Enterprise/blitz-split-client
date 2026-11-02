package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.ReceiptItem
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReading
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ReadReceiptUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeReceiptRepository
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.provider.image.BillImage
import com.quare.blitzsplit.core.provider.image.ImageTooLargeException
import com.quare.blitzsplit.core.provider.image.UnsupportedImageException
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReadReceiptUseCaseTest {
    private lateinit var repository: FakeReceiptRepository
    private lateinit var useCase: ReadReceiptUseCase
    private val encodedSources = mutableListOf<ByteArray>()

    @Test
    fun `GIVEN a photo of a receipt WHEN reading it THEN encodes the photo as taken`() = runTest {
        // Given
        prepareScenario()

        // When
        useCase(source, GroupCurrency.BRL)

        // Then
        assertTrue(source.contentEquals(encodedSources.single()))
    }

    @Test
    fun `GIVEN a photo of a receipt WHEN reading it THEN sends the encoded JPEG with the group currency`() = runTest {
        // Given
        prepareScenario()

        // When
        useCase(source, GroupCurrency.BRL)

        // Then
        val (sentJpeg, sentCurrency) = repository.readPhotos.single()
        assertTrue(encodedJpeg.contentEquals(sentJpeg))
        assertEquals(GroupCurrency.BRL, sentCurrency)
    }

    @Test
    fun `GIVEN the server reads the receipt WHEN reading it THEN gives the encoded photo and the reading`() = runTest {
        // Given
        prepareScenario(reading = restaurantReading)

        // When
        val scan = useCase(source, GroupCurrency.BRL)

        // Then
        assertTrue(encodedJpeg.contentEquals(scan.photo))
        assertEquals(restaurantReading, scan.reading)
    }

    @Test
    fun `GIVEN the photo shows no receipt WHEN reading it THEN keeps the photo with no reading`() = runTest {
        // Given
        prepareScenario(reading = null)

        // When
        val scan = useCase(source, GroupCurrency.BRL)

        // Then
        assertTrue(encodedJpeg.contentEquals(scan.photo))
        assertNull(scan.reading)
    }

    @Test
    fun `GIVEN the server fails WHEN reading a receipt THEN keeps the photo with no reading`() = runTest {
        // Given
        prepareScenario(
            reading = restaurantReading,
            readError = IllegalStateException("503 Service Unavailable"),
        )

        // When
        val scan = useCase(source, GroupCurrency.BRL)

        // Then
        assertTrue(encodedJpeg.contentEquals(scan.photo))
        assertNull(scan.reading)
    }

    @Test
    fun `GIVEN a file that is no image WHEN reading it THEN gives no photo nor reading and sends nothing`() = runTest {
        // Given
        prepareScenario(
            reading = restaurantReading,
            encodeError = UnsupportedImageException(),
        )

        // When
        val scan = useCase(source, GroupCurrency.BRL)

        // Then
        assertNull(scan.photo)
        assertNull(scan.reading)
        assertTrue(repository.readPhotos.isEmpty())
    }

    @Test
    fun `GIVEN a photo too large at every quality WHEN reading it THEN gives no photo nor reading and sends nothing`() =
        runTest {
            // Given
            prepareScenario(
                reading = restaurantReading,
                encodeError = ImageTooLargeException(),
            )

            // When
            val scan = useCase(source, GroupCurrency.BRL)

            // Then
            assertNull(scan.photo)
            assertNull(scan.reading)
            assertTrue(repository.readPhotos.isEmpty())
        }

    private val source = byteArrayOf(1, 2, 3, 4)

    private val encodedJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 9)

    private val restaurantReading = ReceiptReading(
        totalMinor = 64_200L,
        description = "Ostradamus",
        categoryId = "food.restaurant",
        date = LocalDate(2026, 10, 31),
        time = LocalTime(22, 15),
        items = listOf(
            ReceiptItem(
                name = "Ostras gratinadas",
                quantity = 2L,
                unitPriceMinor = 12_900L,
            ),
            ReceiptItem(
                name = "Couvert artístico",
                quantity = 2L,
                unitPriceMinor = 1_500L,
            ),
        ),
        servicePercent = 10L,
    )

    private fun prepareScenario(
        reading: ReceiptReading? = null,
        readError: Throwable? = null,
        encodeError: Throwable? = null,
    ) {
        repository = FakeReceiptRepository(reading).also { it.readError = readError }
        useCase = ReadReceiptUseCase(
            repository = repository,
            encodeImage = { picked ->
                encodedSources += picked
                encodeError?.let { throw it }
                BillImage(
                    jpeg = encodedJpeg,
                    width = 1_200,
                    height = 1_600,
                )
            },
        )
    }
}
