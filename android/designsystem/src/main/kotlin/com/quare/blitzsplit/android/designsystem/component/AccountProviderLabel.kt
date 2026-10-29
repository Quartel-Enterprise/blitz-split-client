package com.quare.blitzsplit.android.designsystem.component

import androidx.annotation.StringRes
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.core.model.AccountProvider

@get:StringRes
val AccountProvider.labelRes: Int
    get() = when (this) {
        AccountProvider.GOOGLE -> R.string.designsystem_google_short
        AccountProvider.APPLE -> R.string.designsystem_apple_short
    }
