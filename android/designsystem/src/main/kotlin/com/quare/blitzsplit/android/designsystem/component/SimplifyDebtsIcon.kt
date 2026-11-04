package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.quare.blitzsplit.android.designsystem.R

/*
 * Why: "Simplificar dívidas" (page 13) draws the boards' three people joined wherever it shows: Editar grupo's row, its
 * confirmation, Saldos' card and new pairs, Registrar pagamento's note, "Por grupo" and Atividade's badge.
 */
@Composable
fun simplifyDebtsIcon(): ImageVector = ImageVector.vectorResource(R.drawable.ic_simplify_debts)
