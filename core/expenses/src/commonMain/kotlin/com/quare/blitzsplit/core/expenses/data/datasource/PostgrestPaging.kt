package com.quare.blitzsplit.core.expenses.data.datasource

import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order

/*
 * Why: page by page until one comes back empty: a row missing from the answer would read as removed on the server and
 * leave the device. Each page starts after the last id of the one before, not at an offset, since other members keep
 * adding bills and payments meanwhile, which would shift an offset and skip a row. A page is the API's max_rows,
 * Supabase's default.
 */
internal suspend inline fun <reified T : Any> Postgrest.fetchAllById(
    table: String,
    idOf: (T) -> String,
    pageSize: Long = 1_000L,
): List<T> {
    val rows = mutableListOf<T>()
    do {
        val lastId = rows.lastOrNull()?.let(idOf)
        val page = from(table)
            .select {
                if (lastId != null) filter { gt("id", lastId) }
                order(
                    column = "id",
                    order = Order.ASCENDING,
                )
                limit(pageSize)
            }.decodeList<T>()
        rows += page
    } while (page.isNotEmpty())
    return rows
}
