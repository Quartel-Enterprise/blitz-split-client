package com.quare.blitzsplit.android.feature.deleteaccount

import androidx.annotation.StringRes
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.android.designsystem.R as DesignR

@get:StringRes
internal val AccountProvider.labelRes: Int
    get() = when (this) {
        AccountProvider.GOOGLE -> DesignR.string.designsystem_google_short
        AccountProvider.APPLE -> DesignR.string.designsystem_apple_short
    }
