package com.quare.blitzsplit.core.presentation.analytics

import com.quare.blitzsplit.core.model.PhotoCropEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class PhotoCropAnalyticsTest {
    private val source = "profile_setup"

    @Test
    fun `GIVEN a drag WHEN mapping it THEN it is not tracked`() {
        // Given
        val event = PhotoCropEvent.OnDragged(
            dx = 0.25f,
            dy = -0.5f,
        )

        // When
        val analytics = event.toEventAnalytics(source)

        // Then
        assertEquals(EventAnalytics.NotTracked, analytics)
    }

    @Test
    fun `GIVEN a pinch WHEN mapping it THEN it is not tracked`() {
        // Given
        val event = PhotoCropEvent.OnPinched(
            zoomChange = 1.2f,
            focusX = 0.1f,
            focusY = -0.1f,
        )

        // When
        val analytics = event.toEventAnalytics(source)

        // Then
        assertEquals(EventAnalytics.NotTracked, analytics)
    }

    @Test
    fun `GIVEN the zoom slider moved WHEN mapping it THEN it is not tracked`() {
        // Given
        val event = PhotoCropEvent.OnZoomChanged(zoom = 2.5f)

        // When
        val analytics = event.toEventAnalytics(source)

        // Then
        assertEquals(EventAnalytics.NotTracked, analytics)
    }

    @Test
    fun `GIVEN Girar tapped WHEN mapping it THEN sends photo_crop_turned with only the source`() {
        // Given
        val event = PhotoCropEvent.OnTurnClicked

        // When
        val analytics = event.toEventAnalytics(source)

        // Then
        assertEquals(EventAnalytics.Automatic("photo_crop_turned", mapOf("source" to "profile_setup")), analytics)
    }

    @Test
    fun `GIVEN Horizontal tapped WHEN mapping it THEN sends photo_crop_mirrored on the horizontal axis`() {
        // Given
        val event = PhotoCropEvent.OnMirrorHorizontallyClicked

        // When
        val analytics = event.toEventAnalytics(source)

        // Then
        assertEquals(
            EventAnalytics.Automatic(
                "photo_crop_mirrored",
                mapOf("source" to "profile_setup", "axis" to "horizontal"),
            ),
            analytics,
        )
    }

    @Test
    fun `GIVEN Vertical tapped WHEN mapping it THEN sends photo_crop_mirrored on the vertical axis`() {
        // Given
        val event = PhotoCropEvent.OnMirrorVerticallyClicked

        // When
        val analytics = event.toEventAnalytics(source)

        // Then
        assertEquals(
            EventAnalytics.Automatic(
                "photo_crop_mirrored",
                mapOf("source" to "profile_setup", "axis" to "vertical"),
            ),
            analytics,
        )
    }

    @Test
    fun `GIVEN Pronto tapped WHEN mapping it THEN sends photo_crop_confirmed with only the source`() {
        // Given
        val event = PhotoCropEvent.OnConfirmClicked

        // When
        val analytics = event.toEventAnalytics(source)

        // Then
        assertEquals(EventAnalytics.Automatic("photo_crop_confirmed", mapOf("source" to "profile_setup")), analytics)
    }

    @Test
    fun `GIVEN the editor dismissed WHEN mapping it THEN sends photo_crop_dismissed with only the source`() {
        // Given
        val event = PhotoCropEvent.OnDismissed

        // When
        val analytics = event.toEventAnalytics(source)

        // Then
        assertEquals(EventAnalytics.Automatic("photo_crop_dismissed", mapOf("source" to "profile_setup")), analytics)
    }

    @Test
    fun `GIVEN another screen as the source WHEN a tap is mapped THEN its source is the one passed in`() {
        // Given
        val otherSource = "edit_group"
        val event = PhotoCropEvent.OnMirrorVerticallyClicked

        // When
        val analytics = event.toEventAnalytics(otherSource)

        // Then
        assertEquals(
            EventAnalytics.Automatic("photo_crop_mirrored", mapOf("source" to "edit_group", "axis" to "vertical")),
            analytics,
        )
    }
}
