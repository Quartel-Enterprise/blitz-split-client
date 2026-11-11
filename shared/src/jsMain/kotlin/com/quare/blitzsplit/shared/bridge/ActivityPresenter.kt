@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.activity.domain.model.ActivityType
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityChangeRow
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityChangeValue
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityDay
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityDebtChange
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityDeletion
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityEmptyState
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityFilter
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityGroupOption
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityItem
import com.quare.blitzsplit.feature.activity.presentation.model.ActivitySimplification
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityStatus
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityTarget
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityUiEvent
import com.quare.blitzsplit.feature.activity.presentation.viewmodel.ActivityViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

// Why: one side of "antes → depois"; the page writes it as the field needs it, in its language.
class JsActivityValue(
    val minor: Double?,
    val currency: String?,
    val fractionDigits: Int,
    val text: String?,
    val millis: Double?,
    // Why: a bill's "Quando" without a time shows its date alone.
    val hasTime: Boolean,
    // Why: a split mode, a group type, a color or a currency, by its code.
    val code: String?,
    val names: Array<String>,
    // Why: someone no longer in the group, whom the page writes "Ex-membro".
    val hasFormerMember: Boolean,
    // Why: a bill's category, resolved in every group's categories (Outros for none); null for any other field.
    val category: JsCategory?,
)

class JsActivityChange(
    // Why: ActivityField's code ("amount", "split", "default_split"…).
    val field: String,
    val before: JsActivityValue,
    val after: JsActivityValue,
)

// Why: who deleted the item's bill or group and when; byName and atMillis are null when the device no longer knows.
class JsActivityDeletion(
    val isGroup: Boolean,
    val subject: String?,
    val byName: String?,
    val byMe: Boolean,
    val atMillis: Double?,
)

/*
 * Why: a line of "Ver mudanças" of "Simplificar dívidas" (13.7b): what someone owed this device's user before and
 * after, positive when they owe the user; a null name is "Ex-membro".
 */
class JsActivityDebtChange(
    val name: String?,
    val beforeMinor: Double,
    val afterMinor: Double,
    // Why: the currency this pair is in, null when not even the group's is known: the amount is left out then.
    val currency: String?,
    val fractionDigits: Int,
)

/*
 * Why: "7 pagamentos viraram 5. Agora Diego te paga R$ 64,00, e Ana não te deve mais." (13.7), only what changed for
 * this device's user, the largest first; newPair and gonePair are the largest of those that came and went.
 */
class JsActivitySimplification(
    val paymentsBefore: Int,
    val paymentsAfter: Int,
    val myChanges: Array<JsActivityDebtChange>,
    val newPair: JsActivityDebtChange?,
    val gonePair: JsActivityDebtChange?,
)

class JsActivityItem(
    val id: String,
    // Why: ActivityKind's code ("bill_created", "member_joined"…).
    val kind: String,
    // Why: ActivityType's code ("bills", "payments", "comments", "people-and-groups", "charges").
    val type: String,
    // Why: null when the server kept no name; actorIsMe is "Você" whatever the name.
    val actorName: String?,
    val actorIsMe: Boolean,
    val actorInitials: String,
    val actorColor: String,
    // Why: the actor's face opens their profile, or Perfil for actorIsMe (openActor).
    val actorOpensProfile: Boolean,
    val groupId: String,
    val groupName: String?,
    val groupInitials: String,
    // Why: null once the group is gone from the device.
    val groupColor: String?,
    val groupType: String?,
    val isGroupOnDevice: Boolean,
    val occurredAt: Double,
    // Why: the day the item is listed under, "today", "yesterday" or "other", and that day's start.
    val relativeDay: String,
    val dayStartMillis: Double,
    val subject: String?,
    val amountMinor: Double?,
    val currency: String?,
    val fractionDigits: Int,
    // Why: a bill's split mode, a group's type or a payment's method, by its code.
    val code: String?,
    val fromName: String?,
    val fromIsMe: Boolean,
    val toName: String?,
    val toIsMe: Boolean,
    val changes: Array<JsActivityChange>,
    // Why: what a comment says now, cut to what a row shows; null once deleted.
    val body: String?,
    // Why: "Ana apagou um comentário em Jantar", with no text, in place of "Ana comentou em Jantar".
    val isCommentDeleted: Boolean,
    val moreChangesCount: Int,
    val hasChangesToShow: Boolean,
    val isPending: Boolean,
    val isUnread: Boolean,
    // Why: "bill", "group", "members" or "balances": what "Abrir" opens.
    val target: String,
    val deletion: JsActivityDeletion?,
    // Why: "Ver mudanças" open in the row (one column).
    val isExpanded: Boolean,
    // Why: "ligou Simplificar dívidas em" rather than "desligou", for "simplify_debts_changed".
    val isSimplifyTurnedOn: Boolean,
    // Why: what "Simplificar dívidas" changed (13.7); null for any other kind and while it waits to go up.
    val simplification: JsActivitySimplification?,
)

class JsActivityDay(
    val dayStartMillis: Double,
    // Why: "today", "yesterday" or "other", which the page writes "Hoje", "Ontem" or the date.
    val relativeDay: String,
    val items: Array<JsActivityItem>,
)

class JsActivityGroup(
    val id: String,
    val name: String,
    val initials: String,
    val color: String,
    val type: String,
)

class JsActivityFilter(
    // Why: null is "Todos os grupos".
    val groupId: String?,
    // Why: ActivityType's codes, in its order; none is every type.
    val types: Array<String>,
    val hasPerson: Boolean,
)

class JsActivityState(
    val days: Array<JsActivityDay>,
    // Why: "3 novas", whatever the filter.
    val unreadCount: Int,
    val canMarkAllRead: Boolean,
    val filter: JsActivityFilter,
    val isFilterActive: Boolean,
    // Why: "Filtrar" open with what is being picked; null while closed.
    val filterDraft: JsActivityFilter?,
    // Why: "Mostrar 4 resultados".
    val draftResultCount: Int,
    val groups: Array<JsActivityGroup>,
    // Why: the removable badges: the group's name, each type's code, the person's name.
    val groupChipName: String?,
    val typeChips: Array<String>,
    val personName: String?,
    // Why: the item beside the list on the desktop, with the other events of its bill ("Mais em Jantar").
    val selectedItem: JsActivityItem?,
    val relatedItems: Array<JsActivityItem>,
    // Why: "offline", "server-error" or "syncing", null when nothing asks for attention.
    val status: String?,
    // Why: "Offline · 2 alterações na fila".
    val queuedCount: Int,
    val showsPlaceholders: Boolean,
    // Why: "Sem internet. Seus dados aparecem quando reconectar." under the placeholders.
    val isWaitingForInternet: Boolean,
    val showsNotSyncedNotice: Boolean,
    val showsOfflineEnd: Boolean,
    val showsTools: Boolean,
    // Why: "nothing-yet" or "no-results", null while there are items or before they were read.
    val emptyState: String?,
    val isLoaded: Boolean,
    val isLoadingMore: Boolean,
    // Why: nothing here matches the filter yet and older pages load on their own: loading rows, not the empty state.
    val isLoadingOlderItems: Boolean,
    val hasMore: Boolean,
    val isRefreshing: Boolean,
    // Why: the item tapped whose bill or group is gone, a toast; null once shown.
    val deletedNotice: JsActivityDeletion?,
    // Why: the tab the sidebar selects around Atividade ("home", "groups", "balances" or "contacts"), the one under it.
    val homeTab: String,
)

class ActivityPresenter(
    route: JsRoute,
) {
    private val activityRoute = route.route as Route.Activity
    private val holder = ViewModelHolder(ActivityViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(activityRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsActivityState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            val dayOf = state.days.flatMap { day -> day.items.map { it.id to day } }.toMap()
            val toItem = { item: ActivityItem -> toJsActivityItem(item, dayOf[item.id], state.expandedItemId) }
            JsActivityState(
                days = state.days
                    .map { day ->
                        JsActivityDay(
                            dayStartMillis = day.dayStartMillis.toDouble(),
                            relativeDay = toJsRelativeDay(day.relativeDay),
                            items = day.items.map(toItem).toTypedArray(),
                        )
                    }.toTypedArray(),
                unreadCount = state.unreadCount,
                canMarkAllRead = state.canMarkAllRead,
                filter = toJsActivityFilter(state.filter),
                isFilterActive = state.filter.isActive,
                filterDraft = state.filterDraft?.let(::toJsActivityFilter),
                draftResultCount = state.draftResultCount,
                groups = state.groups.map(::toJsActivityGroup).toTypedArray(),
                groupChipName = state.groupChipName,
                typeChips = state.typeChips.map(::toTypeCode).toTypedArray(),
                personName = state.personName,
                selectedItem = state.selectedItem?.let(toItem),
                relatedItems = state.relatedItems.map(toItem).toTypedArray(),
                status = when (state.status) {
                    ActivityStatus.OFFLINE -> "offline"
                    ActivityStatus.SERVER_ERROR -> "server-error"
                    ActivityStatus.SYNCING -> "syncing"
                    null -> null
                },
                queuedCount = state.queuedCount,
                showsPlaceholders = state.showsPlaceholders,
                isWaitingForInternet = state.isWaitingForInternet,
                showsNotSyncedNotice = state.showsNotSyncedNotice,
                showsOfflineEnd = state.showsOfflineEnd,
                showsTools = state.showsTools,
                emptyState = when (state.emptyState) {
                    ActivityEmptyState.NOTHING_YET -> "nothing-yet"
                    ActivityEmptyState.NO_RESULTS -> "no-results"
                    null -> null
                },
                isLoaded = state.isLoaded,
                isLoadingMore = state.isLoadingMore,
                isLoadingOlderItems = state.isLoadingOlderItems,
                hasMore = state.hasMore,
                isRefreshing = state.isRefreshing,
                deletedNotice = state.deletedNotice?.let(::toJsActivityDeletion),
                homeTab = toJsHomeTab(state.homeTab),
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: the desktop (from 1024 px) draws the item beside the list, where a click picks it instead of opening it.
    fun detailPaneChanged(isShown: Boolean) {
        viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown))
    }

    fun clickItem(itemId: String) {
        viewModel.onEvent(ActivityUiEvent.OnItemClicked(itemId))
    }

    fun openGroup(itemId: String) {
        viewModel.onEvent(ActivityUiEvent.OnGroupClicked(itemId))
    }

    fun openActor(itemId: String) {
        viewModel.onEvent(ActivityUiEvent.OnActorClicked(itemId))
    }

    fun openTarget(itemId: String) {
        viewModel.onEvent(ActivityUiEvent.OnOpenTargetClicked(itemId))
    }

    fun toggleChanges(itemId: String) {
        viewModel.onEvent(ActivityUiEvent.OnChangesToggled(itemId))
    }

    fun markAllRead() {
        viewModel.onEvent(ActivityUiEvent.OnMarkAllReadClicked)
    }

    fun openNotificationSettings() {
        viewModel.onEvent(ActivityUiEvent.OnNotificationSettingsClicked)
    }

    fun openFilter() {
        viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
    }

    // Why: null is "Todos os grupos".
    fun pickDraftGroup(groupId: String?) {
        viewModel.onEvent(ActivityUiEvent.OnDraftGroupPicked(groupId))
    }

    // Why: an ActivityType's code; an unknown one is ignored.
    fun toggleDraftType(code: String) {
        val type = typeOrNull(code) ?: return
        viewModel.onEvent(ActivityUiEvent.OnDraftTypeToggled(type))
    }

    fun applyFilter() {
        viewModel.onEvent(ActivityUiEvent.OnFilterApplied)
    }

    fun clearFilterDraft() {
        viewModel.onEvent(ActivityUiEvent.OnFilterCleared)
    }

    fun dismissFilter() {
        viewModel.onEvent(ActivityUiEvent.OnFilterDismissed)
    }

    fun removeGroupChip() {
        viewModel.onEvent(ActivityUiEvent.OnGroupChipRemoved)
    }

    fun removeTypeChip(code: String) {
        val type = typeOrNull(code) ?: return
        viewModel.onEvent(ActivityUiEvent.OnTypeChipRemoved(type))
    }

    fun removePersonChip() {
        viewModel.onEvent(ActivityUiEvent.OnPersonChipRemoved)
    }

    fun clearFilters() {
        viewModel.onEvent(ActivityUiEvent.OnClearFiltersClicked)
    }

    fun endReached() {
        viewModel.onEvent(ActivityUiEvent.OnEndReached)
    }

    fun refresh() {
        viewModel.onEvent(ActivityUiEvent.OnRefresh)
    }

    fun emptyAction() {
        viewModel.onEvent(ActivityUiEvent.OnEmptyActionClicked)
    }

    fun deletedNoticeShown() {
        viewModel.onEvent(ActivityUiEvent.OnDeletedNoticeShown)
    }

    // Why: the phone's back, to Início or the contact's profile that opened Atividade.
    fun back() {
        viewModel.onEvent(ActivityUiEvent.OnBackClicked)
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toTypeCode(type: ActivityType): String = when (type) {
    ActivityType.BILLS -> "bills"
    ActivityType.PAYMENTS -> "payments"
    ActivityType.COMMENTS -> "comments"
    ActivityType.PEOPLE_AND_GROUPS -> "people-and-groups"
    ActivityType.CHARGES -> "charges"
}

private fun typeOrNull(code: String): ActivityType? = ActivityType.entries.firstOrNull { toTypeCode(it) == code }

private fun toJsActivityFilter(filter: ActivityFilter): JsActivityFilter = JsActivityFilter(
    groupId = filter.groupId,
    types = filter.types.map(::toTypeCode).toTypedArray(),
    hasPerson = filter.personKey != null,
)

private fun toJsActivityGroup(group: ActivityGroupOption): JsActivityGroup = JsActivityGroup(
    id = group.id,
    name = group.name,
    initials = Initials.of(group.name),
    color = group.color.code,
    type = group.type.code,
)

private fun toJsActivityDeletion(deletion: ActivityDeletion): JsActivityDeletion = JsActivityDeletion(
    isGroup = deletion.isGroup,
    subject = deletion.subject,
    byName = deletion.byName,
    byMe = deletion.byMe,
    atMillis = deletion.atMillis?.toDouble(),
)

private fun toJsActivityValue(value: ActivityChangeValue): JsActivityValue = JsActivityValue(
    minor = value.minor?.toDouble(),
    currency = value.currency?.code,
    fractionDigits = value.currency?.fractionDigits ?: 2,
    text = value.text,
    millis = value.millis?.toDouble(),
    hasTime = value.hasTime,
    code = value.code,
    names = value.names.toTypedArray(),
    hasFormerMember = value.hasFormerMember,
    category = value.category?.let(::toJsCategory),
)

private fun toJsActivityChange(change: ActivityChangeRow): JsActivityChange = JsActivityChange(
    field = change.field.code,
    before = toJsActivityValue(change.before),
    after = toJsActivityValue(change.after),
)

private fun toJsActivityItem(
    item: ActivityItem,
    day: ActivityDay?,
    expandedItemId: String?,
): JsActivityItem = JsActivityItem(
    id = item.id,
    kind = item.kind.code,
    type = toTypeCode(item.kind.type),
    actorName = item.actor.name,
    actorIsMe = item.actor.isMe,
    actorInitials = item.actor.initials,
    actorColor = item.actor.color.code,
    actorOpensProfile = item.actor.opensProfile,
    groupId = item.groupId,
    groupName = item.groupName,
    groupInitials = item.groupName?.let(Initials::of).orEmpty(),
    groupColor = item.groupColor?.code,
    groupType = item.groupType?.code,
    isGroupOnDevice = item.isGroupOnDevice,
    occurredAt = item.occurredAt.toDouble(),
    relativeDay = day?.relativeDay?.let(::toJsRelativeDay) ?: "other",
    dayStartMillis = (day?.dayStartMillis ?: item.occurredAt).toDouble(),
    subject = item.subject,
    amountMinor = item.amountMinor?.toDouble(),
    currency = item.currency?.code,
    fractionDigits = item.currency?.fractionDigits ?: 2,
    code = item.code,
    fromName = item.fromName,
    fromIsMe = item.fromIsMe,
    toName = item.toName,
    toIsMe = item.toIsMe,
    changes = item.changes.map(::toJsActivityChange).toTypedArray(),
    body = item.body,
    isCommentDeleted = item.isCommentDeleted,
    moreChangesCount = item.moreChangesCount,
    hasChangesToShow = item.hasChangesToShow,
    isPending = item.isPending,
    isUnread = item.isUnread,
    target = when (item.target) {
        ActivityTarget.BILL -> "bill"
        ActivityTarget.GROUP -> "group"
        ActivityTarget.MEMBERS -> "members"
        ActivityTarget.BALANCES -> "balances"
    },
    deletion = item.deletion?.let(::toJsActivityDeletion),
    isExpanded = item.id == expandedItemId,
    isSimplifyTurnedOn = item.isSimplifyTurnedOn,
    simplification = item.simplification?.let(::toJsActivitySimplification),
)

private fun toJsActivitySimplification(simplification: ActivitySimplification): JsActivitySimplification =
    JsActivitySimplification(
        paymentsBefore = simplification.paymentsBefore,
        paymentsAfter = simplification.paymentsAfter,
        myChanges = simplification.myChanges.map(::toJsActivityDebtChange).toTypedArray(),
        newPair = simplification.newPair?.let(::toJsActivityDebtChange),
        gonePair = simplification.gonePair?.let(::toJsActivityDebtChange),
    )

private fun toJsActivityDebtChange(change: ActivityDebtChange): JsActivityDebtChange = JsActivityDebtChange(
    name = change.name,
    beforeMinor = change.beforeMinor.toDouble(),
    afterMinor = change.afterMinor.toDouble(),
    currency = change.currency?.code,
    fractionDigits = change.currency?.fractionDigits ?: 2,
)
