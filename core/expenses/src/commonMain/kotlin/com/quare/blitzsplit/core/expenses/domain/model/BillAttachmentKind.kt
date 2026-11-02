package com.quare.blitzsplit.core.expenses.domain.model

// Why: "Anexos" (7.3g): the receipt a bill was read from comes first and isn't one of its 5 photos.
enum class BillAttachmentKind(
    val code: String,
    val limit: Int,
) {
    PHOTO("photo", 5),
    RECEIPT("receipt", 1),
    ;

    companion object {
        fun fromCodeOrNull(code: String): BillAttachmentKind? = entries.firstOrNull { it.code == code }
    }
}
