package com.quare.blitzsplit.feature.splitbill.presentation.model

// Why: what the line under Rachar conta's description says about the category (7.1, 7.1l to 7.1n).
enum class CategoryHint {
    // Why: "Alimentação › Mercado pela palavra “mercado” do seu mapa. Toque no ícone para trocar."
    BY_TERM,

    // Why: "Três palavras do mapa casaram. Vale “uber eats”, a mais longa: Alimentação › Delivery."
    BY_LONGEST_TERM,

    // Why: "Nenhuma palavra do seu mapa. A conta fica em Outros; toque no ícone para escolher."
    NO_TERM,

    // Why: "Você escolheu Lazer › Esportes. Pelo mapa seria Casa › Aluguel (“aluguel”)."
    CHANGED,

    // Why: "Categoria: Alimentação › Mercado. Toque no ícone para trocar.", picked by hand or a saved bill's.
    PICKED,
}
