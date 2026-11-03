package com.quare.blitzsplit.core.expenses.testing

import com.quare.blitzsplit.core.expenses.domain.model.ReceiptCategoryHints

// A read with nothing of the group's: no category of its own and no term, as an older app reads.
val noReceiptCategoryHints = ReceiptCategoryHints(
    groupCategories = emptyList(),
    terms = emptyList(),
)
