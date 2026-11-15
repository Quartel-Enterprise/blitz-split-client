package com.quare.blitzsplit.core.expenses.customcolor

import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

/*
 * Why: the person's own colors in a color picker send the same events from every screen that has one, told apart by
 * source ("category_map", "split_bill", "paying_method"); each name written here so the catalog test reads it.
 */
fun customColorAnalyticsOf(
    event: CustomColorEvent,
    source: String,
): EventAnalytics = when (event) {
    /*
     * Why: no action of the person's: the picker closing by itself, or the gradient and fields as they change ("Add"
     * tells what came of them).
     */
    CustomColorEvent.OnPickerClosed,
    is CustomColorEvent.OnChannelChanged,
    is CustomColorEvent.OnHexChanged,
    is CustomColorEvent.OnHsvChanged,
    -> EventAnalytics.NotTracked

    CustomColorEvent.OnDeletionDismissed -> EventAnalytics.Automatic(
        "custom_color_delete_cancelled",
        mapOf("source" to source),
    )

    CustomColorEvent.OnDeleteConfirmed -> EventAnalytics.Automatic(
        "custom_color_delete_confirmed",
        mapOf("source" to source),
    )

    is CustomColorEvent.OnLongPressed -> EventAnalytics.Automatic(
        "custom_color_delete_asked",
        mapOf("source" to source),
    )

    CustomColorEvent.OnDraftDismissed -> EventAnalytics.Automatic("custom_color_cancelled", mapOf("source" to source))

    CustomColorEvent.OnSaveClicked -> EventAnalytics.Automatic("custom_color_save_clicked", mapOf("source" to source))

    is CustomColorEvent.OnAddClicked -> EventAnalytics.Automatic("custom_color_add_clicked", mapOf("source" to source))
}
