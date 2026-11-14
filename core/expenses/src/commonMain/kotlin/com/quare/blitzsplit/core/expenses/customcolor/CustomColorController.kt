package com.quare.blitzsplit.core.expenses.customcolor

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTint
import com.quare.blitzsplit.core.expenses.domain.model.ColorDraft
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.model.tint
import kotlinx.coroutines.CoroutineScope
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
        }
    }

    // Why: "+" never opens over a full palette, nor twice.
    private fun openDraft(
        current: CategoryTint,
        categoryKey: String?,
    ) {
        val now = state.value
        if (!now.canAdd || now.draft != null) return
        draftCategoryKey = categoryKey
        state.update { it.copy(draft = ColorDraft.createStartingAt(current)) }
    }

    private fun changeDraft(transform: (ColorDraft) -> ColorDraft) {
        state.update { current -> current.copy(draft = current.draft?.let(transform)) }
    }

    // Why: the picker closes at once; the palette reads the color back from Room. A failed write keeps the picker open.
    private fun save() {
        val draft = state.value.draft ?: return
        if (!draft.canSave) return
        val color = draft.color
        val categoryKey = draftCategoryKey
        state.update { it.copy(draft = null) }
        scope.launch {
            useCases
                .addCustomColor(color)
                .onSuccess { onPicked(color.tint, categoryKey) }
                .onFailure { state.update { it.copy(draft = draft) } }
        }
    }

    private fun askToDelete(color: CustomColor) {
        if (!state.value.isDeletable(color) || state.value.deletion != null) return
        scope.launch {
            val usedBy = useCases.findCustomColorUser(color)
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
            val usedBy = useCases.findCustomColorUser(deletion.color)
            if (usedBy != null) {
                state.update { it.copy(deletion = deletion.copy(usedBy = usedBy)) }
                return@launch
            }
            useCases.deleteCustomColor(deletion.color)
            state.update { it.copy(deletion = null) }
        }
    }
}
