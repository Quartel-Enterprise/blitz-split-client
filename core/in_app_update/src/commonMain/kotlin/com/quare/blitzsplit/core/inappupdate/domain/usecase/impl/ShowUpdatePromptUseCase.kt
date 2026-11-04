package com.quare.blitzsplit.core.inappupdate.domain.usecase.impl

import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateAvailability
import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateMechanism
import com.quare.blitzsplit.core.inappupdate.domain.usecase.CompleteUpdateInstall
import com.quare.blitzsplit.core.inappupdate.domain.usecase.ShowUpdatePrompt
import com.quare.blitzsplit.core.inappupdate.domain.usecase.StartUpdate
import com.quare.blitzsplit.core.model.UpdatePromptSource
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateLastUpdatePromptAt
import com.quare.blitzsplit.core.utils.CurrentTimeMillis

/*
 * Why: Android hands the update to Google Play's own flexible flow, which asks and downloads while the app stays in
 * use; iOS and macOS open "Atualização disponível" over the app, which sends to the App Store.
 */
class ShowUpdatePromptUseCase(
    private val mechanism: UpdateMechanism,
    private val startUpdate: StartUpdate,
    private val completeUpdateInstall: CompleteUpdateInstall,
    private val navigator: Navigator,
    private val updateLastUpdatePromptAt: UpdateLastUpdatePromptAt,
    private val currentTimeMillis: CurrentTimeMillis,
) : ShowUpdatePrompt {
    override suspend fun invoke(
        availability: UpdateAvailability.Pending,
        source: UpdatePromptSource,
    ) {
        updateLastUpdatePromptAt(currentTimeMillis())
        when (availability) {
            is UpdateAvailability.Available -> showAvailable(
                availability = availability,
                source = source,
            )

            UpdateAvailability.Downloaded -> completeUpdateInstall()
        }
    }

    private suspend fun showAvailable(
        availability: UpdateAvailability.Available,
        source: UpdatePromptSource,
    ) {
        when (mechanism) {
            UpdateMechanism.GOOGLE_PLAY -> startUpdate()

            UpdateMechanism.APP_STORE -> {
                val storeUrl = availability.storeUrl ?: return
                // Why: a manual check while the sheet is already up leaves it as it is, never one sheet over another.
                if (navigator.backStack.value.lastOrNull() is Route.InAppUpdate) return
                navigator.navigate(
                    Route.InAppUpdate(
                        versionName = availability.versionName,
                        storeUrl = storeUrl,
                        source = source,
                    ),
                )
            }

            UpdateMechanism.NONE -> Unit
        }
    }
}
