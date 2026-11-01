package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull

/*
 * Why: one bill whose items another version wrote in a shape this one can't read (a field missing or of another type)
 * reads as having no items, so it falls back to its shares, instead of failing the page and every bill's sync.
 */
object LenientExpenseItemsSerializer : KSerializer<ExpenseItemsDto?> {
    private val delegate = ExpenseItemsDto.serializer().nullable

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(
        encoder: Encoder,
        value: ExpenseItemsDto?,
    ) {
        delegate.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): ExpenseItemsDto? {
        val jsonDecoder = decoder as? JsonDecoder ?: return delegate.deserialize(decoder)
        val element = jsonDecoder.decodeJsonElement()
        if (element is JsonNull) return null
        return try {
            jsonDecoder.json.decodeFromJsonElement(ExpenseItemsDto.serializer(), element)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
