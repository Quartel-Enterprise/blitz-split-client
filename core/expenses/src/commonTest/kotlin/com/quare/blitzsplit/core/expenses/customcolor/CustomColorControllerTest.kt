package com.quare.blitzsplit.core.expenses.customcolor

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTint
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.ColorDraft
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.model.CustomPalette
import com.quare.blitzsplit.core.expenses.domain.model.CustomPaletteFullException
import com.quare.blitzsplit.core.expenses.domain.model.tint
import com.quare.blitzsplit.core.expenses.testing.FakeCustomColorRepository
import com.quare.blitzsplit.core.expenses.testing.createFakeCustomColorUseCases
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CustomColorControllerTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeCustomColorRepository
    private lateinit var controller: CustomColorController
    private val picked = mutableListOf<Pair<CategoryTint, String?>>()
    private val usedBy = mutableMapOf<CustomColor, Category>()

    private val blue = CustomColor(0x3A7BD5)
    private val yellow = CustomColor(0xFFF59D)
    private val navy = CustomColor(0x1A2340)
    private val food: Category = CategoryTree.appOnly.findOrNull(AppCategory.FOOD.key)!!

    @Test
    fun `GIVEN colors in the palette WHEN observing THEN the state shows them`() = runTest(testDispatcher) {
        // Given
        prepareScenario(palette = listOf(blue, yellow))

        // When
        controller.observe()

        // Then
        assertEquals(listOf(blue, yellow), controller.state.value.palette)
    }

    @Test
    fun `GIVEN the palette observed WHEN a color is added elsewhere THEN the state follows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(palette = listOf(blue))
            controller.observe()

            // When
            repository.colors.value = listOf(blue, navy)
            runCurrent()

            // Then
            assertEquals(listOf(blue, navy), controller.state.value.palette)
        }

    @Test
    fun `GIVEN a category in teal WHEN tapping plus THEN New color opens at teal`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = CustomColorEvent.OnAddClicked(CategoryColor.TEAL.tint, AppCategory.FOOD.key)

        // When
        controller.onEvent(event)

        // Then
        assertEquals(ColorDraft.createStartingAt(CategoryColor.TEAL.tint), controller.state.value.draft)
    }

    @Test
    fun `GIVEN a full palette WHEN tapping plus THEN New color does not open`() = runTest(testDispatcher) {
        // Given
        prepareScenario(palette = List(CustomPalette.MAX_COLORS) { CustomColor(it + 1) })
        controller.observe()
        val event = CustomColorEvent.OnAddClicked(CategoryColor.TEAL.tint, null)

        // When
        controller.onEvent(event)

        // Then
        assertNull(controller.state.value.draft)
    }

    @Test
    fun `GIVEN New color open WHEN tapping plus again THEN the open draft stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        controller.onEvent(CustomColorEvent.OnAddClicked(CategoryColor.TEAL.tint, AppCategory.FOOD.key))
        val again = CustomColorEvent.OnAddClicked(blue.tint, AppCategory.HOME.key)

        // When
        controller.onEvent(again)

        // Then
        assertEquals(ColorDraft.createStartingAt(CategoryColor.TEAL.tint), controller.state.value.draft)
    }

    @Test
    fun `GIVEN New color opened for Food then plus tapped for Home WHEN tapping Add THEN Food gets the color`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            controller.onEvent(CustomColorEvent.OnAddClicked(CategoryColor.TEAL.tint, AppCategory.FOOD.key))
            controller.onEvent(CustomColorEvent.OnAddClicked(blue.tint, AppCategory.HOME.key))

            // When
            controller.onEvent(CustomColorEvent.OnSaveClicked)
            runCurrent()

            // Then
            assertEquals(listOf(AppCategory.FOOD.key), picked.map { it.second })
        }

    @Test
    fun `GIVEN New color open for Food WHEN tapping Add THEN the color joins the palette goes to Food and closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            controller.observe()
            controller.onEvent(CustomColorEvent.OnAddClicked(CategoryColor.TEAL.tint, AppCategory.FOOD.key))
            controller.onEvent(CustomColorEvent.OnHexChanged("#3a7bd5"))

            // When
            controller.onEvent(CustomColorEvent.OnSaveClicked)
            runCurrent()

            // Then
            assertEquals(listOf(blue), repository.addedAt.map { it.first })
            assertEquals(listOf(blue), controller.state.value.palette)
            assertEquals(listOf<Pair<CategoryTint, String?>>(blue.tint to AppCategory.FOOD.key), picked)
            assertNull(controller.state.value.draft)
        }

    @Test
    fun `GIVEN New color open from a form WHEN tapping Add THEN the form gets the color with no category key`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            controller.onEvent(CustomColorEvent.OnAddClicked(CategoryColor.TEAL.tint, null))
            controller.onEvent(CustomColorEvent.OnHexChanged("#1A2340"))

            // When
            controller.onEvent(CustomColorEvent.OnSaveClicked)
            runCurrent()

            // Then
            assertEquals(listOf<Pair<CategoryTint, String?>>(navy.tint to null), picked)
        }

    @Test
    fun `GIVEN the device fails to save WHEN tapping Add THEN New color opens again with the draft`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(saveError = IllegalStateException("disk full"))
            controller.onEvent(CustomColorEvent.OnAddClicked(CategoryColor.TEAL.tint, null))
            controller.onEvent(CustomColorEvent.OnHexChanged("#3A7BD5"))
            val draft = controller.state.value.draft

            // When
            controller.onEvent(CustomColorEvent.OnSaveClicked)
            runCurrent()

            // Then
            assertEquals(draft, controller.state.value.draft)
            assertTrue(picked.isEmpty())
        }

    @Test
    fun `GIVEN half a hex code typed WHEN tapping Add THEN nothing is saved and New color stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            controller.onEvent(CustomColorEvent.OnAddClicked(CategoryColor.TEAL.tint, null))
            controller.onEvent(CustomColorEvent.OnHexChanged("#3A7"))
            val draft = controller.state.value.draft

            // When
            controller.onEvent(CustomColorEvent.OnSaveClicked)
            runCurrent()

            // Then
            assertEquals(draft, controller.state.value.draft)
            assertTrue(repository.addedAt.isEmpty())
            assertTrue(picked.isEmpty())
        }

    @Test
    fun `GIVEN New color open WHEN cancelling THEN it closes and saves nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        controller.onEvent(CustomColorEvent.OnAddClicked(CategoryColor.TEAL.tint, null))
        val event = CustomColorEvent.OnDraftDismissed

        // When
        controller.onEvent(event)

        // Then
        assertNull(controller.state.value.draft)
        assertTrue(repository.addedAt.isEmpty())
    }

    @Test
    fun `GIVEN New color open WHEN dragging the gradient THEN the draft follows`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        controller.onEvent(CustomColorEvent.OnAddClicked(CategoryColor.TEAL.tint, null))
        val event = CustomColorEvent.OnHsvChanged(240.0, 1.0, 1.0)

        // When
        controller.onEvent(event)

        // Then
        assertEquals(
            "#0000FF",
            controller.state.value.draft
                ?.hexText,
        )
    }

    @Test
    fun `GIVEN New color open WHEN typing a channel THEN the draft follows`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        controller.onEvent(CustomColorEvent.OnAddClicked(CustomColor(0x0000FF).tint, null))
        val event = CustomColorEvent.OnChannelChanged(ColorDraft.ColorChannel.RED, "255")

        // When
        controller.onEvent(event)

        // Then
        assertEquals(
            "#FF00FF",
            controller.state.value.draft
                ?.hexText,
        )
    }

    @Test
    fun `GIVEN a palette color no category uses WHEN long pressing it THEN asks to delete it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(palette = listOf(blue))
            controller.observe()

            // When
            controller.onEvent(CustomColorEvent.OnLongPressed(blue))
            runCurrent()

            // Then
            assertEquals(
                CustomColorDeletion(
                    blue,
                    usedBy = null,
                    isDeleting = false,
                ),
                controller.state.value.deletion,
            )
        }

    @Test
    fun `GIVEN a palette color Food uses WHEN long pressing it THEN says Food uses it and blocks the deletion`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(palette = listOf(blue))
            usedBy[blue] = food
            controller.observe()

            // When
            controller.onEvent(CustomColorEvent.OnLongPressed(blue))
            runCurrent()

            // Then
            val deletion = controller.state.value.deletion
            assertEquals(food, deletion?.usedBy)
            assertEquals(true, deletion?.isBlocked)
        }

    @Test
    fun `GIVEN another member color not in the palette WHEN long pressing it THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(palette = listOf(blue))
            controller.observe()

            // When
            controller.onEvent(CustomColorEvent.OnLongPressed(navy))
            runCurrent()

            // Then
            assertNull(controller.state.value.deletion)
        }

    @Test
    fun `GIVEN Delete this color asked WHEN confirming THEN deletes it and closes`() = runTest(testDispatcher) {
        // Given
        prepareScenario(palette = listOf(blue, yellow))
        controller.observe()
        controller.onEvent(CustomColorEvent.OnLongPressed(blue))
        runCurrent()

        // When
        controller.onEvent(CustomColorEvent.OnDeleteConfirmed)
        runCurrent()

        // Then
        assertEquals(listOf(blue), repository.deletedAt.map { it.first })
        assertEquals(listOf(yellow), controller.state.value.palette)
        assertNull(controller.state.value.deletion)
    }

    @Test
    fun `GIVEN a category took the color since asked WHEN confirming THEN it turns blocked and deletes nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(palette = listOf(blue))
            controller.observe()
            controller.onEvent(CustomColorEvent.OnLongPressed(blue))
            runCurrent()
            usedBy[blue] = food

            // When
            controller.onEvent(CustomColorEvent.OnDeleteConfirmed)
            runCurrent()

            // Then
            assertTrue(repository.deletedAt.isEmpty())
            assertEquals(
                CustomColorDeletion(
                    blue,
                    usedBy = food,
                    isDeleting = false,
                ),
                controller.state.value.deletion,
            )
        }

    @Test
    fun `GIVEN This color is in use shown WHEN confirming THEN deletes nothing and it stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(palette = listOf(blue))
            usedBy[blue] = food
            controller.observe()
            controller.onEvent(CustomColorEvent.OnLongPressed(blue))
            runCurrent()
            val deletion = controller.state.value.deletion

            // When
            controller.onEvent(CustomColorEvent.OnDeleteConfirmed)
            runCurrent()

            // Then
            assertTrue(repository.deletedAt.isEmpty())
            assertEquals(deletion, controller.state.value.deletion)
        }

    @Test
    fun `GIVEN Delete this color asked WHEN cancelling THEN it closes and deletes nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(palette = listOf(blue))
        controller.observe()
        controller.onEvent(CustomColorEvent.OnLongPressed(blue))
        runCurrent()

        // When
        controller.onEvent(CustomColorEvent.OnDeletionDismissed)

        // Then
        assertNull(controller.state.value.deletion)
        assertTrue(repository.deletedAt.isEmpty())
    }

    @Test
    fun `GIVEN the color being deleted WHEN dismissing the dialog THEN it stays open while deleting`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                palette = listOf(blue),
                deleteGate = CompletableDeferred(),
            )
            controller.observe()
            controller.onEvent(CustomColorEvent.OnLongPressed(blue))
            runCurrent()
            controller.onEvent(CustomColorEvent.OnDeleteConfirmed)
            runCurrent()

            // When
            controller.onEvent(CustomColorEvent.OnDeletionDismissed)

            // Then
            assertEquals(
                CustomColorDeletion(
                    blue,
                    usedBy = null,
                    isDeleting = true,
                ),
                controller.state.value.deletion,
            )
        }

    @Test
    fun `GIVEN the color being deleted WHEN the deletion ends THEN the dialog closes`() = runTest(testDispatcher) {
        // Given
        val gate = CompletableDeferred<Unit>()
        prepareScenario(
            palette = listOf(blue),
            deleteGate = gate,
        )
        controller.observe()
        controller.onEvent(CustomColorEvent.OnLongPressed(blue))
        runCurrent()
        controller.onEvent(CustomColorEvent.OnDeleteConfirmed)
        runCurrent()

        // When
        gate.complete(Unit)
        runCurrent()

        // Then
        assertNull(controller.state.value.deletion)
        assertEquals(listOf(blue), repository.deletedAt.map { it.first })
    }

    @Test
    fun `GIVEN Delete this color asked WHEN a sync deletes the color elsewhere THEN the question closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(palette = listOf(blue, yellow))
            controller.observe()
            controller.onEvent(CustomColorEvent.OnLongPressed(blue))
            runCurrent()

            // When
            repository.colors.value = listOf(yellow)
            runCurrent()

            // Then
            assertNull(controller.state.value.deletion)
        }

    @Test
    fun `GIVEN Add in flight for Food and plus tapped for Home WHEN the save fails THEN Home keeps its own draft`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(
                saveError = IllegalStateException("disk full"),
                addGate = gate,
            )
            controller.onEvent(CustomColorEvent.OnAddClicked(CategoryColor.ORANGE.tint, AppCategory.FOOD.key))
            controller.onEvent(CustomColorEvent.OnHexChanged("#3A7BD5"))
            controller.onEvent(CustomColorEvent.OnSaveClicked)
            controller.onEvent(CustomColorEvent.OnAddClicked(CategoryColor.PURPLE.tint, AppCategory.HOME.key))
            val homeDraft = controller.state.value.draft

            // When
            gate.complete(Unit)
            runCurrent()

            // Then
            assertEquals(ColorDraft.createStartingAt(CategoryColor.PURPLE.tint), homeDraft)
            assertEquals(homeDraft, controller.state.value.draft)
        }

    @Test
    fun `GIVEN Add in flight and its picker closed WHEN the save fails THEN New color does not open again`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(
                saveError = IllegalStateException("disk full"),
                addGate = gate,
            )
            controller.onEvent(CustomColorEvent.OnAddClicked(CategoryColor.ORANGE.tint, AppCategory.FOOD.key))
            controller.onEvent(CustomColorEvent.OnHexChanged("#3A7BD5"))
            controller.onEvent(CustomColorEvent.OnSaveClicked)
            controller.onEvent(CustomColorEvent.OnPickerClosed)

            // When
            gate.complete(Unit)
            runCurrent()

            // Then
            assertNull(controller.state.value.draft)
            assertTrue(picked.isEmpty())
        }

    @Test
    fun `GIVEN the palette filled up meanwhile WHEN Add fails as full THEN New color does not open again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(saveError = CustomPaletteFullException())
            controller.onEvent(CustomColorEvent.OnAddClicked(CategoryColor.TEAL.tint, null))
            controller.onEvent(CustomColorEvent.OnHexChanged("#3A7BD5"))

            // When
            controller.onEvent(CustomColorEvent.OnSaveClicked)
            runCurrent()

            // Then
            assertNull(controller.state.value.draft)
            assertTrue(picked.isEmpty())
        }

    @Test
    fun `GIVEN Delete this color asked WHEN the device fails to delete THEN the question stays for another try`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                palette = listOf(blue),
                saveError = IllegalStateException("disk full"),
            )
            controller.observe()
            controller.onEvent(CustomColorEvent.OnLongPressed(blue))
            runCurrent()

            // When
            controller.onEvent(CustomColorEvent.OnDeleteConfirmed)
            runCurrent()

            // Then
            assertEquals(
                CustomColorDeletion(
                    color = blue,
                    usedBy = null,
                    isDeleting = false,
                ),
                controller.state.value.deletion,
            )
            assertEquals(listOf(blue), controller.state.value.palette)
        }

    @Test
    fun `GIVEN the categories fail to read WHEN long pressing a color THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            palette = listOf(blue),
            findError = IllegalStateException("db closed"),
        )
        controller.observe()

        // When
        controller.onEvent(CustomColorEvent.OnLongPressed(blue))
        runCurrent()

        // Then
        assertNull(controller.state.value.deletion)
    }

    @Test
    fun `GIVEN New color open WHEN its picker closes THEN the draft goes`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        controller.onEvent(CustomColorEvent.OnAddClicked(CategoryColor.TEAL.tint, AppCategory.FOOD.key))

        // When
        controller.onEvent(CustomColorEvent.OnPickerClosed)

        // Then
        assertNull(controller.state.value.draft)
    }

    @Test
    fun `GIVEN Delete this color asked WHEN its picker closes THEN the question goes`() = runTest(testDispatcher) {
        // Given
        prepareScenario(palette = listOf(blue))
        controller.observe()
        controller.onEvent(CustomColorEvent.OnLongPressed(blue))
        runCurrent()

        // When
        controller.close()

        // Then
        assertNull(controller.state.value.deletion)
    }

    private fun TestScope.prepareScenario(
        palette: List<CustomColor> = emptyList(),
        saveError: Throwable? = null,
        deleteGate: CompletableDeferred<Unit>? = null,
        addGate: CompletableDeferred<Unit>? = null,
        findError: Throwable? = null,
    ) {
        repository = FakeCustomColorRepository(initialColors = palette).also { it.saveError = saveError }
        val fake = createFakeCustomColorUseCases(repository, usedBy = { usedBy[it] })
        controller = CustomColorController(
            useCases = CustomColorUseCases(
                observeCustomColors = fake.observeCustomColors,
                addCustomColor = { color ->
                    addGate?.await()
                    fake.addCustomColor(color)
                },
                deleteCustomColor = { color ->
                    deleteGate?.await()
                    fake.deleteCustomColor(color)
                },
                findCustomColorUser = { color ->
                    findError?.let { throw it }
                    fake.findCustomColorUser(color)
                },
            ),
            scope = backgroundScope,
            onPicked = { tint, categoryKey -> picked += tint to categoryKey },
        )
    }
}
