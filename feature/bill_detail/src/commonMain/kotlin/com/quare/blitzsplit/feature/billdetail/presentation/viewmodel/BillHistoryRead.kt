package com.quare.blitzsplit.feature.billdetail.presentation.viewmodel

import com.quare.blitzsplit.core.activity.domain.usecase.LoadBillHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/*
 * Why: "Histórico" reads the device's 90 days of Atividade and, online, the bill's whole history from the server,
 * which lands in the same events. Until the server answered online the lines shimmer; offline, or once its read
 * failed, the device's lines are all there is to show.
 */
internal class BillHistoryRead(
    private val loadBillHistory: LoadBillHistory,
    private val billId: String,
    private val scope: CoroutineScope,
    private val onChanged: () -> Unit,
) {
    // Why: true until connectivity is known, so nothing claims the device's lines are the whole history before it.
    var isLoading: Boolean = true
        private set

    var hasServerAnswered: Boolean = false
        private set

    // Why: read again each time the device comes back online, as a bill left open may have changed elsewhere.
    fun onConnectivityChanged(isOnline: Boolean) {
        isLoading = isOnline && !hasServerAnswered
        onChanged()
        if (!isOnline) return
        scope.launch {
            val result = loadBillHistory(billId)
            if (result.isSuccess) hasServerAnswered = true
            isLoading = false
            onChanged()
        }
    }
}
