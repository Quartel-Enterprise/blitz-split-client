package com.quare.blitzsplit.core.contacts.data.datasource

import com.quare.blitzsplit.core.contacts.data.dto.HiddenContactDto

interface HiddenContactRemoteDataSource {
    suspend fun upsert(rows: List<HiddenContactDto>)

    suspend fun fetch(userId: String): List<HiddenContactDto>
}
