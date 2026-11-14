package com.quare.blitzsplit.core.expenses.customcolor

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTint
import com.quare.blitzsplit.core.expenses.domain.model.ColorDraft
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.model.CustomPaletteFullException
import com.quare.blitzsplit.core.expenses.domain.model.tint
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Why: the person's own colors (Pierre's beta feedback of 2026-11-14) do the same in every category color picker: "+"
 * opens "New color" (a gradient, or a hex or RGB code), whose "Add" puts the color in the palette and gives it to the
 * category; a long press on one of them asks "Delete this color?", or says which category uses it and deletes nothing.
 * The screen says what picking a color does (onPicked, with the app category's key "+" was opened for, or null for its
 * form), and sends its own Analytics.
 */
class CustomColorController(
    private val useCases: CustomColorUseCases,
    private val scope: CoroutineScope,
    private val onPicked: (tint: CategoryTint, categoryKey: String?) -> Unit,
) {
    val state: StateFlow<CustomColorState>
        field = MutableStateFlow(CustomColorState.idle)

    // Why: the app category "+" was opened for, whose color "Add" changes; null for the screen's form.
    private var draftCategoryKey: String? = null

    // Why: the add in flight, which a second "+" waits for.
    private var saveJob: Job? = null

    // Why: how often a picker closed, so a save failing after its picker closed never opens "New color" again.
    private var closedCount = 0

    fun observe() {
        useCases
            .observeCustomColors()
            .onEach { palette ->
                state.update { current ->
                    // Why: a color deleted elsewhere, by a sync, closes a question still asked about it.
                    val deletion = current.deletion?.takeIf { it.color in palette || it.isDeleting }
                    current.copy(
                        palette = palette,
                        deletion = deletion,
                    )
                }
            }.launchIn(scope)
    }

    fun onEvent(event: CustomColorEvent) {
        when (event) {
            is CustomColorEvent.OnAddClicked -> openDraft(event.current, event.categoryKey)

            is CustomColorEvent.OnHsvChanged -> changeDraft { it.withHsv(event.hue, event.saturation, event.value) }

            is CustomColorEvent.OnHexChanged -> changeDraft { it.withHexText(event.text) }

            is CustomColorEvent.OnChannelChanged -> changeDraft { it.withChannelText(event.channel, event.text) }

            CustomColorEvent.OnSaveClicked -> save()

            CustomColorEvent.OnDraftDismissed -> state.update { it.copy(draft = null) }

            is CustomColorEvent.OnLongPressed -> askToDelete(event.color)

            CustomColorEvent.OnDeleteConfirmed -> delete()

            CustomColorEvent.OnDeletionDismissed -> state.update {
                if (it.deletion?.isDeleting == true) it else it.copy(deletion = null)
            }

            CustomColorEvent.OnPickerClosed -> close()
        }
    }

    /*
     * Why: "New color" and the questions belong to the picker that opened them: once it closes (its form, the bill's
     * Category, "Change color of Food", or a rotation that took its dialog), they go too, so the next picker never opens
     * on them nor sends a color to the earlier category. A deletion in flight finishes on its own.
     */
    fun close() {
        closedCount++
        state.update {
            it.copy(
                draft = null,
                deletion = it.deletion?.takeIf { deletion -> deletion.isDeleting },
            )
        }
    }

    // Why: "+" never opens over a full palette, nor twice.
    private fun openDraft(
        current: CategoryTint,
        categoryKey: String?,
    ) {
        val now = state.value
        // Why: one add at a time, so two never both pass the palette's limit before Room shows the first.
        if (!now.canAdd || now.draft != null || saveJob?.isActive == true) return
        draftCategoryKey = categoryKey
        state.update { it.copy(draft = ColorDraft.createStartingAt(current)) }
    }

    private fun changeDraft(transform: (ColorDraft) -> ColorDraft) {
        state.update { current -> current.copy(draft = current.draft?.let(transform)) }
    }

    /*
     * Why: the picker closes at once; the palette reads the color back from Room. A failed write opens it again, unless
     * another "New color" opened meanwhile, its picker closed (a later picker would open on it), or the palette
     * filled up (a sync brought colors), whose note says so.
     */
    private fun save() {
        val draft = state.value.draft ?: return
        if (!draft.canSave) return
        val color = draft.color
        val categoryKey = draftCategoryKey
        val closedBefore = closedCount
        state.update { it.copy(draft = null) }
        saveJob = scope.launch {
            useCases
                .addCustomColor(color)
                .onSuccess {
                    // Why: a form that closed meanwhile never asked for it; an app category still gets what was picked.
                    if (categoryKey != null || closedCount == closedBefore) onPicked(color.tint, categoryKey)
                }.onFailure { error ->
                    if (error is CustomPaletteFullException) return@onFailure
                    state.update {
                        val isSamePicker = draftCategoryKey == categoryKey && closedCount == closedBefore
                        if (it.draft == null && isSamePicker) it.copy(draft = draft) else it
                    }
                }
        }
    }

    private fun askToDelete(color: CustomColor) {
        if (!state.value.isDeletable(color) || state.value.deletion != null) return
        scope.launch {
            // Why: a read that fails asks nothing, as if the long press hadn't landed.
            val usedBy = suspendRunCatching { useCases.findCustomColorUser(color) }.getOrElse { return@launch }
            state.update { current ->
                if (current.deletion != null || color !in current.palette) {
                    current
                } else {
                    current.copy(
                        deletion = CustomColorDeletion(
                            color = color,
                            usedBy = usedBy,
                            isDeleting = false,
                        ),
                    )
                }
            }
        }
    }

    // Why: asked again at the "Delete", as a sync may have given the color to a category since the question opened.
    private fun delete() {
        val deletion = state.value.deletion ?: return
        if (deletion.isBlocked || deletion.isDeleting) return
        state.update { it.copy(deletion = deletion.copy(isDeleting = true)) }
        scope.launch {
            // Why: a check that fails deletes nothing; the question stays for another try or "Cancel".
            val usedBy = suspendRunCatching { useCases.findCustomColorUser(deletion.color) }.getOrElse {
                state.update { it.copy(deletion = deletion) }
                return@launch
            }
            if (usedBy != null) {
                state.update { it.copy(deletion = deletion.copy(usedBy = usedBy)) }
                return@launch
            }
            useCases
                .deleteCustomColor(deletion.color)
                .onSuccess { state.update { it.copy(deletion = null) } }
                .onFailure { state.update { it.copy(deletion = deletion) } }
        }
    }
}
