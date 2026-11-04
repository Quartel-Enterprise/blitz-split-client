package com.quare.blitzsplit.feature.editgroup.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.TypedText
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveCategoryTree
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.IncomePercents
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.usecase.GetSplitIncomes
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.UpdateDefaultSplit
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveBitcoinInSats
import com.quare.blitzsplit.core.utils.DeviceDecimalSeparator
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitFigures
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitPerson
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class DefaultSplitViewModel(
    private val route: Route.DefaultSplit,
    private val navigator: Navigator,
    private val updateDefaultSplit: UpdateDefaultSplit,
    private val getSplitIncomes: GetSplitIncomes,
    private val decimalSeparator: DeviceDecimalSeparator,
    observeGroup: ObserveGroup,
    observeCategoryTree: ObserveCategoryTree,
    observeBitcoinInSats: ObserveBitcoinInSats,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<DefaultSplitUiState>
        field = MutableStateFlow(
            DefaultSplitUiState(
                groupName = "",
                currency = GroupCurrency.fallback,
                showsSats = false,
                mode = DefaultSplitMode.EQUAL,
                people = emptyList(),
                incomeFallback = emptyMap(),
                isOnline = true,
                isLoaded = false,
                isSaving = false,
                showsSaveError = false,
                hasChanges = false,
                figures = DefaultSplitFigures.of(
                    mode = DefaultSplitMode.EQUAL,
                    people = emptyList(),
                    incomeFallback = emptyMap(),
                    currency = GroupCurrency.fallback,
                    showsSats = false,
                ),
                exceptions = emptyList(),
                categories = CategoryTree.appOnly,
                exceptionEditor = null,
            ),
        )

    val stateWatcher: FlowWatcher<DefaultSplitUiState> = FlowWatcher(uiState)

    // Why: the group went while another screen covered this one, which then leaves as soon as it is on top again.
    private var isGroupGone = false

    // Why: what the screen opened with, so Salvar only turns on once something differs from it.
    private var opened: Opened? = null

    /*
     * Why: the form is filled from the group once, so a sync landing while someone types changes nothing here; only
     * the people follow it: who joins comes in as the rules start them, who is removed goes.
     */
    init {
        // Why: the form opens once bitcoin's unit is known, so no income is filled in the other one first.
        val showsSats = flow { emit(observeBitcoinInSats().first()) }
        combine(observeGroup(route.groupId), showsSats, ::Pair)
            .onEach { (group, sats) ->
                if (opened == null) uiState.update { it.copy(showsSats = sats) }
                isGroupGone = group == null
                if (group == null) leave() else show(group)
            }.launchIn(viewModelScope)
        // Why: "Bitcoin em satoshis" changed while the form is open (the Mac's Ajustes): each income keeps its value.
        observeBitcoinInSats()
            .onEach { sats ->
                updateForm { state ->
                    state.copy(
                        showsSats = sats,
                        people = state.people.map { person ->
                            person.copy(
                                incomeText = TypedText.toUnit(
                                    text = person.incomeText,
                                    currency = state.currency,
                                    showedSats = state.showsSats,
                                    showsSats = sats,
                                    decimalSeparator = decimalSeparator(),
                                ),
                            )
                        },
                    )
                }
            }.launchIn(viewModelScope)
        observeCategoryTree(route.groupId)
            .onEach { tree -> uiState.update { it.copy(categories = tree) } }
            .launchIn(viewModelScope)
        navigator.backStack
            .onEach { if (isGroupGone) leave() }
            .launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: DefaultSplitUiEvent) {
        when (event) {
            // Why: leaving clears the ViewModel, which would cancel a save in flight.
            DefaultSplitUiEvent.OnBackClicked -> if (!uiState.value.isSaving) leave()

            is DefaultSplitUiEvent.OnModeSelected -> updateForm { it.toMode(event.mode) }

            is DefaultSplitUiEvent.OnPersonToggled -> updatePerson(event.memberId) {
                it.copy(isIncluded = !it.isIncluded)
            }

            is DefaultSplitUiEvent.OnPartsIncreased -> updatePerson(event.memberId) {
                it.copy(parts = (it.parts + 1).coerceAtMost(GroupDefaultSplit.MAX_WEIGHT))
            }

            is DefaultSplitUiEvent.OnPartsDecreased -> updatePerson(event.memberId) {
                it.copy(parts = (it.parts - 1).coerceAtLeast(0L))
            }

            is DefaultSplitUiEvent.OnPercentChanged -> updatePerson(event.memberId) {
                it.copy(percentText = TypedText.toPercentText(event.text))
            }

            is DefaultSplitUiEvent.OnIncomeChanged -> updatePerson(event.memberId) {
                it.copy(
                    incomeText = TypedText.toAmountText(
                        previous = it.incomeText,
                        typed = event.text,
                        currency = uiState.value.currency,
                        showsSats = uiState.value.showsSats,
                        decimalSeparator = decimalSeparator(),
                    ),
                )
            }

            DefaultSplitUiEvent.OnSaveClicked -> save()

            DefaultSplitUiEvent.OnAddExceptionClicked -> uiState.update { it.withExceptionOpened(null) }

            is DefaultSplitUiEvent.OnExceptionClicked -> uiState.update { it.withExceptionOpened(event.categoryId) }

            is DefaultSplitUiEvent.OnExceptionCategoryPicked -> uiState.update {
                it.withExceptionCategory(event.categoryId)
            }

            is DefaultSplitUiEvent.OnExceptionModeSelected -> uiState.update { it.withExceptionMode(event.mode) }

            is DefaultSplitUiEvent.OnExceptionPersonToggled -> uiState.update { state ->
                state.withExceptionPerson(event.memberId) { it.copy(isIncluded = !it.isIncluded) }
            }

            is DefaultSplitUiEvent.OnExceptionPartsIncreased -> uiState.update { state ->
                state.withExceptionPerson(event.memberId) {
                    it.copy(parts = (it.parts + 1).coerceAtMost(GroupDefaultSplit.MAX_WEIGHT))
                }
            }

            is DefaultSplitUiEvent.OnExceptionPartsDecreased -> uiState.update { state ->
                state.withExceptionPerson(event.memberId) { it.copy(parts = (it.parts - 1).coerceAtLeast(0L)) }
            }

            is DefaultSplitUiEvent.OnExceptionPercentChanged -> uiState.update { state ->
                state.withExceptionPerson(event.memberId) { it.copy(percentText = TypedText.toPercentText(event.text)) }
            }

            DefaultSplitUiEvent.OnExceptionSaveClicked -> updateForm { it.withExceptionSaved() }

            DefaultSplitUiEvent.OnExceptionRemoveClicked -> updateForm { it.withExceptionRemoved() }

            DefaultSplitUiEvent.OnExceptionDismissed -> uiState.update { it.copy(exceptionEditor = null) }
        }
    }

    private fun show(group: Group) {
        val saved = group.defaultSplit
        if (opened == null) {
            open(group)
            return
        }
        // Why: the form keeps what is typed; someone who joined since comes in as the split being typed starts them.
        updateForm { state ->
            // Why: a group now in another currency keeps each income's number, as Rachar conta keeps the bill's.
            val byId = state.people
                .map { person ->
                    person.copy(
                        incomeText = TypedText.toCurrency(
                            text = person.incomeText,
                            from = state.currency,
                            to = group.currency,
                            showsSats = state.showsSats,
                            decimalSeparator = decimalSeparator(),
                        ),
                    )
                }.associateBy { it.id }
            state.copy(
                groupName = group.name,
                currency = group.currency,
                people = group.members.map { member ->
                    byId[member.id]?.withMember(member) ?: personOf(
                        member = member,
                        saved = GroupDefaultSplit(state.mode, emptyMap(), emptyList()),
                    ).copy(isNew = saved != null)
                },
            )
        }
    }

    // Why: the incomes are only a nicety of this device, so the form opens at once and a failed read leaves them out.
    private fun open(group: Group) {
        val saved = group.defaultSplit
        val split = GroupDefaultSplit(
            mode = saved?.mode ?: DefaultSplitMode.EQUAL,
            weights = group.members.associate { it.id to (saved?.weightOf(it.id) ?: 1L) },
            exceptions = saved?.exceptions.orEmpty(),
        )
        opened = Opened(
            split = split,
            incomes = emptyMap(),
        )
        updateForm { state ->
            state.copy(
                groupName = group.name,
                currency = group.currency,
                mode = split.mode,
                people = group.members.map { member ->
                    personOf(
                        member = member,
                        saved = saved,
                    ).copy(isNew = saved?.isNewcomer(member.id) == true)
                },
                incomeFallback = if (split.mode == DefaultSplitMode.INCOME) split.weights else emptyMap(),
                exceptions = split.exceptions,
                isLoaded = true,
            )
        }
        viewModelScope.launch {
            val incomes = getSplitIncomes(route.groupId).getOrDefault(emptyMap())
            if (incomes.isEmpty() || !split.isGivenBy(incomes)) return@launch
            opened = opened?.copy(incomes = incomes.filterValues { it > 0 })
            updateForm { state ->
                state.copy(
                    people = state.people.map { person ->
                        val income = incomes[person.id]
                        if (income == null || person.incomeText.isNotEmpty()) {
                            person
                        } else {
                            person.copy(incomeText = formatAmount(income, state.currency, state.showsSats))
                        }
                    },
                )
            }
        }
    }

    /*
     * Why: another member may have saved other percentages for "Pela renda" since this device typed its incomes, which
     * then no longer give what the group keeps: the screen shows the group's, not the stale incomes'.
     */
    private fun GroupDefaultSplit.isGivenBy(incomes: Map<String, Long>): Boolean = mode != DefaultSplitMode.INCOME ||
        IncomePercents.of(weights.keys.map { it to (incomes[it] ?: 0L) }) == weights

    // Why: someone the saved split has, or a newcomer as its way to split starts them.
    private fun personOf(
        member: GroupMember,
        saved: GroupDefaultSplit?,
    ): DefaultSplitPerson {
        val weight = saved?.weightOf(member.id) ?: 1L
        val mode = saved?.mode ?: DefaultSplitMode.EQUAL
        return DefaultSplitPerson(
            id = member.id,
            name = member.name,
            initials = Initials.ofMember(member.name, member.isMe),
            avatarColor = member.avatarColor,
            photoUrl = member.photoUrl,
            isMe = member.isMe,
            isNew = false,
            isIncluded = mode != DefaultSplitMode.EQUAL || weight > 0,
            parts = if (mode == DefaultSplitMode.SHARES) weight else 1L,
            percentText = if (mode == DefaultSplitMode.PERCENT ||
                mode == DefaultSplitMode.INCOME
            ) {
                weight.toString()
            } else {
                ""
            },
            incomeText = "",
        )
    }

    private fun DefaultSplitPerson.withMember(member: GroupMember): DefaultSplitPerson = copy(
        name = member.name,
        initials = Initials.ofMember(member.name, member.isMe),
        avatarColor = member.avatarColor,
        isMe = member.isMe,
    )

    /*
     * Why: a new way to split starts from who is in it now, everyone when nobody is: "Igual" with them checked,
     * "Partes" with one each, "Porcentagem" with 100 shared as evenly as whole numbers allow, and "Pela renda" with
     * those percentages until an income is typed.
     */
    private fun DefaultSplitUiState.toMode(mode: DefaultSplitMode): DefaultSplitUiState {
        if (mode == this.mode) return this
        val inSplit = weights.filterValues { it > 0 }.keys.ifEmpty { people.map { it.id }.toSet() }
        val percents = if (usesPercent && addsUp) {
            weights
        } else {
            IncomePercents.of(people.map { it.id to if (it.id in inSplit) 1L else 0L })
        }
        return copy(
            mode = mode,
            people = people.map { person ->
                val isIn = person.id in inSplit
                person.copy(
                    isIncluded = isIn,
                    parts = if (isIn) 1L else 0L,
                    percentText = percents[person.id]?.takeIf { isIn }?.toString() ?: "0",
                )
            },
            incomeFallback = if (mode == DefaultSplitMode.INCOME) percents else incomeFallback,
        )
    }

    private fun updatePerson(
        memberId: String,
        change: (DefaultSplitPerson) -> DefaultSplitPerson,
    ) {
        updateForm { state ->
            state.copy(people = state.people.map { if (it.id == memberId) change(it) else it })
        }
    }

    private fun save() {
        val state = uiState.value
        if (!state.canSave) return
        uiState.update {
            it.copy(
                isSaving = true,
                showsSaveError = false,
            )
        }
        viewModelScope.launch {
            updateDefaultSplit(
                groupId = route.groupId,
                defaultSplit = state.defaultSplit,
                incomes = state.incomes,
            ).onSuccess {
                // Why: the screen may not be on top to leave, so it starts from what it just saved.
                opened = Opened(
                    split = state.defaultSplit,
                    // Why: as isChangedFrom compares them, or an income typed as 0 keeps Salvar on after saving.
                    incomes = state.incomes.filterValues { it > 0 },
                )
                updateForm { current ->
                    current.copy(
                        isSaving = false,
                        people = current.people.map { it.copy(isNew = false) },
                    )
                }
                leave()
            }.onFailure {
                uiState.update {
                    it.copy(
                        isSaving = false,
                        showsSaveError = true,
                    )
                }
            }
        }
    }

    /*
     * Why: every change of the form works out what it gives once, in the same copy that says whether it still holds
     * what it opened with, which enables Salvar.
     */
    private fun updateForm(transform: (DefaultSplitUiState) -> DefaultSplitUiState) {
        uiState.update { state ->
            val next = transform(state)
            val figures = DefaultSplitFigures.of(
                next.mode,
                next.people,
                next.incomeFallback,
                next.currency,
                next.showsSats,
            )
            next.copy(
                figures = figures,
                hasChanges = next.isChangedFrom(opened, figures),
            )
        }
    }

    // Why: someone who joined since it was saved is a change too, so saving takes them in and the notice goes.
    private fun DefaultSplitUiState.isChangedFrom(
        opened: Opened?,
        figures: DefaultSplitFigures,
    ): Boolean {
        if (opened == null) return false
        val split = GroupDefaultSplit(mode, figures.weights, exceptions)
        return split.mode != opened.split.mode ||
            people.any { split.weightOf(it.id) != opened.split.weightOf(it.id) } ||
            people.any { it.isNew } ||
            exceptions != opened.split.exceptions ||
            (mode == DefaultSplitMode.INCOME && figures.incomes.filterValues { it > 0 } != opened.incomes)
    }

    private fun formatAmount(
        amountMinor: Long,
        currency: GroupCurrency,
        showsSats: Boolean,
    ): String = AmountInput.format(
        amountMinor = amountMinor,
        currency = currency,
        showsSats = showsSats,
        decimalSeparator = TypedText.amountSeparatorOf(decimalSeparator()),
    )

    // Why: only while it is still on top: a native back (a swipe, the browser's, a sheet dismissed) may have closed it.
    private fun leave() {
        if (navigator.backStack.value.lastOrNull() == route) navigator.navigateBack()
    }

    private data class Opened(
        val split: GroupDefaultSplit,
        val incomes: Map<String, Long>,
    )
}
