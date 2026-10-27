package com.quare.blitzsplit.feature.profilesetup.presentation.mapper

import com.quare.blitzsplit.core.model.ProfilePhoto
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfilePhotoImageUrlTest {
    @Test
    fun `GIVEN a Google or uploaded photo WHEN building its image url THEN is the url itself`() {
        // Given
        val photo = ProfilePhoto.Remote("https://lh3.googleusercontent.com/a/photo")

        // When
        val url = photo.toImageUrl()

        // Then
        assertEquals("https://lh3.googleusercontent.com/a/photo", url)
    }

    @Test
    fun `GIVEN a photo picked on this device WHEN building its image url THEN is a JPEG data uri of its bytes`() {
        // Given
        val photo = ProfilePhoto.Pending(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xFE.toByte()))

        // When
        val url = photo.toImageUrl()

        // Then
        assertEquals("data:image/jpeg;base64,/9j//g==", url)
    }
}
