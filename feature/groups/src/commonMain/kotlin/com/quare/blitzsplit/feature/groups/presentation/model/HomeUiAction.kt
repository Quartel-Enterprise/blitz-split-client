package com.quare.blitzsplit.feature.groups.presentation.model

sealed interface HomeUiAction {
    // Why: the device couldn't delete the payment "Desfazer" undoes; it still counts.
    data object ShowChangeError : HomeUiAction

    // Why: the card's "Turn on" while the system can still ask; each platform asks its own way and reads it again.
    data object RequestNotificationPermission : HomeUiAction

    /*
     * Why: Apple only, once there is a group: delivery to Notification Center with no prompt (provisional), answered with
     * OnQuietNotificationsAnswered.
     */
    data object RequestQuietNotifications : HomeUiAction

    // Why: the app's notification settings on Android, iOS and macOS, where only they can allow them now.
    data object OpenNotificationSettings : HomeUiAction

    // Why: "Notifications on", once the system allows them after not allowing them, from its prompt or its settings.
    data object ShowNotificationsOn : HomeUiAction

    // Why: "We saw you on your phone." (14.1c), once: the account signed in on a phone app while the card showed.
    data object ShowUsedOnPhone : HomeUiAction
}
