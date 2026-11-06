package com.quare.blitzsplit.core.expenses.data.repository

import com.quare.blitzsplit.core.expenses.domain.repository.PickedReceiptRepository

/*
 * Why: nothing to keep across launches: the photo only matters between Início's tap and Split a bill opening, both on
 * the main thread.
 */
class InMemoryPickedReceiptRepository : PickedReceiptRepository {
    private var photo: ByteArray? = null

    override fun hold(photo: ByteArray) {
        this.photo = photo
    }

    override fun take(): ByteArray? = photo.also { photo = null }
}
