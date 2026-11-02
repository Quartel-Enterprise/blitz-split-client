package com.quare.blitzsplit.feature.billdetail.presentation.model

enum class BillPhotoError {
    // Why: "Não deu para abrir essa imagem."
    UNREADABLE,

    // Why: "Essa foto é grande demais.": no quality brings it under the limit.
    TOO_LARGE,

    // Why: the bill already got its 5 photos, maybe on another device meanwhile.
    NO_ROOM,

    // Why: the device couldn't keep it (its bill went meanwhile).
    FAILED,
}
