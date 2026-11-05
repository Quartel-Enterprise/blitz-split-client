package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

/*
 * Why: what "Buscar" (5.2) takes, the same wherever it opens: in Grupos' place (GroupsUiEvent.OnSearch) or over
 * Início, Saldos and Contatos (Route.Search).
 */
sealed interface SearchUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = toEventAnalytics(source = "search")

    // Why: the same search over the tabs (Route.Search) and in Grupos' place, source saying which.
    fun toEventAnalytics(source: String): EventAnalytics = when (this) {
        is OnQueryChanged -> EventAnalytics.NotTracked

        is OnScopeChanged -> EventAnalytics.Automatic(
            "search_scope_selected",
            mapOf(
                "scope" to scope,
                "source" to source,
            ),
        )

        is OnGroupClicked -> EventAnalytics.Automatic(
            "search_result_clicked",
            mapOf(
                "kind" to "group",
                "source" to source,
            ),
        )

        is OnPersonClicked -> EventAnalytics.Automatic(
            "search_result_clicked",
            mapOf(
                "kind" to "person",
                "source" to source,
            ),
        )

        is OnContactClicked -> EventAnalytics.Automatic(
            "search_result_clicked",
            mapOf(
                "kind" to "contact",
                "source" to source,
            ),
        )

        is OnBillClicked -> EventAnalytics.Automatic(
            "search_result_clicked",
            mapOf("kind" to "bill", "source" to source),
        )

        is OnSuggestionClicked -> EventAnalytics.Automatic(
            "search_suggestion_clicked",
            mapOf("source" to source),
        )

        // Why: a close, which each platform also does its own way: where it went is its screen view.
        OnClosed -> EventAnalytics.NotTracked

        // Why: a tab of the home's bar or rail, which Android draws around the search: where it went is its screen view.
        is OnHomeTabClicked -> EventAnalytics.NotTracked
    }

    data class OnQueryChanged(
        val query: String,
    ) : SearchUiEvent

    // Why: "Tudo", "Grupos", "Pessoas" or "Despesas".
    data class OnScopeChanged(
        val scope: SearchScope,
    ) : SearchUiEvent

    data class OnGroupClicked(
        val groupId: String,
    ) : SearchUiEvent

    // Why: a person found opens the members of the group they were found in.
    data class OnPersonClicked(
        val groupId: String,
    ) : SearchUiEvent

    // Why: a person found once, above their rows in each group, opens their profile, as from Contacts.
    data class OnContactClicked(
        val personKey: String,
    ) : SearchUiEvent

    // Why: "Did you mean “Larissa”?" searches that instead of what was typed.
    data class OnSuggestionClicked(
        val suggestion: String,
    ) : SearchUiEvent

    // Why: a bill found opens over its group, so back goes to the group's bills, as from Despesas.
    data class OnBillClicked(
        val groupId: String,
        val expenseId: String,
    ) : SearchUiEvent

    // Why: back or ✕ on Android, Esc or a click outside on the web, the field cancelled on Apple.
    data object OnClosed : SearchUiEvent

    /*
     * Why: a tab of the bar or rail Android draws around the search, Search selected as its last item (Pierre's call):
     * the tab it was opened over goes back to it, any other takes its place, as the home's tabs do.
     */
    data class OnHomeTabClicked(
        val tab: HomeTab,
    ) : SearchUiEvent
}
