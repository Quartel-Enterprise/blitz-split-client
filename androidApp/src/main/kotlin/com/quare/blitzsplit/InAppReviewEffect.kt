package com.quare.blitzsplit

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.quare.blitzsplit.core.support.launchStoreReview
import com.quare.blitzsplit.shared.app.AppViewModel

// Why: the store's review card, asked for when the shared code says the moment is right (RequestReviewIfNeeded).
@Composable
internal fun InAppReviewEffect(
    appViewModel: AppViewModel,
    activity: Activity,
) {
    LaunchedEffect(appViewModel, activity) {
        appViewModel.reviewRequests.collect { activity.launchStoreReview() }
    }
}
