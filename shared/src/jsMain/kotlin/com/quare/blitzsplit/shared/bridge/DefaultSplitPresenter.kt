@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitPerson
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitRow
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.DefaultSplitViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

/*
 * Why: one person of "Divisão padrão" with what they put in it now. basisPoints is their part of every bill in
 * hundredths of a percent, exampleMinor their part of the example bill (null while nobody is in it).
 */
class JsDefaultSplitRow(
    val id: String,
    val name: String,
    val initials: String,
    val color: String,
    val isMe: Boolean,
    val isNew: Boolean,
    val isIncluded: Boolean,
    val parts: Double,
    val percentText: String,
    val incomeText: String,
    val weight: Double,
    val basisPoints: Double,
    val exampleMinor: Double?,
)

class JsDefaultSplitState(
    val groupName: String,
    // Why: the ISO 4217 code ("BRL") and its digits after the decimal separator.
    val currency: String,
    val fractionDigits: Int,
    // Why: the DefaultSplitMode's code ("equal", "shares", "percent", "income"); modes in the order they are drawn.
    val mode: String,
    val modes: Array<String>,
    val rows: Array<JsDefaultSplitRow>,
    val includedCount: Int,
    val exampleTotalMinor: Double,
    val exampleEachMinor: Double,
    val percentSum: Double,
    val remainingPercent: Double,
    val addsUp: Boolean,
    val showsPercentError: Boolean,
    // Why: "Carla entrou no grupo", the names of whoever joined since the split was saved.
    val newcomers: Array<String>,
    val isLoaded: Boolean,
    val isSaving: Boolean,
    val showsSaveError: Boolean,
    val canSave: Boolean,
    val showsOfflineNotice: Boolean,
)

class DefaultSplitPresenter(
    route: JsRoute,
) {
    private val splitRoute = route.route as Route.DefaultSplit
    private val holder = ViewModelHolder(DefaultSplitViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(splitRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsDefaultSplitState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsDefaultSplitState(
                groupName = state.groupName,
                currency = state.currency.code,
                fractionDigits = state.currency.fractionDigits,
                mode = state.mode.code,
                modes = state.modes.map { it.code }.toTypedArray(),
                rows = state.rows.map(::toJsRow).toTypedArray(),
                includedCount = state.includedCount,
                exampleTotalMinor = state.exampleTotalMinor.toDouble(),
                exampleEachMinor = state.exampleEachMinor.toDouble(),
                percentSum = state.percentSum.toDouble(),
                remainingPercent = state.remainingPercent.toDouble(),
                addsUp = state.addsUp,
                showsPercentError = state.showsPercentError,
                newcomers = state.newcomers.map { it.name }.toTypedArray(),
                isLoaded = state.isLoaded,
                isSaving = state.isSaving,
                showsSaveError = state.showsSaveError,
                canSave = state.canSave,
                showsOfflineNotice = state.showsOfflineNotice,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: the ViewModel keeps only what a field may hold, so the field reads it back at once.
    fun getPercentText(memberId: String): String = personOf(memberId)?.percentText.orEmpty()

    fun getIncomeText(memberId: String): String = personOf(memberId)?.incomeText.orEmpty()

    fun back() {
        viewModel.onEvent(DefaultSplitUiEvent.OnBackClicked)
    }

    fun selectMode(code: String) {
        val mode = DefaultSplitMode.fromCodeOrNull(code) ?: return
        viewModel.onEvent(DefaultSplitUiEvent.OnModeSelected(mode))
    }

    fun togglePerson(memberId: String) {
        viewModel.onEvent(DefaultSplitUiEvent.OnPersonToggled(memberId))
    }

    fun increaseParts(memberId: String) {
        viewModel.onEvent(DefaultSplitUiEvent.OnPartsIncreased(memberId))
    }

    fun decreaseParts(memberId: String) {
        viewModel.onEvent(DefaultSplitUiEvent.OnPartsDecreased(memberId))
    }

    fun changePercent(
        memberId: String,
        text: String,
    ) {
        viewModel.onEvent(DefaultSplitUiEvent.OnPercentChanged(memberId, text))
    }

    fun changeIncome(
        memberId: String,
        text: String,
    ) {
        viewModel.onEvent(DefaultSplitUiEvent.OnIncomeChanged(memberId, text))
    }

    fun save() {
        viewModel.onEvent(DefaultSplitUiEvent.OnSaveClicked)
    }

    fun dispose() {
        holder.clear()
    }

    private fun personOf(memberId: String): DefaultSplitPerson? = viewModel.uiState.value.people
        .firstOrNull { it.id == memberId }
}

private fun toJsRow(row: DefaultSplitRow): JsDefaultSplitRow = JsDefaultSplitRow(
    id = row.person.id,
    name = row.person.name,
    initials = row.person.initials,
    color = row.person.avatarColor.code,
    isMe = row.person.isMe,
    isNew = row.person.isNew,
    isIncluded = row.person.isIncluded,
    parts = row.person.parts.toDouble(),
    percentText = row.person.percentText,
    incomeText = row.person.incomeText,
    weight = row.weight.toDouble(),
    basisPoints = row.basisPoints.toDouble(),
    exampleMinor = row.exampleMinor?.toDouble(),
)
