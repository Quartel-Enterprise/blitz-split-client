package com.quare.blitzsplit.android.platform

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

/*
 * Why: Google Play's in-app review card over this activity. Google Play decides whether it shows and never says
 * which, and a device without the Play Store only fails the request, which changes nothing for the app.
 */
fun Activity.launchStoreReview() {
    val manager = ReviewManagerFactory.create(this)
    manager.requestReviewFlow().addOnCompleteListener { request ->
        if (request.isSuccessful && !isFinishing && !isDestroyed) manager.launchReviewFlow(this, request.result)
    }
}
