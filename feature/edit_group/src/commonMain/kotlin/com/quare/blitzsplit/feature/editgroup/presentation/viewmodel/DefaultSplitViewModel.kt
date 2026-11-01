package com.quare.blitzsplit.feature.editgroup.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
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
import com.quare.blitzsplit.core.utils.DeviceDecimalSeparator
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitPerson
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<DefaultSplitUiState>
        field = MutableStateFlow(
            DefaultSplitUiState(
                groupName = "",
                currency = GroupCurrency.fallback,
                mode = DefaultSplitMode.EQUAL,
                people = emptyList(),
                incomeFallback = emptyMap(),
                isOnline = true,
                isLoaded = false,
                isSaving = false,
                showsSaveError = false,
                hasChanges = false,
            ),
        )

    val stateWatcher: FlowWatcher<DefaultSplitUiState> = FlowWatcher(uiState)

    private val amountSeparators = setOf(',', '.')

    // Why: the group went while another screen covered this one, which then leaves as soon as it is on top again.
    private var isGroupGone = false

    // Why: what the screen opened with, so Salvar only turns on once something differs from it.
    private var opened: Opened? = null

    /*
     * Why: the form is filled from the group once, so a sync landing while someone types changes nothing here; only
     * the people follow it: who joins comes in as the rules start them, who is removed goes.
     */
    init {
        observeGroup(route.groupId)
            .onEach { group ->
                isGroupGone = group == null
                if (group == null) leave() else show(group)
            }.launchIn(viewModelScope)
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
                it.copy(percentText = event.text.filter(Char::isDigit).take(PERCENT_MAX_LENGTH))
            }

            is DefaultSplitUiEvent.OnIncomeChanged -> updatePerson(event.memberId) {
                it.copy(
                    incomeText = event.text
                        .filter { char ->
                            char.isDigit() || char in amountSeparators
                        }.take(AMOUNT_MAX_LENGTH),
                )
            }

            DefaultSplitUiEvent.OnSaveClicked -> save()
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
            val byId = state.people.associateBy { it.id }
            state.copy(
                groupName = group.name,
                currency = group.currency,
                people = group.members.map { member ->
                    byId[member.id]?.withMember(member) ?: personOf(
                        member = member,
                        saved = GroupDefaultSplit(state.mode, emptyMap()),
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
                isLoaded = true,
            )
        }
        viewModelScope.launch {
            val incomes = getSplitIncomes(route.groupId).getOrDefault(emptyMap())
            if (incomes.isEmpty()) return@launch
            opened = opened?.copy(incomes = incomes.filterValues { it > 0 })
            updateForm { state ->
                state.copy(
                    people = state.people.map { person ->
                        val income = incomes[person.id]
                        if (income == null || person.incomeText.isNotEmpty()) {
                            person
                        } else {
                            person.copy(incomeText = formatAmount(income, state.currency))
                        }
                    },
                )
            }
        }
    }

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
                    incomes = state.incomes,
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

    // Why: every change says whether the form still holds what it opened with, which enables Salvar.
    private fun updateForm(transform: (DefaultSplitUiState) -> DefaultSplitUiState) {
        uiState.update { state ->
            val next = transform(state)
            next.copy(hasChanges = next.isChangedFrom(opened))
        }
    }

    // Why: someone who joined since it was saved is a change too, so saving takes them in and the notice goes.
    private fun DefaultSplitUiState.isChangedFrom(opened: Opened?): Boolean {
        if (opened == null) return false
        val split = defaultSplit
        return split.mode != opened.split.mode ||
            people.any { split.weightOf(it.id) != opened.split.weightOf(it.id) } ||
            people.any { it.isNew } ||
            (mode == DefaultSplitMode.INCOME && incomes.filterValues { it > 0 } != opened.incomes)
    }

    private fun formatAmount(
        amountMinor: Long,
        currency: GroupCurrency,
    ): String = AmountInput.format(
        amountMinor = amountMinor,
        currency = currency,
        decimalSeparator = decimalSeparator().takeIf { it in amountSeparators } ?: '.',
    )

    // Why: only while it is still on top: a native back (a swipe, the browser's, a sheet dismissed) may have closed it.
    private fun leave() {
        if (navigator.backStack.value.lastOrNull() == route) navigator.navigateBack()
    }

    private data class Opened(
        val split: GroupDefaultSplit,
        val incomes: Map<String, Long>,
    )

    private companion object {
        const val PERCENT_MAX_LENGTH = 3

        // Why: "100.000.000,00" with room for the separators someone types.
        const val AMOUNT_MAX_LENGTH = 16
    }
}
