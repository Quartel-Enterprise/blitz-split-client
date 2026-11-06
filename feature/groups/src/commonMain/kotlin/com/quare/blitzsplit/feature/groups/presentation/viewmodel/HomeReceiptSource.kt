package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.usecase.HoldPickedReceipt
import com.quare.blitzsplit.core.model.ReceiptPhotoSource
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/*
 * Why: Início's "Ler cupom fiscal" (beta feedback, part 3, item 16), apart from the rest of Home: it asks where the
 * photo comes from right here, as Rachar conta's own does, and the photo opens Rachar conta reading it.
 */
internal class HomeReceiptSource(
    private val uiState: MutableStateFlow<HomeUiState>,
    private val navigator: Navigator,
    private val holdPickedReceipt: HoldPickedReceipt,
    private val trackEvent: TrackEvent,
    // Why: the group "Rachar conta" marks, the most recently used one; null with no group yet.
    private val recentGroupId: () -> String?,
) {
    fun onEvent(event: HomeUiEvent.Receipt) {
        when (event) {
            HomeUiEvent.OnScanReceiptClicked -> ask()

            is HomeUiEvent.OnReceiptSourcePicked -> {
                if (uiState.value.showsReceiptSourceChoice) {
                    trackEvent("receipt_source_picked", mapOf("source" to event.source))
                }
                open(event.source)
            }

            is HomeUiEvent.OnReceiptChoiceSkipped -> open(event.source)

            HomeUiEvent.OnReceiptPickerDismissed -> close()

            is HomeUiEvent.OnReceiptPhotoPicked -> read(event.photo)

            // Why: the camera is each platform's own; its taps are only tracked.
            is HomeUiEvent.OnReceiptTorchClicked,
            HomeUiEvent.OnReceiptCameraAllowClicked,
            HomeUiEvent.OnReceiptCameraSettingsClicked,
            -> Unit
        }
    }

    // Why: online and once Home is read; with no group yet a late tap makes one first, as "Rachar conta" does.
    private fun ask() {
        val state = uiState.value
        if (!state.canScanReceipt || !state.isLoaded || !isOnTop()) return
        trackEvent("scan_receipt_clicked", mapOf("source" to SOURCE))
        if (recentGroupId() == null) {
            navigator.navigate(Route.CreateGroup)
        } else {
            uiState.update {
                it.copy(
                    showsReceiptSourceChoice = true,
                    receiptSource = null,
                )
            }
        }
    }

    // Why: only from the choice still open, so a late tap never opens a picker Home no longer asks for.
    private fun open(source: ReceiptPhotoSource) {
        uiState.update {
            if (it.showsReceiptSourceChoice) {
                it.copy(
                    showsReceiptSourceChoice = false,
                    receiptSource = source,
                )
            } else {
                it
            }
        }
    }

    private fun close() {
        uiState.update {
            it.copy(
                showsReceiptSourceChoice = false,
                receiptSource = null,
            )
        }
    }

    /*
     * Why: the photo, which the gallery and the file chooser hand back after their request ended, opens Rachar conta
     * reading it, asking the group first as "Rachar conta" does, so it is read in the group picked's currency. A photo
     * arriving once Home is covered, or with no group left, is dropped.
     */
    private fun read(photo: ByteArray) {
        close()
        val groupId = recentGroupId()
        if (!uiState.value.isLoaded || groupId == null || !isOnTop()) return
        trackEvent("photo_picked", mapOf("source" to SOURCE))
        holdPickedReceipt(photo)
        navigator.navigate(Route.SplitBill.createPickingGroup(groupId, scansReceipt = true))
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == Route.Home

    private companion object {
        const val SOURCE = "home"
    }
}
