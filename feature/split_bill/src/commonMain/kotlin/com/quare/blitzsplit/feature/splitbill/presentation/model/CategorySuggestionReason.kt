package com.quare.blitzsplit.feature.splitbill.presentation.model

enum class CategorySuggestionReason {
    // Why: the map's term in the description chose it.
    DESCRIPTION,

    // Why: the group's bills use it the most.
    MOST_USED,

    // Why: someone created it in the group.
    GROUP,

    // Why: used in the group, with nothing more to say.
    USED,
}
