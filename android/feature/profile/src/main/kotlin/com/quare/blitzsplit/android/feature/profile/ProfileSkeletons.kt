package com.quare.blitzsplit.android.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.ShimmerAvatarSkeleton
import com.quare.blitzsplit.android.designsystem.component.ShimmerBox
import com.quare.blitzsplit.android.designsystem.component.ShimmerCardSkeleton
import com.quare.blitzsplit.android.designsystem.component.ShimmerLine
import com.quare.blitzsplit.android.designsystem.component.ShimmerRowSkeleton

private const val KEY_SKELETON_ROWS = 2

/*
 * Why: who you are and your account until the profile and the account were read: never no name, nor "Sem conta" with
 * Conectar before the account is known.
 */
@Composable
internal fun ProfileIdentitySkeleton() {
    ShimmerBox(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ShimmerAvatarSkeleton(size = 96.dp)
            ShimmerLine(
                width = 180.dp,
                height = 22.dp,
            )
            ShimmerLine(
                width = 96.dp,
                height = 36.dp,
            )
            ShimmerCardSkeleton(
                height = 152.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

// Why: the keys' rows until they were read, never "no key" with only "Adicionar chave" before them.
@Composable
internal fun PaymentKeysSkeleton() {
    ShimmerBox(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(KEY_SKELETON_ROWS) {
                ShimmerRowSkeleton(
                    leadingShape = MaterialTheme.shapes.medium,
                    trailingWidth = null,
                    containerShape = MaterialTheme.shapes.small,
                )
            }
        }
    }
}

// Why: the theme's three segments until the stored one was read, never the default picked first.
@Composable
internal fun ThemePickerSkeleton(modifier: Modifier = Modifier) {
    ShimmerBox(modifier = modifier) {
        ShimmerLine(height = 40.dp)
    }
}

// Why: a short value of a row ("3 ativas") while it is read.
@Composable
internal fun ProfileValueSkeleton() {
    ShimmerBox {
        ShimmerLine(
            width = 64.dp,
            height = 12.dp,
        )
    }
}

// Why: "Sair" or "Apagar perfil e sair" depend on the account, so the button shimmers until it was read.
@Composable
internal fun SignOutSkeleton() {
    ShimmerBox(modifier = Modifier.fillMaxWidth()) {
        ShimmerLine(height = 48.dp)
    }
}
