package com.quare.blitzsplit.core.contacts.testing

import com.quare.blitzsplit.core.contacts.domain.model.HiddenContact
import com.quare.blitzsplit.core.contacts.domain.repository.HiddenContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// In-memory HiddenContactRepository: the state is public so a test can set and read it, every call is
// recorded, and the failure knobs are public vars.
class FakeHiddenContactRepository(
    private val now: () -> Long = { 0L },
    initialHidden: List<HiddenContact> = emptyList(),
) : HiddenContactRepository {
    val hidden = MutableStateFlow(initialHidden)
    val hasPendingChanges = MutableStateFlow(false)
    val uploadedUserIds = mutableListOf<String>()
    val downloadedUserIds = mutableListOf<String>()
    var setHiddenError: Throwable? = null
    var pushError: Throwable? = null
    var pullError: Throwable? = null
    var clearCount = 0

    override fun observeHidden(): Flow<List<HiddenContact>> = hidden

    override fun observeHasPendingChanges(): Flow<Boolean> = hasPendingChanges

    override suspend fun setHidden(
        personKey: String,
        isHidden: Boolean,
    ) {
        setHiddenError?.let { throw it }
        val stored = hidden.value.firstOrNull { it.personKey == personKey }
        val row = HiddenContact(
            personKey = personKey,
            isHidden = isHidden,
            hiddenAt = if (isHidden) now() else stored?.hiddenAt ?: now(),
        )
        hidden.value = hidden.value.filterNot { it.personKey == personKey } + row
        hasPendingChanges.value = true
    }

    override suspend fun uploadPending(userId: String): Result<Unit> {
        uploadedUserIds += userId
        pushError?.let { return Result.failure(it) }
        hasPendingChanges.value = false
        return Result.success(Unit)
    }

    override suspend fun download(userId: String): Result<Unit> {
        downloadedUserIds += userId
        pullError?.let { return Result.failure(it) }
        return Result.success(Unit)
    }

    override suspend fun clear() {
        clearCount++
        hidden.value = emptyList()
        hasPendingChanges.value = false
    }
}
