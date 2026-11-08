package com.quare.blitzsplit.core.expenses.data

import com.quare.blitzsplit.core.expenses.data.datasource.ReceiptRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptCategoryDto
import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptItemDto
import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptRequestDto
import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptResponseDto
import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptTermDto
import com.quare.blitzsplit.core.expenses.data.mapper.ReceiptMapperImpl
import com.quare.blitzsplit.core.expenses.data.repository.ReceiptRepositoryImpl
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTerm
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptCategoryHints
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptItem
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReading
import com.quare.blitzsplit.core.expenses.testing.noReceiptCategoryHints
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReceiptRepositoryImplTest {
    private lateinit var remote: FakeReceiptRemoteDataSource
    private lateinit var repository: ReceiptRepositoryImpl

    @Test
    fun `GIVEN a receipt photo WHEN reading it THEN sends the JPEG in base64 with the group currency code`() = runTest {
        // Given
        prepareScenario()

        // When
        repository.readReceipt(jpeg, GroupCurrency.EUR, noReceiptCategoryHints)

        // Then
        assertEquals(
            listOf(
                ReadReceiptRequestDto(
                    image = "/9j/4AAQSkZJRg==",
                    currency = "EUR",
                    categories = emptyList(),
                    terms = emptyList(),
                ),
            ),
            remote.requests,
        )
    }

    @Test
    fun `GIVEN the group categories WHEN reading a receipt THEN sends each one with its name and parent`() = runTest {
        // Given
        prepareScenario()
        val hints = ReceiptCategoryHints.of(CategoryTree.of(listOf(pet, racao, reforma)), emptyList())

        // When
        repository.readReceipt(jpeg, GroupCurrency.BRL, hints)

        // Then
        assertEquals(
            setOf(
                ReadReceiptCategoryDto(
                    id = "pet",
                    name = "Pet",
                    parentId = null,
                ),
                ReadReceiptCategoryDto(
                    id = "racao",
                    name = "Ração",
                    parentId = "pet",
                ),
                ReadReceiptCategoryDto(
                    id = "reforma",
                    name = "Reforma",
                    parentId = AppCategory.HOME.key,
                ),
            ),
            remote.requests
                .single()
                .categories
                .toSet(),
        )
    }

    @Test
    fun `GIVEN the map terms WHEN reading a receipt THEN sends each term with its category`() = runTest {
        // Given
        prepareScenario()
        val hints = ReceiptCategoryHints.of(
            tree = CategoryTree.of(listOf(pet, reforma)),
            terms = listOf(
                CategoryTerm("ração", "pet"),
                CategoryTerm("cimento", "reforma"),
                CategoryTerm("aluguel", AppCategory.HOME_RENT.key),
            ),
        )

        // When
        repository.readReceipt(jpeg, GroupCurrency.BRL, hints)

        // Then
        assertEquals(
            listOf(
                ReadReceiptTermDto(
                    term = "ração",
                    categoryId = "pet",
                ),
                ReadReceiptTermDto(
                    term = "cimento",
                    categoryId = "reforma",
                ),
                ReadReceiptTermDto(
                    term = "aluguel",
                    categoryId = AppCategory.HOME_RENT.key,
                ),
            ),
            remote.requests.single().terms,
        )
    }

    @Test
    fun `GIVEN a group category with no name WHEN reading a receipt THEN leaves it out of the request`() = runTest {
        // Given
        prepareScenario()
        val hints = ReceiptCategoryHints(
            groupCategories = listOf(
                petCategory.copy(
                    id = "sem-nome",
                    name = null,
                ),
                petCategory,
            ),
            terms = emptyList(),
        )

        // When
        repository.readReceipt(jpeg, GroupCurrency.BRL, hints)

        // Then
        assertEquals(
            listOf(
                ReadReceiptCategoryDto(
                    id = "pet",
                    name = "Pet",
                    parentId = null,
                ),
            ),
            remote.requests.single().categories,
        )
    }

    @Test
    fun `GIVEN the server picks a group category that was sent WHEN reading a receipt THEN reads that category`() =
        runTest {
            // Given
            prepareScenario(answer = supermarketDto.copy(category = "pet"))
            val hints = ReceiptCategoryHints.of(CategoryTree.of(listOf(pet, reforma)), emptyList())

            // When
            val result = repository.readReceipt(jpeg, GroupCurrency.BRL, hints)

            // Then
            assertEquals("pet", result.getOrThrow()?.categoryId)
        }

    @Test
    fun `GIVEN the server picks a group subcategory that was sent WHEN reading a receipt THEN reads that category`() =
        runTest {
            // Given
            prepareScenario(answer = supermarketDto.copy(category = "reforma"))
            val hints = ReceiptCategoryHints.of(CategoryTree.of(listOf(pet, reforma)), emptyList())

            // When
            val result = repository.readReceipt(jpeg, GroupCurrency.BRL, hints)

            // Then
            assertEquals("reforma", result.getOrThrow()?.categoryId)
        }

    @Test
    fun `GIVEN the server picks a category id that was not sent WHEN reading a receipt THEN reads no category`() =
        runTest {
            // Given
            prepareScenario(answer = supermarketDto.copy(category = "praia"))
            val hints = ReceiptCategoryHints.of(CategoryTree.of(listOf(pet)), emptyList())

            // When
            val result = repository.readReceipt(jpeg, GroupCurrency.BRL, hints)

            // Then
            val reading = assertNotNull(result.getOrThrow())
            assertNull(reading.categoryId)
            assertEquals(8_743L, reading.totalMinor)
        }

    @Test
    fun `GIVEN no hints and the server picks a group category WHEN reading a receipt THEN reads no category`() =
        runTest {
            // Given
            prepareScenario(answer = supermarketDto.copy(category = "pet"))

            // When
            val result = repository.readReceipt(jpeg, GroupCurrency.BRL, noReceiptCategoryHints)

            // Then
            assertNull(assertNotNull(result.getOrThrow()).categoryId)
        }

    @Test
    fun `GIVEN the server reads the receipt WHEN reading it THEN reads it checked for the group currency`() = runTest {
        // Given
        prepareScenario(answer = supermarketDto)

        // When
        val result = repository.readReceipt(jpeg, GroupCurrency.BRL, noReceiptCategoryHints)

        // Then
        assertEquals(
            ReceiptReading(
                totalMinor = 8_743L,
                description = "Supermercado Bom Preço",
                categoryId = "food.market",
                date = LocalDate(2026, 10, 30),
                time = LocalTime(19, 42),
                items = listOf(
                    ReceiptItem(
                        name = "ARROZ TIPO 1 5KG",
                        quantity = 1L,
                        unitPriceMinor = 2_790L,
                    ),
                    ReceiptItem(
                        name = "LEITE INTEGRAL 1L",
                        quantity = 6L,
                        unitPriceMinor = 479L,
                    ),
                ),
                servicePercent = 0L,
            ),
            result.getOrThrow(),
        )
    }

    @Test
    fun `GIVEN the server finds no receipt in the photo WHEN reading it THEN succeeds reading nothing`() = runTest {
        // Given
        prepareScenario(answer = null)

        // When
        val result = repository.readReceipt(jpeg, GroupCurrency.BRL, noReceiptCategoryHints)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `GIVEN the server reads a total no bill can take WHEN reading it THEN succeeds reading nothing`() = runTest {
        // Given
        prepareScenario(answer = supermarketDto.copy(totalMinor = 0L))

        // When
        val result = repository.readReceipt(jpeg, GroupCurrency.BRL, noReceiptCategoryHints)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `GIVEN no internet WHEN reading a receipt THEN fails`() = runTest {
        // Given
        prepareScenario(error = IllegalStateException("offline"))

        // When
        val result = repository.readReceipt(jpeg, GroupCurrency.BRL, noReceiptCategoryHints)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN the server refuses too many reads WHEN reading a receipt THEN fails`() = runTest {
        // Given
        prepareScenario(error = RuntimeException("429 Too Many Requests"))

        // When
        val result = repository.readReceipt(jpeg, GroupCurrency.BRL, noReceiptCategoryHints)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `GIVEN the read is cancelled WHEN reading a receipt THEN the cancellation goes on`() = runTest {
        // Given
        prepareScenario(error = CancellationException("left Rachar conta"))

        // When
        val failure = assertFailsWith<CancellationException> {
            repository.readReceipt(jpeg, GroupCurrency.BRL, noReceiptCategoryHints)
        }

        // Then
        assertEquals("left Rachar conta", failure.message)
    }

    // Why: the first bytes of a JPEG (SOI, APP0 and "JFIF"), whose base64 is known.
    private val jpeg = byteArrayOf(
        0xFF.toByte(),
        0xD8.toByte(),
        0xFF.toByte(),
        0xE0.toByte(),
        0x00,
        0x10,
        0x4A,
        0x46,
        0x49,
        0x46,
    )

    private val supermarketDto = ReadReceiptResponseDto(
        totalMinor = 8_743L,
        description = "Supermercado Bom Preço",
        category = "food.market",
        date = "2026-10-30",
        time = "19:42",
        items = listOf(
            ReadReceiptItemDto(
                name = "ARROZ TIPO 1 5KG",
                quantity = 1L,
                unitPriceMinor = 2_790L,
            ),
            ReadReceiptItemDto(
                name = "LEITE INTEGRAL 1L",
                quantity = 6L,
                unitPriceMinor = 479L,
            ),
        ),
        servicePercent = 0L,
    )

    private val pet = GroupCategory(
        id = "pet",
        groupId = "apto-302",
        parentId = null,
        name = "Pet",
        icon = CategoryIcon.DOTS,
        color = null,
        createdByMemberId = "ana",
        isDeleted = false,
        isPending = false,
    )

    private val racao = pet.copy(
        id = "racao",
        parentId = "pet",
        name = "Ração",
        icon = null,
    )

    private val reforma = pet.copy(
        id = "reforma",
        parentId = AppCategory.HOME.key,
        name = "Reforma",
        icon = null,
    )

    private val petCategory = Category(
        id = "pet",
        app = null,
        name = "Pet",
        icon = CategoryIcon.DOTS,
        color = CategoryColor.PURPLE,
        parentId = null,
        createdByMemberId = "ana",
        isPending = false,
    )

    private fun prepareScenario(
        answer: ReadReceiptResponseDto? = supermarketDto,
        error: Throwable? = null,
    ) {
        remote = FakeReceiptRemoteDataSource(
            answer = answer,
            error = error,
        )
        repository = ReceiptRepositoryImpl(
            remote = remote,
            mapper = ReceiptMapperImpl(),
        )
    }
}

private class FakeReceiptRemoteDataSource(
    private val answer: ReadReceiptResponseDto?,
    private val error: Throwable?,
) : ReceiptRemoteDataSource {
    val requests = mutableListOf<ReadReceiptRequestDto>()

    override suspend fun readReceipt(request: ReadReceiptRequestDto): ReadReceiptResponseDto? {
        requests += request
        error?.let { throw it }
        return answer
    }
}
