package com.quare.blitzsplit.core.expenses.testing

import com.quare.blitzsplit.core.expenses.customcolor.CustomColorUseCases
import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor

/*
 * CustomColorUseCases over a FakeCustomColorRepository, for a screen's ViewModel test: the palette is the repository's,
 * and usedBy answers which category uses a color (none by default). The palette's limit is the real use case's to test.
 */
fun createFakeCustomColorUseCases(
    repository: FakeCustomColorRepository = FakeCustomColorRepository(),
    usedBy: (CustomColor) -> Category? = { null },
): CustomColorUseCases = CustomColorUseCases(
    observeCustomColors = { repository.observeColors() },
    addCustomColor = { repository.add(it, 0L) },
    deleteCustomColor = { repository.delete(it, 0L) },
    findCustomColorUser = { usedBy(it) },
)
