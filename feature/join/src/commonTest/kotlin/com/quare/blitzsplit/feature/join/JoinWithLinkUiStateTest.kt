package com.quare.blitzsplit.feature.join

import com.quare.blitzsplit.feature.join.presentation.model.CameraAccess
import com.quare.blitzsplit.feature.join.presentation.model.CameraStage
import com.quare.blitzsplit.feature.join.presentation.model.JoinWithLinkUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JoinWithLinkUiStateTest {
    @Test
    fun `GIVEN open camera not tapped WHEN drawing the camera area THEN shows the prompt`() {
        // Given
        val state = state(cameraAccess = CameraAccess.NOT_REQUESTED)

        // When
        val stage = state.cameraStage

        // Then
        assertEquals(CameraStage.PROMPT, stage)
    }

    @Test
    fun `GIVEN the system asking for the camera WHEN drawing the camera area THEN shows the viewfinder`() {
        // Given
        val state = state(cameraAccess = CameraAccess.ASKING)

        // When
        val stage = state.cameraStage

        // Then
        assertEquals(CameraStage.VIEWFINDER, stage)
    }

    @Test
    fun `GIVEN the camera allowed WHEN drawing the camera area THEN shows the viewfinder`() {
        // Given
        val state = state(cameraAccess = CameraAccess.GRANTED)

        // When
        val stage = state.cameraStage

        // Then
        assertEquals(CameraStage.VIEWFINDER, stage)
    }

    @Test
    fun `GIVEN the camera denied WHEN drawing the camera area THEN explains the camera is denied`() {
        // Given
        val state = state(cameraAccess = CameraAccess.DENIED)

        // When
        val stage = state.cameraStage

        // Then
        assertEquals(CameraStage.DENIED, stage)
    }

    @Test
    fun `GIVEN the camera allowed but it will not open WHEN drawing the camera area THEN says the camera failed`() {
        // Given
        val state = state(
            cameraAccess = CameraAccess.GRANTED,
            isCameraUnavailable = true,
        )

        // When
        val stage = state.cameraStage

        // Then
        assertEquals(CameraStage.FAILED, stage)
    }

    @Test
    fun `GIVEN the system asking and the camera will not open WHEN drawing the camera area THEN says it failed`() {
        // Given
        val state = state(
            cameraAccess = CameraAccess.ASKING,
            isCameraUnavailable = true,
        )

        // When
        val stage = state.cameraStage

        // Then
        assertEquals(CameraStage.FAILED, stage)
    }

    @Test
    fun `GIVEN the camera denied and unavailable WHEN drawing the camera area THEN explains it is denied`() {
        // Given
        val state = state(
            cameraAccess = CameraAccess.DENIED,
            isCameraUnavailable = true,
        )

        // When
        val stage = state.cameraStage

        // Then
        assertEquals(CameraStage.DENIED, stage)
    }

    @Test
    fun `GIVEN open camera not tapped and the camera unavailable WHEN drawing the camera area THEN shows the prompt`() {
        // Given
        val state = state(
            cameraAccess = CameraAccess.NOT_REQUESTED,
            isCameraUnavailable = true,
        )

        // When
        val stage = state.cameraStage

        // Then
        assertEquals(CameraStage.PROMPT, stage)
    }

    @Test
    fun `GIVEN no camera on the device WHEN drawing the camera area THEN shows no camera instead of the prompt`() {
        // Given
        val state = state(
            cameraAccess = CameraAccess.NOT_REQUESTED,
            hasCamera = false,
        )

        // When
        val stage = state.cameraStage

        // Then
        assertEquals(CameraStage.NO_CAMERA, stage)
    }

    @Test
    fun `GIVEN no camera on the device and the access denied WHEN drawing the camera area THEN shows no camera`() {
        // Given
        val state = state(
            cameraAccess = CameraAccess.DENIED,
            hasCamera = false,
        )

        // When
        val stage = state.cameraStage

        // Then
        assertEquals(CameraStage.NO_CAMERA, stage)
    }

    @Test
    fun `GIVEN no camera on the device and the camera unavailable WHEN drawing the camera area THEN shows no camera`() {
        // Given
        val state = state(
            cameraAccess = CameraAccess.GRANTED,
            hasCamera = false,
            isCameraUnavailable = true,
        )

        // When
        val stage = state.cameraStage

        // Then
        assertEquals(CameraStage.NO_CAMERA, stage)
    }

    @Test
    fun `GIVEN open camera not tapped WHEN checking whether the camera runs THEN it does not`() {
        // Given
        val state = state(cameraAccess = CameraAccess.NOT_REQUESTED)

        // When
        val isRequested = state.isCameraRequested

        // Then
        assertFalse(isRequested)
    }

    @Test
    fun `GIVEN the system asking for the camera WHEN checking whether the camera runs THEN it does`() {
        // Given
        val state = state(cameraAccess = CameraAccess.ASKING)

        // When
        val isRequested = state.isCameraRequested

        // Then
        assertTrue(isRequested)
    }

    @Test
    fun `GIVEN the camera allowed WHEN checking whether the camera runs THEN it does`() {
        // Given
        val state = state(cameraAccess = CameraAccess.GRANTED)

        // When
        val isRequested = state.isCameraRequested

        // Then
        assertTrue(isRequested)
    }

    @Test
    fun `GIVEN the camera denied after open camera WHEN checking whether the camera was requested THEN it was`() {
        // Given
        val state = state(cameraAccess = CameraAccess.DENIED)

        // When
        val isRequested = state.isCameraRequested

        // Then
        assertTrue(isRequested)
    }

    @Test
    fun `GIVEN no camera on the device but access allowed WHEN checking whether the camera runs THEN it does not`() {
        // Given
        val state = state(
            cameraAccess = CameraAccess.GRANTED,
            hasCamera = false,
        )

        // When
        val isRequested = state.isCameraRequested

        // Then
        assertFalse(isRequested)
    }

    private fun state(
        cameraAccess: CameraAccess,
        hasCamera: Boolean = true,
        isCameraUnavailable: Boolean = false,
    ): JoinWithLinkUiState = JoinWithLinkUiState(
        link = "",
        isLinkInvalid = false,
        isQrInvalid = false,
        cameraAccess = cameraAccess,
        hasCamera = hasCamera,
        isCameraUnavailable = isCameraUnavailable,
        hasTorch = false,
        isTorchOn = false,
    )
}
