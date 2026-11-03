package com.quare.blitzsplit.feature.groups.presentation.model

// Why: "Buscar" over Início, Saldos, Contatos or Atividade (Route.Search), always open while it shows.
data class SearchUiState(
    val search: GroupsSearch,
)
