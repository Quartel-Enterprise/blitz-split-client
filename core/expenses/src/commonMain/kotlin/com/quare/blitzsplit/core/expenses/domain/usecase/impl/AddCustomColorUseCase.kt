package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.model.CustomPalette
import com.quare.blitzsplit.core.expenses.domain.model.CustomPaletteFullException
import com.quare.blitzsplit.core.expenses.domain.repository.CustomColorRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.AddCustomColor
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import kotlinx.coroutines.flow.first

// Why: on the device right away, offline included; the person's other devices get it once synced.
class AddCustomColorUseCase(
    private val repository: CustomColorRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : AddCustomColor {
    override suspend fun invoke(color: CustomColor): Result<Unit> {
        val palette = repository.observeColors().first()
        if (color in palette) return Result.success(Unit)
        if (palette.size >= CustomPalette.MAX_COLORS) return Result.failure(CustomPaletteFullException())
        return repository.add(color, currentTimeMillis())
    }
}
