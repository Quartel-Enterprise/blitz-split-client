package com.quare.blitzsplit.core.expenses.export

import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonGroupExportTest {
    private val encoder = JsonGroupExport()
    private val words = exportWords()

    @Test
    fun `GIVEN words in Portuguese WHEN writing the JSON THEN the keys stay in English`() {
        // Given
        val export = exportOf()
        val portuguese = exportWords(
            bills = "Contas",
            payments = "Pagamentos",
            balances = "Saldos",
        )

        // When
        val json = jsonOf(encoder.encode(export, portuguese))

        // Then
        assertEquals(setOf("group", "exportedOn", "members", "bills", "payments", "balances"), json.keys)
        assertEquals(
            "Trip to Rio",
            json
                .getValue("group")
                .jsonObject
                .getValue("name")
                .jsonPrimitive.content,
        )
        assertEquals("2026-11-14", json.getValue("exportedOn").jsonPrimitive.content)
    }

    @Test
    fun `GIVEN a member who left WHEN writing the JSON THEN members have id name isMe hasJoined and hasLeft`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                    payers = mapOf(CARLA_LEFT to 100L),
                ),
            ),
        )

        // When
        val json = jsonOf(encoder.encode(export, words))

        // Then
        val members = json.getValue("members").jsonArray.map { it.jsonObject }
        assertEquals(
            mapOf(
                "id" to JsonPrimitive(PIERRE),
                "name" to JsonPrimitive("Pierre"),
                "isMe" to JsonPrimitive(true),
                "hasJoined" to JsonPrimitive(true),
                "hasLeft" to JsonPrimitive(false),
            ),
            members[0].toMap(),
        )
        assertEquals(JsonPrimitive(false), members[2].getValue("hasJoined"))
        assertEquals(
            mapOf(
                "id" to JsonPrimitive(CARLA_LEFT),
                "name" to JsonPrimitive("Former member"),
                "isMe" to JsonPrimitive(false),
                "hasJoined" to JsonPrimitive(false),
                "hasLeft" to JsonPrimitive(true),
            ),
            members[3].toMap(),
        )
    }

    @Test
    fun `GIVEN a bill past midnight UTC in Sao Paulo WHEN writing the JSON THEN date is local and spentAt UTC`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                    spentAt = NOV_14_0230_UTC,
                ),
            ),
            timeZone = saoPaulo,
        )

        // When
        val json = jsonOf(encoder.encode(export, words))

        // Then
        val bill = json
            .getValue("bills")
            .jsonArray
            .single()
            .jsonObject
        assertEquals("2026-11-13", bill.getValue("date").jsonPrimitive.content)
        assertEquals("2026-11-14T02:30:00Z", bill.getValue("spentAt").jsonPrimitive.content)
    }

    @Test
    fun `GIVEN a bill of 120_50 reais WHEN writing the JSON THEN the amount is the number 120_50 and amountMinor`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 12_050L,
                ),
            ),
        )

        // When
        val bytes = encoder.encode(export, words)

        // Then
        val bill = jsonOf(bytes)
            .getValue("bills")
            .jsonArray
            .single()
            .jsonObject
        val amount = bill.getValue("amount").jsonPrimitive
        assertFalse(amount.isString)
        assertEquals("120.50", amount.content)
        assertEquals("12050", bill.getValue("amountMinor").jsonPrimitive.content)
        assertEquals("BRL", bill.getValue("currency").jsonPrimitive.content)
        assertTrue(bytes.decodeToString().contains("\"amount\": 120.50"))
    }

    @Test
    fun `GIVEN bills in yen and in bitcoin WHEN writing the JSON THEN each amount has its currency decimals`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "ramen",
                    amountMinor = 1_500L,
                    currency = GroupCurrency.JPY,
                    spentAt = NOV_10_NOON,
                ),
                exportBill(
                    id = "pizza",
                    amountMinor = 125_000L,
                    currency = GroupCurrency.BTC,
                    spentAt = NOV_11_NOON,
                ),
            ),
        )

        // When
        val json = jsonOf(encoder.encode(export, words))

        // Then
        val amounts = json.getValue("bills").jsonArray.map { it.jsonObject.getValue("amount").jsonPrimitive }
        assertEquals(listOf("1500", "0.00125000"), amounts.map { it.content })
        assertTrue(amounts.none { it.isString })
    }

    @Test
    fun `GIVEN an app category WHEN writing the JSON THEN the category has its id and its name in the UI words`() {
        // Given
        val export =
            exportOf(
                bills = listOf(
                    exportBill(
                        id = "dinner",
                        amountMinor = 100L,
                        categoryId = "food.restaurant",
                    ),
                ),
            )

        // When
        val json = jsonOf(encoder.encode(export, words))

        // Then
        val category = json
            .getValue("bills")
            .jsonArray
            .single()
            .jsonObject
            .getValue("category")
            .jsonObject
        assertEquals(
            mapOf("id" to JsonPrimitive("food.restaurant"), "name" to JsonPrimitive("Restaurant")),
            category.toMap(),
        )
    }

    @Test
    fun `GIVEN a bill with no category WHEN writing the JSON THEN its category is Other`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                    categoryId = null,
                ),
            ),
        )

        // When
        val json = jsonOf(encoder.encode(export, words))

        // Then
        val category = json
            .getValue("bills")
            .jsonArray
            .single()
            .jsonObject
            .getValue("category")
            .jsonObject
        assertEquals(mapOf("id" to JsonPrimitive("other"), "name" to JsonPrimitive("Other")), category.toMap())
    }

    @Test
    fun `GIVEN a split bill WHEN writing the JSON THEN payers and shares name each member with the amounts`() {
        // Given
        val bill = exportBill(
            id = "dinner",
            amountMinor = 9_000L,
            payers = mapOf(PIERRE to 9_000L),
            shares = mapOf(PIERRE to 4_500L, ANA to 4_500L),
        )
        val export = exportOf(bills = listOf(bill))

        // When
        val json = jsonOf(encoder.encode(export, words))

        // Then
        val shares = json
            .getValue("bills")
            .jsonArray
            .single()
            .jsonObject
            .getValue("shares")
            .jsonArray
        assertEquals(
            mapOf(
                "memberId" to JsonPrimitive(ANA),
                "name" to JsonPrimitive("Ana"),
                "amount" to "45.00",
                "amountMinor" to "4500",
            ),
            shares[1].jsonObject.toContentMap(),
        )
        val payers = json
            .getValue("bills")
            .jsonArray
            .single()
            .jsonObject
            .getValue("payers")
            .jsonArray
        assertEquals(
            listOf(PIERRE),
            payers.map {
                it.jsonObject
                    .getValue("memberId")
                    .jsonPrimitive.content
            },
        )
    }

    @Test
    fun `GIVEN a payment by cash WHEN writing the JSON THEN it has members amount currency and method code`() {
        // Given
        val payment = exportPayment(
            id = "cash",
            amountMinor = 4_000L,
            from = ANA,
            to = PIERRE,
            paidAt = NOV_14_0230_UTC,
            method = SettlementMethod.CASH,
        )
        val export = exportOf(
            payments = listOf(payment),
            timeZone = saoPaulo,
        )

        // When
        val json = jsonOf(encoder.encode(export, words))

        // Then
        assertEquals(
            mapOf(
                "id" to JsonPrimitive("cash"),
                "date" to JsonPrimitive("2026-11-13"),
                "paidAt" to JsonPrimitive("2026-11-14T02:30:00Z"),
                "fromMemberId" to JsonPrimitive(ANA),
                "fromName" to JsonPrimitive("Ana"),
                "toMemberId" to JsonPrimitive(PIERRE),
                "toName" to JsonPrimitive("Pierre"),
                "amount" to "40.00",
                "amountMinor" to "4000",
                "currency" to JsonPrimitive("BRL"),
                "method" to JsonPrimitive("cash"),
            ),
            json
                .getValue("payments")
                .jsonArray
                .single()
                .jsonObject
                .toContentMap(),
        )
    }

    @Test
    fun `GIVEN Pierre paid for Ana WHEN writing the JSON THEN the balances list each member by currency`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 1_000L,
                ),
            ),
        )

        // When
        val json = jsonOf(encoder.encode(export, words))

        // Then
        val balances = json
            .getValue("balances")
            .jsonArray
            .single()
            .jsonObject
        assertEquals("BRL", balances.getValue("currency").jsonPrimitive.content)
        assertEquals(
            listOf(
                mapOf(
                    "memberId" to JsonPrimitive(PIERRE),
                    "name" to JsonPrimitive("Pierre"),
                    "balance" to "10.00",
                    "balanceMinor" to "1000",
                ),
                mapOf(
                    "memberId" to JsonPrimitive(ANA),
                    "name" to JsonPrimitive("Ana"),
                    "balance" to "-10.00",
                    "balanceMinor" to "-1000",
                ),
                mapOf(
                    "memberId" to JsonPrimitive(BRUNO),
                    "name" to JsonPrimitive("Bruno"),
                    "balance" to "0.00",
                    "balanceMinor" to "0",
                ),
            ),
            balances.getValue("members").jsonArray.map { it.jsonObject.toContentMap() },
        )
    }

    @Test
    fun `GIVEN no bills nor payments WHEN writing the JSON THEN bills and payments are empty lists`() {
        // Given
        val export = exportOf()

        // When
        val json = jsonOf(encoder.encode(export, words))

        // Then
        assertEquals(0, json.getValue("bills").jsonArray.size)
        assertEquals(0, json.getValue("payments").jsonArray.size)
    }

    @Test
    fun `GIVEN a marked and an unmarked bill WHEN writing the JSON THEN only the marked bill has its payment method`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                ),
                exportBill(
                    id = "taxi",
                    amountMinor = 100L,
                    spentAt = NOV_11_NOON,
                ),
            ),
            payingMethods = mapOf(
                "dinner" to exportPayingMethod(
                    id = "card",
                    name = "Nubank",
                ),
            ),
        )

        // When
        val json = jsonOf(encoder.encode(export, words))

        // Then
        val bills = json.getValue("bills").jsonArray.map { it.jsonObject }
        assertEquals(
            mapOf(
                "type" to JsonPrimitive("credit_card"),
                "name" to JsonPrimitive("Nubank · Credit card"),
            ),
            bills[0].getValue("paymentMethod").jsonObject.toContentMap(),
        )
        assertFalse("paymentMethod" in bills[1])
    }

    private fun jsonOf(bytes: ByteArray): JsonObject = Json.parseToJsonElement(bytes.decodeToString()).jsonObject

    // Why: numbers as their text, so an amount's decimals are checked as written; strings stay JSON strings.
    private fun JsonObject.toContentMap(): Map<String, Any> = mapValues { (_, value) ->
        val primitive = value.jsonPrimitive
        if (primitive.isString) primitive else primitive.content
    }
}
