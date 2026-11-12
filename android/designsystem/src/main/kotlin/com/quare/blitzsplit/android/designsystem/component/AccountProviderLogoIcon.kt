package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.core.model.AccountProvider

/*
 * Why: the sign in provider's own logo, as the sign in buttons, the photo viewer and Profile's account row draw it.
 * Google's "G" keeps its four colors, so it isn't tinted and only fades as the content around it does; Apple's takes
 * a tint, the content's color unless the place asks for another (black on the photo viewer's white).
 */
@Composable
fun AccountProviderLogoIcon(
    provider: AccountProvider,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    appleTint: Color = LocalContentColor.current,
) {
    when (provider) {
        AccountProvider.GOOGLE -> Image(
            painter = painterResource(R.drawable.ic_google_logo),
            contentDescription = contentDescription,
            modifier = modifier,
            alpha = LocalContentColor.current.alpha,
        )

        AccountProvider.APPLE -> Icon(
            painter = painterResource(R.drawable.ic_apple_logo),
            contentDescription = contentDescription,
            modifier = modifier,
            tint = appleTint,
        )
    }
}
