@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.groups.presentation.model.SearchUiEvent
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.SearchViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

// Why: "Buscar" (⌘K) over Início, Saldos, Contatos or Atividade, the same dialog as Grupos' in its own route.
class SearchPresenter {
    private val holder = ViewModelHolder(SearchViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsGroupsSearch) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state -> toJsGroupsSearch(state.search) }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: Esc or a click outside.
    fun closeSearch() {
        viewModel.onEvent(SearchUiEvent.OnClosed)
    }

    fun search(query: String) {
        viewModel.onEvent(SearchUiEvent.OnQueryChanged(query))
    }

    // Why: by SearchScope's name; a name this version doesn't know does nothing.
    fun searchIn(scope: String) {
        searchScopeOrNull(scope)?.let { viewModel.onEvent(SearchUiEvent.OnScopeChanged(it)) }
    }

    // Why: what was found opens in the search's place.
    fun openSearchedGroup(groupId: String) {
        viewModel.onEvent(SearchUiEvent.OnGroupClicked(groupId))
    }

    fun openSearchedPerson(groupId: String) {
        viewModel.onEvent(SearchUiEvent.OnPersonClicked(groupId))
    }

    fun openSearchedContact(personKey: String) {
        viewModel.onEvent(SearchUiEvent.OnContactClicked(personKey))
    }

    // Why: "Did you mean “Larissa”?" searches the suggestion instead.
    fun searchSuggestion(suggestion: String) {
        viewModel.onEvent(SearchUiEvent.OnSuggestionClicked(suggestion))
    }

    fun openSearchedBill(
        groupId: String,
        expenseId: String,
    ) {
        viewModel.onEvent(SearchUiEvent.OnBillClicked(groupId, expenseId))
    }

    fun dispose() {
        holder.clear()
    }
}
