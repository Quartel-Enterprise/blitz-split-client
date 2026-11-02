package com.quare.blitzsplit.android.designsystem.layout

// Why: a form the canvas draws its own way on a tabletop (Rachar conta, Dividir, Itens da conta) says where "Salvar" goes.
enum class TabletopForm {
    // Why: Dividir's board keeps "Pronto" in the top bar.
    ConfirmInBar,

    // Why: Rachar conta's and Itens' boards close the bottom half's controls with it, within thumb reach.
    ConfirmAtBottom,
}
