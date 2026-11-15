package com.quare.blitzsplit.core.expenses.customcolor

import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor
import com.quare.blitzsplit.core.expenses.domain.model.ColorDraft
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.model.tint
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomColorAnalyticsTest {
    @Test
    fun `GIVEN each action on the person colors WHEN mapping its Analytics THEN sends its event with the source`() {
        // Given
        val events = mapOf(
            CustomColorEvent.OnAddClicked(CategoryColor.TEAL.tint, null) to "custom_color_add_clicked",
            CustomColorEvent.OnSaveClicked to "custom_color_save_clicked",
            CustomColorEvent.OnDraftDismissed to "custom_color_cancelled",
            CustomColorEvent.OnLongPressed(CustomColor(0x3A7BD5)) to "custom_color_delete_asked",
            CustomColorEvent.OnDeleteConfirmed to "custom_color_delete_confirmed",
            CustomColorEvent.OnDeletionDismissed to "custom_color_delete_cancelled",
        )

        // When
        val analytics = events.keys.map { customColorAnalyticsOf(it, PAYING_METHOD) }

        // Then
        assertEquals(
            events.values.map { EventAnalytics.Automatic(it, mapOf("source" to PAYING_METHOD)) },
            analytics,
        )
    }

    @Test
    fun `GIVEN the same action from another screen WHEN mapping its Analytics THEN carries that screen as source`() {
        // Given
        val event = CustomColorEvent.OnSaveClicked

        // When
        val analytics = customColorAnalyticsOf(event, SPLIT_BILL)

        // Then
        assertEquals(
            EventAnalytics.Automatic("custom_color_save_clicked", mapOf("source" to SPLIT_BILL)),
            analytics,
        )
    }

    @Test
    fun `GIVEN the gradient the fields or the picker closing WHEN mapping their Analytics THEN none is tracked`() {
        // Given
        val events = listOf(
            CustomColorEvent.OnHsvChanged(
                hue = 200.0,
                saturation = 0.5,
                value = 0.8,
            ),
            CustomColorEvent.OnHexChanged("#3A7BD5"),
            CustomColorEvent.OnChannelChanged(ColorDraft.ColorChannel.RED, "58"),
            CustomColorEvent.OnPickerClosed,
        )

        // When
        val analytics = events.map { customColorAnalyticsOf(it, PAYING_METHOD) }

        // Then
        assertEquals(List(events.size) { EventAnalytics.NotTracked }, analytics)
    }

    private companion object {
        const val PAYING_METHOD = "paying_method"
        const val SPLIT_BILL = "split_bill"
    }
}
