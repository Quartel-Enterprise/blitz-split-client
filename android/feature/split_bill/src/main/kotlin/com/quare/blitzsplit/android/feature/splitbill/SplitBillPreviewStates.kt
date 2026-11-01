package com.quare.blitzsplit.android.feature.splitbill

import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillConflict
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillGroup
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillPerson
import com.quare.blitzsplit.feature.splitbill.presentation.model.GroupSelection
import com.quare.blitzsplit.feature.splitbill.presentation.model.PayerAmountRow
import com.quare.blitzsplit.feature.splitbill.presentation.model.PayerAmounts
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitRow

private const val PREVIEW_SPENT_AT = 1_761_683_400_000L
private const val PREVIEW_TOTAL_MINOR = 31_200L
private const val PREVIEW_DINNER_TOTAL_MINOR = 64_200L
private const val PREVIEW_ME_ID = "pv"
private const val PREVIEW_ANA_ID = "an"
private const val PREVIEW_DIEGO_ID = "di"
private const val PREVIEW_BRUNO_ID = "br"
private const val PREVIEW_TRIP_ID = "vf"
private val previewPeople: List<BillPerson> = listOf(
    BillPerson(
        PREVIEW_ME_ID,
        "Pierre",
        "PV",
        AvatarColor.BLUE,
        isMe = true,
        hasJoined = true,
    ),
    BillPerson(
        PREVIEW_ANA_ID,
        "Ana",
        "AN",
        AvatarColor.GREEN,
        isMe = false,
        hasJoined = true,
    ),
    BillPerson(
        PREVIEW_BRUNO_ID,
        "Bruno",
        "BR",
        AvatarColor.PEACH,
        isMe = false,
        hasJoined = true,
    ),
    BillPerson(
        "ca",
        "Carla",
        "CA",
        AvatarColor.LILAC,
        isMe = false,
        hasJoined = true,
    ),
    BillPerson(
        "el",
        "Elisa",
        "EL",
        AvatarColor.SAND,
        isMe = false,
        hasJoined = true,
    ),
    BillPerson(
        PREVIEW_DIEGO_ID,
        "Diego",
        "DI",
        AvatarColor.GREEN,
        isMe = false,
        hasJoined = false,
    ),
)
private val previewGroups = listOf(
    BillGroup(PREVIEW_TRIP_ID, "Viagem Floripa", "VF", AvatarColor.PEACH, memberCount = 6),
    BillGroup("preview-home", "Apê 302", "AP", AvatarColor.BLUE, memberCount = 2),
    BillGroup("preview-soccer", "Futebol de quinta", "FQ", AvatarColor.GREEN, memberCount = 10),
    BillGroup("preview-barbecue", "Churrasco de sábado", "CS", AvatarColor.SAND, memberCount = 8),
)

internal fun createPreviewBillState(
    amountText: String = "312,00",
    description: String = "Mercado",
    isOnline: Boolean = true,
    splitEditor: SplitEditor? = null,
    isEditing: Boolean = false,
    conflict: BillConflict? = null,
    groupSelection: GroupSelection? = null,
): SplitBillUiState = SplitBillUiState(
    isEditing = isEditing,
    groupId = PREVIEW_TRIP_ID,
    groupName = "Viagem Floripa",
    groupInitials = "VF",
    groupColor = AvatarColor.PEACH,
    groups = previewGroups,
    groupSelection = groupSelection,
    currency = GroupCurrency.BRL,
    people = previewPeople,
    amountText = amountText,
    description = description,
    hasEditedAmount = true,
    hasEditedDescription = true,
    payerId = splitEditor?.payerIds?.firstOrNull() ?: PREVIEW_ME_ID,
    severalPayers = null,
    payerSelection = null,
    payerAmounts = null,
    spentAt = PREVIEW_SPENT_AT,
    spentDay = RelativeDay.TODAY,
    latestSpentAt = PREVIEW_SPENT_AT,
    split = createPreviewEqualEditor(
        totalMinor = if (amountText.isEmpty() || amountText == "0,00") 0L else PREVIEW_TOTAL_MINOR,
        isAnyoneIn = true,
    ),
    splitEditor = splitEditor,
    conflict = conflict,
    deletion = null,
    isOnline = isOnline,
    isSaving = false,
    isDeleting = false,
    isLoaded = true,
)

internal fun createPreviewEqualEditor(
    totalMinor: Long = PREVIEW_TOTAL_MINOR,
    isAnyoneIn: Boolean,
): SplitEditor = createPreviewEditor(
    mode = SplitMode.EQUAL,
    totalMinor = totalMinor,
    payerId = PREVIEW_ME_ID,
) { person ->
    SplitRow(
        person = person,
        isIncluded = isAnyoneIn && person.id != PREVIEW_DIEGO_ID,
        valueText = "",
        parts = 1L,
        shareMinor = if (isAnyoneIn && person.id != PREVIEW_DIEGO_ID) 6_240L else null,
    )
}

internal fun createPreviewAmountEditor(): SplitEditor {
    val values = mapOf(
        PREVIEW_ME_ID to "118,00",
        "an" to "132,00",
        PREVIEW_BRUNO_ID to "146,00",
        "ca" to "98,00",
        "el" to "",
        PREVIEW_DIEGO_ID to "64,00",
    )
    return createPreviewEditor(
        mode = SplitMode.AMOUNT,
        totalMinor = PREVIEW_DINNER_TOTAL_MINOR,
        payerId = PREVIEW_BRUNO_ID,
    ) { person ->
        SplitRow(
            person = person,
            isIncluded = true,
            valueText = values.getValue(person.id),
            parts = 1L,
            shareMinor = null,
        )
    }
}

internal fun createPreviewPercentEditor(): SplitEditor {
    val percents = mapOf(
        PREVIEW_ME_ID to 30L,
        "an" to 20L,
        PREVIEW_BRUNO_ID to 20L,
        "ca" to 15L,
        "el" to 15L,
        PREVIEW_DIEGO_ID to 0L,
    )
    return createPreviewEditor(
        mode = SplitMode.PERCENT,
        totalMinor = PREVIEW_TOTAL_MINOR,
        payerId = PREVIEW_ME_ID,
    ) { person ->
        val percent = percents.getValue(person.id)
        SplitRow(
            person = person,
            isIncluded = true,
            valueText = percent.toString(),
            parts = 1L,
            shareMinor = (PREVIEW_TOTAL_MINOR * percent / 100).takeIf { percent > 0 },
        )
    }
}

internal fun createPreviewSharesEditor(): SplitEditor {
    val parts = mapOf(
        PREVIEW_ME_ID to 1L,
        "an" to 2L,
        PREVIEW_BRUNO_ID to 1L,
        "ca" to 1L,
        "el" to 1L,
        PREVIEW_DIEGO_ID to 0L,
    )
    return createPreviewEditor(
        mode = SplitMode.SHARES,
        totalMinor = PREVIEW_TOTAL_MINOR,
        payerId = PREVIEW_ME_ID,
    ) { person ->
        val count = parts.getValue(person.id)
        SplitRow(
            person = person,
            isIncluded = true,
            valueText = "",
            parts = count,
            shareMinor = (5_200L * count).takeIf { count > 0 },
        )
    }
}

private fun createPreviewEditor(
    mode: SplitMode,
    totalMinor: Long,
    payerId: String,
    rowOf: (BillPerson) -> SplitRow,
): SplitEditor = SplitEditor(
    mode = mode,
    rows = previewPeople.map(rowOf),
    totalMinor = totalMinor,
    payerIds = listOf(payerId),
    currency = GroupCurrency.BRL,
)

// Why: "Várias pessoas pagaram" once it adds up, as the canvas draws it: Você R$ 200,00 and Ana R$ 112,00.
internal fun createPreviewPayerAmounts(): PayerAmounts {
    val paid = mapOf(PREVIEW_ME_ID to "200,00", PREVIEW_ANA_ID to "112,00")
    return PayerAmounts(
        rows = previewPeople.map { person ->
            PayerAmountRow(
                memberId = person.id,
                person = person,
                amountText = paid[person.id].orEmpty(),
            )
        },
        totalMinor = PREVIEW_TOTAL_MINOR,
        currency = GroupCurrency.BRL,
    )
}
