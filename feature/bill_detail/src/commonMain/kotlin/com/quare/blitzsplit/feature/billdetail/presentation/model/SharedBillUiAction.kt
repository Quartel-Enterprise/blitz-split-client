package com.quare.blitzsplit.feature.billdetail.presentation.model

sealed interface SharedBillUiAction {
    // Why: "Não deu para enviar o comentário. Tente de novo."
    data object ShowCommentError : SharedBillUiAction

    // Why: "Crie seu perfil para comentar.": someone signed in whose name neither the server nor the device knows.
    data object ShowNeedsName : SharedBillUiAction
}
