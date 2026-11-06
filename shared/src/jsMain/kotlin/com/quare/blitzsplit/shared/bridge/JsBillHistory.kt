@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryChange
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryDay
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryEntry
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryKind
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryPart

// Why: Conta's "Histórico" (7.3), flat for the web, as BillDetailPresenter hands it over.

// Why: "Carla R$ 112,00 → R$ 98,00"; a null side is someone out of the split there, a null name "Ex-membro".
class JsBillHistoryPart(
    val name: String?,
    val isMe: Boolean,
    val beforeMinor: Double?,
    val afterMinor: Double?,
)

/*
 * Why: BillHistoryChange, flat. field is "amount" (beforeMinor, afterMinor), "description" (beforeText, afterText),
 * "spent-at" (beforeMillis, afterMillis), "currency" (beforeText, afterText, the ISO codes), "category"
 * (beforeCategory, afterCategory, already resolved, Outros for none), "payers" (beforeNames, afterNames, a null name "Ex-membro", and parts when the same payers paid other
 * amounts) or "split" (beforeMode, afterMode as SplitMode codes, and parts); what a field doesn't use is null or empty.
 */
class JsBillHistoryChange(
    val field: String,
    val beforeMinor: Double?,
    val afterMinor: Double?,
    val beforeText: String?,
    val afterText: String?,
    val beforeMillis: Double?,
    val afterMillis: Double?,
    val beforeCategory: JsCategory?,
    val afterCategory: JsCategory?,
    val beforeNames: Array<String?>,
    val afterNames: Array<String?>,
    val beforeMode: String?,
    val afterMode: String?,
    val parts: Array<JsBillHistoryPart>,
)

/*
 * Why: one line of "Histórico". kind is "created", "edited", "attachments-added", "attachment-removed" or
 * "link-changed"; splitMode a SplitMode code; linkVisibility "members" or "anyone", null once the link was turned off.
 */
class JsBillHistoryEntry(
    val id: String,
    val kind: String,
    val actorName: String?,
    val actorInitials: String,
    val actorColor: String,
    val actorPhotoUrl: String?,
    val actorIsMe: Boolean,
    // Why: the actor still in the group, whose face opens their profile (openPerson); else null.
    val actorMemberId: String?,
    val occurredAt: Double,
    val changes: Array<JsBillHistoryChange>,
    val amountMinor: Double?,
    val splitMode: String?,
    val sharesCount: Int?,
    val attachmentCount: Int,
    val isReceipt: Boolean,
    val linkVisibility: String?,
    val isPending: Boolean,
    val canUndo: Boolean,
)

class JsBillHistoryDay(
    val dayStartMillis: Double,
    // Why: "today", "yesterday" or "other", for "Hoje", "Ontem" or the date.
    val relativeDay: String,
    val entries: Array<JsBillHistoryEntry>,
)

internal fun toJsBillHistoryDay(
    day: BillHistoryDay,
    categories: CategoryTree,
): JsBillHistoryDay = JsBillHistoryDay(
    dayStartMillis = day.dayStartMillis.toDouble(),
    relativeDay = toJsRelativeDay(day.relativeDay),
    entries = day.entries.map { toJsBillHistoryEntry(it, categories) }.toTypedArray(),
)

private fun toJsBillHistoryEntry(
    entry: BillHistoryEntry,
    categories: CategoryTree,
): JsBillHistoryEntry = JsBillHistoryEntry(
    id = entry.id,
    kind = toJsHistoryKind(entry.kind),
    actorName = entry.actorName,
    actorInitials = entry.actorInitials,
    actorColor = entry.actorColor.code,
    actorPhotoUrl = entry.actorPhotoUrl,
    actorIsMe = entry.actorIsMe,
    actorMemberId = entry.actorMemberId,
    occurredAt = entry.occurredAt.toDouble(),
    changes = entry.changes.map { toJsBillHistoryChange(it, categories) }.toTypedArray(),
    amountMinor = entry.amountMinor?.toDouble(),
    splitMode = entry.splitMode?.code,
    sharesCount = entry.sharesCount,
    attachmentCount = entry.attachmentCount,
    isReceipt = entry.isReceipt,
    linkVisibility = entry.linkVisibility?.code,
    isPending = entry.isPending,
    canUndo = entry.canUndo,
)

private fun toJsHistoryKind(kind: BillHistoryKind): String = when (kind) {
    BillHistoryKind.CREATED -> "created"
    BillHistoryKind.EDITED -> "edited"
    BillHistoryKind.ATTACHMENTS_ADDED -> "attachments-added"
    BillHistoryKind.ATTACHMENT_REMOVED -> "attachment-removed"
    BillHistoryKind.LINK_CHANGED -> "link-changed"
}

// Why: one shape for every field, so the page reads it without knowing Kotlin's sealed classes.
private fun toJsBillHistoryChange(
    change: BillHistoryChange,
    categories: CategoryTree,
): JsBillHistoryChange = when (change) {
    is BillHistoryChange.Amount -> buildHistoryChange(
        field = "amount",
        beforeMinor = change.beforeMinor?.toDouble(),
        afterMinor = change.afterMinor?.toDouble(),
    )

    is BillHistoryChange.Description -> buildHistoryChange(
        field = "description",
        beforeText = change.before,
        afterText = change.after,
    )

    is BillHistoryChange.SpentAt -> buildHistoryChange(
        field = "spent-at",
        beforeMillis = change.beforeMillis?.toDouble(),
        afterMillis = change.afterMillis?.toDouble(),
    )

    is BillHistoryChange.Currency -> buildHistoryChange(
        field = "currency",
        beforeText = change.before?.code,
        afterText = change.after?.code,
    )

    is BillHistoryChange.Category -> buildHistoryChange(
        field = "category",
        beforeCategory = toJsCategory(categories.resolve(change.beforeId)),
        afterCategory = toJsCategory(categories.resolve(change.afterId)),
    )

    is BillHistoryChange.Payers -> buildHistoryChange(
        field = "payers",
        beforeNames = change.before.toTypedArray(),
        afterNames = change.after.toTypedArray(),
        parts = change.parts.map(::toJsBillHistoryPart).toTypedArray(),
    )

    is BillHistoryChange.Split -> buildHistoryChange(
        field = "split",
        beforeMode = change.beforeMode?.code,
        afterMode = change.afterMode?.code,
        parts = change.parts.map(::toJsBillHistoryPart).toTypedArray(),
    )
}

private fun buildHistoryChange(
    field: String,
    beforeMinor: Double? = null,
    afterMinor: Double? = null,
    beforeText: String? = null,
    afterText: String? = null,
    beforeMillis: Double? = null,
    afterMillis: Double? = null,
    beforeCategory: JsCategory? = null,
    afterCategory: JsCategory? = null,
    beforeNames: Array<String?> = emptyArray(),
    afterNames: Array<String?> = emptyArray(),
    beforeMode: String? = null,
    afterMode: String? = null,
    parts: Array<JsBillHistoryPart> = emptyArray(),
): JsBillHistoryChange = JsBillHistoryChange(
    field = field,
    beforeMinor = beforeMinor,
    afterMinor = afterMinor,
    beforeText = beforeText,
    afterText = afterText,
    beforeMillis = beforeMillis,
    afterMillis = afterMillis,
    beforeCategory = beforeCategory,
    afterCategory = afterCategory,
    beforeNames = beforeNames,
    afterNames = afterNames,
    beforeMode = beforeMode,
    afterMode = afterMode,
    parts = parts,
)

private fun toJsBillHistoryPart(part: BillHistoryPart): JsBillHistoryPart = JsBillHistoryPart(
    name = part.name,
    isMe = part.isMe,
    beforeMinor = part.beforeMinor?.toDouble(),
    afterMinor = part.afterMinor?.toDouble(),
)
