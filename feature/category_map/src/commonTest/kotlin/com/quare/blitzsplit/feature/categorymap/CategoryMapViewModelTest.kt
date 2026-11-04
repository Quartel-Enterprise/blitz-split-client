package com.quare.blitzsplit.feature.categorymap

import com.quare.blitzsplit.core.account.domain.model.AccountSnapshot
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTermChange
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteGroupCategoryUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryMapUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveMapCategoriesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.RemoveCategoryTermUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.RestoreCategoryMapUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SaveCategoryTermUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SaveGroupCategoryUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryTermRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiEvent
import com.quare.blitzsplit.feature.categorymap.presentation.model.TermError
import com.quare.blitzsplit.feature.categorymap.presentation.viewmodel.CategoryMapViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryMapViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private lateinit var terms: FakeCategoryTermRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: CategoryMapViewModel
    private val isOnline = MutableStateFlow(true)
    private val firstDownload = MutableStateFlow<FirstDownloadProgress?>(null)

    // Why: null holds the group categories back, as a read Room hasn't answered yet.
    private val groupCategories = MutableStateFlow<Unit?>(Unit)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN the app map WHEN it opens THEN Transporte lists uber under Corrida por app`() = runTest {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        val transport = state.sections.first { it.category.id == AppCategory.TRANSPORT.key }
        val ride = transport.rows.first { it.category.id == AppCategory.TRANSPORT_RIDE.key }
        assertTrue(ride.terms.any { it.term == "uber" })
        assertEquals(60, state.sections.sumOf { it.termCount })
        assertTrue(state.showsDeviceOnlyNotice)
    }

    @Test
    fun `GIVEN uber typed WHEN searching THEN finds uber and uber eats marking the word`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnQueryChanged("Úber"))

        // Then
        val results = viewModel.uiState.value.searchResults
        assertEquals(listOf("uber eats", "uber"), results.map { it.term.term }.sorted().reversed())
        assertTrue(results.all { it.matchEnd - it.matchStart == 4 })
    }

    @Test
    fun `GIVEN a new term WHEN typed one letter or a word another category has THEN says why and Mover`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnAddTermClicked(AppCategory.LEISURE_PARTIES.key, ""))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnTermTextChanged("a"))
        val short = viewModel.uiState.value
        viewModel.onEvent(CategoryMapUiEvent.OnTermTextChanged("Uber"))
        val duplicate = viewModel.uiState.value

        // Then
        assertEquals(TermError.TOO_SHORT, short.termError)
        assertFalse(short.canSaveTerm)
        assertEquals("uber", duplicate.duplicateTerm?.term)
        assertEquals(AppCategory.TRANSPORT_RIDE.key, duplicate.duplicateCategory?.id)
        assertTrue(duplicate.canSaveTerm)
    }

    @Test
    fun `GIVEN a new term WHEN typed past 30 letters THEN says it is too long`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnAddTermClicked(AppCategory.LEISURE_PARTIES.key, ""))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnTermTextChanged("a".repeat(31)))

        // Then
        val state = viewModel.uiState.value
        assertEquals(TermError.TOO_LONG, state.termError)
        assertFalse(state.canSaveTerm)
    }

    @Test
    fun `GIVEN uber moved to Festas WHEN saving THEN the map sends it there`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnAddTermClicked(AppCategory.LEISURE_PARTIES.key, "uber"))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnTermSaveClicked)

        // Then
        val state = viewModel.uiState.value
        assertNull(state.termForm)
        assertEquals(AppCategory.LEISURE_PARTIES.key, state.map.findOrNull("uber")?.categoryId)
        assertEquals(1, state.map.pendingCount)
    }

    @Test
    fun `GIVEN a term renamed WHEN saving THEN the old word leaves the map`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnTermClicked("feira"))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnTermTextChanged("hortifruti"))
        viewModel.onEvent(CategoryMapUiEvent.OnTermSaveClicked)

        // Then
        val map = viewModel.uiState.value.map
        assertNull(map.findOrNull("feira"))
        assertEquals(AppCategory.FOOD_MARKET.key, map.findOrNull("hortifruti")?.categoryId)
    }

    @Test
    fun `GIVEN a term removed WHEN undoing THEN it is back where it was`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnTermClicked("cafe"))
        viewModel.onEvent(CategoryMapUiEvent.OnTermRemoveClicked)
        val removed = viewModel.uiState.value

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnUndoRemoveClicked)

        // Then
        assertNull(removed.map.findOrNull("cafe"))
        assertEquals("café", removed.removedTerm?.term)
        val state = viewModel.uiState.value
        assertNull(state.removedTerm)
        assertEquals(AppCategory.FOOD_BAKERY.key, state.map.findOrNull("cafe")?.categoryId)
    }

    @Test
    fun `GIVEN changes and an own term WHEN restoring THEN the app terms come back and the own one stays`() = runTest {
        // Given
        prepareScenario(
            changes = listOf(
                changeOf("uber", AppCategory.LEISURE_PARTIES.key),
                changeOf("picanha", GROUP_CATEGORY_ID),
            ),
        )

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnRestoreClicked)
        val asking = viewModel.uiState.value
        viewModel.onEvent(CategoryMapUiEvent.OnRestoreConfirmed)

        // Then
        assertTrue(asking.isRestoreConfirmOpen)
        assertEquals(listOf("picanha"), asking.ownTerms.map { it.term })
        val state = viewModel.uiState.value
        assertFalse(state.isRestoreConfirmOpen)
        assertEquals(AppCategory.TRANSPORT_RIDE.key, state.map.findOrNull("uber")?.categoryId)
        assertEquals(GROUP_CATEGORY_ID, state.map.findOrNull("picanha")?.categoryId)
    }

    @Test
    fun `GIVEN a group category WHEN the map opens THEN its terms say only in its group`() = runTest {
        // Given
        prepareScenario(changes = listOf(changeOf("picanha", GROUP_CATEGORY_ID)))

        // When
        val state = viewModel.uiState.value

        // Then
        val food = state.sections.first { it.category.id == AppCategory.FOOD.key }
        val barbecue = food.rows.first { it.category.id == GROUP_CATEGORY_ID }
        assertEquals("Viagem Floripa", barbecue.groupName)
        assertEquals(listOf("picanha"), barbecue.terms.map { it.term })
    }

    @Test
    fun `GIVEN a term of a deleted group category WHEN the map opens THEN it reads as its parent and edits there`() =
        runTest {
            // Given
            prepareScenario(
                changes = listOf(changeOf("picanha", GROUP_CATEGORY_ID)),
                isGroupCategoryDeleted = true,
            )

            // When
            val state = viewModel.uiState.value
            viewModel.onEvent(CategoryMapUiEvent.OnTermClicked("picanha"))

            // Then
            val food = state.sections.first { it.category.id == AppCategory.FOOD.key }
            val onlyFood = food.rows.first { it.isOnlyMain }
            assertTrue(onlyFood.terms.any { it.term == "picanha" })
            val editing = viewModel.uiState.value
            assertEquals(AppCategory.FOOD.key, editing.termForm?.categoryId)
            assertEquals(AppCategory.FOOD.key, editing.formCategory?.id)
        }

    @Test
    fun `GIVEN a term of a group no longer on the device WHEN the map opens THEN it is hidden but still queued`() =
        runTest {
            // Given
            prepareScenario(changes = listOf(changeOf("picanha", "gone-category", isPending = true)))

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnQueryChanged("picanha"))
            val searching = viewModel.uiState.value
            viewModel.onEvent(CategoryMapUiEvent.OnAddTermClicked(AppCategory.FOOD.key, "picanha"))

            // Then
            assertTrue(searching.showsNoResult)
            val shownKeys = searching.sections
                .flatMap { it.rows }
                .flatMap { it.terms }
                .map { it.key }
            assertFalse("picanha" in shownKeys)
            assertEquals(1, searching.map.pendingCount)
            val adding = viewModel.uiState.value
            assertNull(adding.duplicateTerm)
            assertTrue(adding.canSaveTerm)
        }

    @Test
    fun `GIVEN offline with a change queued WHEN the map shows THEN the queue notice shows`() = runTest {
        // Given
        prepareScenario(changes = listOf(changeOf("pizzaria", AppCategory.FOOD_RESTAURANT.key, isPending = true)))

        // When
        isOnline.value = false

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.showsOfflineNotice)
        assertTrue(state.map.findOrNull("pizzaria")?.isPending ?: false)
    }

    @Test
    fun `GIVEN opened from the profile WHEN Abrir Perfil THEN goes back to it`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnProfileClicked)

        // Then
        assertEquals(Route.Profile, navigator.backStack.value.last())
    }

    private val trip = Group(
        id = GROUP_ID,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.BLUE,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = emptyList(),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    @Test
    fun `GIVEN the group categories not read yet WHEN the map opens THEN it is not loaded and lists no section`() =
        runTest {
            // Given
            prepareScenario(
                changes = listOf(changeOf("picanha", GROUP_CATEGORY_ID)),
                holdsGroupCategories = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertTrue(state.sections.isEmpty())
            assertFalse(state.isEmpty)
            assertFalse(state.showsDeviceOnlyNotice)
        }

    @Test
    fun `GIVEN the group categories not read yet WHEN they are read THEN the group term shows in its category`() =
        runTest {
            // Given
            prepareScenario(
                changes = listOf(changeOf("picanha", GROUP_CATEGORY_ID)),
                holdsGroupCategories = true,
            )

            // When
            groupCategories.value = Unit

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isLoaded)
            val food = state.sections.first { it.category.id == AppCategory.FOOD.key }
            assertEquals(
                listOf("picanha"),
                food.rows
                    .first { it.category.id == GROUP_CATEGORY_ID }
                    .terms
                    .map { it.term },
            )
        }

    @Test
    fun `GIVEN a restored Google account whose copy is not whole WHEN the map opens THEN it waits with no notice`() =
        runTest {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                firstDownload = pendingDownload,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertFalse(state.canEditMap)
            assertFalse(state.showsDeviceOnlyNotice)
            assertTrue(state.sections.isEmpty())
        }

    @Test
    fun `GIVEN a restored account whose copy is not whole WHEN adding a term or restoring THEN nothing opens`() =
        runTest {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                firstDownload = pendingDownload,
            )

            // When
            viewModel.onEvent(
                CategoryMapUiEvent.OnAddTermClicked(
                    categoryId = null,
                    text = "pizza",
                ),
            )
            viewModel.onEvent(CategoryMapUiEvent.OnRestoreClicked)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.termForm)
            assertFalse(state.isRestoreConfirmOpen)
        }

    @Test
    fun `GIVEN a restored Google account whose copy is not whole WHEN its first full sync ends THEN the map loads`() =
        runTest {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                firstDownload = pendingDownload,
            )

            // When
            firstDownload.value = null

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isLoaded)
            assertFalse(state.showsDeviceOnlyNotice)
            assertTrue(state.sections.isNotEmpty())
        }

    @Test
    fun `GIVEN the map open WHEN Abrir Perfil THEN sends the profile clicked event`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnProfileClicked)

        // Then
        assertEquals(listOf("category_map_profile_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN the map open WHEN picking a category beside its terms THEN sends the section selected without it`() =
        runTest {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnSectionSelected(AppCategory.TRANSPORT.key))

            // Then
            assertEquals(listOf("category_map_section_selected" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN Transporte picked beside its terms WHEN picking it again THEN sends nothing more`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnSectionSelected(AppCategory.TRANSPORT.key))
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnSectionSelected(AppCategory.TRANSPORT.key))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN nothing picked yet WHEN picking the first category shown beside its terms THEN sends nothing`() =
        runTest {
            // Given
            prepareScenario()
            val shown = viewModel.uiState.value.selectedSection
                ?.category
                ?.id
                .orEmpty()

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnSectionSelected(shown))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the term category picker open WHEN tapping its category again to close it THEN sends nothing`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryMapUiEvent.OnTermClicked("uber"))
            viewModel.onEvent(CategoryMapUiEvent.OnTermCategoryClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnTermCategoryClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the map not read yet WHEN adding a term or restoring THEN sends nothing`() = runTest {
        // Given
        prepareScenario(holdsGroupCategories = true)

        // When
        viewModel.onEvent(
            CategoryMapUiEvent.OnAddTermClicked(
                categoryId = null,
                text = "",
            ),
        )
        viewModel.onEvent(CategoryMapUiEvent.OnRestoreClicked)
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the map open WHEN Adicionar termo from the bar THEN sends the add clicked from the map`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(
            CategoryMapUiEvent.OnAddTermClicked(
                categoryId = null,
                text = "",
            ),
        )

        // Then
        assertEquals(
            listOf("category_term_add_clicked" to mapOf<String, Any>("entry" to "map")),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN the map open WHEN Adicionar termo em Transporte THEN sends the add clicked from the category`() =
        runTest {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(
                CategoryMapUiEvent.OnAddTermClicked(
                    categoryId = AppCategory.TRANSPORT.key,
                    text = "",
                ),
            )

            // Then
            assertEquals(
                listOf("category_term_add_clicked" to mapOf<String, Any>("entry" to "category")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a search that found nothing WHEN adding pizzaria THEN sends the add clicked from the search`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryMapUiEvent.OnQueryChanged("pizzaria"))

            // When
            viewModel.onEvent(
                CategoryMapUiEvent.OnAddTermClicked(
                    categoryId = null,
                    text = "pizzaria",
                ),
            )

            // Then
            assertEquals(
                listOf("category_term_add_clicked" to mapOf<String, Any>("entry" to "search")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the map open WHEN a term is tapped THEN sends the term clicked without it`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnTermClicked("uber"))

        // Then
        assertEquals(listOf("category_term_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN Editar termo open WHEN tapping its category THEN sends the term category clicked`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnTermClicked("uber"))
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnTermCategoryClicked)

        // Then
        assertEquals(listOf("category_term_category_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN the term category picker open WHEN picking Festas THEN sends the term category picked without it`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryMapUiEvent.OnTermClicked("uber"))
            viewModel.onEvent(CategoryMapUiEvent.OnTermCategoryClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnTermCategoryPicked(AppCategory.LEISURE_PARTIES.key))

            // Then
            assertEquals(listOf("category_term_category_picked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN Editar termo open WHEN Remover do mapa THEN sends the remove clicked`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnTermClicked("cafe"))
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnTermRemoveClicked)

        // Then
        assertEquals(listOf("category_term_remove_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN a term removed WHEN Desfazer THEN sends the undo clicked`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnTermClicked("cafe"))
        viewModel.onEvent(CategoryMapUiEvent.OnTermRemoveClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnUndoRemoveClicked)

        // Then
        assertEquals(listOf("category_term_undo_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN a new term typed WHEN saving THEN sends the save clicked then the term saved as new`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnAddTermClicked(AppCategory.FOOD_RESTAURANT.key, ""))
        viewModel.onEvent(CategoryMapUiEvent.OnTermTextChanged("pizzaria"))
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnTermSaveClicked)

        // Then
        assertEquals(
            listOf(
                "category_term_save_clicked" to emptyMap(),
                "category_term_saved" to mapOf<String, Any>("is_new" to true),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN a term renamed WHEN saving THEN sends the term saved as not new`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnTermClicked("feira"))
        viewModel.onEvent(CategoryMapUiEvent.OnTermTextChanged("hortifruti"))
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnTermSaveClicked)

        // Then
        assertEquals(
            listOf(
                "category_term_save_clicked" to emptyMap(),
                "category_term_saved" to mapOf<String, Any>("is_new" to false),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN a term moved to Festas in Editar termo WHEN saving THEN sends the term saved as not new`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnTermClicked("uber"))
        viewModel.onEvent(CategoryMapUiEvent.OnTermCategoryPicked(AppCategory.LEISURE_PARTIES.key))
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnTermSaveClicked)

        // Then
        assertEquals(
            listOf(
                "category_term_save_clicked" to emptyMap(),
                "category_term_saved" to mapOf<String, Any>("is_new" to false),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN the device fails to save WHEN saving a new term THEN sends only the save clicked`() = runTest {
        // Given
        prepareScenario(saveError = IllegalStateException("disk full"))
        viewModel.onEvent(CategoryMapUiEvent.OnAddTermClicked(AppCategory.FOOD_RESTAURANT.key, "pizzaria"))
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnTermSaveClicked)

        // Then
        assertEquals(listOf("category_term_save_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN a new term of one letter WHEN saving THEN sends only the save clicked`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnAddTermClicked(AppCategory.FOOD_RESTAURANT.key, ""))
        viewModel.onEvent(CategoryMapUiEvent.OnTermTextChanged("a"))
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnTermSaveClicked)

        // Then
        assertEquals(listOf("category_term_save_clicked" to emptyMap<String, Any>()), trackedEvents)
        assertNull(terms.changes.value.firstOrNull { it.term == "a" })
    }

    @Test
    fun `GIVEN the map open WHEN Restaurar padrao THEN sends the restore clicked`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnRestoreClicked)

        // Then
        assertEquals(listOf("category_map_restore_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN the restore question open WHEN confirming THEN sends the restore confirmed`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnRestoreClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnRestoreConfirmed)

        // Then
        assertEquals(listOf("category_map_restore_confirmed" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN the restore confirmed WHEN the Apple alert dismisses itself THEN sends nothing more`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnRestoreClicked)
        viewModel.onEvent(CategoryMapUiEvent.OnRestoreConfirmed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnRestoreDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the restore question open WHEN dismissing it THEN sends the restore cancelled`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnRestoreClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnRestoreDismissed)

        // Then
        assertEquals(listOf("category_map_restore_cancelled" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN no restore question open WHEN confirming THEN sends nothing`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnRestoreConfirmed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN no restore question open WHEN dismissing THEN sends nothing`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnRestoreDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the map open WHEN tapping back THEN sends nothing`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnBackClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the map open WHEN typing a search THEN sends nothing`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnQueryChanged("ub"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Novo termo open WHEN typing the term THEN sends nothing`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(
            CategoryMapUiEvent.OnAddTermClicked(
                categoryId = null,
                text = "",
            ),
        )
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnTermTextChanged("pizzaria"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Novo termo open WHEN closing it THEN sends nothing`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(
            CategoryMapUiEvent.OnAddTermClicked(
                categoryId = null,
                text = "",
            ),
        )
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnTermDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a term removed WHEN its notice goes away THEN sends nothing`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnTermClicked("cafe"))
        viewModel.onEvent(CategoryMapUiEvent.OnTermRemoveClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnRemovedNoticeDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    private fun changeOf(
        term: String,
        categoryId: String?,
        isPending: Boolean = false,
    ): CategoryTermChange = CategoryTermChange(
        key = com.quare.blitzsplit.core.expenses.domain.model.CategoryMatcher
            .keyOf(term),
        term = term,
        categoryId = categoryId,
        isRestored = false,
        isPending = isPending,
    )

    private val connectedWithGoogle = AccountStatus.Connected("user-1", AccountProvider.GOOGLE)

    private val pendingDownload = FirstDownloadProgress(
        arrivedGroupCount = 0,
        groupCount = null,
        isWaitingForInternet = false,
    )

    private fun prepareScenario(
        changes: List<CategoryTermChange> = emptyList(),
        isGroupCategoryDeleted: Boolean = false,
        status: AccountStatus = AccountStatus.LocalOnly,
        firstDownload: FirstDownloadProgress? = null,
        holdsGroupCategories: Boolean = false,
        saveError: Throwable? = null,
    ) {
        this.firstDownload.value = firstDownload
        groupCategories.value = if (holdsGroupCategories) null else Unit
        terms = FakeCategoryTermRepository(initialChanges = changes).also { it.saveError = saveError }
        val categories = FakeCategoryRepository(
            initialGroupCategories = listOf(
                GroupCategory(
                    id = GROUP_CATEGORY_ID,
                    groupId = GROUP_ID,
                    name = "Churrasco",
                    parentId = AppCategory.FOOD.key,
                    icon = null,
                    createdByMemberId = null,
                    isDeleted = isGroupCategoryDeleted,
                    isPending = false,
                ),
            ),
        )
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.Profile)
            it.navigate(Route.CategoryMap)
        }
        viewModel = CategoryMapViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            navigator = navigator,
            saveCategoryTerm = SaveCategoryTermUseCase(terms) { NOW },
            removeCategoryTerm = RemoveCategoryTermUseCase(terms) { NOW },
            restoreCategoryMap = RestoreCategoryMapUseCase(terms) { NOW },
            saveGroupCategory = SaveGroupCategoryUseCase(categories) { NOW },
            deleteGroupCategory = DeleteGroupCategoryUseCase(categories) { NOW },
            observeCategoryMap = ObserveCategoryMapUseCase(terms),
            observeMapCategories = {
                groupCategories.filterNotNull().flatMapLatest { ObserveMapCategoriesUseCase(categories)() }
            },
            observeGroupsByActivity = { flowOf(listOf(trip)) },
            observeExpenses = { flowOf(emptyList()) },
            observeAccount = {
                flowOf(
                    AccountSnapshot(
                        status = status,
                        recoveryCode = null,
                        recoveryCodePrompt = RecoveryCodePrompt.NOT_DUE,
                        email = null,
                        signOutWhenSynced = false,
                        awaitsFullSync = false,
                    ),
                )
            },
            observeFirstDownload = ObserveFirstDownload { this.firstDownload },
            connectivityObserver = NetworkConnectivityObserver { isOnline },
        )
    }

    private companion object {
        const val NOW = 1_000L
        const val GROUP_ID = "group-1"
        const val GROUP_CATEGORY_ID = "11111111-1111-1111-1111-111111111111"
    }
}
