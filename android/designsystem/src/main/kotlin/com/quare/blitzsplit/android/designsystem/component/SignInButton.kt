package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.R

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    isEnabled: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.designsystem_continue_with_google),
) {
    OutlinedButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        SignInButtonLabelRow(
            label = label,
            isLoading = isLoading,
            leading = "G",
        )
    }
}

@Composable
fun AppleSignInButton(
    onClick: () -> Unit,
    isEnabled: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.designsystem_continue_with_apple),
) {
    val isDark = MaterialTheme.colorScheme.surface.isDarkColor()
    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDark) Color.White else Color.Black,
            contentColor = if (isDark) Color.Black else Color.White,
        ),
    ) {
        SignInButtonLabelRow(
            label = label,
            isLoading = isLoading,
            leading = null,
        )
    }
}

@Composable
private fun SignInButtonLabelRow(
    label: String,
    isLoading: Boolean,
    leading: String?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        } else if (leading != null) {
            Text(
                text = leading,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

private fun Color.isDarkColor(): Boolean = (red * 0.299f + green * 0.587f + blue * 0.114f) < 0.5f
