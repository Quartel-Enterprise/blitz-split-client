package com.quare.blitzsplit.core.watch.domain.usecase

fun interface EndWatchSessionFromPhone {
    /*
     * Why: the phone signed out of userId, so the watch signs out and erases its copy when it is that user's, once what
     * it queued went up; true when it will. Another account's sign out leaves the watch as it is.
     */
    suspend operator fun invoke(userId: String): Boolean
}
