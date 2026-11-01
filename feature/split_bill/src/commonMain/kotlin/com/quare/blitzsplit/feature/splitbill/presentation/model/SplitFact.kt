package com.quare.blitzsplit.feature.splitbill.presentation.model

/*
 * Why: one thing a split says about itself, in Dividir's status and under Rachar conta's "Divisão". The shared code
 * decides which facts each state shows, so the three UIs agree, and each one only words them.
 */
sealed interface SplitFact {
    // Why: Igual's status and summary, "R$ 62,40 cada".
    data class Each(
        val amountMinor: Long,
    ) : SplitFact

    // Why: "Ninguém participa ainda", the status of an Igual nobody is checked in or of Partes with no part.
    data object Nobody : SplitFact

    // Why: "Marque pelo menos uma pessoa para dividir R$ 312,00."
    data class NobodyChecked(
        val totalMinor: Long,
    ) : SplitFact

    // Why: "Dê pelo menos uma parte a alguém para dividir R$ 312,00."
    data class NobodyHasParts(
        val totalMinor: Long,
    ) : SplitFact

    // Why: "R$ 312,00 entre 5 pessoas."
    data class Among(
        val totalMinor: Long,
        val count: Int,
    ) : SplitFact

    // Why: "Diego não participa", "Diego e Elisa não participam" or "Você não participa".
    data class Out(
        val people: List<BillPerson>,
    ) : SplitFact

    // Why: Valores, "Faltam R$ 84,00".
    data class AmountLeft(
        val amountMinor: Long,
    ) : SplitFact

    // Why: Valores, "Passou R$ 12,00".
    data class AmountOver(
        val amountMinor: Long,
    ) : SplitFact

    // Why: Valores' status once the parts add up, "Fechou: R$ 642,00".
    data class AmountAddsUp(
        val totalMinor: Long,
    ) : SplitFact

    // Why: "A soma das partes é igual ao total."
    data object PartsMatchTotal : SplitFact

    // Why: "Já foram R$ 558,00 de R$ 642,00."
    data class AmountSoFar(
        val assignedMinor: Long,
        val totalMinor: Long,
    ) : SplitFact

    // Why: "Elisa está sem valor", the rows left blank or at 0 while the amounts don't add up.
    data class WithoutValue(
        val people: List<BillPerson>,
    ) : SplitFact

    // Why: %, "Faltam 15%".
    data class PercentLeft(
        val percent: Long,
    ) : SplitFact

    // Why: %, "Passou 15%".
    data class PercentOver(
        val percent: Long,
    ) : SplitFact

    // Why: %'s status once it adds up, "Fechou: 100%".
    data object PercentAddsUp : SplitFact

    // Why: Partes, "6 partes de R$ 52,00".
    data class Parts(
        val count: Long,
        val partMinor: Long,
    ) : SplitFact

    // Why: Partes, "Ana tem 2 partes", for whoever has more than one.
    data class PersonParts(
        val person: BillPerson,
        val parts: Long,
    ) : SplitFact

    // Why: Itens, under Rachar conta's "Divisão", "6 itens".
    data class Items(
        val count: Int,
    ) : SplitFact

    // Why: Itens, "2 itens sem ninguém", once the only people in some items left the group.
    data class LooseItems(
        val count: Int,
    ) : SplitFact

    // Why: Rachar conta's "Divisão" opened with the group's "Divisão padrão" and still follows it, "Padrão do grupo".
    data object GroupDefault : SplitFact
}
