package com.quare.blitzsplit.core.support.domain.usecase

import com.quare.blitzsplit.core.support.domain.model.SupportDiagnostics
import kotlinx.coroutines.flow.Flow

fun interface ObserveSupportDiagnostics {
    operator fun invoke(): Flow<SupportDiagnostics>
}
