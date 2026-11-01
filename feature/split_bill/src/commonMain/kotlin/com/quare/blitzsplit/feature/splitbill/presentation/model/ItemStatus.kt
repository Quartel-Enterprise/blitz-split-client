package com.quare.blitzsplit.feature.splitbill.presentation.model

// Why: whether an item's split closes (7.6), the card under its modes; each UI words it.
sealed interface ItemStatus {
    // Why: a status the item editor draws in red, as Pronto waits until it is fixed.
    val isError: Boolean
        get() = false

    // Why: "Ninguém marcado" (7.6g): "Marque quem consumiu este item. Pronto libera quando ele tiver dono."
    data object Nobody : ItemStatus {
        override val isError: Boolean
            get() = true
    }

    // Why: "6 de 6 unidades": "Cada unidade custa R$ 14,00."
    data class UnitsAddUp(
        val quantity: Long,
        val unitPriceMinor: Long,
    ) : ItemStatus

    // Why: "Falta 1 unidade" (7.6f): "5 de 6 marcadas. Diga quem ficou com a outra."
    data class UnitsLeft(
        val marked: Long,
        val quantity: Long,
    ) : ItemStatus {
        override val isError: Boolean
            get() = true

        val missing: Long
            get() = quantity - marked
    }

    // Why: "Passou 1 unidade": "7 de 6 marcadas."
    data class UnitsOver(
        val marked: Long,
        val quantity: Long,
    ) : ItemStatus {
        override val isError: Boolean
            get() = true

        val over: Long
            get() = marked - quantity
    }

    // Why: "R$ 44,50 para cada um": "R$ 178,00 entre 4 pessoas."
    data class Each(
        val eachMinor: Long,
        val totalMinor: Long,
        val count: Int,
    ) : ItemStatus

    // Why: "4 partes de R$ 42,00": "Bruno ficou com 2 partes.", or who it splits among when nobody has more than one.
    data class Parts(
        val parts: Long,
        val partMinor: Long,
        val severalParts: List<ItemPersonParts>,
        val totalMinor: Long,
        val count: Int,
    ) : ItemStatus

    // Why: "Fechou: R$ 60,00": "A soma das partes é igual ao item."
    data class AmountAddsUp(
        val totalMinor: Long,
    ) : ItemStatus

    // Why: "Faltam R$ 10,00": "Já foram R$ 50,00 de R$ 60,00."
    data class AmountLeft(
        val amountMinor: Long,
        val assignedMinor: Long,
        val totalMinor: Long,
    ) : ItemStatus {
        override val isError: Boolean
            get() = true
    }

    // Why: "Passou R$ 10,00": "Já foram R$ 70,00 de R$ 60,00."
    data class AmountOver(
        val amountMinor: Long,
        val assignedMinor: Long,
        val totalMinor: Long,
    ) : ItemStatus {
        override val isError: Boolean
            get() = true
    }

    // Why: "Fechou: 100%": "R$ 60,00 entre 2 pessoas."
    data class PercentAddsUp(
        val totalMinor: Long,
        val count: Int,
    ) : ItemStatus

    // Why: "Faltam 10%": "Já foram 90% de 100%."
    data class PercentLeft(
        val percent: Long,
        val sum: Long,
    ) : ItemStatus {
        override val isError: Boolean
            get() = true
    }

    // Why: "Passou 10%": "Já foram 110% de 100%."
    data class PercentOver(
        val percent: Long,
        val sum: Long,
    ) : ItemStatus {
        override val isError: Boolean
            get() = true
    }
}
