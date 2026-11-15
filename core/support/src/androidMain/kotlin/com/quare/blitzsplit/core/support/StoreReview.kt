package com.quare.blitzsplit.core.support

import android.app.Activity
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import com.quare.blitzsplit.core.utils.suspendRunCatching

/*
 * Why: Google Play's in-app review card over this activity. Google Play decides whether it shows and never says
 * which, and a device without the Play Store only fails the request, which changes nothing for the app.
 */
suspend fun Activity.launchStoreReview() {
    val manager = ReviewManagerFactory.create(this)
    suspendRunCatching { manager.launchReview(this, manager.requestReview()) }
}
