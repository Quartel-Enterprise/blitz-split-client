package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTerm
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptCategoryHints
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptItem
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReadFailure
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReaderException
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReading
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptScan
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ReadReceiptUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeReceiptRepository
import com.quare.blitzsplit.core.expenses.testing.noReceiptCategoryHints
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.provider.image.BillImage
import com.quare.blitzsplit.core.provider.image.ImageTooLargeException
import com.quare.blitzsplit.core.provider.image.UnsupportedImageException
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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
        useCase(source, GroupCurrency.BRL, noReceiptCategoryHints)

        // Then
        assertTrue(source.contentEquals(encodedSources.single()))
    }

    @Test
    fun `GIVEN a photo of a receipt WHEN reading it THEN sends the encoded JPEG with the group currency`() = runTest {
        // Given
        prepareScenario()

        // When
        useCase(source, GroupCurrency.BRL, noReceiptCategoryHints)

        // Then
        val (sentJpeg, sentCurrency) = repository.readPhotos.single()
        assertTrue(encodedJpeg.contentEquals(sentJpeg))
        assertEquals(GroupCurrency.BRL, sentCurrency)
    }

    @Test
    fun `GIVEN the group categories and map terms WHEN reading a receipt THEN sends them as they came`() = runTest {
        // Given
        prepareScenario()
        val hints = ReceiptCategoryHints(
            groupCategories = listOf(pet),
            terms = listOf(CategoryTerm("ração", "pet")),
        )

        // When
        useCase(source, GroupCurrency.BRL, hints)

        // Then
        assertEquals(listOf(hints), repository.readHints)
    }

    @Test
    fun `GIVEN the server reads the receipt WHEN reading it THEN gives the encoded photo and the reading`() = runTest {
        // Given
        prepareScenario(reading = restaurantReading)

        // When
        val scan = useCase(source, GroupCurrency.BRL, noReceiptCategoryHints)

        // Then
        val read = assertIs<ReceiptScan.Read>(scan)
        assertTrue(encodedJpeg.contentEquals(read.photo.jpeg))
        assertEquals(restaurantReading, read.reading)
    }

    @Test
    fun `GIVEN the photo shows no receipt WHEN reading it THEN fails as unreadable`() = runTest {
        // Given
        prepareScenario(reading = null)

        // When
        val scan = useCase(source, GroupCurrency.BRL, noReceiptCategoryHints)

        // Then
        assertEquals(ReceiptReadFailure.UNREADABLE, assertIs<ReceiptScan.Failed>(scan).failure)
    }

    @Test
    fun `GIVEN the server fails with no reason named WHEN reading a receipt THEN fails as the reader unavailable`() =
        runTest {
            // Given
            prepareScenario(
                reading = restaurantReading,
                readError = IllegalStateException("503 Service Unavailable"),
            )

            // When
            val scan = useCase(source, GroupCurrency.BRL, noReceiptCategoryHints)

            // Then
            assertEquals(ReceiptReadFailure.READER_UNAVAILABLE, assertIs<ReceiptScan.Failed>(scan).failure)
        }

    @Test
    fun `GIVEN the server refuses past the daily limit WHEN reading a receipt THEN fails as the limit reached`() =
        runTest {
            // Given
            prepareScenario(
                reading = restaurantReading,
                readError = ReceiptReaderException(ReceiptReadFailure.LIMIT_REACHED, IllegalStateException("429")),
            )

            // When
            val scan = useCase(source, GroupCurrency.BRL, noReceiptCategoryHints)

            // Then
            assertEquals(ReceiptReadFailure.LIMIT_REACHED, assertIs<ReceiptScan.Failed>(scan).failure)
        }

    @Test
    fun `GIVEN a file that is no image WHEN reading it THEN fails as unreadable and sends nothing`() = runTest {
        // Given
        prepareScenario(
            reading = restaurantReading,
            encodeError = UnsupportedImageException(),
        )

        // When
        val scan = useCase(source, GroupCurrency.BRL, noReceiptCategoryHints)

        // Then
        assertEquals(ReceiptReadFailure.UNREADABLE, assertIs<ReceiptScan.Failed>(scan).failure)
        assertTrue(repository.readPhotos.isEmpty())
    }

    @Test
    fun `GIVEN a photo too large at every quality WHEN reading it THEN fails as unreadable and sends nothing`() =
        runTest {
            // Given
            prepareScenario(
                reading = restaurantReading,
                encodeError = ImageTooLargeException(),
            )

            // When
            val scan = useCase(source, GroupCurrency.BRL, noReceiptCategoryHints)

            // Then
            assertEquals(ReceiptReadFailure.UNREADABLE, assertIs<ReceiptScan.Failed>(scan).failure)
            assertTrue(repository.readPhotos.isEmpty())
        }

    private val source = byteArrayOf(1, 2, 3, 4)

    private val encodedJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 9)

    private val pet = Category(
        id = "pet",
        app = null,
        name = "Pet",
        icon = CategoryIcon.DOTS,
        color = CategoryColor.PURPLE,
        parentId = null,
        createdByMemberId = "ana",
        isPending = false,
    )

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
