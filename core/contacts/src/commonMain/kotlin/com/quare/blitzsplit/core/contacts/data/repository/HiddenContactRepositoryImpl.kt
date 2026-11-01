package com.quare.blitzsplit.core.contacts.data.repository

import com.quare.blitzsplit.core.contacts.data.datasource.HiddenContactLocalDataSource
import com.quare.blitzsplit.core.contacts.data.datasource.HiddenContactRemoteDataSource
import com.quare.blitzsplit.core.contacts.data.mapper.HiddenContactMapper
import com.quare.blitzsplit.core.contacts.domain.model.HiddenContact
import com.quare.blitzsplit.core.contacts.domain.repository.HiddenContactRepository
import com.quare.blitzsplit.core.provider.room.entity.HiddenContactEntity
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class HiddenContactRepositoryImpl(
    private val local: HiddenContactLocalDataSource,
    private val remote: HiddenContactRemoteDataSource,
    private val currentTimeMillis: CurrentTimeMillis,
) : HiddenContactRepository {
    override fun observeHidden(): Flow<List<HiddenContact>> = local
        .observeAll()
        .map { rows -> rows.map(HiddenContactMapper::toDomain) }
        .distinctUntilChanged()

    override fun observeHasPendingChanges(): Flow<Boolean> = local.observeHasPending().distinctUntilChanged()

    // Why: "Mostrar" keeps when it was hidden; "Ocultar" starts the count of the bills that bring them back.
    override suspend fun setHidden(
        personKey: String,
        isHidden: Boolean,
    ) {
        val stored = local.getOrNull(personKey)
        val now = currentTimeMillis()
        local.upsert(
            HiddenContactEntity(
                personKey = personKey,
                isHidden = isHidden,
                hiddenAt = if (isHidden) now else stored?.hiddenAt ?: now,
                updatedAt = maxOf(now, (stored?.updatedAt ?: 0L) + 1),
                pendingSync = true,
                ownerUserId = stored?.ownerUserId,
            ),
        )
    }

    override suspend fun uploadPending(userId: String): Result<Unit> = suspendRunCatching {
        val pending = local.getAll().filter { it.pendingSync && (it.ownerUserId == null || it.ownerUserId == userId) }
        if (pending.isEmpty()) return@suspendRunCatching
        remote.upsert(pending.map { HiddenContactMapper.toDto(it, userId) })
        pending.forEach {
            local.markSynced(
                personKey = it.personKey,
                syncedUpdatedAt = it.updatedAt,
            )
        }
    }

    override suspend fun download(userId: String): Result<Unit> = suspendRunCatching {
        val rows = remote.fetch(userId)
        local.deleteOtherUsersRows(userId)
        local.claimUnowned(userId)
        rows.forEach { dto -> local.applyRemote(HiddenContactMapper.toEntity(dto, ownerUserId = userId)) }
    }

    override suspend fun clear() {
        local.clear()
    }
}
