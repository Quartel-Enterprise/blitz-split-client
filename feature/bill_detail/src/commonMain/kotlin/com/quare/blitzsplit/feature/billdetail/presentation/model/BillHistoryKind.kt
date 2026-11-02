package com.quare.blitzsplit.feature.billdetail.presentation.model

// Why: what one line of "Histórico" says someone did to the bill.
enum class BillHistoryKind {
    // Why: "criou a conta", "criou a conta sem internet" while it waits to go up.
    CREATED,

    // Why: "trocou a categoria", "mudou a divisão"…, the first change and the others under it.
    EDITED,

    // Why: "anexou 2 fotos", the ones added in a row by the same person.
    ATTACHMENTS_ADDED,

    // Why: "tirou uma foto".
    ATTACHMENT_REMOVED,

    // Why: "ativou o link para qualquer pessoa", "desativou o link".
    LINK_CHANGED,
}
