package com.quare.blitzsplit.core.groups.data.datasource

import com.quare.blitzsplit.core.groups.data.dto.GroupMemberIncomeDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order

class SupabaseGroupIncomeRemoteDataSource(
    private val postgrest: Postgrest,
) : GroupIncomeRemoteDataSource {
    override suspend fun fetchIncomes(): List<GroupMemberIncomeDto> = fetchAll(groupId = null)

    override suspend fun fetchIncomes(groupId: String): List<GroupMemberIncomeDto> = fetchAll(groupId)

    override suspend fun upsertIncomes(incomes: List<GroupMemberIncomeDto>) {
        postgrest.from(GROUP_MEMBER_INCOMES).upsert(incomes) {
            onConflict = "member_id"
        }
    }

    /*
     * Why: page by page after the last member id, until one comes back empty, as the bills do: a row missing from the
     * answer would read as gone from the server.
     */
    private suspend fun fetchAll(groupId: String?): List<GroupMemberIncomeDto> {
        val rows = mutableListOf<GroupMemberIncomeDto>()
        do {
            val lastId = rows.lastOrNull()?.memberId
            val page = postgrest
                .from(GROUP_MEMBER_INCOMES)
                .select {
                    filter {
                        if (groupId != null) eq("group_id", groupId)
                        if (lastId != null) gt("member_id", lastId)
                    }
                    order(
                        column = "member_id",
                        order = Order.ASCENDING,
                    )
                    limit(PAGE_SIZE)
                }.decodeList<GroupMemberIncomeDto>()
            rows += page
        } while (page.isNotEmpty())
        return rows
    }

    private companion object {
        const val GROUP_MEMBER_INCOMES = "group_member_incomes"
        const val PAGE_SIZE = 1_000L
    }
}
