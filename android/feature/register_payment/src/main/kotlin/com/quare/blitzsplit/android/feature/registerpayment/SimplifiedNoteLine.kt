package com.quare.blitzsplit.android.feature.registerpayment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.simplifyDebtsIcon
import com.quare.blitzsplit.android.designsystem.format.nameListOf
import com.quare.blitzsplit.feature.registerpayment.presentation.model.NotePerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiState
import com.quare.blitzsplit.feature.registerpayment.presentation.model.SimplifiedPairNote

/*
 * Why: why the pair is on the form when "Simplificar dívidas" made or grew it (13.5, 13.5b), in the form and in
 * "Pagar Ana": "Dívida simplificada: Diego paga a você o que devia a Bruno."
 */
@Composable
internal fun SimplifiedNoteLine(
    state: RegisterPaymentUiState,
    modifier: Modifier = Modifier,
) {
    val note = state.simplifiedNote ?: return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = simplifyDebtsIcon(),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = simplifiedNoteTextOf(state, note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun simplifiedNoteTextOf(
    state: RegisterPaymentUiState,
    note: SimplifiedPairNote,
): String {
    if (note.insteadOf.isEmpty()) return stringResource(R.string.register_payment_simplified_none)
    val insteadOf = nameListOf(note.insteadOf.map { notePersonNameOf(it) })
    val payerIsMe = state.from?.isMe == true
    return when {
        !note.isNewPair && payerIsMe -> stringResource(R.string.register_payment_simplified_grown_you, insteadOf)

        !note.isNewPair -> stringResource(R.string.register_payment_simplified_grown, nameOf(state.from), insteadOf)

        payerIsMe -> stringResource(R.string.register_payment_simplified_new_you_pay, nameOf(state.to), insteadOf)

        state.to?.isMe == true -> stringResource(
            R.string.register_payment_simplified_new_pays_you,
            nameOf(state.from),
            insteadOf,
        )

        else -> stringResource(
            R.string.register_payment_simplified_new,
            nameOf(state.from),
            nameOf(state.to),
            insteadOf,
        )
    }
}

@Composable
private fun notePersonNameOf(person: NotePerson): String = if (person.isMe) {
    stringResource(R.string.register_payment_you_owed)
} else {
    person.name ?: stringResource(R.string.register_payment_former_member)
}
