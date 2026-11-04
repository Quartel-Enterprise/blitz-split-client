package com.quare.blitzsplit.core.presentation.analytics

import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TrackedViewModelTest {
    private lateinit var log: MutableList<Entry>
    private lateinit var viewModel: TestViewModel

    @Test
    fun `GIVEN an automatic event with params WHEN the UI sends it THEN tracks exactly its name and params once`() {
        // Given
        prepareScenario()
        val event = TestUiEvent.OnSaveClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(
            listOf(Entry.Tracked("bill_saved", mapOf("is_new" to true, "people" to 3))),
            log.filterIsInstance<Entry.Tracked>(),
        )
    }

    @Test
    fun `GIVEN an automatic event with params WHEN the UI sends it THEN tracks it before the ViewModel handles it`() {
        // Given
        prepareScenario()
        val event = TestUiEvent.OnSaveClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(
            listOf<Entry>(
                Entry.Tracked("bill_saved", mapOf("is_new" to true, "people" to 3)),
                Entry.Handled(TestUiEvent.OnSaveClicked),
            ),
            log,
        )
    }

    @Test
    fun `GIVEN an automatic event without params WHEN the UI sends it THEN tracks it with no params and handles it`() {
        // Given
        prepareScenario()
        val event = TestUiEvent.OnBackClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(
            listOf<Entry>(Entry.Tracked("back_clicked", emptyMap()), Entry.Handled(TestUiEvent.OnBackClicked)),
            log,
        )
    }

    @Test
    fun `GIVEN a handler that fails WHEN the UI sends an automatic event THEN the tap was already tracked`() {
        // Given
        prepareScenario(failingHandler = true)
        val event = TestUiEvent.OnBackClicked

        // When
        val failure = assertFailsWith<IllegalStateException> { viewModel.onEvent(event) }

        // Then
        assertEquals("handler failed", failure.message)
        assertEquals(listOf(Entry.Tracked("back_clicked", emptyMap())), log.filterIsInstance<Entry.Tracked>())
    }

    @Test
    fun `GIVEN a manual event the handler doesnt track WHEN the UI sends it THEN tracks nothing and handles it`() {
        // Given
        prepareScenario()
        val event = TestUiEvent.OnDeleteConfirmed(handlerTracks = false)

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf<Entry>(Entry.Handled(TestUiEvent.OnDeleteConfirmed(handlerTracks = false))), log)
    }

    @Test
    fun `GIVEN a manual event the handler tracks WHEN the UI sends it THEN tracks only what the handler sent`() {
        // Given
        prepareScenario()
        val event = TestUiEvent.OnDeleteConfirmed(handlerTracks = true)

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(
            listOf<Entry>(
                Entry.Handled(TestUiEvent.OnDeleteConfirmed(handlerTracks = true)),
                Entry.Tracked("bill_deleted", mapOf("had_payment" to false)),
            ),
            log,
        )
    }

    @Test
    fun `GIVEN an event that is not tracked WHEN the UI sends it THEN tracks nothing and handles it`() {
        // Given
        prepareScenario()
        val event = TestUiEvent.OnListScrolled

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf<Entry>(Entry.Handled(TestUiEvent.OnListScrolled)), log)
    }

    @Test
    fun `GIVEN an automatic event already sent WHEN the UI sends it again THEN tracks it once per send`() {
        // Given
        prepareScenario()
        viewModel.onEvent(TestUiEvent.OnBackClicked)
        val event = TestUiEvent.OnBackClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(
            listOf<Entry>(
                Entry.Tracked("back_clicked", emptyMap()),
                Entry.Handled(TestUiEvent.OnBackClicked),
                Entry.Tracked("back_clicked", emptyMap()),
                Entry.Handled(TestUiEvent.OnBackClicked),
            ),
            log,
        )
    }

    @Test
    fun `GIVEN a screen refusing the event for now WHEN the UI sends it THEN neither tracks nor handles it`() {
        // Given
        prepareScenario(refusesEvents = true)
        val event = TestUiEvent.OnBackClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertTrue(log.isEmpty())
    }

    private fun prepareScenario(
        failingHandler: Boolean = false,
        refusesEvents: Boolean = false,
    ) {
        log = mutableListOf()
        viewModel =
            TestViewModel(
                trackEvent = { name, params -> log += Entry.Tracked(name, params) },
                log = log,
                failingHandler = failingHandler,
                refusesEvents = refusesEvents,
            )
    }
}

private sealed interface Entry {
    data class Tracked(
        val name: String,
        val params: Map<String, Any>,
    ) : Entry

    data class Handled(
        val event: TestUiEvent,
    ) : Entry
}

private sealed interface TestUiEvent : TrackedUiEvent {
    data object OnSaveClicked : TestUiEvent {
        override val analytics = EventAnalytics.Automatic("bill_saved", mapOf("is_new" to true, "people" to 3))
    }

    data object OnBackClicked : TestUiEvent {
        override val analytics = EventAnalytics.Automatic("back_clicked")
    }

    data class OnDeleteConfirmed(
        val handlerTracks: Boolean,
    ) : TestUiEvent {
        override val analytics = EventAnalytics.Manual("bill_deleted")
    }

    data object OnListScrolled : TestUiEvent {
        override val analytics = EventAnalytics.NotTracked
    }
}

private class TestViewModel(
    private val log: MutableList<Entry>,
    private val failingHandler: Boolean,
    private val refusesEvents: Boolean,
    trackEvent: TrackEvent,
) : TrackedViewModel<TestUiEvent>(trackEvent) {
    override fun isAccepted(event: TestUiEvent): Boolean = !refusesEvents

    override fun handleEvent(event: TestUiEvent) {
        log += Entry.Handled(event)
        if (failingHandler) error("handler failed")
        if (event is TestUiEvent.OnDeleteConfirmed && event.handlerTracks) {
            trackEvent("bill_deleted", mapOf("had_payment" to false))
        }
    }
}
