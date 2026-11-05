@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.DebtSimplificationSummary
import com.quare.blitzsplit.core.expenses.domain.model.SimplifyDebtsConflictNotice
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.navigateBackFrom
import org.koin.mp.KoinPlatform

// Why: "5 pagamentos em vez de 7" (13.1, 13.4), while the group's "Simplificar dívidas" is on.
class JsDebtSimplification(
    val paymentCount: Int,
    val unsimplifiedPaymentCount: Int,
    // Why: "Já está no mínimo de pagamentos" instead.
    val isAlreadyMinimal: Boolean,
)

/*
 * Why: "Ana desligou depois de você. Vale a última mudança…" (13.1e); a null name is "Alguém", and simplifiesDebts how
 * it stands now.
 */
class JsSimplifyConflict(
    val changedByName: String?,
    val simplifiesDebts: Boolean,
)

// Why: someone a sentence of "Simplificar dívidas" names, "você" for isMe and "Ex-membro" with no name.
class JsSimplifyName(
    val name: String?,
    val isMe: Boolean,
)

internal fun toJsDebtSimplification(summary: DebtSimplificationSummary): JsDebtSimplification = JsDebtSimplification(
    paymentCount = summary.paymentCount,
    unsimplifiedPaymentCount = summary.unsimplifiedPaymentCount,
    isAlreadyMinimal = summary.isAlreadyMinimal,
)

internal fun toJsSimplifyConflict(notice: SimplifyDebtsConflictNotice): JsSimplifyConflict = JsSimplifyConflict(
    changedByName = notice.changedByName,
    simplifiesDebts = notice.simplifiesDebts,
)

/*
 * Why: "Como funciona" (13.2) has no ViewModel, only the same words for everyone with the example's amounts in the
 * group's currency; closing it goes back to whichever opened it (Editar grupo or the group's Saldos).
 */
class SimplifyDebtsInfoPresenter(
    route: JsRoute,
) {
    private val infoRoute = route.route as Route.SimplifyDebtsInfo

    // Why: the ISO 4217 code ("BRL") and its digits after the decimal separator, which the example's amounts take.
    val currency: String = infoRoute.currency.code
    val fractionDigits: Int = infoRoute.currency.fractionDigits

    fun close() {
        KoinPlatform.getKoin().get<Navigator>().navigateBackFrom(infoRoute)
    }
}
