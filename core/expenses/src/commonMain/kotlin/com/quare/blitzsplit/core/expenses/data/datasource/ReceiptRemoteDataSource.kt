package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptRequestDto
import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptResponseDto

// Why: reading a receipt's photo happens on the server (read-receipt), which keeps the reader's key.
fun interface ReceiptRemoteDataSource {
    // Why: null when the photo shows no receipt, or nothing that can be saved as a total.
    suspend fun readReceipt(request: ReadReceiptRequestDto): ReadReceiptResponseDto?
}
