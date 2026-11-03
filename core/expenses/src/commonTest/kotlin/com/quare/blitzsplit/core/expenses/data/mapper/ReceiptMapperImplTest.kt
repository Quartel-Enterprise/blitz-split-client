package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptItemDto
import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptResponseDto
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptItem
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReading
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReceiptMapperImplTest {
    private val mapper = ReceiptMapperImpl()

    @Test
    fun `GIVEN a supermarket receipt read whole WHEN mapping it THEN reads every field as the server sent it`() {
        // Given
        val dto = supermarketDto()

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

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
                    ReceiptItem(
                        name = "CAFE TORRADO 500G",
                        quantity = 1L,
                        unitPriceMinor = 1_879L,
                    ),
                    ReceiptItem(
                        name = "BANANA PRATA KG",
                        quantity = 2L,
                        unitPriceMinor = 600L,
                    ),
                ),
                servicePercent = 0L,
            ),
            reading,
        )
    }

    @Test
    fun `GIVEN a restaurant bill with service WHEN mapping it THEN keeps the service and the couvert as an item`() {
        // Given
        val dto = ReadReceiptResponseDto(
            totalMinor = 64_200L,
            description = "Ostradamus",
            category = "food.restaurant",
            date = "2026-10-31",
            time = "22:15",
            items = listOf(
                ReadReceiptItemDto(
                    name = "Ostras gratinadas",
                    quantity = 2L,
                    unitPriceMinor = 12_900L,
                ),
                ReadReceiptItemDto(
                    name = "Moqueca de peixe",
                    quantity = 1L,
                    unitPriceMinor = 26_400L,
                ),
                ReadReceiptItemDto(
                    name = "Couvert artístico",
                    quantity = 2L,
                    unitPriceMinor = 1_500L,
                ),
            ),
            servicePercent = 10L,
        )

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertNotNull(reading)
        assertEquals(10L, reading.servicePercent)
        assertEquals(
            ReceiptItem(
                name = "Couvert artístico",
                quantity = 2L,
                unitPriceMinor = 1_500L,
            ),
            reading.items.last(),
        )
    }

    @Test
    fun `GIVEN a total of zero WHEN mapping the reading THEN reads nothing`() {
        // Given
        val dto = supermarketDto(totalMinor = 0L)

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertNull(reading)
    }

    @Test
    fun `GIVEN a negative total WHEN mapping the reading THEN reads nothing`() {
        // Given
        val dto = supermarketDto(totalMinor = -8_743L)

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertNull(reading)
    }

    @Test
    fun `GIVEN a total of one cent WHEN mapping the reading THEN keeps it`() {
        // Given
        val dto = supermarketDto(totalMinor = 1L)

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertEquals(1L, reading?.totalMinor)
    }

    @Test
    fun `GIVEN a total of exactly 100 million reais WHEN mapping the reading THEN keeps it`() {
        // Given
        val dto = supermarketDto(totalMinor = 10_000_000_000L)

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertEquals(10_000_000_000L, reading?.totalMinor)
    }

    @Test
    fun `GIVEN a total one cent over 100 million reais WHEN mapping the reading THEN reads nothing`() {
        // Given
        val dto = supermarketDto(totalMinor = 10_000_000_001L)

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertNull(reading)
    }

    @Test
    fun `GIVEN a yen total over 100 million yen WHEN mapping the reading THEN reads nothing`() {
        // Given
        val dto = supermarketDto(totalMinor = 100_000_001L)

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.JPY)

        // Then
        assertNull(reading)
    }

    @Test
    fun `GIVEN a yen total of 100 million yen WHEN mapping the reading THEN keeps it in the group currency`() {
        // Given
        val dto = supermarketDto(totalMinor = 100_000_000L)

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.JPY)

        // Then
        assertEquals(100_000_000L, reading?.totalMinor)
    }

    @Test
    fun `GIVEN a description with spaces around WHEN mapping the reading THEN trims it`() {
        // Given
        val dto = supermarketDto(description = "  Supermercado Bom Preço \n")

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertEquals("Supermercado Bom Preço", reading?.description)
    }

    @Test
    fun `GIVEN a blank description WHEN mapping the reading THEN reads no description`() {
        // Given
        val dto = supermarketDto(description = "   ")

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertNotNull(reading)
        assertNull(reading.description)
    }

    @Test
    fun `GIVEN no description WHEN mapping the reading THEN reads no description`() {
        // Given
        val dto = supermarketDto(description = null)

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertNotNull(reading)
        assertNull(reading.description)
    }

    @Test
    fun `GIVEN a description longer than a bill takes WHEN mapping the reading THEN caps it to 100 letters`() {
        // Given
        val dto = supermarketDto(description = "A".repeat(150))

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertEquals("A".repeat(100), reading?.description)
    }

    @Test
    fun `GIVEN a long description cut on a space WHEN mapping the reading THEN ends with no space`() {
        // Given
        val dto = supermarketDto(description = "B".repeat(99) + " Filial Centro")

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertEquals("B".repeat(99), reading?.description)
    }

    @Test
    fun `GIVEN a long description cut inside an emoji WHEN mapping the reading THEN drops the half emoji`() {
        // Given
        val dto = supermarketDto(description = "C".repeat(99) + "🛒 mercado")

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertEquals("C".repeat(99), reading?.description)
    }

    @Test
    fun `GIVEN a known app category WHEN mapping the reading THEN keeps it`() {
        // Given
        val dto = supermarketDto(category = "food")

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertEquals("food", reading?.categoryId)
    }

    @Test
    fun `GIVEN a category the app does not know WHEN mapping the reading THEN reads no category`() {
        // Given
        val dto = supermarketDto(category = "food.supermarket")

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertNotNull(reading)
        assertNull(reading.categoryId)
    }

    @Test
    fun `GIVEN no category WHEN mapping the reading THEN reads no category`() {
        // Given
        val dto = supermarketDto(category = null)

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertNotNull(reading)
        assertNull(reading.categoryId)
    }

    @Test
    fun `GIVEN a day that does not exist WHEN mapping the reading THEN reads no date nor time and keeps the rest`() {
        // Given
        val dto = supermarketDto(
            date = "2026-02-30",
            time = "19:42",
        )

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertNotNull(reading)
        assertNull(reading.date)
        assertNull(reading.time)
        assertEquals(8_743L, reading.totalMinor)
    }

    @Test
    fun `GIVEN a date written the Brazilian way WHEN mapping the reading THEN reads no date`() {
        // Given
        val dto = supermarketDto(date = "30/10/2026")

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertNotNull(reading)
        assertNull(reading.date)
    }

    @Test
    fun `GIVEN a time but no date WHEN mapping the reading THEN reads no time`() {
        // Given
        val dto = supermarketDto(
            date = null,
            time = "19:42",
        )

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertNotNull(reading)
        assertNull(reading.date)
        assertNull(reading.time)
    }

    @Test
    fun `GIVEN a date and an hour that does not exist WHEN mapping the reading THEN keeps the date with no time`() {
        // Given
        val dto = supermarketDto(
            date = "2026-10-30",
            time = "25:10",
        )

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertNotNull(reading)
        assertEquals(LocalDate(2026, 10, 30), reading.date)
        assertNull(reading.time)
    }

    @Test
    fun `GIVEN a date with no time WHEN mapping the reading THEN keeps the date with no time`() {
        // Given
        val dto = supermarketDto(
            date = "2026-10-30",
            time = null,
        )

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertNotNull(reading)
        assertEquals(LocalDate(2026, 10, 30), reading.date)
        assertNull(reading.time)
    }

    @Test
    fun `GIVEN an item with a blank name WHEN mapping the reading THEN drops that item only`() {
        // Given
        val dto = supermarketDto(
            items = listOf(
                ReadReceiptItemDto(
                    name = "   ",
                    quantity = 1L,
                    unitPriceMinor = 1_000L,
                ),
                ReadReceiptItemDto(
                    name = "ARROZ TIPO 1 5KG",
                    quantity = 1L,
                    unitPriceMinor = 2_790L,
                ),
            ),
        )

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertEquals(listOf("ARROZ TIPO 1 5KG"), reading?.items?.map { it.name })
    }

    @Test
    fun `GIVEN an item name with spaces around WHEN mapping the reading THEN trims it`() {
        // Given
        val dto = supermarketDto(
            items = listOf(
                ReadReceiptItemDto(
                    name = "  LEITE INTEGRAL 1L  ",
                    quantity = 6L,
                    unitPriceMinor = 479L,
                ),
            ),
        )

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertEquals(listOf("LEITE INTEGRAL 1L"), reading?.items?.map { it.name })
    }

    @Test
    fun `GIVEN an item name longer than an item takes WHEN mapping the reading THEN caps it to 60 letters`() {
        // Given
        val dto = supermarketDto(
            items = listOf(
                ReadReceiptItemDto(
                    name = "D".repeat(80),
                    quantity = 1L,
                    unitPriceMinor = 999L,
                ),
            ),
        )

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertEquals(listOf("D".repeat(60)), reading?.items?.map { it.name })
    }

    @Test
    fun `GIVEN an item of no unit WHEN mapping the reading THEN drops it`() {
        // Given
        val dto = supermarketDto(
            items = listOf(
                ReadReceiptItemDto(
                    name = "SACOLA",
                    quantity = 0L,
                    unitPriceMinor = 15L,
                ),
            ),
        )

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertEquals(emptyList(), reading?.items)
    }

    @Test
    fun `GIVEN an item of a thousand units WHEN mapping the reading THEN drops it`() {
        // Given
        val dto = supermarketDto(
            items = listOf(
                ReadReceiptItemDto(
                    name = "PREGO 2CM",
                    quantity = 1_000L,
                    unitPriceMinor = 5L,
                ),
            ),
        )

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertEquals(emptyList(), reading?.items)
    }

    @Test
    fun `GIVEN an item of 999 units WHEN mapping the reading THEN keeps it`() {
        // Given
        val dto = supermarketDto(
            items = listOf(
                ReadReceiptItemDto(
                    name = "PREGO 2CM",
                    quantity = 999L,
                    unitPriceMinor = 5L,
                ),
            ),
        )

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertEquals(listOf(ReceiptItem("PREGO 2CM", 999L, 5L)), reading?.items)
    }

    @Test
    fun `GIVEN an item of no price WHEN mapping the reading THEN drops it`() {
        // Given
        val dto = supermarketDto(
            items = listOf(
                ReadReceiptItemDto(
                    name = "BRINDE",
                    quantity = 1L,
                    unitPriceMinor = 0L,
                ),
            ),
        )

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertEquals(emptyList(), reading?.items)
    }

    @Test
    fun `GIVEN a discount line with a negative price WHEN mapping the reading THEN drops it`() {
        // Given
        val dto = supermarketDto(
            items = listOf(
                ReadReceiptItemDto(
                    name = "DESCONTO CLUBE",
                    quantity = 1L,
                    unitPriceMinor = -300L,
                ),
            ),
        )

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertEquals(emptyList(), reading?.items)
    }

    @Test
    fun `GIVEN more than 200 items WHEN mapping the reading THEN keeps the first 200`() {
        // Given
        val dto = supermarketDto(
            items = (1..205).map {
                ReadReceiptItemDto(
                    name = "ITEM $it",
                    quantity = 1L,
                    unitPriceMinor = 100L,
                )
            },
        )

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertNotNull(reading)
        assertEquals(200, reading.items.size)
        assertEquals("ITEM 1", reading.items.first().name)
        assertEquals("ITEM 200", reading.items.last().name)
    }

    @Test
    fun `GIVEN a receipt that lists no items WHEN mapping the reading THEN keeps the total with no items`() {
        // Given
        val dto = supermarketDto(items = emptyList())

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertNotNull(reading)
        assertEquals(8_743L, reading.totalMinor)
        assertTrue(reading.items.isEmpty())
    }

    @Test
    fun `GIVEN a negative service WHEN mapping the reading THEN reads no service`() {
        // Given
        val dto = supermarketDto(servicePercent = -10L)

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertEquals(0L, reading?.servicePercent)
    }

    @Test
    fun `GIVEN a service over 100 percent WHEN mapping the reading THEN caps it at 100`() {
        // Given
        val dto = supermarketDto(servicePercent = 150L)

        // When
        val reading = mapper.toDomain(dto, GroupCurrency.BRL)

        // Then
        assertEquals(100L, reading?.servicePercent)
    }

    private fun supermarketDto(
        totalMinor: Long = 8_743L,
        description: String? = "Supermercado Bom Preço",
        category: String? = "food.market",
        date: String? = "2026-10-30",
        time: String? = "19:42",
        items: List<ReadReceiptItemDto> = listOf(
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
            ReadReceiptItemDto(
                name = "CAFE TORRADO 500G",
                quantity = 1L,
                unitPriceMinor = 1_879L,
            ),
            ReadReceiptItemDto(
                name = "BANANA PRATA KG",
                quantity = 2L,
                unitPriceMinor = 600L,
            ),
        ),
        servicePercent: Long = 0L,
    ): ReadReceiptResponseDto = ReadReceiptResponseDto(
        totalMinor = totalMinor,
        description = description,
        category = category,
        date = date,
        time = time,
        items = items,
        servicePercent = servicePercent,
    )
}
