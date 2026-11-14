package com.quare.blitzsplit.core.expenses.export

import com.quare.blitzsplit.core.model.GroupCurrency
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Instant

/*
 * Why: the data as it is, for a script or another app: English keys, ISO dates ("2026-11-14") and instants in UTC,
 * amounts as decimal numbers in their currency with the minor units beside them, members by id with their names. Only
 * the category names come in the app's language.
 */
internal class JsonGroupExport : GroupExportEncoder {
    private val prettyJson = Json { prettyPrint = true }

    override fun encode(
        export: GroupExport,
        words: GroupExportWords,
    ): ByteArray {
        val names = GroupExportTables.namesOf(export, words)
        val json = buildJsonObject {
            put("group", buildJsonObject { put("name", export.groupName) })
            put("exportedOn", export.exportedOn.toString())
            put(
                "members",
                JsonArray(
                    export.members.map { member ->
                        buildJsonObject {
                            put("id", member.id)
                            put("name", names.getValue(member.id))
                            put("isMe", member.isMe)
                            put("hasJoined", member.hasJoined)
                            put("hasLeft", member.name == null)
                        }
                    },
                ),
            )
            put("bills", JsonArray(export.bills.map { billOf(it, words, names) }))
            put("payments", JsonArray(export.payments.map { paymentOf(it, names) }))
            put(
                "balances",
                JsonArray(
                    export.balances.map { balances ->
                        buildJsonObject {
                            put("currency", balances.currency.code)
                            put("members", amountsOf(balances.members, balances.currency, names, "balance"))
                        }
                    },
                ),
            )
        }
        return prettyJson.encodeToString(JsonObject.serializer(), json).encodeToByteArray()
    }

    private fun billOf(
        bill: ExportBill,
        words: GroupExportWords,
        names: Map<String, String>,
    ): JsonObject = buildJsonObject {
        put("id", bill.id)
        put("date", bill.date.toString())
        put("spentAt", Instant.fromEpochMilliseconds(bill.spentAt).toString())
        put("description", bill.description)
        put(
            "category",
            buildJsonObject {
                put("id", bill.category.id)
                put("name", GroupExportTables.categoryNameOf(bill, words))
            },
        )
        put("amount", amountOf(bill.amountMinor, bill.currency))
        put("amountMinor", bill.amountMinor)
        put("currency", bill.currency.code)
        put("payers", amountsOf(bill.payers, bill.currency, names, "amount"))
        put("shares", amountsOf(bill.shares, bill.currency, names, "amount"))
        bill.payingMethod?.let { method ->
            put(
                "paymentMethod",
                buildJsonObject {
                    put("type", method.type.code)
                    put("name", GroupExportTables.payingMethodNameOf(bill, words))
                },
            )
        }
    }

    private fun paymentOf(
        payment: ExportPayment,
        names: Map<String, String>,
    ): JsonObject = buildJsonObject {
        put("id", payment.id)
        put("date", payment.date.toString())
        put("paidAt", Instant.fromEpochMilliseconds(payment.paidAt).toString())
        put("fromMemberId", payment.fromMemberId)
        put("fromName", names.getValue(payment.fromMemberId))
        put("toMemberId", payment.toMemberId)
        put("toName", names.getValue(payment.toMemberId))
        put("amount", amountOf(payment.amountMinor, payment.currency))
        put("amountMinor", payment.amountMinor)
        put("currency", payment.currency.code)
        put("method", payment.method.code)
    }

    private fun amountsOf(
        amounts: List<ExportAmount>,
        currency: GroupCurrency,
        names: Map<String, String>,
        key: String,
    ): JsonArray = buildJsonArray {
        amounts.forEach { amount ->
            add(
                buildJsonObject {
                    put("memberId", amount.memberId)
                    put("name", names.getValue(amount.memberId))
                    put(key, amountOf(amount.amountMinor, currency))
                    put("${key}Minor", amount.amountMinor)
                },
            )
        }
    }

    // Why: 120.50 as a number with every decimal of the currency, never a Double's 120.5 or 0.1 + 0.2.
    @OptIn(ExperimentalSerializationApi::class)
    private fun amountOf(
        minor: Long,
        currency: GroupCurrency,
    ): JsonElement = JsonUnquotedLiteral(decimalOf(minor, currency))
}
