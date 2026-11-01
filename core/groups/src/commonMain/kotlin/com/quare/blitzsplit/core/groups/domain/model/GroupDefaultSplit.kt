package com.quare.blitzsplit.core.groups.domain.model

import com.quare.blitzsplit.core.model.DefaultSplitMode

/*
 * Why: how a group's new bills split (6.7), which Rachar conta opens with. A group without one (null on the group)
 * splits equally among everyone.
 */
data class GroupDefaultSplit(
    val mode: DefaultSplitMode,
    /*
     * Why: each member's weight by id: 1 in or 0 out for "Igual", the parts for "Partes", the whole percentage for
     * "Porcentagem" and "Pela renda". A member missing from it joined after it was saved.
     */
    val weights: Map<String, Long>,
) {
    // Why: what someone who joined after it was saved starts with: in for "Igual", 1 part, 0%.
    fun weightOf(memberId: String): Long = weights[memberId] ?: newcomerWeight

    fun isNewcomer(memberId: String): Boolean = memberId !in weights

    private val newcomerWeight: Long
        get() = when (mode) {
            DefaultSplitMode.EQUAL, DefaultSplitMode.SHARES -> 1L
            DefaultSplitMode.PERCENT, DefaultSplitMode.INCOME -> 0L
        }

    companion object {
        const val FULL_PERCENT = 100L

        // Why: the most parts Dividir lets a bill hold, so a default never opens a bill its stepper can't keep.
        const val MAX_WEIGHT = 99L
    }
}
