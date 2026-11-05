package com.quare.blitzsplit.core.groups.data.repository

import com.quare.blitzsplit.core.groups.domain.repository.AddPersonRequestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Why: a tap's ask, which only matters while the app runs.
class InMemoryAddPersonRequestRepository : AddPersonRequestRepository {
    private val request = MutableStateFlow<String?>(null)

    override fun observeRequest(): Flow<String?> = request

    override fun post(groupId: String) {
        request.value = groupId
    }

    override fun consume(groupId: String) {
        request.update { if (it == groupId) null else it }
    }
}
