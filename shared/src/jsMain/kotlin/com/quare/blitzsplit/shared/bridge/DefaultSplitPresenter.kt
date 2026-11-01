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
    val joinedPeople: Array<String>,
    val isLoaded: Boolean,
    val isSaving: Boolean,
    val showsSaveError: Boolean,
    val canSave: Boolean,
    val showsOfflineNotice: Boolean,
    // Why: "Exceções por categoria" (6.7f) and the sheet of one, null while it is closed.
    val exceptions: Array<JsSplitException>,
    val exceptionEditor: JsExceptionEditor?,
    val exceptionMains: Array<JsCategory>,
    val exceptionSubcategories: Array<JsCategory>,
    val exceptionCategory: JsCategory?,
    val isExceptionCategoryTaken: Boolean,
    val canSaveException: Boolean,
    val exceptionModes: Array<String>,
)

// Why: one exception; mode is the DefaultSplitMode's code, values the weights of who is in it ("2 para 1").
class JsSplitException(
    val categoryId: String,
    val category: JsCategory,
    val mode: String,
    val values: Array<Double>,
    val includedCount: Int,
    val totalCount: Int,
)

class JsExceptionEditor(
    val editedCategoryId: String?,
    val categoryId: String?,
    val openMainId: String?,
    val mode: String,
    val rows: Array<JsDefaultSplitRow>,
    val exampleTotalMinor: Double,
    val percentSum: Double,
    val remainingPercent: Double,
    val showsPercentError: Boolean,
    val addsUp: Boolean,
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
                joinedPeople = state.joinedPeople.map { it.name }.toTypedArray(),
                isLoaded = state.isLoaded,
                isSaving = state.isSaving,
                showsSaveError = state.showsSaveError,
                canSave = state.canSave,
                showsOfflineNotice = state.showsOfflineNotice,
                exceptions = state.exceptionRows
                    .map { row ->
                        JsSplitException(
                            categoryId = row.exception.categoryId,
                            category = toJsCategory(row.category),
                            mode = row.exception.mode.code,
                            values = row.valuesOf(state.people.map { it.id }).map { it.toDouble() }.toTypedArray(),
                            includedCount = row.includedCount,
                            totalCount = row.totalCount,
                        )
                    }.toTypedArray(),
                exceptionEditor = state.exceptionEditor?.let { editor ->
                    JsExceptionEditor(
                        editedCategoryId = editor.editedCategoryId,
                        categoryId = editor.categoryId,
                        openMainId = editor.openMainId,
                        mode = editor.mode.code,
                        rows = editor.figures.rows.map(::toJsRow).toTypedArray(),
                        exampleTotalMinor = editor.figures.exampleTotalMinor.toDouble(),
                        percentSum = editor.percentSum.toDouble(),
                        remainingPercent = editor.remainingPercent.toDouble(),
                        showsPercentError = editor.showsPercentError,
                        addsUp = editor.addsUp,
                    )
                },
                exceptionMains = state.exceptionMains.map(::toJsCategory).toTypedArray(),
                exceptionSubcategories = state.exceptionSubcategories.map(::toJsCategory).toTypedArray(),
                exceptionCategory = state.exceptionCategory?.let(::toJsCategory),
                isExceptionCategoryTaken = state.isExceptionCategoryTaken,
                canSaveException = state.canSaveException,
                exceptionModes = state.exceptionModes.map { it.code }.toTypedArray(),
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

    fun addException() {
        viewModel.onEvent(DefaultSplitUiEvent.OnAddExceptionClicked)
    }

    fun openException(categoryId: String) {
        viewModel.onEvent(DefaultSplitUiEvent.OnExceptionClicked(categoryId))
    }

    fun pickExceptionCategory(categoryId: String) {
        viewModel.onEvent(DefaultSplitUiEvent.OnExceptionCategoryPicked(categoryId))
    }

    fun selectExceptionMode(code: String) {
        val mode = DefaultSplitMode.fromCodeOrNull(code) ?: return
        viewModel.onEvent(DefaultSplitUiEvent.OnExceptionModeSelected(mode))
    }

    fun toggleExceptionPerson(memberId: String) {
        viewModel.onEvent(DefaultSplitUiEvent.OnExceptionPersonToggled(memberId))
    }

    fun increaseExceptionParts(memberId: String) {
        viewModel.onEvent(DefaultSplitUiEvent.OnExceptionPartsIncreased(memberId))
    }

    fun decreaseExceptionParts(memberId: String) {
        viewModel.onEvent(DefaultSplitUiEvent.OnExceptionPartsDecreased(memberId))
    }

    fun changeExceptionPercent(
        memberId: String,
        text: String,
    ) {
        viewModel.onEvent(DefaultSplitUiEvent.OnExceptionPercentChanged(memberId, text))
    }

    fun getExceptionPercentText(memberId: String): String = viewModel.uiState.value.exceptionEditor
        ?.people
        ?.firstOrNull { it.id == memberId }
        ?.percentText
        .orEmpty()

    fun saveException() {
        viewModel.onEvent(DefaultSplitUiEvent.OnExceptionSaveClicked)
    }

    fun removeException() {
        viewModel.onEvent(DefaultSplitUiEvent.OnExceptionRemoveClicked)
    }

    fun dismissException() {
        viewModel.onEvent(DefaultSplitUiEvent.OnExceptionDismissed)
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
