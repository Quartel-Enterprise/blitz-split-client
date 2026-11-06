package com.quare.blitzsplit.android.platform

// Why: the camera's taps the shared code counts, which each screen sends as its own events.
class ReceiptCameraTaps(
    // Why: the flash's tap, with where it ended up.
    val onTorchClicked: (isOn: Boolean) -> Unit,
    // Why: "Permitir câmera" after a first refusal.
    val onAllowClicked: () -> Unit,
    // Why: "Abrir configurações" once the camera was refused for good.
    val onSettingsClicked: () -> Unit,
)
