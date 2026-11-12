package com.quare.blitzsplit.core.groups.data.datasource

import com.quare.blitzsplit.core.groups.data.dto.GroupMemberIncomeDto

interface GroupIncomeRemoteDataSource {
    // Why: every income of the user's groups; the server's row level security decides which.
    suspend fun fetchIncomes(): List<GroupMemberIncomeDto>

    suspend fun fetchIncomes(groupId: String): List<GroupMemberIncomeDto>

    suspend fun upsertIncomes(incomes: List<GroupMemberIncomeDto>)
}
