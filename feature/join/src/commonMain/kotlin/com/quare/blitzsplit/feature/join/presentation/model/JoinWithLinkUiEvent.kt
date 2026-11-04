package com.quare.blitzsplit.feature.join.presentation.model

import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "join_with_link"

sealed interface JoinWithLinkUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: the platform's camera answering, not a tap.
            is OnCameraAccessChanged, is OnCameraOpened, OnCameraUnavailable -> EventAnalytics.NotTracked

            OnOpenSettingsClicked -> EventAnalytics.Automatic("camera_settings_clicked", mapOf("source" to SOURCE))

            OnTorchClicked -> EventAnalytics.Automatic("join_with_link_torch_clicked")

            // Why: what the camera read, never its content; the invite it opens is a screen view.
            is OnQrScanned -> EventAnalytics.NotTracked

            is OnLinkChanged -> EventAnalytics.NotTracked

            is OnPasted -> EventAnalytics.Automatic("join_with_link_paste_clicked")

            OnClearClicked -> EventAnalytics.Automatic("join_with_link_clear_clicked")

            OnOpenClicked -> EventAnalytics.Automatic("join_with_link_open_clicked")

            OnCloseClicked -> EventAnalytics.NotTracked
        }

    // Why: the platform's answer to the camera permission, asked when the reader opens and again on coming back.
    data class OnCameraAccessChanged(
        val isGranted: Boolean,
    ) : JoinWithLinkUiEvent

    // Why: the platform's camera opened (again, after another app let it go), with or without a flashlight.
    data class OnCameraOpened(
        val hasTorch: Boolean,
    ) : JoinWithLinkUiEvent

    // Why: no camera, or one that won't open; only the paste is left until it opens.
    data object OnCameraUnavailable : JoinWithLinkUiEvent

    // Why: "Abrir configurações" ("Abrir Ajustes" on the iPhone), with the camera denied.
    data object OnOpenSettingsClicked : JoinWithLinkUiEvent

    data object OnTorchClicked : JoinWithLinkUiEvent

    data class OnQrScanned(
        val content: String,
    ) : JoinWithLinkUiEvent

    data class OnLinkChanged(
        val link: String,
    ) : JoinWithLinkUiEvent

    // Why: "Colar", with what the clipboard had: an invite opens straight away.
    data class OnPasted(
        val text: String,
    ) : JoinWithLinkUiEvent

    data object OnClearClicked : JoinWithLinkUiEvent

    // Why: "Abrir convite", for a link typed by hand.
    data object OnOpenClicked : JoinWithLinkUiEvent

    data object OnCloseClicked : JoinWithLinkUiEvent
}
